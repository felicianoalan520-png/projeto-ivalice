package com.ivalice.worldgen.surface;

import com.ivalice.registry.IvaliceBlocks;
import com.ivalice.worldgen.biome.IvaliceBiomes;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.material.VanillaMaterialConditions;
import net.minecraft.data.worldgen.material.VanillaMaterialRules;
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
        HolderGetter<MaterialRule> rules = registryAccess.lookupOrThrow(Registries.MATERIAL_RULE);

        MaterialCondition onFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.ON_FLOOR);
        MaterialCondition underFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.UNDER_FLOOR);
        MaterialCondition deepUnderFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.DEEP_UNDER_FLOOR);
        MaterialCondition veryDeepUnderFloor = MaterialRules.getCondition(conditions, VanillaMaterialConditions.VERY_DEEP_UNDER_FLOOR);
        MaterialCondition notUnderwater = MaterialRules.getCondition(conditions, VanillaMaterialConditions.NOT_UNDERWATER);
        MaterialCondition steep = MaterialRules.steep();
        MaterialCondition aboveSurface = MaterialRules.abovePreliminarySurface();

        MaterialCondition noiseHigh = MaterialRules.noiseCondition2d(Noises.SURFACE, 0.15, Double.MAX_VALUE);
        MaterialCondition noiseMedium = MaterialRules.noiseCondition2d(Noises.SURFACE, -0.15, 0.15);
        MaterialCondition swampWaterLevel = MaterialRules.yBlockCheck(VerticalAnchor.absolute(62), 0);
        MaterialCondition belowSeaLevelPlusOne = MaterialRules.not(MaterialRules.yBlockCheck(VerticalAnchor.absolute(63), 0));
        MaterialCondition swampNoise = MaterialRules.noiseCondition2d(Noises.SWAMP, 0.0, Double.MAX_VALUE);

        // Blocos personalizados de Ivalice
        MaterialRule dalmascaSand = makeStateRule(IvaliceBlocks.DALMASCA_SAND.get());
        MaterialRule dalmascaSandstone = makeStateRule(IvaliceBlocks.DALMASCA_SANDSTONE.get());
        MaterialRule yensaSand = makeStateRule(IvaliceBlocks.YENSA_FINE_SAND.get());
        MaterialRule sunstoneOre = makeStateRule(IvaliceBlocks.SUNSTONE_ORE.get());
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
                        MaterialRules.ifTrue(deepUnderFloor, dalmascaSandstone),
                        MaterialRules.ifTrue(veryDeepUnderFloor, dalmascaSandstone)
                )
        );

        // 2. Dalmasca Westersand: Grandes dunas de Dalmasca Sand com base profunda de Dalmasca Sandstone
        MaterialRule westersandRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.DALMASCA_WESTERSAND),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, terracotta),
                        MaterialRules.ifTrue(onFloor, dalmascaSand),
                        MaterialRules.ifTrue(underFloor, dalmascaSand),
                        MaterialRules.ifTrue(deepUnderFloor, dalmascaSandstone),
                        MaterialRules.ifTrue(veryDeepUnderFloor, dalmascaSandstone)
                )
        );

        // 3. Ogir-Yensa Sandsea: Mar de areia fina alaranjada (Yensa Fine Sand) sobre arenito vermelho
        MaterialRule yensaRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.OGIR_YENSA_SANDSEA),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, redSandstone),
                        MaterialRules.ifTrue(onFloor, yensaSand),
                        MaterialRules.ifTrue(underFloor, yensaSand),
                        MaterialRules.ifTrue(deepUnderFloor, redSandstone),
                        MaterialRules.ifTrue(veryDeepUnderFloor, redSandstone)
                )
        );

        // 4. Giza Plains (Dry): Grama dourada com manchas de areia de Dalmasca e afloramentos de Sunstone
        MaterialRule gizaDryRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.GIZA_PLAINS_DRY),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, dalmascaSandstone),
                        MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, dalmascaSand),
                                MaterialRules.ifTrue(noiseMedium, grassBlock),
                                dalmascaSand
                        ))),
                        MaterialRules.ifTrue(underFloor, dalmascaSandstone)
                )
        );

        // 5. Giza Plains (Rains): Planícies alagadas com poças naturais no nível da água, lama e veios de Sunstone
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

        // 6. Ozmone Plain: Campos amplos com Dalmasca Sandstone nas encostas e solo fértil
        MaterialRule ozmoneRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.OZMONE_PLAIN),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, dalmascaSandstone),
                        MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, coarseDirt),
                                grassBlock
                        ))),
                        MaterialRules.ifTrue(underFloor, dirt)
                )
        );

        // 7. Golmore Jungle & Salikawood: Solo de pedra musgosa de Golmore (Golmore Moss Stone) e podzol
        MaterialRule golmoreRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.GOLMORE_JUNGLE, IvaliceBiomes.SALIKAWOOD),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, golmoreMoss),
                        MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, golmoreMoss),
                                MaterialRules.ifTrue(noiseMedium, golmoreMoss),
                                podzol
                        ))),
                        MaterialRules.ifTrue(underFloor, golmoreMoss)
                )
        );

        // 8. Phon Coast: Praias brancas de calcita/areia de Dalmasca e falésias costeiras
        MaterialRule phonCoastRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.PHON_COAST),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, calcite),
                        MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, calcite),
                                dalmascaSand
                        )),
                        MaterialRules.ifTrue(underFloor, dalmascaSand),
                        MaterialRules.ifTrue(deepUnderFloor, calcite)
                )
        );

        // 9. Feywood: Solo branco de névoa (Feywood Mist Soil) cobrindo toda a floresta mística
        MaterialRule feywoodRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.FEYWOOD),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, calcite),
                        MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(notUnderwater, feywoodSoil)),
                        MaterialRules.ifTrue(underFloor, feywoodSoil),
                        MaterialRules.ifTrue(deepUnderFloor, calcite)
                )
        );

        // 10. Paramina Rift: Permafrost cristalino azul de Paramina dominante na superfície e penhascos
        MaterialRule paraminaRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.PARAMINA_RIFT),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, paraminaIce),
                        MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, paraminaIce),
                                MaterialRules.ifTrue(noiseMedium, paraminaIce),
                                snowBlock
                        )),
                        MaterialRules.ifTrue(underFloor, paraminaIce),
                        MaterialRules.ifTrue(deepUnderFloor, packedIce)
                )
        );

        // 11. Mosphoran Highwaste: Ardósia vulcânica cinza-azulada (Mosphoran Slate) dominante
        MaterialRule mosphoranRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.MOSPHORAN_HIGHWASTE),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, mosphoranSlate),
                        MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, mosphoranSlate),
                                MaterialRules.ifTrue(noiseMedium, mosphoranSlate),
                                coarseDirt
                        )),
                        MaterialRules.ifTrue(underFloor, mosphoranSlate),
                        MaterialRules.ifTrue(deepUnderFloor, mosphoranSlate)
                )
        );

        // 12. Nabreus Deadlands: Pântano sombrio de Nabreus Mire dominante, lama e lagoas rasas
        MaterialRule nabreusRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.NABREUS_DEADLANDS),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(onFloor, MaterialRules.sequence(
                                MaterialRules.ifTrue(swampWaterLevel, MaterialRules.ifTrue(belowSeaLevelPlusOne, MaterialRules.ifTrue(swampNoise, water))),
                                MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                        MaterialRules.ifTrue(noiseHigh, nabreusMire),
                                        MaterialRules.ifTrue(noiseMedium, nabreusMire),
                                        mud
                                ))
                        )),
                        MaterialRules.ifTrue(underFloor, nabreusMire)
                )
        );

        // 13. Cerobi Steppe & Tchita Uplands: Terraços rochosos de Mosphoran Slate e Golmore Moss Stone
        MaterialRule uplandsRule = MaterialRules.ifTrue(
                MaterialRules.isBiome(biomes, IvaliceBiomes.CEROBI_STEPPE, IvaliceBiomes.TCHITA_UPLANDS),
                MaterialRules.sequence(
                        MaterialRules.ifTrue(steep, mosphoranSlate),
                        MaterialRules.ifTrue(onFloor, MaterialRules.ifTrue(notUnderwater, MaterialRules.sequence(
                                MaterialRules.ifTrue(noiseHigh, mosphoranSlate),
                                MaterialRules.ifTrue(noiseMedium, golmoreMoss),
                                grassBlock
                        ))),
                        MaterialRules.ifTrue(underFloor, mosphoranSlate)
                )
        );

        MaterialRule ivaliceSurfaceSequence = MaterialRules.sequence(
                estersandRule,
                westersandRule,
                yensaRule,
                gizaDryRule,
                gizaRainsRule,
                ozmoneRule,
                golmoreRule,
                phonCoastRule,
                feywoodRule,
                paraminaRule,
                mosphoranRule,
                nabreusRule,
                uplandsRule
        );

        Holder.Reference<MaterialRule> bedrockFloor = rules.getOrThrow(VanillaMaterialRules.BEDROCK_FLOOR);
        MaterialRule deepslateRule = MaterialRules.ifTrue(
                MaterialRules.verticalGradient("deepslate", VerticalAnchor.absolute(0), VerticalAnchor.absolute(8)),
                makeStateRule(Blocks.DEEPSLATE)
        );

        return MaterialRules.sequence(
                new MaterialRule.HolderHolder(bedrockFloor),
                MaterialRules.ifTrue(aboveSurface, ivaliceSurfaceSequence),
                deepslateRule
        );
    }
}
