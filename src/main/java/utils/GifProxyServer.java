package utils;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class GifProxyServer {

    private static final int PORT = 7654;
    private static HttpServer server;
    private static final String RAPIDAPI_KEY = "2c19914553mshcf25f09126d1868p19781bjsn23bfb6716cf3";

    public static void start() {
        if (server != null) return;
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
            server.createContext("/gif", exchange -> {
                try {
                    handleGifRequest(exchange);
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, 0);
                    exchange.close();
                }
            });
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            System.out.println("GifProxyServer started on port " + PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public static String proxyUrl(String gifUrl) {
        if (gifUrl == null || gifUrl.isBlank()) return "";
        try {
            return "http://127.0.0.1:" + PORT + "/gif?url="
                    + java.net.URLEncoder.encode(gifUrl, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return gifUrl;
        }
    }

    private static void handleGifRequest(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.startsWith("url=")) {
            exchange.sendResponseHeaders(400, 0);
            exchange.close();
            return;
        }

        String gifUrl = URLDecoder.decode(query.substring(4), StandardCharsets.UTF_8);
        System.out.println("Proxy fetching: " + gifUrl);

        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Cache-Control", "max-age=3600");

        // ── Suivre les redirects manuellement (HTTP → HTTPS) ──
        String currentUrl = gifUrl;
        HttpURLConnection conn = null;
        int redirectCount = 0;

        while (redirectCount < 5) {
            URL url = new URL(currentUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(false); // on gère manuellement

            // Headers RapidAPI seulement pour exercisedb.p.rapidapi.com
            if (currentUrl.contains("rapidapi.com")) {
                conn.setRequestProperty("X-RapidAPI-Key", RAPIDAPI_KEY);
                conn.setRequestProperty("X-RapidAPI-Host", "exercisedb.p.rapidapi.com");
            }
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://exercisedb.p.rapidapi.com");

            int responseCode = conn.getResponseCode();
            System.out.println("Proxy response: " + responseCode + " for " + currentUrl);

            // Gérer les redirects 301 / 302 / 303 / 307 / 308
            if (responseCode == 301 || responseCode == 302
                    || responseCode == 303 || responseCode == 307 || responseCode == 308) {
                String location = conn.getHeaderField("Location");
                if (location == null || location.isBlank()) break;
                System.out.println("Redirect → " + location);
                conn.disconnect();
                currentUrl = location;
                redirectCount++;
                continue;
            }

            // Réponse finale
            if (responseCode == 200) {
                String contentType = conn.getContentType();
                if (contentType == null) contentType = "image/gif";
                exchange.getResponseHeaders().add("Content-Type", contentType);
                exchange.sendResponseHeaders(200, 0);

                try (InputStream in = conn.getInputStream();
                     OutputStream out = exchange.getResponseBody()) {
                    byte[] buf = new byte[8192];
                    int read;
                    while ((read = in.read(buf)) != -1) {
                        out.write(buf, 0, read);
                    }
                }
            } else {
                System.err.println("Proxy final error code: " + responseCode);
                exchange.sendResponseHeaders(responseCode, 0);
            }

            exchange.close();
            return;
        }

        // Trop de redirects
        System.err.println("Too many redirects for: " + gifUrl);
        exchange.sendResponseHeaders(502, 0);
        exchange.close();
    }
}