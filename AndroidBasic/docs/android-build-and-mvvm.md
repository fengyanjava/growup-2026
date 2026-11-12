# 构建与 MVVM：问题复习

## 1. gradle-daemon-jvm.properties 有什么作用？能删除吗？应该提交 Git 吗？

**它声明运行 Gradle Daemon 所需的 JVM 条件，是构建环境配置，建议保留并提交 Git。**

项目中的文件位于 [gradle-daemon-jvm.properties](../gradle/gradle-daemon-jvm.properties)，主要包含：

```properties
toolchainVersion=21
# 另有不同系统、CPU 架构对应的 toolchainUrl 配置
```

- `toolchainVersion=21`：要求使用 JDK 21 运行 Gradle Daemon。
- `toolchainUrl.*`：提供对应平台的 JDK 下载地址，供自动配置所需的 JVM 使用。
- 文件由 `updateDaemonJvm` 任务生成或更新；“自动生成”不等于“应当忽略的缓存”。

三个容易混淆的版本配置：

| 配置 | 控制什么 | 当前项目 |
| --- | --- | --- |
| `gradle/wrapper/gradle-wrapper.properties` | 使用哪个 Gradle 版本 | 9.4.1 |
| `gradle/gradle-daemon-jvm.properties` | 用哪个 JDK 运行 Gradle Daemon | 21 |
| `app/build.gradle.kts` 中的 `compileOptions` | Java 源码与目标字节码的兼容级别 | 11 |

因此，用 JDK 21 执行构建与 Java 11 的兼容设置不冲突。

文件可以删除，但删除后就不再由它统一选择 JVM，需要依赖 Android Studio 的 Gradle JDK、`JAVA_HOME` 等设置。选中的 JDK 必须兼容当前 Gradle 和 Android 构建工具。

此文件存在时，Daemon JVM 条件优先于 `JAVA_HOME` 和 `org.gradle.java.home`。Gradle 官方建议像 Wrapper 配置一样，将生成的文件纳入版本控制。

参考：[Gradle Daemon JVM criteria](https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:daemon_jvm_criteria)。

## 2. Android Studio 中 Compile 和 Assemble 有什么区别？

**Compile 完成源码编译等必要工作；Assemble 继续完成所选构建产物的组装。**

应用模块的构建过程可简化理解为：

```text
Kotlin / Java 源码
    → 编译与必要的前置任务
    → .class 等中间产物
    → DEX 转换、资源处理、打包、按配置签名等
    → APK
```

这只是分工示意，实际任务会根据依赖关系执行，部分步骤也可并行。

| 菜单 | 作用 |
| --- | --- |
| Compile 当前选择 | 编译所选源代码及必要的前置任务，不完成 APK 打包 |
| Compile All Sources | 编译项目所有源代码，不完成 APK 打包 |
| Assemble 'app' Run Configuration | 按当前运行配置构建并组装对应产物 |
| Assemble Module | 组装所选模块及其依赖；应用模块通常产出 APK，Android 库模块通常产出 AAR |
| Assemble Project | 组装项目中的所有模块 |
| Assemble Project with Tests | 同时组装测试组件，不等于执行测试 |
| Run | 按运行配置构建、部署到设备并启动 |
| Generate App Bundles or APKs | 生成对应变体的 AAB / APK |
| Generate Signed App Bundle or APK | 通过向导配置签名并生成发布产物 |

Compile 和 Assemble 都可以使用增量构建和缓存。Assemble 不等于每次先 Clean 再全量编译。

当前项目常用验证命令，在 `AndroidBasic` 目录中执行：

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

其中 `assembleDebug` 构建 Debug APK，`testDebugUnitTest` 执行单元测试，`lintDebug` 执行静态检查。

参考：[Android Studio 构建菜单](https://developer.android.com/build/build-for-release)、[菜单命名调整](https://developer.android.com/studio/releases/past-releases/as-meerkat-release-notes#build-menu)。

## 3. 这个 MVVM 示例如何分工、分包？

示例采用 XML、ViewBinding、ViewModel、LiveData、协程和 Retrofit，不使用 Flow。

| 部分 | 当前实现 | 职责 |
| --- | --- | --- |
| 功能入口 | `MainActivity` | 展示并打开各个独立演示功能 |
| View | `PostListActivity`、`PostAdapter` | 接收交互、观察状态、更新界面 |
| ViewModel | `PostViewModel` | 管理页面状态、协调数据请求 |
| Model / 数据层 | `Post`、`PostRepository`、`PostApi` | 表示数据、提供数据入口、执行接口请求 |
| 依赖组装 | `MvvmDependencies` | 为当前功能组装 API 和 Repository |
| 公共能力 | `core.network`、`core.ui` | 网络基础配置、系统栏适配 |

功能代码集中放在 `feature.mvvm`，内部再分 `data`、`ui`。后续功能新增同级功能包，只有确实共用的能力才放到 `core`。

请求与状态更新：

```text
首次进入 / 刷新 / 重试
    → PostViewModel.loadPosts()
    → PostRepository.getPosts()
    → PostApi.getPosts()
    → GET https://jsonplaceholder.typicode.com/posts
    → ViewModel 发布 LiveData<PostUiState>
    → PostListActivity.render()
```

接口是 [JSONPlaceholder](https://jsonplaceholder.typicode.com/) 的在线演示服务：请求真实经过网络，返回示例文章数据。进入页面后可观察加载、成功、空数据与失败状态。

ViewModel 只公开只读的 `LiveData`，将可修改的 `MutableLiveData` 保留在内部。Activity 不直接创建 HTTP 客户端或执行请求。

代码入口：[PostListActivity](../app/src/main/java/com/example/android_basic/feature/mvvm/ui/PostListActivity.kt)、[PostViewModel](../app/src/main/java/com/example/android_basic/feature/mvvm/ui/PostViewModel.kt)、[Repository](../app/src/main/java/com/example/android_basic/feature/mvvm/data/repository/PostRepository.kt)、[PostApi](../app/src/main/java/com/example/android_basic/feature/mvvm/data/remote/PostApi.kt)。

## 4. by viewModels 中，by 做了什么？

项目代码：

```kotlin
private val viewModel: PostViewModel by viewModels {
    PostViewModel.Factory(MvvmDependencies.postRepository)
}
```

这里使用属性委托：读取 `viewModel` 时，将获取属性值的工作交给 `viewModels()` 返回的委托对象。

- `by` 是 Kotlin 的委托语法，本身不规定延迟初始化或缓存规则。
- `viewModels()` 返回 `Lazy<PostViewModel>`，具体实现负责延迟获取并缓存结果。
- Lambda 返回的是 `Factory`；Factory 负责说明如何创建带 Repository 参数的 ViewModel。

## 5. viewModels 返回 Lazy，为什么属性类型可以是 PostViewModel？

`by` 需要的是委托对象，不是直接赋给属性的值。`Lazy<T>` 的 `getValue()` 扩展会返回 `value`，类型是 `T`。

近似展开：

```kotlin
private val delegate: Lazy<PostViewModel> = viewModels<PostViewModel> {
    PostViewModel.Factory(MvvmDependencies.postRepository)
}

private val viewModel: PostViewModel
    get() = delegate.value
```

因此需要区分：

```kotlin
// 编译错误：Lazy<PostViewModel> 不能直接赋给 PostViewModel
val vm: PostViewModel = viewModels<PostViewModel>()

// 正确：让 Lazy<PostViewModel> 为属性提供 PostViewModel
val vm: PostViewModel by viewModels<PostViewModel>()
```

后一段只示意类型关系；当前 `PostViewModel` 有 Repository 构造参数，实际使用时仍需提供项目中的 Factory。

## 6. ViewModel 具体在什么时候创建？

**第一次读取属性时，尝试获取 ViewModel；Store 中没有对应实例时才创建。**

```text
Activity 构造
    → 创建 viewModels 委托对象
    → 此时尚未创建 PostViewModel

首次读取 viewModel
    → 委托尚无缓存，取得 Store、Factory 等
    → ViewModelProvider 查询当前 Activity 的 ViewModelStore
        ├─ 已有对应实例 → 返回已有实例
        └─ 没有对应实例 → Factory.create()
                            → PostViewModel(repository)
                            → 执行 init { loadPosts() }
    → 委托缓存取得的实例
```

当前项目首次读取发生在 `onCreate()` 的这一行：

```kotlin
viewModel.uiState.observe(this, ::render)
```

前面的按钮代码只是在注册回调，注册时不会读取 Lambda 内的 `viewModel`：

```kotlin
binding.refreshButton.setOnClickListener {
    viewModel.loadPosts() // 点击后才执行
}
```

首次网络请求来自新 ViewModel 执行 `init { loadPosts() }`。注册观察本身负责观察状态，恰好在本项目中也是首次触发属性读取的位置。

## 7. 旋转屏幕后为何不重新请求？Factory 会执行几次？

需要区分两处保存机制，以及两个不同的调用。

| 机制 / 调用 | 行为 |
| --- | --- |
| 委托自身缓存 | 同一 Activity 实例中，多次读取直接返回已缓存的 ViewModel |
| Activity 的 ViewModelStore | 配置变化后保留 ViewModel，供新 Activity 实例重新获取 |
| `factoryProducer()` | 委托首次取值时取得 Factory；新 Activity 的新委托可能再次执行它 |
| `Factory.create()` | 只有 Store 中没有对应 ViewModel 时才负责创建实例 |

旋转时 Activity 与委托都会重建，但 Store 中保留原 ViewModel。因此即使再次构造 Factory，也不会因此再次执行 ViewModel 的构造函数或 `init`。

正常关闭页面并重新进入，或进程结束后重新进入，则需要新的 ViewModel。当前示例没有把列表保存到磁盘，也没有用 SavedStateHandle 保存文章列表。

## 8. by lazy 能代替 by viewModels 吗？

两者都可以提供延迟取值，但承担的职责不同：

| 写法 | 提供什么 |
| --- | --- |
| `by lazy { ... }` | 延迟执行初始化代码，缓存结果 |
| `by viewModels { ... }` | 延迟通过 ViewModelProvider 获取实例，并结合 ViewModelStore 管理作用域与复用 |

直接在 `lazy` 中调用 `PostViewModel(repository)`，不会自动获得 ViewModelStore 的保留与清理机制。若在 `lazy` 内手动使用 ViewModelProvider，生命周期能力来自 Provider / Store。

参考：[ViewModel 作用域](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis)、[通过 Factory 提供依赖](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories)。
