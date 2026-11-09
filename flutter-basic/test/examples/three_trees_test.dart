import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_basic/examples/three_trees/three_trees_page.dart';
import 'package:flutter_basic/examples/three_trees/widgets/paint_tile.dart';

void main() {
  testWidgets('配置更新复用实例，Key 改变替换子树', (tester) async {
    await tester.pumpWidget(const MaterialApp(home: TreeLab()));
    await tester.pump();
    final tile = find.byType(PaintTile);
    final widget = tester.widget<PaintTile>(tile);
    final element = tester.element(tile);
    final render = tester.renderObject<RenderTile>(tile);
    final metrics = widget.metrics;
    final layouts = metrics.layouts;
    final paints = metrics.paints;

    await tester.tap(find.text('重新 build'));
    await tester.pump();
    expect(identical(tester.widget(tile), widget), isFalse);
    expect(identical(tester.element(tile), element), isTrue);
    expect(identical(tester.renderObject(tile), render), isTrue);
    expect(metrics.layouts, layouts);

    await tester.tap(find.text('修改颜色'));
    await tester.pump();
    expect(identical(tester.renderObject(tile), render), isTrue);
    expect(metrics.layouts, layouts);
    expect(metrics.paints, greaterThan(paints));

    await tester.tap(find.text('修改尺寸'));
    await tester.pump();
    expect(render.size, const Size.square(140));
    expect(metrics.layouts, greaterThan(layouts));

    await tester.tap(find.text('切换 Key'));
    await tester.pump();
    expect(identical(tester.element(tile), element), isFalse);
    expect(identical(tester.renderObject(tile), render), isFalse);
    expect(metrics.creates, 2);
    expect(metrics.disposes, 1);
    expect(tester.takeException(), isNull);
  });
}
