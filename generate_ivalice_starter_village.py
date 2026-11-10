"""Generate the Dalmasca refuge and the assets for its town watch."""

import gzip
from hashlib import sha256

from PIL import Image

from generate_geckolib_entities import build_animation_json, build_geometry_json, pack_model_cubes
from generate_ivalice_settlements import ASSETS, DATA, nbt_payload, save_json
from generate_tza_bestiary import style_model


def write_village():
    size = 33
    blocks = {}
    air = "minecraft:air"
    brick = "ivalice:dalmasca_carved_bricks"
    stone = "ivalice:dalmasca_sandstone"
    mosaic = "ivalice:dalmasca_mosaic"
    adobe = "ivalice:giza_painted_adobe"
    lamp = "ivalice:amber_lantern_tile"

    def put(x, y, z, block, **properties):
        blocks[x, y, z] = block, tuple(sorted(properties.items()))

    def fill(x1, y1, z1, x2, y2, z2, block):
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                for x in range(x1, x2 + 1):
                    put(x, y, z, block)

    fill(0, 0, 0, 32, 0, 32, stone)
    fill(0, 1, 0, 32, 8, 32, air)
    fill(14, 0, 0, 18, 0, 32, brick)
    fill(1, 0, 14, 31, 0, 18, brick)
    fill(12, 0, 12, 20, 0, 20, mosaic)
    fill(15, 0, 15, 17, 0, 17, brick)
    for x in (12, 20):
        for z in (12, 20):
            put(x, 0, z, lamp)

    for i in range(size):
        for x, z in ((0, i), (32, i), (i, 0), (i, 32)):
            if z in (0, 32) and 15 <= x <= 17:
                continue
            fill(x, 1, z, x, 2, z, brick)
            if i % 3 == 0:
                put(x, 3, z, brick)

    for gate in (0, 32):
        for x in (14, 18):
            fill(x, 1, gate, x, 5, gate, brick)
            put(x, 4, gate, lamp)
            put(x, 6, gate, adobe)
        fill(15, 4, gate, 17, 4, gate, brick)
        fill(15, 5, gate, 17, 5, gate, adobe)
        put(16, 5, gate, mosaic)
        put(16, 6, gate, lamp)

    def house(x, z, north, workstation):
        facing = "north" if north else "south"
        front = z if north else z + 6
        back = z + 5 if north else z + 1
        fill(x, 0, z, x + 7, 0, z + 6, "ivalice:balfonheim_dockwood")
        fill(x, 1, z, x + 7, 4, z + 6, brick)
        fill(x + 1, 1, z + 1, x + 6, 4, z + 5, air)
        fill(x - 1, 5, z - 1, x + 8, 5, z + 7, adobe)
        fill(x + 1, 6, z + 1, x + 6, 6, z + 5, "minecraft:smooth_sandstone")
        put(x + 3, 4, front, lamp)
        put(x + 2, 1, z + 3, workstation)
        for xx in (x + 1, x + 5):
            put(xx, 2, front, "minecraft:cyan_stained_glass")
        for y, half in ((1, "lower"), (2, "upper")):
            put(x + 3, y, front, "minecraft:spruce_door", half=half, facing=facing)
        put(x + 5, 1, back, "minecraft:cyan_bed", part="head", facing=facing if not north else "north")
        put(x + 5, 1, back + (1 if north else -1), "minecraft:cyan_bed", part="foot", facing=facing)

    house(3, 5, False, "minecraft:brewing_stand")
    house(22, 5, False, "minecraft:grindstone")
    house(3, 21, True, "minecraft:barrel")

    fill(22, 0, 22, 29, 0, 28, brick)
    fill(23, 0, 23, 28, 0, 27, "minecraft:farmland")
    for z in range(23, 28):
        put(25, 0, z, "minecraft:water")
        for x in (23, 24, 26, 27, 28):
            put(x, 0, z, "minecraft:farmland", moisture="7")
            crop = "wheat" if x < 25 else "carrots"
            put(x, 1, z, f"minecraft:{crop}", age=str(4 + (x + z) % 4))
    put(27, 1, 21, "minecraft:composter")
    for x in (21, 30):
        for z in (21, 29):
            put(x, 1, z, brick)
            put(x, 2, z, lamp)

    for x, z in ((4, 13), (10, 13), (22, 13), (28, 13), (4, 19), (10, 19), (22, 19), (28, 19)):
        put(x, 0, z, "minecraft:coarse_dirt")
        put(x, 1, z, "ivalice:dalmasca_emberbloom")

    palette = list(dict.fromkeys(blocks.values()))
    indices = {block: index for index, block in enumerate(palette)}
    states = []
    for block, properties in palette:
        state = {"id": (8, block)}
        if properties:
            state["properties"] = (10, {key: (8, value) for key, value in properties})
        states.append(state)
    entities = []
    for x, z, profession in ((7, 9, "cleric"), (26, 9, "weaponsmith"), (23, 21, "farmer")):
        entities.append({"pos": (9, (6, [x + .5, 1, z + .5])), "blockPos": (9, (3, [x, 1, z])),
                         "nbt": (10, {"id": (8, "minecraft:villager"),
                                      "VillagerData": (10, {"type": (8, "ivalice:dalmasca"),
                                                            "profession": (8, f"minecraft:{profession}"), "level": (3, 1)}),
                                      "VillagerDataFinalized": (1, 1)})})
    template = {
        "DataVersion": (3, 5023),
        "size": (9, (3, [size, 9, size])),
        "palette": (9, (10, states)),
        "blocks": (9, (10, [{"pos": (9, (3, list(pos))), "state": (3, indices[block])}
                            for pos, block in sorted(blocks.items(), key=lambda row: (row[0][1], row[0][2], row[0][0]))])),
        "entities": (9, (10, entities)),
    }
    (DATA / "structure/starter_village.nbt").write_bytes(gzip.compress(b"\x0a\x00\x00" + nbt_payload(10, template), mtime=0))


def write_guard():
    guard = "dalmasca_guard"
    shapes = [
        ("torso", [0, 11, 0], [-4, 10, -2], [8, 10, 4]),
        ("head", [0, 20, 0], [-3, 20, -3], [6, 7, 6], "torso"),
        ("helmet", [0, 25, 0], [-4, 25, -3], [8, 4, 7], "head"),
        ("crest", [0, 27, 0], [-1, 29, -1], [2, 2, 5], "helmet"),
        ("arm_right", [-5, 19, 0], [-7, 11, -2], [3, 9, 4], "torso"),
        ("arm_left", [5, 19, 0], [4, 11, -2], [3, 9, 4], "torso"),
        ("pauldron_right", [-5, 19, 0], [-8, 17, -3], [4, 4, 6], "arm_right"),
        ("pauldron_left", [5, 19, 0], [4, 17, -3], [4, 4, 6], "arm_left"),
        ("leg_right", [-2, 11, 0], [-4, 0, -2], [4, 11, 4]),
        ("leg_left", [2, 11, 0], [0, 0, -2], [4, 11, 4]),
        ("cape", [0, 19, 2], [-4, 8, 2], [8, 11, 1], "torso"),
        ("blade", [-6, 12, 0], [-7, 11, -10], [2, 1, 9], "arm_right"),
        ("hilt", [-6, 12, 0], [-8, 11, -2], [4, 1, 4], "arm_right"),
    ]
    model = style_model("judge_gabranth", shapes, [
        ("leg_right", [30, 0, 0]), ("leg_left", [-30, 0, 0]),
        ("arm_right", [-20, 0, 0]), ("arm_left", [24, 0, 0]), ("cape", [8, 0, 2]),
    ])
    strike = model["animations"]["strike"]
    strike["animation_length"] = .52
    strike["bones"] = {
        "torso": {"rotation": {"0": [0, 0, 0], ".13": [-10, -24, 0], ".29": [12, 24, 0], ".52": [0, 0, 0]}},
        "arm_right": {"rotation": {"0": [0, 0, 0], ".13": [-95, -12, -19],
                                   ".29": [-26, 11, -8], ".52": [0, 0, 0]}},
        "leg_left": {"rotation": {"0": [0, 0, 0], ".29": [-19, 0, 0], ".52": [0, 0, 0]}},
    }
    idle = model["animations"]["idle"]
    idle["bones"]["head"] = {"rotation": {"0": [0, -5, 0], str(idle["animation_length"] / 2): [3, 6, 0],
                                         str(idle["animation_length"]): [0, -5, 0]}}
    uv = pack_model_cubes(model["bones"])
    save_json(ASSETS / f"geckolib/models/entity/{guard}.geo.json", build_geometry_json(guard, model, uv))
    save_json(ASSETS / f"geckolib/animations/entity/{guard}.animation.json", build_animation_json(model))

    cloth, steel, leather, gold, skin = (30, 70, 87), (111, 127, 130), (76, 47, 34), (177, 129, 61), (173, 115, 85)
    image = Image.new("RGBA", (256, 256))
    px = image.load()
    seed = sha256(guard.encode()).digest()
    for bone in model["bones"][1:]:
        name = bone["name"]
        u, v = uv[f"{name}_0"]
        dx, dy, dz = bone["cubes"][0]["size"]
        faces = [("top", u + dz, v, dx, dz, 20), ("bottom", u + dz + dx, v, dx, dz, -26),
                 ("right", u, v + dz, dz, dy, -11), ("front", u + dz, v + dz, dx, dy, 5),
                 ("left", u + dz + dx, v + dz, dz, dy, -7), ("back", u + 2 * dz + dx, v + dz, dx, dy, -17)]
        for face, x0, y0, width, height, shade in faces:
            for py in range(4 * height):
                for pxl in range(4 * width):
                    x, y = pxl / (4 * width), py / (4 * height)
                    material = cloth
                    if name.startswith(("helmet", "pauldron")) or name == "blade":
                        material = steel
                        if y < .09 or y > .91 or (name == "helmet" and face == "front" and .44 < x < .56):
                            material = gold
                    if name == "hilt":
                        material = gold if x < .29 or x > .71 else leather
                    if name == "crest":
                        material = (138, 57, 37)
                    if name == "cape":
                        material = (42, 88, 100) if .08 < x < .92 else gold
                    if name == "torso" and face not in ("top", "bottom"):
                        if .82 < y < .95:
                            material = gold if .42 < x < .56 else leather
                        elif (int(y * 12) + int(x * 9)) % 3 != 0:
                            material = steel
                    if name.startswith("arm_") and y > .68:
                        material = skin if y > .85 else leather
                    if name.startswith("leg_") and y > .6:
                        material = steel if y < .76 else leather
                    if name == "head":
                        material = skin if face != "back" else leather
                        if face == "front":
                            if .37 < y < .47 and (.15 < x < .34 or .66 < x < .85):
                                material = (37, 35, 30)
                            elif .46 < y < .53 and (.21 < x < .31 or .69 < x < .79):
                                material = (72, 92, 77)
                            elif .48 < x < .60 and .51 < y < .68:
                                material = (191, 131, 96)
                            elif .35 < x < .66 and .72 < y < .78:
                                material = (101, 56, 49)
                            elif y > .82 or x < .1 or x > .91:
                                material = (108, 65, 43)
                    noise = seed[(pxl * 19 + py * 11 + x0 + y0) % 32] % 12 - 6
                    highlight = 13 if x < .06 or y < .07 else -9 if x > .93 or y > .94 else 0
                    delta = shade + noise + highlight
                    px[x0 * 4 + pxl, y0 * 4 + py] = (*[min(255, max(0, channel + delta)) for channel in material], 255)
    image.save(ASSETS / f"textures/entity/{guard}.png")


if __name__ == "__main__":
    write_village()
    write_guard()
