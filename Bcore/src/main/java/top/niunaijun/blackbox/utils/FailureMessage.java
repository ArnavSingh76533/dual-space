package top.niunaijun.blackbox.utils;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Small, bounded error descriptions suitable for installation IPC and UI. */
public final class FailureMessage {
    private FailureMessage() { }

    public static String orDefault(String message, String fallback) {
        return message == null || message.trim().isEmpty() ? fallback : message.trim();
    }

    public static String describe(Throwable failure) {
        if (failure == null) return "Unknown installation error";
        StringBuilder result = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = failure; cause != null && seen.add(cause) && seen.size() <= 4; cause = cause.getCause()) {
            if (result.length() > 0) result.append("; caused by ");
            result.append(cause.getClass().getSimpleName());
            if (cause.getMessage() != null && !cause.getMessage().trim().isEmpty()) {
                result.append(": ").append(cause.getMessage().trim());
            }
        }
        return result.length() > 1200 ? result.substring(0, 1200) + "…" : result.toString();
    }
}
