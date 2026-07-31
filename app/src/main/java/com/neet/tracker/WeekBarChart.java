package com.neet.tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Locale;

/** Bar chart of hours logged over the last 7 days. */
public class WeekBarChart extends View {

    private static final int TEAL = Color.parseColor("#0E7C7B");
    private static final int TEAL_DARK = Color.parseColor("#0A5F5E");
    private static final int MUTED = Color.parseColor("#6B8489");

    private LinkedHashMap<String, Double> data = new LinkedHashMap<>();
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WeekBarChart(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        labelPaint.setColor(MUTED);
        labelPaint.setTextSize(sp(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);
    }

    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }
    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    public void setData(LinkedHashMap<String, Double> data) {
        this.data = data;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data.isEmpty()) return;

        int w = getWidth(), h = getHeight();
        float labelSpace = dp(16);
        float chartH = h - labelSpace;
        float n = data.size();
        float gap = dp(6);
        float barW = (w - gap * (n - 1)) / n;

        double max = 0.1;
        for (double v : data.values()) if (v > max) max = v;

        SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat outFmt = new SimpleDateFormat("EEE", Locale.US);

        int i = 0;
        for (String date : data.keySet()) {
            double val = data.get(date);
            float barH = (float) (val / max) * (chartH - dp(4));
            if (barH < dp(2) && val > 0) barH = dp(2);
            float left = i * (barW + gap);
            float right = left + barW;
            float top = chartH - barH;
            float bottom = chartH;

            barPaint.setShader(new LinearGradient(0, top, 0, bottom, TEAL, TEAL_DARK, Shader.TileMode.CLAMP));
            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, dp(4), dp(4), barPaint);

            String label;
            try { label = outFmt.format(inFmt.parse(date)); } catch (Exception e) { label = ""; }
            canvas.drawText(label, left + barW / 2f, h - dp(2), labelPaint);
            i++;
        }
    }
}
