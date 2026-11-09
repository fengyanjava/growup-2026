import 'package:flutter/widgets.dart';

/// of 建立依赖，updateShouldNotify 决定是否通知依赖者。
class LessonScope extends InheritedWidget {
  const LessonScope({super.key, required this.value, required super.child});

  final int value;

  static int of(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<LessonScope>()!.value;

  @override
  bool updateShouldNotify(LessonScope oldWidget) => value != oldWidget.value;
}
