package com.ivalice.worldgen.surface;

import com.ivalice.registry.IvaliceBlocks;
import com.ivalice.worldgen.biome.IvaliceBiomes;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.material.VanillaMaterialConditions;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;

/**
 * Regras de superfície (MaterialRules) para os biomas de Final Fantasy XII via TerraBlender 26.3.
 * Define o bloco de topo, subsolo e variações de ruído para cada região canônica de Ivalice.
 */
public final class IvaliceSurfaceRules {
    private IvaliceSurfaceRules() {}

    private static MaterialRule makeStateRule(Block block) {
        return MaterialRules.state(block.defaultBlockState());
    }

    public static MaterialRule buildOverworldRules(RegistryAccess registryAccess) {
        HolderGetter<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);
        HolderGetter<MaterialCondition> conditions = registryAccess.lookupOrThrow(Registries.MATERIAL_CONDITION);

        MaterialCondition onFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.ON_FLOOR);
        MaterialCondition underFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.UNDER_FLOOR);
        MaterialCondition deepUnderFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.DEEP_UNDER_FLOOR);
        MaterialCondition notUnderwater = MaterialRules.getCondition(conditions, VanillaMaterialConditions.NOT_UNDERWATER);
        MaterialCondition steep = MaterialRules.steep();

        MaterialCondition noiseHigh = MaterialRules.noiseCondition2d(Noises.SURFACE, 0.22, Double.MAX_VALUE);
        MaterialCondition noiseMedium = MaterialRules.noiseCondition2d(Noises.SURFACE, -0.05, 0.22);
        MaterialCondition swampWaterLevel = MaterialRules.yBlockCheck(VerticalAnchor.absolute(62), 0);
        MaterialCondition belowSeaLevelPlusOne = MaterialRules.not(MaterialRules.yBlockCheck(VerticalAnchor.absolute(63), 0));
        MaterialCondition swampNoise = MaterialRules.noiseCondition2d(Noises.SWAMP, 0.0, Double.MAX_VALUE);

        // Blocos personalizados de Ivalice
        MaterialRule dalmascaSand = makeStateRule(IvaliceBlocks.DALMASCA_SAND.get());
        MaterialRule dalmascaSandstone = makeStateRule(IvaliceBlocks.DALMASCA_SANDSTONE.get());
        MaterialRule yensaSand = makeStateRule(IvaliceBlocks.YENSA_FINE_SAND.get());
        MaterialRule golmoreMoss = makeStateRule(IvaliceBlocks.GOLMORE_MOSS_STONE.get());
        MaterialRule feywoodSoil = makeStateRule(IvaliceBlocks.FEYWOOD_MIST_SOIL.get());
        MaterialRule paraminaIce = makeStateRule(IvaliceBlocks.PARAMINA_PERMAFROST.get());
        MaterialRule mosphoranSlate = makeStateRule(IvaliceBlocks.MOSPHORAN_SLATE.get());
        MaterialRule nabreusMire = makeStateRule(IvaliceBlocks.NABREUS_MIRE.get());

        // Blocos vanilla complementares
        MaterialRule grassBlock = makeStateRule(Blocks.GRASS_BLOCK);
        MaterialRule dirt = makeStateRule(Blocks.DIRT);
        MaterialRule coarseDirt = makeStateRule(Blocks.COARSE_DIRT);
        MaterialRule podzol = makeStateRule(Blocks.PODZOL);
        MaterialRule mud = makeStateRule(Blocks.MUD);
        MaterialRule sand = makeStateRule(Blocks.SAND);
        MaterialRule sandstone = makeStateRule(Blocks.SANDSTONE);
        MaterialRule redSandstone = makeStateRule(Blocks.RED_SANDSTONE);
        MaterialRule terracotta = makeStateRule(Blocks.TERRACOTTA);
        MaterialRule snowBlock = makeStateRule(Blocks.SNOW_BLOCK);
        MaterialRule powderSnow = makeStateRule(Blocks.POWDER_SNOW);
        MaterialRule packedIce = makeStateRule(Blocks.PACKED_ICE);
        MaterialRule calcite = makeStateRule(Blocks.CALCITE);
        MaterialRule stone = makeStateRule(Blocks.STONE);
        MaterialRule water = makeStateRule(Blocks.WATER);

        // 1. Dalmasca Estersand: Areia dourada de Dalmasca, afloramentos de arenito de Dalmasca e terracota nas encostas
        MaterialRule estersandRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.DALMASCA_ESTERSAND),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, dalmascaSandstone),
                        MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, dalmascaSandstone),
                                dalmascaSand
                        )),
                        MaterialRules.ifTrue(underFloor, dalmascaSand),
                        MaterialRules.ifTrue(deepUnderFloor, dalmascaSandstone)
                )
        );

        // 2. Dalmasca Westersand: Grandes dunas de Dalmasca Sand com base profunda de Dalmasca Sandstone
        MaterialRule westersandRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.DALMASCA_WESTERSAND),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, terracotta),
                        MaterialRules.ifTrue(onFloor, dalmascaSand),
                        MaterialRules.ifTrue(underFloor, dalmascaSand),
                        MaterialRules.ifTrue(deepUnderFloor, dalmascaSandstone)
                )
        );

        // 3. Ogir-Yensa Sandsea: Mar de areia fina alaranjada (Yensa Fine Sand) sobre arenito vermelho
        MaterialRule yensaRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.OGIR_YENSA_SANDSEA),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(onFloor, yensaSand),
                        MaterialRules.ifTrue(underFloor, yensaSand),
                        MaterialRules.ifTrue(deepUnderFloor, redSandstone)
                )
        );

        // 4. Giza Plains (Dry): Grama dourada com manchas de terra batida e areia de Dalmasca
        MaterialRule gizaDryRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.GIZA_PLAINS_DRY),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, dalmascaSand),
                                MaterialRules.ifTrue(noiseMedium, coarseDirt),
                                grassBlock
                        ))),
                        MaterialRules.ifTrue(underFloor, dirt)
                )
        );

        // 5. Giza Plains (Rains): Planícies alagadas com poças naturais no nível da água e lama fértil
        MaterialRule gizaRainsRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.GIZA_PLAINS_RAINS),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                                MaterialRules.ifTrue(swampWaterLevel, MaterialRules.ifTrue(belowSeaLevelPlusOne, MaterialRules.ifTrue(swampNoise, water))),
                                MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                        MaterialRules.ifTrue(noiseHigh, mud),
                                        grassBlock
                                ))
                        )),
                        MaterialRules.ifTrue(underFloor, mud)
                )
        );

        // 6. Golmore Jungle: Solo de podzol, pedra musgosa de Golmore e grama exuberante
        MaterialRule golmoreRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.GOLMORE_JUNGLE),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, golmoreMoss),
                                MaterialRules.ifTrue(noiseMedium, podzol),
                                grassBlock
                        ))),
                        MaterialRules.ifTrue(underFloor, dirt)
                )
        );

        // 7. Phon Coast: Praias brancas de areia/calcita e falésias costeiras
        MaterialRule phonCoastRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.PHON_COAST),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, calcite),
                        MaterialRules.ifTrue(onFloor, sand),
                        MaterialRules.ifTrue(underFloor, sand),
                        MaterialRules.ifTrue(deepUnderFloor, sandstone)
                )
        );

        // 8. Feywood: Solo branco de névoa (Feywood Mist Soil) e calcita pálida
        MaterialRule feywoodRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.FEYWOOD),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, feywoodSoil),
                                grassBlock
                        ))),
                        MaterialRules.ifTrue(underFloor, feywoodSoil)
                )
        );

        // 9. Paramina Rift: Permafrost cristalino de Paramina, neve profunda e gelo compactado
        MaterialRule paraminaRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.PARAMINA_RIFT),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, paraminaIce),
                        MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, paraminaIce),
                                MaterialRules.ifTrue(noiseMedium, powderSnow),
                                snowBlock
                        )),
                        MaterialRules.ifTrue(underFloor, packedIce),
                        MaterialRules.ifTrue(deepUnderFloor, paraminaIce)
                )
        );

        // 10. Mosphoran Highwaste: Ardósia vulcânica cinza-azulada (Mosphoran Slate) e rocha nua
        MaterialRule mosphoranRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.MOSPHORAN_HIGHWASTE),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, mosphoranSlate),
                        MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, mosphoranSlate),
                                MaterialRules.ifTrue(noiseMedium, coarseDirt),
                                stone
                        )),
                        MaterialRules.ifTrue(underFloor, mosphoranSlate)
                )
        );

        // 11. Nabreus Deadlands: Pântano sombrio de Nabreus Mire, lama e lagoas rasas
        MaterialRule nabreusRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.NABREUS_DEADLANDS),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                                MaterialRules.ifTrue(swampWaterLevel, MaterialRules.ifTrue(belowSeaLevelPlusOne, MaterialRules.ifTrue(swampNoise, water))),
                                MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                        MaterialRules.ifTrue(noiseHigh, nabreusMire),
                                        MaterialRules.ifTrue(noiseMedium, mud),
                                        grassBlock
                                ))
                        )),
                        MaterialRules.ifTrue(underFloor, nabreusMire)
                )
        );

        // 12. Cerobi Steppe & Tchita Uplands: Terraços rochosos com afloramentos de pedra nas encostas
        MaterialRule uplandsRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.CEROBI_STEPPE, IvaliceBiomes.TCHITA_UPLANDS),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, stone),
                        MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, coarseDirt),
                                grassBlock
                        ))),
                        MaterialRules.ifTrue(underFloor, dirt)
                )
        );

        return MaterialRules.sequence(
                estersandRule,
                westersandRule,
                yensaRule,
                gizaDryRule,
                gizaRainsRule,
                golmoreRule,
                phonCoastRule,
                feywoodRule,
                paraminaRule,
                mosphoranRule,
                nabreusRule,
                uplandsRule
        );
    }
}
