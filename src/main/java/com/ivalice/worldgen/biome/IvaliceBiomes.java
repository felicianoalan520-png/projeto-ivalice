package com.ivalice.worldgen.biome;

import com.ivalice.IvaliceMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

import java.util.List;

/**
 * Chaves de registro (ResourceKeys) e Tags das 14 regiões naturais canônicas de Final Fantasy XII: The Zodiac Age.
 * Esses identificadores são utilizados na geração de mundo via TerraBlender, nas regras de superfície (MaterialRules)
 * e futuramente no surgimento condicional de criaturas (Etapa 3) e caçadas do Clã Centurio (Etapa 4).
 */
public final class IvaliceBiomes {
    private IvaliceBiomes() {}

    // 1. Dalmasca Estersand — Desertos rochosos, ravinas áridas e margens do Rio Nebra
    public static final ResourceKey<Biome> DALMASCA_ESTERSAND = createKey("dalmasca_estersand");

    // 2. Dalmasca Westersand — Grandes dunas varridas pelo vento e tempestades de areia
    public static final ResourceKey<Biome> DALMASCA_WESTERSAND = createKey("dalmasca_westersand");

    // 3. Giza Plains (Estação Seca / The Dry) — Planícies douradas ensolaradas e afloramentos de Sunstone
    public static final ResourceKey<Biome> GIZA_PLAINS_DRY = createKey("giza_plains_dry");

    // 4. Giza Plains (Estação das Chuvas / The Rains) — Planícies alagadas sob chuva intensa contínua
    public static final ResourceKey<Biome> GIZA_PLAINS_RAINS = createKey("giza_plains_rains");

    // 5. Ozmone Plain — Campos amplos de savana verde-esmeralda entre Jahara e Golmore
    public static final ResourceKey<Biome> OZMONE_PLAIN = createKey("ozmone_plain");

    // 6. Golmore Jungle — Selva densa com copas escuras, caminhos sombreados e névoa mística
    public static final ResourceKey<Biome> GOLMORE_JUNGLE = createKey("golmore_jungle");

    // 7. Phon Coast — Costa tropical luminosa de águas turquesa, praias brancas e falésias costeiras
    public static final ResourceKey<Biome> PHON_COAST = createKey("phon_coast");

    // 8. Tchita Uplands — Planaltos elevados varridos pelo vento a caminho de Archades
    public static final ResourceKey<Biome> TCHITA_UPLANDS = createKey("tchita_uplands");

    // 9. Paramina Rift — Desfiladeiros congelados, nevascas perpétuas e geleiras ao redor de Mt. Bur-Omisace
    public static final ResourceKey<Biome> PARAMINA_RIFT = createKey("paramina_rift");

    // 10. Salikawood — Floresta antiga de vegetação vibrante e copa dourada/verdejante
    public static final ResourceKey<Biome> SALIKAWOOD = createKey("salikawood");

    // 11. Feywood — Floresta branca encantada coberta por densa névoa de Mist ao sul de Golmore
    public static final ResourceKey<Biome> FEYWOOD = createKey("feywood");

    // 12. Mosphoran Highwaste — Desfiladeiros rochosos, gêiseres de vapor e penhascos montanhosos
    public static final ResourceKey<Biome> MOSPHORAN_HIGHWASTE = createKey("mosphoran_highwaste");

    // 13. Nabreus Deadlands — Pântanos sombrios, águas turvas e neblina fúnebre sobre a antiga Nabudis
    public static final ResourceKey<Biome> NABREUS_DEADLANDS = createKey("nabreus_deadlands");

    // 14. Ogir-Yensa Sandsea — Imenso mar de areia fina alaranjada na fronteira ocidental de Dalmasca
    public static final ResourceKey<Biome> OGIR_YENSA_SANDSEA = createKey("ogir_yensa_sandsea");

    // 15. Cerobi Steppe — Terraços rochosos elevados e moinhos de vento entre Balfonheim e Tchita
    public static final ResourceKey<Biome> CEROBI_STEPPE = createKey("cerobi_steppe");

    // Tags para uso em spawns, estruturas e caçadas
    public static final TagKey<Biome> IS_IVALICE_BIOME = TagKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(IvaliceMod.MODID, "is_ivalice")
    );
    public static final TagKey<Biome> IS_DALMASCA_DESERT = TagKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(IvaliceMod.MODID, "is_dalmasca_desert")
    );
    public static final TagKey<Biome> IS_GIZA_PLAINS = TagKey.create(
            Registries.BIOME, Identifier.fromNamespaceAndPath(IvaliceMod.MODID, "is_giza_plains")
    );

    public static final List<ResourceKey<Biome>> ALL_BIOMES = List.of(
            DALMASCA_ESTERSAND,
            DALMASCA_WESTERSAND,
            GIZA_PLAINS_DRY,
            GIZA_PLAINS_RAINS,
            OZMONE_PLAIN,
            GOLMORE_JUNGLE,
            PHON_COAST,
            TCHITA_UPLANDS,
            PARAMINA_RIFT,
            SALIKAWOOD,
            FEYWOOD,
            MOSPHORAN_HIGHWASTE,
            NABREUS_DEADLANDS,
            OGIR_YENSA_SANDSEA,
            CEROBI_STEPPE
    );

    private static ResourceKey<Biome> createKey(String name) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(IvaliceMod.MODID, name));
    }
}
