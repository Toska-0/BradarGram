package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

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

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
