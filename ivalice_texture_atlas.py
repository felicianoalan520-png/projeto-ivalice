"""Bake material swatches into GeckoLib box UVs and build catalog sprites."""
from functools import lru_cache
from hashlib import sha256
from math import exp, sin
from pathlib import Path

from PIL import Image, ImageFilter, ImageOps

ROOT = Path(__file__).resolve().parent
ART = ROOT / "art"
SCALE = 4
MATERIALS = (
    "spotted_fur", "wolf_fur", "hare_fur", "fish_scales",
    "reptile", "feathers", "cactus", "bark",
    "leather", "steel", "bronze", "linen",
    "lava", "stone", "crystal", "smoke",
)
FAMILIES = {
    "spotted_fur": "hound cat hyena alpha_hyena coeurl thextera",
    "wolf_fur": "wolf white_wolf alpha_wolf giant yeti beast horse behemoth",
    "hare_fur": "hare rat giza_rabbit",
    "fish_scales": "fish ichthon",
    "reptile": "lizard saurian dragon wyvern snake toad turtle wild_saurian alpha_saurian yiazmat zodiark shinryu gil_turtle adamantoise",
    "feathers": "bird chocobo cockatrice bat",
    "cactus": "mandragora malboro cactoid flowering_cactoid rogue_tomato",
    "bark": "treant",
    "leather": "zombie",
    "steel": "mech golem humanoid iron_giant judge_gabranth gilgamesh omega_mk",
    "linen": "esper ghost mateus rubicante golbez kefka_god mindflayer",
    "lava": "bomb elemental belias firemane ifrit_pyreburner",
    "stone": "undead gargoyle facer skeleton_crusader",
    "crystal": "flan white_mousse atb_weapon ultros",
    "bronze": "mimic",
}
BODY_MATERIAL = {entity: material for material, entities in FAMILIES.items() for entity in entities.split()}


@lru_cache
def swatches():
    with Image.open(ART / "ivalice-materials.png") as image:
        image = image.convert("RGB")
        width, height = image.size
        patches = {}
        for i, material in enumerate(MATERIALS):
            x, y = i % 4, i // 4
            tile = image.crop(((x * width) // 4 + 6, (y * height) // 4 + 6,
                               ((x + 1) * width) // 4 - 6, ((y + 1) * height) // 4 - 6))
            # Derive surface contrast from the swatches and let each species supply color.
            relief = ImageOps.autocontrast(tile.convert("L"), cutoff=3).resize((160, 160), Image.Resampling.LANCZOS)
            patches[material] = relief.load()
        return patches


def part_material(body, bone):
    if any(part in bone for part in ("armor", "plate", "chassis", "shoulder")):
        return "steel"
    if any(part in bone for part in ("cape", "robe")):
        return "linen"
    if any(part in bone for part in ("horn", "talon", "fang", "jaw")):
        return "stone"
    if "wing" in bone and body not in ("feathers", "reptile"):
        return "linen"
    if "mane" in bone:
        return "wolf_fur"
    if "halo" in bone:
        return "crystal"
    return body


def mix(base, detail, weight):
    return tuple(round(a + (b - a) * weight) for a, b in zip(base, detail))


def elliptical(x, y, cx, cy, rx, ry):
    return exp(-((x - cx) / rx) ** 2 - ((y - cy) / ry) ** 2)


def anatomy(color, body, bone, cube, face, x, y, coarse_x, coarse_y):
    side = face in ("left", "right")
    if body in ("spotted_fur", "wolf_fur", "hare_fur"):
        if bone == "body" and side:
            belly = (199, 173, 130) if body != "hare_fur" else (236, 224, 199)
            return mix(color, belly, max(0, (y - .59) * .84))
        if bone.startswith("ear_") and face in ("front", "left", "right"):
            tint = (140, 88, 75) if body != "hare_fur" else (179, 117, 108)
            return mix(color, tint, .79 * elliptical(x, y, .49, .54, .39, .49))
        if bone.startswith("leg_") and y > .77 and face != "top":
            toe = .35 + .55 * abs(sin(x * 14))
            return mix(color, (40, 31, 27), toe * (y - .77) * 3)
        if bone == "head" and cube == 0 and side:
            # Project the irises toward the muzzle on both side faces.
            eye_x = .70 if face == "right" else .30
            socket = elliptical(x, y, eye_x, .47, .27, .20)
            iris = elliptical(x, y, eye_x, .45, .115, .10)
            pupil = elliptical(x, y, eye_x, .45, .045, .085)
            color = mix(color, (43, 29, 25), socket * .80)
            color = mix(color, (219, 165, 66) if body != "hare_fur" else (115, 61, 51), iris * .95)
            return mix(color, (26, 20, 16), pupil)
        if bone == "head" and cube == 1 and face == "front":
            nose = elliptical(x, y, .51, .41, .48, .38)
            return mix(color, (39, 28, 26) if body != "hare_fur" else (126, 69, 64), nose)
    if body == "fish_scales":
        if bone == "body" and side:
            color = mix(color, (38, 72, 106), max(0, (.55 - y) * .83))
            color = mix(color, (209, 194, 132), max(0, (y - .56) * 1.2))
            return mix(color, (153, 191, 176), .28 * elliptical(x, y, .44, .48, .68, .08))
        if bone == "head" and cube == 0 and side:
            eye_x = .70 if face == "right" else .30
            color = mix(color, (15, 31, 38), .95 * elliptical(x, y, eye_x, .40, .27, .25))
            color = mix(color, (230, 181, 70), elliptical(x, y, eye_x, .40, .13, .13))
            color = mix(color, (10, 17, 21), elliptical(x, y, eye_x, .40, .065, .075))
            return mix(color, (219, 237, 222), .85 * elliptical(x, y, eye_x - .028, .36, .04, .04))
        if "fin" in bone or bone in ("crest", "tail_2"):
            ray = max(0, sin(coarse_y * 5.7 + coarse_x * 2.6)) ** 5
            return mix(color, (243, 167, 91), .42 * ray)
    if body == "cactus" and face == "front" and bone == "body":
        eye = max(elliptical(x, y, .34, .36, .10, .12), elliptical(x, y, .66, .36, .10, .12))
        mouth = elliptical(x, y, .5, .59, .14, .077)
        return mix(color, (23, 35, 20), .94 * max(eye, mouth))
    if body == "feathers" and "wing" in bone and face in ("top", "bottom"):
        vane = max(0, sin(15 * x + 10 * y + 2 * sin(3 * y))) ** 6
        return mix(color, (51, 36, 27), .38 * vane)
    return color


def write_entity_texture(path, palette, uvs, bones, family):
    """Keep the model's 64-unit UV coordinates and rasterize at four pixels per unit."""
    entity = path.stem
    digest = sha256(entity.encode()).digest()
    body = BODY_MATERIAL.get(family, BODY_MATERIAL.get(entity, "leather"))
    relief = swatches()
    result = Image.new("RGBA", (64 * SCALE, 64 * SCALE))
    for bone in bones:
        name = bone["name"]
        for index, cube in enumerate(bone.get("cubes", [])):
            u, v = uvs[f"{name}_{index}"]
            dx, dy, dz = cube["size"]
            surface = part_material(body, name)
            swatch = relief[surface]
            faces = (
                ("top", u + dz, v, dx, dz), ("bottom", u + dz + dx, v, dx, dz),
                ("right", u, v + dz, dz, dy), ("front", u + dz, v + dz, dx, dy),
                ("left", u + dz + dx, v + dz, dz, dy), ("back", u + 2 * dz + dx, v + dz, dx, dy),
            )
            for face, fx, fy, fw, fh in faces:
                patch = Image.new("RGBA", (fw, fh))
                patch.putdata([palette[y][x] for y in range(fy, fy + fh) for x in range(fx, fx + fw)])
                # Smooth the base colors within each face; never interpolate between UV islands.
                if body in ("spotted_fur", "wolf_fur", "hare_fur", "fish_scales", "cactus"):
                    patch = patch.filter(ImageFilter.GaussianBlur(.62))
                width, height = fw * SCALE, fh * SCALE
                patch = patch.resize((width, height), Image.Resampling.BICUBIC)
                pixels = patch.load()
                # Wrap the sample through adjacent faces without an artificial black outline.
                around = {"right": 0, "front": dz, "left": dz + dx, "back": 2 * dz + dx}.get(face, 0)
                swatch_x = digest[2] + u + around * SCALE
                swatch_y = digest[3] + v
                if body == "spotted_fur" and name == "body":
                    swatch_x = 28 + around * SCALE + digest[2] % 13
                    swatch_y = 35 + digest[3] % 24
                strength = .52 if surface in ("spotted_fur", "reptile", "bark", "lava") else .39
                for y in range(height):
                    for x in range(width):
                        material_x = (swatch_x + x) % 160
                        material_y = (swatch_y + y) % 160
                        sample = swatch[material_x, material_y] / 127.5 - 1
                        r, g, b, a = pixels[x, y]
                        color = tuple(c * (1 + strength * sample) + 15 * sample for c in (r, g, b))
                        color = anatomy(color, body, name, index, face, (x + .5) / width,
                                        (y + .5) / height, x / SCALE, y / SCALE)
                        pixels[x, y] = (*[max(0, min(255, round(c))) for c in color], a)
                result.paste(patch, (fx * SCALE, fy * SCALE))
    path.parent.mkdir(parents=True, exist_ok=True)
    result.save(path)


ICON_ORDER = (
    "blade", "bow", "staff", "axe", "gun", "helm", "armor", "shield",
    "ring", "arrow", "bottle", "crystal", "key", "scroll", "book", "orb",
)


@lru_cache
def icon_tiles(filename, order):
    with Image.open(ART / filename) as image:
        image = image.convert("RGBA")
        width, height = image.size
        result = {}
        for i, kind in enumerate(order):
            x, y = i % 4, i // 4
            tile = image.crop((x * width // 4, y * height // 4, (x + 1) * width // 4, (y + 1) * height // 4))
            alpha = tile.getchannel("A").point(lambda a: 0 if a < 92 else min(255, (a - 92) * 3))
            tile.putalpha(alpha)
            tile = tile.crop(alpha.getbbox())
            result[kind] = tile
        return result


RELIC_ORDER = (
    "tournesol", "masamune", "zodiac_spear", "excalibur",
    "wyrmhero_blade", "tza_dhanusha", "zodiac_escutcheon", "grand_helm",
    "tza_black_robes", "tza_white_robes", "maximillian", "tza_indigo_pendant",
    "tza_ruby_ring", "tza_dark_matter", "tza_megalixir", "teleport_stone",
)


FIELD_ORDER = (
    "crossbow", "wand", "dagger", "mace",
    "pelt", "fang", "bone", "horn",
    "shell", "feather", "ore", "herb",
    "coin", "silk", "glove", "boots",
)


def sprite(tile):
    tile = tile.copy()
    tile.thumbnail((56, 56), Image.Resampling.LANCZOS)
    result = Image.new("RGBA", (64, 64))
    result.paste(tile, ((64 - tile.width) // 2, (64 - tile.height) // 2))
    return result


def color_shift(tile, target_hue, source):
    hsv = tile.convert("RGB").convert("HSV")
    pixels = hsv.load()
    for y in range(tile.height):
        for x in range(tile.width):
            hue, saturation, value = pixels[x, y]
            if hue in source and saturation > 60:
                pixels[x, y] = (target_hue, saturation, value)
    colored = hsv.convert("RGB")
    colored.putalpha(tile.getchannel("A"))
    return colored


def inventory_icon(kind, hue=None):
    tiles = icon_tiles("ivalice-field.png", FIELD_ORDER) if kind in FIELD_ORDER else icon_tiles("ivalice-inventory.png", ICON_ORDER)
    tile = tiles[kind]
    if hue is not None:
        tile = color_shift(tile, hue, range(73, 157))
    return sprite(tile)


def relic_icon(relic):
    tile = icon_tiles("ivalice-relics.png", RELIC_ORDER)[relic]
    if relic == "tza_indigo_pendant":
        tile = color_shift(tile, 165, (*range(225, 256), *range(9)))
    return sprite(tile)
