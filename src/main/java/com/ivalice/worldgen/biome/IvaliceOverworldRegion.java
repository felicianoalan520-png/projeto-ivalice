package com.ivalice.worldgen.biome;

import com.ivalice.IvaliceMod;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.ParameterUtils;
import terrablender.api.ParameterUtils.Continentalness;
import terrablender.api.ParameterUtils.Depth;
import terrablender.api.ParameterUtils.Erosion;
import terrablender.api.ParameterUtils.Humidity;
import terrablender.api.ParameterUtils.Temperature;
import terrablender.api.ParameterUtils.Weirdness;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * Região Overworld do TerraBlender para o Projeto Ivalice.
 * Organiza os 15 biomas canônicos de Final Fantasy XII em uma matriz climática coerente:
 * - Quente/Árido: Dalmasca Estersand, Dalmasca Westersand, Ogir-Yensa Sandsea
 * - Quente/Moderado: Giza Plains (Dry), Giza Plains (Rains), Ozmone Plain
 * - Quente/Costeiro: Phon Coast
 * - Quente/Úmido: Golmore Jungle, Salikawood
 * - Temperado/Elevado: Tchita Uplands, Cerobi Steppe, Mosphoran Highwaste
 * - Frio/Úmido e Sombrio: Feywood, Nabreus Deadlands
 * - Congelado/Montanhoso: Paramina Rift
 */
public class IvaliceOverworldRegion extends Region {

    public IvaliceOverworldRegion(Identifier name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        // 1. Dalmasca Estersand — Deserto rochoso próximo a rios e costas interiores
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.HOT)
                .humidity(Humidity.ARID, Humidity.DRY)
                .continentalness(Continentalness.NEAR_INLAND, Continentalness.MID_INLAND)
                .erosion(Erosion.EROSION_2, Erosion.EROSION_3, Erosion.EROSION_4)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.MID_SLICE_NORMAL_ASCENDING, Weirdness.MID_SLICE_NORMAL_DESCENDING, Weirdness.LOW_SLICE_NORMAL_DESCENDING, Weirdness.VALLEY)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.DALMASCA_ESTERSAND));

        // 2. Dalmasca Westersand — Dunas áridas e ventosas em áreas mais interiores e elevadas
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.HOT)
                .humidity(Humidity.ARID)
                .continentalness(Continentalness.MID_INLAND, Continentalness.FAR_INLAND)
                .erosion(Erosion.EROSION_1, Erosion.EROSION_2, Erosion.EROSION_3)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.HIGH_SLICE_NORMAL_ASCENDING, Weirdness.PEAK_NORMAL, Weirdness.HIGH_SLICE_NORMAL_DESCENDING)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.DALMASCA_WESTERSAND));

        // 3. Ogir-Yensa Sandsea — Grandes extensões de areia fina alaranjada no interior extremo
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.HOT)
                .humidity(Humidity.ARID, Humidity.DRY)
                .continentalness(Continentalness.FAR_INLAND)
                .erosion(Erosion.EROSION_3, Erosion.EROSION_4, Erosion.EROSION_5)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.MID_SLICE_VARIANT_ASCENDING, Weirdness.HIGH_SLICE_VARIANT_ASCENDING, Weirdness.PEAK_VARIANT, Weirdness.HIGH_SLICE_VARIANT_DESCENDING, Weirdness.MID_SLICE_VARIANT_DESCENDING)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.OGIR_YENSA_SANDSEA));

        // 4. Giza Plains (The Dry) — Planícies douradas adjacentes a Dalmasca
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.WARM, Temperature.HOT)
                .humidity(Humidity.NEUTRAL)
                .continentalness(Continentalness.NEAR_INLAND, Continentalness.MID_INLAND)
                .erosion(Erosion.EROSION_3, Erosion.EROSION_4, Erosion.EROSION_5)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.MID_SLICE_NORMAL_ASCENDING, Weirdness.MID_SLICE_NORMAL_DESCENDING, Weirdness.LOW_SLICE_NORMAL_DESCENDING)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.GIZA_PLAINS_DRY));

        // 5. Giza Plains (The Rains) — Bacias baixas e úmidas de Giza Plains
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.WARM)
                .humidity(Humidity.WET)
                .continentalness(Continentalness.NEAR_INLAND, Continentalness.MID_INLAND)
                .erosion(Erosion.EROSION_4, Erosion.EROSION_5, Erosion.EROSION_6)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.VALLEY, Weirdness.LOW_SLICE_VARIANT_ASCENDING, Weirdness.LOW_SLICE_NORMAL_DESCENDING)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.GIZA_PLAINS_RAINS));

        // 6. Ozmone Plain — Campos amplos e savana verdejante
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.WARM)
                .humidity(Humidity.DRY, Humidity.NEUTRAL)
                .continentalness(Continentalness.MID_INLAND, Continentalness.FAR_INLAND)
                .erosion(Erosion.EROSION_3, Erosion.EROSION_4)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.HIGH_SLICE_NORMAL_ASCENDING, Weirdness.MID_SLICE_VARIANT_ASCENDING, Weirdness.MID_SLICE_VARIANT_DESCENDING)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.OZMONE_PLAIN));

        // 7. Phon Coast — Costa tropical luminosa, praias e falésias costeiras
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.WARM, Temperature.HOT, Temperature.NEUTRAL)
                .humidity(Humidity.FULL_RANGE)
                .continentalness(Continentalness.COAST, Continentalness.NEAR_INLAND)
                .erosion(Erosion.EROSION_3, Erosion.EROSION_4, Erosion.EROSION_5, Erosion.EROSION_6)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.FULL_RANGE)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.PHON_COAST));

        // Oceanos e ilhas também usam Phon Coast, sem disputar os climas do interior.
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.FULL_RANGE)
                .humidity(Humidity.FULL_RANGE)
                .continentalness(Continentalness.MUSHROOM_FIELDS, Continentalness.DEEP_OCEAN, Continentalness.OCEAN)
                .erosion(Erosion.FULL_RANGE)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.FULL_RANGE)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.PHON_COAST));

        // 8. Golmore Jungle — Selva densa e sombreada em regiões quentes e úmidas
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.WARM, Temperature.HOT)
                .humidity(Humidity.WET, Humidity.HUMID)
                .continentalness(Continentalness.MID_INLAND, Continentalness.FAR_INLAND)
                .erosion(Erosion.EROSION_1, Erosion.EROSION_2, Erosion.EROSION_3)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.MID_SLICE_NORMAL_ASCENDING, Weirdness.HIGH_SLICE_NORMAL_ASCENDING, Weirdness.PEAK_NORMAL, Weirdness.HIGH_SLICE_NORMAL_DESCENDING)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.GOLMORE_JUNGLE));

        // 9. Salikawood — Floresta exuberante de clima ameno/quente
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.NEUTRAL, Temperature.WARM)
                .humidity(Humidity.WET, Humidity.HUMID)
                .continentalness(Continentalness.NEAR_INLAND, Continentalness.MID_INLAND)
                .erosion(Erosion.EROSION_2, Erosion.EROSION_3, Erosion.EROSION_4)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.MID_SLICE_VARIANT_ASCENDING, Weirdness.HIGH_SLICE_VARIANT_ASCENDING, Weirdness.PEAK_VARIANT, Weirdness.HIGH_SLICE_VARIANT_DESCENDING)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.SALIKAWOOD));

        // 10. Feywood — Floresta mística coberta de névoa (Mist) adjacente a Golmore e Paramina
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.COOL, Temperature.NEUTRAL)
                .humidity(Humidity.WET, Humidity.HUMID)
                .continentalness(Continentalness.MID_INLAND, Continentalness.FAR_INLAND)
                .erosion(Erosion.EROSION_2, Erosion.EROSION_3, Erosion.EROSION_4)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.MID_SLICE_NORMAL_ASCENDING, Weirdness.HIGH_SLICE_NORMAL_ASCENDING, Weirdness.PEAK_NORMAL, Weirdness.HIGH_SLICE_NORMAL_DESCENDING, Weirdness.MID_SLICE_NORMAL_DESCENDING)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.FEYWOOD));

        // 11. Tchita Uplands — Planaltos elevados e campos temperados
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.NEUTRAL, Temperature.COOL)
                .humidity(Humidity.DRY, Humidity.NEUTRAL)
                .continentalness(Continentalness.MID_INLAND, Continentalness.FAR_INLAND)
                .erosion(Erosion.EROSION_1, Erosion.EROSION_2, Erosion.EROSION_3)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.MID_SLICE_NORMAL_ASCENDING, Weirdness.HIGH_SLICE_NORMAL_ASCENDING, Weirdness.PEAK_NORMAL)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.TCHITA_UPLANDS));

        // 12. Cerobi Steppe — Terraços rochosos ventilados e colinas elevadas
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.NEUTRAL, Temperature.COOL)
                .humidity(Humidity.ARID, Humidity.DRY, Humidity.NEUTRAL)
                .continentalness(Continentalness.NEAR_INLAND, Continentalness.MID_INLAND)
                .erosion(Erosion.EROSION_0, Erosion.EROSION_1, Erosion.EROSION_2)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.HIGH_SLICE_VARIANT_ASCENDING, Weirdness.PEAK_VARIANT, Weirdness.HIGH_SLICE_VARIANT_DESCENDING)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.CEROBI_STEPPE));

        // 13. Mosphoran Highwaste — Montanhas rochosas, penhascos íngremes e fontes termais
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.COOL, Temperature.NEUTRAL, Temperature.WARM)
                .humidity(Humidity.ARID, Humidity.DRY)
                .continentalness(Continentalness.MID_INLAND, Continentalness.FAR_INLAND)
                .erosion(Erosion.EROSION_0, Erosion.EROSION_1)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.HIGH_SLICE_NORMAL_ASCENDING, Weirdness.PEAK_NORMAL, Weirdness.HIGH_SLICE_NORMAL_DESCENDING, Weirdness.PEAK_VARIANT)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.MOSPHORAN_HIGHWASTE));

        // 14. Nabreus Deadlands — Pântanos sombrios e terras baixas alagadas
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.COOL, Temperature.NEUTRAL)
                .humidity(Humidity.WET, Humidity.HUMID)
                .continentalness(Continentalness.NEAR_INLAND, Continentalness.MID_INLAND)
                .erosion(Erosion.EROSION_5, Erosion.EROSION_6)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.FULL_RANGE)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.NABREUS_DEADLANDS));

        // 15. Paramina Rift — Desfiladeiros congelados, neve e montanhas glaciais
        new ParameterUtils.ParameterPointListBuilder()
                .temperature(Temperature.ICY)
                .humidity(Humidity.FULL_RANGE)
                .continentalness(Continentalness.INLAND)
                .erosion(Erosion.FULL_RANGE)
                .depth(Depth.SURFACE, Depth.FLOOR)
                .weirdness(Weirdness.FULL_RANGE)
                .build().forEach(point -> addBiome(mapper, point, IvaliceBiomes.PARAMINA_RIFT));
    }
}
