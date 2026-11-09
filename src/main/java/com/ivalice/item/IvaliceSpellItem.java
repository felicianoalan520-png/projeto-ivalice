package com.ivalice.item;

import com.ivalice.combat.BattleManager;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.job.ZodiacJobs;
import net.minecraft.ChatFormatting;
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

import java.util.List;

/**
 * Magicks e Technicks conjuráveis de Final Fantasy XII (Cure, Cura, Fire, Thunder, Blizzard, Hastega, Libra, Steal).
 */
public class IvaliceSpellItem extends IvaliceCatalogItem {
    public enum SpellAction {
        CURE,
        CURA,
        FIRE,
        THUNDER,
        BLIZZARD,
        HASTEGA,
        LIBRA,
        STEAL
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
                serverPlayer.sendOverlayMessage(Component.literal("Sua profissão não permite esta magia."));
                return InteractionResult.FAIL;
            }
            castSpell(serverLevel, serverPlayer);
            player.getCooldowns().addCooldown(player.getItemInHand(hand), cooldownTicks);
        }
        return InteractionResult.SUCCESS;
    }

    private void castSpell(ServerLevel level, ServerPlayer player) {
        AABB area = player.getBoundingBox().inflate(12.0);
        List<IvaliceMonsterEntity> monsters = level.getEntitiesOfClass(IvaliceMonsterEntity.class, area, LivingEntity::isAlive);

        switch (action) {
            case CURE -> {
                player.heal(8.0F);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0));
                player.sendOverlayMessage(Component.literal("✦ [White Magick: Cure] +8 HP recuperado!").withStyle(ChatFormatting.GREEN));
            }
            case CURA -> {
                for (Player ally : level.getEntitiesOfClass(Player.class, area)) {
                    ally.heal(16.0F);
                    ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 180, 1));
                    ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 0));
                }
                player.sendOverlayMessage(Component.literal("✦ [White Magick: Cura] +16 HP e Regeneração em área!").withStyle(ChatFormatting.AQUA));
            }
            case HASTEGA -> {
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 1));
                player.sendOverlayMessage(Component.literal("✦ [Time Magick: Hastega] Velocidade ATB e Movimento duplicados!").withStyle(ChatFormatting.GOLD));
            }
            case FIRE -> {
                if (!monsters.isEmpty()) {
                    BattleManager.castFieldSpell(player, monsters.getFirst(), "fire");
                } else {
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && !(e instanceof Player))) {
                        target.setRemainingFireTicks(100);
                        target.hurtServer(level, level.damageSources().magic(), 10.0F);
                    }
                    player.sendOverlayMessage(Component.literal("🔥 [Black Magick: Fire] Chamas conjuradas ao redor!").withStyle(ChatFormatting.RED));
                }
            }
            case THUNDER -> {
                if (!monsters.isEmpty()) {
                    BattleManager.castFieldSpell(player, monsters.getFirst(), "thunder");
                } else {
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && !(e instanceof Player))) {
                        target.hurtServer(level, level.damageSources().magic(), 12.0F);
                    }
                    player.sendOverlayMessage(Component.literal("⚡ [Black Magick: Thunder] Relâmpagos conjurados!").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            case BLIZZARD -> {
                if (!monsters.isEmpty()) {
                    BattleManager.castFieldSpell(player, monsters.getFirst(), "blizzard");
                } else {
                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && !(e instanceof Player))) {
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 2));
                        target.hurtServer(level, level.damageSources().magic(), 11.0F);
                    }
                    player.sendOverlayMessage(Component.literal("❄ [Black Magick: Blizzard] Cristais de gelo conjurados!").withStyle(ChatFormatting.AQUA));
                }
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
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.15F);
    }
}
