# Android 事件分发：问题复习

整理日期：2026-11-13。根据本轮事件分发原理及自定义 Switch 的追问整理。以普通单指触摸和默认 View / ViewGroup 分发逻辑为主，省略窗口内部传递细节与多指分发。

## 1. 一次点击如何传到 Button？

**一次手势由多个触摸事件组成；点击回调是 View 处理这些事件后判断出来的。**

```text
ACTION_DOWN → 可选的多次 ACTION_MOVE → ACTION_UP
```

简化后的传递层级：

```text
Activity → 外层 ViewGroup → Button
```

| 方法 | 主要职责 |
| --- | --- |
| `dispatchTouchEvent()` | 组织分发，决定接下来交给谁 |
| `ViewGroup.onInterceptTouchEvent()` | 父容器判断是否拦截对子 View 的分发 |
| `onTouchEvent()` | 当前 View 自己处理触摸事件 |

`onClick` 不是原始触摸事件。Button 收到按下、移动、抬起等事件后，满足点击条件才触发它。

## 2. onInterceptTouchEvent 拦截与否有什么区别？

以 DOWN 为例：

```text
父容器 dispatchTouchEvent()
    → onInterceptTouchEvent()
        ├─ false：尝试交给命中的子 View
        └─ true：不交给子 View，由父容器自己处理
```

**拦截和消费是两个决定：**

- `onInterceptTouchEvent()` 返回 `true`：拦住对子 View 的分发。
- `onTouchEvent()` 返回 `true`：当前 View 消费事件。

父容器即使拦截了 DOWN，自己的触摸处理仍可能返回 `false`；拦截本身不保证消费。

## 3. TouchTarget 有什么作用？

**记录哪个直接子 View 接下了手势，让父容器知道后续事件继续交给谁。**

```text
父容器把 DOWN 交给 Button
    → Button.dispatchTouchEvent(DOWN) 返回 true
    → 创建 TouchTarget，child 指向 Button
    → 通过 mFirstTouchTarget 记录
    → 后续 MOVE / UP 按记录继续分发
```

后续事件通常不再重新根据手指位置寻找子 View。因此，手指移出 Button 范围后，Button 仍可能收到 MOVE / UP；是否构成点击则由 Button 的处理逻辑决定。

- Button 不消费 DOWN：不会为它建立这次手势的目标记录。
- 父容器在 DOWN 就拦截：Button 没收到 DOWN，也不会成为目标。
- 父容器中途拦截：给原目标发送 CANCEL，再移除对应记录。

每层 ViewGroup 各自记录自己的直接子 View，外层不会直接跳过中间布局记录最深层 Button。`mFirstTouchTarget` 实际是目标链表入口；这里先按单指、单目标理解。

## 4. ACTION_CANCEL 什么时候发生？

**当当前 View 对这次手势的处理被中断时，需要停止处理并清理状态。** 常见原因是父容器中途拦截；系统取消当前触摸序列时也可能发送 CANCEL。

例如 Button 放在可滚动容器里：

```text
DOWN：父容器不拦截，Button 显示按压效果
MOVE：父容器判断需要滚动，开始拦截
    → Button 收到 CANCEL，取消按压 / 拖动状态
    → 父容器移除 Button 的 TouchTarget
后续 MOVE / UP：交给父容器处理
```

| 事件 | 含义 |
| --- | --- |
| UP | 手指正常抬起，可能满足点击条件 |
| CANCEL | 本次处理被取消，清理状态，不提交这次点击 |

自定义 View 应在 CANCEL 中清理按压、拖动等临时状态，不能只处理 UP。

## 5. 触发拦截的那条 MOVE，会立即交给父容器的 onTouchEvent 吗？

**在已有子 View 目标的默认分发流程里，不会。**

```text
DOWN → Button 接下事件

MOVE 1 → 父容器 onInterceptTouchEvent() 返回 true
       → 把这条事件以 CANCEL 的形式发给 Button
       → 移除对应 TouchTarget
       → 这条 MOVE 不再交给父容器自己的 onTouchEvent()

MOVE 2 → 父容器 onTouchEvent() 开始处理
```

所以自定义滑动容器通常需要在拦截判断阶段记录按下坐标、移动位置。等父容器自己的 `onTouchEvent()` 开始处理时，手势已经进行了一段距离。

## 6. requestDisallowInterceptTouchEvent 的目的是什么？

**子 View 请求父容器及其祖先，暂时不要拦截当前手势。**

```kotlin
parent.requestDisallowInterceptTouchEvent(true)  // 禁止拦截
parent.requestDisallowInterceptTouchEvent(false) // 重新允许判断是否拦截
```

例如拖动 Switch 时，请求禁止拦截，可以避免外层滚动容器抢走手势、给 Switch 发 CANCEL。

父容器会记录禁止拦截标记，在普通触摸分发中跳过自己的 `onInterceptTouchEvent()`。**父容器的 `dispatchTouchEvent()` 仍然执行。**

- `false` 只是重新允许拦截，不表示父容器立即拦截。
- 需要在父容器拦截之前请求；已收到 CANCEL 后，不能靠它把原手势重新抢回来。
- 请求向祖先传递；手势结束后，默认分发流程会清理相关状态。

## 7. 自定义 View 如何处理触摸冲突？

**外部拦截法：父容器决定。**

```text
DOWN：通常不拦截，让子 View 先接收
MOVE：结合方向、距离判断
      父容器需要处理 → 拦截
      子 View 需要处理 → 不拦截
```

例如竖向列表中的横向 Switch：父容器判断为竖向滑动时接管，横向拖动留给 Switch。结合 `touchSlop` 判断，避免轻微抖动就切换处理者。

**内部拦截法：子 View 协调。**

```text
DOWN：子 View 消费，先请求禁止父容器拦截
MOVE：自己需要处理 → 保持禁止
      希望父容器处理 → 重新允许拦截
后续事件：父容器再判断是否接管；接管时给子 View 发 CANCEL
```

这种策略要求父容器先让 DOWN 到达子 View，并遵守禁止拦截请求。具体方向策略按父子控件的交互需要设计，不是所有 Switch 都必须在 DOWN 时一律禁止拦截。

三个易错点：

- 子 View 想接收后续事件，首先要接下 DOWN；通常通过 `onTouchEvent()` 返回 `true` 实现。
- 子 View 对后续 MOVE 返回 `false`，不等于自动把整次手势交给父容器。
- 父容器中途接管时，子 View 通过 CANCEL 收尾。

## 参考

- [AOSP ViewGroup：dispatchTouchEvent、TouchTarget 与拦截逻辑](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/view/ViewGroup.java)
- [ViewParent.requestDisallowInterceptTouchEvent](https://developer.android.com/reference/android/view/ViewParent#requestDisallowInterceptTouchEvent(boolean))
