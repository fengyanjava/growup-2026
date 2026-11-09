import 'package:flutter/material.dart';

import '../models/render_metrics.dart';

/// RenderObjectWidget 是配置与渲染对象之间的桥梁。
class PaintTile extends LeafRenderObjectWidget {
  const PaintTile({
    super.key,
    required this.color,
    required this.extent,
    required this.metrics,
  });
  final Color color;
  final double extent;
  final RenderMetrics metrics;

  @override
  RenderTile createRenderObject(BuildContext context) {
    metrics.creates++;
    return RenderTile(color, extent, metrics);
  }

  @override
  void updateRenderObject(BuildContext context, RenderTile renderObject) {
    metrics.updates++;
    renderObject
      ..color = color
      ..extent = extent;
  }
}

class RenderTile extends RenderBox {
  RenderTile(this._color, this._extent, this.metrics);
  final RenderMetrics metrics;
  Color _color;
  double _extent;

  set color(Color value) {
    if (_color == value) return;
    _color = value;
    markNeedsPaint(); // 颜色不影响几何尺寸。
  }

  set extent(double value) {
    if (_extent == value) return;
    _extent = value;
    markNeedsLayout(); // 尺寸会影响布局。
  }

  @override
  Size computeDryLayout(BoxConstraints constraints) =>
      constraints.constrain(Size.square(_extent));

  @override
  void performLayout() {
    metrics.layouts++;
    size = computeDryLayout(constraints);
  }

  @override
  void paint(PaintingContext context, Offset offset) {
    metrics.paints++;
    context.canvas.drawRRect(
      RRect.fromRectAndRadius(offset & size, const Radius.circular(18)),
      Paint()..color = _color,
    );
  }

  @override
  void dispose() {
    metrics.disposes++;
    super.dispose();
  }
}
