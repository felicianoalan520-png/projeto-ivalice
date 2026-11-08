package com.ivalice.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Consumíveis canônicos de Final Fantasy XII (Potion, Hi-Potion, X-Potion, Ether, Elixir, Phoenix Down, Antidote, Remedy, Gysahl Greens).
 */
public class IvaliceConsumableItem extends IvaliceCatalogItem {
    private final float healAmount;
    private final boolean curesNegative;
    private final List<MobEffectInstance> bonusEffects;

    public IvaliceConsumableItem(
            Properties properties,
            String descriptionText,
            int gilValue,
            String obtainSource,
            String canonicalRef,
            Rarity itemRarity,
            float healAmount,
            boolean curesNegative,
            List<MobEffectInstance> bonusEffects
    ) {
        super(properties, IvaliceItemCategory.CONSUMABLE, descriptionText, gilValue, obtainSource, canonicalRef, itemRarity);
        this.healAmount = healAmount;
        this.curesNegative = curesNegative;
        this.bonusEffects = bonusEffects;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (healAmount > 0) {
                serverPlayer.heal(healAmount);
            }
            if (curesNegative) {
                serverPlayer.removeEffect(MobEffects.POISON);
                serverPlayer.removeEffect(MobEffects.WITHER);
                serverPlayer.removeEffect(MobEffects.BLINDNESS);
                serverPlayer.removeEffect(MobEffects.DARKNESS);
                serverPlayer.removeEffect(MobEffects.SLOWNESS);
                serverPlayer.removeEffect(MobEffects.WEAKNESS);
                serverPlayer.removeEffect(MobEffects.MINING_FATIGUE);
            }
            if (bonusEffects != null) {
                for (MobEffectInstance effect : bonusEffects) {
                    serverPlayer.addEffect(new MobEffectInstance(effect));
                }
            }
            level.playSound(null, serverPlayer.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.25F);
            serverPlayer.sendOverlayMessage(Component.literal(
                    "✚ Usou " + this.getName(player.getItemInHand(hand)).getString() + " (+ " + (int) healAmount + " HP)!"
            ).withStyle(ChatFormatting.GREEN));

            if (!player.getAbilities().instabuild) {
                player.getItemInHand(hand).shrink(1);
            }
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 15);
        }
        return InteractionResult.SUCCESS;
    }
}
