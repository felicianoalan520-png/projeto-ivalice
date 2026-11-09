package com.ivalice.combat;

import com.ivalice.IvaliceMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class IvaliceStatuses {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, IvaliceMod.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> OIL = harmful("oil", 0x9A6031);
    public static final DeferredHolder<MobEffect, MobEffect> SILENCE = harmful("silence", 0xB7B1A2);
    public static final DeferredHolder<MobEffect, MobEffect> SLEEP = freeze("sleep", 0x8E79B8);
    public static final DeferredHolder<MobEffect, MobEffect> STOP = freeze("stop", 0x6C8AAC);
    public static final DeferredHolder<MobEffect, MobEffect> DISABLE = harmful("disable", 0xAD825A);
    public static final DeferredHolder<MobEffect, MobEffect> IMMOBILIZE = freeze("immobilize", 0x745C42);
    public static final DeferredHolder<MobEffect, MobEffect> CONFUSE = harmful("confuse", 0xA56EC8);
    public static final DeferredHolder<MobEffect, MobEffect> DISEASE = harmful("disease", 0x748242);
    public static final DeferredHolder<MobEffect, MobEffect> PETRIFY = freeze("petrify", 0x949087);
    public static final DeferredHolder<MobEffect, MobEffect> DOOM = EFFECTS.register("doom",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x4A3B61) {
                @Override
                public boolean shouldApplyEffectTickThisTick(int ticks, int amplifier) {
                    return ticks == 1;
                }

                @Override
                public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
                    if (entity instanceof com.ivalice.entity.IvaliceMonsterEntity mob
                            && !mob.getStats().rankType().equals("Normal")) return false;
                    entity.hurtServer(level, level.damageSources().magic(), entity.getMaxHealth() * 4);
                    return false;
                }
            });
    public static final DeferredHolder<MobEffect, MobEffect> REVERSE = beneficial("reverse", 0xCB8FC0);
    public static final DeferredHolder<MobEffect, MobEffect> REFLECT = beneficial("reflect", 0xA8D3FA);
    public static final DeferredHolder<MobEffect, MobEffect> FAITH = beneficial("faith", 0xDFB1F2);
    public static final DeferredHolder<MobEffect, MobEffect> SHELL = beneficial("shell", 0x9AB7DD);
    public static final DeferredHolder<MobEffect, MobEffect> DECOY = beneficial("decoy", 0xF0AC64);
    public static final DeferredHolder<MobEffect, MobEffect> RERAISE = beneficial("reraise", 0xEBCC85);

    private IvaliceStatuses() {}

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private static DeferredHolder<MobEffect, MobEffect> harmful(String id, int color) {
        return EFFECTS.register(id, () -> new MobEffect(MobEffectCategory.HARMFUL, color) {});
    }

    private static DeferredHolder<MobEffect, MobEffect> freeze(String id, int color) {
        return EFFECTS.register(id, () -> new MobEffect(MobEffectCategory.HARMFUL, color) {}
                .addAttributeModifier(Attributes.MOVEMENT_SPEED, Identifier.fromNamespaceAndPath("ivalice", id),
                        -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static DeferredHolder<MobEffect, MobEffect> beneficial(String id, int color) {
        return EFFECTS.register(id, () -> new MobEffect(MobEffectCategory.BENEFICIAL, color) {});
    }

    public static boolean unableToAct(LivingEntity entity) {
        return entity.hasEffect(SLEEP) || entity.hasEffect(STOP) || entity.hasEffect(DISABLE) || entity.hasEffect(PETRIFY);
    }

    public static void onDeath(LivingDeathEvent event) {
        var entity = event.getEntity();
        if (!entity.hasEffect(RERAISE)) return;
        var ward = entity.getEffect(RERAISE);
        entity.removeEffect(RERAISE);
        entity.setHealth(ward != null && ward.getAmplifier() > 0 ? entity.getMaxHealth() : Math.max(1, entity.getMaxHealth() * 0.25F));
        event.setCanceled(true);
    }

    public static void ward(LivingEntity entity, boolean full) {
        entity.addEffect(new MobEffectInstance(RERAISE, 1200, full ? 1 : 0));
    }
}
