import 'package:flutter/material.dart';

import 'lesson_scope.dart';

class LifecycleProbe extends StatefulWidget {
  const LifecycleProbe({super.key, required this.label, required this.onEvent});

  final String label;
  final ValueChanged<String> onEvent;

  @override
  State<LifecycleProbe> createState() => _LifecycleProbeState();
}

class _LifecycleProbeState extends State<LifecycleProbe> {
  int _localCount = 0;
  int _inheritedValue = 0;

  @override
  void initState() {
    super.initState();
    widget.onEvent('initState');
    // 此处不能通过 dependOnInheritedWidgetOfExactType 建立依赖。
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // 首次挂载读取并建立依赖；以后收到通知时重新读取。
    _inheritedValue = LessonScope.of(context);
    widget.onEvent('didChangeDependencies: value=$_inheritedValue');
    // 框架接下来会 build，无需额外 setState。
  }

  @override
  void didUpdateWidget(covariant LifecycleProbe oldWidget) {
    super.didUpdateWidget(oldWidget);
    widget.onEvent('didUpdateWidget: ${oldWidget.label} → ${widget.label}');
  }

  @override
  Widget build(BuildContext context) {
    widget.onEvent('build: value=$_inheritedValue, local=$_localCount');
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('父级配置：${widget.label}'),
        Text('依赖值：$_inheritedValue · 本地计数：$_localCount'),
        const SizedBox(height: 8),
        OutlinedButton(
          onPressed: () {
            widget.onEvent('操作：自身 setState');
            setState(() => _localCount++);
          },
          child: const Text('自身 setState'),
        ),
      ],
    );
  }

  @override
  void dispose() {
    widget.onEvent('dispose');
    super.dispose();
  }
}
