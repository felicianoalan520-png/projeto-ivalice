package com.ivalice.registry;

import com.ivalice.IvaliceMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class IvaliceVillagers {
    private static final DeferredRegister<VillagerType> TYPES =
            DeferredRegister.create(Registries.VILLAGER_TYPE, IvaliceMod.MODID);

    static {
        TYPES.register("dalmasca", VillagerType::new);
        TYPES.register("giza", VillagerType::new);
    }

    private IvaliceVillagers() {}

    public static void register(IEventBus eventBus) {
        TYPES.register(eventBus);
    }
}
