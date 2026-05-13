package utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

/**
 * Loads local assets from the XAMPP htdocs/pidevassets folder.
 * Base path: C:/xampp2/htdocs/pidevassets/
 */
public final class WebAssets {

    private static final String ASSETS_DIR = "C:/xampp2/htdocs/pidevassets/";

    private WebAssets() {}

    public static final String HERO_SPORT_IMAGE = "sport-hero.png";
    public static final String HOME_GYM_VIDEO   = "gym-video.mp4";

    /**
     * Returns a file:// URL for the given asset filename inside pidevassets/.
     */
    public static String assetUrl(String filename) {
        String name = filename == null ? "" : filename.replaceFirst("^/+", "");
        return new File(ASSETS_DIR + name).toURI().toString();
    }

    /**
     * Sets an ImageView from a file in the pidevassets folder.
     */
    public static void loadPublicAsset(ImageView imageView, String filename) {
        if (imageView == null) return;
        String url = assetUrl(filename);
        Image image = new Image(url, true);
        imageView.setImage(image);
    }
}
