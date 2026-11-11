package org.telegram.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FerrumConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

/**
 * Ferrum's dedicated settings screen. Preferences are stored locally and remain
 * independent from Telegram authentication/session data.
 */
public class FerrumSettingsActivity extends BaseFragment {

    private static final int REQUEST_BANNER = 7101;
    private LinearLayout content;
    private TextView bannerStatus;
    private TextView ringWidthLabel;

    public FerrumSettingsActivity() {
        super();
    }

    @Override
    public View createView(Context context) {
        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        simpleActionBar.setTitle("Ferrum");
        simpleActionBar.setActionBarMenuOnItemClick(new org.telegram.ui.ActionBar.ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(28));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        addIntro(context);
        addAccentSection(context);
        addBannerSection(context);
        addAvatarSection(context);
        addSecuritySection(context);
        addSyncSection(context);

        fragmentView = scroll;
        return fragmentView;
    }

    private void addIntro(Context context) {
        TextView intro = new TextView(context);
        intro.setText("Персонализация и дополнительные возможности Ferrum. Настройки оформления хранятся на этом устройстве.");
        intro.setTextSize(14);
        intro.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        intro.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(14));
        content.addView(intro, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addAccentSection(Context context) {
        addHeader(context, "ОФОРМЛЕНИЕ");
        addCard(context, "Акцентный цвет", "Выбери основной оттенок Ferrum.");
        int[] colors = new int[] {
            0xFF6E9BFF, 0xFF56C6A9, 0xFFB58AFF, 0xFFFF9B70, 0xFFE879A8, 0xFFE5B85C
        };
        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (int color : colors) {
            Button chip = new Button(context);
            chip.setText("●");
            chip.setTextSize(18);
            chip.setTextColor(color);
            chip.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Theme.getColor(Theme.key_windowBackgroundWhite)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, AndroidUtilities.dp(48), 1f);
            lp.setMargins(AndroidUtilities.dp(2), 0, AndroidUtilities.dp(2), 0);
            row.addView(chip, lp);
            chip.setOnClickListener(v -> {
                FerrumConfig.setAccentColor(context, color);
                Toast.makeText(context, "Акцентный цвет сохранён", Toast.LENGTH_SHORT).show();
            });
        }
        content.addView(row, new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(52)));
    }

    private void addBannerSection(Context context) {
        addHeader(context, "БАННЕР ПРОФИЛЯ");
        addCard(context, "Локальный баннер", "Изображение выбирается из галереи и пока отображается только как сохранённая настройка Ferrum.");
        bannerStatus = addValue(context, bannerName());
        addButton(context, "Выбрать изображение", v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQUEST_BANNER);
        });
        addButton(context, "Удалить баннер", v -> {
            FerrumConfig.clearProfileBanner(context);
            bannerStatus.setText("Баннер не выбран");
        });
        addSeekBar(context, "Масштаб баннера", 50, 300, (int) (FerrumConfig.getBannerScale(context) * 100), value ->
            FerrumConfig.setBannerScale(context, value / 100f));
        addSeekBar(context, "Положение по горизонтали", -100, 100, (int) (FerrumConfig.getBannerOffsetX(context) * 100), value ->
            FerrumConfig.setBannerOffsetX(context, value / 100f));
        addSeekBar(context, "Положение по вертикали", -100, 100, (int) (FerrumConfig.getBannerOffsetY(context) * 100), value ->
            FerrumConfig.setBannerOffsetY(context, value / 100f));
    }

    private void addAvatarSection(Context context) {
        addHeader(context, "АВАТАР");
        addCard(context, "Обводка аватара", "Параметры сохраняются локально. Подключение отрисовки обводки в профиле — следующий этап интеграции.");
        android.widget.Switch enabled = new android.widget.Switch(context);
        enabled.setText("Включить обводку");
        enabled.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        enabled.setChecked(FerrumConfig.isAvatarRingEnabled(context));
        enabled.setOnCheckedChangeListener((button, checked) -> FerrumConfig.setAvatarRingEnabled(context, checked));
        content.addView(enabled, new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(52)));
        int[] colors = new int[] { 0xFF6E9BFF, 0xFF56C6A9, 0xFFB58AFF, 0xFFFF9B70, Color.WHITE };
        addSeekBar(context, "Толщина обводки", 10, 120, (int) (FerrumConfig.getAvatarRingWidth(context) * 10), value ->
            FerrumConfig.setAvatarRingWidth(context, value / 10f));
        LinearLayout row = new LinearLayout(context);
        for (int color : colors) {
            Button chip = new Button(context);
            chip.setText("●");
            chip.setTextColor(color);
            chip.setTextSize(18);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, AndroidUtilities.dp(48), 1f);
            row.addView(chip, lp);
            chip.setOnClickListener(v -> FerrumConfig.setAvatarRingColor(context, color));
        }
        content.addView(row, new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(50)));
        String[] styles = new String[] { "Сплошная", "Градиент", "Пунктир" };
        for (int i = 0; i < styles.length; i++) {
            final int style = i;
            addButton(context, styles[i], v -> FerrumConfig.setAvatarRingStyle(context, style));
        }
    }

    private void addSecuritySection(Context context) {
        addHeader(context, "БЕЗОПАСНОСТЬ");
        addCard(context, "Проверка файлов", "Проверка на базе VirusTotal или локальных сигнатур ещё не подключена. Этот экран не объявляет файлы безопасными и не отправляет их третьим сторонам.");
        android.widget.Switch enabled = new android.widget.Switch(context);
        enabled.setText("Показывать пункт проверки файлов");
        enabled.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        enabled.setChecked(FerrumConfig.isAntivirusEnabled(context));
        enabled.setOnCheckedChangeListener((button, checked) -> FerrumConfig.setAntivirusEnabled(context, checked));
        content.addView(enabled, new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(52)));
    }

    private void addSyncSection(Context context) {
        addHeader(context, "СИНХРОНИЗАЦИЯ");
        addCard(context, "Настройки между устройствами", "Синхронизация пока не подключена. Локальные параметры не загружаются в облако Telegram и не передаются на сервер.");
        TextView status = addValue(context, "Статус: только на этом устройстве");
    }

    private void addHeader(Context context, String title) {
        TextView view = new TextView(context);
        view.setText(title);
        view.setTextSize(11);
        view.setLetterSpacing(0.08f);
        view.setTypeface(AndroidUtilities.bold());
        view.setTextColor(FerrumConfig.getAccentColor(context));
        view.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(18), AndroidUtilities.dp(4), AndroidUtilities.dp(8));
        content.addView(view, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addCard(Context context, String title, String description) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundWhite)));
        TextView heading = new TextView(context);
        heading.setText(title);
        heading.setTextSize(15);
        heading.setTypeface(AndroidUtilities.bold());
        heading.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        card.addView(heading);
        TextView body = new TextView(context);
        body.setText(description);
        body.setTextSize(13);
        body.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(-1, -2);
        bodyLp.topMargin = AndroidUtilities.dp(5);
        card.addView(body, bodyLp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = AndroidUtilities.dp(10);
        content.addView(card, lp);
    }

    private TextView addValue(Context context, String value) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(13);
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        view.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(5), AndroidUtilities.dp(4), AndroidUtilities.dp(8));
        content.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }

    private void addButton(Context context, String title, View.OnClickListener listener) {
        Button button = new Button(context);
        button.setText(title);
        button.setAllCaps(false);
        button.setTextColor(FerrumConfig.getAccentColor(context));
        button.setOnClickListener(listener);
        content.addView(button, new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(48)));
    }

    private interface ValueListener { void onChanged(int value); }

    private void addSeekBar(Context context, String title, int min, int max, int value, ValueListener listener) {
        TextView label = new TextView(context);
        label.setText(title);
        label.setTextSize(13);
        label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        label.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(10), 0, 0);
        content.addView(label, new LinearLayout.LayoutParams(-1, -2));
        SeekBar seek = new SeekBar(context);
        seek.setMax(max - min);
        seek.setProgress(value - min);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) listener.onChanged(progress + min);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        content.addView(seek, new LinearLayout.LayoutParams(-1, AndroidUtilities.dp(42)));
    }

    private String bannerName() {
        String uri = FerrumConfig.getProfileBannerUri(getContext());
        return uri == null ? "Баннер не выбран" : "Выбрано изображение: " + Uri.parse(uri).getLastPathSegment();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_BANNER && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getParentActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                FerrumConfig.setProfileBannerUri(getParentActivity(), uri.toString());
                if (bannerStatus != null) bannerStatus.setText(bannerName());
            } catch (Exception e) {
                Toast.makeText(getParentActivity(), "Не удалось сохранить доступ к изображению", Toast.LENGTH_LONG).show();
            }
        }
    }
}
