"""Build the two portal-compatible Ivalice dimensions and their authored assets."""
import argparse
from copy import deepcopy
from pathlib import Path
import json
import math
import random
from zipfile import ZipFile

from PIL import Image, ImageEnhance, ImageOps

from generate_geckolib_entities import (
    ENTITIES_DATA, build_animation_json, build_geometry_json, generate_texture, pack_model_cubes, snap_cube_to_pixels,
)
from generate_ivalice_settlements import (
    ASSETS, DATA, RESOURCES, block_assets, compute_pbr_maps, save_json, save_png, write_site,
)
from generate_tza_bestiary import bone, style_model
from ivalice_texture_atlas import ART, inventory_icon, relic_icon, write_entity_texture

MINECRAFT = DATA.parent / "minecraft"
REGION_ART = (
    ("ruin_red_earth", "Terra da Catástrofe", "Catastrophe Earth"),
    ("ruin_ash", "Cinza Compactada", "Compacted Ash"),
    ("ruin_basalt", "Basalto da Ruína", "Ruin Basalt"),
    ("rusted_magitek", "Ferro Magitek Oxidado", "Rusted Magitek Iron"),
    ("magitek_plating", "Blindagem Magitek", "Magitek Plating"),
    ("judgment_glass", "Vidro do Julgamento", "Judgment Glass"),
    ("bleached_ash", "Cinza Branca", "Bleached Ash"),
    ("mobliz_flagstone", "Lajota de Mobliz", "Mobliz Flagstone"),
    ("ridorana_limestone", "Calcário de Ridorana", "Ridorana Limestone"),
    ("pharos_ashlar", "Cantaria do Pharos", "Pharos Ashlar"),
    ("giruvegan_stone", "Pedra de Giruvegan", "Giruvegan Stone"),
    ("pharos_runelight", "Luz Rúnica do Pharos", "Pharos Runelight"),
    ("subterra_tiles", "Ladrilho de Subterra", "Subterra Tile"),
    ("ancient_bronze", "Bronze Ancestral", "Ancient Bronze"),
    ("crystal_lattice", "Treliça do Grande Cristal", "Great Crystal Lattice"),
    ("subterra_bedrock", "Rocha de Subterra", "Subterra Stone"),
)
SEALS = (
    ("kefka_seal", "Altar do Julgamento", "Judgment Altar", 5),
    ("pharos_seal", "Pedestal da Noite", "Pedestal of Night", 12),
    ("crystal_seal", "Terminal do Grande Cristal", "Great Crystal Terminal", 14),
)
KEYS = (
    ("judgment_fragment", "Fragmento do Julgamento", "Judgment Fragment", "tza_dark_matter"),
    ("tza_black_orb", "Esfera Negra", "Black Orb", "tza_ruby_ring"),
    ("crystal_sigil", "Selo do Grande Cristal", "Great Crystal Sigil", "teleport_stone"),
)
NETHER = {
    "nether_wastes": ("Ermos da Catástrofe", "Catastrophe Wastes", "#593c32", "ruin_red_earth",
                     ("ruin_tyrannosaur", "ruin_mutant", "ruin_cactuar")),
    "soul_sand_valley": ("Vale da Cinza Branca", "Bleached Ash Valley", "#70625a", "bleached_ash",
                        ("ruin_ray", "ruin_mutant", "ruin_slagworm", "ruin_deathgaze")),
    "basalt_deltas": ("Fendas do Julgamento", "Judgment Fissures", "#554040", "ruin_basalt",
                     ("ruin_slagworm", "ruin_brachiosaur", "ruin_magitek")),
    "crimson_forest": ("Ermos de Mobliz", "Mobliz Badlands", "#633d31", "mobliz_flagstone",
                       ("ruin_tyrannosaur", "ruin_cactuar", "ruin_ray")),
    "warped_forest": ("Cemitério Magitek", "Magitek Graveyard", "#605b56", "rusted_magitek",
                      ("ruin_magitek", "ruin_mutant", "ruin_ray")),
}
END = {
    "the_end": ("Praça de Ridorana", "Ridorana Court", "#242e42", "ridorana_limestone",
                ("brainpan", "ridorana_sentinel", "pallicant")),
    "end_highlands": ("Grande Cristal", "Great Crystal", "#244c59", "giruvegan_stone",
                      ("crystal_warden", "forbidden", "luxollid")),
    "end_midlands": ("Portões de Giruvegan", "Gates of Giruvegan", "#30494c", "pharos_ashlar",
                     ("mythril_golem", "vivian", "crystal_warden")),
    "end_barrens": ("Subterra", "Subterra", "#262236", "subterra_tiles",
                    ("necrofiend", "deidar", "vagrant_soul")),
    "small_end_islands": ("Arquipélago de Ridorana", "Ridorana Archipelago", "#34405a", "ridorana_limestone",
                          ("pallicant", "ridorana_sentinel", "cassie")),
}


def read_vanilla(jar, path):
    return json.loads(jar.read(f"data/minecraft/{path}.json"))


def condition(test, rule):
    return {"type": "minecraft:condition", "if_true": test, "then_run": rule}


def state(block):
    return {"type": "minecraft:block", "result_state": block}


def sequence(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def noise_test(noise, low, high=1e8):
    return {"type": "minecraft:noise_threshold", "noise": noise, "min_threshold": low, "max_threshold": high}


def gradient(start, end, first, last):
    return {"type": "minecraft:gradient", "axis": "y", "from_coordinate": start, "from_value": first,
            "to_coordinate": end, "to_value": last}


def op(name, left, right):
    return {"type": f"minecraft:{name}", "left": left, "right": right}


def terrain(jar):
    nether = read_vanilla(jar, "worldgen/noise_settings/nether")
    for name, octave, amplitudes in (("ruin_ridges", -8, [1.0, .7, .45, .22]),
                                     ("ruin_rifts", -6, [1.0, .75, .35])):
        save_json(DATA / f"worldgen/noise/{name}.json", {
            "base_octave": octave, "octave_count": len(amplitudes), "amplitude_modifiers": amplitudes, "base_amplitude": 1.0,
        })
    broad = {"type": "minecraft:noise", "noise": "ivalice:ruin_ridges", "xz_scale": 1, "y_scale": 0}
    rift = {"type": "minecraft:noise", "noise": "ivalice:ruin_rifts", "xz_scale": 1.35, "y_scale": 0}
    broken = op("add", op("mul", {"type": "minecraft:abs", "input": rift}, 3.8), -.79)
    bed = gradient(2, 18, .8, -.35)
    ground = op("add", gradient(0, 128, 1.17, -1.23), op("mul", broad, .95))
    fissures = op("max", bed, op("add", ground, op("min", 0.0, broken)))
    shape = op("add", {"type": "minecraft:interpolated", "cell_size_xz": 4, "cell_size_y": 4,
                       "input": fissures}, {"type": "minecraft:beardifier"})
    nether["noise_router"]["final_density"] = shape
    nether["debug_functions"] = [{"label": "Ruins", "function": shape}]
    nether["default_block"] = "ivalice:ruin_basalt"
    nether["sea_level"] = 26
    nether["material_rule"] = "ivalice:world_of_ruin"
    save_json(MINECRAFT / "worldgen/noise_settings/nether.json", nether)

    nether_type = read_vanilla(jar, "dimension_type/the_nether")
    nether_type.update(has_ceiling=False, has_skylight=True, skybox="overworld", ambient_light=.28)
    nether_type["attributes"].update({
        "minecraft:visual/fog_color": "#60453e",
        "minecraft:visual/fog_start_distance": 20.0,
        "minecraft:visual/fog_end_distance": 142.0,
        "minecraft:visual/sky_color": "#a96547",
        "minecraft:visual/sun_angle": 72.0,
        "minecraft:visual/sky_light_color": "#e9ad8d",
        "minecraft:visual/sky_light_factor": .42,
        "minecraft:gameplay/sky_light_level": 8.0,
    })
    save_json(MINECRAFT / "dimension_type/the_nether.json", nether_type)

    end = read_vanilla(jar, "worldgen/noise_settings/end")
    end["material_rule"] = "ivalice:ancestral_ruins"
    save_json(MINECRAFT / "worldgen/noise_settings/end.json", end)
    end_type = read_vanilla(jar, "dimension_type/the_end")
    end_type["attributes"].update({
        "minecraft:visual/fog_color": "#283849",
        "minecraft:visual/fog_start_distance": 23.0,
        "minecraft:visual/fog_end_distance": 190.0,
        "minecraft:visual/sky_light_color": "#a3d5e7",
        "minecraft:visual/sky_light_factor": .28,
    })
    save_json(MINECRAFT / "dimension_type/the_end.json", end_type)

    def biome_surface(palette):
        return [condition({"type": "minecraft:biome", "biome_is": "minecraft:" + biome},
                          state("ivalice:" + block)) for biome, (_, _, _, block, _) in palette.items()]

    near_lava = condition({"type": "minecraft:not", "invert": {"type": "minecraft:y_above",
                           "add_stone_depth": False, "anchor": {"absolute": 28}, "surface_depth_multiplier": 0}},
                          state("ivalice:judgment_glass"))
    save_json(DATA / "worldgen/material_rule/world_of_ruin.json", sequence(
        "minecraft:bedrock_floor",
        condition("minecraft:on_floor", sequence(near_lava,
                  condition(noise_test("minecraft:nether_state_selector", .17), state("ivalice:ruin_ash")),
                  *biome_surface(NETHER))),
        condition("minecraft:under_floor", state("ivalice:ruin_red_earth")),
        state("ivalice:ruin_basalt"),
    ))
    # Leave exposed End stone in every island zone for vanilla gateway landing and chorus growth.
    save_json(DATA / "worldgen/material_rule/ancestral_ruins.json", sequence(
        condition("minecraft:on_floor", sequence(
            condition(noise_test("minecraft:nether_state_selector", -.15, .15), state("minecraft:end_stone")),
            *biome_surface(END),
        )),
        condition("minecraft:under_floor", sequence(
            condition({"type": "minecraft:biome", "biome_is": "minecraft:end_barrens"}, state("ivalice:subterra_bedrock")),
            state("ivalice:ridorana_limestone"),
        )),
        state("minecraft:end_stone"),
    ))
    save_json(MINECRAFT / "tags/block/base_stone_nether.json", {"values": ["ivalice:ruin_basalt"]})
    for tag in ("nether_fortress", "bastion_remnant", "nether_fossil", "end_city"):
        save_json(MINECRAFT / f"tags/worldgen/biome/has_structure/{tag}.json", {"replace": True, "values": []})


def spawns(ids):
    return {"argument": {"spawn_costs": {}, "spawns_by_category": {"monster": [
        {"type": "ivalice:" + species, "weight": 1 if species == "ruin_deathgaze" else 16 - 4 * i,
         "count": 1 if i else {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2}}
        for i, species in enumerate(ids)
    ]}}, "modifier": "overlay"}


def column(name, layers, surfaces, frequency):
    def provider(block):
        return {"id": "ivalice:" + block}

    save_json(DATA / f"worldgen/feature/{name}.json", {"type": "minecraft:block_column",
        "direction": "up", "prioritize_tip": True,
        "allowed_placement": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air"]},
        "layers": [{"height": low if low == high else {"type": "minecraft:uniform",
                    "min_inclusive": low, "max_inclusive": high}, "provider": provider(block)}
                   for low, high, block in layers],
    })
    save_json(DATA / f"worldgen/placed_feature/{name}.json", {"feature": "ivalice:" + name, "placement": [
        {"type": "minecraft:rarity_filter", "chance": frequency},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_blocks",
         "offset": [0, -1, 0], "blocks": surfaces}},
        {"type": "minecraft:biome"},
    ]})


def biomes(jar):
    ruin_ground = ["ivalice:" + biome[3] for biome in NETHER.values()] + ["ivalice:ruin_ash"]
    end_ground = ["ivalice:" + biome[3] for biome in END.values()] + ["minecraft:end_stone"]
    column("fractured_steel", [(1, 3, "rusted_magitek"), (1, 2, "magitek_plating")], ruin_ground, 2)
    column("fissure_spike", [(1, 4, "ruin_basalt"), (1, 1, "judgment_glass")], ruin_ground, 3)
    column("crystal_spire", [(1, 3, "giruvegan_stone"), (2, 6, "crystal_lattice")], end_ground, 2)
    column("pharos_stela", [(1, 3, "pharos_ashlar"), (1, 1, "pharos_runelight")], end_ground, 3)

    for is_end, palette in ((False, NETHER), (True, END)):
        for id_, (_, _, fog, _, species) in palette.items():
            biome = read_vanilla(jar, f"worldgen/biome/{id_}")
            biome["attributes"]["minecraft:gameplay/natural_mob_spawns"] = spawns(species)
            biome["attributes"]["minecraft:visual/fog_color"] = fog
            biome["attributes"]["minecraft:visual/ambient_particles"] = {
                "argument": [{"particle": {"type": "minecraft:end_rod" if is_end else "minecraft:white_ash"},
                              "probability": .007 if is_end else .047}],
                "modifier": "append",
            }
            if is_end:
                features = biome["features"]
                while len(features) <= 9:
                    features.append([])
                if id_ == "the_end":
                    features[9].append("ivalice:pharos_stela")
                elif id_ in ("end_midlands", "end_barrens"):
                    features[9].append("ivalice:pharos_stela")
                else:
                    features[9].append("ivalice:crystal_spire")
            else:
                features = [[] for _ in range(10)]
                features[7] = ["minecraft:ore_gold_nether", "minecraft:ore_quartz_nether",
                               "minecraft:ore_ancient_debris_large", "minecraft:ore_debris_small"]
                if id_ in ("nether_wastes", "warped_forest", "crimson_forest"):
                    features[9] = ["ivalice:fractured_steel"]
                else:
                    features[9] = ["ivalice:fissure_spike"]
                biome["features"] = features
                biome["carvers"] = []
            save_json(MINECRAFT / f"worldgen/biome/{id_}.json", biome)


def recipes():
    for id_, ingredients, result in (
        ("ruin_blaze_rod", ["ivalice:fire_stone", "minecraft:gold_ingot"], "minecraft:blaze_rod"),
        ("ruin_ender_pearl", ["ivalice:dark_magicite", "minecraft:amethyst_shard"], "minecraft:ender_pearl"),
    ):
        save_json(DATA / f"recipe/{id_}.json", {"type": "minecraft:crafting_shapeless",
                  "ingredients": ingredients, "result": {"id": result}})


class Site:
    def __init__(self, width, depth, seed):
        self.width = width
        self.depth = depth
        self.random = random.Random(seed)
        self.blocks = {}
        self.chests = []

    def put(self, x, y, z, block, **properties):
        self.blocks[x, y, z] = (block if ":" in block else "ivalice:" + block, tuple(sorted(properties.items())))

    def box(self, x0, y0, z0, x1, y1, z1, block):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                for z in range(z0, z1 + 1):
                    self.put(x, y, z, block)

    def perimeter(self, x0, z0, x1, z1, y, block, missing=0):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    if self.random.random() >= missing:
                        self.put(x, y, z, block)

    def foundation(self, stone, paving):
        self.box(0, -2, 0, self.width - 1, -1, self.depth - 1, stone)
        self.box(0, 0, 0, self.width - 1, 0, self.depth - 1, paving)
        for x in range(2, self.width - 1, 6):
            for z in range(2, self.depth - 1, 6):
                depth = self.random.randint(3, 7)
                self.box(x - 1, -depth, z - 1, x + 1, -3, z + 1, stone)
        # Keep the approaches clear when a template intersects a hill.
        self.box(0, 1, 0, self.width - 1, 3, self.depth - 1, "minecraft:air")

    def chest(self, x, y, z, loot):
        self.put(x, y, z, "minecraft:chest", facing="south", type="single", waterlogged="false")
        self.chests.append(((x, y, z), "ivalice:chests/" + loot))

    def spiral(self, x, z, y, turns, stairs, support):
        # One flight occupies each wall of the 7x7 shaft.
        route = ([(i, 0, "east") for i in range(6)] + [(6, i, "south") for i in range(6)]
                 + [(i, 6, "west") for i in range(6, 0, -1)] + [(0, i, "north") for i in range(6, 0, -1)])
        for i in range(turns):
            dx, dz, facing = route[i % len(route)]
            self.put(x + dx, y + i - 1, z + dz, support)
            self.put(x + dx, y + i, z + dz, stairs, facing=facing, half="bottom", shape="straight", waterlogged="false")
            self.box(x + dx, y + i + 1, z + dz, x + dx, y + i + 2, z + dz, "minecraft:air")

    def save(self, name, regions, spacing, salt):
        base = min(pos[1] for pos in self.blocks)
        top = max(pos[1] for pos in self.blocks)
        write_site(name, None, ["minecraft:" + region for region in regions], self.blocks, [],
                   (self.width, top - base + 1, self.depth), base, base, salt, spacing,
                   "beard_box", self.chests)
        # Keep the lowest foundation safely above the lava sea or the End void.
        path = DATA / f"worldgen/structure/{name}.json"
        structure = json.loads(path.read_text(encoding="utf-8"))
        structure["dimension_padding"] = {"bottom": 30 if any(region in NETHER for region in regions) else 34}
        save_json(path, structure)


def kefka_tower():
    s = Site(39, 39, 196)
    s.foundation("ruin_basalt", "rusted_magitek")
    for x in (5, 19, 33):
        for z in (6, 32):
            s.box(x - 2, 1, z - 2, x + 2, 7, z + 2, "magitek_plating")
            s.box(x - 1, 8, z - 1, x + 1, s.random.randint(10, 17), z + 1, "ruin_basalt")
            s.box(x - 1, 1, z - 2, x + 1, 4, z - 2, "judgment_glass")
    for y in range(1, 23):
        inset = 1 if y < 15 else 2
        s.perimeter(10 + inset, 9 + inset, 28 - inset, 28 - inset, y,
                    "ruin_basalt" if y % 5 else "magitek_plating", .12 if y > 8 else .03)
        for x in (13, 25):
            s.put(x, y, 9 + inset, "judgment_glass")
    s.box(16, 1, 8, 22, 5, 10, "minecraft:air")
    s.box(12, 1, 11, 26, 20, 26, "minecraft:air")
    for y in (6, 12, 18):
        s.box(12, y, 12, 26, y, 26, "magitek_plating")
        s.box(14, y, 14, 22, y, 22, "minecraft:air")
        s.box(12, y + 1, 13, 13, y + 1, 25, "judgment_glass")
    s.box(11, 24, 10, 27, 24, 27, "magitek_plating")
    s.box(14, 24, 14, 20, 24, 20, "minecraft:air")
    s.box(12, 25, 11, 26, 25, 26, "ruin_basalt")
    s.spiral(14, 14, 1, 24, "minecraft:polished_blackstone_brick_stairs", "rusted_magitek")
    s.perimeter(12, 11, 26, 26, 26, "magitek_plating")
    s.perimeter(12, 11, 26, 26, 27, "ruin_basalt", .43)
    s.put(23, 26, 19, "kefka_seal")
    for x, z in ((22, 16), (24, 16), (22, 22), (24, 22)):
        s.box(x, 26, z, x, 29, z, "judgment_glass")
    for x in (11, 27):
        s.box(x, 24, 10, x, 31, 10, "ruin_basalt")
        s.box(x, 24, 27, x, 31, 27, "ruin_basalt")
    s.chest(14, 1, 24, "kefka_tower")
    s.chest(24, 13, 23, "kefka_tower")
    s.save("kefka_tower", ("basalt_deltas", "nether_wastes", "warped_forest"), 24, 6182719)


def magitek_boneyard():
    s = Site(31, 29, 348)
    s.foundation("ruin_basalt", "ruin_ash")
    s.box(13, 0, 0, 17, 0, 28, "mobliz_flagstone")
    for x, z in ((5, 4), (22, 5), (5, 17), (22, 17)):
        s.box(x - 2, 0, z - 2, x + 4, 0, z + 4, "rusted_magitek")
        for y in range(1, 5):
            s.perimeter(x - 2, z - 2, x + 4, z + 4, y, "magitek_plating", .28)
        for dx, dz in ((0, 1), (4, 2), (1, 4)):
            s.box(x + dx, 1, z + dz, x + dx, s.random.randrange(4, 9), z + dz, "rusted_magitek")
        s.put(x + 1, 1, z + 1, "judgment_glass")
    s.box(4, 5, 13, 26, 5, 14, "magitek_plating")
    for x in range(5, 26, 3):
        s.put(x, 6, 13, "rusted_magitek")
    s.chest(6, 1, 7, "magitek_salvage")
    s.chest(23, 1, 19, "magitek_salvage")
    s.save("magitek_boneyard", ("warped_forest", "soul_sand_valley", "nether_wastes"), 16, 6679142)


def mobliz_ruins():
    s = Site(31, 27, 52)
    s.foundation("ruin_red_earth", "mobliz_flagstone")
    s.box(12, 0, 0, 17, 0, 26, "ruin_ash")
    for x, z in ((2, 3), (21, 3), (2, 16), (21, 16)):
        for y in range(1, 5):
            s.perimeter(x, z, x + 7, z + 7, y, "mobliz_flagstone", .14 + y * .06)
        s.box(x + 2, 1, z, x + 4, 3, z, "minecraft:air")
        s.box(x + 1, 4, z + 5, x + 6, 4, z + 7, "rusted_magitek")
    # The surviving shelter sits behind a collapsed street.
    s.box(9, -3, 10, 21, -3, 21, "ruin_basalt")
    s.box(10, -2, 11, 20, 0, 20, "minecraft:air")
    s.box(11, 1, 11, 20, 1, 20, "mobliz_flagstone")
    for i in range(3):
        s.put(15, -i, 10 + i, "minecraft:stone_brick_stairs", facing="north", half="bottom",
              shape="straight", waterlogged="false")
    s.put(18, -1, 17, "minecraft:lantern", hanging="true", waterlogged="false")
    s.chest(19, -2, 19, "mobliz_cache")
    s.save("mobliz_ruins", ("crimson_forest", "nether_wastes", "soul_sand_valley"), 18, 762014)


def pharos():
    s = Site(35, 37, 196)
    s.foundation("ridorana_limestone", "pharos_ashlar")
    for x in (3, 31):
        for z in range(3, 36, 6):
            s.box(x - 1, 1, z - 1, x + 1, 4, z + 1, "ridorana_limestone")
            s.box(x, 5, z, x, 6, z, "pharos_runelight")
    s.box(14, 0, 0, 20, 0, 28, "giruvegan_stone")
    for y in range(1, 28):
        inset = y // 9
        s.perimeter(8 + inset, 8 + inset, 26 - inset, 29 - inset, y,
                    "ridorana_limestone" if y % 5 else "ancient_bronze", .10 if y > 7 else 0)
        for x in (12, 22):
            s.put(x, y, 8 + inset, "pharos_runelight" if y % 3 == 0 else "pharos_ashlar")
    s.box(14, 1, 8, 20, 5, 8, "minecraft:air")
    s.box(10, 1, 10, 24, 26, 27, "minecraft:air")
    for y in (7, 14, 21, 25):
        s.box(11, y, 11, 23, y, 26, "pharos_ashlar")
        s.box(13, y, 13, 21, y, 21, "minecraft:air")
    s.spiral(13, 13, 1, 24, "minecraft:stone_brick_stairs", "pharos_ashlar")
    s.box(11, 24, 12, 14, 24, 13, "pharos_ashlar")
    s.box(11, 25, 12, 12, 25, 24, "pharos_ashlar")
    s.box(12, 28, 12, 22, 28, 24, "pharos_ashlar")
    for x, z in ((12, 12), (22, 12), (12, 24), (22, 24)):
        s.box(x, 29, z, x, 32, z, "pharos_runelight")
    s.box(15, 29, 16, 19, 30, 20, "ancient_bronze")
    s.box(16, 31, 17, 18, 33, 19, "pharos_runelight")
    s.chest(22, 1, 24, "ridorana_reliquary")
    s.chest(22, 25, 24, "ridorana_reliquary")
    s.save("ridorana_pharos", ("end_midlands", "end_highlands"), 23, 3884254)


def subterra():
    s = Site(29, 27, 12)
    s.foundation("subterra_bedrock", "subterra_tiles")
    s.box(5, -6, 6, 23, -6, 23, "subterra_bedrock")
    s.box(6, -5, 7, 22, -1, 22, "minecraft:air")
    for y in range(-5, 1):
        s.perimeter(5, 6, 23, 23, y, "subterra_tiles")
    s.box(12, 0, 4, 16, 1, 10, "minecraft:air")
    for i in range(6):
        for x in range(12, 17):
            s.put(x, -i, 5 + i, "minecraft:polished_blackstone_brick_stairs", facing="north",
                  half="bottom", shape="straight", waterlogged="false")
    for x in (7, 21):
        for z in (10, 16, 21):
            s.box(x, -5, z, x, -2, z, "ancient_bronze")
            s.put(x, -3, z, "pharos_runelight")
    s.put(14, -5, 18, "pharos_seal")
    s.chest(19, -5, 21, "subterra_cache")
    for x in (9, 19):
        s.box(x - 1, 1, 3, x + 1, 3, 3, "subterra_tiles")
        s.put(x, 4, 3, "pharos_runelight")
    s.save("subterra_vault", ("end_barrens", "end_midlands"), 17, 447009)


def giruvegan():
    s = Site(33, 31, 1400)
    s.foundation("ridorana_limestone", "giruvegan_stone")
    for y in range(1, 7):
        s.perimeter(2, 2, 30, 28, y, "pharos_ashlar", .17 if y > 2 else 0)
    s.box(12, 1, 2, 20, 5, 2, "minecraft:air")
    s.box(14, 0, 0, 18, 0, 30, "ancient_bronze")
    for x, z in ((7, 10), (25, 10), (7, 23), (25, 23)):
        s.box(x - 2, 1, z - 2, x + 2, 8, z + 2, "pharos_ashlar")
        s.box(x, 1, z - 2, x, 8, z - 2, "pharos_runelight")
        s.box(x - 1, 9, z - 1, x + 1, 9, z + 1, "ancient_bronze")
    s.box(6, 9, 10, 26, 10, 10, "pharos_ashlar")
    s.box(8, 7, 10, 11, 8, 10, "pharos_ashlar")
    s.box(21, 7, 10, 24, 8, 10, "pharos_ashlar")
    for x in range(13, 20):
        s.put(x, 9, 10, "pharos_runelight")
    s.chest(23, 1, 25, "giruvegan_cache")
    s.save("giruvegan_gate", ("end_midlands", "end_highlands"), 17, 6492307)


def observatory():
    s = Site(33, 33, 157)
    s.foundation("ridorana_limestone", "giruvegan_stone")
    for x in range(1, 32):
        for z in range(1, 32):
            r = math.hypot(x - 16, z - 16)
            if 8.5 <= r <= 10.5 or 13.4 <= r <= 14.6:
                s.put(x, 0, z, "crystal_lattice")
            elif r < 6:
                s.put(x, 0, z, "subterra_tiles")
    for i in range(8):
        theta = i * math.tau / 8
        x, z = 16 + round(12 * math.cos(theta)), 16 + round(12 * math.sin(theta))
        s.box(x - 1, 1, z - 1, x + 1, 6, z + 1, "ancient_bronze")
        s.box(x, 7, z, x, s.random.randint(8, 13), z, "crystal_lattice")
    s.put(16, 1, 16, "crystal_seal")
    s.chest(16, 1, 24, "crystal_observatory")
    s.save("great_crystal_observatory", ("end_highlands", "end_midlands"), 23, 275313)

def loot_tables():
    tables = {
        "magitek_salvage": (("ivalice:judgment_fragment", 1, 1), ("minecraft:blaze_rod", 2, 5),
            ("minecraft:netherite_upgrade_smithing_template", 1, 1), ("ivalice:gemsteel", 1, 2),
            ("minecraft:gold_ingot", 2, 6), ("minecraft:nether_wart", 1, 3), ("minecraft:soul_sand", 3, 7)),
        "mobliz_cache": (("ivalice:judgment_fragment", 1, 1), ("ivalice:hi_potion", 2, 6),
            ("minecraft:blaze_rod", 1, 2), ("ivalice:ether", 1, 3), ("ivalice:fire_stone", 1, 4)),
        "kefka_tower": (("ivalice:judgment_fragment", 1, 1), ("ivalice:empyreal_soul", 1, 2),
            ("ivalice:gemsteel", 1, 2), ("ivalice:elixir", 1, 1), ("minecraft:ancient_debris", 1, 2)),
        "ridorana_reliquary": (("ivalice:tza_black_orb", 1, 3), ("ivalice:water_stone", 2, 5),
            ("ivalice:high_arcana", 1, 2), ("minecraft:ender_pearl", 1, 4), ("ivalice:ether", 1, 3)),
        "subterra_cache": (("ivalice:tza_black_orb", 1, 1), ("ivalice:dark_magicite", 2, 5),
            ("ivalice:serpentarius", 1, 1), ("minecraft:shulker_shell", 1, 3), ("ivalice:x_potion", 1, 2)),
        "giruvegan_cache": (("ivalice:crystal_sigil", 1, 1), ("ivalice:high_arcana", 1, 2),
            ("minecraft:ender_pearl", 2, 6), ("ivalice:teleport_stone", 1, 4), ("ivalice:ether", 1, 2)),
        "crystal_observatory": (("minecraft:elytra", 1, 1), ("ivalice:serpentarius", 1, 1),
            ("minecraft:shulker_shell", 2, 4), ("ivalice:excalibur", 1, 1), ("ivalice:tza_megalixir", 1, 1)),
    }
    for name, ((guaranteed, low, high), *contents) in tables.items():
        def entry(item, low, high):
            return {"type": "minecraft:item", "name": item, "functions": [{"function": "minecraft:set_count",
                "count": low if low == high else {"type": "minecraft:uniform", "min": low, "max": high}}]}
        save_json(DATA / f"loot_table/chests/{name}.json", {"type": "minecraft:chest", "pools": [
            {"rolls": 1, "entries": [entry(guaranteed, low, high)]},
            {"rolls": 3, "entries": [entry(item, low, high) for item, low, high in contents]},
        ]})


def models():
    def authored(base, shapes, gait, material, hues):
        result = style_model(base, shapes, gait)
        result["colors"].update(hues)
        return result, material

    dinosaur = [
        ("body", [0, 11, 0], [-4, 7, -4], [8, 9, 11]),
        ("neck", [0, 11, -3], [-3, 10, -7], [6, 7, 5], "body"),
        ("head", [0, 15, -6], [-3, 14, -11], [6, 5, 7], "neck"),
        ("jaw", [0, 14, -5], [-2, 12, -11], [4, 2, 6], "head"),
        ("tail", [0, 10, 7], [-2, 8, 6], [4, 4, 10], "body"),
        ("tail_tip", [0, 10, 15], [-1, 9, 15], [2, 2, 6], "tail"),
        ("leg_left", [2, 8, 2], [2, 0, -1], [4, 8, 5]),
        ("leg_right", [-2, 8, 2], [-6, 0, -1], [4, 8, 5]),
        ("arm_left", [3, 11, -4], [3, 8, -6], [2, 4, 3], "body"),
        ("arm_right", [-3, 11, -4], [-5, 8, -6], [2, 4, 3], "body"),
    ]
    stride = [("leg_left", [25, 0, 0]), ("leg_right", [-25, 0, 0]), ("tail", [0, 21, 0])]
    result = {
        "ruin_tyrant": authored("wild_saurian", dinosaur, stride, "saurian",
            {"base": "#977350", "shadow": "#473727", "highlight": "#c0a46b", "eye": "#f0c34a", "belly": "#dac0a1"}),
        "ruin_sauropod": authored("wild_saurian", [
            ("body", [0, 10, 0], [-5, 7, -4], [10, 8, 11]),
            ("neck", [0, 9, -3], [-2, 10, -7], [4, 15, 4], "body"),
            ("head", [0, 24, -4], [-3, 22, -10], [6, 5, 6], "neck"),
            ("tail", [0, 11, 6], [-2, 8, 6], [4, 4, 10], "body"),
            ("front_left", [3, 9, -2], [3, 0, -4], [3, 9, 4]),
            ("front_right", [-3, 9, -2], [-6, 0, -4], [3, 9, 4]),
            ("hind_left", [3, 8, 4], [3, 0, 2], [3, 8, 4]),
            ("hind_right", [-3, 8, 4], [-6, 0, 2], [3, 8, 4]),
        ], [("front_left", [18, 0, 0]), ("front_right", [-18, 0, 0]), ("hind_left", [-18, 0, 0]),
            ("hind_right", [18, 0, 0]), ("tail", [0, 19, 0]), ("neck", [8, 0, 0])], "saurian",
            {"base": "#556d50", "shadow": "#273c37", "highlight": "#a1ae70", "eye": "#dc8240"}),
        "ruin_cactuar": authored("cactoid", [
            ("body", [0, 8, 0], [-2, 5, -2], [4, 9, 4]),
            ("head", [0, 12, 0], [-2, 11, -2], [4, 6, 4], "body"),
            ("crest", [0, 17, 0], [0, 16, 0], [1, 3, 1], "head"),
            ("arm_left", [2, 11, 0], [2, 7, -1], [5, 2, 2]),
            ("arm_right", [-2, 10, 0], [-7, 10, -1], [5, 2, 2]),
            ("leg_left", [1, 6, 0], [1, 0, -1], [2, 7, 3]),
            ("leg_right", [-1, 6, 0], [-3, 0, -1], [2, 7, 3]),
        ], [("leg_left", [44, 0, 0]), ("leg_right", [-44, 0, 0]),
            ("arm_left", [0, 0, 25]), ("arm_right", [0, 0, 25])], "cactoid",
            {"base": "#568d39", "shadow": "#285530", "highlight": "#a4b75b", "crest": "#be7238"}),
        "ruin_ray": authored("ichthon", [
            ("body", [0, 7, 0], [-3, 6, -5], [6, 3, 9]),
            ("head", [0, 7, -4], [-3, 6, -7], [6, 3, 3], "body"),
            ("wing_left", [3, 7, 0], [3, 6, -3], [8, 1, 6], "body"),
            ("wing_right", [-3, 7, 0], [-11, 6, -3], [8, 1, 6], "body"),
            ("wing_tip_left", [10, 7, 0], [10, 6, -1], [4, 1, 4], "wing_left"),
            ("wing_tip_right", [-10, 7, 0], [-14, 6, -1], [4, 1, 4], "wing_right"),
            ("tail", [0, 7, 4], [-1, 6, 4], [2, 1, 9], "body"),
        ], [("wing_left", [0, 0, 29]), ("wing_right", [0, 0, -29]), ("tail", [0, 14, 0])], "fish",
            {"base": "#826963", "shadow": "#402e40", "highlight": "#bc906e", "wings": "#906449", "eye": "#eeaf71"}),
        "ruin_slagworm": authored("wild_saurian", [
            ("body", [0, 4, 4], [-4, 0, -2], [8, 8, 11]),
            ("neck", [0, 4, -2], [-3, 2, -7], [6, 9, 6]),
            ("head", [0, 9, -5], [-4, 7, -11], [8, 6, 7], "neck"),
            ("jaw", [0, 7, -4], [-4, 5, -11], [8, 2, 7], "head"),
            ("fangs", [0, 8, -6], [-3, 6, -12], [6, 2, 1], "head"),
            ("tail", [0, 4, 8], [-3, 1, 8], [6, 5, 9], "body"),
            ("tail_tip", [0, 4, 16], [-2, 1, 16], [4, 3, 5], "tail"),
        ], [("neck", [12, 0, 0]), ("tail", [0, 35, 0]), ("tail_tip", [0, -26, 0])], "snake",
            {"base": "#b48a43", "shadow": "#4b311e", "highlight": "#c3a064", "eye": "#e18b30"}),
        "ruin_deathgaze": authored("kefka_god", [
            ("body", [0, 13, 0], [-4, 7, -3], [8, 11, 6]),
            ("head", [0, 16, -2], [-3, 15, -7], [6, 6, 6], "body"),
            ("horn_left", [2, 20, -2], [2, 19, -3], [2, 5, 2], "head"),
            ("horn_right", [-2, 20, -2], [-4, 19, -3], [2, 5, 2], "head"),
            ("wing_left", [4, 16, 1], [4, 12, 0], [9, 9, 1], "body"),
            ("wing_right", [-4, 16, 1], [-13, 12, 0], [9, 9, 1], "body"),
            ("wing_tip_left", [12, 19, 1], [12, 15, 0], [4, 8, 1], "wing_left"),
            ("wing_tip_right", [-12, 19, 1], [-16, 15, 0], [4, 8, 1], "wing_right"),
            ("tail", [0, 8, 2], [-2, 2, 1], [4, 7, 4], "body"),
        ], [("wing_left", [0, 39, 17]), ("wing_right", [0, -39, -17]), ("tail", [0, 17, 0])], "ghost",
            {"base": "#483e62", "shadow": "#1d1929", "highlight": "#8d819f", "wings": "#594768",
             "horns": "#c6b4b9", "eye": "#ff6851"}),
    }

    for name, base, material, hues in (
        ("ruin_mutant", "yeti", "zombie", {"base": "#7f8170", "shadow": "#3c373d", "highlight": "#a68a76", "eye": "#e3aa41"}),
        ("ruin_magitek", "iron_giant", "mech", {"base": "#73584b", "shadow": "#2d3235", "highlight": "#ada68f",
                                              "armor": "#565e59", "eye": "#f46643"}),
        ("ridorana_sentinel", "judge_gabranth", "golem", {"base": "#9b9f95", "armor": "#77898c", "shadow": "#404b4e",
                                                       "highlight": "#c3c9b0", "eye": "#89e6ed"}),
        ("crystal_warden", "mateus", "ghost", {"base": "#4b858a", "shadow": "#273e64", "highlight": "#97c8c1",
                                             "cape": "#24626a", "halo": "#5bbfdd", "eye": "#b9fbff"}),
    ):
        rig = deepcopy(ENTITIES_DATA[base])
        rig["colors"].update(hues)
        result[name] = rig, material
    return result


MOBS = (
    ("ruin_tyrannosaur", "Tyrannosaur", "ruin_tyrant", "Ermos da Catástrofe", 42, 220, 15, .25, 1.48, 1.85, "CLEAVE", "ice", "tza_tyrant_hide"),
    ("ruin_brachiosaur", "Brachiosaur", "ruin_sauropod", "Fendas do Julgamento", 62, 390, 23, .20, 1.72, 2.6, "SCATHE", "ice", "gemsteel"),
    ("ruin_cactuar", "Cactuar", "ruin_cactuar", "Ermos de Mobliz", 28, 36, 8, .40, .52, 1.1, "WIND", "water", "cactus_bloom"),
    ("ruin_ray", "Land Ray", "ruin_ray", "Vale da Cinza Branca", 34, 110, 10, .27, 1.15, .84, "THUNDER", "water", "wind_stone"),
    ("ruin_slagworm", "Slagworm", "ruin_slagworm", "Fendas do Julgamento", 49, 255, 17, .22, 1.5, 1.47, "DARKRA", "ice", "earth_stone"),
    ("ruin_mutant", "Mutant", "ruin_mutant", "Ermos da Catástrofe", 34, 125, 12, .24, .82, 1.65, "DARK", "holy", "dark_magicite"),
    ("ruin_magitek", "Remanescente Magitek", "ruin_magitek", "Cemitério Magitek", 47, 245, 17, .22, 1.23, 2.25, "THUNDARA", "water", "gemsteel"),
    ("ruin_deathgaze", "Deathgaze", "ruin_deathgaze", "Vale da Cinza Branca", 73, 465, 23, .26, 1.83, 2.55, "DARKGA", "holy", "empyreal_soul"),
    ("ridorana_sentinel", "Sentinela de Ridorana", "ridorana_sentinel", "Praça de Ridorana", 35, 142, 11, .24, .94, 1.72, "CLEAVE", "thunder", "high_arcana"),
    ("crystal_warden", "Guardião do Grande Cristal", "crystal_warden", "Grande Cristal", 53, 240, 16, .23, 1.04, 1.92, "BLIZZARA", "fire", "tza_ice_crystal"),
)


def ruin_mobs():
    rigs = models()
    ids = {line.split("\t", 2)[1] for line in (RESOURCES / "ivalice/tza_catalog.tsv").read_text(encoding="utf-8").splitlines()}
    rows = []
    for id_, name, rig, area, level, hp, attack, speed, width, height, spell, weak, material in MOBS:
        category = "Boss" if id_ == "ruin_deathgaze" else "Normal"
        family = "Construct" if id_ in ("ruin_magitek", "ridorana_sentinel", "crystal_warden") else "Beast"
        region = ("Mundo em Ruínas" if id_.startswith("ruin_") else "Ruínas Ancestrais") + " • " + area
        primary = material if material in ids else "earth_stone"
        rare = "judgment_fragment" if id_.startswith("ruin_") else "crystal_sigil" if id_ == "crystal_warden" else "tza_black_orb"
        def roll(chance, item):
            return {"chance": chance, "items": [item]}
        rows.append({"id": id_, "name": name, "family": family, "kind": category, "model": rig, "original": False,
            "habitat": region, "regions": [area], "mode": "dimension", "aquatic": False, "neutral": False, "width": width,
            "height": height, "level": level, "hp": hp, "attack": attack, "speed": speed, "gil": level * 14,
            "drops": [[roll(.62 + .04*i, primary), roll(.045 + .015*i, rare)] for i in range(4)],
            "vanillaLoot": False, "steal": [[roll(.55, primary), roll(.08, rare)], [roll(.8, primary), roll(.17, rare)]],
            "poach": [roll(.9, primary), roll(.1, rare)], "affinities": {weak: 1.6, "fire" if id_.startswith("ruin_") else "dark": .5},
            "move": spell})
        definition, palette = rigs[rig]
        for b in definition["bones"]:
            for c in b.get("cubes", []):
                snap_cube_to_pixels(c)
        uv = pack_model_cubes(definition["bones"])
        if rig in ("ruin_tyrant", "ruin_sauropod", "ruin_slagworm"):
            for animation in ("strike", "cast"):
                definition["animations"][animation]["bones"]["neck"] = {
                    "rotation": {"0.0": [0, 0, 0], "0.23": [-16, 0, 0], "0.47": [18, 0, 0], "0.71": [0, 0, 0]}}
        if rig in ("ruin_deathgaze", "ruin_ray"):
            for animation in ("idle", "cast"):
                for side, amount in (("left", 23), ("right", -23)):
                    definition["animations"][animation]["bones"][f"wing_{side}"] = {
                        "rotation": {"0": [0, 0, 0], "0.45": [0, 0, amount], "0.9": [0, 0, 0]}}
        save_json(ASSETS / f"geckolib/models/entity/bestiary/{rig}.geo.json", build_geometry_json(rig, definition, uv))
        save_json(ASSETS / f"geckolib/animations/entity/bestiary/{rig}.animation.json", build_animation_json(definition))
        write_entity_texture(ASSETS / f"textures/entity/bestiary/{id_}.png",
                             generate_texture(id_, definition, uv), uv, definition["bones"], palette)
    save_json(RESOURCES / "ivalice/ruin_bestiary.json", rows)


def art():
    radiant = {"judgment_glass", "pharos_runelight", "crystal_lattice", *(id_ for id_, _, _, _ in SEALS)}
    pickaxe = MINECRAFT / "tags/block/mineable/pickaxe.json"
    pickaxe_tag = json.loads(pickaxe.read_text(encoding="utf-8"))
    with Image.open(ART / "ivalice-dimensions.png") as atlas:
        atlas = atlas.convert("RGB")
        w, h = atlas.size
        for id_, index in [(id_, i) for i, (id_, _, _) in enumerate(REGION_ART)] + [(id_, i) for id_, _, _, i in SEALS]:
            x, y = index % 4, index // 4
            tile = atlas.crop((x*w//4+8, y*h//4+8, (x+1)*w//4-8, (y+1)*h//4-8)).resize((32, 32), Image.Resampling.LANCZOS)
            is_seal = any(id_ == block for block, _, _, _ in SEALS)
            if is_seal:
                ring = Image.new("RGB", (32, 32), (98, 82, 72))
                ring.paste(tile.resize((26,26), Image.Resampling.LANCZOS), (3,3))
                tile = ring
            tile = tile.convert("RGBA")
            path = ASSETS / f"textures/block/{id_}.png"
            if id_ in radiant:
                frames = Image.new("RGBA", (32, 32 * 8))
                for frame in range(8):
                    color = ImageEnhance.Brightness(tile).enhance(.85 + .16 * math.cos(frame * math.tau / 8))
                    frames.paste(color, (0, frame * 32))
                frames.save(path)
                save_json(path.with_suffix(".png.mcmeta"), {"animation": {"frametime": 3, "interpolate": True}})
            else:
                tile.save(path)
            pixels = [[tile.getpixel((x,y)) for x in range(32)] for y in range(32)]
            gray = ImageOps.grayscale(tile)
            depth = [[64 + gray.getpixel((x,y)) * .65 for x in range(32)] for y in range(32)]
            smooth = [[166 if id_ in radiant else 69]*32 for _ in range(32)]
            metal = [[145 if id_ in ("rusted_magitek", "magitek_plating", "ancient_bronze") else 10]*32 for _ in range(32)]
            emissive = [[max(0, round((gray.getpixel((x,y))-80)*1.1)) if id_ in radiant else 0 for x in range(32)] for y in range(32)]
            normal, specular = compute_pbr_maps(pixels, depth, smooth, metal, emissive)
            save_png(path.with_name(id_ + "_n.png"), normal)
            save_png(path.with_name(id_ + "_s.png"), specular)
            block_assets(id_, False)
            if id_ != "ruin_ash" and "ivalice:" + id_ not in pickaxe_tag["values"]:
                pickaxe_tag["values"].append("ivalice:" + id_)
    save_json(pickaxe, pickaxe_tag)
    for id_, _, _, source in KEYS:
        image = inventory_icon("orb") if id_ == "tza_black_orb" else relic_icon(source)
        if id_ != "judgment_fragment":
            pixels = image.load()
            for y in range(image.height):
                for x in range(image.width):
                    r,g,b,a = pixels[x,y]
                    if id_ == "tza_black_orb":
                        pixels[x,y] = (round(b*.57), round(g*.55), round(r*.65),a)
                    else:
                        pixels[x,y] = (round(r*.46),round(r*.84),max(g,b),a)
        image.save(ASSETS / f"textures/item/{id_}.png")
        save_json(ASSETS / f"models/item/{id_}.json", {"parent": "minecraft:item/generated",
                  "textures": {"layer0": f"ivalice:item/{id_}"}})
        save_json(ASSETS / f"items/{id_}.json", {"model": {"type": "minecraft:model", "model": f"ivalice:item/{id_}"}})


def translations():
    messages = {
        "ruin_seal.key": ("O altar aguarda %s.", "The altar is waiting for %s."),
        "ruin_seal.dormant": ("O altar permanece adormecido enquanto houver um combate ou guardião por perto.",
                              "The altar lies dormant while a battle or guardian is nearby."),
        "ruin_seal.space": ("Afaste-se e libere o espaço acima do altar.", "Step back and clear the space above the altar."),
        "ruin_seal.awakened": ("%s despertou.", "%s has awakened."),
        "dimension.ruin": ("Mundo em Ruínas", "World of Ruin"),
        "dimension.ruin.subtitle": ("FFVI • Um mundo depois do Julgamento", "FFVI • A world after the Judgment"),
        "dimension.ancient": ("Ruínas Ancestrais", "Ancestral Ruins"),
        "dimension.ancient.subtitle": ("FFXII • Ridorana e o Grande Cristal", "FFXII • Ridorana and the Great Crystal"),
        "ruin_seal.kefka_hint": ("Encontre a Torre de Kefka no Nether e use o Fragmento do Julgamento no altar do topo.",
                                "Find Kefka's Tower in the Nether and use a Judgment Fragment at the summit altar."),
    }
    for lang, index in (("pt_br", 0), ("en_us", 1)):
        path = ASSETS / f"lang/{lang}.json"
        s = json.loads(path.read_text(encoding="utf-8"))
        for entry in (*REGION_ART, *SEALS):
            s["block.ivalice." + entry[0]] = entry[1 + index]
        for entry in KEYS:
            s["item.ivalice." + entry[0]] = entry[1 + index]
        for id_, name, *_ in MOBS:
            s["entity.ivalice." + id_] = {"Remanescente Magitek": "Magitek Remnant",
                "Sentinela de Ridorana": "Ridorana Sentinel", "Guardião do Grande Cristal": "Great Crystal Warden"}.get(name, name) if index else name
        for name, message in messages.items():
            s["ivalice." + name] = message[index]
        save_json(path, s)
        # biome display names are under the original keys so existing biome palettes keep working.
        biome_path = RESOURCES / f"assets/minecraft/lang/{lang}.json"
        entries = {f"biome.minecraft.{key}": value[index] for palette in (NETHER, END) for key, value in palette.items()}
        save_json(biome_path, entries)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("minecraft_jar", type=Path)
    args = parser.parse_args()
    with ZipFile(args.minecraft_jar) as jar:
        terrain(jar)
        biomes(jar)
    recipes()
    for site in (kefka_tower, magitek_boneyard, mobliz_ruins, pharos, subterra, giruvegan, observatory):
        site()
    loot_tables()
    translations()
    ruin_mobs()
    art()
    print(f"Generated 2 dimensions, {len(NETHER)+len(END)} zones, 7 structures and {len(MOBS)} new creatures")


if __name__ == "__main__":
    main()
