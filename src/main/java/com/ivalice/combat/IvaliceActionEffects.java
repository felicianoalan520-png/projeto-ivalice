package com.ivalice.combat;

import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceBestiary;
import com.ivalice.entity.IvaliceMonsterEntity;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.EnumSet;
import java.util.List;

import static com.ivalice.combat.BattleManager.Action.*;

final class IvaliceActionEffects {
    private static final EnumSet<BattleManager.Action> SELF = EnumSet.of(
            BLINDNA, VOX, POISONA, RAISE, STONA, REGEN, CLEANSE, ESUNA, CURAJA, RENEW, ARISE, ESUNAGA,
            HASTE, FLOAT, REFLECT, REFLECTGA, PROTECT, SHELL, BRAVERY, FAITH, PROTECTGA, SHELLGA,
            REVERSE, BERSERK, DECOY, VANISH, VANISHGA, BUBBLE, FIRST_AID, CHARGE, INFUSE, REVIVE);
    private static final EnumSet<BattleManager.Action> AREA = EnumSet.of(
            BIO, SCOURGE, DISPELGA, SLOWGA, BLINDGA, TOXIFY, SILENCEGA, SLEEPGA, GRAVIGA, TRAVELER, GIL_TOSS, HOROLOGY);

    private IvaliceActionEffects() {}

    static boolean targetsSelf(BattleManager.Action action) {
        return SELF.contains(action);
    }

    static boolean isArea(BattleManager.Action action) {
        return AREA.contains(action);
    }

    static boolean hasEffect(BattleManager.Action action) {
        return action.school != null && action.element == null && action != CURE && action != CURA
                && action != CURAGA && action != HASTEGA;
    }

    static BattleVisual.Style style(BattleManager.Action action) {
        if (action.school == com.ivalice.item.IvaliceItemCategory.TECHNICK || !hasEffect(action)) return BattleVisual.Style.TECHNIQUE;
        return targetsSelf(action) ? BattleVisual.Style.CURE : BattleVisual.Style.TECHNIQUE;
    }

    static String apply(ServerPlayer caster, IvaliceMonsterEntity enemy, BattleManager.Action action) {
        boolean self = targetsSelf(action);
        if (!self && (enemy == null || !enemy.isAlive())) return "Sem alvo.";
        LivingEntity target = self ? caster : enemy;
        if (!self && action.mp > 0 && enemy.hasEffect(IvaliceStatuses.REFLECT)
                && action != DISPEL && action != DISPELGA) target = caster;
        int level = IvalicePlayerData.get(caster).getBattleLevel();
        float attack = (float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE);
        switch (action) {
            case BLINDNA -> remove(target, MobEffects.BLINDNESS, MobEffects.DARKNESS);
            case VOX -> target.removeEffect(IvaliceStatuses.SILENCE);
            case POISONA -> target.removeEffect(MobEffects.POISON);
            case STONA -> remove(target, IvaliceStatuses.PETRIFY, IvaliceStatuses.STOP);
            case CLEANSE -> target.removeEffect(IvaliceStatuses.DISEASE);
            case ESUNA, ESUNAGA -> clear(target, MobEffectCategory.HARMFUL);
            case DISPEL, DISPELGA -> clear(target, MobEffectCategory.BENEFICIAL);
            case RAISE, REVIVE -> IvaliceStatuses.ward(target, false);
            case ARISE -> IvaliceStatuses.ward(target, true);
            case REGEN -> effect(target, MobEffects.REGENERATION, 480, 1);
            case CURAJA -> heal(caster, target, 42 + level);
            case RENEW -> heal(caster, target, target.getMaxHealth());
            case HASTE -> effect(target, MobEffects.SPEED, 420, 1);
            case FLOAT -> effect(target, MobEffects.SLOW_FALLING, 600, 0);
            case REFLECT, REFLECTGA -> effect(target, IvaliceStatuses.REFLECT, 360, 0);
            case PROTECT, PROTECTGA -> effect(target, MobEffects.RESISTANCE, 480, 0);
            case SHELL, SHELLGA -> effect(target, IvaliceStatuses.SHELL, 480, 0);
            case BRAVERY -> effect(target, MobEffects.STRENGTH, 400, 1);
            case FAITH -> effect(target, IvaliceStatuses.FAITH, 400, 0);
            case REVERSE -> effect(target, IvaliceStatuses.REVERSE, 180, 0);
            case BERSERK -> {
                effect(target, MobEffects.STRENGTH, 300, 2);
                effect(target, MobEffects.SPEED, 300, 0);
                effect(target, IvaliceStatuses.SILENCE, 300, 0);
            }
            case DECOY -> effect(target, IvaliceStatuses.DECOY, 300, 0);
            case VANISH, VANISHGA -> effect(target, MobEffects.INVISIBILITY, 480, 0);
            case BUBBLE -> {
                int amplifier = Math.clamp((int) (target.getMaxHealth() / 4) - 1, 0, 40);
                effect(target, MobEffects.HEALTH_BOOST, 600, amplifier);
                target.heal((amplifier + 1) * 4);
            }
            case BLIND, BLINDGA -> inflict(target, MobEffects.BLINDNESS, 280, 0);
            case OIL -> inflict(target, IvaliceStatuses.OIL, 460, 0);
            case POISON, TOXIFY -> inflict(target, MobEffects.POISON, 400, action == TOXIFY ? 1 : 0);
            case SILENCE, SILENCEGA, ADDLE -> inflict(target, IvaliceStatuses.SILENCE, 330, 0);
            case SLEEP, SLEEPGA -> inflict(target, IvaliceStatuses.SLEEP, 260, 0);
            case SLOW, SLOWGA -> inflict(target, MobEffects.SLOWNESS, 360, 1);
            case IMMOBILIZE -> inflict(target, IvaliceStatuses.IMMOBILIZE, 300, 0);
            case DISABLE -> inflict(target, IvaliceStatuses.DISABLE, 180, 0);
            case BLEED -> inflict(target, MobEffects.WITHER, 200, 0);
            case BREAK -> inflict(target, IvaliceStatuses.PETRIFY, 180, 0);
            case STOP -> inflict(target, IvaliceStatuses.STOP, 160, 0);
            case COUNTDOWN -> inflict(target, IvaliceStatuses.DOOM, 220, 0);
            case CONFUSE, CHARM -> inflict(target, IvaliceStatuses.CONFUSE, 340, 0);
            case WITHER -> inflict(target, MobEffects.WEAKNESS, 420, 1);
            case EXPOSE -> inflict(target, IvaliceStatuses.DISEASE, 380, 0);
            case SHEAR -> {
                target.removeEffect(IvaliceStatuses.SHELL);
                inflict(target, IvaliceStatuses.OIL, 340, 0);
            }
            case ACHILLES -> inflict(target, IvaliceStatuses.OIL, 360, 0);
            case STAMP -> {
                for (var status : caster.getActiveEffects()) {
                    if (status.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                        inflict(target, status.getEffect(), Math.min(360, status.getDuration()), status.getAmplifier());
                    }
                }
            }
            case FIRST_AID -> {
                if (caster.getHealth() > caster.getMaxHealth() * .4F) return "First Aid exige HP crítico.";
                heal(caster, caster, 6 + level / 4F);
            }
            case CHARGE -> {
                if (caster.getRandom().nextFloat() < .70F) return "Charge • +" + BattleManager.restoreMp(caster, 18 + level / 2) + " MP";
                BattleManager.exhaustMp(caster);
                return "Charge falhou • MP zerado.";
            }
            case INFUSE -> {
                float used = BattleManager.drainMp(caster);
                heal(caster, caster, used * .55F);
                return "Infuse • converteu " + (int) used + " MP.";
            }
            case SYPHON -> {
                int drained = BattleManager.restoreMp(caster, 16 + level / 3);
                inflict(target, IvaliceStatuses.SILENCE, 90, 0);
                return "Syphon • +" + drained + " MP";
            }
            case BIO, SCOURGE -> {
                inflict(target, action == BIO ? MobEffects.POISON : IvaliceStatuses.DISEASE, 280, 0);
                damage(caster, target, (action == BIO ? 22 : 39) + level * .45F, true);
            }
            case SHOCK -> damage(caster, target, 48 + level * .6F, true);
            case BALANCE -> damage(caster, target, caster.getMaxHealth() - caster.getHealth(), true);
            case GRAVITY, GRAVIGA -> damage(caster, target, Math.min(48, target.getHealth() * (action == GRAVIGA ? .45F : .25F)), true);
            case DRAIN -> {
                float previous = target.getHealth();
                damage(caster, target, 12 + level * .35F, true);
                heal(caster, caster, Math.max(0, previous - target.getHealth()));
            }
            case DEATH, WARP -> {
                if (enemy.getStats().rankType().equals("Normal") && (enemy.getHealth() <= enemy.getMaxHealth() * .30F
                        || caster.getRandom().nextFloat() < .20F)) {
                    if (action == WARP) enemy.discard();
                    else damage(caster, target, target.getMaxHealth() * 4, true);
                } else return action.label + " falhou.";
            }
            case SOULEATER -> {
                float cost = caster.getMaxHealth() * .10F;
                caster.setHealth(Math.max(1, caster.getHealth() - cost));
                damage(caster, enemy, (attack + level * .6F) * 1.65F, false);
            }
            case BONECRUSHER -> {
                if (enemy.getHealth() > enemy.getMaxHealth() * .25F) {
                    caster.setHealth(Math.max(1, caster.getHealth() * .50F));
                    return "Bonecrusher falhou.";
                }
                damage(caster, enemy, 28 + level, false);
            }
            case SIGHT_UNSEEING -> {
                if (!caster.hasEffect(MobEffects.BLINDNESS)) return "Sight Unseeing exige cegueira.";
                damage(caster, enemy, Math.max(10, enemy.getHealth() - 1), false);
            }
            case SHADES_OF_BLACK -> {
                String[] elements = {"fire", "ice", "thunder", "wind", "water", "dark"};
                String element = elements[caster.getRandom().nextInt(elements.length)];
                float result = (14 + level * .65F) * BattleManager.getElementalMultiplier(enemy.getStats(), element);
                if (result < 0) enemy.heal(-result);
                else damage(caster, enemy, result, true);
                return "Shades of Black • " + element;
            }
            case NEEDLES -> damage(caster, enemy, 20, false);
            case NUMEROLOGY -> damage(caster, enemy, 5 + (level * 7 % 37), false);
            case HOROLOGY -> damage(caster, enemy, 5 + (caster.level().getGameTime() / 20 % 10) * (level / 7F + 1), false);
            case TRAVELER -> damage(caster, enemy, Math.min(55, 6 + caster.getStats().getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)) / 10000F), false);
            case TELEKINESIS -> damage(caster, enemy, attack + 5 + level * .55F, false);
            case GIL_TOSS -> {
                var profile = IvalicePlayerData.get(caster);
                int gil = Math.min(profile.gil, 10 + level * 2);
                if (gil <= 0) return "Gil Toss precisa de Gil.";
                profile.gil -= gil;
                IvalicePlayerData.save(caster);
                damage(caster, enemy, gil / 2F, false);
            }
            case POACH -> {
                var entry = IvaliceBestiary.find(enemy.getSpeciesId());
                if (entry == null || entry.poach().isEmpty() || enemy.getHealth() > enemy.getMaxHealth() * .25F) {
                    return "Poach exige um alvo elegível com HP crítico.";
                }
                String id = IvaliceBestiary.poach(entry, caster.getRandom());
                if (id == null) return "Poach falhou.";
                String reward = BattleManager.giveLoot(caster, id);
                if (reward == null) return "Poach falhou.";
                IvalicePlayerData.save(caster);
                enemy.discard();
                return "Poach • " + reward;
            }
            default -> throw new IllegalStateException("Missing implementation for " + action);
        }
        return action.label;
    }

    static void heal(ServerPlayer caster, LivingEntity target, float amount) {
        if (target.hasEffect(IvaliceStatuses.DISEASE)) return;
        if (target.hasEffect(IvaliceStatuses.REVERSE)) damage(caster, target, amount, false);
        else target.heal(amount);
    }

    static void clear(LivingEntity target, MobEffectCategory category) {
        for (var status : List.copyOf(target.getActiveEffects())) {
            if (status.getEffect().value().getCategory() == category) target.removeEffect(status.getEffect());
        }
    }

    @SafeVarargs
    static void remove(LivingEntity target, Holder<MobEffect>... statuses) {
        for (var status : statuses) target.removeEffect(status);
    }

    static void effect(LivingEntity target, Holder<MobEffect> status, int ticks, int power) {
        target.addEffect(new MobEffectInstance(status, ticks, power));
    }

    private static void inflict(LivingEntity target, Holder<MobEffect> status, int ticks, int power) {
        if (target instanceof IvaliceMonsterEntity monster && !monster.getStats().rankType().equals("Normal")) ticks = Math.min(70, ticks);
        effect(target, status, ticks, power);
    }

    private static void damage(ServerPlayer caster, LivingEntity target, float amount, boolean magic) {
        if (amount <= 0) return;
        if (target instanceof IvaliceMonsterEntity enemy) BattleManager.strike(caster, enemy, amount, magic);
        else target.hurtServer(caster.level(), caster.level().damageSources().magic(), amount);
    }
}
