package com.ivalice.combat;

import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.entity.IvaliceMonsterStats;
import com.ivalice.item.IvaliceCatalogItem;
import com.ivalice.item.IvaliceItemCategory;
import com.ivalice.job.ZodiacJob;
import com.ivalice.job.ZodiacJobs;
import com.ivalice.network.IvaliceStatePayload;
import com.ivalice.registry.IvaliceItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative encounters. World AI never resolves damage for a locked combatant. */
public final class BattleManager {
    public static final int MAX_ENEMIES = 7;
    public static final int AREA_MP_SURCHARGE = 6;
    private static final float AREA_DAMAGE_SCALE = 0.60F;
    private static final double ENCOUNTER_RADIUS = 10.0;
    private static final Map<UUID, Session> BATTLES = new HashMap<>();
    private static final Map<UUID, Session> ENEMIES = new HashMap<>();
    private static int nextRevision = 1;

    private BattleManager() {}

    private static final class Session {
        final ServerPlayer player;
        final List<IvaliceMonsterEntity> enemies;
        final Set<UUID> revealed = new HashSet<>();
        final int maxMp;
        int mp;
        int round = 1;
        int revision = nextRevision++;
        boolean resolving;

        Session(ServerPlayer player, List<IvaliceMonsterEntity> enemies) {
            this.player = player;
            this.enemies = enemies;
            this.maxMp = IvalicePlayerData.get(player).unlockedLicenses.contains("MAGICK_LORE_1") ? 150 : 100;
            this.mp = maxMp;
        }
    }

    private enum Action {
        ATTACK(0, null, 0), FIRE(12, "Fire", 12), BLIZZARD(12, "Ice", 12),
        THUNDER(22, "Thunder", 13), AERO(14, "Wind", 12), WATER_MAGIC(14, "Water", 12),
        CURE(10, null, 0), HASTEGA(18, null, 0), POTION(0, null, 0),
        LIBRA(0, null, 0), STEAL(0, null, 0), GAMBIT(0, null, 0), FLEE(0, null, 0);

        final int mp;
        final String element;
        final float damage;

        Action(int mp, String element, float damage) {
            this.mp = mp;
            this.element = element;
            this.damage = damage;
        }

        static Action fromId(String id) {
            try {
                return valueOf(id.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    public static boolean engage(ServerPlayer player, IvaliceMonsterEntity target) {
        Session current = BATTLES.get(player.getUUID());
        if (current != null) {
            sendState(current, "Escolha a próxima ação.");
            return true;
        }
        if (!player.isAlive() || player.isSpectator() || !target.isAlive() || isEngaged(target)
                || player.distanceToSqr(target) > 36 || !player.hasLineOfSight(target)) {
            player.sendSystemMessage(Component.literal("Aproxime-se e mire em uma criatura livre para batalhar."));
            return false;
        }

        List<IvaliceMonsterEntity> enemies = new ArrayList<>();
        enemies.add(target);
        player.level().getEntitiesOfClass(IvaliceMonsterEntity.class, player.getBoundingBox().inflate(ENCOUNTER_RADIUS),
                mob -> mob != target && mob.isAlive() && !isEngaged(mob)
                        && player.distanceToSqr(mob) <= ENCOUNTER_RADIUS * ENCOUNTER_RADIUS
                        && player.hasLineOfSight(mob)
                        && (!"Neutral".equalsIgnoreCase(mob.getStats().behavior()) || mob.getTarget() == player))
                .stream().sorted(Comparator.comparingDouble(player::distanceToSqr))
                .limit(MAX_ENEMIES - 1L).forEach(enemies::add);

        Session session = new Session(player, enemies);
        BATTLES.put(player.getUUID(), session);
        var profile = IvalicePlayerData.get(player);
        boolean changed = false;
        for (IvaliceMonsterEntity mob : enemies) {
            ENEMIES.put(mob.getUUID(), session);
            mob.setTarget(null);
            mob.getNavigation().stop();
            mob.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            if (profile.getKills(mob.getSpeciesId()) >= 3) {
                session.revealed.add(mob.getUUID());
            }
            changed |= profile.encounteredMonsters.add(mob.getSpeciesId());
        }
        if (changed) {
            IvalicePlayerData.save(player);
        }
        sendState(session, enemies.size() == 1 ? "Um inimigo se aproxima." : enemies.size() + " inimigos no encontro.");
        return true;
    }

    public static boolean isEngaged(IvaliceMonsterEntity monster) {
        return ENEMIES.containsKey(monster.getUUID());
    }

    public static boolean isInBattle(ServerPlayer player) {
        return BATTLES.containsKey(player.getUUID());
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Session session = BATTLES.get(player.getUUID());
        if (session != null) {
            if (!player.isAlive() || player.isSpectator()) {
                finish(session, "Encontro encerrado.");
            } else if (prune(session)) {
                if (session.enemies.isEmpty()) {
                    finish(session, "Encontro encerrado.");
                } else {
                    session.revision = nextRevision++;
                    sendState(session, "Os alvos mudaram.");
                }
            }
        } else if (player.isAlive() && player.tickCount % 40 == 0) {
            var profile = IvalicePlayerData.get(player);
            boolean changed = false;
            for (IvaliceMonsterEntity mob : player.level().getEntitiesOfClass(IvaliceMonsterEntity.class,
                    player.getBoundingBox().inflate(12), mob -> mob.isAlive() && player.hasLineOfSight(mob))) {
                changed |= profile.encounteredMonsters.add(mob.getSpeciesId());
            }
            if (changed) {
                IvalicePlayerData.save(player);
            }
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        IvalicePlayerData.unload(event.getEntity().getUUID());
        Session session = BATTLES.get(event.getEntity().getUUID());
        if (session != null) {
            release(session);
        }
    }

    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof IvaliceMonsterEntity mob) {
            Session session = ENEMIES.get(mob.getUUID());
            if (session != null && (!session.resolving || event.getSource().getEntity() != session.player)) {
                event.setCanceled(true);
            }
        } else if (event.getEntity() instanceof ServerPlayer player
                && event.getSource().getEntity() instanceof Mob attacker) {
            Session session = BATTLES.get(player.getUUID());
            if (session != null && (!session.resolving || ENEMIES.get(attacker.getUUID()) != session)) {
                event.setCanceled(true);
            }
        }
    }

    public static void executeTurnAction(ServerPlayer player, int targetId, String actionId, int revision) {
        Session session = BATTLES.get(player.getUUID());
        if (session == null) {
            sendClosed(player, "Mire em uma criatura e pressione Num 2.");
            return;
        }
        if (revision != 0 && revision != session.revision) {
            sendState(session, "A rodada já mudou.");
            return;
        }
        boolean area = actionId.endsWith("_area");
        String baseId = area ? actionId.substring(0, actionId.length() - "_area".length()) : actionId;
        Action action = Action.fromId(baseId);
        if (action == Action.FLEE && !area) {
            finish(session, "Você recuou.");
            return;
        }
        prune(session);
        if (!player.isAlive() || session.enemies.isEmpty()) {
            finish(session, "Encontro encerrado.");
            return;
        }
        IvaliceMonsterEntity target = session.enemies.stream()
                .filter(mob -> mob.getId() == targetId).findFirst().orElse(null);
        if (action == null || target == null || (area && action.element == null)) {
            sendState(session, "Escolha um alvo válido.");
            return;
        }
        var profile = IvalicePlayerData.get(player);
        if (action == Action.GAMBIT) {
            if (profile.activeGambits.isEmpty()) {
                sendState(session, "Ative os Gambits no menu Personagem.");
                return;
            }
            action = chooseGambit(session, target);
        }
        if (!canUse(player, action)) {
            sendState(session, "Suas profissões não permitem esta ação ou equipamento.");
            return;
        }
        int cost = action.mp + (area ? AREA_MP_SURCHARGE : 0);
        if (session.mp < cost) {
            sendState(session, "MP insuficiente.");
            return;
        }
        if (action == Action.POTION && player.getInventory().countItem(IvaliceItems.HI_POTION.get()) == 0) {
            sendState(session, "Você precisa de uma Hi-Potion.");
            return;
        }

        session.revision = nextRevision++;
        List<IvaliceMonsterEntity> order = session.enemies.stream().sorted(
                Comparator.comparingDouble(BattleManager::speed).reversed()).toList();
        double playerSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 3.0;
        boolean doubleAttack = action == Action.ATTACK && playerSpeed >= speed(order.getFirst()) * 1.35;
        boolean acted = false;
        int counters = 0;
        String result = "";
        session.resolving = true;
        try {
            for (IvaliceMonsterEntity enemy : order) {
                if (!acted && playerSpeed >= speed(enemy)) {
                    result = act(session, action, target, doubleAttack, area, cost);
                    acted = true;
                }
                if (!player.isAlive()) {
                    break;
                }
                if (enemy.isAlive()) {
                    player.setInvulnerableTime(0);
                    player.hurtServer(player.level(), player.level().damageSources().mobAttack(enemy),
                            (float) Math.max(1.5, enemy.getStats().attackDamage() * 0.45));
                    counters++;
                }
            }
            if (!acted && player.isAlive()) {
                result = act(session, action, target, false, area, cost);
            }
        } finally {
            session.resolving = false;
        }
        prune(session);
        if (!player.isAlive()) {
            finish(session, "Você caiu em combate.");
        } else if (session.enemies.isEmpty()) {
            finish(session, "Vitória!");
        } else {
            session.round++;
            sendState(session, result + "  •  Respostas inimigas: " + counters);
        }
    }

    private static String act(Session session, Action action, IvaliceMonsterEntity target, boolean doubleAttack,
                              boolean area, int cost) {
        ServerPlayer player = session.player;
        var profile = IvalicePlayerData.get(player);
        int level = profile.getBattleLevel();
        session.mp -= cost;
        switch (action) {
            case ATTACK -> {
                float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) + 3.0F + (level - 1) * 0.65F;
                strike(player, target, calculateFieldDamage(player, target, base), false);
                if (doubleAttack) {
                    IvaliceMonsterEntity second = target.isAlive() ? target : session.enemies.stream()
                            .filter(IvaliceMonsterEntity::isAlive).findFirst().orElse(null);
                    if (second != null) {
                        strike(player, second, calculateFieldDamage(player, second, base), false);
                        return "Ataque duplo";
                    }
                }
                return "Ataque";
            }
            case CURE -> player.heal(8.0F + (level - 1) * 0.35F);
            case HASTEGA -> player.addEffect(new MobEffectInstance(MobEffects.SPEED, 600, 1));
            case POTION -> {
                if (consumePotion(player)) player.heal(12.0F);
            }
            case LIBRA -> {
                session.revealed.add(target.getUUID());
                return "Libra • Fraqueza: " + target.getStats().weakness();
            }
            case STEAL -> {
                return steal(player, target);
            }
            default -> {
                int jobPower = profile.jobs.stream().map(ZodiacJob::byId).filter(java.util.Objects::nonNull)
                        .mapToInt(job -> job.magick).sum();
                float bonus = (profile.unlockedLicenses.contains("MAGICK_LORE_1") ? 1.25F : 1.0F) + jobPower * 0.06F;
                if (area) {
                    for (IvaliceMonsterEntity enemy : session.enemies) {
                        castDamage(player, enemy, action, bonus * AREA_DAMAGE_SCALE);
                    }
                    return spellName(action) + " em área";
                }
                castDamage(player, target, action, bonus);
            }
        }
        return switch (action) {
            case POTION -> "Hi-Potion";
            case CURE -> "Cura";
            case HASTEGA -> "Hastega";
            default -> spellName(action);
        };
    }

    private static boolean canUse(ServerPlayer player, Action action) {
        return switch (action) {
            case ATTACK -> ZodiacJobs.permits(player, player.getMainHandItem());
            case CURE -> ZodiacJobs.permits(player, IvaliceItemCategory.MAGICK_WHITE);
            case HASTEGA -> ZodiacJobs.permits(player, IvaliceItemCategory.MAGICK_TIME_ARCANE);
            case FIRE, BLIZZARD, THUNDER, AERO, WATER_MAGIC -> ZodiacJobs.permits(player, IvaliceItemCategory.MAGICK_BLACK);
            default -> true;
        };
    }

    private static String spellName(Action action) {
        return switch (action) {
            case BLIZZARD -> "Blizzard";
            case AERO -> "Aero";
            case WATER_MAGIC -> "Aqua";
            default -> action.element;
        };
    }

    private static Action chooseGambit(Session session, IvaliceMonsterEntity target) {
        if (canUse(session.player, Action.CURE) && session.mp >= Action.CURE.mp && session.player.getHealth() < session.player.getMaxHealth() * 0.5F) {
            return Action.CURE;
        }
        for (Action spell : List.of(Action.FIRE, Action.BLIZZARD, Action.THUNDER, Action.AERO, Action.WATER_MAGIC)) {
            if (canUse(session.player, spell) && spell.mp <= session.mp && spell.element.equalsIgnoreCase(target.getStats().weakness())) {
                return spell;
            }
        }
        return Action.ATTACK;
    }

    private static boolean consumePotion(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(IvaliceItems.HI_POTION.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static String steal(ServerPlayer player, IvaliceMonsterEntity target) {
        Item loot = IvaliceItems.getItemById(target.getStats().stealItemId());
        if (target.isStolenFrom() || loot == null) {
            return "Nenhum espólio para roubar.";
        }
        target.setStolenFrom(true);
        player.getInventory().placeItemBackInInventory(new ItemStack(loot), Prediction.SERVER_ONLY);
        return "Roubou " + target.getStats().stealItemId() + ".";
    }

    private static void castDamage(ServerPlayer player, IvaliceMonsterEntity target, Action spell, float bonus) {
        if (spell.element != null) {
            float base = spell.damage + (IvalicePlayerData.get(player).getBattleLevel() - 1) * 0.4F;
            strike(player, target, base * bonus * getElementalMultiplier(target.getStats(), spell.element), true);
        }
    }

    private static void strike(ServerPlayer player, IvaliceMonsterEntity target, float amount, boolean magic) {
        if (target.isAlive()) {
            target.setInvulnerableTime(0);
            ServerLevel level = player.level();
            target.hurtWithCalculatedDamage(level, magic ? level.damageSources().indirectMagic(player, player)
                    : level.damageSources().playerAttack(player), amount);
        }
    }

    public static void castFieldSpell(ServerPlayer player, IvaliceMonsterEntity target, String id) {
        if (isInBattle(player) || isEngaged(target)) {
            return;
        }
        Action spell = Action.fromId(id);
        if (spell == Action.LIBRA) {
            player.sendSystemMessage(Component.literal("Libra • Fraqueza: " + target.getStats().weakness()));
        } else if (spell == Action.STEAL) {
            player.sendSystemMessage(Component.literal(steal(player, target)));
        } else if (spell != null) {
            castDamage(player, target, spell, 1.0F);
        }
    }

    public static void onPlayerFieldStrike(ServerPlayer player, IvaliceMonsterEntity monster) {
        player.sendOverlayMessage(Component.literal(monster.getStats().displayName() + " • "
                + (int) Math.ceil(monster.getHealth()) + " HP • Num 2 para batalhar"));
    }

    private static double speed(IvaliceMonsterEntity mob) {
        return mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    private static boolean prune(Session session) {
        return session.enemies.removeIf(mob -> {
            if (mob.isAlive() && mob.level() == session.player.level() && session.player.distanceToSqr(mob) <= 256) {
                return false;
            }
            ENEMIES.remove(mob.getUUID(), session);
            return true;
        });
    }

    private static void release(Session session) {
        BATTLES.remove(session.player.getUUID(), session);
        for (IvaliceMonsterEntity mob : session.enemies) {
            ENEMIES.remove(mob.getUUID(), session);
        }
    }

    private static void finish(Session session, String message) {
        release(session);
        sendClosed(session.player, message);
    }

    private static void sendClosed(ServerPlayer player, String message) {
        CompoundTag state = new CompoundTag();
        state.putString("kind", "closed");
        state.putString("message", message);
        IvaliceStatePayload.send(player, state);
    }

    private static void sendState(Session session, String message) {
        CompoundTag state = new CompoundTag();
        state.putString("kind", "battle");
        state.putString("message", message);
        state.putInt("round", session.round);
        state.putInt("revision", session.revision);
        state.putInt("mp", session.mp);
        state.putInt("maxMp", session.maxMp);
        state.putBoolean("black", canUse(session.player, Action.FIRE));
        state.putBoolean("white", canUse(session.player, Action.CURE));
        state.putBoolean("time", canUse(session.player, Action.HASTEGA));
        ListTag enemies = new ListTag();
        for (IvaliceMonsterEntity mob : session.enemies) {
            CompoundTag enemy = new CompoundTag();
            enemy.putInt("id", mob.getId());
            enemy.putString("species", mob.getSpeciesId());
            enemy.putInt("hp", (int) Math.ceil(mob.getHealth()));
            enemy.putBoolean("libra", session.revealed.contains(mob.getUUID()));
            enemies.add(enemy);
        }
        state.put("enemies", enemies);
        IvaliceStatePayload.send(session.player, state);
    }

    public static float calculateFieldDamage(ServerPlayer player, IvaliceMonsterEntity monster, float baseAmount) {
        IvaliceMonsterStats stats = monster.getStats();
        ItemStack held = player.getMainHandItem();
        float mult = 1.0F;
        if (held.getItem() instanceof IvaliceCatalogItem catalogItem) {
            String desc = catalogItem.getDescriptionText().toLowerCase();
            if (desc.contains("fogo") || desc.contains("fire")) mult = getElementalMultiplier(stats, "Fire");
            else if (desc.contains("gelo") || desc.contains("ice")) mult = getElementalMultiplier(stats, "Ice");
            else if (desc.contains("trovão") || desc.contains("thunder")) mult = getElementalMultiplier(stats, "Thunder");
            else if (desc.contains("água") || desc.contains("water")) mult = getElementalMultiplier(stats, "Water");
            else if (desc.contains("vento") || desc.contains("wind")) mult = getElementalMultiplier(stats, "Wind");
            else if (desc.contains("sagrado") || desc.contains("holy")) mult = getElementalMultiplier(stats, "Holy");
        }
        return baseAmount * mult;
    }

    public static float getElementalMultiplier(IvaliceMonsterStats stats, String element) {
        if (stats.weakness().equalsIgnoreCase(element)) {
            return 2.0F;
        }
        if (stats.resistance().equalsIgnoreCase(element)) {
            return 0.5F;
        }
        return 1.0F;
    }

    public static void onMonsterDefeated(ServerPlayer player, IvaliceMonsterEntity monster) {
        IvaliceMonsterStats stats = monster.getStats();
        IvalicePlayerData.PlayerProfile profile = IvalicePlayerData.get(player);

        profile.recordKill(stats.id(), stats.gilReward(), stats.lpReward());
        int kills = profile.getKills(stats.id());

        player.sendSystemMessage(Component.literal(String.format(
                "★ VITÓRIA! %s derrotado! Recompensas: +%d Gil | +%d LP | Bestiário: %d abate(s)",
                stats.displayName(), stats.gilReward(), stats.lpReward(), kills
        )).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

        if (kills == 1) {
            player.sendSystemMessage(Component.literal("[Clan Primer] Nova entrada registrada no Bestiário: " + stats.displayName() + "!")
                    .withStyle(ChatFormatting.AQUA));
        } else if (kills == 3) {
            player.sendSystemMessage(Component.literal("[Sage Knowledge] Página secreta do Bestiário desbloqueada para " + stats.displayName() + "!")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        // Verifica se este monstro era uma Caçada (Clan Centurio Mark Hunt)
        for (IvalicePlayerData.MarkHuntEntry hunt : IvalicePlayerData.MARK_HUNTS) {
            if (hunt.entityId().equalsIgnoreCase(stats.id()) && !profile.completedHunts.contains(hunt.id())) {
                profile.completedHunts.add(hunt.id());
                if (hunt.id().equals(profile.activeHuntId)) {
                    profile.activeHuntId = "";
                }
                profile.gil += hunt.gilReward();
                profile.lp += hunt.lpReward();

                Item bounty = IvaliceItems.getItemById(hunt.bountyItemId());
                if (bounty != null) {
                    player.getInventory().add(new ItemStack(bounty, 1));
                }

                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetTitleTextPacket(
                        Component.literal("MARK HUNT COMPLETE!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                ));
                player.connection.send(new ClientboundSetSubtitleTextPacket(
                        Component.literal(hunt.markName() + " — +" + hunt.gilReward() + " Gil & +" + hunt.lpReward() + " LP").withStyle(ChatFormatting.YELLOW)
                ));

                player.sendSystemMessage(Component.literal(String.format(
                        "═══ [CLÃ CENTURIO — CAÇADA CONCLUÍDA!] ═══\nAlvo: %s (%s)\nRecompensa de Montblanc: +%d Gil, +%d LP e 1x %s!\nNovo Rank do Clã: %s",
                        hunt.markName(), hunt.rank(), hunt.gilReward(), hunt.lpReward(), hunt.bountyItemId(), profile.getClanRank()
                )).withStyle(ChatFormatting.GOLD));
            }
        }

        IvalicePlayerData.save(player);
    }

}
