import 'package:flutter/material.dart';

import 'example_registry.dart';

class ExampleCatalogPage extends StatelessWidget {
  const ExampleCatalogPage({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Flutter 示例集')),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('从实验理解 Flutter', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 8),
        const Text('选择一个示例，动手操作并对照源码学习。'),
        const SizedBox(height: 16),
        for (final example in examples)
          Card(
            child: ListTile(
              key: ValueKey(example.id),
              title: Text(example.title),
              subtitle: Text(example.description),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => Navigator.of(context).push(
                MaterialPageRoute<void>(
                  settings: RouteSettings(name: '/examples/${example.id}'),
                  builder: example.builder,
                ),
              ),
            ),
          ),
      ],
    ),
  );
}
