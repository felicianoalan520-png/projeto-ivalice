package com.ivalice.combat;

import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.registry.IvaliceItems;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Locale;

import static com.ivalice.combat.IvaliceActionEffects.*;

public final class IvaliceConsumables {
    public enum Use {
        POTION, HI_POTION, X_POTION, ETHER, HI_ETHER, ELIXIR, PHOENIX_DOWN, GOLD_NEEDLE, ECHO_HERBS,
        ANTIDOTE, EYE_DROPS, PRINCE_S_KISS, HANDKERCHIEF, CHRONOS_TEAR, NU_KHAI_SAND, SERUM, REMEDY,
        BACCHUS_S_WINE, MEGALIXIR, BALTORO_SEED, DOMAINE_CALVADOS, REVERSE_MOTE, HASTEGA_MOTE,
        REFLECTGA_MOTE, VANISHGA_MOTE, FLOAT_MOTE, CURA_MOTE, BUBBLE_MOTE,
        SOLEIL_FANG("fire", true), RIME_FANG("ice", true), LIGHTNING_FANG("thunder", true),
        DARK_MOTE("dark", true), AERO_MOTE("wind", true), AQUARA_MOTE("water", true),
        HOLY_MOTE("holy", false), SCATHE_MOTE("", true), BALANCE_MOTE("", true), WARP_MOTE("", true),
        BIO_MOTE("", true), SHOCK_MOTE("", false), GRAVITY_MOTE("", false), DISPEL_MOTE("", false),
        AEROGA_MOTE("wind", true), DARK_ENERGY("", true), DARK_MATTER("", true), KNOT_OF_RUST("", false),
        METEORITE_1("", false), METEORITE_2("", false), METEORITE_3("", false), METEORITE_4("", false),
        EKSIR_BERRIES("", false);

        final String element;
        private final boolean area;

        Use() {
            this(null, false);
        }

        Use(String element, boolean area) {
            this.element = element;
            this.area = area;
        }

        public boolean area() {
            return area;
        }

        public boolean needsEnemy(ServerPlayer player) {
            return element != null || (reversible() && wearing(player, "tza_nihopalaoa"));
        }

        private boolean reversible() {
            return switch (this) {
                case POTION, HI_POTION, X_POTION, ELIXIR, PHOENIX_DOWN, GOLD_NEEDLE, ECHO_HERBS, ANTIDOTE,
                        EYE_DROPS, PRINCE_S_KISS, HANDKERCHIEF, CHRONOS_TEAR, NU_KHAI_SAND, SERUM, REMEDY -> true;
                default -> false;
            };
        }

        public BattleVisual.Style style() {
            return switch (this) {
                case SOLEIL_FANG -> BattleVisual.Style.FIRE;
                case RIME_FANG -> BattleVisual.Style.ICE;
                case LIGHTNING_FANG -> BattleVisual.Style.THUNDER;
                case DARK_MOTE, DARK_MATTER, DARK_ENERGY -> BattleVisual.Style.DARK;
                case AERO_MOTE, AEROGA_MOTE -> BattleVisual.Style.WIND;
                case AQUARA_MOTE -> BattleVisual.Style.WATER;
                case HOLY_MOTE -> BattleVisual.Style.CURE;
                default -> BattleVisual.Style.ITEM;
            };
        }
    }

    private IvaliceConsumables() {}

    public static Use byId(String id) {
        try {
            return Use.valueOf(id.replaceFirst("^tza_", "").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean wearing(ServerPlayer player, String id) {
        var item = IvaliceItems.getItemById(id);
        return item != null && player.getOffhandItem().is(item);
    }

    public static boolean applicable(Use use, List<? extends LivingEntity> targets) {
        return !targets.isEmpty() && (use != Use.EKSIR_BERRIES || targets.stream().anyMatch(entity ->
                entity instanceof IvaliceMonsterEntity mob && mob.getSpeciesId().equals("garuda")));
    }

    static String apply(ServerPlayer player, Use use, List<? extends LivingEntity> targets) {
        boolean reversed = use.reversible() && wearing(player, "tza_nihopalaoa");
        float potency = wearing(player, "tza_pheasant_netsuke") ? 1.5F : 1;
        int rust = IvalicePlayerData.get(player).rustCharge;
        int extraMp = 0;
        for (LivingEntity target : targets) {
            if (!target.isAlive()) continue;
            switch (use) {
                case POTION -> restore(player, target, 6 * potency, reversed);
                case HI_POTION -> restore(player, target, 12 * potency, reversed);
                case X_POTION -> restore(player, target, 26 * potency, reversed);
                case ETHER, HI_ETHER -> extraMp += BattleManager.restoreMp(player, (int) ((use == Use.ETHER ? 34 : 72) * potency));
                case ELIXIR, MEGALIXIR -> {
                    restore(player, target, target.getMaxHealth(), reversed);
                    if (!reversed && target instanceof ServerPlayer ally) extraMp += BattleManager.restoreMp(ally, Integer.MAX_VALUE);
                }
                case PHOENIX_DOWN -> {
                    if (reversed) itemDamage(player, target, target.getMaxHealth() * .50F, "");
                    else IvaliceStatuses.ward(target, false);
                }
                case GOLD_NEEDLE -> remedy(target, reversed, IvaliceStatuses.PETRIFY);
                case ECHO_HERBS -> remedy(target, reversed, IvaliceStatuses.SILENCE);
                case ANTIDOTE -> remedy(target, reversed, MobEffects.POISON);
                case EYE_DROPS -> remedy(target, reversed, MobEffects.BLINDNESS);
                case PRINCE_S_KISS -> remedy(target, reversed, IvaliceStatuses.SLEEP);
                case HANDKERCHIEF -> remedy(target, reversed, IvaliceStatuses.OIL);
                case CHRONOS_TEAR -> {
                    remedy(target, reversed, IvaliceStatuses.STOP);
                    remedy(target, reversed, MobEffects.SLOWNESS);
                }
                case NU_KHAI_SAND -> remedy(target, reversed, IvaliceStatuses.CONFUSE);
                case SERUM -> remedy(target, reversed, IvaliceStatuses.DISEASE);
                case REMEDY -> {
                    if (reversed) {
                        remedy(target, true, MobEffects.POISON);
                        remedy(target, true, MobEffects.BLINDNESS);
                        remedy(target, true, IvaliceStatuses.SILENCE);
                        remedy(target, true, MobEffects.SLOWNESS);
                    } else clear(target, MobEffectCategory.HARMFUL);
                }
                case BACCHUS_S_WINE -> {
                    effect(target, MobEffects.STRENGTH, 360, 2);
                    effect(target, IvaliceStatuses.SILENCE, 360, 0);
                }
                case DOMAINE_CALVADOS -> effect(target, MobEffects.STRENGTH, 480, 1);
                case BALTORO_SEED -> {
                    effect(target, MobEffects.RESISTANCE, 480, 0);
                    effect(target, MobEffects.REGENERATION, 480, 1);
                    effect(target, IvaliceStatuses.SHELL, 480, 0);
                    effect(target, MobEffects.SPEED, 480, 1);
                    effect(target, IvaliceStatuses.FAITH, 480, 0);
                    effect(target, MobEffects.STRENGTH, 480, 1);
                }
                case REVERSE_MOTE -> effect(target, IvaliceStatuses.REVERSE, 180, 0);
                case HASTEGA_MOTE -> effect(target, MobEffects.SPEED, 600, 1);
                case REFLECTGA_MOTE -> effect(target, IvaliceStatuses.REFLECT, 360, 0);
                case VANISHGA_MOTE -> effect(target, MobEffects.INVISIBILITY, 600, 0);
                case FLOAT_MOTE -> effect(target, MobEffects.SLOW_FALLING, 600, 0);
                case CURA_MOTE -> restore(player, target, 18 * potency, false);
                case BUBBLE_MOTE -> {
                    int amplifier = Math.clamp((int) target.getMaxHealth() / 4 - 1, 0, 40);
                    effect(target, MobEffects.HEALTH_BOOST, 600, amplifier);
                    target.heal((amplifier + 1) * 4);
                }
                case SOLEIL_FANG, RIME_FANG, LIGHTNING_FANG ->
                        itemDamage(player, target, Math.min(46, target.getMaxHealth() * .25F * potency), use.element);
                case DARK_MOTE -> itemDamage(player, target, 17, use.element);
                case AERO_MOTE -> itemDamage(player, target, 23, use.element);
                case AQUARA_MOTE -> itemDamage(player, target, 28, use.element);
                case HOLY_MOTE -> itemDamage(player, target, 48, use.element);
                case SCATHE_MOTE -> itemDamage(player, target, 58, use.element);
                case AEROGA_MOTE -> itemDamage(player, target, 42, use.element);
                case SHOCK_MOTE -> itemDamage(player, target, 44, use.element);
                case DARK_ENERGY -> itemDamage(player, target, 78, use.element);
                case BALANCE_MOTE -> itemDamage(player, target, player.getMaxHealth() - player.getHealth(), use.element);
                case GRAVITY_MOTE -> itemDamage(player, target, Math.min(55, target.getMaxHealth() * .25F), use.element);
                case DISPEL_MOTE -> clear(target, MobEffectCategory.BENEFICIAL);
                case WARP_MOTE -> {
                    if (target instanceof IvaliceMonsterEntity enemy && enemy.getStats().rankType().equals("Normal")
                            && (enemy.getHealth() < enemy.getMaxHealth() * .3F || player.getRandom().nextFloat() < .2F)) enemy.discard();
                }
                case BIO_MOTE -> {
                    itemDamage(player, target, 30, use.element);
                    effect(target, MobEffects.WITHER, 180, 0);
                }
                case METEORITE_1 -> effect(target, MobEffects.WITHER, 240, 0);
                case METEORITE_2 -> effect(target, IvaliceStatuses.DISEASE, 280, 0);
                case METEORITE_3 -> itemDamage(player, target, 1 + player.getRandom().nextFloat() * player.getMaxHealth(), "");
                case METEORITE_4 -> itemDamage(player, target, 15 + player.getRandom().nextInt(55), "");
                case EKSIR_BERRIES -> {
                    if (target instanceof IvaliceMonsterEntity mob && mob.getSpeciesId().equals("garuda")) {
                        effect(target, MobEffects.WEAKNESS, 600, 2);
                        effect(target, IvaliceStatuses.DISABLE, 100, 0);
                    }
                }
                case KNOT_OF_RUST -> {
                    int amount = Math.max(1, (int) (player.getMaxHealth() * (.15F + player.getRandom().nextFloat() * .65F)));
                    itemDamage(player, target, amount, "");
                    IvalicePlayerData.get(player).rustCharge = Math.min(128, rust + amount);
                    IvalicePlayerData.save(player);
                }
                case DARK_MATTER -> itemDamage(player, target, 8 + rust * .5F, "");
            }
        }
        if (use == Use.DARK_MATTER) {
            IvalicePlayerData.get(player).rustCharge = 0;
            IvalicePlayerData.save(player);
        }
        return extraMp > 0 ? "+" + extraMp + " MP" : "";
    }

    private static void restore(ServerPlayer player, LivingEntity target, float amount, boolean reversed) {
        if (reversed) itemDamage(player, target, amount, "");
        else heal(player, target, amount);
    }

    private static void remedy(LivingEntity target, boolean reversed, Holder<MobEffect> status) {
        if (reversed) effect(target, status, 220, 0);
        else target.removeEffect(status);
    }

    private static void itemDamage(ServerPlayer player, LivingEntity target, float amount, String element) {
        if (!(target instanceof IvaliceMonsterEntity enemy)) return;
        if (!element.isEmpty()) amount *= BattleManager.getElementalMultiplier(enemy.getStats(), element);
        if (amount < 0) enemy.heal(-amount);
        else BattleManager.strike(player, enemy, amount, false);
    }
}
