package org.telegram.messenger;

import android.util.SparseIntArray;

import org.telegram.ui.ActionBar.Theme;

/**
 * Цветовые пакеты интерфейса. Спокойные пастельные палитры, у каждой своя светлая и тёмная версия.
 * Пакет подменяет ключевые цвета темы в {@link Theme#getColor(int, boolean[], boolean)}.
 */
public class ModTheme {

    // роли цветов в массиве пакета
    public static final int PRIMARY = 0;
    public static final int CONTAINER = 1;
    public static final int CONTAINER_HI = 2;
    public static final int ON_CONTAINER = 3;
    public static final int ON_CONTAINER_VAR = 4;
    public static final int SURFACE = 5;
    public static final int SURFACE_DIM = 6;
    public static final int ON_PRIMARY = 7;

    /** Названия пакетов (индекс 0 - стандартные цвета). */
    public static final int[] PACK_NAMES = {
            R.string.ModPackDefault,
            R.string.ModPackSky,
            R.string.ModPackPink,
            R.string.ModPackMint,
            R.string.ModPackLavender,
            R.string.ModPackPeach,
            R.string.ModPackGraphite
    };

    // P, C, CH, OC, OCV, S, SD, OP
    private static final int[][] LIGHT = {
            null,
            {0xFF3A86C8, 0xFFD9EAF8, 0xFFC6DFF3, 0xFF0E2D4A, 0xFF2C5F8C, 0xFFFFFFFF, 0xFFEDF3F9, 0xFFFFFFFF}, // голубой
            {0xFFC95F86, 0xFFF9DDE7, 0xFFF3CCDA, 0xFF4A1328, 0xFF8C3957, 0xFFFFFFFF, 0xFFFAEFF3, 0xFFFFFFFF}, // розовый
            {0xFF35967F, 0xFFD6F0E7, 0xFFC3E6DA, 0xFF0C3A2D, 0xFF2F6E5C, 0xFFFFFFFF, 0xFFEDF7F3, 0xFFFFFFFF}, // мятный
            {0xFF7F63C4, 0xFFE6DEF7, 0xFFD9CEF1, 0xFF2A1B52, 0xFF5B459A, 0xFFFFFFFF, 0xFFF3F0FA, 0xFFFFFFFF}, // сиреневый
            {0xFFD9804F, 0xFFFBE2D4, 0xFFF6D3C0, 0xFF47220F, 0xFF8A4B26, 0xFFFFFFFF, 0xFFFBF2EC, 0xFFFFFFFF}, // персиковый
            {0xFF566E7F, 0xFFDDE5EB, 0xFFCDD8E0, 0xFF1A2935, 0xFF45596A, 0xFFFFFFFF, 0xFFF0F3F5, 0xFFFFFFFF}  // графит
    };
    private static final int[][] DARK = {
            null,
            {0xFF8CC0EE, 0xFF1F4466, 0xFF29547B, 0xFFDCEBFA, 0xFFA9CCEA, 0xFF17212B, 0xFF0F1720, 0xFF0B2740},
            {0xFFEFA5BE, 0xFF5C2A40, 0xFF70374F, 0xFFFBE2EB, 0xFFE8B4C6, 0xFF231820, 0xFF1A1017, 0xFF4A1328},
            {0xFF86D0B8, 0xFF1D4A3E, 0xFF285C4E, 0xFFD9F4EA, 0xFFA4D8C7, 0xFF151F1C, 0xFF0E1614, 0xFF0B3328},
            {0xFFBBA8F0, 0xFF45346F, 0xFF56438A, 0xFFEBE4FC, 0xFFCBBEF1, 0xFF1D1829, 0xFF15111E, 0xFF2A1B52},
            {0xFFF1B08A, 0xFF5B3421, 0xFF70432C, 0xFFFCE8DB, 0xFFEEC3A9, 0xFF241A14, 0xFF1A120D, 0xFF47220F},
            {0xFFA3B8C8, 0xFF2E3D49, 0xFF3B4D5B, 0xFFE0E9EF, 0xFFB6C6D2, 0xFF1B2227, 0xFF12171A, 0xFF1A2935}
    };

    private static final SparseIntArray[] lightMaps = new SparseIntArray[LIGHT.length];
    private static final SparseIntArray[] darkMaps = new SparseIntArray[DARK.length];

    public static synchronized void invalidate() {
        for (int i = 0; i < lightMaps.length; i++) {
            lightMaps[i] = null;
            darkMaps[i] = null;
        }
    }

    /** Копия цветов пакета (см. роли выше) или null для стандартных цветов. */
    public static int[] colors(int pack, boolean isDark) {
        int[][] src = isDark ? DARK : LIGHT;
        if (pack <= 0 || pack >= src.length || src[pack] == null) {
            return null;
        }
        return src[pack].clone();
    }

    /** @return цвет или 0, если ключ не переопределяется. */
    public static int getColor(int key, boolean isDark) {
        int pack = ModConfig.getColorPack();
        if (pack <= 0 || pack >= LIGHT.length) {
            return 0;
        }
        SparseIntArray map = isDark ? darkMaps[pack] : lightMaps[pack];
        if (map == null) {
            map = build(pack, isDark);
        }
        return map.get(key, 0);
    }

    private static synchronized SparseIntArray build(int pack, boolean isDark) {
        int[] c = (isDark ? DARK : LIGHT)[pack];
        SparseIntArray map = new SparseIntArray();

        map.put(Theme.key_chats_actionBackground, c[PRIMARY]);
        map.put(Theme.key_chats_actionPressedBackground, c[ON_CONTAINER_VAR]);
        map.put(Theme.key_chats_actionIcon, c[ON_PRIMARY]);

        map.put(Theme.key_windowBackgroundWhiteBlueText, c[PRIMARY]);
        map.put(Theme.key_windowBackgroundWhiteBlueHeader, c[PRIMARY]);
        map.put(Theme.key_windowBackgroundWhiteLinkText, c[PRIMARY]);
        map.put(Theme.key_dialogTextBlue, c[PRIMARY]);
        map.put(Theme.key_dialogButton, c[PRIMARY]);
        map.put(Theme.key_switchTrackChecked, c[PRIMARY]);
        map.put(Theme.key_switch2TrackChecked, c[PRIMARY]);
        map.put(Theme.key_chats_unreadCounter, c[PRIMARY]);
        map.put(Theme.key_chats_unreadCounterText, c[ON_PRIMARY]);

        map.put(Theme.key_chat_outBubble, c[CONTAINER]);
        map.put(Theme.key_chat_outBubbleGradient1, c[CONTAINER]);
        map.put(Theme.key_chat_outBubbleGradient2, c[CONTAINER]);
        map.put(Theme.key_chat_outBubbleGradient3, c[CONTAINER]);
        map.put(Theme.key_chat_outBubbleSelected, c[CONTAINER_HI]);
        map.put(Theme.key_chat_messageTextOut, c[ON_CONTAINER]);
        map.put(Theme.key_chat_outTimeText, c[ON_CONTAINER_VAR]);
        map.put(Theme.key_chat_messageLinkOut, c[ON_CONTAINER_VAR]);

        map.put(Theme.key_windowBackgroundWhite, c[SURFACE]);
        map.put(Theme.key_windowBackgroundGray, c[SURFACE_DIM]);

        // нижняя панель навигации (glass-табы ссылаются на эти ключи), кнопка отправки
        map.put(Theme.key_chat_messagePanelSend, c[PRIMARY]);

        // нижние стеклянные вкладки главного экрана (GlassTabView)
        map.put(Theme.key_glass_tabSelected, c[PRIMARY]);
        map.put(Theme.key_glass_tabSelectedText, c[ON_CONTAINER_VAR]);
        map.put(Theme.key_glass_tabUnselected, c[ON_CONTAINER_VAR]);

        // фирменный синий Telegram: счётчики на табах и прочие брендовые акценты
        map.put(Theme.key_telegram_color, c[PRIMARY]);
        map.put(Theme.key_telegram_color_text, c[ON_CONTAINER_VAR]);
        map.put(Theme.key_telegram_color_dialogsLogo, c[PRIMARY]);

        // верхняя стеклянная панель: подложка, иконки и подписи на стекле
        map.put(Theme.key_glass_targetMainTopPanel, c[CONTAINER]);
        map.put(Theme.key_glass_targetMainTabs, c[CONTAINER]);
        map.put(Theme.key_glass_defaultIcon, c[ON_CONTAINER_VAR]);
        map.put(Theme.key_glass_defaultText, c[ON_CONTAINER_VAR]);

        // синие подписи в списке чатов и бейдж выбранной папки
        map.put(Theme.key_chats_message, c[PRIMARY]);
        map.put(Theme.key_chats_nameMessage, c[PRIMARY]);
        map.put(Theme.key_chats_nameMessage_threeLines, c[PRIMARY]);
        map.put(Theme.key_chats_attachMessage, c[PRIMARY]);
        map.put(Theme.key_chats_draft, c[ON_CONTAINER_VAR]);
        map.put(Theme.key_chats_tabUnreadActiveBackground, c[PRIMARY]);

        // акцентные тексты и значения в настройках/диалогах
        map.put(Theme.key_windowBackgroundWhiteValueText, c[PRIMARY]);
        map.put(Theme.key_windowBackgroundWhiteBlueText2, c[PRIMARY]);
        map.put(Theme.key_windowBackgroundWhiteBlueText3, c[PRIMARY]);
        map.put(Theme.key_windowBackgroundWhiteBlueText4, c[PRIMARY]);
        map.put(Theme.key_windowBackgroundWhiteBlueText5, c[PRIMARY]);
        map.put(Theme.key_windowBackgroundWhiteBlueText6, c[PRIMARY]);
        map.put(Theme.key_windowBackgroundWhiteBlueText7, c[PRIMARY]);
        map.put(Theme.key_dialogTextBlue2, c[PRIMARY]);
        map.put(Theme.key_dialogTextBlue4, c[PRIMARY]);

        // остатки синего: ссылки во входящих, кнопки диалогов, поиск, галочки, онлайн-индикатор
        map.put(Theme.key_chat_messageLinkIn, c[PRIMARY]);
        map.put(Theme.key_chat_inReplyLine, c[PRIMARY]);
        map.put(Theme.key_radioBackgroundChecked, c[PRIMARY]);
        map.put(Theme.key_checkboxCheck, c[ON_PRIMARY]);
        map.put(Theme.key_windowBackgroundWhiteBlueButton, c[PRIMARY]);
        map.put(Theme.key_dialogSearchText, c[PRIMARY]);
        map.put(Theme.key_chat_fieldOverlayText, c[PRIMARY]);
        map.put(Theme.key_chats_onlineCircle, c[PRIMARY]);

        // кнопки-«таблетки», прогресс, галочки проверок
        map.put(Theme.key_featuredStickers_addButton, c[PRIMARY]);
        map.put(Theme.key_dialog_inlineProgress, c[PRIMARY]);
        map.put(Theme.key_chats_verifiedBackground, c[PRIMARY]);
        map.put(Theme.key_profile_verifiedBackground, c[PRIMARY]);

        // вторичные оттенки: линия ответа у исходящих, приглушённый счётчик
        map.put(Theme.key_chat_outReplyLine, c[ON_CONTAINER_VAR]);
        map.put(Theme.key_chats_unreadCounterMuted, c[ON_CONTAINER_VAR]);

        if (isDark) {
            darkMaps[pack] = map;
        } else {
            lightMaps[pack] = map;
        }
        return map;
    }

    /** Применяет цвета сразу: Telegram перекрашивает весь интерфейс тем же уведомлением, что и смена темы. */
    public static void applyLive() {
        invalidate();
        AndroidUtilities.runOnUIThread(() -> {
            try {
                NotificationCenter.getGlobalInstance().postNotificationName(
                        NotificationCenter.needSetDayNightTheme, Theme.getActiveTheme(), false, null, -1);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        });
    }
}
