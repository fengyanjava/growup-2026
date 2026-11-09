import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_basic/app.dart';

void main() {
  testWidgets('区分依赖通知、配置更新和自身重建', (tester) async {
    await tester.pumpWidget(const MyApp());
    await tester.tap(find.byKey(const ValueKey('dependencies')));
    await tester.pumpAndSettle();

    String log() => tester
        .widget<SelectableText>(find.byKey(const ValueKey('lifecycle-log')))
        .data!;
    expect(
      log(),
      contains(
        '1. initState\n2. didChangeDependencies: value=0\n3. build: value=0, local=0',
      ),
    );

    Future<String> act(String label) async {
      final before = log();
      await tester.ensureVisible(find.text(label));
      await tester.tap(find.text(label));
      await tester.pumpAndSettle();
      return log().substring(before.length);
    }

    var added = await act('修改依赖值');
    expect(added, contains('didChangeDependencies: value=1'));
    expect(added, contains('build: value=1, local=0'));
    expect(added, isNot(contains('didUpdateWidget')));
    expect(added, isNot(contains('initState')));

    added = await act('修改父级配置');
    expect(added, contains('didUpdateWidget: 配置 0 → 配置 1'));
    expect(added, contains('build: value=1, local=0'));
    expect(added, isNot(contains('didChangeDependencies')));

    added = await act('自身 setState');
    expect(added, contains('build: value=1, local=1'));
    expect(added, isNot(contains('didChangeDependencies')));
    expect(added, isNot(contains('didUpdateWidget')));

    added = await act('父级空重建');
    expect(added, contains('操作：父级重建，依赖值不变'));
    expect(added, isNot(contains('build:')));
    expect(added, isNot(contains('didChangeDependencies')));

    added = await act('更换 Key');
    expect(added, contains('initState'));
    expect(added, contains('didChangeDependencies: value=1'));
    expect(added, contains('build: value=1, local=0'));
    expect(added, contains('dispose'));
    expect(tester.takeException(), isNull);

    await tester.pageBack();
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
  });
}
