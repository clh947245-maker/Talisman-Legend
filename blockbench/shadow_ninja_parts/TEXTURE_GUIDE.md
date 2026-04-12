# 暗影忍者贴图说明

贴图脚本会生成下面两个文件：

- `textures/shadow_ninja_uv_template.png`：用于绘制的 UV 展开模板
- `textures/shadow_ninja_texture_base.png`：已经铺好基础配色的起始贴图

推荐使用流程：

1. 用 Blockbench、Aseprite 或 Photoshop 打开 `shadow_ninja_uv_template.png`。
2. 把 `shadow_ninja_texture_base.png` 作为配色参考图层放在上方或下方。
3. 新建图层继续补阴影、高光和细节，最后导出合并后的正式贴图。

建议配色分区：

- 头部和兜帽：接近纯黑，边缘带一点冷灰高光
- 面罩主体：深青灰色
- 眼罩边框：青色
- 眼睛：高亮红色
- 胸前外层和护肩：深炭灰，边缘略亮
- 袖子和裤子主体：近黑色
- 腰带和领口：暗红色
- 手、小腿、脚：低饱和蓝绿色

建议后续细化内容：

- 在护肩和面罩边缘补 1 像素亮边。
- 在裤腿和袖子下方补更深的褶皱阴影。
- 让眼睛保持全身最高对比度，作为视觉焦点。
- 没有使用到的 UV 空间可以保持透明，或者先不处理。
