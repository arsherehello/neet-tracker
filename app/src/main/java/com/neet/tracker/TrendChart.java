package com.neet.tracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Line chart of test score percentages, in chronological order. */
public class TrendChart extends View {

    private static final int TEAL = Color.parseColor("#0E7C7B");
    private static final int CORAL = Color.parseColor("#FF6A55");
    private static final int BORDER = Color.parseColor("#DCEAE8");

    private List<Double> percentages = new ArrayList<>();
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public TrendChart(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        linePaint.setColor(TEAL);
        linePaint.setStrokeWidth(dp(2));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        dotPaint.setColor(CORAL);
        dotPaint.setStyle(Paint.Style.FILL);

        axisPaint.setColor(BORDER);
        axisPaint.setStrokeWidth(dp(1));
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    public void setPercentages(List<Double> percentages) {
        this.percentages = percentages;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        float pad = dp(10);

        canvas.drawLine(pad, h - pad, w - pad, h - pad, axisPaint);

        if (percentages.size() < 2) {
            if (percentages.size() == 1) {
                float x = w / 2f;
                float y = (float) (h - pad - (percentages.get(0) / 100.0) * (h - pad * 2));
                canvas.drawCircle(x, y, dp(3.5f), dotPaint);
            }
            return;
        }

        int n = percentages.size();
        float stepX = (w - pad * 2) / (n - 1);
        Path path = new Path();
        float[] xs = new float[n], ys = new float[n];

        for (int i = 0; i < n; i++) {
            float x = pad + i * stepX;
            float y = (float) (h - pad - (percentages.get(i) / 100.0) * (h - pad * 2));
            xs[i] = x; ys[i] = y;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        canvas.drawPath(path, linePaint);
        for (int i = 0; i < n; i++) canvas.drawCircle(xs[i], ys[i], dp(3.5f), dotPaint);
    }
}
