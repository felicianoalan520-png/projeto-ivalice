package com.ivalice.registry;

import com.ivalice.IvaliceMod;
import com.ivalice.item.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;

/**
 * Catálogo Completo de Itens, Equipamentos, Consumíveis, Espólios (Loot), Magicks, Technicks e Spawn Eggs
 * de Final Fantasy XII: The Zodiac Age (Etapas 2, 3, 4 e 5).
 */
public class IvaliceItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IvaliceMod.MODID);

    private static final List<DeferredItem<? extends Item>> EQUIPMENT_TAB_ITEMS = new ArrayList<>();
    private static final List<DeferredItem<? extends Item>> LOOT_AND_CONSUMABLES_TAB_ITEMS = new ArrayList<>();
    private static final Map<String, DeferredItem<? extends Item>> BY_ID = new LinkedHashMap<>();

    // =========================================================================
    // 0. CRISTAL DE TELEPORTE E GUIAS DE SISTEMAS (ETAPAS 1, 4 e 5)
    // =========================================================================
    public static final DeferredItem<Item> TELEPORT_STONE = registerGuide(
            "teleport_stone", IvaliceItemCategory.KEY_ITEM,
            "Cristal laranja usado para teleporte entre Gate Crystals e Regiões de Ivalice. Clique Direito para abrir o Mapa de Teleporte!",
            200, "Drop de Thextera / Lojas do Clã Centurio", "FFXII Key / Loot Item: Teleport Stone", Rarity.RARE, 0, null
    );

    public static final DeferredItem<Item> CLAN_PRIMER = registerGuide(
            "clan_primer", IvaliceItemCategory.KEY_ITEM,
            "Compêndio oficial do Clã Centurio contendo o Bestiário completo, Sabedoria dos Sábios e Quadro de Caçadas. Clique Direito!",
            500, "Entregue por Montblanc na Sede do Clã Centurio", "FFXII Key Item: Clan Primer", Rarity.EPIC, 1, null
    );

    public static final DeferredItem<Item> LICENSE_TOME = registerGuide(
            "license_tome", IvaliceItemCategory.KEY_ITEM,
            "Tomo do Tabuleiro de Licenças Zodiacal. Clique Direito para gastar LP em bônus permanentes de HP, Ataque e Velocidade!",
            500, "Guilda de Rabanastre", "FFXII System: Zodiac License Board", Rarity.EPIC, 3, null
    );

    public static final DeferredItem<Item> GAMBIT_SCROLL = registerGuide(
            "gambit_scroll", IvaliceItemCategory.KEY_ITEM,
            "Pergaminho de configuração de Gambits e acesso ao Bazaar de Rabanastre. Clique Direito!",
            400, "Loja de Gambits de Yamoora (Rabanastre)", "FFXII System: Gambits & Bazaar", Rarity.RARE, 4, null
    );

    public static final DeferredItem<Item> ESPER_BELIAS_GLYPH = registerGuide(
            "esper_belias_glyph", IvaliceItemCategory.KEY_ITEM,
            "Glifo zodiacal de Áries. Clique Direito para invocar o Esper Belias, the Gigas!",
            5000, "Tumba de Raithwall / Caçada Rank S", "FFXII Esper: Belias, the Gigas", Rarity.EPIC, -1, "belias"
    );

    public static final DeferredItem<Item> ESPER_MATEUS_GLYPH = registerGuide(
            "esper_mateus_glyph", IvaliceItemCategory.KEY_ITEM,
            "Glifo zodiacal de Peixes. Clique Direito para invocar o Esper Mateus, the Corrupt!",
            6500, "Santuário de Miriam / Paramina Rift", "FFXII Esper: Mateus, the Corrupt", Rarity.EPIC, -1, "mateus"
    );

    // =========================================================================
    // 1. ARMAS CANÔNICAS (WEAPONS — 16 Categorias de FFXII)
    // =========================================================================
    public static final DeferredItem<Item> MYTHRIL_SWORD = registerWeapon("mythril_sword", IvaliceItemCategory.WEAPON_SWORD,
            3.0F, -2.4F, "Espada inicial forjada em mythril leve, empunhada por Vaan em Rabanastre.", 130, "Armaria de Rabanastre", "FFXII Sword: Mythril Sword", Rarity.COMMON);

    public static final DeferredItem<Item> DEATHBRINGER = registerWeapon("deathbringer", IvaliceItemCategory.WEAPON_SWORD,
            7.0F, -2.4F, "Lâmina sombria capaz de desferir golpes fatais contra feras e demônios.", 8000, "Barheim Passage / Ithuno", "FFXII Sword: Deathbringer", Rarity.RARE);

    public static final DeferredItem<Item> DURANDAL = registerWeapon("durandal", IvaliceItemCategory.WEAPON_SWORD,
            9.0F, -2.3F, "A mais poderosa espada de uma mão em Ivalice, imbuída de brilho sagrado (Holy).", 12500, "Bazaar: Durandal", "FFXII Sword: Durandal", Rarity.EPIC);

    public static final DeferredItem<Item> SAVE_THE_QUEEN = registerWeapon("save_the_queen", IvaliceItemCategory.WEAPON_GREATSWORD,
            8.0F, -2.6F, "Espada de cavalaria sagrada (Holy) consagrada à proteção da Ordem de Dalmasca.", 9500, "Mosphoran Highwaste / Bazaar", "FFXII Greatsword: Save the Queen", Rarity.RARE);

    public static final DeferredItem<Item> EXCALIBUR = registerWeapon("excalibur", IvaliceItemCategory.WEAPON_GREATSWORD,
            11.0F, -2.5F, "Espada lendária de elemento Sagrado (Holy) oculta no Grande Cristal de Giruvegan.", 18000, "Great Crystal", "FFXII Greatsword: Excalibur", Rarity.EPIC);

    public static final DeferredItem<Item> TOURNESOL = registerWeapon("tournesol", IvaliceItemCategory.WEAPON_GREATSWORD,
            14.0F, -2.4F, "A Espada Girassol suprema de Ivalice, forjada com Gemsteel, Empyreal Soul e Serpentarius (Fogo/Sagrado).", 35000, "Bazaar: The Sunflowered Blade", "FFXII Greatsword: Tournesol", Rarity.EPIC);

    public static final DeferredItem<Item> WYRMHERO_BLADE = registerWeapon("wyrmhero_blade", IvaliceItemCategory.WEAPON_GREATSWORD,
            15.0F, -2.8F, "Relíquia divina imbuída de Fé e Bravery permanentes (Elemento Sagrado / Holy).", 50000, "Provação de Yiazmat & Omega", "FFXII Greatsword: Wyrmhero Blade", Rarity.EPIC);

    public static final DeferredItem<Item> GLADIUS = registerWeapon("gladius", IvaliceItemCategory.WEAPON_DAGGER,
            5.5F, -1.8F, "Adaga afiada encantada com o elemento Vento (Wind), letal contra Cactoids e Rogue Tomato!", 2200, "Dalmasca Westersand (Lindbur Wolf)", "FFXII Dagger: Gladius", Rarity.UNCOMMON);

    public static final DeferredItem<Item> ZORLIN_SHAPE = registerWeapon("zorlin_shape", IvaliceItemCategory.WEAPON_DAGGER,
            7.5F, -1.7F, "Adaga aerodinâmica de combate veloz usada por caçadores veteranos em Tchita Uplands.", 6400, "Babanastre / Jahara", "FFXII Dagger: Zorlin Shape", Rarity.RARE);

    public static final DeferredItem<Item> SHIKARI_NAGASA = registerWeapon("shikari_nagasa", IvaliceItemCategory.WEAPON_DAGGER,
            9.5F, -1.6F, "Lâmina furtiva suprema dos caçadores Shikari, imbuída nas águas sombrias (Water).", 14000, "Mina de Lushu / Mina de Henne", "FFXII Ninja Sword / Dagger: Shikari Nagasa", Rarity.EPIC);

    public static final DeferredItem<Item> MASAMUNE = registerWeapon("masamune", IvaliceItemCategory.WEAPON_KATANA,
            11.5F, -2.0F, "Katana obra-prima de Gilgamesh com altíssima taxa de combos consecutivos.", 22000, "Caçada Gilgamesh / Bazaar: Master-crafted Blade", "FFXII Katana: Masamune", Rarity.EPIC);

    public static final DeferredItem<Item> ZODIAC_SPEAR = registerWeapon("zodiac_spear", IvaliceItemCategory.WEAPON_SPEAR,
            15.0F, -2.5F, "A Lança Zodiacal mais poderosa de Final Fantasy XII, guardada nas profundezas de Nabudis.", 40000, "Necrohol of Nabudis / Henne Mines", "FFXII Spear: Zodiac Spear", Rarity.EPIC);

    public static final DeferredItem<Item> GUNGNIR = registerWeapon("gungnir", IvaliceItemCategory.WEAPON_SPEAR,
            9.5F, -2.5F, "Lança ardente imbuída de Fogo (Fire), capaz de incinerar feras de Paramina e Feywood.", 11000, "Pharos at Ridorana", "FFXII Spear: Gungnir", Rarity.RARE);

    public static final DeferredItem<Item> WHALE_WHISKER = registerWeapon("whale_whisker", IvaliceItemCategory.WEAPON_POLE,
            10.5F, -2.1F, "Bastão místico de monge que atinge tanto inimigos terrestres quanto voadores (Elemento Água).", 14500, "Pharos / Bazaar", "FFXII Pole: Whale Whisker", Rarity.EPIC);

    public static final DeferredItem<Item> YOICHI_BOW = registerWeapon("yoichi_bow", IvaliceItemCategory.WEAPON_BOW,
            8.5F, -2.2F, "Arco longo tradicional entregue por Sorbet após a derrota da Mark Rank V White Mousse.", 9200, "Recompensa da Caçada White Mousse", "FFXII Bow: Yoichi Bow", Rarity.RARE);

    public static final DeferredItem<Item> SAGITTARIUS = registerWeapon("sagittarius", IvaliceItemCategory.WEAPON_BOW,
            12.0F, -2.1F, "O arco zodiacal supremo de prata pura, encantado com ventos celestiais (Wind).", 26000, "Bazaar: Silver Bow", "FFXII Bow: Sagittarius", Rarity.EPIC);

    public static final DeferredItem<Item> GASTRAFETHES = registerWeapon("gastrafetes", IvaliceItemCategory.WEAPON_CROSSBOW,
            9.0F, -2.2F, "Besta pesada de disparo rápido encontrada nos penhascos de Mosphoran Highwaste.", 8800, "Phon Coast / Mosphoran", "FFXII Crossbow: Gastrophetes", Rarity.RARE);

    public static final DeferredItem<Item> ALTAIR = registerWeapon("altair", IvaliceItemCategory.WEAPON_GUN,
            5.0F, -2.6F, "Arma de fogo clássica de Balthier; seus disparos ignoram a defesa física convencional.", 600, "Equipamento Inicial de Balthier", "FFXII Gun: Altair", Rarity.COMMON);

    public static final DeferredItem<Item> FOMALHAUT = registerWeapon("fomalhaut", IvaliceItemCategory.WEAPON_GUN,
            11.0F, -2.6F, "Rifle de precisão lendário (Elemento Trovão / Thunder) cobiçado por piratas dos céus.", 19500, "Mina de Lhusu / Cerobi Steppe", "FFXII Gun: Fomalhaut", Rarity.EPIC);

    public static final DeferredItem<Item> FRANCISC = registerWeapon("francisca", IvaliceItemCategory.WEAPON_AXE_HAMMER,
            8.0F, -2.8F, "Machado de batalha de alto impacto usado pelos guerreiros de Jahara.", 3800, "Ozmone Plain / Jahara", "FFXII Axe: Francisca", Rarity.UNCOMMON);

    public static final DeferredItem<Item> SCORPION_TAIL = registerWeapon("scorpion_tail", IvaliceItemCategory.WEAPON_AXE_HAMMER,
            12.5F, -2.8F, "Martelo-escorpião supremo imbuído de relâmpagos (Thunder).", 24000, "Bazaar: The Scorpion", "FFXII Hammer: Scorpion Tail", Rarity.EPIC);

    public static final DeferredItem<Item> CHERRY_STAFF = registerWeapon("cherry_staff", IvaliceItemCategory.WEAPON_STAFF,
            4.5F, -2.3F, "Cajado entalhado em madeira de cerejeira que amplifica magias de Vento (Wind).", 1600, "Giza Plains / Rabanastre", "FFXII Staff: Cherry Staff", Rarity.UNCOMMON);

    public static final DeferredItem<Item> STAFF_OF_THE_MAGI = registerWeapon("staff_of_the_magi", IvaliceItemCategory.WEAPON_STAFF,
            9.0F, -2.2F, "Cajado arcano supremo que potencializa Gelo (Ice), Vento (Wind) e Luz Sagrada (Holy).", 18500, "Santuário de Miriam / Pharos", "FFXII Staff: Staff of the Magi", Rarity.EPIC);

    public static final DeferredItem<Item> ROD_OF_FAITH = registerWeapon("rod_of_faith", IvaliceItemCategory.WEAPON_ROD,
            6.5F, -2.2F, "Bastão sagrado concedido após derrotar Mateus; inspira Faith ao portador.", 12000, "Bur-Omisace / Esper Mateus", "FFXII Rod: Rod of Faith", Rarity.RARE);

    public static final DeferredItem<Item> EUCLIDS_SEXTANT = registerWeapon("euclids_sextant", IvaliceItemCategory.WEAPON_MEASURE,
            5.0F, -1.9F, "Instrumento de medição geométrica que concede Bubble e precisão tática em batalha.", 7200, "Jaharan / Balfonheim", "FFXII Measure: Euclid's Sextant", Rarity.RARE);

    public static final DeferredItem<Item> VOLCANO = registerWeapon("volcano", IvaliceItemCategory.WEAPON_HAND_BOMB,
            8.5F, -2.5F, "Lançador de bombas explosivas de Fogo (Fire) originário do Mar de Areia de Yensa.", 6800, "Ogir-Yensa Sandsea", "FFXII Hand-bomb: Volcano", Rarity.RARE);

    // =========================================================================
    // 2. EQUIPAMENTOS DEFENSIVOS (ARMADURAS, ESCUDOS E ACESSÓRIOS)
    // =========================================================================
    public static final DeferredItem<Item> ESCUTCHEON = registerCatalog("escutcheon", IvaliceItemCategory.DEFENSE_SHIELD,
            "Escudo leve de bronze polido utilizado pela guarda real de Dalmasca.", 250, "Rabanastre", "FFXII Shield: Escutcheon", Rarity.COMMON, true);

    public static final DeferredItem<Item> AEGIS_SHIELD = registerCatalog("aegis_shield", IvaliceItemCategory.DEFENSE_SHIELD,
            "Escudo encantado capaz de bloquear até mesmo feitiços elementais inimigos.", 6800, "Balfonheim / Tchita Uplands", "FFXII Shield: Aegis Shield", Rarity.RARE, true);

    public static final DeferredItem<Item> ZODIAC_ESCUTCHEON = registerCatalog("zodiac_escutcheon", IvaliceItemCategory.DEFENSE_SHIELD,
            "O escudo definitivo de Ivalice, oferecendo evasão física máxima e imunidade a Trovão.", 32000, "Barheim Passage / Great Crystal", "FFXII Shield: Zodiac Escutcheon", Rarity.EPIC, true);

    public static final DeferredItem<Item> LEATHER_CAP = registerArmor("leather_cap", IvaliceItemCategory.DEFENSE_LIGHT_ARMOR, ArmorType.HELMET,
            "Gorro de couro costurado com peles de lobo de Dalmasca (+HP e Defesa Leve).", 120, "Rabanastre", "FFXII Light Helm: Leather Cap", Rarity.COMMON);

    public static final DeferredItem<Item> LEATHER_CLOTHING = registerArmor("leather_clothing", IvaliceItemCategory.DEFENSE_LIGHT_ARMOR, ArmorType.CHESTPLATE,
            "Túnica leve de viagem ideal para o calor escaldante do Deserto de Dalmasca.", 150, "Rabanastre", "FFXII Light Armor: Leather Clothing", Rarity.COMMON);

    public static final DeferredItem<Item> MIRAGE_VEST = registerArmor("mirage_vest", IvaliceItemCategory.DEFENSE_LIGHT_ARMOR, ArmorType.CHESTPLATE,
            "Colete de caçador tecido com fios ilusórios de Feywood (+HP e Velocidade).", 8400, "Balfonheim / Dalan", "FFXII Light Armor: Mirage Vest", Rarity.RARE);

    public static final DeferredItem<Item> BRAVE_SUIT = registerArmor("brave_suit", IvaliceItemCategory.DEFENSE_LIGHT_ARMOR, ArmorType.CHESTPLATE,
            "Traje supremo de armadura leve que concede Bravery permanente ao usuário.", 24000, "Henne Mines", "FFXII Light Armor: Brave Suit", Rarity.EPIC);

    public static final DeferredItem<Item> IRON_HELM = registerArmor("iron_helm", IvaliceItemCategory.DEFENSE_HEAVY_ARMOR, ArmorType.HELMET,
            "Elmo pesado de ferro forjado para cavaleiros da linha de frente (+Força e Defesa).", 450, "Nalbina Fortress", "FFXII Heavy Helm: Iron Helm", Rarity.COMMON);

    public static final DeferredItem<Item> SCALE_ARMOR = registerArmor("scale_armor", IvaliceItemCategory.DEFENSE_HEAVY_ARMOR, ArmorType.CHESTPLATE,
            "Cota de escamas metálicas usada por mercenários nas Planícies de Ozmone.", 650, "Nalbina / Jahara", "FFXII Heavy Armor: Scale Armor", Rarity.UNCOMMON);

    public static final DeferredItem<Item> MAXIMILIAN = registerArmor("maximilian", IvaliceItemCategory.DEFENSE_HEAVY_ARMOR, ArmorType.CHESTPLATE,
            "Armadura pesada lendária de Nabudis que eleva drasticamente a Defesa e Força Física.", 16500, "Necrohol of Nabudis", "FFXII Heavy Armor: Maximilian", Rarity.EPIC);

    public static final DeferredItem<Item> GRAND_ARMOR = registerArmor("grand_armor", IvaliceItemCategory.DEFENSE_HEAVY_ARMOR, ArmorType.CHESTPLATE,
            "A mais imponente armadura de placas de Ivalice, usada apenas pelos Juízes Magisters.", 29000, "Necrohol of Nabudis (Helvinek)", "FFXII Heavy Armor: Grand Armor", Rarity.EPIC);

    public static final DeferredItem<Item> MAGES_HABIT = registerArmor("mages_habit", IvaliceItemCategory.DEFENSE_MYSTIC_ARMOR, ArmorType.CHESTPLATE,
            "Veste mística bordada com fios de Magicite que amplia o poder mágico.", 1100, "Bhujerba", "FFXII Mystic Armor: Mage's Habit", Rarity.UNCOMMON);

    public static final DeferredItem<Item> WHITE_ROBE = registerArmor("white_robe", IvaliceItemCategory.DEFENSE_MYSTIC_ARMOR, ArmorType.CHESTPLATE,
            "Manto imaculado de Bur-Omisace que potencializa magias Sagradas e de Cura.", 11500, "Archades / Phon Coast", "FFXII Mystic Armor: White Robe", Rarity.RARE);

    public static final DeferredItem<Item> LORDLY_ROBE = registerArmor("lordly_robe", IvaliceItemCategory.DEFENSE_MYSTIC_ARMOR, ArmorType.CHESTPLATE,
            "Manto místico supremo dos altos arcanistas de Ivalice.", 25000, "Pharos at Ridorana", "FFXII Mystic Armor: Lordly Robe", Rarity.EPIC);

    public static final DeferredItem<Item> GOLDEN_AMULET = registerCatalog("golden_amulet", IvaliceItemCategory.DEFENSE_ACCESSORY,
            "Amuleto dourado que dobra os Pontos de Licença (LP x2) recebidos em combate!", 4500, "Mosphoran Highwaste / Recompensa de Caçada", "FFXII Accessory: Golden Amulet", Rarity.RARE, true);

    public static final DeferredItem<Item> RIBBON = registerCatalog("ribbon", IvaliceItemCategory.DEFENSE_ACCESSORY,
            "Laço encantado lendário que protege o portador contra todos os efeitos negativos de status.", 25000, "Cerobi Steppe / Pharos Subterra", "FFXII Accessory: Ribbon", Rarity.EPIC, true);

    public static final DeferredItem<Item> GENJI_GLOVES = registerCatalog("genji_gloves", IvaliceItemCategory.DEFENSE_ACCESSORY,
            "Manoplas orientais de Gilgamesh que aumentam drasticamente a chance de múltiplos ataques consecutivos.", 30000, "Roubo da Caçada Gilgamesh / Phon Coast", "FFXII Accessory: Genji Gloves", Rarity.EPIC, true);

    public static final DeferredItem<Item> HERMES_SANDALS = registerArmor("hermes_sandals", IvaliceItemCategory.DEFENSE_ACCESSORY, ArmorType.BOOTS,
            "Botas aladas que concedem velocidade de movimento superior pelos campos de Ivalice.", 15000, "Barheim Passage / Bazaar", "FFXII Accessory: Hermes Sandals", Rarity.RARE);

    // =========================================================================
    // 3. CONSUMÍVEIS FUNCIONAIS (CONSUMABLES)
    // =========================================================================
    public static final DeferredItem<Item> POTION = registerConsumable("potion",
            "Tônico medicinal básico de Rabanastre. Restaura 6 pontos de vida (3 corações) instantaneamente.",
            70, "Lojas de Rabanastre / Roubo de Lobos", "FFXII Consumable: Potion", Rarity.COMMON,
            6.0F, false, List.of());

    public static final DeferredItem<Item> HI_POTION = registerConsumable("hi_potion",
            "Poção concentrada de alta pureza. Restaura 12 pontos de vida (6 corações) e concede Regeneração curta.",
            210, "Bazaar: Flask of Oily Liquid / Lojas", "FFXII Consumable: Hi-Potion", Rarity.UNCOMMON,
            12.0F, false, List.of(new MobEffectInstance(MobEffects.REGENERATION, 100, 0)));

    public static final DeferredItem<Item> X_POTION = registerConsumable("x_potion",
            "Poção máxima da Ordem de Ambrósia. Restaura 24 pontos de vida e fortalece a resistência.",
            680, "Balfonheim / Archades", "FFXII Consumable: X-Potion", Rarity.RARE,
            24.0F, false, List.of(new MobEffectInstance(MobEffects.REGENERATION, 160, 1)));

    public static final DeferredItem<Item> ETHER = registerConsumable("ether",
            "Essência de Mist engarrafada. Restaura energia arcana e concede Velocidade e Visão Noturna.",
            500, "Baús de Ivalice / Bazaar", "FFXII Consumable: Ether", Rarity.UNCOMMON,
            4.0F, false, List.of(new MobEffectInstance(MobEffects.SPEED, 300, 0), new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0)));

    public static final DeferredItem<Item> ELIXIR = registerConsumable("elixir",
            "Relíquia alquímica suprema. Restaura toda a vida, remove status negativos e concede Absorção dourada!",
            5000, "Grande Cristal / Caçadas Rank S", "FFXII Consumable: Elixir", Rarity.EPIC,
            40.0F, true, List.of(new MobEffectInstance(MobEffects.ABSORPTION, 1200, 2), new MobEffectInstance(MobEffects.REGENERATION, 300, 1)));

    public static final DeferredItem<Item> PHOENIX_DOWN = registerConsumable("phoenix_down",
            "Pluma mística da Fênix. Restaura 16 HP e concede Resistência ao Fogo e Absorção protetora.",
            250, "Bazaar: Essence of the Phoenix / Lojas", "FFXII Consumable: Phoenix Down", Rarity.RARE,
            16.0F, true, List.of(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0), new MobEffectInstance(MobEffects.ABSORPTION, 600, 1)));

    public static final DeferredItem<Item> ANTIDOTE = registerConsumable("antidote",
            "Antídoto herbal que purifica imediatamente Veneno e Decomposição (Wither).",
            50, "Lojas de Dalmasca", "FFXII Consumable: Antidote", Rarity.COMMON,
            2.0F, true, List.of());

    public static final DeferredItem<Item> REMEDY = registerConsumable("remedy",
            "Panaceia universal que cura todos os status negativos e concede Regeneração.",
            400, "Lojas do Clã Centurio", "FFXII Consumable: Remedy", Rarity.UNCOMMON,
            6.0F, true, List.of(new MobEffectInstance(MobEffects.REGENERATION, 140, 0)));

    public static final DeferredItem<Item> GYSAHL_GREENS = registerConsumable("gysahl_greens",
            "Verdura aromática colhida em Giza e Ozmone; favorita dos Chocobos e revigorante para viajantes (+Velocidade).",
            90, "Colheita de Gysahl Greens Selvagem", "FFXII Consumable: Gysahl Greens", Rarity.COMMON,
            4.0F, false, List.of(new MobEffectInstance(MobEffects.SPEED, 400, 1)));

    // =========================================================================
    // 4. ESPÓLIOS E MATERIAIS DE MONSTROS (LOOT / BAZAAR GOODS)
    // =========================================================================
    public static final DeferredItem<Item> WOLF_PELT = registerCatalog("wolf_pelt", IvaliceItemCategory.LOOT_MATERIAL,
            "Pelagem áspera de lobo do deserto. Usada no Bazaar para criar o Pacote de Poções.", 48, "Drop/Roubo de Dalmasca Wolf e Thextera", "FFXII Loot: Wolf Pelt", Rarity.COMMON, false);

    public static final DeferredItem<Item> CACTUS_FRUIT = registerCatalog("cactus_fruit", IvaliceItemCategory.LOOT_MATERIAL,
            "Fruto suculento colhido de Cactoids no Deserto de Dalmasca.", 62, "Drop/Roubo de Cactoid", "FFXII Loot: Cactus Fruit", Rarity.COMMON, false);

    public static final DeferredItem<Item> GIZA_RABBIT_FUR = registerCatalog("giza_rabbit_fur", IvaliceItemCategory.LOOT_MATERIAL,
            "Lã macia e alva de Giza Rabbit, valorizada por alfaiates de Rabanastre.", 55, "Drop de Giza Rabbit", "FFXII Loot: Giza Rabbit Fur", Rarity.COMMON, false);

    public static final DeferredItem<Item> SMALL_FEATHER = registerCatalog("small_feather", IvaliceItemCategory.LOOT_MATERIAL,
            "Pluma leve deixada por Cockatrices. Ingrediente para 'Essence of the Phoenix' no Bazaar.", 75, "Drop de Cockatrice", "FFXII Loot: Small Feather", Rarity.COMMON, false);

    public static final DeferredItem<Item> FISH_SCALE = registerCatalog("fish_scale", IvaliceItemCategory.LOOT_MATERIAL,
            "Escama azul iridescente de Ichthon do Rio Nebra e Phon Coast.", 68, "Drop de Ichthon", "FFXII Loot: Fish Scale", Rarity.COMMON, false);

    public static final DeferredItem<Item> MALBORO_VINE = registerCatalog("malboro_vine", IvaliceItemCategory.LOOT_MATERIAL,
            "Vinha fibrosa colhida nas profundezas da Selva de Golmore. Essencial para forjar a Masamune!", 420, "Golmore Jungle / Feywood", "FFXII Loot: Malboro Vine", Rarity.UNCOMMON, false);

    public static final DeferredItem<Item> FIRE_STONE = registerCatalog("fire_stone", IvaliceItemCategory.LOOT_MATERIAL,
            "Cristal imbuído com Mist elemental de Fogo.", 120, "Drop em Dalmasca e Ogir-Yensa", "FFXII Loot: Fire Stone", Rarity.COMMON, false);

    public static final DeferredItem<Item> WATER_STONE = registerCatalog("water_stone", IvaliceItemCategory.LOOT_MATERIAL,
            "Cristal imbuído com Mist elemental de Água.", 120, "Drop de Ichthon em Giza (Rains) e Phon Coast", "FFXII Loot: Water Stone", Rarity.COMMON, false);

    public static final DeferredItem<Item> WIND_STONE = registerCatalog("wind_stone", IvaliceItemCategory.LOOT_MATERIAL,
            "Cristal imbuído com Mist elemental de Vento.", 120, "Drop de Cockatrice em Ozmone e Cerobi", "FFXII Loot: Wind Stone", Rarity.COMMON, false);

    public static final DeferredItem<Item> EARTH_STONE = registerCatalog("earth_stone", IvaliceItemCategory.LOOT_MATERIAL,
            "Cristal imbuído com Mist elemental de Terra.", 120, "Drop de Cactoid no Estersand", "FFXII Loot: Earth Stone", Rarity.COMMON, false);

    public static final DeferredItem<Item> DARK_MAGICITE = registerCatalog("dark_magicite", IvaliceItemCategory.LOOT_MATERIAL,
            "Magicite carregada de energia negra, necessária para armas de elite no Bazaar.", 380, "Nabreus Deadlands / White Mousse", "FFXII Loot: Dark Magicite", Rarity.UNCOMMON, false);

    public static final DeferredItem<Item> GEMSTEEL = registerCatalog("gemsteel", IvaliceItemCategory.LOOT_MATERIAL,
            "Liga metálica escarlate lendária. Um dos 3 componentes da espada suprema Tournesol!", 3000, "Esper Belias / Bazaar: Scarletite + Damascus", "FFXII Loot: Gemsteel", Rarity.EPIC, false);

    public static final DeferredItem<Item> EMPYREAL_SOUL = registerCatalog("empyreal_soul", IvaliceItemCategory.LOOT_MATERIAL,
            "Joia espiritual que abriga a alma dos antigos. Componente da espada Tournesol!", 3000, "Roubo de Belias / Bazaar: Soul of Thamasa", "FFXII Loot: Empyreal Soul", Rarity.EPIC, false);

    public static final DeferredItem<Item> SERPENTARIUS = registerCatalog("serpentarius", IvaliceItemCategory.LOOT_MATERIAL,
            "Gema zodiacal da 13ª constelação (Ophiuchus). Componente da espada Tournesol!", 3000, "Esper Mateus / Bazaar: Snake Skin + Serpent Eye", "FFXII Loot: Serpentarius", Rarity.EPIC, false);

    // =========================================================================
    // 5. ITENS-CHAVE DE CAÇADAS E PROGRESSÃO (KEY ITEMS)
    // =========================================================================
    public static final DeferredItem<Item> SHADESTONE = registerCatalog("shadestone", IvaliceItemCategory.KEY_ITEM,
            "Pedra escura de Giza Plains que absorve energia solar nos Sunstone Outcrops para se tornar uma Sunstone.", 150, "Ancião Brunoa (Giza Plains)", "FFXII Key Item: Shadestone", Rarity.UNCOMMON, false);

    public static final DeferredItem<Item> SUNSTONE = registerCatalog("sunstone", IvaliceItemCategory.KEY_ITEM,
            "Cristal solar incandescente carregado nos afloramentos de Giza Plains (Estação Seca).", 800, "Mineração de Sunstone Ore em Giza Plains", "FFXII Key Item: Sunstone", Rarity.RARE, false);

    public static final DeferredItem<Item> GODDESS_MAGICITE = registerCatalog("goddess_magicite", IvaliceItemCategory.KEY_ITEM,
            "Tesouro real do Palácio de Rabanastre, um dos três fragmentos de Nethicite de Raithwall.", 2500, "Palácio Real de Rabanastre", "FFXII Key Item: Goddess's Magicite", Rarity.EPIC, false);

    public static final DeferredItem<Item> DAWN_SHARD = registerCatalog("dawn_shard", IvaliceItemCategory.KEY_ITEM,
            "O Fragmento da Aurora de Deifacted Nethicite guardado por Belias na Tumba de Raithwall.", 5000, "Tumba de Raithwall", "FFXII Key Item: Dawn Shard", Rarity.EPIC, false);

    public static final DeferredItem<Item> GALBANA_LILIES = registerCatalog("galbana_lilies", IvaliceItemCategory.KEY_ITEM,
            "Flores vermelhas colhidas após derrotar o Rogue Tomato (Primeira Caçada do Clã Centurio).", 500, "Recompensa da Caçada Rogue Tomato", "FFXII Hunt Trophy: Galbana Lilies", Rarity.RARE, false);

    public static final DeferredItem<Item> CACTUS_BLOOM = registerCatalog("cactus_bloom", IvaliceItemCategory.KEY_ITEM,
            "Flor escarlate rara obtida do Flowering Cactoid para a esposa de Dantro no Estersand.", 750, "Drop da Caçada Flowering Cactoid", "FFXII Hunt Trophy: Cactus Bloom", Rarity.RARE, false);

    public static final DeferredItem<Item> WYRMFIRE_SHOT = registerCatalog("wyrmfire_shot", IvaliceItemCategory.KEY_ITEM,
            "Munição incendiária especial (Elemento Fogo) entregue por Dantro como recompensa do Flowering Cactoid.", 1800, "Recompensa da Caçada Flowering Cactoid", "FFXII Ammo / Reward: Wyrmfire Shot", Rarity.RARE, false);

    public static final DeferredItem<Item> BROKEN_KEY = registerCatalog("broken_key", IvaliceItemCategory.KEY_ITEM,
            "Chave da eclusa de Garamsythe recuperada do interior da Mark Rank V White Mousse.", 2200, "Drop da Caçada White Mousse", "FFXII Hunt Trophy: Broken Key", Rarity.RARE, false);

    // =========================================================================
    // 6. MAGICKS E TECHNICKS (GRIMÓRIOS ATIVOS)
    // =========================================================================
    public static final DeferredItem<Item> MAGICK_CURE = registerSpell("magick_cure", IvaliceItemCategory.MAGICK_WHITE,
            "White Magick 1: Restaura 8 HP e purifica ferimentos leves.", 200, "Loja de Magias de Rabanastre", "FFXII White Magick: Cure", Rarity.COMMON, IvaliceSpellItem.SpellAction.CURE, 80);

    public static final DeferredItem<Item> MAGICK_CURA = registerSpell("magick_cura", IvaliceItemCategory.MAGICK_WHITE,
            "White Magick 4: Restaura 16 HP em área e concede Regeneração e Absorção!", 1500, "Bur-Omisace / Jahara", "FFXII White Magick: Cura", Rarity.RARE, IvaliceSpellItem.SpellAction.CURA, 120);

    public static final DeferredItem<Item> MAGICK_FIRE = registerSpell("magick_fire", IvaliceItemCategory.MAGICK_BLACK,
            "Black Magick 1: Dispara chamas contra inimigos próximos (2x Dano em Lobos, Giza Rabbit e White Mousse!).", 200, "Loja de Magias de Rabanastre", "FFXII Black Magick: Fire", Rarity.COMMON, IvaliceSpellItem.SpellAction.FIRE, 70);

    public static final DeferredItem<Item> MAGICK_THUNDER = registerSpell("magick_thunder", IvaliceItemCategory.MAGICK_BLACK,
            "Black Magick 1: Invoca relâmpagos em área (2x Dano em Ichthon e Mateus!).", 200, "Loja de Magias de Rabanastre", "FFXII Black Magick: Thunder", Rarity.COMMON, IvaliceSpellItem.SpellAction.THUNDER, 80);

    public static final DeferredItem<Item> MAGICK_BLIZZARD = registerSpell("magick_blizzard", IvaliceItemCategory.MAGICK_BLACK,
            "Black Magick 1: Congela inimigos próximos com cristais de gelo e lentidão.", 200, "Loja de Magias de Rabanastre", "FFXII Black Magick: Blizzard", Rarity.COMMON, IvaliceSpellItem.SpellAction.BLIZZARD, 70);

    public static final DeferredItem<Item> MAGICK_HASTEGA = registerSpell("magick_hastega", IvaliceItemCategory.MAGICK_TIME_ARCANE,
            "Time Magick 7: Acelera drasticamente a velocidade de ataque e movimento (Haste II).", 4800, "Loja de Magias de Balfonheim", "FFXII Time Magick: Hastega", Rarity.EPIC, IvaliceSpellItem.SpellAction.HASTEGA, 200);

    public static final DeferredItem<Item> TECHNICK_LIBRA = registerSpell("technick_libra", IvaliceItemCategory.TECHNICK,
            "Technick Tática: Analisa a criatura mais próxima revelando Nível, HP, Fraqueza Elemental e Espólio.", 500, "Mercado de Rabanastre", "FFXII Technick: Libra", Rarity.UNCOMMON, IvaliceSpellItem.SpellAction.LIBRA, 40);

    public static final DeferredItem<Item> TECHNICK_STEAL = registerSpell("technick_steal", IvaliceItemCategory.TECHNICK,
            "Technick de Vaan e Balthier: Rouba diretamente um espólio da criatura de Ivalice mais próxima!", 600, "Habilidade Inicial de Vaan", "FFXII Technick: Steal", Rarity.UNCOMMON, IvaliceSpellItem.SpellAction.STEAL, 60);

    // =========================================================================
    // 7. OVOS DE INVOCAÇÃO DE CRIATURAS, MARKS E ESPERS (12 SPAWN EGGS)
    // =========================================================================
    public static final DeferredItem<Item> WOLF_SPAWN_EGG = registerSpawnEgg("wolf_spawn_egg", IvaliceEntities.WOLF);
    public static final DeferredItem<Item> CACTOID_SPAWN_EGG = registerSpawnEgg("cactoid_spawn_egg", IvaliceEntities.CACTOID);
    public static final DeferredItem<Item> HYENA_SPAWN_EGG = registerSpawnEgg("hyena_spawn_egg", IvaliceEntities.HYENA);
    public static final DeferredItem<Item> GIZA_RABBIT_SPAWN_EGG = registerSpawnEgg("giza_rabbit_spawn_egg", IvaliceEntities.GIZA_RABBIT);
    public static final DeferredItem<Item> COCKATRICE_SPAWN_EGG = registerSpawnEgg("cockatrice_spawn_egg", IvaliceEntities.COCKATRICE);
    public static final DeferredItem<Item> ICHTHON_SPAWN_EGG = registerSpawnEgg("ichthon_spawn_egg", IvaliceEntities.ICHTHON);
    public static final DeferredItem<Item> ROGUE_TOMATO_SPAWN_EGG = registerSpawnEgg("rogue_tomato_spawn_egg", IvaliceEntities.ROGUE_TOMATO);
    public static final DeferredItem<Item> THEXTERA_SPAWN_EGG = registerSpawnEgg("thextera_spawn_egg", IvaliceEntities.THEXTERA);
    public static final DeferredItem<Item> FLOWERING_CACTOID_SPAWN_EGG = registerSpawnEgg("flowering_cactoid_spawn_egg", IvaliceEntities.FLOWERING_CACTOID);
    public static final DeferredItem<Item> WHITE_MOUSSE_SPAWN_EGG = registerSpawnEgg("white_mousse_spawn_egg", IvaliceEntities.WHITE_MOUSSE);
    public static final DeferredItem<Item> BELIAS_SPAWN_EGG = registerSpawnEgg("belias_spawn_egg", IvaliceEntities.BELIAS);
    public static final DeferredItem<Item> MATEUS_SPAWN_EGG = registerSpawnEgg("mateus_spawn_egg", IvaliceEntities.MATEUS);

    // =========================================================================
    // MÉTODOS AUXILIARES DE REGISTRO EM LOTE
    // =========================================================================
    private static DeferredItem<Item> registerGuide(
            String id, IvaliceItemCategory category, String desc, int gil, String source, String ref, Rarity rarity, int tab, String esper
    ) {
        DeferredItem<Item> item = ITEMS.registerItem(id, props -> new IvaliceGuideItem(
                props.rarity(rarity).stacksTo(1), category, desc, gil, source, ref, rarity, tab, esper
        ));
        LOOT_AND_CONSUMABLES_TAB_ITEMS.add(item);
        BY_ID.put(id, item);
        return item;
    }

    private static DeferredItem<Item> registerWeapon(
            String id, IvaliceItemCategory category, float attackBonus, float attackSpeed,
            String desc, int gil, String source, String ref, Rarity rarity
    ) {
        DeferredItem<Item> item = ITEMS.registerItem(id, props -> new IvaliceCatalogItem(
                props.sword(ToolMaterial.DIAMOND, attackBonus, attackSpeed).rarity(rarity).stacksTo(1),
                category, desc, gil, source, ref, rarity
        ));
        EQUIPMENT_TAB_ITEMS.add(item);
        BY_ID.put(id, item);
        return item;
    }

    private static DeferredItem<Item> registerArmor(
            String id, IvaliceItemCategory category, ArmorType armorType,
            String desc, int gil, String source, String ref, Rarity rarity
    ) {
        DeferredItem<Item> item = ITEMS.registerItem(id, props -> new IvaliceCatalogItem(
                props.humanoidArmor(ArmorMaterials.DIAMOND, armorType).rarity(rarity).stacksTo(1),
                category, desc, gil, source, ref, rarity
        ));
        EQUIPMENT_TAB_ITEMS.add(item);
        BY_ID.put(id, item);
        return item;
    }

    private static DeferredItem<Item> registerCatalog(
            String id, IvaliceItemCategory category, String desc, int gil, String source, String ref, Rarity rarity, boolean equipmentTab
    ) {
        int stackSize = equipmentTab ? 1 : 64;
        DeferredItem<Item> item = ITEMS.registerItem(id, props -> new IvaliceCatalogItem(
                props.rarity(rarity).stacksTo(stackSize), category, desc, gil, source, ref, rarity
        ));
        if (equipmentTab) {
            EQUIPMENT_TAB_ITEMS.add(item);
        } else {
            LOOT_AND_CONSUMABLES_TAB_ITEMS.add(item);
        }
        BY_ID.put(id, item);
        return item;
    }

    private static DeferredItem<Item> registerConsumable(
            String id, String desc, int gil, String source, String ref, Rarity rarity,
            float healAmount, boolean curesNegative, List<MobEffectInstance> effects
    ) {
        DeferredItem<Item> item = ITEMS.registerItem(id, props -> new IvaliceConsumableItem(
                props.rarity(rarity).stacksTo(64), desc, gil, source, ref, rarity, healAmount, curesNegative, effects
        ));
        LOOT_AND_CONSUMABLES_TAB_ITEMS.add(item);
        BY_ID.put(id, item);
        return item;
    }

    private static DeferredItem<Item> registerSpell(
            String id, IvaliceItemCategory category, String desc, int gil, String source, String ref, Rarity rarity,
            IvaliceSpellItem.SpellAction action, int cooldownTicks
    ) {
        DeferredItem<Item> item = ITEMS.registerItem(id, props -> new IvaliceSpellItem(
                props.rarity(rarity).stacksTo(1), category, desc, gil, source, ref, rarity, action, cooldownTicks
        ));
        EQUIPMENT_TAB_ITEMS.add(item);
        BY_ID.put(id, item);
        return item;
    }

    @SuppressWarnings("unchecked")
    private static DeferredItem<Item> registerSpawnEgg(String id, net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.entity.EntityType<?>, ?> entityHolder) {
        DeferredItem<Item> item = ITEMS.registerSimpleItem(id, props -> props.spawnEgg((net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>) entityHolder.get()));
        LOOT_AND_CONSUMABLES_TAB_ITEMS.add(item);
        BY_ID.put(id, item);
        return item;
    }

    public static List<DeferredItem<? extends Item>> getEquipmentTabItems() {
        return Collections.unmodifiableList(EQUIPMENT_TAB_ITEMS);
    }

    public static List<DeferredItem<? extends Item>> getLootAndConsumablesTabItems() {
        return Collections.unmodifiableList(LOOT_AND_CONSUMABLES_TAB_ITEMS);
    }

    public static Item getItemById(String id) {
        DeferredItem<? extends Item> holder = BY_ID.get(id);
        return holder != null ? holder.get() : null;
    }

    public static Map<String, DeferredItem<? extends Item>> getAllRegisteredItems() {
        return Collections.unmodifiableMap(BY_ID);
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
