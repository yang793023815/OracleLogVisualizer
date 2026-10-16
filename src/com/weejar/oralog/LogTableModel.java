/*
 * Decompiled with CFR 0.152, modified.
 */
package com.weejar.oralog;

import com.weejar.oralog.LogEntry;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.table.AbstractTableModel;

public final class LogTableModel
extends AbstractTableModel {
    private static final String[] COLUMNS = new String[]{"时间戳", "节点", "来源文件", "消息内容"};
    private final List<LogEntry> all = new ArrayList<LogEntry>();
    private final List<LogEntry> visible = new ArrayList<LogEntry>();
    private String filterText = "";
    private Pattern filterPattern;
    private boolean caseInsensitive = true;
    private long rangeFromMs = Long.MIN_VALUE;
    private long rangeToMs = Long.MAX_VALUE;
    private String rangeText = "";

    public void setEntries(List<LogEntry> list) {
        this.all.clear();
        this.visible.clear();
        if (list != null) {
            this.all.addAll(list);
        }
        this.rebuildVisible();
        this.fireTableDataChanged();
    }

    public void appendEntries(List<LogEntry> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        int n = this.all.size();
        this.all.addAll(list);
        if (this.filterPattern == null && !this.hasTimeRange()) {
            int n2 = this.all.size();
            this.visible.addAll(this.all.subList(n, n2));
            this.fireTableRowsInserted(this.visible.size() - (n2 - n), this.visible.size() - 1);
        } else {
            for (int i = n; i < this.all.size(); ++i) {
                LogEntry logEntry = this.all.get(i);
                if (!this.matches(logEntry)) continue;
                this.visible.add(logEntry);
            }
            this.fireTableDataChanged();
        }
    }

    public int totalCount() {
        return this.all.size();
    }

    public int visibleCount() {
        return this.visible.size();
    }

    public LogEntry entryAt(int n) {
        return this.visible.get(n);
    }

    public void setFilterText(String string) {
        String string2;
        String string3 = string2 = string == null ? "" : string.trim();
        if (string2.equals(this.filterText)) {
            return;
        }
        this.filterText = string2;
        Pattern pattern = null;
        if (!string2.isEmpty()) {
            try {
                int n = this.caseInsensitive ? 2 : 0;
                pattern = Pattern.compile(string2, n);
            }
            catch (PatternSyntaxException patternSyntaxException) {
                return;
            }
        }
        this.filterPattern = pattern;
        this.rebuildVisible();
        this.fireTableDataChanged();
    }

    public String getFilterText() {
        return this.filterText;
    }

    public boolean isCaseInsensitive() {
        return this.caseInsensitive;
    }

    public void setCaseInsensitive(boolean bl) {
        this.caseInsensitive = bl;
    }

    /*
     * 设置时间范围过滤。格式: "开始,结束"，如 "2026-08-12 10:00:00,2026-08-12 11:00:00"
     * 时间部分可省略到分钟；只给日期时 开始=当天00:00:00 结束=当天23:59:59.999
     * 只填一边也允许。返回 null=成功，否则为错误提示。
     */
    public String setTimeRange(String text) {
        String s = text == null ? "" : text.trim();
        if (s.isEmpty()) {
            this.rangeFromMs = Long.MIN_VALUE;
            this.rangeToMs = Long.MAX_VALUE;
            this.rangeText = "";
            this.rebuildVisible();
            this.fireTableDataChanged();
            return null;
        }
        String[] parts = s.split("[,，;]");
        if (parts.length > 2) {
            return "格式错误，请用 '开始,结束' 两个时间点（逗号分隔）";
        }
        Long from = null;
        Long to = null;
        try {
            if (!parts[0].trim().isEmpty()) {
                from = parseRangePoint(parts[0].trim(), false);
            }
            if (parts.length == 2 && !parts[1].trim().isEmpty()) {
                to = parseRangePoint(parts[1].trim(), true);
            }
        }
        catch (Exception e) {
            return "时间格式错误（示例: 2026-08-12 10:00:00,2026-08-12 11:30:00）";
        }
        if (from != null && to != null && from > to) {
            return "开始时间晚于结束时间";
        }
        this.rangeFromMs = from == null ? Long.MIN_VALUE : from;
        this.rangeToMs = to == null ? Long.MAX_VALUE : to;
        this.rangeText = s;
        this.rebuildVisible();
        this.fireTableDataChanged();
        return null;
    }

    public String getTimeRange() {
        return this.rangeText;
    }

    public boolean hasTimeRange() {
        return this.rangeFromMs != Long.MIN_VALUE || this.rangeToMs != Long.MAX_VALUE;
    }

    /*
     * 支持 "yyyy-MM-dd HH:mm:ss"、"yyyy-MM-dd HH:mm"、"yyyy-MM-dd"、"yyyy/MM/dd..."、日期带 'T'
     */
    private static Long parseRangePoint(String s, boolean isEnd) throws DateTimeParseException {
        String norm = s.replace('/', '-').replace('T', ' ').trim();
        norm = norm.replaceAll(" +", " ");
        if (!norm.contains(":")) {
            // 纯日期: yyyy-MM-dd
            java.time.LocalDate ld;
            if (norm.contains("-")) {
                String[] dp = norm.split("-");
                if (dp.length != 3) {
                    throw new DateTimeParseException("日期格式应为 yyyy-MM-dd", norm, 0);
                }
                ld = java.time.LocalDate.of(Integer.parseInt(dp[0].trim()), Integer.parseInt(dp[1].trim()), Integer.parseInt(dp[2].trim()));
            } else {
                throw new DateTimeParseException("日期格式应为 yyyy-MM-dd", norm, 0);
            }
            if (isEnd) {
                return ld.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1;
            }
            return ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]");
        LocalDateTime dt = LocalDateTime.parse(norm, f);
        return dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void rebuildVisible() {
        this.visible.clear();
        if (this.filterPattern == null && !this.hasTimeRange()) {
            this.visible.addAll(this.all);
        } else {
            for (LogEntry logEntry : this.all) {
                if (!this.matches(logEntry)) continue;
                this.visible.add(logEntry);
            }
        }
    }

    private boolean matches(LogEntry logEntry) {
        if (this.hasTimeRange()) {
            if (logEntry.timestampMs == 0L) {
                return false;
            }
            if (logEntry.timestampMs < this.rangeFromMs || logEntry.timestampMs > this.rangeToMs) {
                return false;
            }
        }
        if (this.filterPattern == null) {
            return true;
        }
        if (this.filterPattern.matcher(logEntry.message).find()) {
            return true;
        }
        if (this.filterPattern.matcher(logEntry.location).find()) {
            return true;
        }
        return this.filterPattern.matcher(logEntry.timestampStr).find();
    }

    @Override
    public int getRowCount() {
        return this.visible.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int n) {
        return COLUMNS[n];
    }

    @Override
    public Object getValueAt(int n, int n2) {
        if (n < 0 || n >= this.visible.size()) {
            return "";
        }
        LogEntry logEntry = this.visible.get(n);
        switch (n2) {
            case 0: {
                return logEntry.timestampStr;
            }
            case 1: {
                return logEntry.nodeName == null ? "" : logEntry.nodeName;
            }
            case 2: {
                return logEntry.location;
            }
            case 3: {
                return logEntry.message;
            }
        }
        return "";
    }

    @Override
    public boolean isCellEditable(int n, int n2) {
        return false;
    }
}
