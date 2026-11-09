package com.ivalice.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.ivalice.combat.BattleManager;
import com.ivalice.combat.IvaliceStatuses;
import com.ivalice.data.IvalicePlayerData;
import com.ivalice.registry.IvaliceItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Map;
import java.util.UUID;

/**
 * Entidade base para todas as criaturas, Elite Marks (Caçadas do Clã Centurio) e Espers de Final Fantasy XII.
 * Integra animações 3D GeckoLib 5.5.7, atributos canônicos de Ivalice, comportamento Hostil/Neutro,
 * engajamento no Sistema de Combate por Turnos (Etapa 3.3), Bestiário e recompensas de Caçadas (Etapa 4).
 */
public class IvaliceMonsterEntity extends PathfinderMob implements GeoEntity {
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final String speciesId;
    private boolean stolenFrom = false;
    private boolean alphaLeader = false;
    private boolean alphaInitialized = false;
    private UUID summoner;
    private int summonTime;

    public boolean isSummoned() {
        return summoner != null;
    }

    public boolean isSummonedBy(ServerPlayer player) {
        return player.getUUID().equals(summoner);
    }

    public void bindSummoner(ServerPlayer player) {
        summoner = player.getUUID();
        summonTime = 400;
        alphaInitialized = true;
        targetSelector.removeAllGoals(goal -> true);
        setTarget(null);
    }

    @Override
    public boolean shouldBeSaved() {
        return !isSummoned() && super.shouldBeSaved();
    }

    public IvaliceMonsterEntity(EntityType<? extends IvaliceMonsterEntity> type, Level level, String speciesId) {
        super(type, level);
        this.speciesId = speciesId;
        IvaliceMonsterStats stats = getStats();
        this.xpReward = stats.xpReward();
        this.alphaLeader = "Alpha".equalsIgnoreCase(stats.rankType()) || speciesId.startsWith("alpha_");
        if (!"Neutral".equalsIgnoreCase(stats.behavior()) || this.alphaLeader) {
            this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    private boolean isPackSpecies() {
        return switch (speciesId) {
            case "wolf", "hyena", "white_wolf", "wild_saurian", "coeurl" -> true;
            default -> false;
        };
    }

    public boolean isAlphaLeader() {
        return alphaLeader;
    }

    public void promoteToAlpha() {
        if (this.alphaLeader) return;
        this.alphaLeader = true;
        var scaleAttr = this.getAttribute(Attributes.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(1.32D);
        var hpAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr != null) {
            hpAttr.setBaseValue(hpAttr.getBaseValue() * 1.85D);
            this.setHealth(this.getMaxHealth());
        }
        var atkAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atkAttr != null) atkAttr.setBaseValue(atkAttr.getBaseValue() * 1.45D);
        updateCustomNameplate();
    }

    public String getSpeciesId() {
        return speciesId;
    }

    public IvaliceMonsterStats getStats() {
        return IvaliceMonsterStats.byId(speciesId);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("ivalice_stolen", stolenFrom);
        output.putBoolean("ivalice_alpha", alphaLeader);
        output.putBoolean("ivalice_alpha_initialized", alphaInitialized);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        stolenFrom = input.getBooleanOr("ivalice_stolen", false);
        alphaLeader = input.getBooleanOr("ivalice_alpha", alphaLeader);
        alphaInitialized = input.getBooleanOr("ivalice_alpha_initialized", false);
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
    }

    public static AttributeSupplier.Builder createAttributes(String speciesId) {
        IvaliceMonsterStats stats = IvaliceMonsterStats.byId(speciesId);
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, stats.maxHealth())
                .add(Attributes.ATTACK_DAMAGE, stats.attackDamage())
                .add(Attributes.MOVEMENT_SPEED, stats.moveSpeed())
                .add(Attributes.FLYING_SPEED, stats.moveSpeed())
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .add(Attributes.ARMOR, stats.level() * 0.4D);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (IvaliceStatuses.unableToAct(this)) return false;
        if (isSummoned() && !(target instanceof IvaliceMonsterEntity mob && !mob.isSummoned())) return false;
        return !(target instanceof ServerPlayer player && BattleManager.isInBattle(player)) && super.canAttack(target);
    }

    private boolean isServerBattleParticipant() {
        return level() instanceof ServerLevel && BattleManager.isEngaged(this);
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi() && !isServerBattleParticipant();
    }

    @Override
    protected void pushEntities() {
        if (!isServerBattleParticipant()) super.pushEntities();
    }

    @Override
    public boolean isPushable() {
        return !isServerBattleParticipant() && super.isPushable();
    }

    @Override
    public void tick() {
        if (summoner != null && level() instanceof ServerLevel server) {
            var owner = server.getServer().getPlayerList().getPlayer(summoner);
            if (owner == null || owner.level() != server || !owner.isAlive() || --summonTime <= 0
                    || distanceToSqr(owner) > 48 * 48) {
                discard();
                return;
            }
            if (tickCount % 10 == 0) {
                var aimed = BattleManager.aimedEnemy(owner);
                setTarget(aimed != this ? aimed : null);
            }
        }
        if (!level().isClientSide() && !alphaInitialized) {
            alphaInitialized = true;
            if (!alphaLeader && isPackSpecies() && this.random.nextFloat() < 0.12F) {
                promoteToAlpha();
            } else if (alphaLeader) {
                var scaleAttr = this.getAttribute(Attributes.SCALE);
                if (scaleAttr != null && scaleAttr.getBaseValue() <= 1.01D) {
                    scaleAttr.setBaseValue(1.25D);
                }
            }
        }
        if (isServerBattleParticipant()) {
            setDeltaMovement(Vec3.ZERO);
            setJumping(false);
            if (tickCount % 15 == 0 && (alphaLeader || getStats().rankType().contains("Esper") || getStats().rankType().contains("Mark"))
                    && level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                        getX(), getY() + getBbHeight() * 0.5, getZ(), 6, 0.35, 0.35, 0.35, 0.02);
            }
        }
        super.tick();
        if (!this.level().isClientSide()) {
            this.setAggressive(this.getTarget() != null && this.getTarget().isAlive());
            if (this.tickCount % 20 == 0) {
                updateCustomNameplate();
            }
            if (this.alphaLeader && this.tickCount % 25 == 0 && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                        getX(), getY() + getBbHeight() * 0.45, getZ(), 2, 0.25, 0.25, 0.25, 0.01);
            }
        }
    }

    private void updateCustomNameplate() {
        IvaliceMonsterStats stats = getStats();
        ChatFormatting rankColor = ChatFormatting.WHITE;
        if (stats.rankType().contains("Esper")) {
            rankColor = ChatFormatting.LIGHT_PURPLE;
        } else if (stats.rankType().contains("Elite Mark")) {
            rankColor = ChatFormatting.RED;
        } else if (alphaLeader || "Alpha".equalsIgnoreCase(stats.rankType())) {
            rankColor = ChatFormatting.GOLD;
        } else if ("Neutral".equalsIgnoreCase(stats.behavior())) {
            rankColor = ChatFormatting.GREEN;
        } else {
            rankColor = ChatFormatting.YELLOW;
        }

        int hp = (int) Math.ceil(this.getHealth());
        int maxHp = (int) Math.ceil(this.getMaxHealth());
        int lvl = alphaLeader && !speciesId.startsWith("alpha_") ? stats.level() + 5 : stats.level();
        String prefix = (alphaLeader && !stats.displayName().contains("★")) ? "★ [ALFA] " : "";
        this.setCustomName(Component.literal(
                String.format("%s[Lv.%d] %s (%d/%d HP)", prefix, lvl, stats.displayName(), hp, maxHp)
        ).withStyle(rankColor));
        this.setCustomNameVisible(true);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof ServerPlayer serverPlayer) {
            // Verifica multiplicador elemental da arma ou magia usada pelo jogador
            float finalAmount = BattleManager.calculateFieldDamage(serverPlayer, this, amount);
            boolean hit = super.hurtServer(level, source, finalAmount);
            if (hit && this.isAlive()) {
                BattleManager.onPlayerFieldStrike(serverPlayer, this);
            }
            updateCustomNameplate();
            return hit;
        }
        return hurtWithCalculatedDamage(level, source, amount);
    }

    public boolean hurtWithCalculatedDamage(ServerLevel level, DamageSource source, float amount) {
        boolean hit = super.hurtServer(level, source, amount);
        updateCustomNameplate();
        return hit;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit) {
        if (isSummoned()) return;
        super.dropCustomDeathLoot(level, damageSource, recentlyHit);
        IvaliceMonsterStats stats = getStats();

        IvaliceBestiary.Entry entry = IvaliceBestiary.find(speciesId);
        if (entry != null) {
            int chain = damageSource.getEntity() instanceof ServerPlayer player
                    ? IvalicePlayerData.get(player).recordDropChain(entry.family(), level.getGameTime()) : 0;
            if (!entry.vanillaLoot() && !entry.drops().isEmpty()) {
                var pool = entry.drops().get(Math.min(chain, entry.drops().size() - 1));
                for (var roll : pool) {
                    if (random.nextFloat() >= roll.chance()) continue;
                    Item item = IvaliceItems.getItemById(roll.pick(random));
                    if (item != null) spawnAtLocation(level, new ItemStack(item));
                }
            }
        } else {
            dropLegacyRewards(level, stats);
        }
        if (damageSource.getEntity() instanceof ServerPlayer player) dropMonographLoot(level, player, stats);
        if (alphaLeader) {
            String[] stones = {"fire_stone", "water_stone", "wind_stone", "earth_stone", "dark_magicite"};
            Item bonusStone = IvaliceItems.getItemById(stones[random.nextInt(stones.length)]);
            if (bonusStone != null) spawnAtLocation(level, new ItemStack(bonusStone, 1 + random.nextInt(2)));
        }

        if (("mateus".equals(speciesId) || "zodiark".equals(speciesId) || "yiazmat".equals(speciesId)
                || "ifrit_pyreburner".equals(speciesId) || "shinryu".equals(speciesId)
                || "omega_mk".equals(speciesId) || "kefka_god".equals(speciesId))
                && damageSource.getEntity() instanceof ServerPlayer && random.nextFloat() < 0.35F) {
            spawnAtLocation(level, new ItemStack(IvaliceItems.ZODIAC_RESET_SEAL.get()));
        }
        if (damageSource.getEntity() instanceof ServerPlayer player) BattleManager.onMonsterDefeated(player, this);
    }

    private void dropMonographLoot(ServerLevel level, ServerPlayer player, IvaliceMonsterStats stats) {
        String family = stats.family().toLowerCase(java.util.Locale.ROOT);
        String monograph = family.contains("beast") || family.contains("avion") ? "hunter"
                : family.contains("dragon") || family.contains("plant") ? "dragoon"
                : family.contains("undead") || family.contains("giant") ? "warmage"
                : family.contains("insect") || family.contains("construct") ? "scholar"
                : family.contains("fiend") ? "knight" : "mage";
        Item knowledge = IvaliceItems.getItemById("tza_" + monograph + "_s_monograph");
        Item canopicJar = IvaliceItems.getItemById("tza_canopic_jar");
        if (knowledge != null && player.getInventory().countItem(knowledge) > 0 && random.nextFloat() < .10F) {
            Item rare = IvaliceItems.getItemById("high_arcana");
            if (rare != null) spawnAtLocation(level, new ItemStack(rare));
        }
        if (canopicJar != null && player.getInventory().countItem(canopicJar) > 0 && random.nextFloat() < .10F) {
            Item arcana = IvaliceItems.getItemById("arcana");
            if (arcana != null) spawnAtLocation(level, new ItemStack(arcana));
        }
    }

    private void dropLegacyRewards(ServerLevel level, IvaliceMonsterStats stats) {
        Item primary = IvaliceItems.getItemById(stats.primaryLootId());
        if (primary != null && (!"Normal".equals(stats.rankType()) || alphaLeader || random.nextFloat() < 0.78F)) {
            int count = (!"Normal".equals(stats.rankType()) || alphaLeader) ? 2 + random.nextInt(2) : 1 + random.nextInt(2);
            spawnAtLocation(level, new ItemStack(primary, count));
        }
        Item rare = IvaliceItems.getItemById(stats.rareLootId());
        if (rare != null && (!"Normal".equals(stats.rankType()) || alphaLeader || random.nextFloat() < 0.22F)) {
            spawnAtLocation(level, new ItemStack(rare));
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (BattleManager.isEngaged(this) || (target instanceof ServerPlayer player && BattleManager.isInBattle(player))
                || IvaliceStatuses.unableToAct(this)) return false;
        playBattleAnimation("strike");
        net.minecraft.world.phys.Vec3 dir = target.position().subtract(this.position()).normalize();
        this.push(dir.x * 0.35, 0.18, dir.z * 0.35);
        return super.doHurtTarget(level, target);
    }

    public void playBattleAnimation(String animation) {
        triggerAnim("movement", animation);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 4, state -> {
            if (state.isMoving()) {
                if (this.isAggressive() || this.getDeltaMovement().horizontalDistanceSqr() > 0.016D) {
                    return state.setAndContinue(RUN_ANIM);
                }
                return state.setAndContinue(WALK_ANIM);
            }
            return state.setAndContinue(IDLE_ANIM);
        }).triggerableAnim("strike", RawAnimation.begin().thenPlay("strike"))
                .triggerableAnim("cast", RawAnimation.begin().thenPlay("cast"))
                .triggerableAnim("flinch", RawAnimation.begin().thenPlay("flinch"))
                .triggerableAnim("run", RawAnimation.begin().thenPlay("run")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
