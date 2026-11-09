# Flutter 示例集

运行 `flutter run`，从首页进入「Flutter 三棵树」，依次点击四个按钮观察对象编号与生命周期计数。

## 项目结构与新增示例

```text
lib/
  main.dart                         # 仅启动应用
  app.dart                          # 应用主题与首页
  catalog/
    example_entry.dart              # 示例描述模型
    example_registry.dart           # 统一注册示例
    example_catalog_page.dart       # 示例目录和导航
  examples/
    three_trees/
      three_trees_page.dart         # 实验页面与交互
      models/                       # 快照和渲染计数模型
      widgets/                      # 实验组件和 RenderObject
```

新增示例：在 `lib/examples/<示例名>/` 创建页面及其组件，在 `lib/catalog/example_registry.dart` 添加一个 `ExampleEntry`（唯一 id、标题、介绍、builder），首页会自动显示入口。每个示例自行提供 Scaffold，导航自动提供返回功能。多个示例需要相同组件时再提取公共组件。

示例测试放在 `test/examples/`，首页导航测试位于 `test/widget_test.dart`。

## 观察顺序

| 操作 | Widget | Element / RenderObject | 渲染行为 |
| --- | --- | --- | --- |
| 重新 build | 创建新配置 | 复用 | 属性相同，无须主动布局或重绘 |
| 修改颜色 | 创建新配置 | 复用 | `markNeedsPaint` |
| 修改尺寸 | 创建新配置 | 复用 | `markNeedsLayout` |
| 切换 Key | 创建新配置 | 替换实验子树 | 创建、布局、绘制，释放旧对象 |

页面只观察 `Probe → DemoTile → PaintTile` 这段真实子树。每行编号来自 `identityHashCode`，用于本次运行内辅助观察身份（哈希理论上可能碰撞）；测试使用 `identical` 验证真实身份。

```text
Widget 配置树          Element 实例树           RenderObject 渲染树
Probe                 StatelessElement          │
  DemoTile              StatelessElement        │ 无独立渲染对象
    PaintTile             LeafRenderObjectElement → RenderTile
```

## 对照源码读

实现位于 `lib/examples/three_trees/`：

- `three_trees_page.dart` 中的 `TreeLab`：`setState` 更新实验配置，帧结束后遍历 Element 读取三棵树。
- `widgets/demo_tile.dart` 中的 `Probe` / `DemoTile`：普通 StatelessWidget，只负责组合配置。
- `widgets/paint_tile.dart` 中的 `PaintTile`：LeafRenderObjectWidget，通过 `createRenderObject` 创建渲染对象，通过 `updateRenderObject` 更新已有对象。
- 同文件中的 `RenderTile`：RenderBox，尺寸变化请求布局、颜色变化请求绘制；`performLayout` 遵守父级约束，`paint` 绘制圆角方块。

Widget 不可变、通常短命，但 const 实例可以复用；Element 管理挂载、更新与子节点，BuildContext 是其接口；RenderObject 保存布局与绘制所需状态。State 存放在 StatefulElement 上，不存放在 Widget 上。

在本例同一父节点位置，`runtimeType` 和 `key` 相同即可更新原 Element；改变 LocalKey 会替换对应子树。复杂列表匹配、GlobalKey 跨位置迁移不在本实验范围内。并非所有 Widget 都有自己的 RenderObject，因此三棵树不一一对应。

`setState → 标记 Element 需要 build → 更新 Widget 配置 → 按属性变化请求 layout / paint`。布局时约束向下、尺寸向上、父节点决定位置。build、layout、paint 是不同阶段，build 不代表必然重新布局或绘制。

计数是整个实验累计值，在实验帧末采样；窗口变化及系统重绘也可能影响计数。观察面板通过独立 ValueNotifier 更新，不会因为刷新编号而重建实验子树。

运行验证：

```sh
flutter analyze
flutter test
```
