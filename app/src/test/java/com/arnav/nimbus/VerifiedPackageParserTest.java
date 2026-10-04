package com.arnav.nimbus;

import org.junit.Test;
import java.io.IOException;
import java.util.*;
import top.niunaijun.blackbox.utils.VerifiedPackageParser;
import static org.junit.Assert.*;

public class VerifiedPackageParserTest {
    private static final String GMS = "com.google.android.gms";
    private static final String MODULE = "com.google.android.gms.dynamite_cronetdynamite";

    private static final class Parser implements VerifiedPackageParser.Parser<String> {
        String normal = GMS, explicit = GMS;
        IOException normalFailure;
        final List<Boolean> attempts = new ArrayList<>();
        @Override public String parse(boolean isolated) throws IOException {
            attempts.add(isolated);
            if (!isolated && normalFailure != null) throw normalFailure;
            return isolated ? explicit : normal;
        }
        @Override public String packageName(String result) { return result; }
    }

    @Test public void correctManifestNeedsOnlyOneParse() throws Exception {
        Parser parser = new Parser();
        assertEquals(GMS, VerifiedPackageParser.parse(GMS, parser));
        assertEquals(Collections.singletonList(false), parser.attempts);
    }

    @Test public void reportedCronetModuleMismatchRetriesTheBaseManifest() throws Exception {
        Parser parser = new Parser(); parser.normal = MODULE;
        assertEquals(GMS, VerifiedPackageParser.parse(GMS, parser));
        assertEquals(Arrays.asList(false, true), parser.attempts);
    }

    @Test public void unrelatedModuleIsNeverAcceptedAsPlayServices() {
        Parser parser = new Parser(); parser.normal = MODULE; parser.explicit = MODULE;
        Exception failure = failure(parser);
        assertTrue(failure.getMessage().contains("Expected package " + GMS));
        assertTrue(failure.getMessage().contains(MODULE));
        assertEquals(MODULE, parser.explicit);
    }

    @Test public void normalParserFailureCanUseExplicitBaseManifest() throws Exception {
        Parser parser = new Parser(); parser.normalFailure = new IOException("Resource load failed");
        assertEquals(GMS, VerifiedPackageParser.parse(GMS, parser));
        assertEquals(Arrays.asList(false, true), parser.attempts);
    }

    @Test public void failedFallbackRetainsBothCauses() {
        Parser parser = new Parser(); parser.normalFailure = new IOException("Resource load failed"); parser.explicit = null;
        Exception failure = failure(parser);
        assertTrue(failure.getMessage().contains("Resource load failed"));
        assertTrue(failure.getMessage().contains("Explicit base manifest"));
        assertTrue(failure.getMessage().contains("returned null"));
    }

    private static Exception failure(Parser parser) {
        try { VerifiedPackageParser.parse(GMS, parser); fail("Expected package mismatch"); }
        catch (Exception expected) { return expected; }
        throw new AssertionError("No parse failure");
    }
}
