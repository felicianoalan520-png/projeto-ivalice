package com.ivalice.registry;

import com.ivalice.IvaliceMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Infraestrutura de registro de entidades para a Etapa 3 (Monstros de Ivalice, Chefes e Espers com modelos GeckoLib).
 */
public class IvaliceEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, IvaliceMod.MODID);

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
