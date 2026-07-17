package com.neet.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Handles all read/write of tracker data.
 * Schema (JSON):
 * {
 *   "checklist": { "physics|Class 11|Motion in a Plane|pw": true, ... },
 *   "logs": { "2026-07-15": { "physics": 2.5, "sleep": 7, ... }, ... },
 *   "tests": [ { "date":"2026-07-10","score":560,"total":720 }, ... ],
 *   "chapterMeta": { "physics|Class 11|Motion in a Plane": { "completedAt":"...", "revisions":[{"stage":1,"due":"...","done":false}, ...] } }
 * }
 */
public class DataStore {
    private static final String PREFS = "neet_tracker_prefs";
    private static final String KEY_DATA = "neet_tracker_data";
    private static final int[] REVISION_STAGE_DAYS = {1, 3, 7, 15, 30};

    private final SharedPreferences prefs;
    private JSONObject root;

    public DataStore(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        String raw = prefs.getString(KEY_DATA, null);
        try {
            if (raw != null) {
                root = new JSONObject(raw);
            } else {
                root = new JSONObject();
                root.put("checklist", new JSONObject());
                root.put("logs", new JSONObject());
                root.put("tests", new JSONArray());
                root.put("chapterMeta", new JSONObject());
            }
        } catch (JSONException e) {
            root = new JSONObject();
        }
    }

    private void save() {
        prefs.edit().putString(KEY_DATA, root.toString()).apply();
    }

    public static String todayStr() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private static String addDaysToDateStr(String dateStr, int days) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Calendar cal = Calendar.getInstance();
            cal.setTime(fmt.parse(dateStr));
            cal.add(Calendar.DATE, days);
            return fmt.format(cal.getTime());
        } catch (Exception e) { return dateStr; }
    }

    private static int daysBetween(String fromStr, String toStr) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            long diff = fmt.parse(toStr).getTime() - fmt.parse(fromStr).getTime();
            return (int) Math.round(diff / 86400000.0);
        } catch (Exception e) { return 0; }
    }

    // ---------- Checklist (subtask completion) ----------
    private String chapterKey(String subject, String group, String chapter, String subtask) {
        return subject + "|" + group + "|" + chapter + "|" + subtask;
    }

    public boolean isSubtaskDone(String subject, String group, String chapter, String subtask) {
        try {
            JSONObject checklist = root.getJSONObject("checklist");
            String k = chapterKey(subject, group, chapter, subtask);
            return checklist.optBoolean(k, false);
        } catch (JSONException e) { return false; }
    }

    public void setSubtaskDone(String subject, String group, String chapter, String subtask, boolean done) {
        try {
            JSONObject checklist = root.getJSONObject("checklist");
            checklist.put(chapterKey(subject, group, chapter, subtask), done);
            maybeScheduleRevisions(subject, group, chapter);
            save();
        } catch (JSONException e) { /* ignore */ }
    }

    /** Once all 5 subtasks of a chapter are done for the first time, schedule forgetting-curve revisions. */
    private void maybeScheduleRevisions(String subject, String group, String chapter) throws JSONException {
        for (String st : Syllabus.SUBTASK_KEYS) {
            if (!isSubtaskDone(subject, group, chapter, st)) return;
        }
        if (!root.has("chapterMeta")) root.put("chapterMeta", new JSONObject());
        JSONObject chapterMeta = root.getJSONObject("chapterMeta");
        String key = subject + "|" + group + "|" + chapter;
        if (chapterMeta.has(key)) return;

        JSONObject meta = new JSONObject();
        meta.put("completedAt", todayStr());
        JSONArray revs = new JSONArray();
        for (int stageDays : REVISION_STAGE_DAYS) {
            JSONObject r = new JSONObject();
            r.put("stage", stageDays);
            r.put("due", addDaysToDateStr(todayStr(), stageDays));
            r.put("done", false);
            revs.put(r);
        }
        meta.put("revisions", revs);
        chapterMeta.put(key, meta);
    }

    public void markRevisionDone(String subject, String group, String chapter, int stage) {
        try {
            JSONObject chapterMeta = root.optJSONObject("chapterMeta");
            if (chapterMeta == null) return;
            String key = subject + "|" + group + "|" + chapter;
            JSONObject meta = chapterMeta.optJSONObject(key);
            if (meta == null) return;
            JSONArray revs = meta.optJSONArray("revisions");
            if (revs == null) return;
            for (int i = 0; i < revs.length(); i++) {
                JSONObject r = revs.optJSONObject(i);
                if (r != null && r.optInt("stage") == stage) {
                    r.put("done", true);
                    r.put("doneAt", todayStr());
                }
            }
            save();
        } catch (JSONException e) { /* ignore */ }
    }

    /** Returns {"due": [...], "upcoming": [...]} — each item has subject,label,color,group,chapter,stage,days */
    public JSONObject getRevisionQueues() {
        JSONArray dueOut = new JSONArray();
        JSONArray upcomingOut = new JSONArray();
        try {
            JSONObject chapterMeta = root.optJSONObject("chapterMeta");
            if (chapterMeta != null) {
                String today = todayStr();
                List<JSONObject> dueList = new ArrayList<JSONObject>();
                List<JSONObject> upcomingList = new ArrayList<JSONObject>();
                Iterator<String> keys = chapterMeta.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String[] parts = key.split("\\|", 3);
                    if (parts.length < 3) continue;
                    String subject = parts[0], group = parts[1], chapter = parts[2];
                    Syllabus.Subject subj = Syllabus.SUBJECTS.get(subject);
                    if (subj == null) continue;
                    JSONObject meta = chapterMeta.optJSONObject(key);
                    if (meta == null) continue;
                    JSONArray revs = meta.optJSONArray("revisions");
                    if (revs == null) continue;
                    JSONObject nextStage = null;
                    for (int i = 0; i < revs.length(); i++) {
                        JSONObject r = revs.optJSONObject(i);
                        if (r != null && !r.optBoolean("done", false)) { nextStage = r; break; }
                    }
                    if (nextStage == null) continue;

                    String due = nextStage.optString("due");
                    JSONObject item = new JSONObject();
                    item.put("subject", subject);
                    item.put("label", subj.label);
                    item.put("color", subj.color);
                    item.put("group", group);
                    item.put("chapter", chapter);
                    item.put("stage", nextStage.optInt("stage"));
                    if (due.compareTo(today) <= 0) {
                        item.put("days", daysBetween(due, today));
                        dueList.add(item);
                    } else {
                        item.put("days", daysBetween(today, due));
                        upcomingList.add(item);
                    }
                }
                Collections.sort(dueList, new Comparator<JSONObject>() {
                    public int compare(JSONObject a, JSONObject b) { return b.optInt("days") - a.optInt("days"); }
                });
                Collections.sort(upcomingList, new Comparator<JSONObject>() {
                    public int compare(JSONObject a, JSONObject b) { return a.optInt("days") - b.optInt("days"); }
                });
                for (JSONObject o : dueList) dueOut.put(o);
                int limit = Math.min(8, upcomingList.size());
                for (int i = 0; i < limit; i++) upcomingOut.put(upcomingList.get(i));
            }
        } catch (JSONException e) { /* ignore */ }
        JSONObject result = new JSONObject();
        try { result.put("due", dueOut); result.put("upcoming", upcomingOut); } catch (JSONException e) { /* ignore */ }
        return result;
    }

    /** Fraction (0-1) of subtasks completed across all chapters in a subject */
    public double subjectProgress(String subjectKey) {
        Syllabus.Subject subj = Syllabus.SUBJECTS.get(subjectKey);
        if (subj == null) return 0;
        int total = 0, done = 0;
        try {
            JSONObject checklist = root.getJSONObject("checklist");
            for (Map.Entry<String, String[]> g : subj.groups.entrySet()) {
                for (String chapter : g.getValue()) {
                    for (String st : Syllabus.SUBTASK_KEYS) {
                        total++;
                        if (checklist.optBoolean(chapterKey(subjectKey, g.getKey(), chapter, st), false)) done++;
                    }
                }
            }
        } catch (JSONException e) { /* ignore */ }
        return total == 0 ? 0 : (double) done / total;
    }

    // ---------- Daily hour logs ----------
    public double getLogHours(String date, String activityKey) {
        try {
            JSONObject logs = root.getJSONObject("logs");
            JSONObject day = logs.optJSONObject(date);
            if (day == null) return 0;
            return day.optDouble(activityKey, 0);
        } catch (JSONException e) { return 0; }
    }

    public void setLogHours(String date, String activityKey, double hours) {
        try {
            JSONObject logs = root.getJSONObject("logs");
            JSONObject day = logs.optJSONObject(date);
            if (day == null) { day = new JSONObject(); logs.put(date, day); }
            day.put(activityKey, Math.max(0, hours));
            save();
        } catch (JSONException e) { /* ignore */ }
    }

    /** Sum of Physics+Chemistry+Biology hours only (excludes sleep/chores) — "study" hours */
    public double dayStudyHours(String date) {
        double total = 0;
        for (String k : Syllabus.SUBJECT_KEYS) total += getLogHours(date, k);
        return total;
    }

    /** Sum of every logged activity (subjects + sleep + chores) */
    public double dayTotalHours(String date) {
        List<String> keys = new ArrayList<String>(Arrays.asList(Syllabus.SUBJECT_KEYS));
        keys.addAll(Syllabus.EXTRA_ACTIVITIES.keySet());
        double total = 0;
        for (String k : keys) total += getLogHours(date, k);
        return total;
    }

    public double todayStudyHours() { return dayStudyHours(todayStr()); }
    public double todayTotalHours() { return dayTotalHours(todayStr()); }

    /** Current streak: consecutive days (ending today or yesterday) with study hours > 0 */
    public int currentStreak() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        int streak = 0;
        for (int i = 0; i < 3650; i++) {
            String d = fmt.format(cal.getTime());
            boolean hasStudy = dayStudyHours(d) > 0;
            if (hasStudy) { streak++; cal.add(Calendar.DATE, -1); }
            else if (i == 0) { cal.add(Calendar.DATE, -1); } // allow today to still be empty
            else break;
        }
        return streak;
    }

    /** Last 7 days (oldest first) as {date, totalHours} pairs, using all-activity totals */
    public LinkedHashMap<String, Double> last7DaysTotals() {
        LinkedHashMap<String, Double> out = new LinkedHashMap<String, Double>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -6);
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        for (int i = 0; i < 7; i++) {
            String d = fmt.format(cal.getTime());
            out.put(d, dayTotalHours(d));
            cal.add(Calendar.DATE, 1);
        }
        return out;
    }

    // ---------- Tests (no subject tagging) ----------
    public void addTest(String date, double score, double total) {
        try {
            JSONArray tests = root.getJSONArray("tests");
            JSONObject t = new JSONObject();
            t.put("date", date);
            t.put("score", score); t.put("total", total);
            tests.put(t);
            save();
        } catch (JSONException e) { /* ignore */ }
    }

    public void deleteTest(int index) {
        try {
            JSONArray tests = root.getJSONArray("tests");
            JSONArray updated = new JSONArray();
            for (int i = 0; i < tests.length(); i++) if (i != index) updated.put(tests.get(i));
            root.put("tests", updated);
            save();
        } catch (JSONException e) { /* ignore */ }
    }

    public JSONArray getTests() {
        try { return root.getJSONArray("tests"); } catch (JSONException e) { return new JSONArray(); }
    }

    public int subjectCompletedCount(String subjectKey) {
        Syllabus.Subject subj = Syllabus.SUBJECTS.get(subjectKey);
        if (subj == null) return 0;
        int done = 0;
        try {
            JSONObject checklist = root.getJSONObject("checklist");
            for (Map.Entry<String, String[]> g : subj.groups.entrySet()) {
                for (String chapter : g.getValue()) {
                    for (String st : Syllabus.SUBTASK_KEYS) {
                        if (checklist.optBoolean(chapterKey(subjectKey, g.getKey(), chapter, st), false)) done++;
                    }
                }
            }
        } catch (JSONException e) { /* ignore */ }
        return done;
    }

    public int subjectTotalSubtasks(String subjectKey) {
        Syllabus.Subject subj = Syllabus.SUBJECTS.get(subjectKey);
        if (subj == null) return 0;
        int total = 0;
        for (Map.Entry<String, String[]> g : subj.groups.entrySet()) {
            total += g.getValue().length * Syllabus.SUBTASK_KEYS.length;
        }
        return total;
    }

    public double chapterProgress(String subject, String group, String chapter) {
        int done = 0;
        for (String st : Syllabus.SUBTASK_KEYS) {
            if (isSubtaskDone(subject, group, chapter, st)) done++;
        }
        return (double) done / Syllabus.SUBTASK_KEYS.length;
    }

    // ---------- Countdown ----------
    public static long millisUntilExam() {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            long diff = fmt.parse(Syllabus.NEET_EXAM_DATE).getTime() - System.currentTimeMillis();
            return Math.max(0, diff);
        } catch (Exception e) { return 0; }
    }
}
