# Key 与顺序对调

从首页进入「Key 与顺序对调」。每组先给 A 点一次 +1（变成 2），再点击「对调顺序」。

| 场景 | 对调后从左到右 | State 身份 |
| --- | --- | --- |
| 无 Key | B=2，A=5 | 留在原位置，状态与名称错配 |
| Key 在 CounterTile 内层 | B=5，A=1 | 被替换，手动计数丢失 |
| Key 在 Row 的直接子节点 Expanded 上 | B=5，A=2 | 跟随 A/B 移动，状态保留 |

`widgets/counter_tile.dart` 将名称保存在 Widget 配置中，将计数保存在 State 中；State 编号用于直观看身份变化。

`widgets/key_comparison.dart` 统一生成三组结构，仅改变 Key 放置方式。LocalKey 不跨父节点匹配，所以只在内部 CounterTile 加 Key，不能指导外层 Row 调整子树身份。

初始值只在 initState 读取。错误层级时重新创建的 State 又拿到初始值，可能看起来“数值仍跟着名字”，因此需要先 +1，并结合 State 编号观察。

测试使用 identical 验证 State 身份，并检查对调后计数。
