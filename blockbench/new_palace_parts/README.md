This folder contains the new Blockbench geometry kit for the redesigned palace.

Files:
- `new_palace_full.geo.json`: assembled reference model generated from the editable parts.
- other `*.json`: individual editable palace sections.
- `generate_new_palace.ps1`: source-of-truth generator for the palace part files and full model.
- `generate_new_palace_structure.ps1`: converts the model into a structure-template style JSON.
- `generate_new_palace_nbt.py`: converts the structure JSON into the final `new_palace.nbt`.

Workflow:
1. Open any part `*.json` in Blockbench and adjust that section.
2. Run `generate_new_palace.ps1` after geometry edits to refresh all part outputs plus `new_palace_full.geo.json`.
3. Run `generate_new_palace_structure.ps1` to rebuild `new_palace_structure.json`.
4. Run `generate_new_palace_nbt.py` to rebuild the in-game `new_palace.nbt`.

Current palace layout:
- `foundation_base`
- `courtyard_floor`
- `outer_ring_walls`
- `front_gate`
- `front_stairway`
- `corner_watchtowers`
- `entrance_hall`
- `side_corridors`
- `interior_layout`
- `main_hall_shell`
- `left_wing_shell`
- `right_wing_shell`
- `rear_keep`
- `grand_roof`
- `roof_accents`

Design notes:
- Bedrock / GeckoLib geometry json format, directly openable in Blockbench.
- Texture canvas is `2048 x 4096` for large palace detailing.
- The palace is built as a monumental sandstone complex with a raised imperial platform, a tall gatehouse, a vast entrance hall, a central audience hall, twin side wings, side corridors, and an elevated rear keep.
- The shell is intentionally hollowed into multiple major interior spaces so it can be explored after export into Minecraft blocks.
- Current structure footprint is roughly `82 x 48 x 115` blocks after conversion.

Suggested build order:
1. `foundation_base`
2. `front_gate` and `front_stairway`
3. `outer_ring_walls` and `corner_watchtowers`
4. `entrance_hall`
5. `side_corridors`
6. `left_wing_shell` and `right_wing_shell`
7. `main_hall_shell`
8. `rear_keep`
9. `interior_layout`
10. `grand_roof` and `roof_accents`
