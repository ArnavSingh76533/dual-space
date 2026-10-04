package top.niunaijun.blackbox.utils;

/** Rejects a mismatched manifest and retries with an explicitly selected base manifest. */
public final class VerifiedPackageParser {
    private VerifiedPackageParser() { }

    public interface Parser<T> {
        T parse(boolean explicitManifest) throws Throwable;
        String packageName(T parsed);
    }

    public static <T> T parse(String expected, Parser<T> parser) throws Exception {
        Throwable normalFailure;
        try {
            return verify(expected, parser.parse(false), parser);
        } catch (Throwable failure) {
            rethrowFatal(failure);
            normalFailure = failure;
        }
        try {
            return verify(expected, parser.parse(true), parser);
        } catch (Throwable failure) {
            rethrowFatal(failure);
            throw new Exception("Could not parse installed package " + expected
                    + ". Normal parser: " + FailureMessage.describe(normalFailure)
                    + ". Explicit base manifest: " + FailureMessage.describe(failure), failure);
        }
    }

    private static <T> T verify(String expected, T parsed, Parser<T> parser) {
        String actual = parsed == null ? null : parser.packageName(parsed);
        if (expected == null || !expected.equals(actual)) {
            throw new IllegalStateException("Expected package " + expected + ", but the manifest parser returned " + actual);
        }
        return parsed;
    }

    private static void rethrowFatal(Throwable failure) {
        if (failure instanceof VirtualMachineError) throw (VirtualMachineError) failure;
        if (failure instanceof ThreadDeath) throw (ThreadDeath) failure;
    }
}
