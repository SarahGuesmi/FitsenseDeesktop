package services;

import models.LoginAttempt;
import models.User;
import utils.DbConnection;
import utils.UuidUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * LoginSecurityService — records login attempts into the existing
 * `login_attempt` table and detects unusual login locations.
 *
 * Table columns used:
 *   id, user_id, ip_address, status, country, city, region, isp,
 *   timestamp, email_email
 */
public class LoginSecurityService {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";

    private static final int    SPAM_GUARD_MINUTES = 5;
    private static final String FROM_EMAIL = "sarahguesmi223@gmail.com";
    private static final String FROM_NAME  = "FitSense Security";

    private final Connection    cnx;
    private final IPInfoService ipInfo = new IPInfoService();

    public LoginSecurityService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * recordAndCheck — inserts a login_attempt row and checks for location change.
     * Returns true if an unusual location was detected (caller should force logout).
     */
    public boolean recordAndCheck(User user, String status) {
        try {
            IPInfoService.LocationInfo loc = ipInfo.resolve(null);
            LoginAttempt attempt = buildAttempt(user, loc, status);
            insert(attempt);

            if (STATUS_SUCCESS.equals(status)) {
                return checkUnusualLocation(user, attempt);
            }
        } catch (Exception e) {
            System.err.println("LoginSecurityService.recordAndCheck: " + e.getMessage());
        }
        return false;
    }

    /**
     * getRecentHistory — last N attempts for a user, newest first.
     */
    public List<LoginAttempt> getRecentHistory(UUID userId, int limit) {
        List<LoginAttempt> list = new ArrayList<>();
        String sql = "SELECT * FROM `login_attempt` WHERE `user_id` = ? "
                   + "ORDER BY `timestamp` DESC LIMIT ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(userId));
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (Exception e) {
            System.err.println("LoginSecurityService.getRecentHistory: " + e.getMessage());
        }
        return list;
    }

    /**
     * getAllHistory — returns all login attempts for dashboard statistics.
     */
    public List<LoginAttempt> getAllHistory() {
        List<LoginAttempt> list = new ArrayList<>();
        String sql = "SELECT * FROM `login_attempt` ORDER BY `timestamp` ASC";
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (Exception e) {
            System.err.println("LoginSecurityService.getAllHistory: " + e.getMessage());
        }
        return list;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean checkUnusualLocation(User user, LoginAttempt current) throws Exception {
        // Spam guard: skip if another SUCCESS was recorded in the last N minutes
        String spamSql = "SELECT COUNT(*) FROM `login_attempt` "
                + "WHERE `user_id` = ? AND `status` = 'SUCCESS' "
                + "AND `timestamp` > ? AND `id` != ?";
        try (PreparedStatement stmt = cnx.prepareStatement(spamSql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(user.getId()));
            stmt.setTimestamp(2, Timestamp.valueOf(
                    current.getTimestamp().minusMinutes(SPAM_GUARD_MINUTES)));
            stmt.setBytes(3, UuidUtil.toBytes16(current.getId()));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) return false;
            }
        }

        // Get last 10 successful logins (excluding current)
        String historySql = "SELECT `city` FROM `login_attempt` "
                + "WHERE `user_id` = ? AND `status` = 'SUCCESS' AND `id` != ? "
                + "ORDER BY `timestamp` DESC LIMIT 10";
        List<String> knownCities = new ArrayList<>();
        try (PreparedStatement stmt = cnx.prepareStatement(historySql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(user.getId()));
            stmt.setBytes(2, UuidUtil.toBytes16(current.getId()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String c = rs.getString("city");
                    if (c != null && !c.isBlank()) knownCities.add(c.toLowerCase());
                }
            }
        }

        // First ever login — nothing to compare
        if (knownCities.isEmpty()) return false;

        String currentCity = current.getCity() == null ? "" : current.getCity().toLowerCase();
        if (!currentCity.isBlank() && !knownCities.contains(currentCity)) {
            sendUnusualLocationAlert(user, current);
            return true;
        }
        return false;
    }

    private void sendUnusualLocationAlert(User user, LoginAttempt attempt) {
        String name = user.getFirstname() != null && !user.getFirstname().isBlank()
                ? user.getFirstname() : user.getEmail();
        String time = attempt.getTimestamp()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm"));

        String htmlBody = "<div style='font-family:sans-serif;max-width:520px;margin:auto;"
            + "background:#0d121f;padding:32px;border-radius:12px'>"
            + "<h2 style='color:#ff5c6e;margin-top:0'>⚠ Unusual Login Detected</h2>"
            + "<p style='color:#c9cbda'>Hi <strong>" + name + "</strong>,</p>"
            + "<p style='color:#c9cbda'>We detected a sign-in to your FitSense account from a new location:</p>"
            + "<table style='border-collapse:collapse;width:100%;margin:16px 0;background:#13131b;"
            + "border-radius:8px;overflow:hidden'>"
            + row("📍 Location", attempt.locationString())
            + row("🌐 IP Address", attempt.getIpAddress())
            + row("🕐 Time",       time)
            + row("📡 ISP",        safe(attempt.getIsp()))
            + "</table>"
            + "<p style='color:#ff5c6e;font-weight:bold'>Your session has been terminated for your security.</p>"
            + "<p style='color:#a6a8b6'>If this was you, simply sign in again.<br>"
            + "If not, please reset your password immediately.</p>"
            + "<p style='color:#555;font-size:12px;margin-top:24px'>— FitSense Security Team</p>"
            + "</div>";

        try {
            sendEmail(user.getEmail(),
                    "⚠ Unusual login detected on your FitSense account", htmlBody);
        } catch (Exception e) {
            System.err.println("LoginSecurityService: alert email failed — " + e.getMessage());
        }
    }

    private String row(String label, String value) {
        return "<tr>"
             + "<td style='padding:10px 14px;color:#6f7285;white-space:nowrap'>" + label + "</td>"
             + "<td style='padding:10px 14px;color:#ffffff;font-weight:600'>" + value + "</td>"
             + "</tr>";
    }

    private void sendEmail(String to, String subject, String htmlBody)
            throws IOException, InterruptedException {
        String apiKey = loadSendGridKey();
        String body = "{"
            + "\"personalizations\":[{\"to\":[{\"email\":\"" + to + "\"}]}],"
            + "\"from\":{\"email\":\"" + FROM_EMAIL + "\",\"name\":\"" + FROM_NAME + "\"},"
            + "\"subject\":\"" + subject + "\","
            + "\"content\":[{\"type\":\"text/html\",\"value\":\""
            + htmlBody.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "")
            + "\"}]}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
    }

    private LoginAttempt buildAttempt(User user, IPInfoService.LocationInfo loc, String status) {
        LoginAttempt a = new LoginAttempt();
        a.setId(UUID.randomUUID());
        a.setUserId(user.getId());
        a.setIpAddress(loc.ip);
        a.setStatus(status);
        a.setCountry(loc.country);
        a.setCity(loc.city);
        a.setRegion(loc.region);
        a.setIsp(loc.isp);
        a.setTimestamp(LocalDateTime.now());
        a.setEmailEmail(user.getEmail());
        return a;
    }

    private void insert(LoginAttempt a) throws SQLException {
        String sql = "INSERT INTO `login_attempt` "
                + "(`id`,`user_id`,`ip_address`,`status`,`country`,`city`,`region`,"
                + "`isp`,`timestamp`,`email_email`) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1,    UuidUtil.toBytes16(a.getId()));
            stmt.setBytes(2,    UuidUtil.toBytes16(a.getUserId()));
            stmt.setString(3,   a.getIpAddress());
            stmt.setString(4,   a.getStatus());
            stmt.setString(5,   a.getCountry());
            stmt.setString(6,   a.getCity());
            stmt.setString(7,   a.getRegion());
            stmt.setString(8,   a.getIsp());
            stmt.setTimestamp(9, Timestamp.valueOf(a.getTimestamp()));
            stmt.setString(10,  a.getEmailEmail());
            stmt.executeUpdate();
        }
    }

    private LoginAttempt mapRow(ResultSet rs) throws SQLException {
        LoginAttempt a = new LoginAttempt();
        a.setId(UuidUtil.fromResultSet(rs, "id"));
        // user_id may be null in some rows
        try { a.setUserId(UuidUtil.fromResultSet(rs, "user_id")); } catch (Exception ignored) {}
        a.setIpAddress(rs.getString("ip_address"));
        a.setStatus(rs.getString("status"));
        a.setCountry(rs.getString("country"));
        a.setCity(rs.getString("city"));
        a.setRegion(rs.getString("region"));
        a.setIsp(rs.getString("isp"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) a.setTimestamp(ts.toLocalDateTime());
        a.setEmailEmail(rs.getString("email_email"));
        return a;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String loadSendGridKey() {
        try (java.io.InputStream in =
                LoginSecurityService.class.getResourceAsStream("/config.properties")) {
            if (in == null) return "";
            java.util.Properties p = new java.util.Properties();
            p.load(in);
            return p.getProperty("sendgrid.api.key", "");
        } catch (Exception e) { return ""; }
    }
}
