package utils;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * ResetTokenCallbackServer — a minimal HTTP server on localhost:8765.
 *
 * When the user clicks the reset link in their email, the browser opens
 * http://localhost:8765/reset?token=XXX
 * This server captures the token and passes it to the JavaFX app via a callback.
 *
 * Usage:
 *   ResetTokenCallbackServer.start(token -> Platform.runLater(() -> openResetPage(token)));
 *   // later, when no longer needed:
 *   ResetTokenCallbackServer.stop();
 */
public class ResetTokenCallbackServer {

    private static final int PORT = 8765;
    private static ServerSocket serverSocket;
    private static ExecutorService executor;

    public static void start(Consumer<String> onTokenReceived) {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "reset-token-server");
            t.setDaemon(true);
            return t;
        });
        executor.submit(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                while (!serverSocket.isClosed()) {
                    try (Socket client = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                         PrintWriter out = new PrintWriter(client.getOutputStream())) {

                        String requestLine = in.readLine();
                        if (requestLine == null) continue;

                        // Parse GET /reset?selector=XXX&token=YYY HTTP/1.1
                        String[] params = extractParams(requestLine);

                        // Send a friendly HTML response to the browser
                        String body = "<html><body style='font-family:sans-serif;text-align:center;padding:60px'>"
                                + "<h2 style='color:#9f7cff'>Return to FitSense</h2>"
                                + "<p>You can close this tab and complete the reset in the app.</p>"
                                + "</body></html>";
                        out.print("HTTP/1.1 200 OK\r\n");
                        out.print("Content-Type: text/html\r\n");
                        out.print("Content-Length: " + body.length() + "\r\n");
                        out.print("Connection: close\r\n\r\n");
                        out.print(body);
                        out.flush();

                        if (params != null) {
                            onTokenReceived.accept(params[0] + "|" + params[1]);
                            stop();
                        }
                    } catch (IOException ignored) {}
                }
            } catch (IOException e) {
                if (!serverSocket.isClosed()) e.printStackTrace();
            }
        });
    }

    public static void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
        if (executor != null) executor.shutdownNow();
    }

    private static String[] extractParams(String requestLine) {
        // requestLine: "GET /reset?selector=XXX&token=YYY HTTP/1.1"
        try {
            String path = requestLine.split(" ")[1];
            if (!path.contains("?")) return null;
            String query = path.substring(path.indexOf('?') + 1);
            String selector = null, token = null;
            for (String param : query.split("&")) {
                if (param.startsWith("selector="))
                    selector = URLDecoder.decode(param.substring(9), "UTF-8");
                else if (param.startsWith("token="))
                    token = URLDecoder.decode(param.substring(6), "UTF-8");
            }
            return (selector != null && token != null) ? new String[]{selector, token} : null;
        } catch (Exception ignored) {}
        return null;
    }
}
