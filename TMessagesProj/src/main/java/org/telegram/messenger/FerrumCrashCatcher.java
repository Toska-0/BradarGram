package org.telegram.messenger;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Saves uncaught crash stack traces to small files so the next launch can show
 * them to the user. Intentionally dependency-free: it must work even when the
 * rest of the app is broken.
 */
public final class FerrumCrashCatcher {

    private static final String FILE_NAME = "ferrum_last_crash.txt";
    private static final String COUNT_PREFS = "ferrum_crash";
    private static final int MAX_BYTES = 64 * 1024;

    private FerrumCrashCatcher() { }

    public static void install(Context context) {
        try {
            final Context app = context.getApplicationContext();
            final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            if (app == null || previous instanceof Catcher) {
                return;
            }
            Thread.setDefaultUncaughtExceptionHandler(new Catcher(app, previous));
        } catch (Throwable ignore) { }
    }

    /** Number of crashes since the last successful (60s stable) session. */
    public static int getCrashCount(Context context) {
        try {
            return context.getApplicationContext()
                    .getSharedPreferences(COUNT_PREFS, Context.MODE_PRIVATE).getInt("count", 0);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static void clearCrashCount(Context context) {
        try {
            context.getApplicationContext()
                    .getSharedPreferences(COUNT_PREFS, Context.MODE_PRIVATE)
                    .edit().remove("count").apply();
        } catch (Throwable ignore) { }
    }

    /** Returns the saved crash log (if any) and deletes the files. */
    public static String consumeLastCrash(Context context) {
        try {
            Context app = context.getApplicationContext();
            File file = new File(app.getFilesDir(), FILE_NAME);
            if (!file.exists()) {
                return null;
            }
            byte[] bytes = readAll(file);
            file.delete();
            File ext = app.getExternalFilesDir(null);
            if (ext != null) {
                new File(ext, FILE_NAME).delete();
            }
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            return text.length() > MAX_BYTES ? text.substring(0, MAX_BYTES) : text;
        } catch (Throwable t) {
            return null;
        }
    }

    private static byte[] readAll(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static void save(Context app, Thread thread, Throwable throwable) {
        try {
            StringWriter sw = new StringWriter();
            sw.write("time: " + new java.util.Date().toString() + "\n");
            sw.write("thread: " + thread.getName() + "\n");
            sw.write("\nStack trace:\n");
            throwable.printStackTrace(new PrintWriter(sw));
            Throwable cause = throwable.getCause();
            int depth = 0;
            while (cause != null && depth < 5) {
                sw.write("\nCaused by:\n");
                cause.printStackTrace(new PrintWriter(sw));
                cause = cause.getCause();
                depth++;
            }
            byte[] data = sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (data.length > MAX_BYTES) {
                byte[] trimmed = new byte[MAX_BYTES];
                System.arraycopy(data, 0, trimmed, 0, MAX_BYTES);
                data = trimmed;
            }
            try (FileOutputStream out = new FileOutputStream(new File(app.getFilesDir(), FILE_NAME))) {
                out.write(data);
            }
            try {
                File ext = app.getExternalFilesDir(null);
                if (ext != null) {
                    try (FileOutputStream out = new FileOutputStream(new File(ext, FILE_NAME))) {
                        out.write(data);
                    }
                }
            } catch (Throwable ignore) { }
            try {
                app.getSharedPreferences(COUNT_PREFS, Context.MODE_PRIVATE)
                        .edit().putInt("count", getCrashCount(app) + 1).apply();
            } catch (Throwable ignore) { }
        } catch (Throwable ignore) { }
    }

    private static final class Catcher implements Thread.UncaughtExceptionHandler {
        private final Context app;
        private final Thread.UncaughtExceptionHandler previous;

        Catcher(Context app, Thread.UncaughtExceptionHandler previous) {
            this.app = app;
            this.previous = previous;
        }

        @Override
        public void uncaughtException(Thread thread, Throwable throwable) {
            save(app, thread, throwable);
            if (previous != null) {
                previous.uncaughtException(thread, throwable);
            }
        }
    }
}
