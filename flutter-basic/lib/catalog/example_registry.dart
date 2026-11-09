import '../examples/dependencies/dependencies_page.dart';
import '../examples/three_trees/three_trees_page.dart';
import 'example_entry.dart';

/// 新增示例时在这里注册，目录页会自动生成入口。
final List<ExampleEntry> examples = List.unmodifiable([
  ExampleEntry(
    id: 'dependencies',
    title: 'didChangeDependencies 生命周期',
    description: '对比依赖变化、配置更新与 setState 的真实回调顺序。',
    builder: (_) => const DependenciesPage(),
  ),
  ExampleEntry(
    id: 'three-trees',
    title: 'Flutter 三棵树',
    description: '观察 Widget、Element、RenderObject 的关系、复用与更新。',
    builder: (_) => const TreeLab(),
  ),
]);
