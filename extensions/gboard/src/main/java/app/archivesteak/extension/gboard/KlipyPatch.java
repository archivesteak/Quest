package app.archivesteak.extension.gboard;

import app.archivesteak.extension.gboard.settings.Settings;

/**
 * KLIPY's API is compatible with Tenor's, so Gboard's requests only need KLIPY's address and a KLIPY API key.
 */
@SuppressWarnings("unused")
public final class KlipyPatch {
    private static final String TENOR_API = "https://tenor.googleapis.com/";
    private static final String KLIPY_API = "https://api.klipy.com/";

    /**
     * Injection point. Overrides the server flags holding the addresses of Tenor's API.
     */
    public static Object overrideUrl(Object url) {
        if (url instanceof String && ((String) url).startsWith(TENOR_API) && isEnabled()) {
            return KLIPY_API + ((String) url).substring(TENOR_API.length());
        }
        return url;
    }

    /**
     * Injection point. Overrides the server flag holding the key of Tenor's API.
     */
    public static Object overrideApiKey(Object key) {
        return isEnabled() ? getApiKey() : key;
    }

    private static boolean isEnabled() {
        return Settings.USE_KLIPY.get() && !getApiKey().isEmpty();
    }

    private static String getApiKey() {
        String key = Settings.KLIPY_API_KEY.get();
        return key.isEmpty() ? getDefaultApiKey() : key;
    }

    /**
     * @return The API key set when patching, or an empty string. Implemented by the patch.
     */
    public static String getDefaultApiKey() {
        return "";
    }
}
