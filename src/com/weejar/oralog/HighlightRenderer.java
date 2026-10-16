/*
 * Decompiled with CFR 0.152.
 */
package com.weejar.oralog;

import com.weejar.oralog.LogEntry;
import com.weejar.oralog.LogTableModel;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public final class HighlightRenderer
extends DefaultTableCellRenderer {
    private static final Color ERROR_BG = new Color(255, 200, 200);
    private static final Color ERROR_FG = new Color(128, 0, 0);
    private static final Color WARNING_BG = new Color(255, 245, 176);
    private static final Color WARNING_FG = new Color(96, 80, 0);
    private static final Color STICKY_BG = new Color(239, 244, 251);
    private final List<String> customKeywords = new ArrayList<String>();
    private Pattern customPattern;

    public HighlightRenderer() {
        this.setOpaque(true);
    }

    public void setCustomKeywords(String string) {
        this.customKeywords.clear();
        this.customPattern = null;
        if (string == null) {
            return;
        }
        for (String string2 : string.split(",")) {
            String string3 = string2.trim();
            if (string3.isEmpty()) continue;
            this.customKeywords.add(string3);
        }
        if (!this.customKeywords.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder("(");
            for (int i = 0; i < this.customKeywords.size(); ++i) {
                if (i > 0) {
                    stringBuilder.append("|");
                }
                stringBuilder.append(Pattern.quote(this.customKeywords.get(i)));
            }
            stringBuilder.append(")");
            try {
                this.customPattern = Pattern.compile(stringBuilder.toString(), 2);
            }
            catch (PatternSyntaxException patternSyntaxException) {
                this.customPattern = null;
            }
        }
    }

    public List<String> getCustomKeywords() {
        return Collections.unmodifiableList(this.customKeywords);
    }

    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object object, boolean bl, boolean bl2, int n, int n2) {
        super.getTableCellRendererComponent(jTable, object, bl, bl2, n, n2);
        LogTableModel logTableModel = (LogTableModel)jTable.getModel();
        LogEntry logEntry = logTableModel.entryAt(n);
        if (n2 == 0) {
            this.setHorizontalAlignment(2);
            this.setText(object == null ? "" : object.toString());
        } else if (n2 == 3) {
            this.setHorizontalAlignment(2);
            this.setText(this.highlightedHtml(object == null ? "" : object.toString()));
        } else {
            this.setHorizontalAlignment(2);
            this.setText(this.highlightedHtml(object == null ? "" : object.toString()));
        }
        if (bl) {
            this.setBackground(jTable.getSelectionBackground());
            this.setForeground(jTable.getSelectionForeground());
        } else {
            switch (logEntry.level) {
                case ERROR: {
                    this.setBackground(ERROR_BG);
                    this.setForeground(ERROR_FG);
                    break;
                }
                case WARNING: {
                    this.setBackground(WARNING_BG);
                    this.setForeground(WARNING_FG);
                    break;
                }
                default: {
                    this.setBackground(logEntry.sticky ? STICKY_BG : jTable.getBackground());
                    this.setForeground(jTable.getForeground());
                }
            }
        }
        return this;
    }

    private String highlightedHtml(String string) {
        Pattern pattern;
        if (string == null || string.isEmpty()) {
            return "";
        }
        String[] stringArray = new String[]{"ERROR", "FAIL", "ORA-", "OFFLINE", "FATAL", "Exception", "CRS-8500: Oracle Clusterware OHASD process is starting", "crash", "Starting ORACLE instance", "eviction", "misscount", "has been evicted", "reconfiguration", "OHASD exiting", "Reboot", "Number of voting files", "CLSGCHN", "SKGXP", "clssnmvDiskCheck: Aborting, 0 of", "numInterfaces 0", "Unable to discover any voting files", "clssscProcessKillShutdown: Initiating shutdown due to"};
        StringBuilder stringBuilder = new StringBuilder("(");
        for (int i = 0; i < stringArray.length; ++i) {
            if (i > 0) {
                stringBuilder.append("|");
            }
            stringBuilder.append(Pattern.quote(stringArray[i]));
        }
        if (this.customPattern != null) {
            stringBuilder.append("|").append(this.customPattern.pattern());
        }
        stringBuilder.append(")");
        try {
            pattern = Pattern.compile(stringBuilder.toString(), 2);
        }
        catch (PatternSyntaxException patternSyntaxException) {
            return HighlightRenderer.escape(string);
        }
        StringBuilder stringBuilder2 = new StringBuilder("<html><body style='white-space:nowrap'>");
        int n = 0;
        Matcher matcher = pattern.matcher(string);
        while (matcher.find()) {
            boolean bl;
            if (matcher.start() > n) {
                stringBuilder2.append(HighlightRenderer.escape(string.substring(n, matcher.start())));
            }
            String string2 = string.substring(matcher.start(), matcher.end());
            boolean bl2 = bl = this.customPattern != null && this.customPattern.matcher(string2).matches();
            if (bl) {
                stringBuilder2.append("<span style='background:#FFF59D;color:#000'>").append(HighlightRenderer.escape(string2)).append("</span>");
            } else {
                stringBuilder2.append("<span style='background:#FF8A80;color:#000;font-weight:bold'>").append(HighlightRenderer.escape(string2)).append("</span>");
            }
            n = matcher.end();
        }
        if (n < string.length()) {
            stringBuilder2.append(HighlightRenderer.escape(string.substring(n)));
        }
        stringBuilder2.append("</body></html>");
        return stringBuilder2.toString();
    }

    private static String escape(String string) {
        StringBuilder stringBuilder = new StringBuilder(string.length() + 16);
        block8: for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            switch (c) {
                case '<': {
                    stringBuilder.append("&lt;");
                    continue block8;
                }
                case '>': {
                    stringBuilder.append("&gt;");
                    continue block8;
                }
                case '&': {
                    stringBuilder.append("&amp;");
                    continue block8;
                }
                case '\"': {
                    stringBuilder.append("&quot;");
                    continue block8;
                }
                case '\n': {
                    stringBuilder.append("<br>");
                    continue block8;
                }
                case '\t': {
                    stringBuilder.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                    continue block8;
                }
                default: {
                    stringBuilder.append(c);
                }
            }
        }
        return stringBuilder.toString();
    }
}

