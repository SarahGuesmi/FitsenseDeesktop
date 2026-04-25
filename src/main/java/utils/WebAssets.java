package utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// AppConfig reads from config.properties

/**
 * Loads images from the Symfony web app over HTTP (Option B).
 * <p>
 * File on disk: {@code public/assets/images/sport-hero.png}<br>
 * URL: {@code http://&lt;host&gt;/assets/images/sport-hero.png}
 * <p>
 * Override base URL with system property {@code fitsense.web.url}, e.g.
 * {@code -Dfitsense.web.url=https://fitsense.example.com}
 */
public final class WebAssets {

    private static final String DEFAULT_BASE = "http://localhost:8000";

    private WebAssets() {
    }

    /** Path under {@code public/} for the sign-in / sign-up hero (no leading slash). */
    public static final String HERO_SPORT_IMAGE = "assets/images/sport-hero.png";

    /**
     * Home page background video (served by Symfony from {@code public/}).
     */
    public static final String HOME_GYM_VIDEO = "assets/images/gym-video.mp4";

    public static String baseUrl() {
        return AppConfig.get("fitsense.web.url", DEFAULT_BASE).replaceAll("/+$", "");
    }

    /**
     * @param pathUnderPublic no leading slash, e.g. {@code assets/images/sport-hero.png}
     *                        For exercise images use {@code uploads/exercises/filename.jpg}
     */
    public static String assetUrl(String pathUnderPublic) {
        String path = pathUnderPublic == null ? "" : pathUnderPublic.replaceFirst("^/+", "");
        // Exercise images are stored in XAMPP pidevassets
        if (path.startsWith("uploads/exercises/")) {
            String filename = path.replace("uploads/exercises/", "");
            return "http://localhost/pidevassets/" + filename;
        }
        return baseUrl() + "/" + path;
    }

    /**
     * Sets an {@link ImageView} from a path under Symfony's {@code public/} folder.
     *
     * @param imageView target view
     * @param pathUnderPublic e.g. {@code assets/images/sport-hero.png}
     */
    public static void loadPublicAsset(ImageView imageView, String pathUnderPublic) {
        if (imageView == null) {
            return;
        }
        String url = assetUrl(pathUnderPublic);
        Image image = new Image(url, true);
        imageView.setImage(image);
    }
}
