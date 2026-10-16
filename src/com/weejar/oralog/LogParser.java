/*
 * Decompiled with CFR 0.152.
 */
package com.weejar.oralog;

import com.weejar.oralog.LogEntry;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LogParser {
    public static final String[] DEFAULT_EXTENSIONS = new String[]{".log", ".trc", ".txt"};
    private static final Set<String> MESSAGE_FILE_NAMES = Set.of("message", "messages");
    private static final Set<String> EXCLUDE_TRC_DIRS = Set.of("incident", "titanagent", "zabbix");
    private static final Set<String> MESSAGE_ONLY_PARENTS = Set.of("log");
    private static final Pattern CRS_TS = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s*(.*)$");
    private static final Pattern TRC_TS = Pattern.compile("^\\*\\*\\*\\s+(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?)(Z|[+\\-]\\d{2}:?\\d{2})?\\s*$");
    private static final Pattern ISO_TS = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?)(Z|[+\\-]\\d{2}:?\\d{2})?\\s*(.*)$");
    private static final Pattern RDBMS_TS = Pattern.compile("^([A-Z][a-z]{2}\\s+[A-Z][a-z]{2}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2}\\s+\\d{4})\\s*(.*)$");
    private static final Pattern LISTENER_TS = Pattern.compile("^(\\d{2}-[A-Z]{3}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})\\s*(.*)$");
    private static final Pattern SYSLOG_TS = Pattern.compile("^([A-Z][a-z]{2})\\s+(\\d{1,2})\\s+(\\d{2}:\\d{2}:\\d{2})\\s+(.*)$");
    private static final DateTimeFormatter CRS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter UNIFIED_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter RDBMS_FMT = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter LISTENER_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
    private static final Map<String, Month> MONTH_MAP = new HashMap<String, Month>(16);
    private static final String[] ERROR_KEYWORDS;
    private static final String[] WARNING_KEYWORDS;

    public List<LogEntry> scan(Path path, String[] stringArray) throws IOException {
        return this.scan(path, stringArray, null);
    }

    public List<LogEntry> scan(Path path, String[] stringArray, String nodeName) throws IOException {
        if (stringArray == null || stringArray.length == 0) {
            stringArray = DEFAULT_EXTENSIONS;
        }
        ArrayList<LogEntry> arrayList = new ArrayList<LogEntry>(8192);
        List<Path> list = this.collectFiles(path, stringArray);
        for (Path path2 : list) {
            String string = this.relativize(path, path2);
            List<LogEntry> parsed = this.parseFile(path2, string);
            if (nodeName != null && !nodeName.isEmpty()) {
                for (LogEntry e : parsed) {
                    e.nodeName = nodeName;
                }
            }
            arrayList.addAll(parsed);
        }
        arrayList.sort((logEntry, logEntry2) -> {
            int n = Long.compare(logEntry.timestampMs, logEntry2.timestampMs);
            if (n != 0) {
                return n;
            }
            int n2 = logEntry.location.compareTo(logEntry2.location);
            if (n2 != 0) {
                return n2;
            }
            return logEntry.message.compareTo(logEntry2.message);
        });
        return arrayList;
    }


    public List<LogEntry> scanWithNode(Path path, String[] exts, String nodeName) throws IOException {
        List<LogEntry> list = this.scan(path, exts, nodeName == null || nodeName.isEmpty() ? null : nodeName);
        if (nodeName == null || nodeName.isEmpty()) {
            for (LogEntry entry : list) {
                String inferred = Main.inferNode(entry.location);
                if (!inferred.isEmpty()) {
                    entry.nodeName = inferred;
                }
            }
        }
        return list;
    }

    private List<Path> collectFiles(Path path3, String[] stringArray) throws IOException {
        ArrayList<Path> arrayList = new ArrayList<Path>();
        Files.walk(path3, new FileVisitOption[0]).filter(path -> Files.isRegularFile(path, new LinkOption[0])).forEach(path2 -> {
            String string = path2.getFileName().toString().toLowerCase(Locale.ROOT);
            boolean bl = string.endsWith(".trc");
            if (bl && this.isUnderExcludedDir(path3, (Path)path2)) {
                return;
            }
            if (this.isUnderVarLog(path3, (Path)path2)) {
                String string2 = string;
                int n = string2.lastIndexOf(46);
                if (n > 0) {
                    string2 = string2.substring(0, n);
                }
                if (MESSAGE_FILE_NAMES.contains(string2)) {
                    arrayList.add((Path)path2);
                }
                return;
            }
            for (String string3 : stringArray) {
                if (!string.endsWith(string3.toLowerCase(Locale.ROOT))) continue;
                arrayList.add((Path)path2);
                return;
            }
            Object object = string;
            int n = ((String)object).lastIndexOf(46);
            if (n > 0) {
                object = ((String)object).substring(0, n);
            }
            if (MESSAGE_FILE_NAMES.contains(object)) {
                arrayList.add((Path)path2);
            }
        });
        return arrayList;
    }

    private boolean isUnderExcludedDir(Path path, Path path2) {
        try {
            Path path3 = path.relativize(path2);
            for (Path path4 : path3) {
                String string = path4.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!EXCLUDE_TRC_DIRS.contains(string)) continue;
                return true;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    private boolean isUnderVarLog(Path path, Path path2) {
        try {
            if (this.isVarLogDir(path)) {
                return true;
            }
            Path path3 = path.relativize(path2);
            for (int i = 0; i < path3.getNameCount(); ++i) {
                String string;
                Path path4 = path3.getName(i);
                String string2 = path4.getFileName().toString().toLowerCase(Locale.ROOT);
                if (!MESSAGE_ONLY_PARENTS.contains(string2) || i <= 0 || !"var".equals(string = path3.getName(i - 1).getFileName().toString().toLowerCase(Locale.ROOT))) continue;
                return true;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    private boolean isVarLogDir(Path path) {
        try {
            String string = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (!MESSAGE_ONLY_PARENTS.contains(string)) {
                return false;
            }
            Path path2 = path.getParent();
            if (path2 == null) {
                return false;
            }
            return "var".equals(path2.getFileName().toString().toLowerCase(Locale.ROOT));
        }
        catch (Exception exception) {
            return false;
        }
    }

    private String relativize(Path path, Path path2) {
        try {
            return path.relativize(path2).toString().replace('\\', '/');
        }
        catch (Exception exception) {
            return path2.getFileName().toString();
        }
    }

    List<LogEntry> parseFile(Path path, String string) throws IOException {
        ArrayList<LogEntry> arrayList = new ArrayList<LogEntry>();
        long l = 0L;
        String string2 = "";
        try (InputStream inputStream = Files.newInputStream(path, new OpenOption[0]);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader);){
            String string3;
            while ((string3 = bufferedReader.readLine()) != null) {
                boolean bl;
                String string4;
                String string5;
                long l2;
                if (string3.isEmpty()) continue;
                ParsedTs parsedTs = this.tryParseTimestamp(string3);
                if (parsedTs != null) {
                    l2 = parsedTs.epochMs;
                    string5 = parsedTs.text;
                    string4 = parsedTs.remainder;
                    bl = false;
                    l = l2;
                    string2 = string5;
                } else if (l != 0L) {
                    l2 = l;
                    string5 = string2;
                    string4 = string3;
                    bl = true;
                } else {
                    l2 = 0L;
                    string5 = "                  ";
                    string4 = string3;
                    bl = false;
                }
                LogEntry.Level level = this.detectLevel(string4);
                if (string5.isBlank() || string4.isBlank()) continue;
                String string11 = l2 > 0L ? formatUnified(l2) : "(无时间戳)";
                arrayList.add(new LogEntry(l2, string11, string, string4, level, bl));
            }
        }
        catch (IOException iOException) {
            throw iOException;
        }
        catch (Exception exception) {
            arrayList.add(new LogEntry(0L, "                  ", string, "[OracleLogVisualizer] Skipped file due to parse error: " + exception.getMessage(), LogEntry.Level.ERROR, false));
        }
        return arrayList;
    }

    ParsedTs tryParseTimestamp(String string) {
        Object object;
        Matcher matcher = CRS_TS.matcher(string);
        if (matcher.find()) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(matcher.group(1) + " " + matcher.group(2), CRS_FMT);
                long l = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                return new ParsedTs(l, matcher.group(1) + " " + matcher.group(2), matcher.group(3));
            }
            catch (DateTimeParseException dateTimeParseException) {
                // empty catch block
            }
        }
        if ((matcher = TRC_TS.matcher(string)).find()) {
            try {
                OffsetDateTime offsetDateTime;
                String string2 = matcher.group(1);
                String string3 = matcher.group(2);
                if (string3 == null) {
                    LocalDateTime localDateTime = LocalDateTime.parse(string2);
                    offsetDateTime = localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
                } else {
                    String string4;
                    String string5 = string4 = string3.endsWith("Z") || string3.contains(":") ? string3 : string3.substring(0, 3) + ":" + string3.substring(3);
                    if ("Z".equals(string4)) {
                        string4 = "+00:00";
                    }
                    offsetDateTime = OffsetDateTime.parse(string2 + string4);
                }
                return new ParsedTs(offsetDateTime.toInstant().toEpochMilli(), matcher.group(0).replaceFirst("^\\*\\*\\*\\s+", ""), "");
            }
            catch (DateTimeParseException dateTimeParseException) {
                // empty catch block
            }
        }
        if ((matcher = ISO_TS.matcher(string)).find()) {
            try {
                OffsetDateTime offsetDateTime;
                object = matcher.group(1);
                String string6 = matcher.group(2);
                if (string6 == null) {
                    LocalDateTime localDateTime = LocalDateTime.parse((CharSequence)object);
                    offsetDateTime = localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
                } else {
                    String string7;
                    String string8 = string7 = string6.endsWith("Z") || string6.contains(":") ? string6 : string6.substring(0, 3) + ":" + string6.substring(3);
                    if ("Z".equals(string7)) {
                        string7 = "+00:00";
                    }
                    offsetDateTime = OffsetDateTime.parse((String)object + string7);
                }
                return new ParsedTs(offsetDateTime.toInstant().toEpochMilli(), (String)object + (string6 == null ? "" : string6), matcher.group(3));
            }
            catch (DateTimeParseException dateTimeParseException) {
                // empty catch block
            }
        }
        if ((matcher = RDBMS_TS.matcher(string)).find()) {
            try {
                String rdbStr = matcher.group(1);
                // 跳过星期字段(日志里星期可能不准): "Sat Aug 12 10:00:01 2026" -> "Aug 12 10:00:01 2026"
                String noWeek = rdbStr.replaceFirst("^[A-Za-z]{3}\\s+", "");
                object = LocalDateTime.parse(noWeek, RDBMS_FMT);

                long l = ((LocalDateTime)object).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                return new ParsedTs(l, matcher.group(1), matcher.group(2));
            }
            catch (DateTimeParseException dateTimeParseException) {
                // empty catch block
            }
        }
        if ((matcher = LISTENER_TS.matcher(string)).find()) {
            try {
                object = LocalDateTime.parse(matcher.group(1), LISTENER_FMT);
                long l = ((LocalDateTime)object).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                return new ParsedTs(l, matcher.group(1), matcher.group(2));
            }
            catch (DateTimeParseException dateTimeParseException) {
                // empty catch block
            }
        }
        if ((matcher = SYSLOG_TS.matcher(string)).find()) {
            try {
                object = MONTH_MAP.get(matcher.group(1).toUpperCase(Locale.ROOT));
                if (object == null) {
                    throw new IllegalArgumentException("bad month: " + matcher.group(1));
                }
                int n = Integer.parseInt(matcher.group(2).trim());
                String string9 = matcher.group(3);
                String[] stringArray = string9.split(":");
                int n2 = Integer.parseInt(stringArray[0]);
                int n3 = Integer.parseInt(stringArray[1]);
                int n4 = Integer.parseInt(stringArray[2]);
                LocalDate localDate = LocalDate.now();
                int n5 = localDate.getYear();
                if (((Month)object).getValue() > localDate.getMonthValue()) {
                    --n5;
                }
                LocalDateTime localDateTime = LocalDateTime.of(n5, (Month)object, n, n2, n3, n4);
                long l = localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                String string10 = matcher.group(1) + " " + n + " " + string9;
                return new ParsedTs(l, string10, matcher.group(4));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return null;
    }

    public static String formatUnified(long epochMs) {
        return java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()).format(UNIFIED_FMT);
    }

    private LogEntry.Level detectLevel(String string) {
        if (string == null || string.isEmpty()) {
            return LogEntry.Level.INFO;
        }
        String string2 = string.toUpperCase(Locale.ROOT);
        for (String string3 : ERROR_KEYWORDS) {
            if (!string2.contains(string3.toUpperCase(Locale.ROOT))) continue;
            return LogEntry.Level.ERROR;
        }
        for (String string3 : WARNING_KEYWORDS) {
            if (!string2.contains(string3.toUpperCase(Locale.ROOT))) continue;
            return LogEntry.Level.WARNING;
        }
        return LogEntry.Level.INFO;
    }

    public void sort(List<LogEntry> list) {
        Collections.sort(list, (logEntry, logEntry2) -> Long.compare(logEntry.timestampMs, logEntry2.timestampMs));
    }

    static {
        String[] stringArray = new String[]{"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
        Month[] monthArray = Month.values();
        for (int i = 0; i < 12; ++i) {
            MONTH_MAP.put(stringArray[i], monthArray[i]);
        }
        ERROR_KEYWORDS = new String[]{"ERROR", "FATAL", "ORA-", "OFFLINE", "PANIC", "ASSERTION FAILURE", "MMON shutdown", "Instance terminated", "reconfiguration failed"};
        WARNING_KEYWORDS = new String[]{"WARNING", "WARN", "Deprecated", "CHECK"};
    }

    private static final class ParsedTs {
        final long epochMs;
        final String text;
        final String remainder;

        ParsedTs(long l, String string, String string2) {
            this.epochMs = l;
            this.text = string;
            this.remainder = string2;
        }
    }
}

