package top.niunaijun.blackbox.core;

import top.niunaijun.blackbox.utils.FailureMessage;

/** Installs missing dependencies in order and verifies them against the selected space. */
public final class GoogleSetupRunner {
    private GoogleSetupRunner() { }

    public interface Backend {
        boolean isInstalled(String packageName) throws Exception;
        void install(String packageName) throws Exception;
    }

    public static final class SetupFailure extends Exception {
        public final String packageName;
        private SetupFailure(String packageName, String phase, Exception cause) {
            super("Google setup failed for " + packageName + " while " + phase + ": " + FailureMessage.describe(cause), cause);
            this.packageName = packageName;
        }
    }

    public static void run(String[] ordered, Backend backend) throws SetupFailure {
        for (String pkg : ordered) {
            String phase = "checking this space";
            try {
                if (backend.isInstalled(pkg)) continue;
                phase = "installing";
                backend.install(pkg);
                phase = "verifying this space";
                if (!backend.isInstalled(pkg)) {
                    throw new IllegalStateException("Installer returned success, but the package is missing from this space. Restart the engine and retry setup.");
                }
            } catch (Exception failure) {
                // Keep completed dependencies and existing app data so the next attempt can resume.
                throw new SetupFailure(pkg, phase, failure);
            }
        }
    }
}
