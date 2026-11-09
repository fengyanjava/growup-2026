import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_basic/app.dart';
import 'package:flutter_basic/examples/keys/widgets/counter_tile.dart';
import 'package:flutter_basic/examples/keys/widgets/key_comparison.dart';

void main() {
  for (final placement in KeyPlacement.values) {
    testWidgets('${placement.name} 对调后的状态身份与计数', (tester) async {
      await tester.pumpWidget(const MyApp());
      await tester.tap(find.byKey(const ValueKey('keys')));
      await tester.pumpAndSettle();
      final group = find.byKey(ValueKey(placement.name));
      final tiles = find.descendant(
        of: group,
        matching: find.byType(CounterTile),
      );
      final oldA = tester.state(tiles.at(0));
      final oldB = tester.state(tiles.at(1));
      final increment = find.descendant(
        of: tiles.at(0),
        matching: find.text('+1'),
      );
      await tester.ensureVisible(increment);
      await tester.tap(increment);
      await tester.pump();
      expect(
        find.descendant(of: tiles.at(0), matching: find.text('计数：2')),
        findsOneWidget,
      );

      final swap = find.descendant(of: group, matching: find.text('对调顺序'));
      await tester.ensureVisible(swap);
      await tester.tap(swap);
      await tester.pump();
      expect(tester.widget<CounterTile>(tiles.at(0)).name, 'B');
      expect(tester.widget<CounterTile>(tiles.at(1)).name, 'A');
      final left = tester.state(tiles.at(0));
      final right = tester.state(tiles.at(1));
      late int leftCount;
      late int rightCount;
      switch (placement) {
        case KeyPlacement.none:
          expect(identical(left, oldA), isTrue);
          expect(identical(right, oldB), isTrue);
          leftCount = 2;
          rightCount = 5;
        case KeyPlacement.inner:
          expect(identical(left, oldA), isFalse);
          expect(identical(left, oldB), isFalse);
          expect(identical(right, oldA), isFalse);
          expect(identical(right, oldB), isFalse);
          expect(oldA.mounted, isFalse);
          expect(oldB.mounted, isFalse);
          leftCount = 5;
          rightCount = 1;
        case KeyPlacement.outer:
          expect(identical(left, oldB), isTrue);
          expect(identical(right, oldA), isTrue);
          leftCount = 5;
          rightCount = 2;
      }
      expect(
        find.descendant(of: tiles.at(0), matching: find.text('计数：$leftCount')),
        findsOneWidget,
      );
      expect(
        find.descendant(of: tiles.at(1), matching: find.text('计数：$rightCount')),
        findsOneWidget,
      );

      final reset = find.descendant(of: group, matching: find.text('重置本组'));
      await tester.tap(reset);
      await tester.pump();
      expect(tester.widget<CounterTile>(tiles.at(0)).name, 'A');
      expect(
        find.descendant(of: tiles.at(0), matching: find.text('计数：1')),
        findsOneWidget,
      );
      expect(
        find.descendant(of: tiles.at(1), matching: find.text('计数：5')),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
    });
  }
}
