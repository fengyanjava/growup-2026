import '../examples/keys/keys_page.dart';
import '../examples/dependencies/dependencies_page.dart';
import '../examples/three_trees/three_trees_page.dart';
import 'example_entry.dart';

/// 新增示例时在这里注册，目录页会自动生成入口。
final List<ExampleEntry> examples = List.unmodifiable([
  ExampleEntry(
    id: 'keys',
    title: 'Key 与顺序对调',
    description: '对比不加 Key、Key 放错层级和正确放置时的状态变化。',
    builder: (_) => const KeysPage(),
  ),
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
