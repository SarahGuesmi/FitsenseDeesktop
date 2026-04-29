package services;

import utils.DbConnection;
import utils.UuidUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

/**
 * PasswordResetService — uses the existing `reset_password_request` table.
 *
 * Table columns:
 *   id             BINARY(16)   — random UUID for the row
 *   user_id        BINARY(16)   — FK to app_user.id
 *   hashed_token   VARCHAR(100) — SHA-256 hex of the raw token
 *   selector       VARCHAR(20)  — short random string sent in the URL
 *   requested_at   DATETIME
 *   expires_at     DATETIME
 *
 * The reset URL sent by email is:
 *   http://localhost:8765/reset?selector=SELECTOR&token=RAW_TOKEN
 *
 * On validation we look up by selector, then compare SHA-256(raw_token) == hashed_token.
 */
public class PasswordResetService {

    private static final String SENDGRID_API_KEY = loadApiKey();
    private static final String FROM_EMAIL       = "nourammarr23@icloud.com";
    private static final String FROM_NAME        = "FitSense";
    private static final int    EXPIRY_MINUTES   = 30;
    private static final String RESET_BASE_URL   = "http://localhost:8765/reset";

    private final Connection cnx;

    public PasswordResetService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * sendResetEmail — looks up the user, creates a reset request row, sends the email.
     * Returns false if no account exists for that email.
     */
    public boolean sendResetEmail(String email) throws SQLException, IOException, InterruptedException {
        UserService userService = new UserService();
        models.User user = userService.findByEmail(email);
        if (user == null) return false;

        // Remove any previous requests for this user
        deleteRequestsForUser(user.getId());

        // Generate selector (20 chars) and raw token (32 bytes URL-safe base64)
        String selector = generateSelector();
        String rawToken = generateToken();
        String hashedToken = sha256Hex(rawToken);

        LocalDateTime now       = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRY_MINUTES);

        String sql = "INSERT INTO `reset_password_request` "
                   + "(`id`, `user_id`, `hashed_token`, `selector`, `requested_at`, `expires_at`) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(UUID.randomUUID()));
            stmt.setBytes(2, UuidUtil.toBytes16(user.getId()));
            stmt.setString(3, hashedToken);
            stmt.setString(4, selector);
            stmt.setTimestamp(5, Timestamp.valueOf(now));
            stmt.setTimestamp(6, Timestamp.valueOf(expiresAt));
            stmt.executeUpdate();
        }

        String resetLink = RESET_BASE_URL + "?selector=" + selector + "&token=" + rawToken;
        sendEmail(email, resetLink);
        return true;
    }

    /**
     * validateToken — looks up by selector, verifies SHA-256(rawToken) matches stored hash,
     * checks expiry. Returns the user's email on success, null otherwise.
     */
    public String validateToken(String selector, String rawToken) throws SQLException {
        String sql = "SELECT r.hashed_token, r.expires_at, u.email_email "
                   + "FROM `reset_password_request` r "
                   + "JOIN `fitsense`.`app_user` u ON u.id = r.user_id "
                   + "WHERE r.selector = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, selector);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) return null;

                Timestamp expiresAt = rs.getTimestamp("expires_at");
                if (expiresAt == null || expiresAt.toLocalDateTime().isBefore(LocalDateTime.now())) {
                    deleteBySelector(selector);
                    return null;
                }

                String storedHash = rs.getString("hashed_token");
                if (!sha256Hex(rawToken).equals(storedHash)) return null;

                return rs.getString("email_email");
            }
        }
    }

    /**
     * resetPassword — updates the password and removes the used reset request.
     */
    public void resetPassword(String selector, String rawToken, String newPassword) throws SQLException {
        String email = validateToken(selector, rawToken);
        if (email == null) throw new IllegalArgumentException("Invalid or expired reset link.");

        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder =
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

        String sql = "UPDATE `fitsense`.`app_user` SET `password` = ? WHERE `email_email` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, encoder.encode(newPassword));
            stmt.setString(2, email);
            stmt.executeUpdate();
        }

        deleteBySelector(selector);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void deleteRequestsForUser(UUID userId) throws SQLException {
        try (PreparedStatement stmt = cnx.prepareStatement(
                "DELETE FROM `reset_password_request` WHERE `user_id` = ?")) {
            stmt.setBytes(1, UuidUtil.toBytes16(userId));
            stmt.executeUpdate();
        }
    }

    private void deleteBySelector(String selector) throws SQLException {
        try (PreparedStatement stmt = cnx.prepareStatement(
                "DELETE FROM `reset_password_request` WHERE `selector` = ?")) {
            stmt.setString(1, selector);
            stmt.executeUpdate();
        }
    }

    private String generateSelector() {
        byte[] bytes = new byte[15]; // 15 bytes → 20 base64url chars
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private void sendEmail(String toEmail, String resetLink) throws IOException, InterruptedException {
        String body = "{"
            + "\"personalizations\":[{\"to\":[{\"email\":\"" + toEmail + "\"}]}],"
            + "\"from\":{\"email\":\"" + FROM_EMAIL + "\",\"name\":\"" + FROM_NAME + "\"},"
            + "\"subject\":\"Reset your FitSense password\","
            + "\"content\":[{\"type\":\"text/html\",\"value\":\""
            + htmlBody(resetLink).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "")
            + "\"}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                .header("Authorization", "Bearer " + SENDGRID_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("SendGrid error " + response.statusCode() + ": " + response.body());
        }
    }

    private String htmlBody(String resetLink) {
        return "<div style='font-family:sans-serif;max-width:480px;margin:auto'>"
             + "<h2 style='color:#9f7cff'>FitSense - Password Reset</h2>"
             + "<p>Click the button below to reset your password. "
             + "This link expires in <strong>" + EXPIRY_MINUTES + " minutes</strong>.</p>"
             + "<a href='" + resetLink + "' style='display:inline-block;padding:12px 28px;"
             + "background:#9f7cff;color:#fff;border-radius:8px;text-decoration:none;"
             + "font-weight:bold'>Reset Password</a>"
             + "<p style='color:#888;font-size:12px;margin-top:24px'>"
             + "If you did not request this, you can safely ignore this email.</p>"
             + "</div>";
    }

    private static String loadApiKey() {
        try (InputStream in = PasswordResetService.class.getResourceAsStream("/config.properties")) {
            if (in == null) throw new RuntimeException("config.properties not found in classpath");
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("sendgrid.api.key");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load SendGrid API key: " + e.getMessage(), e);
        }
    }
}
