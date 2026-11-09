package com.ivalice.item;

import com.ivalice.combat.BattleManager;
import com.ivalice.combat.IvaliceConsumables;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public final class IvaliceUseItem extends IvaliceCatalogItem {
    private final IvaliceConsumables.Use effect;

    public IvaliceUseItem(Properties properties, String description, int price, String source, String name,
                          Rarity rarity, IvaliceConsumables.Use effect) {
        super(properties, IvaliceItemCategory.CONSUMABLE, description, price, source, name, rarity);
        this.effect = effect;
    }

    public IvaliceConsumables.Use effect() {
        return effect;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        var stack = player.getItemInHand(hand);
        if (!BattleManager.useFieldItem(serverPlayer, stack, effect)) return InteractionResult.FAIL;
        player.getCooldowns().addCooldown(stack, 20);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
