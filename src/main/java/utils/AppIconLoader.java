package utils;

import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Utility class to load and set the application icon on JavaFX stages.
 * Uses the sport-hero.png logo as the application icon.
 */
public class AppIconLoader {
    
    private static Image cachedIcon = null;
    
    /**
     * Sets the application icon on the given stage.
     * Uses cached icon for performance if already loaded.
     * 
     * @param stage The stage to set the icon on
     */
    public static void setIcon(Stage stage) {
        if (stage == null) return;
        
        try {
            if (cachedIcon == null) {
                cachedIcon = loadIcon();
            }
            
            if (cachedIcon != null && !cachedIcon.isError()) {
                stage.getIcons().add(cachedIcon);
            }
        } catch (Exception e) {
            System.err.println("Failed to set application icon: " + e.getMessage());
        }
    }
    
    /**
     * Loads the application icon from resources or external path.
     * 
     * @return The loaded Image or null if failed
     */
    private static Image loadIcon() {
        try {
            // Try to load from bundled resources first (preferred method)
            try {
                Image icon = new Image(AppIconLoader.class.getResourceAsStream("/images/sport-hero.png"));
                if (icon != null && !icon.isError()) {
                    System.out.println("Application icon loaded from resources");
                    return icon;
                }
            } catch (Exception e) {
                System.out.println("Could not load icon from resources, trying external path...");
            }
            
            // Fallback to external file path
            java.io.File iconFile = new java.io.File("C:/xampp2/htdocs/pidevassets/sport-hero.png");
            if (iconFile.exists()) {
                Image icon = new Image(iconFile.toURI().toString());
                if (icon != null && !icon.isError()) {
                    System.out.println("Application icon loaded from external path");
                    return icon;
                }
            } else {
                System.err.println("Application icon not found at: " + iconFile.getAbsolutePath());
            }
            
        } catch (Exception e) {
            System.err.println("Failed to load application icon: " + e.getMessage());
        }
        
        return null;
    }
}