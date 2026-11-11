package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.ui.ActionBar.Theme;
import org.json.JSONObject;

/**
 * Local configuration storage for Ferrum-specific features.
 *
 * This class deliberately stores presentation preferences only. It does not
 * store Telegram credentials, message contents, or authentication tokens.
 * Account-level synchronization can be added later through an explicit,
 * user-consented sync layer.
 */
public final class FerrumConfig {

    private static final String PREFS_NAME = "ferrum_settings";

    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_PROFILE_BANNER_URI = "profile_banner_uri";
    private static final String KEY_BANNER_SCALE = "banner_scale";
    private static final String KEY_BANNER_OFFSET_X = "banner_offset_x";
    private static final String KEY_BANNER_OFFSET_Y = "banner_offset_y";
    private static final String KEY_AVATAR_RING_ENABLED = "avatar_ring_enabled";
    private static final String KEY_AVATAR_RING_COLOR = "avatar_ring_color";
    private static final String KEY_AVATAR_RING_WIDTH = "avatar_ring_width";
    private static final String KEY_AVATAR_RING_STYLE = "avatar_ring_style";
    private static final String KEY_ANTIVIRUS_ENABLED = "antivirus_enabled";

    public static final int DEFAULT_ACCENT_COLOR = 0xFF7C9BFF;
    public static final int DEFAULT_AVATAR_RING_COLOR = 0xFF7C9BFF;
    public static final float DEFAULT_BANNER_SCALE = 1.0f;
    public static final float DEFAULT_AVATAR_RING_WIDTH = 2.0f;

    public static final int AVATAR_RING_STYLE_SOLID = 0;
    public static final int AVATAR_RING_STYLE_GRADIENT = 1;
    public static final int AVATAR_RING_STYLE_DASHED = 2;

    private FerrumConfig() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getAccentColor(Context context) {
        return prefs(context).getInt(KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR);
    }

    public static void setAccentColor(Context context, int color) {
        prefs(context).edit().putInt(KEY_ACCENT_COLOR, color).apply();
    }

    /** Applies the saved Ferrum accent to Telegram's shared accent-driven UI colors. */
    public static void applyAccentTheme(Context context) {
        int color = getAccentColor(context);
        Theme.setColor(Theme.key_actionBarDefaultIcon, color, false);
        Theme.setColor(Theme.key_windowBackgroundWhiteBlueText, color, false);
        Theme.setColor(Theme.key_featuredStickers_addButton, color, false);
        Theme.setColor(Theme.key_chat_messageLinkOut, color, false);
    }

    public static String getProfileBannerUri(Context context) {
        return prefs(context).getString(KEY_PROFILE_BANNER_URI, null);
    }

    public static void setProfileBannerUri(Context context, String uri) {
        prefs(context).edit().putString(KEY_PROFILE_BANNER_URI, uri).apply();
    }

    public static void clearProfileBanner(Context context) {
        prefs(context).edit().remove(KEY_PROFILE_BANNER_URI).apply();
    }

    public static float getBannerScale(Context context) {
        return prefs(context).getFloat(KEY_BANNER_SCALE, DEFAULT_BANNER_SCALE);
    }

    public static void setBannerScale(Context context, float scale) {
        prefs(context).edit()
                .putFloat(KEY_BANNER_SCALE, clamp(scale, 0.5f, 3.0f))
                .apply();
    }

    public static float getBannerOffsetX(Context context) {
        return prefs(context).getFloat(KEY_BANNER_OFFSET_X, 0.0f);
    }

    public static void setBannerOffsetX(Context context, float offset) {
        prefs(context).edit().putFloat(KEY_BANNER_OFFSET_X, clamp(offset, -1.0f, 1.0f)).apply();
    }

    public static float getBannerOffsetY(Context context) {
        return prefs(context).getFloat(KEY_BANNER_OFFSET_Y, 0.0f);
    }

    public static void setBannerOffsetY(Context context, float offset) {
        prefs(context).edit().putFloat(KEY_BANNER_OFFSET_Y, clamp(offset, -1.0f, 1.0f)).apply();
    }

    public static boolean isAvatarRingEnabled(Context context) {
        return prefs(context).getBoolean(KEY_AVATAR_RING_ENABLED, false);
    }

    public static void setAvatarRingEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_AVATAR_RING_ENABLED, enabled).apply();
    }

    public static int getAvatarRingColor(Context context) {
        return prefs(context).getInt(KEY_AVATAR_RING_COLOR, DEFAULT_AVATAR_RING_COLOR);
    }

    public static void setAvatarRingColor(Context context, int color) {
        prefs(context).edit().putInt(KEY_AVATAR_RING_COLOR, color).apply();
    }

    public static float getAvatarRingWidth(Context context) {
        return prefs(context).getFloat(KEY_AVATAR_RING_WIDTH, DEFAULT_AVATAR_RING_WIDTH);
    }

    public static void setAvatarRingWidth(Context context, float width) {
        prefs(context).edit()
                .putFloat(KEY_AVATAR_RING_WIDTH, clamp(width, 1.0f, 12.0f))
                .apply();
    }

    public static int getAvatarRingStyle(Context context) {
        int style = prefs(context).getInt(KEY_AVATAR_RING_STYLE, AVATAR_RING_STYLE_SOLID);
        if (style < AVATAR_RING_STYLE_SOLID || style > AVATAR_RING_STYLE_DASHED) {
            return AVATAR_RING_STYLE_SOLID;
        }
        return style;
    }

    public static void setAvatarRingStyle(Context context, int style) {
        if (style < AVATAR_RING_STYLE_SOLID || style > AVATAR_RING_STYLE_DASHED) {
            style = AVATAR_RING_STYLE_SOLID;
        }
        prefs(context).edit().putInt(KEY_AVATAR_RING_STYLE, style).apply();
    }

    public static boolean isAntivirusEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ANTIVIRUS_ENABLED, false);
    }

    public static void setAntivirusEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ANTIVIRUS_ENABLED, enabled).apply();
    }

    /** Exports portable preferences. Image URI is intentionally excluded because it is device-local. */
    public static String exportPortableSettings(Context context) throws Exception {
        JSONObject json = new JSONObject();
        json.put("format", "ferrum-settings");
        json.put("version", 1);
        json.put("accentColor", getAccentColor(context));
        json.put("bannerScale", getBannerScale(context));
        json.put("bannerOffsetX", getBannerOffsetX(context));
        json.put("bannerOffsetY", getBannerOffsetY(context));
        json.put("avatarRingEnabled", isAvatarRingEnabled(context));
        json.put("avatarRingColor", getAvatarRingColor(context));
        json.put("avatarRingWidth", getAvatarRingWidth(context));
        json.put("avatarRingStyle", getAvatarRingStyle(context));
        json.put("antivirusEnabled", isAntivirusEnabled(context));
        return json.toString(2);
    }

    /** Imports only known, validated presentation preferences. Existing banner URI remains local. */
    public static void importPortableSettings(Context context, String source) throws Exception {
        JSONObject json = new JSONObject(source);
        if (!"ferrum-settings".equals(json.optString("format")) || json.optInt("version", -1) != 1) {
            throw new IllegalArgumentException("Неподдерживаемый формат файла Ferrum");
        }
        SharedPreferences.Editor edit = prefs(context).edit();
        edit.putInt(KEY_ACCENT_COLOR, json.optInt("accentColor", DEFAULT_ACCENT_COLOR));
        edit.putFloat(KEY_BANNER_SCALE, clamp((float) json.optDouble("bannerScale", DEFAULT_BANNER_SCALE), 0.5f, 3.0f));
        edit.putFloat(KEY_BANNER_OFFSET_X, clamp((float) json.optDouble("bannerOffsetX", 0), -1.0f, 1.0f));
        edit.putFloat(KEY_BANNER_OFFSET_Y, clamp((float) json.optDouble("bannerOffsetY", 0), -1.0f, 1.0f));
        edit.putBoolean(KEY_AVATAR_RING_ENABLED, json.optBoolean("avatarRingEnabled", false));
        edit.putInt(KEY_AVATAR_RING_COLOR, json.optInt("avatarRingColor", DEFAULT_AVATAR_RING_COLOR));
        edit.putFloat(KEY_AVATAR_RING_WIDTH, clamp((float) json.optDouble("avatarRingWidth", DEFAULT_AVATAR_RING_WIDTH), 1.0f, 12.0f));
        int style = json.optInt("avatarRingStyle", AVATAR_RING_STYLE_SOLID);
        if (style < AVATAR_RING_STYLE_SOLID || style > AVATAR_RING_STYLE_DASHED) style = AVATAR_RING_STYLE_SOLID;
        edit.putInt(KEY_AVATAR_RING_STYLE, style);
        edit.putBoolean(KEY_ANTIVIRUS_ENABLED, json.optBoolean("antivirusEnabled", false));
        edit.apply();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
