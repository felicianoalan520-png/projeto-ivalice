package com.ivalice.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ivalice.IvaliceMod;
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
        public String activeHuntId = "";
        public Set<String> jobs = new LinkedHashSet<>();
        public Set<String> encounteredMonsters = new LinkedHashSet<>();
        public Map<String, Integer> bestiaryKills = new LinkedHashMap<>();
        public Set<String> completedHunts = new LinkedHashSet<>();
        public Set<String> unlockedLicenses = new LinkedHashSet<>();
        public List<String> activeGambits = new ArrayList<>();
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
            long xp = 0;
            for (IvaliceMonsterStats monster : IvaliceMonsterStats.ALL_MONSTERS) {
                xp += (long) getKills(monster.id()) * monster.xpReward();
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

        double bonusHp = 0.0;
        if (profile.unlockedLicenses.contains("HP_LORE_1")) bonusHp += 4.0;   // +2 corações
        if (profile.unlockedLicenses.contains("HP_LORE_2")) bonusHp += 6.0;   // +3 corações
        if (profile.unlockedLicenses.contains("HP_LORE_3")) bonusHp += 10.0;  // +5 corações

        double bonusAttack = 0.0;
        if (profile.unlockedLicenses.contains("BATTLE_LORE_1")) bonusAttack += 2.0;
        if (profile.unlockedLicenses.contains("BATTLE_LORE_2")) bonusAttack += 3.0;
        if (profile.unlockedLicenses.contains("GREATSWORD_TOURNESOL")) bonusAttack += 2.0;

        double bonusSpeed = 0.0;
        if (profile.unlockedLicenses.contains("SWIFTNESS_1")) bonusSpeed += 0.02;
        if (profile.unlockedLicenses.contains("SWIFTNESS_2")) bonusSpeed += 0.03;

        double bonusArmor = 0.0;
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
            new LicenseNode("MAGICK_LORE_1", "Magick Lore (+25% Magia)", "Augment", 40, "Potencializa o dano e cura de todas as Magicks."),
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

    public static final List<MarkHuntEntry> MARK_HUNTS = List.of(
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
            )
    );

    // Catálogo de Pacotes do Bazaar de Rabanastre
    public record BazaarPackage(
            String id,
            String packageName,
            String rewardItemId,
            int rewardCount,
            int gilCost,
            Map<String, Integer> requiredLoot,
            String description
    ) {}

    public static final List<BazaarPackage> BAZAAR_PACKAGES = List.of(
            new BazaarPackage(
                    "potion_pack",
                    "Flask of Oily Liquid (Pacote de Poções)",
                    "hi_potion",
                    5,
                    150,
                    Map.of("wolf_pelt", 2, "cactus_fruit", 1),
                    "2x Wolf Pelt + 1x Cactus Fruit -> Desbloqueia 5x Hi-Potions."
            ),
            new BazaarPackage(
                    "phoenix_flight",
                    "Essence of the Phoenix (Voo da Fênix)",
                    "phoenix_down",
                    3,
                    300,
                    Map.of("wind_stone", 2, "small_feather", 2),
                    "2x Wind Stone + 2x Small Feather -> Desbloqueia 3x Phoenix Downs."
            ),
            new BazaarPackage(
                    "masamune_blade",
                    "Master-crafted Blade (Katana Masamune)",
                    "masamune",
                    1,
                    2500,
                    Map.of("malboro_vine", 2, "dark_magicite", 2),
                    "2x Malboro Vine + 2x Dark Magicite -> Forja a lendária Katana Masamune."
            ),
            new BazaarPackage(
                    "sunflowered_blade",
                    "The Sunflowered Blade (Espada Tournesol)",
                    "tournesol",
                    1,
                    5000,
                    Map.of("gemsteel", 1, "empyreal_soul", 1, "serpentarius", 1),
                    "1x Gemsteel + 1x Empyreal Soul + 1x Serpentarius -> A suprema Greatsword Tournesol!"
            )
    );
}
