
import io
m = io.open('Main.java', encoding='utf-8').read()
tm = io.open('LogTableModel.java', encoding='utf-8').read()
R = [
 ('String APP_NAME = "Oracle RAC Log Visualizer"', 'String APP_NAME = "Oracle RAC 日志可视化工具"'),
 ('tableColumn.setHeaderValue("TimeStamp")', 'tableColumn.setHeaderValue("时间戳")'),
 ('tableColumn2.setHeaderValue("Location")', 'tableColumn2.setHeaderValue("来源文件")'),
 ('tableColumn3.setHeaderValue("Message")', 'tableColumn3.setHeaderValue("消息内容")'),
 ('"TimeStamp", "Location", "Message"', '"时间戳", "来源文件", "消息内容"'),
 ('new JMenu("File")', 'new JMenu("文件")'),
 ("new AbstractAction(\"Reload Last Directory\")", "new AbstractAction(\"重新加载上次目录\")"),
 ("new AbstractAction(\"Export Visible to CSV...\")", "new AbstractAction(\"导出可见行到CSV...\")"),
 ("new AbstractAction(\"Exit\")", "new AbstractAction(\"退出\")"),
 ('new JMenu("Filter")', 'new JMenu("过滤")'),
 ("new AbstractAction(\"Set Custom Highlight Keywords...\")", "new AbstractAction(\"设置自定义高亮关键词...\")"),
 ("new AbstractAction(\"Reset Custom Keywords\")", "new AbstractAction(\"重置自定义关键词\")"),
 ("new AbstractAction(\"Toggle Error-Only Filter\")", "new AbstractAction(\"切换仅错误过滤\")"),
 ("new AbstractAction(\"Startup Records (CRS/DB/OS)\")", "new AbstractAction(\"启动记录 (CRS/DB/OS)\")"),
 ("new AbstractAction(\"Clear Filter\")", "new AbstractAction(\"清空过滤\")"),
 ('new JMenu("View")', 'new JMenu("视图")'),
 ("new AbstractAction(\"Jump to First Row\")", "new AbstractAction(\"跳到第一行\")"),
 ("new AbstractAction(\"Jump to Last Row\")", "new AbstractAction(\"跳到最后一行\")"),
 ('new JMenu("Help")', 'new JMenu("帮助")'),
 ("new AbstractAction(\"About\")", "new AbstractAction(\"关于\")"),
 ("new AbstractAction(\"Open Directory...\")", "new AbstractAction(\"打开目录...\")"),
 ("new AbstractAction(\"Open Single File...\")", "new AbstractAction(\"打开单个文件...\")"),
 ('new JLabel("Search in Message:")', 'new JLabel("消息检索:")'),
 ('new JButton("Search")', 'new JButton("搜索")'),
 ('new JButton("Clear")', 'new JButton("清空")'),
 ('new JLabel(" Active Filter : ")', 'new JLabel(" 当前过滤: ")'),
 ('new JLabel("  (none)  ")', 'new JLabel("  (无)  ")'),
 ('new JLabel("Ready.")', 'new JLabel("就绪。")'),
 ('this.activeFilterLabel.setText("  (none)  ")', 'this.activeFilterLabel.setText("  (无)  ")'),
 ('"No previous directory has been opened yet."', '"尚未打开过任何目录。"'),
 ('"Choose a directory containing Oracle logs / traces"', '"选择包含Oracle日志/trace的目录"'),
 ('"Choose a single Oracle log / trace file"', '"选择单个Oracle日志/trace文件"'),
 ('"Failed to load file: "', '"加载文件失败: "'),
 ('"Not a directory: "', '"不是目录: "'),
 ('"Scanning " + String.valueOf(path) + " ..."', '"正在扫描 " + String.valueOf(path) + " ..."'),
 ('"Loading " + String.valueOf(path.getFileName()) + " ..."', '"正在加载 " + String.valueOf(path.getFileName()) + " ..."'),
 ('"Load cancelled. "', '"加载被取消。"'),
 ('" rows visible."', '" 行可见。"'),
 ('"Loaded " + Main.this.model.totalCount() + " rows. "', '"已加载 " + Main.this.model.totalCount() + " 行，"'),
 ('" match current filter."', '" 行匹配当前过滤。"'),
 ('"Total: " + this.model.totalCount() + "  Visible: " + this.model.visibleCount() + "  Filter: "', '"总计: " + this.model.totalCount() + "  可见: " + this.model.visibleCount() + "  过滤: "'),
 ('"Export visible rows to CSV"', '"导出可见行到CSV"'),
 ('"Exported " + n + " rows to "', '"已导出 " + n + " 行到 "'),
 ('"Failed to export: "', '"导出失败: "'),
 ('"Error"', '"错误"'),
 ('"Comma-separated keywords to highlight (case-insensitive):"', '"逗号分隔的高亮关键词（不区分大小写）:"'),
 ('"About Oracle RAC Log Visualizer"', '"关于 Oracle RAC 日志可视化工具"'),
]
cnt = 0
for old, new in R:
    if old in m:
        m = m.replace(old, new); cnt += 1
    else:
        print("MISS(main):", old)
tm2 = tm.replace('"TimeStamp", "Location", "Message"', '"时间戳", "来源文件", "消息内容"')
if tm2 == tm:
    print("MISS(tm): COLUMNS")
io.open('Main.java','w',encoding='utf-8').write(m)
io.open('LogTableModel.java','w',encoding='utf-8').write(tm2)
print("replaced:", cnt)
