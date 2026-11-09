package com.ivalice.worldgen;

import com.ivalice.combat.BattleManager;
import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.registry.IvaliceEntities;
import com.ivalice.registry.IvaliceItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

public final class RuinSealBlock extends Block {
    private final ResourceKey<Level> dimension;
    private final String keyItem;
    private final String boss;
    private final String hunt;

    public RuinSealBlock(Properties properties, ResourceKey<Level> dimension, String keyItem, String boss, String hunt) {
        super(properties);
        this.dimension = dimension;
        this.keyItem = keyItem;
        this.boss = boss;
        this.hunt = hunt;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable("ivalice.ruin_seal.key",
                    IvaliceItems.getItemById(keyItem).getName(new ItemStack(IvaliceItems.getItemById(keyItem)))));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(IvaliceItems.getItemById(keyItem))) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer user)) return InteractionResult.SUCCESS;

        if (!server.dimension().equals(dimension) || BattleManager.isInBattle(user)
                || !server.getEntitiesOfClass(IvaliceMonsterEntity.class, new AABB(pos).inflate(72),
                        mob -> mob.isAlive() && mob.getSpeciesId().equals(boss)).isEmpty()) {
            user.sendOverlayMessage(Component.translatable("ivalice.ruin_seal.dormant"));
            return InteractionResult.FAIL;
        }

        var creature = IvaliceEntities.getById(boss).create(server, EntitySpawnReason.EVENT);
        if (creature == null) return InteractionResult.FAIL;
        BlockPos spawn = pos.above();
        creature.setPos(spawn.getX() + .5, spawn.getY(), spawn.getZ() + .5);
        if (!server.noCollision(creature) || !server.getFluidState(spawn).isEmpty()
                || !server.getEntitiesOfClass(Player.class, creature.getBoundingBox(), Player::isAlive).isEmpty()) {
            user.sendOverlayMessage(Component.translatable("ivalice.ruin_seal.space"));
            return InteractionResult.FAIL;
        }
        creature.setPersistenceRequired();
        if (!server.addFreshEntity(creature)) return InteractionResult.FAIL;

        if (!user.getAbilities().instabuild) stack.shrink(1);
        var profile = IvalicePlayerData.get(user);
        profile.activeHuntId = hunt;
        IvalicePlayerData.save(user);
        server.sendParticles(dimension.equals(Level.NETHER) ? ParticleTypes.FLAME : ParticleTypes.END_ROD,
                spawn.getX() + .5, spawn.getY() + 1.0, spawn.getZ() + .5, 64, 1.1, 1.5, 1.1, .16);
        server.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, .7F);
        user.sendSystemMessage(Component.translatable("ivalice.ruin_seal.awakened", creature.getType().getDescription())
                .withStyle(ChatFormatting.GOLD));
        return InteractionResult.SUCCESS;
    }
}
