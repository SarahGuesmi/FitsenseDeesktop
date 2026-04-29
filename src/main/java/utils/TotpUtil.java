package utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * TotpUtil — minimal TOTP (RFC 6238) implementation.
 * No external library needed — uses only javax.crypto (built into the JDK).
 *
 * Compatible with Google Authenticator, Authy, and any RFC 6238 app.
 */
public final class TotpUtil {

    private static final int    DIGITS    = 6;
    private static final int    PERIOD    = 30;   // seconds per window
    private static final int    WINDOW    = 1;    // ±1 window tolerance for clock skew
    private static final String ALGORITHM = "HmacSHA1";

    private TotpUtil() {}

    /**
     * Generates a new random Base32-encoded secret (160 bits).
     * Store this in app_user.google_authenticator_secret.
     */
    public static String generateSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * Builds the otpauth:// URI used to generate the QR code.
     * The user scans this with Google Authenticator.
     */
    public static String buildOtpAuthUri(String secret, String email, String issuer) {
        return "otpauth://totp/"
                + urlEncode(issuer + ":" + email)
                + "?secret=" + secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=6&period=30";
    }

    /**
     * Builds a Google Charts QR code image URL for the otpauth URI.
     * The JavaFX ImageView can load this directly via WebAssets or HTTP.
     */
    public static String buildQrCodeUrl(String secret, String email, String issuer) {
        String otpUri = buildOtpAuthUri(secret, email, issuer);
        return "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data="
                + urlEncode(otpUri);
    }

    /**
     * Verifies a 6-digit code against the secret.
     * Checks the current window ± WINDOW to tolerate small clock differences.
     */
    public static boolean verify(String secret, String code) {
        if (secret == null || code == null || code.length() != DIGITS) return false;
        long counter = Instant.now().getEpochSecond() / PERIOD;
        for (int i = -WINDOW; i <= WINDOW; i++) {
            if (generateCode(secret, counter + i).equals(code)) return true;
        }
        return false;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String generateCode(String secret, long counter) {
        try {
            byte[] key = base32Decode(secret);
            byte[] msg = new byte[8];
            for (int i = 7; i >= 0; i--) {
                msg[i] = (byte) (counter & 0xFF);
                counter >>= 8;
            }
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            byte[] hash = mac.doFinal(msg);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset]     & 0x7F) << 24)
                       | ((hash[offset + 1] & 0xFF) << 16)
                       | ((hash[offset + 2] & 0xFF) << 8)
                       |  (hash[offset + 3] & 0xFF);
            int otp = binary % (int) Math.pow(10, DIGITS);
            return String.format("%0" + DIGITS + "d", otp);
        } catch (Exception e) {
            throw new RuntimeException("TOTP generation failed", e);
        }
    }

    // ── Base32 (RFC 4648) ─────────────────────────────────────────────────────

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_CHARS.charAt((buffer >> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) sb.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 31));
        return sb.toString();
    }

    private static byte[] base32Decode(String s) {
        s = s.toUpperCase().replaceAll("[^A-Z2-7]", "");
        int outputLen = s.length() * 5 / 8;
        byte[] out = new byte[outputLen];
        int buffer = 0, bitsLeft = 0, idx = 0;
        for (char c : s.toCharArray()) {
            buffer = (buffer << 5) | BASE32_CHARS.indexOf(c);
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[idx++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return out;
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return s;
        }
    }
}
