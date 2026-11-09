package com.ivalice.item;

import com.ivalice.client.ClientScreenHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
            Properties properties,
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
            if (player instanceof ServerPlayer serverPlayer) {
                if (!IvaliceReferenceItem.summon(serverPlayer, summonEsperId)) return InteractionResult.FAIL;
                player.getCooldowns().addCooldown(player.getItemInHand(hand), 600);
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) {
            ClientScreenHelper.openHubScreen(hubTab);
        }
        return InteractionResult.SUCCESS;
    }
}
