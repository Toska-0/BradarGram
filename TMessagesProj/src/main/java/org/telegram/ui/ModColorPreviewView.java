package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.ModTheme;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

/** Небольшое превью выбранного цветового пакета: мини-чат, кнопка, переключатель и счётчик. */
public class ModColorPreviewView extends View {

    private int pack;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF card = new RectF();
    private final RectF tmp = new RectF();

    public ModColorPreviewView(Context context) {
        super(context);
    }

    private static int dp(float v) {
        return AndroidUtilities.dp(v);
    }

    public void setPack(int pack) {
        this.pack = pack;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), dp(220));
    }

    private int[] colors(boolean dark) {
        if (pack > 0) {
            int[] c = ModTheme.colors(pack, dark);
            if (c != null) {
                return c;
            }
        }
        // стандартные цвета текущей темы
        return new int[]{
                Theme.getColor(Theme.key_chats_actionBackground),
                Theme.getColor(Theme.key_chat_outBubble),
                Theme.getColor(Theme.key_chat_outBubbleSelected),
                Theme.getColor(Theme.key_chat_messageTextOut),
                Theme.getColor(Theme.key_chat_outTimeText),
                Theme.getColor(Theme.key_windowBackgroundWhite),
                Theme.getColor(Theme.key_windowBackgroundGray),
                Theme.getColor(Theme.key_chats_actionIcon)
        };
    }

    private float drawBubbleText(Canvas canvas, String text, float maxWidth, float x, float baseline, int color, float size) {
        textPaint.setTextSize(dp(size));
        textPaint.setColor(color);
        CharSequence shown = TextUtils.ellipsize(text, textPaint, maxWidth, TextUtils.TruncateAt.END);
        canvas.drawText(shown, 0, shown.length(), x, baseline, textPaint);
        return textPaint.measureText(shown, 0, shown.length());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final boolean dark = Theme.isCurrentThemeDark();
        final int[] c = colors(dark);
        final int textColor = dark ? 0xFFE6EAEE : 0xFF1C2024;
        final int w = getWidth();
        final int h = getHeight();
        final float pad = dp(16);
        final float r = dp(14);

        card.set(pad, dp(8), w - pad, h - dp(8));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(c[ModTheme.SURFACE_DIM]);
        canvas.drawRoundRect(card, r, r, paint);

        // верхняя панель
        final float hb = dp(34);
        tmp.set(card.left, card.top, card.right, card.top + hb);
        paint.setColor(c[ModTheme.SURFACE]);
        canvas.drawRoundRect(tmp, r, r, paint);
        canvas.drawRect(card.left, card.top + r, card.right, card.top + hb, paint);
        paint.setColor(c[ModTheme.PRIMARY]);
        canvas.drawCircle(card.left + dp(20), card.top + hb / 2f, dp(9), paint);
        paint.setColor(textColor);
        tmp.set(card.left + dp(36), card.top + hb / 2f - dp(3.5f), card.left + dp(36) + dp(70), card.top + hb / 2f + dp(3.5f));
        canvas.drawRoundRect(tmp, dp(3.5f), dp(3.5f), paint);

        // входящее сообщение
        final float maxBubble = card.width() * 0.8f;
        String hello = LocaleController.getString(R.string.ModPreviewHello);
        textPaint.setTextSize(dp(13));
        float helloW = Math.min(textPaint.measureText(hello), maxBubble - dp(24));
        float inTop = card.top + hb + dp(14);
        tmp.set(card.left + dp(12), inTop, card.left + dp(12) + helloW + dp(24), inTop + dp(34));
        paint.setColor(c[ModTheme.SURFACE]);
        canvas.drawRoundRect(tmp, dp(14), dp(14), paint);
        drawBubbleText(canvas, hello, helloW, tmp.left + dp(12), tmp.top + dp(21), textColor, 13);

        // исходящее сообщение
        String reply = LocaleController.getString(R.string.ModPreviewReply);
        textPaint.setTextSize(dp(13));
        float replyW = Math.min(Math.max(textPaint.measureText(reply), dp(46)), maxBubble - dp(24));
        float outTop = inTop + dp(34) + dp(10);
        tmp.set(card.right - dp(12) - replyW - dp(24), outTop, card.right - dp(12), outTop + dp(44));
        paint.setColor(c[ModTheme.CONTAINER]);
        canvas.drawRoundRect(tmp, dp(14), dp(14), paint);
        drawBubbleText(canvas, reply, replyW, tmp.left + dp(12), tmp.top + dp(19), c[ModTheme.ON_CONTAINER], 13);
        textPaint.setTextSize(dp(10));
        textPaint.setColor(c[ModTheme.ON_CONTAINER_VAR]);
        canvas.drawText("12:41", tmp.right - dp(12) - textPaint.measureText("12:41"), tmp.top + dp(35), textPaint);

        // кнопка-«плюс»
        final float fabX = card.right - dp(34);
        final float fabY = card.bottom - dp(32);
        paint.setColor(c[ModTheme.PRIMARY]);
        canvas.drawCircle(fabX, fabY, dp(20), paint);
        paint.setColor(c[ModTheme.ON_PRIMARY]);
        canvas.drawRoundRect(fabX - dp(8), fabY - dp(1.25f), fabX + dp(8), fabY + dp(1.25f), dp(1.25f), dp(1.25f), paint);
        canvas.drawRoundRect(fabX - dp(1.25f), fabY - dp(8), fabX + dp(1.25f), fabY + dp(8), dp(1.25f), dp(1.25f), paint);

        // переключатель
        final float swLeft = card.left + dp(14);
        final float swTop = card.bottom - dp(43);
        tmp.set(swLeft, swTop, swLeft + dp(40), swTop + dp(22));
        paint.setColor(c[ModTheme.PRIMARY]);
        canvas.drawRoundRect(tmp, dp(11), dp(11), paint);
        paint.setColor(c[ModTheme.ON_PRIMARY]);
        canvas.drawCircle(swLeft + dp(40) - dp(11), swTop + dp(11), dp(8), paint);

        // счётчик непрочитанных
        final float chipLeft = swLeft + dp(40) + dp(18);
        tmp.set(chipLeft, swTop + dp(1), chipLeft + dp(28), swTop + dp(21));
        paint.setColor(c[ModTheme.PRIMARY]);
        canvas.drawRoundRect(tmp, dp(10), dp(10), paint);
        textPaint.setTextSize(dp(11));
        textPaint.setFakeBoldText(true);
        textPaint.setColor(c[ModTheme.ON_PRIMARY]);
        canvas.drawText("3", tmp.centerX() - textPaint.measureText("3") / 2f, tmp.centerY() + dp(4), textPaint);
        textPaint.setFakeBoldText(false);
    }
}
