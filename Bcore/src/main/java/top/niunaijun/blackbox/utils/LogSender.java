package top.niunaijun.blackbox.utils;
import java.io.File;
/** External uploads are disabled in Dual Space. */
public final class LogSender {
    public static String send(String chatId, File logFile, String caption) {
        return "External log uploads are disabled";
    }
}
