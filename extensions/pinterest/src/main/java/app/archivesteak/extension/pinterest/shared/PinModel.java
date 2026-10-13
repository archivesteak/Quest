package app.archivesteak.extension.pinterest.shared;

import java.util.Date;
import java.util.Map;

/**
 * Accessors for Pinterest's Pin model. The model class and its getters are obfuscated,
 * so every method here is implemented by the patch.
 */
@SuppressWarnings("unused")
public final class PinModel {
    public static Date getCreatedAt(Object pin) {
        return null;
    }

    /**
     * Images of the Pin keyed by size, such as "orig" or "736x".
     */
    public static Map<String, ?> getImages(Object pin) {
        return null;
    }

    public static String getImageUrl(Object image) {
        return null;
    }

    /**
     * Video encodings of the Pin keyed by encoding name, or null if the Pin has no video.
     */
    public static Map<String, ?> getVideoList(Object pin) {
        return null;
    }

    public static String getVideoUrl(Object videoEncoding) {
        return null;
    }
}
