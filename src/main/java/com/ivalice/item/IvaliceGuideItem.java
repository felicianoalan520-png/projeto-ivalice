package com.ivalice.item;

import com.ivalice.client.ClientScreenHelper;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.registry.IvaliceEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/**
 * Item interativo de Final Fantasy XII que abre uma aba específica do Hub de Ivalice
 * (Cristal de Teleporte, Bestiário Clan Primer, Tabuleiro de Licenças, Gambits ou Bazaar)
 * ou invoca um Esper lendário (Belias / Mateus).
 */
public class IvaliceGuideItem extends IvaliceCatalogItem {
    private final int hubTab;
    private final String summonEsperId;

    public IvaliceGuideItem(
            Item.Properties properties,
            IvaliceItemCategory category,
            String descriptionText,
            int gilValue,
            String obtainSource,
            String canonicalRef,
            Rarity itemRarity,
            int hubTab,
            String summonEsperId
    ) {
        super(properties, category, descriptionText, gilValue, obtainSource, canonicalRef, itemRarity);
        this.hubTab = hubTab;
        this.summonEsperId = summonEsperId;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (summonEsperId != null && !summonEsperId.isEmpty()) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                EntityType<IvaliceMonsterEntity> esperType = IvaliceEntities.getById(summonEsperId);
                BlockPos spawnPos = player.blockPosition().relative(player.getDirection(), 4);
                IvaliceMonsterEntity esper = esperType.create(serverLevel, EntitySpawnReason.EVENT);
                if (esper != null) {
                    esper.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5);
                    serverLevel.addFreshEntity(esper);
                    serverLevel.playSound(null, spawnPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.9F, 1.0F);
                    serverPlayer.sendSystemMessage(Component.literal(
                            "✦ [INVOCAÇÃO DE ESPER] O selo místico brilha e invoca " + esper.getStats().displayName() + "!"
                    ).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                    player.getCooldowns().addCooldown(player.getItemInHand(hand), 100);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) {
            ClientScreenHelper.openHubScreen(hubTab);
        }
        return InteractionResult.SUCCESS;
    }
}
