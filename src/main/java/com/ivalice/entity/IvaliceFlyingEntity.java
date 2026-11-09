package com.ivalice.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;

public final class IvaliceFlyingEntity extends IvaliceMonsterEntity {
    public IvaliceFlyingEntity(EntityType<? extends IvaliceMonsterEntity> type, Level level, String id) {
        super(type, level, id);
        moveControl = new FlyingMoveControl(this, 18, true);
        setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        var navigator = new FlyingPathNavigation(this, level);
        navigator.setCanOpenDoors(false);
        return navigator;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.08D, true));
        goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, .82D));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }
}
