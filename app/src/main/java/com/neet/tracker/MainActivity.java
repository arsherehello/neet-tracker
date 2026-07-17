package com.neet.tracker;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {

    // ---- palette, ported from BASE_CSS custom properties ----
    private static final int BG = Color.parseColor("#F5FAF9");
    private static final int SURFACE = Color.parseColor("#FFFFFF");
    private static final int INK = Color.parseColor("#0B2A2E");
    private static final int MUTED = Color.parseColor("#6B8489");
    private static final int BORDER = Color.parseColor("#DCEAE8");
    private static final int TEAL = Color.parseColor("#0E7C7B");
    private static final int TEAL_DARK = Color.parseColor("#0A5F5E");
    private static final int CORAL = Color.parseColor("#FF6A55");
    private static final int MOOD_BAR_WIDTH_DP = 140;
    private static final String[] TAB_LABELS = {"Today", "Syllabus", "Revise", "Tests"};

    private DataStore store;
    private FrameLayout content;
    private TextView streakNum, todayTotalLine, moodCaption;
    private TextView cdDays, cdHours, cdMin, cdSec;
    private View moodBarFill, moodFace;
    private Button[] tabBtns;
    private int activeTab = 0;
    private SubjectPieChart pieP, pieC, pieB;

    private final Handler tickHandler = new Handler();
    private final Runnable tickRunnable = new Runnable() {
        public void run() {
            updateCountdown();
            tickHandler.postDelayed(this, 1000);
        }
    };

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private GradientDrawable rounded(int fill, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private GradientDrawable roundedBordered(int fill, int radiusDp, int borderColor, int borderWidthDp) {
        GradientDrawable d = rounded(fill, radiusDp);
        d.setStroke(dp(borderWidthDp), borderColor);
        return d;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new DataStore(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        root.addView(buildHeader());
        root.addView(buildTabBar());

        content = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setPadding(dp(16), 0, dp(16), 0);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        showTab(0);
        refreshHeader();
    }

    @Override
    protected void onResume() {
        super.onResume();
        tickHandler.post(tickRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        tickHandler.removeCallbacks(tickRunnable);
    }

    // ---------------- HEADER ----------------
    private LinearLayout buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(20), dp(16), dp(12));

        TextView eyebrow = new TextView(this);
        eyebrow.setText("NEET \u00b7 PHYSICS \u00b7 CHEMISTRY \u00b7 BIOLOGY");
        eyebrow.setTextColor(TEAL);
        eyebrow.setTextSize(10);
        eyebrow.setTypeface(Typeface.MONOSPACE);
        eyebrow.setLetterSpacing(0.06f);
        header.addView(eyebrow);

        TextView title = new TextView(this);
        title.setText("Prep Tracker");
        title.setTextColor(INK);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(2), 0, dp(14));
        header.addView(title);

        // ---- donut row: per-subject completion, beside streak/countdown ----
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout donutRow = new LinearLayout(this);
        donutRow.setOrientation(LinearLayout.HORIZONTAL);
        pieP = subjectRing();
        pieC = subjectRing();
        pieB = subjectRing();
        donutRow.addView(ringCol(pieP, "Phys"));
        donutRow.addView(ringCol(pieC, "Chem"));
        donutRow.addView(ringCol(pieB, "Biol"));
        LinearLayout.LayoutParams donutLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        donutRow.setLayoutParams(donutLp);
        statsRow.addView(donutRow);

        LinearLayout scCol = new LinearLayout(this);
        scCol.setOrientation(LinearLayout.VERTICAL);
        scCol.setGravity(Gravity.END);
        LinearLayout.LayoutParams scColLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scColLp.leftMargin = dp(12);
        scCol.setLayoutParams(scColLp);
        View leftBorder = null; // (visual left border approximated by padding, kept simple)
        scCol.setPadding(dp(12), 0, 0, 0);
        GradientDrawable borderBg = new GradientDrawable();
        borderBg.setColor(Color.TRANSPARENT);
        scCol.setBackground(null);

        // countdown block
        LinearLayout countdownBlock = new LinearLayout(this);
        countdownBlock.setOrientation(LinearLayout.VERTICAL);
        countdownBlock.setGravity(Gravity.END);
        TextView countdownTitle = new TextView(this);
        countdownTitle.setText("NEET 2027");
        countdownTitle.setTextColor(MUTED);
        countdownTitle.setTextSize(10);
        countdownTitle.setTypeface(Typeface.DEFAULT_BOLD);
        countdownTitle.setPadding(0, 0, 0, dp(4));
        countdownBlock.addView(countdownTitle);

        LinearLayout countdownRow = new LinearLayout(this);
        countdownRow.setOrientation(LinearLayout.HORIZONTAL);
        countdownRow.setGravity(Gravity.CENTER_VERTICAL);
        cdDays = new TextView(this);
        cdHours = new TextView(this);
        cdMin = new TextView(this);
        cdSec = new TextView(this);
        countdownRow.addView(countdownUnit(cdDays, "DAYS"));
        countdownRow.addView(countdownColon());
        countdownRow.addView(countdownUnit(cdHours, "HRS"));
        countdownRow.addView(countdownColon());
        countdownRow.addView(countdownUnit(cdMin, "MIN"));
        countdownRow.addView(countdownColon());
        countdownRow.addView(countdownUnit(cdSec, "SEC"));
        countdownBlock.addView(countdownRow);
        scCol.addView(countdownBlock);

        // streak block
        LinearLayout streakBlock = new LinearLayout(this);
        streakBlock.setOrientation(LinearLayout.VERTICAL);
        streakBlock.setGravity(Gravity.END);
        LinearLayout.LayoutParams streakBlockLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        streakBlockLp.topMargin = dp(10);
        streakBlock.setLayoutParams(streakBlockLp);
        streakNum = new TextView(this);
        streakNum.setTextColor(CORAL);
        streakNum.setTextSize(20);
        streakNum.setTypeface(Typeface.DEFAULT_BOLD);
        streakBlock.addView(streakNum);
        TextView streakLabel = new TextView(this);
        streakLabel.setText("DAY STREAK");
        streakLabel.setTextColor(MUTED);
        streakLabel.setTextSize(10);
        streakBlock.addView(streakLabel);
        scCol.addView(streakBlock);

        statsRow.addView(scCol);
        header.addView(statsRow);

        // ---- emoji meter: gradient bar with moving face ----
        FrameLayout moodTrack = new FrameLayout(this);
        moodTrack.setBackground(rounded(BORDER, 5));
        LinearLayout.LayoutParams moodTrackLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(10));
        moodTrackLp.topMargin = dp(14);
        moodTrack.setLayoutParams(moodTrackLp);

        moodBarFill = new View(this);
        GradientDrawable moodGradient = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{TEAL, CORAL});
        moodGradient.setCornerRadius(dp(5));
        moodBarFill.setBackground(moodGradient);
        FrameLayout.LayoutParams moodFillLp = new FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        moodBarFill.setLayoutParams(moodFillLp);
        moodTrack.addView(moodBarFill);

        moodFace = new TextView(this);
        ((TextView) moodFace).setTextSize(16);
        ((TextView) moodFace).setText("\uD83D\uDE34");
        FrameLayout.LayoutParams moodFaceLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        moodFaceLp.topMargin = -dp(7);
        moodFace.setLayoutParams(moodFaceLp);
        moodTrack.addView(moodFace);

        header.addView(moodTrack);

        moodCaption = new TextView(this);
        moodCaption.setTextColor(MUTED);
        moodCaption.setTextSize(12);
        LinearLayout.LayoutParams captionLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        captionLp.topMargin = dp(8);
        moodCaption.setLayoutParams(captionLp);
        header.addView(moodCaption);

        return header;
    }

    private LinearLayout countdownUnit(TextView numView, String label) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(2), 0, dp(2), 0);
        col.setLayoutParams(lp);
        col.setMinimumWidth(dp(26));

        numView.setTextColor(TEAL_DARK);
        numView.setTextSize(18);
        numView.setTypeface(Typeface.DEFAULT_BOLD);
        numView.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(numView);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(MUTED);
        lbl.setTextSize(8);
        lbl.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(lbl);
        return col;
    }

    private TextView countdownColon() {
        TextView colon = new TextView(this);
        colon.setText(":");
        colon.setTextColor(BORDER);
        colon.setTextSize(16);
        colon.setTypeface(Typeface.DEFAULT_BOLD);
        return colon;
    }

    private SubjectPieChart subjectRing() {
        return new SubjectPieChart(this, null);
    }

    private LinearLayout ringCol(SubjectPieChart ring, String label) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        colLp.setMargins(0, 0, dp(10), 0);
        col.setLayoutParams(colLp);

        LinearLayout.LayoutParams ringLp = new LinearLayout.LayoutParams(dp(48), dp(48));
        ring.setLayoutParams(ringLp);
        col.addView(ring);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(MUTED);
        tv.setTextSize(10);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(3), 0, 0);
        col.addView(tv);
        return col;
    }

    // ---------------- TAB BAR ----------------
    private LinearLayout buildTabBar() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(dp(8), 0, dp(8), 0);

        tabBtns = new Button[TAB_LABELS.length];
        for (int i = 0; i < TAB_LABELS.length; i++) {
            final int idx = i;
            Button b = flatTab(TAB_LABELS[i]);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showTab(idx); }
            });
            tabBtns[i] = b;
            tabRow.addView(b);
        }
        wrap.addView(tabRow);

        View bottomBorder = new View(this);
        bottomBorder.setBackgroundColor(BORDER);
        bottomBorder.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        wrap.addView(bottomBorder);

        return wrap;
    }

    private Button flatTab(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        b.setLayoutParams(lp);
        return b;
    }

    private void showTab(int index) {
        activeTab = index;
        for (int i = 0; i < tabBtns.length; i++) tabBtns[i].setTextColor(i == index ? TEAL_DARK : MUTED);

        content.removeAllViews();
        if (index == 0) content.addView(buildTodayTab());
        else if (index == 1) content.addView(buildSyllabusTab());
        else if (index == 2) content.addView(buildRevisionTab());
        else content.addView(buildTestsTab());
    }

    private void refreshHeader() {
        double studyToday = store.todayStudyHours();
        double allToday = store.todayTotalHours();
        int streak = store.currentStreak();
        streakNum.setText(String.valueOf(streak));

        double intensity = Math.min(studyToday / 6.0, 1.0);
        int pct = (int) Math.round(intensity * 100);
        int trackW = dp(MOOD_BAR_WIDTH_DP);
        int fillW = Math.max((int) (trackW * intensity), dp(4));
        ViewGroup.LayoutParams flp = moodBarFill.getLayoutParams();
        flp.width = fillW;
        moodBarFill.setLayoutParams(flp);

        FrameLayout.LayoutParams faceLp = (FrameLayout.LayoutParams) moodFace.getLayoutParams();
        int faceLeft = (int) (trackW * intensity) - dp(9);
        if (faceLeft < 0) faceLeft = 0;
        if (faceLeft > trackW - dp(10)) faceLeft = trackW - dp(10);
        faceLp.leftMargin = faceLeft;
        moodFace.setLayoutParams(faceLp);
        ((TextView) moodFace).setText(moodEmoji(intensity));

        if (studyToday > 0) {
            moodCaption.setText(String.format(Locale.US, "%.1f hrs of study today \u00b7 %.1f / 24 hrs logged overall", studyToday, allToday));
        } else {
            moodCaption.setText("No study logged yet today \u2014 log something to get moving");
        }

        double pPhy = store.subjectProgress("physics");
        double pChem = store.subjectProgress("chemistry");
        double pBio = store.subjectProgress("biology");
        setSingleRing(pieP, TEAL, pPhy);
        setSingleRing(pieC, Color.parseColor(Syllabus.SUBJECTS.get("chemistry").color), pChem);
        setSingleRing(pieB, Color.parseColor(Syllabus.SUBJECTS.get("biology").color), pBio);

        updateCountdown();
        updateWidget();
    }

    private String moodEmoji(double intensity) {
        if (intensity <= 0) return "\uD83D\uDE34";
        if (intensity < 0.25) return "\uD83D\uDE10";
        if (intensity < 0.5) return "\uD83D\uDE42";
        if (intensity < 0.75) return "\uD83D\uDE03";
        return "\uD83D\uDD25";
    }

    private void setSingleRing(SubjectPieChart ring, int color, double fraction) {
        LinkedHashMap<String, Double> shares = new LinkedHashMap<String, Double>();
        LinkedHashMap<String, Integer> colors = new LinkedHashMap<String, Integer>();
        shares.put("v", fraction);
        colors.put("v", color);
        ring.setData(shares, colors, Math.round(fraction * 100) + "%");
    }

    private void updateCountdown() {
        long ms = DataStore.millisUntilExam();
        long days = ms / (24L * 3600 * 1000);
        long hours = (ms / (3600L * 1000)) % 24;
        long mins = (ms / (60L * 1000)) % 60;
        long secs = (ms / 1000L) % 60;
        cdDays.setText(String.valueOf(days));
        cdHours.setText(String.format(Locale.US, "%02d", hours));
        cdMin.setText(String.format(Locale.US, "%02d", mins));
        cdSec.setText(String.format(Locale.US, "%02d", secs));
    }

    private void updateWidget() {
        AppWidgetManager mgr = AppWidgetManager.getInstance(this);
        ComponentName cn = new ComponentName(this, TrackerWidgetProvider.class);
        int[] ids = mgr.getAppWidgetIds(cn);
        if (ids.length > 0) TrackerWidgetProvider.updateAll(this, mgr, ids);
    }

    // ---------------- TODAY TAB (log + week chart + calendar) ----------------
    private LinearLayout buildTodayTab() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, dp(16), 0, dp(24));

        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackground(roundedBordered(SURFACE, 14, BORDER, 1));
        l.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView cardTitle = new TextView(this);
        cardTitle.setText("Log today's hours");
        cardTitle.setTextColor(INK);
        cardTitle.setTextSize(16);
        cardTitle.setTypeface(Typeface.DEFAULT_BOLD);
        cardTitle.setPadding(0, 0, 0, dp(14));
        l.addView(cardTitle);

        List<String> keys = new ArrayList<String>(Arrays.asList(Syllabus.SUBJECT_KEYS));
        keys.addAll(Syllabus.EXTRA_ACTIVITIES.keySet());
        final String today = DataStore.todayStr();

        Map<String, Integer> extraColors = new HashMap<String, Integer>();
        extraColors.put("sleep", Color.parseColor("#6C7FD8"));
        extraColors.put("chores", Color.parseColor("#B98CE0"));

        final TextView capNote = new TextView(this);
        capNote.setTextColor(CORAL);
        capNote.setTypeface(Typeface.DEFAULT_BOLD);
        capNote.setTextSize(12);
        capNote.setText("Daily cap reached \u2014 24h logged");
        capNote.setVisibility(View.GONE);
        capNote.setPadding(0, dp(4), 0, 0);

        for (final String key : keys) {
            String label;
            int subjColor;
            if (Syllabus.SUBJECTS.containsKey(key)) {
                label = Syllabus.SUBJECTS.get(key).label;
                subjColor = Color.parseColor(Syllabus.SUBJECTS.get(key).color);
            } else {
                label = Syllabus.EXTRA_ACTIVITIES.get(key);
                subjColor = extraColors.containsKey(key) ? extraColors.get(key) : TEAL;
            }
            int tint = Color.argb(22, Color.red(subjColor), Color.green(subjColor), Color.blue(subjColor));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(12), dp(10), dp(10), dp(10));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(8);
            row.setLayoutParams(rowLp);

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setColor(tint);
            rowBg.setCornerRadius(dp(10));
            row.setBackground(rowBg);

            View leftBar = new View(this);
            leftBar.setBackgroundColor(subjColor);
            LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(3), ViewGroup.LayoutParams.MATCH_PARENT);
            barLp.rightMargin = dp(10);
            leftBar.setLayoutParams(barLp);
            row.addView(leftBar);

            TextView name = new TextView(this);
            name.setText(label);
            name.setTextColor(INK);
            name.setTextSize(14);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            name.setLayoutParams(nameLp);
            row.addView(name);

            final TextView value = new TextView(this);
            value.setTextColor(INK);
            value.setTypeface(Typeface.MONOSPACE);
            value.setTextSize(13);
            value.setPadding(dp(10), 0, dp(10), 0);
            value.setText(String.format(Locale.US, "%.1fh", store.getLogHours(today, key)));

            Button minus = pillStepButton("\u2212");
            Button plus = pillStepButton("+");

            minus.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    double cur = store.getLogHours(today, key);
                    store.setLogHours(today, key, Math.max(0, cur - 0.5));
                    value.setText(String.format(Locale.US, "%.1fh", store.getLogHours(today, key)));
                    updateCapNote(capNote);
                    refreshHeader();
                }
            });
            plus.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    double dayTotal = store.dayTotalHours(today);
                    double remaining = 24 - dayTotal;
                    if (remaining <= 0.001) { updateCapNote(capNote); return; }
                    double inc = Math.min(0.5, remaining);
                    double cur = store.getLogHours(today, key);
                    store.setLogHours(today, key, cur + inc);
                    value.setText(String.format(Locale.US, "%.1fh", store.getLogHours(today, key)));
                    updateCapNote(capNote);
                    refreshHeader();
                }
            });

            row.addView(minus);
            row.addView(value);
            row.addView(plus);
            l.addView(row);
        }
        l.addView(capNote);
        updateCapNote(capNote);

        wrapper.addView(l);

        // ---- this-week bar chart ----
        LinearLayout weekCard = new LinearLayout(this);
        weekCard.setOrientation(LinearLayout.VERTICAL);
        weekCard.setPadding(dp(18), dp(18), dp(18), dp(14));
        weekCard.setBackground(roundedBordered(SURFACE, 14, BORDER, 1));
        LinearLayout.LayoutParams weekCardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        weekCardLp.topMargin = dp(16);
        weekCard.setLayoutParams(weekCardLp);

        TextView weekTitle = new TextView(this);
        weekTitle.setText("Last 7 days");
        weekTitle.setTextColor(INK);
        weekTitle.setTextSize(16);
        weekTitle.setTypeface(Typeface.DEFAULT_BOLD);
        weekTitle.setPadding(0, 0, 0, dp(12));
        weekCard.addView(weekTitle);

        WeekBarChart weekChart = new WeekBarChart(this, null);
        weekChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(110)));
        weekChart.setData(store.last7DaysTotals());
        weekCard.addView(weekChart);

        wrapper.addView(weekCard);
        wrapper.addView(buildCalendarCard());
        return wrapper;
    }

    private void updateCapNote(TextView capNote) {
        double total = store.dayTotalHours(DataStore.todayStr());
        capNote.setVisibility(total >= 24 ? View.VISIBLE : View.GONE);
    }

    private Button pillStepButton(String symbol) {
        Button b = new Button(this);
        b.setText(symbol);
        b.setTextColor(INK);
        b.setTextSize(15);
        b.setPadding(0, 0, 0, 0);
        b.setBackground(roundedBordered(SURFACE, 8, BORDER, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(30), dp(30));
        b.setLayoutParams(lp);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        return b;
    }

    // ---------------- CALENDAR HEATMAP ----------------
    private LinearLayout buildCalendarCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(roundedBordered(SURFACE, 14, BORDER, 1));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(16);
        card.setLayoutParams(cardLp);

        TextView title = new TextView(this);
        title.setText("Study hours calendar");
        title.setTextColor(INK);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 0, 0, dp(12));
        card.addView(title);

        LinearLayout navRow = new LinearLayout(this);
        navRow.setOrientation(LinearLayout.HORIZONTAL);
        navRow.setGravity(Gravity.CENTER_VERTICAL);
        Button prevBtn = pillStepButton("\u2039");
        final TextView monthLabel = new TextView(this);
        monthLabel.setTextColor(INK);
        monthLabel.setTypeface(Typeface.DEFAULT_BOLD);
        monthLabel.setTextSize(14);
        monthLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams monthLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        monthLabel.setLayoutParams(monthLp);
        Button nextBtn = pillStepButton("\u203A");
        navRow.addView(prevBtn);
        navRow.addView(monthLabel);
        navRow.addView(nextBtn);
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        navLp.bottomMargin = dp(10);
        navRow.setLayoutParams(navLp);
        card.addView(navRow);

        final GridLayout grid = new GridLayout(this);
        grid.setColumnCount(7);
        card.addView(grid);

        LinearLayout legendRow = new LinearLayout(this);
        legendRow.setOrientation(LinearLayout.HORIZONTAL);
        legendRow.setGravity(Gravity.CENTER_VERTICAL);
        legendRow.setPadding(0, dp(12), 0, 0);
        TextView lowLabel = new TextView(this);
        lowLabel.setText("Low");
        lowLabel.setTextColor(MUTED);
        lowLabel.setTextSize(10.5f);
        legendRow.addView(lowLabel);
        double[] legendHours = {0, 2, 4, 6, 8};
        for (double h : legendHours) {
            View sw = new View(this);
            sw.setBackground(rounded(heatColor(h, false), 4));
            LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(dp(14), dp(14));
            swLp.setMargins(dp(5), 0, dp(5), 0);
            sw.setLayoutParams(swLp);
            legendRow.addView(sw);
        }
        TextView highLabel = new TextView(this);
        highLabel.setText("High");
        highLabel.setTextColor(MUTED);
        highLabel.setTextSize(10.5f);
        legendRow.addView(highLabel);
        card.addView(legendRow);

        final int[] monthOffset = {0};
        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = new Runnable() {
            public void run() {
                grid.removeAllViews();
                Calendar view = Calendar.getInstance();
                view.set(Calendar.DAY_OF_MONTH, 1);
                view.add(Calendar.MONTH, monthOffset[0]);
                int year = view.get(Calendar.YEAR);
                int month = view.get(Calendar.MONTH);
                SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM yyyy", Locale.US);
                monthLabel.setText(monthFmt.format(view.getTime()));

                int startWeekday = view.get(Calendar.DAY_OF_WEEK) - 1; // 0=Sun
                int daysInMonth = view.getActualMaximum(Calendar.DAY_OF_MONTH);
                String todayKey = DataStore.todayStr();

                String[] dow = {"S", "M", "T", "W", "T", "F", "S"};
                for (String d : dow) {
                    TextView h = new TextView(MainActivity.this);
                    h.setText(d);
                    h.setTextColor(MUTED);
                    h.setTextSize(10.5f);
                    h.setTypeface(Typeface.DEFAULT_BOLD);
                    h.setGravity(Gravity.CENTER);
                    GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
                    glp.width = dp(38);
                    glp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    h.setLayoutParams(glp);
                    grid.addView(h);
                }

                for (int i = 0; i < startWeekday; i++) {
                    View empty = new View(MainActivity.this);
                    GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
                    glp.width = dp(38);
                    glp.height = dp(38);
                    empty.setLayoutParams(glp);
                    grid.addView(empty);
                }

                for (int day = 1; day <= daysInMonth; day++) {
                    String dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
                    double hours = store.dayStudyHours(dateKey);
                    boolean isToday = dateKey.equals(todayKey);
                    boolean isFuture = dateKey.compareTo(todayKey) > 0;

                    FrameLayout cell = new FrameLayout(MainActivity.this);
                    GradientDrawable cellBg = new GradientDrawable();
                    cellBg.setColor(heatColor(hours, isFuture));
                    cellBg.setCornerRadius(dp(6));
                    if (isToday) cellBg.setStroke(dp(2), CORAL);
                    cell.setBackground(cellBg);

                    TextView dayLabel = new TextView(MainActivity.this);
                    dayLabel.setText(String.valueOf(day));
                    dayLabel.setTextSize(11);
                    dayLabel.setTypeface(Typeface.MONOSPACE);
                    dayLabel.setTextColor(isFuture ? INK : Color.WHITE);
                    FrameLayout.LayoutParams dayLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    dayLp.gravity = Gravity.CENTER;
                    dayLabel.setLayoutParams(dayLp);
                    cell.addView(dayLabel);

                    GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
                    glp.width = dp(36);
                    glp.height = dp(36);
                    glp.setMargins(dp(1), dp(1), dp(1), dp(1));
                    cell.setLayoutParams(glp);
                    grid.addView(cell);
                }
            }
        };

        prevBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { monthOffset[0]--; rebuild[0].run(); }
        });
        nextBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { monthOffset[0]++; rebuild[0].run(); }
        });

        rebuild[0].run();
        return card;
    }

    private static int heatColor(double hours, boolean isFuture) {
        if (isFuture) return BORDER;
        double capped = Math.min(Math.max(hours, 0), 8);
        float hue = (float) (capped / 8.0 * 120.0); // 0=red, 120=green
        return hslToColor(hue, 0.68f, 0.50f);
    }

    private static int hslToColor(float h, float s, float l) {
        float c = (1 - Math.abs(2 * l - 1)) * s;
        float x = c * (1 - Math.abs((h / 60f) % 2 - 1));
        float m = l - c / 2f;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; }
        else if (h < 120) { r = x; g = c; b = 0; }
        else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; }
        else if (h < 300) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        int ri = Math.round((r + m) * 255), gi = Math.round((g + m) * 255), bi = Math.round((b + m) * 255);
        return Color.rgb(ri, gi, bi);
    }

    // ---------------- SYLLABUS TAB ----------------
    private LinearLayout buildSyllabusTab() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, dp(16), 0, dp(24));

        final EditText searchInput = styledInput("Search chapters across Physics, Chemistry, Biology\u2026");
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchLp.bottomMargin = dp(14);
        l.addView(searchInput, searchLp);

        final List<ChapterEntry> chapterEntries = new ArrayList<ChapterEntry>();

        for (final Syllabus.Subject subj : Syllabus.SUBJECTS.values()) {
            int subjColor = Color.parseColor(subj.color);

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(16), dp(16), dp(16));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(SURFACE);
            cardBg.setCornerRadius(dp(14));
            cardBg.setStroke(dp(1), BORDER);
            card.setBackground(cardBg);

            View accent = new View(this);
            accent.setBackgroundColor(subjColor);
            LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
            accentLp.bottomMargin = dp(10);
            accent.setLayoutParams(accentLp);
            card.addView(accent);

            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dp(14);
            card.setLayoutParams(cardLp);

            LinearLayout headerRow = new LinearLayout(this);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.CENTER_VERTICAL);

            final TextView headerTitle = new TextView(this);
            headerTitle.setTextSize(16);
            headerTitle.setTypeface(Typeface.DEFAULT_BOLD);
            headerTitle.setTextColor(subjColor);
            headerTitle.setText(subj.label);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            headerTitle.setLayoutParams(titleLp);
            headerRow.addView(headerTitle);

            final TextView pctText = new TextView(this);
            pctText.setTypeface(Typeface.MONOSPACE);
            pctText.setTextColor(MUTED);
            pctText.setTextSize(12);
            pctText.setText(Math.round(store.subjectProgress(subj.key) * 100) + "%");
            headerRow.addView(pctText);
            card.addView(headerRow);

            final ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(100);
            progressBar.setProgress((int) Math.round(store.subjectProgress(subj.key) * 100));
            LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6));
            pbLp.topMargin = dp(10);
            pbLp.bottomMargin = dp(6);
            progressBar.setLayoutParams(pbLp);
            card.addView(progressBar);

            final LinearLayout chapterHolder = new LinearLayout(this);
            chapterHolder.setOrientation(LinearLayout.VERTICAL);
            chapterHolder.setVisibility(View.GONE);
            headerRow.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    chapterHolder.setVisibility(
                            chapterHolder.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
            });

            for (Map.Entry<String, String[]> group : subj.groups.entrySet()) {
                TextView groupLabel = new TextView(this);
                groupLabel.setText(group.getKey().toUpperCase(Locale.US));
                groupLabel.setTextSize(11);
                groupLabel.setTypeface(Typeface.DEFAULT_BOLD);
                groupLabel.setTextColor(MUTED);
                groupLabel.setPadding(0, dp(10), 0, dp(6));
                chapterHolder.addView(groupLabel);

                final String groupKey = group.getKey();

                for (final String chapter : group.getValue()) {
                    LinearLayout chRow = new LinearLayout(this);
                    chRow.setOrientation(LinearLayout.VERTICAL);
                    chRow.setPadding(dp(6), dp(7), dp(6), dp(7));

                    LinearLayout chHeaderRow = new LinearLayout(this);
                    chHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
                    chHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

                    TextView chName = new TextView(this);
                    chName.setText(chapter);
                    chName.setTextColor(INK);
                    chName.setTextSize(13.5f);
                    LinearLayout.LayoutParams chNameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    chName.setLayoutParams(chNameLp);
                    chHeaderRow.addView(chName);

                    final TextView chPct = new TextView(this);
                    chPct.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                    chPct.setTextSize(11);
                    chPct.setPadding(dp(8), dp(2), dp(8), dp(2));
                    updateChapterBadge(chPct, subjColor, store.chapterProgress(subj.key, groupKey, chapter));
                    chHeaderRow.addView(chPct);
                    chRow.addView(chHeaderRow);

                    final LinearLayout subtaskHolder = new LinearLayout(this);
                    subtaskHolder.setOrientation(LinearLayout.VERTICAL);
                    subtaskHolder.setPadding(dp(20), dp(4), 0, 0);
                    subtaskHolder.setVisibility(View.GONE);
                    chHeaderRow.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            subtaskHolder.setVisibility(
                                    subtaskHolder.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                        }
                    });

                    for (int i = 0; i < Syllabus.SUBTASK_KEYS.length; i++) {
                        final String stKey = Syllabus.SUBTASK_KEYS[i];
                        String stLabel = Syllabus.SUBTASK_LABELS[i];
                        CheckBox cb = new CheckBox(this);
                        cb.setText(stLabel);
                        cb.setTextColor(MUTED);
                        cb.setTextSize(12.5f);
                        cb.setChecked(store.isSubtaskDone(subj.key, groupKey, chapter, stKey));
                        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            public void onCheckedChanged(CompoundButton btn, boolean checked) {
                                store.setSubtaskDone(subj.key, groupKey, chapter, stKey, checked);
                                pctText.setText(Math.round(store.subjectProgress(subj.key) * 100) + "%");
                                progressBar.setProgress((int) Math.round(store.subjectProgress(subj.key) * 100));
                                updateChapterBadge(chPct, subjColor, store.chapterProgress(subj.key, groupKey, chapter));
                                refreshHeader();
                            }
                        });
                        subtaskHolder.addView(cb);
                    }
                    chRow.addView(subtaskHolder);
                    chapterHolder.addView(chRow);
                    chapterEntries.add(new ChapterEntry(chapter, chRow, card, chapterHolder));

                    View chDivider = new View(this);
                    chDivider.setBackgroundColor(BG);
                    chDivider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
                    chapterHolder.addView(chDivider);
                }
            }
            card.addView(chapterHolder);
            l.addView(card);
        }

        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void afterTextChanged(android.text.Editable s) {
                String q = s.toString().trim().toLowerCase(Locale.US);
                java.util.LinkedHashSet<LinearLayout> cardsWithMatch = new java.util.LinkedHashSet<LinearLayout>();
                java.util.LinkedHashSet<LinearLayout> allCards = new java.util.LinkedHashSet<LinearLayout>();
                for (ChapterEntry e : chapterEntries) {
                    allCards.add(e.card);
                    boolean matches = q.length() == 0 || e.name.toLowerCase(Locale.US).contains(q);
                    e.row.setVisibility(matches ? View.VISIBLE : View.GONE);
                    if (matches) cardsWithMatch.add(e.card);
                    e.chapterHolder.setVisibility(q.length() == 0 ? View.GONE : View.VISIBLE);
                }
                for (LinearLayout card : allCards) {
                    boolean show = q.length() == 0 || cardsWithMatch.contains(card);
                    card.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            }
        });

        return l;
    }

    private void updateChapterBadge(TextView badge, int subjColor, double fraction) {
        int pct = (int) Math.round(fraction * 100);
        badge.setText(pct + "%");
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(20));
        if (pct >= 100) {
            bg.setColor(subjColor);
            badge.setTextColor(Color.WHITE);
        } else {
            bg.setColor(Color.argb(30, Color.red(subjColor), Color.green(subjColor), Color.blue(subjColor)));
            badge.setTextColor(subjColor);
        }
        badge.setBackground(bg);
    }

    private static class ChapterEntry {
        final String name;
        final View row;
        final LinearLayout card;
        final LinearLayout chapterHolder;
        ChapterEntry(String name, View row, LinearLayout card, LinearLayout chapterHolder) {
            this.name = name; this.row = row; this.card = card; this.chapterHolder = chapterHolder;
        }
    }

    // ---------------- REVISE TAB ----------------
    private LinearLayout buildRevisionTab() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, dp(16), 0, dp(24));

        LinearLayout introCard = new LinearLayout(this);
        introCard.setOrientation(LinearLayout.VERTICAL);
        introCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        introCard.setBackground(roundedBordered(SURFACE, 14, BORDER, 1));
        LinearLayout.LayoutParams introLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        introLp.bottomMargin = dp(16);
        introCard.setLayoutParams(introLp);
        TextView introTitle = new TextView(this);
        introTitle.setText("Forgetting-curve revisions");
        introTitle.setTextColor(INK);
        introTitle.setTextSize(16);
        introTitle.setTypeface(Typeface.DEFAULT_BOLD);
        introTitle.setPadding(0, 0, 0, dp(8));
        introCard.addView(introTitle);
        TextView introBody = new TextView(this);
        introBody.setText("Finishing all 5 tasks on a chapter schedules spaced revisions at Day 1, 3, 7, 15, and 30 \u2014 mark each one off as you revise to move to the next.");
        introBody.setTextColor(MUTED);
        introBody.setTextSize(12.5f);
        introCard.addView(introBody);
        l.addView(introCard);

        JSONObject queues = store.getRevisionQueues();
        JSONArray due = queues.optJSONArray("due");
        JSONArray upcoming = queues.optJSONArray("upcoming");
        if (due == null) due = new JSONArray();
        if (upcoming == null) upcoming = new JSONArray();

        LinearLayout dueCard = new LinearLayout(this);
        dueCard.setOrientation(LinearLayout.VERTICAL);
        dueCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        dueCard.setBackground(roundedBordered(SURFACE, 14, BORDER, 1));
        LinearLayout.LayoutParams dueLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dueLp.bottomMargin = dp(16);
        dueCard.setLayoutParams(dueLp);
        TextView dueTitle = new TextView(this);
        dueTitle.setText("Due now");
        dueTitle.setTextColor(INK);
        dueTitle.setTextSize(16);
        dueTitle.setTypeface(Typeface.DEFAULT_BOLD);
        dueTitle.setPadding(0, 0, 0, dp(10));
        dueCard.addView(dueTitle);

        if (due.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Nothing due \u2014 completed chapters will schedule revisions here automatically.");
            empty.setTextColor(MUTED);
            empty.setTextSize(13);
            dueCard.addView(empty);
        } else {
            for (int i = 0; i < due.length(); i++) {
                JSONObject item = due.optJSONObject(i);
                if (item != null) dueCard.addView(buildRevisionRow(item, true));
            }
        }
        l.addView(dueCard);

        if (upcoming.length() > 0) {
            LinearLayout upcomingCard = new LinearLayout(this);
            upcomingCard.setOrientation(LinearLayout.VERTICAL);
            upcomingCard.setPadding(dp(18), dp(18), dp(18), dp(18));
            upcomingCard.setBackground(roundedBordered(SURFACE, 14, BORDER, 1));
            TextView upcomingTitle = new TextView(this);
            upcomingTitle.setText("Upcoming");
            upcomingTitle.setTextColor(INK);
            upcomingTitle.setTextSize(16);
            upcomingTitle.setTypeface(Typeface.DEFAULT_BOLD);
            upcomingTitle.setPadding(0, 0, 0, dp(10));
            upcomingCard.addView(upcomingTitle);
            for (int i = 0; i < upcoming.length(); i++) {
                JSONObject item = upcoming.optJSONObject(i);
                if (item != null) upcomingCard.addView(buildRevisionRow(item, false));
            }
            l.addView(upcomingCard);
        }

        return l;
    }

    private static final Map<Integer, String> STAGE_LABELS = new HashMap<Integer, String>();
    static {
        STAGE_LABELS.put(1, "Day 1"); STAGE_LABELS.put(3, "Day 3"); STAGE_LABELS.put(7, "Day 7");
        STAGE_LABELS.put(15, "Day 15"); STAGE_LABELS.put(30, "Day 30");
    }

    private LinearLayout buildRevisionRow(final JSONObject item, boolean isDue) {
        final String subject = item.optString("subject");
        String label = item.optString("label");
        int color = Color.parseColor(item.optString("color"));
        final String group = item.optString("group");
        final String chapter = item.optString("chapter");
        final int stage = item.optInt("stage");
        int days = item.optInt("days");
        String stageLabel = STAGE_LABELS.containsKey(stage) ? STAGE_LABELS.get(stage) : ("Day " + stage);

        int tint = Color.argb(16, Color.red(color), Color.green(color), Color.blue(color));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable rowBg = new GradientDrawable();
        rowBg.setColor(tint);
        rowBg.setCornerRadius(dp(10));
        row.setBackground(rowBg);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);

        View leftBar = new View(this);
        leftBar.setBackgroundColor(color);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(dp(3), ViewGroup.LayoutParams.MATCH_PARENT);
        barLp.rightMargin = dp(10);
        leftBar.setLayoutParams(barLp);
        row.addView(leftBar);

        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        infoCol.setLayoutParams(infoLp);

        TextView subjLine = new TextView(this);
        subjLine.setText(label + " \u00b7 " + stageLabel);
        subjLine.setTextColor(color);
        subjLine.setTypeface(Typeface.DEFAULT_BOLD);
        subjLine.setTextSize(10);
        infoCol.addView(subjLine);

        TextView chapterLine = new TextView(this);
        chapterLine.setText(chapter);
        chapterLine.setTextColor(INK);
        chapterLine.setTypeface(Typeface.DEFAULT_BOLD);
        chapterLine.setTextSize(14);
        infoCol.addView(chapterLine);

        TextView sinceLine = new TextView(this);
        String sinceText = isDue
                ? (days == 0 ? "Due today" : (days + (days == 1 ? " day overdue" : " days overdue"))) + " \u00b7 " + group
                : "In " + days + (days == 1 ? " day" : " days") + " \u00b7 " + group;
        sinceLine.setText(sinceText);
        sinceLine.setTextColor(MUTED);
        sinceLine.setTextSize(11.5f);
        infoCol.addView(sinceLine);

        row.addView(infoCol);

        if (isDue) {
            Button markBtn = new Button(this);
            markBtn.setText("Mark revised");
            markBtn.setAllCaps(false);
            markBtn.setTextColor(Color.WHITE);
            markBtn.setTextSize(12);
            markBtn.setTypeface(Typeface.DEFAULT_BOLD);
            markBtn.setBackground(rounded(TEAL, 8));
            markBtn.setPadding(dp(12), 0, dp(12), 0);
            LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
            markBtn.setLayoutParams(markLp);
            markBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    store.markRevisionDone(subject, group, chapter, stage);
                    showTab(2);
                }
            });
            row.addView(markBtn);
        }

        return row;
    }

    // ---------------- TESTS TAB ----------------
    private LinearLayout buildTestsTab() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, dp(16), 0, dp(24));

        LinearLayout formCard = new LinearLayout(this);
        formCard.setOrientation(LinearLayout.VERTICAL);
        formCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        formCard.setBackground(roundedBordered(SURFACE, 14, BORDER, 1));
        LinearLayout.LayoutParams formCardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        formCardLp.bottomMargin = dp(16);
        formCard.setLayoutParams(formCardLp);

        TextView formTitle = new TextView(this);
        formTitle.setText("Add a test score");
        formTitle.setTextColor(INK);
        formTitle.setTextSize(16);
        formTitle.setTypeface(Typeface.DEFAULT_BOLD);
        formTitle.setPadding(0, 0, 0, dp(12));
        formCard.addView(formTitle);

        final EditText dateInput = styledInput("Date (yyyy-mm-dd)");
        dateInput.setText(DataStore.todayStr());
        formCard.addView(dateInput, fieldLp());

        final EditText scoreInput = styledInput("Score (out of 720)");
        scoreInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        formCard.addView(scoreInput, fieldLp());

        Button addBtn = new Button(this);
        addBtn.setText("Add score");
        addBtn.setAllCaps(false);
        addBtn.setTypeface(Typeface.DEFAULT_BOLD);
        addBtn.setTextColor(Color.WHITE);
        addBtn.setBackground(rounded(TEAL, 8));
        LinearLayout.LayoutParams addBtnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        addBtnLp.topMargin = dp(10);
        addBtn.setLayoutParams(addBtnLp);
        addBtn.setPadding(dp(20), 0, dp(20), 0);
        formCard.addView(addBtn);

        l.addView(formCard);

        LinearLayout trendCard = new LinearLayout(this);
        trendCard.setOrientation(LinearLayout.VERTICAL);
        trendCard.setPadding(dp(18), dp(18), dp(18), dp(14));
        trendCard.setBackground(roundedBordered(SURFACE, 14, BORDER, 1));
        LinearLayout.LayoutParams trendCardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trendCardLp.bottomMargin = dp(16);
        trendCard.setLayoutParams(trendCardLp);

        TextView trendTitle = new TextView(this);
        trendTitle.setText("Trend");
        trendTitle.setTextColor(INK);
        trendTitle.setTextSize(16);
        trendTitle.setTypeface(Typeface.DEFAULT_BOLD);
        trendTitle.setPadding(0, 0, 0, dp(10));
        trendCard.addView(trendTitle);

        final TrendChart trendChart = new TrendChart(this, null);
        trendChart.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));
        trendCard.addView(trendChart);
        l.addView(trendCard);

        LinearLayout historyCard = new LinearLayout(this);
        historyCard.setOrientation(LinearLayout.VERTICAL);
        historyCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        historyCard.setBackground(roundedBordered(SURFACE, 14, BORDER, 1));
        TextView historyTitle = new TextView(this);
        historyTitle.setText("History");
        historyTitle.setTextColor(INK);
        historyTitle.setTextSize(16);
        historyTitle.setTypeface(Typeface.DEFAULT_BOLD);
        historyTitle.setPadding(0, 0, 0, dp(10));
        historyCard.addView(historyTitle);

        final LinearLayout listHolder = new LinearLayout(this);
        listHolder.setOrientation(LinearLayout.VERTICAL);
        historyCard.addView(listHolder);
        l.addView(historyCard);

        final Runnable[] refresh = new Runnable[1];
        refresh[0] = new Runnable() {
            public void run() {
                listHolder.removeAllViews();
                JSONArray tests = store.getTests();

                List<JSONObject> sorted = new ArrayList<JSONObject>();
                for (int j = 0; j < tests.length(); j++) {
                    JSONObject obj = tests.optJSONObject(j);
                    if (obj != null) sorted.add(obj);
                }
                Collections.sort(sorted, new Comparator<JSONObject>() {
                    public int compare(JSONObject a, JSONObject b) {
                        return a.optString("date").compareTo(b.optString("date"));
                    }
                });
                List<Double> pcts = new ArrayList<Double>();
                for (JSONObject obj : sorted) {
                    double sc = obj.optDouble("score", 0), tot = obj.optDouble("total", 1);
                    pcts.add(tot > 0 ? (100.0 * sc / tot) : 0.0);
                }
                trendChart.setPercentages(pcts);

                if (tests.length() == 0) {
                    TextView empty = new TextView(MainActivity.this);
                    empty.setText("No scores logged yet \u2014 add your first mock test above.");
                    empty.setTextColor(MUTED);
                    empty.setTextSize(13);
                    listHolder.addView(empty);
                    return;
                }
                for (int i = 0; i < tests.length(); i++) {
                    JSONObject t = tests.optJSONObject(i);
                    if (t == null) continue;
                    LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(12), dp(10), dp(12), dp(10));
                    row.setBackground(roundedBordered(SURFACE, 10, BORDER, 1));
                    LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    rowLp.bottomMargin = dp(8);
                    row.setLayoutParams(rowLp);

                    TextView dateLine = new TextView(MainActivity.this);
                    dateLine.setText(t.optString("date"));
                    dateLine.setTextColor(INK);
                    dateLine.setTypeface(Typeface.DEFAULT_BOLD);
                    dateLine.setTextSize(13.5f);
                    LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    dateLine.setLayoutParams(dateLp);
                    row.addView(dateLine);

                    double score = t.optDouble("score", 0), total = t.optDouble("total", 720);
                    TextView scoreView = new TextView(MainActivity.this);
                    scoreView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                    scoreView.setTextColor(TEAL_DARK);
                    scoreView.setTextSize(13);
                    scoreView.setText(((int) score) + "/" + ((int) total) + "  (" + Math.round(100.0 * score / total) + "%)");
                    row.addView(scoreView);

                    Button del = new Button(MainActivity.this);
                    del.setText("\u2715");
                    del.setTextColor(MUTED);
                    del.setBackgroundColor(Color.TRANSPARENT);
                    del.setPadding(dp(10), 0, 0, 0);
                    final int idx = i;
                    del.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            store.deleteTest(idx);
                            refresh[0].run();
                        }
                    });
                    row.addView(del);
                    listHolder.addView(row);
                }
            }
        };

        addBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String date = dateInput.getText().toString().trim();
                String scoreStr = scoreInput.getText().toString().trim();
                if (date.length() == 0 || scoreStr.length() == 0) return;
                try {
                    store.addTest(date, Double.parseDouble(scoreStr), 720.0);
                    scoreInput.setText("");
                    refresh[0].run();
                } catch (NumberFormatException ignored) { }
            }
        });

        refresh[0].run();
        return l;
    }

    private EditText styledInput(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(INK);
        e.setHintTextColor(MUTED);
        e.setBackground(roundedBordered(SURFACE, 8, BORDER, 1));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        return e;
    }

    private LinearLayout.LayoutParams fieldLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        return lp;
    }
}
