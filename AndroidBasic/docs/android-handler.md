# Android Handler：问题复习

整理日期：2026-11-13。根据本轮 Handler 原理、执行顺序和并发时序面试题整理。示例省略 import；消息队列和 Native 唤醒细节主要参照 Android 14 AOSP，属于实现分析，不代表所有版本的内部代码完全相同。

## 1. Handler 的消息如何完成一次分发？

**Handler 投递消息，MessageQueue 保存消息，Looper 循环取消息，再交给消息指定的 Handler。**

```kotlin
val handler = Handler(Looper.getMainLooper())
handler.post {
    textView.text = "完成"
}
```

内部调用链可简化为：

```text
post(Runnable)
    → 把 Runnable 包装进 Message.callback
    → 入队前设置 Message.target = 当前 Handler
    → 放入 Handler 绑定的 Looper 的 MessageQueue
    → Looper 循环取出可以执行的 Message
    → msg.target.dispatchMessage(msg)
    → Handler 执行 msg.callback.run()
```

| 字段 | 作用 |
| --- | --- |
| `Message.target` | 指定由哪个 Handler 分发 |
| `Message.callback` | `post` 携带的 Runnable 任务 |

`post` 也走消息机制。多个 Handler 可以绑定同一个 Looper，共用它的消息队列，由每条消息的 `target` 区分接收者。

Handler 不创建线程。执行发生在运行对应 `Looper.loop()` 的线程上，与调用 `post()` 的线程无关。

参考：[Handler 源码](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/os/Handler.java)、[Looper 源码](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/os/Looper.java)。

## 2. Looper.loop 为什么不会阻塞主线程？

**问题的前提不够准确：它会持续占据主线程的执行流程，没有可执行工作时也确实会阻塞等待；但这不等于页面卡死。**

主线程本来就是依靠事件循环处理生命周期、界面更新、输入等工作。

```kotlin
// 概念示意，省略回收、异常及监控等逻辑
while (true) {
    val msg = queue.next() ?: return
    msg.target.dispatchMessage(msg)
}
```

- 有可执行消息：取出、执行，再处理下一条。
- 无可执行工作：通过底层等待机制休眠，避免空转；需要处理新工作时唤醒。
- 某个任务执行太久：迟迟不能返回循环，后续 UI 工作无法及时处理，才会造成卡顿，并可能触发 ANR。

```kotlin
Handler(Looper.getMainLooper()).post {
    Thread.sleep(10_000) // 主线程被当前任务占住，后续工作只能等
}
```

“等待工作”与“有工作却被耗时任务阻挡”需要分开理解；等待本身不会自动构成 ANR。

## 3. 为什么 View.post 通常可以获取宽高？

**常见首次布局场景中，Runnable 真正执行时，当前遍历已经完成；但 post 不承诺等待布局完成。**

例如在 Activity 的 `onCreate()` 中：

```kotlin
setContentView(R.layout.activity_main)
view.post {
    Log.d("Size", "${view.width}, ${view.height}")
}
```

当 View 尚未 attach 时，典型顺序是：

```text
post：任务暂存到 View 自己的队列
    → 首次视图遍历中 attach
    → 将暂存任务投递给主线程 Handler
    → 当前遍历继续 measure / layout 等工作
    → 当前调用结束，Looper 才能执行排队的 Runnable
    → 此时可读取 layout 确定的宽高
```

关键是任务在 **attach 时入队**，不是专门等 layout 完成才入队；同一个线程不能同时执行当前遍历与排队任务。

View 已 attach 时，`post()` 直接交给其 Handler。因此不能保证任意调用时机都能得到预期尺寸；需要精确等待某次布局，应使用布局回调。布局完成也不保证尺寸一定非零，例如控件本身可能没有布局出可见尺寸。

参考：[View.post 与 attach 时转移任务](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r1/core/java/android/view/View.java)、[ViewRootImpl 首次遍历](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r1/core/java/android/view/ViewRootImpl.java)。

## 4. 同步消息与异步消息有什么区别？

**区别在于是否受到同步屏障阻挡，不在于是否另开线程。**

| 消息类型 | 遇到同步屏障时 |
| --- | --- |
| 同步消息 | 队列遇到屏障后，后面的同步消息暂缓取出 |
| 异步消息 | 可以绕过屏障，被队列找到并取出执行 |

普通 Handler 默认投递同步消息。没有屏障时，通常仍按可执行时间及入队顺序调度，异步消息不会只凭“异步”身份自动插队。

假设以下消息都已到执行时间：

```text
同步消息 A → 同步屏障 → 同步消息 B → 异步消息 C

执行 A → 遇到屏障 → 跳过 B，执行 C
屏障移除后 → B 才能被取出执行
```

- “同步消息”不表示发送线程同步等待结果，`sendMessage()` 仍只是投递。
- 异步消息仍在所属 Looper 的线程执行。
- 异步消息不能抢占已经执行中的任务。

## 5. 同步屏障有哪些细节？

**它是 `target == null` 的特殊队列标记，不是要交给某个 Handler 执行的普通消息。**

- 在队列取消息遇到屏障时，才会阻挡后面的同步消息。
- 不影响屏障前面的消息，不中断正在执行的代码。
- 可以继续查找屏障后的异步消息。
- 需要主动移除；如果一直不移除，后面的同步消息会持续受阻。

参考：[MessageQueue 的同步屏障与 next 实现](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r1/core/java/android/os/MessageQueue.java)。

## 6. 同步屏障和异步消息用在什么场景？

**典型场景是系统调度下一帧的界面更新。**

```text
ViewRootImpl 安排视图遍历
    → 插入同步屏障
    → 队列遇到屏障，暂缓后面的普通同步消息
VSYNC 到来
    → Choreographer 投递异步帧消息，绕过屏障
    → 执行该帧回调
    → 进入视图遍历时移除屏障
    → 执行 measure / layout / draw 等工作
```

这是对队列执行顺序的协调，不能解决主线程已经被耗时任务占住的问题。移除屏障后，当前遍历仍在主线程继续执行，排队消息不会因此并发执行。

- 同步屏障通常由系统框架维护，相关插入、移除接口属于隐藏 API。
- 应用或调度库明确需要让回调不受屏障影响时，可以使用异步 Handler。

```kotlin
// API 28+
val handler = Handler.createAsync(Looper.getMainLooper())
handler.post {
    // 主线程执行，可绕过同步屏障
}
```

异步消息可能改变与其他 Handler 消息之间的执行顺序，不宜当成普通业务任务的通用“加速开关”。

参考：[Choreographer 的异步帧消息](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r1/core/java/android/view/Choreographer.java)、[ViewRootImpl 的 scheduleTraversals / doTraversal](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r1/core/java/android/view/ViewRootImpl.java)、[Handler.createAsync](https://developer.android.com/reference/android/os/Handler#createAsync(android.os.Looper))。

## 7. postDelayed(runnable, 1000) 能保证一秒后执行吗？

**不能，它指定最早可执行时间，而不是精确执行时间。**

```text
计算 when = SystemClock.uptimeMillis() + 1000
    → 立即把消息放进队列
    → 未到期时等待
    → 到期后具备执行条件
    → 等线程可用、消息可被取出时执行
```

不是“一秒后才入队”，也不会让调用线程为了延迟而睡一秒。

- 当前任务耗时、其他已到期任务、同步屏障等，都可能延后执行。
- `uptimeMillis()` 不包含设备深度睡眠时间，因此深度睡眠会增加实际经过的等待时间。
- 成功入队不保证最终执行，例如之后队列退出或任务被移除。

参考：[Handler.postDelayed](https://developer.android.com/reference/android/os/Handler#postDelayed(java.lang.Runnable,%20long))。

## 8. 子线程直接创建 Handler 为什么可能崩溃？

**无参 Handler 使用当前线程的 Looper，而普通子线程默认没有 Looper。**

`Looper.myLooper()` 通过 `ThreadLocal` 获取当前线程的 Looper。没有初始化时返回 `null`，无参 Handler 无法完成绑定。

如果需要由该子线程处理消息，顺序是：

```text
Looper.prepare()：初始化当前线程的 Looper 和队列
创建 Handler：绑定这个 Looper
Looper.loop()：当前线程开始循环处理消息
```

`prepare()` 只初始化，`loop()` 才开始处理消息。实际使用时明确传入 Looper，可以避免隐式选择线程；无参构造已被弃用。

在子线程中写下面这句，则不要求该子线程拥有 Looper：

```kotlin
val handler = Handler(Looper.getMainLooper())
```

它绑定主线程 Looper，消息由主线程处理。**创建 Handler 的线程与处理消息的线程可以不同。**

参考：[Handler 构造说明](https://developer.android.com/reference/android/os/Handler)、[Looper.prepare / myLooper](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/os/Looper.java)。

## 9. 准备休眠与发送消息恰好交错，会不会丢失唤醒？

**不会因为“先唤醒、后等待”这个时序漏掉通知。** 以 Android 14 的 Java MessageQueue 与 Native Looper 实现为例，有两层配合。

第一层：队列检查、等待标记与入队操作通过同一把队列锁协调。

```text
主线程在队列锁内检查消息
    → 确定需要等待，设置 mBlocked = true
    → 释放队列锁
    → 到 nativePollOnce 中等待

发送线程在同一把锁内入队
    → 根据队列位置、mBlocked 等判断是否需要 nativeWake
```

`mBlocked` 表示准备等待，不保证线程此刻已经睡着。`nativePollOnce()` 在 Java 队列锁外调用，因此睡眠不会一直占着队列锁，其他线程仍能入队。

第二层：Native 唤醒使用 eventfd，信号有可保留的状态。

```text
已进入 epoll_wait → 写入 eventfd，使等待返回
尚未进入等待    → 写入后保留可读状态，随后等待也会返回
```

不是只有对方正在等待的瞬间，唤醒才有效。锁负责协调队列和等待状态，eventfd 负责让这个时间窗口中的通知不会丢失。

这里不是说“每次入队都必须唤醒”：队列会判断新消息是否改变下一次需要处理工作的时间等条件，避免无意义唤醒。

参考：[MessageQueue.next / enqueueMessage](https://github.com/aosp-mirror/platform_frameworks_base/blob/android-14.0.0_r1/core/java/android/os/MessageQueue.java)、[Native Looper 的 wake / epoll_wait](https://github.com/aosp-mirror/platform_system_core/blob/android-14.0.0_r1/libutils/Looper.cpp)。
