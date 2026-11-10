package com.ivalice.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.ivalice.combat.BattleManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class IvaliceGuardEntity extends PathfinderMob implements GeoEntity {
    public static final int PATROL_RADIUS = 12;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation STRIKE = RawAnimation.begin().thenPlay("strike");
    private static final String POST_KEY = "ivalice_guard_post";

    private final AnimatableInstanceCache animations = GeckoLibUtil.createInstanceCache(this);

    public IvaliceGuardEntity(EntityType<? extends IvaliceGuardEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 48)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.FOLLOW_RANGE, PATROL_RADIUS);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.14, true));
        goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 0.9));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.68));
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 6, true, true,
                (target, level) -> isThreat(target)));
    }

    private boolean isThreat(LivingEntity target) {
        if (!isWithinHome(target.blockPosition())) return false;
        if (target instanceof IvaliceMonsterEntity monster) {
            return !monster.isSummoned() && !BattleManager.isEngaged(monster)
                    && (!"Neutral".equalsIgnoreCase(monster.getStats().behavior()) || monster.getTarget() == this);
        }
        return target instanceof Enemy;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return isThreat(target) && super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof LivingEntity living) || !isThreat(living)) return false;
        triggerAnim("movement", "strike");
        return super.doHurtTarget(level, target);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (getHomeRadius() > 0) output.store(POST_KEY, BlockPos.CODEC, getHomePosition());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        input.read(POST_KEY, BlockPos.CODEC).ifPresent(post -> setHomeTo(post, PATROL_RADIUS));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 4, state -> state.setAndContinue(
                state.isMoving() ? (isAggressive() ? RUN : WALK) : IDLE)).triggerableAnim("strike", STRIKE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animations;
    }
}
