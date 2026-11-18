# Provider：ChangeNotifier 如何触发 Widget 重建

`ChangeNotifierProvider` 将两套机制连接起来：通过 `ChangeNotifier.addListener()` 接收状态通知，再通过 Flutter 的 `InheritedElement` 依赖机制触发依赖者重建。

理解这条链路，需要区分三个对象：

| 对象 | 职责 |
| --- | --- |
| `ChangeNotifier` | 保存业务状态，调用已注册的监听回调 |
| Provider 内部的 `InheritedElement` | 监听 model，管理依赖关系并发起依赖通知 |
| Consumer 或普通 Widget 对应的 Element | 登记依赖，收到通知后被标记为需要重建 |

以下代码是关键源码的节选或简化形式，私有类名可能随版本变化。示例使用 `package:provider`，用于说明机制，不代表当前项目已添加该依赖。

## 1. Consumer 如何建立依赖

`Consumer<T>` 是无状态组件，其构建逻辑的关键部分是：

```dart
return builder(
  context,
  Provider.of<T>(context),
  child,
);
```

`Provider.of<T>(context)` 默认 `listen: true`，内部会执行：

```dart
context.dependOnInheritedWidgetOfExactType<
    _InheritedProviderScope<T?>>();
```

这会建立依赖关系：当前 `context` 对应的 Element 被登记为目标 `InheritedElement` 的依赖者。Consumer 随后把读取到的 model 传给 `builder`。

准确地说，不是把 Element “放到 InheritedWidget 里面”，而是在 Element 之间记录依赖：

```text
Widget 树中的父子关系             Element 之间的依赖关系

ChangeNotifierProvider           Provider 内部的 InheritedElement
  └── Consumer                     └── 记录 Consumer 的 Element
        └── Text
```

Widget 是配置；长期管理依赖者的是对应的 `InheritedElement`。仅仅成为 Provider 的后代，不会自动建立依赖。

## 2. ChangeNotifierProvider 如何监听 model

相关继承关系是：

```text
ChangeNotifierProvider<T>
  → ListenableProvider<T>
  → InheritedProvider<T>
```

Provider 内部使用 `_InheritedProviderScope` 及其 `_InheritedProviderScopeElement`。开始监听 model 时，`ListenableProvider` 执行：

```dart
static VoidCallback _startListening(
  InheritedContext<Listenable?> e,
  Listenable? value,
) {
  value?.addListener(e.markNeedsNotifyDependents);
  return () => value?.removeListener(e.markNeedsNotifyDependents);
}
```

- `value` 是业务 model，也就是 `ChangeNotifier` 实例。
- `e` 是 Provider 内部的 `_InheritedProviderScopeElement`。
- 返回的函数用于在结束这次监听时移除回调。

监听通常在首次获取 Provider 的值时建立；默认的惰性初始化不意味着 Widget 一挂载就已经创建 model 并订阅它。

因此，model 调用 `notifyListeners()` 时，会同步调用 Provider 注册的 `markNeedsNotifyDependents()` 回调。Consumer 本身没有逐个给 model 注册 `addListener`，它依赖的是中间的 `InheritedElement`。

## 3. 从 notifyListeners 到 Consumer 重建

### 3.1 标记 Provider 内部的 Element

`markNeedsNotifyDependents()` 的核心逻辑是：

```dart
void markNeedsNotifyDependents() {
  if (!_isNotifyDependentsEnabled) return;

  markNeedsBuild();
  _shouldNotifyDependents = true;
}
```

它先将 Provider 内部的 Element 标记为需要构建，再记录“构建时需要通知依赖者”。此时不会立即执行 Consumer 的 `builder`。

### 3.2 在构建时通知依赖者

框架执行这个 Element 的 `build()` 时，相关逻辑是：

```dart
// 省略其他构建逻辑。
if (_shouldNotifyDependents) {
  _shouldNotifyDependents = false;
  notifyClients(widget);
}
```

Flutter 的 `InheritedElement.notifyClients()` 遍历已登记的依赖者，调用 `notifyDependent()`。

Provider 重写了 `notifyDependent()`：对普通 Consumer 或 `watch` 建立的依赖，会通知依赖者；对 `context.select` 建立的依赖，会先判断所选结果是否变化。需要通知时执行：

```dart
dependent.didChangeDependencies();
```

### 3.3 标记 Consumer 对应的 Element

Flutter 的 `Element.didChangeDependencies()` 内部调用 `markNeedsBuild()`，将依赖者标记为 dirty，交给框架调度重建。

Consumer 重建时，再次获取 model 并执行 `builder`，读取的是 model 的最新状态。Consumer 是无状态组件，也能通过 Element 的依赖机制重建，无需调用 `setState()`。

```text
model 修改状态
  → model.notifyListeners()
  → 同步执行 Provider 注册的回调
  → Provider 内部 Element.markNeedsNotifyDependents()
  → 标记 Provider 内部 Element 需要构建
  → 框架执行该 Element.build()
  → notifyClients()
  → notifyDependent()
  → Consumer 的 Element.didChangeDependencies()
  → Consumer 的 Element.markNeedsBuild()
  → 框架重建 Consumer，重新执行 builder
```

回调通知是同步的，UI 重建由 Flutter 调度执行。一次 `notifyListeners()` 不等于立刻执行一次 `builder`；在构建处理前连续通知，通常会合并为一次重建，界面读取最终状态。

## 4. 普通 Widget 放在 Provider 下面会不会重建

取决于它是否建立依赖，而不是它是否使用了 Consumer。

| 使用方式 | 是否建立依赖 | 是否通过本次通知触发重建 |
| --- | --- | --- |
| 不读取 Provider | 否 | 否 |
| `context.read<Counter>()` | 否 | 否 |
| `Provider.of<Counter>(context, listen: false)` | 否 | 否 |
| `context.watch<Counter>()` | 是 | 是 |
| `Provider.of<Counter>(context)` | 是 | 是，默认 `listen: true` |
| `context.select<Counter, int>((m) => m.count)` | 是 | 所选结果变化时触发 |
| `Consumer<Counter>` | 是 | Consumer 对应的 Element 被标记重建 |

例如，普通 `StatelessWidget` 也可以监听状态：

```dart
class CountText extends StatelessWidget {
  const CountText({super.key});

  @override
  Widget build(BuildContext context) {
    final counter = context.watch<Counter>();
    return Text('${counter.count}');
  }
}
```

这里登记的是 `CountText` 的 Element。收到通知后，框架重新执行 `CountText.build()`；即使父组件使用 `const CountText()`，也不会阻止依赖变化触发的重建。

如果改为在 `CountText.build()` 返回的子树中放置 `Consumer<Counter>`，登记的就是 Consumer 的 Element。通知会触发 Consumer 的 `builder`，不会因此直接触发外层 `CountText.build()`。

Consumer 的 `child` 参数可以复用不依赖状态的 Widget 实例，减少这条更新路径上的子树构建；如果该 child 自身也订阅了状态，它仍可能独立重建。

表格只描述 Provider 通知引起的重建。父组件更新、`setState()` 等其他原因也可能触发 Widget 重建。重建起点缩小，也不代表其下方子树完全不会更新。

## 5. 哪些是 Provider 的设计，哪些是 Flutter 的机制

| 环节 | 提供方 |
| --- | --- |
| `ChangeNotifier.addListener()` / `notifyListeners()` | Flutter 的通知机制 |
| 给 model 注册 `markNeedsNotifyDependents` 回调 | Provider |
| 收到回调后标记自身，在内部 Element 的 `build()` 中调用 `notifyClients()` | Provider |
| 登记依赖者，`notifyClients()` 遍历依赖者 | Flutter 的 `InheritedElement` |
| 重写 `notifyDependent()`，处理普通依赖和 `select` 过滤 | Provider |
| 依赖者的 `didChangeDependencies()` → `markNeedsBuild()` → 重建 | Flutter 的 Element 机制 |

普通 `InheritedWidget` 的典型更新路径是：

```text
父组件构建出新的 InheritedWidget
  → 对应的 InheritedElement 更新
  → updateShouldNotify(oldWidget) 返回 true
  → notifyClients()
  → 通知依赖者并将其标记为需要重建
```

Inherited 机制本身不会自动监听放进去的 ChangeNotifier。Provider 为 model 的通知建立了另一条入口：

```text
同一个 model 调用 notifyListeners()
  → Provider 的监听回调
  → Provider 内部 Element 被标记需要构建
  → 构建时显式调用 notifyClients()
  → 接入 Flutter 原有的依赖通知和重建机制
```

Provider 内部 `_InheritedProviderScope.updateShouldNotify()` 返回 `false`。这条 ChangeNotifier 通知路径通过显式调用 `notifyClients()`，绕过了该判断。

因此，无需替换 model 实例，也无需比较 model 的字段是否变化。对于普通 Consumer，即使状态实际没有变化，只要发送通知，也会触发这条更新流程；需要按所选数据过滤时，可使用 `context.select` 或 `Selector`。

## 6. 阅读源码

- [Consumer：调用 Provider.of 并执行 builder](https://github.com/rrousselGit/provider/blob/master/packages/provider/lib/src/consumer.dart)
- [Provider.of：获取数据并建立依赖](https://github.com/rrousselGit/provider/blob/master/packages/provider/lib/src/provider.dart)
- [ChangeNotifierProvider：继承 ListenableProvider](https://github.com/rrousselGit/provider/blob/master/packages/provider/lib/src/change_notifier_provider.dart)
- [ListenableProvider：注册和移除 model 的监听回调](https://github.com/rrousselGit/provider/blob/master/packages/provider/lib/src/listenable_provider.dart)
- [InheritedProvider：标记、构建和通知依赖者](https://github.com/rrousselGit/provider/blob/master/packages/provider/lib/src/inherited_provider.dart)
- [Flutter Element.didChangeDependencies](https://api.flutter.dev/flutter/widgets/Element/didChangeDependencies.html)
- [Flutter Element.markNeedsBuild](https://api.flutter.dev/flutter/widgets/Element/markNeedsBuild.html)
- [Flutter InheritedWidget](https://api.flutter.dev/flutter/widgets/InheritedWidget-class.html)
