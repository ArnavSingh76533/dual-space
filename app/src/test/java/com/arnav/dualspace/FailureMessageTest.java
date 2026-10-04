package com.arnav.dualspace;

import org.junit.Test;
import java.io.IOException;
import java.util.Collections;
import top.niunaijun.blackbox.utils.FailureMessage;
import static org.junit.Assert.*;

public class FailureMessageTest {
    @Test public void exceptionsWithoutMessagesStillIdentifyTheFailure() {
        assertEquals("NullPointerException", FailureMessage.describe(new NullPointerException()));
        assertEquals("Unknown installation error", FailureMessage.describe(null));
        assertEquals("Installation failed", FailureMessage.orDefault(" \n", "Installation failed"));
        assertEquals("Installation failed", FailureMessage.orDefault(null, "Installation failed"));
    }

    @Test public void wrappedErrorsRetainTheirCause() {
        String description = FailureMessage.describe(new IllegalStateException("Manifest parse", new IOException("APK unreadable")));
        assertTrue(description.contains("Manifest parse"));
        assertTrue(description.contains("IOException: APK unreadable"));
    }

    @Test public void cyclicOrLongExceptionsProduceBoundedMessages() {
        Exception first = new Exception("First"), second = new Exception("Second");
        first.initCause(second); second.initCause(first);
        assertEquals("Exception: First; caused by Exception: Second", FailureMessage.describe(first));
        assertTrue(FailureMessage.describe(new IOException(String.join("", Collections.nCopies(5000, "x")))).length() <= 1201);
    }
}
