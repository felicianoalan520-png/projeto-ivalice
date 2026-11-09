package com.ivalice.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ivalice.IvaliceMod;
import com.ivalice.entity.IvaliceBestiary;
import com.ivalice.entity.IvaliceMonsterStats;
import com.ivalice.job.ZodiacJob;
import com.ivalice.network.IvaliceStatePayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Armazena e gerencia o progresso persistente do jogador em Ivalice (Etapas 2, 3, 4 e 5):
 * - Gil (Moeda de Ivalice)
 * - LP (License Points)
 * - Rank no Clã Centurio
 * - Bestiário (Clan Primer: contagem de abates por espécie e páginas de sabedoria desbloqueadas)
 * - Caçadas (Mark Hunts ativas e concluídas)
 * - Licenças desbloqueadas (License Board + modificadores reais de atributos)
 * - Gambits ativos (regras automáticas de suporte/combate)
 * - Materiais vendidos no Bazaar e pacotes desbloqueados/comprados
 */
public class IvalicePlayerData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, PlayerProfile> PROFILES = new ConcurrentHashMap<>();

    public static class PlayerProfile {
        public int gil = 500;
        public int lp = 30;
        public int rustCharge;
        public long adjustedExperience;
        public String activeHuntId = "";
        public Set<String> jobs = new LinkedHashSet<>();
        public Set<String> encounteredMonsters = new LinkedHashSet<>();
        public Map<String, Integer> bestiaryKills = new LinkedHashMap<>();
        private transient String lastChainFamily;
        private transient long lastChainKill;
        private transient int chainKills;

        public int recordDropChain(String family, long gameTime) {
            if (!family.equals(lastChainFamily) || gameTime < lastChainKill || gameTime - lastChainKill > 2400) {
                chainKills = 0;
            }
            lastChainFamily = family;
            lastChainKill = gameTime;
            return Math.min(3, (chainKills++) / 5);
        }
        public Set<String> completedHunts = new LinkedHashSet<>();
        public Set<String> unlockedLicenses = new LinkedHashSet<>();
        public Set<String> learnedSpells = new LinkedHashSet<>();
        public List<String> activeGambits = new ArrayList<>();
        public Set<String> learnedGambits = new LinkedHashSet<>();
        public List<String> gambitRules = new ArrayList<>();
        public Map<String, Integer> bazaarLootSold = new LinkedHashMap<>();
        public Set<String> bazaarPurchased = new LinkedHashSet<>();

        public PlayerProfile() {
            // Gambits básicos iniciais
            activeGambits.add("SELF_HP_50_POTION");
            activeGambits.add("FOE_WEAKNESS_MAGIC");
            activeGambits.add("FOE_NEAREST_ATTACK");
            addStarterLicenses();
        }

        public void addStarterLicenses() {
            unlockedLicenses.add("WHITE_MAGICK_1");
            unlockedLicenses.add("LIGHT_ARMOR_1");
            unlockedLicenses.add("SWORDS_1");
            learnedSpells.add("cure");
            learnedSpells.add("fire");
        }

        public int getKills(String speciesId) {
            return bestiaryKills.getOrDefault(speciesId, 0);
        }

        public void recordKill(String speciesId, int rewardGil, int rewardLp) {
            encounteredMonsters.add(speciesId);
            bestiaryKills.put(speciesId, getKills(speciesId) + 1);
            this.gil += rewardGil;
            this.lp += rewardLp;
        }

        public int getBattleLevel() {
            long xp = adjustedExperience;
            for (var entry : bestiaryKills.entrySet()) {
                var monster = IvaliceMonsterStats.byId(entry.getKey());
                if (monster.id().equals(entry.getKey())) xp += (long) entry.getValue() * monster.xpReward();
            }
            int level = 1;
            while (level < 99 && xp >= 100L * level * (level + 1) / 2) {
                level++;
            }
            return level;
        }

        public String getClanRank() {
            int hunts = completedHunts.size();
            int totalKills = bestiaryKills.values().stream().mapToInt(Integer::intValue).sum();
            if (hunts >= 5 || totalKills >= 50) return "Knight of the Round (Cavaleiro da Távola)";
            if (hunts >= 4 || totalKills >= 30) return "Order of Ambrosia (Ordem de Ambrósia)";
            if (hunts >= 3 || totalKills >= 20) return "Riskbreaker (Quebrador de Riscos)";
            if (hunts >= 2 || totalKills >= 12) return "High Guardian (Alto Guardião)";
            if (hunts >= 1 || totalKills >= 5) return "Vanguard (Vanguarda)";
            return "Moppet (Recruta Centurio)";
        }
    }

    public static PlayerProfile get(UUID uuid) {
        return PROFILES.computeIfAbsent(uuid, k -> new PlayerProfile());
    }

    public static PlayerProfile get(ServerPlayer player) {
        Path file = getPlayerFile(player);
        PlayerProfile profile = PROFILES.computeIfAbsent(player.getUUID(), k -> loadFromDisk(file));
        return profile;
    }

    public static void unload(UUID uuid) {
        PROFILES.remove(uuid);
    }

    public static void save(ServerPlayer player) {
        PlayerProfile profile = get(player);
        Path file = getPlayerFile(player);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(profile, writer);
            }
        } catch (IOException e) {
            IvaliceMod.LOGGER.error("[Projeto Ivalice] Falha ao salvar dados de Ivalice para {}: {}", player.getName().getString(), e.getMessage());
        }
        IvaliceStatePayload.sendProfile(player);
    }

    private static PlayerProfile loadFromDisk(Path file) {
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                PlayerProfile loaded = GSON.fromJson(reader, PlayerProfile.class);
                if (loaded != null) {
                    if (loaded.learnedSpells == null) {
                        loaded.learnedSpells = new LinkedHashSet<>();
                    }
                    if (loaded.learnedSpells.isEmpty()) {
                        loaded.learnedSpells.add("cure");
                        loaded.learnedSpells.add("fire");
                    }
                    loaded.encounteredMonsters.addAll(loaded.bestiaryKills.keySet());
                    return loaded;
                }
            } catch (Exception e) {
                IvaliceMod.LOGGER.warn("[Projeto Ivalice] Erro ao ler perfil salvo, criando novo: {}", e.getMessage());
            }
        }
        return new PlayerProfile();
    }

    private static Path getPlayerFile(ServerPlayer player) {
        Path worldRoot = player.level().getServer().getWorldPath(LevelResource.ROOT);
        return worldRoot.resolve("ivalice_players").resolve(player.getUUID().toString() + ".json");
    }

    /**
     * Aplica os bônus passivos do Tabuleiro de Licenças (License Board) diretamente nos atributos do jogador.
     */
    public static void applyLicenseAttributes(ServerPlayer player) {
        PlayerProfile profile = get(player);
        int level = profile.getBattleLevel();

        double bonusHp = (level - 1) * 0.40; // +39.2 HP no Nível 99 (quase +20 corações extras pela progressão 1->99)
        if (profile.unlockedLicenses.contains("HP_LORE_1")) bonusHp += 4.0;   // +2 corações
        if (profile.unlockedLicenses.contains("HP_LORE_2")) bonusHp += 6.0;   // +3 corações
        if (profile.unlockedLicenses.contains("HP_LORE_3")) bonusHp += 10.0;  // +5 corações

        double bonusAttack = (level - 1) * 0.22; // +21.5 ATQ base físico no Nível 99
        if (profile.unlockedLicenses.contains("BATTLE_LORE_1")) bonusAttack += 2.0;
        if (profile.unlockedLicenses.contains("BATTLE_LORE_2")) bonusAttack += 3.0;
        if (profile.unlockedLicenses.contains("GREATSWORD_TOURNESOL")) bonusAttack += 2.0;

        double bonusSpeed = 0.0;
        if (profile.unlockedLicenses.contains("SWIFTNESS_1")) bonusSpeed += 0.02;
        if (profile.unlockedLicenses.contains("SWIFTNESS_2")) bonusSpeed += 0.03;

        double bonusArmor = (level - 1) * 0.12; // +11.7 Defesa natural no Nível 99
        if (profile.unlockedLicenses.contains("SHIELD_BLOCK_1")) bonusArmor += 3.0;
        if (profile.unlockedLicenses.contains("HEAVY_ARMOR_MAX")) bonusArmor += 4.0;

        for (String id : profile.jobs) {
            ZodiacJob job = ZodiacJob.byId(id);
            if (job != null) {
                bonusAttack += job.attack;
                bonusArmor += job.armor;
                bonusSpeed += job.speed;
            }
        }

        applyModifier(player.getAttribute(Attributes.MAX_HEALTH), "license_hp_bonus", bonusHp);
        applyModifier(player.getAttribute(Attributes.ATTACK_DAMAGE), "license_attack_bonus", bonusAttack);
        applyModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), "license_speed_bonus", bonusSpeed);
        applyModifier(player.getAttribute(Attributes.ARMOR), "license_armor_bonus", bonusArmor);
    }

    private static void applyModifier(AttributeInstance instance, String path, double value) {
        if (instance == null) return;
        Identifier id = Identifier.fromNamespaceAndPath(IvaliceMod.MODID, path);
        instance.removeModifier(id);
        if (value > 0.0) {
            instance.addPermanentModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    // Catálogo de Licenças disponíveis no Tabuleiro de Licenças
    public record LicenseNode(String id, String name, String category, int lpCost, String description) {}

    public static final List<LicenseNode> LICENSE_CATALOG = List.of(
            new LicenseNode("HP_LORE_1", "HP Lore +100 (+4 HP)", "Augment", 25, "Aumenta a vida máxima permanentemente em +4 pontos."),
            new LicenseNode("HP_LORE_2", "HP Lore +200 (+6 HP)", "Augment", 50, "Aumenta a vida máxima permanentemente em +6 pontos."),
            new LicenseNode("HP_LORE_3", "HP Lore +500 (+10 HP)", "Augment", 90, "Aumenta a vida máxima permanentemente em +10 pontos."),
            new LicenseNode("BATTLE_LORE_1", "Battle Lore I (+2 ATQ)", "Augment", 30, "Aumenta a força física em batalha em +2 de dano."),
            new LicenseNode("BATTLE_LORE_2", "Battle Lore II (+3 ATQ)", "Augment", 65, "Aumenta a força física em batalha em +3 de dano."),
            new LicenseNode("SWIFTNESS_1", "Swiftness I (+Velocidade)", "Augment", 35, "Reduz o tempo de recarga ATB e aumenta a velocidade."),
            new LicenseNode("SWIFTNESS_2", "Swiftness II (+Velocidade II)", "Augment", 75, "Aumenta ainda mais a velocidade de movimento e ATB."),
            new LicenseNode("MAGICK_LORE_1", "Magick Lore I (+25% Magia)", "Augment", 40, "Potencializa o dano e cura de todas as Magicks e +50 MP."),
            new LicenseNode("MAGICK_LORE_2", "Magick Lore II (Magias -ga)", "Magick", 85, "Domínio das magias de 3º escalão (Firaga, Blizzaga, Thundaga, Waterga, Aeroga, Curaga)."),
            new LicenseNode("ARCANE_SUPREMACY", "Magia Arcana Suprema (Tier 4)", "Magick", 160, "Domínio das magias cataclísmicas (Flare, Ardor, Collapse, Scathe, Holy) e +100 MP."),
            new LicenseNode("SHIELD_BLOCK_1", "Shield Block (+3 DEF)", "Augment", 35, "Aumenta a defesa base e bloqueio com escudos."),
            new LicenseNode("HEAVY_ARMOR_MAX", "Heavy Armor 10 (+4 DEF)", "Armor", 60, "Domínio da Armadura Maximiliana (+4 de Defesa)."),
            new LicenseNode("GREATSWORD_TOURNESOL", "Greatswords 4 (Tournesol)", "Weapon", 100, "Maestria suprema com espadas de duas mãos (+2 ATQ).")
    );

    // Catálogo de Caçadas (Clan Centurio Mark Hunts)
    public record MarkHuntEntry(
            String id,
            String markName,
            String rank,
            String entityId,
            String biomeId,
            String petitioner,
            int gilReward,
            int lpReward,
            String bountyItemId,
            String description
    ) {}

    private static final List<MarkHuntEntry> LEGACY_HUNTS = List.of(
            new MarkHuntEntry(
                    "rogue_tomato",
                    "Rogue Tomato (Tomate Perdido)",
                    "Rank I",
                    "rogue_tomato",
                    "dalmasca_estersand",
                    "Tomaj (Taverna Sandsea - Rabanastre)",
                    300,
                    15,
                    "galbana_lilies",
                    "Um Alraune feroz que atacou caravanas no Deserto Leste de Dalmasca."
            ),
            new MarkHuntEntry(
                    "thextera",
                    "Thextera (Lobo Mutante)",
                    "Rank I",
                    "thextera",
                    "dalmasca_westersand",
                    "Gatsly (Mercado Muthru)",
                    500,
                    20,
                    "teleport_stone",
                    "Um lobo alfa gigante avistado nos penhascos do Deserto Oeste de Dalmasca."
            ),
            new MarkHuntEntry(
                    "flowering_cactoid",
                    "Flowering Cactoid (Cactoide Florido)",
                    "Rank I",
                    "flowering_cactoid",
                    "dalmasca_estersand",
                    "Dantro (Acampamento do Estersand)",
                    750,
                    25,
                    "wyrmfire_shot",
                    "Um Cactoid raro com uma flor escarlate no topo; usa 1.000 Needles quando ameaçado."
            ),
            new MarkHuntEntry(
                    "firemane_hunt",
                    "Firemane (Pesadelo Flamejante)",
                    "Rank III (Boss)",
                    "firemane",
                    "dalmasca_westersand",
                    "Guarda Real de Rabanastre",
                    2200,
                    55,
                    "gemsteel",
                    "Espírito equino de fogo puro que calcina o terreno por onde passa."
            ),
            new MarkHuntEntry(
                    "white_mousse",
                    "White Mousse (Mousse Branco)",
                    "Rank V",
                    "white_mousse",
                    "paramina_rift",
                    "Sorbet (Bairro Oeste)",
                    2800,
                    65,
                    "yoichi_bow",
                    "Um Flan ancestral de gelo e água que domina fendas geladas e pântanos."
            ),
            new MarkHuntEntry(
                    "belias_trial",
                    "Belias, the Gigas (Provação do Esper)",
                    "Rank S (Esper)",
                    "belias",
                    "ogir_yensa_sandsea",
                    "Clã Centurio — Montblanc",
                    5000,
                    120,
                    "esper_belias_glyph",
                    "O Guardião do Fogo criado pelos deuses; derrotá-lo concede o Glifo de Belias."
            ),
            new MarkHuntEntry(
                    "mateus_trial",
                    "Mateus, the Corrupt (Provação do Esper)",
                    "Rank S (Esper)",
                    "mateus",
                    "paramina_rift",
                    "Clã Centurio — Gran Kiltias Anastasis",
                    6500,
                    150,
                    "esper_mateus_glyph",
                    "O Esper do Gelo que reina sobre o Santuário de Bur-Omisace e Paramina Rift."
            ),
            new MarkHuntEntry(
                    "gabranth_duel",
                    "Judge Magister Gabranth (Duelo Imperial)",
                    "Rank VII (Boss)",
                    "judge_gabranth",
                    "tchita_uplands",
                    "Resistência de Dalmasca",
                    9500,
                    180,
                    "grand_armor",
                    "O Juiz Magister da 9ª Agência Imperial em pessoa; mestre de Quickenings."
            ),
            new MarkHuntEntry(
                    "gilgamesh_hunt",
                    "Gilgamesh (O Espadachim Misterioso)",
                    "Rank VII",
                    "gilgamesh",
                    "phon_coast",
                    "Montblanc (Sede do Clã Centurio)",
                    12000,
                    220,
                    "genji_gloves",
                    "Guerreiro lendário de seis braços que busca as espadas mais raras de Ivalice."
            ),
            new MarkHuntEntry(
                    "zodiark_trial",
                    "Zodiark, Keeper of Precepts (Esper Supremo)",
                    "Rank S (Esper)",
                    "zodiark",
                    "feywood",
                    "Sábios de Jahara",
                    18000,
                    350,
                    "zodiac_spear",
                    "O 13º Esper proibido das Trevas; seu Final Eclipse devasta toda a região."
            ),
            new MarkHuntEntry(
                    "yiazmat_farewell",
                    "Yiazmat (Farewell to a Legend)",
                    "Rank S+ (Superboss)",
                    "yiazmat",
                    "cerobi_steppe",
                    "Montblanc — A Caçada Final",
                    50000,
                    500,
                    "wyrmhero_blade",
                    "O Dragão Sagrado supremo de Final Fantasy XII. Apenas lendas sobrevivem ao seu Cyclone."
            ),
            // === Caçadas Interdimensionais no Nether (Final Fantasy IV & Final Fantasy XV) ===
            new MarkHuntEntry(
                    "rubicante_nether",
                    "Rubicante, Archfiend of Fire [FFIV • Nether]",
                    "Rank VII (Nether)",
                    "rubicante",
                    "dalmasca_westersand",
                    "Edge & Rydia (Fenda do Nether)",
                    6800,
                    110,
                    "gemsteel",
                    "O Arquidemônio do Fogo de Final Fantasy IV habita o Nether. Derrotá-lo rende Gemsteel para forjar magias Tier 3 e Tier 4."
            ),
            new MarkHuntEntry(
                    "golbez_nether",
                    "Golbez, Clad in Dark [FFIV • Nether]",
                    "Rank VII (Nether)",
                    "golbez",
                    "nabreus_deadlands",
                    "Cecil Harvey (Santuário Lunar)",
                    14000,
                    240,
                    "empyreal_soul",
                    "O feiticeiro encouraçado de FFIV domina o Vale das Almas no Nether; concede Empyreal Soul e Dark Magicite."
            ),
            new MarkHuntEntry(
                    "ifrit_ffxv_nether",
                    "Ifrit, the Infernian [FFXV • Nether]",
                    "Rank S (Astral)",
                    "ifrit_pyreburner",
                    "ogir_yensa_sandsea",
                    "Oráculo de Eos",
                    26000,
                    380,
                    "tza_ardor",
                    "O Deus Astral do Fogo de Final Fantasy XV aguarda nas fortalezas vulcânicas do Nether."
            ),
            new MarkHuntEntry(
                    "adamantoise_ffxv",
                    "Adamantoise, Lonely Rumbler [FFXV • Nether]",
                    "Rank S+ (Titã Nv.92)",
                    "adamantoise",
                    "mosphoran_highwaste",
                    "Takka (Hammerhead / Clã Centurio)",
                    42000,
                    480,
                    "zodiac_escutcheon",
                    "A tartaruga-montanha colossal de FFXV que estremece o Nether inteiro."
            ),
            // === Caçadas Interdimensionais no Fim / The End (Final Fantasy V & Final Fantasy VI) ===
            new MarkHuntEntry(
                    "ultros_end",
                    "Ultros, Octopus Royalty [FFVI • The End]",
                    "Rank V (The End)",
                    "ultros",
                    "phon_coast",
                    "Empresário da Ópera (Fenda do End)",
                    5800,
                    95,
                    "water_stone",
                    "O infame polvo roxo de Final Fantasy VI apareceu nas ilhas da Dimensão do Fim!"
            ),
            new MarkHuntEntry(
                    "atb_weapon_end",
                    "Atma Weapon [FFVI • The End]",
                    "Rank S (Nv.86)",
                    "atb_weapon",
                    "feywood",
                    "Esper Ancião de Thamasa",
                    36000,
                    420,
                    "excalibur",
                    "A arma biológica ancestral do Continente Flutuante de FFVI paira na Dimensão do Fim."
            ),
            new MarkHuntEntry(
                    "shinryu_end",
                    "Shinryu, Dragon of the Rift [FFV • The End]",
                    "Rank S+ (Nv.90)",
                    "shinryu",
                    "paramina_rift",
                    "Bartz Klauser (Viajante da Fenda)",
                    44000,
                    460,
                    "serpentarius",
                    "O dragão cósmico de Final Fantasy V que habita a Dimensão do Fim e desencadeia Tidal Wave."
            ),
            new MarkHuntEntry(
                    "omega_mk_end",
                    "Omega, Ancient Warmachine [FFV • The End]",
                    "Rank S+ (Nv.96)",
                    "omega_mk",
                    "cerobi_steppe",
                    "Cid Previa (Arquivos Antigos)",
                    55000,
                    550,
                    "fomalhaut",
                    "A máquina autônoma suprema de Final Fantasy V que patrulha as ruínas da Dimensão do Fim."
            ),
            new MarkHuntEntry(
                    "kefka_god_end",
                    "Kefka, God of Magic [FFVI • Nível 99]",
                    "Rank S++ (Deus Nv.99)",
                    "kefka_god",
                    "Torre de Kefka (Nether)",
                    "Terra Branford & Returners",
                    75000,
                    750,
                    "ribbon",
                    "Leve o Fragmento do Julgamento ao altar da Torre no Mundo em Ruínas. Kefka alterna magias conforme perde vida."
            )
    );

    public static final List<MarkHuntEntry> MARK_HUNTS = buildHunts();

    private static List<MarkHuntEntry> buildHunts() {
        var hunts = new ArrayList<>(LEGACY_HUNTS);
        for (var entry : IvaliceBestiary.entries().values()) {
            if (!entry.isHunt() || entry.original()) continue;
            var stats = IvaliceMonsterStats.byId(entry.id());
            hunts.add(new MarkHuntEntry(entry.id(), entry.name(), entry.kind(), entry.id(), entry.regions().getFirst(),
                    "Clã Centurio", Math.max(50, entry.gil() / 3), stats.lpReward(), "",
                    "Habitat original: " + entry.displayHabitat() + ". Região do contrato: "
                            + entry.regions().getFirst().replace('_', ' ') + " • Nível " + entry.minimumLevel() + "+."));
        }
        return List.copyOf(hunts);
    }

    // Catálogo de Pacotes de Alquimia & Bazaar de Rabanastre
    public record BazaarPackage(
            String id,
            String packageName,
            String rewardItemId,
            int rewardCount,
            int gilCost,
            Map<String, Integer> requiredLoot,
            String description
    ) {
        public boolean requiresLoot() {
            return id.startsWith("grimoire_") || id.endsWith("_blade");
        }
    }

    public static final List<BazaarPackage> BAZAAR_PACKAGES = List.of(
            new BazaarPackage(
                    "potion_pack",
                    "Alquimia: Flask of Oily Liquid (Hi-Potions)",
                    "hi_potion",
                    5,
                    150,
                    Map.of("wolf_pelt", 2, "cactus_fruit", 1),
                    "2x Wolf Pelt + 1x Cactus Fruit -> Sintetiza 5x Hi-Potions."
            ),
            new BazaarPackage(
                    "x_potion_alchemy",
                    "Alquimia: Tônico de Ambrósia (X-Potions)",
                    "x_potion",
                    4,
                    480,
                    Map.of("tza_drab_wool", 2, "water_stone", 2),
                    "2x Drab Wool + 2x Water Stone -> Sintetiza 4x X-Potions."
            ),
            new BazaarPackage(
                    "ether_alchemy",
                    "Alquimia: Destilado de Mist (Ethers)",
                    "ether",
                    3,
                    600,
                    Map.of("fish_scale", 2, "water_stone", 1, "wind_stone", 1),
                    "2x Fish Scale + 1x Water Stone + 1x Wind Stone -> Sintetiza 3x Ethers."
            ),
            new BazaarPackage(
                    "phoenix_flight",
                    "Alquimia: Essence of the Phoenix (Phoenix Downs)",
                    "phoenix_down",
                    3,
                    300,
                    Map.of("wind_stone", 2, "small_feather", 2),
                    "2x Wind Stone + 2x Small Feather -> Sintetiza 3x Phoenix Downs."
            ),
            new BazaarPackage(
                    "elixir_alchemy",
                    "Alquimia Real: Pedra Filosofal (Elixir)",
                    "elixir",
                    2,
                    3500,
                    Map.of("fire_stone", 2, "water_stone", 2, "dark_magicite", 1),
                    "2x Fire Stone + 2x Water Stone + 1x Dark Magicite -> Sintetiza 2x Elixirs."
            ),
            new BazaarPackage(
                    "grimoire_firaga",
                    "Alquimia Arcana (Tier 3): Pergaminho de Firaga",
                    "tza_firaga",
                    1,
                    2800,
                    Map.of("fire_stone", 4, "dark_magicite", 1),
                    "4x Fire Stone (Dalmasca/Rubicante) + 1x Dark Magicite -> Fabricar Firaga (Nv. 42+)."
            ),
            new BazaarPackage(
                    "grimoire_blizzaga",
                    "Alquimia Arcana (Tier 3): Pergaminho de Blizzaga",
                    "tza_blizzaga",
                    1,
                    2800,
                    Map.of("water_stone", 4, "dark_magicite", 1),
                    "4x Water Stone (Paramina/Ultros) + 1x Dark Magicite -> Fabricar Blizzaga (Nv. 44+)."
            ),
            new BazaarPackage(
                    "grimoire_thundaga",
                    "Alquimia Arcana (Tier 3): Pergaminho de Thundaga",
                    "tza_thundaga",
                    1,
                    2800,
                    Map.of("wind_stone", 4, "dark_magicite", 1),
                    "4x Wind Stone (Phon/Coeurl) + 1x Dark Magicite -> Fabricar Thundaga (Nv. 46+)."
            ),
            new BazaarPackage(
                    "grimoire_waterga",
                    "Alquimia Arcana (Tier 3): Pergaminho de Waterga",
                    "tza_waterga",
                    1,
                    3000,
                    Map.of("water_stone", 4, "fish_scale", 3),
                    "4x Water Stone + 3x Fish Scale (Ichthon/Ultros no End) -> Fabricar Waterga (Nv. 50+)."
            ),
            new BazaarPackage(
                    "grimoire_aeroga",
                    "Alquimia Arcana (Tier 3): Pergaminho de Aeroga",
                    "tza_aeroga",
                    1,
                    3000,
                    Map.of("wind_stone", 4, "small_feather", 3),
                    "4x Wind Stone + 3x Small Feather (Cerobi/Chocobo) -> Fabricar Aeroga (Nv. 52+)."
            ),
            new BazaarPackage(
                    "grimoire_darkga",
                    "Alquimia Arcana (Tier 3): Pergaminho de Darkga",
                    "tza_darkga",
                    1,
                    3200,
                    Map.of("dark_magicite", 4, "earth_stone", 2),
                    "4x Dark Magicite (Nabreus/Mindflayer no Nether) + 2x Earth Stone -> Fabricar Darkga (Nv. 56+)."
            ),
            new BazaarPackage(
                    "grimoire_curaga",
                    "Alquimia Branca (Tier 3): Pergaminho de Curaga",
                    "tza_curaga",
                    1,
                    2600,
                    Map.of("water_stone", 3, "tza_drab_wool", 3),
                    "3x Water Stone + 3x Drab Wool -> Fabricar Curaga (Nv. 38+)."
            ),
            new BazaarPackage(
                    "grimoire_hastega",
                    "Alquimia Temporal (Tier 3): Pergaminho de Hastega",
                    "magick_hastega",
                    1,
                    2600,
                    Map.of("wind_stone", 3, "small_feather", 2, "dark_magicite", 1),
                    "3x Wind Stone + 2x Small Feather + 1x Dark Magicite -> Fabricar Hastega (Nv. 36+)."
            ),
            new BazaarPackage(
                    "grimoire_holy",
                    "Alquimia Sagrada (Tier 4): Tomo de Holy",
                    "tza_holy",
                    1,
                    7200,
                    Map.of("sunstone", 2, "water_stone", 3),
                    "2x Sunstone (Giza/Ifrit) + 3x Water Stone -> Fabricar Holy (Nv. 68+ • Raridade Épica)."
            ),
            new BazaarPackage(
                    "grimoire_flare",
                    "Alquimia Astral (Tier 4): Tomo de Flare",
                    "tza_flare",
                    1,
                    7400,
                    Map.of("fire_stone", 5, "gemsteel", 1),
                    "5x Fire Stone + 1x Gemsteel (Nether: Iron Giant/Rubicante) -> Fabricar Flare (Nv. 74+ • Raridade Épica)."
            ),
            new BazaarPackage(
                    "grimoire_ardor",
                    "Alquimia Proibida (Tier 4): Tomo de Ardor",
                    "tza_ardor",
                    1,
                    8200,
                    Map.of("fire_stone", 6, "gemsteel", 2),
                    "6x Fire Stone + 2x Gemsteel (Nether: Ifrit/Omega) -> Fabricar Ardor (Nv. 82+ • Raridade Épica)."
            ),
            new BazaarPackage(
                    "grimoire_collapse",
                    "Alquimia Proibida (Tier 4): Tomo de Collapse",
                    "tza_collapse",
                    1,
                    8800,
                    Map.of("dark_magicite", 5, "empyreal_soul", 1),
                    "5x Dark Magicite + 1x Empyreal Soul (Golbez/Adamantoise no Nether) -> Fabricar Collapse (Nv. 88+)."
            ),
            new BazaarPackage(
                    "grimoire_scathe",
                    "Alquimia Divina (Tier 4): Tomo de Scathe",
                    "tza_scathe",
                    1,
                    9800,
                    Map.of("gemsteel", 1, "empyreal_soul", 1, "serpentarius", 1),
                    "1x Gemsteel (Nether) + 1x Empyreal Soul + 1x Serpentarius (The End: Shinryu/Gil Turtle) -> Fabricar Scathe (Nv. 95+)."
            ),
            new BazaarPackage(
                    "gemsteel_alloy",
                    "Forja Alquímica: Liga Gemsteel",
                    "gemsteel",
                    1,
                    4200,
                    Map.of("fire_stone", 5, "dark_magicite", 2),
                    "5x Fire Stone + 2x Dark Magicite -> Forja 1x Gemsteel."
            ),
            new BazaarPackage(
                    "masamune_blade",
                    "Bazaar: Master-crafted Blade (Katana Masamune)",
                    "masamune",
                    1,
                    8500,
                    Map.of("malboro_vine", 2, "dark_magicite", 2, "gemsteel", 1),
                    "2x Malboro Vine + 2x Dark Magicite + 1x Gemsteel -> Forja a lendária Katana Masamune."
            ),
            new BazaarPackage(
                    "sunflowered_blade",
                    "Bazaar: The Sunflowered Blade (Espada Tournesol)",
                    "tournesol",
                    1,
                    15000,
                    Map.of("gemsteel", 1, "empyreal_soul", 1, "serpentarius", 1),
                    "1x Gemsteel + 1x Empyreal Soul + 1x Serpentarius -> A suprema Greatsword Tournesol!"
            )
    );
}
