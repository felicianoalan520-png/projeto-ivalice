package com.ivalice.registry;

import com.ivalice.IvaliceMod;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Infraestrutura de registro de itens de Final Fantasy XII.
 * Na Etapa 1, registra os BlockItems naturais e o Cristal de Teleporte como ícone da aba criativa.
 * Preparado para receber o catálogo em lotes na Etapa 2 (Armas, Armaduras, Consumíveis, Loot/Espólios, Magicks/Technicks).
 */
public class IvaliceItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(IvaliceMod.MODID);

    // Ícone representativo do Projeto Ivalice (Teleport Stone / Cristal de Teleporte)
    public static final DeferredItem<Item> TELEPORT_STONE = ITEMS.registerSimpleItem(
            "teleport_stone",
            props -> props.rarity(Rarity.RARE).stacksTo(64)
    );

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
