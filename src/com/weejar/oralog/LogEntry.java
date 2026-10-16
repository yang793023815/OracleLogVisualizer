/*
 * Decompiled with CFR 0.152, modified.
 */
package com.weejar.oralog;

import java.util.Objects;

public final class LogEntry {
    public final long timestampMs;
    public final String timestampStr;
    public final String location;
    public final String message;
    public final Level level;
    public final boolean sticky;
    public String nodeName; // 运行时由分析器填充, 非final

    public LogEntry(long l, String string, String string2, String string3, Level level, boolean bl) {
        this(l, string, string2, string3, level, bl, null);
    }

    public LogEntry(long l, String string, String string2, String string3, Level level, boolean bl, String node) {
        this.timestampMs = l;
        this.timestampStr = string;
        this.location = string2;
        this.message = string3;
        this.level = level;
        this.sticky = bl;
        this.nodeName = node;
    }

    public boolean equals(Object object) {
        if (!(object instanceof LogEntry)) {
            return false;
        }
        LogEntry logEntry = (LogEntry)object;
        return this.timestampMs == logEntry.timestampMs && Objects.equals(this.timestampStr, logEntry.timestampStr) && Objects.equals(this.location, logEntry.location) && Objects.equals(this.message, logEntry.message);
    }

    public int hashCode() {
        return Objects.hash(this.timestampMs, this.timestampStr, this.location, this.message);
    }

    public static enum Level {
        INFO,
        WARNING,
        ERROR;

    }
}
