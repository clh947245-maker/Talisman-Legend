import gzip
import json
import os
import struct
from collections import OrderedDict
from dataclasses import dataclass


TAG_END = 0
TAG_BYTE = 1
TAG_SHORT = 2
TAG_INT = 3
TAG_LONG = 4
TAG_FLOAT = 5
TAG_DOUBLE = 6
TAG_BYTE_ARRAY = 7
TAG_STRING = 8
TAG_LIST = 9
TAG_COMPOUND = 10
TAG_INT_ARRAY = 11
TAG_LONG_ARRAY = 12

DATA_VERSION_1_21_1 = 3955

NON_SOLID_BLOCKS = {
    "minecraft:lantern",
    "minecraft:wall_torch",
}

DIRECTION_VECTORS = {
    "north": (0, -1),
    "south": (0, 1),
    "west": (-1, 0),
    "east": (1, 0),
}

OPPOSITE_DIRECTIONS = {
    "north": "south",
    "south": "north",
    "west": "east",
    "east": "west",
}

LIGHTING_ANCHORS = (
    {"floor": (33, 5, 21), "torch_dirs": ("north", "south")},
    {"floor": (40, 5, 25), "torch_dirs": ("west", "east")},
    {"floor": (50, 5, 25), "torch_dirs": ("north", "south")},
    {"floor": (33, 5, 43), "torch_dirs": ("west", "north")},
    {"floor": (41, 5, 43), "torch_dirs": ("east", "west")},
    {"floor": (35, 5, 80), "torch_dirs": ("west", "south")},
    {"floor": (40, 5, 84), "torch_dirs": ("west", "east", "north")},
    {"floor": (47, 5, 84), "torch_dirs": ("east", "south")},
    {"floor": (36, 11, 100), "torch_dirs": ("west", "south")},
    {"floor": (40, 11, 100), "torch_dirs": ("west", "east")},
    {"floor": (44, 11, 100), "torch_dirs": ("east", "south")},
    {"floor": (34, 16, 60), "torch_dirs": ("west", "north")},
    {"floor": (48, 16, 60), "torch_dirs": ("east", "north")},
    {"floor": (34, 16, 76), "torch_dirs": ("west", "south")},
    {"floor": (48, 16, 76), "torch_dirs": ("east", "south")},
)


@dataclass(frozen=True)
class NbtList:
    child_type: int
    items: list


@dataclass(frozen=True)
class NbtInt:
    value: int


def nbt_int(value: int) -> NbtInt:
    return NbtInt(int(value))


def nbt_int_list(values) -> NbtList:
    return NbtList(TAG_INT, [nbt_int(v) for v in values])


def pack_string(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def infer_tag_type(value):
    if isinstance(value, NbtInt):
        return TAG_INT
    if isinstance(value, NbtList):
        return TAG_LIST
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, int):
        return TAG_INT
    if isinstance(value, float):
        return TAG_DOUBLE
    if isinstance(value, dict):
        return TAG_COMPOUND
    raise TypeError(f"Unsupported NBT value type: {type(value)!r}")


def write_tag_payload(tag_type, value) -> bytes:
    if tag_type == TAG_STRING:
        return pack_string(value)
    if tag_type == TAG_INT:
        actual = value.value if isinstance(value, NbtInt) else int(value)
        return struct.pack(">i", actual)
    if tag_type == TAG_DOUBLE:
        return struct.pack(">d", value)
    if tag_type == TAG_LIST:
        items = value.items if isinstance(value, NbtList) else value
        child_type = value.child_type if isinstance(value, NbtList) else TAG_END
        payload = b"".join(write_tag_payload(child_type, item) for item in items)
        return bytes([child_type]) + struct.pack(">i", len(items)) + payload
    if tag_type == TAG_COMPOUND:
        payload = bytearray()
        for key, child in value.items():
            child_type = infer_tag_type(child)
            payload.append(child_type)
            payload.extend(pack_string(key))
            payload.extend(write_tag_payload(child_type, child))
        payload.append(TAG_END)
        return bytes(payload)

    raise TypeError(f"Unsupported NBT tag id: {tag_type}")


def write_named_tag(name: str, tag_type: int, value) -> bytes:
    return bytes([tag_type]) + pack_string(name) + write_tag_payload(tag_type, value)


def read_named_tag(buffer: bytes, offset: int = 0):
    tag_type = buffer[offset]
    offset += 1
    name_length = struct.unpack_from(">H", buffer, offset)[0]
    offset += 2
    name = buffer[offset:offset + name_length].decode("utf-8")
    offset += name_length
    value, offset = read_payload(buffer, offset, tag_type)
    return tag_type, name, value, offset


def read_payload(buffer: bytes, offset: int, tag_type: int):
    if tag_type == TAG_INT:
        value = struct.unpack_from(">i", buffer, offset)[0]
        return value, offset + 4
    if tag_type == TAG_STRING:
        length = struct.unpack_from(">H", buffer, offset)[0]
        offset += 2
        value = buffer[offset:offset + length].decode("utf-8")
        return value, offset + length
    if tag_type == TAG_LIST:
        child_type = buffer[offset]
        length = struct.unpack_from(">i", buffer, offset + 1)[0]
        offset += 5
        items = []
        for _ in range(length):
            item, offset = read_payload(buffer, offset, child_type)
            items.append(item)
        return {"child_type": child_type, "items": items}, offset
    if tag_type == TAG_COMPOUND:
        value = OrderedDict()
        while True:
            child_type = buffer[offset]
            offset += 1
            if child_type == TAG_END:
                break
            name_length = struct.unpack_from(">H", buffer, offset)[0]
            offset += 2
            key = buffer[offset:offset + name_length].decode("utf-8")
            offset += name_length
            child_value, offset = read_payload(buffer, offset, child_type)
            value[key] = {"type": child_type, "value": child_value}
        return value, offset
    raise ValueError(f"Unsupported parser tag id: {tag_type}")


def verify_structure_nbt(path: str):
    with gzip.open(path, "rb") as handle:
        raw = handle.read()

    root_type, root_name, root_value, end_offset = read_named_tag(raw)
    if root_type != TAG_COMPOUND or root_name != "":
        raise ValueError("Root tag is not an unnamed compound.")

    required = ("DataVersion", "size", "palette", "blocks", "entities")
    for key in required:
        if key not in root_value:
            raise ValueError(f"Missing required root key: {key}")

    size_tag = root_value["size"]
    if size_tag["type"] != TAG_LIST or size_tag["value"]["child_type"] != TAG_INT:
        raise ValueError("size must be a TAG_List of TAG_Int.")
    if len(size_tag["value"]["items"]) != 3:
        raise ValueError("size must contain exactly three integers.")

    blocks_tag = root_value["blocks"]
    if blocks_tag["type"] != TAG_LIST or blocks_tag["value"]["child_type"] != TAG_COMPOUND:
        raise ValueError("blocks must be a TAG_List of TAG_Compound.")

    if blocks_tag["value"]["items"]:
        first_block = blocks_tag["value"]["items"][0]
        pos_tag = first_block["pos"]
        state_tag = first_block["state"]
        if pos_tag["type"] != TAG_LIST or pos_tag["value"]["child_type"] != TAG_INT:
            raise ValueError("blocks[].pos must be a TAG_List of TAG_Int.")
        if state_tag["type"] != TAG_INT:
            raise ValueError("blocks[].state must be a TAG_Int.")

    if end_offset != len(raw):
        raise ValueError("NBT parser did not consume the entire file.")


def palette_entry_key(entry: dict):
    properties = entry.get("Properties", {})
    return (
        entry["Name"],
        tuple(sorted((str(key), str(value)) for key, value in properties.items())),
    )


def ensure_palette_entry(palette: list, state_lookup: dict, entry: dict) -> int:
    normalized = OrderedDict([("Name", entry["Name"])])
    if entry.get("Properties"):
        normalized["Properties"] = OrderedDict(
            (str(key), str(value)) for key, value in sorted(entry["Properties"].items())
        )

    key = palette_entry_key(normalized)
    if key in state_lookup:
        return state_lookup[key]

    palette.append(normalized)
    state_lookup[key] = len(palette) - 1
    return state_lookup[key]


def add_palace_lighting(template: dict):
    palette = template["palette"]
    state_lookup = {
        palette_entry_key(entry): index
        for index, entry in enumerate(palette)
    }
    blocks_by_pos = {
        tuple(block["pos"]): int(block["state"])
        for block in template["blocks"]
    }
    size_x, size_y, size_z = template["size"]

    glowstone_state = ensure_palette_entry(
        palette,
        state_lookup,
        {"Name": "minecraft:glowstone"},
    )
    lantern_state = ensure_palette_entry(
        palette,
        state_lookup,
        {
            "Name": "minecraft:lantern",
            "Properties": {"hanging": "true"},
        },
    )
    wall_torch_states = {
        facing: ensure_palette_entry(
            palette,
            state_lookup,
            {
                "Name": "minecraft:wall_torch",
                "Properties": {"facing": facing},
            },
        )
        for facing in OPPOSITE_DIRECTIONS.values()
    }

    def in_bounds(pos) -> bool:
        x, y, z = pos
        return 0 <= x < size_x and 0 <= y < size_y and 0 <= z < size_z

    def get_state(pos):
        return blocks_by_pos.get(tuple(pos))

    def is_air(pos) -> bool:
        return get_state(pos) is None

    def is_solid(pos) -> bool:
        state = get_state(pos)
        if state is None:
            return False
        return palette[state]["Name"] not in NON_SOLID_BLOCKS

    def set_block(pos, state: int):
        if not in_bounds(pos):
            return False
        blocks_by_pos[tuple(pos)] = state
        return True

    def find_hanging_lantern_positions(floor_pos):
        x, floor_y, z = floor_pos
        for support_y in range(floor_y + 2, size_y):
            support_pos = (x, support_y, z)
            lantern_pos = (x, support_y - 1, z)
            if is_solid(support_pos) and is_air(lantern_pos):
                return support_pos, lantern_pos
        return None, None

    def find_wall_torch_position(center_pos, search_direction: str):
        x, floor_y, z = center_pos
        dx, dz = DIRECTION_VECTORS[search_direction]
        for torch_y in (floor_y + 2, floor_y + 3):
            for step in range(1, 13):
                torch_pos = (x + dx * step, torch_y, z + dz * step)
                support_pos = (torch_pos[0] + dx, torch_y, torch_pos[2] + dz)
                front_pos = (torch_pos[0] - dx, torch_y, torch_pos[2] - dz)
                if not in_bounds(torch_pos) or not in_bounds(support_pos):
                    break
                if (
                    is_air(torch_pos)
                    and is_solid(support_pos)
                    and (not in_bounds(front_pos) or is_air(front_pos))
                ):
                    return torch_pos
        return None

    added_glowstone = 0
    added_lanterns = 0
    added_wall_torches = 0

    for anchor in LIGHTING_ANCHORS:
        floor_pos = anchor["floor"]
        support_pos, lantern_pos = find_hanging_lantern_positions(floor_pos)
        if support_pos is not None and lantern_pos is not None:
            previous_support_state = get_state(support_pos)
            previous_lantern_state = get_state(lantern_pos)
            if set_block(support_pos, glowstone_state) and previous_support_state != glowstone_state:
                added_glowstone += 1
            if previous_lantern_state is None and set_block(lantern_pos, lantern_state):
                added_lanterns += 1

        for search_direction in anchor["torch_dirs"]:
            torch_pos = find_wall_torch_position(floor_pos, search_direction)
            if torch_pos is None:
                continue
            torch_state = wall_torch_states[OPPOSITE_DIRECTIONS[search_direction]]
            previous_torch_state = get_state(torch_pos)
            if previous_torch_state is None and set_block(torch_pos, torch_state):
                added_wall_torches += 1

    template["blocks"] = [
        OrderedDict(
            [
                ("state", state),
                ("pos", [x, y, z]),
            ]
        )
        for (x, y, z), state in sorted(blocks_by_pos.items(), key=lambda item: (item[0][1], item[0][2], item[0][0]))
    ]

    return {
        "glowstone": added_glowstone,
        "lanterns": added_lanterns,
        "wall_torches": added_wall_torches,
    }


def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(os.path.dirname(script_dir))
    json_path = os.path.join(script_dir, "new_palace_structure.json")
    local_nbt_path = os.path.join(script_dir, "new_palace_structure.nbt")
    source_nbt_path = os.path.join(
        project_root,
        "src",
        "main",
        "resources",
        "data",
        "chen_mod",
        "structures",
        "new_palace.nbt",
    )
    shengzhu_source_nbt_path = os.path.join(
        project_root,
        "src",
        "main",
        "resources",
        "data",
        "chen_mod",
        "structures",
        "shengzhu_palace.nbt",
    )
    legacy_source_nbt_path = os.path.join(
        project_root,
        "src",
        "main",
        "resources",
        "data",
        "chen_mod",
        "structure",
        "new_palace.nbt",
    )
    legacy_shengzhu_source_nbt_path = os.path.join(
        project_root,
        "src",
        "main",
        "resources",
        "data",
        "chen_mod",
        "structure",
        "shengzhu_palace.nbt",
    )
    build_nbt_path = os.path.join(
        project_root,
        "build",
        "resources",
        "main",
        "data",
        "chen_mod",
        "structures",
        "new_palace.nbt",
    )
    shengzhu_build_nbt_path = os.path.join(
        project_root,
        "build",
        "resources",
        "main",
        "data",
        "chen_mod",
        "structures",
        "shengzhu_palace.nbt",
    )
    legacy_build_nbt_path = os.path.join(
        project_root,
        "build",
        "resources",
        "main",
        "data",
        "chen_mod",
        "structure",
        "new_palace.nbt",
    )
    legacy_shengzhu_build_nbt_path = os.path.join(
        project_root,
        "build",
        "resources",
        "main",
        "data",
        "chen_mod",
        "structure",
        "shengzhu_palace.nbt",
    )

    with open(json_path, "r", encoding="utf-8") as handle:
        template = json.load(handle)

    lighting_counts = add_palace_lighting(template)

    root = OrderedDict()
    root["DataVersion"] = nbt_int(DATA_VERSION_1_21_1)
    root["size"] = nbt_int_list(template["size"])
    palette_entries = []
    for entry in template["palette"]:
        palette_entry = OrderedDict([("Name", entry["Name"])])
        if "Properties" in entry and entry["Properties"]:
            properties = OrderedDict()
            for key, value in entry["Properties"].items():
                properties[key] = str(value)
            palette_entry["Properties"] = properties
        palette_entries.append(palette_entry)

    root["palette"] = NbtList(
        TAG_COMPOUND,
        palette_entries,
    )
    root["blocks"] = NbtList(
        TAG_COMPOUND,
        [
            OrderedDict(
                [
                    ("state", nbt_int(block["state"])),
                    ("pos", nbt_int_list(block["pos"])),
                ]
            )
            for block in template["blocks"]
        ],
    )
    root["entities"] = NbtList(TAG_COMPOUND, [])

    raw_nbt = write_named_tag("", TAG_COMPOUND, root)

    for path in (
        local_nbt_path,
        source_nbt_path,
        shengzhu_source_nbt_path,
        legacy_source_nbt_path,
        legacy_shengzhu_source_nbt_path,
        build_nbt_path,
        shengzhu_build_nbt_path,
        legacy_build_nbt_path,
        legacy_shengzhu_build_nbt_path,
    ):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with gzip.open(path, "wb") as handle:
            handle.write(raw_nbt)
        verify_structure_nbt(path)

    print(f"Wrote {local_nbt_path}")
    print(f"Wrote {source_nbt_path}")
    print(f"Wrote {shengzhu_source_nbt_path}")
    print(f"Wrote {legacy_source_nbt_path}")
    print(f"Wrote {legacy_shengzhu_source_nbt_path}")
    print(f"Wrote {build_nbt_path}")
    print(f"Wrote {shengzhu_build_nbt_path}")
    print(f"Wrote {legacy_build_nbt_path}")
    print(f"Wrote {legacy_shengzhu_build_nbt_path}")
    print(f"DataVersion: {DATA_VERSION_1_21_1}")
    print(f"Size: {template['size'][0]} x {template['size'][1]} x {template['size'][2]}")
    print(f"Blocks: {len(template['blocks'])}")
    print(
        "Lighting: "
        f"{lighting_counts['glowstone']} glowstone, "
        f"{lighting_counts['lanterns']} lanterns, "
        f"{lighting_counts['wall_torches']} wall torches"
    )


if __name__ == "__main__":
    main()
