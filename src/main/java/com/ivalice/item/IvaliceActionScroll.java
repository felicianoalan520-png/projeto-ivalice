package com.ivalice.item;

import com.ivalice.combat.BattleManager;
import com.ivalice.data.IvalicePlayerData;
import com.ivalice.job.ZodiacJobs;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public final class IvaliceActionScroll extends IvaliceCatalogItem {
    private final BattleManager.Action action;

    public IvaliceActionScroll(Properties properties, IvaliceItemCategory category, int price, String source,
                              Rarity rarity, BattleManager.Action action) {
        super(properties, category, "Aprenda e use pelo menu de batalha • Nível " + action.minLevel,
                price, source, action.label, rarity);
        this.action = action;
    }

    public BattleManager.Action action() {
        return action;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        var profile = IvalicePlayerData.get(serverPlayer);
        if (profile.getBattleLevel() < action.minLevel || !ZodiacJobs.permits(serverPlayer, action.school)) {
            serverPlayer.sendOverlayMessage(Component.literal(action.label + " exige nível " + action.minLevel
                    + " e a profissão correspondente."));
            return InteractionResult.FAIL;
        }
        if (profile.learnedSpells.add(action.id())) {
            IvalicePlayerData.save(serverPlayer);
            serverPlayer.sendSystemMessage(Component.literal("Aprendeu " + action.label + "."));
        }
        if (BattleManager.castFieldAction(serverPlayer, action)) {
            player.getCooldowns().addCooldown(player.getItemInHand(hand), 70);
        }
        return InteractionResult.SUCCESS;
    }
}
