package com.arnav.nimbus;

import org.junit.Test;
import java.io.IOException;
import java.util.*;
import top.niunaijun.blackbox.core.GoogleSetupRunner;
import static org.junit.Assert.*;

public class GoogleSetupRunnerTest {
    private static final String GSF = "com.google.android.gsf";
    private static final String GMS = "com.google.android.gms";
    private static final String STORE = "com.android.vending";
    private static final String[] ORDER = {GSF, GMS, STORE};

    private static final class Backend implements GoogleSetupRunner.Backend {
        final Set<String> installed = new HashSet<>();
        final List<String> installs = new ArrayList<>();
        String failInstall, failCheck, unverified, failVerify;
        @Override public boolean isInstalled(String pkg) throws IOException {
            if (pkg.equals(failCheck) || (pkg.equals(failVerify) && installs.contains(pkg))) {
                throw new IOException("Package service disconnected");
            }
            return installed.contains(pkg);
        }
        @Override public void install(String pkg) throws IOException {
            installs.add(pkg);
            if (pkg.equals(failInstall)) throw new IOException("Cannot parse manifest");
            if (!pkg.equals(unverified)) installed.add(pkg);
        }
    }

    @Test public void installsAndVerifiesInDependencyOrder() throws Exception {
        Backend backend = new Backend();
        GoogleSetupRunner.run(ORDER, backend);
        assertEquals(Arrays.asList(ORDER), backend.installs);
        assertEquals(new HashSet<>(Arrays.asList(ORDER)), backend.installed);
    }

    @Test public void existingComponentsAreNotReinstalled() throws Exception {
        Backend backend = new Backend(); backend.installed.add(GSF); backend.installed.add(GMS);
        GoogleSetupRunner.run(ORDER, backend);
        assertEquals(Collections.singletonList(STORE), backend.installs);
    }

    @Test public void failedInstallStopsAndNextAttemptResumes() throws Exception {
        Backend backend = new Backend(); backend.failInstall = GMS;
        GoogleSetupRunner.SetupFailure failure = failure(backend);
        assertEquals(GMS, failure.packageName);
        assertTrue(failure.getMessage().contains("installing"));
        assertTrue(failure.getMessage().contains("Cannot parse manifest"));
        assertEquals(Collections.singleton(GSF), backend.installed);
        assertFalse(backend.installs.contains(STORE));
        backend.failInstall = null;
        GoogleSetupRunner.run(ORDER, backend);
        assertEquals(Arrays.asList(GSF, GMS, GMS, STORE), backend.installs);
    }

    @Test public void claimedSuccessWithMissingPackageIsFailure() {
        Backend backend = new Backend(); backend.unverified = GMS;
        GoogleSetupRunner.SetupFailure failure = failure(backend);
        assertEquals(GMS, failure.packageName);
        assertTrue(failure.getMessage().contains("missing from this space"));
        assertFalse(backend.installs.contains(STORE));
    }

    @Test public void unavailableStateDoesNotCauseAnInstall() {
        Backend backend = new Backend(); backend.failCheck = GSF;
        GoogleSetupRunner.SetupFailure failure = failure(backend);
        assertTrue(failure.getMessage().contains("checking this space"));
        assertTrue(failure.getMessage().contains("Package service disconnected"));
        assertTrue(backend.installs.isEmpty());
    }

    @Test public void lostVerificationConnectionIsNotReportedAsMissing() {
        Backend backend = new Backend(); backend.failVerify = GMS;
        GoogleSetupRunner.SetupFailure failure = failure(backend);
        assertTrue(failure.getMessage().contains("verifying this space"));
        assertFalse(failure.getMessage().contains("missing from this space"));
        assertTrue(backend.installed.contains(GMS));
    }

    private static GoogleSetupRunner.SetupFailure failure(Backend backend) {
        try { GoogleSetupRunner.run(ORDER, backend); fail("Expected setup failure"); }
        catch (GoogleSetupRunner.SetupFailure expected) { return expected; }
        throw new AssertionError("No setup failure");
    }
}
