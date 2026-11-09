import 'package:flutter/material.dart';

/// 每个示例只需提供稳定标识、介绍与页面构造方法。
class ExampleEntry {
  const ExampleEntry({
    required this.id,
    required this.title,
    required this.description,
    required this.builder,
  });

  final String id;
  final String title;
  final String description;
  final WidgetBuilder builder;
}
