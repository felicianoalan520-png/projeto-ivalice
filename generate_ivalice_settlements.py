"""Build the regional blocks, plants, clothes and hamlet templates for Minecraft 26.3."""
import gzip
import json
import math
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
    "yensa_salt_reed": (["ogir_yensa_sandsea", "dalmasca_westersand"], "Junco de sal de Yensa", "Yensa Salt Reed", (164, 101, 51), (237, 192, 116)),
    "giza_rain_reed": (["giza_plains_rains", "giza_plains_dry", "ozmone_plain"], "Junco das chuvas de Giza", "Giza Rain Reed", (95, 130, 75), (200, 215, 124)),
    "tchita_wind_aster": (["tchita_uplands", "cerobi_steppe", "ozmone_plain"], "Áster dos ventos de Tchita", "Tchita Wind Aster", (116, 107, 185), (227, 213, 255)),
    "salika_amber_bloom": (["salikawood", "golmore_jungle"], "Flor-âmbar de Salika", "Salika Amber Bloom", (205, 110, 40), (255, 196, 106)),
    "nabreus_mist_bell": (["nabreus_deadlands", "feywood"], "Sino de névoa de Nabreus", "Nabreus Mist Bell", (105, 93, 151), (223, 205, 247)),
    "paramina_frost_bell": (["paramina_rift", "feywood", "mosphoran_highwaste"], "Sino de gelo de Paramina", "Paramina Frost Bell", (100, 170, 188), (230, 253, 253)),
    "mosphoran_stonebloom": (["mosphoran_highwaste", "cerobi_steppe", "tchita_uplands"], "Flor-da-pedra de Mosphoran", "Mosphoran Stonebloom", (169, 70, 79), (243, 167, 161)),
    "phon_sea_lily": (["phon_coast"], "Lírio costeiro de Phon", "Phon Sea Lily", (83, 167, 178), (224, 247, 233)),
}
MASONRY = {
    "dalmasca_mosaic": ("Mosaico de Dalmasca", "Dalmasca Mosaic", (58, 110, 119), (28, 64, 74), "minecraft:cyan_terracotta"),
    "archadian_bricks": ("Tijolos imperiais de Archades", "Archadian Imperial Bricks", (113, 119, 126), (55, 63, 74), "minecraft:polished_deepslate"),
    "balfonheim_dockwood": ("Pranchas do porto de Balfonheim", "Balfonheim Dock Planks", (134, 93, 63), (70, 44, 34), "minecraft:spruce_planks"),
    "bhujerban_limestone": ("Calcário lavrado de Bhujerba", "Bhujerban Limestone", (205, 194, 158), (125, 117, 97), "minecraft:calcite"),
    "amber_lantern_tile": ("Luminária âmbar de Ivalice", "Ivalice Amber Lantern Tile", (244, 168, 59), (99, 61, 29), "minecraft:glowstone"),
    "dalmasca_carved_bricks": ("Tijolos lavrados de Dalmasca", "Dalmasca Carved Bricks", (206, 169, 112), (152, 112, 77), "ivalice:dalmasca_sandstone"),
    "giza_painted_adobe": ("Adobe pintado de Giza", "Giza Painted Adobe", (164, 99, 62), (103, 67, 51), "minecraft:terracotta"),
    "rabanastre_marble_pillar": ("Pilar de mármore de Rabanastre", "Rabanastre Marble Pillar", (228, 219, 198), (168, 154, 128), "minecraft:quartz_block"),
    "rabanastre_gold_inlay": ("Pedra real folheada a ouro", "Dalmascan Gold-Inlaid Stone", (214, 178, 92), (118, 90, 42), "minecraft:gold_ingot"),
    "archadian_imperial_glass": ("Vitral imperial de Archades", "Archadian Imperial Stained Glass", (112, 168, 212), (42, 64, 92), "minecraft:glass"),
    "archadian_steel_plate": ("Placa de aço magitek de Archades", "Archadian Magitek Steel Plate", (92, 102, 114), (44, 50, 58), "minecraft:iron_block"),
    "bhujerban_sky_cobble": ("Calçamento aéreo de Bhujerba", "Bhujerban Sky Cobblestone", (188, 182, 162), (108, 102, 88), "minecraft:cobblestone"),
    "nabreus_ancient_ruin_stone": ("Pedra de ruína ancestral de Nabreus", "Nabreus Ancient Ruin Stone", (98, 108, 96), (48, 56, 46), "minecraft:mossy_cobblestone"),
    "feywood_runed_marble": ("Mármore rúnico de Feywood", "Feywood Runed Marble", (175, 212, 224), (82, 148, 188), "minecraft:amethyst_shard"),
    "scorched_magma_earth": ("Terra calcinada por magia", "Magick-Scorched Magma Earth", (68, 36, 28), (235, 98, 24), "minecraft:magma_block"),
    "glacier_rime_stone": ("Rocha glacial cristalina", "Glacier Rime Stone", (148, 206, 238), (74, 132, 184), "minecraft:packed_ice"),
}

TERRAIN_BLOCKS = [
    "dalmasca_sand",
    "dalmasca_sandstone",
    "yensa_fine_sand",
    "sunstone_ore",
    "golmore_darkwood_log",
    "golmore_darkwood_log_top",
    "golmore_moss_stone",
    "feywood_mist_soil",
    "paramina_permafrost",
    "mosphoran_slate",
    "nabreus_mire",
]


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


def clamp(v, lo=0, hi=255):
    return max(lo, min(hi, int(v)))


def blank(size):
    return [[(0, 0, 0, 0)] * size for _ in range(size)]


def blank_scalar(size, val=0):
    return [[val] * size for _ in range(size)]


def shade(rgb, delta, alpha=255):
    return (clamp(rgb[0] + delta), clamp(rgb[1] + delta), clamp(rgb[2] + delta), alpha)


def hash2d(x, y, seed=1337):
    n = (x * 374761393 + y * 668265263 + seed * 1013904223) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def smooth_noise(x, y, scale, seed=1337):
    gx = x / scale
    gy = y / scale
    ix = int(gx)
    iy = int(gy)
    fx = gx - ix
    fy = gy - iy
    sx = fx * fx * (3.0 - 2.0 * fx)
    sy = fy * fy * (3.0 - 2.0 * fy)
    w = max(1, int(32 / scale))
    x0 = ix % w
    x1 = (ix + 1) % w
    y0 = iy % w
    y1 = (iy + 1) % w
    v00 = hash2d(x0, y0, seed)
    v10 = hash2d(x1, y0, seed)
    v01 = hash2d(x0, y1, seed)
    v11 = hash2d(x1, y1, seed)
    top = v00 * (1.0 - sx) + v10 * sx
    bot = v01 * (1.0 - sx) + v11 * sx
    return top * (1.0 - sy) + bot * sy


def compute_pbr_maps(diffuse_pixels, height_map, smoothness_map, f0_map, emissive_map=None):
    W = 32
    H = 32
    normals = blank(W)
    speculars = blank(W)

    # Calculate min and max height to stretch contrast across 18..255 for deep POM relief
    h_min = min(min(row) for row in height_map)
    h_max = max(max(row) for row in height_map)
    h_span = max(1.0, float(h_max - h_min))

    # Precompute normalized height map stretched to 18..255
    stretched_h = [[18.0 + ((height_map[y][x] - h_min) / h_span) * 237.0 for x in range(W)] for y in range(H)]

    for y in range(H):
        for x in range(W):
            h_curr = stretched_h[y][x]
            # Multiplier 5.5 for deep normal relief on carved runes, mortar, bevels
            dx = (stretched_h[y][(x + 1) % W] - stretched_h[y][(x - 1) % W]) / 255.0 * 5.5
            dy = (stretched_h[(y + 1) % H][x] - stretched_h[(y - 1) % H][x]) / 255.0 * 5.5
            inv_len = 1.0 / math.sqrt(dx * dx + dy * dy + 1.0)
            nx = -dx * inv_len
            ny = -dy * inv_len

            nr = clamp((nx * 0.5 + 0.5) * 255)
            ng = clamp((ny * 0.5 + 0.5) * 255)
            # LabPBR 1.3: Blue channel is cavity / ambient occlusion in crevices (140 to 255)
            h_norm = (stretched_h[y][x] - 18.0) / 237.0
            nb = clamp(140 + int(h_norm * 115))
            # LabPBR 1.3: Alpha channel is heightmap for POM (18 to 255)
            na = clamp(max(18, min(255, int(h_curr))))
            normals[y][x] = (nr, ng, nb, na)

            sr = clamp(smoothness_map[y][x])
            sg = clamp(f0_map[y][x])
            sb = 120
            sa = clamp(emissive_map[y][x]) if emissive_map else 0
            speculars[y][x] = (sr, sg, sb, sa)

    return normals, speculars


def generate_pbr_block(name, base=(128, 128, 128), mortar=(64, 64, 64)):
    W = 32
    diffuse = blank(W)
    height = blank_scalar(W, 128)
    smoothness = blank_scalar(W, 60)
    f0 = blank_scalar(W, 40)
    emissive = blank_scalar(W, 0)

    for y in range(W):
        for x in range(W):
            if name == "rabanastre_marble_pillar":
                if y < 3 or y > 28:
                    if y in (0, 31):
                        diffuse[y][x] = shade(base, 10)
                        height[y][x] = 210
                        smoothness[y][x] = 210
                        f0[y][x] = 50
                    elif y in (1, 30):
                        diffuse[y][x] = shade(mortar, 5)
                        height[y][x] = 180
                        smoothness[y][x] = 200
                        f0[y][x] = 50
                    else:
                        diffuse[y][x] = (245, 205, 80, 255)
                        height[y][x] = 230
                        smoothness[y][x] = 230
                        f0[y][x] = 238
                else:
                    fx = x % 8
                    d = abs(fx - 3.5) / 3.5
                    if fx in (0, 7):
                        diffuse[y][x] = shade(base, 15)
                        height[y][x] = 220
                        smoothness[y][x] = 210
                        f0[y][x] = 50
                    elif fx in (3, 4):
                        diffuse[y][x] = shade(mortar, -5)
                        height[y][x] = 145
                        smoothness[y][x] = 195
                        f0[y][x] = 50
                    else:
                        diffuse[y][x] = shade(base, int((d - 0.5) * 20))
                        height[y][x] = int(145 + d * 75)
                        smoothness[y][x] = 200
                        f0[y][x] = 50
                    vein = smooth_noise(x * 1.5, y * 0.7, 8, 42)
                    if 0.48 < vein < 0.53:
                        diffuse[y][x] = shade((210, 185, 140), 10)
                        smoothness[y][x] = 220
                        f0[y][x] = 55

            elif name == "rabanastre_gold_inlay":
                dx = abs(x - 15.5)
                dy = abs(y - 15.5)
                is_border = (x in (0, 31) or y in (0, 31))
                is_center = (dx + dy <= 3.5)
                is_star = (dx + dy <= 10.5 and (dx <= 1.5 or dy <= 1.5))
                is_diag = (abs(dx - dy) <= 1.5 and dx <= 8.5 and dy <= 8.5 and dx + dy >= 6.0)
                is_ring = (9.5 <= max(dx, dy) <= 11.5 and dx + dy <= 17.5)
                is_corner = (abs(dx - 12.5) + abs(dy - 12.5) <= 2.5)
                is_gold = is_center or is_star or is_diag or is_ring or is_corner
                if is_border:
                    diffuse[y][x] = shade(mortar, 0)
                    height[y][x] = 100
                    smoothness[y][x] = 50
                    f0[y][x] = 30
                elif is_gold:
                    shine = int((hash2d(x, y, 99) - 0.5) * 12)
                    diffuse[y][x] = shade((255, 230, 120) if (is_center or dx == 0 or dy == 0) else (235, 190, 65), shine)
                    height[y][x] = 205
                    smoothness[y][x] = 225
                    f0[y][x] = 238
                else:
                    grain = int((hash2d(x, y, 77) - 0.5) * 16)
                    diffuse[y][x] = shade(base, grain)
                    height[y][x] = 175
                    smoothness[y][x] = 75
                    f0[y][x] = 40

            elif name == "archadian_imperial_glass":
                is_border = (x in (0, 1, 30, 31) or y in (0, 1, 30, 31))
                is_came = is_border or ((x + y) % 16 in (0, 1)) or ((x - y + 32) % 16 in (0, 1))
                if is_came:
                    diffuse[y][x] = shade(mortar, int((hash2d(x, y, 12) - 0.5) * 10))
                    height[y][x] = 235
                    smoothness[y][x] = 165
                    f0[y][x] = 210
                else:
                    dist_came = min((x + y) % 16, 16 - (x + y) % 16, (x - y + 32) % 16, 16 - (x - y + 32) % 16)
                    glass_shade = int((dist_came - 4) * 8)
                    diffuse[y][x] = shade(base, glass_shade, alpha=215)
                    height[y][x] = clamp(170 + dist_came * 8)
                    smoothness[y][x] = 235
                    f0[y][x] = 45

            elif name == "archadian_steel_plate":
                is_seam = (y in (15, 16) or y in (0, 31) or x in (0, 31))
                centers = [(4, 4), (27, 4), (4, 11), (27, 11), (4, 20), (27, 20), (4, 27), (27, 27),
                           (15, 4), (16, 4), (15, 27), (16, 27)]
                min_bolt_d = min(math.hypot(x - bx, y - by) for bx, by in centers)
                if min_bolt_d <= 1.8:
                    diffuse[y][x] = shade((135, 150, 165), int((1.8 - min_bolt_d) * 35))
                    height[y][x] = 245
                    smoothness[y][x] = 220
                    f0[y][x] = 240
                elif is_seam:
                    diffuse[y][x] = shade(mortar, -10)
                    height[y][x] = 80
                    smoothness[y][x] = 90
                    f0[y][x] = 160
                else:
                    brush = int((hash2d(x, y, 55) - 0.5) * 14 + ((x * 7) % 5 - 2))
                    diffuse[y][x] = shade(base, brush)
                    height[y][x] = 190
                    smoothness[y][x] = 195
                    f0[y][x] = 230

            elif name == "bhujerban_sky_cobble":
                cx = (x // 8) * 8 + 4 + int((hash2d(x // 8, y // 8, 1) - 0.5) * 3)
                cy = (y // 8) * 8 + 4 + int((hash2d(x // 8, y // 8, 2) - 0.5) * 3)
                d = math.hypot(x - cx, y - cy)
                if d > 3.4:
                    diffuse[y][x] = shade(mortar, int((hash2d(x, y, 31) - 0.5) * 14))
                    height[y][x] = 85
                    smoothness[y][x] = 45
                    f0[y][x] = 32
                else:
                    dome = math.sqrt(max(0.0, 1.0 - (d / 3.5) ** 2))
                    speckle = int((hash2d(x, y, 44) - 0.5) * 18)
                    diffuse[y][x] = shade(base, int((dome - 0.5) * 30) + speckle)
                    height[y][x] = int(120 + dome * 105)
                    smoothness[y][x] = 75
                    f0[y][x] = 40

            elif name == "nabreus_ancient_ruin_stone":
                is_split = (y == 16 + int(math.sin(x * 0.4) * 2.0))
                is_crack = (abs((x * 3 + y * 2) % 19 - 9) <= 1 and hash2d(x, y, 88) > 0.4)
                is_moss = (smooth_noise(x, y, 6, 101) > 0.62)
                if is_crack:
                    diffuse[y][x] = shade((35, 42, 32), 0)
                    height[y][x] = 65
                    smoothness[y][x] = 35
                    f0[y][x] = 25
                elif is_moss:
                    diffuse[y][x] = shade((65, 95, 52), int((hash2d(x, y, 92) - 0.5) * 20))
                    height[y][x] = 145
                    smoothness[y][x] = 40
                    f0[y][x] = 25
                elif is_split:
                    diffuse[y][x] = shade(mortar, -10)
                    height[y][x] = 80
                    smoothness[y][x] = 45
                    f0[y][x] = 30
                else:
                    weathering = int((hash2d(x, y, 71) - 0.5) * 24)
                    diffuse[y][x] = shade(base, weathering)
                    height[y][x] = 175 + weathering // 2
                    smoothness[y][x] = 60
                    f0[y][x] = 38

            elif name == "feywood_runed_marble":
                rad = math.hypot(x - 15.5, y - 15.5)
                is_circle = abs(rad - 6.5) <= 1.0
                is_cross = (abs(x - 15.5) <= 1.0 and abs(y - 15.5) <= 4.0) or (abs(y - 15.5) <= 1.0 and abs(x - 15.5) <= 4.0)
                is_star = (abs(x - y) <= 1.0 or abs(x + y - 31) <= 1.0) and (8.0 <= rad <= 12.0)
                is_rune = is_circle or is_cross or is_star
                if is_rune:
                    core = (abs(x - 15.5) <= 0.5 or abs(y - 15.5) <= 0.5 or abs(rad - 6.5) <= 0.4)
                    diffuse[y][x] = (210, 255, 255, 255) if core else (90, 230, 255, 255)
                    height[y][x] = 120
                    smoothness[y][x] = 230
                    f0[y][x] = 60
                    emissive[y][x] = 210
                else:
                    marble_grain = int((smooth_noise(x, y, 8, 33) - 0.5) * 22)
                    diffuse[y][x] = shade(base, marble_grain)
                    height[y][x] = 200
                    smoothness[y][x] = 205
                    f0[y][x] = 55
                    emissive[y][x] = 0

            elif name == "scorched_magma_earth":
                v = abs(smooth_noise(x, y, 7, 77) - 0.5) * 2.0
                if v < 0.16:
                    inner = v < 0.08
                    diffuse[y][x] = (255, 230, 80, 255) if inner else (240, 95, 20, 255)
                    height[y][x] = 90
                    smoothness[y][x] = 185
                    f0[y][x] = 50
                    emissive[y][x] = 210
                else:
                    rock_grain = int((hash2d(x, y, 84) - 0.5) * 24)
                    diffuse[y][x] = shade(base, rock_grain)
                    height[y][x] = 210
                    smoothness[y][x] = 55
                    f0[y][x] = 35
                    emissive[y][x] = 0

            elif name == "glacier_rime_stone":
                facet = (x * 3 + y * 5) % 11
                if facet in (0, 10):
                    diffuse[y][x] = shade((230, 248, 255), 0)
                    height[y][x] = 235
                    smoothness[y][x] = 230
                    f0[y][x] = 55
                else:
                    slope = (facet / 10.0) * 35 - 15
                    diffuse[y][x] = shade(base, int(slope))
                    height[y][x] = int(150 + (facet / 10.0) * 75)
                    smoothness[y][x] = 215
                    f0[y][x] = 50

            elif name == "dalmasca_mosaic":
                tx, ty = x % 8, y % 8
                if tx in (0, 7) or ty in (0, 7):
                    diffuse[y][x] = shade(mortar, int((hash2d(x, y, 17) - 0.5) * 8))
                    height[y][x] = 90
                    smoothness[y][x] = 50
                    f0[y][x] = 30
                elif abs(tx - 3.5) + abs(ty - 3.5) <= 1.5:
                    diffuse[y][x] = shade((225, 175, 75), int((hash2d(x, y, 29) - 0.5) * 10))
                    height[y][x] = 205
                    smoothness[y][x] = 220
                    f0[y][x] = 230
                else:
                    dome = 1.0 - (abs(tx - 3.5) + abs(ty - 3.5)) / 7.0
                    diffuse[y][x] = shade(base, int((dome - 0.5) * 25))
                    height[y][x] = int(160 + dome * 45)
                    smoothness[y][x] = 190
                    f0[y][x] = 48

            elif name == "archadian_bricks":
                row = y // 8
                depth = y % 8
                x_off = (x + (row % 2) * 16) % 32
                bx = x_off % 16
                if depth == 7 or bx == 15:
                    diffuse[y][x] = shade(mortar, int((hash2d(x, y, 61) - 0.5) * 8))
                    height[y][x] = 85
                    smoothness[y][x] = 45
                    f0[y][x] = 30
                else:
                    bevel_x = min(bx, 14 - bx)
                    bevel_y = min(depth, 6 - depth)
                    bevel = min(bevel_x, bevel_y)
                    grain = int((hash2d(x, y, 62) - 0.5) * 18)
                    diffuse[y][x] = shade(base, grain + (12 if bevel >= 2 else -8))
                    height[y][x] = clamp(120 + bevel * 28 + grain // 2)
                    smoothness[y][x] = 85
                    f0[y][x] = 45

            elif name == "balfonheim_dockwood":
                row = y // 8
                py = y % 8
                if py == 7:
                    diffuse[y][x] = shade((52, 34, 26), 0)
                    height[y][x] = 75
                    smoothness[y][x] = 40
                    f0[y][x] = 25
                elif x in (4, 27) and py in (3, 4):
                    diffuse[y][x] = shade((160, 150, 145), 0)
                    height[y][x] = 230
                    smoothness[y][x] = 175
                    f0[y][x] = 220
                else:
                    grain = int((hash2d(x // 2, py, row * 17) - 0.5) * 20 + math.sin(x * 0.3) * 6)
                    camber = int(math.sin(py / 6.0 * math.pi) * 25)
                    diffuse[y][x] = shade(base, grain + camber - 10)
                    height[y][x] = clamp(155 + camber + grain // 2)
                    smoothness[y][x] = 80
                    f0[y][x] = 35

            elif name == "bhujerban_limestone":
                is_border = (x < 3 or x > 28 or y < 3 or y > 28)
                is_center = (abs(x - 15.5) <= 4.0 and abs(y - 15.5) <= 4.0)
                grain = int((hash2d(x, y, 73) - 0.5) * 16)
                if is_border:
                    diffuse[y][x] = shade(base, grain + 12)
                    height[y][x] = 210
                    smoothness[y][x] = 90
                    f0[y][x] = 42
                elif is_center:
                    diffuse[y][x] = shade(base, grain + 18)
                    height[y][x] = 225
                    smoothness[y][x] = 110
                    f0[y][x] = 45
                else:
                    diffuse[y][x] = shade(base, grain - 8)
                    height[y][x] = 175
                    smoothness[y][x] = 85
                    f0[y][x] = 42

            elif name == "amber_lantern_tile":
                is_outer = (x < 3 or x > 28 or y < 3 or y > 28)
                is_lattice = (abs(x - y) <= 1 or abs(x + y - 31) <= 1)
                if is_outer or is_lattice:
                    diffuse[y][x] = shade(mortar, int((hash2d(x, y, 41) - 0.5) * 14))
                    height[y][x] = 235
                    smoothness[y][x] = 180
                    f0[y][x] = 225
                    emissive[y][x] = 0
                else:
                    rad = math.hypot(x - 15.5, y - 15.5)
                    is_vein = (abs(rad - 6.0) <= 1.2 or (hash2d(x, y, 82) > 0.65))
                    if is_vein:
                        diffuse[y][x] = shade((255, 220, 110), int((hash2d(x, y, 83) - 0.5) * 15))
                        height[y][x] = 175
                        smoothness[y][x] = 220
                        f0[y][x] = 55
                        emissive[y][x] = 210
                    else:
                        diffuse[y][x] = shade(base, int((hash2d(x, y, 81) - 0.5) * 18))
                        height[y][x] = 160
                        smoothness[y][x] = 205
                        f0[y][x] = 50
                        emissive[y][x] = 0

            elif name == "dalmasca_carved_bricks":
                row = y // 8
                depth = y % 8
                x_off = (x + (row % 2) * 16) % 32
                bx = x_off % 16
                if depth == 7 or bx == 15:
                    diffuse[y][x] = shade(mortar, 0)
                    height[y][x] = 90
                    smoothness[y][x] = 40
                    f0[y][x] = 28
                elif depth in (3, 4) and 2 <= bx <= 13:
                    diffuse[y][x] = shade(mortar, 10)
                    height[y][x] = 140
                    smoothness[y][x] = 60
                    f0[y][x] = 35
                else:
                    grain = int((hash2d(x, y, 38) - 0.5) * 20)
                    diffuse[y][x] = shade(base, grain + (10 if depth < 3 else 0))
                    height[y][x] = 190 + grain // 2
                    smoothness[y][x] = 65
                    f0[y][x] = 38

            elif name == "giza_painted_adobe":
                is_band = 11 <= y <= 20
                grain = int((hash2d(x, y, 91) - 0.5) * 16)
                undulation = int(math.sin(x * 0.3) * 10 + math.sin(y * 0.4) * 8)
                if is_band:
                    chevron = (x + y) % 8 < 4
                    diffuse[y][x] = shade((235, 205, 140) if chevron else (65, 145, 135), grain)
                    height[y][x] = 190 + undulation
                    smoothness[y][x] = 60
                    f0[y][x] = 32
                else:
                    diffuse[y][x] = shade(base, grain + undulation)
                    height[y][x] = 180 + undulation
                    smoothness[y][x] = 55
                    f0[y][x] = 30

            # Terrain blocks
            elif name == "dalmasca_sand":
                wave = math.sin(y * 0.45 + math.sin(x * 0.25) * 1.8)
                grain = int((hash2d(x, y, 105) - 0.5) * 22)
                diffuse[y][x] = shade((226, 192, 119), int(wave * 16) + grain)
                height[y][x] = clamp(170 + wave * 30 + grain)
                smoothness[y][x] = 50
                f0[y][x] = 32

            elif name == "dalmasca_sandstone":
                layer = (y // 7)
                layer_tone = (12, -8, 8, -12, 6)[layer % 5]
                grain = int((hash2d(x, y, 111) - 0.5) * 18)
                diffuse[y][x] = shade((207, 169, 98), layer_tone + grain)
                height[y][x] = clamp(180 + layer_tone * 2 + grain)
                smoothness[y][x] = 65
                f0[y][x] = 38

            elif name == "yensa_fine_sand":
                wave = math.sin((x + y * 1.5) * 0.35)
                grain = int((hash2d(x, y, 117) - 0.5) * 20)
                diffuse[y][x] = shade((216, 145, 78), int(wave * 18) + grain)
                height[y][x] = clamp(170 + wave * 32 + grain)
                smoothness[y][x] = 52
                f0[y][x] = 34

            elif name == "sunstone_ore":
                centers = [(8, 8), (23, 10), (14, 22), (25, 25)]
                min_dist = min(math.hypot(x - cx, y - cy) for cx, cy in centers)
                if min_dist <= 2.8:
                    core = min_dist <= 1.4
                    diffuse[y][x] = (255, 240, 160, 255) if core else (255, 180, 45, 255)
                    height[y][x] = 240
                    smoothness[y][x] = 220
                    f0[y][x] = 60
                    emissive[y][x] = 210
                else:
                    rock_grain = int((hash2d(x, y, 123) - 0.5) * 22)
                    diffuse[y][x] = shade((149, 141, 117), rock_grain)
                    height[y][x] = clamp(165 + rock_grain)
                    smoothness[y][x] = 65
                    f0[y][x] = 38
                    emissive[y][x] = 0

            elif name == "golmore_darkwood_log":
                furrow = math.sin(x * 0.75 + hash2d(x // 2, y // 4, 131) * 2.5)
                grain = int((hash2d(x, y, 132) - 0.5) * 16)
                diffuse[y][x] = shade((62, 44, 32), int(furrow * 20) + grain)
                height[y][x] = clamp(160 + furrow * 55 + grain)
                smoothness[y][x] = 60
                f0[y][x] = 32

            elif name == "golmore_darkwood_log_top":
                d_center = math.hypot(x - 15.5, y - 15.5)
                if d_center >= 13.5:
                    diffuse[y][x] = shade((62, 44, 32), int((hash2d(x, y, 137) - 0.5) * 18))
                    height[y][x] = 215
                    smoothness[y][x] = 60
                    f0[y][x] = 32
                else:
                    rings = math.sin(d_center * 1.6)
                    grain = int((hash2d(x, y, 138) - 0.5) * 14)
                    diffuse[y][x] = shade((108, 83, 60), int(rings * 16) + grain)
                    height[y][x] = clamp(175 + rings * 25 + grain)
                    smoothness[y][x] = 70
                    f0[y][x] = 35

            elif name == "golmore_moss_stone":
                moss_val = smooth_noise(x, y, 6, 141)
                if moss_val > 0.45:
                    grain = int((hash2d(x, y, 142) - 0.5) * 18)
                    diffuse[y][x] = shade((65, 120, 50), grain)
                    height[y][x] = 210
                    smoothness[y][x] = 45
                    f0[y][x] = 28
                else:
                    grain = int((hash2d(x, y, 143) - 0.5) * 22)
                    diffuse[y][x] = shade((56, 75, 60), grain)
                    height[y][x] = 160
                    smoothness[y][x] = 65
                    f0[y][x] = 38

            elif name == "feywood_mist_soil":
                loam_grain = int((hash2d(x, y, 151) - 0.5) * 18)
                mycelium = smooth_noise(x, y, 5, 152) > 0.65
                if mycelium:
                    diffuse[y][x] = shade((235, 230, 245), 0)
                else:
                    diffuse[y][x] = shade((212, 220, 228), loam_grain)
                height[y][x] = clamp(175 + loam_grain)
                smoothness[y][x] = 65
                f0[y][x] = 40

            elif name == "paramina_permafrost":
                ice_val = smooth_noise(x, y, 6, 161)
                if ice_val > 0.48:
                    diffuse[y][x] = shade((190, 235, 255), int((hash2d(x, y, 162) - 0.5) * 16))
                    height[y][x] = 225
                    smoothness[y][x] = 205
                    f0[y][x] = 50
                else:
                    diffuse[y][x] = shade((125, 175, 205), int((hash2d(x, y, 163) - 0.5) * 20))
                    height[y][x] = 160
                    smoothness[y][x] = 60
                    f0[y][x] = 36

            elif name == "mosphoran_slate":
                slab = (x * 2 + y * 3) % 9
                grain = int((hash2d(x, y, 171) - 0.5) * 16)
                diffuse[y][x] = shade((71, 81, 91), (slab - 4) * 8 + grain)
                height[y][x] = clamp(130 + slab * 11 + grain)
                smoothness[y][x] = 85
                f0[y][x] = 50

            elif name == "nabreus_mire":
                mud_wave = math.sin(x * 0.3) * math.cos(y * 0.3)
                grain = int((hash2d(x, y, 181) - 0.5) * 16)
                diffuse[y][x] = shade((55, 63, 57), int(mud_wave * 12) + grain)
                height[y][x] = clamp(160 + mud_wave * 20 + grain)
                smoothness[y][x] = 80
                f0[y][x] = 32

            else:
                grain = int((hash2d(x, y, 199) - 0.5) * 20)
                diffuse[y][x] = shade(base, grain)
                height[y][x] = clamp(170 + grain)
                smoothness[y][x] = 65
                f0[y][x] = 40

    normals, speculars = compute_pbr_maps(diffuse, height, smoothness, f0, emissive)
    return diffuse, normals, speculars


def masonry_pixels(base, mortar, painted=False, name=""):
    if name:
        return generate_pbr_block(name, base, mortar)[0]
    return generate_pbr_block("generic_masonry", base, mortar)[0]



def plant_pixels(index, petals, bright):
    pixels = blank(32)
    for stem in range(5):
        start = 15 + (stem - 2) * 3
        top = (8, 4, 10, 6, 12)[stem] + (index % 2) * 2
        tip = (5, 11, 16, 21, 26)[stem]
        for y in range(top, 32):
            t = (y - top) / max(1, (31 - top))
            x = round(tip + (start - tip) * t + math.sin(t * math.pi) * (stem - 2) * 0.8)
            x = max(1, min(30, x))
            # Stem with 3D cylindrical highlight and dark edge
            pixels[y][x] = shade((68, 112, 48), (y % 4) * 5)
            if x + 1 < 32:
                pixels[y][x + 1] = shade((44, 78, 32), -(y % 3) * 4)
            if y in (16, 20, 24, 27):
                side = -1 if (stem + y) % 2 == 0 else 1
                for length in range(1, 7):
                    lx = max(0, min(31, x + side * length))
                    ly = max(0, min(31, y - length // 2))
                    pixels[ly][lx] = shade((86, 134, 58), 18 - 4 * length)
                    if ly + 1 < 32:
                        pixels[ly + 1][lx] = shade((52, 88, 36), 6 - 3 * length)
        # Ornate 32x32 blossom head with inner pistil, bright core, layered petals, and dark sepal rim
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                dist = math.hypot(dx, dy)
                if dist <= 3.3:
                    py = max(0, min(31, top + dy))
                    px = max(0, min(31, tip + dx))
                    if dist <= 1.1:
                        pixels[py][px] = shade((255, 248, 175), 10)
                    elif dist <= 2.1:
                        pixels[py][px] = shade(bright, int((1.5 - dy) * 8))
                    else:
                        pixels[py][px] = shade(petals, int(-dy * 6 - (dist - 2.0) * 14))
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
                   "condition": {"type": "minecraft:survives_explosion"}}],
    })


def flora_and_blocks():
    labels = [{}, {}]
    for index, (name, (biomes, pt, en, petals, bright)) in enumerate(PLANTS.items()):
        block_assets(name, True)
        plant_px = plant_pixels(index, petals, bright)
        save_png(ASSETS / f"textures/block/{name}.png", plant_px)
        # Also write flat LabPBR _n.png and _s.png for plants so shaders never sample missing PBR maps
        plant_n = [[(128, 128, 255, 255) for x in range(32)] for y in range(32)]
        plant_s = [[(45, 25, 180, 0) for x in range(32)] for y in range(32)]
        save_png(ASSETS / f"textures/block/{name}_n.png", plant_n)
        save_png(ASSETS / f"textures/block/{name}_s.png", plant_s)
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
        diffuse, normals, speculars = generate_pbr_block(name, base, mortar)
        save_png(ASSETS / f"textures/block/{name}.png", diffuse)
        save_png(ASSETS / f"textures/block/{name}_n.png", normals)
        save_png(ASSETS / f"textures/block/{name}_s.png", speculars)
        save_json(DATA / f"recipe/{name}.json", {
            "type": "minecraft:crafting_shaped", "pattern": ["XX", "XX"], "key": {"X": material},
            "result": {"id": f"ivalice:{name}", "count": 4},
        })

    for name in TERRAIN_BLOCKS:
        diffuse, normals, speculars = generate_pbr_block(name)
        save_png(ASSETS / f"textures/block/{name}.png", diffuse)
        save_png(ASSETS / f"textures/block/{name}_n.png", normals)
        save_png(ASSETS / f"textures/block/{name}_s.png", speculars)

    # Upgrade initial 3 flora blocks (estersand_scrub, gysahl_greens_wild, feywood_white_lily) to 32x32 HD + PBR maps
    extra_flora = [
        ("estersand_scrub", (158, 128, 68), (218, 188, 112)),
        ("gysahl_greens_wild", (78, 158, 52), (168, 232, 92)),
        ("feywood_white_lily", (188, 224, 242), (248, 255, 255)),
    ]
    for idx, (fname, petals, bright) in enumerate(extra_flora):
        f_px = plant_pixels(idx + 20, petals, bright)
        save_png(ASSETS / f"textures/block/{fname}.png", f_px)
        f_n = [[(128, 128, 255, 255) for x in range(32)] for y in range(32)]
        f_s = [[(55, 28, 180, 180 if (fname == "feywood_white_lily" and f_px[y][x][3] > 0 and y < 14) else 0) for x in range(32)] for y in range(32)]
        save_png(ASSETS / f"textures/block/{fname}_n.png", f_n)
        save_png(ASSETS / f"textures/block/{fname}_s.png", f_s)

    from upgrade_ivalice_item_sprites import save_png_32x32, upgrade_sprite_32x32

    seal = blank(16)
    for y in range(2, 14):
        for x in range(2, 14):
            distance = abs(x - 7.5) + abs(y - 7.5)
            if distance <= 8:
                seal[y][x] = shade((209, 161, 79) if distance >= 6 or (x + y) % 6 == 0 else (64, 100, 125), 12 - y)
    seal_texture = ASSETS / "textures/item/zodiac_reset_seal.png"
    save_png_32x32(seal_texture, upgrade_sprite_32x32(seal_texture.name, 16, 16, seal))
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



def regional_groves():
    worldgen = DATA / "worldgen"
    template = json.loads((worldgen / "feature/golmore_darkwood_tree.json").read_text(encoding="utf-8"))
    placement = json.loads((worldgen / "placed_feature/golmore_darkwood_tree.json").read_text(encoding="utf-8"))
    groves = [
        ("salika_amber_grove", "salikawood", "minecraft:birch_leaves", "minecraft:stripped_dark_oak_log",
         6, 4, 3, 6, ["minecraft:trees_birch_and_oak_leaf_litter", "minecraft:trees_flower_forest", "ivalice:golmore_darkwood_tree"]),
        ("feywood_silver_grove", "feywood", "minecraft:pale_oak_leaves", "minecraft:pale_oak_log",
         7, 3, 2, 7, ["minecraft:trees_snowy", "minecraft:trees_grove", "minecraft:trees_taiga"]),
        ("nabreus_mire_grove", "nabreus_deadlands", "minecraft:mangrove_leaves", "ivalice:golmore_darkwood_log",
         5, 3, 2, 3, ["minecraft:trees_swamp"]),
    ]
    for name, biome, leaf, log, height, spread, radius, count, replaced in groves:
        feature = {
            **template,
            "decorators": template["decorators"] if biome == "nabreus_deadlands" else [],
            "foliage_provider": {**template["foliage_provider"], "id": leaf},
            "trunk_provider": {**template["trunk_provider"], "id": log},
            "trunk_placer": {**template["trunk_placer"], "base_height": height, "height_rand_a": spread},
            "foliage_placer": {**template["foliage_placer"], "radius": radius},
        }
        filters = [{"type": "minecraft:count", "count": count}, *placement["placement"][1:]]
        save_json(worldgen / f"feature/{name}.json", feature)
        save_json(worldgen / f"placed_feature/{name}.json", {"feature": f"ivalice:{name}", "placement": filters})
        path = worldgen / f"biome/{biome}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        reference = f"ivalice:{name}"
        vegetation = [entry for entry in data["features"][9] if entry not in [*replaced, reference]]
        tree_position = next((i for i, entry in enumerate(vegetation) if entry.startswith("ivalice:patch_")), len(vegetation))
        vegetation.insert(tree_position, reference)
        data["features"][9] = vegetation
        save_json(path, data)


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

    residents = [(7, 1, 9, "none"), (18, 1, 10, "none"), (12, 1, 9, "farmer"), (14, 1, 13, "mason")]
    write_site(name, villager_type, biomes, blocks, residents, (26, 8, 22), -2, -2, salt, 30, "beard_thin")
    return {f"ivalice:{biome}": {"villager_type": f"ivalice:{villager_type}"} for biome in biomes}


def write_site(name, villager_type, biomes, blocks, residents, dimensions, base, height_offset, salt, spacing,
               terrain, chests=()):
    palette = list(dict.fromkeys(blocks.values()))
    indices = {block: index for index, block in enumerate(palette)}
    states = []
    for block, properties in palette:
        state = {"id": (8, block)}
        if properties:
            state["properties"] = (10, {key: (8, value) for key, value in properties})
        states.append(state)
    chests = dict(chests)
    block_tags = []
    for (x, y, z), block in sorted(blocks.items(), key=lambda row: (row[0][1], row[0][0], row[0][2])):
        tag = {"pos": (9, (3, [x, y - base, z])), "state": (3, indices[block])}
        if (x, y, z) in chests:
            tag["nbt"] = (10, {"id": (8, "minecraft:chest"), "LootTable": (8, chests[x, y, z])})
        block_tags.append(tag)

    entities = []
    for x, y, z, profession in residents:
        entities.append({
            "pos": (9, (6, [x + 0.5, y - base, z + 0.5])),
            "blockPos": (9, (3, [x, y - base, z])),
            "nbt": (10, {
                "id": (8, "minecraft:villager"),
                "VillagerData": (10, {"type": (8, f"ivalice:{villager_type}"),
                                      "profession": (8, f"minecraft:{profession}"), "level": (3, 1)}),
                "VillagerDataFinalized": (1, 1),
            }),
        })
    template = {"DataVersion": (3, 5023), "size": (9, (3, list(dimensions))),
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
        "start_pool": f"ivalice:{name}", "start_height": {"absolute": height_offset}, "size": 1,
        "project_start_to_heightmap": "WORLD_SURFACE_WG", "terrain_adaptation": terrain,
        "step": "surface_structures", "spawn_overrides": {}, "max_distance_from_center": 80, "use_expansion_hack": False,
    })
    save_json(DATA / f"worldgen/structure_set/{name}.json", {
        "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": spacing // 3, "salt": salt},
        "structures": [{"structure": f"ivalice:{name}", "weight": 1}],
    })


def city(name, villager_type, biomes, wall, roof, paving, garden, salt, theme, reward_names):
    width, length = 56, 48
    blocks = {}
    residents = []
    chests = []
    lights = "ivalice:amber_lantern_tile"
    water = theme == "port"
    floating = theme == "sky"
    base = -17 if water else -10 if floating else -4
    height_offset = -1 if water else 43 if floating else -4

    def put(x, y, z, material, **properties):
        blocks[x, y, z] = material, tuple(sorted(properties.items()))

    def fill(x0, y0, z0, x1, y1, z1, material):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    put(x, y, z, material)

    def building(x0, z0, x1, z1, height, front, material=wall, y0=1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                for y in range(y0, y0 + height + 1):
                    edge = x in (x0, x1) or z in (z0, z1)
                    if y == y0 + height:
                        material_at = roof
                    elif edge:
                        if z == front and abs(x - (x0 + x1) / 2) < 1.55 and y <= y0 + 2:
                            material_at = "minecraft:air"
                        elif y % 4 == 3 and (x + z) % 4 in (1, 2):
                            material_at = "minecraft:glass"
                        else:
                            material_at = material
                    else:
                        material_at = "minecraft:air"
                    put(x, y, z, material_at)
        for x in range(x0, x1 + 1, 2):
            put(x, y0 + height + 1, z0, material)
            put(x, y0 + height + 1, z1, material)
        put(x0 + 1, y0 + 3, front, lights)
        put(x1 - 1, y0 + 3, front, lights)

    for x in range(2, width - 2):
        for z in range(2, length - 2):
            if floating:
                radius = ((x - 27.5) / 27) ** 2 + ((z - 23.5) / 24) ** 2
                foundation = min(10, max(2, round(10 * (1 - radius))))
                for y in range(-foundation, 0):
                    put(x, y, z, "minecraft:calcite" if (x * 3 + y * 5 + z) % 17 < 4 else "minecraft:stone")
            elif not water:
                for y in range(base, 0):
                    put(x, y, z, wall)
            lane = x in range(24, 32) or z in range(20, 28)
            put(x, 0, z, paving if lane else wall)
            if not water and not floating and lane:
                for y in range(1, 4):
                    put(x, y, z, "minecraft:air")

    if water:
        for x in range(3, 54, 8):
            for z in range(3, 47, 8):
                fill(x, base, z, x + 1, 0, z + 1, "minecraft:stripped_spruce_log")
        for x in (6, 24, 42):
            fill(x, 0, 38, x + 5, 0, 47, "ivalice:balfonheim_dockwood")
            for z in (42, 47):
                fill(x, -12, z, x, 1, z, "minecraft:stripped_spruce_log")
                put(x, 2, z, lights)
    elif floating:
        for x in (22, 33):
            fill(x, 1, 36, x + 1, 7, 38, wall)
            put(x, 8, 37, lights)
        fill(23, 7, 36, 32, 7, 38, roof)
        for x in range(13, 43):
            for z in range(38, 46):
                put(x, 0, z, paving if z % 3 else lights)
    else:
        for x in (2, 53):
            for z in range(2, 46):
                if z not in range(20, 28):
                    fill(x, 1, z, x, 4, z, wall)
                    if z % 2 == 0:
                        put(x, 5, z, roof)
        for x in range(2, 54):
            if x not in range(24, 32):
                for z in (2, 45):
                    fill(x, 1, z, x, 4, z, wall)
        for x, z in [(2, 2), (48, 2), (2, 40), (48, 40)]:
            building(x, z, x + 5, z + 5, 8 if theme == "desert" else 12, z + 5)

    for x, z, height in [(7, 8, 5), (17, 8, 6), (38, 8, 5), (7, 31, 6), (17, 33, 5), (38, 31, 6)]:
        front = z + 7 if z < 20 else z
        building(x, z, x + 7, z + 7, height + (2 if theme == "imperial" else 0), front)
        put(x + 2, 1, z + 4, "minecraft:crafting_table")
        put(x + 5, 1, z + 4, "minecraft:red_bed", facing="north", part="foot", occupied="false")
        put(x + 5, 1, z + 3, "minecraft:red_bed", facing="north", part="head", occupied="false")
        residents.append((x + 4, 1, z + 4, "none"))

    # The silhouette of the central landmark differs between cities.
    if theme == "imperial":
        building(24, 5, 34, 16, 14, 16)
        for x in (24, 34):
            for z in (5, 16):
                fill(x, 10, z, x, 18, z, wall)
                put(x, 19, z, lights)
    elif theme == "desert":
        building(26, 5, 35, 16, 8, 16)
        fill(27, 10, 7, 34, 10, 14, roof)
        fill(29, 11, 8, 32, 11, 13, roof)
        put(30, 12, 10, lights)
    elif theme == "port":
        building(25, 5, 36, 15, 8, 15, "minecraft:stripped_spruce_wood")
        fill(31, 10, 10, 31, 16, 10, "minecraft:stripped_spruce_log")
        put(31, 17, 10, lights)
    else:
        building(25, 6, 36, 15, 6, 15)
        for x in (24, 37):
            fill(x, 1, 10, x, 12, 10, wall)
            put(x, 13, 10, lights)

    for x, z in [(12, 20), (18, 20), (36, 20), (43, 20), (12, 27), (43, 27)]:
        fill(x, 1, z, x, 3, z, "minecraft:oak_fence")
        fill(x + 3, 1, z, x + 3, 3, z, "minecraft:oak_fence")
        fill(x, 4, z - 1, x + 3, 4, z + 1, roof)
        put(x + 1, 1, z, "minecraft:barrel", facing="up", open="false")
        put(x + 2, 1, z, "minecraft:composter")
        residents.append((x + 2, 1, z + 2, "farmer"))

    for x, z in [(23, 19), (33, 19), (23, 28), (33, 28), (5, 19), (47, 28)]:
        put(x, 0, z, "minecraft:rooted_dirt")
        put(x, 1, z, garden)
        put(x, 1, z + 1, lights)
    for x, z in [(24, 19), (35, 29), (27, 14)]:
        put(x, 1, z, "minecraft:chest", facing="south", type="single", waterlogged="false")
        chests.append(((x, 1, z), f"ivalice:chests/{name}"))

    items = {}
    catalog_file = RESOURCES / "ivalice/tza_catalog.tsv"
    if catalog_file.exists():
        for row in catalog_file.read_text(encoding="utf-8").splitlines():
            _, item_id, _, _, _, label = row.split("\t", 5)
            items[label] = item_id
    rewards = [items[label] for label in reward_names if label in items] or ["potion", "hi_potion"]
    save_json(DATA / f"loot_table/chests/{name}.json", {
        "type": "minecraft:chest",
        "pools": [{"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4},
                   "entries": [{"type": "minecraft:item", "name": f"ivalice:{item}"} for item in rewards]}],
    })
    write_site(name, villager_type, biomes, blocks, residents, (width, 23 - base, length), base, height_offset,
               salt, 78, "none" if floating or water else "beard_thin", chests)
    return {f"ivalice:{biome}": {"villager_type": f"ivalice:{villager_type}"} for biome in biomes}


def cities():
    types = city("rabanastre", "dalmasca", ["dalmasca_estersand", "dalmasca_westersand"],
                 "ivalice:dalmasca_carved_bricks", "ivalice:dalmasca_mosaic", "minecraft:smooth_sandstone",
                 "ivalice:dalmasca_emberbloom", 30926103, "desert",
                 ["Potion", "Phoenix Down", "Longsword", "Iron Sword", "Cure", "Fire"])
    types.update(city("archades", "archades", ["tchita_uplands", "cerobi_steppe"],
                      "ivalice:archadian_bricks", "minecraft:blue_terracotta", "minecraft:polished_deepslate",
                      "ivalice:tchita_wind_aster", 30926111, "imperial",
                      ["Hi-Potion", "Diamond Sword", "Gold Hairpin", "Haste", "Platinum Shield"]))
    types.update(city("balfonheim_port", "balfonheim", ["phon_coast"],
                      "ivalice:balfonheim_dockwood", "minecraft:red_terracotta", "minecraft:dark_oak_planks",
                      "ivalice:phon_sea_lily", 30926119, "port",
                      ["X-Potion", "Hi-Ether", "Duel Mask", "Fomalhaut", "Megalixir"]))
    types.update(city("bhujerba", "bhujerba", ["mosphoran_highwaste"],
                      "ivalice:bhujerban_limestone", "minecraft:lime_terracotta", "minecraft:smooth_quartz",
                      "ivalice:mosphoran_stonebloom", 30926127, "sky",
                      ["Hi-Potion", "Aero", "Javelin", "Mage Masher", "Bhujerban Madhu"]))
    villager_clothes("archades", (79, 95, 126), (173, 151, 118), (233, 212, 150))
    villager_clothes("balfonheim", (72, 111, 104), (153, 73, 51), (220, 183, 103))
    villager_clothes("bhujerba", (174, 161, 116), (100, 124, 80), (242, 223, 165))
    return types


def main():
    flora_and_blocks()
    regional_groves()
    villagers = hamlet("estersand_outpost", "dalmasca", ["dalmasca_estersand", "dalmasca_westersand"],
                       "ivalice:dalmasca_carved_bricks", "minecraft:cyan_terracotta", "ivalice:dalmasca_emberbloom", 30926011)
    villagers.update(hamlet("giza_hamlet", "giza", ["giza_plains_dry", "ozmone_plain"],
                           "ivalice:giza_painted_adobe", "minecraft:orange_terracotta", "ivalice:giza_sunbloom", 30926029))
    villagers.update(cities())
    save_json(RESOURCES / "data/neoforge/data_maps/worldgen/biome/villager_types.json", {"values": villagers})
    villager_clothes("dalmasca", (200, 174, 133), (42, 99, 112), (224, 183, 102))
    villager_clothes("giza", (119, 134, 87), (175, 95, 64), (233, 192, 112))


if __name__ == "__main__":
    main()
