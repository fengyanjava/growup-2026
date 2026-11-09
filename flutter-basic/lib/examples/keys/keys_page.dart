import 'package:flutter/material.dart';

import 'widgets/key_comparison.dart';

class KeysPage extends StatelessWidget {
  const KeysPage({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Key 与顺序对调')),
    // 保持实验实例挂载，避免滚动出屏幕影响 State 的观察。
    body: SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '为什么加了 Key，状态还是没跟着走？',
            style: Theme.of(context).textTheme.headlineSmall,
          ),
          const SizedBox(height: 8),
          const Text(
            '每组 A 初始为 1、B 初始为 5。先给 A 点一次 +1，再对调，观察名称、计数和 State 编号。名称属于 Widget，计数属于 State。',
          ),
          const SizedBox(height: 12),
          const KeyComparison(
            key: ValueKey('none'),
            placement: KeyPlacement.none,
            title: '① 不加 Key：状态留在原位置',
            explanation: '同一位置的类型和 Key 都相同，Element 被复用。对调后名称改变，但计数留在原位置：原来 A 的计数会显示在 B 上。',
          ),
          const KeyComparison(
            key: ValueKey('inner'),
            placement: KeyPlacement.inner,
            title: '② Key 放在内部：状态被重建',
            explanation: '结构是 Row → Expanded → Padding → CounterTile(key)。外层仍按位置复用；每个 Padding 内部的 Key 从 A 变成 B（或反过来），旧子节点被替换。手动增加的计数丢失，State 编号改变。',
          ),
          const KeyComparison(
            key: ValueKey('outer'),
            placement: KeyPlacement.outer,
            title: '③ Key 放在排序层：状态跟着身份移动',
            explanation: '结构是 Row → Expanded(key) → Padding → CounterTile。Row 能按 A/B 的 Key 匹配自己的直接子节点，整个子树移动，计数与 State 编号跟着 A/B 保留。',
          ),
          const SizedBox(height: 12),
          const Text(
            '要点：LocalKey 在同一父节点的子节点之间参与匹配。应把稳定的业务 Key 放在参与排序的直接子节点上；每次 build 创建 UniqueKey 会改变身份，无法保留原 State。',
          ),
        ],
      ),
    ),
  );
}
