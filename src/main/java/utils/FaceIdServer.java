package utils;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * FaceIdServer — minimal HTTP server on port 8766.
 *
 * Serves two endpoints:
 *   GET  /faceid?token=XXX  → mobile login page (HTML)
 *   POST /faceid             → receives { token, email } and triggers login on PC
 *
 * The PC generates a session token, encodes it in a QR code URL, and starts this server.
 * When the phone submits the email, onEmailReceived is called with the email.
 */
public class FaceIdServer {

    public static final int PORT = 8766;

    private static ServerSocket serverSocket;
    private static ExecutorService executor;
    private static String expectedToken;
    private static Consumer<String> onEmailReceived;

    /** Start the server for a specific token. Calls onEmailReceived when phone submits. */
    public static void start(String token, Consumer<String> callback) {
        expectedToken   = token;
        onEmailReceived = callback;

        executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "faceid-server");
            t.setDaemon(true);
            return t;
        });

        executor.submit(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));
                System.out.println("FaceIdServer listening on port " + PORT);
                while (!serverSocket.isClosed()) {
                    try {
                        Socket client = serverSocket.accept();
                        executor.submit(() -> handle(client));
                    } catch (IOException ignored) {}
                }
            } catch (IOException e) {
                System.err.println("FaceIdServer failed to start: " + e.getMessage());
            }
        });
    }

    public static void stop() {
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
        if (executor != null) executor.shutdownNow();
    }

    // ── Request handler ───────────────────────────────────────────────────────

    private static void handle(Socket client) {
        try (client;
             BufferedReader in  = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             OutputStream   out = client.getOutputStream()) {

            // Read request line + headers
            String requestLine = in.readLine();
            if (requestLine == null) return;

            String method = requestLine.split(" ")[0];
            String path   = requestLine.split(" ").length > 1 ? requestLine.split(" ")[1] : "/";

            // Read headers to get Content-Length
            int contentLength = 0;
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }
            }

            if ("GET".equals(method) && path.startsWith("/faceid")) {
                String token = extractParam(path, "token");
                serveLoginPage(out, token);
            } else if ("POST".equals(method) && path.startsWith("/faceid")) {
                // Read body
                char[] body = new char[contentLength];
                in.read(body, 0, contentLength);
                String bodyStr = new String(body);
                String email = extractFormParam(bodyStr, "email");
                String token = extractFormParam(bodyStr, "token");

                if (expectedToken != null && expectedToken.equals(token) && email != null && !email.isBlank()) {
                    serveSuccessPage(out);
                    stop();
                    if (onEmailReceived != null) onEmailReceived.accept(email.trim());
                } else {
                    serveErrorPage(out, "Invalid session or email.");
                }
            } else {
                send404(out);
            }
        } catch (Exception e) {
            System.err.println("FaceIdServer.handle: " + e.getMessage());
        }
    }

    // ── HTML pages ────────────────────────────────────────────────────────────

    private static void serveLoginPage(OutputStream out, String token) throws IOException {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset='UTF-8'>
                <meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>
                <title>FitSense — Secure Login</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body { 
                        background: #0a0a0e; 
                        color: #fff; 
                        font-family: -apple-system, system-ui, sans-serif; 
                        min-height: 100vh; 
                        display: flex; 
                        flex-direction: column; 
                        align-items: center; 
                        justify-content: center; 
                    }
                    .brand { font-size: 20px; font-weight: 900; color: #9f7cff; margin-bottom: 40px; letter-spacing: 3px; }
                    .container { width: 100%; max-width: 360px; padding: 20px; text-align: center; }
                    .icon-box { 
                        width: 80px; height: 80px; background: rgba(159,124,255,0.1); 
                        border-radius: 20px; display: flex; align-items: center; 
                        justify-content: center; margin: 0 auto 24px;
                        border: 1px solid rgba(159,124,255,0.2);
                        box-shadow: 0 10px 20px rgba(0,0,0,0.3);
                    }
                    h1 { font-size: 24px; font-weight: 700; margin-bottom: 8px; }
                    p { color: #8e91a1; font-size: 15px; margin-bottom: 40px; }
                    .card { 
                        background: #13131b; border-radius: 24px; padding: 32px 24px; 
                        border: 1px solid rgba(255,255,255,0.08); width: 100%; 
                    }
                    input { 
                        width: 100%; background: #0d0d14; color: #fff; border: 1px solid rgba(255,255,255,0.1); 
                        border-radius: 12px; padding: 16px; font-size: 16px; margin-bottom: 20px; outline: none; 
                    }
                    input:focus { border-color: #9f7cff; }
                    button { 
                        width: 100%; background: #9f7cff; color: #fff; border: none; 
                        border-radius: 14px; padding: 16px; font-size: 17px; font-weight: 700; 
                        cursor: pointer; transition: 0.2s; 
                    }
                    button:disabled { opacity: 0.5; cursor: not-allowed; }
                    .loading-spinner { 
                        width: 24px; height: 24px; border: 3px solid rgba(255,255,255,0.3); 
                        border-top-color: #fff; border-radius: 50%; animation: spin 1s linear infinite; 
                        display: none; margin: 0 auto;
                    }
                    @keyframes spin { to { transform: rotate(360deg); } }
                    .hidden { display: none; }
                </style>
            </head>
            <body>
                <div class='brand'>FITSENSE</div>
                <div class='container' id='main-ui'>
                    <div class='icon-box'>
                        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#9f7cff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/><path d="m9 12 2 2 4-4"/>
                        </svg>
                    </div>
                    <h1>Biometric Security</h1>
                    <p>Verify your identity to access your FitSense dashboard.</p>
                    <div class='card'>
                        <input type='email' id='email-input' placeholder='Enter your email' required autofocus>
                        <button id='auth-btn' onclick='authenticate()'>
                            <span id='btn-text'>CONTINUE</span>
                            <div id='spinner' class='loading-spinner'></div>
                        </button>
                    </div>
                </div>
                
                <form id='hidden-form' method='POST' action='/faceid' class='hidden'>
                    <input type='hidden' name='token' value='%s'>
                    <input type='hidden' name='email' id='form-email'>
                </form>

                <script>
                    const emailInput = document.getElementById('email-input');
                    const authBtn = document.getElementById('auth-btn');
                    const btnText = document.getElementById('btn-text');
                    const spinner = document.getElementById('spinner');
                    const formEmail = document.getElementById('form-email');
                    const hiddenForm = document.getElementById('hidden-form');

                    async function authenticate() {
                        const email = emailInput.value.trim();
                        if (!email || !email.includes('@')) {
                            alert('Please enter a valid email.');
                            return;
                        }

                        authBtn.disabled = true;
                        btnText.classList.add('hidden');
                        spinner.style.display = 'block';

                        try {
                            // The "Mock Registration" trick to trigger native FaceID/Biometrics
                            await navigator.credentials.create({
                                publicKey: {
                                    challenge: crypto.getRandomValues(new Uint8Array(32)),
                                    rp: { name: "FitSense", id: window.location.hostname },
                                    user: {
                                        id: crypto.getRandomValues(new Uint8Array(16)),
                                        name: email,
                                        displayName: email
                                    },
                                    pubKeyCredParams: [{alg: -7, type: "public-key"}, {alg: -257, type: "public-key"}],
                                    timeout: 60000,
                                    authenticatorSelection: {
                                        userVerification: "required",
                                        residentKey: "preferred"
                                    }
                                }
                            });

                            // If we get here, biometric auth was successful
                            formEmail.value = email;
                            hiddenForm.submit();
                        } catch (err) {
                            console.error('Auth error:', err);
                            alert('Security verification failed or cancelled. Please try again.');
                            authBtn.disabled = false;
                            btnText.classList.remove('hidden');
                            spinner.style.display = 'none';
                        }
                    }
                </script>
            </body>
            </html>
            """.replace("%s", token);

        sendHtml(out, 200, html);
    }

    private static void serveSuccessPage(OutputStream out) throws IOException {
        String html = "<!DOCTYPE html><html><head>"
            + "<meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>FitSense — Authenticated</title>"
            + "<style>"
            + "body { background: #0a0a0e; color: #fff; font-family: 'Segoe UI', sans-serif;"
            + "  display: flex; flex-direction: column; align-items: center;"
            + "  justify-content: center; min-height: 100vh; text-align: center; padding: 24px; }"
            + "h1 { font-size: 32px; font-weight: 800; color: #34d399; margin-bottom: 12px; }"
            + "p  { color: #a6a8b6; font-size: 16px; }"
            + "</style></head><body>"
            + "<h1>✅ Authenticated!</h1>"
            + "<p>You can now return to the FitSense app on your computer.</p>"
            + "</body></html>";
        sendHtml(out, 200, html);
    }

    private static void serveErrorPage(OutputStream out, String msg) throws IOException {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
            + "<style>body{background:#0a0a0e;color:#ff5c6e;font-family:sans-serif;"
            + "display:flex;align-items:center;justify-content:center;min-height:100vh;}</style>"
            + "</head><body><h2>⚠ " + msg + "</h2></body></html>";
        sendHtml(out, 400, html);
    }

    private static void send404(OutputStream out) throws IOException {
        sendHtml(out, 404, "<html><body>Not found</body></html>");
    }

    private static void sendHtml(OutputStream out, int status, String html) throws IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        String header = "HTTP/1.1 " + status + " OK\r\n"
                + "Content-Type: text/html; charset=UTF-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    // ── Param helpers ─────────────────────────────────────────────────────────

    private static String extractParam(String path, String key) {
        if (!path.contains("?")) return "";
        String query = path.substring(path.indexOf('?') + 1);
        for (String p : query.split("&")) {
            if (p.startsWith(key + "=")) {
                try { return URLDecoder.decode(p.substring(key.length() + 1), "UTF-8"); }
                catch (Exception e) { return ""; }
            }
        }
        return "";
    }

    private static String extractFormParam(String body, String key) {
        for (String p : body.split("&")) {
            if (p.startsWith(key + "=")) {
                try { return URLDecoder.decode(p.substring(key.length() + 1), "UTF-8"); }
                catch (Exception e) { return ""; }
            }
        }
        return null;
    }

    /** Returns the machine's local network IP (not loopback) */
    public static String getLocalIp() {
        try {
            java.util.Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                java.util.Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address) return addr.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }
}
