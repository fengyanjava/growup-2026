import 'package:flutter/material.dart';

import 'models/render_metrics.dart';
import 'models/tree_snapshot.dart';
import 'widgets/demo_tile.dart';

/// GlobalKey 只用于定位观察入口；实验切换的是内部 DemoTile 的 LocalKey。
class TreeLab extends StatefulWidget {
  const TreeLab({super.key});

  @override
  State<TreeLab> createState() => _TreeLabState();
}

class _TreeLabState extends State<TreeLab> {
  final _probeKey = GlobalKey();
  final _metrics = RenderMetrics();
  bool _purple = false;
  bool _large = false;
  int _generation = 0;
  int _builds = 0;
  String _experiment = '初次挂载：创建 Element，再创建 RenderObject。';

  @override
  void initState() {
    super.initState();
    _inspectAfterFrame();
  }

  void _inspectAfterFrame() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final widgets = <String>[];
      final elements = <String>[];
      final renders = <String>[];
      void visit(Element element, int depth) {
        final indent = '  ' * depth;
        widgets.add(
          '$indent${element.widget.runtimeType} #${identityHashCode(element.widget).toRadixString(16)}',
        );
        elements.add(
          '$indent${element.runtimeType} #${identityHashCode(element).toRadixString(16)}',
        );
        if (element is RenderObjectElement) {
          final render = element.renderObject;
          renders.add(
            '${render.runtimeType} #${identityHashCode(render).toRadixString(16)}',
          );
        }
        element.visitChildren((child) => visit(child, depth + 1));
      }

      final root = _probeKey.currentContext as Element?;
      if (root != null) visit(root, 0);
      // 只更新观察面板，不重新 build 被观察的实验子树。
      _panel.value = TreeSnapshot(
        widgets,
        elements,
        renders,
        '创建 ${_metrics.creates} · 更新 ${_metrics.updates} · 布局 ${_metrics.layouts} · 绘制 ${_metrics.paints} · 释放 ${_metrics.disposes}',
      );
    });
  }

  final _panel = ValueNotifier<TreeSnapshot>(
    const TreeSnapshot([], [], [], '等待首帧'),
  );

  void _change(String explanation, VoidCallback change) {
    setState(() {
      change();
      _builds++;
      _experiment = explanation;
    });
    _inspectAfterFrame();
  }

  @override
  void dispose() {
    _panel.dispose();
    super.dispose();
  }

  Widget _section(String title, Widget child) => Card(
    elevation: 0,
    color: Colors.white,
    child: Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 12),
          child,
        ],
      ),
    ),
  );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Flutter 三棵树实验室')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const Text(
            '一份配置，一份实例，一份渲染',
            style: TextStyle(fontSize: 26, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          const Text(
            'Widget 描述 UI；Element 管理挂载与更新；RenderObject 负责布局、绘制与命中测试。下面的编号来自实际对象，并非模拟数据。',
          ),
          const SizedBox(height: 16),
          _section(
            '01 / 动手实验',
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    OutlinedButton(
                      onPressed: () => _change(
                        '相同类型 + 相同 Key：新 Widget 配置交给原 Element，RenderObject 继续复用。',
                        () {},
                      ),
                      child: const Text('重新 build'),
                    ),
                    OutlinedButton(
                      onPressed: () => _change(
                        '只改颜色：复用 Element 和 RenderObject，通过 markNeedsPaint 请求重绘。',
                        () => _purple = !_purple,
                      ),
                      child: const Text('修改颜色'),
                    ),
                    OutlinedButton(
                      onPressed: () => _change(
                        '修改尺寸：通过 markNeedsLayout 请求布局；布局后可能需要重绘。',
                        () => _large = !_large,
                      ),
                      child: const Text('修改尺寸'),
                    ),
                    OutlinedButton(
                      onPressed: () => _change(
                        'Key 改变：旧 Element 子树被替换，创建新的 Element 和 RenderObject。',
                        () => _generation++,
                      ),
                      child: const Text('切换 Key'),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Text('手动重建：$_builds 次 · DemoTile key: $_generation'),
                const SizedBox(height: 8),
                Text(_experiment),
                SizedBox(
                  height: 170,
                  child: Center(
                    child: Probe(
                      key: _probeKey,
                      child: DemoTile(
                        key: ValueKey(_generation),
                        color: _purple
                            ? Colors.deepPurple
                            : const Color(0xff3457d5),
                        extent: _large ? 140 : 90,
                        metrics: _metrics,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          ValueListenableBuilder<TreeSnapshot>(
            valueListenable: _panel,
            builder: (context, snapshot, _) => Column(
              children: [
                _section(
                  '02 / 当前真实子树',
                  LayoutBuilder(
                    builder: (context, constraints) {
                      final cards = [
                        _tree(
                          'Widget · 配置',
                          snapshot.widgets,
                          const Color(0xff3457d5),
                        ),
                        _tree(
                          'Element · 实例',
                          snapshot.elements,
                          const Color(0xff008577),
                        ),
                        _tree(
                          'RenderObject · 渲染',
                          snapshot.renders,
                          const Color(0xffad5b13),
                        ),
                      ];
                      return Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            '观察范围：Probe 及其后代。缩进代表父子关系；渲染树跳过没有 RenderObject 的组件。',
                          ),
                          const SizedBox(height: 12),
                          if (constraints.maxWidth >= 900)
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: cards
                                  .map((card) => Expanded(child: card))
                                  .toList(),
                            )
                          else
                            ...cards,
                        ],
                      );
                    },
                  ),
                ),
                _section(
                  '03 / RenderObject 生命周期计数',
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(snapshot.counts),
                      const SizedBox(height: 8),
                      const Text(
                        '计数在每次实验帧结束后采样。窗口尺寸、系统重绘等也可能触发布局或绘制，不能把次数当作固定规律。',
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          _section(
            '04 / 为什么需要三棵树？',
            const Text(
              '① Widget 是不可变配置，build 可以创建新配置；const Widget 也可能直接复用。\n\n'
              '② Element 是持久的挂载实例，持有当前 Widget。BuildContext 就是 Element 的接口，StatefulElement 还持有 State。更新匹配的核心条件是 runtimeType 和 key 相同。\n\n'
              '③ 只有 RenderObjectWidget 对应的 Element 才管理 RenderObject。本例 Probe、DemoTile 都没有自己的 RenderObject，最终的 PaintTile 才创建 RenderTile。\n\n'
              '④ setState 标记对应 Element 需要重建；更新配置后，RenderObject 根据属性变化决定是否请求布局或绘制，并非每次 build 都重建渲染树。\n\n'
              '⑤ 布局遵循：约束向下、尺寸向上、父节点决定位置。绘制将布局结果画到画布。Key 帮助框架识别身份；这里更换 LocalKey 会替换子树。',
            ),
          ),
        ],
      ),
    );
  }

  Widget _tree(String title, List<String> lines, Color color) => Container(
    width: double.infinity,
    margin: const EdgeInsets.all(4),
    padding: const EdgeInsets.all(12),
    decoration: BoxDecoration(
      color: color.withValues(alpha: 0.07),
      borderRadius: BorderRadius.circular(12),
    ),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: TextStyle(color: color, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 8),
        SelectableText(
          lines.join('\n'),
          style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
        ),
      ],
    ),
  );
}
