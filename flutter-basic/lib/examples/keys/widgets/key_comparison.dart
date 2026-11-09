import 'package:flutter/material.dart';

import 'counter_tile.dart';

enum KeyPlacement { none, inner, outer }

class KeyComparison extends StatefulWidget {
  const KeyComparison({
    super.key,
    required this.placement,
    required this.title,
    required this.explanation,
  });

  final KeyPlacement placement;
  final String title;
  final String explanation;

  @override
  State<KeyComparison> createState() => _KeyComparisonState();
}

class _KeyComparisonState extends State<KeyComparison> {
  bool _reversed = false;
  int _generation = 0;

  Widget _item(String name) {
    // Row 直接比较的子节点是 Expanded，而不是内部 CounterTile。
    // LocalKey 无法跨越各自父 Element，去另一个父节点下寻找同名子节点。
    return Expanded(
      key: widget.placement == KeyPlacement.outer ? ValueKey(name) : null,
      child: Padding(
        padding: const EdgeInsets.all(4),
        child: CounterTile(
          key: widget.placement == KeyPlacement.inner ? ValueKey(name) : null,
          name: name,
          initialCount: name == 'A' ? 1 : 5,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(widget.title, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8),
          Text(widget.explanation),
          const SizedBox(height: 12),
          // 仅重置按钮改变 Row 的 Key，用于清空本组实验状态。
          Row(
            key: ValueKey(_generation),
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              for (final name in (_reversed ? ['B', 'A'] : ['A', 'B']))
                _item(name),
            ],
          ),
          Wrap(
            spacing: 8,
            children: [
              FilledButton.tonal(
                onPressed: () => setState(() => _reversed = !_reversed),
                child: const Text('对调顺序'),
              ),
              TextButton(
                onPressed: () => setState(() {
                  _reversed = false;
                  _generation++;
                }),
                child: const Text('重置本组'),
              ),
            ],
          ),
        ],
      ),
    ),
  );
}
