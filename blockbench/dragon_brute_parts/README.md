这个目录里放的是基于参考三视图拆分出来的“龙人 / Dragon Brute”Blockbench 几何蓝图。

文件说明：
- `dragon_brute_full.geo.json`：完整拼装版，整体高度约 `80` 个 Blockbench 单位，也就是 `5` 个方块高
- 其他 `*.json`：按部位拆开的独立几何，方便分别导入 Blockbench 微调

建模约定：
- 格式使用 Bedrock Geometry JSON，便于直接被 Blockbench 打开
- 贴图尺寸统一预留为 `128x128`
- 所有拆件都保持同一套世界坐标，导入后能直接对齐回完整模型
- 当前是纯模型蓝图，没有额外绑定到 NeoForge 的实体渲染逻辑

部位划分：
- 躯干：`pelvis`、`belt`、`torso`、`chest_plates`
- 头部：`neck`、`head`、`jaw`、`left_horn`、`right_horn`、`left_head_spikes`、`right_head_spikes`、`back_spines`
- 手臂：`left_arm`、`left_forearm`、`left_hand`、`right_arm`、`right_forearm`、`right_hand`
- 下半身：`loin_front`、`loin_back`、`left_thigh`、`left_shin`、`left_foot`、`right_thigh`、`right_shin`、`right_foot`、`tail`
