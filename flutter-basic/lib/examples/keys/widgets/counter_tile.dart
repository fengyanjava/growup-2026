import 'package:flutter/material.dart';

/// name 来自 Widget 配置；count 和实例编号属于 State。
/// 对调后同时观察这两类数据，就能看出 State 是否跟着业务身份移动。
class CounterTile extends StatefulWidget {
  const CounterTile({
    super.key,
    required this.name,
    required this.initialCount,
  });

  final String name;
  final int initialCount;

  @override
  State<CounterTile> createState() => _CounterTileState();
}

class _CounterTileState extends State<CounterTile> {
  late int _count;

  @override
  void initState() {
    super.initState();
    _count = widget.initialCount;
  }

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(12),
    decoration: BoxDecoration(
      color: (widget.name == 'A' ? Colors.blue : Colors.orange).withValues(
        alpha: 0.12,
      ),
      borderRadius: BorderRadius.circular(12),
    ),
    child: Column(
      children: [
        Text(widget.name, style: Theme.of(context).textTheme.titleLarge),
        Text('计数：$_count'),
        Text(
          'State #${identityHashCode(this).toRadixString(16)}',
          style: const TextStyle(fontSize: 11),
        ),
        const SizedBox(height: 8),
        OutlinedButton(
          onPressed: () => setState(() => _count++),
          child: const Text('+1'),
        ),
      ],
    ),
  );
}
