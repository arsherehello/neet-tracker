package com.neet.tracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.widget.RemoteViews;

import java.util.Locale;

public class TrackerWidgetProvider extends AppWidgetProvider {

    private static final int TRACK = Color.parseColor("#DCEAE8");
    private static final int INK = Color.parseColor("#0B2A2E");

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAll(context, appWidgetManager, appWidgetIds);
    }

    public static void updateAll(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
        DataStore store = new DataStore(context);
        String today = DataStore.todayStr();
        float density = context.getResources().getDisplayMetrics().density;
        int pieSizePx = Math.round(46 * density);

        double physicsH = store.getLogHours(today, "physics");
        double chemistryH = store.getLogHours(today, "chemistry");
        double biologyH = store.getLogHours(today, "biology");
        double sleepH = store.getLogHours(today, "sleep");
        double choresH = store.getLogHours(today, "chores");
        double totalToday = store.todayTotalHours();

        double maxBar = Math.max(0.1, Math.max(physicsH, Math.max(chemistryH, Math.max(biologyH, Math.max(sleepH, choresH)))));

        int physicsColor = Color.parseColor(Syllabus.SUBJECTS.get("physics").color);
        int chemistryColor = Color.parseColor(Syllabus.SUBJECTS.get("chemistry").color);
        int biologyColor = Color.parseColor(Syllabus.SUBJECTS.get("biology").color);

        Bitmap piePhysics = makePieBitmap(pieSizePx, physicsColor, store.subjectProgress("physics"));
        Bitmap pieChemistry = makePieBitmap(pieSizePx, chemistryColor, store.subjectProgress("chemistry"));
        Bitmap pieBiology = makePieBitmap(pieSizePx, biologyColor, store.subjectProgress("biology"));

        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            views.setTextViewText(R.id.widget_total_big, String.format(Locale.US, "%.1fh", totalToday));
            views.setTextViewText(R.id.widget_total_caption, "logged today");

            views.setImageViewBitmap(R.id.widget_pie_physics, piePhysics);
            views.setImageViewBitmap(R.id.widget_pie_chemistry, pieChemistry);
            views.setImageViewBitmap(R.id.widget_pie_biology, pieBiology);

            views.setTextViewText(R.id.widget_bar_physics_label, String.format(Locale.US, "Physics %.1fh", physicsH));
            views.setProgressBar(R.id.widget_bar_physics, 100, (int) Math.round(physicsH / maxBar * 100), false);

            views.setTextViewText(R.id.widget_bar_chemistry_label, String.format(Locale.US, "Chemistry %.1fh", chemistryH));
            views.setProgressBar(R.id.widget_bar_chemistry, 100, (int) Math.round(chemistryH / maxBar * 100), false);

            views.setTextViewText(R.id.widget_bar_biology_label, String.format(Locale.US, "Biology %.1fh", biologyH));
            views.setProgressBar(R.id.widget_bar_biology, 100, (int) Math.round(biologyH / maxBar * 100), false);

            views.setTextViewText(R.id.widget_bar_sleep_label, String.format(Locale.US, "Sleep %.1fh", sleepH));
            views.setProgressBar(R.id.widget_bar_sleep, 100, (int) Math.round(sleepH / maxBar * 100), false);

            views.setTextViewText(R.id.widget_bar_chores_label, String.format(Locale.US, "Chores %.1fh", choresH));
            views.setProgressBar(R.id.widget_bar_chores, 100, (int) Math.round(choresH / maxBar * 100), false);

            Intent launch = new Intent(context, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, launch,
                    PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            views.setOnClickPendingIntent(R.id.widget_root, pi);

            mgr.updateAppWidget(id, views);
        }
    }

    /** Draws a small donut: colored arc for `fraction` completion, light track for the rest, % label in the center. */
    private static Bitmap makePieBitmap(int sizePx, int color, double fraction) {
        Bitmap bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        float stroke = sizePx * 0.16f;
        arcPaint.setStrokeWidth(stroke);
        RectF rect = new RectF(stroke / 2f, stroke / 2f, sizePx - stroke / 2f, sizePx - stroke / 2f);

        arcPaint.setColor(TRACK);
        canvas.drawArc(rect, 0, 360, false, arcPaint);

        if (fraction > 0) {
            arcPaint.setColor(color);
            canvas.drawArc(rect, -90, (float) (Math.min(1.0, fraction) * 360), false, arcPaint);
        }

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(INK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        textPaint.setTextSize(sizePx * 0.26f);
        float textY = sizePx / 2f - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(Math.round(fraction * 100) + "%", sizePx / 2f, textY, textPaint);

        return bmp;
    }
}
