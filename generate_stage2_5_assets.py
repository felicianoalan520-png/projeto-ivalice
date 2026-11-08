"""
Projeto Ivalice - Asset & Model Generator (Stage 2.5)
Minecraft 26.3 / NeoForge
Generates 16x16 pixel-art PNG textures and item model definitions for all items in IvaliceItems.java.
Preserves teleport_stone and existing block items without UTF-8 BOM.
"""

import os
import sys
import json
import struct
import zlib

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ASSETS_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "assets", "ivalice")
TEXTURES_DIR = os.path.join(ASSETS_DIR, "textures", "item")
ITEMS_DIR = os.path.join(ASSETS_DIR, "items")
MODELS_DIR = os.path.join(ASSETS_DIR, "models", "item")
LANG_DIR = os.path.join(ASSETS_DIR, "lang")

PROTECTED_ITEMS = {
    "teleport_stone",
    "dalmasca_sand",
    "dalmasca_sandstone",
    "estersand_scrub",
    "feywood_mist_soil",
    "feywood_white_lily",
    "golmore_darkwood_log",
    "golmore_moss_stone",
    "gysahl_greens_wild",
    "mosphoran_slate",
    "nabreus_mire",
    "paramina_permafrost",
    "sunstone_ore",
    "yensa_fine_sand",
}

def hex_rgba(hex_code, alpha=255):
    """Convert hex string (#RRGGBB or #RRGGBBAA) to (R, G, B, A) tuple."""
    hex_code = hex_code.lstrip("#")
    if len(hex_code) == 6:
        return (int(hex_code[0:2], 16), int(hex_code[2:4], 16), int(hex_code[4:6], 16), alpha)
    elif len(hex_code) == 8:
        return (int(hex_code[0:2], 16), int(hex_code[2:4], 16), int(hex_code[4:6], 16), int(hex_code[6:8], 16))
    raise ValueError(f"Invalid hex color: {hex_code}")

def save_png_16x16(filepath, pixels):
    """Writes a 16x16 PNG image using pure Python (zlib/struct) without external dependencies."""
    raw = bytearray()
    for y in range(16):
        raw.append(0)  # Filter type 0 (None)
        for x in range(16):
            r, g, b, a = pixels[y][x]
            raw.extend((r, g, b, a))

    def make_chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)
    png_bytes = b"\x89PNG\r\n\x1a\n" + make_chunk(b"IHDR", ihdr) + make_chunk(b"IDAT", idat) + make_chunk(b"IEND", b"")

    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, "wb") as f:
        f.write(png_bytes)

def parse_ascii_art(art_lines, palette):
    """Converts 16 lines of 16 characters into a 16x16 pixel matrix of RGBA tuples."""
    pixels = [[(0, 0, 0, 0) for _ in range(16)] for _ in range(16)]
    for y, line in enumerate(art_lines):
        line = line.strip()
        if len(line) != 16:
            raise ValueError(f"Line {y} has length {len(line)} (expected 16): '{line}'")
        for x, ch in enumerate(line):
            if ch in palette and palette[ch] is not None:
                color = palette[ch]
                if isinstance(color, str):
                    color = hex_rgba(color)
                elif len(color) == 3:
                    color = (*color, 255)
                pixels[y][x] = color
    return pixels

# Standard Minecraft Spawn Egg Silhouette (16x16)
EGG_TEMPLATE = [
    "................",
    ".....OOOOOO.....",
    "...OOHHHBBBOO...",
    "..OHHHhhsBBSSO..",
    "..OHHhsshBBBSSO.",
    ".OHHBBdBBBhhSOO.",
    ".OHBBBBBBhssBSO.",
    ".OHBhhBBBBdSSSO.",
    ".OHhssBBBhhBSSO.",
    ".OHBdSBBhssSSSO.",
    ".OHBBSSBBdSSSSO.",
    "..OHBBhhsSSSSO..",
    "..OHBhssdSSSOO..",
    "...OOBBdSSSOO...",
    ".....OOOOOO.....",
    "................"
]

def make_spawn_egg(outline, base_high, base_mid, base_shadow, spot_high, spot_mid, spot_shadow):
    return parse_ascii_art(EGG_TEMPLATE, {
        ".": None,
        "O": outline,
        "H": base_high,
        "B": base_mid,
        "S": base_shadow,
        "h": spot_high,
        "s": spot_mid,
        "d": spot_shadow,
    })

# ==========================================
# 1. WEAPONS (14 items, parent handheld)
# ==========================================
WEAPONS = [
    {
        "id": "mythril_blade",
        "en": "Mythril Blade",
        "pt": "Espada de Mithril",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "L": "#d8ebf8", "M": "#8bb5d8", "S": "#477499", "s": "#1e374d", "b": "#1e374d",
            "Y": "#fef08a", "G": "#facc15", "g": "#ca8a04", "h": "#5c3317", "H": "#3b1e0b"
        },
        "art": [
            "................",
            "..............W.",
            ".............LWs",
            "............LMWS",
            "...........LMWS.",
            "..........LMWS..",
            ".........LMWS...",
            "........LMWS....",
            ".......LMWS.....",
            "...Gg.LMWS......",
            "..GYGGLMS.......",
            ".gYYgGGs........",
            "..gYGh..........",
            "...gh...........",
            "...G............",
            "................"
        ]
    },
    {
        "id": "save_the_queen",
        "en": "Save the Queen",
        "pt": "Salve a Rainha (Save the Queen)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "w": "#e2e8f0", "G": "#fde047", "g": "#eab308", "d": "#a16207",
            "C": "#38bdf8", "c": "#0284c7", "s": "#64748b", "h": "#1e293b"
        },
        "art": [
            "................",
            "..............W.",
            ".............WWs",
            "............WGWs",
            "...........WGWs.",
            "..........WGWs..",
            ".........WGWs...",
            "..g.....WGWs....",
            ".gGg...WGWs.....",
            ".gGGg.WGWs..g...",
            "..gGGgGgs..gGg..",
            "...gGCgg..gGGg..",
            "....gGgg..gGg...",
            ".....gGh.dg.....",
            "......gh........",
            ".......C........"
        ]
    },
    {
        "id": "tournesol",
        "en": "Tournesol",
        "pt": "Tournesol (Espada Girassol)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "F": "#facc15", "O": "#f97316", "s": "#c2410c",
            "R": "#ef4444", "r": "#991b1b", "h": "#78350f"
        },
        "art": [
            "................",
            "..............W.",
            ".............YWs",
            "............FYYs",
            "...........FYYs.",
            "..........FYYs..",
            "...F.....FYYs...",
            "..FFY...FYYs....",
            ".FYFYF.FYYs.F...",
            "..FYYFFFYY.FYY..",
            "...FYYRYs.FYYF..",
            "....FYFF...FF...",
            ".....FYh.FF.....",
            "......Fh........",
            ".......Fh.......",
            "........R......."
        ]
    },
    {
        "id": "zodiac_spear",
        "en": "Zodiac Spear",
        "pt": "Lança do Zodíaco (Zodiac Spear)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "c": "#38bdf8", "P": "#f8fafc", "p": "#cbd5e1", "q": "#64748b",
            "G": "#facc15", "g": "#ca8a04", "s": "#475569", "S": "#1e293b"
        },
        "art": [
            "..............W.",
            ".............cWc",
            "............qPPc",
            "...........qPPc.",
            "..........qPGs..",
            ".........gSG....",
            "........sPs.....",
            ".......sPs......",
            "......sPs.......",
            ".....sPs........",
            "....gSG.........",
            "...qPGs.........",
            "..sPs...........",
            ".gG.............",
            ".G..............",
            "................"
        ]
    },
    {
        "id": "gladius",
        "en": "Gladius",
        "pt": "Gládio (Gladius)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "E": "#a7f3d0", "G": "#10b981", "g": "#047857", "s": "#064e3b",
            "S": "#e2e8f0", "h": "#334155", "H": "#0f172a"
        },
        "art": [
            "................",
            ".............W..",
            "............EEg.",
            "...........EEgs.",
            "..........GEgs..",
            ".........GEgs...",
            "........GEgs....",
            ".......GEgs.....",
            "...S..GEgs......",
            "..ShSGEgs.......",
            ".ShhSSss........",
            "..ShH...........",
            "...sH...........",
            "....S...........",
            "................",
            "................"
        ]
    },
    {
        "id": "shikari_nagasa",
        "en": "Shikari Nagasa",
        "pt": "Shikari Nagasa",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "V": "#cbd5e1", "v": "#475569", "s": "#1e293b", "k": "#0f172a",
            "P": "#6b21a8", "h": "#18181b"
        },
        "art": [
            "................",
            ".............W..",
            "............WVs.",
            "...........WVvs.",
            "..........WVvs..",
            ".........WVvs...",
            "........WVvs....",
            ".......WVvs.....",
            "......WVvs......",
            "...s.WVvs.......",
            "..sPsVvs........",
            ".sPPPss.........",
            "..sPh...........",
            "...sh...........",
            "...ss...........",
            "................"
        ]
    },
    {
        "id": "masamune",
        "en": "Masamune",
        "pt": "Masamune",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "V": "#cbd5e1", "r": "#ef4444", "s": "#475569",
            "G": "#facc15", "g": "#ca8a04", "h": "#18181b", "H": "#27272a"
        },
        "art": [
            "................",
            "..............W.",
            ".............WVs",
            "...........WVrs.",
            "..........WVrs..",
            "........WVrs....",
            ".......WVrs.....",
            ".....WVrs.......",
            "....WVrs........",
            "...sVrs.........",
            "..gGss..........",
            ".GhhG...........",
            ".HhG............",
            ".sH.............",
            "................",
            "................"
        ]
    },
    {
        "id": "wyrmhero_blade",
        "en": "Wyrmhero Blade",
        "pt": "Espada do Herói Wyrm (Wyrmhero Blade)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "D": "#67e8f9", "C": "#0891b2", "s": "#164e63",
            "Y": "#fde047", "G": "#ca8a04", "r": "#ef4444", "h": "#78350f"
        },
        "art": [
            "................",
            "..............W.",
            ".............DWs",
            "............DDWs",
            "...........DDWs.",
            "..........DDWs..",
            ".........DDWs...",
            "...G....DDWs....",
            "..GYG..DDWs.....",
            ".GYYYDCDWs..G...",
            "..GYYrGDs..GYG..",
            "...GYGDD..GYYG..",
            "....GYGD..GYG...",
            ".....GYh.GG.....",
            "......Gh........",
            ".......G........"
        ]
    },
    {
        "id": "burning_bow",
        "en": "Burning Bow",
        "pt": "Arco Flamejante (Burning Bow)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "f": "#dc2626", "F": "#ea580c", "O": "#f97316", "Y": "#fde047", "s": "#e2e8f0"
        },
        "art": [
            "....ff..........",
            "...fYYf.........",
            "..fYOff.........",
            "..fOF.ss........",
            ".fOF...s........",
            ".fF....s........",
            ".fF....s........",
            ".fF....s........",
            ".fOF...s........",
            "..fOF..s........",
            "..fYOffs........",
            "...fYYf.s.......",
            "....ff...s......",
            "..........s.....",
            "................",
            "................"
        ]
    },
    {
        "id": "sagittarius",
        "en": "Sagittarius",
        "pt": "Sagitário (Sagittarius)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "G": "#fde047", "g": "#eab308", "d": "#a16207", "c": "#67e8f9", "C": "#0284c7", "W": "#ffffff"
        },
        "art": [
            "....gg..........",
            "...gGGg.........",
            "..gGdgg.........",
            "..gdc.cc........",
            ".gdC...c........",
            ".gC....c........",
            ".gCW...c........",
            ".gC....c........",
            ".gdC...c........",
            "..gdc..c........",
            "..gGdggc........",
            "...gGGg.c.......",
            "....gg...c......",
            "..........c.....",
            "................",
            "................"
        ]
    },
    {
        "id": "altair",
        "en": "Altair",
        "pt": "Altair",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "B": "#facc15", "b": "#ca8a04", "S": "#cbd5e1", "s": "#475569",
            "W": "#78350f", "w": "#451a03", "k": "#18181b"
        },
        "art": [
            "................",
            ".........BBBs...",
            "........BBBBs...",
            ".......BsBBBs...",
            "......Bs...ss...",
            "..sssSs.........",
            ".sWWsss.........",
            ".sWWWWks........",
            "..sWWWWss.......",
            "...sWWWWWss.....",
            "....sssWWss.....",
            "......ssWWs.....",
            "........sss.....",
            "................",
            "................",
            "................"
        ]
    },
    {
        "id": "fomalhaut",
        "en": "Fomalhaut",
        "pt": "Fomalhaut",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "S": "#64748b", "s": "#334155", "k": "#0f172a", "G": "#facc15", "g": "#ca8a04",
            "W": "#78350f", "w": "#451a03"
        },
        "art": [
            "..............Ss",
            "............SsSs",
            "..........SsSss.",
            "........SsSsGs..",
            "......SsSsGss...",
            "....ssSsSss.....",
            "..ssGgGsss......",
            ".sWWGssss.......",
            "sWWWWkss........",
            ".sWWWWss........",
            "..sWWWWss.......",
            "...sssWWss......",
            ".....ssWWs......",
            ".......sss......",
            "................",
            "................"
        ]
    },
    {
        "id": "oak_staff",
        "en": "Oak Staff",
        "pt": "Cajado de Carvalho",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "C": "#93c5fd", "c": "#3b82f6", "d": "#1d4ed8",
            "O": "#92400e", "o": "#78350f", "k": "#451a03"
        },
        "art": [
            "............ko..",
            "...........kCWk.",
            "..........kcCCdk",
            "..........kcddok",
            "...........koo..",
            "..........kOk...",
            ".........kOk....",
            "........kok.....",
            ".......kok......",
            "......kOk.......",
            ".....kOk........",
            "....kok.........",
            "...kok..........",
            "..kOk...........",
            ".kok............",
            "k...............",
        ]
    },
    {
        "id": "staff_of_the_magi",
        "en": "Staff of the Magi",
        "pt": "Cajado dos Magos (Staff of the Magi)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "P": "#f472b6", "p": "#c084fc", "d": "#7c3aed", "c": "#38bdf8",
            "G": "#fde047", "g": "#eab308", "k": "#a16207"
        },
        "art": [
            "..........kGk...",
            ".........kGWGk..",
            "........kGpcddk.",
            "........kGPPcdk.",
            ".........kGddk..",
            ".........kggk...",
            "........kGgk....",
            ".......kGgk.....",
            "......kggk......",
            ".....kGgk.......",
            "....kGgk........",
            "...kggk.........",
            "..kGgk..........",
            ".kGgk...........",
            "kggk............",
            "k...............",
        ]
    }
]

# ==========================================
# 2. DEFENSIVE EQUIPMENT (17 items)
# ==========================================
DEFENSIVE = [
    {
        "id": "escucheon",
        "en": "Escutcheon",
        "pt": "Escudo (Escutcheon)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "I": "#cbd5e1", "i": "#64748b", "s": "#334155",
            "B": "#d97706", "b": "#92400e", "k": "#1e293b"
        },
        "art": [
            "................",
            "...IIIIIIIIII...",
            "..IiiiiiiiiiiI..",
            "..IiBBssiiBBiI..",
            "..IiBBssiiBBiI..",
            "..IiBBssiiBBiI..",
            "..IissssssssiI..",
            "..IissssssssiI..",
            "..IiBBssiiBBiI..",
            "...IibbssiibI...",
            "...IibbssiibI...",
            "....IibssbiI....",
            ".....IisssI.....",
            "......IiiI......",
            ".......II.......",
            "................"
        ]
    },
    {
        "id": "zodiac_escucheon",
        "en": "Zodiac Escutcheon",
        "pt": "Escudo do Zodíaco (Zodiac Escutcheon)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "G": "#fde047", "g": "#eab308", "d": "#a16207",
            "B": "#1e3a8a", "b": "#0f172a", "W": "#ffffff"
        },
        "art": [
            "................",
            "...GGGGGGGGGG...",
            "..GGgGgGgGgGGG..",
            "..GgBBBBBBBBgG..",
            "..GBBBdGGdBBBG..",
            "..GgBBdGWdBBgG..",
            "..GBBdGGGGdBBG..",
            "..GgBdGGGGdBgG..",
            "..GBBBdGGdBBBG..",
            "...GgBBdGBBgG...",
            "...GBBBBBBBBG...",
            "....GgBBBBgG....",
            ".....GBBBBG.....",
            "......GggG......",
            ".......GG.......",
            "................"
        ]
    },
    {
        "id": "leather_cap",
        "en": "Leather Cap",
        "pt": "Gorro de Couro",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "L": "#d97706", "l": "#b45309", "d": "#78350f", "k": "#451a03", "B": "#facc15"
        },
        "art": [
            "................",
            "................",
            ".....kkkkkk.....",
            "...kkLLLLLLkk...",
            "..kLLLLLLLLLLk..",
            "..kLLLLLLLLLLk..",
            ".kLLLLBBLLLLLLk.",
            ".kLLLLBBLLLLLLk.",
            ".kllllllllllllk.",
            ".kddkllllllkddk.",
            ".kddk......kddk.",
            "..kk........kk..",
            "................",
            "................",
            "................",
            "................"
        ]
    },
    {
        "id": "leather_clothing",
        "en": "Leather Clothing",
        "pt": "Traje de Couro",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "L": "#d97706", "l": "#b45309", "d": "#78350f", "k": "#451a03", "B": "#facc15"
        },
        "art": [
            "................",
            "..kkk......kkk..",
            ".kLLLk....kLLLk.",
            ".kLLLLkkkkLLLLk.",
            ".kLLLLddddLLLLk.",
            ".kllllllllllllk.",
            "..klllBBBBlllk..",
            "..klllkkkklllk..",
            "..klllBBBBlllk..",
            "..klllkkkklllk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "mirage_vest",
        "en": "Mirage Vest",
        "pt": "Colete da Miragem (Mirage Vest)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "P": "#c084fc", "p": "#9333ea", "T": "#2dd4bf", "t": "#0f766e",
            "S": "#f1f5f9", "s": "#64748b", "k": "#1e1b4b"
        },
        "art": [
            "................",
            "..kkk......kkk..",
            ".kPPTk....kPPTk.",
            ".kPPTTSSSSPPTTk.",
            ".kPPtTssssPPtTk.",
            ".kppttppppttkkk.",
            "..kptsssssstk...",
            "..kptkTTTTktk...",
            "..kptsssssstk...",
            "..kptkppppktk...",
            "..ktttttttttk...",
            "..ktttttttttk...",
            "..ktttttttttk...",
            "...kkkkkkkkk....",
            "................",
            "................"
        ]
    },
    {
        "id": "brave_suit",
        "en": "Brave Suit",
        "pt": "Traje Bravo (Brave Suit)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "R": "#ef4444", "r": "#dc2626", "d": "#991b1b",
            "K": "#27272a", "k": "#18181b", "G": "#facc15"
        },
        "art": [
            "................",
            "..kkk......kkk..",
            ".kKKKk....kKKKk.",
            ".kKKRRkkkkRRKKk.",
            ".kKKRRddddRRKKk.",
            ".krrrrrrrrrrkkk.",
            "..krkkGGGGkkrk..",
            "..krkkddddkkrk..",
            "..krkkGGGGkkrk..",
            "..krkkddddkkrk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "..kKKKKKKKKKKk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "bronze_helm",
        "en": "Bronze Helm",
        "pt": "Elmo de Bronze",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "B": "#f59e0b", "b": "#d97706", "d": "#92400e", "k": "#451a03", "s": "#1c1917"
        },
        "art": [
            "................",
            "................",
            ".....kkkkkk.....",
            "...kkBBBBBBkk...",
            "..kBBBBBBBBBBk..",
            "..kBBBBBBBBBBk..",
            ".kbbbbbbbbbbbbk.",
            ".kbbssssssssbbk.",
            ".kbbddddddddbbk.",
            ".kddkbbbbbbkddk.",
            ".kddk......kddk.",
            "..kk........kk..",
            "................",
            "................",
            "................",
            "................"
        ]
    },
    {
        "id": "bronze_armor",
        "en": "Bronze Armor",
        "pt": "Armadura de Bronze",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "B": "#f59e0b", "b": "#d97706", "d": "#92400e", "k": "#451a03", "Y": "#fef08a"
        },
        "art": [
            "................",
            "..kkk......kkk..",
            ".kBBBk....kBBBk.",
            ".kBBBBkkkkBBBBk.",
            ".kBBBBddddBBBBk.",
            ".kbbbbbbbbbbbbk.",
            "..kbbbYbbYbbbk..",
            "..kbbbbbbbbbbk..",
            "..kbbbddddbbbk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "maximillian",
        "en": "Maximillian",
        "pt": "Maximillian",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "S": "#475569", "s": "#334155", "k": "#0f172a",
            "G": "#facc15", "g": "#ca8a04"
        },
        "art": [
            "................",
            "..kkk......kkk..",
            ".kSSSk....kSSSk.",
            ".kSSGGkkkkGGSSk.",
            ".kSSGGggggGGSSk.",
            ".kssssssssssssk.",
            "..ksssGGGGsssk..",
            "..kssssGGssssk..",
            "..kssGGGGGGssk..",
            "..kssssssssssk..",
            "..kkkksssskkkk..",
            "..kkkksssskkkk..",
            "..kkkkkkkkkkkk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "grand_helm",
        "en": "Grand Helm",
        "pt": "Grande Elmo (Grand Helm)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "P": "#f8fafc", "p": "#cbd5e1", "s": "#64748b", "k": "#1e293b",
            "G": "#facc15", "g": "#ca8a04", "R": "#ef4444"
        },
        "art": [
            "......GssG......",
            ".....GgRRGg.....",
            "....kGPppPGk....",
            "...kGPPPPPPGk...",
            "..kGPPPPPPPPGk..",
            "..kPPPPPPPPPPk..",
            ".kppppppppppppk.",
            ".kppssssssssppk.",
            ".kppGGGGGGGGppk.",
            ".ksskpGGGGpkssk.",
            ".kssk......kssk.",
            "..kk........kk..",
            "................",
            "................",
            "................",
            "................"
        ]
    },
    {
        "id": "grand_armor",
        "en": "Grand Armor",
        "pt": "Grande Armadura (Grand Armor)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "P": "#f8fafc", "p": "#cbd5e1", "s": "#64748b", "k": "#1e293b",
            "G": "#facc15", "g": "#ca8a04", "R": "#ef4444"
        },
        "art": [
            "................",
            "..kkk......kkk..",
            ".kPPPk....kPPPk.",
            ".kPPGGkkkkGGPPk.",
            ".kPPGGssssGGPPk.",
            ".kppppppRpppppk.",
            "..kpppGGGGpppk..",
            "..kppgGRRGgppk..",
            "..kpppGGGGpppk..",
            "..kssssssssssk..",
            "..kssssGGssssk..",
            "..kssssGGssssk..",
            "..kssssssssssk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "mage_habit",
        "en": "Mage's Habit",
        "pt": "Hábito do Mago",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "B": "#3b82f6", "b": "#1d4ed8", "d": "#1e3a8a", "k": "#0f172a",
            "G": "#fde047", "C": "#38bdf8"
        },
        "art": [
            "................",
            ".....kkkkkk.....",
            "...kkBBBBBBkk...",
            "..kBBBBBBBBBBk..",
            "..kBBBBCCBBBBk..",
            "..kbbbbCCbbbbk..",
            "..kbbbbbbbbbbk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "..kGGGGGGGGGGk..",
            "..kddddddddddk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "lordly_robe",
        "en": "Lordly Robes",
        "pt": "Vestes Senhoriais (Lordly Robes)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "w": "#f1f5f9", "s": "#cbd5e1", "k": "#64748b",
            "G": "#fde047", "g": "#ca8a04", "C": "#38bdf8"
        },
        "art": [
            "................",
            ".....kkkkkk.....",
            "...kkWWWWWWkk...",
            "..kWWWWWWWWWWk..",
            "..kWWWWCCWWWWk..",
            "..kwwwwCCwwwwk..",
            "..kwwwwGGwwwwk..",
            "..kssssGGssssk..",
            "..kssssGGssssk..",
            "..kssssGGssssk..",
            "..kssssGGssssk..",
            "..kGGGGGGGGGGk..",
            "..kssssssssssk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "ribbon",
        "en": "Ribbon",
        "pt": "Fita Protetora (Ribbon)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "R": "#ef4444", "r": "#dc2626", "d": "#991b1b",
            "Y": "#fde047", "C": "#38bdf8", "P": "#f472b6", "k": "#7f1d1d"
        },
        "art": [
            "................",
            "................",
            "..kk........kk..",
            ".kRRk......kRRk.",
            "kRPRRk....kRRCRk",
            "kRPRRRkYYkRRRCRk",
            "kRRRRRkYYkRRRRRk",
            ".kRRRRkYYkRRRRk.",
            "..kRRk....kRRk..",
            "...kk..kk..kk...",
            "......kRRk......",
            ".....kRRRRk.....",
            "....kRRddRRk....",
            "...kRRk..kRRk...",
            "...kk......kk...",
            "................"
        ]
    },
    {
        "id": "genji_gloves",
        "en": "Genji Gloves",
        "pt": "Luvas de Genji",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "R": "#ef4444", "r": "#b91c1c", "d": "#7f1d1d",
            "G": "#facc15", "g": "#ca8a04", "k": "#18181b"
        },
        "art": [
            "................",
            "................",
            "...kkkk..kkkk...",
            "..kRRRRkkRRRRk..",
            "..kRGGkRRkGGRk..",
            "..krrrkkkkrrrk..",
            "..krrrkkkkrrrk..",
            "..kGGGGkkGGGGk..",
            "..kddddkkddddk..",
            "..kddddkkddddk..",
            "..kGGGGkkGGGGk..",
            "..kddddkkddddk..",
            "...kkkk..kkkk...",
            "................",
            "................",
            "................"
        ]
    },
    {
        "id": "bubble_belt",
        "en": "Bubble Belt",
        "pt": "Cinto da Bolha (Bubble Belt)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "L": "#92400e", "l": "#78350f", "d": "#451a03", "k": "#451a03",
            "C": "#67e8f9", "c": "#0284c7", "W": "#ffffff", "G": "#facc15"
        },
        "art": [
            "................",
            "................",
            "................",
            "................",
            "................",
            "kkkkkkkkkkkkkkkk",
            "LLLLGGLLLLGGLLLL",
            "LLLCWLLLLLCWLLLL",
            "LLLcclLLLLcclLLL",
            "LLLLGGLLLLGGLLLL",
            "dddddddddddddddd",
            "................",
            "................",
            "................",
            "................",
            "................"
        ]
    },
    {
        "id": "golden_amulet",
        "en": "Golden Amulet",
        "pt": "Amuleto Dourado (Golden Amulet)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "G": "#fde047", "g": "#eab308", "d": "#a16207", "k": "#713f12",
            "A": "#f59e0b", "W": "#ffffff"
        },
        "art": [
            "..kk........kk..",
            "...kk......kk...",
            "....kk....kk....",
            ".....kk..kk.....",
            "......kGGk......",
            ".....kGGGGk.....",
            "....kGGGGGGk....",
            "...kGGgWWgGGk...",
            "...kGGgAAgGGk...",
            "...kGGgAAgGGk...",
            "...kGGggggGGk...",
            "....kGGddGGk....",
            ".....kGGGGk.....",
            "......kddk......",
            "................",
            "................"
        ]
    }
]

# ==========================================
# 3. CONSUMABLES (11 items)
# ==========================================
CONSUMABLES = [
    {
        "id": "potion",
        "en": "Potion",
        "pt": "Poção",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "K": "#78350f", "k": "#451a03",
            "G": "#e0f2fe", "g": "#38bdf8", "B": "#0284c7", "b": "#1e3a8a", "W": "#ffffff"
        },
        "art": [
            "................",
            "................",
            "......kKKk......",
            "......kKKk......",
            "......kGGk......",
            ".....kGGGGk.....",
            "....kGWBBBBk....",
            "...kGGWBBBBbk...",
            "...kGGBBBBBbk...",
            "...kGGBBgBBbk...",
            "...kggBBBBbbk...",
            "...kggbbbbbbk...",
            "....kbbbbbbk....",
            ".....kkkkkk.....",
            "................",
            "................"
        ]
    },
    {
        "id": "hi_potion",
        "en": "Hi-Potion",
        "pt": "Hi-Poção",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "K": "#cbd5e1", "k": "#475569",
            "G": "#e0f2fe", "g": "#34d399", "B": "#059669", "b": "#064e3b", "W": "#ffffff"
        },
        "art": [
            "................",
            "................",
            "......kKKk......",
            "......kKKk......",
            ".....kGGGGk.....",
            "....kGGGGGGk....",
            "...kGGWBBBBGk...",
            "...kGGWBBBBbk...",
            "...kGGgBBBBbk...",
            "...kGGgBBBBbk...",
            "...kggBBBBbbk...",
            "...kggbbbbbbk...",
            "....kbbbbbbk....",
            ".....kkkkkk.....",
            "................",
            "................"
        ]
    },
    {
        "id": "x_potion",
        "en": "X-Potion",
        "pt": "X-Poção",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "K": "#fde047", "k": "#a16207",
            "G": "#fef08a", "g": "#facc15", "B": "#f59e0b", "b": "#b45309", "W": "#ffffff"
        },
        "art": [
            "................",
            "......kKKk......",
            "......kKKk......",
            ".....kKKKKk.....",
            "....kGGGGGGk....",
            "...kGGWBBBBGk...",
            "...kGGWBBBBbk...",
            "...kGGgWBBbbk...",
            "...kGGgBBBBbk...",
            "...kggBBBBbbk...",
            "...kggbbbbbbk...",
            "....kbbbbbbk....",
            ".....kKKKKk.....",
            "......kkkk......",
            "................",
            "................"
        ]
    },
    {
        "id": "ether",
        "en": "Ether",
        "pt": "Éter",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "K": "#cbd5e1", "k": "#475569",
            "G": "#f3e8ff", "g": "#c084fc", "B": "#9333ea", "b": "#581c87", "W": "#ffffff"
        },
        "art": [
            "................",
            "................",
            "......kKKk......",
            "......kKKk......",
            "......kGGk......",
            ".....kGGGGk.....",
            "....kGWBBBBk....",
            "...kGGWBBBBbk...",
            "...kGGWBBBBbk...",
            "...kGGgBBBBbk...",
            "...kggBBBBbbk...",
            "...kggbbbbbbk...",
            "....kbbbbbbk....",
            ".....kkkkkk.....",
            "................",
            "................"
        ]
    },
    {
        "id": "elixir",
        "en": "Elixir",
        "pt": "Elixir",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "G": "#fde047", "g": "#ca8a04", "k": "#713f12",
            "R": "#f87171", "r": "#dc2626", "d": "#7f1d1d", "W": "#ffffff"
        },
        "art": [
            "................",
            "......kGGk......",
            "......kGGk......",
            "..k...kGGk...k..",
            ".kGk.kGGGGk.kGk.",
            ".kGkkGWRRRkGkGk.",
            ".kGkkGWRRRkGkGk.",
            "..kkkGGrrrkGkk..",
            "....kggRRRrk....",
            "....kggddddk....",
            ".....kggddk.....",
            "......kGGk......",
            ".....kGGGGk.....",
            ".....kkkkkk.....",
            "................",
            "................"
        ]
    },
    {
        "id": "phoenix_down",
        "en": "Phoenix Down",
        "pt": "Pena de Fênix (Phoenix Down)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "O": "#f97316", "R": "#ef4444", "r": "#991b1b", "k": "#450a0a"
        },
        "art": [
            ".............W..",
            "............YWk.",
            "...........YYOk.",
            "..........YOOkk.",
            ".........YOORk..",
            "........YOORk...",
            ".......YOORk....",
            "......YOORk.....",
            ".....YOORk......",
            "....YOORk.......",
            "...YOORk........",
            "..YOrrk.........",
            ".YOrk...........",
            "krk.............",
            "k...............",
            "................"
        ]
    },
    {
        "id": "antidote",
        "en": "Antidote",
        "pt": "Antídoto",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "K": "#78350f", "k": "#451a03",
            "G": "#dcfce7", "g": "#4ade80", "B": "#16a34a", "b": "#14532d", "P": "#a855f7"
        },
        "art": [
            "................",
            "................",
            "......kKKk......",
            "......kKKk......",
            "......kGGk......",
            ".....kGGGGk.....",
            "....kGGGBBBk....",
            "...kGGGBBBBbBk..",
            "...kGGGPBBBbbk..",
            "...kGGPPPBbbbk..",
            "...kggGPBBbbbk..",
            "...kggggbbbbbk..",
            "....kbbbbbbk....",
            ".....kkkkkk.....",
            "................",
            "................"
        ]
    },
    {
        "id": "eye_drops",
        "en": "Eye Drops",
        "pt": "Colírio (Eye Drops)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "S": "#f1f5f9", "s": "#94a3b8", "k": "#334155",
            "Y": "#fef08a", "y": "#facc15", "d": "#ca8a04", "W": "#ffffff"
        },
        "art": [
            ".......kk.......",
            ".......SS.......",
            "......kSSk......",
            "......kSSk......",
            ".....kssssk.....",
            "....kSYYYYYk....",
            "...kSYWYYYYyk...",
            "...kSYWYYYYyk...",
            "...kSYYYYYYyk...",
            "...kSYYYYYYyk...",
            "...kSyyyyyyyk...",
            "...kSdddddddk...",
            "....kddddddk....",
            ".....kkkkkk.....",
            ".......kk.......",
            ".......WW.......",
        ]
    },
    {
        "id": "echo_herbs",
        "en": "Echo Herbs",
        "pt": "Ervas do Eco (Echo Herbs)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "L": "#86efac", "l": "#22c55e", "d": "#15803d", "k": "#14532d",
            "T": "#d4a373", "t": "#a97446"
        },
        "art": [
            ".....k....k.....",
            "....kLk..kLk....",
            "...kLLk..kLLk...",
            "...kLlk..kLlk...",
            "....kldk.kldk...",
            "..k..kkd.kdk..k.",
            ".kLk..kldkd..kLk",
            ".kLlk..kldk.kLlk",
            "..kldk.kldk.kldk",
            "...kddkkddkkddk.",
            "....kkddddddkk..",
            ".....kkTTTTkk...",
            "......kttttk....",
            "......kttttk....",
            ".......kkkk.....",
            "................"
        ]
    },
    {
        "id": "remedy",
        "en": "Remedy",
        "pt": "Remédio Panaceia (Remedy)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "G": "#fde047", "g": "#ca8a04", "k": "#713f12",
            "R": "#f472b6", "Y": "#fef08a", "E": "#4ade80", "C": "#38bdf8", "W": "#ffffff"
        },
        "art": [
            "................",
            "......kGGk......",
            "......kGGk......",
            "......kGGk......",
            ".....kWWWWk.....",
            "....kWWWWWWk....",
            "...kWWWRRRRWk...",
            "...kWWRRRRRRk...",
            "...kWWYYYYYYk...",
            "...kWEEEEEEEk...",
            "...kWCCCCCCCk...",
            "...kCCCCCCCCk...",
            "....kCCCCCCk....",
            ".....kkkkkk.....",
            "................",
            "................"
        ]
    },
    {
        "id": "gysahl_greens",
        "en": "Gysahl Greens",
        "pt": "Verduras Gysahl",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "L": "#86efac", "l": "#22c55e", "d": "#15803d", "k": "#14532d",
            "Y": "#facc15", "y": "#ca8a04"
        },
        "art": [
            "...kk...kk......",
            "..kLLk.kLLk..kk.",
            ".kLLLLkLLLLkkLLk",
            ".kLLllkLllllkLLk",
            "..klllllllllldk.",
            ".kLLlllllllllldk",
            "kLLllllllllllddk",
            "kLllllllllldddk.",
            ".kllllllddddk...",
            "..klllddddkk....",
            "...kddddkkYYk...",
            "....kkkk.kYYk...",
            ".........kyyk...",
            "..........kyk...",
            "...........kk...",
            "................"
        ]
    }
]

# ==========================================
# 4. LOOT / MATERIALS (11 items)
# ==========================================
LOOT = [
    {
        "id": "wolf_pelt",
        "en": "Wolf Pelt",
        "pt": "Pele de Lobo",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "F": "#94a3b8", "f": "#64748b", "d": "#334155", "k": "#0f172a",
            "T": "#e2e8f0"
        },
        "art": [
            "......k..k......",
            ".....kFkkFk.....",
            ".....kFFFFk.....",
            "....kFFTTFFk....",
            "..kkkFFTTFFkkk..",
            ".kFFFFFTTFFFFFk.",
            ".kFFFFFTTFFFFFk.",
            "..kffffffdffk...",
            "..kffffTTfffk...",
            "..kffffTTfffk...",
            ".kffffffdfffffk.",
            ".kfffkffffkfffk.",
            "..kk..kffk..kk..",
            "......kffk......",
            ".......kk.......",
            "................"
        ]
    },
    {
        "id": "cactus_fruit",
        "en": "Cactus Fruit",
        "pt": "Fruta de Cacto",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "R": "#fb7185", "r": "#f43f5e", "d": "#be123c", "k": "#881337",
            "Y": "#fef08a", "y": "#fde047", "G": "#22c55e"
        },
        "art": [
            "................",
            ".......YY.......",
            "......kRRk......",
            "....YkRRRRkY....",
            "...kRRRRRRRRk...",
            "..kYRRRRRRRRYk..",
            "..kRRRRRRRRRRk..",
            ".kYRRRRRRRRRRYk.",
            "..krrrrrrrrrrk..",
            "..kYrrrrrrrryY..",
            "...kddddddddk...",
            "....kYddddYk....",
            ".....kGGGGk.....",
            "......kGGk......",
            ".......kk.......",
            "................"
        ]
    },
    {
        "id": "earth_stone",
        "en": "Earth Stone",
        "pt": "Pedra da Terra",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "L": "#fde047", "B": "#d97706", "b": "#92400e", "d": "#451a03", "k": "#291002"
        },
        "art": [
            "................",
            "......kkkk......",
            "....kkBBBBkk....",
            "...kLBBBBBBBk...",
            "..kLLBBBBBBBBk..",
            ".kLLBBBBbbbbbbk.",
            ".kLLBBbbbbbbbbk.",
            ".kBBBBbbbbddddk.",
            ".kBBbbbbddddddk.",
            "..kbbbdddddddk..",
            "..kbbbdddddddk..",
            "...kddddddddk...",
            "....kkddddkk....",
            "......kkkk......",
            "................",
            "................"
        ]
    },
    {
        "id": "fire_stone",
        "en": "Fire Stone",
        "pt": "Pedra do Fogo",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "O": "#f97316", "R": "#ef4444", "r": "#991b1b", "k": "#450a0a"
        },
        "art": [
            "................",
            "......kkkk......",
            "....kkOOOOkk....",
            "...kYOOOOOOOk...",
            "..kWYOOOOOOOOk..",
            ".kWWYYOORRRRRRk.",
            ".kWWYYOORRRRRRk.",
            ".kYYOORRRRrrrrk.",
            ".kOORRRRrrrrrrk.",
            "..kRRRRrrrrrrk..",
            "..kRRRRrrrrrrk..",
            "...krrrrrrrrk...",
            "....kkrrrrkk....",
            "......kkkk......",
            "................",
            "................"
        ]
    },
    {
        "id": "water_stone",
        "en": "Water Stone",
        "pt": "Pedra da Água",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "C": "#67e8f9", "B": "#38bdf8", "b": "#0284c7", "d": "#1e3a8a", "k": "#0f172a"
        },
        "art": [
            "................",
            "......kkkk......",
            "....kkBBBBkk....",
            "...kCBBBBBBBk...",
            "..kWCBBBBBBBBk..",
            ".kWWCBBBbbbbbbk.",
            ".kWWCBBbbbbbbbk.",
            ".kCBBBbbbbddddk.",
            ".kBBbbbbddddddk.",
            "..kbbbdddddddk..",
            "..kbbbdddddddk..",
            "...kddddddddk...",
            "....kkddddkk....",
            "......kkkk......",
            "................",
            "................"
        ]
    },
    {
        "id": "storm_stone",
        "en": "Storm Stone",
        "pt": "Pedra da Tempestade",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "Y": "#fef08a", "P": "#c084fc", "p": "#9333ea", "d": "#581c87", "k": "#2e1065"
        },
        "art": [
            "................",
            "......kkkk......",
            "....kkPPPPkk....",
            "...kPPPPPPPYk...",
            "..kPPPYPPPYYPk..",
            ".kPPPYYPPYPPPPk.",
            ".kPPPYPPYppddpk.",
            ".kPPYPPYppddddk.",
            ".kppYppdddddddk.",
            "..kppddddddddk..",
            "..kppddddddddk..",
            "...kddddddddk...",
            "....kkddddkk....",
            "......kkkk......",
            "................",
            "................"
        ]
    },
    {
        "id": "wind_slicer_pinion",
        "en": "Wind Slicer Pinion",
        "pt": "Pluma Corta-Vento",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "L": "#a7f3d0", "G": "#34d399", "g": "#059669", "d": "#064e3b", "k": "#022c22"
        },
        "art": [
            ".............W..",
            "............WLk.",
            "...........WLGk.",
            "..........WLGkk.",
            ".........WLGgk..",
            "........WLGgk...",
            ".......WGgdk....",
            "......WGgdk.....",
            ".....WGgdk......",
            "....WGgdk.......",
            "...WGgdk........",
            "..WLGdk.........",
            ".WLdk...........",
            "kdk.............",
            "k...............",
            "................"
        ]
    },
    {
        "id": "ichthon_scale",
        "en": "Ichthon Scale",
        "pt": "Escama de Ichthon",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "T": "#2dd4bf", "t": "#0f766e", "P": "#c084fc", "d": "#1e1b4b", "k": "#0f172a"
        },
        "art": [
            "................",
            ".....kkkkkk.....",
            "...kkTTTTTTkk...",
            "..kTTTTTTTTTTk..",
            ".kTTWWTTTTTTTTk.",
            ".kTTWWTTTTTTTTk.",
            "kTTTTTTTTttttttk",
            "kTTTTTTttttttttk",
            "kTTTTttttttttPPk",
            "kTTttttttttPPPPk",
            ".ktttttttPPPPddk",
            ".ktttttPPPPddddk",
            "..kttPPPPddddk..",
            "...kkPPddddkk...",
            ".....kkddkk.....",
            ".......kk......."
        ]
    },
    {
        "id": "sunstone",
        "en": "Giza Sunstone",
        "pt": "Pedra do Sol de Giza",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "G": "#facc15", "O": "#f97316", "d": "#c2410c", "k": "#7c2d12"
        },
        "art": [
            ".......YY.......",
            "......kYYk......",
            "....kkGGGGkk....",
            "...kYGGGGGGYk...",
            "..kYGGGGGGGGYk..",
            ".kYGGWGGGGGGGYk.",
            ".kYGGWGGGGGGGYk.",
            "kGGGGGGGOOOOOOGk",
            "kGGGGGOOOOOOOOGk",
            ".kOGGOOOOOOOOk..",
            ".kOGGOOOOOOOOk..",
            "..kOOOOOOOOOOk..",
            "...kOOOOOOOOk...",
            "....kkddddkk....",
            "......kddk......",
            ".......dd......."
        ]
    },
    {
        "id": "high_arcana",
        "en": "High Arcana",
        "pt": "Grande Arcana (High Arcana)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "P": "#f472b6", "R": "#e11d48", "V": "#9333ea", "d": "#4c1d95", "k": "#2e1065"
        },
        "art": [
            ".......kk.......",
            "......kWWk......",
            ".....kWWPPk.....",
            "....kWWPPPPk....",
            "...kWWPPPPPRk...",
            "..kWWPPPPPRRRk..",
            ".kWWPPPPRRRRRVk.",
            "kWWPPPRRRRRVVVVk",
            "kWWPPRRRRVVVVVVk",
            ".kPRRRRVVVVVVVk.",
            "..kRRVVVVVVVVd..",
            "...kRVVVVVVVdk..",
            "....kVVVVVddk...",
            ".....kVVdddk....",
            "......kdddk.....",
            ".......kkk......"
        ]
    },
    {
        "id": "soul_of_thamasa",
        "en": "Soul of Thamasa",
        "pt": "Alma de Thamasa",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "C": "#a5f3fc", "c": "#06b6d4", "B": "#0891b2",
            "G": "#fde047", "g": "#ca8a04", "k": "#164e63"
        },
        "art": [
            "......GGGG......",
            "....GGg..gGG....",
            "...Gg.kkkk.gG...",
            "..Gg.kWWWWk.gG..",
            ".Gg.kWWCCCCk.gG.",
            ".Gg.kWWCCCCk.gG.",
            "Gg.kCCCCcccck.gG",
            "Gg.kCCCCcccck.gG",
            "Gg.kccccBBBBk.gG",
            "Gg.kccccBBBBk.gG",
            ".Gg.kBBBBBBk.gG.",
            ".Gg.kBBBBBBk.gG.",
            "..Gg.kkkkkk.gG..",
            "...Gg......gG...",
            "....GGg..gGG....",
            "......GGGG......"
        ]
    }
]

# ==========================================
# 5. SPECIAL ITEMS, MAGICKS & TECHNICKS (15 items)
# ==========================================
SPECIAL = [
    {
        "id": "clan_primer",
        "en": "Clan Primer",
        "pt": "Manual do Clã Centurio (Clan Primer)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "L": "#92400e", "l": "#78350f", "d": "#451a03", "k": "#291002",
            "G": "#fde047", "g": "#ca8a04"
        },
        "art": [
            "................",
            "...kkkkkkkkkk...",
            "..kLLLLLLLLLLk..",
            "..kLLLLLLLLLLk..",
            "..kLLGGLLGGLLk..",
            "..kLLGGGGGGLLk..",
            "..kLLLGGGGLLLk..",
            "..kLLLLGGLLLLk..",
            "..klllLGGlllll..",
            "..kllllllllllk..",
            "..kllllllllllk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "license_tome",
        "en": "Zodiac License Tome",
        "pt": "Tomo do Sistema de Licenças",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#f8fafc", "w": "#e2e8f0", "s": "#94a3b8", "k": "#334155",
            "G": "#facc15", "g": "#b45309", "R": "#ef4444"
        },
        "art": [
            "................",
            "...kkkkkkkkkk...",
            "..kWWWWWWWWWWk..",
            "..kWGwGwGwGwWk..",
            "..kGwGwGwGwGwk..",
            "..kWGwGwGwGwWk..",
            "..kGwGwGwGwGwk..",
            "..kWGwGwGwGwWk..",
            "..kGwGwGwGwGwk..",
            "..kWGwGwGwGwWk..",
            "..kGwGwGwGwGwk..",
            "..kwwwwwwwwwwk..",
            "..kssssssssssk..",
            "...kkkkkkRkkk...",
            "........kRk.....",
            ".........k......"
        ]
    },
    {
        "id": "gambit_scroll",
        "en": "Gambit Scroll",
        "pt": "Pergaminho de Gambit",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "P": "#fef3c7", "p": "#fde68a", "d": "#d97706", "k": "#78350f",
            "B": "#2563eb", "b": "#1e40af", "R": "#ef4444"
        },
        "art": [
            "................",
            ".....kkkkkk.....",
            "....kPPPPPPk....",
            "...kPPppppPPk...",
            "...kPppppppPk...",
            "...kPppBBPppk...",
            "...kPpBBBBppk...",
            "...kPpBBBBppk...",
            "...kPppBBPppk...",
            "...kPppppppPk...",
            "...kPppppppPk...",
            "...kPPddddPPk...",
            "....kddddddk....",
            ".....kkkkkk.....",
            "......kRRk......",
            "................"
        ]
    },
    {
        "id": "dawn_shard",
        "en": "Dawn Shard",
        "pt": "Estilhaço da Alvorada (Dawn Shard)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "G": "#fde047", "g": "#ca8a04", "k": "#713f12",
            "n": "#0f172a", "W": "#ffffff"
        },
        "art": [
            ".....GGGGGG.....",
            "...GGGGGGGGGG...",
            "..GGgknnnnkgGG..",
            "..GgnnnnnnnngG..",
            ".GGgnnWnnnnngGG.",
            ".GgnnnnnnnnnngG.",
            ".GgnnnnnnnnnngG.",
            ".GgnnnnWnnnnngG.",
            ".GgnnnnnnnnnngG.",
            ".GgnnnnnWnnnngG.",
            ".GGgnnnnnnnngGG.",
            "..GgnnnnnnnngG..",
            "..GGgknnnnkgGG..",
            "...GGGGGGGGGG...",
            ".....GGGGGG.....",
            "................"
        ]
    },
    {
        "id": "goddess_magicite",
        "en": "Goddess's Magicite",
        "pt": "Magicita da Deusa",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "G": "#facc15", "g": "#ca8a04", "k": "#713f12",
            "W": "#ffffff", "C": "#e0f2fe", "c": "#7dd3fc", "b": "#0284c7"
        },
        "art": [
            ".......Gk.......",
            "......kGWk......",
            ".....kGCWck.....",
            "....kGCCWbck....",
            "...kGCCCCWbck...",
            "..kGCCCCWWbck...",
            ".kGCCCCWbbbcck..",
            "kGCCCCCCbbbccck.",
            ".kGCCCCbbbbcck..",
            "..kGgCbbbbck....",
            "...kGgbbbck.....",
            "....kGgbck......",
            ".....kGgk.......",
            "......kk........",
            "................",
            "................"
        ]
    },
    {
        "id": "magick_cure",
        "en": "Magick: Cure",
        "pt": "Magia: Cure",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "L": "#bbf7d0", "G": "#22c55e", "g": "#15803d", "k": "#14532d"
        },
        "art": [
            "......kkkk......",
            "....kkLLLLkk....",
            "...kLLLLLLLLk...",
            "..kLLLLWWLLLLk..",
            ".kLLLLWWWWLLLLk.",
            ".kLLLLWWWWLLLLk.",
            "kLLWWWWWWWWWWLLk",
            "kLLWWWWWWWWWWLLk",
            "kGGLLWWWWWWLLGGk",
            "kGGGGLLWWLLGGGGk",
            ".kGGGGGGGGGGGGk.",
            ".kGGGGGGGGGGGGk.",
            "..kggggggggggk..",
            "...kggggggggk...",
            "....kkggggkk....",
            "......kkkk......"
        ]
    },
    {
        "id": "magick_cura",
        "en": "Magick: Cura",
        "pt": "Magia: Cura",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "Y": "#fde047", "W": "#ffffff", "L": "#86efac", "G": "#16a34a", "g": "#14532d", "k": "#052e16"
        },
        "art": [
            "......kkkk......",
            "....kkYYYYkk....",
            "...kYYLLLLYYk...",
            "..kYYLLWWLLYYk..",
            ".kYYLLWWWWLLYYk.",
            ".kYYLLWWWWLLYYk.",
            "kYYWWWWWWWWWWYYk",
            "kYYWWWWWWWWWWYYk",
            "kGGLLWWWWWWLLGGk",
            "kGGGGLLWWLLGGGGk",
            ".kGGGGGGGGGGGGk.",
            ".kggggggggggggk.",
            "..kggggggggggk..",
            "...kggggggggk...",
            "....kkggggkk....",
            "......kkkk......"
        ]
    },
    {
        "id": "magick_fire",
        "en": "Magick: Fire",
        "pt": "Magia: Fire",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "O": "#f97316", "R": "#dc2626", "d": "#7f1d1d", "k": "#450a0a"
        },
        "art": [
            "......kkkk......",
            "....kkYYYYkk....",
            "...kYYOOOOYYk...",
            "..kYYOOWWOOYYk..",
            ".kYYOOWWWWOOYYk.",
            ".kYYOOWWWWOOYYk.",
            "kYYOOOOOOOOOOYYk",
            "kOOOOOOOOOOOOOOk",
            "kRROOOOOOOOOOORk",
            "kRRRROOOOOORRRRk",
            ".kRRRRRRRRRRRRk.",
            ".kRRRRRRRRRRRRk.",
            "..kddddddddddk..",
            "...kddddddddk...",
            "....kkddddkk....",
            "......kkkk......"
        ]
    },
    {
        "id": "magick_thunder",
        "en": "Magick: Thunder",
        "pt": "Magia: Thunder",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "P": "#c084fc", "p": "#9333ea", "d": "#581c87", "k": "#2e1065"
        },
        "art": [
            "......kkkk......",
            "....kkPPPPkk....",
            "...kPPPPPPPYk...",
            "..kPPPPPYPPYYk..",
            ".kPPPPYYPPYPPPk.",
            ".kPPPYYPPYPPPPk.",
            "kPPPYPPYPPPPPPPk",
            "kPPYPPYPPPPPPPPk",
            "kppYppYppppppppk",
            "kppppYpppppppppk",
            ".kppYppppppppPk.",
            ".kppddddddddddk.",
            "..kddddddddddk..",
            "...kddddddddk...",
            "....kkddddkk....",
            "......kkkk......"
        ]
    },
    {
        "id": "magick_blizzard",
        "en": "Magick: Blizzard",
        "pt": "Magia: Blizzard",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "C": "#e0f2fe", "c": "#38bdf8", "B": "#0284c7", "d": "#1e3a8a", "k": "#0f172a"
        },
        "art": [
            "......kkkk......",
            "....kkCCCCkk....",
            "...kCCCWWCCCk...",
            "..kCCCCWWCCCCk..",
            ".kCCWWCCCCWWCCk.",
            ".kCCCWWCCWWCCCk.",
            "kCCCCWWWWWWCCCCk",
            "kWWWWWWWWWWWWWWk",
            "kCCCCWWWWWWCCCCk",
            "kcccCWWCCWWcccck",
            ".kccWWCCCCWWcck.",
            ".kcccccWWccccck.",
            "..kBBBBWWBBBBk..",
            "...kBBBBBBBBk...",
            "....kkddddkk....",
            "......kkkk......"
        ]
    },
    {
        "id": "magick_hastega",
        "en": "Magick: Hastega",
        "pt": "Magia: Hastega",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "y": "#fde047", "A": "#f59e0b", "d": "#b45309", "k": "#78350f"
        },
        "art": [
            "......kkkk......",
            "....kkYYYYkk....",
            "...kYYAAAAyyk...",
            "..kYYAWWAAAYYk..",
            ".kYYAWWWWAAYYk..",
            ".kYYAAWWAAYYYk..",
            "kYYAAAWWAAYYYYk.",
            "kAAAAAWWAAAAAAk.",
            "kAAAAAWWAAAAAAk.",
            "kAAAAAWWAAAAAAk.",
            ".kAAAAddAAAAAk..",
            ".kddddddddddk...",
            "..kddddddddk....",
            "...kddddddk.....",
            "....kkddkk......",
            "......kk........"
        ]
    },
    {
        "id": "technick_libra",
        "en": "Technick: Libra",
        "pt": "Técnica: Libra",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "P": "#fef3c7", "p": "#fde68a", "d": "#d97706", "k": "#78350f",
            "G": "#eab308", "W": "#ffffff"
        },
        "art": [
            "................",
            ".....kkkkkk.....",
            "....kPPPPPPk....",
            "...kPPpGGpPPk...",
            "...kPpGGGGpPk...",
            "...kPGpWWpGPk...",
            "...kPGpWWpGPk...",
            "...kPpGGGGpPk...",
            "...kPPpGGpPPk...",
            "...kPppppppPk...",
            "...kPppppppPk...",
            "...kPPddddPPk...",
            "....kddddddk....",
            ".....kkkkkk.....",
            "................",
            "................"
        ]
    },
    {
        "id": "technick_steal",
        "en": "Technick: Steal",
        "pt": "Técnica: Steal (Roubar)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "P": "#cbd5e1", "p": "#94a3b8", "d": "#64748b", "k": "#1e293b",
            "G": "#fbbf24", "g": "#d97706"
        },
        "art": [
            "................",
            ".....kkkkkk.....",
            "....kPPPPPPk....",
            "...kPPpkkpPPk...",
            "...kPpkGGkpPk...",
            "...kPkGGGGkPk...",
            "...kPkGGGGkPk...",
            "...kPpkGGkpPk...",
            "...kPPpkkpPPk...",
            "...kPppppppPk...",
            "...kPppppppPk...",
            "...kPPddddPPk...",
            "....kddddddk....",
            ".....kkkkkk.....",
            "................",
            "................"
        ]
    },
    {
        "id": "esper_belias_glyph",
        "en": "Belias Glyph",
        "pt": "Glifo do Esper Belias",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "R": "#991b1b", "r": "#7f1d1d", "d": "#450a0a", "k": "#1c0404",
            "Y": "#fef08a", "O": "#f97316"
        },
        "art": [
            "......kkkk......",
            "....kkRRRRkk....",
            "...kRRRRRRRRk...",
            "..kRRYRRRRYRRk..",
            ".kRRYYORROYYRRk.",
            ".kRROYYOOYYORRk.",
            "kRRRROYYYYORRRRk",
            "kRRRRROYYORRRRRk",
            "kRRRRROYYORRRRRk",
            "kRRRROYYYYORRRRk",
            ".kRROYYOOYYORRk.",
            ".kRRYYORROYYRRk.",
            "..kRRYRRRRYRRk..",
            "...kRRRRRRRRk...",
            "....kkddddkk....",
            "......kkkk......"
        ]
    },
    {
        "id": "esper_mateus_glyph",
        "en": "Mateus Glyph",
        "pt": "Glifo do Esper Mateus",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "B": "#1e3a8a", "b": "#1e40af", "d": "#0f172a", "k": "#020617",
            "C": "#67e8f9", "W": "#ffffff"
        },
        "art": [
            "......kkkk......",
            "....kkBBBBkk....",
            "...kBBBBBBBBk...",
            "..kBBCBBBBCBBk..",
            ".kBBCWBCWCCBBk..",
            ".kBBCWCCCWCCBBk.",
            ".kBBWCCCCCWBBk..",
            ".kBBBCWWCBBBBk..",
            ".kBBBCWWCBBBBk..",
            ".kBBWCCCCCWBBk..",
            ".kBBCWCCCWCCBBk.",
            ".kBBCWBCWCCBBk..",
            "..kBBCBBBBCBBk..",
            "...kBBBBBBBBk...",
            "....kkddddkk....",
            "......kkkk......"
        ]
    }
]

# ==========================================
# 6. SPAWN EGGS FOR 12 FFXII ENTITIES
# ==========================================
SPAWN_EGGS = [
    {
        "id": "wolf_spawn_egg",
        "en": "Hyena Wolf Spawn Egg",
        "pt": "Ovo Gerador de Lobo das Areias",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#3b2d24",
            base_high="#b8a898", base_mid="#8a7868", base_shadow="#5c4e40",
            spot_high="#eae2d5", spot_mid="#c4b8a8", spot_shadow="#827868"
        )
    },
    {
        "id": "cactoid_spawn_egg",
        "en": "Cactoid Spawn Egg",
        "pt": "Ovo Gerador de Cactoide",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#163810",
            base_high="#6bb84b", base_mid="#428c2b", base_shadow="#235c18",
            spot_high="#e65c75", spot_mid="#b82e46", spot_shadow="#6b1525"
        )
    },
    {
        "id": "hyena_spawn_egg",
        "en": "Hyena Spawn Egg",
        "pt": "Ovo Gerador de Hiena",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#2d220e",
            base_high="#d4ab5d", base_mid="#a6823c", base_shadow="#6b5220",
            spot_high="#705a38", spot_mid="#483820", spot_shadow="#281e10"
        )
    },
    {
        "id": "giza_rabbit_spawn_egg",
        "en": "Giza Rabbit Spawn Egg",
        "pt": "Ovo Gerador de Lebre de Giza",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#423b32",
            base_high="#f0e6d6", base_mid="#c7b8a2", base_shadow="#8c7c6a",
            spot_high="#faa0b2", spot_mid="#cc6a7e", spot_shadow="#8a3d4d"
        )
    },
    {
        "id": "cockatrice_spawn_egg",
        "en": "Cockatrice Spawn Egg",
        "pt": "Ovo Gerador de Cocatriz",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#152b20",
            base_high="#6bb88e", base_mid="#437d5e", base_shadow="#254b38",
            spot_high="#e85860", spot_mid="#b8282e", spot_shadow="#691216"
        )
    },
    {
        "id": "ichthon_spawn_egg",
        "en": "Ichthon Spawn Egg",
        "pt": "Ovo Gerador de Ichthon",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#0c2530",
            base_high="#47b5de", base_mid="#2280a3", base_shadow="#12485c",
            spot_high="#fa8c50", spot_mid="#d65620", spot_shadow="#7a2a0d"
        )
    },
    {
        "id": "rogue_tomato_spawn_egg",
        "en": "Rogue Tomato Spawn Egg",
        "pt": "Ovo Gerador de Tomate Renegado",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#3b0808",
            base_high="#f05454", base_mid="#b82424", base_shadow="#6b1010",
            spot_high="#6cd44c", spot_mid="#3e8c2a", spot_shadow="#1f4e15"
        )
    },
    {
        "id": "thextera_spawn_egg",
        "en": "Thextera Spawn Egg",
        "pt": "Ovo Gerador de Thextera",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#2b1709",
            base_high="#c4793d", base_mid="#8a4e22", base_shadow="#4a2910",
            spot_high="#5ce8e8", spot_mid="#1db5b5", spot_shadow="#0c4848"
        )
    },
    {
        "id": "flowering_cactoid_spawn_egg",
        "en": "Flowering Cactoid Spawn Egg",
        "pt": "Ovo Gerador de Cactoide Florido",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#12330e",
            base_high="#5eb852", base_mid="#38852e", base_shadow="#1e5218",
            spot_high="#fae048", spot_mid="#dba81e", spot_shadow="#7a6010"
        )
    },
    {
        "id": "white_mousse_spawn_egg",
        "en": "White Mousse Spawn Egg",
        "pt": "Ovo Gerador de Mousse Branco",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#2d423e",
            base_high="#cbf0e6", base_mid="#88b8ae", base_shadow="#4a6e66",
            spot_high="#94c955", spot_mid="#5a8030", spot_shadow="#2e4718"
        )
    },
    {
        "id": "belias_spawn_egg",
        "en": "Belias the Gigas Spawn Egg",
        "pt": "Ovo Gerador de Belias o Gigante",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#260404",
            base_high="#ba2d2d", base_mid="#801818", base_shadow="#420808",
            spot_high="#ffa834", spot_mid="#e66800", spot_shadow="#803800"
        )
    },
    {
        "id": "mateus_spawn_egg",
        "en": "Mateus the Corrupt Spawn Egg",
        "pt": "Ovo Gerador de Mateus o Corrompido",
        "parent": "minecraft:item/generated",
        "pixels": make_spawn_egg(
            outline="#081426",
            base_high="#457cb5", base_mid="#234978", base_shadow="#0f2240",
            spot_high="#9bf2ff", spot_mid="#42b8d6", spot_shadow="#1b6880"
        )
    }
]

# ==========================================
# 7. ADDITIONAL ITEMS (38 items registered in IvaliceItems.java)
# ==========================================
ADDITIONAL_ITEMS = [
    # --- 15 Weapons ---
    {
        "id": "mythril_sword",
        "en": "Mythril Sword",
        "pt": "Espada de Mithril",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "L": "#d8ebf8", "M": "#8bb5d8", "S": "#477499", "s": "#1e374d",
            "Y": "#fef08a", "G": "#facc15", "g": "#ca8a04", "h": "#5c3317"
        },
        "art": [
            "................",
            "..............W.",
            ".............LWs",
            "............LMWS",
            "...........LMWS.",
            "..........LMWS..",
            ".........LMWS...",
            "........LMWS....",
            ".......LMWS.....",
            "...Gg.LMWS......",
            "..GYGGLMS.......",
            ".gYYgGGs........",
            "..gYGh..........",
            "...gh...........",
            "...G............",
            "................"
        ]
    },
    {
        "id": "deathbringer",
        "en": "Deathbringer",
        "pt": "Lâmina da Morte (Deathbringer)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#f87171", "R": "#dc2626", "r": "#991b1b", "k": "#0f172a",
            "v": "#3b0764", "s": "#1e1b4b", "I": "#94a3b8", "E": "#ef4444", "h": "#18181b"
        },
        "art": [
            "................",
            "..............W.",
            ".............WRk",
            "............WRrk",
            "...........WRrk.",
            "..........WRrk..",
            ".........WRrk...",
            "........WRrk....",
            ".......WRrk.....",
            "...Is.WRrk......",
            "..IEIsrvs.......",
            ".sIIEIsks.......",
            "..sIIsh.........",
            "...sh...........",
            "...I............",
            "................"
        ]
    },
    {
        "id": "durandal",
        "en": "Durandal",
        "pt": "Durandal",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "w": "#f1f5f9", "s": "#94a3b8",
            "G": "#fde047", "g": "#eab308", "d": "#a16207",
            "C": "#38bdf8", "c": "#0284c7", "h": "#1e293b"
        },
        "art": [
            "................",
            "..............W.",
            ".............WWs",
            "............WGWs",
            "...........WGWs.",
            "..........WGWs..",
            ".........WGWs...",
            "........WGWs....",
            "...Gg..WGWs.....",
            "..GCGGgWGWs.....",
            ".gGGGGGGss......",
            "..gGCCGdh.......",
            "...gGdh.........",
            "....dh..........",
            "....dC..........",
            "................"
        ]
    },
    {
        "id": "excalibur",
        "en": "Excalibur",
        "pt": "Excalibur",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "y": "#facc15", "s": "#ca8a04",
            "C": "#38bdf8", "G": "#fde047", "g": "#eab308", "h": "#f8fafc"
        },
        "art": [
            "................",
            "..............W.",
            ".............WYs",
            "............WYYs",
            "...........WYYs.",
            "..........WYYs..",
            ".........WYYs...",
            "..g.....WYYs....",
            ".gGg...WYYs.....",
            ".gGGg.WYYs..g...",
            "..gGGgGgs..gGg..",
            "...gGCgg..gGGg..",
            "....gGgg..gGg...",
            ".....gGh.sg.....",
            "......gh........",
            ".......G........"
        ]
    },
    {
        "id": "zorlin_shape",
        "en": "Zorlin Shape",
        "pt": "Zorlin Shape",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "S": "#e2e8f0", "s": "#94a3b8", "k": "#475569",
            "G": "#facc15", "g": "#ca8a04", "h": "#1e293b", "H": "#0f172a"
        },
        "art": [
            "................",
            ".............W..",
            "............WSk.",
            "...........WSsk.",
            "..........WSsk..",
            ".........WSsk...",
            "........WSsk....",
            ".......WSsk.....",
            "...G..WSsk......",
            "..GhGWSsk.......",
            ".GhhGGsk........",
            "..GhH...........",
            "...sH...........",
            "....G...........",
            "................",
            "................"
        ]
    },
    {
        "id": "gungnir",
        "en": "Gungnir",
        "pt": "Gungnir",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "O": "#f97316", "R": "#ef4444",
            "k": "#7f1d1d", "B": "#d97706", "b": "#92400e", "s": "#78350f", "S": "#451a03"
        },
        "art": [
            "..............W.",
            ".............YOY",
            "............kRRk",
            "...........kRRk.",
            "..........kRBs..",
            ".........bSB....",
            "........sSs.....",
            ".......sSs......",
            "......sSs.......",
            ".....sSs........",
            "....bSB.........",
            "...kRBs.........",
            "..sSs...........",
            ".bB.............",
            ".B..............",
            "................"
        ]
    },
    {
        "id": "whale_whisker",
        "en": "Whale Whisker",
        "pt": "Bigode de Baleia (Whale Whisker)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "S": "#f1f5f9", "s": "#94a3b8",
            "C": "#38bdf8", "c": "#0284c7", "d": "#1e3a8a", "k": "#0f172a"
        },
        "art": [
            "..............W.",
            ".............SsS",
            "............sCSk",
            "...........sCc..",
            "..........sCck..",
            ".........sCck...",
            "........sCck....",
            ".......sCck.....",
            "......sCck......",
            ".....sCck.......",
            "....sCck........",
            "...sCck.........",
            "..sCSk..........",
            ".SsS............",
            ".S..............",
            "................"
        ]
    },
    {
        "id": "yoichi_bow",
        "en": "Yoichi Bow",
        "pt": "Arco de Yoichi",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "k": "#18181b", "R": "#dc2626", "r": "#991b1b",
            "G": "#facc15", "g": "#ca8a04", "s": "#e2e8f0"
        },
        "art": [
            "....GG..........",
            "...GkkG.........",
            "..GkRkGG........",
            "..GkR.ss........",
            ".GkR...s........",
            ".Gkr...s........",
            ".GgG...s........",
            ".GgG...s........",
            ".Gkr...s........",
            "..GkR..s........",
            "..GkRkGG........",
            "...GkkG.s.......",
            "....GG...s......",
            "..........s.....",
            "................",
            "................"
        ]
    },
    {
        "id": "gastrafetes",
        "en": "Gastraphetes",
        "pt": "Gastrófete (Gastraphetes)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "B": "#f59e0b", "b": "#b45309", "s": "#cbd5e1", "k": "#475569",
            "W": "#78350f", "w": "#451a03", "I": "#f8fafc"
        },
        "art": [
            "............BBb.",
            "...........BbIs.",
            "..........BbsIs.",
            ".........Bbs.Is.",
            "........Bbs..Is.",
            "...ss..Bbs...Is.",
            "..sWWsBbs....Is.",
            ".sWWWWks.....Is.",
            "..sWWWWBs....Is.",
            "...sWWWWWBs..Is.",
            "....ssWWWWBssIs.",
            "......ssWWBBBBb.",
            "........sss..ss.",
            "................",
            "................",
            "................"
        ]
    },
    {
        "id": "francisca",
        "en": "Francisca",
        "pt": "Francisca",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "S": "#f1f5f9", "s": "#94a3b8", "I": "#64748b", "i": "#334155",
            "G": "#facc15", "g": "#ca8a04", "H": "#a16207", "h": "#713f12", "k": "#451a03"
        },
        "art": [
            "..........kSSW..",
            ".........kSIISW.",
            "........kSIiiiS.",
            ".......kSIiGgii.",
            "......kSIiGgH...",
            ".....kSIiHh.....",
            "....kSIiHh......",
            "...kSIiHh.......",
            "..kSIIHh........",
            ".kSSIHh.........",
            ".kIIHh..........",
            "..kHh...........",
            "...Hh...........",
            "....Hh..........",
            ".....Hh.........",
            "......k........."
        ]
    },
    {
        "id": "scorpion_tail",
        "en": "Scorpion Tail",
        "pt": "Cauda de Escorpião (Scorpion Tail)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "y": "#facc15",
            "C": "#64748b", "c": "#334155", "k": "#0f172a", "s": "#475569", "G": "#ca8a04"
        },
        "art": [
            "...........kCWk.",
            "..........kCCYck",
            ".........kCCyCck",
            ".........kCyCck.",
            ".........kCCck..",
            "........kCCck...",
            ".......kCCck....",
            ".......kGgk.....",
            "......ksGk......",
            ".....ksGk.......",
            "....ksGk........",
            "...ksGk.........",
            "..ksGk..........",
            ".ksGk...........",
            "ksk.............",
            "k...............",
        ]
    },
    {
        "id": "cherry_staff",
        "en": "Cherry Staff",
        "pt": "Cajado de Cerejeira",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "P": "#fbcfe8", "p": "#f472b6",
            "E": "#86efac", "e": "#22c55e", "B": "#9d5338", "b": "#6c3322", "k": "#451a03"
        },
        "art": [
            "...........kpk..",
            "..........kPWEk.",
            ".........kpEEek.",
            ".........kPeeek.",
            "..........kbbk..",
            ".........kBbk...",
            "........kBbk....",
            ".......kBbk.....",
            "......kBbk......",
            ".....kBbk.......",
            "....kBbk........",
            "...kBbk.........",
            "..kBbk..........",
            ".kBbk...........",
            "kbbk............",
            "k...............",
        ]
    },
    {
        "id": "rod_of_faith",
        "en": "Rod of Faith",
        "pt": "Bastão da Fé (Rod of Faith)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "G": "#fde047", "g": "#eab308", "d": "#a16207", "k": "#713f12"
        },
        "art": [
            "..........kGk...",
            ".........kGWGk..",
            "........kGYWYGk.",
            "........kGWWWGk.",
            ".........kGWGk..",
            ".........kggk...",
            "........kGgk....",
            ".......kGgk.....",
            "......kggk......",
            ".....kGgk.......",
            "....kGgk........",
            "...kggk.........",
            "..kGgk..........",
            ".kGgk...........",
            "kggk............",
            "k...............",
        ]
    },
    {
        "id": "euclids_sextant",
        "en": "Euclid's Sextant",
        "pt": "Sextante de Euclides",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "G": "#fde047", "g": "#ca8a04", "d": "#713f12",
            "S": "#e2e8f0", "s": "#94a3b8", "C": "#67e8f9", "W": "#ffffff", "k": "#334155"
        },
        "art": [
            "................",
            ".......dGg......",
            "......dGWgG.....",
            ".....dGCWggg....",
            "....dGGgggGg....",
            "...dGgggg.Gg....",
            "..dGggs...Gg....",
            ".dGgs....SSss...",
            "dGgs....SSsss...",
            "dgG....SSssss...",
            ".gG...SSssss....",
            "..gG.SSssss.....",
            "...gGSSsss......",
            "....gSSSS.......",
            ".....sss........",
            "................",
        ]
    },
    {
        "id": "volcano",
        "en": "Volcano",
        "pt": "Vulcão (Volcano)",
        "parent": "minecraft:item/handheld",
        "palette": {
            ".": None,
            "B": "#f59e0b", "b": "#b45309", "k": "#78350f",
            "R": "#ef4444", "O": "#f97316", "Y": "#fef08a", "W": "#ffffff"
        },
        "art": [
            "............YYW.",
            "...........kROk.",
            "..........kRROk.",
            ".........kBBBkk.",
            "........kBBBBk..",
            ".......kBBBBk...",
            "......kBBBBk....",
            ".....kBBBBk.....",
            "....kBBBBk......",
            "...kBBBBk.......",
            "..kBBBBk........",
            ".kBBBBk.........",
            "kbbbbk..........",
            "kbbbk...........",
            ".kkk............",
            "................",
        ]
    },

    # --- 9 Defensive / Armor ---
    {
        "id": "escutcheon",
        "en": "Escutcheon",
        "pt": "Escudo (Escutcheon)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "B": "#f59e0b", "b": "#d97706", "d": "#92400e", "k": "#451a03",
            "G": "#facc15", "s": "#334155"
        },
        "art": [
            "................",
            "...GGGGGGGGGG...",
            "..GbbbbbbbbbbG..",
            "..GbBBssbbBBbG..",
            "..GbBBssbbBBbG..",
            "..GbBBssbbBBbG..",
            "..GbssssssssbG..",
            "..GbssssssssbG..",
            "..GbBBssbbBBbG..",
            "...GbbssbbddG...",
            "...GbbssbbddG...",
            "....GbsbddG.....",
            ".....GbssG......",
            "......GddG......",
            ".......GG.......",
            "................"
        ]
    },
    {
        "id": "aegis_shield",
        "en": "Aegis Shield",
        "pt": "Escudo Égide (Aegis Shield)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "S": "#f8fafc", "s": "#cbd5e1", "d": "#64748b", "k": "#1e293b",
            "C": "#2dd4bf", "c": "#0f766e", "W": "#ffffff"
        },
        "art": [
            "................",
            ".....kkkkkk.....",
            "...kkSSSSSSkk...",
            "..kSSSSSSSSSSk..",
            ".kSSSSkkkkSSSSk.",
            ".kSSSsCWWCkSSSk.",
            "kSSSSsCCCCkSSSSk",
            "kSSSssCCCCkkSSSk",
            "kSSSssCCCCkkSSSk",
            "kSSSssdccdkkSSSk",
            ".kSSSskkkkSSSk..",
            ".kSSSSssssSSSk..",
            "..kSSssssssSk...",
            "...kkddddddkk...",
            ".....kkddkk.....",
            ".......kk.......",
        ]
    },
    {
        "id": "zodiac_escutcheon",
        "en": "Zodiac Escutcheon",
        "pt": "Escudo do Zodíaco (Zodiac Escutcheon)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "G": "#fde047", "g": "#eab308", "d": "#a16207",
            "B": "#1e3a8a", "b": "#0f172a", "W": "#ffffff"
        },
        "art": [
            "................",
            "...GGGGGGGGGG...",
            "..GGgGgGgGgGGG..",
            "..GgBBBBBBBBgG..",
            "..GBBBdGGdBBBG..",
            "..GgBBdGWdBBgG..",
            "..GBBdGGGGdBBG..",
            "..GgBdGGGGdBgG..",
            "..GBBBdGGdBBBG..",
            "...GgBBdGBBgG...",
            "...GBBBBBBBBG...",
            "....GgBBBBgG....",
            ".....GBBBBG.....",
            "......GggG......",
            ".......GG.......",
            "................"
        ]
    },
    {
        "id": "iron_helm",
        "en": "Iron Helm",
        "pt": "Elmo de Ferro",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "I": "#cbd5e1", "i": "#94a3b8", "d": "#475569", "k": "#1e293b", "s": "#0f172a"
        },
        "art": [
            "................",
            "................",
            ".....kkkkkk.....",
            "...kkIIIIIIkk...",
            "..kIIIIIIIIIIk..",
            "..kIIIIIIIIIIk..",
            ".kiiiiiiiiiiik..",
            ".kiisssssssiik..",
            ".kiidddddddiik..",
            ".kddkiiiiiikddk.",
            ".kddk......kddk.",
            "..kk........kk..",
            "................",
            "................",
            "................",
            "................"
        ]
    },
    {
        "id": "scale_armor",
        "en": "Scale Armor",
        "pt": "Armadura de Escamas",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "B": "#f59e0b", "b": "#d97706", "I": "#94a3b8", "i": "#475569", "k": "#1e293b"
        },
        "art": [
            "................",
            "..kkk......kkk..",
            ".kIIIk....kIIIk.",
            ".kIIIIkkkkIIIIk.",
            ".kIIIIiiiiIIIIk.",
            ".kiiiiiiiiiiiik.",
            "..kBiBiBiBiBik..",
            "..kiBiBiBiBikk..",
            "..kBiBiBiBiBik..",
            "..kiBiBiBiBikk..",
            "..kBiBiBiBiBik..",
            "..kiiiiiiiiiik..",
            "..kbbbbbbbbbbk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "maximilian",
        "en": "Maximilian",
        "pt": "Maximilian",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "S": "#475569", "s": "#334155", "k": "#0f172a",
            "G": "#facc15", "g": "#ca8a04"
        },
        "art": [
            "................",
            "..kkk......kkk..",
            ".kSSSk....kSSSk.",
            ".kSSGGkkkkGGSSk.",
            ".kSSGGggggGGSSk.",
            ".kssssssssssssk.",
            "..ksssGGGGsssk..",
            "..kssssGGssssk..",
            "..kssGGGGGGssk..",
            "..kssssssssssk..",
            "..kkkksssskkkk..",
            "..kkkksssskkkk..",
            "..kkkkkkkkkkkk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "mages_habit",
        "en": "Mage's Habit",
        "pt": "Hábito do Mago",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "B": "#3b82f6", "b": "#1d4ed8", "d": "#1e3a8a", "k": "#0f172a",
            "G": "#fde047", "C": "#38bdf8"
        },
        "art": [
            "................",
            ".....kkkkkk.....",
            "...kkBBBBBBkk...",
            "..kBBBBBBBBBBk..",
            "..kBBBBCCBBBBk..",
            "..kbbbbCCbbbbk..",
            "..kbbbbbbbbbbk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "..kddddddddddk..",
            "..kGGGGGGGGGGk..",
            "..kddddddddddk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "white_robe",
        "en": "White Robe",
        "pt": "Manto Branco (White Robe)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "w": "#f1f5f9", "s": "#cbd5e1", "k": "#64748b",
            "G": "#fde047", "g": "#ca8a04"
        },
        "art": [
            "................",
            ".....kkkkkk.....",
            "...kkWWWWWWkk...",
            "..kWWWWWWWWWWk..",
            "..kWWWWGGWWWWk..",
            "..kwwwwGGwwwwk..",
            "..kwwwwGGwwwwk..",
            "..kssssGGssssk..",
            "..kssssGGssssk..",
            "..kssssGGssssk..",
            "..kssssGGssssk..",
            "..kGGGGGGGGGGk..",
            "..kssssssssssk..",
            "...kkkkkkkkkk...",
            "................",
            "................"
        ]
    },
    {
        "id": "hermes_sandals",
        "en": "Hermes' Sandals",
        "pt": "Sandálias de Hermes",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "w": "#f1f5f9", "s": "#cbd5e1",
            "G": "#facc15", "g": "#ca8a04", "L": "#92400e", "k": "#451a03"
        },
        "art": [
            "................",
            "................",
            "...Ww......Ww...",
            "..WWww....WWww..",
            ".WWWswk..WWWswk.",
            ".WWWskk..WWWskk.",
            "..kWWGGkkkWWGGk.",
            "...kGggGkkGggGk.",
            "...kGggGkkGggGk.",
            "...kGGGGkkGGGGk.",
            "...kLLLLkkLLLLk.",
            "...kLLLLkkLLLLk.",
            "...kLLLLkkLLLLk.",
            "...kkkkk..kkkkk.",
            "................",
            "................"
        ]
    },

    # --- 14 Loot / Key Items ---
    {
        "id": "giza_rabbit_fur",
        "en": "Giza Rabbit Fur",
        "pt": "Pelo de Lebre de Giza",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "w": "#f8fafc", "s": "#e2e8f0", "p": "#fce7f3", "k": "#94a3b8"
        },
        "art": [
            "......k..k......",
            ".....kWkkWk.....",
            ".....kWWWWk.....",
            "....kWWppWWk....",
            "..kkkWWppWWkkk..",
            ".kWWWWWppWWWWWk.",
            ".kWWWWWWWWWWWWk.",
            "..kwwwwwwwwwwk..",
            "..kwwwwppwwwwk..",
            "..kwwwwppwwwwk..",
            ".kwwwwwwwwwwwwk.",
            ".kwwwkwwwwkwwwk.",
            "..kk..kwwk..kk..",
            "......kwwk......",
            ".......kk.......",
            "................"
        ]
    },
    {
        "id": "small_feather",
        "en": "Small Feather",
        "pt": "Pluma Pequena",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "G": "#a7f3d0", "g": "#34d399", "d": "#059669", "k": "#064e3b"
        },
        "art": [
            "................",
            ".............W..",
            "............WGk.",
            "...........WGgk.",
            "..........WGgkk.",
            ".........WGgdk..",
            "........WGgdk...",
            ".......WGgdk....",
            "......WGgdk.....",
            ".....WGgdk......",
            "....WGgdk.......",
            "...WGdk.........",
            "..Wdk...........",
            ".kdk............",
            "k...............",
            "................"
        ]
    },
    {
        "id": "fish_scale",
        "en": "Fish Scale",
        "pt": "Escama de Peixe",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "T": "#2dd4bf", "t": "#0f766e", "P": "#c084fc", "d": "#1e1b4b", "k": "#0f172a"
        },
        "art": [
            "................",
            ".....kkkkkk.....",
            "...kkTTTTTTkk...",
            "..kTTTTTTTTTTk..",
            ".kTTWWTTTTTTTTk.",
            ".kTTWWTTTTTTTTk.",
            "kTTTTTTTTttttttk",
            "kTTTTTTttttttttk",
            "kTTTTttttttttPPk",
            "kTTttttttttPPPPk",
            ".ktttttttPPPPddk",
            ".ktttttPPPPddddk",
            "..kttPPPPddddk..",
            "...kkPPddddkk...",
            ".....kkddkk.....",
            ".......kk......."
        ]
    },
    {
        "id": "malboro_vine",
        "en": "Malboro Vine",
        "pt": "Vinha de Malboro",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "G": "#4ade80", "g": "#16a34a", "P": "#9333ea", "p": "#581c87", "k": "#14532d"
        },
        "art": [
            "....kk..........",
            "...kGgk..kk.....",
            "...kgPgkkGgk....",
            "....kpPPgPgk....",
            ".....kpPPgk.....",
            "..kk..kpPgk.....",
            ".kGgkkpPPgk.kk..",
            ".kgPpPPpPgkkGgk.",
            "..kppPPppPppPgk.",
            "...kppPPpppPgk..",
            "....kppPPpgk....",
            ".....kpppgk.....",
            "......kppgk.....",
            ".......kpk......",
            "........kk......",
            "................"
        ]
    },
    {
        "id": "wind_stone",
        "en": "Wind Stone",
        "pt": "Pedra do Vento",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "L": "#a7f3d0", "G": "#34d399", "g": "#059669", "d": "#064e3b", "k": "#022c22"
        },
        "art": [
            "................",
            "......kkkk......",
            "....kkGGGGkk....",
            "...kLGGGGGGGLk..",
            "..kWLGGGGGGGGLk.",
            ".kWWLLGGGgggggdk",
            ".kWWLLGGggggggdk",
            ".kLLGGGgggggdddk",
            ".kGGggggggdddddk",
            "..kgggggddddddk.",
            "..kgggggddddddk.",
            "...kddddddddk...",
            "....kkddddkk....",
            "......kkkk......",
            "................",
            "................"
        ]
    },
    {
        "id": "dark_magicite",
        "en": "Dark Magicite",
        "pt": "Magicita Negra (Dark Magicite)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "P": "#c084fc", "p": "#9333ea", "d": "#581c87", "k": "#1e1b4b", "K": "#090514"
        },
        "art": [
            ".......kk.......",
            "......kPPk......",
            ".....kPppPk.....",
            "....kPppppPk....",
            "...kPppppppPk...",
            "..kPppppppppPk..",
            ".kPppppddddppPk.",
            "kPpppddddddddpPk",
            ".kppddddddddppk.",
            "..kddddddddddk..",
            "...kddddddddk...",
            "....kddddddk....",
            ".....kddddk.....",
            "......kddk......",
            ".......kk.......",
            "................"
        ]
    },
    {
        "id": "gemsteel",
        "en": "Gemsteel",
        "pt": "Aço Precioso (Gemsteel)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "R": "#ef4444", "r": "#b91c1c", "d": "#7f1d1d", "k": "#450a0a"
        },
        "art": [
            "................",
            "................",
            "................",
            ".....kkkkkkkk...",
            "....kRRRRRRRRk..",
            "...kRWRRRRRRRRk.",
            "..kRRWRRRRRRRRk.",
            ".kRRRRRRRRRRRRk.",
            ".kRRRrrrrrrrrrk.",
            ".kRRrrrrrrrrrrk.",
            ".kRrrrrrrrrrrrk.",
            ".krrrrrrrrddddk.",
            ".kddddddddddddk.",
            "..kkkkkkkkkkkk..",
            "................",
            "................"
        ]
    },
    {
        "id": "empyreal_soul",
        "en": "Empyreal Soul",
        "pt": "Alma Empírea (Empyreal Soul)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "G": "#fde047", "g": "#ca8a04", "k": "#713f12",
            "W": "#ffffff", "C": "#67e8f9", "c": "#0284c7"
        },
        "art": [
            "......GGGG......",
            "....GGGGGGGG....",
            "...GGgkCCCkgGG..",
            "..GGgkkWWkkkgGG.",
            ".GGgkkCCCCkkkgGG",
            ".GGgkCCCCCCkgGG.",
            "GGgkCCCCCCCCkgGG",
            "GGgkCCWCCCCkkgGG",
            "GGgkCCCCCCCCkgGG",
            "GGgkCCCCWCCkkgGG",
            ".GGgkCCCCCCkgGG.",
            ".GGgkkCCCCkkkgGG",
            "..GGgkkWWkkkgGG.",
            "...GGgkCCCkgGG..",
            "....GGGGGGGG....",
            "......GGGG......"
        ]
    },
    {
        "id": "serpentarius",
        "en": "Serpentarius",
        "pt": "Serpentário (Serpentarius)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "L": "#6ee7b7", "G": "#10b981", "d": "#064e3b", "k": "#022c22"
        },
        "art": [
            "......kkkk......",
            "....kkddddkk....",
            "...kddddddddk...",
            "..kddkGGGGkddk..",
            ".kddGGLWWLGkddk.",
            ".kddGLLkkLLGddk.",
            "kddGLkddddkLGddk",
            "kddGLkddddkLGddk",
            "kddkLGkddkGLkddk",
            "kdddkLGkkGLkdddk",
            ".kdddkLGGGLkddk.",
            ".kddddkLLLkkddk.",
            "..kddddkkddddk..",
            "...kddddddddk...",
            "....kkddddkk....",
            "......kkkk......"
        ]
    },
    {
        "id": "shadestone",
        "en": "Shadestone",
        "pt": "Pedra da Sombra (Shadestone)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "S": "#64748b", "s": "#475569", "d": "#334155", "p": "#3b0764", "k": "#1e293b"
        },
        "art": [
            "................",
            "......kkkk......",
            "....kkSSSSkk....",
            "...kSSSSSSSSk...",
            "..kSSssssssssk..",
            ".kSSsssssssssdk.",
            ".kSSssssppsssddk",
            ".kSssspppppssddk",
            ".kssspppppddsddk",
            "..kssspppdddddk.",
            "..ksssddddddddk.",
            "...kddddddddk...",
            "....kkddddkk....",
            "......kkkk......",
            "................",
            "................"
        ]
    },
    {
        "id": "galbana_lilies",
        "en": "Galbana Lilies",
        "pt": "Lírios de Galbana",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "R": "#ef4444", "r": "#dc2626", "d": "#991b1b",
            "Y": "#fde047", "G": "#22c55e", "g": "#15803d", "k": "#14532d"
        },
        "art": [
            ".....k....k.....",
            "....kRk..kRk....",
            "...kRYk..kRYk...",
            "...kRrk..kRrk...",
            "....krdk.krdk...",
            "..k..kkd.kdk..k.",
            ".kRk..krdkd..kRk",
            ".kRrk..krdk.kRrk",
            "..krdk.krdk.krdk",
            "...kddkkddkkddk.",
            "....kkGggGkk....",
            ".....kkGggGk....",
            "......kGggk.....",
            "......kGgk......",
            ".......kk.......",
            "................"
        ]
    },
    {
        "id": "cactus_bloom",
        "en": "Cactus Bloom",
        "pt": "Flor de Cacto",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "R": "#fb7185", "r": "#f43f5e", "d": "#be123c",
            "Y": "#fef08a", "y": "#facc15", "G": "#22c55e", "k": "#881337"
        },
        "art": [
            ".......kk.......",
            "......kRrk......",
            ".....kRRrrk.....",
            "....kRRRYrrk....",
            "...kRRRYYYrrk...",
            "..kRRRYyyyyrrk..",
            "..kRRYyyyyyyrk..",
            "..kRRyyyyyyyrk..",
            "...kRryyyyyrk...",
            "....kRryyyrk....",
            ".....kddddk.....",
            "......kGGk......",
            ".....kGGGGk.....",
            "......kGGk......",
            ".......kk.......",
            "................"
        ]
    },
    {
        "id": "wyrmfire_shot",
        "en": "Wyrmfire Shot",
        "pt": "Projétil Fogo de Dragão (Wyrmfire Shot)",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "W": "#ffffff", "Y": "#fef08a", "O": "#f97316", "R": "#ef4444",
            "B": "#facc15", "b": "#ca8a04", "k": "#78350f"
        },
        "art": [
            ".......kk.......",
            "......kYYk......",
            ".....kYWOYk.....",
            "....kYWOOOYk....",
            "...kYWOOOOORk...",
            "...kYROOOOORk...",
            "...kRRROOORRk...",
            "...kRRRRRRRRk...",
            "...kBBBBBBBBk...",
            "...kBBBBBBBBk...",
            "...kBBBBBBBBk...",
            "...kBBBBBBBBk...",
            "...kbbbbbbbbk...",
            "...kbbbbbbbbk...",
            "...kBBBBBBBBk...",
            "....kkkkkkkk...."
        ]
    },
    {
        "id": "broken_key",
        "en": "Broken Key",
        "pt": "Chave Quebrada",
        "parent": "minecraft:item/generated",
        "palette": {
            ".": None,
            "I": "#94a3b8", "i": "#64748b", "d": "#475569", "k": "#1e293b",
            "G": "#34d399", "g": "#059669"
        },
        "art": [
            "................",
            ".......kkk......",
            "......kIIIk.....",
            ".....kIIdIIk....",
            ".....kIdkGIIk...",
            "......kIIIIk....",
            ".......kddk.....",
            "........kd......",
            "................",
            "........kI......",
            "........kIIk....",
            "........kIdk....",
            "........kIIk....",
            "........kIdk....",
            ".........kk.....",
            "................"
        ]
    }
]

def main():
    print("==================================================")
    print("Projeto Ivalice - Generating Stage 2.5 Assets")
    print("==================================================")

    all_items = []
    all_items.extend(WEAPONS)
    all_items.extend(DEFENSIVE)
    all_items.extend(CONSUMABLES)
    all_items.extend(LOOT)
    all_items.extend(SPECIAL)
    all_items.extend(SPAWN_EGGS)
    all_items.extend(ADDITIONAL_ITEMS)

    print(f"Total items to process: {len(all_items)}")

    # Ensure directories exist
    os.makedirs(TEXTURES_DIR, exist_ok=True)
    os.makedirs(ITEMS_DIR, exist_ok=True)
    os.makedirs(MODELS_DIR, exist_ok=True)
    os.makedirs(LANG_DIR, exist_ok=True)

    # Load existing lang files
    en_us_path = os.path.join(LANG_DIR, "en_us.json")
    pt_br_path = os.path.join(LANG_DIR, "pt_br.json")

    en_us_data = {}
    if os.path.exists(en_us_path):
        with open(en_us_path, "r", encoding="utf-8") as f:
            en_us_data = json.load(f)

    pt_br_data = {}
    if os.path.exists(pt_br_path):
        with open(pt_br_path, "r", encoding="utf-8") as f:
            pt_br_data = json.load(f)

    generated_count = 0
    skipped_count = 0

    for item in all_items:
        item_id = item["id"]
        if item_id in PROTECTED_ITEMS:
            print(f"  [SKIP PROTECTED] {item_id}")
            skipped_count += 1
            continue

        # 1. Generate PNG Texture
        png_path = os.path.join(TEXTURES_DIR, f"{item_id}.png")
        if "pixels" in item:
            pixels = item["pixels"]
        elif "art" in item and "palette" in item:
            pixels = parse_ascii_art(item["art"], item["palette"])
        else:
            raise ValueError(f"Item {item_id} has neither 'pixels' nor 'art/palette' definition!")
        save_png_16x16(png_path, pixels)

        # 2. Generate Minecraft 26.3 item definition: assets/ivalice/items/<id>.json
        item_json_path = os.path.join(ITEMS_DIR, f"{item_id}.json")
        item_json_data = {
            "model": {
                "type": "minecraft:model",
                "model": f"ivalice:item/{item_id}"
            }
        }
        with open(item_json_path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(item_json_data, f, indent=2)
            f.write("\n")

        # 3. Generate item model: assets/ivalice/models/item/<id>.json
        model_json_path = os.path.join(MODELS_DIR, f"{item_id}.json")
        parent_model = item.get("parent", "minecraft:item/generated")
        model_json_data = {
            "parent": parent_model,
            "textures": {
                "layer0": f"ivalice:item/{item_id}"
            }
        }
        with open(model_json_path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(model_json_data, f, indent=2)
            f.write("\n")

        # 4. Update lang mappings if not already present
        lang_key = f"item.ivalice.{item_id}"
        if lang_key not in en_us_data:
            en_us_data[lang_key] = item["en"]
        if lang_key not in pt_br_data:
            pt_br_data[lang_key] = item["pt"]

        generated_count += 1

    # Save updated lang files WITHOUT UTF-8 BOM
    with open(en_us_path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(en_us_data, f, indent=2, ensure_ascii=False)
        f.write("\n")

    with open(pt_br_path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(pt_br_data, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print("--------------------------------------------------")
    print(f"Summary: Generated {generated_count} items ({skipped_count} skipped protected).")
    print("All textures and models written cleanly without UTF-8 BOM.")
    print("Updated en_us.json and pt_br.json successfully.")
    print("==================================================")

if __name__ == "__main__":
    main()
