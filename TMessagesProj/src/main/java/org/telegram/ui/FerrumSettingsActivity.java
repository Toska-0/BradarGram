package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.ModConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

/**
 * Ferrum main settings page: animated header card and a list of categories.
 * Each category opens a separate page (FerrumCategoryActivity).
 */
public class FerrumSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter adapter;

    private static class Item {
        static final int TYPE_HEADER = 0;
        static final int TYPE_CATEGORY = 1;
        static final int TYPE_VALUE = 2;
        static final int TYPE_INFO = 3;
        int type, category, icon;
        String title, value;
    }

    @Override
    protected boolean useSimpleActionBar() { return true; }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        simpleActionBar.setTitle(LocaleController.getString(R.string.FerrumSettingsEntry));
        simpleActionBar.setActionBarMenuOnItemClick(new org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });

        FrameLayout frame = new FrameLayout(context);
        fragmentView = frame;
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        ArrayList<Item> items = buildItems(context);

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter(context, items));
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) return;
            Item item = items.get(position);
            if (item.type == Item.TYPE_CATEGORY) {
                presentFragment(new FerrumCategoryActivity(item.category));
            }
        });
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private ArrayList<Item> buildItems(Context context) {
        ArrayList<Item> items = new ArrayList<>();
        items.add(headerItem());
        items.add(categoryItem(FerrumCategoryActivity.CAT_APPEARANCE, LocaleController.getString(R.string.FerrumSectionAppearance), R.drawable.msg_customize));
        items.add(categoryItem(FerrumCategoryActivity.CAT_PROFILE, LocaleController.getString(R.string.FerrumCategoryProfile), R.drawable.msg_openprofile));
        items.add(categoryItem(FerrumCategoryActivity.CAT_PRIVACY, LocaleController.getString(R.string.FerrumSectionPrivacy), R.drawable.msg_secret));
        items.add(categoryItem(FerrumCategoryActivity.CAT_TELEGRAM, LocaleController.getString(R.string.FerrumSectionTelegram), R.drawable.msg_gift_premium));
        items.add(categoryItem(FerrumCategoryActivity.CAT_SECURITY, LocaleController.getString(R.string.FerrumSectionSecurity), R.drawable.msg_permissions));
        items.add(categoryItem(FerrumCategoryActivity.CAT_DATA, LocaleController.getString(R.string.FerrumSectionSync), R.drawable.msg_saved));
        Item version = new Item();
        version.type = Item.TYPE_VALUE;
        version.title = LocaleController.getString(R.string.FerrumVersion);
        version.value = ModConfig.MOD_VERSION;
        items.add(version);
        Item info = new Item();
        info.type = Item.TYPE_INFO;
        info.title = LocaleController.getString(R.string.FerrumSettingsEntryInfo);
        items.add(info);
        return items;
    }

    private Item headerItem() {
        Item item = new Item();
        item.type = Item.TYPE_HEADER;
        return item;
    }

    private Item categoryItem(int category, String title, int icon) {
        Item item = new Item();
        item.type = Item.TYPE_CATEGORY;
        item.category = category;
        item.title = title;
        item.icon = icon;
        return item;
    }

    private static class FerrumHeaderView extends View {
        private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint logoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint logoTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint subPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix shaderMatrix = new Matrix();

        FerrumHeaderView(Context context) {
            super(context);
            titlePaint.setTypeface(AndroidUtilities.bold());
            titlePaint.setTextSize(AndroidUtilities.dpf2(34));
            logoTextPaint.setTypeface(AndroidUtilities.bold());
            logoTextPaint.setTextSize(AndroidUtilities.dpf2(20));
            logoTextPaint.setColor(Color.WHITE);
            subPaint.setTextSize(AndroidUtilities.dpf2(13));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(170), MeasureSpec.EXACTLY));
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
            final boolean dark = Theme.isCurrentThemeDark();
            final int accent = FerrumConfig.getAccentColor(getContext());
            final int base = Theme.getColor(Theme.key_windowBackgroundWhite);

            final float left = AndroidUtilities.dp(14);
            final float top = AndroidUtilities.dp(10);
            final float right = getMeasuredWidth() - AndroidUtilities.dp(14);
            final float bottom = getMeasuredHeight() - AndroidUtilities.dp(6);
            final float radius = AndroidUtilities.dp(24);

            final int gradTop = ColorUtils.blendARGB(accent, base, dark ? 0.80f : 0.78f);
            final int gradBottom = ColorUtils.blendARGB(accent, base, dark ? 0.62f : 0.60f);
            cardPaint.setShader(new LinearGradient(0, top, 0, bottom, gradTop, gradBottom, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(left, top, right, bottom, radius, radius, cardPaint);
            cardPaint.setShader(null);

            // logo: rounded square with the first letter
            final float logoSize = AndroidUtilities.dp(44);
            final float logoX = left + AndroidUtilities.dp(18);
            final float logoY = top + AndroidUtilities.dp(18);
            logoPaint.setShader(new LinearGradient(logoX, logoY, logoX + logoSize, logoY + logoSize,
                    ColorUtils.blendARGB(accent, Color.WHITE, 0.25f), accent, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(logoX, logoY, logoX + logoSize, logoY + logoSize, AndroidUtilities.dp(12), AndroidUtilities.dp(12), logoPaint);
            logoPaint.setShader(null);
            final String letter = "F";
            final float lw = logoTextPaint.measureText(letter);
            final Paint.FontMetrics lm = logoTextPaint.getFontMetrics();
            canvas.drawText(letter, logoX + (logoSize - lw) / 2f, logoY + (logoSize - (lm.ascent + lm.descent)) / 2f, logoTextPaint);

            // title with a cyclic shimmer sweeping through the accent color
            final String title = LocaleController.getString(R.string.FerrumSettingsEntry);
            final float titleX = logoX + logoSize + AndroidUtilities.dp(14);
            final float titleBaseline = logoY + logoSize / 2f + AndroidUtilities.dp(12);
            final int highlight = ColorUtils.blendARGB(accent, Color.WHITE, dark ? 0.55f : 0.45f);
            LinearGradient shimmer = new LinearGradient(0, 0, AndroidUtilities.dp(120), 0,
                    new int[]{accent, highlight, accent},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.REPEAT);
            final float period = AndroidUtilities.dp(240);
            final float phase = (System.currentTimeMillis() % 2600L) / 2600f;
            shaderMatrix.setTranslate(-period + period * phase, 0);
            shimmer.setLocalMatrix(shaderMatrix);
            titlePaint.setShader(shimmer);
            canvas.drawText(title, titleX, titleBaseline, titlePaint);
            titlePaint.setShader(null);

            // version line
            subPaint.setColor(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 0.55f));
            String version = String.format(LocaleController.getString(R.string.FerrumVersionFormat), ModConfig.MOD_VERSION);
            canvas.drawText(version, left + AndroidUtilities.dp(18), bottom - AndroidUtilities.dp(16), subPaint);

            if (isAttachedToWindow()) {
                postInvalidateOnAnimation();
            }
        }
    }

    private static class FerrumCategoryCell extends FrameLayout {
        private final ImageView iconView;
        private final TextView textView;

        FerrumCategoryCell(Context context) {
            super(context);
            iconView = new ImageView(context);
            iconView.setScaleType(ImageView.ScaleType.CENTER);
            addView(iconView, LayoutHelper.createFrame(24, 24, Gravity.CENTER_VERTICAL | (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT), 18, 0, 18, 0));
            textView = new TextView(context);
            textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setTypeface(AndroidUtilities.bold());
            textView.setGravity(Gravity.CENTER_VERTICAL);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.FILL_VERTICAL, 60, 0, 18, 0));
            setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 0));
        }

        void set(Item item, boolean divider) {
            textView.setText(item.title);
            iconView.setImageResource(item.icon);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            iconView.setColorFilter(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 0.75f), android.graphics.PorterDuff.Mode.SRC_IN);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(52), MeasureSpec.EXACTLY));
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;
        private final ArrayList<Item> items;

        ListAdapter(Context c, ArrayList<Item> items) {
            this.context = c;
            this.items = items;
        }

        @Override public int getItemCount() { return items.size(); }
        @Override public int getItemViewType(int p) { return items.get(p).type; }
        @Override public boolean isEnabled(RecyclerView.ViewHolder h) {
            int p = h.getAdapterPosition();
            return p >= 0 && p < items.size() && items.get(p).type == Item.TYPE_CATEGORY;
        }

        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View v;
            if (type == Item.TYPE_HEADER) v = new FerrumHeaderView(context);
            else if (type == Item.TYPE_CATEGORY) v = new FerrumCategoryCell(context);
            else if (type == Item.TYPE_VALUE) v = new TextSettingsCell(context);
            else v = new TextInfoPrivacyCell(context);
            v.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(v);
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int p) {
            Item item = items.get(p);
            if (item.type == Item.TYPE_CATEGORY) {
                ((FerrumCategoryCell) h.itemView).set(item, p + 1 < items.size() && items.get(p + 1).type == Item.TYPE_CATEGORY);
            } else if (item.type == Item.TYPE_VALUE) {
                ((TextSettingsCell) h.itemView).setTextAndValue(item.title, item.value, false);
            } else if (item.type == Item.TYPE_INFO) {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) h.itemView;
                cell.setText(item.title);
                cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
            }
        }
    }
}
