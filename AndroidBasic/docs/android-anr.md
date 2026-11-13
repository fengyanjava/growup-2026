# Android ANR：问题复习

整理日期：2026-11-13。根据本轮系统 ANR 的记录、上报、分析及历史退出信息追问整理。尚未展开的应用内心跳监控不单独收录，本篇以系统现场和历史退出记录为主；项目未新增 ANR 监控代码。

## 1. 系统发生 ANR 后，如何记录并上报？

**系统采集现场，应用读取并保存记录，再由后台任务上报。** ANR 不是 Java 异常，不能依靠 `UncaughtExceptionHandler` 捕获。

Android 11（API 30）起，可以通过 `ActivityManager.getHistoricalProcessExitReasons()` 读取最近的进程退出记录。常见补报时机是应用下次启动后，在后台查询：

```kotlin
// API 30+；省略 ActivityManager 的获取和 import，查询与 trace 读取放在后台。
val records = activityManager.getHistoricalProcessExitReasons(
    packageName,
    0,  // 不限定历史 PID
    10  // 最多取最近 10 条匹配记录
)

records.forEach { info ->
    if (info.reason == ApplicationExitInfo.REASON_ANR) {
        Log.d("ANR", "进程=${info.processName}，退出时间=${info.timestamp}")
        // 读取可用 trace，保存本地，再交给后台上传流程。
    }
}
```

推荐的业务流程：

```text
后台读取系统退出记录和可用 trace
    → 关联应用版本、设备、页面、此前操作等业务信息
    → 保存到本地
    → 后台上传到监控平台
    → 成功后标记已上报，失败则重试
```

可以结合时间戳、进程等信息识别记录，避免每次启动重复上报。页面、操作路径等业务现场需要应用事先记录，不能指望退出记录自动提供。

覆盖范围需要分清：

- `REASON_ANR` 表示进程因 ANR 退出；不是所有 ANR 都会立即结束进程。
- ANR 后恢复、之后因其他原因退出的进程，退出记录的原因可能不是 ANR，但仍可能附带先前的 ANR trace。
- trace 可能为空或被覆盖，历史退出查询不能保证拿到每一次 ANR 的完整现场。

因此，仅筛选 `reason == REASON_ANR` 是最基础的补报方式，不等于完整 ANR 监控覆盖。

参考：[历史退出记录查询](https://developer.android.com/reference/android/app/ActivityManager#getHistoricalProcessExitReasons(java.lang.String,%20int,%20int))、[ApplicationExitInfo.getTraceInputStream](https://developer.android.com/reference/android/app/ApplicationExitInfo#getTraceInputStream())。

## 2. getHistoricalProcessExitReasons 能获取哪些信息？

**返回 `List<ApplicationExitInfo>`，按退出时间从新到旧排列，覆盖多种进程退出原因。**

| Kotlin 属性 | 含义与边界 |
| --- | --- |
| `reason` | ANR、Java 崩溃、Native 崩溃、低内存被杀、用户请求等退出原因 |
| `timestamp` | 进程退出时间，毫秒时间戳；不能直接当成 ANR 最初发生时间 |
| `pid`、`processName` | 已退出进程的 ID 和实际进程名 |
| `description` | 系统提供的可读描述，可能为空；格式不保证跨版本稳定 |
| `importance` | 退出前的进程重要级别，如前台、前台服务、缓存进程 |
| `status` | 主动退出时的退出码，或由信号终止时的信号编号 |
| `pss`、`rss` | 最近一次采样的内存值，单位 kB；不一定是退出瞬间的内存，未采样时可能为 0 |
| `traceInputStream` | 系统保存的 trace 输入流，ANR 分析的重要现场，可能为空 |
| `processStateSummary` | 应用先前通过 `setProcessStateSummary()` 主动设置的少量自定义状态，可能为空 |

常用参数：

| 参数 | 含义 |
| --- | --- |
| `packageName` | 指定包；传 `null` 时匹配调用者 UID 所属包 |
| `pid` | 过滤已退出的进程；`0` 表示不限定 PID |
| `maxNum` | 最大返回条数；`0` 表示取所有匹配且仍保留的记录 |

普通应用主要查询自己的记录；不能任意读取其他 UID 的记录。

**系统通过有限的环形缓冲区保存历史，旧记录会被覆盖；trace 也可能独立被覆盖。** 它是近期退出信息，不是无限、完整的应用运行日志。

参考：[ApplicationExitInfo 字段定义](https://developer.android.com/reference/android/app/ApplicationExitInfo)、[查询参数、排序和保留限制](https://developer.android.com/reference/android/app/ActivityManager#getHistoricalProcessExitReasons(java.lang.String,%20int,%20int))。

## 3. 开发者拿到 ANR 后，如何定位和修复？

**先看系统报告的 ANR 原因和相关线程堆栈，再追踪阻塞链，而不是只盯着主线程最后一行。**

| 现场表现 | 排查与修复方向 |
| --- | --- |
| 主线程执行文件、数据库等 I/O | 将阻塞操作移到后台线程 |
| 主线程执行大量计算 | 将计算移到合适的线程，减少重复计算 |
| 主线程等待锁 | 找持锁线程，检查锁内耗时操作、锁顺序和死锁 |
| 主线程等待同步 Binder 调用 | 继续分析被调用进程及调用耗时，排查跨进程阻塞 |

主线程等待锁时，真正耗时的代码可能在持锁线程；主线程等待 Binder 时，根因可能在另一个进程。应结合完整现场、版本和用户操作复现，再修改对应路径。

修复后验证两件事：

1. 原场景复现时，阻塞链是否消失。
2. 上线后，相同类型 ANR 的发生率是否下降。

本地调试可以配合系统 bugreport；普通应用不能直接把 `/data/anr` 当作可自由读取的业务目录。

参考：[Android 官方 ANR 排查与修复指南](https://developer.android.com/topic/performance/issues/anr)。
