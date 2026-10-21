package com.winlator.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Small, private, bounded log for startup and native runtime diagnosis. */
public final class DiagnosticLogger {
    private static final String FILE_NAME = "fluxlator-diagnostics.log";
    private static final long MAX_LOG_BYTES = 512 * 1024;
    private static final Object LOCK = new Object();
    private static Context appContext;

    private DiagnosticLogger() {}

    public static void initialize(Context context) {
        synchronized (LOCK) {
            appContext = context.getApplicationContext();
        }
        info("App", "Diagnostic logging initialized");
    }

    public static void installCrashHandler() {
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            log("FATAL", "UncaughtException", "Process crashed on thread " + thread.getName(), error);
            if (previous != null) previous.uncaughtException(thread, error);
        });
    }

    public static void info(String tag, String message) {
        log("INFO", tag, message, null);
    }

    public static void warn(String tag, String message) {
        log("WARN", tag, message, null);
    }

    public static void error(String tag, String message, Throwable error) {
        log("ERROR", tag, message, error);
    }

    private static void log(String level, String tag, String message, Throwable error) {
        Context context = appContext;
        if (context == null) return;

        String safeTag = tag == null ? "App" : tag.replace('\n', ' ').replace('\r', ' ');
        String safeMessage = message == null ? "" : message.replace('\r', ' ').replace('\n', ' ');
        StringBuilder line = new StringBuilder();
        line.append(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date()))
            .append(" ").append(level).append("/").append(safeTag)
            .append(" [pid=").append(Process.myPid()).append(", thread=").append(Thread.currentThread().getName()).append("] ")
            .append(safeMessage).append('\n');
        if (error != null) {
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            line.append(trace).append('\n');
        }

        synchronized (LOCK) {
            try {
                File file = getLogFile(context);
                rotateIfNeeded(file, line.length());
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8))) {
                    writer.write(line.toString());
                }
            }
            catch (IOException e) {
                Log.e("Fluxlator", "Could not persist diagnostic entry", e);
            }
        }
    }

    private static File getLogFile(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    private static void rotateIfNeeded(File file, int nextEntryLength) {
        if (!file.exists() || file.length() + nextEntryLength <= MAX_LOG_BYTES) return;
        File previous = new File(file.getParentFile(), FILE_NAME + ".1");
        if (previous.exists()) previous.delete();
        if (!file.renameTo(previous)) file.delete();
    }

    public static String readAll() {
        Context context = appContext;
        if (context == null) return "Diagnostics are not initialized yet.";
        StringBuilder result = new StringBuilder(buildDeviceSummary(context));
        synchronized (LOCK) {
            appendFile(result, new File(context.getFilesDir(), FILE_NAME + ".1"));
            appendFile(result, getLogFile(context));
        }
        if (result.length() == 0) return "No diagnostic events have been recorded yet.";
        return result.toString();
    }

    private static void appendFile(StringBuilder result, File file) {
        if (!file.isFile()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line).append('\n');
        }
        catch (IOException e) {
            result.append("Unable to read ").append(file.getName()).append(": ").append(e.getMessage()).append('\n');
        }
    }

    private static String buildDeviceSummary(Context context) {
        String version = "unknown";
        try {
            PackageInfo info;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                info = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(0));
            }
            else {
                //noinspection deprecation
                info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            }
            version = info.versionName + " (" + info.versionCode + ")";
        }
        catch (PackageManager.NameNotFoundException ignored) {}

        StringBuilder result = new StringBuilder("Fluxlator diagnostics\n")
            .append("App version: ").append(version).append('\n')
            .append("Android: ").append(Build.VERSION.RELEASE).append(" / API ").append(Build.VERSION.SDK_INT).append('\n')
            .append("Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append('\n')
            .append("ABI: ");
        String[] abis = Build.SUPPORTED_ABIS;
        for (int i = 0; i < abis.length; i++) {
            if (i > 0) result.append(", ");
            result.append(abis[i]);
        }
        result.append("\nLogs are stored privately on this device.\n\n");
        return result.toString();
    }

    public static void clear() {
        Context context = appContext;
        if (context == null) return;
        synchronized (LOCK) {
            new File(context.getFilesDir(), FILE_NAME).delete();
            new File(context.getFilesDir(), FILE_NAME + ".1").delete();
        }
        info("Diagnostics", "Log history cleared by user");
    }
}
