import 'package:flutter/material.dart';

import 'widgets/lesson_scope.dart';
import 'widgets/lifecycle_probe.dart';

class DependenciesPage extends StatefulWidget {
  const DependenciesPage({super.key});

  @override
  State<DependenciesPage> createState() => _DependenciesPageState();
}

class _DependenciesPageState extends State<DependenciesPage> {
  final _events = <String>[];
  final _visibleEvents = ValueNotifier<List<String>>([]);
  bool _scheduled = false;
  int _value = 0;
  int _config = 0;
  int _generation = 0;
  late Widget _probe;

  @override
  void initState() {
    super.initState();
    _probe = _createProbe();
  }

  Widget _createProbe() => LifecycleProbe(
    key: ValueKey(_generation),
    label: '配置 $_config',
    onEvent: _record,
  );

  void _record(String event) {
    _events.add(event);
    if (_scheduled) return;
    _scheduled = true;
    // 生命周期回调可能处于 build 阶段，帧结束后只刷新日志区域。
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scheduled = false;
      if (!mounted) return;
      _visibleEvents.value = List.of(_events);
    });
  }

  @override
  void dispose() {
    _visibleEvents.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('didChangeDependencies 实验')),
    body: SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '依赖变化，还是配置变化？',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 12),
          const Text(
            '下方子组件通过 LessonScope.of(context) 建立 InheritedWidget 依赖。按钮分别改变依赖、父级配置与本地状态；日志记录实际生命周期。',
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              FilledButton(
                onPressed: () {
                  _record('操作：修改依赖');
                  setState(() => _value++);
                },
                child: const Text('修改依赖值'),
              ),
              OutlinedButton(
                onPressed: () {
                  _record('操作：父级配置更新');
                  setState(() {
                    _config++;
                    _probe = _createProbe();
                  });
                },
                child: const Text('修改父级配置'),
              ),
              OutlinedButton(
                onPressed: () {
                  _record('操作：父级重建，依赖值不变');
                  setState(() {});
                },
                child: const Text('父级空重建'),
              ),
              OutlinedButton(
                onPressed: () {
                  _record('操作：更换 Key');
                  setState(() {
                    _generation++;
                    _probe = _createProbe();
                  });
                },
                child: const Text('更换 Key'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: LessonScope(value: _value, child: _probe),
            ),
          ),
          const SizedBox(height: 16),
          const Text(
            '如何观察',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
          ),
          const SizedBox(height: 8),
          const Text(
            '• 首次挂载：initState → didChangeDependencies → build。\n'
            '• 修改依赖值：didChangeDependencies → build。\n'
            '• 修改父级配置：didUpdateWidget → build。\n'
            '• 自身 setState：build。\n'
            '• 父级空重建：没有子组件生命周期回调。\n'
            '• 更换 Key：创建新 State，重新执行初始化；旧 State 会 dispose。',
          ),
          const SizedBox(height: 12),
          const Text(
            '本例缓存子 Widget 实例，让修改依赖时只观察依赖通知。父级配置按钮才创建同类型、同 Key 的新 Widget。空重建时 updateShouldNotify 返回 false，且子 Widget 实例没变，因此不会触发子组件重建。真实业务中两种更新可能在同一帧同时发生。',
          ),
          const SizedBox(height: 12),
          const Text(
            'Theme.of、MediaQuery.of 也是类似的依赖读取。didChangeDependencies 可能执行多次，不应无条件重复创建订阅或发起网络请求。',
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              const Expanded(
                child: Text(
                  '实际回调日志',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                ),
              ),
              TextButton(
                onPressed: () {
                  _events.clear();
                  _visibleEvents.value = [];
                },
                child: const Text('清空日志'),
              ),
            ],
          ),
          ValueListenableBuilder<List<String>>(
            valueListenable: _visibleEvents,
            builder: (context, events, _) => SelectableText(
              key: const ValueKey('lifecycle-log'),
              events.indexed
                  .map((entry) => '${entry.$1 + 1}. ${entry.$2}')
                  .join('\n'),
              style: const TextStyle(fontFamily: 'monospace', height: 1.7),
            ),
          ),
        ],
      ),
    ),
  );
}
