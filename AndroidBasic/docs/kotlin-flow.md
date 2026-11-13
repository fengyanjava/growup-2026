# Kotlin Flow：问题复习

整理日期：2026-11-13。根据本轮正式 Flow 复习、追问和项目实际代码整理，先看执行过程，再理解行为与边界。

## 整理范围与示例约定

- 用户在开始改代码前明确要求不记入总结的最初一段讲解已排除。本笔记从后来“开始 Flow 的复习”后的正式内容整理；相同概念如在正式复习中重新讲过，按后一次讲解收录。
- 原有独立语法知识点的排除约定继续沿用。本轮专门追问的 List `==`、状态对象比较，作为 StateFlow 更新问题的一部分记录，不另开数据类语法课程。
- 原 LiveData 示例在 `feature.mvvm`；Flow 示例在 `feature.mvvmflow`。本笔记以 Flow 示例为准，保留两版供对照。
- 项目还有 `feature.flow` 下的独立练习。`search`、`searchFlow`、`postsFlow`、`keywordFlow` 等搜索示例是假设，不表示文章页面已实现搜索功能。
- 示例通常省略 import、外层类和协程入口。`collect`、`first` 等挂起调用应放在协程或挂起函数中；Android UI 的访问与渲染应在主线程执行。
- 当前项目使用 kotlinx.coroutines `1.10.2`。`debounce` 需要 `@OptIn(FlowPreview::class)`；本轮的 `flatMapLatest/Concat/Merge` 需要 `@OptIn(ExperimentalCoroutinesApi::class)`。

## 阅读导航

| 部分 | 问题 |
| --- | --- |
| 基础执行 | 1—6：发送与接收、冷流、顺序执行、上下文、转换与异常 |
| 状态与生命周期 | 7—13：StateFlow、只读暴露、去重、合并、持续收集、repeatOnLifecycle 及实现 |
| 共享上游 | 14—18：stateIn、启动策略、SharedFlow、replay、shareIn |
| 常用操作符 | 19—25：collectLatest、缓冲、去重、开始与结束、重试、组合、有限收集 |
| 上下文与回调、搜索 | 26—31：emit 约束、channelFlow、callbackFlow、发送方式、debounce、内部流切换与并发 |
| 状态更新追问 | 32—35：List 相等、新状态、不可变更新、update、共享流异常 |
| 项目串联 | 36：mvvmflow 中一次请求和页面生命周期的完整过程 |

## 1. Flow 怎样发送和接收数据？

**`flow { }` 定义数据生产过程，`emit` 发送值，`collect` 接收并处理值。**

```kotlin
val numbers = flow {
    emit(1)
    emit(2)
}

numbers.collect { value ->
    println(value)
}
```

```text
调用 collect → 执行 flow 代码块
emit(1) → collect 的 Lambda 收到 1
emit(2) → collect 的 Lambda 收到 2
代码块结束 → 本次收集正常结束
```

这里的普通 Flow 可以正常结束；StateFlow 的持续等待见第 11 节。

## 2. flow { } 是冷流还是热流？

**`flow { }` 创建冷流。只定义它，代码块不会执行；每次收集，都会重新执行一次。**

```kotlin
val numbers = flow {
    println("开始执行")
    emit(1)
}

numbers.collect { println("A: $it") }
numbers.collect { println("B: $it") }
```

```text
开始执行
A: 1
开始执行
B: 1
```

如果请求写在 `flow { }` 内，每次重新收集这个冷流，都会重新执行请求代码。创建 Flow 对象本身不发请求。

## 3. emit 和 collect 默认怎样配合执行？

**默认顺序执行：处理完一个值，再继续发送方后面的代码。**

```kotlin
flow {
    println("准备发送")
    emit(1)
    println("发送之后")
}.collect { value ->
    println("收到 $value")
    delay(1_000)
    println("处理完成")
}
```

```text
准备发送 → 收到 1 → 等待一秒 → 处理完成 → 发送之后
```

`emit(1)` 调用链中会执行下游处理。下游挂起时，这次发送也尚未返回。`buffer` 等操作符会改变上下游衔接方式，见第 20 节。

## 4. flow 会自动切换后台线程吗？flowOn 改变哪里？

**不会。默认沿用收集方的协程上下文。** 在 Activity 的 `lifecycleScope.launch` 中，不指定其他调度器时，普通 Flow 的生产和收集都在主线程执行。

```kotlin
lifecycleScope.launch {
    flow {
        println("发送：${Thread.currentThread().name}")
        emit(1)
    }
        .flowOn(Dispatchers.IO)
        .collect {
            println("接收：${Thread.currentThread().name}")
        }
}
```

```text
flowOn 前面的上游 → IO 调度器安排执行
collect 所在的下游 → 保留收集方的上下文，这里是主线程
```

`flowOn` 影响它前面的上游，不把后面的 `collect` 一起搬到 IO。包一层 `flow` 也不会让阻塞工作自动变成异步工作。

项目中的 Retrofit `suspend` 接口支持异步网络等待，可以从主线程调用，无需仅为这次请求再包 `Dispatchers.IO`。

## 5. map 和 onEach 有什么区别？

**`map` 转换值，`onEach` 执行操作并继续传递原值。**

```kotlin
flow {
    emit(1)
    emit(2)
}
    .map { it * 10 }
    .onEach { println("日志：$it") }
    .collect { println("收到：$it") }
```

```text
1 → map 转成 10 → onEach 记录 10 → collect 收到 10
2 → map 转成 20 → onEach 记录 20 → collect 收到 20
```

`onEach` 的 Lambda 返回值不会替换流中的数据。记录日志可以放在 `onEach`；把数据转换成页面需要的形式可以用 `map`。

## 6. catch 能处理哪些异常？处理后会回去继续吗？

**`catch` 处理它上游的异常，可以发送备用值；处理后不会回到出错位置继续。**

```kotlin
flow {
    emit(1)
    throw IllegalStateException("加载失败")
}
    .catch {
        emit(-1)
    }
    .collect { println(it) }
```

输出 `1`、`-1`，然后正常结束。原上游不会从抛异常的地方恢复。

```kotlin
flow { emit(1) }
    .catch { println("上游异常") }
    .collect {
        throw IllegalStateException("处理失败")
    }
```

这个 `catch` 捕获不到下游 `collect` 的异常。项目中 `render()` 的错误也不能靠它前面的 `catch` 兜住；需要处理整个收集调用的异常时，可以在外部使用适当的 `try/catch`。

`NetworkResult.Failure`、`PostUiState.Error` 都是普通对象，不是抛出的异常，不会因为被发送就触发 `catch`。

## 7. StateFlow 为什么是热流？新收集者收到什么？

**StateFlow 持有当前状态，无需等待收集者出现就可以更新。新收集者先收到当前值。**

```kotlin
val state = MutableStateFlow(0)
state.value = 1
state.value = 2

state.collect { println(it) }
```

开始收集时先收到 `2`，不会依次补发 `0`、`1`。之后继续等待状态变化。

与冷流不同，收集 StateFlow 不会为每个收集者重新执行一遍生产逻辑。项目请求在 ViewModel 中独立进行，Activity 可以停止、恢复收集它保存的状态。

## 8. MutableStateFlow、StateFlow 和 asStateFlow 怎么配合？

```kotlin
private val _uiState =
    MutableStateFlow<PostUiState>(PostUiState.Loading)

val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()
```

- `MutableStateFlow` 必须有初始值；ViewModel 内部可以修改 `.value`。
- `StateFlow` 对外提供只读访问，Activity 可以读取 `.value` 或收集，不能给 `.value` 赋值。
- `asStateFlow()` 提供只读视图，不复制状态，也不启动新的上游任务。

```kotlin
// ViewModel 修改。
_uiState.value = PostUiState.Empty

// Activity 接收。
viewModel.uiState.collect { render(it) }
```

## 9. StateFlow 为什么不重复通知相等的状态？

**StateFlow 按 `equals`，即 Kotlin 的 `==`，判断状态是否相等。**

假设当前已是 `Empty`，再次赋值：

```kotlin
_uiState.value = PostUiState.Empty
```

不会产生新通知。对于项目中的 `Success(posts)`，新建包装对象也不一定意味着状态变化：列表内容相等时，两个 `Success` 仍相等。

同理，初始状态已经是 `Loading`，请求开始时再赋一次 `Loading` 不会额外通知；新收集者仍会先收到当前的 `Loading`。

List、对象身份与状态快照的追问见第 32—34 节。

## 10. StateFlow 能保证每个中间状态都被收集到吗？

**不能。收集者处理较慢时，中间状态可能被合并，只接收最新状态。**

```text
收集者正在处理 0
生产方连续更新为 1、2、3
收集者处理完 0 → 接着收到最新的 3
```

`1`、`2` 不会为这个慢收集者排队逐个交付。这叫合并（conflation）。

```kotlin
_uiState.value = PostUiState.Loading
_uiState.value = PostUiState.Success(posts)
```

如果变化很快，页面可能直接观察到 `Success`，不能依赖每一次赋值都执行一次 `render`。StateFlow 表达“现在的状态”，不保证逐次交付所有变化。

## 11. StateFlow.collect 什么时候结束？多个状态流怎么收集？

**StateFlow 的收集不会因为处理完一个值就正常结束，会继续等待变化。**

```kotlin
viewModel.uiState.collect { render(it) }
// 正常情况下不会走到这里；协程取消也不会让普通后续代码接着执行。
```

所以两个 StateFlow 不能在同一个协程里顺序 `collect`，否则第二个无法开始。需要分别启动子协程：

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { postState.collect { renderPosts(it) } }
        launch { userState.collect { renderUser(it) } }
    }
}
```

这是同时存在文章和用户状态的假设。当前文章示例只有一个 `uiState`，可以直接在 `repeatOnLifecycle` 内收集。

## 12. repeatOnLifecycle 怎样管理收集？

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { render(it) }
    }
}
```

```text
达到 STARTED（含 RESUMED）→ 启动内部协程，收集当前状态及变化
低于 STARTED             → 取消内部协程，停止收集
重新达到 STARTED         → 创建新协程，重新执行整个代码块
Activity 销毁           → 外层 lifecycleScope 也被取消
```

`collect` 本身不负责页面生命周期。这里依靠 `repeatOnLifecycle` 在页面停止时取消收集，而不是只依靠销毁时才取消的外层作用域。

项目的请求在 `viewModelScope` 中。停止 Activity 的收集不等于取消请求，恢复收集也不会自动调用 `loadPosts()`。

## 13. 【追问】repeatOnLifecycle 是如何实现的？

**核心是生命周期观察者和一个记录当前内部任务的 `Job`。** 以 `STARTED` 为目标状态，简化逻辑如下，省略同步和清理细节：

```kotlin
var job: Job? = null

val observer = LifecycleEventObserver { _, event ->
    when (event) {
        Lifecycle.Event.ON_START -> {
            job = scope.launch { block() }
        }
        Lifecycle.Event.ON_STOP -> {
            job?.cancel()
            job = null
        }
        else -> Unit
    }
}

lifecycle.addObserver(observer)
```

```text
ON_START → 协程 A 执行 collect
ON_STOP  → 取消协程 A
ON_START → 协程 B 重新执行 collect
```

**不是暂停原来的 collect 再恢复，而是取消旧协程，重新执行传入的代码块。**

实际实现还包括：

- 主线程上进行生命周期观察者的注册和移除。
- 挂起等待生命周期销毁或调用方取消，随后清理内部任务和观察者。
- 使用 `Mutex` 和内部 `coroutineScope`，保证前一轮代码及其子任务彻底结束后，新一轮代码才执行。

源码参照：[AndroidX RepeatOnLifecycle.kt](https://github.com/androidx/androidx/blob/androidx-main/lifecycle/lifecycle-runtime/src/commonMain/kotlin/androidx/lifecycle/RepeatOnLifecycle.kt)。上面的代码只说明核心思路，不是完整实现。

## 14. stateIn 做什么？

**`stateIn` 把普通 Flow 转成 StateFlow，让多个收集者共享上游执行产生的最新状态。**

```kotlin
val number: StateFlow<Int> = flow {
    emit(1)
    emit(2)
}.stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = 0,
)
```

```text
先创建初始值为 0 的 StateFlow
在指定作用域启动上游收集
上游发出 1、2 → 保存的状态依次更新
Activity 收集得到当前状态，不会为每个 Activity 收集者单独执行上游
```

`scope` 管理共享上游任务，`started` 控制启动和停止，`initialValue` 提供初始状态。快速更新仍可能发生第 10 节的合并，不保证观察到 `0、1、2` 全部状态。

## 15. Eagerly、Lazily、WhileSubscribed 有什么区别？

| 策略 | 何时开始收集上游 | 没有收集者后 |
| --- | --- | --- |
| `Eagerly` | 调用转换时就启动上游收集 | 不仅因无人收集而停止 |
| `Lazily` | 第一个收集者出现时 | 启动后，不仅因无人收集而停止 |
| `WhileSubscribed` | 第一个收集者出现时 | 达到停止等待时间后，停止上游收集 |

```kotlin
SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)
```

```text
最后一个收集者离开 → 等待五秒
五秒内有人回来     → 保持上游收集
五秒后仍没人       → 停止上游收集
之后有人回来       → 重新收集上游
```

`WhileSubscribed()` 默认停止等待时间为零。上例只配置停止等待时间，停止后仍保留已有状态或 replay 缓存，不表示五秒后清空。

如果上游冷流里包含网络请求，停止后重新收集可能重新请求。当前项目使用独立的 `loadPosts()` 主动请求，恢复页面收集不触发请求。

上游本身可以正常结束；以上策略不意味着有限上游会永远运行。指定作用域被取消时，其管理的上游收集也会取消。

## 16. SharedFlow 的默认行为是什么？

**SharedFlow 可以把数据广播给多个收集者，不要求初始值。默认不向后来加入的收集者补发历史值。**

```kotlin
val signals = MutableSharedFlow<Int>()
signals.emit(1) // 此时没有收集者。

signals.collect { println(it) }
```

不会打印 `1`，而是等待之后发送的值。默认没有收集者且不保留历史值时，这次发送的数据被丢弃，`emit` 不会等待未来收集者。

SharedFlow 和 StateFlow 的收集本身都不会正常完成；上游转换成共享流后，上游完成也不等于下游收集结束。

## 17. replay 保留什么？SharedFlow 会过滤重复值吗？

```kotlin
val signals = MutableSharedFlow<Int>(replay = 1)
signals.emit(1)
signals.emit(2)
signals.collect { println(it) }
```

新收集者先收到 `2`，然后等待后续数据。

| replay | 新收集者加入时 |
| --- | --- |
| `0` | 不补发历史值 |
| `1` | 补发最近一个值 |
| `2` | 按顺序补发最近两个值 |

**SharedFlow 不自动按 `==` 去重。**

```kotlin
val signals = MutableSharedFlow<Int>(replay = 2)
signals.emit(1)
signals.emit(1)
signals.collect { println(it) }
```

会收到两次 `1`。这与只保存最新状态并过滤相等更新的 StateFlow 不同。

## 18. shareIn 与 stateIn 怎么对应？

```kotlin
val numbers = flow {
    emit(1)
    emit(2)
}.shareIn(
    scope = viewModelScope,
    started = SharingStarted.Lazily,
    replay = 1,
)
```

第一个收集者出现才开始执行上游；其他收集者共享这次上游执行，而不是各自重新生产一份。

| 转换 | 结果 | 初始或历史值参数 |
| --- | --- | --- |
| `stateIn` | StateFlow | `initialValue` |
| `shareIn` | SharedFlow | `replay` |

两者都使用 `scope` 和 `started` 管理上游。启动、停止和可能重启的行为取决于策略；不能理解成上游在整个应用生命期只会执行一次。

## 19. collectLatest 怎样取消旧处理？

```kotlin
flow {
    emit(1)
    delay(100)
    emit(2)
}.collectLatest { value ->
    println("开始处理 $value")
    delay(500)
    println("完成处理 $value")
}
```

```text
开始处理 1 → 新值 2 到来，取消处理 1
开始处理 2 → 等待五百毫秒 → 完成处理 2
```

不会打印“完成处理 1”。取消的是上一次 Lambda 的处理，不是整个上游；取消仍遵循协程的协作式取消机制，不能强行打断任意阻塞代码。

## 20. buffer、conflate、collectLatest 怎样区别？

默认上下游顺序衔接。`buffer` 通过通道和不同协程衔接上下游，允许生产和消费并发推进：

```kotlin
flow {
    emit(1)
    emit(2)
    emit(3)
}
    .buffer(capacity = 2)
    .collect {
        delay(1_000)
        println(it)
    }
```

下游处理 `1` 时，上游可以继续把 `2、3` 放入缓冲区。默认溢出策略下，缓冲区满时上游挂起等待，不丢值，也不改变顺序。并发不代表自动切换到不同线程。

```kotlin
flow {
    emit(1)
    delay(100)
    emit(2)
    emit(3)
}
    .conflate()
    .collect {
        println("开始 $it")
        delay(500)
        println("完成 $it")
    }
```

下游处理 `1` 时收到 `2、3`，只保留最新待处理值 `3`。处理完 `1` 再处理 `3`。

| 方式 | 下游处理较慢时 |
| --- | --- |
| `buffer(2)`，默认策略 | 缓冲待处理值，满后上游等待 |
| `conflate()` | 当前处理继续，待处理值只保留最新一个 |
| `collectLatest` | 取消尚未完成的旧处理，处理新值 |

## 21. distinctUntilChanged 是全局去重吗？

**不是，只过滤连续相等的值。**

```kotlin
flow {
    emit(1)
    emit(1)
    emit(2)
    emit(1)
}
    .distinctUntilChanged()
    .collect { println(it) }
```

输出 `1、2、1`。第二个 `1` 被过滤，最后一个 `1` 与前一个 `2` 不同，会保留。

StateFlow 本身已有相等性去重，不需要额外添加这个操作符。

## 22. onStart 和 onCompletion 分别在什么时候执行？

```kotlin
flow {
    println("执行上游")
    emit(1)
}
    .onStart {
        println("准备开始")
        emit(0)
    }
    .onCompletion { cause ->
        println("结束原因：$cause")
    }
    .collect { println("收到 $it") }
```

```text
准备开始 → 收到 0 → 执行上游 → 收到 1 → 结束原因：null
```

- `onStart` 在开始收集上游前执行，也可以发送值。重新收集上述冷流时会重新执行它。
- `onCompletion` 类似收集过程的 `finally`；正常结束时 `cause == null`，异常或取消时是相应异常。
- `onCompletion` 不是 `catch`，观察和收尾不会自动消除原来的异常。

## 23. retry 重试的是哪一段？重试次数怎么算？

以下是直接调用 Retrofit 接口的教学示例：

```kotlin
flow {
    emit(api.getPosts())
}
    .retry(retries = 2) { error ->
        error is IOException
    }
    .collect { println(it.size) }
```

首次执行后，符合条件的异常最多再重试两次，因此最多执行三次请求。条件不满足或次数用完，异常继续传播。

**重试是重新收集整个上游，不是从出错位置继续。**

当前 `repository.getPosts()` 已把普通请求异常转为 `NetworkResult.Failure`，不会因此触发 `retry`；不能直接套用上面 API 示例，认为返回失败对象就会自动重试。

## 24. combine 和 zip 有什么区别？

**`combine` 使用每个上游的最新值；`zip` 按顺序一一配对。**

```kotlin
combine(postsFlow, keywordFlow) { posts, keyword ->
    posts.filter { it.title.contains(keyword, ignoreCase = true) }
}.collect { renderPosts(it) }
```

等每个上游都至少发出一个值后，任何一方更新，就用各方最新值重新计算。StateFlow 自带当前值，可以提供首次组合所需的值。

```text
关键词更新 → 已有列表 + 新关键词
列表更新   → 新列表 + 已有关键词
```

```kotlin
numbers.zip(letters) { number, letter ->
    "$number$letter"
}.collect { println(it) }
```

假设两边依次发出 `1、2` 和 `A、B`，输出 `1A、2B`。每个值只参与一组配对；任一方结束且无法继续配对时，组合也结束。

## 25. first、first { 条件 } 和 take(n) 怎么结束收集？

```kotlin
val number = flow {
    emit(1)
    emit(2)
}.first()
// 返回 1，停止收集，不会继续执行到 emit(2)。
```

`StateFlow.first()` 直接拿当前值；当前是 `Loading` 就返回 `Loading`，不会自动等待成功。

```kotlin
val state = viewModel.uiState.first {
    it is PostUiState.Success
}
```

这里忽略不匹配的状态，直到第一个 `Success` 才返回。`Error` 是普通状态，不会让等待自动抛异常；若始终没有 `Success`，StateFlow 上的这次等待会持续到取消。

```kotlin
flow {
    emit(1)
    emit(2)
    emit(3)
}
    .take(2)
    .collect { println(it) }
```

收到 `1、2` 后，停止收集上游，整个调用正常结束，不再执行到 `emit(3)`。`take` 返回的仍是 Flow；`first` 是返回一个值的挂起终端操作。

## 26. 为什么不能切换上下文后直接 emit？channelFlow 解决什么？

**普通 `flow { }` 要在它自身的执行上下文中发出数据。** 假设在主线程收集，下面代码会在运行时抛出上下文约束异常：

```kotlin
flow {
    withContext(Dispatchers.IO) {
        emit(1) // 错误示例：改变上下文后直接 emit。
    }
}.collect { println(it) }
```

需要整体切换上游时使用 `flowOn`，见第 4 节。在 `withContext` 中只计算数据、返回原上下文后再 `emit`，则是允许的：

```kotlin
flow {
    val value = withContext(Dispatchers.IO) {
        readBlockingData() // 假设这是阻塞式数据读取。
    }
    emit(value)
}
```

需要多个子协程生产数据时，可以使用 `channelFlow`：

```kotlin
channelFlow {
    launch {
        delay(100)
        send("A")
    }
    launch {
        send("B")
    }
}.collect { println(it) }
```

它使用 `send`，通过内部通道安全接收不同协程的数据；等待构建块及子协程完成，并消费完已发送值后，本次收集结束。它仍是冷流，开始收集才执行构建块。

## 27. callbackFlow 怎样把持续回调转换成 Flow？

沿用本轮输入框示例，在主线程收集：

```kotlin
fun EditText.textChanges(): Flow<String> = callbackFlow {
    val watcher = doOnTextChanged { text, _, _, _ ->
        trySend(text?.toString().orEmpty())
    }

    awaitClose {
        removeTextChangedListener(watcher)
    }
}
```

```text
开始收集 → 注册文字监听器
输入变化 → 回调触发，发送文字
取消收集 → awaitClose 清理监听器
```

`callbackFlow` 是冷流，仅调用 `textChanges()` 不注册监听；每次重新收集都会重新执行注册过程。

`awaitClose` 挂起等待通道关闭或收集被取消，并负责清理。对于这种注册后持续等待回调的写法，不能省略它；构建块返回而通道仍打开会抛异常，注册的外部资源也需要得到清理。

参照：[callbackFlow 官方说明](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/callback-flow.html)。

## 28. send 与 trySend 有什么区别？如何配合生命周期清理？

| 调用 | 缓冲区满时 | 结果 |
| --- | --- | --- |
| `send(value)` | 默认可以挂起等待空位 | 挂起函数；关闭等失败可抛异常 |
| `trySend(value)` | 不等待，立即返回失败 | 非挂起，返回 `ChannelResult` |

```kotlin
val result = trySend(value)
if (result.isFailure) {
    println("发送失败")
}
```

`trySend` 不会仅因为通道满或关闭就直接抛异常。普通回调不能直接调用挂起的 `send`，所以输入框回调中使用 `trySend`；忽略结果就意味着失败时没有额外处理。

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        editText.textChanges().collect { println(it) }
    }
}
```

```text
可见 → 开始收集并注册监听
停止 → 取消收集，awaitClose 移除监听
回来 → 重新收集并注册新监听
```

`repeatOnLifecycle` 负责取消和重启；`awaitClose` 负责清理外部回调。取消协程本身不会替任意外部 API 自动注销监听。

## 29. debounce 如何减少输入触发？怎样组成搜索流程？

**`debounce(300)` 收到值后等待三百毫秒；期间有新值就替换待发送值，重新计时。**

```text
0ms 输入 a → 100ms 输入 ab → 200ms 输入 abc → 停止输入
500ms 下游收到 abc
```

在主线程、生命周期收集块中，可以组合已学操作符：

```kotlin
editText.textChanges()
    .debounce(300)
    .distinctUntilChanged()
    .collectLatest { keyword ->
        val posts = search(keyword) // 假设返回文章列表的挂起函数。
        renderPosts(posts)
    }
```

```text
输入停顿 → 过滤与上次相同的关键词 → 执行搜索
新关键词到达 collectLatest → 取消尚未结束的旧处理
```

**取消旧搜索要等新关键词通过 debounce，不是每按一次键就立即取消。** 所在函数需添加 `@OptIn(FlowPreview::class)`。

上述等待规则针对持续输入流；有限上游正常结束时，`debounce` 会发出最后一个待发送值，不必再等待完整超时。

## 30. flatMapLatest 与 map 返回 Flow 有什么区别？

假设 `searchFlow(keyword)` 返回 `Flow<List<Post>>`：

```kotlin
keywordFlow
    .flatMapLatest { keyword -> searchFlow(keyword) }
    .collect { posts -> renderPosts(posts) }
```

```text
android → 收集 searchFlow("android")
kotlin  → 取消旧内部流收集，切换到 searchFlow("kotlin")
内部流发出的文章列表 → 交给下游 collect
```

| 写法 | 得到的类型 | 是否自动收集内部 Flow |
| --- | --- | --- |
| `keywordFlow.map { searchFlow(it) }` | `Flow<Flow<List<Post>>>` | 否，只把 Flow 对象作为值发出 |
| `keywordFlow.flatMapLatest { searchFlow(it) }` | `Flow<List<Post>>` | 是，新关键词取消旧内部收集 |

冷的内部 Flow 仅被创建并不会执行。`collectLatest` 取消上一轮 Lambda 处理；`flatMapLatest` 切换内部 Flow，并把内部值转交下游。

## 31. flatMapConcat 与 flatMapMerge 怎样处理内部 Flow？

```kotlin
flow {
    emit("A")
    emit("B")
}.flatMapConcat { name ->
    flow {
        emit("$name-1")
        delay(100)
        emit("$name-2")
    }
}.collect { println(it) }
```

输出 `A-1、A-2、B-1、B-2`。第一个内部流收集完成后，才继续下一个；第一个一直不结束，后面的也不会开始收集。

把上例改成 `.flatMapMerge(concurrency = 2)`，最多并发收集两个内部流。结果可以交错，例如 `A-1、B-1、A-2、B-2`；各内部流自己的顺序保持，不保证不同流之间的先后。

| 操作符 | 内部流处理方式 |
| --- | --- |
| `flatMapLatest` | 新值到来，取消旧内部收集，切换新流 |
| `flatMapConcat` | 顺序收集，等待上一个完成 |
| `flatMapMerge` | 限定并发数量，合并多个内部流的数据 |

`flatMapMerge` 的转换 Lambda 本身仍顺序调用，并发的是产生后的内部流收集；也不代表自动换后台线程。本节这些 API 同样需要实验性 API 的 opt-in。

## 32. 【追问】List 的 == 比什么？Success 新建了为什么仍相等？

**List 的 `==` 比较数量和每个位置的元素是否相等，顺序也参与比较。**

```kotlin
val a = listOf(1, 2, 3)

println(a == listOf(1, 2, 3)) // true
println(a == listOf(3, 2, 1)) // false：顺序不同。
println(a == listOf(1, 2, 4)) // false：元素不同。
println(a == listOf(1, 2))    // false：数量不同。
```

`==` 会使用对象的 `equals`；`===` 判断是否为同一个对象。元素比较也依赖元素自己的相等规则，不能把所有类型的元素都理解成自动逐字段比较。

项目的 `Post` 和 `Success` 都是数据类，其相等判断基于主构造函数中的属性：

```kotlin
val aList = listOf(Post(1, 1, "标题", "正文"))
val bList = listOf(Post(1, 1, "标题", "正文"))

val first = PostUiState.Success(aList)
val second = PostUiState.Success(bList)

println(first === second) // false：两个 Success 对象。
println(first == second)  // true：内容相等。
```

```text
Success 比较 posts
    → List 比较数量、顺序、对应元素
    → Post 比较 userId、id、title、body
```

因此：

```kotlin
_uiState.value = PostUiState.Success(aList)
_uiState.value = PostUiState.Success(bList)
```

是否更新取决于内容，不取决于列表变量名或包装对象是否新建。内容相同不重复通知；数量、顺序或元素内容不同，才被视为新状态。

## 33. 【追问】怎样避免原地修改列表导致状态不更新？

**已发布状态当作只读快照；更新时创建新数据和新状态。**

错误示例，假设已有 `newPost`：

```kotlin
val posts = mutableListOf<Post>()
_uiState.value = PostUiState.Success(posts)

posts.add(newPost) // 改了旧状态内部的列表，StateFlow 不会自动发现。
_uiState.value = PostUiState.Success(posts) // 两边列表已是同一份修改后的内容。
```

即使包装成新的 `Success`，旧状态引用的也是这份列表，内容比较仍相等。`List` 是只读接口，并不保证底层对象不被其他引用修改。

正确思路：

```kotlin
val current = _uiState.value
if (current is PostUiState.Success) {
    _uiState.value = current.copy(
        posts = current.posts + newPost
    )
}
```

`+ newPost` 创建新列表，`copy` 创建新状态，旧数据保持不动。列表元素如果自身可变，也要避免原地修改；当前项目的 `Post` 属性都是 `val`。

接口刷新时，正常发布 `Success(result.data)` 即可；数据没有变化就不重复通知。若业务要求相同内容也每次触发一次操作，可以用 SharedFlow 表达事件，不应为强制通知而改变 `Success` 的相等规则。

## 34. update 如何安全地基于旧状态更新？

**单次 `.value` 读写是线程安全的，但“先读、计算、再写”这个组合不是原子的。**

两个协程都读取同一个旧列表，各自新增后写回，后写入的一方可能覆盖前一次新增。`update` 提供原子的状态转换：

```kotlin
import kotlinx.coroutines.flow.update

_uiState.update { current ->
    if (current is PostUiState.Success) {
        current.copy(posts = current.posts + newPost)
    } else {
        current
    }
}
```

```text
读取当前状态 → 计算新状态 → 尝试写回
期间若被其他协程更新 → 用最新状态重新计算并重试
```

Lambda 可能执行多次，只计算新状态，不放网络请求、写文件、修改外部列表等副作用。旧状态也不能在 Lambda 中原地修改。

`update` 仍按 `==` 判断是否变化，不是强制通知工具。这是教学中的追加文章示例；当前文章页仍使用 `loadPosts()` 的请求结果直接更新状态，并用 `loadJob` 忽略重复加载。

参照：[MutableStateFlow.update](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/update.html)。

## 35. stateIn/shareIn 的上游异常应该在哪里处理？

**在转换前处理：把 `catch` 或 `retry` 放在 `stateIn/shareIn` 前面。**

假设 `upstream` 是 `Flow<PostUiState>`：

```kotlin
val uiState = upstream
    .catch {
        emit(PostUiState.Error(NetworkError.UNKNOWN))
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PostUiState.Loading,
    )
```

```text
上游异常 → catch 转成 Error → StateFlow 保存 → Activity 渲染 Error
```

`stateIn/shareIn` 在传入作用域中启动共享上游收集协程。未处理的上游异常会结束这个协程，交给该作用域的异常机制处理；不会作为流异常直接传给下游订阅者。因此在转换后添加 `catch`，捕获不到这个上游异常。

不能把“异常不直接传给订阅者”理解成异常被自动安全处理；是否影响父作用域、是否成为未处理异常，仍取决于作用域的异常机制。

上游正常结束也不会让共享流的 `collect` 正常返回。需要给页面表达结束或失败时，应将其转成明确的数据状态。

参照：[stateIn](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/state-in.html)、[shareIn](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/share-in.html)。

## 36. 当前 mvvmflow 的请求与页面收集怎样串起来？

项目入口：[PostViewModel](../app/src/main/java/com/example/android_basic/feature/mvvmflow/ui/PostViewModel.kt)、[PostListActivity](../app/src/main/java/com/example/android_basic/feature/mvvmflow/ui/PostListActivity.kt)、[PostUiState](../app/src/main/java/com/example/android_basic/feature/mvvmflow/ui/PostUiState.kt)、[PostRepository](../app/src/main/java/com/example/android_basic/feature/mvvmflow/data/repository/PostRepository.kt)。

首次获取 ViewModel 时：

```text
创建 PostViewModel
    → MutableStateFlow 初始为 Loading
    → init 调用 loadPosts()
    → loadJob 正在运行则忽略重复请求
    → viewModelScope.launch
    → PostRepository.getPosts()
    → safeRequest { Retrofit suspend 接口 }
    → NetworkResult.Success / Failure
    → 映射 Success / Empty / Error
    → 更新 _uiState.value
```

Activity 中：

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            render(state)
        }
    }
}
```

开始收集先收到当前状态，之后接收变化。请求很快时可能直接收到最终状态，不保证先显示一次加载画面。

| 场景 | 当前代码的行为 |
| --- | --- |
| 首次打开 | 新建 ViewModel 时自动请求 |
| 刷新、重试 | 调用 `loadPosts()`；加载中忽略重复操作 |
| 退到后台 | 停止页面收集，ViewModel 中的请求可以继续 |
| 回到前台 | 收集当前最新状态，不因此重新发请求 |
| 旋转屏幕 | 复用保留的 ViewModel，已有状态和请求继续，`init` 不重跑 |
| 页面真正结束、ViewModel 被清理 | `viewModelScope` 取消，请求随之取消 |

当前网络层通过 `safeRequest` 将普通请求异常映射为公共 `NetworkError`；取消异常重新抛出，不转成错误状态。Repository 返回 `NetworkResult<List<Post>>` 的挂起函数，未改成返回 Flow。

**当前方案是 `loadPosts()` 主动请求、MutableStateFlow 保存状态、Activity 按生命周期收集。** `asStateFlow()` 仅暴露只读状态。前面演示的 `stateIn/shareIn`、搜索切换流是拓展用法，没有混入这套实际请求流程。

## 官方核对入口

- [Kotlin Flow：冷流、上下文与异常](https://kotlinlang.org/docs/coroutines-flow.html)
- [Android：StateFlow 与 SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [debounce](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/debounce.html)
- [flatMapLatest](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-latest.html)、[flatMapConcat](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-concat.html)、[flatMapMerge](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/flat-map-merge.html)

更多协程取消、作用域和统一网络异常基础见 [Kotlin 协程笔记](kotlin-coroutines.md)。
