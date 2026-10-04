package com.arnav.dualspace;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashSet;
import top.niunaijun.blackbox.fake.device.IdentityValues;

public class IdentityValuesTest {
    @Test public void generatedIdentitiesHaveCorrectFormatsAndDoNotCollide() {
        HashSet<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String id = IdentityValues.hex(8);
            assertTrue(id.matches("[0-9a-f]{16}")); assertTrue(ids.add(id));
            assertTrue(IdentityValues.digits(20).matches("[1-9][0-9]{19}"));
            String mac = IdentityValues.mac();
            assertTrue(mac.matches("(?:[0-9a-f]{2}:){5}[0-9a-f]{2}"));
            int first = Integer.parseInt(mac.substring(0, 2), 16);
            assertEquals(0, first & 1); assertEquals(2, first & 2);
            String imei = IdentityValues.deviceId(); assertTrue(imei.matches("[0-9]{15}"));
            int sum = 0;
            for (int j = 0; j < 15; j++) { int n = imei.charAt(j) - '0'; if (j % 2 == 1) { n *= 2; if (n > 9) n -= 9; } sum += n; }
            assertEquals(0, sum % 10);
        }
    }
}
