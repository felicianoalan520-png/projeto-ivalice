"""
Projeto Ivalice - 2D Item Sprite HD Upgrader (16x16 -> 32x32 HD)
Minecraft 26.3 / NeoForge

Upgrades all 2D item textures in:
- src/main/resources/assets/ivalice/textures/item/*.png
- src/main/resources/assets/ivalice/textures/item/catalog/*.png
from 16x16 to 32x32 HD ornate pixel-art sprites with:
- Crisp dark silhouettes & borders
- Specular rim highlights (top-left) & shadow depth (bottom-right)
- Subpixel internal micro-detailing
- Ornate metallic/arcane gradients
- Weapons: specular fuller reflections, ornate crossguards & pommels
- Armor/Shields: polished plate curvature, golden/silver rim inlays & crests
- Grimoires/Scrolls: arcane runic seals & gilded clasps
- Potions/Elixirs: embossed glass curvature, cork stoppers & effervescent bubbles
- Crystals/Magicite: geometric facet reflections & refractive inner glow
- Spawn Eggs: rich shaded oval curvature & contrasting monster spots
"""

import os
import glob
import math
import struct
import zlib

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
ITEM_DIR = os.path.join(BASE_DIR, "src", "main", "resources", "assets", "ivalice", "textures", "item")

# ==============================================================================
# 1. PURE PYTHON PNG CODEC (READ ALL BIT DEPTHS & COLOR TYPES, WRITE 32x32 RGBA)
# ==============================================================================

def read_png(filepath):
    with open(filepath, "rb") as f:
        data = f.read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"Not a PNG: {filepath}")
    idx = 8
    raw_data = bytearray()
    w, h = 0, 0
    bit_depth, color_type = 8, 6
    palette = []
    transparency = None

    while idx < len(data):
        length = struct.unpack(">I", data[idx:idx+4])[0]
        ctype = data[idx+4:idx+8]
        cdata = data[idx+8:idx+8+length]
        idx += 12 + length

        if ctype == b"IHDR":
            w, h, bit_depth, color_type, comp, filt, inter = struct.unpack(">IIBBBBB", cdata)
        elif ctype == b"PLTE":
            for i in range(0, len(cdata), 3):
                palette.append((cdata[i], cdata[i+1], cdata[i+2], 255))
        elif ctype == b"tRNS":
            transparency = cdata
        elif ctype == b"IDAT":
            raw_data.extend(cdata)
        elif ctype == b"IEND":
            break

    decomp = zlib.decompress(bytes(raw_data))
    pixels = []

    if color_type == 6:  # RGBA
        bpp = 4
        stride = w * bpp + 1
        prev_row = [0] * (w * 4)
        for y in range(h):
            filter_type = decomp[y * stride]
            scanline = list(decomp[y * stride + 1 : (y + 1) * stride])
            unfiltered = [0] * (w * 4)
            for x in range(w * 4):
                a = unfiltered[x - bpp] if x >= bpp else 0
                b = prev_row[x]
                c = prev_row[x - bpp] if x >= bpp else 0
                filt_val = scanline[x]
                if filter_type == 0:
                    val = filt_val
                elif filter_type == 1:
                    val = (filt_val + a) & 0xFF
                elif filter_type == 2:
                    val = (filt_val + b) & 0xFF
                elif filter_type == 3:
                    val = (filt_val + ((a + b) // 2)) & 0xFF
                elif filter_type == 4:
                    p = a + b - c
                    pa = abs(p - a)
                    pb = abs(p - b)
                    pc = abs(p - c)
                    pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                    val = (filt_val + pr) & 0xFF
                else:
                    val = filt_val
                unfiltered[x] = val
            prev_row = unfiltered
            row = []
            for x in range(w):
                row.append((unfiltered[x * 4], unfiltered[x * 4 + 1], unfiltered[x * 4 + 2], unfiltered[x * 4 + 3]))
            pixels.append(row)

    elif color_type == 2:  # RGB
        bpp = 3
        stride = w * bpp + 1
        prev_row = [0] * (w * 3)
        for y in range(h):
            filter_type = decomp[y * stride]
            scanline = list(decomp[y * stride + 1 : (y + 1) * stride])
            unfiltered = [0] * (w * 3)
            for x in range(w * 3):
                a = unfiltered[x - bpp] if x >= bpp else 0
                b = prev_row[x]
                c = prev_row[x - bpp] if x >= bpp else 0
                filt_val = scanline[x]
                if filter_type == 0:
                    val = filt_val
                elif filter_type == 1:
                    val = (filt_val + a) & 0xFF
                elif filter_type == 2:
                    val = (filt_val + b) & 0xFF
                elif filter_type == 3:
                    val = (filt_val + ((a + b) // 2)) & 0xFF
                elif filter_type == 4:
                    p = a + b - c
                    pa = abs(p - a)
                    pb = abs(p - b)
                    pc = abs(p - c)
                    pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                    val = (filt_val + pr) & 0xFF
                else:
                    val = filt_val
                unfiltered[x] = val
            prev_row = unfiltered
            row = []
            for x in range(w):
                row.append((unfiltered[x * 3], unfiltered[x * 3 + 1], unfiltered[x * 3 + 2], 255))
            pixels.append(row)

    elif color_type == 3:  # Indexed-colour
        bpp = 1
        stride = w * bpp + 1
        prev_row = [0] * w
        for y in range(h):
            filter_type = decomp[y * stride]
            scanline = list(decomp[y * stride + 1 : (y + 1) * stride])
            unfiltered = [0] * w
            for x in range(w):
                a = unfiltered[x - 1] if x > 0 else 0
                b = prev_row[x]
                c = prev_row[x - 1] if x > 0 else 0
                filt_val = scanline[x]
                if filter_type == 0:
                    val = filt_val
                elif filter_type == 1:
                    val = (filt_val + a) & 0xFF
                elif filter_type == 2:
                    val = (filt_val + b) & 0xFF
                elif filter_type == 3:
                    val = (filt_val + ((a + b) // 2)) & 0xFF
                elif filter_type == 4:
                    p = a + b - c
                    pa = abs(p - a)
                    pb = abs(p - b)
                    pc = abs(p - c)
                    pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                    val = (filt_val + pr) & 0xFF
                else:
                    val = filt_val
                unfiltered[x] = val
            prev_row = unfiltered
            row = []
            for x in range(w):
                pal_idx = unfiltered[x]
                color = palette[pal_idx] if pal_idx < len(palette) else (0, 0, 0, 0)
                if transparency and pal_idx < len(transparency):
                    color = (color[0], color[1], color[2], transparency[pal_idx])
                row.append(color)
            pixels.append(row)
    else:
        raise ValueError(f"Unsupported color type: {color_type}")

    return w, h, pixels

def save_png_32x32(filepath, pixels):
    raw = bytearray()
    for y in range(32):
        raw.append(0)  # Filter type: None
        for x in range(32):
            r, g, b, a = pixels[y][x]
            raw.extend((r, g, b, a))

    def make_chunk(tag, data):
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    ihdr = struct.pack(">IIBBBBB", 32, 32, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)
    png_bytes = b"\x89PNG\r\n\x1a\n" + make_chunk(b"IHDR", ihdr) + make_chunk(b"IDAT", idat) + make_chunk(b"IEND", b"")

    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, "wb") as f:
        f.write(png_bytes)

# ==============================================================================
# 2. COLOR & SHADING UTILITIES
# ==============================================================================

def shade_color(col, factor):
    r, g, b, a = col
    return (
        max(0, min(255, int(r * factor))),
        max(0, min(255, int(g * factor))),
        max(0, min(255, int(b * factor))),
        a
    )

def blend_color(base, overlay, alpha_factor=1.0):
    r1, g1, b1, a1 = base
    r2, g2, b2, a2 = overlay
    norm_a = (a2 / 255.0) * alpha_factor
    nr = int(r1 * (1.0 - norm_a) + r2 * norm_a)
    ng = int(g1 * (1.0 - norm_a) + g2 * norm_a)
    nb = int(b1 * (1.0 - norm_a) + b2 * norm_a)
    return (max(0, min(255, nr)), max(0, min(255, ng)), max(0, min(255, nb)), a1)

# ==============================================================================
# 3. 32x32 HD ORNATE PIXEL ART GENERATOR / UPGRADER
# ==============================================================================

def upgrade_sprite_32x32(item_name, orig_w, orig_h, src_px):
    """
    Transforms a 16x16 sprite into a rich, detailed 32x32 HD sprite with:
    - Crisp dark silhouettes
    - Directional light (specular rim top-left, shadow drop bottom-right)
    - Subpixel internal micro-detailing
    - Category-appropriate ornaments
    """
    # 1. Base 2x upscale
    grid = [[(0, 0, 0, 0) for _ in range(32)] for _ in range(32)]
    for sy in range(min(16, orig_h)):
        for sx in range(min(16, orig_w)):
            col = src_px[sy][sx]
            for dy in (0, 1):
                for dx in (0, 1):
                    grid[sy * 2 + dy][sx * 2 + dx] = col

    # 2. Compute solid mask & boundary detection
    solid = [[grid[y][x][3] > 15 for x in range(32)] for y in range(32)]
    is_border = [[False for _ in range(32)] for _ in range(32)]
    is_rim_top_left = [[False for _ in range(32)] for _ in range(32)]
    is_shadow_bot_right = [[False for _ in range(32)] for _ in range(32)]

    for y in range(32):
        for x in range(32):
            if not solid[y][x]:
                continue
            # Check 8-neighbors
            neighbors = []
            for ny in (y - 1, y, y + 1):
                for nx in (x - 1, x, x + 1):
                    if nx == x and ny == y:
                        continue
                    if 0 <= nx < 32 and 0 <= ny < 32:
                        neighbors.append(solid[ny][nx])
                    else:
                        neighbors.append(False)
            if not all(neighbors):
                is_border[y][x] = True

            # Directional rim checks
            has_empty_tl = (
                (y > 0 and not solid[y - 1][x]) or
                (x > 0 and not solid[y][x - 1]) or
                (y > 0 and x > 0 and not solid[y - 1][x - 1])
            )
            has_empty_br = (
                (y < 31 and not solid[y + 1][x]) or
                (x < 31 and not solid[y][x + 1]) or
                (y < 31 and x < 31 and not solid[y + 1][x + 1])
            )
            if has_empty_tl:
                is_rim_top_left[y][x] = True
            if has_empty_br:
                is_shadow_bot_right[y][x] = True

    # 3. Apply base pixel art shading & micro-detailing
    out = [[grid[y][x] for x in range(32)] for y in range(32)]
    for y in range(32):
        for x in range(32):
            if not solid[y][x]:
                continue
            col = grid[y][x]

            # Crisp dark outline on outer border
            if is_border[y][x]:
                dark_rim = shade_color(col, 0.44)
                col = blend_color(dark_rim, (20, 16, 24, 255), 0.40)
            elif is_rim_top_left[y][x]:
                # Specular rim highlight
                col = shade_color(col, 1.25)
                col = blend_color(col, (255, 255, 255, 255), 0.28)
            elif is_shadow_bot_right[y][x]:
                # Bottom-right shadow
                col = shade_color(col, 0.72)
            else:
                # Interior micro-detailing (diagonal subpixel lighting)
                sub_x = x % 2
                sub_y = y % 2
                if sub_x == 0 and sub_y == 0:
                    col = shade_color(col, 1.10)
                elif sub_x == 1 and sub_y == 1:
                    col = shade_color(col, 0.90)

            out[y][x] = col

    # 4. Category-Specific Ornate Enhancements
    lname = item_name.lower()

    # Category A: Weapons (Swords, Greatswords, Katanas, Daggers, Bows, Staves, Guns)
    is_weapon = any(w in lname for w in (
        "sword", "blade", "bow", "staff", "spear", "rod", "axe", "gun",
        "dagger", "durandal", "excalibur", "masamune", "tournesol",
        "wyrmhero", "deathbringer", "altair"
    ))
    if is_weapon:
        # Metallic fuller glint along center diagonal / blade line
        for y in range(4, 28):
            for x in range(4, 28):
                if solid[y][x] and not is_border[y][x]:
                    # Diagonal blade fuller reflection
                    if abs(x - y) <= 1 or abs(x - (31 - y)) <= 1:
                        if (x + y) % 3 == 0:
                            out[y][x] = blend_color(out[y][x], (255, 255, 255, 255), 0.45)
                    # Golden guard/pommel accents near hilt
                    if 13 <= x <= 18 and 13 <= y <= 18:
                        if (x * 3 + y * 7) % 5 == 0:
                            out[y][x] = blend_color(out[y][x], (250, 204, 21, 255), 0.60)

    # Category B: Shields & Armor & Helms
    is_armor = any(w in lname for w in (
        "shield", "armor", "helm", "suit", "mail", "cuirass", "greaves", "belt", "boots"
    ))
    if is_armor:
        # Curved dome specular highlight & golden center emblem
        for y in range(6, 26):
            for x in range(6, 26):
                if solid[y][x] and not is_border[y][x]:
                    # Curvature highlight on upper quadrant
                    if y < 14 and (x - y) in (0, 1, 2):
                        out[y][x] = blend_color(out[y][x], (255, 255, 255, 255), 0.35)
                    # Ornate golden crest in center
                    d = abs(x - 15.5) + abs(y - 15.5)
                    if d <= 3.0:
                        out[y][x] = blend_color(out[y][x], (245, 195, 65, 255), 0.55)

    # Category C: Spell Scrolls, Grimoires & Primers
    is_magic_book = any(w in lname for w in (
        "spell", "scroll", "book", "primer", "tome", "grimoire"
    ))
    if is_magic_book:
        # Arcane glowing rune in the center & golden corner clasps
        for y in range(4, 28):
            for x in range(4, 28):
                if solid[y][x] and not is_border[y][x]:
                    dx = abs(x - 15.5)
                    dy = abs(y - 15.5)
                    if (dx <= 1.0 and dy <= 4.0) or (dy <= 1.0 and dx <= 4.0) or (abs(dx - dy) <= 0.8 and dx <= 3.5):
                        out[y][x] = (56, 189, 248, 255)  # Arcane cyan glow
                    elif dx + dy <= 5.0 and (dx + dy) % 2 == 0:
                        out[y][x] = blend_color(out[y][x], (192, 132, 252, 255), 0.65)  # Arcane purple aura

    # Category D: Potions, Elixirs & Vials
    is_potion = any(w in lname for w in (
        "potion", "elixir", "antidote", "remedy", "ether", "bottle", "down"
    ))
    if is_potion:
        # Glass reflections & effervescent bubbles
        for y in range(6, 26):
            for x in range(8, 24):
                if solid[y][x] and not is_border[y][x]:
                    # Glass reflection streak on left shoulder
                    if x in (9, 10) and 10 <= y <= 20:
                        out[y][x] = blend_color(out[y][x], (255, 255, 255, 255), 0.55)
                    # Floating bubbles
                    if (x * 7 + y * 13) % 23 == 0:
                        out[y][x] = (255, 255, 255, 240)
                    # Brass stopper on top neck
                    if y <= 10 and 13 <= x <= 18:
                        out[y][x] = blend_color(out[y][x], (217, 119, 6, 255), 0.70)

    # Category E: Crystals, Magicite & Shards
    is_crystal = any(w in lname for w in (
        "crystal", "magicite", "shard", "stone", "gem", "teleport"
    ))
    if is_crystal:
        # Faceted geometric reflections
        for y in range(4, 28):
            for x in range(4, 28):
                if solid[y][x] and not is_border[y][x]:
                    facet = (x * 2 + y * 3) % 7
                    if facet in (0, 1):
                        out[y][x] = blend_color(out[y][x], (255, 255, 255, 255), 0.40)
                    elif facet in (5, 6):
                        out[y][x] = shade_color(out[y][x], 0.78)
                    # Diamond gleam at the top apex
                    if 6 <= y <= 9 and 14 <= x <= 17:
                        out[y][x] = (255, 255, 255, 255)

    # Category F: Spawn Eggs
    is_egg = "spawn_egg" in lname
    if is_egg:
        # Rich oval curvature & crisp creature spots
        for y in range(4, 28):
            for x in range(6, 26):
                if solid[y][x] and not is_border[y][x]:
                    dx = (x - 15.5) / 9.0
                    dy = (y - 16.0) / 11.0
                    d2 = dx * dx + dy * dy
                    if d2 < 0.95:
                        dome = math.sqrt(max(0.0, 1.0 - d2))
                        if y < 12 and x < 16:
                            out[y][x] = blend_color(out[y][x], (255, 255, 255, 255), 0.42 * dome)
                        spot = ((x // 3) * 7 + (y // 3) * 11) % 13
                        if spot in (0, 1):
                            out[y][x] = shade_color(out[y][x], 0.60)
                        elif spot in (5, 6):
                            out[y][x] = blend_color(out[y][x], (250, 204, 21, 255), 0.55)

    return out

# ==============================================================================
# 4. BATCH UPGRADE PIPELINE
# ==============================================================================

def main():
    print("================================================================")
    print(" Projeto Ivalice - 2D Item Sprite HD Upgrader (16x16 -> 32x32)")
    print("================================================================")

    all_pngs = sorted(glob.glob(os.path.join(ITEM_DIR, "**", "*.png"), recursive=True))
    print(f"Found {len(all_pngs)} item PNG textures to upgrade in-place.")

    upgraded_count = 0
    for fpath in all_pngs:
        fname = os.path.basename(fpath)
        rel_path = os.path.relpath(fpath, ITEM_DIR)

        w, h, px = read_png(fpath)
        out_px = upgrade_sprite_32x32(fname, w, h, px)
        save_png_32x32(fpath, out_px)

        upgraded_count += 1
        if upgraded_count % 20 == 0 or upgraded_count == len(all_pngs):
            print(f"  [{upgraded_count}/{len(all_pngs)}] Upgraded {rel_path} to 32x32 HD")

    print("\n================================================================")
    print(f" Successfully upgraded all {upgraded_count} item textures to 32x32 HD!")
    print("================================================================")

if __name__ == "__main__":
    main()
