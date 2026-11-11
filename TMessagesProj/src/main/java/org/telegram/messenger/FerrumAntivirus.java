package org.telegram.messenger;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Privacy-first local file triage. This is heuristic analysis, NOT a malware
 * signature engine and must never report a file as guaranteed safe.
 */
public final class FerrumAntivirus {

    public static final int RISK_LOW = 0;
    public static final int RISK_MEDIUM = 1;
    public static final int RISK_HIGH = 2;
    private static final long MAX_SCAN_BYTES = 100L * 1024L * 1024L;
    private static final String HISTORY = "ferrum_scan_history";

    public interface Callback {
        void onComplete(Result result);
    }

    public static final class Result {
        public final String fileName;
        public final String sha256;
        public final long size;
        public final int risk;
        public final ArrayList<String> findings;

        Result(String fileName, String sha256, long size, int risk, ArrayList<String> findings) {
            this.fileName = fileName;
            this.sha256 = sha256;
            this.size = size;
            this.risk = risk;
            this.findings = findings;
        }

        public String riskLabel() {
            return risk == RISK_HIGH ? "Высокий" : risk == RISK_MEDIUM ? "Повышенный" : "Низкий";
        }
    }

    private FerrumAntivirus() { }

    public static void scan(Context context, Uri uri, Callback callback) {
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            Result result;
            try {
                result = scanNow(app, uri);
                saveHistory(app, result);
            } catch (Exception e) {
                ArrayList<String> findings = new ArrayList<>();
                findings.add("Не удалось полностью прочитать файл: " + e.getMessage());
                result = new Result("Неизвестный файл", "", 0, RISK_MEDIUM, findings);
            }
            final Result completed = result;
            AndroidUtilities.runOnUIThread(() -> callback.onComplete(completed));
        }, "FerrumFileScan").start();
    }

    private static Result scanNow(Context context, Uri uri) throws Exception {
        String name = queryName(context, uri);
        File temp = File.createTempFile("ferrum-scan-", ".bin", context.getCacheDir());
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long total = 0;
        byte[] header = new byte[16];
        int headerSize = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(temp)) {
            if (in == null) throw new IllegalStateException("Источник файла недоступен");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_SCAN_BYTES) throw new IllegalArgumentException("Размер файла превышает лимит 100 МБ");
                if (headerSize < header.length) {
                    int copy = Math.min(read, header.length - headerSize);
                    System.arraycopy(buffer, 0, header, headerSize, copy);
                    headerSize += copy;
                }
                digest.update(buffer, 0, read);
                out.write(buffer, 0, read);
            }
        }

        ArrayList<String> findings = new ArrayList<>();
        int risk = RISK_LOW;
        String lower = name.toLowerCase(Locale.US);
        String extension = extension(lower);
        if (lower.matches(".*\\.(pdf|jpg|jpeg|png|txt|docx?)\\.(apk|exe|js|bat|cmd|ps1|sh)$")) {
            risk = RISK_HIGH;
            findings.add("Двойное расширение: имя маскирует исполняемый тип файла");
        }
        if (isExecutableExtension(extension)) {
            risk = Math.max(risk, RISK_MEDIUM);
            findings.add("Файл относится к исполняемому или устанавливаемому типу (" + extension + ")");
        }

        boolean zip = headerSize >= 4 && header[0] == 'P' && header[1] == 'K';
        if (zip) {
            try (ZipFile archive = new ZipFile(temp)) {
                Enumeration<? extends ZipEntry> entries = archive.entries();
                int inspected = 0;
                while (entries.hasMoreElements() && inspected++ < 5000) {
                    ZipEntry entry = entries.nextElement();
                    String path = entry.getName().replace('\\', '/');
                    String pathLower = path.toLowerCase(Locale.US);
                    if (path.startsWith("/") || path.contains("../") || path.equals("..")) {
                        risk = RISK_HIGH;
                        findings.add("В архиве обнаружен путь, выходящий за пределы папки распаковки");
                    }
                    if (pathLower.endsWith(".vbs") || pathLower.endsWith(".js") || pathLower.endsWith(".bat")
                            || pathLower.endsWith(".cmd") || pathLower.endsWith(".ps1") || pathLower.endsWith(".sh")) {
                        risk = Math.max(risk, RISK_MEDIUM);
                        findings.add("В архиве есть потенциально исполняемый файл: " + path);
                    }
                    if (pathLower.endsWith("vbaproject.bin")) {
                        risk = Math.max(risk, RISK_MEDIUM);
                        findings.add("В документе/архиве присутствует VBA-макрос");
                    }
                    if (pathLower.endsWith("androidmanifest.xml") || pathLower.endsWith("classes.dex")) {
                        risk = Math.max(risk, RISK_MEDIUM);
                        findings.add("Архив содержит компоненты Android-приложения");
                    }
                }
                if (inspected >= 5000) findings.add("Проверка архива ограничена первыми 5000 элементами");
            } catch (Exception e) {
                risk = Math.max(risk, RISK_MEDIUM);
                findings.add("ZIP-подобный файл не удалось корректно разобрать");
            }
        } else if (headerSize >= 4 && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F') {
            String sample = readSample(temp, 2 * 1024 * 1024);
            if (sample.contains("/JavaScript") || sample.contains("/JS ") || sample.contains("/OpenAction") || sample.contains("/Launch")) {
                risk = Math.max(risk, RISK_MEDIUM);
                findings.add("PDF содержит активные действия или JavaScript");
            }
        }

        if (findings.isEmpty()) {
            findings.add("Подозрительных признаков по встроенным эвристикам не найдено");
        }
        findings.add("Это эвристическая проверка, а не подтверждение отсутствия вирусов");
        String sha = toHex(digest.digest());
        temp.delete();
        return new Result(name, sha, total, risk, findings);
    }

    private static String queryName(Context context, Uri uri) {
        String name = uri.getLastPathSegment();
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (index >= 0) name = cursor.getString(index);
            }
        } catch (Exception ignore) { }
        return TextUtils.isEmpty(name) ? "Файл" : name;
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }

    private static boolean isExecutableExtension(String ext) {
        return "apk".equals(ext) || "exe".equals(ext) || "msi".equals(ext) || "jar".equals(ext)
                || "dex".equals(ext) || "js".equals(ext) || "vbs".equals(ext)
                || "bat".equals(ext) || "cmd".equals(ext) || "ps1".equals(ext) || "sh".equals(ext);
    }

    private static String readSample(File file, int max) throws Exception {
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while (out.size() < max && (read = in.read(buffer, 0, Math.min(buffer.length, max - out.size()))) != -1) out.write(buffer, 0, read);
            return new String(out.toByteArray(), java.nio.charset.StandardCharsets.ISO_8859_1);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) result.append(String.format(Locale.US, "%02x", b & 0xff));
        return result.toString();
    }

    private static void saveHistory(Context context, Result result) {
        android.content.SharedPreferences prefs = context.getSharedPreferences(HISTORY, Context.MODE_PRIVATE);
        String old = prefs.getString("items", "");
        String item = System.currentTimeMillis() + "|" + result.risk + "|" + result.sha256 + "|" + result.fileName.replace("|", "_");
        String next = item + (TextUtils.isEmpty(old) ? "" : "\n" + old);
        String[] lines = next.split("\\n");
        StringBuilder limited = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, 30); i++) {
            if (i > 0) limited.append('\n');
            limited.append(lines[i]);
        }
        prefs.edit().putString("items", limited.toString()).apply();
    }

    public static String getHistory(Context context) {
        return context.getApplicationContext().getSharedPreferences(HISTORY, Context.MODE_PRIVATE).getString("items", "");
    }
}
