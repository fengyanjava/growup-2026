import 'package:flutter/material.dart';

import '../models/render_metrics.dart';
import 'paint_tile.dart';

class Probe extends StatelessWidget {
  const Probe({super.key, required this.child});
  final Widget child;
  @override
  Widget build(BuildContext context) => child;
}

class DemoTile extends StatelessWidget {
  const DemoTile({
    super.key,
    required this.color,
    required this.extent,
    required this.metrics,
  });
  final Color color;
  final double extent;
  final RenderMetrics metrics;
  @override
  Widget build(BuildContext context) =>
      PaintTile(color: color, extent: extent, metrics: metrics);
}
