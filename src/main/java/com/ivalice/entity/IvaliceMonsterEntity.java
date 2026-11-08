package com.ivalice.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.ivalice.combat.BattleManager;
import com.ivalice.data.IvalicePlayerData;
import com.ivalice.registry.IvaliceItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Map;

/**
 * Entidade base para todas as criaturas, Elite Marks (Caçadas do Clã Centurio) e Espers de Final Fantasy XII.
 * Integra animações 3D GeckoLib 5.5.7, atributos canônicos de Ivalice, comportamento Hostil/Neutro,
 * engajamento no Sistema de Combate por Turnos (Etapa 3.3), Bestiário e recompensas de Caçadas (Etapa 4).
 */
public class IvaliceMonsterEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final String speciesId;
    private boolean stolenFrom = false;

    public IvaliceMonsterEntity(EntityType<? extends IvaliceMonsterEntity> type, Level level, String speciesId) {
        super(type, level);
        this.speciesId = speciesId;
        IvaliceMonsterStats stats = getStats();
        this.xpReward = stats.xpReward();
    }

    public String getSpeciesId() {
        return speciesId;
    }

    public IvaliceMonsterStats getStats() {
        return IvaliceMonsterStats.byId(speciesId);
    }

    public boolean isStolenFrom() {
        return stolenFrom;
    }

    public void setStolenFrom(boolean stolenFrom) {
        this.stolenFrom = stolenFrom;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Criaturas Hostis e Bosses atacam à vista; Neutras (Cactoid, Giza Rabbit) só reagem quando provocadas!
        if (!"Neutral".equalsIgnoreCase(getStats().behavior())) {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    public static AttributeSupplier.Builder createAttributes(String speciesId) {
        IvaliceMonsterStats stats = IvaliceMonsterStats.byId(speciesId);
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, stats.maxHealth())
                .add(Attributes.ATTACK_DAMAGE, stats.attackDamage())
                .add(Attributes.MOVEMENT_SPEED, stats.moveSpeed())
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .add(Attributes.ARMOR, stats.level() * 0.4D);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 20 == 0) {
            updateCustomNameplate();
        }
    }

    private void updateCustomNameplate() {
        IvaliceMonsterStats stats = getStats();
        ChatFormatting rankColor = ChatFormatting.WHITE;
        if (stats.rankType().contains("Esper")) {
            rankColor = ChatFormatting.LIGHT_PURPLE;
        } else if (stats.rankType().contains("Elite Mark")) {
            rankColor = ChatFormatting.RED;
        } else if ("Neutral".equalsIgnoreCase(stats.behavior())) {
            rankColor = ChatFormatting.GREEN;
        } else {
            rankColor = ChatFormatting.GOLD;
        }

        int hp = (int) Math.ceil(this.getHealth());
        int maxHp = (int) Math.ceil(this.getMaxHealth());
        this.setCustomName(Component.literal(
                String.format("[Lv.%d] %s (%d/%d HP)", stats.level(), stats.displayName(), hp, maxHp)
        ).withStyle(rankColor));
        this.setCustomNameVisible(true);
    }

    /**
     * Clicar com o botão direito na criatura abre ou inicia o Combate por Turnos de Final Fantasy XII (Etapa 3.3),
     * ou exibe informações de Libra se estiver segurando o Clan Primer!
     */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BattleManager.startOrResumeBattle(serverPlayer, this);
            return InteractionResult.SUCCESS;
        }
        if (this.level().isClientSide()) {
            BattleManager.openClientBattleScreen(this);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Ao atacar uma criatura de Ivalice no mundo, engaja automaticamente o sistema de combate FFXII
     * (aplicando Gambits automáticos, fraquezas elementais e feedback de turno) ou abre a tela de batalha se agachado.
     */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer serverPlayer) {
            // Verifica multiplicador elemental da arma ou magia usada pelo jogador
            float finalAmount = BattleManager.calculateFieldDamage(serverPlayer, this, amount);
            boolean hit = super.hurtServer(level, source, finalAmount);
            if (hit && this.isAlive()) {
                BattleManager.onPlayerFieldStrike(serverPlayer, this, finalAmount);
            }
            updateCustomNameplate();
            return hit;
        }
        boolean hit = super.hurtServer(level, source, amount);
        updateCustomNameplate();
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        IvaliceMonsterStats stats = getStats();

        // Drop primário garantido (1 a 2 unidades)
        Item primary = IvaliceItems.getItemById(stats.primaryLootId());
        if (primary != null) {
            int count = 1 + this.random.nextInt(2);
            this.spawnAtLocation(level, new ItemStack(primary, count));
        }

        // Drop raro (25% de chance em monstros comuns, 100% em Elite Marks e Espers)
        boolean dropRare = stats.rankType().contains("Mark") || stats.rankType().contains("Esper") || this.random.nextFloat() < 0.25F;
        if (dropRare) {
            Item rare = IvaliceItems.getItemById(stats.rareLootId());
            if (rare != null) {
                this.spawnAtLocation(level, new ItemStack(rare, 1));
            }
        }

        // Atualiza Bestiário, Gil, LP e verifica conclusão de Caçada (Mark Hunt) para o jogador!
        if (damageSource.getEntity() instanceof ServerPlayer serverPlayer) {
            BattleManager.onMonsterDefeated(serverPlayer, this);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(WALK_ANIM);
            }
            return state.setAndContinue(IDLE_ANIM);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
