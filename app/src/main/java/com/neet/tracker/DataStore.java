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
 *   "tests": [ { "subject":"physics","date":"2026-07-10","score":68,"total":90 }, ... ]
 * }
 */
public class DataStore {
    private static final String PREFS = "neet_tracker_prefs";
    private static final String KEY_DATA = "neet_tracker_data";

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
            save();
        } catch (JSONException e) { /* ignore */ }
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

    public double todayTotalHours() {
        String today = todayStr();
        double total = 0;
        List<String> keys = new ArrayList<>(Arrays.asList(Syllabus.SUBJECT_KEYS));
        keys.addAll(Syllabus.EXTRA_ACTIVITIES.keySet());
        for (String k : keys) total += getLogHours(today, k);
        return total;
    }

    /** Current daily-logging streak (consecutive days with any hours logged, ending today or yesterday) */
    public int currentStreak() {
        try {
            JSONObject logs = root.getJSONObject("logs");
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            int streak = 0;
            for (int i = 0; i < 3650; i++) {
                String d = fmt.format(cal.getTime());
                JSONObject day = logs.optJSONObject(d);
                boolean hasLog = false;
                if (day != null) {
                    Iterator<String> it = day.keys();
                    while (it.hasNext()) { if (day.optDouble(it.next(), 0) > 0) { hasLog = true; break; } }
                }
                if (hasLog) { streak++; cal.add(Calendar.DATE, -1); }
                else if (i == 0) { cal.add(Calendar.DATE, -1); } // allow today to be empty so far
                else break;
            }
            return streak;
        } catch (JSONException e) { return 0; }
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

    // ---------- Aggregates for charts ----------
    public double dayTotalHours(String date) {
        List<String> keys = new ArrayList<>(Arrays.asList(Syllabus.SUBJECT_KEYS));
        keys.addAll(Syllabus.EXTRA_ACTIVITIES.keySet());
        double total = 0;
        for (String k : keys) total += getLogHours(date, k);
        return total;
    }

    /** Last 7 days (oldest first) as {date, totalHours} pairs */
    public LinkedHashMap<String, Double> last7DaysTotals() {
        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
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

    public static long daysUntilExam() {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            long diff = fmt.parse(Syllabus.NEET_EXAM_DATE).getTime() - System.currentTimeMillis();
            return Math.max(0, diff / (1000 * 60 * 60 * 24));
        } catch (Exception e) { return 0; }
    }
}
