package com.ivalice.combat;

import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.registry.IvaliceBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Coreografia visual de ataques físicos, técnicas e magias de todos os escalões (Tier 1 a Tier 4 Cataclísmico),
 * incluindo movimentação dinâmica de combate, projéteis em espiral, relâmpagos reais e destruição/transformação
 * regional do cenário (Firaga, Blizzaga, Thundaga, Waterga, Aeroga, Darkga, Flare, Ardor, Collapse, Scathe, Holy).
 */
public final class BattleVisual {
    public enum Style {
        SLASH(8, 1, ParticleTypes.CRIT, SoundEvents.PLAYER_ATTACK_SWEEP),
        CLEAVE(10, 2, ParticleTypes.SWEEP_ATTACK, SoundEvents.PLAYER_ATTACK_STRONG),

        // Tier 1 (Básicas)
        FIRE(14, 1, ParticleTypes.FLAME, SoundEvents.BLAZE_SHOOT),
        WATER(14, 1, ParticleTypes.SPLASH, SoundEvents.GENERIC_SPLASH),
        ICE(14, 1, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK),
        THUNDER(13, 1, ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_IMPACT),
        WIND(13, 1, ParticleTypes.CLOUD, SoundEvents.WIND_CHARGE_BURST.value()),
        DARK(14, 1, ParticleTypes.PORTAL, SoundEvents.WARDEN_ATTACK_IMPACT),

        // Tier 2 (Intermediárias -ra)
        FIRA(17, 2, ParticleTypes.FLAME, SoundEvents.FIRECHARGE_USE),
        BLIZZARA(17, 2, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK),
        THUNDARA(16, 2, ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_THUNDER),
        AQUARA(17, 2, ParticleTypes.SPLASH, SoundEvents.PLAYER_SPLASH_HIGH_SPEED),
        AERORA(16, 2, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.BREEZE_WIND_CHARGE_BURST.value()),
        DARKRA(17, 2, ParticleTypes.REVERSE_PORTAL, SoundEvents.WARDEN_SONIC_CHARGE),

        // Tier 3 (Avançadas -ga — Destruição de Cenário em Grande Escala)
        FIRAGA(22, 3, ParticleTypes.LAVA, SoundEvents.GENERIC_EXPLODE.value()),
        BLIZZAGA(22, 3, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK),
        THUNDAGA(20, 3, ParticleTypes.ELECTRIC_SPARK, SoundEvents.LIGHTNING_BOLT_THUNDER),
        WATERGA(22, 3, ParticleTypes.SPLASH, SoundEvents.GENERIC_EXPLODE.value()),
        AEROGA(21, 3, ParticleTypes.GUST_EMITTER_LARGE, SoundEvents.BREEZE_WIND_CHARGE_BURST.value()),
        DARKGA(22, 3, ParticleTypes.SCULK_SOUL, SoundEvents.WARDEN_SONIC_BOOM),

        // Tier 4 (Magias Supremas / Cataclísmicas — Alteração Regional de Terreno)
        ARDOR(26, 4, ParticleTypes.LAVA, SoundEvents.GENERIC_EXPLODE.value()),
        FLARE(25, 4, ParticleTypes.FLAME, SoundEvents.GENERIC_EXPLODE.value()),
        COLLAPSE(26, 4, ParticleTypes.SONIC_BOOM, SoundEvents.WARDEN_SONIC_BOOM),
        SCATHE(26, 4, ParticleTypes.END_ROD, SoundEvents.DRAGON_FIREBALL_EXPLODE),
        HOLY(24, 4, ParticleTypes.END_ROD, SoundEvents.BEACON_ACTIVATE),

        // Suporte, Itens e Técnicas
        CURE(12, 1, ParticleTypes.HEART, SoundEvents.EXPERIENCE_ORB_PICKUP),
        CURAGA(16, 3, ParticleTypes.HAPPY_VILLAGER, SoundEvents.BEACON_POWER_SELECT),
        HASTE(12, 2, ParticleTypes.WAX_ON, SoundEvents.EXPERIENCE_ORB_PICKUP),
        ITEM(9, 1, ParticleTypes.HAPPY_VILLAGER, SoundEvents.GENERIC_DRINK.value()),
        TECHNIQUE(10, 1, ParticleTypes.ENCHANT, SoundEvents.ENCHANTMENT_TABLE_USE);

        public final int impactTick;
        public final int tier;
        public final ParticleOptions particle;
        public final SoundEvent sound;

        Style(int impactTick, int tier, ParticleOptions particle, SoundEvent sound) {
            this.impactTick = impactTick;
            this.tier = tier;
            this.particle = particle;
            this.sound = sound;
        }

        public int endTick() {
            return impactTick + (tier >= 3 ? 12 : 8);
        }
    }

    private BattleVisual() {}

    public static void begin(ServerLevel level, LivingEntity actor, Style style) {
        if (actor instanceof IvaliceMonsterEntity mob) {
            mob.playBattleAnimation(style == Style.SLASH || style == Style.CLEAVE ? "strike" : "cast");
        }
        actor.swingAndResetAttackStrength(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
        if (style != Style.SLASH && style != Style.CLEAVE) {
            int ringCount = 12 * style.tier;
            double radius = 0.6 + style.tier * 0.35;
            Vec3 origin = actor.position().add(0, 0.2, 0);
            for (int i = 0; i < ringCount; i++) {
                double angle = (Math.PI * 2 * i) / ringCount;
                Vec3 pos = origin.add(Math.cos(angle) * radius, 0.1, Math.sin(angle) * radius);
                particles(level, ParticleTypes.ENCHANT, pos, 2, 0.08, 0.02);
                if (style.tier >= 3) {
                    particles(level, style.particle, pos.add(0, 0.3, 0), 1, 0.05, 0.03);
                }
            }
        }
        level.playSound(null, actor.blockPosition(), style.sound, SoundSource.PLAYERS,
                style.tier >= 3 ? 1.2F : 0.75F, style.tier >= 3 ? 0.85F : 1.0F);
    }

    public static void animate(ServerLevel level, LivingEntity actor, LivingEntity target, Style style, int age) {
        if (!target.isAlive() && age <= style.impactTick) return;
        Vec3 start = center(actor);
        boolean playerTarget = target instanceof ServerPlayer;
        Vec3 destination = playerTarget ? target.position().add(0, .2, 0) : center(target);

        if (age == style.impactTick) {
            impact(level, target, style, false);
            return;
        }
        if (age > style.impactTick) {
            if ((age & 1) == 0) {
                switch (style) {
                    case FIRE, FIRA, FIRAGA, ARDOR, FLARE ->
                            particles(level, ParticleTypes.FLAME, destination, 6 * style.tier, 0.35 * style.tier, 0.03);
                    case WATER, AQUARA, WATERGA ->
                            particles(level, ParticleTypes.DRIPPING_WATER, destination, 7 * style.tier, 0.35 * style.tier, 0.03);
                    case ICE, BLIZZARA, BLIZZAGA ->
                            particles(level, ParticleTypes.SNOWFLAKE, destination, 6 * style.tier, 0.35 * style.tier, 0.02);
                    case THUNDER, THUNDARA, THUNDAGA ->
                            particles(level, ParticleTypes.ELECTRIC_SPARK, destination, 6 * style.tier, 0.35 * style.tier, 0.06);
                    default -> {}
                }
            }
            return;
        }
        if (age < 3 || style == Style.SLASH || style == Style.CLEAVE) return;

        double time = Math.clamp((double) (age - 2) / Math.max(1, style.impactTick - 2), 0.0, 1.0);
        Vec3 travel = start.lerp(destination, time);

        switch (style) {
            case FIRE, FIRA -> {
                double arc = Math.sin(time * Math.PI) * (style == Style.FIRA ? 1.1 : 0.45);
                Vec3 pos = travel.add(0, arc, 0);
                particles(level, ParticleTypes.FLAME, pos, style == Style.FIRA ? 12 : 6, 0.18, 0.02);
                if (age % 3 == 0) particles(level, ParticleTypes.SMOKE, pos, 2, 0.12, 0.01);
                if (style == Style.FIRA) {
                    double spiral = age * 0.9;
                    particles(level, ParticleTypes.LAVA, pos.add(Math.cos(spiral) * 0.6, 0, Math.sin(spiral) * 0.6), 2, 0.08, 0.02);
                }
            }
            case FIRAGA, ARDOR, FLARE -> {
                // Esfera solar colossal descendo do céu e anéis de fogo convergindo
                Vec3 meteor = destination.add(0, (1.0 - time) * (style == Style.ARDOR ? 14.0 : 9.5), 0);
                double radius = (1.0 - time * 0.5) * (style.tier == 4 ? 4.5 : 2.8);
                particles(level, ParticleTypes.FLAME, meteor, 28, 0.65, 0.05);
                particles(level, ParticleTypes.LAVA, meteor, 10, 0.45, 0.04);
                particles(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, meteor, 6, 0.5, 0.02);
                for (int i = 0; i < 8; i++) {
                    double angle = age * 0.45 + (Math.PI * 2 * i) / 8;
                    Vec3 ringPos = destination.add(Math.cos(angle) * radius, 0.25, Math.sin(angle) * radius);
                    particles(level, ParticleTypes.FLAME, ringPos, 3, 0.1, 0.02);
                }
            }
            case WATER, AQUARA, WATERGA -> {
                double height = style == Style.WATERGA ? 8.0 : (style == Style.AQUARA ? 5.0 : 3.2);
                double spread = style == Style.WATERGA ? 1.8 : 0.45;
                particles(level, ParticleTypes.FALLING_WATER, destination.add(0, (1 - time) * height, 0), 10 * style.tier, spread, 0.04);
                particles(level, ParticleTypes.SPLASH, destination.add(0, 0.3, 0), 8 * style.tier, spread, 0.12);
            }
            case THUNDER, THUNDARA, THUNDAGA -> {
                double spread = style == Style.THUNDAGA ? 2.2 : 0.35;
                particles(level, ParticleTypes.ELECTRIC_SPARK, destination.add(0, (1 - time) * 6.5, 0), 10 * style.tier, spread, 0.08);
                if (style.tier >= 2) {
                    particles(level, ParticleTypes.LARGE_SMOKE, destination.add(0, 6.0, 0), 8, spread * 1.2, 0.01);
                }
            }
            case WIND, AERORA, AEROGA -> {
                double radius = (0.4 + style.tier * 0.55) * (0.5 + time * 0.7);
                for (int i = 0; i < 4 * style.tier; i++) {
                    double angle = age * 0.75 + (Math.PI * 2 * i) / (4 * style.tier);
                    double ring = playerTarget ? radius + .7 : radius;
                    Vec3 swirl = destination.add(Math.cos(angle) * ring, (i % 4) * (playerTarget ? .2 : .65),
                            Math.sin(angle) * ring);
                    particles(level, ParticleTypes.CLOUD, swirl, playerTarget ? 1 : 2, .12, .03);
                }
            }
            case ICE, BLIZZARA, BLIZZAGA -> {
                double spread = 0.25 * style.tier;
                particles(level, ParticleTypes.SNOWFLAKE, travel, 8 * style.tier, spread, 0.04);
                particles(level, ParticleTypes.WHITE_ASH, destination.add(0, 1.5, 0), 6 * style.tier, spread * 2.0, 0.05);
            }
            case DARK, DARKRA, DARKGA, COLLAPSE -> {
                double radius = (1.1 - time * 0.8) * (1.2 * style.tier);
                for (int i = 0; i < 6; i++) {
                    double angle = age * 0.6 + (Math.PI * 2 * i) / 6;
                    Vec3 implosion = destination.add(Math.cos(angle) * radius, 0.6 + Math.sin(age * 0.3) * 0.4, Math.sin(angle) * radius);
                    particles(level, ParticleTypes.PORTAL, implosion, 5, 0.15, 0.08);
                    particles(level, ParticleTypes.SQUID_INK, implosion, 2, 0.1, 0.02);
                }
            }
            case HOLY, SCATHE -> {
                double radius = (1.0 - time * 0.5) * 3.2;
                for (int i = 0; i < 8; i++) {
                    double angle = age * 0.5 + (Math.PI * 2 * i) / 8;
                    Vec3 beam = destination.add(Math.cos(angle) * radius, (1.0 - time) * 8.0, Math.sin(angle) * radius);
                    particles(level, ParticleTypes.END_ROD, beam, 3, 0.08, 0.02);
                }
            }
            default -> {
                double angle = age * 0.72;
                particles(level, style.particle, destination.add(Math.cos(angle) * 0.64, time * 0.6, Math.sin(angle) * 0.64),
                        3 * style.tier, 0.12, 0.02);
            }
        }
    }

    public static void impact(ServerLevel level, LivingEntity target, Style style) {
        impact(level, target, style, true);
    }

    private static void impact(ServerLevel level, LivingEntity target, Style style, boolean affectWorld) {
        boolean nearCamera = !affectWorld && target instanceof ServerPlayer;
        Vec3 center = nearCamera ? target.position().add(0, .25, 0) : center(target);
        if (style == Style.SLASH || style == Style.CLEAVE) {
            particles(level, ParticleTypes.SWEEP_ATTACK, center, style == Style.CLEAVE ? 4 : 1, 0.25, 0.0);
            BlockState ground = level.getBlockState(target.blockPosition().below());
            if (!ground.isAir()) {
                particles(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), target.position(), 14, 0.35, 0.12);
            }
        }

        boolean gust = style == Style.WIND || style == Style.AERORA || style == Style.AEROGA;
        if (!nearCamera || !gust) {
            int count = nearCamera ? 10 : (style.tier >= 3 ? 65 : (style.tier == 2 ? 36 : 18));
            particles(level, style.particle, center, count, .35 * style.tier, .14);
        }

        if (style.tier >= 3) {
            if (!nearCamera) particles(level, ParticleTypes.EXPLOSION_EMITTER, center, 1, .2, 0.0);
            level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.3F, 0.8F);
        }

        switch (style) {
            case FIRE, FIRA, FIRAGA, ARDOR, FLARE -> {
                particles(level, ParticleTypes.LAVA, center, 6 * style.tier, 0.25 * style.tier, 0.05);
                particles(level, ParticleTypes.FLAME, center, 18 * style.tier, 0.45 * style.tier, 0.08);
                if (affectWorld) target.igniteForSeconds(2.5F * style.tier);
            }
            case WATER, AQUARA, WATERGA -> {
                particles(level, ParticleTypes.FALLING_WATER, center.add(0, 1.0, 0), 18 * style.tier, 0.45 * style.tier, 0.06);
                particles(level, ParticleTypes.SPLASH, center, 24 * style.tier, 0.5 * style.tier, 0.15);
                if (affectWorld) target.clearFire();
            }
            case THUNDER, THUNDARA, THUNDAGA -> {
                if (style.tier >= 2) {
                    spawnVisualLightning(level, target.position());
                    if (style == Style.THUNDAGA) {
                        spawnVisualLightning(level, target.position().add(2.5, 0, 1.5));
                        spawnVisualLightning(level, target.position().add(-2.5, 0, -1.5));
                    }
                }
            }
            case WIND, AERORA, AEROGA -> {
                if (nearCamera) particles(level, ParticleTypes.CLOUD, center, 5, .45, .02);
                else particles(level, ParticleTypes.GUST_EMITTER_LARGE, center, style.tier, .4 * style.tier, .05);
                if (affectWorld) target.push(0, 0.22 * style.tier, 0);
            }
            case COLLAPSE, SCATHE, HOLY -> {
                particles(level, ParticleTypes.SONIC_BOOM, center, 3, 0.4, 0.02);
                particles(level, ParticleTypes.END_ROD, center, 50, 1.2, 0.18);
            }
            default -> {}
        }

        if (target instanceof IvaliceMonsterEntity mob) {
            mob.playBattleAnimation("flinch");
        }

        // Interação real com o mundo (fogo, gelo, crateras e transformação regional de blocos conforme o Tier!)
        if (affectWorld) applyWorldImpact(level, target.blockPosition(), style);
    }

    private static void spawnVisualLightning(ServerLevel level, Vec3 pos) {
        for (int h = 0; h <= 14; h++) {
            double jitterX = (level.getRandom().nextDouble() - 0.5) * 0.45;
            double jitterZ = (level.getRandom().nextDouble() - 0.5) * 0.45;
            Vec3 columnPos = pos.add(jitterX, h * 0.85, jitterZ);
            particles(level, ParticleTypes.ELECTRIC_SPARK, columnPos, 8, 0.16, 0.05);
            if (h % 2 == 0) {
                particles(level, ParticleTypes.END_ROD, columnPos, 3, 0.08, 0.02);
            }
        }
        particles(level, ParticleTypes.EXPLOSION, pos.add(0, 1.0, 0), 2, 0.15, 0.02);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 1.5F, 0.9F);
        level.playSound(null, BlockPos.containing(pos), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.WEATHER, 1.2F, 1.0F);
    }

    /**
     * Aplica efeitos físicos e transformações de terreno no mundo conforme a escala da magia:
     * - Tier 1: Efeitos superficiais leves (chamas pontuais, camada de neve, extinção de fogo).
     * - Tier 2 (-ra): Alteração de blocos num raio de 3 blocos (gelo, queima de vegetação, terra chamuscada).
     * - Tier 3 (-ga): Destruição de cenário e transformação elemental num raio de 6 blocos (crateras, pilares de gelo, magma).
     * - Tier 4 (Ardor/Flare/Collapse/Scathe/Holy): Transformação regional de bioma/superfície num raio de 9 blocos!
     */
    public static void applyWorldImpact(ServerLevel level, BlockPos center, Style style) {
        if (style == Style.SLASH || style == Style.CLEAVE || style == Style.CURE || style == Style.CURAGA
                || style == Style.HASTE || style == Style.ITEM || style == Style.TECHNIQUE) {
            return;
        }

        int radius = switch (style.tier) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 6;
            default -> 9;
        };

        // Em magias Tier 3 e Tier 4, abre uma cratera de impacto real no epicentro (preservando blocos indestrutíveis e baús)
        if (style.tier >= 3 && (style == Style.FIRAGA || style == Style.ARDOR || style == Style.FLARE
                || style == Style.COLLAPSE || style == Style.SCATHE || style == Style.THUNDAGA)) {
            int craterRadius = style.tier == 4 ? 3 : 2;
            for (int dx = -craterRadius; dx <= craterRadius; dx++) {
                for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                    if (dx * dx + dz * dz > craterRadius * craterRadius) continue;
                    for (int dy = -1; dy <= 2; dy++) {
                        BlockPos p = center.offset(dx, dy, dz);
                        if (canModifyTerrainBlock(level, p)) {
                            BlockState st = level.getBlockState(p);
                            if (dy >= 0 && (st.is(BlockTags.REPLACEABLE) || st.is(BlockTags.LEAVES) || st.is(BlockTags.FLOWERS))) {
                                level.destroyBlock(p, false);
                            } else if (dy == -1 && (dx * dx + dz * dz <= (craterRadius - 1) * (craterRadius - 1))) {
                                level.destroyBlock(p, false);
                            }
                        }
                    }
                }
            }
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distSq = dx * dx + dz * dz;
                if (distSq > radius * radius) continue;

                int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX() + dx, center.getZ() + dz) - 1;
                BlockPos groundPos = new BlockPos(center.getX() + dx, topY, center.getZ() + dz);
                BlockPos abovePos = groundPos.above();

                if (!canModifyTerrainBlock(level, groundPos)) continue;
                BlockState ground = level.getBlockState(groundPos);
                BlockState above = level.getBlockState(abovePos);

                switch (style) {
                    case FIRE, FIRA, FIRAGA, ARDOR, FLARE -> {
                        if (style.tier >= 3 && level.getRandom().nextFloat() < (style.tier == 4 ? 0.72F : 0.48F)) {
                            BlockState scorched = switch (level.getRandom().nextInt(4)) {
                                case 0 -> IvaliceBlocks.SCORCHED_MAGMA_EARTH.get().defaultBlockState();
                                case 1 -> Blocks.MAGMA_BLOCK.defaultBlockState();
                                case 2 -> style.tier == 4 ? Blocks.BLACKSTONE.defaultBlockState() : Blocks.BASALT.defaultBlockState();
                                default -> IvaliceBlocks.DALMASCA_SAND.get().defaultBlockState();
                            };
                            level.setBlockAndUpdate(groundPos, scorched);
                        } else if (style.tier == 2 && ground.is(Blocks.GRASS_BLOCK) && level.getRandom().nextFloat() < 0.45F) {
                            level.setBlockAndUpdate(groundPos, Blocks.COARSE_DIRT.defaultBlockState());
                        }
                        if ((above.isAir() || above.is(BlockTags.REPLACEABLE)) && level.getRandom().nextFloat() < (0.18F * style.tier)) {
                            level.setBlockAndUpdate(abovePos, BaseFireBlock.getState(level, abovePos));
                        }
                    }
                    case ICE, BLIZZARA, BLIZZAGA -> {
                        if (ground.is(Blocks.WATER)) {
                            level.setBlockAndUpdate(groundPos, style.tier >= 3 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.ICE.defaultBlockState());
                        } else if (style.tier >= 3) {
                            if (level.getRandom().nextFloat() < 0.55F) {
                                level.setBlockAndUpdate(groundPos, level.getRandom().nextBoolean()
                                        ? IvaliceBlocks.GLACIER_RIME_STONE.get().defaultBlockState()
                                        : IvaliceBlocks.PARAMINA_PERMAFROST.get().defaultBlockState());
                            }
                            // Ergue pilares de cristal glacial nas bordas do impacto de Blizzaga!
                            if (distSq >= 3.0 && distSq <= 16.0 && level.getRandom().nextFloat() < 0.22F && above.isAir()) {
                                int spireHeight = 2 + level.getRandom().nextInt(3);
                                for (int h = 0; h < spireHeight; h++) {
                                    BlockPos sp = abovePos.above(h);
                                    if (level.getBlockState(sp).isAir()) {
                                        level.setBlockAndUpdate(sp, h == spireHeight - 1
                                                ? Blocks.BLUE_ICE.defaultBlockState()
                                                : IvaliceBlocks.GLACIER_RIME_STONE.get().defaultBlockState());
                                    }
                                }
                            } else if (above.isAir() && level.getRandom().nextFloat() < 0.55F) {
                                level.setBlockAndUpdate(abovePos, Blocks.SNOW.defaultBlockState());
                            }
                        } else if (above.isAir() && level.getRandom().nextFloat() < 0.35F * style.tier) {
                            level.setBlockAndUpdate(abovePos, Blocks.SNOW.defaultBlockState());
                        }
                    }
                    case THUNDER, THUNDARA, THUNDAGA -> {
                        if (style.tier >= 2 && distSq <= (style.tier == 3 ? 16 : 6) && level.getRandom().nextFloat() < 0.38F) {
                            level.setBlockAndUpdate(groundPos, level.getRandom().nextBoolean()
                                    ? IvaliceBlocks.SCORCHED_MAGMA_EARTH.get().defaultBlockState()
                                    : Blocks.OBSIDIAN.defaultBlockState());
                        }
                    }
                    case WATER, AQUARA, WATERGA -> {
                        if (above.is(BlockTags.FIRE)) {
                            level.setBlockAndUpdate(abovePos, Blocks.AIR.defaultBlockState());
                        }
                        if (style.tier >= 3 && level.getRandom().nextFloat() < 0.45F) {
                            level.setBlockAndUpdate(groundPos, level.getRandom().nextBoolean()
                                    ? IvaliceBlocks.NABREUS_MIRE.get().defaultBlockState()
                                    : Blocks.MUD.defaultBlockState());
                        }
                    }
                    case WIND, AERORA, AEROGA -> {
                        if ((above.is(BlockTags.REPLACEABLE) || above.is(BlockTags.FLOWERS) || above.is(BlockTags.LEAVES))
                                && level.getRandom().nextFloat() < 0.35F * style.tier) {
                            level.destroyBlock(abovePos, true);
                        }
                    }
                    case DARK, DARKRA, DARKGA, COLLAPSE, SCATHE -> {
                        if (style.tier >= 3 && level.getRandom().nextFloat() < 0.55F) {
                            BlockState riftBlock = switch (level.getRandom().nextInt(3)) {
                                case 0 -> IvaliceBlocks.FEYWOOD_RUNED_MARBLE.get().defaultBlockState();
                                case 1 -> IvaliceBlocks.NABREUS_MIRE.get().defaultBlockState();
                                default -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
                            };
                            level.setBlockAndUpdate(groundPos, riftBlock);
                        }
                    }
                    case HOLY -> {
                        if (level.getRandom().nextFloat() < 0.58F) {
                            level.setBlockAndUpdate(groundPos, level.getRandom().nextInt(4) == 0
                                    ? IvaliceBlocks.RABANASTRE_GOLD_INLAY.get().defaultBlockState()
                                    : IvaliceBlocks.BHUJERBAN_LIMESTONE.get().defaultBlockState());
                            if (above.isAir() && level.getRandom().nextFloat() < 0.16F) {
                                level.setBlockAndUpdate(abovePos, IvaliceBlocks.FEYWOOD_WHITE_LILY.get().defaultBlockState());
                            }
                        }
                    }
                    default -> {}
                }
            }
        }
    }

    private static boolean canModifyTerrainBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || level.getBlockEntity(pos) != null) {
            return false;
        }
        return !state.is(Blocks.BEDROCK) && !state.is(Blocks.END_PORTAL_FRAME) && !state.is(Blocks.SPAWNER);
    }

    private static Vec3 center(LivingEntity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.60, 0);
    }

    private static void particles(ServerLevel level, ParticleOptions particle, Vec3 center, int count, double spread, double speed) {
        level.sendParticles(particle, center.x, center.y, center.z, count, spread, spread, spread, speed);
    }
}
