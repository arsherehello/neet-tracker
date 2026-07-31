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
import android.os.Bundle;
import android.widget.RemoteViews;

/** Renders the subject-completion rings + mood bar as one bitmap for the home-screen widget. */
public class TrackerWidgetProvider extends AppWidgetProvider {

    private static final int BG = Color.parseColor("#F5FAF9");
    private static final int INK = Color.parseColor("#0B2A2E");
    private static final int BORDER = Color.parseColor("#DCEAE8");
    private static final int TEAL = Color.parseColor("#0E7C7B");
    private static final int CORAL = Color.parseColor("#FF6A55");

    private static final int DEFAULT_W_DP = 220;
    private static final int DEFAULT_H_DP = 130;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAll(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        updateAll(context, appWidgetManager, new int[]{appWidgetId});
    }

    public static void updateAll(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
        DataStore store = new DataStore(context);
        float density = context.getResources().getDisplayMetrics().density;

        for (int id : appWidgetIds) {
            int wDp = DEFAULT_W_DP, hDp = DEFAULT_H_DP;
            Bundle options = mgr.getAppWidgetOptions(id);
            if (options != null) {
                int minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
                int minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
                if (minW > 0) wDp = minW;
                if (minH > 0) hDp = minH;
            }
            int wPx = Math.round(wDp * density);
            int hPx = Math.round(hDp * density);

            Bitmap bmp = renderContent(store, density, wPx, hPx);

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

    private static Bitmap renderContent(DataStore store, float density, int wPx, int hPx) {
        Bitmap bmp = Bitmap.createBitmap(Math.max(wPx, 1), Math.max(hPx, 1), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        float cornerRadius = 22 * density;
        RectF cardRect = new RectF(0, 0, wPx, hPx);
        Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardPaint.setColor(BG);
        c.drawRoundRect(cardRect, cornerRadius, cornerRadius, cardPaint);

        float dp = density;
        float pad = 10 * dp;

        double physicsPct = store.subjectProgress("physics");
        double chemistryPct = store.subjectProgress("chemistry");
        double biologyPct = store.subjectProgress("biology");
        int physicsColor = Color.parseColor(Syllabus.SUBJECTS.get("physics").color);
        int chemistryColor = Color.parseColor(Syllabus.SUBJECTS.get("chemistry").color);
        int biologyColor = Color.parseColor(Syllabus.SUBJECTS.get("biology").color);

        double studyToday = store.todayStudyHours();
        double intensity = Math.min(studyToday / 6.0, 1.0);

        // ---- ring row, evenly spread across the available width ----
        float ringsAreaW = wPx - pad * 2;
        float ringDiam = Math.min(ringsAreaW / 3.4f, (hPx - pad * 2) * 0.55f);
        ringDiam = Math.max(ringDiam, 20 * dp);
        float slot = ringsAreaW / 3f;
        float ringY = pad;

        String[] ringLabels = {"PHYS", "CHEM", "BIOL"};
        int[] ringColors = {physicsColor, chemistryColor, biologyColor};
        double[] ringPcts = {physicsPct, chemistryPct, biologyPct};
        for (int i = 0; i < 3; i++) {
            float slotCenter = pad + slot * i + slot / 2f;
            float ringX = slotCenter - ringDiam / 2f;
            drawRing(c, ringX, ringY, ringDiam, ringColors[i], ringPcts[i], ringLabels[i], dp);
        }

        // ---- mood bar ----
        float barY = ringY + ringDiam + 22 * dp;
        float barH = Math.max(6 * dp, Math.min(10 * dp, hPx * 0.06f));
        float barLeft = pad;
        float barRight = wPx - pad;
        if (barY + barH + 14 * dp <= hPx && barRight > barLeft) {
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
            facePaint.setTextSize(Math.min(20 * dp, hPx - (barY + barH)));
            facePaint.setTextAlign(Paint.Align.CENTER);
            float faceX = barLeft + fillW;
            if (faceX < barLeft + 12 * dp) faceX = barLeft + 12 * dp;
            if (faceX > barRight - 12 * dp) faceX = barRight - 12 * dp;
            float faceY = Math.min(barY + barH + 16 * dp, hPx - 2 * dp);
            c.drawText(moodEmoji(intensity), faceX, faceY, facePaint);
        }

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
        labelPaint.setTextSize(Math.max(7 * dp, diam * 0.19f));
        labelPaint.setTextAlign(Paint.Align.CENTER);
        c.drawText(label, cx, y + diam + 13 * dp, labelPaint);
    }
}
