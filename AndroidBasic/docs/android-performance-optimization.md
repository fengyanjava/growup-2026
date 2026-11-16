# Android 性能优化：面试问答与开发排查

整理日期：2026-11-16。根据本轮复习内容整理，以传统 Android View 为主，涵盖启动、包大小、内存、卡顿和异常监控。代码为教学片段，省略部分 import、测试配置及业务实现；示例类名、方法名不代表项目已有代码。项目未新增性能或异常监控实现。

## 常用优化手段速查

### 启动优化

- **精简初始化**：删除无用、重复的初始化，启动阶段只执行必要任务。
- **延迟、按需初始化**：非首屏依赖的 SDK、数据库和功能模块，使用时再初始化。
- **异步执行耗时任务**：将允许后台执行的 I/O、解码、计算等移出主线程。
- **管理初始化依赖**：明确任务先后关系，无依赖且允许并行的任务合理并行。
- **排查隐式初始化**：检查 SDK 注册的 ContentProvider、App Startup 初始化器。
- **减少同步等待**：减少主线程等锁、等待子线程、同步 Binder 调用及重复查询。
- **减轻首屏布局**：简化布局，减少首次创建的 View，延迟加载暂不可见的内容。
- **优化首屏资源**：控制图片尺寸，减少首屏同步解码和非必要资源加载。
- **使用 Baseline Profile**：指导关键代码提前编译。
- **使用 Startup Profile**：在构建时优化启动代码的 DEX 布局。

参考：[官方启动优化指南](https://developer.android.com/topic/performance/appstartup/analysis-optimization)、[Profile 说明](https://developer.android.com/topic/performance/baselineprofiles/overview)。

### 包大小优化

- **开启 R8**：删除不可达代码，进行代码优化和名称压缩。
- **优化 keep 规则**：避免保留整个包、整个 SDK，准确保留确实需要的代码。
- **开启资源压缩**：移除未使用资源，清理废弃图片、布局等。
- **优化图片**：调整分辨率和压缩质量，合适时使用 WebP，简单图标使用 VectorDrawable。
- **精简依赖**：删除无用、重复 SDK，按需引入功能模块。
- **优化 so 体积**：按支持范围选择 ABI 或按架构拆包，移除发布包中不需要的调试符号。
- **精简资源配置**：按产品需求保留语言、屏幕密度等资源。
- **精简内置文件**：清理字体、模型、音视频、离线网页等大文件，支持按需下载。
- **使用 AAB 分发**：由 Google Play 按设备配置下发所需资源和代码。

参考：[官方包体积优化指南](https://developer.android.com/topic/performance/reduce-apk-size)。

### 内存优化

- **避免长生命周期引用页面**：防止单例、静态变量长期持有 Activity、Fragment 或 View。
- **清理生命周期相关引用**：移除监听、回调和订阅；Fragment View 销毁时清理 ViewBinding。
- **取消无用任务**：让协程、请求等与所属生命周期匹配，避免任务长期持有页面。
- **优化图片内存**：按显示尺寸解码，控制图片加载并发、预加载量和缓存容量。
- **减少临时对象**：减少绘制、滑动、列表绑定等高频路径中的对象分配。
- **限制缓存容量**：设置容量上限、淘汰策略，清理失效数据。
- **降低内存峰值**：分页加载，流式处理大文件，减少大数组、集合和数据模型的重复复制。
- **及时释放资源**：关闭文件流、Cursor，释放播放器及 native 资源。
- **减少无用常驻内容**：按需加载大对象、功能数据和模块，缩短不必要的持有时间。

参考：[官方内存管理指南](https://developer.android.com/topic/performance/memory/manage-app-memory)。

### 卡顿优化

- **减少主线程工作**：将允许后台执行的 I/O、图片解码、复杂计算移到后台。
- **减少主线程阻塞**：缩短持锁范围，排查死锁、同步 Binder 和等待其他线程的情况。
- **优化布局层级**：减少不必要的嵌套、重复测量和布局。
- **减少无效刷新**：避免不必要的 `requestLayout()`、`invalidate()` 和高频重复更新。
- **优化自定义绘制**：提前计算，复用 Paint、Path 等对象，减少绘制中的复杂操作。
- **优化 RecyclerView 绑定**：减少 `onBindViewHolder()` 中的计算、同步读取和重复设置。
- **优化列表更新**：使用 ListAdapter 异步计算差异，结合 payload 局部刷新，减少整表刷新。
- **优化图片链路**：按尺寸加载、合理缓存，避免滑动时集中解码大图。
- **减少内存抖动**：控制高频对象分配，降低频繁 GC 的干扰。
- **减少过度绘制**：清理重复背景和不必要的遮挡绘制。
- **控制昂贵绘制效果**：减少大面积透明叠加、不必要的离屏效果和过于复杂的动画。

参考：[官方卡顿优化指南](https://developer.android.com/topic/performance/issues/render)、[过度绘制说明](https://developer.android.com/topic/performance/rendering/overdraw)。

### 异常监控与稳定性治理

- **统一采集 Java / Kotlin Crash**：记录完整异常栈、原因链和线程信息，保留原异常处理链。
- **采集 Native Crash**：记录信号、寄存器、线程栈、so 信息和 Build ID。
- **监控 ANR**：结合系统报告、历史退出记录和线程现场，分析主线程及相关后台线程。
- **监控 OOM**：关联内存趋势、页面操作、大对象和泄漏线索。
- **补充业务现场**：记录版本、设备、所在页面和关键操作路径。
- **保存还原文件**：按构建归档 R8 `mapping.txt` 和 native 符号文件。
- **可靠保存与上报**：先保存本地，后续后台上传，处理去重、失败重试和已上传标记。
- **分类聚合与告警**：关注高频问题、影响用户数、新版本新增问题和异常突增。
- **区分正常流程与故障**：正常协程取消不当作故障；被处理的业务失败按需记录。
- **验证修复效果**：复现原场景，发布后持续观察相同问题的发生率和影响范围。

参考：[官方 Crash 指南](https://developer.android.com/topic/performance/issues/crash)、[ANR 指南](https://developer.android.com/topic/performance/anrs/diagnose-and-fix-anrs)。

这些手段都需要配合测量：启动看 **TTID / TTFD**，包大小看 **APK Analyzer**，内存看 **分配趋势和 Heap Dump**，卡顿看 **System Trace / Perfetto**，稳定性看 **线上发生率和影响用户数**。

## 1. 整体回答框架

**先明确现象和指标，再定位瓶颈、实施优化，最后在相同条件下验证。**

| 方向 | 主要关注点 | 常见表现 |
| --- | --- | --- |
| 启动优化 | 首帧之前的工作和等待、主要内容就绪时间 | 启动等待久、首页加载慢 |
| 包大小优化 | 代码、资源、so 和大文件的体积 | 下载体积大、安装包大 |
| 内存优化 | 泄漏、常驻占用、分配频率和峰值 | 内存持续上涨、频繁 GC、OOM |
| 卡顿优化 | 主线程、RenderThread、GPU 和调度等待 | 滑动卡顿、动画掉帧、响应慢 |
| 异常监控 | Crash、ANR、OOM 的采集、定位和修复验证 | 线上故障难复现、缺少现场 |

异常监控偏向稳定性保障，通常与性能优化一起治理。网络和耗电也是更广义的性能方向，本轮未展开。

## 2. 启动优化

### 2.1 点击图标后，到首页显示经历了什么？

以应用进程原本不存在的普通冷启动为例，简化过程如下：

```text
系统收到启动请求
  → 创建应用进程，进入主线程
  → 创建 Application、绑定 Context
  → 初始化该进程中需要自动创建的 ContentProvider
  → Application.onCreate()
  → 创建首页 Activity，执行生命周期回调
  → View 测量、布局、记录绘制指令
  → 后续渲染与显示链路完成首帧
```

`Activity.onResume()` 返回不等于首帧已经显示。系统启动窗口的外观和行为会随 Android 版本、主题及配置变化。

ContentProvider 可能早于 `Application.onCreate()` 执行。只给后者计时，会漏掉 SDK 自动初始化等更早的工作。

参考：[启动过程](https://developer.android.com/topic/performance/issues/launch-time)、[Application.onCreate](https://developer.android.com/reference/android/app/Application#onCreate())。

### 2.2 TTID 和 TTFD 分别衡量什么？

| 指标 | 衡量终点 | 获取方式与边界 |
| --- | --- | --- |
| TTID：首次显示耗时 | 应用 UI 首帧显示 | 系统自动记录；简单的直接启动场景可在 Logcat 搜索 `Displayed` |
| TTFD：完全显示耗时 | 主要内容显示、页面达到业务定义的可用状态 | 应用通过 `reportFullyDrawn()` 等机制报告就绪状态 |

```text
0 ms       收到启动请求
400 ms     首帧显示，只有加载动画     ← TTID
1200 ms    首页列表显示，可以使用     ← TTFD
```

这里 TTFD 是 1200 ms，包含前面的 TTID，不是再额外等待 1200 ms。

```kotlin
// Activity 中：主要内容已经显示、页面可用时调用。
reportFullyDrawn()
```

业务需要定义“可用”的标准。仅网络响应返回时，View 可能还没完成绘制，不能直接把这个时刻当成内容显示完成。

Logcat 示例：

```text
Displayed com.example.app/.MainActivity: +850ms
```

这类日志由系统进程输出，要去掉只显示当前应用的过滤条件。多 Activity 跳转场景还要区分单个 Activity 和整体启动的计时含义。

参考：[TTID](https://developer.android.com/topic/performance/issues/launch-time#time-initial)、[TTFD](https://developer.android.com/topic/performance/issues/launch-time#time-full)。

### 2.3 冷、温、热启动为什么要分开测？

| 类型 | 典型状态 | 主要工作 |
| --- | --- | --- |
| 冷启动 | 进程不存在 | 创建进程、初始化 Application、创建 Activity、显示 |
| 温启动 | 进程还在，Activity 需要重建 | 复用进程、重建 Activity、显示 |
| 热启动 | 进程和 Activity 都还保留着 | 将已有 Activity 带回前台 |

表中是便于理解的典型场景，实际温启动还可能涉及状态恢复。按 Home 后马上打开，如果进程和页面都保留着，通常属于热启动；后台进程已经被杀掉则需要重新创建进程。

**优化前测冷启动、优化后测热启动，不能据此证明优化有效。**

参考：[启动类型](https://developer.android.com/topic/performance/issues/launch-time#understand-different)。

### 2.4 常用启动优化方法有哪些？

- **精简初始化**：删除无用、重复工作，只保留启动阶段必须执行的任务。
- **按需初始化**：首页不依赖的 SDK、数据库或功能模块，等真正需要时再初始化。
- **合理异步执行**：把允许后台进行的 I/O、计算等移出主线程，并处理好依赖关系。
- **减少主线程等待**：排查锁竞争、同步 Binder 调用和等待子线程完成等情况。
- **检查隐式初始化**：检查 SDK 的 ContentProvider、App Startup 自动任务。
- **减轻首屏布局开销**：简化布局层级，减少首次创建的 View，延迟加载暂不可见的内容。
- **优化首屏资源**：减少大图解码、过大图片和非必要资源的同步加载。
- **使用 Baseline Profile**：为关键路径提供提前编译依据。
- **使用 Startup Profile**：在构建时优化启动代码的 DEX 布局。

按需初始化只是改变耗时发生的时机，首次使用功能时的等待也要关注。能否后台执行则取决于任务和 SDK 的线程要求。

参考：[启动分析与优化](https://developer.android.com/topic/performance/appstartup/analysis-optimization)。

### 2.5 初始化移到子线程，启动就一定更快吗？

**不一定，要看主线程是否还在等结果。** 假设 SDK 允许后台初始化：

```kotlin
// 在 Application.onCreate() 中。
val task = Thread { initSdk() }
task.start()
task.join() // 主线程仍然等待初始化完成。
```

```text
主线程：启动子线程 → 等待初始化 → 继续创建首页
子线程：             执行初始化
```

如果首页必须依赖结果，初始化耗时仍在首屏的等待路径上。首页不依赖结果时，才有机会与页面创建并行。

### 2.6 如何确认具体初始化任务花了多久？

用 Trace 给同步代码段加标记，`android.os.Trace.beginSection/endSection` 从 API 18 开始支持：

```kotlin
Trace.beginSection("initSdk")
try {
    initSdk()
} finally {
    Trace.endSection()
}
```

开始和结束必须在同一线程正确配对。`finally` 确保异常路径也能结束标记。

查看流程：

1. 打开 Android Studio Profiler，选择设备、应用和 System Trace 任务。
2. 要覆盖 `Application.onCreate()`，选择从进程启动开始录制；事后附加会漏掉已经执行的初始化。
3. 首页显示后停止录制，在 Threads 中展开对应线程，放大启动阶段，找到 `initSdk`。
4. 点击区间查看持续时间。也可以导出文件，在 [Perfetto](https://ui.perfetto.dev/) 中通过 Open trace file 打开并搜索标记。

不同 Studio 版本入口名称可能不同。先用可调试构建熟悉流程，正式测量使用可分析的 Release 构建。自定义事件需要正确配置应用跟踪权限和采集目标；自行用 Perfetto 录制时，应将目标包纳入 `atrace_apps`。

参考：[自定义事件](https://developer.android.com/topic/performance/tracing/custom-events)、[Profiler 录制](https://developer.android.com/studio/profile#start-profiling)、[应用 atrace 配置](https://perfetto.dev/docs/getting-started/atrace)。

### 2.7 Trace 持续 300 ms，就等于 CPU 执行了 300 ms 吗？

**不等于。区间长度包含执行、阻塞和等待调度等时间。**

```text
initSdk：|────────── 共 300 ms ──────────|
主线程： |执行 20 ms| 等待 260 ms |执行 20 ms|
```

录制包含 CPU 调度信息后，对照同一时间范围的线程状态：

| 状态 | 含义 | 后续排查方向 |
| --- | --- | --- |
| Running | 正在 CPU 上执行 | 执行了哪些代码，是否有重复计算 |
| Runnable | 已经可以执行，但尚未分到 CPU | CPU 竞争、调度延迟 |
| Sleeping / 阻塞等待 | 等锁、I/O、其他线程等条件 | 沿等待关系找真正耗时的位置 |

这些是系统调度轨道的状态，不应与 Java 线程状态名称简单等同。

参考：[Perfetto CPU 调度](https://perfetto.dev/docs/data-sources/cpu-scheduling)。

### 2.8 怎样发现主线程上的隐蔽磁盘访问？

开发阶段可在主线程尽早开启 StrictMode，例如在 `Application.onCreate()` 中：

```kotlin
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .penaltyLog()
            .build()
    )
}
```

后续触发可检测的磁盘读写时，Logcat 会输出违规信息和调用栈，例如 `DiskReadViolation`。

- 它检测的是发生了磁盘访问，并不要求这次访问已经很慢；实际影响结合 Trace 判断。
- 它是尽力检测机制，部分 JNI 中的访问可能检测不到。
- 在 `Application.onCreate()` 中开启规则，无法追溯已经执行完的 Provider 初始化。

参考：[StrictMode](https://developer.android.com/reference/android/os/StrictMode)。

### 2.9 SDK 的自动初始化从哪里找？

依赖库的 Manifest 会参与合并。打开 app 模块的 `AndroidManifest.xml`，切到 **Merged Manifest**，找到 `<provider>` 后查看 **Manifest Sources / Merging Log**，确认声明来自哪个 SDK。

**合并 Manifest 确认注册来源，Trace 确认实际耗时。** Provider 的存在本身不能说明它执行了昂贵初始化，还要检查具体实现。

参考：[查看合并 Manifest](https://developer.android.com/build/manage-manifests#inspect_the_merged_manifest_and_find_conflicts)。

### 2.10 App Startup 会自动异步初始化吗？

不会。它让多个任务共享 `InitializationProvider`，并通过 `dependencies()` 管理依赖顺序。Provider 自动初始化路径在主线程同步调用相关初始化器，`create()` 中的同步耗时仍会影响启动。

例如 SDK 依赖配置初始化：

```kotlin
class SdkInitializer : Initializer<MySdk> {
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(ConfigInitializer::class.java)

    override fun create(context: Context): MySdk =
        MySdk.initialize(context)
}
```

```text
ConfigInitializer.create() 返回
  → SdkInitializer.create()
```

App Startup 会复用已经初始化的结果。依赖关系保证的是初始化器调用顺序；如果配置的 `create()` 只是发起异步任务就返回，不能据此保证配置已经加载完。

参考：[App Startup](https://developer.android.com/topic/libraries/app-startup)、[Initializer 依赖](https://developer.android.com/reference/kotlin/androidx/startup/Initializer#dependencies())、[AndroidX 实现](https://github.com/androidx/androidx/blob/androidx-main/startup/startup-runtime/src/main/java/androidx/startup/AppInitializer.java)。

### 2.11 App Startup 怎样改成按需初始化？

先移除对应初始化器的自动注册，再在功能需要时手动触发。假设没有其他自动任务依赖它，在应用 Manifest 的 `<application>` 中合并：

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="com.example.SdkInitializer"
        tools:node="remove" />
</provider>
```

`<manifest>` 上需声明 `xmlns:tools="http://schemas.android.com/tools"`，初始化器类名需替换为实际完整类名。

```kotlin
val sdk = AppInitializer.getInstance(context)
    .initializeComponent(SdkInitializer::class.java)
```

手动触发也会先初始化尚未完成的依赖，调用本身仍同步执行。如果另一个自动任务的依赖链仍包含它，它仍可能被提前触发；仅删除自己的 `<meta-data>` 不足以阻断所有依赖入口。

参考：[手动与延迟初始化](https://developer.android.com/topic/libraries/app-startup#manual-initialization)。

### 2.12 Baseline Profile 如何生成、验证？

**它为关键代码路径提供提前编译依据，减少解释执行和即时编译开销。** 等待网络等外部耗时不会因此直接消失。

```text
自动化测试运行启动、首页等关键流程
  → 采集执行到的类和方法，生成 baseline-prof.txt
  → 随应用构建、发布
  → Android 运行时据此进行相应提前编译
```

采集测试模块中的核心片段：

```kotlin
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() {
        rule.collect(packageName = "com.example.app") {
            pressHome()
            startActivityAndWait()
        }
    }
}
```

若主要内容在首帧之后异步加载，还应在采集流程中等待内容就绪，覆盖相应代码路径。

`BaselineProfileRule` 的采集设备条件是：无 root 的 Android 13（API 33）及以上，或 root 的 Android 9（API 28）及以上。这是生成端条件，不代表用户设备也必须达到 Android 13。

验证时，在 Macrobenchmark 启动测试中分别传入以下 `compilationMode`：

```kotlin
CompilationMode.None()

CompilationMode.Partial(
    baselineProfileMode = BaselineProfileMode.Require
)
```

- `None()` 清除编译状态、不做预编译，运行中的 JIT 仍然可以发生。
- `Partial(Require)` 要求使用 Baseline Profile 进行部分预编译。
- 两组使用同一设备、同一个 Release 包和同一流程，多次测量相同类型的启动，比较 TTID、TTFD 等结果。
- 配置文件成功生成，只证明完成了采集；实际收益需要对照测量。

Baseline Profile 指导设备端代码预编译，也可覆盖滚动、页面跳转；Startup Profile 主要在构建时优化启动代码的 DEX 布局，作用阶段不同。

参考：[Profile 原理与区别](https://developer.android.com/topic/performance/baselineprofiles/overview)、[生成指南](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile)、[采集设备要求](https://developer.android.com/reference/androidx/benchmark/macro/junit4/BaselineProfileRule)、[效果验证](https://developer.android.com/topic/performance/baselineprofiles/measure-baselineprofile)。

## 3. 包大小优化

### 3.1 怎样确认体积主要花在哪里？

Android Studio 中选择 **Build → Analyze APK**，打开 Release APK，展开目录查看大小。也可直接把 APK / AAB 拖入编辑器。

| 内容 | 主要含义 |
| --- | --- |
| `classes*.dex` | 应用及依赖库的 Kotlin / Java 编译代码 |
| `res/` | 图片、布局等 Android 资源 |
| `assets/` | 字体、模型、网页等打包文件 |
| `lib/` | 不同 CPU 架构的 native so 库 |

先确定最大开销，再进入对应目录定位。优化前后用 APK Analyzer 比较同类 Release 产物；APK 文件体积、工具估算的下载体积和安装后的占用是不同口径。

参考：[APK Analyzer](https://developer.android.com/studio/debug/apk-analyzer)。

### 3.2 常用优化方法有哪些？

- **开启 R8 优化**：删除无用代码、优化代码、缩短类和方法名，避免过于宽泛的 keep 规则。
- **开启资源压缩**：移除未使用资源，清理废弃图片、布局、字体。
- **优化图片**：按显示需求控制尺寸与压缩质量，合适时使用 WebP，简单图标使用 VectorDrawable。
- **精简依赖**：移除未使用、功能重复的 SDK，按需引入模块。
- **优化 so**：按设备支持范围选择 ABI 或按架构拆包，去掉发布包中不需要的 native 调试符号，并单独保存符号供崩溃分析。
- **精简资源配置**：按产品需求保留语言、屏幕密度等资源。
- **减少内置大文件**：字体、模型、音视频等按需下载。
- **使用 AAB 分发**：Google Play 按设备配置下发所需内容，减少用户下载体积；AAB 本身是发布格式。

参考：[R8 优化](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization)、[缩减应用体积](https://developer.android.com/topic/performance/reduce-apk-size)。

## 4. 内存优化

### 4.1 常用优化方法有哪些？

- **避免泄漏**：防止单例、静态变量等长期持有 Activity、Fragment 或 View。
- **管理生命周期**：移除监听、取消不再需要的任务；Fragment 的 View 销毁时清理 ViewBinding 引用。
- **减少高频分配**：减少 `onDraw()`、滑动回调等路径中的临时对象，降低 GC 压力。
- **限制缓存**：设置容量、淘汰策略，清理失效内容。
- **降低内存峰值**：大列表分页，大文件流式处理，减少大数组和集合的重复复制。
- **及时释放资源**：关闭文件流、Cursor，按生命周期释放播放器和 native 资源。
- **优化图片内存**：按显示尺寸解码，控制加载并发、预加载和缓存数量，合理使用图片库的复用能力。

参考：[内存管理](https://developer.android.com/topic/performance/memory/manage-app-memory)、[图片加载](https://developer.android.com/topic/performance/graphics/load-bitmap)。

### 4.2 怎样验证内存问题？

用 Memory Profiler 观察内存、分配和 GC 趋势，Heap Dump 分析大对象及引用链。反复进入、退出同一页面，检查本应释放的对象是否仍被持有，以及内存峰值是否改善。

参考：[Heap Dump](https://developer.android.com/studio/profile/capture-heap-dump)。

## 5. 卡顿优化

### 5.1 一帧如何执行，怎样初步区分瓶颈？

开启硬件加速的传统 View 页面，大致执行链如下：

```text
VSYNC 驱动
  → 主线程处理输入、动画，必要时测量、布局、记录绘制指令
  → RenderThread 处理绘制数据并提交 GPU 命令
  → GPU 异步执行
  → SurfaceFlinger 等系统显示链路合成、送显
```

`onDraw()` 中的 Canvas 操作主要记录绘制指令，不能等同于 GPU 已完成。RenderThread 也是 CPU 线程，其长区间可能包含等待。不同帧的阶段可以重叠。

| 同一帧的现象 | 初步排查方向 |
| --- | --- |
| 主线程业务、布局或绘制记录很长 | 主线程工作、阻塞或调度延迟 |
| 主线程及时完成，RenderThread 很长 | 命令处理、提交及其等待 |
| CPU 侧及时完成，GPU completion 明显晚 | GPU 执行或排队，继续结合显示链路判断 |

录制 System Trace 后选中卡顿帧，沿同一帧查看相关线程与 GPU 完成信息，不能只凭某个线程区间长就认定 GPU 慢。

Android 12（API 31）及以上可用 Perfetto FrameTimeline 对照 Expected / Actual Timeline；Studio 提供 Janky frames 展示。Android 11 主要看 Frame Lifecycle，Android 10 及以下展示更有限。

60 Hz 的刷新间隔约 16.67 ms，120 Hz 约 8.33 ms。刷新间隔不应机械地当作所有阶段串行相加的固定总预算；判断优先依据该帧的预期时间线与实际截止时间。

参考：[绘制与异步 GPU 执行](https://developer.android.com/topic/performance/rendering/inspect-gpu-rendering)、[Studio 帧分析与版本差异](https://developer.android.com/studio/profile/jank-detection)、[FrameTimeline](https://perfetto.dev/docs/data-sources/frametimeline)。

### 5.2 常用卡顿优化方法有哪些？

- **减少主线程耗时**：将允许后台执行的 I/O、图片解码和复杂计算移到后台。
- **减少主线程等待**：排查锁竞争、同步 Binder 调用和等待其他线程完成。
- **优化布局**：简化层级，减少重复测量、布局和不必要的 `requestLayout()`。
- **优化自定义绘制**：提前计算、复用绘制对象，减少 `onDraw()` 中的复杂运算和临时对象。
- **优化图片加载**：按显示尺寸加载，合理缓存、预加载，避免滑动时反复解码大图。
- **减少内存抖动**：控制高频对象分配，降低频繁 GC 干扰。
- **优化 RecyclerView**：减轻 `onBindViewHolder()` 的工作，用 ListAdapter 异步计算差异，结合 payload 局部更新，减少整表刷新。
- **降低 GPU 负担**：移除重复背景，减少无意义的过度绘制，控制大面积透明叠加和离屏效果。

优化后重复相同的滑动、动画操作，结合实际帧截止时间比较卡顿帧和耗时分布。

参考：[慢渲染排查](https://developer.android.com/topic/performance/issues/render)、[ListAdapter](https://developer.android.com/reference/androidx/recyclerview/widget/ListAdapter)、[过度绘制](https://developer.android.com/topic/performance/rendering/overdraw)。

## 6. 异常监控

### 6.1 通常需要做哪些事？

- 采集 Java / Kotlin 未捕获异常的类型、完整调用栈和异常原因链。
- 采集 native 崩溃的信号、线程现场与堆栈，保留匹配的 native 符号。
- 结合系统报告、线程栈与 Trace 监控 ANR。
- 结合内存趋势、操作路径和开发环境复现排查 OOM。
- 补充版本、系统、设备、页面和关键操作信息。
- 保存每次发布的 R8 `mapping.txt`，用于还原混淆堆栈。
- 按异常特征聚合，关注影响用户数、发生率、新版本新增和突增问题。
- 复现验证修复，并在发布后观察相同问题及指标是否下降。

### 6.2 Native Crash 怎样记录现场？

```text
启动时安装 native 崩溃采集器
  → 收到 SIGSEGV、SIGABRT 等崩溃信号
  → 采集现场并保存本地报告
  → 进程退出
  → 后续正常启动时上传
  → 用匹配的符号文件还原堆栈
```

Java `try/catch` 和 Java 未捕获异常处理器无法直接捕获 `SIGSEGV` 这类 native 崩溃信号。以 Breakpad 为例，它通过 `sigaction` 注册信号处理函数，并协调崩溃转储；具体采集也可能由独立进程完成。

| 信息 | 作用 |
| --- | --- |
| 信号、错误码、访问地址 | 判断非法访问、主动 abort 等原因 |
| 进程、线程、寄存器 | 保存崩溃瞬间执行现场 |
| 调用栈、so 名称、指令偏移 | 定位执行位置 |
| 内存映射、Build ID | 匹配准确的库和符号 |
| App 版本、设备、关键操作 | 辅助复现 |

Android 系统自身会生成 tombstone，包含线程栈、寄存器和内存映射。线上通常接入支持 NDK 的采集 SDK，例如 Crashlytics NDK，并随每次发布归档、上传对应 native 符号。

Android 12（API 31）起，`ApplicationExitInfo.getTraceInputStream()` 可为 `REASON_CRASH_NATIVE` 返回 tombstone 的 protobuf 数据；记录可能被覆盖，输入流也可能为空。不要将它当作普通文本 ANR trace 直接解析，也不能假设普通应用可任意读取系统 tombstone 目录。

参考：[系统 native 崩溃报告](https://source.android.com/docs/core/tests/debug/native-crash)、[Breakpad 实现](https://github.com/google/breakpad/blob/main/src/client/linux/handler/exception_handler.cc)、[Crashlytics NDK](https://firebase.google.com/docs/crashlytics/android/get-started-ndk)、[系统 trace API](https://developer.android.com/reference/android/app/ApplicationExitInfo#getTraceInputStream())。

### 6.3 Native 地址怎样还原成源码？

例如原始报告：

```text
#00 pc 0000000000012340 .../libimage.so
```

准备好匹配的未剥离、保留调试信息的 so，使用 NDK 工具：

```sh
ndk-stack \
  -sym /path/to/unstripped/arm64-v8a \
  -dump native-crash.txt
```

`-sym` 是符号库目录，`-dump` 是完整崩溃日志。保留日志的起始标记等内容，以便工具识别。调试信息完整时可还原函数、文件和行号。

**符号必须匹配实际构建和 ABI。** 可用 Build ID 校验；仅 App 版本号相同不足以保证匹配。

参考：[ndk-stack](https://developer.android.com/ndk/guides/ndk-stack)。

### 6.4 ANR 能通过 UncaughtExceptionHandler 捕获吗？

不能直接捕获。ANR 是系统检测到应用未按时完成某项工作，并不会因此向应用抛出一个 Java 异常。

例如主线程等待锁，无法及时响应输入，系统检测超时后采集诊断信息。等待期间可能没有任何异常抛出。不同 ANR 类型的触发条件和超时规则不同，不能全部套用同一个秒数。

参考：[ANR 类型与排查](https://developer.android.com/topic/performance/issues/anr)。

### 6.5 怎样在下次启动时读取 ANR 线索？

Android 11（API 30）起，可以查询近期进程退出记录，查询与 trace 读取放在后台：

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val am = context.getSystemService(ActivityManager::class.java)
    val records = am.getHistoricalProcessExitReasons(
        context.packageName, 0, 10
    )
    val anr = records.firstOrNull {
        it.reason == ApplicationExitInfo.REASON_ANR
    }
    // 读取 anr 的时间、进程及可用 trace，保存后交给上传流程。
}
```

`0` 表示不限定历史 PID，`10` 是最多返回条数；结果按退出时间从新到旧排列。`timestamp` 是退出时间，不应直接当成 ANR 最初发生时间。

- 系统保留记录有限，trace 也可能为空或独立被覆盖。
- ANR 不一定立即导致进程退出。
- ANR 后恢复、之后因其他原因退出时，退出原因可能不是 ANR，但仍可能附带此前的 ANR trace。
- 因此只筛选 `REASON_ANR` 不能完整统计所有 ANR；上报还要去重、关联现场并处理失败重试。

完整字段和历史退出记录边界参见已有 [Android ANR 笔记](android-anr.md)。

参考：[退出查询](https://developer.android.com/reference/android/app/ActivityManager#getHistoricalProcessExitReasons(java.lang.String,int,int))、[trace 保留边界](https://developer.android.com/reference/android/app/ApplicationExitInfo#getTraceInputStream())。

### 6.6 主线程 Blocked，接着查什么？

找它等待的锁，再找持锁线程。以下为简化示意：

```text
"main" tid=1 Blocked
  at Repository.getData(...)
  - waiting to lock <0x1234> held by thread 18

"worker" tid=18
  at Repository.readLargeFile(...)
  - locked <0x1234>
```

后台线程执行文件读取，但长时间占锁，主线程会被间接拖住。应在保持数据一致性的前提下缩短持锁范围，将允许移出的耗时 I/O 移到锁外。

### 6.7 主线程停在 nativePollOnce，是系统误报吗？

不能仅凭这个栈判断。它通常意味着采集瞬间线程正在等待 Looper 事件，可能存在以下时间差：

```text
主线程长时间忙碌或阻塞
  → 系统检测超时
  → 主线程恢复
  → 堆栈采集完成，此时线程已经空闲
```

也可能是导致组件超时的工作在其他线程。先确认 ANR 类型、时间、进程，再分析相关线程和 Trace。**线程栈是瞬间快照，不是完整的耗时历史。**

参考：[nativePollOnce 排查](https://developer.android.com/topic/performance/anrs/diagnose-and-fix-anrs#nativepollonce)、[bugreport 解读](https://source.android.com/docs/core/tests/debug/read-bug-reports)。

### 6.8 广播用了 goAsync，为什么仍可能 ANR？

`goAsync()` 允许 `onReceive()` 先返回，但系统继续等待 `PendingResult.finish()`。它本身不创建线程，也不取消超时限制。

```kotlin
// onReceive() 内的简化片段，executor 是已有线程池。
val pending = goAsync()
executor.execute {
    try {
        handleBroadcast()
    } finally {
        pending.finish()
    }
}
```

```text
广播派发 → 接收与任务提交 → 后台任务排队、执行 → finish()
```

即使主线程已经空闲，线程池排队过久、任务太慢或遗漏 `finish()`，都可能导致广播超时。实际实现还需处理任务无法提交等路径，不能只考虑任务正常开始后的 finally。

具体超时预算与广播优先级、系统版本和调度条件有关。排查要看整个广播处理路径，包括进程可能需要的冷启动。

参考：[广播 ANR 时间线与原因](https://developer.android.com/topic/performance/anrs/diagnose-and-fix-anrs#broadcast-receiver-timeout)。

### 6.9 Java / Kotlin 未捕获异常怎样统一采集？

可用默认 `UncaughtExceptionHandler` 包装已有处理链。`saveCrash()` 表示尽量简单的本地保存逻辑：

```kotlin
val previous = requireNotNull(
    Thread.getDefaultUncaughtExceptionHandler()
)

Thread.setDefaultUncaughtExceptionHandler { thread, error ->
    try {
        saveCrash(thread, error)
    } finally {
        previous.uncaughtException(thread, error)
    }
}
```

流程是尽快保存线程、完整异常和版本信息，再交还原处理器；网络上传放到后续正常运行阶段。不要覆盖后就丢弃系统或其他 SDK 的原处理链。

默认处理器不是所有线程异常的强制统一入口：线程自身的处理器和线程组等会影响分发；任务框架也可能先保存异常。

参考：[默认未捕获异常处理器](https://developer.android.com/reference/java/lang/Thread#setDefaultUncaughtExceptionHandler(java.lang.Thread.UncaughtExceptionHandler))。

### 6.10 async 失败一定会进入全局 Crash 处理器吗？

不一定。`async` 把失败保存在 `Deferred` 中，`await()` 会再次抛出对应异常。下面在已有协程中，用 `supervisorScope` 隔离子任务失败，假设 `loadData()` 可能抛出 `IOException`：

```kotlin
supervisorScope {
    val task = async { loadData() }
    try {
        val data = task.await()
        showData(data)
    } catch (e: IOException) {
        recordFailure(e)
    }
}
```

普通父子关系下，`async` 失败还可能取消父协程，不能概括为“不 await 就没有其他影响”。`CoroutineExceptionHandler` 不会直接处理 `async` 自身保存的异常；监控时要明确结果在哪里被等待、失败在哪里被处理。

参考：[协程异常传播](https://kotlinlang.org/docs/exception-handling.html)。更多作用域和取消机制参见 [Kotlin 协程笔记](kotlin-coroutines.md)。

### 6.11 协程取消要上报成故障吗？

正常取消通常不应当作故障。例如页面退出后取消请求，可能抛出 `CancellationException`。统一捕获 `Exception` 时要先保留取消语义：

```kotlin
try {
    loadData()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    reportFailure(e) // 再按业务需要分类记录。
}
```

吞掉取消异常可能让后续逻辑继续执行；把正常取消当故障上报也会增加监控噪声。

参考：[取消与异常](https://kotlinlang.org/docs/exception-handling.html#cancellation-and-exceptions)。
