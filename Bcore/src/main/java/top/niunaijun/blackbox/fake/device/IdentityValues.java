package top.niunaijun.blackbox.fake.device;

import java.security.SecureRandom;
import java.util.Locale;

/** Formats container identifiers; never changes physical hardware identifiers. */
public final class IdentityValues {
    private static final SecureRandom RANDOM = new SecureRandom();
    private IdentityValues() {}
    public static String hex(int bytes) {
        byte[] values = new byte[bytes];
        RANDOM.nextBytes(values);
        StringBuilder s = new StringBuilder();
        for (byte b : values) s.append(String.format(Locale.US, "%02x", b & 255));
        return s.toString();
    }
    public static String digits(int length) {
        StringBuilder s = new StringBuilder();
        s.append(1 + RANDOM.nextInt(9));
        while (s.length() < length) s.append(RANDOM.nextInt(10));
        return s.toString();
    }
    public static String deviceId() {
        String base = digits(14);
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            int n = base.charAt(i) - '0';
            if (i % 2 == 1) { n *= 2; if (n > 9) n -= 9; }
            sum += n;
        }
        return base + ((10 - sum % 10) % 10);
    }
    public static String mac() {
        byte[] b = new byte[6];
        RANDOM.nextBytes(b);
        b[0] = (byte) ((b[0] | 2) & 254); // Locally administered, unicast.
        return String.format(Locale.US, "%02x:%02x:%02x:%02x:%02x:%02x",
                b[0] & 255, b[1] & 255, b[2] & 255, b[3] & 255, b[4] & 255, b[5] & 255);
    }
}
