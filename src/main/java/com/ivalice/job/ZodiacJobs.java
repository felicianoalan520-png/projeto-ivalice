package com.ivalice.job;

import com.ivalice.combat.BattleManager;
import com.ivalice.data.IvalicePlayerData;
import com.ivalice.item.IvaliceCatalogItem;
import com.ivalice.item.IvaliceItemCategory;
import com.ivalice.registry.IvaliceItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;

public final class ZodiacJobs {
    public static final int SECOND_JOB_LEVEL = 5;

    private ZodiacJobs() {}

    public static boolean permits(ServerPlayer player, IvaliceItemCategory category) {
        var profile = IvalicePlayerData.get(player);
        if (profile.jobs.isEmpty() || category == null) return true;
        return switch (category) {
            case AMMUNITION, CONSUMABLE, LOOT_MATERIAL, KEY_ITEM, DEFENSE_ACCESSORY, TECHNICK, ESPER_GLYPH -> true;
            default -> profile.jobs.stream().map(ZodiacJob::byId).anyMatch(job -> job != null && job.permits(category));
        };
    }

    public static boolean permits(ServerPlayer player, ItemStack stack) {
        return !(stack.getItem() instanceof IvaliceCatalogItem item) || permits(player, item.getCategory());
    }

    public static boolean choose(ServerPlayer player, String id) {
        var profile = IvalicePlayerData.get(player);
        ZodiacJob job = ZodiacJob.byId(id);
        if (BattleManager.isInBattle(player) || job == null || profile.jobs.contains(id)) return false;
        int slots = profile.getBattleLevel() >= SECOND_JOB_LEVEL ? 2 : 1;
        if (profile.jobs.size() >= slots) {
            player.sendSystemMessage(Component.literal(slots == 1
                    ? "A segunda profissão abre no nível " + SECOND_JOB_LEVEL + "."
                    : "As duas profissões estão definidas. Use um Selo Zodiacal para recomeçar."));
            return false;
        }
        profile.jobs.add(id);
        update(player);
        player.sendSystemMessage(Component.literal("Profissão escolhida: " + job.label + "."));
        return true;
    }

    public static boolean reset(ServerPlayer player) {
        var profile = IvalicePlayerData.get(player);
        if (BattleManager.isInBattle(player) || profile.jobs.isEmpty()) return false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item.is(IvaliceItems.ZODIAC_RESET_SEAL.get())) {
                item.shrink(1);
                profile.jobs.clear();
                profile.unlockedLicenses.clear();
                profile.addStarterLicenses();
                update(player);
                player.sendSystemMessage(Component.literal("Profissões e licenças reiniciadas. Escolha novamente no menu."));
                return true;
            }
        }
        player.sendSystemMessage(Component.literal("Você precisa de um Selo Zodiacal."));
        return false;
    }

    private static void update(ServerPlayer player) {
        IvalicePlayerData.applyLicenseAttributes(player);
        checkEquipment(player);
        IvalicePlayerData.save(player);
    }

    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !permits(player, player.getMainHandItem())) {
            event.setCanceled(true);
            player.sendOverlayMessage(Component.literal("Sua profissão não pode usar esta arma."));
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive()) {
            checkEquipment(player);
        }
    }

    private static void checkEquipment(ServerPlayer player) {
        if (IvalicePlayerData.get(player).jobs.isEmpty()) return;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot == EquipmentSlot.MAINHAND) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (!permits(player, stack)) {
                player.setItemSlot(slot, ItemStack.EMPTY);
                player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
                player.sendOverlayMessage(Component.literal("Equipamento devolvido: incompatível com suas profissões."));
            }
        }
    }
}
