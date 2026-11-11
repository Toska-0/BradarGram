package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;

/**
 * Monochrome Ferrum launch animation.
 * The mark mirrors the app icon: a metallic F enclosed by a silver ring.
 */
public final class FerrumSplashView extends View {

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint wordmarkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path markPath = new Path();
    private final long startedAt = android.os.SystemClock.uptimeMillis();

    public FerrumSplashView(Context context) {
        super(context);
        setWillNotDraw(false);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        backgroundPaint.setShader(new LinearGradient(
                0, 0, 0, 1,
                new int[]{0xFF080B12, 0xFF05070C, 0xFF030303},
                null, Shader.TileMode.CLAMP));

        haloPaint.setStyle(Paint.Style.STROKE);
        haloPaint.setStrokeWidth(AndroidUtilities.dp(12));
        haloPaint.setColor(0x18D8D9DC);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(AndroidUtilities.dp(3.2f));
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setShader(new LinearGradient(
                0, 0, AndroidUtilities.dp(120), AndroidUtilities.dp(120),
                new int[]{0xFF77797E, 0xFFE0E1E4, 0xFF85878C},
                null, Shader.TileMode.CLAMP));

        markPaint.setStyle(Paint.Style.FILL);
        markPaint.setShader(new LinearGradient(
                0, -AndroidUtilities.dp(48), 0, AndroidUtilities.dp(48),
                new int[]{0xFFF0F0F2, 0xFF85868B},
                null, Shader.TileMode.CLAMP));

        wordmarkPaint.setColor(0xFFB9BABE);
        wordmarkPaint.setTextAlign(Paint.Align.CENTER);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        canvas.drawRect(0, 0, w, h, backgroundPaint);

        float elapsed = (android.os.SystemClock.uptimeMillis() - startedAt) / 1000f;
        float intro = clamp(elapsed / 0.62f);
        float settle = 1f - (float) Math.pow(1f - intro, 3f);
        float pulse = 1f + (float) Math.sin(elapsed * 2.4f) * 0.012f;
        float cx = w / 2f;
        float cy = h / 2f - AndroidUtilities.dp(12);
        float radius = AndroidUtilities.dp(61);

        canvas.save();
        canvas.translate(cx, cy);

        float ringScale = 0.72f + 0.28f * settle;
        canvas.save();
        canvas.scale(ringScale * pulse, ringScale * pulse);
        haloPaint.setAlpha((int) (45 * settle));
        canvas.drawCircle(0, 0, radius, haloPaint);
        ringPaint.setAlpha((int) (255 * settle));
        canvas.drawCircle(0, 0, radius, ringPaint);
        canvas.restore();

        float markScale = 0.68f + 0.32f * clamp((elapsed - 0.12f) / 0.58f);
        canvas.save();
        canvas.scale(markScale, markScale);
        markPaint.setAlpha((int) (255 * clamp((elapsed - 0.10f) / 0.48f)));
        buildMarkPath();
        canvas.drawPath(markPath, markPaint);
        canvas.restore();

        canvas.restore();

        int wordAlpha = (int) (190 * clamp((elapsed - 0.42f) / 0.42f));
        wordmarkPaint.setAlpha(wordAlpha);
        wordmarkPaint.setTextSize(AndroidUtilities.dp(11));
        canvas.drawText("F E R R U M", cx, cy + AndroidUtilities.dp(96), wordmarkPaint);

        if (elapsed < 1.7f) {
            postInvalidateDelayed(16);
        }
    }

    private void buildMarkPath() {
        float d = AndroidUtilities.density;
        markPath.reset();
        markPath.moveTo(-31 * d, -36 * d);
        markPath.lineTo(34 * d, -36 * d);
        markPath.lineTo(20 * d, -18 * d);
        markPath.lineTo(-13 * d, -18 * d);
        markPath.lineTo(-16 * d, -5 * d);
        markPath.lineTo(22 * d, -5 * d);
        markPath.lineTo(8 * d, 13 * d);
        markPath.lineTo(-20 * d, 13 * d);
        markPath.lineTo(-25 * d, 36 * d);
        markPath.lineTo(-45 * d, 36 * d);
        markPath.lineTo(-35 * d, -26 * d);
        markPath.quadTo(-34 * d, -36 * d, -31 * d, -36 * d);
        markPath.close();
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
