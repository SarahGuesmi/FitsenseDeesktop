package services;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Enumeration;
import java.util.Properties;

/**
 * IPInfoService — resolves geographic info for an IP via ipinfo.io.
 *
 * For local/loopback addresses it auto-detects the machine's public IP
 * by calling api.ipify.org, then resolves that.
 *
 * Token is read from config.properties: ipinfo.token=YOUR_TOKEN
 * Free tier: 50 000 requests/month — plenty for a desktop app.
 */
public class IPInfoService {

    private static final String TOKEN = loadToken();

    // ── Public API ────────────────────────────────────────────────────────────

    public static class LocationInfo {
        public String ip      = "";
        public String city    = "";
        public String region  = "";
        public String country = "";
        public String isp     = "";
    }

    /**
     * Resolves location for the given IP.
     * Pass null or empty string to auto-detect the current machine's public IP.
     */
    public LocationInfo resolve(String ip) {
        LocationInfo info = new LocationInfo();
        try {
            String target = ip;

            // If local/loopback, get the real public IP first
            if (target == null || target.isBlank()
                    || target.equals("127.0.0.1") || target.equals("::1")
                    || target.startsWith("192.168.") || target.startsWith("10.")
                    || target.startsWith("172.")) {
                target = fetchPublicIp();
            }

            info.ip = target;
            if (target == null || target.isBlank()) return info;

            String url = "https://ipinfo.io/" + target + "?token=" + TOKEN;
            String json = httpGet(url);
            if (json == null) return info;

            info.city    = extractField(json, "city");
            info.region  = extractField(json, "region");
            info.country = extractField(json, "country");
            // ISP is in the "org" field, e.g. "AS1234 Tunisie Telecom"
            String org = extractField(json, "org");
            info.isp = org.contains(" ") ? org.substring(org.indexOf(' ') + 1) : org;

        } catch (Exception e) {
            System.err.println("IPInfoService: " + e.getMessage());
        }
        return info;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String fetchPublicIp() {
        try {
            return httpGet("https://api.ipify.org");
        } catch (Exception e) {
            return null;
        }
    }

    private String httpGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> resp = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());
        return resp.statusCode() == 200 ? resp.body() : null;
    }

    /** Minimal JSON field extractor — avoids pulling in a JSON library */
    private String extractField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return "";
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return "";
        int end = json.indexOf('"', start + 1);
        if (end < 0) return "";
        return json.substring(start + 1, end);
    }

    private static String loadToken() {
        try (InputStream in = IPInfoService.class.getResourceAsStream("/config.properties")) {
            if (in == null) return "";
            Properties p = new Properties();
            p.load(in);
            return p.getProperty("ipinfo.token", "");
        } catch (Exception e) {
            return "";
        }
    }
}
