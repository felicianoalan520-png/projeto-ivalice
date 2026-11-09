"""Register playable inventory records from the pinned TZA save tables, preserving existing IDs."""
import argparse
from collections import Counter
import colorsys
import json
from pathlib import Path
import re
import unicodedata

from generate_ivalice_settlements import ASSETS, RESOURCES, save_json
from ivalice_texture_atlas import RELIC_ORDER, inventory_icon, relic_icon

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


WEAPON_ICONS = {
    "BOW": "bow", "CROSSBOW": "crossbow", "STAFF": "wand", "ROD": "wand", "MEASURE": "wand",
    "SPEAR": "staff", "POLE": "staff", "AXE_HAMMER": "axe", "MACE": "mace",
    "DAGGER": "dagger", "GUN": "gun", "HAND_BOMB": "orb",
}
LOOT_ICONS = {
    "pelt": "pelt fur skin hide wool leather fleece", "fang": "fang tooth teeth talon claw",
    "bone": "bone skull", "horn": "horn antler tusk", "shell": "shell carapace scale husk",
    "feather": "feather plumage wing", "ore": "ore metal steel ingot iron silver gold electrum mythril",
    "herb": "grass leaf leaves flower root seed fruit moss greens onion tomato vegetable",
    "coin": "coin coins gil", "silk": "silk thread cotton cloth yarn linen",
}


def icon_for(name, kind, category):
    if kind == "weapon":
        return WEAPON_ICONS.get(category.removeprefix("WEAPON_"), "blade")
    words = set(slug(name).split("_"))
    if kind == "loot":
        return next((icon for icon, group in LOOT_ICONS.items() if words.intersection(group.split())), "crystal")
    if kind == "accessory":
        if words.intersection(("boots", "sandals", "shoes", "anklet")):
            return "boots"
        if words.intersection(("glove", "gloves", "cuffs", "gauntlets", "armguard")):
            return "glove"
    if kind == "consumable" and words.intersection(("fang", "bone", "sand", "meteorite", "stone", "mote")):
        return "fang" if "fang" in words else "crystal"
    return {"head": "helm", "chest": "armor", "shield": "shield", "accessory": "ring", "ammo": "arrow",
            "consumable": "bottle", "key": "key", "map": "scroll", "spell": "book",
            "technick": "scroll", "gambit": "scroll", "esper": "orb"}[kind]


def color_for(code, kind):
    hue = ((code * 47) % 360) / 360
    if kind == "map":
        hue = 0.12 + (code % 4) * 0.02
    red, green, blue = colorsys.hsv_to_rgb(hue, 0.32 + (code % 7) * 0.05, 0.92 + (code % 4) * 0.025)
    r, g, b = [round(255 * (.62 + value * .38)) for value in (red, green, blue)]
    return (r << 16 | g << 8 | b) - 0x1000000


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
            shape = icon_for(name, kind, category)
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
        inventory_icon(shape).save(ASSETS / f"textures/item/catalog/{shape}.png")
        save_json(ASSETS / f"models/item/catalog/{shape}.json",
                  {"parent": "minecraft:item/generated", "textures": {"layer0": image}})
    starter_icons = {
        "mythril_sword": "blade", "shortbow": "bow", "iron_spear": "staff", "altair": "gun",
        "potion": "bottle", "hi_potion": "bottle", "x_potion": "bottle",
        "clan_primer": "book", "license_tome": "book", "gambit_scroll": "scroll",
    }
    for item, shape in starter_icons.items():
        texture = ASSETS / f"textures/item/{item}.png"
        if texture.is_file():
            bottle_hue = {"hi_potion": 153, "x_potion": 193}.get(item)
            inventory_icon(shape, bottle_hue).save(texture)
    for relic in RELIC_ORDER:
        if relic.startswith("tza_"):
            save_json(ASSETS / f"items/{relic}.json", {"model": {
                "type": "minecraft:model", "model": f"ivalice:item/catalog/{relic}"
            }})
            save_json(ASSETS / f"models/item/catalog/{relic}.json", {
                "parent": "minecraft:item/generated", "textures": {"layer0": f"ivalice:item/catalog/{relic}"}
            })
            relic_icon(relic).save(ASSETS / f"textures/item/catalog/{relic}.png")
        else:
            relic_icon(relic).save(ASSETS / f"textures/item/{relic}.png")
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
