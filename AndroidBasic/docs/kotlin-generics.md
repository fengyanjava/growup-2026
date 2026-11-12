# Kotlin 泛型：问题复习

## 1. out 有什么作用？

**声明协变，允许把“提供子类型的对象”当作“提供父类型的对象”使用。**

```kotlin
interface Producer<out T> {
    fun produce(): T
}

val stringProducer: Producer<String> = object : Producer<String> {
    override fun produce(): String = "Android"
}

val anyProducer: Producer<Any> = stringProducer
val value: Any = anyProducer.produce()
```

String 是 Any 的子类型；声明 out 后，Producer<String> 也是 Producer<Any> 的子类型。

安全的原因是：调用者要一个 Any，实际返回 String，符合要求。

out 常用于对外提供 T 的 API，例如返回值和只读属性。不能直接把它放在普通公开方法的输入位置：

```kotlin
interface Producer<out T> {
    fun produce(): T      // 正确
    val value: T          // 正确

    // 以下用于说明编译边界：
    // fun consume(value: T) // 编译错误
    // var current: T       // 编译错误，setter 要接收 T
}
```

“生产者用 out”是记忆方式。out 不会自动把对象变成不可变对象，也不表示泛型参数可以在所有类型位置随意使用。

## 2. 为什么 List<String> 可以赋给 List<Any>，MutableList 不可以？

Kotlin 的只读 List 在声明处使用 out：

```kotlin
val strings: List<String> = listOf("Android", "Kotlin")
val values: List<Any> = strings // 正确
```

这里没有复制或转换列表，只是通过更宽泛的类型使用它。

MutableList 需要接收新元素，因此不能统一声明为协变：

```kotlin
val strings: MutableList<String> = mutableListOf("Android")
// val values: MutableList<Any> = strings // 编译错误
```

否则调用者就能执行 `values.add(123)`，破坏字符串列表的类型约定。

“只读 List”描述当前 API 不提供修改入口，不保证底层对象绝对不可变，也不保证其他持有者无法修改它。

## 3. in 有什么作用？

**声明逆变，允许把“能够接收父类型的对象”当作“接收子类型的对象”使用。**

```kotlin
interface Consumer<in T> {
    fun consume(value: T)
}

val anyConsumer: Consumer<Any> = object : Consumer<Any> {
    override fun consume(value: Any) {
        println(value)
    }
}

val stringConsumer: Consumer<String> = anyConsumer
stringConsumer.consume("Android")
```

能够处理所有 Any 的对象，当然也能处理 String。因此 Consumer<Any> 可以作为 Consumer<String> 使用。

反过来不安全：只能处理 String 的对象，不能承担“处理所有 Any”的职责。

in 允许 T 用作普通输入参数，禁止直接把它用作公开的返回类型：

```kotlin
interface Consumer<in T> {
    fun consume(value: T) // 正确
    // fun produce(): T  // 编译错误
}
```

## 4. out 与 in 的赋值方向如何记？

已知 String 是 Any 的子类型：

| 声明 | 角色 | 安全替换方向 |
| --- | --- | --- |
| Producer<out T> | 生产 T | Producer<String> → Producer<Any> |
| Consumer<in T> | 消费 T | Consumer<Any> → Consumer<String> |
| 未声明型变的 Box<T> | 可能同时读写 T | 通常不能直接进行以上替换 |

记忆：生产者 out，消费者 in。判断安全性时，想一想调用方能传什么、实际对象能接受什么，以及调用方会拿到什么。

这里讨论普通的公开输入、输出位置；更复杂的嵌套函数类型，需要按完整类型位置继续分析。

## 5. 声明处的 out 与使用处的 out 有何区别？

声明处确定整个类型的型变规则：

```kotlin
interface Producer<out T>
```

有些类型同时读写 T，不能整体声明协变。例如：

```kotlin
class Box<T>(var value: T)
```

如果某个使用位置只需要读取，可以对当前引用进行 out 投影：

```kotlin
fun printBox(box: Box<out Any>) {
    val value: Any = box.value
    println(value)

    // box.value = 123 // 编译错误
}

val textBox: Box<String> = Box("Android")
val numberBox: Box<Int> = Box(123)

printBox(textBox)
printBox(numberBox)
```

`Box<out Any>` 表示实际的内容类型可能是某种 Any 的子类型。读取为 Any 安全；写入一个任意 Any 则不安全。

限制只针对投影后的引用，原有引用仍可以操作：

```kotlin
textBox.value = "Kotlin" // 正确，写入 String
```

没有复制 Box，也没有冻结对象。

## 6. 使用处的 in 表示什么？能不能读取？

**保证可以写入指定类型，但读取时只能获得其上界层面的类型信息。**

```kotlin
fun updateText(box: Box<in String>) {
    val oldValue: Any? = box.value
    println("旧值：$oldValue")

    box.value = "Android"
}

val textBox: Box<String> = Box("旧文本")
val anyBox: Box<Any> = Box(123)

updateText(textBox)
updateText(anyBox)
```

两种 Box 都可以接收 String。但第二个 Box 原先可能装数字，因此不能把读取结果直接视为 String：

```kotlin
fun readText(box: Box<in String>) {
    // val text: String = box.value // 编译错误
    val unknown: Any? = box.value  // 正确
}
```

当前 `Box<T>` 的默认上界是 Any?，所以读取结果是 Any?。

| 对当前 Box 的投影 | 读取 value | 写入 value |
| --- | --- | --- |
| Box<out Any> | Any | 禁止 |
| Box<in String> | Any? | 可以写 String |

in 投影不是“完全不能读取”，而是无法保证读到的值恰好是 String。

## 7. Box<*> 表示什么？为何不是 Box<Any>？

**星投影表示具体类型参数未知，但操作仍然受类型系统保护。**

```kotlin
fun inspect(box: Box<*>) {
    val value: Any? = box.value
    println(value)

    // box.value = "Android" // 编译错误
    // box.value = 123       // 编译错误
    // box.value = null      // 编译错误
}

inspect(Box("Android"))
inspect(Box(123))
inspect(Box(null))
```

即使 null 也不能写入，因为实际 Box 可能要求非空的 String。

| 类型 | 含义 | 当前例子中的读取 | 写入 |
| --- | --- | --- | --- |
| Box<Any> | 明确能存放任意非空对象 | Any | 任意非空对象 |
| Box<*> | 不知道具体的内容类型 | Any? | 禁止 |

```kotlin
val textBox: Box<String> = Box("Android")
val unknown: Box<*> = textBox // 正确
// val anyBox: Box<Any> = textBox // 编译错误
```

这里 Box<*> 的读写能力可按 Box<out Any?> 理解。对于有其他上界或已声明型变的泛型，星投影的具体含义需结合声明分析，不能一概认定读取结果永远是 Any?。

## 8. T : 某个类型，冒号约束了什么？

**为泛型参数声明上界：实参类型必须是该上界或其子类型。**

项目中：

```kotlin
<VM : ViewModel>
```

表示 VM 必须是 ViewModel 或其子类型，所以可以用 PostViewModel，不能用 String。

上界也让函数内部能够调用相应成员：

```kotlin
fun <T : CharSequence> printLength(value: T) {
    println(value.length)
}

printLength("Android")
printLength(StringBuilder("Hi"))
// printLength(123) // 编译错误
```

## 9. T : Any 与没有写上界有什么区别？

未声明上界时，默认上界是 Any?，允许可空类型。

```kotlin
class Box<T>(var value: T)

Box("Android")
Box(null)
```

`T : Any` 将泛型参数限制为非空类型：

```kotlin
fun <T : Any> printValue(value: T) {
    println(value)
}

printValue("Android")
printValue(123)
// printValue(null) // 编译错误
```

## 10. 上界如何影响星投影的读取类型？

```kotlin
class TextBox<T : CharSequence>(val value: T)

fun inspect(box: TextBox<*>) {
    println(box.value.length)
}
```

虽然不知道具体 T，但知道它满足 CharSequence，因此读取结果可以作为非空的 CharSequence 使用。

| 类型声明 | 星投影读取属性时可确定的类型 |
| --- | --- |
| Box<T>，默认上界 Any? | Any? |
| TextBox<T : CharSequence> | CharSequence |

## 11. 一个类型参数需要多个上界时怎么办？

使用 where，要求多个约束同时满足：

```kotlin
interface Named {
    val name: String
}

interface Aged {
    val age: Int
}

fun <T> printInfo(value: T)
    where T : Named, T : Aged {
    println("${value.name}，${value.age} 岁")
}
```

符合约束的类型可以调用：

```kotlin
class Person(
    override val name: String,
    override val age: Int
) : Named, Aged

printInfo(Person("张三", 20))
```

仅实现其中一个接口不满足条件。where 写在参数列表及返回类型之后、函数体之前：

```text
fun <T> 函数名(参数): 返回类型
    where T : 约束一, T : 约束二 {
    函数体
}
```

## 参考

- [Kotlin 泛型：型变、类型投影与星投影](https://kotlinlang.org/docs/generics.html)。
- [泛型上界](https://kotlinlang.org/docs/generics.html#upper-bounds)。
- reified 与运行时类型信息见 [Kotlin 特性笔记](kotlin-features.md#13-reified-有什么作用为何与-inline-一起出现)。
