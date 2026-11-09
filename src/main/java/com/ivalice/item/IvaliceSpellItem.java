package com.ivalice.item;

import com.ivalice.combat.BattleManager;
import com.ivalice.combat.BattleVisual;
import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.job.ZodiacJobs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Locale;

/**
 * Pergaminhos e Grimórios conjuráveis de Final Fantasy XII (do Tier 1 básico até Tier 3 -ga e Tier 4 Cataclísmico).
 * Para usar uma magia no combate por turnos ou em campo, o jogador precisa aprender o pergaminho (Clique Direito).
 */
public class IvaliceSpellItem extends IvaliceCatalogItem {
    public enum SpellAction {
        // Tier 1 (Nível 1 a 8 — Magias Básicas • Raridade COMMON)
        CURE(1, 1, 5.0, BattleVisual.Style.CURE, Rarity.COMMON),
        FIRE(1, 1, 4.5, BattleVisual.Style.FIRE, Rarity.COMMON),
        BLIZZARD(1, 3, 4.5, BattleVisual.Style.ICE, Rarity.COMMON),
        THUNDER(1, 4, 4.5, BattleVisual.Style.THUNDER, Rarity.COMMON),
        AQUA(1, 5, 4.5, BattleVisual.Style.WATER, Rarity.COMMON),
        AERO(1, 7, 4.5, BattleVisual.Style.WIND, Rarity.COMMON),
        DARK(1, 8, 4.5, BattleVisual.Style.DARK, Rarity.COMMON),

        // Tier 2 (Nível 14 a 28 — Vendidas até o Tier 2 em Aldeões • Raridade UNCOMMON)
        CURA(2, 14, 8.0, BattleVisual.Style.CURE, Rarity.UNCOMMON),
        FIRA(2, 16, 7.5, BattleVisual.Style.FIRA, Rarity.UNCOMMON),
        BLIZZARA(2, 18, 7.5, BattleVisual.Style.BLIZZARA, Rarity.UNCOMMON),
        THUNDARA(2, 20, 7.5, BattleVisual.Style.THUNDARA, Rarity.UNCOMMON),
        AQUARA(2, 22, 7.5, BattleVisual.Style.AQUARA, Rarity.UNCOMMON),
        AERORA(2, 25, 7.5, BattleVisual.Style.AERORA, Rarity.UNCOMMON),
        DARKRA(2, 28, 7.5, BattleVisual.Style.DARKRA, Rarity.UNCOMMON),

        // Tier 3 -ga (Nível 36 a 56 — Somente Fabricadas na Alquimia com Espólios Regionais/Nether • Raridade RARE)
        HASTEGA(3, 36, 10.0, BattleVisual.Style.HASTE, Rarity.RARE),
        CURAGA(3, 38, 12.0, BattleVisual.Style.CURAGA, Rarity.RARE),
        FIRAGA(3, 42, 13.0, BattleVisual.Style.FIRAGA, Rarity.RARE),
        BLIZZAGA(3, 44, 13.0, BattleVisual.Style.BLIZZAGA, Rarity.RARE),
        THUNDAGA(3, 46, 13.0, BattleVisual.Style.THUNDAGA, Rarity.RARE),
        WATERGA(3, 50, 13.0, BattleVisual.Style.WATERGA, Rarity.RARE),
        AEROGA(3, 52, 13.0, BattleVisual.Style.AEROGA, Rarity.RARE),
        DARKGA(3, 56, 13.0, BattleVisual.Style.DARKGA, Rarity.RARE),

        // Tier 4 Cataclísmicas (Nível 68 a 95 — Fabricadas com Relíquias do Nether & The End • Raridade EPIC)
        HOLY(4, 68, 18.0, BattleVisual.Style.HOLY, Rarity.EPIC),
        FLARE(4, 74, 18.0, BattleVisual.Style.FLARE, Rarity.EPIC),
        ARDOR(4, 82, 20.0, BattleVisual.Style.ARDOR, Rarity.EPIC),
        COLLAPSE(4, 88, 20.0, BattleVisual.Style.COLLAPSE, Rarity.EPIC),
        SCATHE(4, 95, 20.0, BattleVisual.Style.SCATHE, Rarity.EPIC),

        // Technicks
        LIBRA(1, 1, 12.0, BattleVisual.Style.TECHNIQUE, Rarity.UNCOMMON),
        STEAL(1, 2, 8.0, BattleVisual.Style.TECHNIQUE, Rarity.UNCOMMON);

        public final int tier;
        public final int minLevel;
        public final double radius;
        public final BattleVisual.Style visualStyle;
        public final Rarity spellRarity;

        SpellAction(int tier, int minLevel, double radius, BattleVisual.Style visualStyle, Rarity spellRarity) {
            this.tier = tier;
            this.minLevel = minLevel;
            this.radius = radius;
            this.visualStyle = visualStyle;
            this.spellRarity = spellRarity;
        }

        public String battleId() {
            return switch (this) {
                case AQUA -> "water_magic";
                default -> name().toLowerCase(Locale.ROOT);
            };
        }
    }

    private final SpellAction action;
    private final int cooldownTicks;

    public IvaliceSpellItem(
            Properties properties,
            IvaliceItemCategory category,
            String descriptionText,
            int gilValue,
            String obtainSource,
            String canonicalRef,
            Rarity itemRarity,
            SpellAction action,
            int cooldownTicks
    ) {
        super(properties, category, descriptionText, gilValue, obtainSource, canonicalRef, itemRarity);
        this.action = action;
        this.cooldownTicks = cooldownTicks;
    }

    public SpellAction getAction() {
        return action;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (!ZodiacJobs.permits(serverPlayer, getCategory())) {
                serverPlayer.sendOverlayMessage(Component.literal("Sua profissão atual não permite esta escola de magia."));
                return InteractionResult.FAIL;
            }
            var profile = IvalicePlayerData.get(serverPlayer);
            int playerLevel = profile.getBattleLevel();
            if (playerLevel < action.minLevel) {
                serverPlayer.sendSystemMessage(Component.literal(String.format(
                        "⚠ [GRIMÓRIO DE IVALICE] Você precisa alcançar o Nível %d (Atual: Nível %d) para dominar %s!",
                        action.minLevel, playerLevel, getCanonicalRef()
                )).withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            String spellId = action.battleId();
            if (action != SpellAction.LIBRA && action != SpellAction.STEAL && !profile.learnedSpells.contains(spellId)) {
                profile.learnedSpells.add(spellId);
                IvalicePlayerData.save(serverPlayer);
                serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.9F, 1.15F);
                serverPlayer.sendSystemMessage(Component.literal(String.format(
                        "📜 [MAGIA APRENDIDA!] Você inscreveu '%s' (Tier %d) em seu Grimório! Agora pode usá-la no Menu de Batalha e em campo!",
                        getCanonicalRef(), action.tier
                )).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }

            castSpell(serverLevel, serverPlayer);
            player.getCooldowns().addCooldown(player.getItemInHand(hand), cooldownTicks);
        }
        return InteractionResult.SUCCESS;
    }

    private void castSpell(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(action.radius);
        List<IvaliceMonsterEntity> monsters = level.getEntitiesOfClass(IvaliceMonsterEntity.class, area, LivingEntity::isAlive);

        BattleVisual.begin(level, player, action.visualStyle);

        switch (action) {
            case CURE, CURA, CURAGA -> {
                float heal = action == SpellAction.CURAGA ? 32.0F : (action == SpellAction.CURA ? 16.0F : 8.0F);
                for (Player ally : level.getEntitiesOfClass(Player.class, area)) {
                    ally.heal(heal);
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120 * action.tier, action.tier - 1));
                    if (action.tier >= 2) {
                        ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, action.tier - 2));
                    }
                    BattleVisual.impact(level, ally, action.visualStyle);
                }
                player.sendOverlayMessage(Component.literal("✦ [White Magick: " + action.name() + "] +" + (int) heal + " HP recuperado!").withStyle(ChatFormatting.GREEN));
            }
            case HASTEGA -> {
                for (Player ally : level.getEntitiesOfClass(Player.class, area)) {
                    ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 1));
                    BattleVisual.impact(level, ally, action.visualStyle);
                }
                player.sendOverlayMessage(Component.literal("✦ [Time Magick: Hastega] Velocidade ATB e Movimento ampliados!").withStyle(ChatFormatting.GOLD));
            }
            case LIBRA -> {
                if (!monsters.isEmpty()) {
                    BattleManager.castFieldSpell(player, monsters.getFirst(), "libra");
                } else {
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player)) {
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 300, 0));
                    }
                    player.sendOverlayMessage(Component.literal("👁 [Technick: Libra] Área escaneada!").withStyle(ChatFormatting.YELLOW));
                }
            }
            case STEAL -> {
                if (!monsters.isEmpty()) {
                    BattleManager.castFieldSpell(player, monsters.getFirst(), "steal");
                } else {
                    player.sendOverlayMessage(Component.literal("✋ [Technick: Steal] Aproxime-se de uma criatura de Ivalice para roubar seu espólio!").withStyle(ChatFormatting.YELLOW));
                }
            }
            default -> {
                if (!monsters.isEmpty()) {
                    int limit = action.tier >= 2 ? monsters.size() : 1;
                    for (int i = 0; i < limit; i++) {
                        BattleManager.castFieldSpell(player, monsters.get(i), action.battleId());
                    }
                } else {
                    // Mesmo sem monstros de Ivalice no raio, permite impactar outras criaturas ou o próprio cenário mirado!
                    List<LivingEntity> others = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && !(e instanceof Player));
                    if (!others.isEmpty()) {
                        float dmg = switch (action.tier) {
                            case 1 -> 12.0F;
                            case 2 -> 24.0F;
                            case 3 -> 45.0F;
                            default -> 85.0F;
                        };
                        for (LivingEntity target : others) {
                            BattleVisual.impact(level, target, action.visualStyle);
                            target.hurtServer(level, level.damageSources().indirectMagic(player, player), dmg);
                        }
                    } else {
                        HitResult hit = player.pick(Math.min(28.0, action.radius + 6.0), 0.0F, false);
                        BlockPos impactPos = BlockPos.containing(hit.getLocation());
                        BattleVisual.applyWorldImpact(level, impactPos, action.visualStyle);
                    }
                }
                player.sendOverlayMessage(Component.literal("✦ [Magick Tier " + action.tier + ": " + action.name() + "] Conjurada com impacto no mundo!")
                        .withStyle(action.tier >= 3 ? ChatFormatting.GOLD : ChatFormatting.RED));
            }
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.15F);
    }
}
