# didChangeDependencies 生命周期实验

从首页选择「didChangeDependencies 生命周期」。页面日志来自真实 State 回调。

| 操作 | 预期子组件回调 |
| --- | --- |
| 初次进入 | initState → didChangeDependencies → build |
| 修改依赖值 | didChangeDependencies → build |
| 修改父级配置 | didUpdateWidget → build |
| 自身 setState | build |
| 父级空重建 | 无 |
| 更换 Key | 新 State 初始化，旧 State dispose |

- `dependencies_page.dart`：实验操作和帧末日志显示。日志区域单独刷新，避免干扰子组件。
- `widgets/lesson_scope.dart`：`of` 通过 `dependOnInheritedWidgetOfExactType` 建立依赖，`updateShouldNotify` 按值是否变化决定是否通知。
- `widgets/lifecycle_probe.dart`：实际生命周期回调；在 `didChangeDependencies` 读取依赖。

首次挂载即会执行 didChangeDependencies；以后依赖通知会再次触发它。不是任意祖先重建都会触发。通过 GlobalKey 移动子树等改变继承环境的情况也可能触发，本例聚焦依赖通知。

本例特意缓存子 Widget，修改依赖时不创建新的子配置。只有「修改父级配置」才创建同类型、同 Key 的新 Widget，从而隔离两种更新。实际业务中，同一帧可以既更新配置又更新依赖。

initState 不应用依赖查找方法订阅 InheritedWidget；didChangeDependencies 可以，并且之后框架会执行 build，无须额外 setState。异步操作完成后更新界面仍应遵循 mounted 检查与 setState 的常规规则。
