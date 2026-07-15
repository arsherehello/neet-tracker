package com.neet.tracker;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Color;
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

    private DataStore store;
    private FrameLayout content;
    private TextView headerStats;
    private LinearLayout logTab, syllabusTab, testsTab;
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new DataStore(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F5FAF9"));

        headerStats = new TextView(this);
        headerStats.setTextSize(13);
        headerStats.setPadding(dp(16), dp(16), dp(16), dp(8));
        headerStats.setTextColor(Color.parseColor("#0B2A2E"));
        root.addView(headerStats);

        LinearLayout tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setPadding(dp(12), 0, dp(12), dp(8));
        final Button btnLog = tabButton("Log");
        final Button btnSyllabus = tabButton("Syllabus");
        final Button btnTests = tabButton("Tests");
        tabBar.addView(btnLog);
        tabBar.addView(btnSyllabus);
        tabBar.addView(btnTests);
        root.addView(tabBar);

        content = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);

        logTab = buildLogTab();
        syllabusTab = buildSyllabusTab();
        testsTab = buildTestsTab();

        btnLog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showTab(logTab); }
        });
        btnSyllabus.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showTab(syllabusTab); }
        });
        btnTests.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showTab(testsTab); }
        });

        showTab(logTab);
        refreshHeader();
    }

    private Button tabButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(4), 0, dp(4), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void showTab(LinearLayout tab) {
        content.removeAllViews();
        content.addView(tab);
    }

    private void refreshHeader() {
        double today = store.todayTotalHours();
        int streak = store.currentStreak();
        long daysLeft = DataStore.daysUntilExam();
        headerStats.setText(String.format(Locale.US,
                "Today: %.1fh logged   \u2022   Streak: %d day(s)   \u2022   NEET in %d days", today, streak, daysLeft));
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
        l.setPadding(dp(16), dp(8), dp(16), dp(24));

        List<String> keys = new ArrayList<String>(Arrays.asList(Syllabus.SUBJECT_KEYS));
        keys.addAll(Syllabus.EXTRA_ACTIVITIES.keySet());
        final String today = DataStore.todayStr();

        for (final String key : keys) {
            String label = Syllabus.SUBJECTS.containsKey(key)
                    ? Syllabus.SUBJECTS.get(key).label
                    : Syllabus.EXTRA_ACTIVITIES.get(key);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(10), dp(10), dp(10));
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.bottomMargin = dp(8);
            row.setLayoutParams(rowLp);
            row.setBackgroundColor(Color.parseColor("#FFFFFF"));

            TextView name = new TextView(this);
            name.setText(label);
            name.setTextSize(14);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            name.setLayoutParams(nameLp);
            row.addView(name);

            final TextView value = new TextView(this);
            value.setTextSize(13);
            value.setPadding(dp(8), 0, dp(8), 0);
            value.setText(String.format(Locale.US, "%.1fh", store.getLogHours(today, key)));

            Button minus = new Button(this); minus.setText("-");
            Button plus = new Button(this); plus.setText("+");

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
        return l;
    }

    // ---------------- SYLLABUS TAB ----------------
    private LinearLayout buildSyllabusTab() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(16), dp(8), dp(16), dp(24));

        for (final Syllabus.Subject subj : Syllabus.SUBJECTS.values()) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(12), dp(12), dp(12));
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardLp.bottomMargin = dp(12);
            card.setLayoutParams(cardLp);

            final TextView header = new TextView(this);
            header.setTextSize(16);
            header.setTypeface(null, android.graphics.Typeface.BOLD);
            header.setTextColor(Color.parseColor(subj.color));
            header.setText(subj.label + "  (" + Math.round(store.subjectProgress(subj.key) * 100) + "%)");
            card.addView(header);

            final LinearLayout chapterHolder = new LinearLayout(this);
            chapterHolder.setOrientation(LinearLayout.VERTICAL);
            chapterHolder.setVisibility(View.GONE);
            header.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    chapterHolder.setVisibility(
                            chapterHolder.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                }
            });

            for (Map.Entry<String, String[]> group : subj.groups.entrySet()) {
                TextView groupLabel = new TextView(this);
                groupLabel.setText(group.getKey());
                groupLabel.setTextSize(11);
                groupLabel.setTextColor(Color.parseColor("#6B8489"));
                groupLabel.setPadding(0, dp(10), 0, dp(4));
                chapterHolder.addView(groupLabel);

                final String groupKey = group.getKey();

                for (final String chapter : group.getValue()) {
                    LinearLayout chRow = new LinearLayout(this);
                    chRow.setOrientation(LinearLayout.VERTICAL);
                    chRow.setPadding(dp(4), dp(6), dp(4), dp(6));

                    TextView chName = new TextView(this);
                    chName.setText(chapter);
                    chName.setTextSize(13.5f);
                    chRow.addView(chName);

                    final LinearLayout subtaskHolder = new LinearLayout(this);
                    subtaskHolder.setOrientation(LinearLayout.VERTICAL);
                    subtaskHolder.setPadding(dp(20), 0, 0, 0);
                    subtaskHolder.setVisibility(View.GONE);
                    chName.setOnClickListener(new View.OnClickListener() {
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
                        cb.setTextSize(12.5f);
                        cb.setChecked(store.isSubtaskDone(subj.key, groupKey, chapter, stKey));
                        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            public void onCheckedChanged(CompoundButton btn, boolean checked) {
                                store.setSubtaskDone(subj.key, groupKey, chapter, stKey, checked);
                                header.setText(subj.label + "  (" + Math.round(store.subjectProgress(subj.key) * 100) + "%)");
                                refreshHeader();
                            }
                        });
                        subtaskHolder.addView(cb);
                    }
                    chRow.addView(subtaskHolder);
                    chapterHolder.addView(chRow);
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
        l.setPadding(dp(16), dp(8), dp(16), dp(24));

        final Spinner subjectSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"physics", "chemistry", "biology"});
        subjectSpinner.setAdapter(adapter);
        l.addView(subjectSpinner);

        final EditText dateInput = new EditText(this);
        dateInput.setHint("Date (yyyy-mm-dd)");
        dateInput.setText(DataStore.todayStr());
        l.addView(dateInput);

        final EditText scoreInput = new EditText(this);
        scoreInput.setHint("Score");
        scoreInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        l.addView(scoreInput);

        final EditText totalInput = new EditText(this);
        totalInput.setHint("Total marks");
        totalInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        l.addView(totalInput);

        Button addBtn = new Button(this);
        addBtn.setText("Add Test");
        l.addView(addBtn);

        final LinearLayout listHolder = new LinearLayout(this);
        listHolder.setOrientation(LinearLayout.VERTICAL);
        listHolder.setPadding(0, dp(16), 0, 0);
        l.addView(listHolder);

        final Runnable[] refresh = new Runnable[1];
        refresh[0] = new Runnable() {
            public void run() {
                listHolder.removeAllViews();
                JSONArray tests = store.getTests();
                for (int i = 0; i < tests.length(); i++) {
                    JSONObject t = tests.optJSONObject(i);
                    if (t == null) continue;
                    LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(dp(8), dp(8), dp(8), dp(8));
                    TextView tv = new TextView(MainActivity.this);
                    double score = t.optDouble("score", 0), total = t.optDouble("total", 1);
                    tv.setText(t.optString("subject") + " \u2014 " + t.optString("date") + " \u2014 "
                            + score + "/" + total + " (" + Math.round(100.0 * score / total) + "%)");
                    LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                    tv.setLayoutParams(tvLp);
                    row.addView(tv);

                    Button del = new Button(MainActivity.this);
                    del.setText("\u2715");
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
}
