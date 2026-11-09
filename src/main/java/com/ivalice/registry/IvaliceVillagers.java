package com.ivalice.registry;

import com.ivalice.IvaliceMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;

public final class IvaliceVillagers {
    private static final DeferredRegister<VillagerType> TYPES =
            DeferredRegister.create(Registries.VILLAGER_TYPE, IvaliceMod.MODID);

    static {
        TYPES.register("dalmasca", VillagerType::new);
        TYPES.register("giza", VillagerType::new);
        TYPES.register("archades", VillagerType::new);
        TYPES.register("balfonheim", VillagerType::new);
        TYPES.register("bhujerba", VillagerType::new);
    }

    private IvaliceVillagers() {}

    public static void register(IEventBus eventBus) {
        TYPES.register(eventBus);
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getTarget() instanceof Villager villager) {
            enrichVillagerOffers(villager, player);
        } else if (event.getTarget() instanceof WanderingTrader trader) {
            enrichWandererOffers(trader);
        }
    }

    private static void enrichVillagerOffers(Villager villager, ServerPlayer player) {
        MerchantOffers offers = villager.getOffers();
        int level = Math.max(1, villager.getVillagerData().level());
        var profHolder = villager.getVillagerData().profession();

        // 1. Clérigos e Bibliotecários — Vendem APENAS Pergaminhos de Magia até o Tier 2!
        // (Magias Tier 3 -ga e Tier 4 Cataclísmicas devem obrigatoriamente ser fabricadas via Alquimia com espólios!)
        if (profHolder.is(VillagerProfession.CLERIC) || profHolder.is(VillagerProfession.LIBRARIAN)) {
            // Tier 1 (Nível 1 a 8)
            addSell(offers, IvaliceItems.MAGICK_CURE.get(), 2, 1, 8, 4);
            addSell(offers, IvaliceItems.MAGICK_FIRE.get(), 2, 1, 8, 4);
            addSell(offers, IvaliceItems.MAGICK_BLIZZARD.get(), 2, 1, 8, 4);
            addSell(offers, IvaliceItems.MAGICK_THUNDER.get(), 2, 1, 8, 4);
            addSellById(offers, "tza_aqua", 3, 1, 8, 5);
            addSellById(offers, "tza_aero", 3, 1, 8, 5);
            addSellById(offers, "tza_dark", 3, 1, 8, 5);

            // Tier 2 (Nível 14 a 28 — Limite máximo vendido por mercadores!)
            if (level >= 2) {
                addSell(offers, IvaliceItems.TECHNICK_LIBRA.get(), 4, 1, 6, 8);
                addSell(offers, IvaliceItems.MAGICK_CURA.get(), 7, 1, 5, 12);
                addSellById(offers, "tza_fira", 7, 1, 6, 10);
                addSellById(offers, "tza_blizzara", 7, 1, 6, 10);
                addSellById(offers, "tza_thundara", 8, 1, 6, 10);
            }
            if (level >= 3) {
                addSellById(offers, "tza_aquara", 9, 1, 5, 14);
                addSellById(offers, "tza_aerora", 9, 1, 5, 14);
                addSellById(offers, "tza_darkra", 10, 1, 5, 16);
            }
            player.sendOverlayMessage(Component.literal("📜 [Mercador Arcano] Grimórios até Tier 2 disponíveis! (Tier 3 -ga e Tier 4 exigem fabricação em Alquimia & Bazaar)").withStyle(ChatFormatting.GOLD));
        }

        // 2. Armeiros e Ferreiros — Armas e Armaduras Básicas/Regionais (NUNCA itens lendários!)
        if (profHolder.is(VillagerProfession.WEAPONSMITH) || profHolder.is(VillagerProfession.TOOLSMITH)) {
            addSell(offers, IvaliceItems.MYTHRIL_SWORD.get(), 3, 1, 6, 4);
            addSell(offers, IvaliceItems.ALTAIR.get(), 4, 1, 6, 5);
            addSell(offers, IvaliceItems.CHERRY_STAFF.get(), 6, 1, 5, 8);
            addSell(offers, IvaliceItems.GLADIUS.get(), 8, 1, 4, 10);
            if (level >= 2) {
                addSell(offers, IvaliceItems.FRANCISC.get(), 11, 1, 4, 15);
                addSell(offers, IvaliceItems.ZORLIN_SHAPE.get(), 15, 1, 3, 20);
            }
            addBuy(offers, IvaliceItems.FIRE_STONE.get(), 4, 1, 12, 3);
            addBuy(offers, IvaliceItems.DARK_MAGICITE.get(), 2, 2, 10, 8);
        }

        if (profHolder.is(VillagerProfession.ARMORER) || profHolder.is(VillagerProfession.LEATHERWORKER)) {
            addSell(offers, IvaliceItems.LEATHER_CAP.get(), 2, 1, 8, 3);
            addSell(offers, IvaliceItems.LEATHER_CLOTHING.get(), 3, 1, 8, 4);
            addSell(offers, IvaliceItems.ESCUTCHEON.get(), 3, 1, 6, 4);
            addSell(offers, IvaliceItems.IRON_HELM.get(), 5, 1, 6, 8);
            addSell(offers, IvaliceItems.SCALE_ARMOR.get(), 6, 1, 5, 10);
            if (level >= 2) {
                addSell(offers, IvaliceItems.MAGES_HABIT.get(), 8, 1, 4, 12);
                addSell(offers, IvaliceItems.AEGIS_SHIELD.get(), 14, 1, 3, 18);
            }
            addBuy(offers, IvaliceItems.WOLF_PELT.get(), 6, 1, 16, 2);
            addBuy(offers, IvaliceItems.GIZA_RABBIT_FUR.get(), 6, 1, 16, 2);
        }

        // 3. Fazendeiros, Açougueiros e Pescadores — Consumíveis e Compra de Espólios
        if (profHolder.is(VillagerProfession.FARMER) || profHolder.is(VillagerProfession.BUTCHER)
                || profHolder.is(VillagerProfession.FISHERMAN) || profHolder.is(VillagerProfession.SHEPHERD)) {
            addSell(offers, IvaliceItems.POTION.get(), 1, 2, 16, 2);
            addSell(offers, IvaliceItems.ANTIDOTE.get(), 1, 2, 16, 2);
            addSell(offers, IvaliceItems.GYSAHL_GREENS.get(), 1, 3, 16, 3);
            addSell(offers, IvaliceItems.HI_POTION.get(), 3, 2, 12, 6);
            addSell(offers, IvaliceItems.PHOENIX_DOWN.get(), 4, 1, 10, 8);
            addSell(offers, IvaliceItems.REMEDY.get(), 5, 1, 10, 10);
            addSell(offers, IvaliceItems.ETHER.get(), 6, 1, 8, 12);
            if (level >= 3) {
                addSell(offers, IvaliceItems.X_POTION.get(), 8, 1, 8, 15);
            }
            addBuy(offers, IvaliceItems.CACTUS_FRUIT.get(), 5, 1, 16, 2);
            addBuy(offers, IvaliceItems.SMALL_FEATHER.get(), 5, 1, 16, 2);
            addBuy(offers, IvaliceItems.FISH_SCALE.get(), 5, 1, 16, 2);
        }
    }

    private static void enrichWandererOffers(AbstractVillager trader) {
        MerchantOffers offers = trader.getOffers();
        addSell(offers, IvaliceItems.TELEPORT_STONE.get(), 2, 2, 8, 4);
        addSell(offers, IvaliceItems.HI_POTION.get(), 2, 2, 10, 4);
        addSell(offers, IvaliceItems.ETHER.get(), 4, 1, 6, 6);
        addSellById(offers, "tza_aerora", 8, 1, 3, 12);
        addSellById(offers, "tza_aquara", 8, 1, 3, 12);
        addSellById(offers, "tza_darkra", 9, 1, 3, 14);
        addSell(offers, IvaliceItems.GOLDEN_AMULET.get(), 16, 1, 2, 20);
    }

    private static void addSellById(MerchantOffers offers, String id, int emeralds, int count, int maxUses, int xp) {
        Item item = IvaliceItems.getItemById(id);
        if (item != null) {
            addSell(offers, item, emeralds, count, maxUses, xp);
        }
    }

    private static void addSell(MerchantOffers offers, Item item, int emeralds, int count, int maxUses, int xp) {
        if (offers == null || item == null) return;
        for (MerchantOffer existing : offers) {
            if (existing.getResult().is(item)) return;
        }
        offers.add(new MerchantOffer(
                new ItemCost(Items.EMERALD, emeralds),
                new ItemStack(item, count),
                maxUses, xp, 0.05F
        ));
    }

    private static void addBuy(MerchantOffers offers, Item lootItem, int lootCount, int emeraldsOut, int maxUses, int xp) {
        if (offers == null || lootItem == null) return;
        for (MerchantOffer existing : offers) {
            if (existing.getBaseCostA().is(lootItem)) return;
        }
        offers.add(new MerchantOffer(
                new ItemCost(lootItem, lootCount),
                Optional.empty(),
                new ItemStack(Items.EMERALD, emeraldsOut),
                maxUses, xp, 0.05F
        ));
    }
}
