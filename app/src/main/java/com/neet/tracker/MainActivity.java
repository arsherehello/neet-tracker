package com.neet.tracker;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;

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

    private DataStore store;
    private FrameLayout content;
    private TextView streakNum, countdownNum, todayTotalLine;
    private LinearLayout logTab, syllabusTab, testsTab;
    private Button tabLogBtn, tabSyllabusBtn, tabTestsBtn;
    private View tabLogUnderline, tabSyllabusUnderline, tabTestsUnderline;

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

    private GradientDrawable roundedLeftBar(int tintFill, int barColor) {
        // approximates .log-row's tinted bg + colored left border
        GradientDrawable d = new GradientDrawable();
        d.setColor(tintFill);
        d.setCornerRadius(dp(10));
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

        logTab = buildLogTab();
        syllabusTab = buildSyllabusTab();
        testsTab = buildTestsTab();

        showTab(0);
        refreshHeader();
    }

    // ---------------- HEADER ----------------
    private LinearLayout buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(16), dp(20), dp(16), dp(12));

        TextView eyebrow = new TextView(this);
        eyebrow.setText("NEET TRACKER");
        eyebrow.setTextColor(TEAL);
        eyebrow.setTextSize(11);
        eyebrow.setTypeface(Typeface.MONOSPACE);
        eyebrow.setLetterSpacing(0.08f);
        header.addView(eyebrow);

        TextView title = new TextView(this);
        title.setText("Dashboard");
        title.setTextColor(INK);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(2), 0, dp(14));
        header.addView(title);

        // stats row: today total (left) | streak + countdown (right)
        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams leftLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        leftCol.setLayoutParams(leftLp);

        todayTotalLine = new TextView(this);
        todayTotalLine.setTextColor(MUTED);
        todayTotalLine.setTextSize(13);
        leftCol.addView(todayTotalLine);
        statsRow.addView(leftCol);

        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.HORIZONTAL);
        rightCol.setGravity(Gravity.CENTER_VERTICAL);
        View divider = new View(this);
        divider.setBackgroundColor(BORDER);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(dp(2), dp(40));
        divLp.setMargins(dp(4), 0, dp(16), 0);
        divider.setLayoutParams(divLp);
        rightCol.addView(divider);

        LinearLayout streakBlock = new LinearLayout(this);
        streakBlock.setOrientation(LinearLayout.VERTICAL);
        streakBlock.setGravity(Gravity.END);
        streakNum = new TextView(this);
        streakNum.setTextColor(CORAL);
        streakNum.setTextSize(22);
        streakNum.setTypeface(Typeface.DEFAULT_BOLD);
        streakBlock.addView(streakNum);
        TextView streakLabel = new TextView(this);
        streakLabel.setText("DAY STREAK");
        streakLabel.setTextColor(MUTED);
        streakLabel.setTextSize(10);
        streakBlock.addView(streakLabel);
        LinearLayout.LayoutParams streakLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        streakLp.setMargins(0, 0, dp(20), 0);
        streakBlock.setLayoutParams(streakLp);
        rightCol.addView(streakBlock);

        LinearLayout countdownBlock = new LinearLayout(this);
        countdownBlock.setOrientation(LinearLayout.VERTICAL);
        countdownBlock.setGravity(Gravity.END);
        countdownNum = new TextView(this);
        countdownNum.setTextColor(TEAL_DARK);
        countdownNum.setTextSize(24);
        countdownNum.setTypeface(Typeface.DEFAULT_BOLD);
        countdownBlock.addView(countdownNum);
        TextView countdownLabel = new TextView(this);
        countdownLabel.setText("DAYS TO NEET");
        countdownLabel.setTextColor(MUTED);
        countdownLabel.setTextSize(10);
        countdownBlock.addView(countdownLabel);
        rightCol.addView(countdownBlock);

        statsRow.addView(rightCol);
        header.addView(statsRow);
        return header;
    }

    // ---------------- TAB BAR ----------------
    private LinearLayout buildTabBar() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(dp(10), 0, dp(10), 0);

        tabLogBtn = flatTab("Log");
        tabSyllabusBtn = flatTab("Syllabus");
        tabTestsBtn = flatTab("Tests");
        tabRow.addView(tabLogBtn);
        tabRow.addView(tabSyllabusBtn);
        tabRow.addView(tabTestsBtn);
        wrap.addView(tabRow);

        View bottomBorder = new View(this);
        bottomBorder.setBackgroundColor(BORDER);
        bottomBorder.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        wrap.addView(bottomBorder);

        tabLogBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showTab(0); } });
        tabSyllabusBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showTab(1); } });
        tabTestsBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showTab(2); } });

        return wrap;
    }

    private Button flatTab(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        b.setLayoutParams(lp);
        return b;
    }

    private void showTab(int index) {
        tabLogBtn.setTextColor(index == 0 ? TEAL_DARK : MUTED);
        tabSyllabusBtn.setTextColor(index == 1 ? TEAL_DARK : MUTED);
        tabTestsBtn.setTextColor(index == 2 ? TEAL_DARK : MUTED);

        content.removeAllViews();
        if (index == 0) content.addView(logTab);
        else if (index == 1) content.addView(syllabusTab);
        else content.addView(testsTab);
    }

    private void refreshHeader() {
        double today = store.todayTotalHours();
        int streak = store.currentStreak();
        long daysLeft = DataStore.daysUntilExam();
        todayTotalLine.setText(String.format(Locale.US, "Today  \u00b7  %.1fh logged", today));
        streakNum.setText(String.valueOf(streak));
        countdownNum.setText(String.valueOf(daysLeft));
        updateWidget();
    }

    private void updateWidget() {
        AppWidgetManager mgr = AppWidgetManager.getInstance(this);
        ComponentName cn = new ComponentName(this, TrackerWidgetProvider.class);
        int[] ids = mgr.getAppWidgetIds(cn);
        if (ids.length > 0) TrackerWidgetProvider.updateAll(this, mgr, ids);
    }

    // ---------------- LOG TAB ----------------
    private LinearLayout buildLogTab() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, dp(16), 0, dp(24));
        l.setBackground(roundedBordered(SURFACE, 14, BORDER, 1));
        l.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView cardTitle = new TextView(this);
        cardTitle.setText("Today's log");
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
                    refreshHeader();
                }
            });
            plus.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    double cur = store.getLogHours(today, key);
                    store.setLogHours(today, key, cur + 0.5);
                    value.setText(String.format(Locale.US, "%.1fh", store.getLogHours(today, key)));
                    refreshHeader();
                }
            });

            row.addView(minus);
            row.addView(value);
            row.addView(plus);
            l.addView(row);
        }

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(16);
        cardLp.bottomMargin = dp(20);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(l);
        return wrapper;
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

    // ---------------- SYLLABUS TAB ----------------
    private LinearLayout buildSyllabusTab() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(0, dp(16), 0, dp(24));

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

            // top accent strip
            View accent = new View(this);
            accent.setBackgroundColor(subjColor);
            LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
            accentLp.bottomMargin = dp(10);
            accent.setLayoutParams(accentLp);
            card.addView(accent);

            LinearLayout cardLp2 = null;
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
                                refreshHeader();
                            }
                        });
                        subtaskHolder.addView(cb);
                    }
                    chRow.addView(subtaskHolder);
                    chapterHolder.addView(chRow);

                    View chDivider = new View(this);
                    chDivider.setBackgroundColor(BG);
                    chDivider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
                    chapterHolder.addView(chDivider);
                }
            }
            card.addView(chapterHolder);
            l.addView(card);
        }
        return l;
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
        formTitle.setText("Log a test");
        formTitle.setTextColor(INK);
        formTitle.setTextSize(16);
        formTitle.setTypeface(Typeface.DEFAULT_BOLD);
        formTitle.setPadding(0, 0, 0, dp(12));
        formCard.addView(formTitle);

        final Spinner subjectSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"physics", "chemistry", "biology"});
        subjectSpinner.setAdapter(adapter);
        formCard.addView(subjectSpinner, fieldLp());

        final EditText dateInput = styledInput("Date (yyyy-mm-dd)");
        dateInput.setText(DataStore.todayStr());
        formCard.addView(dateInput, fieldLp());

        final EditText scoreInput = styledInput("Score");
        scoreInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        formCard.addView(scoreInput, fieldLp());

        final EditText totalInput = styledInput("Total marks");
        totalInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        formCard.addView(totalInput, fieldLp());

        Button addBtn = new Button(this);
        addBtn.setText("Add Test");
        addBtn.setAllCaps(false);
        addBtn.setTypeface(Typeface.DEFAULT_BOLD);
        addBtn.setTextColor(Color.WHITE);
        addBtn.setBackground(rounded(TEAL, 8));
        LinearLayout.LayoutParams addBtnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        addBtnLp.topMargin = dp(8);
        addBtn.setLayoutParams(addBtnLp);
        addBtn.setPadding(dp(20), 0, dp(20), 0);
        formCard.addView(addBtn);

        l.addView(formCard);

        final LinearLayout listHolder = new LinearLayout(this);
        listHolder.setOrientation(LinearLayout.VERTICAL);
        l.addView(listHolder);

        final Runnable[] refresh = new Runnable[1];
        refresh[0] = new Runnable() {
            public void run() {
                listHolder.removeAllViews();
                JSONArray tests = store.getTests();
                if (tests.length() == 0) {
                    TextView empty = new TextView(MainActivity.this);
                    empty.setText("No tests logged yet.");
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

                    LinearLayout infoCol = new LinearLayout(MainActivity.this);
                    infoCol.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    infoCol.setLayoutParams(infoLp);

                    TextView subjLine = new TextView(MainActivity.this);
                    String subjKey = t.optString("subject");
                    subjLine.setText(capitalize(subjKey));
                    subjLine.setTextColor(INK);
                    subjLine.setTypeface(Typeface.DEFAULT_BOLD);
                    subjLine.setTextSize(13.5f);
                    infoCol.addView(subjLine);

                    TextView dateLine = new TextView(MainActivity.this);
                    dateLine.setText(t.optString("date"));
                    dateLine.setTextColor(MUTED);
                    dateLine.setTextSize(11);
                    infoCol.addView(dateLine);
                    row.addView(infoCol);

                    double score = t.optDouble("score", 0), total = t.optDouble("total", 1);
                    TextView scoreView = new TextView(MainActivity.this);
                    scoreView.setTypeface(Typeface.MONOSPACE);
                    scoreView.setTextColor(TEAL_DARK);
                    scoreView.setTextSize(13);
                    scoreView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
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
                String subject = (String) subjectSpinner.getSelectedItem();
                String date = dateInput.getText().toString().trim();
                String scoreStr = scoreInput.getText().toString().trim();
                String totalStr = totalInput.getText().toString().trim();
                if (date.length() == 0 || scoreStr.length() == 0 || totalStr.length() == 0) return;
                try {
                    store.addTest(subject, date, Double.parseDouble(scoreStr), Double.parseDouble(totalStr));
                    scoreInput.setText(""); totalInput.setText("");
                    refresh[0].run();
                } catch (NumberFormatException ignored) { }
            }
        });

        refresh[0].run();
        return l;
    }

    private String capitalize(String s) {
        if (s == null || s.length() == 0) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
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
