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
 * Abas do Modo Criativo para o Projeto Ivalice.
 */
public class IvaliceCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IvaliceMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> IVALICE_WORLD_TAB =
            CREATIVE_MODE_TABS.register("ivalice_world_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ivalice.world"))
                    .withTabsBefore(CreativeModeTabs.NATURAL_BLOCKS)
                    .icon(() -> IvaliceItems.TELEPORT_STONE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(IvaliceItems.TELEPORT_STONE.get());
                        IvaliceBlocks.getRegisteredBlockItems().forEach(item -> output.accept(item.get()));
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
