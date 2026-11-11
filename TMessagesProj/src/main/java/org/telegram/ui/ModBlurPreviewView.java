package org.telegram.ui;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

/** Небольшое превью: как выглядит панель с размытием и без него. */
public class ModBlurPreviewView extends View {

    private boolean blurDisabled;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blurPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF card = new RectF();
    private final RectF bar = new RectF();
    private final RectF tmp = new RectF();
    private final Path clip = new Path();

    private static final int[] BLOB_COLORS = {0xFFF4A6C0, 0xFF8EC5F2, 0xFF9AD9B8, 0xFFF6C98A};
    private static final float[][] BLOBS = {
            {0.16f, 0.62f, 0.17f},
            {0.42f, 0.78f, 0.22f},
            {0.70f, 0.66f, 0.18f},
            {0.90f, 0.80f, 0.15f}
    };

    public ModBlurPreviewView(Context context) {
        super(context);
        // BlurMaskFilter надёжно работает на программном слое, превью маленькое
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        blurPaint.setMaskFilter(new BlurMaskFilter(dp(9), BlurMaskFilter.Blur.NORMAL));
        textPaint.setTextSize(dp(12));
    }

    private static int dp(float v) {
        return AndroidUtilities.dp(v);
    }

    public void setBlurDisabled(boolean disabled) {
        if (blurDisabled != disabled) {
            blurDisabled = disabled;
            invalidate();
        } else {
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), dp(136));
    }

    private void drawContent(Canvas canvas, Paint p, boolean dark) {
        for (int i = 0; i < BLOBS.length; i++) {
            p.setColor(BLOB_COLORS[i]);
            canvas.drawCircle(card.left + card.width() * BLOBS[i][0], card.top + card.height() * BLOBS[i][1], card.height() * BLOBS[i][2], p);
        }
        p.setColor(dark ? 0xFF7C8A98 : 0xFF9FB0C0);
        float x = card.left + dp(12);
        tmp.set(x, card.top + dp(34), x + card.width() * 0.55f, card.top + dp(41));
        canvas.drawRoundRect(tmp, dp(3.5f), dp(3.5f), p);
        tmp.set(x, card.top + dp(48), x + card.width() * 0.38f, card.top + dp(55));
        canvas.drawRoundRect(tmp, dp(3.5f), dp(3.5f), p);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final boolean dark = Theme.isCurrentThemeDark();
        final int w = getWidth();
        final int h = getHeight();
        final float pad = dp(16);
        final float r = dp(14);

        card.set(pad, dp(8), w - pad, h - dp(8));
        canvas.save();
        clip.reset();
        clip.addRoundRect(card, r, r, Path.Direction.CW);
        canvas.clipPath(clip);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(dark ? 0xFF1B2430 : 0xFFEAF1F8);
        canvas.drawRect(card, paint);
        drawContent(canvas, paint, dark);

        final float barH = dp(40);
        bar.set(card.left + dp(12), card.bottom - dp(12) - barH, card.right - dp(12), card.bottom - dp(12));
        final float br = barH / 2f;

        if (!blurDisabled) {
            canvas.save();
            Path barClip = new Path();
            barClip.addRoundRect(bar, br, br, Path.Direction.CW);
            canvas.clipPath(barClip);
            drawContent(canvas, blurPaint, dark);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(dark ? 0x99141B24 : 0x99FFFFFF);
            canvas.drawRoundRect(bar, br, br, paint);
            canvas.restore();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(dark ? 0x40FFFFFF : 0xB3FFFFFF);
            canvas.drawRoundRect(bar, br, br, paint);
        } else {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(dark ? 0xFF1F2933 : 0xFFFFFFFF);
            canvas.drawRoundRect(bar, br, br, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(dark ? 0x33FFFFFF : 0x1F000000);
            canvas.drawRoundRect(bar, br, br, paint);
        }

        // значки на панели
        paint.setStyle(Paint.Style.FILL);
        final float cy = bar.centerY();
        for (int i = 0; i < 3; i++) {
            float cx = bar.left + bar.width() * (0.2f + 0.3f * i);
            paint.setColor(i == 0 ? (dark ? 0xFF8CC0EE : 0xFF3A86C8) : (dark ? 0xFF8A97A4 : 0xFF8C9AA8));
            canvas.drawCircle(cx, cy, dp(6), paint);
        }
        canvas.restore();

        textPaint.setColor(dark ? 0xFFC3CED9 : 0xFF4A5A68);
        textPaint.setFakeBoldText(true);
        canvas.drawText(LocaleController.getString(blurDisabled ? R.string.ModBlurPreviewOff : R.string.ModBlurPreviewOn),
                card.left + dp(12), card.top + dp(21), textPaint);
    }
}
