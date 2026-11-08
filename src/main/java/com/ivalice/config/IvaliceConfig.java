package com.ivalice.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Sistema de configurações do Projeto Ivalice.
 * Controla a geração de biomas, pesos de distribuição via TerraBlender e o sistema sazonal de Giza Plains.
 */
public class IvaliceConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_OVERWORLD_BIOMES;
    public static final ModConfigSpec.IntValue OVERWORLD_REGION_WEIGHT;
    public static final ModConfigSpec.BooleanValue ENABLE_GIZA_SEASONAL_CYCLE;
    public static final ModConfigSpec.IntValue GIZA_DRY_SEASON_DAYS;
    public static final ModConfigSpec.IntValue GIZA_RAINS_SEASON_DAYS;

    static {
        BUILDER.push("worldgen");

        ENABLE_OVERWORLD_BIOMES = BUILDER
                .comment("Habilita a geração natural das regiões de Ivalice (Final Fantasy XII) no Overworld via TerraBlender.")
                .define("enableOverworldBiomes", true);

        OVERWORLD_REGION_WEIGHT = BUILDER
                .comment("Peso da região de Ivalice no TerraBlender (valores maiores tornam os biomas de FFXII mais frequentes no mundo). Padrão: 25.")
                .defineInRange("overworldRegionWeight", 25, 1, 100);

        BUILDER.pop();

        BUILDER.push("seasons");

        ENABLE_GIZA_SEASONAL_CYCLE = BUILDER
                .comment("Habilita o ciclo climático sazonal de Giza Plains (alternância entre Estação Seca / The Dry e Estação das Chuvas / The Rains).")
                .define("enableGizaSeasonalCycle", true);

        GIZA_DRY_SEASON_DAYS = BUILDER
                .comment("Duração da Estação Seca (Giza Plains - The Dry) em dias do Minecraft.")
                .defineInRange("gizaDrySeasonDays", 4, 1, 30);

        GIZA_RAINS_SEASON_DAYS = BUILDER
                .comment("Duração da Estação das Chuvas (Giza Plains - The Rains) em dias do Minecraft.")
                .defineInRange("gizaRainsSeasonDays", 2, 1, 30);

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
}
