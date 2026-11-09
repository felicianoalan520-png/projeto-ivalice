"""Build factual bestiary data and original GeckoLib variants from the reference tables."""
import argparse
from collections import Counter, defaultdict
from copy import deepcopy
from hashlib import sha256
from html import unescape
import json
from math import sqrt
from pathlib import Path
import re

from generate_geckolib_entities import (
    ENTITIES_DATA, build_animation_json, build_geometry_json, generate_texture,
    pack_model_cubes, snap_cube_to_pixels,
)
from generate_tza_catalog import slug
from ivalice_texture_atlas import write_entity_texture
from generate_ivalice_settlements import ASSETS, DATA, RESOURCES, blank, save_json, save_png

LOCATIONS = {
    "dalmasca estersand": "dalmasca_estersand", "dalmasca westersand": "dalmasca_westersand",
    "dalmasca sands": "dalmasca_estersand dalmasca_westersand",
    "giza plains - the dry": "giza_plains_dry", "giza plains - the rains": "giza_plains_rains",
    "giza plains": "giza_plains_dry giza_plains_rains",
    "ozmone plain": "ozmone_plain", "golmore jungle": "golmore_jungle", "phon coast": "phon_coast",
    "tchita uplands": "tchita_uplands", "paramina rift": "paramina_rift", "salikawood": "salikawood",
    "feywood": "feywood", "mosphoran highwaste": "mosphoran_highwaste", "nabreus deadlands": "nabreus_deadlands",
    "ogir-yensa sandsea": "ogir_yensa_sandsea", "nam-yensa sandsea": "ogir_yensa_sandsea",
    "yensan sandsea": "ogir_yensa_sandsea", "cerobi steppe": "cerobi_steppe",
}
# Corresponding regions for places that do not have their own generation yet.
REGIONAL_CONTRACTS = {
    "garamsythe": "giza_plains_rains", "barheim": "dalmasca_estersand", "lhusu": "mosphoran_highwaste",
    "raithwall": "ogir_yensa_sandsea", "henne": "ozmone_plain",
    "stilshrine": "paramina_rift", "bur-omisace": "paramina_rift", "sochen": "tchita_uplands",
    "giruvegan": "feywood", "great crystal": "feywood", "ridorana": "phon_coast", "pharos": "phon_coast",
    "subterra": "nabreus_deadlands", "zertinan": "dalmasca_westersand", "necrohol": "nabreus_deadlands",
    "nalbina": "dalmasca_estersand", "leviathan": "ogir_yensa_sandsea", "draklor": "tchita_uplands",
    "bahamut": "dalmasca_westersand", "archadian empire": "tchita_uplands", "trial mode": "cerobi_steppe",
}
BOSS_HABITATS = {
    "air_cutter_remora": "Nalbina Fortress", "doctor_cid": "Draklor Laboratory, Pharos",
    "ba_gamnan": "Lhusu Mines", "garuda": "Tomb of Raithwall", "demon_wall": "Tomb of Raithwall",
    "elder_wyrm": "Golmore Jungle", "king_bomb": "Salikawood", "ahriman": "Sochen Cave Palace",
    "rafflesia": "The Feywood", "daedalus": "Giruvegan", "tyrant": "Giruvegan", "hydro": "Pharos",
    "fury": "Necrohol of Nabudis", "humbaba_mistant": "Necrohol of Nabudis",
    "mimic_queen": "Barheim Passage", "firemane": "Garamsythe Waterway", "vinuskar": "Stilshrine of Miriam",
    "judge_ghis": "Dreadnought Leviathan", "gabranth": "Sky Fortress Bahamut",
    "belias": "Tomb of Raithwall", "mateus": "Stilshrine of Miriam", "hashmal": "Pharos",
    "shemhazai": "Giruvegan", "famfrit": "Pharos", "adrammelech": "Zertinan Caverns",
    "ultima": "Great Crystal", "chaos": "Necrohol of Nabudis", "cuchulainn": "Garamsythe Waterway",
    "zodiark": "Henne Mines",
    "yiazmat": "Ridorana Cataract", "enkidu": "Lhusu Mines", "chickatrice": "Giza Plains - The Dry",
}
EXISTING = {"yellow_chocobo": "chocobo", "crusader": "skeleton_crusader", "gabranth": "judge_gabranth"}
# This ID is already used by the interdimensional encounter.
CONFLICTS = {"mindflayer": "ivalice_mindflayer", "adamantoise": "ivalice_adamantoise"}
ITEM_ALIASES = {
    "maximillian": "maximilian", "battle_bamboo": "battle_bamboo", "shadow_mote": "dark_mote",
    "aqua_mote": "aquara_mote", "freezing_fang": "rime_fang", "thunder_fang": "lightning_fang",
    "alarm_clock": "prince_s_kiss", "smelling_salts": "nu_khai_sand", "c9h8o4": "serum",
    "barutoro_no_tane": "baltoro_seed", "iron_helmet": "iron_helm", "great_serpentskin": "great_serpentskin",
}
# Preserve the biome-sensitive Zodiac Age tables that are already implemented.
EXISTING_DROPS = {"hyena", "giza_rabbit", "malboro", "wolf", "cactoid", "cockatrice",
                  "ichthon", "wild_saurian", "mandragora", "yeti", "bomb", "white_wolf"}
MAGIC = [
    ("scathe", "SCATHE"), ("ardor", "ARDOR"), ("flare", "FLARE"), ("holy", "HOLY"),
    ("firaga", "FIRAGA"), ("blizzaga", "BLIZZAGA"), ("thundaga", "THUNDAGA"), ("aeroga", "AEROGA"),
    ("waterga", "WATERGA"), ("aquaga", "WATERGA"), ("darkga", "DARKGA"), ("fira", "FIRA"),
    ("blizzara", "BLIZZARA"), ("thundara", "THUNDARA"), ("aerora", "AERORA"), ("aquara", "AQUARA"),
    ("darkra", "DARKRA"), ("fire", "FIRE"), ("blizzard", "ICE"), ("thunder", "THUNDER"),
    ("aero", "WIND"), ("water", "WATER"), ("aqua", "WATER"), ("dark", "DARK"), ("bio", "DARKRA"),
]
FAMILIES = {
    "wolf": ("wolf", .85, .95), "hound": ("hyena", 1, 1), "cat": ("coeurl", 1.05, 1.15),
    "beast": ("behemoth", 1.35, 1.55), "horse": ("sleipnir", 1.25, 1.65),
    "hare": ("giza_rabbit", .55, .7), "rat": ("rodent", .57, .55),
    "lizard": ("croc", 1.1, .75), "saurian": ("wild_saurian", 1.28, 1.65),
    "dragon": ("yiazmat", 1.75, 1.75), "wyvern": ("wyvern", 1.35, 1.5), "snake": ("serpent", .9, 1.2),
    "toad": ("toad", 1.1, .85), "turtle": ("gil_turtle", 1.3, .95),
    "fish": ("ichthon", .75, .75), "chocobo": ("chocobo", .9, 1.65), "bird": ("raptor", .85, .9),
    "cockatrice": ("cockatrice", .9, .95), "bat": ("bat", .85, .7),
    "mantis": ("mantis", .95, 1.35), "mimic": ("mimic", 1.05, .9),
    "malboro": ("malboro", 1.15, 1.2), "treant": ("treant", 1.15, 1.7), "mandragora": ("mandragora", .63, .95),
    "flan": ("white_mousse", 1.05, .85), "bomb": ("bomb", .9, .95),
    "elemental": ("elemental", 1, 1.3), "ghost": ("wraith", .9, 1.65),
    "undead": ("skeleton_crusader", .85, 1.55), "zombie": ("zombie", .85, 1.55),
    "gargoyle": ("gargoyle", 1.1, 1.45), "giant": ("yeti", 1.13, 1.65),
    "humanoid": ("judge_gabranth", .85, 1.85), "golem": ("iron_giant", 1.25, 2.0),
    "facer": ("facer", .9, 1.25), "esper": ("esper", 1.4, 2.15), "mech": ("omega_mk", 1.25, 1.45),
}
FAMILY_RULES = [
    ("elemental", "elemental entite chaosjet"), ("cockatrice", "cockatrice chickatrice cluckatrice"),
    ("chocobo", "chocobo trickster"), ("turtle", "titantoise tortoise rocktoise snapper darksteel"),
    ("mantis", "mantis antlion"), ("mimic", "mimic crystalbug"), ("snake", "serpent basilisk marilith"),
    ("lizard", "gator croc lizard"), ("saurian", "tyrant saurian tyrannosaur"), ("wyvern", "wyvern aevis garuda"),
    ("dragon", "wyrm dragon fafnir"), ("malboro", "malboro rafflesia carrot"), ("treant", "treant"),
    ("mandragora", "mandragora nightshade cactus cactoid alraune onion pumpkin topstalk"),
    ("flan", "flan amorph mousse orthros jelly slime"), ("bomb", "bomb balloon"), ("fish", "piranha ichthian ichthon yensa slyt"),
    ("bat", "steeling vampyr seeker bloodwing"), ("bird", "diver avion phoenix vulture"),
    ("hare", "dreamhare bunny wyrdhare"), ("rat", "rat rodent"), ("wolf", "wolf lobo dingo"),
    ("hound", "hellhound cerberus hound"), ("cat", "coeurl panther"), ("horse", "sleipnir nightmare ixion"),
    ("beast", "behemoth chimera fenrir"), ("toad", "toad"), ("ghost", "ghost reaper wraith lich ahriman mindflayer"),
    ("undead", "skeleton crusader skull bones"), ("zombie", "zombie corpse ghoul"),
    ("gargoyle", "gargoyle imp fiend diabolos"), ("facer", "facer"), ("giant", "headless slaven yeti"),
    ("humanoid", "imperial bangaa seeq garif baknamy humanoid judge"),
    ("mech", "automaton warmech omega armament"), ("golem", "construct golem guardian"),
]
MARK_MODELS = {
    "the_undying": "dragon", "sephira": "elemental", "vayne_novus": "humanoid", "vayne": "humanoid",
    "enkidu": "hound", "pandaemonium": "turtle", "gilgamesh": "humanoid", "shadowseer": "ghost",
    "piscodaemon": "ghost", "roblon": "golem", "demon_wall": "facer", "mimic_queen": "mimic",
    "humbaba_mistant": "giant", "elder_wyrm": "dragon", "firemane": "horse",
}
FALLBACK_REGIONS = {
    "fish": "phon_coast", "bat": "mosphoran_highwaste", "toad": "giza_plains_rains",
    "elemental": "feywood", "ghost": "nabreus_deadlands", "undead": "nabreus_deadlands",
    "zombie": "nabreus_deadlands", "gargoyle": "golmore_jungle", "mandragora": "salikawood",
    "malboro": "golmore_jungle", "treant": "golmore_jungle", "esper": "feywood",
    "humanoid": "tchita_uplands", "mech": "tchita_uplands", "golem": "mosphoran_highwaste",
    "mimic": "mosphoran_highwaste", "dragon": "cerobi_steppe", "wyvern": "cerobi_steppe",
    "saurian": "dalmasca_estersand", "snake": "tchita_uplands", "lizard": "giza_plains_rains",
    "turtle": "phon_coast", "bomb": "salikawood", "flan": "nabreus_deadlands",
}
REGION_LEVELS = {
    "dalmasca_estersand": 5, "dalmasca_westersand": 9, "giza_plains_dry": 6, "giza_plains_rains": 14,
    "ogir_yensa_sandsea": 16, "ozmone_plain": 20, "golmore_jungle": 24, "paramina_rift": 26,
    "mosphoran_highwaste": 31, "salikawood": 32, "phon_coast": 34, "tchita_uplands": 38,
    "feywood": 42, "nabreus_deadlands": 46, "cerobi_steppe": 48,
}


def key(name):
    return slug(name.replace("�", "ú").replace("’", "'"))


def plain(html):
    return unescape(re.sub(r"<[^>]*>", "", html)).strip()


def references(index_path, hunts_path):
    regions, hunts = {}, {}
    text = index_path.read_text(encoding="utf-8")
    for area, contents in re.findall(r"<h3>(.*?)</h3>\s*<table\b[^>]*>(.*?)</table>", text, re.S):
        area = plain(area)
        if area.lower().removeprefix("the ") not in LOCATIONS and not any(word in area.lower() for word in REGIONAL_CONTRACTS):
            continue
        for cell in re.findall(r"<td\b[^>]*>(.*?)</td>", contents, re.S):
            for name in re.split(r"<br\s*/?>", cell):
                regions[key(plain(name))] = area
    for row in re.findall(r"<tr\b[^>]*>(.*?)</tr>", hunts_path.read_text(encoding="utf-8"), re.S):
        cells = re.findall(r"<td\b[^>]*>(.*?)</td>", row, re.S)
        if len(cells) != 4 or "<strong>" not in cells[0]:
            continue
        name = plain(re.search(r"<strong>(.*?)</strong>", cells[0], re.S)[1])
        habitat = plain(re.split(r"<br\s*/?>", cells[2])[0])
        rank = re.search(r"Rank\s+([IVX]+)", plain(cells[0]))
        reward = re.search(r"([\d,]+)\s+Gil", plain(cells[3]))
        hunts[key(name)] = (habitat, rank[1] if rank else "", int(reward[1].replace(",", "")) if reward else 0)
    return regions, hunts


def habitat_regions(locations):
    natural, contract = [], []
    for place in locations:
        place = place.lower().removeprefix("the ").split(" (")[0].strip(" *")
        matches = [value for area, value in LOCATIONS.items() if place == area or place.startswith(area + ",")]
        if "giza plains" in place:
            matches = ["giza_plains_rains" if "rain" in place else "giza_plains_dry" if "dry" in place else LOCATIONS["giza plains"]]
        for region in matches:
            natural.extend(region.split())
        for area, region in REGIONAL_CONTRACTS.items():
            if area in place:
                contract.append(region)
    return list(dict.fromkeys(natural)), list(dict.fromkeys(contract))


def lookup_items():
    names = {}
    for line in (RESOURCES / "ivalice/tza_catalog.tsv").read_text(encoding="utf-8").splitlines():
        _, id_, _, _, _, name = line.split("\t", 5)
        names[key(name)] = id_
    return names


def parse_loot(text, items):
    drops = []
    for odds, raw in re.findall(r"\[([\d.]+)%\]\s*([^\n]+)", text or ""):
        choices = []
        for part in raw.split(", "):
            name = key(part.strip().strip("*"))
            gil = re.fullmatch(r"(\d+)_gil", name.replace(",", ""))
            if gil:
                choices.append("gil:" + gil[1])
                continue
            id_ = items.get(ITEM_ALIASES.get(name, name))
            if id_ and id_ not in choices:
                choices.append(id_)
        if choices:
            drops.append({"chance": float(odds) / 100, "items": choices})
    return drops


def family_for(row):
    name = key(row["name"])
    if name in MARK_MODELS:
        return MARK_MODELS[name]
    if row["type"] == "Esper":
        return "esper"
    species = slug(row["species"]).replace("_", " ")
    for family, words in FAMILY_RULES:
        if any(re.search(r"\b" + word + r"\b", species) for word in words.split()):
            return family
    for family, words in FAMILY_RULES:
        if any(word in name.split("_") for word in words.split()):
            return family
    return "humanoid" if row["type"] == "Boss" else "beast"


def first_number(variations, field):
    for variant in variations:
        match = re.search(r"\d[\d,]*", str(variant.get(field) or ""))
        if match and int(match[0].replace(",", "")) > 0:
            return int(match[0].replace(",", ""))
    return 0


def extra_foes():
    bahamut = "Sky Fortress Bahamut"
    facts = [
        ("Vayne", "Imperial/Archadian Empire", "Boss", bahamut, 50, 76755, "", ""),
        ("Vayne Novus", "Imperial/Archadian Empire", "Boss", bahamut, 53, 104210, "Aeroga", ""),
        ("The Undying", "Fell Wyrm/Dragon", "Boss", bahamut, 55, 228299, "Firaga", "[3%] Dark Matter"),
        ("Sephira", "Elemental/Construct", "Normal enemy", bahamut, 50, 12121, "", "[3%] Hi-Ether"),
        ("Judge Drace", "Judge/Imperial Army", "Boss", "Trial Mode - Stage 100", 60, 55000, "Darkga", ""),
        ("Judge Zargabaath", "Judge/Imperial Army", "Boss", "Trial Mode - Stage 100", 60, 60000, "", ""),
    ]
    for name, species, kind, place, level, hp, magicks, steal in facts:
        yield {"name": name, "species": species, "type": kind, "aggression": "Aggressive",
               "variations": [{"location": place, "level": level, "hp": hp}],
               "magicks": magicks, "items": {"steal": steal}, "affinities": {"holy": .5} if name == "The Undying" else {}}


def create_records(source, index, hunts):
    regional, hunt_facts = references(index, hunts)
    items = lookup_items()
    rows = json.loads(source.read_text(encoding="utf-8"))
    indexed = {key(row["name"]) for row in rows}
    rows.extend(row for row in extra_foes() if key(row["name"]) not in indexed)
    records = []
    for row in sorted(rows, key=lambda row: key(row["name"])):
        name = row["name"].replace("�", "ú")
        name_key = key(name)
        id_ = EXISTING.get(name_key, CONFLICTS.get(name_key, name_key))
        original = id_ in ENTITIES_DATA
        type_ = row["type"]
        kind = {"Rare Game": "Rare Game", "Mark": "Mark", "Elite Mark": "Elite Mark",
                "Ultimate Mark": "Elite Mark", "Esper": "Esper", "Boss": "Boss"}.get(type_, "Normal")
        family = family_for(row)
        variations = row.get("variations", [])
        facts = hunt_facts.get(name_key)
        locations = [x["location"] for x in variations if x.get("location")]
        if facts:
            locations.insert(0, facts[0])
        elif not locations and name_key in BOSS_HABITATS:
            locations.append(BOSS_HABITATS[name_key])
        if not locations and name_key in regional:
            locations.append(regional[name_key])
        natural, contract = habitat_regions(locations)
        regions = natural or contract or [FALLBACK_REGIONS.get(family, "ozmone_plain")]
        aquatic = family == "fish"
        mode = "water" if aquatic else "ground"
        if kind != "Normal" or not natural or not locations:
            mode = "contract"
        neutral = row.get("aggression", "").lower().startswith(("docile", "passive", "neutral"))
        strength = first_number(variations, "level")
        if name_key == "dive_talon" and not strength:
            strength = 27
        level = min(99, strength or REGION_LEVELS[regions[0]] + {"Mark": 5, "Elite Mark": 12, "Rare Game": 7, "Boss": 13, "Esper": 18}.get(kind, 0))
        if facts and facts[1]:
            kind = kind + " " + facts[1]
        model, width, height = FAMILIES[family]
        boss = type_ in ("Boss", "Esper", "Elite Mark", "Ultimate Mark")
        base_hp = first_number(variations, "hp")
        hp = round(12 + 3.5 * level + min(55, sqrt(base_hp) * 0.22) + (65 + level * 2 if boss else 0))
        loot = row.get("items", {})
        drops = loot.get("dropChain") or [loot.get("drop", "")]
        steal = loot.get("steal", [])
        steal = [steal] if isinstance(steal, str) else steal
        magicks = str(row.get("magicks") or "").lower()
        attack = next((style for word, style in MAGIC if re.search(r"\b" + word + r"\b", magicks)), "")
        records.append({
            "id": id_, "name": name, "family": row["species"], "kind": kind, "model": family, "original": original,
            "habitat": re.sub(r"\s+", " ", locations[0]) if locations else "",
            "regions": regions, "mode": mode, "aquatic": aquatic, "neutral": neutral,
            "width": width, "height": height, "level": level, "hp": hp,
            "attack": round(2.2 + 0.17 * level + (2 if boss else 0), 2),
            "speed": .32 if family in ("wolf", "cat", "hare", "rat", "bird") else .24 if family in ("golem", "turtle", "flan") else .28,
            "gil": facts[2] if facts and facts[2] else level * (24 if boss else 7),
            "drops": [] if id_ in EXISTING_DROPS else [parse_loot(text, items) for text in drops],
            "vanillaLoot": id_ in EXISTING_DROPS, "steal": [parse_loot(text, items) for text in steal],
            "poach": parse_loot(loot.get("poach", ""), items),
            "affinities": {("thunder" if e == "lightning" else e): amount
                           for e, amount in (row.get("affinities") or {}).items() if isinstance(amount, (float, int))},
            "move": attack,
        })
    if len({r["id"] for r in records}) != len(records):
        raise ValueError("Duplicate monster ID")
    return records


def bone(name, pivot, origin, size, parent="root"):
    return {"name": name, "pivot": pivot, "parent": parent, "cubes": [{"origin": origin, "size": size}]}


def style_model(base, shapes, gait):
    model = deepcopy(ENTITIES_DATA[base])
    bones = [{"name": "root", "pivot": [0, 0, 0]}, *[bone(*shape) for shape in shapes]]
    model["bones"] = bones
    for name in ("idle", "walk", "run", "strike", "cast", "flinch"):
        effect = model["animations"][name]
        duration = effect["animation_length"]
        effect["bones"] = {}
        if name in ("walk", "run"):
            factor = 1.5 if name == "run" else 1
            effect["bones"] = {
                b: {"rotation": {"0": [0, 0, 0], str(duration / 4): [x * factor for x in angles],
                                  str(3 * duration / 4): [-x * factor for x in angles], str(duration): [0, 0, 0]}}
                for b, angles in gait
            }
        height, advance = {"idle": (.28, 0), "walk": (.65, 0), "run": (1.1, 0),
                           "strike": (1.3, -4), "cast": (2.4, 0), "flinch": (.6, 1.2)}[name]
        effect["bones"]["root"] = {"position": {"0": [0, 0, 0], str(duration / 2): [0, height, advance],
                                                str(duration): [0, 0, 0]}}
    return model


def new_family_models():
    # Sizes and poses use Bedrock model pixels. Sides have separate pivots for gait.
    models = {}
    def add(name, base, shapes, gait=()):
        models[name] = style_model(base, shapes, gait)

    add("toad", "white_mousse", [
        ("body", [0, 4, 0], [-5, 2, -3], [10, 6, 9]),
        ("head", [0, 5, -2], [-5, 3, -7], [10, 5, 5], "body"),
        ("eye_left", [3, 8, -5], [2, 7, -6], [3, 3, 3], "head"),
        ("eye_right", [-3, 8, -5], [-5, 7, -6], [3, 3, 3], "head"),
        ("leg_left", [4, 4, 4], [4, 0, 1], [4, 4, 6]),
        ("leg_right", [-4, 4, 4], [-8, 0, 1], [4, 4, 6]),
    ], [("leg_left", [26, 0, 0]), ("leg_right", [26, 0, 0])])
    add("serpent", "wild_saurian", [
        ("body", [0, 3, 3], [-3, 0, -3], [6, 5, 10]),
        ("neck", [0, 4, -3], [-2, 3, -6], [4, 10, 4]),
        ("head", [0, 12, -4], [-3, 11, -9], [6, 5, 6], "neck"),
        ("horns", [0, 13, -4], [-4, 10, -3], [8, 6, 1], "head"),
        ("tail", [0, 2, 6], [-2, 0, 6], [4, 3, 7]),
    ], [("tail", [0, 28, 0]), ("neck", [0, -16, 0])])
    add("bat", "cockatrice", [
        ("body", [0, 6, 0], [-2, 4, -2], [4, 6, 5]), ("head", [0, 8, -2], [-2, 7, -5], [4, 4, 4]),
        ("wing_left", [2, 7, 0], [2, 7, -1], [8, 1, 6]),
        ("wing_right", [-2, 7, 0], [-10, 7, -1], [8, 1, 6]),
        ("ear_left", [1, 11, -3], [1, 10, -3], [1, 3, 1], "head"),
        ("ear_right", [-1, 11, -3], [-2, 10, -3], [1, 3, 1], "head"),
    ], [("wing_left", [0, 0, 38]), ("wing_right", [0, 0, -38])])
    add("raptor", "chocobo", [
        ("body", [0, 8, 0], [-3, 5, -3], [6, 7, 8]),
        ("head", [0, 11, -3], [-2, 9, -6], [4, 5, 4], "body"),
        ("talons_beak", [0, 10, -5], [-1, 8, -9], [2, 4, 4], "head"),
        ("wing_left", [3, 10, 0], [3, 9, -2], [8, 2, 6], "body"),
        ("wing_right", [-3, 10, 0], [-11, 9, -2], [8, 2, 6], "body"),
        ("tail", [0, 8, 4], [-3, 7, 4], [6, 2, 8], "body"),
        ("talons_left", [1, 6, 0], [1, 2, -3], [2, 4, 5], "body"),
        ("talons_right", [-1, 6, 0], [-3, 2, -3], [2, 4, 5], "body"),
    ], [("wing_left", [0, 0, 42]), ("wing_right", [0, 0, -42])])
    models["raptor"]["colors"].update({"base": "#a69172", "shadow": "#4a4138", "highlight": "#d8c19d",
                                       "wings": "#806f59", "talons": "#d6a55b"})
    add("elemental", "bomb", [
        ("body", [0, 12, 0], [-4, 8, -4], [8, 8, 8]),
        ("head", [0, 12, -2], [-3, 9, -5], [6, 6, 2], "body"),
        ("halo", [0, 12, 0], [-6, 11, -6], [12, 2, 12]),
        ("crest", [0, 17, 0], [-2, 16, -2], [4, 5, 4]),
        ("tail", [0, 8, 0], [-2, 3, -2], [4, 5, 4]),
    ], [("halo", [0, 32, 12])])
    add("wraith", "mateus", [
        ("robe", [0, 13, 0], [-4, 4, -3], [8, 14, 6]),
        ("head", [0, 18, -1], [-3, 16, -4], [6, 7, 6]),
        ("arm_left", [4, 15, -1], [3, 6, -2], [4, 10, 4]),
        ("arm_right", [-4, 15, -1], [-7, 6, -2], [4, 10, 4]),
    ], [("arm_left", [-20, 0, -8]), ("arm_right", [20, 0, 8])])
    add("mantis", "wild_saurian", [
        ("body", [0, 8, 0], [-3, 4, 0], [6, 6, 9]), ("neck", [0, 8, 0], [-2, 8, -4], [4, 9, 4]),
        ("head", [0, 16, -3], [-3, 15, -7], [6, 5, 5], "neck"),
        ("arm_left", [2, 13, -3], [2, 10, -10], [2, 4, 7], "neck"),
        ("arm_right", [-2, 13, -3], [-4, 10, -10], [2, 4, 7], "neck"),
        ("leg_left", [3, 6, 2], [3, 0, 0], [3, 6, 6]),
        ("leg_right", [-3, 6, 2], [-6, 0, 0], [3, 6, 6]),
    ], [("leg_left", [26, 0, 0]), ("leg_right", [-26, 0, 0]), ("arm_left", [16, 0, 0]), ("arm_right", [-16, 0, 0])])
    add("mimic", "bomb", [
        ("body", [0, 5, 0], [-6, 3, -4], [12, 5, 8]),
        ("head", [0, 9, 3], [-6, 8, -4], [12, 5, 8]),
        ("fangs", [0, 8, -4], [-5, 7, -5], [10, 3, 1], "head"),
        ("leg_left", [4, 5, 0], [4, 0, -3], [3, 5, 6]),
        ("leg_right", [-4, 5, 0], [-7, 0, -3], [3, 5, 6]),
    ], [("leg_left", [22, 0, 0]), ("leg_right", [-22, 0, 0]), ("head", [-14, 0, 0])])
    add("facer", "iron_giant", [
        ("body", [0, 10, 0], [-5, 4, -3], [10, 14, 6]),
        ("head", [0, 12, -4], [-4, 8, -6], [8, 9, 4]),
        ("horns", [0, 15, 0], [-7, 12, -1], [14, 4, 2]),
        ("jaw", [0, 7, -3], [-3, 4, -5], [6, 5, 3], "head"),
    ], [("body", [8, 0, 12]), ("jaw", [12, 0, 0])])
    add("treant", "yeti", [
        ("body", [0, 11, 0], [-4, 5, -3], [8, 15, 6]),
        ("head", [0, 16, -1], [-3, 13, -5], [6, 7, 5]),
        ("crest", [0, 20, 0], [-6, 19, -3], [12, 4, 6]),
        ("arm_left", [4, 15, 0], [4, 6, -2], [4, 12, 4]),
        ("arm_right", [-4, 15, 0], [-8, 6, -2], [4, 12, 4]),
        ("leg_left", [2, 6, 0], [0, 0, -2], [4, 7, 5]), ("leg_right", [-2, 6, 0], [-4, 0, -2], [4, 7, 5]),
    ], [("leg_left", [18, 0, 0]), ("leg_right", [-18, 0, 0]), ("arm_right", [12, 0, 0])])
    # Related variants still differ in proportions, silhouette, and head shape.
    for name, base, scaling in [
        ("sleipnir", "wolf", (.87, 1.55, 1.12)), ("rodent", "wolf", (.75, .56, .70)),
        ("croc", "wolf", (1.14, .60, 1.44)), ("wyvern", "shinryu", (.73, .82, .73)),
        ("zombie", "yeti", (.69, .99, .74)),
        ("gargoyle", "kefka_god", (.67, .73, .74)), ("esper", "mateus", (1.03, 1.07, 1.01)),
    ]:
        model = deepcopy(ENTITIES_DATA[base])
        for b in model["bones"]:
            b["pivot"] = [round(v * ratio, 3) for v, ratio in zip(b["pivot"], scaling)]
            for c in b.get("cubes", []):
                for field in ("origin", "size"):
                    c[field] = [round(v * ratio, 3) for v, ratio in zip(c[field], scaling)]
        models[name] = model
    return models


def write_models(records):
    custom = new_family_models()
    groups = defaultdict(list)
    for row in records:
        if not row["original"]:
            groups[row["model"]].append(row)
    for family, variants in groups.items():
        name = FAMILIES[family][0]
        model = deepcopy(custom[name] if name in custom else ENTITIES_DATA[name])
        for b in model["bones"]:
            for c in b.get("cubes", []):
                snap_cube_to_pixels(c)
        try:
            uv = pack_model_cubes(model["bones"])
        except ValueError:
            for b in model["bones"]:
                b["pivot"] = [v * .82 for v in b["pivot"]]
                for c in b.get("cubes", []):
                    c["origin"] = [v * .82 for v in c["origin"]]
                    c["size"] = [v * .82 for v in c["size"]]
                    snap_cube_to_pixels(c)
            uv = pack_model_cubes(model["bones"])
        save_json(ASSETS / f"geckolib/models/entity/bestiary/{family}.geo.json", build_geometry_json(f"bestiary_{family}", model, uv))
        save_json(ASSETS / f"geckolib/animations/entity/bestiary/{family}.animation.json", build_animation_json(model))
        original = deepcopy(model)
        colors = original["colors"]
        for row in variants:
            digest = sha256(row["id"].encode()).digest()
            palette = deepcopy(original)
            for role, value in colors.items():
                channels = [int(value.lstrip("#")[i:i+2], 16) for i in (0, 2, 4)]
                bias = (digest[0] % 29 - 14, digest[1] % 27 - 13, digest[2] % 31 - 15)
                if row["kind"] != "Normal":
                    bias = tuple(v * 2 for v in bias)
                adjusted = [max(12, min(251, v + d)) for v, d in zip(channels, bias)]
                palette["colors"][role] = "#" + "".join(f"{v:02x}" for v in adjusted)
            pixels = generate_texture(row["id"], palette, uv)
            write_entity_texture(ASSETS / f"textures/entity/bestiary/{row['id']}.png", pixels, uv,
                                 palette["bones"], family)


def write_spawns(records):
    by_biome = defaultdict(list)
    for row in records:
        if row["original"] or row["mode"] == "contract":
            continue
        for biome in row["regions"]:
            by_biome[biome].append(row)
    for path in sorted((DATA / "worldgen/biome").glob("*.json")):
        raw = path.read_bytes()
        data = json.loads(raw.decode("utf-8-sig"))
        groups = data["attributes"]["minecraft:gameplay/natural_mob_spawns"]["argument"]["spawns_by_category"]
        added_ids = {"ivalice:" + row["id"] for row in records if not row["original"]}
        for category in ("creature", "monster", "water_creature"):
            groups[category] = [entry for entry in groups.get(category, []) if entry["type"] not in added_ids]
        for row in by_biome[path.stem]:
            category = "water_creature" if row["mode"] == "water" else "creature" if row["neutral"] else "monster"
            maximum = 1 if row["hp"] > 145 else 2
            groups[category].append({
                "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": maximum},
                "type": "ivalice:" + row["id"], "weight": 4 if maximum == 1 else 6,
            })
        serialized = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
        if b"\r\n" in raw:
            serialized = serialized.replace("\n", "\r\n")
        path.write_bytes(serialized.encode("utf-8"))


def verify_index(path, records):
    indexed = {key(r["name"]) for r in records}
    aliases = {
        "happy_bunny": lambda name: name in ("giza_rabbit", "ozmone_hare"),
        "zombie": lambda name: name == "zombie_female",
        "spector": lambda name: name == "specter",
        "chocobo": lambda name: name.endswith("_chocobo"),
        "crystalbug": lambda name: name.startswith("crystalbug_"),
        "holy_element": lambda name: name == "holy_elemental",
        "seeq": lambda name: name.startswith("seeq_"),
        "garif": lambda name: name.startswith("garif_"),
        "bangaa": lambda name: name.startswith("bangaa_"),
        "imperial": lambda name: name.startswith("imperial_"),
        "imperial_elite": lambda name: name.startswith("imperial_"),
        "imperial_armament": lambda name: name.endswith("_rook") or name == "air_cutter_remora",
        "judge_gabranth": lambda name: name == "gabranth",
    }
    names = {key(unescape(name)) for name in re.findall(
        r'<a href="/Games/Final-Fantasy-XII/Clan-Primer/Bestiary/[^"]+\.html">([^<]+)</a>',
        path.read_text(encoding="utf-8"))}
    if len(names) != 384:
        raise ValueError(f"Expected 384 bestiary index groups, found {len(names)}")
    missing = sorted(name for name in names if name not in indexed and not any(
        aliases.get(name, lambda _: False)(variant) for variant in indexed))
    if missing:
        raise ValueError(f"Missing bestiary index groups: {missing}")
    print(f"Validated all {len(names)} numbered bestiary groups")


def write_effect_icons(labels):
    for index, name in enumerate(labels):
        digest = sha256(name.encode()).digest()
        hue = (110 + digest[0] // 2, 90 + digest[1] // 2, 100 + digest[2] // 2)
        pixels = blank(18)
        for y in range(1, 17):
            for x in range(1, 17):
                radius = abs(x - 8.5) + abs(y - 8.5)
                if radius > 8:
                    continue
                highlight = (x + index) % 5 == 0 or (y - index) % 6 == 0
                shade = 1.35 if radius < 6 and highlight else .55 if radius > 6 else 1
                pixels[y][x] = tuple(min(255, int(channel * shade)) for channel in hue) + (255,)
        save_png(ASSETS / f"textures/mob_effect/{name}.png", pixels)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("bestiary", type=Path)
    parser.add_argument("locations", type=Path)
    parser.add_argument("hunts", type=Path)
    parser.add_argument("--numbered-index", type=Path)
    args = parser.parse_args()
    records = create_records(args.bestiary, args.locations, args.hunts)
    if args.numbered_index:
        verify_index(args.numbered_index, records)
    target = RESOURCES / "ivalice/tza_bestiary.json"
    target.write_text("[\n" + ",\n".join(json.dumps(row, ensure_ascii=False, separators=(",", ":")) for row in records)
                      + "\n]\n", encoding="utf-8", newline="\n")
    for language in ("pt_br", "en_us"):
        path = ASSETS / f"lang/{language}.json"
        names = json.loads(path.read_text(encoding="utf-8"))
        for row in records:
            if not row["original"]:
                names[f"entity.ivalice.{row['id']}"] = row["name"]
        effect_labels = {
            "oil": "Óleo", "silence": "Silêncio", "sleep": "Sono", "stop": "Parada",
            "disable": "Incapacitação", "immobilize": "Imobilização", "confuse": "Confusão",
            "disease": "Doença", "petrify": "Petrificação", "doom": "Condenação", "reverse": "Reversão",
            "reflect": "Reflexo", "faith": "Fé", "shell": "Barreira Mágica", "decoy": "Isca", "reraise": "Ressurgir",
        }
        for effect, label in effect_labels.items():
            names[f"effect.ivalice.{effect}"] = label if language == "pt_br" else effect.capitalize()
        save_json(path, names)
    write_effect_icons(effect_labels)
    write_models(records)
    write_spawns(records)
    print(f"{len(records)} records", Counter(r["mode"] for r in records),
          f"{sum(r['original'] for r in records)} reused entity IDs")


if __name__ == "__main__":
    main()
