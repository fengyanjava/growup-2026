# Flutter 性能优化面试问答

整理本轮已讨论的问题。每题先给出面试回答，再说明执行过程与易错边界；代码为局部示意，省略页面、模型及控制器的完整定义。

## 一、回答框架

### 1. 面试问到 Flutter 性能优化，可以从哪些方向回答？

**回答：**先明确问题和测量指标，用工具定位瓶颈，再有针对性地优化，最后在相同设备、相同场景下验证收益。

| 方向 | 关注点 | 常见手段 |
| --- | --- | --- |
| 重建与布局 | UI 线程中的重复工作 | 缩小状态更新范围、复用稳定子树、避免 build 中重计算和多余布局 |
| 绘制与渲染 | 重绘范围和场景复杂度 | 按需设置 RepaintBoundary，排查不必要的离屏绘制等效果 |
| 列表与图片 | 条目创建、图片解码及缓存成本 | 懒构建、已知条目尺寸、合适的解码尺寸、控制缓存与预加载 |
| 耗时计算 | 主 isolate 被同步计算占用 | 先测量，必要时将重计算移到其他 isolate |
| 内存与资源 | 泄漏、内存膨胀和频繁分配 | 取消任务与订阅、释放资源、检查对象持有关系 |
| 启动与首屏 | 首屏之前的等待链 | 只保留必要依赖，延迟或按需执行其他任务 |

可口述为：

> 我会先判断是滑动卡顿、响应慢、首屏慢，还是内存持续增长。对卡顿问题，在真机 profile 模式下用 DevTools 区分 UI 与 Raster 耗时：UI 慢检查重建、布局和同步计算，Raster 慢检查绘制复杂度和重绘范围。列表、图片、内存和启动再按场景专项处理。优化后对比帧耗时分布、内存或首屏时间，确认收益。

继续追问时，选一个真实问题说明“现象 → 定位 → 修改原因 → 验证结果”。没有实测数据时说明验证方法，不编造提升比例。

## 二、一帧的执行与性能测量

### 2. 怎样区分 Dart UI 线程耗时和 Raster 线程耗时？

**回答：**UI 线程准备场景，Raster 线程处理场景的渲染。在 DevTools 的慢帧中分别检查两项耗时，判断瓶颈所在阶段。

```text
UI：执行 Dart → build / layout / paint → 提交场景
                                           ↓
Raster：处理绘制指令 → 与 GPU 协作生成画面
```

- UI 包括应用和框架的 Dart 工作；paint 主要记录绘制指令，不等于像素已经显示到屏幕上。
- Raster 线程运行在 CPU 上，负责引擎图形工作；其耗时不等于 GPU 的纯执行时间。
- 场景可能准备很快，却画起来很贵，因此 UI 快不代表 Raster 也快。

在真机运行 `flutter run --profile`，打开 DevTools → Performance，复现卡顿，选择 Flutter frames 中的慢帧。图表中每帧有 UI 和 Raster 两项耗时；进一步结合 Frame analysis 和 Timeline events 查看原因。

以 60Hz、预算约 16.7ms 为例：

| UI | Raster | 初步判断 |
| --- | --- | --- |
| 22ms | 5ms | 优先排查 UI 阶段 |
| 5ms | 23ms | 优先排查渲染阶段 |
| 21ms | 24ms | 两边都需要排查，可先检查 UI |

120Hz 的预算约为 8.3ms。Debug 的额外检查与执行方式会干扰性能判断。上述 DevTools 帧图针对移动端和桌面端；Web 使用 Chrome DevTools Performance 路径。

参考：[Performance 面板](https://docs.flutter.dev/tools/devtools/performance)、[性能分析与线程说明](https://docs.flutter.dev/perf/ui-performance)。

### 3. UI 和 Raster 各花 10ms，为什么合计 20ms 仍可能达到 60fps？

**回答：**同一帧两个阶段有依赖，不同帧可以流水线重叠。持续交付画面的间隔和单帧经历的总时间，是不同指标。

忽略其他开销，假设每隔 16.7ms 开始一帧：

```text
帧 A：UI [0～10]       Raster [10～20]
帧 B：UI [16.7～26.7]  Raster [26.7～36.7]
帧 C：UI [33.4～43.4]  Raster [43.4～53.4]
```

Raster 处理 A 时，UI 可以准备 B。完成时刻仍间隔 16.7ms，稳定后可能持续输出 60fps，但每帧延迟仍有 20ms。

不能仅凭 UI + Raster 超过预算就断定达不到目标帧率；也不能仅凭两项各自未超预算，就保证最低延迟或没有其他调度问题。

```dart
// 绑定初始化后注册一次；实际测量时避免大量日志干扰。
SchedulerBinding.instance.addTimingsCallback((frames) {
  for (final f in frames) {
    debugPrint(
      'UI=${f.buildDuration.inMicroseconds / 1000}ms '
      'Raster=${f.rasterDuration.inMicroseconds / 1000}ms '
      'Span=${f.totalSpan.inMicroseconds / 1000}ms',
    );
  }
});
```

`buildDuration` 是 UI 线程准备整帧的时间，不只是某个 Widget 的 build 方法；`totalSpan` 从该帧 VSync 起点算到 Raster 完成，包含中间等待，不一定等于前两项之和，也不是完整的“点击到屏幕显示”延迟。

参考：[FrameTiming](https://api.flutter.dev/flutter/dart-ui/FrameTiming-class.html)、[totalSpan](https://api.flutter.dev/flutter/dart-ui/FrameTiming/totalSpan.html)。

### 4. 平均帧耗时很低，为什么用户仍然觉得卡？

**回答：**平均值会掩盖少数严重慢帧。要分别检查 UI 和 Raster 的耗时分布，而不只看平均 FPS 或平均耗时。

例如十帧 UI 耗时为 `5、5、5、5、5、5、5、5、5、70ms`，平均只有 11.5ms，但最后一帧仍可能造成明显停顿。前面省下的时间不能存给后面的慢帧使用。

- 平均耗时：整体工作量。
- P90、P99：较慢的那部分帧。P99 为 25ms，大致表示 99% 的样本不超过 25ms。
- 最慢帧：是否仍存在严重停顿。

优化前后应使用相同设备、相同操作场景，并关注超过目标预算的帧是否减少。

参考：[官方性能指标](https://docs.flutter.dev/perf/metrics)。

## 三、重建、布局与动画

### 5. 连续调用两次 setState，会立即执行两次 build 吗？

**回答：**不会直接同步执行 build。两个回调都会同步执行，但同一个 Element 在重建前重复被标脏，不会重复安排相同的重建请求。

```dart
void onTap() {
  setState(() => count++);
  setState(() => count++);
  debugPrint('回调结束：$count');
}
```

从 count = 0 开始：第一次回调改为 1 并标脏；第二次改为 2，但已是 dirty；先打印 2，随后构建阶段读取到 2。这次点击通常只引起一次该节点的 build。

被合并的是重建请求，不是状态修改。setState 回调内的同步重计算也会立刻占用 UI 线程；仅将计算移到 setState 回调外，并不能自动解决主 isolate 阻塞。

参考：[setState](https://api.flutter.dev/flutter/widgets/State/setState.html)、[markNeedsBuild](https://api.flutter.dev/flutter/widgets/Element/markNeedsBuild.html)。

### 6. build 执行后，一定会重新 layout 和 paint 吗？

**回答：**不一定。build 更新 Widget 配置，是否布局、绘制由属性变化及 RenderObject 的脏标记决定。

```dart
SizedBox(
  width: 100,
  height: 100,
  child: ColoredBox(color: selected ? Colors.red : Colors.blue),
)
```

假设首次布局已完成、父约束不变，仅切换颜色：框架更新配置，复用对应 Element 和 RenderObject，颜色 setter 调用 markNeedsPaint；颜色变化本身不要求重新 layout。

因此，复用节点不代表不 build，发生 build 也不代表布局和绘制无条件全部重做。反过来，没有 rebuild 的区域也可能因重绘传播而执行 paint。

参考：[ColoredBox 更新](https://api.flutter.dev/flutter/widgets/ColoredBox/updateRenderObject.html)、[颜色 setter 源码](https://github.com/flutter/flutter/blob/master/packages/flutter/lib/src/widgets/basic.dart)。

### 7. 拆成 StatelessWidget，就能避免跟着父组件 rebuild 吗？

**回答：**单纯拆出组件不能保证。父组件每次返回新的子 Widget 配置，StatelessElement 更新后仍会执行子组件 build。

```dart
InfoPanel();       // 每次普通构造会产生新配置
const InfoPanel(); // 相同常量配置可复用同一个实例
```

| 判断 | 作用 |
| --- | --- |
| 同一个 Widget 实例（默认相等判断下） | 可跳过此次父级传来的节点更新 |
| 不同实例，但类型与 key 相同 | 复用 Element、更新配置；这个 StatelessWidget 会 rebuild |

const 不是“永远不 build”：如果组件依赖 Theme 等继承环境，对应依赖变化仍可触发重建。不依赖 const，缓存并复用同一个 Widget 实例也可利用这一更新捷径。

参考：[Element.updateChild](https://api.flutter.dev/flutter/widgets/Element/updateChild.html)。

### 8. 只有计数器变化，怎样避免整个页面跟着 rebuild？

**回答：**将频繁变化的状态放到实际使用它的局部组件中，让重建从更小的节点开始。

```text
Page
├─ HeavyHeader
└─ Counter：自己保存 count，自己调用 setState
```

Counter.setState 只将对应 Element 标脏，不会向上触发 Page.build，因此不会经由 Page 更新 HeavyHeader。这个边界不依赖 Counter 调用处是否写 const。

- 状态下沉：缩小本次重建的起点。
- 复用 Widget 实例：父级已重建时，跳过稳定子节点的更新。

需要共享的状态仍应放在合适的公共层级，再控制监听和更新范围。

参考：[控制 build 成本](https://docs.flutter.dev/perf/best-practices#control-build-cost)。

### 9. 动画每帧变化，怎样避免复杂静态内容反复 build？

**回答：**将不依赖动画值的子树放在 AnimatedBuilder.child 中，让动画更新期间复用同一个 Widget 实例。

```dart
AnimatedBuilder(
  animation: controller,
  child: HeavyPanel(data: data),
  builder: (_, child) => Transform.rotate(
    angle: controller.value,
    child: child,
  ),
)
```

外层创建面板配置；动画通知使 builder 再次执行；新的 Transform 仍使用原 child，因此不会因这次动画更新重建面板。若在 builder 内每次创建 HeavyPanel，则会更新面板配置并触发其 build。

child 不必是 const；外层数据变化时仍可正常更新它。这减少的是 build 工作，动画的绘制、合成成本仍需检查。

参考：[AnimatedBuilder 优化](https://api.flutter.dev/flutter/widgets/AnimatedBuilder-class.html#performance-optimizations)。

### 10. 平移动画为什么可以考虑 Transform，而不总是改布局位置？

**回答：**纯视觉变换可以避免因每帧位置变化而重新布局，但不改变组件在布局中占据的空间。

```dart
Transform.translate(
  offset: Offset(controller.value * 100, 0),
  child: child,
)
```

AnimatedPositioned 改变位置时会触发 relayout；Transform 在布局之后、绘制时应用变换，仅改变平移量不要求子树重新布局。

视觉上移动后，布局占位仍在原处，周围组件不会被推开。纯视觉移动可考虑 Transform / SlideTransition；需要改变尺寸或周围排布时，仍需要相应布局。

参考：[Transform](https://api.flutter.dev/flutter/widgets/Transform-class.html)、[AnimatedPositioned 与 SlideTransition](https://api.flutter.dev/flutter/widgets/AnimatedPositioned-class.html)。

## 四、绘制与渲染

### 11. 静态图表没有 rebuild，为什么还会 paint？RepaintBoundary 有什么用？

**回答：**重绘范围与重建范围不同。如果图表和旁边的动画缺少合适的重绘边界，动画的重绘可能连带图表执行 paint。

```dart
Row(
  children: [
    RepaintBoundary(child: ComplexStaticChart()),
    BusyAnimation(),
  ],
)
```

可以把边界理解为“图表拥有一张独立绘图纸”：首次正常绘制；旁边动画变化时，如果图表自身未被标记需要重绘，就复用其已有 layer，跳过内部 paint。图表自身变化时仍会重绘。

RepaintBoundary 隔离重绘传播，不阻止 build 或 layout，也不保证缓存为位图。引擎可能额外进行光栅缓存，但复用绘制指令不代表 GPU 无须处理这部分画面。

适合绘制复杂、与周围重绘频率不同的区域。它也增加 layer 成本，应对比 paint 与整体帧耗时确认收益。

参考：[RepaintBoundary](https://api.flutter.dev/flutter/widgets/RepaintBoundary-class.html)。

### 12. 整组 Opacity 为什么可能比直接画半透明颜色更贵？

**回答：**整组半透明可能需要先画入离屏缓冲区，再按统一透明度合成到最终画面，增加存储、绘制、合成及绘制目标切换的成本。

```text
Opacity(opacity: 0.5, child: ComplexPanel())
    ↓
先绘制整组到中间区域 → 整组以 50% 透明度合成
```

如果只需要半透明纯色背景，可以直接指定颜色 alpha：

```dart
const ColoredBox(color: Color.fromRGBO(255, 0, 0, 0.5))
```

不能把整组透明一律替换成各子组件分别透明：存在重叠时，分别透明会使下面的内容透出来，效果可能不同。实际是否有显著离屏开销，以 profile 结果为准。

参考：[Opacity](https://api.flutter.dev/flutter/widgets/Opacity-class.html)、[saveLayer 原理与成本](https://api.flutter.dev/flutter/dart-ui/Canvas/saveLayer.html)。

## 五、长列表、滚动与节点身份

### 13. ListView.builder 比提前生成 children 省在哪里？

**回答：**它按视口和缓存区域需求创建条目，避免提前创建全部 Widget 配置，并按需构建条目子树。

```dart
// 立即创建一万个 ProductTile 配置对象。
ListView(
  children: List.generate(10000, (i) => ProductTile(product: products[i])),
)

// 布局需要条目时再调用回调。
ListView.builder(
  itemCount: products.length,
  itemBuilder: (_, i) => ProductTile(product: products[i]),
)
```

第一种写法不代表所有条目都已执行内部 build、layout、paint；首先提前发生的是 Widget 配置对象的创建。先生成全部 Widget 再从 itemBuilder 返回它们，会失去按需创建配置的收益。UI 懒构建也不等于数据分页。

参考：[ListView](https://api.flutter.dev/flutter/widgets/ListView/ListView.html)、[ListView.builder](https://api.flutter.dev/flutter/widgets/ListView/ListView.builder.html)。

### 14. 列表项固定高度时，itemExtent 有什么作用？

**回答：**提前提供滚动方向上的条目尺寸，让列表直接计算条目位置，减少通过子项布局获取高度的工作，尤其有利于大幅滚动跳转。

```dart
ListView.builder(
  itemCount: products.length,
  itemExtent: 72,
  itemBuilder: (_, i) => ProductTile(product: products[i]),
)
```

每项高 72 时，第 i 项起点为 i × 72。条目内部仍需布局，itemExtent 不会跳过全部 layout。

itemExtent 会强制条目使用指定尺寸，不是估算值。需要换行、展开或随字体缩放而增高的内容，不应随便固定高度。

参考：[itemExtent](https://api.flutter.dev/flutter/widgets/ListView/itemExtent.html)。

### 15. ScrollView 包 ListView，开启 shrinkWrap 后会全部创建 item 吗？

**回答：**如果是同方向的 SingleChildScrollView 包普通有限的 ListView.builder，且内层没有高度上限，首次布局会将整个列表展开，创建、构建并布局全部条目。

```dart
SingleChildScrollView(
  child: ListView.builder(
    shrinkWrap: true,
    physics: const NeverScrollableScrollPhysics(),
    itemCount: 1000,
    itemBuilder: (_, i) => Text('第 $i 项'),
  ),
)
```

执行过程：外层提供无界高度 → 内层按内容确定高度 → 布局中不断调用 itemBuilder → 直到列表末尾。屏幕看不见的条目，也会在这个展开过程中创建。

这不代表 shrinkWrap 在所有场景都构建全部条目。如果用 SizedBox(height: 300) 等为内层提供有限视口，通常仍按视口及缓存范围准备条目。NeverScrollableScrollPhysics 只禁止内层用户滚动，不会恢复无界展开时丢失的懒构建收益。

判断关键是父级约束与内层视口大小，而非仅看 shrinkWrap 的布尔值。shrink wrapping 本身也可能增加尺寸重新计算的成本。

参考：[shrinkWrap](https://api.flutter.dev/flutter/widgets/ScrollView/shrinkWrap.html)、[SingleChildScrollView](https://api.flutter.dev/flutter/widgets/SingleChildScrollView-class.html)、[列表布局实现](https://api.flutter.dev/flutter/rendering/RenderSliverList/performLayout.html)。

### 16. CustomScrollView + SliverList 为什么能保留懒构建？

**回答：**顶部内容和列表共享一个有限滚动视口，列表直接按传入的滚动位置、剩余绘制区域与缓存范围提供内容。

```dart
CustomScrollView(
  slivers: [
    SliverToBoxAdapter(child: Header()),
    SliverList.builder(
      itemCount: products.length,
      itemBuilder: (_, i) => ProductTile(product: products[i]),
    ),
  ],
)
```

假设视口高 800、Header 高 200、每项高 100，忽略缓存时列表初始只需提供约 600 高的内容。滚动后，视口更新需求，列表再准备对应条目。

SliverList 不是另一层独立滚动容器，而是同一视口中的一段。这里的前提是 CustomScrollView 自身有合适的有限约束，不是将它再次无界展开。

参考：[CustomScrollView](https://api.flutter.dev/flutter/widgets/CustomScrollView-class.html)、[SliverConstraints](https://api.flutter.dev/flutter/rendering/SliverConstraints-class.html)。

### 17. 条目滑出去再回来，State 为什么可能丢失？怎样保活？

**回答：**条目离开视口及缓存范围，且未请求保活时，节点和 State 可能被销毁；滑回时创建新 State，局部计数等就恢复初值。

```dart
class _ItemState extends State<Item>
    with AutomaticKeepAliveClientMixin<Item> {
  @override
  bool get wantKeepAlive => true;

  @override
  Widget build(BuildContext context) {
    super.build(context); // 必须调用，忽略返回值。
    return Text(widget.title);
  }
}
```

普通 ListView.builder 默认提供接收保活请求的机制。addAutomaticKeepAlives: true 不等于默认保留全部条目；wantKeepAlive 变化时需要调用 updateKeepAlive。

保活减少重复创建、保留局部状态，但增加内存占用。如果浏览过的条目全部长期保活，内存可能随滚动增长。点赞、选中等业务状态通常放在外部数据模型中，让节点重建后也能恢复。

参考：[AutomaticKeepAliveClientMixin](https://api.flutter.dev/flutter/widgets/AutomaticKeepAliveClientMixin-mixin.html)、[addAutomaticKeepAlives](https://api.flutter.dev/flutter/widgets/SliverChildBuilderDelegate/addAutomaticKeepAlives.html)。

### 18. ValueKey 能代替 KeepAlive 吗？

**回答：**不能。Key 负责节点身份匹配，不保存已销毁的 State。

```dart
ProductTile(key: ValueKey(product.id), product: product)
```

即使滑回来时 Key 值相同，如果旧 State 已被销毁，仍要创建新 State。

| 机制 | 解决的问题 |
| --- | --- |
| ValueKey | 更新时节点对应哪条数据 |
| KeepAlive | 离开可见区域后是否保留节点和 State |
| 外部数据模型 | 节点销毁后如何恢复业务数据 |

Key 也不应被理解为保证任何懒列表重排都能自动保留状态；涉及重排时，还需满足相应列表委托的匹配要求。

参考：[Widget.key](https://api.flutter.dev/flutter/widgets/Widget/key.html)、[ValueKey](https://api.flutter.dev/flutter/foundation/ValueKey-class.html)。

### 19. 每次构建都给条目生成 UniqueKey，会更高效吗？

**回答：**通常不会。每个 UniqueKey 只与自身相等，反复创建会让同一数据的新旧配置无法按原节点更新。

```dart
ProductTile(key: UniqueKey(), product: product) // 每次都是新身份。
ProductTile(key: ValueKey(product.id), product: product) // 稳定数据身份。
```

在对应位置更新时：类型相同但 Key 不同 → 移除旧 Element → 创建新节点和 State。这可能丢失局部状态并重复初始化、订阅等工作。

新的 ValueKey 对象只要类型和保存的值相同，就可以相等。稳定 Key 允许复用节点，不保证跳过 build。

参考：[Widget.key 匹配规则](https://api.flutter.dev/flutter/widgets/Widget/key.html)、[ValueKey 与 UniqueKey](https://api.flutter.dev/flutter/foundation/ValueKey-class.html)。

## 六、图片

### 20. 图片只显示 100×100，为什么仍可能占用十几 MB？

**回答：**显示尺寸不等于解码尺寸。图片下载后会解码为像素数据，内存占用与像素数有关，不等于压缩文件大小。

```text
下载压缩图片 → 解码为像素数据 → 缩放到显示区域
```

常见的每像素 4 字节情况下，2000×2000 原图约需 16MB 像素内存。仅设置 Image 的 width、height，不能自动保证按显示尺寸解码。

```dart
final dpr = MediaQuery.devicePixelRatioOf(context);
return Image.network(
  url,
  width: 100,
  height: 100,
  cacheWidth: (100 * dpr).ceil(),
);
```

假设原图为正方形、DPR 为 3，100 个逻辑像素对应 300 个物理像素，约 300×300 解码的像素内存为 0.36MB。单独指定解码宽度时按比例缩放；实际尺寸还需满足图片比例和显示方式的清晰度需求。

cacheWidth/cacheHeight 默认按物理像素解释，主要减少解码图像和图片缓存内存，并不会因此减少原 URL 的下载字节数。Web 的 Image.network 自定义解码尺寸行为存在平台限制，不能直接照搬原生端结论；部分较新 SDK 也提供逻辑缓存尺寸选项，使用时检查实际 SDK API。

参考：[Image 内存说明](https://api.flutter.dev/flutter/widgets/Image-class.html#memory-usage)、[Image.network 参数与平台行为](https://api.flutter.dev/flutter/widgets/Image/Image.network.html)。

## 七、异步与重计算

### 21. 接口请求用了 async/await，收到数据后为什么仍卡顿？

**回答：**async/await 允许异步等待，不会自动把后续同步计算移出主 isolate。

```dart
final text = await fetchJson(); // 网络等待期间可以处理其他任务。
final data = jsonDecode(text); // 仍在当前 isolate 同步解析。
```

如果解析占用主 isolate 80ms，这期间它无法及时处理界面帧任务。对已经确认造成卡顿的重计算，可以考虑 compute 或 Isolate.run：

```dart
import 'dart:convert';
import 'package:flutter/foundation.dart';

List<dynamic> parseItems(String text) => jsonDecode(text) as List<dynamic>;

Future<List<dynamic>> loadItems() async {
  final text = await fetchJson();
  return await compute(parseItems, text);
}
```

原生平台上，compute 在其他 isolate 解析，主 isolate 等待结果期间可继续处理界面。Web 上 compute 在当前事件循环执行，没有相同的 isolate 并行效果。

isolate 有创建和数据传递成本，不应把所有小计算都搬过去。区分异步 I/O 和 CPU 密集计算，再评估是否值得并行。

参考：[Dart 并发模型](https://dart.dev/language/concurrency)、[compute](https://api.flutter.dev/flutter/foundation/compute.html)。

## 八、内存与生命周期

### 22. Dart 有 GC，为什么还会内存泄漏？mounted 检查能解决吗？

**回答：**GC 回收不可达对象，不按“页面是否离开屏幕”决定回收。定时器、订阅或全局集合持续引用 State，可能使它无法回收。

```text
定时器继续运行 → 持有回调 → 回调引用 State → State 仍可达
```

```dart
late final Timer _timer;

@override
void initState() {
  super.initState();
  _timer = Timer.periodic(const Duration(seconds: 1), (_) {
    setState(() => seconds++);
  });
}

@override
void dispose() {
  _timer.cancel();
  super.dispose();
}
```

只加 if (!mounted) return 可以避免销毁后 setState，但不会自动取消任务或断开回调持有关系。应取消页面拥有的任务、订阅并释放资源；GC 不会替你调用这些业务清理方法。

验证时，可在 DevTools Memory 中比较多次进入、退出页面后的对象存活情况，检查持有路径，区分合理缓存与持续泄漏。

参考：[setState 生命周期建议](https://api.flutter.dev/flutter/widgets/State/setState.html)、[Memory 工具](https://docs.flutter.dev/tools/devtools/memory)。

## 九、启动与首屏

### 23. 启动慢，为什么先检查 runApp 之前等待的任务？

**回答：**即使初始化任务都是异步的，只要 main 等它们全部结束才 runApp，应用界面就无法提前开始构建。

```dart
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await loadLocalSettings();
  await initAnalytics();
  await prefetchRecommendations();
  runApp(const MyApp());
}
```

执行链是“设置 → 统计 → 推荐 → runApp → 首屏”。如果统计和推荐预取不是首屏依赖，可以把它们安排到首屏之后或按需执行，只保留必要初始化。

依赖关系必须正确：首屏需要的服务应先准备好，或让 UI 明确展示加载状态。runApp 返回也不意味着首帧已显示，不能只把重同步任务挪到它后面就认定不会影响首屏。

分别测量“第一帧出现”和“页面可用”：提前显示 loading 可能改善前者，但不一定缩短用户看到实际内容的等待。

参考：[runApp](https://api.flutter.dev/flutter/widgets/runApp.html)、[启动性能指标](https://docs.flutter.dev/perf/metrics)。

## 复习时优先分清的边界

| 容易混淆的概念 | 区别 |
| --- | --- |
| build / layout / paint | 更新配置、计算几何、记录绘制指令是不同阶段 |
| 复用 Element / 跳过 build | 类型与 Key 相同允许复用；同实例更新捷径才可能跳过父级引起的更新 |
| 流畅度 / 单帧延迟 | 流水线交付间隔与一帧总跨度不同 |
| const / RepaintBoundary | 前者可帮助复用配置，后者隔离重绘传播 |
| Key / KeepAlive | 身份匹配与节点存活是不同问题 |
| builder / 分页 | UI 按需构建与数据分批加载不同 |
| async / isolate | 异步等待不会自动把同步计算移到其他 isolate |
| 图片文件大小 / 解码内存 | 压缩字节数与像素缓冲区大小不同 |
| mounted 检查 / 资源清理 | 防止非法更新，不等于取消任务、断开引用 |
| 第一帧 / 页面可用 | 出现一帧画面，不等于实际内容已准备完成 |
