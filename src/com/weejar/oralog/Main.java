/*
 * Decompiled with CFR 0.152.
 */
package com.weejar.oralog;

import com.weejar.oralog.HighlightRenderer;
import com.weejar.oralog.LogEntry;
import com.weejar.oralog.LogParser;
import com.weejar.oralog.LogTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

public final class Main
extends JFrame {
    private static final String APP_NAME = "Oracle RAC 日志可视化工具";
    private final LogTableModel model = new LogTableModel();
    private final HighlightRenderer renderer = new HighlightRenderer();
    private final JTable table;
    private final JTextField filterField = new JTextField(40);
    private JTextField timeRangeField;
    private final JLabel activeFilterLabel = new JLabel("  (无)  ");
    private final JLabel statusLabel = new JLabel("就绪。");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final Preferences prefs = Preferences.userNodeForPackage(Main.class);
    private SwingWorker<Void, ProgressTick> currentLoad;

    public Main() {
        super(APP_NAME);
        this.setDefaultCloseOperation(3);
        this.setSize(1280, 760);
        this.setMinimumSize(new Dimension(900, 500));
        this.setLocationRelativeTo(null);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception exception) {
            // empty catch block
        }
        this.table = this.buildTable();
        this.setJMenuBar(this.buildMenuBar());
        this.setContentPane(this.buildContent());
        this.installFilterListeners();
    }

    private JTable buildTable() {
        JTable jTable = new JTable(this.model);
        jTable.setAutoCreateColumnsFromModel(false);
        jTable.setRowSelectionAllowed(true);
        jTable.setColumnSelectionAllowed(false);
        jTable.setSelectionMode(1);
        jTable.setShowGrid(false);
        jTable.setIntercellSpacing(new Dimension(0, 0));
        jTable.setFont(new Font("Monospaced", 0, 12));
        jTable.setAutoResizeMode(0);
        DefaultTableColumnModel defaultTableColumnModel = new DefaultTableColumnModel();
        TableColumn tableColumn = new TableColumn(0);
        tableColumn.setHeaderValue("时间戳");
        tableColumn.setPreferredWidth(180);
        tableColumn.setMinWidth(120);
        TableColumn tableNode = new TableColumn(1);
        tableNode.setHeaderValue("节点");
        tableNode.setPreferredWidth(90);
        tableNode.setMinWidth(50);
        TableColumn tableColumn2 = new TableColumn(2);
        tableColumn2.setHeaderValue("来源文件");
        tableColumn2.setPreferredWidth(280);
        tableColumn2.setMinWidth(160);
        TableColumn tableColumn3 = new TableColumn(3);
        tableColumn3.setHeaderValue("消息内容");
        tableColumn3.setPreferredWidth(780);
        tableColumn3.setMinWidth(300);
        defaultTableColumnModel.addColumn(tableColumn);
        defaultTableColumnModel.addColumn(tableNode);
        defaultTableColumnModel.addColumn(tableColumn2);
        defaultTableColumnModel.addColumn(tableColumn3);
        jTable.setColumnModel(defaultTableColumnModel);
        for (int i = 0; i < defaultTableColumnModel.getColumnCount(); ++i) {
            defaultTableColumnModel.getColumn(i).setCellRenderer(this.renderer);
        }
        TableRowSorter<LogTableModel> tableRowSorter = new TableRowSorter<LogTableModel>(this.model);
        tableRowSorter.setComparator(0, (object, object2) -> {
            if (object == null || object2 == null) {
                return 0;
            }
            return object.toString().compareTo(object2.toString());
        });
        jTable.setRowSorter(tableRowSorter);
        return jTable;
    }

    private JMenuBar buildMenuBar() {
        JMenuBar jMenuBar = new JMenuBar();
        JMenu jMenu = new JMenu("文件");
        jMenu.setMnemonic('F');
        jMenu.add(new JMenuItem(this.openDirectoryAction()));
        jMenu.add(new JMenuItem(this.openFileAction()));
        jMenu.add(new JSeparator());
        jMenu.add(new JMenuItem(new AbstractAction("重新加载上次目录"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String string = Main.this.prefs.get("lastDir", null);
                if (string == null) {
                    Main.this.showInfo("尚未打开过任何目录。");
                    return;
                }
                Main.this.openDirectory(Paths.get(string, new String[0]));
            }
        }));
        jMenu.add(new JMenuItem(new AbstractAction("导出可见行到CSV..."){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.this.exportVisibleToCsv();
            }
        }));
        jMenu.add(new JSeparator());
        jMenu.add(new JMenuItem(new AbstractAction("退出"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (Main.this.currentLoad != null && !Main.this.currentLoad.isDone()) {
                    Main.this.currentLoad.cancel(true);
                }
                Main.this.dispose();
                System.exit(0);
            }
        }));
        jMenuBar.add(jMenu);
        JMenu jMenu2 = new JMenu("过滤");
        jMenu2.setMnemonic('l');
        jMenu2.add(new JMenuItem(new AbstractAction("设置自定义高亮关键词..."){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String string = String.join((CharSequence)",", Main.this.renderer.getCustomKeywords());
                String string2 = JOptionPane.showInputDialog(Main.this, "逗号分隔的高亮关键词（不区分大小写）:", string);
                if (string2 != null) {
                    Main.this.renderer.setCustomKeywords(string2);
                    Main.this.table.repaint();
                }
            }
        }));
        jMenu2.add(new JMenuItem(new AbstractAction("重置自定义关键词"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.this.renderer.setCustomKeywords("");
                Main.this.table.repaint();
            }
        }));
        jMenu2.add(new JSeparator());
        jMenu2.add(new JMenuItem(new AbstractAction("切换仅错误过滤"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String string = Main.this.filterField.getText();
                if (string.contains("ERROR") || string.contains("ORA-")) {
                    Main.this.filterField.setText("");
                } else {
                    Main.this.filterField.setText("ERROR|ORA-|FATAL");
                }
            }
        }));
        jMenu2.add(new JMenuItem(new AbstractAction("启动记录 (CRS/DB/OS)"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String string = "CRS-8500: Oracle Clusterware OHASD process is starting|Starting ORACLE instance|kernel: Initializing cgroup subsys";
                String string2 = Main.this.filterField.getText();
                if (string2.contains("CRS-8500") || string2.contains("Starting ORACLE")) {
                    Main.this.filterField.setText("");
                } else {
                    Main.this.filterField.setText(string);
                }
            }
        }));
        jMenu2.add(new JMenuItem(new AbstractAction("时间范围过滤..."){
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String string2 = Main.this.model.getTimeRange();
                String string3 = JOptionPane.showInputDialog(Main.this, "输入时间范围（开始,结束），如:\n2026-08-12 10:00:00,2026-08-12 11:30:00\n时间部分可省略到分钟；只给日期表示整天。留空取消。", string2);
                if (string3 != null) {
                    Main.this.timeRangeField.setText(string3);
                    Main.this.applyTimeFilter();
                }
            }
        }));
        jMenu2.add(new JMenuItem(new AbstractAction("清空过滤"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.this.filterField.setText("");
            }
        }));
        jMenuBar.add(jMenu2);
        JMenu jMenu3 = new JMenu("视图");
        jMenu3.setMnemonic('V');
        jMenu3.add(new JMenuItem(new AbstractAction("跳到第一行"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (Main.this.table.getRowCount() > 0) {
                    Main.this.scrollToRow(0);
                    Main.this.table.setRowSelectionInterval(0, 0);
                }
            }
        }));
        jMenu3.add(new JMenuItem(new AbstractAction("跳到最后一行"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int n = Main.this.table.getRowCount();
                if (n > 0) {
                    Main.this.scrollToRow(n - 1);
                    Main.this.table.setRowSelectionInterval(n - 1, n - 1);
                }
            }
        }));
        jMenuBar.add(jMenu3);
        JMenu jMenu4 = new JMenu("帮助");
        jMenu4.setMnemonic('H');
        jMenu4.add(new JMenuItem(new AbstractAction("关于"){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                JOptionPane.showMessageDialog(Main.this, "<html><body style='width:340px'><h3>Oracle RAC Log Visualizer</h3><p>Aggregates Oracle ADRCI / RAC trace files from a chosen directory, sorts them by timestamp and lets you filter and highlight entries.</p><p><b>Author:</b> Zhang Weizhao</p><p><b>Copyright:</b> <a href='https://www.anbob.com'>www.anbob.com</a></p><p><b>Contact:</b> <a href='mailto:weejar@gmail.com'>weejar@gmail.com</a></p><hr><p><b>Shortcuts:</b> Ctrl+O = open directory, Ctrl+F = focus filter, Ctrl+L = clear filter.</p></body></html>", "关于 Oracle RAC 日志可视化工具", 1);
            }
        }));
        jMenuBar.add(jMenu4);
        return jMenuBar;
    }

    private Action openDirectoryAction() {
        AbstractAction abstractAction = new AbstractAction("打开目录..."){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.this.chooseAndOpenDirectory();
            }
        };
        abstractAction.putValue("AcceleratorKey", KeyStroke.getKeyStroke("ctrl O"));
        return abstractAction;
    }

    private Action openFileAction() {
        AbstractAction abstractAction = new AbstractAction("打开单个文件..."){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.this.chooseAndOpenFile();
            }
        };
        abstractAction.putValue("AcceleratorKey", KeyStroke.getKeyStroke("ctrl shift O"));
        return abstractAction;
    }

    private JPanel buildContent() {
        JPanel jPanel = new JPanel(new BorderLayout());
        JPanel jPanel2 = new JPanel();
        jPanel2.setLayout(new BoxLayout(jPanel2, 2));
        jPanel2.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JLabel jLabel = new JLabel("消息检索:");
        jLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        JButton jButton = new JButton("搜索");
        jButton.addActionListener(actionEvent -> this.applyFilter());
        JButton jButton2 = new JButton("清空");
        jButton2.addActionListener(actionEvent -> this.filterField.setText(""));
        JLabel jLabelTime = new JLabel("时间范围:");
        jLabelTime.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));
        this.timeRangeField = new JTextField(28);
        this.timeRangeField.setToolTipText("格式: 开始,结束 例: 2026-08-12 10:00:00,2026-08-12 11:30:00。只填日期则包含整日。留空=不过滤");
        JButton jButtonTime = new JButton("时间过滤");
        jButtonTime.addActionListener(actionEvent -> this.applyTimeFilter());
        JLabel jLabel2 = new JLabel(" 当前过滤: ");
        this.activeFilterLabel.setForeground(new Color(26, 79, 139));
        jPanel2.add(jLabel);
        jPanel2.add(this.filterField);
        jPanel2.add(Box.createHorizontalStrut(4));
        jPanel2.add(jButton);
        jPanel2.add(Box.createHorizontalStrut(4));
        jPanel2.add(jButton2);
        jPanel2.add(Box.createHorizontalStrut(12));
        jPanel2.add(jLabelTime);
        jPanel2.add(this.timeRangeField);
        jPanel2.add(Box.createHorizontalStrut(4));
        jPanel2.add(jButtonTime);
        jPanel2.add(Box.createHorizontalStrut(12));
        jPanel2.add(jLabel2);
        jPanel2.add(this.activeFilterLabel);
        JPanel jPanel3 = new JPanel(new BorderLayout());
        jPanel3.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        this.statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        this.progress.setPreferredSize(new Dimension(160, 14));
        this.progress.setVisible(false);
        jPanel3.add((Component)this.statusLabel, "Center");
        jPanel3.add((Component)this.progress, "East");
        JScrollPane jScrollPane = new JScrollPane(this.table);
        jScrollPane.setHorizontalScrollBarPolicy(30);
        jScrollPane.setVerticalScrollBarPolicy(22);
        jPanel.add((Component)jPanel2, "North");
        jPanel.add((Component)jScrollPane, "Center");
        jPanel.add((Component)jPanel3, "South");
        return jPanel;
    }

    private void installFilterListeners() {
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke("ctrl F"), "focusFilter");
        this.getRootPane().getActionMap().put("focusFilter", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.this.filterField.requestFocusInWindow();
            }
        });
        this.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke("ctrl L"), "clearFilter");
        this.getRootPane().getActionMap().put("clearFilter", new AbstractAction(){

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Main.this.filterField.setText("");
                Main.this.timeRangeField.setText("");
                Main.this.applyTimeFilter();
            }
        });
        this.filterField.getDocument().addDocumentListener(new DocumentListener(){
            private long lastEdit = 0L;

            private void maybeApply() {
                long l;
                this.lastEdit = l = System.currentTimeMillis();
                Timer timer = new Timer(180, actionEvent -> {
                    if (System.currentTimeMillis() - this.lastEdit >= 170L) {
                        Main.this.applyFilter();
                    }
                });
                timer.setRepeats(false);
                timer.start();
            }

            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                this.maybeApply();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                this.maybeApply();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                this.maybeApply();
            }
        });
        this.filterField.addActionListener(actionEvent -> this.applyFilter());
    }

    private void applyTimeFilter() {
        String string = this.timeRangeField.getText();
        String err = this.model.setTimeRange(string);
        if (err != null) {
            this.showError(err);
            return;
        }
        String t = this.model.getTimeRange();
        if (!t.isEmpty()) {
            this.activeFilterLabel.setText(" 时间:[" + t + "]" + (this.model.getFilterText().isEmpty() ? "" : " 消息:[" + this.model.getFilterText() + "]"));
        } else if (this.model.getFilterText().isEmpty()) {
            this.activeFilterLabel.setText("  (无)  ");
        } else {
            this.activeFilterLabel.setText(" 消息:[" + this.model.getFilterText() + "] ");
        }
        this.updateStatus();
        this.table.repaint();
    }

    private void applyFilter() {
        String string = this.filterField.getText();
        this.model.setFilterText(string);
        if (string == null || string.trim().isEmpty()) {
            this.activeFilterLabel.setText("  (无)  ");
        } else {
            this.activeFilterLabel.setText(" " + string + " ");
        }
        this.updateStatus();
        this.table.repaint();
    }

    private void chooseAndOpenDirectory() {
        File file;
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setFileSelectionMode(1);
        jFileChooser.setDialogTitle("选择包含Oracle日志/trace的目录");
        String string = this.prefs.get("lastDir", null);
        if (string != null && (file = new File(string)).exists()) {
            jFileChooser.setSelectedFile(file);
        }
        if (jFileChooser.showOpenDialog(this) == 0 && (file = jFileChooser.getSelectedFile()) != null) {
            this.openDirectory(file.toPath());
        }
    }

    private void chooseAndOpenFile() {
        File file;
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setFileSelectionMode(0);
        jFileChooser.setDialogTitle("选择单个Oracle日志/trace文件");
        String string = this.prefs.get("lastFile", null);
        if (string != null && (file = new File(string)).exists()) {
            jFileChooser.setSelectedFile(file);
        }
        if (jFileChooser.showOpenDialog(this) == 0 && (file = jFileChooser.getSelectedFile()) != null) {
            try {
                this.openSingleFile(file.toPath());
            }
            catch (Exception exception) {
                this.showError("加载文件失败: " + exception.getMessage());
            }
        }
    }

    private void openDirectory(Path path) {
        if (path == null || !Files.isDirectory(path, new LinkOption[0])) {
            this.showError("不是目录: " + String.valueOf(path));
            return;
        }
        this.prefs.put("lastDir", path.toAbsolutePath().toString());
        this.model.setEntries(Collections.emptyList());
        this.setBusy(true, "正在扫描 " + String.valueOf(path) + " ...");
        this.startLoad(() -> new LogParser().scan(path, LogParser.DEFAULT_EXTENSIONS, null));
    }

    private void openSingleFile(Path path) throws Exception {
        this.model.setEntries(Collections.emptyList());
        this.setBusy(true, "正在加载 " + String.valueOf(path.getFileName()) + " ...");
        this.startLoad(() -> {
            LogParser logParser = new LogParser();
            List<LogEntry> list = logParser.parseFile(path, path.getFileName().toString());
            list.sort((logEntry, logEntry2) -> Long.compare(logEntry.timestampMs, logEntry2.timestampMs));
            return list;
        });
    }

    private void startLoad(final LoadSupplier loadSupplier) {
        if (this.currentLoad != null && !this.currentLoad.isDone()) {
            this.currentLoad.cancel(true);
        }
        this.setCursor(Cursor.getPredefinedCursor(3));
        this.currentLoad = new SwingWorker<Void, ProgressTick>(){

            @Override
            protected Void doInBackground() throws Exception {
                SwingUtilities.invokeLater(() -> Main.this.progress.setIndeterminate(true));
                List<LogEntry> list = loadSupplier.produce();
                list.sort((logEntry, logEntry2) -> {
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
                int n = list.size();
                int n2 = Math.max(2000, n / 50);
                for (int i = 0; i < n; i += n2) {
                    if (this.isCancelled()) {
                        return null;
                    }
                    int n3 = Math.min(n, i + n2);
                    List<LogEntry> list2 = list.subList(i, n3);
                    this.publish(new ProgressTick(list2, n3, n));
                }
                return null;
            }

            @Override
            protected void process(List<ProgressTick> list) {
                SwingUtilities.invokeLater(() -> {
                    Main.this.progress.setIndeterminate(false);
                    for (ProgressTick progressTick : list) {
                        Main.this.model.appendEntries(progressTick.rows);
                        Main.this.progress.setMaximum(progressTick.total);
                        Main.this.progress.setValue(progressTick.processed);
                    }
                    Main.this.updateStatus();
                });
            }

            @Override
            protected void done() {
                Main.this.setCursor(Cursor.getDefaultCursor());
                Main.this.progress.setVisible(false);
                Main.this.progress.setIndeterminate(false);
                Main.this.updateStatus();
                if (this.isCancelled()) {
                    Main.this.statusLabel.setText("加载被取消。" + Main.this.model.visibleCount() + " of " + Main.this.model.totalCount() + " 行可见。");
                } else {
                    Main.this.statusLabel.setText("已加载 " + Main.this.model.totalCount() + " 行，" + Main.this.model.visibleCount() + " 行匹配当前过滤。");
                }
            }
        };
        this.currentLoad.execute();
    }

    private void setBusy(boolean bl, String string) {
        this.progress.setVisible(bl);
        this.progress.setIndeterminate(bl);
        this.statusLabel.setText(string);
    }

    private void updateStatus() {
        this.statusLabel.setText("总计: " + this.model.totalCount() + "  可见: " + this.model.visibleCount()
            + "  过滤: " + (this.model.getFilterText().isEmpty() ? "(无)" : this.model.getFilterText())
            + "  时间: " + (this.model.hasTimeRange() ? "[" + this.model.getTimeRange() + "]" : "(全部)"));
    }

    private void showInfo(String string) {
        JOptionPane.showMessageDialog(this, string);
    }

    private void showError(String string) {
        JOptionPane.showMessageDialog(this, string, "错误", 0);
    }

    private void scrollToRow(int n) {
        if (n < 0) {
            return;
        }
        Rectangle rectangle = this.table.getCellRect(n, 0, true);
        rectangle.x = 0;
        rectangle.width = this.table.getParent().getWidth();
        this.table.scrollRectToVisible(rectangle);
    }


    /*
     * 从文件相对路径/文件名推断 RAC 节点名。
     * 策略: 路径中出现 "nodeN"/"racN"/"racnodeN"/"serverN"/host 字样直接用；
     *       文件名含 "1"/"2" 如 alert_rac1.log / messages-rac1 / ocr1 等取后缀编号；
     *       否则返回整个第一级目录名作为节点标识。
     */
    public static String inferNode(String relPath) {
        if (relPath == null || relPath.isEmpty()) {
            return "";
        }
        String lower = relPath.toLowerCase(java.util.Locale.ROOT);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "(?:^|[/\\_-])(rac|racnode|node|host|server|sys|db|prod|ora)(\\d{1,2})(?:[/\\._-]|$)").matcher(lower);
        if (m.find()) {
            return m.group(1) + m.group(2);
        }
        //Gi 目录结构: .../<节点名>/.../log  -> 取第一级目录
        String[] segs = relPath.split("[/\\\\]");
        if (segs.length > 1) {
            String first = segs[0].trim();
            // 剔除常见非节点目录
            String fl = first.toLowerCase(java.util.Locale.ROOT);
            if (!fl.equals("var") && !fl.equals("log") && !fl.equals("logs") && !fl.equals("trace")
                && !fl.equals("traces") && !fl.equals("tmp") && !fl.equals("crs") && !fl.equals("rdbms")
                && !fl.equals("grid") && !fl.equals("oracle") && !fl.equals("diag")) {
                return first;
            }
        }
        String fname = segs[segs.length - 1];
        // 文件名带 _1 / -1 / 1.log 后缀编号
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("_+(\\d{1,2})\\.").matcher(fname);
        if (m2.find()) {
            return "node" + m2.group(1);
        }
        return "";
    }

    /*
     * 打开上级目录: 每个第一级子目录视为一个节点, 若无子目录则单目录模式。
     */
    private void openDirectoryMultiNode(Path path) {
        if (path == null || !Files.isDirectory(path, new LinkOption[0])) {
            this.showError("不是目录: " + String.valueOf(path));
            return;
        }
        this.prefs.put("lastDir", path.toAbsolutePath().toString());
        this.model.setEntries(Collections.emptyList());
        // 收集子目录
        List<Path> children = new java.util.ArrayList<Path>();
        try (java.util.stream.Stream<Path> st = Files.list(path)) {
            st.filter(p2 -> Files.isDirectory(p2, new LinkOption[0])).sorted().forEach(children::add);
        }
        catch (IOException e) {
            this.showError("读取目录失败: " + e.getMessage());
            return;
        }
        this.setBusy(true, "正在扫描 " + String.valueOf(path) + " (多节点: " + children.size() + " 个子目录) ...");
        final List<Path> dirs = children.isEmpty() ? java.util.Collections.singletonList(path) : children;
        this.startLoad(() -> {
            LogParser logParser = new LogParser();
            List<LogEntry> list = new java.util.ArrayList<LogEntry>();
            for (Path dir : dirs) {
                String nodeName = children.isEmpty() ? "" : dir.getFileName().toString();
                list.addAll(logParser.scanWithNode(dir, LogParser.DEFAULT_EXTENSIONS, nodeName));
            }
            return list;
        });
    }

    private void exportVisibleToCsv() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setDialogTitle("导出可见行到CSV");
        jFileChooser.setSelectedFile(new File("oracle-logs.csv"));
        if (jFileChooser.showSaveDialog(this) != 0) {
            return;
        }
        File file = jFileChooser.getSelectedFile();
        try (PrintWriter printWriter = new PrintWriter(file, "UTF-8");){
            printWriter.println("TimeStamp,Location,Message");
            int n = this.model.getRowCount();
            for (int i = 0; i < n; ++i) {
                LogEntry logEntry = this.model.entryAt(i);
                printWriter.print(Main.csv(logEntry.timestampStr));
                printWriter.print(',');
                printWriter.print(Main.csv(logEntry.location));
                printWriter.print(',');
                printWriter.println(Main.csv(logEntry.message));
            }
            this.showInfo("已导出 " + n + " 行到 " + file.getAbsolutePath());
        }
        catch (IOException iOException) {
            this.showError("导出失败: " + iOException.getMessage());
        }
    }

    private static String csv(String string) {
        if (string == null) {
            return "\"\"";
        }
        boolean bl = string.indexOf(44) >= 0 || string.indexOf(34) >= 0 || string.indexOf(10) >= 0 || string.indexOf(13) >= 0;
        String string2 = string.replace("\"", "\"\"");
        return bl ? "\"" + string2 + "\"" : string2;
    }

    public static void main(String[] stringArray) {
        SwingUtilities.invokeLater(() -> {
            Main main = new Main();
            main.setVisible(true);
            if (stringArray.length >= 1) {
                Path path = Paths.get(stringArray[0], new String[0]);
                if (Files.isDirectory(path, new LinkOption[0])) {
                    main.openDirectory(path);
                } else if (Files.isRegularFile(path, new LinkOption[0])) {
                    try {
                        main.openSingleFile(path);
                    }
                    catch (Exception exception) {
                        main.showError(exception.getMessage());
                    }
                }
            }
        });
    }

    private static interface LoadSupplier {
        public List<LogEntry> produce() throws Exception;
    }

    private static final class ProgressTick {
        final List<LogEntry> rows;
        final int processed;
        final int total;

        ProgressTick(List<LogEntry> list, int n, int n2) {
            this.rows = list;
            this.processed = n;
            this.total = n2;
        }
    }
}

