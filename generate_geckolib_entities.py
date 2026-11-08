"""
Projeto Ivalice - GeckoLib 3D Entity & Biome Spawn Generator
Minecraft 26.3 / NeoForge - GeckoLib 5.5.7

Generates:
1. Bedrock/GeckoLib 1.12.0 geometry JSONs for 12 FFXII entities at BOTH:
   - src/main/resources/assets/ivalice/geckolib/models/entity/<name>.geo.json
   - src/main/resources/assets/ivalice/geo/entity/<name>.geo.json
2. GeckoLib 1.8.0 animation JSONs (idle + walk) for all 12 entities at BOTH:
   - src/main/resources/assets/ivalice/geckolib/animations/entity/<name>.animation.json
   - src/main/resources/assets/ivalice/animations/entity/<name>.animation.json
3. 64x64 RGBA PNG textures with rich canonical FFXII shading at:
   - src/main/resources/assets/ivalice/textures/entity/<name>.png
4. Updates all 15 biome JSONs in:
   - src/main/resources/data/ivalice/worldgen/biome/*.json
   without UTF-8 BOM, including canonical regional spawns in "creature" category.
"""

import os
import sys
import json
import math
import struct
import zlib
import glob

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ASSETS_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "assets", "ivalice")
DATA_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "data", "ivalice")

GEO_DIR_1 = os.path.join(ASSETS_DIR, "geckolib", "models", "entity")
GEO_DIR_2 = os.path.join(ASSETS_DIR, "geo", "entity")

ANIM_DIR_1 = os.path.join(ASSETS_DIR, "geckolib", "animations", "entity")
ANIM_DIR_2 = os.path.join(ASSETS_DIR, "animations", "entity")

TEXTURES_DIR = os.path.join(ASSETS_DIR, "textures", "entity")
BIOMES_DIR = os.path.join(DATA_DIR, "worldgen", "biome")

# ==============================================================================
# 1. PURE PYTHON 64x64 PNG ENCODER (ZERO EXTERNAL DEPENDENCY)
# ==============================================================================

def hex_rgba(hex_code, alpha=255):
    h = hex_code.lstrip("#")
    if len(h) == 6:
        return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), alpha)
    elif len(h) == 8:
        return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), int(h[6:8], 16))
    raise ValueError(f"Invalid hex color: {hex_code}")

def shade_color(color, factor):
    """Adjust color brightness: factor > 1 brightens, factor < 1 darkens."""
    r, g, b, a = color
    nr = max(0, min(255, int(r * factor)))
    ng = max(0, min(255, int(g * factor)))
    nb = max(0, min(255, int(b * factor)))
    return (nr, ng, nb, a)

def blend_colors(base, overlay, alpha_factor=1.0):
    r1, g1, b1, a1 = base
    r2, g2, b2, a2 = overlay
    norm_a = (a2 / 255.0) * alpha_factor
    nr = int(r1 * (1.0 - norm_a) + r2 * norm_a)
    ng = int(g1 * (1.0 - norm_a) + g2 * norm_a)
    nb = int(b1 * (1.0 - norm_a) + b2 * norm_a)
    return (max(0, min(255, nr)), max(0, min(255, ng)), max(0, min(255, nb)), a1)

def save_png_64x64(filepath, pixels):
    raw = bytearray()
    for y in range(64):
        raw.append(0)  # Filter type: None
        for x in range(64):
            r, g, b, a = pixels[y][x]
            raw.extend((r, g, b, a))

    def make_chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", 64, 64, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)
    png_bytes = b"\x89PNG\r\n\x1a\n" + make_chunk(b"IHDR", ihdr) + make_chunk(b"IDAT", idat) + make_chunk(b"IEND", b"")

    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, "wb") as f:
        f.write(png_bytes)

# ==============================================================================
# 2. 2D UV PACKER (STRICT NON-OVERLAPPING IN 64x64)
# ==============================================================================

def pack_model_cubes(bones_def):
    """
    Packs all cubes from bone definitions into a 64x64 texture map without overlap.
    Returns: dict mapping cube_key -> (u, v)
    """
    cubes_list = []
    for b in bones_def:
        bname = b["name"]
        for idx, c in enumerate(b.get("cubes", [])):
            sx, sy, sz = c["size"]
            w = int(math.ceil(2 * (sx + sz)))
            h = int(math.ceil(sz + sy))
            ckey = f"{bname}_{idx}"
            cubes_list.append({
                "key": ckey,
                "w": w,
                "h": h,
                "bone": bname,
                "cube_idx": idx,
                "size": [int(math.ceil(sx)), int(math.ceil(sy)), int(math.ceil(sz))]
            })

    # Sort descending by area and height
    cubes_list.sort(key=lambda item: (item["w"] * item["h"], item["h"], item["w"]), reverse=True)

    grid = [[False for _ in range(64)] for _ in range(64)]
    packed = {}

    for c in cubes_list:
        w, h = c["w"], c["h"]
        placed = False
        for y in range(64 - h + 1):
            for x in range(64 - w + 1):
                can_place = True
                for py in range(y, y + h):
                    if any(grid[py][x:x+w]):
                        can_place = False
                        break
                if can_place:
                    for py in range(y, y + h):
                        for px in range(x, x + w):
                            grid[py][px] = True
                    packed[c["key"]] = (x, y)
                    placed = True
                    break
            if placed:
                break
        if not placed:
            raise ValueError(f"Could not fit cube {c['key']} ({w}x{h}) into 64x64 texture!")

    return packed

# ==============================================================================
# 3. 12 CANONICAL FFXII ENTITIES DEFINITIONS (MODELS, BONES, ANIMATIONS, TEXTURES)
# ==============================================================================

ENTITIES_DATA = {
    # --------------------------------------------------------------------------
    # 1. Dalmasca Wolf
    # --------------------------------------------------------------------------
    "wolf": {
        "theme_color": "#2c1c13",
        "colors": {
            "base": "#b89058", "shadow": "#7a542b", "highlight": "#d8b27d",
            "belly": "#edd8ba", "muzzle": "#382318", "eye": "#f59e0b",
            "eye_pupil": "#18181b", "nose": "#1c1917"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 8, 0],
             "cubes": [{"origin": [-3, 5, -6], "size": [6, 7, 12]}]},
            {"name": "neck", "parent": "body", "pivot": [0, 9, -5],
             "cubes": [{"origin": [-2, 7, -8], "size": [4, 5, 4]}]},
            {"name": "head", "parent": "neck", "pivot": [0, 10, -7],
             "cubes": [
                 {"origin": [-2.5, 8.5, -11], "size": [5, 5, 5]},
                 {"origin": [-1.5, 8.5, -14.5], "size": [3, 2.5, 4]}  # snout
             ]},
            {"name": "ear_left", "parent": "head", "pivot": [1.5, 13.5, -8.5],
             "cubes": [{"origin": [1.2, 13.5, -8.5], "size": [1.5, 3, 1]}]},
            {"name": "ear_right", "parent": "head", "pivot": [-1.5, 13.5, -8.5],
             "cubes": [{"origin": [-2.7, 13.5, -8.5], "size": [1.5, 3, 1]}]},
            {"name": "tail", "parent": "body", "pivot": [0, 11, 5],
             "cubes": [{"origin": [-1.25, 9, 5], "size": [2.5, 2.5, 8]}]},
            {"name": "leg_fl", "parent": "root", "pivot": [2, 7, -4],
             "cubes": [{"origin": [1, 0, -5], "size": [2, 7, 2]}]},
            {"name": "leg_fr", "parent": "root", "pivot": [-2, 7, -4],
             "cubes": [{"origin": [-3, 0, -5], "size": [2, 7, 2]}]},
            {"name": "leg_bl", "parent": "root", "pivot": [2, 7, 4],
             "cubes": [{"origin": [1, 0, 3], "size": [2, 7, 2]}]},
            {"name": "leg_br", "parent": "root", "pivot": [-2, 7, 4],
             "cubes": [{"origin": [-3, 0, 3], "size": [2, 7, 2]}]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "body": {"position": {"0.0": [0, 0, 0], "1.0": [0, -0.3, 0], "2.0": [0, 0, 0]}},
                    "head": {"rotation": {"0.0": [0, 0, 0], "0.6": [-4, 3, 0], "1.3": [2, -3, 0], "2.0": [0, 0, 0]}},
                    "tail": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 14, 0], "1.0": [0, 0, 0], "1.5": [0, -14, 0], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "leg_fl": {"rotation": {"0.0": [0, 0, 0], "0.25": [26, 0, 0], "0.5": [0, 0, 0], "0.75": [-26, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_fr": {"rotation": {"0.0": [0, 0, 0], "0.25": [-26, 0, 0], "0.5": [0, 0, 0], "0.75": [26, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_bl": {"rotation": {"0.0": [0, 0, 0], "0.25": [-26, 0, 0], "0.5": [0, 0, 0], "0.75": [26, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_br": {"rotation": {"0.0": [0, 0, 0], "0.25": [26, 0, 0], "0.5": [0, 0, 0], "0.75": [-26, 0, 0], "1.0": [0, 0, 0]}},
                    "body": {"position": {"0.0": [0, 0, 0], "0.25": [0, 0.4, 0], "0.5": [0, 0, 0], "0.75": [0, 0.4, 0], "1.0": [0, 0, 0]}},
                    "tail": {"rotation": {"0.0": [0, 0, 0], "0.25": [0, -16, 0], "0.5": [0, 0, 0], "0.75": [0, 16, 0], "1.0": [0, 0, 0]}}
                }
            }
        }
    },

    # --------------------------------------------------------------------------
    # 2. Cactoid
    # --------------------------------------------------------------------------
    "cactoid": {
        "theme_color": "#064e3b",
        "colors": {
            "base": "#22c55e", "shadow": "#15803d", "highlight": "#4ade80",
            "ribs": "#14532d", "spines": "#fef08a", "flower": "#facc15",
            "eye": "#09090b", "eye_pupil": "#09090b"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 7, 0],
             "cubes": [
                 {"origin": [-3, 6, -3], "size": [6, 12, 6]},
                 {"origin": [-3.5, 8, -2.5], "size": [7, 8, 5]}
             ]},
            {"name": "arm_left", "parent": "body", "pivot": [3, 13, 0],
             "cubes": [
                 {"origin": [3, 11.5, -1.5], "size": [4, 3, 3]},
                 {"origin": [5, 13.5, -1.5], "size": [3, 6, 3]}
             ]},
            {"name": "arm_right", "parent": "body", "pivot": [-3, 11, 0],
             "cubes": [
                 {"origin": [-7, 9.5, -1.5], "size": [4, 3, 3]},
                 {"origin": [-7, 5.5, -1.5], "size": [3, 6, 3]}
             ]},
            {"name": "flower", "parent": "body", "pivot": [0, 18, 0],
             "cubes": [{"origin": [-1.5, 18, -1.5], "size": [3, 3, 3]}]},
            {"name": "leg_left", "parent": "root", "pivot": [1.75, 6, 0],
             "cubes": [{"origin": [0.5, 0, -1.5], "size": [2.5, 6, 3]}]},
            {"name": "leg_right", "parent": "root", "pivot": [-1.75, 6, 0],
             "cubes": [{"origin": [-3, 0, -1.5], "size": [2.5, 6, 3]}]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "body": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 2], "2.0": [0, 0, 0]}},
                    "arm_left": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 6], "2.0": [0, 0, 0]}},
                    "arm_right": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, -6], "2.0": [0, 0, 0]}},
                    "flower": {"rotation": {"0.0": [0, 0, 0], "0.5": [4, 0, 4], "1.5": [-4, 0, -4], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "leg_left": {"rotation": {"0.0": [0, 0, 0], "0.25": [36, 0, 0], "0.5": [0, 0, 0], "0.75": [-36, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_right": {"rotation": {"0.0": [0, 0, 0], "0.25": [-36, 0, 0], "0.5": [0, 0, 0], "0.75": [36, 0, 0], "1.0": [0, 0, 0]}},
                    "arm_left": {"rotation": {"0.0": [0, 0, 0], "0.25": [-25, 0, 0], "0.5": [0, 0, 0], "0.75": [25, 0, 0], "1.0": [0, 0, 0]}},
                    "arm_right": {"rotation": {"0.0": [0, 0, 0], "0.25": [25, 0, 0], "0.5": [0, 0, 0], "0.75": [-25, 0, 0], "1.0": [0, 0, 0]}},
                    "body": {"position": {"0.0": [0, 0, 0], "0.25": [0, 0.6, 0], "0.5": [0, 0, 0], "0.75": [0, 0.6, 0], "1.0": [0, 0, 0]}}
                }
            }
        }
    },

    # --------------------------------------------------------------------------
    # 3. Giza Hyena
    # --------------------------------------------------------------------------
    "hyena": {
        "theme_color": "#261a12",
        "colors": {
            "base": "#d97706", "shadow": "#92400e", "highlight": "#f59e0b",
            "spots": "#451a03", "mane": "#292524", "eye": "#eab308",
            "eye_pupil": "#18181b", "muzzle": "#1c1917"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 8, 0],
             "cubes": [
                 {"origin": [-3.5, 6.5, -6], "size": [7, 8, 7]},
                 {"origin": [-3, 6, 1], "size": [6, 6, 6]},
                 {"origin": [-1, 13, -5], "size": [2, 3, 9]}  # mane
             ]},
            {"name": "neck", "parent": "body", "pivot": [0, 10, -5],
             "cubes": [{"origin": [-2.25, 8, -8], "size": [4.5, 5, 4]}]},
            {"name": "head", "parent": "neck", "pivot": [0, 11, -7],
             "cubes": [
                 {"origin": [-2.75, 9.5, -11], "size": [5.5, 5, 5]},
                 {"origin": [-1.75, 9.5, -14.5], "size": [3.5, 3, 4]}  # snout
             ]},
            {"name": "ear_left", "parent": "head", "pivot": [1.5, 14, -8.5],
             "cubes": [{"origin": [1.5, 14, -8.5], "size": [2, 3, 1]}]},
            {"name": "ear_right", "parent": "head", "pivot": [-1.5, 14, -8.5],
             "cubes": [{"origin": [-3.5, 14, -8.5], "size": [2, 3, 1]}]},
            {"name": "tail", "parent": "body", "pivot": [0, 10, 6.5],
             "cubes": [{"origin": [-1, 4.5, 6], "size": [2, 6, 2]}]},
            {"name": "leg_fl", "parent": "root", "pivot": [2.5, 7.5, -4],
             "cubes": [{"origin": [1.25, 0, -5.25], "size": [2.5, 8, 2.5]}]},
            {"name": "leg_fr", "parent": "root", "pivot": [-2.5, 7.5, -4],
             "cubes": [{"origin": [-3.75, 0, -5.25], "size": [2.5, 8, 2.5]}]},
            {"name": "leg_bl", "parent": "root", "pivot": [2.25, 6, 4.5],
             "cubes": [{"origin": [1, 0, 3.25], "size": [2.5, 6.5, 2.5]}]},
            {"name": "leg_br", "parent": "root", "pivot": [-2.25, 6, 4.5],
             "cubes": [{"origin": [-3.5, 0, 3.25], "size": [2.5, 6.5, 2.5]}]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "body": {"position": {"0.0": [0, 0, 0], "1.0": [0, -0.4, 0], "2.0": [0, 0, 0]}},
                    "head": {"rotation": {"0.0": [0, 0, 0], "0.7": [5, 4, 0], "1.4": [3, -4, 0], "2.0": [0, 0, 0]}},
                    "tail": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 10, 0], "1.0": [0, 0, 0], "1.5": [0, -10, 0], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "leg_fl": {"rotation": {"0.0": [0, 0, 0], "0.25": [28, 0, 0], "0.5": [0, 0, 0], "0.75": [-28, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_fr": {"rotation": {"0.0": [0, 0, 0], "0.25": [-28, 0, 0], "0.5": [0, 0, 0], "0.75": [28, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_bl": {"rotation": {"0.0": [0, 0, 0], "0.25": [-24, 0, 0], "0.5": [0, 0, 0], "0.75": [24, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_br": {"rotation": {"0.0": [0, 0, 0], "0.25": [24, 0, 0], "0.5": [0, 0, 0], "0.75": [-24, 0, 0], "1.0": [0, 0, 0]}},
                    "body": {"position": {"0.0": [0, 0, 0], "0.25": [0, 0.5, 0], "0.5": [0, 0, 0], "0.75": [0, 0.5, 0], "1.0": [0, 0, 0]}}
                }
            }
        }
    },

    # --------------------------------------------------------------------------
    # 4. Giza Rabbit
    # --------------------------------------------------------------------------
    "giza_rabbit": {
        "theme_color": "#e7e5e4",
        "colors": {
            "base": "#fafaf9", "shadow": "#d6d3d1", "highlight": "#ffffff",
            "ear_inner": "#f472b6", "nose": "#db2777", "eye": "#e11d48",
            "eye_pupil": "#881337"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 3.5, 0],
             "cubes": [{"origin": [-2.5, 2, -3], "size": [5, 4.5, 6.5]}]},
            {"name": "head", "parent": "body", "pivot": [0, 5, -2.5],
             "cubes": [
                 {"origin": [-2.25, 4, -6.5], "size": [4.5, 4, 4.5]},
                 {"origin": [-1, 4, -7.5], "size": [2, 1.5, 1.5]}
             ]},
            {"name": "ear_left", "parent": "head", "pivot": [1, 7.5, -5],
             "cubes": [{"origin": [0.75, 7.5, -5], "size": [1.5, 6, 1]}]},
            {"name": "ear_right", "parent": "head", "pivot": [-1, 7.5, -5],
             "cubes": [{"origin": [-2.25, 7.5, -5], "size": [1.5, 6, 1]}]},
            {"name": "tail", "parent": "body", "pivot": [0, 4, 3],
             "cubes": [{"origin": [-1.25, 3, 3], "size": [2.5, 2.5, 2.5]}]},
            {"name": "leg_fl", "parent": "root", "pivot": [1.5, 2.5, -2],
             "cubes": [{"origin": [0.75, 0, -2.5], "size": [1.5, 3, 1.5]}]},
            {"name": "leg_fr", "parent": "root", "pivot": [-1.5, 2.5, -2],
             "cubes": [{"origin": [-2.25, 0, -2.5], "size": [1.5, 3, 1.5]}]},
            {"name": "leg_bl", "parent": "root", "pivot": [1.75, 3, 1.5],
             "cubes": [
                 {"origin": [1, 1, 0.5], "size": [2, 3, 3]},
                 {"origin": [1, 0, -0.5], "size": [2, 1.5, 3.5]}
             ]},
            {"name": "leg_br", "parent": "root", "pivot": [-1.75, 3, 1.5],
             "cubes": [
                 {"origin": [-3, 1, 0.5], "size": [2, 3, 3]},
                 {"origin": [-3, 0, -0.5], "size": [2, 1.5, 3.5]}
             ]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "head": {"rotation": {"0.0": [0, 0, 0], "0.5": [-3, 6, 0], "1.0": [0, 0, 0], "1.5": [3, -6, 0], "2.0": [0, 0, 0]}},
                    "ear_left": {"rotation": {"0.0": [0, 0, 0], "0.6": [0, 0, 10], "1.2": [0, 0, -6], "2.0": [0, 0, 0]}},
                    "ear_right": {"rotation": {"0.0": [0, 0, 0], "0.8": [0, 0, -10], "1.4": [0, 0, 6], "2.0": [0, 0, 0]}},
                    "tail": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 12, 0], "1.0": [0, 0, 0], "1.5": [0, -12, 0], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "body": {
                        "position": {"0.0": [0, 0, 0], "0.3": [0, 1.5, 0], "0.6": [0, 0, 0], "1.0": [0, 0, 0]},
                        "rotation": {"0.0": [0, 0, 0], "0.3": [-15, 0, 0], "0.6": [10, 0, 0], "1.0": [0, 0, 0]}
                    },
                    "leg_fl": {"rotation": {"0.0": [0, 0, 0], "0.3": [-25, 0, 0], "0.6": [22, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_fr": {"rotation": {"0.0": [0, 0, 0], "0.3": [-25, 0, 0], "0.6": [22, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_bl": {"rotation": {"0.0": [0, 0, 0], "0.3": [35, 0, 0], "0.6": [-20, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_br": {"rotation": {"0.0": [0, 0, 0], "0.3": [35, 0, 0], "0.6": [-20, 0, 0], "1.0": [0, 0, 0]}}
                }
            }
        }
    },

    # --------------------------------------------------------------------------
    # 5. Cockatrice
    # --------------------------------------------------------------------------
    "cockatrice": {
        "theme_color": "#78350f",
        "colors": {
            "base": "#d4a373", "shadow": "#8d5b2c", "highlight": "#faedcd",
            "crest": "#dc2626", "wattle": "#ef4444", "beak": "#ea580c",
            "talons": "#ca8a04", "eye": "#facc15", "eye_pupil": "#18181b"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 7, 0],
             "cubes": [{"origin": [-4, 4, -4.5], "size": [8, 8, 9]}]},
            {"name": "head", "parent": "body", "pivot": [0, 9, -4],
             "cubes": [
                 {"origin": [-2.5, 7.5, -7.5], "size": [5, 5, 5]},
                 {"origin": [-1.25, 7.8, -10], "size": [2.5, 2, 3]},  # beak
                 {"origin": [-0.5, 12, -7.5], "size": [1, 4, 4]},     # crest
                 {"origin": [-0.75, 6, -7], "size": [1.5, 2.5, 1.5]}  # wattle
             ]},
            {"name": "wing_left", "parent": "body", "pivot": [4, 8, -1],
             "cubes": [{"origin": [4, 5.5, -3], "size": [1.5, 4.5, 6]}]},
            {"name": "wing_right", "parent": "body", "pivot": [-4, 8, -1],
             "cubes": [{"origin": [-5.5, 5.5, -3], "size": [1.5, 4.5, 6]}]},
            {"name": "tail", "parent": "body", "pivot": [0, 8, 4.5],
             "cubes": [{"origin": [-2, 6.5, 4], "size": [4, 3, 4]}]},
            {"name": "leg_left", "parent": "root", "pivot": [2, 4, 0],
             "cubes": [{"origin": [1, 0, -1.5], "size": [2, 4, 3]}]},
            {"name": "leg_right", "parent": "root", "pivot": [-2, 4, 0],
             "cubes": [{"origin": [-3, 0, -1.5], "size": [2, 4, 3]}]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "head": {"rotation": {"0.0": [0, 0, 0], "0.4": [12, 0, 0], "0.7": [0, 0, 0], "1.3": [-8, 6, 0], "2.0": [0, 0, 0]}},
                    "crest": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 0, 6], "1.5": [0, 0, -6], "2.0": [0, 0, 0]}},
                    "wing_left": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 10], "2.0": [0, 0, 0]}},
                    "wing_right": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, -10], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "body": {
                        "rotation": {"0.0": [0, 0, 0], "0.25": [0, 0, 6], "0.5": [0, 0, 0], "0.75": [0, 0, -6], "1.0": [0, 0, 0]},
                        "position": {"0.0": [0, 0, 0], "0.25": [0, 0.4, 0], "0.5": [0, 0, 0], "0.75": [0, 0.4, 0], "1.0": [0, 0, 0]}
                    },
                    "leg_left": {"rotation": {"0.0": [0, 0, 0], "0.25": [32, 0, 0], "0.5": [0, 0, 0], "0.75": [-32, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_right": {"rotation": {"0.0": [0, 0, 0], "0.25": [-32, 0, 0], "0.5": [0, 0, 0], "0.75": [32, 0, 0], "1.0": [0, 0, 0]}},
                    "wing_left": {"rotation": {"0.0": [0, 0, 0], "0.25": [0, 0, 32], "0.5": [0, 0, -10], "0.75": [0, 0, 32], "1.0": [0, 0, 0]}},
                    "wing_right": {"rotation": {"0.0": [0, 0, 0], "0.25": [0, 0, -32], "0.5": [0, 0, 10], "0.75": [0, 0, -32], "1.0": [0, 0, 0]}}
                }
            }
        }
    },

    # --------------------------------------------------------------------------
    # 6. Ichthon
    # --------------------------------------------------------------------------
    "ichthon": {
        "theme_color": "#083344",
        "colors": {
            "base": "#06b6d4", "shadow": "#0e7490", "highlight": "#67e8f9",
            "dorsal": "#0c4a6e", "belly": "#cffafe", "fins": "#38bdf8",
            "eye": "#facc15", "eye_pupil": "#0f172a"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 8, 0],
             "cubes": [
                 {"origin": [-2.5, 5, -5], "size": [5, 7, 10]},
                 {"origin": [-0.5, 12, -3], "size": [1, 4, 7]},   # dorsal fin
                 {"origin": [-0.5, 2, -1], "size": [1, 3, 5]}     # ventral fin
             ]},
            {"name": "head", "parent": "body", "pivot": [0, 8, -5],
             "cubes": [
                 {"origin": [-2.25, 5.5, -9.5], "size": [4.5, 6, 5]},
                 {"origin": [-1.75, 4.5, -9], "size": [3.5, 2, 4.5]}  # jaw
             ]},
            {"name": "fin_left", "parent": "body", "pivot": [2.5, 7, -2],
             "cubes": [{"origin": [2.5, 6.5, -3], "size": [5, 1, 3]}]},
            {"name": "fin_right", "parent": "body", "pivot": [-2.5, 7, -2],
             "cubes": [{"origin": [-7.5, 6.5, -3], "size": [5, 1, 3]}]},
            {"name": "tail_1", "parent": "body", "pivot": [0, 8, 5],
             "cubes": [{"origin": [-1.75, 6, 5], "size": [3.5, 5, 5]}]},
            {"name": "tail_2", "parent": "tail_1", "pivot": [0, 8, 10],
             "cubes": [{"origin": [-0.5, 5, 10], "size": [1, 7, 5]}]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "body": {"position": {"0.0": [0, 0, 0], "1.0": [0, 0.8, 0], "2.0": [0, 0, 0]}},
                    "tail_1": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 10, 0], "1.0": [0, 0, 0], "1.5": [0, -10, 0], "2.0": [0, 0, 0]}},
                    "tail_2": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 18, 0], "1.0": [0, 0, 0], "1.5": [0, -18, 0], "2.0": [0, 0, 0]}},
                    "fin_left": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 12], "2.0": [0, 0, 0]}},
                    "fin_right": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, -12], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "body": {
                        "position": {"0.0": [0, 0, 0], "0.25": [0, 0.4, 0], "0.5": [0, 0, 0], "0.75": [0, 0.4, 0], "1.0": [0, 0, 0]},
                        "rotation": {"0.0": [0, 0, 0], "0.25": [0, -6, 0], "0.5": [0, 0, 0], "0.75": [0, 6, 0], "1.0": [0, 0, 0]}
                    },
                    "tail_1": {"rotation": {"0.0": [0, 0, 0], "0.25": [0, 22, 0], "0.5": [0, 0, 0], "0.75": [0, -22, 0], "1.0": [0, 0, 0]}},
                    "tail_2": {"rotation": {"0.0": [0, 0, 0], "0.25": [0, -32, 0], "0.5": [0, 0, 0], "0.75": [0, 32, 0], "1.0": [0, 0, 0]}},
                    "fin_left": {"rotation": {"0.0": [0, 0, 0], "0.25": [0, 0, 25], "0.5": [0, 0, -10], "0.75": [0, 0, 25], "1.0": [0, 0, 0]}},
                    "fin_right": {"rotation": {"0.0": [0, 0, 0], "0.25": [0, 0, -25], "0.5": [0, 0, 10], "0.75": [0, 0, -25], "1.0": [0, 0, 0]}}
                }
            }
        }
    },

    # --------------------------------------------------------------------------
    # 7. Rogue Tomato [Elite Mark Rank I]
    # --------------------------------------------------------------------------
    "rogue_tomato": {
        "theme_color": "#450a0a",
        "colors": {
            "base": "#dc2626", "shadow": "#991b1b", "highlight": "#f87171",
            "crown": "#15803d", "leaf": "#22c55e", "eye": "#facc15",
            "eye_pupil": "#7f1d1d", "mouth": "#450a0a"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 9, 0],
             "cubes": [{"origin": [-5, 5, -5], "size": [10, 9, 10]}]},
            {"name": "crown", "parent": "body", "pivot": [0, 14, 0],
             "cubes": [
                 {"origin": [-1, 14, -1], "size": [2, 4, 2]},
                 {"origin": [1, 14, -1], "size": [4, 1, 2]},
                 {"origin": [-5, 14, -1], "size": [4, 1, 2]},
                 {"origin": [-1, 14, 1], "size": [2, 1, 4]},
                 {"origin": [-1, 14, -5], "size": [2, 1, 4]}
             ]},
            {"name": "arm_left", "parent": "body", "pivot": [5, 9, 0],
             "cubes": [{"origin": [5, 8, -1], "size": [3, 2, 2]}]},
            {"name": "arm_right", "parent": "body", "pivot": [-5, 9, 0],
             "cubes": [{"origin": [-8, 8, -1], "size": [3, 2, 2]}]},
            {"name": "leg_left", "parent": "root", "pivot": [2.5, 5, 0],
             "cubes": [{"origin": [1, 0, -1.5], "size": [3, 5, 3]}]},
            {"name": "leg_right", "parent": "root", "pivot": [-2.5, 5, 0],
             "cubes": [{"origin": [-4, 0, -1.5], "size": [3, 5, 3]}]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "body": {
                        "position": {"0.0": [0, 0, 0], "1.0": [0, -0.4, 0], "2.0": [0, 0, 0]},
                        "scale": {"0.0": [1, 1, 1], "1.0": [1.05, 0.95, 1.05], "2.0": [1, 1, 1]}
                    },
                    "crown": {"rotation": {"0.0": [0, 0, 0], "1.0": [-4, 0, 0], "2.0": [0, 0, 0]}},
                    "arm_left": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 16], "2.0": [0, 0, 0]}},
                    "arm_right": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, -16], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "body": {
                        "position": {"0.0": [0, 0, 0], "0.25": [0, 1.2, 0], "0.5": [0, 0, 0], "0.75": [0, 1.2, 0], "1.0": [0, 0, 0]},
                        "rotation": {"0.0": [0, 0, 0], "0.25": [8, 0, 0], "0.5": [0, 0, 0], "0.75": [8, 0, 0], "1.0": [0, 0, 0]}
                    },
                    "leg_left": {"rotation": {"0.0": [0, 0, 0], "0.25": [36, 0, 0], "0.5": [0, 0, 0], "0.75": [-36, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_right": {"rotation": {"0.0": [0, 0, 0], "0.25": [-36, 0, 0], "0.5": [0, 0, 0], "0.75": [36, 0, 0], "1.0": [0, 0, 0]}},
                    "crown": {"rotation": {"0.0": [0, 0, 0], "0.25": [-16, 0, 0], "0.5": [6, 0, 0], "0.75": [-16, 0, 0], "1.0": [0, 0, 0]}}
                }
            }
        }
    },

    # --------------------------------------------------------------------------
    # 8. Thextera [Elite Mark Rank I]
    # --------------------------------------------------------------------------
    "thextera": {
        "theme_color": "#18181b",
        "colors": {
            "base": "#27272a", "shadow": "#18181b", "highlight": "#52525b",
            "mane": "#facc15", "mane_shadow": "#b45309", "eye": "#fbbf24",
            "eye_pupil": "#78350f", "teeth": "#f4f4f5"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 9, 0],
             "cubes": [
                 {"origin": [-4, 5.5, -7], "size": [8, 8, 14]},
                 {"origin": [-1, 13.5, -6], "size": [2, 3, 11]}  # spine spikes
             ]},
            {"name": "neck", "parent": "body", "pivot": [0, 10.5, -6],
             "cubes": [
                 {"origin": [-2.5, 8.5, -10], "size": [5, 5, 5]},
                 {"origin": [-3.5, 7.5, -9.5], "size": [7, 7, 5]}  # mane ruff
             ]},
            {"name": "head", "parent": "neck", "pivot": [0, 11.5, -9],
             "cubes": [
                 {"origin": [-3, 9.5, -14], "size": [6, 5.5, 6]},
                 {"origin": [-1.75, 9.5, -18], "size": [3.5, 3, 4.5]}  # snout
             ]},
            {"name": "ear_left", "parent": "head", "pivot": [1.5, 14.5, -11],
             "cubes": [{"origin": [1.5, 14.5, -11], "size": [2, 3.5, 1]}]},
            {"name": "ear_right", "parent": "head", "pivot": [-1.5, 14.5, -11],
             "cubes": [{"origin": [-3.5, 14.5, -11], "size": [2, 3.5, 1]}]},
            {"name": "tail", "parent": "body", "pivot": [0, 12, 6.5],
             "cubes": [{"origin": [-1.5, 9.5, 6.5], "size": [3, 3, 9]}]},
            {"name": "leg_fl", "parent": "root", "pivot": [3, 8, -4.5],
             "cubes": [{"origin": [1.5, 0, -6], "size": [3, 8, 3]}]},
            {"name": "leg_fr", "parent": "root", "pivot": [-3, 8, -4.5],
             "cubes": [{"origin": [-4.5, 0, -6], "size": [3, 8, 3]}]},
            {"name": "leg_bl", "parent": "root", "pivot": [3, 8, 5],
             "cubes": [{"origin": [1.5, 0, 3.5], "size": [3, 8, 3]}]},
            {"name": "leg_br", "parent": "root", "pivot": [-3, 8, 5],
             "cubes": [{"origin": [-4.5, 0, 3.5], "size": [3, 8, 3]}]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "body": {"position": {"0.0": [0, 0, 0], "1.0": [0, -0.4, 0], "2.0": [0, 0, 0]}},
                    "head": {"rotation": {"0.0": [0, 0, 0], "0.7": [-5, 4, 0], "1.4": [3, -4, 0], "2.0": [0, 0, 0]}},
                    "tail": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 15, 0], "1.0": [0, 0, 0], "1.5": [0, -15, 0], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "leg_fl": {"rotation": {"0.0": [0, 0, 0], "0.25": [28, 0, 0], "0.5": [0, 0, 0], "0.75": [-28, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_fr": {"rotation": {"0.0": [0, 0, 0], "0.25": [-28, 0, 0], "0.5": [0, 0, 0], "0.75": [28, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_bl": {"rotation": {"0.0": [0, 0, 0], "0.25": [-28, 0, 0], "0.5": [0, 0, 0], "0.75": [28, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_br": {"rotation": {"0.0": [0, 0, 0], "0.25": [28, 0, 0], "0.5": [0, 0, 0], "0.75": [-28, 0, 0], "1.0": [0, 0, 0]}},
                    "body": {"position": {"0.0": [0, 0, 0], "0.25": [0, 0.5, 0], "0.5": [0, 0, 0], "0.75": [0, 0.5, 0], "1.0": [0, 0, 0]}},
                    "tail": {"rotation": {"0.0": [0, 0, 0], "0.25": [0, -18, 0], "0.5": [0, 0, 0], "0.75": [0, 18, 0], "1.0": [0, 0, 0]}}
                }
            }
        }
    },

    # --------------------------------------------------------------------------
    # 9. Flowering Cactoid [Elite Mark Rank I]
    # --------------------------------------------------------------------------
    "flowering_cactoid": {
        "theme_color": "#064e3b",
        "colors": {
            "base": "#10b981", "shadow": "#047857", "highlight": "#34d399",
            "flower": "#e11d48", "flower_light": "#fb7185", "stamen": "#fbbf24",
            "spines": "#fef08a", "eye": "#09090b", "eye_pupil": "#09090b"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 8, 0],
             "cubes": [{"origin": [-3.5, 7, -3.5], "size": [7, 14, 7]}]},
            {"name": "flower", "parent": "body", "pivot": [0, 21, 0],
             "cubes": [
                 {"origin": [-1.5, 21, -1.5], "size": [3, 2, 3]},
                 {"origin": [-2, 21.5, -4.5], "size": [4, 1.5, 3]},
                 {"origin": [-2, 21.5, 1.5], "size": [4, 1.5, 3]},
                 {"origin": [1.5, 21.5, -2], "size": [3, 1.5, 4]},
                 {"origin": [-4.5, 21.5, -2], "size": [3, 1.5, 4]}
             ]},
            {"name": "arm_left", "parent": "body", "pivot": [3.5, 15, 0],
             "cubes": [
                 {"origin": [3.5, 13.5, -1.75], "size": [5, 3.5, 3.5]},
                 {"origin": [6, 16, -1.75], "size": [3.5, 7, 3.5]}
             ]},
            {"name": "arm_right", "parent": "body", "pivot": [-3.5, 13, 0],
             "cubes": [
                 {"origin": [-8.5, 11.5, -1.75], "size": [5, 3.5, 3.5]},
                 {"origin": [-8.5, 6, -1.75], "size": [3.5, 7, 3.5]}
             ]},
            {"name": "leg_left", "parent": "root", "pivot": [2.25, 7, 0],
             "cubes": [{"origin": [0.75, 0, -1.5], "size": [3, 7, 3]}]},
            {"name": "leg_right", "parent": "root", "pivot": [-2.25, 7, 0],
             "cubes": [{"origin": [-3.75, 0, -1.5], "size": [3, 7, 3]}]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "body": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 3], "2.0": [0, 0, 0]}},
                    "flower": {"rotation": {"0.0": [0, 0, 0], "0.5": [6, 0, 6], "1.0": [0, 0, 0], "1.5": [-6, 0, -6], "2.0": [0, 0, 0]}},
                    "arm_left": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 8], "2.0": [0, 0, 0]}},
                    "arm_right": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, -8], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "leg_left": {"rotation": {"0.0": [0, 0, 0], "0.25": [38, 0, 0], "0.5": [0, 0, 0], "0.75": [-38, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_right": {"rotation": {"0.0": [0, 0, 0], "0.25": [-38, 0, 0], "0.5": [0, 0, 0], "0.75": [38, 0, 0], "1.0": [0, 0, 0]}},
                    "arm_left": {"rotation": {"0.0": [0, 0, 0], "0.25": [-25, 0, 0], "0.5": [0, 0, 0], "0.75": [25, 0, 0], "1.0": [0, 0, 0]}},
                    "arm_right": {"rotation": {"0.0": [0, 0, 0], "0.25": [25, 0, 0], "0.5": [0, 0, 0], "0.75": [-25, 0, 0], "1.0": [0, 0, 0]}},
                    "body": {"position": {"0.0": [0, 0, 0], "0.25": [0, 0.7, 0], "0.5": [0, 0, 0], "0.75": [0, 0.7, 0], "1.0": [0, 0, 0]}},
                    "flower": {"rotation": {"0.0": [0, 0, 0], "0.25": [-12, 0, 0], "0.5": [0, 0, 0], "0.75": [12, 0, 0], "1.0": [0, 0, 0]}}
                }
            }
        }
    },

    # --------------------------------------------------------------------------
    # 10. White Mousse [Elite Mark Rank V]
    # --------------------------------------------------------------------------
    "white_mousse": {
        "theme_color": "#0c4a6e",
        "colors": {
            "base": "#f8fafc", "shadow": "#bae6fd", "highlight": "#ffffff",
            "glaze": "#7dd3fc", "eye": "#0284c7", "eye_pupil": "#0369a1",
            "core": "#38bdf8"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 6, 0],
             "cubes": [
                 {"origin": [-7.5, 0, -7.5], "size": [15, 4, 15]},
                 {"origin": [-6, 4, -6], "size": [12, 6, 12]},
                 {"origin": [-4.5, 10, -4.5], "size": [9, 5, 9]}
             ]},
            {"name": "crown", "parent": "body", "pivot": [0, 15, 0],
             "cubes": [{"origin": [-2, 15, -2], "size": [4, 4, 4]}]},
            {"name": "eye_left", "parent": "body", "pivot": [3, 8.5, -6],
             "cubes": [{"origin": [1.75, 7.5, -6.5], "size": [2.5, 2.5, 1]}]},
            {"name": "eye_right", "parent": "body", "pivot": [-3, 8.5, -6],
             "cubes": [{"origin": [-4.25, 7.5, -6.5], "size": [2.5, 2.5, 1]}]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "body": {
                        "position": {"0.0": [0, 0, 0], "1.0": [0, -0.6, 0], "2.0": [0, 0, 0]},
                        "scale": {"0.0": [1, 1, 1], "1.0": [1.06, 0.92, 1.06], "2.0": [1, 1, 1]}
                    },
                    "crown": {"rotation": {"0.0": [0, 0, 0], "0.5": [6, 0, 6], "1.5": [-6, 0, -6], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "body": {
                        "position": {"0.0": [0, 0, 0], "0.3": [0, 1.4, 0], "0.7": [0, 0, 0], "1.0": [0, 0, 0]},
                        "scale": {"0.0": [1, 1, 1], "0.3": [0.92, 1.15, 0.92], "0.7": [1.12, 0.88, 1.12], "1.0": [1, 1, 1]}
                    },
                    "crown": {"rotation": {"0.0": [0, 0, 0], "0.3": [-16, 0, 0], "0.7": [10, 0, 0], "1.0": [0, 0, 0]}}
                }
            }
        }
    },

    # --------------------------------------------------------------------------
    # 11. Belias, the Gigas [Esper Rank S]
    # --------------------------------------------------------------------------
    "belias": {
        "theme_color": "#1c1917",
        "colors": {
            "base": "#b91c1c", "shadow": "#7f1d1d", "highlight": "#ef4444",
            "armor": "#18181b", "armor_light": "#27272a", "magma": "#f59e0b",
            "horns": "#ca8a04", "eye": "#fef08a", "eye_pupil": "#d97706"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 16, 0],
             "cubes": [
                 {"origin": [-6, 16, -4], "size": [12, 9, 8]},
                 {"origin": [-4, 9, -3], "size": [8, 7, 6]}
             ]},
            {"name": "head", "parent": "body", "pivot": [0, 25, -1],
             "cubes": [
                 {"origin": [-3, 25, -4], "size": [6, 6, 6]},
                 {"origin": [-2, 25, -6.5], "size": [4, 3, 3]}  # jaw
             ]},
            {"name": "horn_left", "parent": "head", "pivot": [3, 29, -2],
             "cubes": [{"origin": [3, 29, -2.5], "size": [3, 7, 3]}]},
            {"name": "horn_right", "parent": "head", "pivot": [-3, 29, -2],
             "cubes": [{"origin": [-6, 29, -2.5], "size": [3, 7, 3]}]},
            {"name": "shoulder_left", "parent": "body", "pivot": [6.5, 23, 0],
             "cubes": [{"origin": [6, 21, -2.5], "size": [5, 5, 5]}]},
            {"name": "shoulder_right", "parent": "body", "pivot": [-6.5, 23, 0],
             "cubes": [{"origin": [-11, 21, -2.5], "size": [5, 5, 5]}]},
            {"name": "arm_upper_l", "parent": "shoulder_left", "pivot": [8, 22, 0],
             "cubes": [{"origin": [7, 12, -2], "size": [4, 10, 4]}]},
            {"name": "arm_upper_r", "parent": "shoulder_right", "pivot": [-8, 22, 0],
             "cubes": [{"origin": [-11, 12, -2], "size": [4, 10, 4]}]},
            {"name": "arm_lower_l", "parent": "body", "pivot": [5, 17, 0],
             "cubes": [{"origin": [4.5, 9, -1.5], "size": [3, 8, 3]}]},
            {"name": "arm_lower_r", "parent": "body", "pivot": [-5, 17, 0],
             "cubes": [{"origin": [-7.5, 9, -1.5], "size": [3, 8, 3]}]},
            {"name": "leg_left", "parent": "root", "pivot": [3.5, 9, 0],
             "cubes": [{"origin": [1.5, 0, -2], "size": [4, 10, 4]}]},
            {"name": "leg_right", "parent": "root", "pivot": [-3.5, 9, 0],
             "cubes": [{"origin": [-5.5, 0, -2], "size": [4, 10, 4]}]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "body": {"position": {"0.0": [0, 0, 0], "1.0": [0, -0.5, 0], "2.0": [0, 0, 0]}},
                    "head": {"rotation": {"0.0": [0, 0, 0], "1.0": [4, 0, 0], "2.0": [0, 0, 0]}},
                    "arm_upper_l": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 8], "2.0": [0, 0, 0]}},
                    "arm_upper_r": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, -8], "2.0": [0, 0, 0]}},
                    "arm_lower_l": {"rotation": {"0.0": [0, 0, 0], "1.0": [-8, 0, 0], "2.0": [0, 0, 0]}},
                    "arm_lower_r": {"rotation": {"0.0": [0, 0, 0], "1.0": [-8, 0, 0], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "leg_left": {"rotation": {"0.0": [0, 0, 0], "0.25": [24, 0, 0], "0.5": [0, 0, 0], "0.75": [-24, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_right": {"rotation": {"0.0": [0, 0, 0], "0.25": [-24, 0, 0], "0.5": [0, 0, 0], "0.75": [24, 0, 0], "1.0": [0, 0, 0]}},
                    "arm_upper_l": {"rotation": {"0.0": [0, 0, 0], "0.25": [-20, 0, 0], "0.5": [0, 0, 0], "0.75": [20, 0, 0], "1.0": [0, 0, 0]}},
                    "arm_upper_r": {"rotation": {"0.0": [0, 0, 0], "0.25": [20, 0, 0], "0.5": [0, 0, 0], "0.75": [-20, 0, 0], "1.0": [0, 0, 0]}},
                    "arm_lower_l": {"rotation": {"0.0": [0, 0, 0], "0.25": [16, 0, 0], "0.5": [0, 0, 0], "0.75": [-16, 0, 0], "1.0": [0, 0, 0]}},
                    "arm_lower_r": {"rotation": {"0.0": [0, 0, 0], "0.25": [-16, 0, 0], "0.5": [0, 0, 0], "0.75": [16, 0, 0], "1.0": [0, 0, 0]}},
                    "body": {"position": {"0.0": [0, 0, 0], "0.25": [0, 0.5, 0], "0.5": [0, 0, 0], "0.75": [0, 0.5, 0], "1.0": [0, 0, 0]}}
                }
            }
        }
    },

    # --------------------------------------------------------------------------
    # 12. Mateus, the Corrupt [Esper Rank S]
    # --------------------------------------------------------------------------
    "mateus": {
        "theme_color": "#082f49",
        "colors": {
            "base": "#1e3a8a", "shadow": "#0f172a", "highlight": "#3b82f6",
            "silver": "#e2e8f0", "silver_shadow": "#94a3b8", "halo": "#38bdf8",
            "glyph": "#67e8f9", "eye": "#00f2fe", "eye_pupil": "#0284c7"
        },
        "bones": [
            {"name": "root", "pivot": [0, 0, 0]},
            {"name": "body", "parent": "root", "pivot": [0, 16, 0],
             "cubes": [
                 {"origin": [-4, 16, -3], "size": [8, 9, 6]},
                 {"origin": [-5, 3, -3.5], "size": [10, 13, 7]},   # skirt
                 {"origin": [-3, 16, -4.5], "size": [6, 7, 2]}     # captive ice goddess motif
             ]},
            {"name": "head", "parent": "body", "pivot": [0, 25, 0],
             "cubes": [
                 {"origin": [-3, 25, -3], "size": [6, 6, 6]},
                 {"origin": [-3.5, 31, -3.5], "size": [7, 3, 7]}   # crown
             ]},
            {"name": "halo", "parent": "body", "pivot": [0, 24, 4],
             "cubes": [
                 {"origin": [-6, 29, 4], "size": [12, 2, 1]},
                 {"origin": [-6, 19, 4], "size": [12, 2, 1]},
                 {"origin": [5, 19, 4], "size": [2, 10, 1]},
                 {"origin": [-7, 19, 4], "size": [2, 10, 1]}
             ]},
            {"name": "shoulder_left", "parent": "body", "pivot": [5, 23, 0],
             "cubes": [{"origin": [4, 21, -2], "size": [4, 4, 4]}]},
            {"name": "shoulder_right", "parent": "body", "pivot": [-5, 23, 0],
             "cubes": [{"origin": [-8, 21, -2], "size": [4, 4, 4]}]},
            {"name": "arm_left", "parent": "shoulder_left", "pivot": [5.5, 21, 0],
             "cubes": [{"origin": [4.5, 11, -1.5], "size": [3, 10, 3]}]},
            {"name": "arm_right", "parent": "shoulder_right", "pivot": [-5.5, 21, 0],
             "cubes": [{"origin": [-7.5, 11, -1.5], "size": [3, 10, 3]}]},
            {"name": "leg_left", "parent": "root", "pivot": [2, 7, 0],
             "cubes": [{"origin": [0.5, 0, -1.5], "size": [3, 7, 3]}]},
            {"name": "leg_right", "parent": "root", "pivot": [-2, 7, 0],
             "cubes": [{"origin": [-3.5, 0, -1.5], "size": [3, 7, 3]}]}
        ],
        "animations": {
            "idle": {
                "loop": True, "animation_length": 2.0,
                "bones": {
                    "body": {"position": {"0.0": [0, 0, 0], "1.0": [0, 0.6, 0], "2.0": [0, 0, 0]}},
                    "halo": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 180], "2.0": [0, 0, 360]}},
                    "arm_left": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, 6], "2.0": [0, 0, 0]}},
                    "arm_right": {"rotation": {"0.0": [0, 0, 0], "1.0": [0, 0, -6], "2.0": [0, 0, 0]}}
                }
            },
            "walk": {
                "loop": True, "animation_length": 1.0,
                "bones": {
                    "body": {
                        "position": {"0.0": [0, 0, 0], "0.25": [0, 0.4, 0], "0.5": [0, 0, 0], "0.75": [0, 0.4, 0], "1.0": [0, 0, 0]},
                        "rotation": {"0.0": [0, 0, 0], "0.25": [4, 0, 0], "0.5": [0, 0, 0], "0.75": [4, 0, 0], "1.0": [0, 0, 0]}
                    },
                    "halo": {"rotation": {"0.0": [0, 0, 0], "0.5": [0, 0, 180], "1.0": [0, 0, 360]}},
                    "leg_left": {"rotation": {"0.0": [0, 0, 0], "0.25": [18, 0, 0], "0.5": [0, 0, 0], "0.75": [-18, 0, 0], "1.0": [0, 0, 0]}},
                    "leg_right": {"rotation": {"0.0": [0, 0, 0], "0.25": [-18, 0, 0], "0.5": [0, 0, 0], "0.75": [18, 0, 0], "1.0": [0, 0, 0]}}
                }
            }
        }
    }
}

# ==============================================================================
# 4. PROCEDURAL 64x64 TEXTURE PAINTER
# ==============================================================================

def generate_texture(name, entity_def, packed_uvs):
    """Generates a rich 64x64 pixel art PNG texture for the entity."""
    c_map = entity_def["colors"]
    theme_rgba = hex_rgba(entity_def["theme_color"])
    base_rgba = hex_rgba(c_map["base"])
    shadow_rgba = hex_rgba(c_map["shadow"])
    highlight_rgba = hex_rgba(c_map["highlight"])
    eye_rgba = hex_rgba(c_map.get("eye", "#facc15"))
    pupil_rgba = hex_rgba(c_map.get("eye_pupil", "#09090b"))

    # Initialize canvas with theme background gradient
    pixels = [[(0, 0, 0, 255) for _ in range(64)] for _ in range(64)]
    for y in range(64):
        for x in range(64):
            # Subtle radial vignette
            dist = math.sqrt((x - 31.5) ** 2 + (y - 31.5) ** 2) / 45.0
            bg = shade_color(theme_rgba, max(0.4, 1.0 - dist * 0.5))
            pixels[y][x] = bg

    # Paint each bone's cubes into their mapped UV regions
    for b in entity_def["bones"]:
        bname = b["name"]
        for idx, c in enumerate(b.get("cubes", [])):
            ckey = f"{bname}_{idx}"
            if ckey not in packed_uvs:
                continue
            u, v = packed_uvs[ckey]
            dx, dy, dz = [int(math.ceil(s)) for s in c["size"]]

            # Bedrock 6-Face Box UV Coordinates
            # Top face (+Y): [u + dz .. u + dz + dx, v .. v + dz]
            # Bottom face (-Y): [u + dz + dx .. u + 2*dz + dx, v .. v + dz]
            # Right face (-X): [u .. u + dz, v + dz .. v + dz + dy]
            # Front face (+Z): [u + dz .. u + dz + dx, v + dz .. v + dz + dy]
            # Left face (+X): [u + dz + dx .. u + 2*dz + dx, v + dz .. v + dz + dy]
            # Back face (-Z): [u + 2*dz + dx .. u + 2*(dz + dx), v + dz .. v + dz + dy]

            faces = [
                ("top", u + dz, v, dx, dz, 1.15),
                ("bottom", u + dz + dx, v, dx, dz, 0.75),
                ("right", u, v + dz, dz, dy, 0.90),
                ("front", u + dz, v + dz, dx, dy, 1.05),
                ("left", u + dz + dx, v + dz, dz, dy, 0.90),
                ("back", u + 2 * dz + dx, v + dz, dz, dy, 0.80)
            ]

            # Choose specific color scheme per bone
            cube_base = base_rgba
            if "belly" in c_map and ("body" in bname or "snout" in bname):
                cube_base = blend_colors(base_rgba, hex_rgba(c_map["belly"]), 0.4)
            elif "flower" in bname or "crest" in bname:
                flower_hex = c_map.get("flower", c_map.get("crest", "#ef4444"))
                cube_base = hex_rgba(flower_hex)
            elif "halo" in bname:
                cube_base = hex_rgba(c_map.get("halo", "#38bdf8"))
            elif "horn" in bname or "talons" in bname:
                horn_hex = c_map.get("horns", c_map.get("talons", "#ca8a04"))
                cube_base = hex_rgba(horn_hex)
            elif "armor" in bname or "shoulder" in bname:
                armor_hex = c_map.get("armor", c_map.get("silver", "#334155"))
                cube_base = hex_rgba(armor_hex)
            elif "crown" in bname and "leaf" in c_map:
                cube_base = hex_rgba(c_map["crown"])
            elif "mane" in bname:
                cube_base = hex_rgba(c_map["mane"])

            for fname, fx, fy, fw, fh, lum in faces:
                for py in range(fy, fy + fh):
                    for px in range(fx, fx + fw):
                        if 0 <= px < 64 and 0 <= py < 64:
                            # Edge shading
                            is_edge = (px == fx or px == fx + fw - 1 or py == fy or py == fy + fh - 1)
                            factor = lum * (0.85 if is_edge else 1.0)
                            col = shade_color(cube_base, factor)

                            # Detailed procedural textures per creature
                            if name in ("wolf", "thextera") and "body" in bname and fname == "front":
                                if py > fy + fh // 2:
                                    col = blend_colors(col, hex_rgba(c_map.get("belly", "#edd8ba")), 0.6)
                            elif name == "hyena" and (fname in ("front", "back", "left", "right")):
                                # Spotted pattern
                                if (px + py * 3) % 4 == 0:
                                    col = hex_rgba(c_map["spots"])
                            elif name in ("cactoid", "flowering_cactoid"):
                                # Vertical spine ridges and needle dots
                                if (px - fx) % 2 == 0:
                                    col = shade_color(col, 0.88)
                                if (px + py * 2) % 5 == 0:
                                    col = hex_rgba(c_map["spines"])
                            elif name == "ichthon":
                                # Scaled pattern and luminous aqua line
                                if py == fy + fh // 2:
                                    col = hex_rgba(c_map["highlight"])
                                elif (px + py) % 2 == 0:
                                    col = shade_color(col, 1.12)
                            elif name == "cockatrice" and ("wing" in bname or "body" in bname):
                                # Feather banding
                                if (py - fy) % 2 == 0:
                                    col = hex_rgba(c_map["shadow"])
                            elif name == "rogue_tomato" and bname == "body":
                                # Glossy tomato specular reflection
                                if px == fx + 2 and py == fy + 2:
                                    col = (255, 255, 255, 255)
                                elif px in (fx + 1, fx + 3) and py in (fy + 1, fy + 3):
                                    col = hex_rgba(c_map["highlight"])
                            elif name == "white_mousse":
                                # Glossy flan bubbles
                                if (px * 7 + py * 11) % 19 == 0:
                                    col = (255, 255, 255, 255)
                                elif (px * 3 + py * 5) % 11 == 0:
                                    col = hex_rgba(c_map["glaze"])
                            elif name == "belias":
                                # Magma vein cracks
                                if (px * 3 + py * 7) % 7 == 0:
                                    col = hex_rgba(c_map["magma"])
                            elif name == "mateus":
                                # Silver edge filigree and glyphs
                                if is_edge:
                                    col = hex_rgba(c_map["silver"])
                                elif (px + py) % 4 == 0:
                                    col = hex_rgba(c_map["glyph"])

                            pixels[py][px] = col

            # Facial Features on the Head front face
            if bname == "head" and idx == 0:
                front_x = u + dz
                front_y = v + dz
                # Draw left and right eyes
                eye_w = max(1, dx // 4)
                eye_h = max(1, dy // 3)
                ey_pos = front_y + dy // 3

                # Left eye
                ex_l = front_x + max(0, dx // 6)
                for ey in range(ey_pos, min(64, ey_pos + eye_h)):
                    for ex in range(ex_l, min(64, ex_l + eye_w)):
                        pixels[ey][ex] = eye_rgba
                # Specular dot / pupil
                if 0 <= ex_l < 64 and 0 <= ey_pos < 64:
                    pixels[ey_pos][ex_l] = pupil_rgba

                # Right eye
                ex_r = front_x + dx - max(0, dx // 6) - eye_w
                for ey in range(ey_pos, min(64, ey_pos + eye_h)):
                    for ex in range(ex_r, min(64, ex_r + eye_w)):
                        pixels[ey][ex] = eye_rgba
                if 0 <= ex_r < 64 and 0 <= ey_pos < 64:
                    pixels[ey_pos][ex_r] = pupil_rgba

    return pixels

# ==============================================================================
# 5. GEOMETRY & ANIMATION BUILDERS
# ==============================================================================

def build_geometry_json(name, entity_def, packed_uvs):
    """Builds valid Bedrock/GeckoLib 1.12.0 geometry dict."""
    bones = []
    for b in entity_def["bones"]:
        bname = b["name"]
        bone_dict = {
            "name": bname,
            "pivot": b["pivot"]
        }
        if "parent" in b:
            bone_dict["parent"] = b["parent"]
        if "rotation" in b:
            bone_dict["rotation"] = b["rotation"]

        raw_cubes = b.get("cubes", [])
        if raw_cubes:
            cubes_arr = []
            for idx, c in enumerate(raw_cubes):
                ckey = f"{bname}_{idx}"
                uv = list(packed_uvs[ckey])
                cubes_arr.append({
                    "origin": c["origin"],
                    "size": c["size"],
                    "uv": uv
                })
            bone_dict["cubes"] = cubes_arr

        bones.append(bone_dict)

    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [
            {
                "description": {
                    "identifier": f"geometry.ivalice.{name}",
                    "texture_width": 64,
                    "texture_height": 64,
                    "visible_bounds_width": 3,
                    "visible_bounds_height": 3,
                    "visible_bounds_offset": [0, 1, 0]
                },
                "bones": bones
            }
        ]
    }

def build_animation_json(entity_def):
    """Builds valid GeckoLib 1.8.0 animation dict."""
    return {
        "format_version": "1.8.0",
        "animations": entity_def["animations"]
    }

# ==============================================================================
# 6. BIOME SPAWNS UPDATER (MINECRAFT 26.3 SCHEMA)
# ==============================================================================

# Regional distribution of canonical FFXII creatures
BIOME_CREATURE_MAP = {
    # 1. Dalmasca & Yensa Deserts
    "dalmasca_estersand": ["ivalice:wolf", "ivalice:cactoid", "ivalice:cockatrice"],
    "dalmasca_westersand": ["ivalice:wolf", "ivalice:cactoid", "ivalice:cockatrice"],
    "ogir_yensa_sandsea": ["ivalice:wolf", "ivalice:cactoid", "ivalice:cockatrice"],

    # 2. Plains, Uplands & Woodlands
    "giza_plains_dry": ["ivalice:giza_rabbit", "ivalice:hyena", "ivalice:wolf", "ivalice:cockatrice"],
    "ozmone_plain": ["ivalice:giza_rabbit", "ivalice:hyena", "ivalice:wolf", "ivalice:cockatrice"],
    "tchita_uplands": ["ivalice:giza_rabbit", "ivalice:hyena", "ivalice:wolf", "ivalice:cockatrice"],
    "salikawood": ["ivalice:giza_rabbit", "ivalice:hyena", "ivalice:wolf", "ivalice:cockatrice"],

    # 3. Rain Plains & Coasts
    "giza_plains_rains": ["ivalice:ichthon", "ivalice:giza_rabbit", "ivalice:hyena"],
    "phon_coast": ["ivalice:ichthon", "ivalice:giza_rabbit", "ivalice:hyena"],

    # 4. Jungles, Mountains & Steppes
    "golmore_jungle": ["ivalice:hyena", "ivalice:cockatrice"],
    "mosphoran_highwaste": ["ivalice:hyena", "ivalice:cockatrice"],
    "cerobi_steppe": ["ivalice:hyena", "ivalice:cockatrice"],

    # 5. Glaciers, Enchanged Forests & Deadlands
    "paramina_rift": ["ivalice:wolf", "ivalice:ichthon"],
    "feywood": ["ivalice:wolf", "ivalice:ichthon"],
    "nabreus_deadlands": ["ivalice:wolf", "ivalice:ichthon"],
}

CREATURE_SPAWN_PARAMS = {
    "ivalice:wolf": {"weight": 14, "min": 2, "max": 4},
    "ivalice:cactoid": {"weight": 12, "min": 1, "max": 3},
    "ivalice:cockatrice": {"weight": 12, "min": 2, "max": 4},
    "ivalice:giza_rabbit": {"weight": 18, "min": 2, "max": 5},
    "ivalice:hyena": {"weight": 14, "min": 2, "max": 4},
    "ivalice:ichthon": {"weight": 14, "min": 2, "max": 4},
}

def update_biome_spawns():
    """Updates the 15 biome JSON files without UTF-8 BOM."""
    updated_count = 0
    pattern = os.path.join(BIOMES_DIR, "*.json")
    files = glob.glob(pattern)

    for fpath in files:
        bname = os.path.splitext(os.path.basename(fpath))[0]
        if bname not in BIOME_CREATURE_MAP:
            continue

        with open(fpath, "r", encoding="utf-8") as f:
            data = json.load(f)

        attrs = data.setdefault("attributes", {})
        mob_spawns = attrs.setdefault("minecraft:gameplay/natural_mob_spawns", {})
        arg = mob_spawns.setdefault("argument", {})
        spawns_by_cat = arg.setdefault("spawns_by_category", {})
        creature_list = spawns_by_cat.setdefault("creature", [])

        # Filter out existing ivalice spawns to avoid duplicates
        preserved_vanilla = [c for c in creature_list if not str(c.get("type", "")).startswith("ivalice:")]

        # Build new ivalice spawns
        new_ivalice_spawns = []
        for mob_id in BIOME_CREATURE_MAP[bname]:
            params = CREATURE_SPAWN_PARAMS.get(mob_id, {"weight": 10, "min": 1, "max": 3})
            new_ivalice_spawns.append({
                "count": {
                    "type": "minecraft:uniform",
                    "max_inclusive": params["max"],
                    "min_inclusive": params["min"]
                },
                "type": mob_id,
                "weight": params["weight"]
            })

        # Combine with Ivalice creatures at the top
        spawns_by_cat["creature"] = new_ivalice_spawns + preserved_vanilla

        # Write file cleanly without BOM
        with open(fpath, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")

        updated_count += 1
        print(f"  [Biome] Updated {bname}.json with {len(new_ivalice_spawns)} canonical FFXII creature spawns.")

    return updated_count

# ==============================================================================
# 7. MAIN EXECUTION PIPELINE
# ==============================================================================

def main():
    print("================================================================")
    print(" Projeto Ivalice - GeckoLib 3D Entity & Biome Spawn Generator")
    print("================================================================")

    # 1. Ensure all destination directories exist
    for d in (GEO_DIR_1, GEO_DIR_2, ANIM_DIR_1, ANIM_DIR_2, TEXTURES_DIR):
        os.makedirs(d, exist_ok=True)

    generated_entities = []

    for name, entity_def in ENTITIES_DATA.items():
        print(f"\nProcessing entity '{name}'...")

        # 2. Pack UVs in 64x64
        packed_uvs = pack_model_cubes(entity_def["bones"])

        # 3. Build & Save Geometry JSON at both locations
        geo_dict = build_geometry_json(name, entity_def, packed_uvs)
        geo_path_1 = os.path.join(GEO_DIR_1, f"{name}.geo.json")
        geo_path_2 = os.path.join(GEO_DIR_2, f"{name}.geo.json")

        for gp in (geo_path_1, geo_path_2):
            with open(gp, "w", encoding="utf-8") as f:
                json.dump(geo_dict, f, indent=2, ensure_ascii=False)
                f.write("\n")

        # 4. Build & Save Animation JSON at both locations
        anim_dict = build_animation_json(entity_def)
        anim_path_1 = os.path.join(ANIM_DIR_1, f"{name}.animation.json")
        anim_path_2 = os.path.join(ANIM_DIR_2, f"{name}.animation.json")

        for ap in (anim_path_1, anim_path_2):
            with open(ap, "w", encoding="utf-8") as f:
                json.dump(anim_dict, f, indent=2, ensure_ascii=False)
                f.write("\n")

        # 5. Build & Save 64x64 RGBA Texture
        pixels = generate_texture(name, entity_def, packed_uvs)
        tex_path = os.path.join(TEXTURES_DIR, f"{name}.png")
        save_png_64x64(tex_path, pixels)

        generated_entities.append(name)
        print(f"  -> Generated geometry: {name}.geo.json (both locations)")
        print(f"  -> Generated animation: {name}.animation.json (both locations)")
        print(f"  -> Generated texture: {name}.png ({os.path.getsize(tex_path)} bytes)")

    # 6. Update Biomes
    print("\nUpdating Biome JSON files...")
    biomes_updated = update_biome_spawns()

    print("\n================================================================")
    print(f" Successfully generated assets for all {len(generated_entities)} entities!")
    print(f" Successfully updated {biomes_updated} biome JSON files!")
    print("================================================================")

if __name__ == "__main__":
    main()
