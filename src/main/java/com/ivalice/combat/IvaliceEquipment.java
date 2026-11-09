package com.ivalice.combat;

import com.ivalice.IvaliceMod;
import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.item.IvaliceCatalogItem;
import com.ivalice.item.IvaliceItemCategory;
import com.ivalice.job.ZodiacJobs;
import com.ivalice.registry.IvaliceTzaCatalog;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;
import java.util.Optional;

public final class IvaliceEquipment {
    private IvaliceEquipment() {}

    public static Item.Properties configure(Item.Properties properties, String id, IvaliceItemCategory category) {
        var attributes = ItemAttributeModifiers.builder();
        if (category == IvaliceItemCategory.DEFENSE_SHIELD) {
            var entry = IvaliceTzaCatalog.getEntry(id);
            int rank = entry == null ? 10 : entry.rank();
            double defense = Math.min(6, 1 + rank * .23);
            add(attributes, id, Attributes.ARMOR, defense);
            return properties.attributes(attributes.build()).durability(170 + rank * 20)
                    .equippableUnswappable(EquipmentSlot.OFFHAND)
                    .delayedComponent(DataComponents.BLOCKS_ATTACKS, context -> new BlocksAttacks(
                            .2F, 1F, List.of(new BlocksAttacks.DamageReduction(90F, Optional.empty(), 0F, .9F)),
                            new BlocksAttacks.ItemDamageFunction(3F, 1F, 1F),
                            Optional.of(context.getOrThrow(DamageTypeTags.BYPASSES_SHIELD)),
                            Optional.of(SoundEvents.SHIELD_BLOCK), Optional.of(SoundEvents.SHIELD_BREAK)))
                    .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK);
        }
        if (category != IvaliceItemCategory.DEFENSE_ACCESSORY) return properties;
        double defense = switch (id) {
            case "tza_ring_of_renewal" -> 6;
            case "tza_gillie_boots", "tza_amber_armlet" -> 3;
            case "tza_rose_corsage", "tza_berserker_bracers", "tza_opal_ring", "tza_steel_poleyns" -> 2;
            case "tza_gauntlets", "tza_steel_gorget", "tza_leather_gorget", "tza_black_belt", "tza_pheasant_netsuke",
                    "ribbon", "dawn_shard", "goddess_magicite" -> 1;
            default -> 0;
        };
        double hp = switch (id) {
            case "tza_quasimodo_boots" -> 20;
            case "tza_magick_gloves" -> 8;
            case "tza_orrachea_armlet", "tza_blazer_gloves", "tza_ring_of_renewal" -> 4;
            default -> 0;
        };
        double speed = switch (id) {
            case "tza_germinas_boots" -> .044;
            case "tza_sash" -> .025;
            case "tza_gillie_boots", "tza_indigo_pendant" -> .015;
            case "tza_turtleshell_choker", "tza_jade_collar", "tza_cat_ear_hood", "tza_amber_armlet",
                    "tza_winged_boots", "tza_thief_s_cuffs" -> .008;
            default -> 0;
        };
        double attack = switch (id) {
            case "hermes_sandals" -> 3;
            case "tza_power_armlet", "tza_battle_harness", "tza_gillie_boots", "tza_firefly" -> 1.5;
            default -> 0;
        };
        add(attributes, id, Attributes.ARMOR, defense);
        add(attributes, id, Attributes.MAX_HEALTH, hp);
        add(attributes, id, Attributes.MOVEMENT_SPEED, speed);
        add(attributes, id, Attributes.ATTACK_DAMAGE, attack);
        return properties.attributes(attributes.build());
    }

    private static void add(ItemAttributeModifiers.Builder builder, String id, Holder<Attribute> attribute, double amount) {
        if (amount == 0) return;
        String stat = attribute.unwrapKey().orElseThrow().identifier().getPath();
        builder.add(attribute, new AttributeModifier(Identifier.fromNamespaceAndPath(IvaliceMod.MODID, id + "/" + stat),
                amount, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.OFFHAND);
    }

    public static boolean has(ServerPlayer player, String id) {
        return offhandId(player).equals(id);
    }

    private static String offhandId(ServerPlayer player) {
        return BuiltInRegistries.ITEM.getKey(player.getOffhandItem().getItem()).getPath();
    }

    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0) return;
        switch (offhandId(player)) {
            case "ribbon" -> {
                IvaliceActionEffects.clear(player, MobEffectCategory.HARMFUL);
                status(player, MobEffects.REGENERATION, 0);
            }
            case "tza_ring_of_renewal" -> status(player, MobEffects.REGENERATION, 0);
            case "tza_ruby_ring" -> status(player, IvaliceStatuses.REFLECT, 0);
            case "hermes_sandals" -> status(player, MobEffects.SPEED, 1);
            case "tza_winged_boots" -> status(player, MobEffects.SLOW_FALLING, 0);
            case "tza_berserker_bracers" -> {
                status(player, MobEffects.STRENGTH, 1);
                status(player, IvaliceStatuses.SILENCE, 0);
            }
            case "bubble_belt" -> {
                if (!player.hasEffect(MobEffects.HEALTH_BOOST)) {
                    int power = Math.clamp((int) player.getMaxHealth() / 4 - 1, 0, 40);
                    IvaliceActionEffects.effect(player, MobEffects.HEALTH_BOOST, 36, power);
                    player.heal((power + 1) * 4);
                } else if (player.getEffect(MobEffects.HEALTH_BOOST).getDuration() < 36) {
                    status(player, MobEffects.HEALTH_BOOST, player.getEffect(MobEffects.HEALTH_BOOST).getAmplifier());
                }
            }
            case "tza_bangle" -> {
                for (var monster : player.level().getEntitiesOfClass(IvaliceMonsterEntity.class, player.getBoundingBox().inflate(9),
                        mob -> mob.isAlive() && player.hasLineOfSight(mob))) status(monster, MobEffects.GLOWING, 0);
            }
            case "tza_rose_corsage" -> player.removeEffect(IvaliceStatuses.SILENCE);
            case "tza_black_belt" -> IvaliceActionEffects.remove(player, IvaliceStatuses.DISABLE, IvaliceStatuses.IMMOBILIZE);
            case "tza_nishijin_belt" -> player.removeEffect(IvaliceStatuses.SLEEP);
            case "tza_bowline_sash" -> player.removeEffect(IvaliceStatuses.CONFUSE);
            case "tza_gillie_boots" -> player.removeEffect(IvaliceStatuses.OIL);
            case "tza_power_armlet" -> player.removeEffect(IvaliceStatuses.STOP);
            case "tza_germinas_boots" -> player.removeEffect(IvaliceStatuses.IMMOBILIZE);
            case "tza_fuzzy_miter" -> player.removeEffect(IvaliceStatuses.PETRIFY);
            case "tza_sash" -> player.removeEffect(MobEffects.SLOWNESS);
            case "tza_quasimodo_boots" -> player.removeEffect(MobEffects.WITHER);
            case "tza_tourmaline_ring" -> IvaliceActionEffects.remove(player, MobEffects.WITHER, MobEffects.POISON);
            case "tza_manufactured_nethicite", "goddess_magicite", "dawn_shard" -> {
                status(player, IvaliceStatuses.SHELL, 0);
                status(player, IvaliceStatuses.SILENCE, 0);
            }
            default -> {}
        }
    }

    private static void status(net.minecraft.world.entity.LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
        IvaliceActionEffects.effect(entity, effect, 35, amplifier);
    }

    public static String ammoFamily(IvaliceItemCategory category) {
        return switch (category) {
            case WEAPON_BOW -> "arrows";
            case WEAPON_CROSSBOW -> "bolts";
            case WEAPON_GUN -> "shot";
            case WEAPON_HAND_BOMB -> "bombs";
            default -> "";
        };
    }

    public static boolean ranged(ItemStack weapon) {
        return weapon.getItem() instanceof IvaliceCatalogItem item && !ammoFamily(item.getCategory()).isEmpty();
    }

    static ItemStack ammunition(ServerPlayer player, ItemStack weapon) {
        if (!(weapon.getItem() instanceof IvaliceCatalogItem item)) return ItemStack.EMPTY;
        String family = ammoFamily(item.getCategory());
        if (family.isEmpty()) return ItemStack.EMPTY;
        var inventory = player.getInventory();
        if (isAmmo(player.getOffhandItem(), family)) return player.getOffhandItem();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isAmmo(stack, family)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean isAmmo(ItemStack stack, String family) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IvaliceCatalogItem item)
                || item.getCategory() != IvaliceItemCategory.AMMUNITION) return false;
        String id = BuiltInRegistries.ITEM.getKey(item).getPath();
        return id.endsWith("_" + family) || ("bombs".equals(family) && "tza_castellanos".equals(id));
    }

    public static boolean useRanged(ServerPlayer player, ItemStack weapon) {
        if (BattleManager.isInBattle(player) || IvaliceStatuses.unableToAct(player) || !ZodiacJobs.permits(player, weapon)) return false;
        IvaliceMonsterEntity enemy = BattleManager.aimedEnemy(player);
        if (enemy == null) {
            player.sendOverlayMessage(Component.literal("Mire em uma criatura de Ivalice no alcance."));
            return false;
        }
        return fire(player, enemy, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) + 3, weapon);
    }

    static boolean fire(ServerPlayer player, IvaliceMonsterEntity enemy, float damage, ItemStack weapon) {
        ItemStack ammo = ammunition(player, weapon);
        if (ammo.isEmpty() && !player.getAbilities().instabuild) {
            player.sendOverlayMessage(Component.literal("Falta munição compatível."));
            return false;
        }
        String id = BuiltInRegistries.ITEM.getKey(ammo.getItem()).getPath();
        var entry = IvaliceTzaCatalog.getEntry(id);
        if (entry != null) damage += 1 + entry.rank() * .16F;
        String element = switch (id) {
            case "tza_fiery_arrows", "wyrmfire_shot" -> "fire";
            case "tza_lightning_arrows" -> "thunder";
            case "tza_icecloud_arrows" -> "ice";
            case "tza_aqua_shot", "tza_water_bombs" -> "water";
            case "tza_windslicer_shot" -> "wind";
            case "tza_dark_shot" -> "dark";
            case "tza_mud_shot", "tza_stone_shot", "tza_stone_bolts" -> "earth";
            default -> "";
        };
        if (!element.isEmpty()) damage *= BattleManager.getElementalMultiplier(enemy.getStats(), element);
        if (!player.getAbilities().instabuild) ammo.shrink(1);
        var level = player.level();
        var end = enemy.position().add(0, enemy.getBbHeight() * .62, 0);
        for (int i = 1; i <= 10; i++) {
            var pos = player.getEyePosition().lerp(end, i / 10.0);
            level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 2, .06, .06, .06, .02);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, .85F, 1.1F);
        if (damage < 0) enemy.heal(-damage);
        else BattleManager.strike(player, enemy, damage, false);
        if (enemy.isAlive()) {
            switch (id) {
                case "tza_silent_shot" -> status(enemy, IvaliceStatuses.SILENCE, 0);
                case "tza_stun_bombs", "tza_time_bolts" -> status(enemy, IvaliceStatuses.STOP, 0);
                case "tza_poison_bombs", "tza_assassin_s_arrows" -> IvaliceActionEffects.effect(enemy, MobEffects.POISON, 220, 0);
                case "tza_oil_bombs" -> IvaliceActionEffects.effect(enemy, IvaliceStatuses.OIL, 220, 0);
                case "tza_chaos_bombs" -> IvaliceActionEffects.effect(enemy, IvaliceStatuses.CONFUSE, 200, 0);
                case "tza_stink_bombs" -> IvaliceActionEffects.effect(enemy, IvaliceStatuses.DISEASE, 200, 0);
                case "tza_sapping_bolts" -> IvaliceActionEffects.effect(enemy, MobEffects.WITHER, 160, 0);
                case "tza_lead_bolts" -> IvaliceActionEffects.effect(enemy, MobEffects.SLOWNESS, 140, 1);
                case "tza_black_bolts" -> IvaliceActionEffects.effect(enemy, MobEffects.BLINDNESS, 140, 0);
                default -> {}
            }
        }
        return true;
    }
}
