# Kotlin 协程：问题复习

整理日期：2026-11-12。按本次会话的问题与追问整理，先看执行过程，再联系 Android 和当前项目；不使用 Flow。

明确要求忽略的独立知识点未收录。下文标记“重点”的内容，是会话中明确要求重点记录的部分。

## 阅读导航

| 部分 | 问题 |
| --- | --- |
| 挂起与线程 | 1—4：suspend、launch、主线程、delay 与阻塞 |
| 执行环境 | 5—6：withContext、调度器 |
| 任务与生命周期 | 7—10：Job、取消、取消异常、viewModelScope |
| 并发与作用域 | 11—15：父子任务、async/await、coroutineScope、失败传播、supervisorScope |
| 超时和收尾 | 16—20：超时、finally、NonCancellable、cancelAndJoin |
| 项目异常处理 | 21—23：safeRequest、公共错误类型、CoroutineExceptionHandler |
| 进一步理解 | 24—28：runBlocking、Continuation、状态机、回调转换、CoroutineContext |
| 共享数据 | 29—31：count 的竞态、Mutex、临界区范围 |

## 项目代码与教学示例的区别

当前项目的实际调用链是：

```text
PostViewModel.loadPosts()
    → viewModelScope.launch
    → PostRepository.getPosts()
    → safeRequest { api.getPosts() }
    → NetworkResult<List<Post>>
    → LiveData<PostUiState>
    → PostListActivity.render()
```

| 调用或类型 | 当前含义 |
| --- | --- |
| `api.getPosts()` | Retrofit 挂起接口，返回 `List<Post>`；请求错误会抛异常 |
| `repository.getPosts()` | 返回 `NetworkResult<List<Post>>`；普通请求错误已经转换为 `Failure` |
| `NetworkError` | 公共错误分类，包含 `NETWORK`、`HTTP`、`UNKNOWN` |
| `PostUiState.Error` | 直接携带公共 `NetworkError`，已删除 `PostError` |

涉及“作者接口”“页面聚合数据”“下载任务”的代码都是教学假设，当前项目没有这些功能。并发和异常传播示例中的 `postApi`、`authorApi` 表示直接返回数据的接口，便于展示未处理异常的传播；实际 Repository 的 `Failure` 需要通过 `when` 判断，不会自动抛出。

项目入口：[PostViewModel](../app/src/main/java/com/example/android_basic/feature/mvvm/ui/PostViewModel.kt)、[PostRepository](../app/src/main/java/com/example/android_basic/feature/mvvm/data/repository/PostRepository.kt)、[NetworkPostRepository](../app/src/main/java/com/example/android_basic/feature/mvvm/data/repository/NetworkPostRepository.kt)、[PostApi](../app/src/main/java/com/example/android_basic/feature/mvvm/data/remote/PostApi.kt)。

## 1. suspend 的挂起与恢复是什么意思？与线程、launch 有什么关系？

**挂起是协程暂时等待，恢复是满足条件后从暂停的位置继续执行。等待期间，线程可以处理其他工作。**

```kotlin
viewModelScope.launch {
    _uiState.value = PostUiState.Loading

    val result = repository.getPosts()

    // 请求完成后才到这里，根据 result 更新界面状态。
}
```

执行过程：

1. `launch` 启动协程，按 `viewModelScope` 的默认配置在主线程执行。
2. 更新加载状态，调用 Repository，再进入 Retrofit 请求。
3. 请求结果尚未准备好，协程挂起；后面的状态更新暂不执行。
4. 主线程继续处理点击、绘制等工作。
5. 请求完成，协程恢复，在主线程接收结果并更新状态。

| 概念 | 作用 |
| --- | --- |
| 线程 | 实际执行代码 |
| 协程 | 可以挂起、恢复的任务，其执行由协程机制和调度器管理 |
| `launch` | 启动协程，返回 `Job`；不代表创建新线程 |
| `suspend` | 声明函数允许挂起；不保证一定挂起，也不自动切换到后台 |

普通代码一般需要通过协程构建器进入协程，才能调用挂起函数；挂起函数内部也可以继续调用其他挂起函数。

给一个阻塞函数加上 `suspend`，不会自动让它变得不阻塞。项目等待网络时不阻塞主线程，依赖的是 Retrofit 的异步挂起实现。

## 2. viewModelScope.launch 会阻塞后面的代码吗？

**它不要求后面的代码等待整个协程完成。但在主线程调用时，协程通常会立即执行到第一次挂起或结束，才返回到 launch 后面的代码。**

```kotlin
fun loadPosts() {
    println("A：launch 前")

    viewModelScope.launch {
        println("B：开始请求")
        val result = repository.getPosts()
        println("C：请求完成：$result")
    }

    println("D：launch 后")
}
```

假设在主线程调用，而且请求确实挂起等待，顺序是：

```text
A → B → 请求挂起 → D → 网络完成 → C
```

`loadPosts()` 可以先返回，协程留待之后恢复。调用方的普通函数不会因此一起挂起。

如果协程在主线程上立即执行很长的计算且不挂起，这部分代码仍会占用主线程，后面的 D 也要等待它让出执行机会。`launch` 不会自动把耗时计算放到后台。

## 3. viewModelScope.launch 默认是主线程吗？

**在当前 Android 项目中是。launch 继承 viewModelScope 的调度配置，默认使用 `Dispatchers.Main.immediate`。**

- 调用时已经在主线程：可以立即进入协程体。
- 调用时在其他线程：安排到主线程再执行。
- 等待 Retrofit 请求时：协程可以挂起，底层网络传输由网络库处理。
- 请求完成后：协程按原有调度配置继续在主线程执行。

协程入口位于主线程，不代表底层网络传输也在主线程执行。主线程上的普通耗时计算仍然可能卡住界面。

## 4. delay 和 Thread.sleep 都等待，为什么效果不同？

```kotlin
viewModelScope.launch {
    println("A")
    delay(2_000)
    println("B")
}
println("C")
```

假设在主线程调用，执行顺序是 `A → C → 约 2 秒后 B`。`delay()` 挂起协程，等待期间不占用主线程。

把 `delay()` 换成 `Thread.sleep()`，主线程会被阻塞，顺序变成 `A → 阻塞约 2 秒 → B → C`。

| 方法 | 等待时发生什么 |
| --- | --- |
| `delay()` | 协程挂起，线程可以处理其他工作 |
| `Thread.sleep()` | 阻塞执行它的线程；主线程执行时会影响界面响应 |

因此，这样仍然会阻塞调用线程：

```kotlin
suspend fun waitTwoSeconds() {
    Thread.sleep(2_000)
}
```

判断是否阻塞，要看函数内部的具体实现。

## 5. 【重点】withContext 如何切换执行环境、等待并返回结果？

**withContext 临时使用指定的上下文执行代码块，等待代码块和其中子任务完成，再回到调用方的上下文继续执行。**

假设需要读取本地缓存文件：

```kotlin
viewModelScope.launch {
    println("A：准备读取") // 主线程

    val text = withContext(Dispatchers.IO) {
        println("B：读取文件") // IO 后台线程
        cacheFile.readText()
    }

    println("C：读取到 ${text.length} 个字符") // 回到主线程
}
```

执行过程：

1. 外层协程在主线程执行 A。
2. 文件读取安排到 IO 线程池，执行 B 和 `readText()`。
3. 外层协程等待读取完成；主线程可以处理界面工作。
4. 代码块最后一个表达式 `readText()` 的结果成为 `withContext` 的返回值。
5. 回到主线程，结果赋给 `text`，执行 C。

**“等待结果”与“阻塞线程”不同：C 等待读取完成，但主线程不需要一直守着文件读取。**

| 对照 | `launch` | `withContext` |
| --- | --- | --- |
| 调用方式 | 启动并发任务 | 进入指定上下文，完成后才返回 |
| 返回值 | `Job` | 代码块的结果 |
| 后续代码 | 不要求等任务完成 | 等本次调用完成后继续 |

`withContext` 是否真的切换到另一条线程取决于上下文和调度器。本例从主线程进入 `Dispatchers.IO`，文件读取会安排到后台；它本身不是创建一条专用新线程。

当前项目的 Retrofit 挂起接口已经异步执行网络请求，无需为了“网络请求”再额外包一层 `Dispatchers.IO`。`safeRequest` 也不会自动切换线程。

## 6. Main、IO、Default 怎么选择？

| 调度器 | 适用工作 |
| --- | --- |
| `Dispatchers.Main` | 更新 UI、简短的界面逻辑 |
| `Dispatchers.IO` | 阻塞式文件读写、阻塞式网络调用等等待外部资源的工作 |
| `Dispatchers.Default` | 大量排序、复杂统计等持续使用 CPU 的工作 |

假设文章需要进行耗时统计：

```kotlin
viewModelScope.launch {
    val statistics = withContext(Dispatchers.Default) {
        calculateStatistics(posts)
    }

    // 回到主线程使用 statistics。
}
```

简单读取 `posts.size` 等操作可直接执行。对确实耗时的阻塞或计算操作，在合适的数据处理层选择执行环境，让调用方可以安全地从主线程调用。

## 7. launch 返回的 Job 是什么？isActive 表示什么？

**Job 代表一次协程任务，用来管理状态、取消和等待；它不直接保存任务的业务结果。**

项目代码：

```kotlin
private var loadJob: Job? = null

fun loadPosts() {
    if (loadJob?.isActive == true) return

    loadJob = viewModelScope.launch {
        // 加载文章并更新状态。
    }
}
```

1. 初次调用时，`loadJob` 为 `null`，可以启动任务。
2. 请求执行期间再次调用，`isActive` 为 `true`，直接返回。
3. 整个任务正常完成后，`isActive` 为 `false`，可以再次加载。

**挂起等待网络时，isActive 仍然可以是 true。** 它表示任务处于活动状态，不表示这一刻正在占用线程。

## 8. Job.cancel() 如何取消任务？取消等于暂停吗？

```kotlin
loadJob?.cancel()
```

这会发出取消请求。假设正在等待 Retrofit：

1. 挂起调用响应取消。
2. 取消传递给底层 HTTP 请求。
3. 挂起调用通过 `CancellationException` 退出，正常的成功处理不再执行。

| 状态 | 含义 |
| --- | --- |
| 挂起 | 暂时等待，之后可以恢复 |
| 取消 | 请求结束任务；重新执行需要创建新任务 |

取消依赖代码配合，不是强行终止线程。支持取消的挂起函数可以响应取消；一段不响应取消的长时间计算不会因为调用 `cancel()` 就被强行打断。

## 9. 为什么重新抛出 CancellationException？没有外层 catch 会 crash 吗？

```kotlin
try {
    api.getPosts()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    // 处理业务错误。
}
```

`CancellationException` 是 `Exception` 的子类。如果只写宽泛的 `catch (Exception)`，取消也会被捕获，可能被误显示成“请求失败”，取消后的普通业务代码也可能继续执行。

执行过程：

```text
任务被取消
    → 挂起调用抛出 CancellationException
    → 专门的 catch 原样重新抛出
    → 退出业务代码
    → 协程框架按取消流程处理
```

**在这里重新抛出取消异常，不会因为没有普通的外层 catch 就导致应用 crash。协程框架识别取消异常，并按任务取消处理。** 其他未处理异常从 `launch` 抛出，在 Android 中则可能导致 crash。

没有捕获取消异常时，它本来就会自然向外传播；不必在每个挂起函数中重复添加捕获再抛出。当前项目把这个规则集中在公共 `safeRequest` 中。

## 10. viewModelScope 与页面生命周期有什么关系？

**viewModelScope 随 ViewModel 被清除而自动取消，跟随的是 ViewModel 的生命周期。**

| 操作 | ViewModel | 已启动的请求 |
| --- | --- | --- |
| 旋转屏幕，Activity 因配置变化重建 | 原 ViewModel 保留 | 可以继续 |
| 按 Home 进入后台 | 通常保留 | 可以继续 |
| 按返回键结束当前示例 Activity | 对应 ViewModel 被清除 | 未完成任务自动取消 |

项目通过 `by viewModels` 获取 ViewModel。配置变化后，新 Activity 重新观察原 ViewModel 的 `LiveData`，已有状态可以继续交给界面渲染；原 ViewModel 的 `init` 不会因为这次重建再执行一次。

ViewModel 被清除时，无需为了结束 `viewModelScope` 中的任务，再手动逐个取消它们。进程重建不是配置变化保留，当前示例没有磁盘缓存。

## 11. 父协程与子协程是什么关系？

```kotlin
val parentJob = viewModelScope.launch {
    println("A：父协程开始")

    launch {
        delay(1_000)
        println("C：子协程结束")
    }

    println("B：父协程代码到末尾")
}
```

内层直接调用的 `launch` 使用当前作用域，创建子任务。通常输出 `A → B → 约 1 秒后 C`。

**父协程的代码块到末尾，不代表整个父任务已经完成。** 父任务正常完成前，需要等子任务完成。

取消父任务，也会向下取消尚未完成的子任务。这是结构化并发的基本规则之一。

## 12. 【重点】async / await 怎样让两个请求并发执行并返回结果？

假设文章页需要文章和作者两份数据，两个接口互不依赖。直接顺序调用时：

```kotlin
val posts = postApi.getPosts()
val authors = authorApi.getAuthors()
```

先等文章，再开始请求作者。如果分别耗时 2 秒和 1 秒，总时间约 3 秒。

先启动两个任务，再取结果：

```kotlin
viewModelScope.launch {
    val postsTask = async {
        postApi.getPosts()
    }
    val authorsTask = async {
        authorApi.getAuthors()
    }

    val posts = postsTask.await()
    val authors = authorsTask.await()

    println("${posts.size} 篇文章，${authors.size} 位作者")
}
```

执行过程：

1. 第一个 `async` 发起文章请求，等待时挂起。
2. 外层继续执行，第二个 `async` 发起作者请求。
3. `postsTask.await()` 等待文章结果；作者任务仍可以继续进行。
4. 文章返回后，再取作者结果。作者任务已完成时，`await()` 直接取得结果。
5. 两份结果都有了，继续后面的代码。

等待时间可以重叠，总耗时约为较慢请求的 2 秒，不包含额外处理等开销。

| 写法 | 返回什么 |
| --- | --- |
| `launch { ... }` | `Job`，用于任务管理 |
| `async { ... }` | `Deferred<T>`，代表将来可以取得的结果 |
| `task.await()` | 结果 `T`；未就绪时挂起等待，也可能抛出任务的异常 |

`async` 的结果来自代码块最后一个表达式。它也继承作用域的执行环境，不自动切到后台线程；异步网络等待可以在同一主线程调度下重叠进行。

如果第一个 `async` 后立即 `await()`，再启动第二个任务，等待就仍然是顺序的。

## 13. 【重点】coroutineScope 是启动一个新的协程吗？为什么要包住两个任务？

**它创建一个子作用域，在当前调用流程中执行，并等待代码块及所有子任务完成后才返回。真正启动并发子任务的是里面的 launch / async。**

实现上有用于管理这个作用域的协程对象和自身的 `Job`。使用时，它不像 `launch` 那样启动任务后就把 `Job` 返回给调用方；调用方需要等这次 `coroutineScope` 完成。

```kotlin
viewModelScope.launch {
    coroutineScope {
        launch { /* 任务 A */ }
        launch { /* 任务 B */ }
    }

    println("A、B 都已经结束")
}
```

执行过程：进入子作用域 → 启动 A、B → 作用域代码到末尾 → 等 A、B 全部结束 → 返回 → 打印。

### 追问：外层协程本来会等子任务，为什么还需要 coroutineScope？

```kotlin
viewModelScope.launch {
    var count = 0

    repeat(2) {
        launch {
            delay(100)
            count++
        }
    }

    println(count) // 子任务还在等待，打印 0。
}
```

要区分两个时间点：

- 外层任务正式完成前，会等待子任务。
- 外层代码中的这行 `println()` 不会因此自动延迟到子任务完成后。

把启动任务的部分放进 `coroutineScope`，就明确把“等待它们结束”的位置放到了打印之前。等待时挂起调用方，不阻塞主线程。

### 怎样把并发操作封装进挂起函数？

```kotlin
suspend fun loadPageData(): PageData = coroutineScope {
    val postsTask = async { postApi.getPosts() }
    val authorsTask = async { authorApi.getAuthors() }

    PageData(
        posts = postsTask.await(),
        authors = authorsTask.await()
    )
}
```

`PageData` 是假设用来保存两份数据的模型。普通 `suspend` 函数不会自动提供 `async` 所需要的 `CoroutineScope`；`coroutineScope` 可以提供这个组织子任务的范围。

调用方能用顺序方式拿到聚合结果：

```kotlin
viewModelScope.launch {
    val data = loadPageData()
    // 两份数据都准备好后，才到这里。
}
```

`coroutineScope` 本身不切换线程。调用方取消时，取消会沿父子关系传入子任务。

## 14. coroutineScope 中一个任务失败，另一个会怎样？

**子任务发生未捕获的非取消异常，会让作用域失败，并取消其他尚未完成的子任务。**

以前面的文章和作者请求为例：

1. 两个任务都已启动。
2. 文章请求抛出未捕获的 `IOException`。
3. `coroutineScope` 失败，作者任务被取消。
4. 子任务结束后，异常从 `coroutineScope` 向调用方抛出。
5. 调用方可以在 `loadPageData()` 外用 `try-catch` 处理整个加载失败。

这个规则适合“多份结果必须一起成功，才能完成整体工作”的情况。

### 追问：只在 authorsTask.await() 外加 try-catch，能隔离失败吗？

不能保证。**任务失败的传播不等待 await。** 作者子任务一旦发生未捕获异常，作用域的失败与取消就已经触发。

即使捕获了 `await()` 抛出的异常，也不能撤销已经发生的取消；如果前面正在等待文章，甚至可能先因取消退出，走不到作者的 `await()`。

如果作者请求允许失败，可以在作者子任务内部处理：

```kotlin
val authorsTask = async {
    try {
        authorApi.getAuthors()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IOException) {
        emptyList()
    }
}
```

网络错误在子任务内部转换为空列表，该子任务以正常结果完成，没有未处理的网络异常向父任务传播。

| 捕获位置 | `coroutineScope` 中的效果 |
| --- | --- |
| `async` 内部 | 可以把预期错误转换成正常返回值，避免此次失败向上传播 |
| 仅在 `await()` 外部 | 子任务已经失败，不能据此撤销作用域取消 |

## 15. supervisorScope 怎样隔离失败？还需要 try-catch 吗？

**子任务自身失败，不会直接让 supervisorScope 取消其他子任务；失败结果仍然需要处理。**

假设文章必须成功，作者网络错误允许用空列表降级：

```kotlin
suspend fun loadPageData(): PageData = supervisorScope {
    val postsTask = async { postApi.getPosts() }
    val authorsTask = async { authorApi.getAuthors() }

    val posts = postsTask.await()
    val authors = try {
        authorsTask.await()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: IOException) {
        emptyList()
    }

    PageData(posts = posts, authors = authors)
}
```

作者先发生网络错误时：作者任务失败 → 文章任务继续 → 文章完成 → 作者的 `await()` 抛出异常 → 捕获并转换为空列表 → 返回页面数据。

两部分职责不同：

- `supervisorScope` 隔离子任务的失败传播。
- `try-catch` 决定如何处理失败结果。

如果 `await()` 的异常没有捕获，异常会从作用域代码块抛出；**作用域本身失败时，仍会取消其他未完成的子任务。** 例如，上例文章请求失败后，`postsTask.await()` 抛出的异常会让整个加载退出。

## 16. withTimeout 如何限制等待时间？

```kotlin
viewModelScope.launch {
    try {
        val result = withTimeout(5_000) {
            repository.getPosts()
        }
        println(result)
    } catch (_: TimeoutCancellationException) {
        println("请求超时")
    } catch (cancelled: CancellationException) {
        throw cancelled
    }
}
```

1. 进入 `withTimeout`，开始计算时间限制。
2. 5 秒内完成，就返回结果，不需要等满 5 秒。
3. 超时仍未完成，就取消内部任务，并抛出 `TimeoutCancellationException`。
4. 支持取消的 Retrofit 请求响应取消。

`TimeoutCancellationException` 是 `CancellationException` 的子类。若需要把超时显示成页面错误，应先单独处理超时，再向外传递其他取消。

项目当前 `safeRequest` 会重新抛出取消异常，所以这类协程超时不会被它转换成普通 `Failure`。底层网络库自身的连接、读写超时若以 `IOException` 抛出，则会分类为 `NETWORK`。

超时也依赖内部操作响应取消，不能强行打断一段不响应取消的长时间计算。

## 17. withTimeoutOrNull 为什么返回 null？会吞掉所有异常吗？

```kotlin
val result = withTimeoutOrNull(5_000) {
    repository.getPosts()
}

if (result == null) {
    println("本次等待超时")
} else {
    // 继续判断 NetworkResult.Success / Failure。
}
```

它只把**自己设置的超时**转换成 `null`。

| 情况 | 结果 |
| --- | --- |
| 及时完成 | 返回代码块结果 |
| 自己的超时发生 | 取消内部任务，返回 `null` |
| 代码块抛出普通异常 | 异常继续向外传播 |
| 外层任务被取消 | 继续按取消流程退出 |

对直接返回 `List<Post>` 的 API，`null` 可表示超时，空列表表示成功但没有数据。当前 Repository 返回的是 `NetworkResult<List<Post>>`，因此 `null` 表示这层超时，非空结果还要进一步判断成功和失败。

## 18. 协程取消后会执行 finally 吗？重新抛出异常会跳过它吗？

**已进入 try 的协程，因正常完成、异常或协程取消离开时，都会经过对应的 finally。重新抛出取消异常不会跳过它。**

```kotlin
viewModelScope.launch {
    try {
        repository.getPosts()
    } finally {
        println("本次加载结束，执行收尾")
    }
}
```

请求期间取消时：挂起调用抛出取消异常 → 进入 `finally` 收尾 → 取消异常继续向外传播。

如果有 `catch`，先执行匹配的 `catch`，即使里面重新抛出异常，也仍然会经过 `finally`。

`finally` 适合放资源关闭、监听移除、结束日志等收尾操作。进入这里不代表请求成功；还没开始执行就被取消的协程，也不能指望它执行尚未进入的 `try-finally`。

## 19. finally 中也需要挂起清理，为什么用 NonCancellable？

已经取消的协程中，`finally` 里的普通代码可以执行，但 `delay()` 等支持取消的挂起函数会响应取消，可能中断后续收尾。

```kotlin
viewModelScope.launch {
    try {
        repository.getPosts()
    } finally {
        withContext(NonCancellable) {
            delay(200) // 模拟必要的挂起式清理。
            println("清理完成")
        }
    }
}
```

执行过程：请求取消 → 进入 `finally` → 清理块允许挂起等待 → 清理完成 → 原来的取消继续向外传播。

`NonCancellable`：

- 允许这一小段必要的清理完成，不撤销整个任务的取消。
- 本身不是调度器，不自动切到后台线程。
- 适合必要、耗时可控的挂起式收尾，不应把整段正常业务工作都包进去。

当前 Retrofit 请求的取消由网络库处理，不需要为了请求本身添加这层包装。

## 20. cancelAndJoin 与 cancel 有什么区别？

| 方法 | 作用 |
| --- | --- |
| `cancel()` | 请求取消，不专门等待任务完成全部收尾 |
| `cancelAndJoin()` | 请求取消，并挂起等待任务结束 |

```kotlin
viewModelScope.launch {
    val oldJob = launch {
        try {
            delay(10_000)
        } finally {
            withContext(NonCancellable) {
                delay(200)
                println("A：旧任务清理完成")
            }
        }
    }

    delay(100) // 让旧任务先运行起来。
    oldJob.cancelAndJoin()
    println("B：旧任务已结束")
}
```

A 一定先于 B：外层协程会等旧任务完成收尾，再继续。等待期间不阻塞主线程。

`join` 等待任务结束，不负责取得业务结果；获取结果用 `await()`。如果功能要求“取消旧加载，等它结束，再开始新加载”，可以用这种组织方式。当前文章页采用加载中忽略重复请求的策略。

## 21. 网络异常如何统一处理？当前项目最终采用了什么方案？

**公共层捕获并分类请求异常，返回统一结果；页面根据结果更新状态。取消异常继续传播。**

项目公共定义：

```kotlin
enum class NetworkError {
    NETWORK,
    HTTP,
    UNKNOWN,
}

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Failure(val error: NetworkError) : NetworkResult<Nothing>
}
```

统一包装：

```kotlin
suspend fun <T> safeRequest(block: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (cancelled: CancellationException) {
        // 取消交回协程框架处理，不能转换成页面请求失败。
        throw cancelled
    } catch (exception: Exception) {
        val error = when (exception) {
            is IOException -> NetworkError.NETWORK
            is HttpException -> NetworkError.HTTP
            else -> NetworkError.UNKNOWN
        }
        NetworkResult.Failure(error)
    }
}
```

执行过程：执行请求 Lambda → 成功包装数据，失败分类并包装错误 → 调用方判断结果；取消则通过异常退出。

网络 Repository 接入：

```kotlin
override suspend fun getPosts(): NetworkResult<List<Post>> = safeRequest {
    api.getPosts()
}
```

ViewModel 使用：

```kotlin
_uiState.value = when (val result = repository.getPosts()) {
    is NetworkResult.Success -> {
        if (result.data.isEmpty()) {
            PostUiState.Empty
        } else {
            PostUiState.Success(result.data)
        }
    }
    is NetworkResult.Failure -> PostUiState.Error(result.error)
}
```

错误分类是当前示例的简化策略：`IOException` 对应连接、读写等网络问题；`HttpException` 对应非成功 HTTP 状态；其他 `Exception` 对应未知错误。取消始终先单独处理。

代码入口：[NetworkResult](../app/src/main/java/com/example/android_basic/core/network/NetworkResult.kt)、[safeRequest](../app/src/main/java/com/example/android_basic/core/network/SafeRequest.kt)。

## 22. 为什么公共结果、请求包装和错误类型放在 core.network？为什么删除 PostError？

这些类型没有文章业务依赖，各功能都能复用，因此统一放入 `com.example.android_basic.core.network`。

| 位置 | 内容 |
| --- | --- |
| `core.network` | `NetworkClient`、`NetworkResult`、`NetworkError`、`safeRequest` |
| `feature.mvvm.data` | `Post`、`PostApi`、文章 Repository |
| `feature.mvvm.ui` | `PostUiState`、ViewModel、Activity |

当前只有 `:app` 一个 Gradle 模块，`core.network` 是公共包；以后拆分多个功能模块，可以提取为真正的 `:core:network` library，供各模块依赖。

原 `PostError` 与公共错误一一对应，没有文章特有的语义，所以已经删除重复定义和转换：

```kotlin
data class Error(val reason: NetworkError) : PostUiState
```

从 `NetworkResult.Failure` 到页面状态，全程携带同一个 `NetworkError`。Activity 再按类型选择字符串资源：

```kotlin
when (state.reason) {
    NetworkError.NETWORK -> R.string.posts_network_error
    NetworkError.HTTP -> R.string.posts_server_error
    NetworkError.UNKNOWN -> R.string.posts_unknown_error
}
```

公共层负责错误分类，页面负责文案和展示。公共网络层不依赖具体 feature、Activity、LiveData 或页面资源。

## 23. 截图中的 CoroutineExceptionHandler 是什么？适合统一请求异常吗？

**它是协程上下文中的未处理异常回调入口，适合在合适的协程边界做异常兜底。**

```kotlin
val handler = CoroutineExceptionHandler { _, exception ->
    println("任务失败：${exception.message}")
}

viewModelScope.launch(handler) {
    val posts = postApi.getPosts() // 直接调用 API，假设抛出未处理异常。
    println(posts) // 失败后不会继续到这里。
}
```

第一个回调参数是异常对应的上下文，示例用 `_` 忽略；第二个是异常对象。截图中的 `errorContext` 只是创建处理器，再把异常转交给 `onError`。

执行过程：启动任务 → 请求抛出未捕获异常 → 协程失败 → 处理器收到异常 → 执行日志、上报或通知等逻辑。

**收到回调时，协程已经失败，处理器不能让原协程从出错处继续运行。** 在协程内部用 `try-catch` 处理错误，则可以继续后面的代码。

`viewModelScope` 默认使用 `SupervisorJob`，它直接启动的 `launch` 可以使用这种方式处理未捕获异常。它不是所有子协程和所有 `async` 的通用 `catch`：普通子任务的异常会按父子关系传播；`async` 将异常保存在 `Deferred` 中，同时普通父子关系下的失败仍可能影响父任务。

| 方案 | 失败时的行为 |
| --- | --- |
| `safeRequest` | 在请求内部捕获，转换成 `Failure`，调用方继续判断 |
| `CoroutineExceptionHandler` | 协程因未处理异常失败后，接收通知 |

当前项目里，已转换成 `Failure` 的错误不会再触发处理器。正常取消也不会作为这类未处理错误通知它；必要收尾仍放在 `finally` 中。

截图的 `launch(onError, block)` 封装若用于项目，需要留意：

- 返回底层 `launch` 的 `Job`，才能继续管理活动状态与取消；截图默认返回 `Unit`。
- `viewModelScope` 已默认使用 `Main.immediate`，显式传 `Main` 会改变立即执行的调度行为。
- 处理器不保证回调线程；若更新 UI 或 `LiveData.value`，需保证主线程。处理器自身也应避免再次抛出异常。

## 24. runBlocking 为什么会阻塞线程？里面不是可以 delay 吗？

```kotlin
fun main() {
    println("A：进入前")

    val result = runBlocking {
        delay(1_000)
        "加载完成"
    }

    println("B：$result")
}
```

打印 A → `runBlocking` 启动协程并阻塞调用线程等待完成 → 内部协程挂起等待约 1 秒 → 返回字符串 → 打印 B。

- `delay` 挂起内部协程。
- `runBlocking` 仍然让调用线程等待整个任务完成；它也会等待子任务。

因此，在 Android 主线程上用 `runBlocking { repository.getPosts() }`，等待期间会影响界面消息处理。项目页面加载继续使用 `viewModelScope.launch`。

`runBlocking` 常见于命令行入口、简单演示，以及需要同步调用挂起代码的边界。

## 25. Continuation 是什么？挂起后为什么记得局部变量？

**Continuation 表示后续计算如何继续，编译器生成的对象保存恢复所需的状态。**

```kotlin
viewModelScope.launch {
    val startTime = System.currentTimeMillis()
    println("A：开始请求")

    val result = repository.getPosts()

    println("B：开始时间是 $startTime，结果是 $result")
}
```

请求挂起后，线程可以执行其他工作。恢复时仍然能使用 `startTime`，是因为编译器生成的代码保存了后面需要的数据及执行进度。

可以理解为：

```text
保存的状态：等待 getPosts() 返回
恢复要用的数据：startTime
恢复入口：接收结果，继续后面的代码
```

编译时，挂起函数会增加隐藏的 `Continuation` 参数，调用方不用手写回调参数。恢复可以传入结果，也可以传入异常。

Continuation 及生成的代码承接结果并记录如何继续；调度器决定恢复执行安排到哪里运行。

## 26. 编译器怎样用状态机记录执行进度？

编译器围绕可能挂起的位置组织执行阶段，用进度字段选择恢复入口，字段通常叫 `label`。

以第 25 节为例，用 `0`、`1` 示意：

1. 初次进入，`label = 0`，记录时间并打印 A。
2. 调用请求前，把进度设为 `label = 1`，表示恢复后要进入请求返回阶段。
3. 若请求挂起，结束本次执行，保留状态。
4. 请求完成后进入恢复入口，根据 `label = 1` 接收返回值或异常。
5. 继续打印 B，不会重新记录时间或再打印一次 A。

进度、保存的局部变量、恢复入口共同支持暂停和恢复。这里是执行模型示意，不是编译产物的逐字还原。

如果挂起函数立即返回结果，则无需真的挂起，可以直接在本次执行中继续。调用挂起函数不等于必然暂停。

## 27. 怎样把老式回调接口转换成 suspend 调用？

**suspendCancellableCoroutine 可以包装一次性回调，连接结果返回、协程恢复及取消。**

假设旧接口成功和失败二选一，只返回一次结果，并返回可取消的请求对象：

```kotlin
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun loadPostsAwait(): List<Post> =
    suspendCancellableCoroutine { continuation ->
        val request = oldApi.loadPosts(
            onSuccess = { posts ->
                continuation.resume(posts)
            },
            onFailure = { error ->
                continuation.resumeWithException(error)
            }
        )

        continuation.invokeOnCancellation {
            request.cancel()
        }
    }
```

执行过程：

1. 调用挂起函数，注册回调并发起请求。
2. 结果尚未就绪，协程挂起。
3. 成功回调调用 `resume(posts)`，提交结果，协程按原调度配置恢复。
4. 失败回调调用 `resumeWithException(error)`，让挂起调用以异常完成。
5. 外层取消时，取消回调调用 `request.cancel()`，把取消传给底层请求。

旧 API 的取消操作需要支持这种并发取消调用。示例假设接口遵守一次性回调约定。

可接入公共请求包装：

```kotlin
val result = safeRequest { loadPostsAwait() }
```

失败回调变成异常，再由 `safeRequest` 分类；取消继续传播。当前 Retrofit 挂起接口已经提供对应能力，无需再手动套这一层。

## 28. CoroutineContext 是什么？上下文里的加号有什么作用？

**CoroutineContext 是一组运行配置，元素通过 Key 区分。**

| 元素 | 作用 |
| --- | --- |
| `Job` | 任务状态、父子关系、取消 |
| 调度器 | 执行如何安排，例如主线程或后台线程池 |
| `CoroutineExceptionHandler` | 在适用边界接收未处理异常 |

`viewModelScope.launch(handler)` 继承作用域的配置，添加处理器，并创建新的子任务及其 Job，保持由父作用域管理的取消关系。

```kotlin
val context = handler + Dispatchers.Main
```

`+` 表示组合上下文元素。相同 Key 的元素组合时，右边替换左边：

```kotlin
Dispatchers.Main + Dispatchers.IO // 最终保留 IO 调度器。
```

因此，显式传 `Dispatchers.Main` 会覆盖默认的 `Main.immediate` 调度配置。

## 29. 为什么两个协程给 count 加一，最终可能得到 1？

```kotlin
viewModelScope.launch {
    var count = 0

    coroutineScope {
        repeat(2) {
            launch {
                val oldCount = count
                delay(100)
                count = oldCount + 1
            }
        }
    }

    println(count) // 可能是 1。
}
```

即使都在主线程，挂起也允许执行过程交错：

| 顺序 | 操作 | count |
| --- | --- | --- |
| 1 | A 读取旧值 0，挂起 | 0 |
| 2 | B 也读取旧值 0，挂起 | 0 |
| 3 | A 恢复，写入 0 + 1 | 1 |
| 4 | B 恢复，也写入 0 + 1 | 1 |

这里 `coroutineScope` 已保证打印发生在任务完成后。问题是“读取旧值 → 等待 → 写回”的整体操作发生交错，导致一次更新丢失。

同一主线程上一段没有挂起点的 `count++` 不会被协程机制在中间自动切开；换成多线程并发时，还需要另外考虑共享读写的线程安全。

项目中，如果允许多个文章请求同时更新状态，也可能出现后发请求先返回、先发请求后返回，旧结果覆盖较新结果的逻辑竞态。现有 `loadJob.isActive` 检查通过忽略加载期间的新操作，避免这种请求竞争。

## 30. 如何让两个协程读取到前一个更新后的 count？

**用同一个 Mutex，保护从读取到写回的整个操作。**

```kotlin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

viewModelScope.launch {
    var count = 0
    val mutex = Mutex()

    coroutineScope {
        repeat(2) {
            launch {
                mutex.withLock {
                    val oldCount = count
                    delay(100)
                    count = oldCount + 1
                }
            }
        }
    }

    println(count) // 两个任务正常完成后为 2。
}
```

假设 A 先拿到锁：

1. A 读取 0；`delay` 挂起期间仍持有锁。
2. B 等待锁，暂时不能读取旧值；等待时挂起，不阻塞线程。
3. A 更新为 1，离开代码块，自动释放锁。
4. B 拿到锁，读取 1，最终更新为 2。
5. 两个任务完成后打印 2。

只保护读取、读完就释放锁，无法保证后面的写回一致。所有需要相互协调的访问必须使用同一把锁；每个任务各自创建一把锁，不能起到协调作用。

## 31. 为什么要缩小 Mutex 的保护范围？

**锁只保护必须一起完成的共享数据操作，与这些数据无关的耗时工作可以在锁外并发执行。**

如果等待与 `count` 无关：

```kotlin
viewModelScope.launch {
    var count = 0
    val mutex = Mutex()

    coroutineScope {
        repeat(2) {
            launch {
                delay(100) // 独立的耗时工作，等待可以重叠。

                mutex.withLock {
                    count++
                }
            }
        }
    }

    println(count) // 2
}
```

执行过程：两个任务先分别等待 → A 拿锁更新 0 为 1 并立即释放 → B 拿锁更新 1 为 2 → 两个任务完成。

锁保护的代码块叫“临界区”。把无关等待放到外面，可以减少其他任务等锁的时间。例如，独立下载工作先执行，只在累计完成数量时拿锁。

移动代码的前提是锁外工作不依赖必须保持一致的共享状态。如果耗时处理必须基于先前读取的 `count`，不能直接移出去，需要重新分析整个读写过程。

## 参考资料

- [Kotlin：协程异常与监督](https://kotlinlang.org/docs/exception-handling.html)：异常传播、取消异常、处理器及监督的官方说明。
- [Android：ViewModel API](https://developer.android.com/reference/androidx/lifecycle/ViewModel)：`viewModelScope` 的默认配置与生命周期。
- [Kotlin 语言规范：异步与协程](https://kotlinlang.org/spec/asynchronous-programming-with-coroutines.html)：Continuation、CPS、状态机及上下文。
- [suspendCancellableCoroutine API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/suspend-cancellable-coroutine.html)：一次性回调转换与取消传播。
- [项目所用协程 1.10.2 的异常处理器源码](https://github.com/Kotlin/kotlinx.coroutines/blob/1.10.2/kotlinx-coroutines-core/common/src/CoroutineExceptionHandler.kt)：未处理异常边界和回调线程约定。
