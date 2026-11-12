package org.telegram.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FerrumConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.io.InputStream;

/**
 * Полноэкранный редактор баннера профиля: масштаб щипком и перетаскивание жестом,
 * как в редакторе аватара. Показывает баннер ровно так, как он выглядит в профиле.
 */
public class FerrumBannerEditor extends BaseFragment {

    private static final int DONE_ITEM = 1;

    private BannerEditView editView;
    private float scale, offsetX, offsetY;
    private Bitmap bitmap;

    @Override
    protected boolean useSimpleActionBar() { return true; }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        scale = FerrumConfig.getBannerScale(context);
        offsetX = FerrumConfig.getBannerOffsetX(context);
        offsetY = FerrumConfig.getBannerOffsetY(context);

        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        simpleActionBar.setTitle(LocaleController.getString(R.string.FerrumBannerEditorTitle));
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == DONE_ITEM) {
                    saveAndClose();
                }
            }
        });
        simpleActionBar.createMenu().addItem(DONE_ITEM, R.drawable.ic_ab_done);

        FrameLayout frame = new FrameLayout(context);
        fragmentView = frame;
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        editView = new BannerEditView(context);
        frame.addView(editView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        TextView reset = new TextView(context);
        reset.setText(LocaleController.getString(R.string.Reset));
        reset.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        reset.setTextColor(Color.WHITE);
        reset.setShadowLayer(AndroidUtilities.dp(3), 0, AndroidUtilities.dp(1), 0x66000000);
        int pad = AndroidUtilities.dp(14);
        reset.setPadding(pad, pad, pad, pad);
        reset.setOnClickListener(v -> {
            scale = 1.0f;
            offsetX = 0.0f;
            offsetY = 0.0f;
            editView.invalidate();
        });
        frame.addView(reset, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT));

        TextView done = new TextView(context);
        done.setText(LocaleController.getString(R.string.Done));
        done.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        done.setTextColor(Color.WHITE);
        done.setShadowLayer(AndroidUtilities.dp(3), 0, AndroidUtilities.dp(1), 0x66000000);
        done.setPadding(pad, pad, pad, pad);
        done.setOnClickListener(v -> saveAndClose());
        frame.addView(done, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.RIGHT));

        loadBitmap(context);
        return fragmentView;
    }

    private void loadBitmap(Context context) {
        final String bannerUri = FerrumConfig.getProfileBannerUri(context);
        if (TextUtils.isEmpty(bannerUri)) {
            finishFragment();
            return;
        }
        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            Bitmap bmp = null;
            try {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                try (InputStream in = appContext.getContentResolver().openInputStream(Uri.parse(bannerUri))) {
                    BitmapFactory.decodeStream(in, null, bounds);
                }
                int sample = 1;
                while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) {
                    sample *= 2;
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = sample;
                try (InputStream in = appContext.getContentResolver().openInputStream(Uri.parse(bannerUri))) {
                    bmp = BitmapFactory.decodeStream(in, null, options);
                }
            } catch (Throwable t) {
                FileLog.e(t);
            }
            final Bitmap result = bmp;
            AndroidUtilities.runOnUIThread(() -> {
                if (getParentActivity() == null) {
                    return;
                }
                if (result == null) {
                    Toast.makeText(getParentActivity(), LocaleController.getString(R.string.FerrumBannerLoadError), Toast.LENGTH_SHORT).show();
                    finishFragment();
                    return;
                }
                bitmap = result;
                editView.invalidate();
            });
        }, "FerrumBannerEditor").start();
    }

    private void saveAndClose() {
        Context c = getParentActivity();
        if (c == null) {
            return;
        }
        FerrumConfig.setBannerScale(c, scale);
        FerrumConfig.setBannerOffsetX(c, offsetX);
        FerrumConfig.setBannerOffsetY(c, offsetY);
        finishFragment();
    }

    /** Рисует баннер той же математикой, что и TopView в профиле: center-crop, масштаб, сдвиг. */
    private class BannerEditView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Matrix matrix = new Matrix();
        private final ScaleGestureDetector scaleDetector;
        private float lastX, lastY;

        BannerEditView(Context context) {
            super(context);
            scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    scale = Math.max(0.5f, Math.min(3.0f, scale * detector.getScaleFactor()));
                    invalidate();
                    return true;
                }
            });
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            if (bitmap == null) {
                return;
            }
            paint.setAlpha(122);
            float vw = getWidth(), vh = getHeight();
            float bw = bitmap.getWidth(), bh = bitmap.getHeight();
            float base = Math.max(vw / bw, vh / bh);
            matrix.reset();
            matrix.postScale(base, base);
            matrix.postTranslate((vw - bw * base) / 2f, (vh - bh * base) / 2f);
            matrix.postScale(scale, scale, vw / 2f, vh / 2f);
            matrix.postTranslate(AndroidUtilities.dp(60) * offsetX, AndroidUtilities.dp(45) * offsetY);
            canvas.drawBitmap(bitmap, matrix, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = event.getX();
                    lastY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!scaleDetector.isInProgress()) {
                        offsetX = clamp(offsetX + (event.getX() - lastX) / AndroidUtilities.dp(60));
                        offsetY = clamp(offsetY + (event.getY() - lastY) / AndroidUtilities.dp(45));
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    performClick();
                    break;
            }
            lastX = event.getX();
            lastY = event.getY();
            invalidate();
            return true;
        }

        @Override
        public boolean performClick() {
            return super.performClick();
        }

        private float clamp(float value) {
            return Math.max(-3.0f, Math.min(3.0f, value));
        }
    }
}
