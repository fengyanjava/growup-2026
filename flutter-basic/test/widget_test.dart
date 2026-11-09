import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_basic/app.dart';
import 'package:flutter_basic/catalog/example_catalog_page.dart';
import 'package:flutter_basic/examples/three_trees/three_trees_page.dart';

void main() {
  testWidgets('从示例目录进入实验并返回', (tester) async {
    await tester.pumpWidget(const MyApp());
    expect(find.byType(ExampleCatalogPage), findsOneWidget);
    await tester.tap(find.byKey(const ValueKey('three-trees')));
    await tester.pumpAndSettle();
    expect(find.byType(TreeLab), findsOneWidget);
    expect(find.text('重新 build'), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();
    expect(find.byType(ExampleCatalogPage), findsOneWidget);
    expect(find.byType(TreeLab), findsNothing);
    expect(tester.takeException(), isNull);
  });
}
