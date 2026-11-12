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
    private static final String KEY_UI_PRESET = "ui_preset";
    private static final String KEY_ANTIVIRUS_ENABLED = "antivirus_enabled";
    private static final String KEY_ANTIVIRUS_ENGINE = "antivirus_engine";
    private static final String KEY_AI_PROVIDER = "antivirus_ai_provider";
    private static final String KEY_AI_API_KEY = "antivirus_ai_api_key_";
    private static final String KEY_VT_API_KEY = "antivirus_vt_api_key_enc";
    private static final String KEYSTORE_ALIAS = "FerrumApiKeys";

    public static final int DEFAULT_ACCENT_COLOR = 0xFF7C9BFF;
    public static final int DEFAULT_AVATAR_RING_COLOR = 0xFF7C9BFF;
    public static final float DEFAULT_BANNER_SCALE = 1.0f;
    public static final float DEFAULT_AVATAR_RING_WIDTH = 2.0f;

    public static final int AVATAR_RING_STYLE_SOLID = 0;
    public static final int AVATAR_RING_STYLE_GRADIENT = 1;
    public static final int AVATAR_RING_STYLE_DASHED = 2;

    public static final int UI_PRESET_DEFAULT = 0;
    public static final int UI_PRESET_FULL_WIDTH = 1;
    public static final int UI_PRESET_FLOATING = 2;

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
        prefs(context).edit().putFloat(KEY_BANNER_OFFSET_X, clamp(offset, -3.0f, 3.0f)).apply();
    }

    public static float getBannerOffsetY(Context context) {
        return prefs(context).getFloat(KEY_BANNER_OFFSET_Y, 0.0f);
    }

    public static void setBannerOffsetY(Context context, float offset) {
        prefs(context).edit().putFloat(KEY_BANNER_OFFSET_Y, clamp(offset, -3.0f, 3.0f)).apply();
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

    /** Preset for the main screen layout: bottom navigation shape and search field corners. */
    public static int getUiPreset(Context context) {
        int preset = prefs(context).getInt(KEY_UI_PRESET, UI_PRESET_DEFAULT);
        if (preset < UI_PRESET_DEFAULT || preset > UI_PRESET_FLOATING) {
            return UI_PRESET_DEFAULT;
        }
        return preset;
    }

    public static void setUiPreset(Context context, int preset) {
        if (preset < UI_PRESET_DEFAULT || preset > UI_PRESET_FLOATING) {
            preset = UI_PRESET_DEFAULT;
        }
        prefs(context).edit().putInt(KEY_UI_PRESET, preset).apply();
    }

    public static boolean isAntivirusEnabled(Context context) {
        return prefs(context).getBoolean(KEY_ANTIVIRUS_ENABLED, false);
    }

    public static void setAntivirusEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_ANTIVIRUS_ENABLED, enabled).apply();
    }

    public static final int ANTIVIRUS_ENGINE_AI = 0;
    public static final int ANTIVIRUS_ENGINE_VIRUSTOTAL = 1;

    public static int getAntivirusEngine(Context context) {
        int engine = prefs(context).getInt(KEY_ANTIVIRUS_ENGINE, ANTIVIRUS_ENGINE_AI);
        return engine == ANTIVIRUS_ENGINE_VIRUSTOTAL ? engine : ANTIVIRUS_ENGINE_AI;
    }

    public static void setAntivirusEngine(Context context, int engine) {
        if (engine != ANTIVIRUS_ENGINE_VIRUSTOTAL) engine = ANTIVIRUS_ENGINE_AI;
        prefs(context).edit().putInt(KEY_ANTIVIRUS_ENGINE, engine).apply();
    }

    public static String getAiProvider(Context context) {
        String provider = prefs(context).getString(KEY_AI_PROVIDER, "Gemini");
        return "Claude".equals(provider) || "ChatGPT".equals(provider) ? provider : "Gemini";
    }

    public static void setAiProvider(Context context, String provider) {
        if (!"Gemini".equals(provider) && !"Claude".equals(provider) && !"ChatGPT".equals(provider)) {
            provider = "Gemini";
        }
        prefs(context).edit().putString(KEY_AI_PROVIDER, provider).apply();
    }

    public static void setAiApiKey(Context context, String value) throws Exception {
        storeSecret(context, KEY_AI_API_KEY + getAiProvider(context).toLowerCase(java.util.Locale.US), value);
    }

    public static String getAiApiKey(Context context) {
        return readSecret(context, KEY_AI_API_KEY + getAiProvider(context).toLowerCase(java.util.Locale.US));
    }

    public static void setVirusTotalApiKey(Context context, String value) throws Exception {
        storeSecret(context, KEY_VT_API_KEY, value);
    }

    public static String getVirusTotalApiKey(Context context) {
        return readSecret(context, KEY_VT_API_KEY);
    }

    private static void storeSecret(Context context, String preferenceKey, String value) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            prefs(context).edit().remove(preferenceKey).apply();
            return;
        }
        javax.crypto.SecretKey secretKey = getOrCreateSecretKey();
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(value.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] iv = cipher.getIV();
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
        String encoded = android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP);
        prefs(context).edit().putString(preferenceKey, encoded).apply();
    }

    private static String readSecret(Context context, String preferenceKey) {
        String encoded = prefs(context).getString(preferenceKey, null);
        if (encoded == null || encoded.isEmpty()) return "";
        try {
            byte[] combined = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP);
            if (combined.length <= 12) return "";
            byte[] iv = java.util.Arrays.copyOfRange(combined, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(combined, 12, combined.length);
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, getOrCreateSecretKey(), new javax.crypto.spec.GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static javax.crypto.SecretKey getOrCreateSecretKey() throws Exception {
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        java.security.Key existing = keyStore.getKey(KEYSTORE_ALIAS, null);
        if (existing instanceof javax.crypto.SecretKey) return (javax.crypto.SecretKey) existing;
        javax.crypto.KeyGenerator generator = javax.crypto.KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        android.security.keystore.KeyGenParameterSpec spec = new android.security.keystore.KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        generator.init(spec);
        return generator.generateKey();
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
        json.put("uiPreset", getUiPreset(context));
        json.put("antivirusEnabled", isAntivirusEnabled(context));
        json.put("antivirusEngine", getAntivirusEngine(context));
        json.put("aiProvider", getAiProvider(context));
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
        edit.putFloat(KEY_BANNER_OFFSET_X, clamp((float) json.optDouble("bannerOffsetX", 0), -3.0f, 3.0f));
        edit.putFloat(KEY_BANNER_OFFSET_Y, clamp((float) json.optDouble("bannerOffsetY", 0), -3.0f, 3.0f));
        edit.putBoolean(KEY_AVATAR_RING_ENABLED, json.optBoolean("avatarRingEnabled", false));
        edit.putInt(KEY_AVATAR_RING_COLOR, json.optInt("avatarRingColor", DEFAULT_AVATAR_RING_COLOR));
        edit.putFloat(KEY_AVATAR_RING_WIDTH, clamp((float) json.optDouble("avatarRingWidth", DEFAULT_AVATAR_RING_WIDTH), 1.0f, 12.0f));
        int style = json.optInt("avatarRingStyle", AVATAR_RING_STYLE_SOLID);
        if (style < AVATAR_RING_STYLE_SOLID || style > AVATAR_RING_STYLE_DASHED) style = AVATAR_RING_STYLE_SOLID;
        edit.putInt(KEY_AVATAR_RING_STYLE, style);
        int uiPreset = json.optInt("uiPreset", UI_PRESET_DEFAULT);
        if (uiPreset < UI_PRESET_DEFAULT || uiPreset > UI_PRESET_FLOATING) uiPreset = UI_PRESET_DEFAULT;
        edit.putInt(KEY_UI_PRESET, uiPreset);
        edit.putBoolean(KEY_ANTIVIRUS_ENABLED, json.optBoolean("antivirusEnabled", false));
        int engine = json.optInt("antivirusEngine", ANTIVIRUS_ENGINE_AI);
        edit.putInt(KEY_ANTIVIRUS_ENGINE, engine == ANTIVIRUS_ENGINE_VIRUSTOTAL ? ANTIVIRUS_ENGINE_VIRUSTOTAL : ANTIVIRUS_ENGINE_AI);
        String provider = json.optString("aiProvider", "Gemini");
        edit.putString(KEY_AI_PROVIDER, "Gemini".equals(provider) || "Claude".equals(provider) || "ChatGPT".equals(provider) ? provider : "Gemini");
        edit.apply();
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
