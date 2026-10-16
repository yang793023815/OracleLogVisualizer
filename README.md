# OracleLogVisualizer

Oracle RAC 日志可视化分析工具（Java Swing，单 jar，零依赖）。聚合打开一个目录下的 Oracle 日志 / trace 文件（alert log、CRS、trace、listener 等），统一解析时间戳、按时间排序，支持过滤、高亮、CSV 导出。

> 原版工具：Zhang Weizhao (weejar, [www.anbob.com](https://www.anbob.com))， oralog —— About 弹窗中保留原作者版权信息。
> 本仓库在原版反编译源码基础上做了中文本地化与多项增强（详见下文"本仓库增强"）。

## 使用

```bash
# 需要 Java 11+（源码使用了 Set.of / String.isBlank）
java -jar OracleLogVisualizer-cn.jar
```

或直接双击 `OracleLogVisualizer.command`（macOS）。

- `文件` → 打开目录 / 打开单个文件；`Ctrl+O` 打开、`Ctrl+F` 聚焦过滤框、`Ctrl+L` 清空过滤
- 支持的日志格式（时间戳自动识别）：alert log 英文格式（`Sat Aug 12 10:00:01 2026`）、CRS 启停记录、trace `***` 格式、ISO 格式、listener 格式
- 默认扫描时排除 `incident/`、`titanagent/`、`zabbix/` 子目录与 listener 相关 message 文件干扰

## 功能

| 区域 | 功能 |
|---|---|
| 过滤菜单 | 关键词高亮（逗号分隔、不区分大小写）、仅错误过滤、启动记录（CRS/DB/OS）、**时间范围过滤**、清空过滤 |
| 工具栏 | 消息检索 + **时间范围**（`开始,结束`，如 `2026-08-12 10:00:00,2026-08-12 11:30:00`；只填日期=整日；时间可省略到分钟） |
| 视图 | 按列排序（统一时间戳格式保证字典序正确）、跳到首/末行 |
| 文件 | 导出可见行到 CSV、重新加载上次目录 |

## 本仓库增强（相对原版）

1. **时间范围过滤**：工具栏输入框 + 菜单项；错误输入弹窗提示，状态栏实时显示生效范围；无时间戳行在过滤时自动隐藏
2. **RAC 多节点聚合分析**：打开上级目录时自动把每个一级子目录识别为节点（`rac1/racnode1/node1` 等命名），递归合并所有节点日志按时间排序；单目录打开时从文件相对路径推断节点（如 `alert_racnode1.log → racnode1`），表格新增"节点"列
3. **时间戳统一格式**：全部日志行统一为 `yyyy-MM-dd HH:mm:ss.SSS`（毫秒），跨格式可比较、排序正确；无时间戳行显示 `(无时间戳)`
4. **中文界面本地化** + 两处解析 bug 修复（英文 alert log 星期字段与实际日期不符时不抛异常；trace `***` 时间戳行同样修复）

## 构建

```bash
./build.sh    # 自动检测 javac（需 Java 11+，OpenJDK 17/21 已验证），产出 OracleLogVisualizer-cn.jar
```

源码位于 `src/com/weejar/oralog/`，共 5 个类 + 1 个本地化脚本（`cn_localize.py`，原版英文文案 → 中文的替换清单）。

## 目录结构

- `OracleLogVisualizer-cn.jar` —— 当前主程序（中文化 + 增强版）
- `OracleLogVisualizer.jar` —— 原版英文程序（供对照）
- `src/` —— 反编译 + 修复 + 中文化后的完整源码
- `manifest.txt`、`build.sh` —— 打包配置与构建脚本

## License

原版版权归 weejar (anbob.com) 所有；本地化与增强部分遵循原版发布条件。仅供个人使用，请勿商用。
