package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

/** Lightweight animated Ferrum launch mark, drawn natively without bitmap assets. */
public final class FerrumSplashView extends View {

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final long startedAt = android.os.SystemClock.uptimeMillis();

    public FerrumSplashView(Context context) {
        super(context);
        setWillNotDraw(false);
        setAlpha(1f);
        backgroundPaint.setColor(0xFF080B12);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(AndroidUtilities.dp(2));
        ringPaint.setColor(0xFF6E9BFF);
        dotPaint.setColor(0xFF8BC7FF);
        textPaint.setColor(0xFFF4F7FF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        textPaint.setTextSize(AndroidUtilities.dp(34));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        canvas.drawRect(0, 0, w, h, backgroundPaint);

        float elapsed = (android.os.SystemClock.uptimeMillis() - startedAt) / 1000f;
        float cx = w / 2f;
        float cy = h / 2f - AndroidUtilities.dp(8);
        float radius = AndroidUtilities.dp(34);
        float pulse = 1f + (float) Math.sin(elapsed * 4.2f) * 0.07f;

        canvas.save();
        canvas.rotate(elapsed * 150f, cx, cy);
        canvas.drawCircle(cx, cy, radius * pulse, ringPaint);
        canvas.drawCircle(cx + radius * pulse, cy, AndroidUtilities.dp(4), dotPaint);
        canvas.restore();

        textPaint.setAlpha((int) (220 + 35 * Math.sin(elapsed * 3.1f)));
        canvas.drawText("Ferrum", cx, cy + AndroidUtilities.dp(78), textPaint);

        if (elapsed < 1.45f) {
            postInvalidateDelayed(16);
        }
    }
}
