"""Build the regional blocks, plants, clothes and hamlet templates for Minecraft 26.3."""
import gzip
import json
from pathlib import Path
import struct
import zlib

RESOURCES = Path(__file__).resolve().parent / "src/main/resources"
ASSETS = RESOURCES / "assets/ivalice"
DATA = RESOURCES / "data/ivalice"

PLANTS = {
    "dalmasca_emberbloom": (["dalmasca_estersand", "dalmasca_westersand"], "Flor-brasa de Dalmasca", "Dalmasca Emberbloom", (201, 64, 38), (254, 162, 64)),
    "giza_sunbloom": (["giza_plains_dry", "ozmone_plain"], "Flor-do-sol de Giza", "Giza Sunbloom", (228, 146, 47), (255, 225, 93)),
    "golmore_orchid": (["golmore_jungle", "salikawood"], "Orquídea de Golmore", "Golmore Orchid", (141, 49, 122), (242, 145, 205)),
    "phon_sea_lily": (["phon_coast"], "Lírio costeiro de Phon", "Phon Sea Lily", (83, 167, 178), (224, 247, 233)),
}
MASONRY = {
    "dalmasca_carved_bricks": ("Tijolos lavrados de Dalmasca", "Dalmasca Carved Bricks", (206, 169, 112), (152, 112, 77), "ivalice:dalmasca_sandstone"),
    "giza_painted_adobe": ("Adobe pintado de Giza", "Giza Painted Adobe", (164, 99, 62), (103, 67, 51), "minecraft:terracotta"),
}


def save_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def save_png(path, pixels):
    height, width = len(pixels), len(pixels[0])

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))

    rows = b"".join(b"\0" + bytes(channel for pixel in row for channel in pixel) for row in pixels)
    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b""))


def blank(size):
    return [[(0, 0, 0, 0)] * size for _ in range(size)]


def shade(rgb, delta):
    return tuple(max(0, min(255, c + delta)) for c in rgb) + (255,)


def masonry_pixels(base, mortar, painted):
    pixels = blank(16)
    for y in range(16):
        for x in range(16):
            row, depth = divmod(y, 4)
            offset = (x + row % 2 * 4) % 8
            if depth == 3 or offset == 0:
                pixels[y][x] = shade(mortar, (x * 7 + y * 11) % 7)
            else:
                grain = (x * 17 + y * 29 + x * y * 11) % 21 - 10
                pixels[y][x] = shade(base, grain + (12 if depth == 0 else 0))
                if (x + y * 2) % 8 == 3 and depth == 1:
                    pixels[y][x] = shade((68, 119, 109) if painted else (130, 106, 76), grain)
    return pixels


def plant_pixels(index, petals, bright):
    pixels = blank(16)
    for stem in range(3):
        start = 7 + (stem - 1)
        top = (5, 2, 6)[stem] + index % 2
        tip = (3, 8, 12)[stem]
        for y in range(top, 16):
            x = round(tip + (start - tip) * (y - top) / (15 - top))
            pixels[y][x] = shade((70, 105, 52), y % 3 * 8)
            if y in (9, 12):
                side = -1 if (stem + y) % 2 == 0 else 1
                for length in range(1, 4):
                    pixels[y - length // 2][x + side * length] = shade((82, 121, 56), 13 - 5 * length)
        for dx, dy in [(0, 0), (-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, -1)]:
            pixels[top + dy][tip + dx] = shade(bright if dx == dy == 0 else petals, 8 if dy < 0 else 0)
    return pixels


def block_assets(name, plant):
    model = f"ivalice:block/{name}"
    texture = {"cross": model} if plant else {"all": model}
    save_json(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": model}}})
    save_json(ASSETS / f"models/block/{name}.json",
              {"parent": "minecraft:block/cross" if plant else "minecraft:block/cube_all", "textures": texture})
    item_model = {"parent": "minecraft:item/generated", "textures": {"layer0": model}} if plant else {"parent": model}
    save_json(ASSETS / f"models/item/{name}.json", item_model)
    save_json(ASSETS / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"ivalice:item/{name}"}})
    save_json(DATA / f"loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"ivalice:{name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    })


def flora_and_blocks():
    labels = [{}, {}]
    for index, (name, (biomes, pt, en, petals, bright)) in enumerate(PLANTS.items()):
        block_assets(name, True)
        save_png(ASSETS / f"textures/block/{name}.png", plant_pixels(index, petals, bright))
        labels[0][f"block.ivalice.{name}"], labels[1][f"block.ivalice.{name}"] = pt, en
        reference = f"ivalice:patch_{name}"
        state = {"id": f"ivalice:{name}"}
        save_json(DATA / f"worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": state})
        save_json(DATA / f"worldgen/placed_feature/patch_{name}.json", {
            "feature": f"ivalice:{name}",
            "placement": [
                {"type": "minecraft:count", "count": 5},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
                {"type": "minecraft:biome"},
                {"type": "minecraft:block_predicate_filter", "predicate": {
                    "type": "minecraft:all_of",
                    "predicates": [{"type": "minecraft:matching_block_tag", "tag": "minecraft:air"},
                                   {"type": "minecraft:would_survive", "state": state}]
                }}
            ],
        })
        for biome in biomes:
            path = DATA / f"worldgen/biome/{biome}.json"
            data = json.loads(path.read_text(encoding="utf-8"))
            if reference not in data["features"][9]:
                data["features"][9].append(reference)
                save_json(path, data)

    for name, (pt, en, base, mortar, material) in MASONRY.items():
        block_assets(name, False)
        labels[0][f"block.ivalice.{name}"], labels[1][f"block.ivalice.{name}"] = pt, en
        save_png(ASSETS / f"textures/block/{name}.png", masonry_pixels(base, mortar, name.startswith("giza")))
        save_json(DATA / f"recipe/{name}.json", {
            "type": "minecraft:crafting_shaped", "pattern": ["XX", "XX"], "key": {"X": material},
            "result": {"id": f"ivalice:{name}", "count": 4},
        })

    seal = blank(16)
    for y in range(2, 14):
        for x in range(2, 14):
            distance = abs(x - 7.5) + abs(y - 7.5)
            if distance <= 8:
                seal[y][x] = shade((209, 161, 79) if distance >= 6 or (x + y) % 6 == 0 else (64, 100, 125), 12 - y)
    save_png(ASSETS / "textures/item/zodiac_reset_seal.png", seal)
    save_json(ASSETS / "items/zodiac_reset_seal.json", {"model": {"type": "minecraft:model", "model": "ivalice:item/zodiac_reset_seal"}})
    save_json(ASSETS / "models/item/zodiac_reset_seal.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": "ivalice:item/zodiac_reset_seal"},
    })
    labels[0]["item.ivalice.zodiac_reset_seal"], labels[1]["item.ivalice.zodiac_reset_seal"] = "Selo Zodiacal", "Zodiac Seal"

    for lang, updates in zip(("pt_br", "en_us"), labels):
        path = ASSETS / f"lang/{lang}.json"
        existing = json.loads(path.read_text(encoding="utf-8"))
        existing.update(updates)
        save_json(path, existing)

    path = RESOURCES / "data/minecraft/tags/block/mineable/pickaxe.json"
    tag = json.loads(path.read_text(encoding="utf-8"))
    for name in MASONRY:
        value = f"ivalice:{name}"
        if value not in tag["values"]:
            tag["values"].append(value)
    save_json(path, tag)


def villager_clothes(region, cloth, sash, trim):
    pixels = blank(64)
    boxes = [(16, 20, 8, 12, 6), (0, 38, 8, 20, 6), (44, 22, 4, 8, 4),
             (40, 38, 8, 4, 4), (32, 0, 8, 10, 8), (0, 22, 4, 12, 4)]
    for u, v, width, height, depth in boxes:
        for y in range(v, v + height + depth):
            for x in range(u, u + 2 * (width + depth)):
                if y < v + depth and not u + depth <= x < u + depth + 2 * width:
                    continue
                row = y - v - depth
                if v == 0 and row > 3 and x < u + width + depth:
                    continue
                color = sash if row in (height - 4, height - 3) else cloth
                if row == height - 1 or (row == 2 and x % 4 == 0):
                    color = trim
                pixels[y][x] = shade(color, (x * 13 + y * 17) % 19 - 9)
    for age in ("type", "baby"):
        save_png(ASSETS / f"textures/entity/villager/{age}/{region}.png", pixels)


# NBT field types needed by structure templates.
def nbt_payload(kind, value):
    if kind in (1, 3, 6):
        return struct.pack({1: ">b", 3: ">i", 6: ">d"}[kind], value)
    if kind == 8:
        value = value.encode("utf-8")
        return struct.pack(">H", len(value)) + value
    if kind == 9:
        element, items = value
        return struct.pack(">Bi", element, len(items)) + b"".join(nbt_payload(element, item) for item in items)
    if kind == 10:
        return b"".join(bytes([tag]) + nbt_payload(8, key) + nbt_payload(tag, item)
                        for key, (tag, item) in value.items()) + b"\0"
    raise ValueError(f"Unsupported NBT type: {kind}")


def hamlet(name, villager_type, biomes, brick, roof, plant, salt):
    blocks = {}

    def set_block(x, y, z, block, **properties):
        blocks[x, y, z] = (block, tuple(sorted(properties.items())))

    for x in range(26):
        for z in range(22):
            for y in (-2, -1):
                set_block(x, y, z, brick)
            set_block(x, 0, z, "minecraft:smooth_sandstone" if x in range(10, 16) or z in range(8, 14) else brick)

    def house(left, back, facing):
        width, depth = 7, 6
        for x in range(width):
            for z in range(depth):
                for y in range(1, 5):
                    if y == 4:
                        set_block(left + x, y, back + z, roof)
                    elif (x in (0, width - 1) or z in (0, depth - 1)) and not (
                            x == width // 2 and z == (depth - 1 if facing == "south" else 0) and y <= 2):
                        wall = "minecraft:glass_pane" if y == 2 and x in (1, width - 2) and z in (0, depth - 1) else brick
                        set_block(left + x, y, back + z, wall)
                    else:
                        set_block(left + x, y, back + z, "minecraft:air")
        for x in range(width):
            set_block(left + x, 5, back, brick)
            set_block(left + x, 5, back + depth - 1, brick)
        set_block(left + 1, 1, back + 2, "minecraft:crafting_table")
        set_block(left + 5, 1, back + 3, "minecraft:yellow_bed", facing="north", part="foot", occupied="false")
        set_block(left + 5, 1, back + 2, "minecraft:yellow_bed", facing="north", part="head", occupied="false")

    for x, z, facing in [(2, 2, "south"), (17, 2, "south"), (2, 14, "north"), (17, 14, "north")]:
        house(x, z, facing)

    for x in (10, 15):
        for z in (8, 13):
            set_block(x, 1, z, brick)
            set_block(x, 2, z, "minecraft:lantern")
    for x, z in [(9, 7), (16, 7), (9, 14), (16, 14)]:
        set_block(x, 0, z, "minecraft:rooted_dirt")
        set_block(x, 1, z, plant)
    set_block(12, 1, 10, "minecraft:composter")
    set_block(13, 1, 11, "minecraft:stonecutter", facing="north")
    set_block(13, 2, 12, "minecraft:bell", attachment="floor", facing="north", powered="false")
    set_block(13, 1, 12, brick)

    palette = list(dict.fromkeys(blocks.values()))
    indices = {block: index for index, block in enumerate(palette)}
    states = []
    for block, properties in palette:
        state = {"id": (8, block)}
        if properties:
            state["properties"] = (10, {key: (8, value) for key, value in properties})
        states.append(state)
    block_tags = [{"pos": (9, (3, [x, y + 2, z])), "state": (3, indices[block])}
                  for (x, y, z), block in blocks.items()]
    entities = []
    for x, z, profession in [(7, 9, "none"), (18, 10, "none"), (12, 9, "farmer"), (14, 13, "mason")]:
        entities.append({
            "pos": (9, (6, [x + 0.5, 3.0, z + 0.5])),
            "blockPos": (9, (3, [x, 3, z])),
            "nbt": (10, {
                "id": (8, "minecraft:villager"),
                "VillagerData": (10, {"type": (8, f"ivalice:{villager_type}"),
                                      "profession": (8, f"minecraft:{profession}"), "level": (3, 1)}),
                "VillagerDataFinalized": (1, 1),
            }),
        })
    template = {"DataVersion": (3, 5023), "size": (9, (3, [26, 8, 22])),
                "palette": (9, (10, states)), "blocks": (9, (10, block_tags)), "entities": (9, (10, entities))}
    path = DATA / f"structure/{name}.nbt"
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(gzip.compress(b"\x0a\0\0" + nbt_payload(10, template), mtime=0))
    save_json(DATA / f"worldgen/template_pool/{name}.json", {"fallback": "minecraft:empty", "elements": [{
        "weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": f"ivalice:{name}",
                                 "processors": {"processors": []}, "projection": "rigid"},
    }]})
    save_json(DATA / f"worldgen/structure/{name}.json", {
        "type": "minecraft:jigsaw", "biomes": [f"ivalice:{biome}" for biome in biomes],
        "start_pool": f"ivalice:{name}", "start_height": {"absolute": -2}, "size": 1,
        "project_start_to_heightmap": "WORLD_SURFACE_WG", "terrain_adaptation": "beard_thin",
        "step": "surface_structures", "spawn_overrides": {}, "max_distance_from_center": 48, "use_expansion_hack": False,
    })
    save_json(DATA / f"worldgen/structure_set/{name}.json", {
        "placement": {"type": "minecraft:random_spread", "spacing": 30, "separation": 10, "salt": salt},
        "structures": [{"structure": f"ivalice:{name}", "weight": 1}],
    })
    return {f"ivalice:{biome}": {"villager_type": f"ivalice:{villager_type}"} for biome in biomes}


def main():
    flora_and_blocks()
    villagers = hamlet("estersand_outpost", "dalmasca", ["dalmasca_estersand", "dalmasca_westersand"],
                       "ivalice:dalmasca_carved_bricks", "minecraft:cyan_terracotta", "ivalice:dalmasca_emberbloom", 30926011)
    villagers.update(hamlet("giza_hamlet", "giza", ["giza_plains_dry", "ozmone_plain"],
                           "ivalice:giza_painted_adobe", "minecraft:orange_terracotta", "ivalice:giza_sunbloom", 30926029))
    save_json(RESOURCES / "data/neoforge/data_maps/worldgen/biome/villager_types.json", {"values": villagers})
    villager_clothes("dalmasca", (200, 174, 133), (42, 99, 112), (224, 183, 102))
    villager_clothes("giza", (119, 134, 87), (175, 95, 64), (233, 192, 112))


if __name__ == "__main__":
    main()
