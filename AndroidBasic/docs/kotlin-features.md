# Kotlin 特性：问题复习

## 1. by 和 by lazy 有什么区别？

**`by` 是属性委托语法，`lazy` 是创建委托对象的标准库函数；`by lazy` 是二者组合。**

```kotlin
val name: String by lazy {
    println("初始化")
    "Android"
}
```

第一次读取 `name`，执行 Lambda 并缓存结果；以后读取返回缓存。

| 部分 | 职责 |
| --- | --- |
| `by` | 将属性访问交给委托的 `getValue()`，对于 `var` 还包括 `setValue()` |
| `lazy { ... }` | 返回 `Lazy<T>`；首次取值时执行初始化并缓存 |
| `viewModels { ... }` | 返回 AndroidX 的 Lazy 委托；通过 ViewModelProvider 获取实例 |

`by` 本身不保证延迟初始化、缓存或线程安全。具体行为由委托实现决定。

关于 `Lazy<PostViewModel>` 如何提供 `PostViewModel`、实例何时创建，见 [ViewModel 委托与初始化](android-build-and-mvvm.md#5-viewmodels-返回-lazy为什么属性类型可以是-postviewmodel)。

参考：[Kotlin 属性委托](https://kotlinlang.org/docs/delegated-properties.html)。

## 2. by 还有哪些用法？

### 2.1 监听属性赋值：observable

```kotlin
import kotlin.properties.Delegates

var name: String by Delegates.observable("初始值") { property, old, new ->
    println("${property.name}：$old → $new")
}

name = "Android"
```

赋值生效后执行回调。即使新旧值相等，赋值也会触发回调。

### 2.2 决定是否接受赋值：vetoable

```kotlin
import kotlin.properties.Delegates

var age: Int by Delegates.vetoable(18) { _, _, new ->
    new >= 0
}

age = 20
age = -1
println(age) // 20
```

回调在修改生效前执行，返回 `true` 才接受新值。

### 2.3 委托给 Map

```kotlin
class UserInfo(data: Map<String, Any>) {
    val name: String by data
    val age: Int by data
}

val user = UserInfo(mapOf("name" to "张三", "age" to 20))
println(user.name) // 张三
```

按属性名读取对应 key。Map 中的 key 和值类型应与属性匹配；并不是自动进行字段校验或类型转换。

### 2.4 委托给另一个属性

```kotlin
class Profile {
    var name: String = ""
    var oldName: String by this::name
}
```

两个属性入口对应同一份值。可用于属性重命名时保留旧名称。

### 2.5 自定义属性委托

```kotlin
import kotlin.reflect.KProperty

class UppercaseDelegate(private val text: String) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return text.uppercase()
    }
}

val title: String by UppercaseDelegate("android")
println(title) // ANDROID
```

每次读取会进入 `getValue()`。缓存需要自己实现。若委托用于可写的 `var`，还需要符合约定的 `setValue()`。

### 2.6 接口委托

```kotlin
interface Printer {
    fun print()
}

class ConsolePrinter : Printer {
    override fun print() {
        println("打印内容")
    }
}

class ReportPrinter(printer: Printer) : Printer by printer
```

`ReportPrinter` 实现 `Printer`，未自行覆盖的接口成员由传入的 `printer` 对象提供。它也可以自行 override 某个成员。

属性声明中的 `by` 委托属性访问；类声明中的 `: 接口 by 对象` 委托接口实现。

参考：[接口委托](https://kotlinlang.org/docs/delegation.html)。

## 3. 为什么 factoryProducer = () -> Factory(...) 会报错？

**函数类型的声明与 Lambda 表达式是两种不同语法。**

```kotlin
// 左侧声明函数类型；右侧是具体 Lambda
val producer: () -> ViewModelProvider.Factory = {
    PostViewModel.Factory(MvvmDependencies.postRepository)
}
```

- `() -> ViewModelProvider.Factory`：无普通参数、返回 Factory 的函数类型。
- `{ PostViewModel.Factory(...) }`：一个无参 Lambda，最后一行产生返回值。
- `PostViewModel.Factory(...)`：直接调用构造函数，得到 Factory 对象。
- `producer()`：执行保存的函数，得到 Factory 对象。

正确传参：

```kotlin
private val viewModel: PostViewModel by viewModels(
    factoryProducer = {
        PostViewModel.Factory(MvvmDependencies.postRepository)
    }
)
```

下面这段不是合法的 Kotlin Lambda 表达式：

```kotlin
// 编译错误
factoryProducer = () -> PostViewModel.Factory(MvvmDependencies.postRepository)
```

有普通参数时，Lambda 写成 `{ 参数 -> 函数体 }`；无参数时通常直接写 `{ 函数体 }`。

## 4. 尾随 Lambda 只对应最后一个参数吗？

**是，对应函数声明中的最后一个参数，而且这个参数必须是函数类型。**

```kotlin
fun request(onSuccess: () -> Unit, onError: () -> Unit) {
    // ...
}

request(onSuccess = { println("成功") }) {
    println("失败") // 对应 onError
}
```

不会向前寻找“最后一个函数类型参数”。如果声明中的最后一个参数是 Int，即使它有默认值，也不能把前面的 Lambda 移到括号外：

```kotlin
fun execute(action: () -> Unit, count: Int = 1) {
    repeat(count) { action() }
}

execute(action = { println("执行") }) // 正确
// execute { println("执行") }      // 编译错误
```

`viewModels` 的 `factoryProducer` 正好位于最后，因此项目可以使用尾随 Lambda：

```kotlin
private val viewModel: PostViewModel by viewModels {
    PostViewModel.Factory(MvvmDependencies.postRepository)
}
```

## 5. let、run、also、apply、with 如何区分？

共同点是提供一个临时作用域。主要看“如何访问对象”和“返回什么”。

| 函数 | 调用形式 | 对象在块内的角色 | 返回值 | 常见用途 |
| --- | --- | --- | --- | --- |
| let | `obj.let { ... }` | 参数 `it`，或自定义名称 | 块的结果 | 转换、配合安全调用处理非空对象 |
| run | `obj.run { ... }` | 接收者 `this` | 块的结果 | 围绕对象计算结果 |
| also | `obj.also { ... }` | 参数 `it`，或自定义名称 | 原对象 | 日志、调试、附加操作 |
| apply | `obj.apply { ... }` | 接收者 `this` | 原对象 | 初始化配置 |
| with | `with(obj) { ... }` | 接收者 `this` | 块的结果 | 集中操作已有对象 |

此处的 run 指带接收者的扩展形式。表格中的用途是常见习惯，不是编译器限制。

### let 与 run：返回结果

```kotlin
val first: Int = "Android".let { text ->
    text.length
}

val second: Int = "Android".run {
    length // this.length
}
```

`it` 是省略单个普通参数名时的默认名称；`this` 是接收者，不是同一个概念。

### apply：配置并返回原对象

```kotlin
val builder: StringBuilder = StringBuilder().apply {
    append("Android")
    append(" MVVM")
}
```

即使最后一行得到其他结果，apply 仍返回原对象。

### also：附加操作后仍返回原对象

```kotlin
val name: String = "Android".also {
    println("名称：$it")
    it.length // 这个结果不会成为 also 的返回值
}
```

在项目已有的请求协程中，可以通过 also 记录文章数量，然后继续使用同一份结果列表。

### with：对象作为普通参数传入

```kotlin
val message: String = with(builder) {
    append(" 示例")
    toString()
}
```

项目的 `render()` 使用 `with(binding)`，使代码块中可以省略 `binding.`。

这些函数本身不会启动协程或切换线程。let 本身也不会跳过 null；`obj?.let { ... }` 中的空值短路来自 `?.`。

参考：[作用域函数](https://kotlinlang.org/docs/scope-functions.html)。

## 6. 什么是高阶函数？传递函数与调用函数有何区别？

**接收函数作为参数，或返回函数的函数，称为高阶函数。**

```kotlin
fun calculate(a: Int, b: Int, operation: (Int, Int) -> Int): Int {
    return operation(a, b)
}

val sum = calculate(3, 5) { x, y -> x + y }     // 8
val product = calculate(3, 5) { x, y -> x * y } // 15
```

定义、传递、调用是不同动作：

```kotlin
val operation: (Int, Int) -> Int = { x, y -> x + y }

calculate(3, 5, operation) // 传递函数，由 calculate 调用
operation(3, 5)           // 直接调用，得到 8
```

在 `viewModels(factoryProducer = { ... })` 中传入的也是函数，内部执行 `factoryProducer()` 时才产生 Factory。

## 7. 什么是闭包？函数返回后为何还能访问局部变量？

**Lambda 可以捕获外部作用域的变量，并保留访问这些变量的能力。**

```kotlin
fun createCounter(): () -> Int {
    var count = 0
    return {
        count++
        count
    }
}

val first = createCounter()
println(first()) // 1
println(first()) // 2

val second = createCounter()
println(second()) // 1
```

- 返回的 Lambda 仍能访问那一次函数调用中的 `count`。
- 捕获可变局部变量时，不是只保存其初始值的快照；后续调用能看到更新后的状态。
- 每次执行 `createCounter()` 创建独立的局部变量，因此两个计数器互不影响。

项目按钮监听中的 Lambda 通过捕获外部 Activity，能够在点击时访问 `viewModel`。

## 8. T.() -> R 是什么？它和 (T) -> R 有何区别？

**这是带接收者的函数类型。接收者通过 this 访问，普通参数通过参数名或 it 访问。**

| 类型 | 对象的角色 | 块内访问 |
| --- | --- | --- |
| `(T) -> R` | 一个普通参数 | `it` 或自定义参数名 |
| `T.() -> R` | 接收者，没有普通参数 | `this`，可省略 |
| `A.(B) -> C` | A 是接收者，B 是普通参数 | A 用 this，B 用参数名；返回 C |

接收者与普通参数可以同时存在：

```kotlin
val appendAndCount: StringBuilder.(String) -> Int = { text ->
    append(text) // this.append(text)
    length       // this.length，作为结果
}

val builder = StringBuilder()
val count = builder.appendAndCount("Android") // 7

// 另一种合法调用形式，同样提供接收者和普通参数
val nextCount = appendAndCount(builder, " MVVM")
```

声明 Lambda 不会自动创建接收者对象，调用者需要提供实际对象。

自己定义一个使用接收者 Lambda 的函数：

```kotlin
fun buildText(block: StringBuilder.() -> Unit): String {
    val builder = StringBuilder()
    builder.block()
    return builder.toString()
}

val text = buildText {
    append("Android")
    append(" MVVM")
}
```

`buildText` 创建并提供 StringBuilder；代码块里的 `this` 指向该对象。这里没有普通参数，所以不会自动提供 `it`。

参考：[带接收者的函数类型与调用](https://kotlinlang.org/docs/lambdas.html#function-literals-with-receiver)。

## 9. Compose 中会用到带接收者的 Lambda 吗？

**会。Row、Column、Box 通过不同的 Scope 提供布局相关的能力。**

以 Row 为例，省略其他参数和实现后的声明示意：

```kotlin
@Composable
fun Row(content: @Composable RowScope.() -> Unit) {
    // 框架提供 RowScope 接收者并调用 content
}
```

参数类型可以拆成：

- `@Composable`：这是可组合的函数类型，可以在符合 Compose 规则的上下文中调用可组合函数。
- `RowScope`：接收者类型，为代码块提供 Row 的作用域能力。
- `()`：无普通参数。
- `Unit`：不是通过返回一个 View 对象来交付 UI。

```kotlin
Row {
    Text("文章标题", modifier = Modifier.weight(1f))
    Text("更多")
}
```

这结合了尾随 Lambda 与接收者 Lambda。`weight` 是 RowScope 提供的 Modifier 成员扩展；块内的 `this` 是 RowScope，并非一个 Row View 对象。Text 是可组合函数，也不是 RowScope 的成员方法。

LazyColumn 的外层配置块则使用 `LazyListScope.() -> Unit`，在其中通过 item / items 描述列表项：

```kotlin
LazyColumn {
    item { Text("标题") }
    items(3) { index -> Text("第 $index 项") }
}
```

LazyColumn 的外层块负责登记列表项；真正绘制每个条目的可组合内容放在内部的 item 内容块中，由惰性列表按需处理。

参考：[Row 源码](https://github.com/androidx/androidx/blob/androidx-main/compose/foundation/foundation-layout/src/commonMain/kotlin/androidx/compose/foundation/layout/Row.kt)、[布局作用域](https://developer.android.com/develop/ui/compose/modifiers#scope-safety)、[LazyListScope](https://developer.android.com/develop/ui/compose/lists#lazylist-scope-dsl)。

## 10. inline 是什么？会改变执行时机吗？

**inline 是编译期的内联机制，可将函数体与可内联的 Lambda 展开到调用位置。**

```kotlin
inline fun execute(action: () -> Unit) {
    println("开始")
    action()
    println("结束")
}

execute { println("执行任务") }
```

语义上可以近似理解为：

```kotlin
println("开始")
println("执行任务")
println("结束")
```

它能减少部分调用与 Lambda 对象开销，但不保证消除所有对象或让所有场景都更快。运行时仍遵守代码原有的执行语义。

`viewModels` 是 inline 函数，也不会因此提前创建 ViewModel；延迟取值由返回的 Lazy 委托控制。

## 11. noinline 有什么作用？为什么 viewModels 需要它？

**让某个函数参数不参与内联，保留为可以保存、传递的普通函数值。**

```kotlin
var savedAction: (() -> Unit)? = null

inline fun prepare(before: () -> Unit, noinline later: () -> Unit) {
    before()
    savedAction = later
}

prepare(
    before = { println("立即执行") },
    later = { println("稍后执行") }
)

savedAction?.invoke()
```

不加 noinline，就不能把可内联参数直接保存到 `savedAction`。

项目依赖的 viewModels 声明为：

```kotlin
public inline fun <reified VM : ViewModel> ComponentActivity.viewModels(
    noinline extrasProducer: (() -> CreationExtras)? = null,
    noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM>
```

这里展示函数签名，省略了实现。Producer 函数需要由 ViewModelLazy 持有，供取值时调用，因此标记为 noinline。可空的函数类型参数本身也不能按普通非空函数参数那样内联使用。

noinline 不表示自动延迟执行，调用时机由使用它的代码决定。

## 12. crossinline 和 noinline 有何区别？

**crossinline 仍允许内联，但禁止这个 Lambda 非局部返回。**

当参数需要被另一层 Lambda 或对象方法间接调用时，常使用它：

```kotlin
inline fun createTask(crossinline action: () -> Unit): Runnable {
    return Runnable { action() }
}

val task = createTask { println("执行任务") }
task.run() // 当前线程中执行
```

action 被 Runnable 的 run 间接调用，不能允许它用裸 return 退出原调用者。去掉 crossinline，会因为当前调用结构无法支持非局部返回而编译失败。

| 参数 | 内联 | 非局部返回 | 典型用途 |
| --- | --- | --- | --- |
| 普通可内联参数 | 是 | 在允许的内联调用结构中支持 | 直接在内联函数中调用 |
| noinline 参数 | 否 | 不支持 | 将函数值直接保存、传递 |
| crossinline 参数 | 是 | 不支持 | 在包装的 Lambda / 对象方法中调用 |

crossinline 不会自动启动线程或改变调度方式。这个例子何时执行，由调用 task.run() 的位置决定。

参考：[内联、noinline 与 crossinline](https://kotlinlang.org/docs/inline-functions.html)。

## 13. reified 有什么作用？为何与 inline 一起出现？

**让内联函数能够利用调用处的具体泛型类型，进行类型判断或获取类信息。**

普通泛型函数不能直接写 `value is T`。使用 reified 后可以：

```kotlin
inline fun <reified T> isType(value: Any): Boolean {
    return value is T
}

println(isType<String>("Android")) // true
println(isType<String>(123))       // false
```

也可以使用 `T::class`：

```kotlin
inline fun <reified T> printType() {
    println(T::class)
}
```

函数的 reified 类型参数只能声明在 inline 函数上。内联使编译器能在调用处利用具体类型生成相应代码。

在 `viewModels<PostViewModel>()` 中，内部使用的 `VM::class` 对应 `PostViewModel::class`，框架因此知道要获取哪个类型的 ViewModel。省略显式泛型时，也可从属性类型推断 VM。

reified 不会恢复所有嵌套泛型实参信息。例如 `isType<List<String>>(value)` 不会逐项验证列表中的元素都是 String。

## 14. ::render 是立即调用函数吗？

**不是，它获取已有函数的引用，可以把函数作为值保存或传递。**

```kotlin
fun double(number: Int): Int = number * 2

val operation: (Int) -> Int = ::double
println(operation(3)) // 6
```

| 写法 | 结果 |
| --- | --- |
| `double(3)` | 立即执行，得到 6 |
| `::double` | 获取函数引用 |

函数引用可以替代单纯转发参数的 Lambda：

```kotlin
val first = listOf(1, 2, 3).map { double(it) }
val second = listOf(1, 2, 3).map(::double)
```

项目中的：

```kotlin
viewModel.uiState.observe(this, ::render)
```

效果可理解为：

```kotlin
viewModel.uiState.observe(this) { state -> render(state) }
```

当前 Activity 内的 `::render` 可明确写为 `this::render`，引用绑定在当前实例上。函数引用的参数与返回类型，需要与使用位置要求的函数类型兼容。

参考：[函数引用](https://kotlinlang.org/docs/lambdas.html#callable-reference)。
