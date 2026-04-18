This folder contains a separated Blockbench geometry kit for Shengzhu's palace.

Files:
- `shengzhu_palace_full.geo.json`: assembled palace reference model.
- other `*.json`: individual editable palace sections.
- `generate_shengzhu_palace.ps1`: source-of-truth generator.

Design goals:
- based on the supplied concept image, but scaled up into a more imposing post-boss palace.
- split into large architectural chunks so every zone can be adjusted in Blockbench independently.
- keeps the main courtyard and central hall readable for future explorable block construction.

Parts:
- foundation_base
- courtyard_floor
- outer_ring_walls
- front_gate
- front_stairway
- corner_watchtowers
- left_wing_shell
- right_wing_shell
- side_buttresses
- main_hall_shell
- rear_keep
- grand_roof
- upper_roof
- central_tower
- left_front_pagoda
- right_front_pagoda
- left_mid_pagoda
- right_mid_pagoda
- left_rear_pagoda
- right_rear_pagoda
- roof_accents

Notes:
- Bedrock / GeckoLib geometry json format, directly openable in Blockbench.
- Texture canvas reserved as 2048x4096 for direct palace coloring.
- Approximate full footprint is ~17 x 18 blocks with a tall multi-tier silhouette.