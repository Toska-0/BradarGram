package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/** Настройки мода. Хранятся в отдельном SharedPreferences-файле "modconfig". */
public class ModConfig {

    public static final String MOD_VERSION = "1.2";

    private static final String PREFS = "modconfig";

    public static final String KEY_NO_TYPING = "no_typing_status";
    public static final String KEY_CONFIRM_CALLS = "confirm_calls";
    public static final String KEY_HIDE_PREMIUM = "hide_premium";
    public static final String KEY_HIDE_STARS = "hide_stars";
    public static final String KEY_LOCAL_PREMIUM = "local_premium";
    public static final String KEY_SAVE_DELETED = "save_deleted";
    public static final String KEY_NO_READ = "no_read_receipts";
    public static final String KEY_NO_BLUR = "no_blur";
    private static final String KEY_COLOR_PACK = "color_pack";

    private static volatile boolean loaded;
    private static volatile boolean noTyping;
    private static volatile boolean confirmCalls;
    private static volatile boolean hidePremium;
    private static volatile boolean hideStars;
    private static volatile boolean localPremium;
    private static volatile boolean saveDeleted;
    private static volatile boolean noRead;
    private static volatile boolean noBlur;
    private static volatile int colorPack;

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static synchronized void load() {
        if (loaded) {
            return;
        }
        SharedPreferences p = prefs();
        noTyping = p.getBoolean(KEY_NO_TYPING, false);
        confirmCalls = p.getBoolean(KEY_CONFIRM_CALLS, false);
        hidePremium = p.getBoolean(KEY_HIDE_PREMIUM, false);
        hideStars = p.getBoolean(KEY_HIDE_STARS, false);
        localPremium = p.getBoolean(KEY_LOCAL_PREMIUM, false);
        saveDeleted = p.getBoolean(KEY_SAVE_DELETED, false);
        noRead = p.getBoolean(KEY_NO_READ, false);
        noBlur = p.getBoolean(KEY_NO_BLUR, false);
        colorPack = p.getInt(KEY_COLOR_PACK, 0);
        loaded = true;
    }

    public static boolean get(String key) {
        load();
        switch (key) {
            case KEY_NO_TYPING: return noTyping;
            case KEY_CONFIRM_CALLS: return confirmCalls;
            case KEY_HIDE_PREMIUM: return hidePremium;
            case KEY_HIDE_STARS: return hideStars;
            case KEY_LOCAL_PREMIUM: return localPremium;
            case KEY_SAVE_DELETED: return saveDeleted;
            case KEY_NO_READ: return noRead;
            case KEY_NO_BLUR: return noBlur;
        }
        return false;
    }

    public static boolean toggle(String key) {
        load();
        boolean value = !get(key);
        switch (key) {
            case KEY_NO_TYPING: noTyping = value; break;
            case KEY_CONFIRM_CALLS: confirmCalls = value; break;
            case KEY_HIDE_PREMIUM: hidePremium = value; break;
            case KEY_HIDE_STARS: hideStars = value; break;
            case KEY_LOCAL_PREMIUM: localPremium = value; break;
            case KEY_SAVE_DELETED: saveDeleted = value; break;
            case KEY_NO_READ: noRead = value; break;
            case KEY_NO_BLUR: noBlur = value; break;
            default: return false;
        }
        prefs().edit().putBoolean(key, value).apply();
        return value;
    }

    /** 0 - стандартные цвета, 1.. - цветовые пакеты из {@link ModTheme}. */
    public static int getColorPack() {
        load();
        return colorPack;
    }

    public static void setColorPack(int pack) {
        load();
        colorPack = pack;
        prefs().edit().putInt(KEY_COLOR_PACK, pack).apply();
        ModTheme.invalidate();
    }

    public static boolean isNoTyping() { load(); return noTyping; }
    public static boolean isConfirmCalls() { load(); return confirmCalls; }
    public static boolean isHidePremium() { load(); return hidePremium; }
    public static boolean isHideStars() { load(); return hideStars; }
    public static boolean isLocalPremium() { load(); return localPremium; }
    public static boolean isSaveDeleted() { load(); return saveDeleted; }
    public static boolean isNoRead() { load(); return noRead; }
    public static boolean isNoBlur() { load(); return noBlur; }
}
