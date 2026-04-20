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


if __name__ == "__main__":
    main()
