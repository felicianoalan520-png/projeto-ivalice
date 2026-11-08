package com.ivalice.registry;

import com.ivalice.IvaliceMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Abas do Modo Criativo para o Projeto Ivalice (Etapas 1, 2, 3, 4 e 5).
 */
public class IvaliceCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IvaliceMod.MODID);

    // 1. Regiões, Blocos Naturais & Teleporte
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IVALICE_WORLD_TAB =
            CREATIVE_MODE_TABS.register("ivalice_world_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ivalice.world"))
                    .withTabsBefore(CreativeModeTabs.NATURAL_BLOCKS)
                    .icon(() -> IvaliceItems.TELEPORT_STONE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(IvaliceItems.TELEPORT_STONE.get());
                        output.accept(IvaliceItems.CLAN_PRIMER.get());
                        output.accept(IvaliceItems.LICENSE_TOME.get());
                        output.accept(IvaliceItems.GAMBIT_SCROLL.get());
                        IvaliceBlocks.getRegisteredBlockItems().forEach(item -> output.accept(item.get()));
                    })
                    .build());

    // 2. Armas, Equipamentos Defensivos, Magicks & Technicks
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IVALICE_EQUIPMENT_TAB =
            CREATIVE_MODE_TABS.register("ivalice_equipment_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ivalice.equipment"))
                    .withTabsBefore(IVALICE_WORLD_TAB.getId())
                    .icon(() -> IvaliceItems.TOURNESOL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        IvaliceItems.getEquipmentTabItems().forEach(item -> output.accept(item.get()));
                    })
                    .build());

    // 3. Consumíveis, Espólios (Loot), Itens-Chave, Espers & Criaturas
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IVALICE_LOOT_AND_MONSTERS_TAB =
            CREATIVE_MODE_TABS.register("ivalice_loot_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ivalice.loot"))
                    .withTabsBefore(IVALICE_EQUIPMENT_TAB.getId())
                    .icon(() -> IvaliceItems.CLAN_PRIMER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        IvaliceItems.getLootAndConsumablesTabItems().forEach(item -> output.accept(item.get()));
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
