"""Register playable inventory records from the pinned TZA save tables, preserving existing IDs."""
import argparse
from collections import Counter
import colorsys
import json
from pathlib import Path
import re
import unicodedata

from generate_ivalice_settlements import ASSETS, RESOURCES, blank, save_json, save_png

WEAPONS = {
    "Sword": "SWORD", "Greatsword": "GREATSWORD", "Ninja Sword": "DAGGER", "Dagger": "DAGGER",
    "Katana": "KATANA", "Spear": "SPEAR", "Pole": "POLE", "Bow": "BOW", "Crossbow": "CROSSBOW",
    "Gun": "GUN", "Axe": "AXE_HAMMER", "Hammer": "AXE_HAMMER", "Rod": "ROD", "Staff": "STAFF",
    "Mace": "MACE", "Measure": "MEASURE", "Hand-bomb": "HAND_BOMB",
}


def slug(value):
    value = unicodedata.normalize("NFKD", value).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", "_", value.casefold()).strip("_")


def table(source, name):
    body = re.search(r"\[\] " + name + r" = \{(.*?)\n        \};", source, re.S)[1]
    return [json.loads(value) for value in re.findall(r'"(?:\\.|[^"\\])*"', body)]


def icon_for(kind, category):
    if kind == "weapon":
        if category in ("WEAPON_BOW", "WEAPON_CROSSBOW"):
            return "bow"
        if category in ("WEAPON_STAFF", "WEAPON_ROD", "WEAPON_MEASURE", "WEAPON_SPEAR", "WEAPON_POLE"):
            return "staff"
        if category in ("WEAPON_AXE_HAMMER", "WEAPON_MACE"):
            return "axe"
        if category == "WEAPON_GUN":
            return "gun"
        if category == "WEAPON_HAND_BOMB":
            return "orb"
        return "blade"
    return {"head": "helm", "chest": "armor", "shield": "shield", "accessory": "ring", "ammo": "arrow",
            "consumable": "bottle", "loot": "crystal", "key": "key", "map": "scroll", "spell": "book",
            "technick": "scroll", "gambit": "scroll", "esper": "orb"}[kind]


def icon_pixels(shape):
    pixels = blank(16)

    def point(x, y, gray):
        if 0 <= x < 16 and 0 <= y < 16:
            pixels[y][x] = (gray, gray, gray, 255)

    def line(x0, y0, x1, y1, size=1):
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            x, y = round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n)
            for dy in range(size):
                for dx in range(size):
                    point(x + dx, y + dy, 230 - 24 * dy - 15 * dx)

    if shape in ("blade", "staff", "axe", "arrow", "gun"):
        line(3, 12, 12, 3, 2)
        if shape == "blade":
            line(6, 8, 12, 2, 3)
            line(4, 7, 8, 11, 2)
            line(2, 13, 4, 13, 2)
        elif shape == "staff":
            for x, y in [(11, 2), (12, 1), (13, 2), (14, 3), (13, 4), (12, 5)]:
                point(x, y, 248)
        elif shape == "axe":
            for x in range(9, 15):
                for y in range(2, 7):
                    if (x - 11) ** 2 + (y - 4) ** 2 < 11:
                        point(x, y, 150 + 15 * (x - 9))
        elif shape == "arrow":
            line(9, 2, 13, 2)
            line(13, 2, 13, 6)
            line(2, 11, 4, 11)
            line(4, 12, 4, 14)
        elif shape == "gun":
            line(7, 8, 12, 3, 3)
            line(7, 8, 10, 10, 2)
    elif shape == "bow":
        line(4, 2, 6, 5)
        line(6, 5, 6, 10, 2)
        line(6, 10, 4, 13)
        line(4, 2, 4, 13)
        line(2, 7, 13, 7, 2)
        line(11, 5, 13, 7)
        line(13, 7, 11, 9)
    else:
        for y in range(2, 15):
            for x in range(1, 15):
                dx = abs(x - 7.5)
                dy = abs(y - 7.5)
                if shape == "helm":
                    on = (dx < 5.7 and 3 <= y < 10) or (y in (10, 11) and 3 <= dx < 5)
                elif shape == "armor":
                    on = (dx < 6 and 4 <= y <= 7) or (dx < 3.7 and 7 <= y <= 13)
                    on &= not (y < 6 and dx < 1.6)
                elif shape == "shield":
                    on = 2 <= y <= 13 and dx <= min(5.7, 13.8 - y)
                elif shape == "ring":
                    radius = (x - 7.5) ** 2 + (y - 8.5) ** 2
                    on = 9 < radius < 26 or (dy > 2 and y < 6 and dx < 1.6)
                elif shape == "key":
                    radius = (x - 6) ** 2 + (y - 5) ** 2
                    on = 2 <= radius < 15 or (5 <= x <= 7 and 8 <= y <= 13) or (y in (11, 13) and 7 <= x <= 10)
                elif shape == "bottle":
                    on = (dx < 2.6 and 2 <= y <= 6) or (dx < 4.5 and 6 <= y <= 13)
                elif shape == "orb":
                    on = dx * dx + dy * dy < 30
                elif shape == "crystal":
                    on = dx + dy * 0.72 < 4.7
                else:
                    on = dx < 5 and 3 <= y <= 12
                if on:
                    gray = 140 + ((x * 17 + y * 3) % 39) + (42 if x < 7 else 0)
                    if shape in ("book", "scroll") and 5 <= y <= 10 and y % 2 == 0:
                        gray += 40
                    if shape == "bottle" and y in (4, 8):
                        gray += 33
                    point(x, y, min(gray, 255))
    return pixels


def color_for(code, kind):
    hue = ((code * 47) % 360) / 360
    if kind == "map":
        hue = 0.12 + (code % 4) * 0.02
    red, green, blue = colorsys.hsv_to_rgb(hue, 0.32 + (code % 7) * 0.05, 0.92 + (code % 4) * 0.025)
    return (round(red * 255) << 16 | round(green * 255) << 8 | round(blue * 255)) - 0x1000000


def spell_school(code):
    if code < 0x3014 or code in (0x3040, 0x3041, 0x3042, 0x3043, 0x3044, 0x3045):
        return "MAGICK_WHITE"
    if code < 0x302B or 0x304C <= code <= 0x304E:
        return "MAGICK_BLACK"
    return "MAGICK_TIME_ARCANE"


def catalog(source):
    def rows(name, base, limit=None):
        return [(base + i, n.strip()) for i, n in enumerate(table(source, name)[:limit]) if n.strip()]

    for code, name in rows("Items", 0):
        if name == "Meteorite":
            name += f" ({code - 0x19 + 1})"
        yield code, name, "CONSUMABLE", "consumable"
    types = table(source, "EquipmentType")
    for code, name in rows("Equipment", 0x1000, 420):
        if name == "Unarmed":
            continue
        name = name.replace("C�chulainn", "Cúchulainn").replace("Diamon Shield", "Diamond Shield")
        kind = types[code - 0x1000]
        if kind in WEAPONS:
            yield code, name, f"WEAPON_{WEAPONS[kind]}", "weapon"
        elif kind.endswith(("helm", "armor")):
            defense = kind.split()[0].upper()
            yield code, name, f"DEFENSE_{defense}_ARMOR", "head" if kind.endswith("helm") else "chest"
        else:
            category, form = {
                "Shield": ("DEFENSE_SHIELD", "shield"), "Accessory": ("DEFENSE_ACCESSORY", "accessory"),
                "Weapon": ("ESPER_GLYPH", "esper"),
            }.get(kind, ("AMMUNITION", "ammo"))
            yield code, name, category, form
    for code, name in rows("Loot", 0x2000):
        yield code, name, "LOOT_MATERIAL", "loot"
    for code, name in rows("KeyItems", 0x8000):
        if 0x806D <= code <= 0x806F:
            continue  # Hunt status strings, not inventory objects.
        if code < 0x8060:
            yield code, name + (" Candle" if 0x8040 <= code < 0x8060 else " Map") if not name.endswith(" Candle") else name, "KEY_ITEM", "map"
        else:
            yield code, name, "KEY_ITEM", "key"
    for code, name in rows("Magicks", 0x3000):
        yield code, name, spell_school(code), "spell"
    for code, name in rows("Technicks", 0x4000):
        yield code, name, "TECHNICK", "technick"
    for code, name in rows("Gambits", 0x5000):
        yield code, "Gambit " + name, "KEY_ITEM", "gambit"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("editor", type=Path, help="checkout of mercurialuke/ffxii-save-editor")
    parser.add_argument("utils", type=Path, help="checkout of LuminaSapphira/ff12tza-utils")
    args = parser.parse_args()
    source = (args.editor / "editor/Tables.cs").read_text(encoding="utf-8")
    target = RESOURCES / "ivalice"
    target.mkdir(parents=True, exist_ok=True)
    langs = {lang: json.loads((ASSETS / f"lang/{lang}.json").read_text(encoding="utf-8"))
             for lang in ("en_us", "pt_br")}
    existing = {slug(re.sub(r"^(?:Magick|Technick|Esper Glyph):\s*", "", value)): key.removeprefix("item.ivalice.")
                for key, value in langs["en_us"].items() if key.startswith("item.ivalice.")
                and not key.startswith("item.ivalice.tza_")}
    known = set(existing.values())
    records = []
    icons = set()
    ranks = Counter()
    added = Counter()
    for code, name, category, kind in catalog(source):
        base = slug(name)
        alias = existing.get(base)
        if alias:
            id_ = alias
        else:
            id_ = f"tza_{base}"
            if id_ in known:
                id_ = f"{id_}_{code:04x}"
            known.add(id_)
            shape = icon_for(kind, category)
            icons.add(shape)
            save_json(ASSETS / f"items/{id_}.json", {"model": {
                "type": "minecraft:model", "model": f"ivalice:item/catalog/{shape}",
                "tints": [{"type": "minecraft:constant", "value": color_for(code, kind)}],
            }})
            for mapping in langs.values():
                mapping[f"item.ivalice.{id_}"] = name
            added[kind] += 1
        ranks[category] += 1
        rank = ranks[category]
        records.append(f"{code:04X}\t{id_}\t{category}\t{kind}\t{rank}\t{name}")

    for shape in sorted(icons):
        image = f"ivalice:item/catalog/{shape}"
        save_png(ASSETS / f"textures/item/catalog/{shape}.png", icon_pixels(shape))
        save_json(ASSETS / f"models/item/catalog/{shape}.json",
                  {"parent": "minecraft:item/generated", "textures": {"layer0": image}})
    for lang, mapping in langs.items():
        save_json(ASSETS / f"lang/{lang}.json", mapping)
    (target / "tza_catalog.tsv").write_text("\n".join(records) + "\n", encoding="utf-8", newline="\n")
    notice = RESOURCES / "META-INF/IVALICE-CATALOG-NOTICE.txt"
    notice.parent.mkdir(parents=True, exist_ok=True)
    notice.write_text(
        "Catalog reference data (names and type IDs).\n"
        "https://github.com/mercurialuke/ffxii-save-editor/tree/141173948633f8b673d7e8199b161c6c19c753c3\n"
        "https://github.com/LuminaSapphira/ff12tza-utils/tree/ef3fb92b4dbc5932b2800923447ddff247646aec\n\n"
        + (args.editor / "LICENSE").read_text(encoding="utf-8") + "\n"
        + (args.utils / "LICENSE").read_text(encoding="utf-8"),
        encoding="utf-8", newline="\n",
    )
    print(f"{len(records)} catalog records, {sum(added.values())} new registrations. New entries by kind: {dict(added)}")


if __name__ == "__main__":
    main()
