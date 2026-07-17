package com.neet.tracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.RemoteViews;

import java.util.Locale;

/** Renders the whole app-header (rings, countdown, streak, mood bar) as one bitmap for the home-screen widget. */
public class TrackerWidgetProvider extends AppWidgetProvider {

    private static final int BG = Color.parseColor("#F5FAF9");
    private static final int INK = Color.parseColor("#0B2A2E");
    private static final int MUTED = Color.parseColor("#6B8489");
    private static final int BORDER = Color.parseColor("#DCEAE8");
    private static final int TEAL = Color.parseColor("#0E7C7B");
    private static final int TEAL_DARK = Color.parseColor("#0A5F5E");
    private static final int CORAL = Color.parseColor("#FF6A55");

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAll(context, appWidgetManager, appWidgetIds);
    }

    public static void updateAll(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
        DataStore store = new DataStore(context);
        float density = context.getResources().getDisplayMetrics().density;
        int wPx = Math.round(340 * density);
        int hPx = Math.round(190 * density);

        Bitmap bmp = renderHeader(store, density, wPx, hPx);

        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            views.setImageViewBitmap(R.id.widget_content_image, bmp);

            Intent launch = new Intent(context, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, launch,
                    PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            views.setOnClickPendingIntent(R.id.widget_root, pi);

            mgr.updateAppWidget(id, views);
        }
    }

    private static String moodEmoji(double intensity) {
        if (intensity <= 0) return "\uD83D\uDE34";
        if (intensity < 0.25) return "\uD83D\uDE10";
        if (intensity < 0.5) return "\uD83D\uDE42";
        if (intensity < 0.75) return "\uD83D\uDE03";
        return "\uD83D\uDD25";
    }

    private static Bitmap renderHeader(DataStore store, float density, int wPx, int hPx) {
        Bitmap bmp = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(BG);

        float dp = density;
        float pad = 14 * dp;

        double physicsPct = store.subjectProgress("physics");
        double chemistryPct = store.subjectProgress("chemistry");
        double biologyPct = store.subjectProgress("biology");
        int physicsColor = Color.parseColor(Syllabus.SUBJECTS.get("physics").color);
        int chemistryColor = Color.parseColor(Syllabus.SUBJECTS.get("chemistry").color);
        int biologyColor = Color.parseColor(Syllabus.SUBJECTS.get("biology").color);

        double studyToday = store.todayStudyHours();
        double allToday = store.todayTotalHours();
        double intensity = Math.min(studyToday / 6.0, 1.0);
        int streak = store.currentStreak();

        long ms = DataStore.millisUntilExam();
        long days = ms / (24L * 3600 * 1000);
        long hours = (ms / (3600L * 1000)) % 24;
        long mins = (ms / (60L * 1000)) % 60;
        long secs = (ms / 1000L) % 60;

        // ---- ring row (left) ----
        float ringDiam = 46 * dp;
        float ringGap = 14 * dp;
        float ringX = pad;
        float ringY = pad;
        String[] ringLabels = {"PHYS", "CHEM", "BIOL"};
        int[] ringColors = {physicsColor, chemistryColor, biologyColor};
        double[] ringPcts = {physicsPct, chemistryPct, biologyPct};
        for (int i = 0; i < 3; i++) {
            drawRing(c, ringX, ringY, ringDiam, ringColors[i], ringPcts[i], ringLabels[i], dp);
            ringX += ringDiam + ringGap;
        }

        // ---- vertical divider ----
        float dividerX = ringX - ringGap / 2f + 6 * dp;
        Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(BORDER);
        dividerPaint.setStrokeWidth(2 * dp);
        c.drawLine(dividerX, pad, dividerX, pad + ringDiam + 26 * dp, dividerPaint);

        // ---- right column: countdown + streak ----
        float rightX = wPx - pad;
        Paint smallCapsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        smallCapsPaint.setColor(MUTED);
        smallCapsPaint.setTextSize(9 * dp);
        smallCapsPaint.setTypeface(Typeface.DEFAULT_BOLD);
        smallCapsPaint.setTextAlign(Paint.Align.RIGHT);
        c.drawText("NEET 2027", rightX, pad + 8 * dp, smallCapsPaint);

        String[] units = {String.valueOf(days), String.format(Locale.US, "%02d", hours),
                String.format(Locale.US, "%02d", mins), String.format(Locale.US, "%02d", secs)};
        String[] unitLabels = {"DAYS", "HRS", "MIN", "SEC"};

        Paint numPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        numPaint.setColor(TEAL_DARK);
        numPaint.setTypeface(Typeface.DEFAULT_BOLD);
        numPaint.setTextSize(17 * dp);
        numPaint.setTextAlign(Paint.Align.RIGHT);

        Paint unitLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        unitLabelPaint.setColor(MUTED);
        unitLabelPaint.setTextSize(7 * dp);
        unitLabelPaint.setTextAlign(Paint.Align.CENTER);

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(BORDER);
        dotPaint.setTextSize(14 * dp);
        dotPaint.setTypeface(Typeface.DEFAULT_BOLD);
        dotPaint.setTextAlign(Paint.Align.CENTER);

        float numY = pad + 26 * dp;
        float unitGap = 26 * dp;
        float cursorX = rightX;
        float[] unitCenters = new float[4];
        // measure widths right-to-left so blocks read left->right visually as DAYS:HRS:MIN:SEC
        float[] widths = new float[4];
        for (int i = 0; i < 4; i++) widths[i] = Math.max(numPaint.measureText(units[i]), 20 * dp);
        float totalW = 0;
        for (float w : widths) totalW += w;
        totalW += unitGap * 3;
        float startX = rightX - totalW;
        float x = startX;
        for (int i = 0; i < 4; i++) {
            float blockCenter = x + widths[i] / 2f;
            unitCenters[i] = blockCenter;
            Paint rightAligned = new Paint(numPaint);
            rightAligned.setTextAlign(Paint.Align.CENTER);
            c.drawText(units[i], blockCenter, numY, rightAligned);
            c.drawText(unitLabels[i], blockCenter, numY + 10 * dp, unitLabelPaint);
            x += widths[i];
            if (i < 3) {
                c.drawText(":", x + unitGap / 2f, numY - 4 * dp, dotPaint);
                x += unitGap;
            }
        }

        // streak block, right-aligned below countdown
        Paint streakNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        streakNumPaint.setColor(CORAL);
        streakNumPaint.setTypeface(Typeface.DEFAULT_BOLD);
        streakNumPaint.setTextSize(20 * dp);
        streakNumPaint.setTextAlign(Paint.Align.RIGHT);
        float streakY = numY + 34 * dp;
        c.drawText(String.valueOf(streak), rightX, streakY, streakNumPaint);

        Paint streakLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        streakLabelPaint.setColor(MUTED);
        streakLabelPaint.setTextSize(9 * dp);
        streakLabelPaint.setTextAlign(Paint.Align.RIGHT);
        c.drawText("DAY STREAK", rightX, streakY + 12 * dp, streakLabelPaint);

        // ---- mood bar ----
        float barY = pad + ringDiam + 34 * dp;
        float barH = 9 * dp;
        float barLeft = pad;
        float barRight = wPx - pad;
        RectF trackRect = new RectF(barLeft, barY, barRight, barY + barH);
        Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(BORDER);
        c.drawRoundRect(trackRect, barH / 2f, barH / 2f, trackPaint);

        float fillW = Math.max((float) ((barRight - barLeft) * intensity), 6 * dp);
        RectF fillRect = new RectF(barLeft, barY, barLeft + fillW, barY + barH);
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setShader(new LinearGradient(barLeft, 0, barRight, 0, TEAL, CORAL, Shader.TileMode.CLAMP));
        c.drawRoundRect(fillRect, barH / 2f, barH / 2f, fillPaint);

        Paint facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        facePaint.setTextSize(20 * dp);
        facePaint.setTextAlign(Paint.Align.CENTER);
        float faceX = barLeft + fillW;
        if (faceX < barLeft + 12 * dp) faceX = barLeft + 12 * dp;
        if (faceX > barRight - 12 * dp) faceX = barRight - 12 * dp;
        c.drawText(moodEmoji(intensity), faceX, barY + barH + 16 * dp, facePaint);

        // ---- caption ----
        String captionText = studyToday > 0
                ? String.format(Locale.US, "%.1f hrs of study today \u00b7 %.1f / 24 hrs logged overall", studyToday, allToday)
                : "No study logged yet today \u2014 the line is flat until you log something";
        TextPaint captionPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        captionPaint.setColor(MUTED);
        captionPaint.setTextSize(11 * dp);
        int captionWidth = wPx - Math.round(pad * 2);
        StaticLayout captionLayout = new StaticLayout(captionText, captionPaint, captionWidth,
                Layout.Alignment.ALIGN_NORMAL, 1.1f, 0f, false);
        c.save();
        c.translate(barLeft, barY + barH + 26 * dp);
        captionLayout.draw(c);
        c.restore();

        return bmp;
    }

    private static void drawRing(Canvas c, float x, float y, float diam, int color, double fraction, String label, float dp) {
        float stroke = diam * 0.14f;
        RectF rect = new RectF(x + stroke / 2f, y + stroke / 2f, x + diam - stroke / 2f, y + diam - stroke / 2f);

        Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(stroke);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        arcPaint.setColor(BORDER);
        c.drawArc(rect, 0, 360, false, arcPaint);

        float sweep = (float) (Math.max(0.012, fraction) * 360);
        arcPaint.setColor(color);
        c.drawArc(rect, -90, sweep, false, arcPaint);

        Paint pctPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pctPaint.setColor(INK);
        pctPaint.setTypeface(Typeface.DEFAULT_BOLD);
        pctPaint.setTextSize(diam * 0.24f);
        pctPaint.setTextAlign(Paint.Align.CENTER);
        float cx = x + diam / 2f;
        float cy = y + diam / 2f - ((pctPaint.descent() + pctPaint.ascent()) / 2f);
        c.drawText(Math.round(fraction * 100) + "%", cx, cy, pctPaint);

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(color);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);
        labelPaint.setTextSize(9.5f * dp);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        c.drawText(label, cx, y + diam + 13 * dp, labelPaint);
    }
}
