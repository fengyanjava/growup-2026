# Dart 异步机制与 Flutter 错误上报

## 1. 事件循环与微任务

一个 Isolate 一次执行一段 Dart 代码。当前同步代码结束后，先清空微任务队列，再处理一个事件，然后重复这个过程。

| 队列 | 常见来源 | 执行顺序 |
| --- | --- | --- |
| 微任务队列 | `scheduleMicrotask()`、`Future.microtask()` | 下一个事件之前执行，直到队列为空 |
| 事件队列 | `Timer`、`Future(() {})`、I/O 和 UI 事件 | 微任务清空后处理一个事件 |

```dart
print('A');
Future(() => print('B'));
scheduleMicrotask(() => print('C'));
print('D');
// A → D → C → B
```

微任务适合在当前同步调用结束后尽快执行短操作，例如延迟通知、合并同一轮同步调用中的多次更新：

```dart
bool notificationScheduled = false;

void requestNotify() {
  if (notificationScheduled) return;
  notificationScheduled = true;

  scheduleMicrotask(() {
    notificationScheduled = false;
    notifyListeners();
  });
}
```

这只合并微任务执行前发生的更新，不等于跨事件防抖或每帧通知一次。持续添加微任务会让 UI 等事件得不到执行。

| 需求 | 方式 |
| --- | --- |
| 当前同步代码结束后、下一个事件前执行 | `scheduleMicrotask()` |
| 排到后续事件执行 | `Timer.run()`、`Future(() {})` |
| Flutter 当前帧结束后操作 | `addPostFrameCallback()` |
| 并行处理耗时计算 | 其他 Isolate |

## 2. 耗时操作为什么可能阻塞 UI

Flutter 主 Isolate 需要执行界面相关 Dart 代码。耗时的同步计算占用它时，会导致动画卡顿、点击无响应。

- **I/O 等待**：网络请求、异步文件读写使用异步 API，等待期间可处理其他事件。
- **CPU 计算**：大 JSON 解析、图片处理、复杂循环需要考虑其他 Isolate，例如 `Isolate.run()` 或 `compute()`。

`async`、`Future`、微任务都不会自动把计算移到后台：

```dart
Future<void> example() async {
  heavyCalculation(); // 仍在调用它的 Isolate 执行
}

Future(() => heavyCalculation()); // 推迟执行，仍可能阻塞 UI
```

`await` 暂停当前异步函数，不阻塞整个 Isolate。等待结束后，函数继续在原来的 Isolate 执行，因此请求后的解析仍可能造成卡顿：

```dart
final response = await http.get(url);
final body = response.body;
final data = await Isolate.run(() => jsonDecode(body));
```

Isolate 有创建和数据传递开销，适合足够重的计算。其他 Isolate 不能直接修改主 Isolate 的 Widget 状态。Flutter Web 的 `compute()` 在当前事件循环中执行，不提供原生平台上的 Isolate 并行效果，重计算需考虑 Web Worker 等方案。

## 3. 谁在执行 HTTP 请求

在 Android/iOS 上，常见的 `package:http` 默认实现路径是：

```text
主 Isolate 中的 package:http Dart 代码
  → dart:io HttpClient / Socket
  → Dart VM 原生网络实现
  → 操作系统 socket API / 内核网络栈
```

主 Isolate 执行请求构造和响应处理等 Dart 代码；Dart 运行时与操作系统负责底层网络 I/O。等待网络数据时，底层利用 I/O 通知机制，不占住主 Isolate 持续等待，也不需要每个请求创建一个 Isolate。

数据到达后，运行时通知对应 Isolate，由 Dart 代码处理响应、完成 Future，再恢复 `await` 后的代码。

这里的“原生”主要指运行时原生实现和操作系统，不代表默认请求通过 Platform Channel 调用 Kotlin/Swift。使用其他 HTTP 客户端时路径可能不同；Flutter Web 使用浏览器提供的网络能力。

## 4. Flutter 错误捕获入口

| 错误来源 | 处理入口 |
| --- | --- |
| 框架捕获的 build、layout、paint 等错误 | `FlutterError.onError` |
| 指定 Zone 内未处理的同步、异步错误 | `runZonedGuarded` 的错误处理器 |
| 主 Isolate 中未被其他机制处理的错误 | `PlatformDispatcher.instance.onError` |
| 业务代码已捕获的异常 | 在 `catch` 中主动上报 |
| 其他 Isolate 的未处理错误 | 错误监听、消息传递，或处理其返回 Future 的错误 |
| Android/iOS 原生崩溃 | 支持原生崩溃采集的 SDK |

这些入口不是每个异常依次经过的流水线。错误已被某个入口处理后，不会自动再交给下一个入口。

### FlutterError.onError：框架主动报告

Flutter 框架捕获部分错误后，调用 `FlutterError.reportError(details)`，后者调用 `FlutterError.onError`。它是回调调用，不是重新抛出异常。

```text
build 等操作抛错
  → 框架 catch
  → FlutterError.reportError(details)
  → FlutterError.onError(details)
```

核心逻辑可简化为：

```dart
static void reportError(FlutterErrorDetails details) {
  onError?.call(details);
}
```

业务代码也能主动调用 `FlutterError.reportError()`。正常情况下，已报告的错误不会自动进入 Zone；如果 `onError` 自己抛错，该错误则会继续传播。

### runZonedGuarded：指定范围内的未处理错误

它捕获传入 `body` 向外抛出的同步错误，以及该 Zone 中未处理的异步错误：

```dart
runZonedGuarded(() {
  Timer.run(() {
    throw StateError('异步任务失败');
  });
}, (error, stack) {
  // 上报未处理错误
});
```

被业务 `try/catch` 或 Flutter 框架消化的错误，不会自动到达这里。若用它包裹应用启动，Binding 初始化和 `runApp()` 应在同一个 Zone 中。

### PlatformDispatcher.onError：主 Isolate 兜底

常见场景是未处理的异步回调异常，例如按钮异步操作在 `await` 后抛错，且没有被业务或 Zone 接住。

它的准确范围是主 Isolate 中未处理的错误，不是“所有异步错误”，也不是专门处理原生异常。返回 `true` 表示该入口已处理错误，不代表失败的业务已经恢复。

## 5. 统一上报结构

下面采用框架入口和主 Isolate 兜底入口。`uploadError`、`MyApp` 由项目提供：

```dart
import 'dart:async';
import 'dart:ui';
import 'package:flutter/material.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  FlutterError.onError = (details) {
    FlutterError.presentError(details);
    unawaited(reportError(
      details.exception,
      details.stack ?? StackTrace.current,
      source: 'flutter',
    ));
  };

  PlatformDispatcher.instance.onError = (error, stack) {
    unawaited(reportError(error, stack, source: 'uncaught'));
    return true;
  };

  runApp(const MyApp());
}

Future<void> reportError(
  Object error,
  StackTrace stack, {
  required String source,
}) async {
  try {
    await uploadError({
      'errorType': error.runtimeType.toString(),
      'message': error.toString(),
      'stack': stack.toString(),
      'source': source,
      'time': DateTime.now().toUtc().toIso8601String(),
    });
  } catch (uploadFailure) {
    debugPrint('错误上报失败：$uploadFailure');
  }
}
```

业务已捕获但仍需排查的异常，应主动调用统一上报函数。预期内的取消、校验失败等通常作为业务结果处理。

生产环境还需考虑：

- 上报失败不递归上报；重复捕获应去重。
- 异步上传在进程退出前不保证完成，应缓存并重试，或交由成熟 SDK 处理。
- 附带版本、构建号、系统、设备、页面、关键操作和请求追踪 ID。
- 保存对应构建的符号文件，以还原混淆堆栈。
- 对敏感信息脱敏；系统强杀、部分 OOM 等不能依赖 Dart 回调捕获。
- 接入 SDK 时遵循其初始化方案，避免覆盖已有处理器或重复注册。

## 6. Zone 的概念与粒度

Zone 是 Dart 的执行上下文，不是线程或 Isolate，也不提供并行能力。其范围由 `runZoned()` / `runZonedGuarded()` 的调用位置划定，可以是一项业务操作，也可以覆盖应用启动后的大量执行流程。

在 Zone 中注册的异步回调通常会记住注册时的 Zone，因此经过 `await`、Timer、微任务后，仍可使用对应上下文。Zone 可以嵌套，子 Zone 继承并覆盖父 Zone 的部分行为。

常见用途：

| 场景 | 用途 |
| --- | --- |
| 错误上报 | 为指定执行范围设置未处理错误处理器 |
| 日志追踪 | 跨多层异步调用携带 traceId 等数据 |
| 测试工具 | 拦截定时器、微任务，控制异步行为 |
| 日志工具 | 拦截对应 Zone 中的 `print()` |

```dart
final traceIdKey = Object();

void log(String message) {
  print('[${Zone.current[traceIdKey]}] $message');
}

void runOperation() {
  runZoned(() async {
    await Future.delayed(Duration.zero);
    log('操作完成');
  }, zoneValues: {
    traceIdKey: 'operation-123',
  });
}
```

Zone 并不是给某个 Future 的所有后续操作永久绑定上下文；回调通常使用注册时的 Zone。不同错误 Zone 之间存在错误传播边界，跨边界使用失败的 Future 可能出现等待方无法收到错误的情况。

普通业务优先使用 `async/await` 和 `try/catch`；需要统一管理跨异步调用的上下文或行为时，再引入 Zone。

## 7. Symbol 与 Zone key

`#traceId` 是 Dart 的 Symbol 字面量，类型为 `Symbol`，表示标识符名称，不会读取同名变量：

```dart
final traceId = '123';
print(traceId);  // 123
print(#traceId); // Symbol("traceId")
```

Symbol 是普通对象值，可以赋给变量、作为参数、放入 List 或作为 Map key，并非仅用于 Map。常见用途包括：

- `noSuchMethod` / `Invocation` 中表示被调用成员的名称。
- 反射 API 中表示成员名称；Flutter 不支持 `dart:mirrors`。
- 作为 Map、Zone 上下文的 key。

```dart
Symbol name = #traceId;
final symbols = [#traceId, #userId];
final values = <Symbol, String>{#traceId: '123'};
```

`#traceId` 不是 Zone 特有语法。Zone key 也可以是字符串或对象；读取时查找当前 Zone 及其父 Zone 的相应值。

Symbol 不保证每次创建都唯一，`#traceId == #traceId` 为 `true`。普通文本使用 String；动态成员名称使用 Symbol；需要避免模块间 key 冲突时，使用共享的独立 `Object()` 实例。

## 参考资料

- [Dart 并发与事件循环](https://dart.dev/language/concurrency)
- [Dart HttpClient](https://api.dart.dev/dart-io/HttpClient-class.html)
- [Flutter 错误处理](https://docs.flutter.dev/testing/errors)
- [Dart Zone](https://dart.dev/libraries/async/zones)
- [runZonedGuarded API](https://api.dart.dev/dart-async/runZonedGuarded.html)
