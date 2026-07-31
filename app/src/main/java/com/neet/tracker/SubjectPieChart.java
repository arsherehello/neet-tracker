package com.neet.tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small donut chart: slice per subject sized by its share of total completed subtasks. */
public class SubjectPieChart extends View {

    private static final int TRACK = Color.parseColor("#DCEAE8");
    private static final int INK = Color.parseColor("#0B2A2E");

    private LinkedHashMap<String, Double> shares = new LinkedHashMap<>();
    private LinkedHashMap<String, Integer> colors = new LinkedHashMap<>();
    private String centerText = "0%";

    private final Paint slicePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    public SubjectPieChart(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        slicePaint.setStyle(Paint.Style.STROKE);
        textPaint.setColor(INK);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    public void setData(LinkedHashMap<String, Double> shares, LinkedHashMap<String, Integer> colors, String centerText) {
        this.shares = shares;
        this.colors = colors;
        this.centerText = centerText;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        float strokeWidth = dp(6);
        slicePaint.setStrokeWidth(strokeWidth);
        float pad = strokeWidth / 2f + dp(1);
        bounds.set(pad, pad, w - pad, h - pad);

        boolean anyProgress = false;
        for (double v : shares.values()) if (v > 0) { anyProgress = true; break; }

        if (!anyProgress) {
            slicePaint.setColor(TRACK);
            canvas.drawArc(bounds, 0, 360, false, slicePaint);
        } else {
            float startAngle = -90f;
            float usedSweep = 0f;
            for (Map.Entry<String, Double> e : shares.entrySet()) {
                double share = e.getValue();
                if (share <= 0) continue;
                float sweep = (float) (share * 360);
                Integer color = colors.get(e.getKey());
                slicePaint.setColor(color != null ? color : TRACK);
                canvas.drawArc(bounds, startAngle, sweep, false, slicePaint);
                startAngle += sweep;
                usedSweep += sweep;
            }
            if (usedSweep < 359.5f) {
                slicePaint.setColor(TRACK);
                canvas.drawArc(bounds, startAngle, 360 - usedSweep, false, slicePaint);
            }
        }

        textPaint.setTextSize(w * 0.24f);
        float textY = h / 2f - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(centerText, w / 2f, textY, textPaint);
    }
}
