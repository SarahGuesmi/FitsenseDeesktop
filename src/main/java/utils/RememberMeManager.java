package utils;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * RememberMeManager — persists the "remember me" email between app restarts.
 *
 * Stores a small properties file in the user's home directory:
 *   ~/.fitsense/remember_me.properties
 *
 * This is intentionally simple — we only save the email (not the password).
 * The user still has to type their password every time; we just pre-fill the email.
 */
public final class RememberMeManager {

    private static final Path PREFS_FILE = Paths.get(
            System.getProperty("user.home"), ".fitsense", "remember_me.properties");

    private RememberMeManager() {}

    /** Saves the email to disk. Call this after a successful login with "remember me" checked. */
    public static void save(String email) {
        try {
            Files.createDirectories(PREFS_FILE.getParent());
            Properties props = new Properties();
            props.setProperty("email", email);
            try (OutputStream out = Files.newOutputStream(PREFS_FILE)) {
                props.store(out, null);
            }
        } catch (IOException e) {
            System.err.println("RememberMe: could not save — " + e.getMessage());
        }
    }

    /**
     * Loads the saved email. Returns an empty string if nothing was saved.
     * Call this in SignInController.initialize() to pre-fill the email field.
     */
    public static String load() {
        if (!Files.exists(PREFS_FILE)) return "";
        try (InputStream in = Files.newInputStream(PREFS_FILE)) {
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("email", "");
        } catch (IOException e) {
            return "";
        }
    }

    /** Clears the saved email. Call this when the user unchecks "remember me" and signs in. */
    public static void clear() {
        try {
            Files.deleteIfExists(PREFS_FILE);
        } catch (IOException e) {
            System.err.println("RememberMe: could not clear — " + e.getMessage());
        }
    }

    /** Returns true if a saved email exists on disk. */
    public static boolean isRemembered() {
        return Files.exists(PREFS_FILE) && !load().isEmpty();
    }
}
