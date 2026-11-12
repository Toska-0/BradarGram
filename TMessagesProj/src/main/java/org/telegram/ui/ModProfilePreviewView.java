package org.telegram.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FerrumConfig;
import org.telegram.ui.ActionBar.Theme;

/**
 * Ferrum: live preview of the profile banner and avatar ring settings.
 * Renders a mock profile header so banner scale/offset and ring
 * width/color/style can be judged without opening the profile.
 */
public class ModProfilePreviewView extends View {

    private static Bitmap bannerBitmap;
    private static String bannerBitmapUri;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clipPath = new Path();
    private final RectF rect = new RectF();

    public ModProfilePreviewView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) {
            width = AndroidUtilities.displaySize.x;
        }
        setMeasuredDimension(width, AndroidUtilities.dp(158));
    }

    /** Re-reads config and schedules a background banner load if needed. */
    public void refresh() {
        ensureBannerLoaded();
        invalidate();
    }

    private void ensureBannerLoaded() {
        final Context context = getContext();
        final String uri = FerrumConfig.getProfileBannerUri(context);
        if (uri == null) {
            if (bannerBitmapUri != null) {
                bannerBitmapUri = null;
                bannerBitmap = null;
            }
            return;
        }
        if (uri.equals(bannerBitmapUri) && bannerBitmap != null && !bannerBitmap.isRecycled()) {
            return;
        }
        bannerBitmapUri = uri;
        bannerBitmap = null;
        new Thread(() -> {
            try {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(context.getContentResolver().openInputStream(Uri.parse(uri)), null, bounds);
                int sample = 1;
                while (bounds.outWidth / (sample * 2) >= 1080) {
                    sample *= 2;
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = sample;
                Bitmap bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(Uri.parse(uri)), null, options);
                if (bitmap != null) {
                    bannerBitmap = bitmap;
                    postInvalidate();
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }, "FerrumBannerPreview").start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            drawPreview(canvas);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private void drawPreview(Canvas canvas) {
        final Context context = getContext();
        final int width = getWidth();
        final int margin = AndroidUtilities.dp(16);
        final int bannerTop = AndroidUtilities.dp(12);
        final int bannerHeight = AndroidUtilities.dp(92);
        final int radius = AndroidUtilities.dp(12);

        rect.set(margin, bannerTop, width - margin, bannerTop + bannerHeight);
        clipPath.reset();
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clipPath);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(null);
        if (bannerBitmap != null && !bannerBitmap.isRecycled()) {
            float scale = FerrumConfig.getBannerScale(context);
            float offsetX = FerrumConfig.getBannerOffsetX(context);
            float offsetY = FerrumConfig.getBannerOffsetY(context);
            float base = Math.max(rect.width() / bannerBitmap.getWidth(), rect.height() / bannerBitmap.getHeight());
            float s = base * scale;
            float drawW = bannerBitmap.getWidth() * s;
            float drawH = bannerBitmap.getHeight() * s;
            float freeX = Math.max(0, drawW - rect.width());
            float freeY = Math.max(0, drawH - rect.height());
            float left = rect.left - freeX * (offsetX + 1f) / 2f;
            float top = rect.top - freeY * (offsetY + 1f) / 2f;
            canvas.drawBitmap(bannerBitmap, left, top, paint);
        } else {
            int accent = FerrumConfig.getAccentColor(context);
            paint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                    accent, lighten(accent, 0.25f), Shader.TileMode.CLAMP));
            canvas.drawRect(rect, paint);
            paint.setShader(null);
        }
        canvas.restore();

        float avatarRadius = AndroidUtilities.dp(26);
        float cx = margin + avatarRadius + AndroidUtilities.dp(6);
        float cy = bannerTop + bannerHeight - AndroidUtilities.dp(4);

        if (FerrumConfig.isAvatarRingEnabled(context)) {
            float ringWidth = AndroidUtilities.dp(FerrumConfig.getAvatarRingWidth(context));
            int ringColor = FerrumConfig.getAvatarRingColor(context);
            int style = FerrumConfig.getAvatarRingStyle(context);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(ringWidth);
            paint.setColor(ringColor);
            if (style == FerrumConfig.AVATAR_RING_STYLE_GRADIENT) {
                paint.setShader(new LinearGradient(cx - avatarRadius, cy - avatarRadius,
                        cx + avatarRadius, cy + avatarRadius, ringColor, lighten(ringColor, 0.35f), Shader.TileMode.CLAMP));
                paint.setPathEffect(null);
            } else if (style == FerrumConfig.AVATAR_RING_STYLE_DASHED) {
                paint.setShader(null);
                paint.setPathEffect(new DashPathEffect(new float[]{AndroidUtilities.dp(5), AndroidUtilities.dp(4)}, 0));
            } else {
                paint.setShader(null);
                paint.setPathEffect(null);
            }
            canvas.drawCircle(cx, cy, avatarRadius + ringWidth / 2f, paint);
            paint.setShader(null);
            paint.setPathEffect(null);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        canvas.drawCircle(cx, cy, avatarRadius, paint);
        paint.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        canvas.drawCircle(cx, cy, avatarRadius - AndroidUtilities.dp(3), paint);

        float barLeft = cx + avatarRadius + AndroidUtilities.dp(10);
        float barRight = width - margin - AndroidUtilities.dp(24);
        if (barRight > barLeft) {
            rect.set(barLeft, cy - AndroidUtilities.dp(14), barRight, cy - AndroidUtilities.dp(4));
            canvas.drawRoundRect(rect, AndroidUtilities.dp(5), AndroidUtilities.dp(5), paint);
            rect.set(barLeft, cy + AndroidUtilities.dp(2), barLeft + (barRight - barLeft) * 0.6f, cy + AndroidUtilities.dp(8));
            canvas.drawRoundRect(rect, AndroidUtilities.dp(3), AndroidUtilities.dp(3), paint);
        }
    }

    private static int lighten(int color, float amount) {
        int r = Math.min(255, (int) (Color.red(color) + 255 * amount));
        int g = Math.min(255, (int) (Color.green(color) + 255 * amount));
        int b = Math.min(255, (int) (Color.blue(color) + 255 * amount));
        return Color.argb(Color.alpha(color), r, g, b);
    }
}
