package com.ivalice.combat;

import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceBestiary;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.entity.IvaliceMonsterStats;
import com.ivalice.item.IvaliceCatalogItem;
import com.ivalice.item.IvaliceItemCategory;
import com.ivalice.item.IvaliceUseItem;
import com.ivalice.job.ZodiacJob;
import com.ivalice.job.ZodiacJobs;
import com.ivalice.network.IvaliceStatePayload;
import com.ivalice.registry.IvaliceItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
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
        Round roundInProgress;

        Session(ServerPlayer player, List<IvaliceMonsterEntity> enemies) {
            this.player = player;
            this.enemies = enemies;
            var profile = IvalicePlayerData.get(player);
            int baseMp = 90 + profile.getBattleLevel() * 4;
            if (IvaliceEquipment.has(player, "tza_leather_gorget")) baseMp += 30;
            if (IvaliceEquipment.has(player, "dawn_shard")) baseMp = 0;
            if (profile.unlockedLicenses.contains("MAGICK_LORE_1")) baseMp += 50;
            if (profile.unlockedLicenses.contains("MAGICK_LORE_2")) baseMp += 60;
            if (profile.unlockedLicenses.contains("ARCANE_SUPREMACY")) baseMp += 100;
            this.maxMp = baseMp;
            this.mp = maxMp;
        }
    }

    private static final class Round {
        final List<Beat> beats = new ArrayList<>();
        int index;
        int age;
        int counters;
        String result = "";
    }

    private record Beat(LivingEntity actor, List<? extends LivingEntity> targets, BattleVisual.Style style,
                        String label, Runnable apply) {}

    public enum Action {
        ATTACK(0, null, 0, 1, 1, "Ataque", null),
        // Tier 1 (Nível 1 a 8)
        FIRE(10, "Fire", 12, 1, 1, "Fire", IvaliceItemCategory.MAGICK_BLACK),
        BLIZZARD(10, "Ice", 12, 1, 3, "Blizzard", IvaliceItemCategory.MAGICK_BLACK),
        THUNDER(12, "Thunder", 13, 1, 4, "Thunder", IvaliceItemCategory.MAGICK_BLACK),
        WATER_MAGIC(11, "Water", 12, 1, 5, "Aqua", IvaliceItemCategory.MAGICK_BLACK),
        AERO(11, "Wind", 12, 1, 7, "Aero", IvaliceItemCategory.MAGICK_BLACK),
        DARK(12, "Dark", 13, 1, 8, "Dark", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        CURE(8, null, 0, 1, 1, "Cure", IvaliceItemCategory.MAGICK_WHITE),

        // Tier 2 (Nível 14 a 28 — Vendidas até Tier 2)
        CURA(18, null, 0, 2, 14, "Cura", IvaliceItemCategory.MAGICK_WHITE),
        FIRA(20, "Fire", 26, 2, 16, "Fira", IvaliceItemCategory.MAGICK_BLACK),
        BLIZZARA(20, "Ice", 26, 2, 18, "Blizzara", IvaliceItemCategory.MAGICK_BLACK),
        THUNDARA(22, "Thunder", 28, 2, 20, "Thundara", IvaliceItemCategory.MAGICK_BLACK),
        AQUARA(20, "Water", 26, 2, 22, "Aquara", IvaliceItemCategory.MAGICK_BLACK),
        AERORA(20, "Wind", 26, 2, 25, "Aerora", IvaliceItemCategory.MAGICK_BLACK),
        DARKRA(22, "Dark", 28, 2, 28, "Darkra", IvaliceItemCategory.MAGICK_TIME_ARCANE),

        // Tier 3 -ga (Nível 36 a 56 — Somente Fabricadas em Alquimia & Bazaar)
        HASTEGA(24, null, 0, 3, 36, "Hastega", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        CURAGA(30, null, 0, 3, 38, "Curaga", IvaliceItemCategory.MAGICK_WHITE),
        FIRAGA(36, "Fire", 52, 3, 42, "Firaga", IvaliceItemCategory.MAGICK_BLACK),
        BLIZZAGA(36, "Ice", 52, 3, 44, "Blizzaga", IvaliceItemCategory.MAGICK_BLACK),
        THUNDAGA(38, "Thunder", 54, 3, 46, "Thundaga", IvaliceItemCategory.MAGICK_BLACK),
        WATERGA(36, "Water", 52, 3, 50, "Waterga", IvaliceItemCategory.MAGICK_BLACK),
        AEROGA(36, "Wind", 52, 3, 52, "Aeroga", IvaliceItemCategory.MAGICK_BLACK),
        DARKGA(38, "Dark", 55, 3, 56, "Darkga", IvaliceItemCategory.MAGICK_TIME_ARCANE),

        // Tier 4 Cataclísmicas (Nível 68 a 95 — Fabricadas com Relíquias do Nether & The End)
        HOLY(56, "Holy", 92, 4, 68, "Holy", IvaliceItemCategory.MAGICK_WHITE),
        FLARE(52, "Fire", 86, 4, 74, "Flare", IvaliceItemCategory.MAGICK_BLACK),
        ARDOR(64, "Fire", 108, 4, 82, "Ardor", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        COLLAPSE(66, "Dark", 114, 4, 88, "Collapse", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        SCATHE(68, "Holy", 125, 4, 95, "Scathe", IvaliceItemCategory.MAGICK_BLACK),

        BLINDNA(5, null, 0, 1, 2, "Blindna", IvaliceItemCategory.MAGICK_WHITE),
        VOX(5, null, 0, 1, 2, "Vox", IvaliceItemCategory.MAGICK_WHITE),
        POISONA(5, null, 0, 1, 3, "Poisona", IvaliceItemCategory.MAGICK_WHITE),
        RAISE(18, null, 0, 2, 13, "Raise", IvaliceItemCategory.MAGICK_WHITE),
        STONA(8, null, 0, 1, 9, "Stona", IvaliceItemCategory.MAGICK_WHITE),
        REGEN(12, null, 0, 1, 9, "Regen", IvaliceItemCategory.MAGICK_WHITE),
        CLEANSE(10, null, 0, 1, 10, "Cleanse", IvaliceItemCategory.MAGICK_WHITE),
        ESUNA(16, null, 0, 2, 15, "Esuna", IvaliceItemCategory.MAGICK_WHITE),
        CURAJA(34, null, 0, 3, 44, "Curaja", IvaliceItemCategory.MAGICK_WHITE),
        DISPEL(12, null, 0, 2, 18, "Dispel", IvaliceItemCategory.MAGICK_WHITE),
        DISPELGA(24, null, 0, 3, 35, "Dispelga", IvaliceItemCategory.MAGICK_WHITE),
        RENEW(40, null, 0, 4, 54, "Renew", IvaliceItemCategory.MAGICK_WHITE),
        ARISE(36, null, 0, 4, 46, "Arise", IvaliceItemCategory.MAGICK_WHITE),
        ESUNAGA(26, null, 0, 3, 34, "Esunaga", IvaliceItemCategory.MAGICK_WHITE),
        BIO(20, null, 0, 2, 20, "Bio", IvaliceItemCategory.MAGICK_BLACK),
        SHOCK(32, null, 0, 3, 40, "Shock", IvaliceItemCategory.MAGICK_BLACK),
        SCOURGE(31, null, 0, 3, 42, "Scourge", IvaliceItemCategory.MAGICK_BLACK),
        BLIND(7, null, 0, 1, 3, "Blind", IvaliceItemCategory.MAGICK_BLACK),
        OIL(8, null, 0, 1, 6, "Oil", IvaliceItemCategory.MAGICK_BLACK),
        POISON(8, null, 0, 1, 5, "Poison", IvaliceItemCategory.MAGICK_BLACK),
        SILENCE(8, null, 0, 1, 5, "Silence", IvaliceItemCategory.MAGICK_BLACK),
        SLEEP(9, null, 0, 1, 8, "Sleep", IvaliceItemCategory.MAGICK_BLACK),
        BLINDGA(16, null, 0, 2, 22, "Blindga", IvaliceItemCategory.MAGICK_BLACK),
        TOXIFY(20, null, 0, 2, 26, "Toxify", IvaliceItemCategory.MAGICK_BLACK),
        SILENCEGA(18, null, 0, 2, 23, "Silencega", IvaliceItemCategory.MAGICK_BLACK),
        SLEEPGA(18, null, 0, 2, 25, "Sleepga", IvaliceItemCategory.MAGICK_BLACK),
        HASTE(11, null, 0, 1, 10, "Haste", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        FLOAT(9, null, 0, 1, 7, "Float", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        SLOW(8, null, 0, 1, 4, "Slow", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        IMMOBILIZE(9, null, 0, 1, 6, "Immobilize", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        DISABLE(10, null, 0, 1, 8, "Disable", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        BLEED(12, null, 0, 2, 12, "Bleed", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        BREAK(15, null, 0, 2, 15, "Break", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        STOP(18, null, 0, 2, 20, "Stop", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        SLOWGA(20, null, 0, 2, 25, "Slowga", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        COUNTDOWN(20, null, 0, 3, 30, "Countdown", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        REFLECT(14, null, 0, 2, 16, "Reflect", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        REFLECTGA(28, null, 0, 3, 32, "Reflectga", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        BALANCE(14, null, 0, 2, 12, "Balance", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        WARP(22, null, 0, 3, 29, "Warp", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        PROTECT(9, null, 0, 1, 6, "Protect", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        SHELL(9, null, 0, 1, 6, "Shell", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        BRAVERY(15, null, 0, 2, 14, "Bravery", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        FAITH(15, null, 0, 2, 14, "Faith", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        PROTECTGA(23, null, 0, 2, 26, "Protectga", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        SHELLGA(23, null, 0, 2, 26, "Shellga", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        REVERSE(25, null, 0, 3, 28, "Reverse", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        BERSERK(10, null, 0, 1, 8, "Berserk", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        DEATH(25, null, 0, 3, 34, "Death", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        CONFUSE(12, null, 0, 2, 13, "Confuse", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        DECOY(9, null, 0, 1, 7, "Decoy", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        VANISH(10, null, 0, 1, 9, "Vanish", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        VANISHGA(22, null, 0, 2, 25, "Vanishga", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        DRAIN(14, null, 0, 2, 12, "Drain", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        SYPHON(4, null, 0, 2, 14, "Syphon", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        BUBBLE(20, null, 0, 2, 18, "Bubble", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        GRAVITY(13, null, 0, 1, 10, "Gravity", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        GRAVIGA(26, null, 0, 2, 26, "Graviga", IvaliceItemCategory.MAGICK_TIME_ARCANE),
        FIRST_AID(0, null, 0, 1, 1, "First Aid", IvaliceItemCategory.TECHNICK),
        SHADES_OF_BLACK(0, null, 0, 1, 4, "Shades of Black", IvaliceItemCategory.TECHNICK),
        HOROLOGY(0, null, 0, 1, 5, "Horology", IvaliceItemCategory.TECHNICK),
        STAMP(0, null, 0, 1, 5, "Stamp", IvaliceItemCategory.TECHNICK),
        ACHILLES(0, null, 0, 1, 6, "Achilles", IvaliceItemCategory.TECHNICK),
        CHARGE(0, null, 0, 1, 6, "Charge", IvaliceItemCategory.TECHNICK),
        INFUSE(0, null, 0, 1, 7, "Infuse", IvaliceItemCategory.TECHNICK),
        SOULEATER(0, null, 0, 1, 7, "Souleater", IvaliceItemCategory.TECHNICK),
        WITHER(0, null, 0, 1, 8, "Wither", IvaliceItemCategory.TECHNICK),
        ADDLE(0, null, 0, 1, 8, "Addle", IvaliceItemCategory.TECHNICK),
        BONECRUSHER(0, null, 0, 1, 9, "Bonecrusher", IvaliceItemCategory.TECHNICK),
        TELEKINESIS(0, null, 0, 1, 9, "Telekinesis", IvaliceItemCategory.TECHNICK),
        EXPOSE(0, null, 0, 1, 10, "Expose", IvaliceItemCategory.TECHNICK),
        SHEAR(0, null, 0, 1, 10, "Shear", IvaliceItemCategory.TECHNICK),
        CHARM(0, null, 0, 1, 11, "Charm", IvaliceItemCategory.TECHNICK),
        REVIVE(0, null, 0, 1, 11, "Revive", IvaliceItemCategory.TECHNICK),
        SIGHT_UNSEEING(0, null, 0, 2, 12, "Sight Unseeing", IvaliceItemCategory.TECHNICK),
        NUMEROLOGY(0, null, 0, 2, 12, "Numerology", IvaliceItemCategory.TECHNICK),
        POACH(0, null, 0, 2, 13, "Poach", IvaliceItemCategory.TECHNICK),
        NEEDLES(0, null, 0, 2, 13, "1000 Needles", IvaliceItemCategory.TECHNICK),
        TRAVELER(0, null, 0, 2, 14, "Traveler", IvaliceItemCategory.TECHNICK),
        GIL_TOSS(0, null, 0, 2, 14, "Gil Toss", IvaliceItemCategory.TECHNICK),

        POTION(0, null, 0, 1, 1, "Hi-Potion", null),
        ITEM_USE(0, null, 0, 1, 1, "Item", null),
        LIBRA(0, null, 0, 1, 1, "Libra", null),
        STEAL(0, null, 0, 1, 2, "Steal", null),
        GAMBIT(0, null, 0, 1, 1, "Gambit", null),
        FLEE(0, null, 0, 1, 1, "Fugir", null);

        public final int mp;
        public final String element;
        public final float damage;
        public final int tier;
        public final int minLevel;
        public final String label;
        public final IvaliceItemCategory school;

        Action(int mp, String element, float damage, int tier, int minLevel, String label, IvaliceItemCategory school) {
            this.mp = mp;
            this.element = element;
            this.damage = damage;
            this.tier = tier;
            this.minLevel = minLevel;
            this.label = label;
            this.school = school;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public boolean isInnateArea() {
            return tier >= 3 && element != null;
        }

        public boolean targetsSelf() {
            return this == CURE || this == CURA || this == CURAGA || this == HASTEGA || this == POTION
                    || IvaliceActionEffects.targetsSelf(this);
        }

        public boolean offersArea() {
            return element != null || IvaliceActionEffects.isArea(this);
        }

        public static Action fromId(String id) {
            try {
                return valueOf(id.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    public static boolean engage(ServerPlayer player, IvaliceMonsterEntity target) {
        if (target.isSummoned()) return false;
        Session current = BATTLES.get(player.getUUID());
        if (current != null) {
            if (current.roundInProgress != null) sendAnimation(current, "Rodada em andamento");
            else sendState(current, "Escolha a próxima ação.");
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
                mob -> mob != target && mob.isAlive() && !mob.isSummoned() && !isEngaged(mob)
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
            } else if (session.roundInProgress != null) {
                advanceRound(session);
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
        if (event.getSource().is(DamageTypes.GENERIC_KILL)) return;
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
        boolean area = actionId.endsWith("_area");
        String baseId = area ? actionId.substring(0, actionId.length() - "_area".length()) : actionId;
        String itemId = baseId.startsWith("item_") ? baseId.substring(5) : null;
        Action action = itemId == null ? Action.fromId(baseId) : Action.ITEM_USE;
        if (action == Action.FLEE && !area) {
            finish(session, "Você recuou.");
            return;
        }
        if (session.roundInProgress != null) {
            sendAnimation(session, "Rodada em andamento");
            return;
        }
        if (revision != 0 && revision != session.revision) {
            sendState(session, "A rodada já mudou.");
            return;
        }
        prune(session);
        if (!player.isAlive() || session.enemies.isEmpty()) {
            finish(session, "Encontro encerrado.");
            return;
        }
        IvaliceMonsterEntity target = session.enemies.stream()
                .filter(mob -> mob.getId() == targetId).findFirst().orElse(null);
        if (action != null && action.isInnateArea()) {
            area = true;
        }
        if (action == null || target == null || (area && !action.offersArea())) {
            sendState(session, "Escolha um alvo válido.");
            return;
        }
        var profile = IvalicePlayerData.get(player);
        if (action == Action.GAMBIT) {
            if (profile.activeGambits.isEmpty()) {
                sendState(session, "Ative os Gambits no menu Personagem.");
                return;
            }
            var choice = IvaliceGambits.choose(player, session.enemies, target, session.mp, session.maxMp);
            if (choice == null) {
                action = chooseGambit(session, target);
            } else {
                action = choice.action();
                target = choice.target();
            }
            if (action.isInnateArea()) area = true;
        }
        if (IvaliceStatuses.unableToAct(player) || (action.mp > 0 && player.hasEffect(IvaliceStatuses.SILENCE))) {
            sendState(session, "O estado do personagem impede esta ação.");
            if (IvaliceStatuses.unableToAct(player)) planRound(session, null, target, false, 0);
            return;
        }
        if (!canUse(player, action)) {
            sendState(session, "Você precisa da profissão correta e ter aprendido este pergaminho.");
            return;
        }
        int cost = action.mp + (area && !action.isInnateArea() ? AREA_MP_SURCHARGE : 0);
        if (IvaliceEquipment.has(player, "tza_sage_s_ring")) cost = (cost + 1) / 2;
        boolean useGil = cost > 0 && IvaliceEquipment.has(player, "tza_turtleshell_choker");
        if (useGil && profile.gil < cost) {
            sendState(session, "Gil insuficiente para conjurar com o Turtleshell Choker.");
            return;
        }
        if (!useGil && session.mp < cost) {
            sendState(session, "MP insuficiente.");
            return;
        }
        if (action == Action.POTION && player.getInventory().countItem(IvaliceItems.HI_POTION.get()) == 0) {
            sendState(session, "Você precisa de uma Hi-Potion.");
            return;
        }

        if (action == Action.ITEM_USE) {
            Item item = itemId == null ? null : IvaliceItems.getItemById(itemId);
            if (!(item instanceof IvaliceUseItem consumable) || player.getInventory().countItem(item) < 1
                    || !IvaliceConsumables.applicable(consumable.effect(), battleItemTargets(session, target, consumable.effect()))) {
                sendState(session, "Item indisponível ou alvo incompatível.");
                return;
            }
            session.revision = nextRevision++;
            planRound(session, action, target, false, 0, consumable);
            return;
        }
        session.revision = nextRevision++;
        planRound(session, action, target, area, useGil ? -cost : cost);
    }

    private static void planRound(Session session, Action action, IvaliceMonsterEntity target, boolean area, int cost) {
        planRound(session, action, target, area, cost, null);
    }

    private static void planRound(Session session, Action action, IvaliceMonsterEntity target, boolean area, int cost,
                                  IvaliceUseItem consumable) {
        Round round = new Round();
        session.roundInProgress = round;
        List<IvaliceMonsterEntity> order = session.enemies.stream()
                .sorted(Comparator.comparingDouble(BattleManager::speed).reversed()).toList();
        double playerSpeed = session.player.getAttributeValue(Attributes.MOVEMENT_SPEED) * 3.0;
        boolean doubleAttack = action == Action.ATTACK
                && (playerSpeed >= speed(order.getFirst()) * 1.35
                || (IvaliceEquipment.has(session.player, "genji_gloves") && session.player.getRandom().nextFloat() < .45F));
        boolean queued = false;
        for (IvaliceMonsterEntity enemy : order) {
            if (!queued && playerSpeed >= speed(enemy)) {
                if (action != null) addPlayerBeats(session, round, action, target, area, cost, doubleAttack, consumable);
                queued = true;
            }
            BattleVisual.Style style = enemy.hasEffect(IvaliceStatuses.SILENCE) ? BattleVisual.Style.SLASH : enemyStyle(enemy, session.round);
            round.beats.add(new Beat(enemy, List.of(session.player), style, shortName(enemy) + " • " + enemyMove(enemy, style),
                    () -> applyCounter(session, enemy, style)));
        }
        if (!queued && action != null) addPlayerBeats(session, round, action, target, area, cost, false, consumable);
        sendAnimation(session, "Rodada " + session.round);
    }

    private static void addPlayerBeats(Session session, Round round, Action action, IvaliceMonsterEntity target,
                                       boolean area, int cost, boolean doubleAttack, IvaliceUseItem consumable) {
        if (consumable != null) {
            var effect = consumable.effect();
            var targets = battleItemTargets(session, target, effect);
            round.beats.add(new Beat(session.player, targets, effect.style(), consumable.getCanonicalRef(), () -> {
                var inventory = session.player.getInventory();
                if (!IvaliceConsumables.applicable(effect, battleItemTargets(session, target, effect))) return;
                for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                    ItemStack stack = inventory.getItem(slot);
                    if (!stack.is(consumable)) continue;
                    if (!session.player.getAbilities().instabuild) stack.shrink(1);
                    String result = IvaliceConsumables.apply(session.player, effect, battleItemTargets(session, target, effect));
                    round.result = consumable.getCanonicalRef() + (result.isEmpty() ? "" : " • " + result);
                    return;
                }
            }));
            return;
        }
        List<? extends LivingEntity> targets = action.targetsSelf() ? List.of(session.player)
                : area ? List.copyOf(session.enemies) : List.of(target);
        round.beats.add(new Beat(session.player, targets, style(action), actionLabel(action) + (area && !action.isInnateArea() ? " em área" : ""),
                () -> {
                    round.result = act(session, action, target, area, cost);
                    if (doubleAttack) {
                        IvaliceMonsterEntity second = target.isAlive() ? target : session.enemies.stream()
                                .filter(IvaliceMonsterEntity::isAlive).findFirst().orElse(null);
                        if (second != null) {
                            round.beats.add(round.index + 1, new Beat(session.player, List.of(second),
                                    BattleVisual.Style.CLEAVE, "Combo Duplo", () -> {
                                        act(session, action, second, false, 0);
                                        round.result = "Combo Duplo";
                                    }));
                        }
                    }
                }));
    }

    private static List<? extends LivingEntity> battleItemTargets(Session session, IvaliceMonsterEntity target,
                                                                  IvaliceConsumables.Use effect) {
        if (!effect.needsEnemy(session.player)) return supportTargets(session.player, effect);
        return effect.area() ? session.enemies.stream().filter(LivingEntity::isAlive).toList()
                : target.isAlive() ? List.of(target) : List.of();
    }

    private static List<ServerPlayer> supportTargets(ServerPlayer player, IvaliceConsumables.Use effect) {
        boolean party = switch (effect) {
            case MEGALIXIR, HASTEGA_MOTE, REFLECTGA_MOTE, VANISHGA_MOTE, FLOAT_MOTE, CURA_MOTE -> true;
            default -> false;
        };
        return party ? player.level().getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(10),
                ally -> ally.isAlive() && (ally == player || player.isAlliedTo(ally))) : List.of(player);
    }

    public static boolean useFieldItem(ServerPlayer player, ItemStack stack, IvaliceConsumables.Use effect) {
        if (isInBattle(player) || effect == IvaliceConsumables.Use.ETHER || effect == IvaliceConsumables.Use.HI_ETHER) {
            player.sendOverlayMessage(Component.literal("Use este item no menu da batalha."));
            return false;
        }
        var target = effect.needsEnemy(player) ? aimedEnemy(player) : null;
        List<? extends LivingEntity> targets;
        if (!effect.needsEnemy(player)) {
            targets = supportTargets(player, effect);
        } else if (target == null) {
            targets = List.of();
        } else if (effect.area()) {
            targets = player.level().getEntitiesOfClass(IvaliceMonsterEntity.class, target.getBoundingBox().inflate(4),
                            mob -> mob.isAlive() && !isEngaged(mob) && player.hasLineOfSight(mob)).stream()
                    .sorted(Comparator.comparingDouble(player::distanceToSqr)).limit(MAX_ENEMIES).toList();
        } else {
            targets = List.of(target);
        }
        if (!IvaliceConsumables.applicable(effect, targets)) {
            player.sendOverlayMessage(Component.literal("Mire em um alvo que aceite este item."));
            return false;
        }
        BattleVisual.begin(player.level(), player, effect.style());
        IvaliceConsumables.apply(player, effect, targets);
        player.sendOverlayMessage(stack.getHoverName());
        return true;
    }

    private static void advanceRound(Session session) {
        Round round = session.roundInProgress;
        prune(session);
        if (session.enemies.isEmpty() && round.age == 0) {
            completeRound(session);
            return;
        }
        while (round.index < round.beats.size() && round.age == 0) {
            Beat next = round.beats.get(round.index);
            if (next.actor.isAlive() && next.actor.level() == session.player.level()
                    && (next.actor == session.player || ENEMIES.get(next.actor.getUUID()) == session)
                    && next.targets.stream().anyMatch(target -> target.isAlive() && target.level() == session.player.level()
                            && (target == session.player || ENEMIES.get(target.getUUID()) == session))) {
                break;
            }
            round.index++;
        }
        if (round.index >= round.beats.size()) {
            completeRound(session);
            return;
        }

        Beat beat = round.beats.get(round.index);
        ServerLevel level = session.player.level();
        if (round.age == 0) {
            BattleVisual.begin(level, beat.actor, beat.style);
            sendAnimation(session, beat.label);
        }
        for (LivingEntity target : beat.targets) {
            if (target.level() == level) BattleVisual.animate(level, beat.actor, target, beat.style, round.age);
        }
        if (round.age == beat.style.impactTick && beat.actor.isAlive()) {
            session.resolving = true;
            try {
                beat.apply.run();
            } finally {
                session.resolving = false;
            }
        }
        if (++round.age >= beat.style.endTick()) {
            round.age = 0;
            round.index++;
        }
    }

    private static void applyCounter(Session session, IvaliceMonsterEntity enemy, BattleVisual.Style style) {
        ServerPlayer player = session.player;
        if (!player.isAlive() || ENEMIES.get(enemy.getUUID()) != session || IvaliceStatuses.unableToAct(enemy)) return;
        if (enemy.hasEffect(IvaliceStatuses.CONFUSE) && enemy.getRandom().nextBoolean()) {
            enemy.hurtWithCalculatedDamage(player.level(), player.level().damageSources().mobAttack(enemy),
                    (float) enemy.getAttributeValue(Attributes.ATTACK_DAMAGE) * .4F);
            return;
        }
        if (enemy.hasEffect(IvaliceStatuses.SILENCE)) style = BattleVisual.Style.SLASH;
        if ((style == BattleVisual.Style.SLASH || style == BattleVisual.Style.CLEAVE)
                && enemy.hasEffect(MobEffects.BLINDNESS) && enemy.getRandom().nextFloat() < .55F) return;
        player.damageCooldownTime = 0;
        ServerLevel level = player.level();
        var damage = (style == BattleVisual.Style.SLASH || style == BattleVisual.Style.CLEAVE)
                ? level.damageSources().mobAttack(enemy)
                : level.damageSources().indirectMagic(enemy, enemy);
        float mult = enemy.isAlphaLeader() ? 0.58F : 0.45F;
        float amount = (float) Math.max(1.5, enemy.getAttributeValue(Attributes.ATTACK_DAMAGE) * mult);
        if (IvaliceEquipment.has(player, "tza_jade_collar") && player.getRandom().nextFloat() < .20F) return;
        if (IvaliceEquipment.has(player, "tza_diamond_armlet") && (style == BattleVisual.Style.THUNDER
                || style == BattleVisual.Style.THUNDARA || style == BattleVisual.Style.THUNDAGA)) return;
        if ((IvaliceEquipment.has(player, "tza_tourmaline_ring") || IvaliceEquipment.has(player, "tza_cat_ear_hood"))
                && (style == BattleVisual.Style.ICE || style == BattleVisual.Style.BLIZZARA || style == BattleVisual.Style.BLIZZAGA)) amount *= .5F;
        if ((IvaliceEquipment.has(player, "tza_agate_ring") || IvaliceEquipment.has(player, "tza_cat_ear_hood"))
                && (style == BattleVisual.Style.WIND || style == BattleVisual.Style.AERORA || style == BattleVisual.Style.AEROGA)) amount *= .5F;
        if (IvaliceEquipment.has(player, "tza_sash") && (style == BattleVisual.Style.FIRE
                || style == BattleVisual.Style.FIRA || style == BattleVisual.Style.FIRAGA)) amount *= .5F;
        if (IvaliceEquipment.has(player, "tza_sage_s_ring") && style == BattleVisual.Style.HOLY) {
            player.heal(amount);
            return;
        }
        if (damage.getDirectEntity() == enemy && player.hasEffect(IvaliceStatuses.REFLECT)
                && style != BattleVisual.Style.SLASH && style != BattleVisual.Style.CLEAVE) {
            strike(player, enemy, amount, true);
        } else if (player.hasEffect(IvaliceStatuses.REVERSE)) {
            player.heal(amount);
        } else {
            if (player.hasEffect(IvaliceStatuses.SHELL) && style != BattleVisual.Style.SLASH && style != BattleVisual.Style.CLEAVE) amount *= .65F;
            player.hurtServer(level, damage, amount);
            if (player.isAlive() && style == BattleVisual.Style.DARKGA && enemy.getSpeciesId().equals("ruin_deathgaze")
                    && !player.hasEffect(IvaliceStatuses.DOOM) && !IvaliceEquipment.has(player, "ribbon")) {
                player.addEffect(new MobEffectInstance(IvaliceStatuses.DOOM, 400));
            }
        }
        if (player.isAlive() && IvaliceEquipment.has(player, "tza_battle_harness") && player.getRandom().nextFloat() < .25F) {
            strike(player, enemy, (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE), false);
        }
        session.roundInProgress.counters++;
    }

    private static void completeRound(Session session) {
        Round round = session.roundInProgress;
        session.roundInProgress = null;
        if (!session.player.isAlive()) {
            finish(session, "Você caiu em combate.");
        } else if (session.enemies.isEmpty()) {
            finish(session, "Vitória!");
        } else {
            session.round++;
            sendState(session, round.result + "  •  Respostas inimigas: " + round.counters);
        }
    }

    private static String shortName(IvaliceMonsterEntity enemy) {
        String name = enemy.getStats().displayName();
        int annotation = name.indexOf(" (");
        return annotation < 0 ? name : name.substring(0, annotation);
    }

    private static BattleVisual.Style enemyStyle(IvaliceMonsterEntity enemy, int round) {
        return switch (enemy.getSpeciesId()) {
            case "white_mousse", "ultros", "shinryu" -> BattleVisual.Style.WATERGA;
            case "ichthon" -> BattleVisual.Style.WATER;
            case "belias", "alpha_saurian", "rubicante" -> BattleVisual.Style.FIRAGA;
            case "ifrit_pyreburner", "omega_mk" -> BattleVisual.Style.ARDOR;
            case "firemane", "bomb" -> BattleVisual.Style.FIRA;
            case "mateus" -> BattleVisual.Style.BLIZZAGA;
            case "yeti" -> BattleVisual.Style.BLIZZARA;
            case "coeurl" -> BattleVisual.Style.THUNDARA;
            case "behemoth" -> BattleVisual.Style.THUNDAGA;
            case "malboro", "mindflayer" -> BattleVisual.Style.DARKRA;
            case "skeleton_crusader" -> BattleVisual.Style.DARK;
            case "zodiark", "golbez" -> BattleVisual.Style.COLLAPSE;
            case "atb_weapon" -> BattleVisual.Style.SCATHE;
            case "kefka_god" -> {
                float health = enemy.getHealth() / enemy.getMaxHealth();
                if (health <= .33F) yield round % 2 == 0 ? BattleVisual.Style.SCATHE : BattleVisual.Style.COLLAPSE;
                if (health <= .67F) yield round % 2 == 0 ? BattleVisual.Style.HOLY : BattleVisual.Style.ARDOR;
                yield round % 2 == 0 ? BattleVisual.Style.THUNDAGA : BattleVisual.Style.FIRAGA;
            }
            case "shadowseer" -> round % 2 == 0 ? BattleVisual.Style.DARKGA : BattleVisual.Style.HOLY;
            case "omega_mark_xii" -> round % 2 == 0 ? BattleVisual.Style.THUNDAGA : BattleVisual.Style.SCATHE;
            case "yiazmat" -> BattleVisual.Style.AEROGA;
            case "judge_gabranth", "gilgamesh", "iron_giant", "adamantoise", "gil_turtle", "alpha_wolf", "alpha_hyena" -> BattleVisual.Style.CLEAVE;
            default -> {
                var entry = IvaliceBestiary.find(enemy.getSpeciesId());
                yield entry == null || entry.move().isEmpty() || (entry.kind().equals("Normal") && round % 3 == 0)
                        ? BattleVisual.Style.SLASH : BattleVisual.Style.valueOf(entry.move());
            }
        };
    }

    private static String enemyMove(IvaliceMonsterEntity enemy, BattleVisual.Style style) {
        return switch (enemy.getSpeciesId()) {
            case "white_mousse" -> "Waterga";
            case "ichthon" -> "Aqua";
            case "belias" -> "Hellfire";
            case "alpha_saurian" -> "Magma Breath";
            case "firemane" -> "Bushfire";
            case "bomb" -> "Fira";
            case "mateus" -> "Frostwave";
            case "yeti" -> "Blizzara";
            case "coeurl" -> "Blaster";
            case "behemoth" -> "Thundaga";
            case "malboro" -> "Bad Breath";
            case "skeleton_crusader" -> "Dark";
            case "zodiark" -> "Final Eclipse";
            case "yiazmat" -> "Cyclone";
            case "judge_gabranth" -> "Guilt (Quickening)";
            case "gilgamesh" -> "Ultimate Illusion";
            case "mindflayer" -> "Mind Blast (FFIV/XV)";
            case "rubicante" -> "Inferno (FFIV)";
            case "iron_giant" -> "Grand Sword (FFXV)";
            case "golbez" -> "Twin Meteor (FFIV)";
            case "ifrit_pyreburner" -> "Hellfire Pyre (FFXV)";
            case "adamantoise" -> "Tectonic Roar (FFXV)";
            case "ultros" -> "Octopus Ink (FFVI)";
            case "gil_turtle" -> "Turtle Quake (FFV)";
            case "atb_weapon" -> "Flare Star (FFVI)";
            case "shinryu" -> "Tidal Wave (FFV)";
            case "omega_mk" -> "Wave Cannon (FFV)";
            case "kefka_god" -> switch (style) {
                case SCATHE -> "Forsaken";
                case COLLAPSE -> "Fallen One";
                case HOLY -> "Light of Judgment";
                case ARDOR -> "Flare Star";
                case THUNDAGA -> "Trine";
                default -> "Firaga";
            };
            case "shadowseer" -> style == BattleVisual.Style.HOLY ? "Shining Ray" : "Darkga";
            case "omega_mark_xii" -> style == BattleVisual.Style.THUNDAGA ? "Discharge" : "Wave Cannon";
            case "ruin_deathgaze" -> "Doom";
            case "ruin_magitek" -> "Magitek Laser";
            default -> switch (style) {
                case SLASH, CLEAVE -> enemy.isAlphaLeader() ? "Investida Alfa" : "Ataque";
                case ICE -> "Blizzard";
                case WIND -> "Aero";
                case WATER -> "Aqua";
                case WATERGA -> "Aquaga";
                default -> style.name().charAt(0) + style.name().substring(1).toLowerCase(Locale.ROOT);
            };
        };
    }

    private static BattleVisual.Style style(Action action) {
        return switch (action) {
            case ATTACK -> BattleVisual.Style.SLASH;
            case FIRE -> BattleVisual.Style.FIRE;
            case FIRA -> BattleVisual.Style.FIRA;
            case FIRAGA -> BattleVisual.Style.FIRAGA;
            case BLIZZARD -> BattleVisual.Style.ICE;
            case BLIZZARA -> BattleVisual.Style.BLIZZARA;
            case BLIZZAGA -> BattleVisual.Style.BLIZZAGA;
            case THUNDER -> BattleVisual.Style.THUNDER;
            case THUNDARA -> BattleVisual.Style.THUNDARA;
            case THUNDAGA -> BattleVisual.Style.THUNDAGA;
            case AERO -> BattleVisual.Style.WIND;
            case AERORA -> BattleVisual.Style.AERORA;
            case AEROGA -> BattleVisual.Style.AEROGA;
            case WATER_MAGIC -> BattleVisual.Style.WATER;
            case AQUARA -> BattleVisual.Style.AQUARA;
            case WATERGA -> BattleVisual.Style.WATERGA;
            case DARK -> BattleVisual.Style.DARK;
            case DARKRA -> BattleVisual.Style.DARKRA;
            case DARKGA -> BattleVisual.Style.DARKGA;
            case FLARE -> BattleVisual.Style.FLARE;
            case ARDOR -> BattleVisual.Style.ARDOR;
            case COLLAPSE -> BattleVisual.Style.COLLAPSE;
            case SCATHE -> BattleVisual.Style.SCATHE;
            case HOLY -> BattleVisual.Style.HOLY;
            case CURE, CURA -> BattleVisual.Style.CURE;
            case CURAGA -> BattleVisual.Style.CURAGA;
            case HASTEGA -> BattleVisual.Style.HASTE;
            case POTION -> BattleVisual.Style.ITEM;
            default -> IvaliceActionEffects.style(action);
        };
    }

    private static String actionLabel(Action action) {
        return action.label;
    }

    private static void sendAnimation(Session session, String message) {
        CompoundTag state = new CompoundTag();
        state.putString("kind", "animation");
        state.putString("message", message);
        IvaliceStatePayload.send(session.player, state);
    }

    private static String act(Session session, Action action, IvaliceMonsterEntity target, boolean area, int cost) {
        ServerPlayer player = session.player;
        var profile = IvalicePlayerData.get(player);
        int level = profile.getBattleLevel();
        if (cost < 0) {
            profile.gil += cost;
            IvalicePlayerData.save(player);
        } else session.mp -= cost;
        if (IvaliceActionEffects.hasEffect(action)) {
            if (area && !action.targetsSelf()) {
                for (var enemy : List.copyOf(session.enemies)) IvaliceActionEffects.apply(player, enemy, action);
                return action.label + " em área";
            }
            return IvaliceActionEffects.apply(player, target, action);
        }
        switch (action) {
            case ATTACK -> {
                float base = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) + 3.0F + (level - 1) * 0.65F;
                if (IvaliceEquipment.ranged(player.getMainHandItem())) {
                    return IvaliceEquipment.fire(player, target, base, player.getMainHandItem()) ? "Disparo" : "Faltou munição.";
                }
                if (IvaliceEquipment.has(player, "tza_steel_gorget") && player.getHealth() < player.getMaxHealth() * .25F) base *= 2;
                if (IvaliceEquipment.has(player, "tza_blazer_gloves") && player.getHealth() >= player.getMaxHealth()) base *= 1.25F;
                if (IvaliceEquipment.has(player, "tza_amber_armlet") && player.getMainHandItem().isEmpty()) base += 6;
                strike(player, target, calculateFieldDamage(player, target, base), false);
                return "Ataque";
            }
            case CURE -> IvaliceActionEffects.heal(player, player, 8.0F + (level - 1) * 0.35F);
            case CURA -> {
                IvaliceActionEffects.heal(player, player, 18.0F + (level - 1) * 0.55F);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0));
            }
            case CURAGA -> {
                IvaliceActionEffects.heal(player, player, 36.0F + (level - 1) * 0.85F);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
            }
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
                float bonus = (profile.unlockedLicenses.contains("MAGICK_LORE_1") ? 1.25F : 1.0F)
                        + (profile.unlockedLicenses.contains("MAGICK_LORE_2") ? 0.25F : 0.0F)
                        + (profile.unlockedLicenses.contains("ARCANE_SUPREMACY") ? 0.35F : 0.0F)
                        + jobPower * 0.06F;
                if (area) {
                    float areaScale = action.isInnateArea() ? 1.0F : AREA_DAMAGE_SCALE;
                    for (IvaliceMonsterEntity enemy : List.copyOf(session.enemies)) {
                        castDamage(player, enemy, action, bonus * areaScale);
                    }
                    // Magias de Tier 3 (-ga) e Tier 4 também atingem monstros próximos numa área massiva do mundo!
                    if (action.tier >= 3) {
                        double splashRadius = action.tier == 4 ? 18.0 : 12.0;
                        for (IvaliceMonsterEntity nearby : player.level().getEntitiesOfClass(IvaliceMonsterEntity.class,
                                target.getBoundingBox().inflate(splashRadius),
                                mob -> mob.isAlive() && !session.enemies.contains(mob))) {
                            if (!isEngaged(nearby)) {
                                float dmg = (action.damage + (level - 1) * 0.45F) * bonus * 0.75F
                                        * getElementalMultiplier(nearby.getStats(), action.element);
                                if (dmg < 0) nearby.heal(-dmg);
                                if (dmg > 0) nearby.hurtWithCalculatedDamage(player.level(),
                                        player.level().damageSources().indirectMagic(player, player), dmg);
                            }
                        }
                    }
                    return spellName(action) + (action.isInnateArea() ? " [Cataclismo em Área]" : " em área");
                }
                castDamage(player, target, action, bonus);
            }
        }
        return action.label;
    }

    public static boolean canUse(ServerPlayer player, Action action) {
        if (action == Action.ATTACK) {
            return ZodiacJobs.permits(player, player.getMainHandItem());
        }
        if (action.school != null) {
            if (!ZodiacJobs.permits(player, action.school)) {
                return false;
            }
            var profile = IvalicePlayerData.get(player);
            if (profile.getBattleLevel() < action.minLevel) {
                return false;
            }
            if (profile.learnedSpells.contains(action.id())) {
                return true;
            }
            // Se o jogador tiver o pergaminho correspondente no inventário, aprende automaticamente!
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if ((stack.getItem() instanceof com.ivalice.item.IvaliceSpellItem spellItem
                        && spellItem.getAction().battleId().equalsIgnoreCase(action.id())
                        && profile.getBattleLevel() >= spellItem.getAction().minLevel)
                        || (stack.getItem() instanceof com.ivalice.item.IvaliceActionScroll scroll && scroll.action() == action)) {
                    profile.learnedSpells.add(action.id());
                    IvalicePlayerData.save(player);
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private static String spellName(Action action) {
        return action.label;
    }

    private static Action chooseGambit(Session session, IvaliceMonsterEntity target) {
        for (Action heal : List.of(Action.CURAGA, Action.CURA, Action.CURE)) {
            if (canUse(session.player, heal) && session.mp >= heal.mp && session.player.getHealth() < session.player.getMaxHealth() * 0.5F) {
                return heal;
            }
        }
        List<Action> offensive = List.of(
                Action.ARDOR, Action.COLLAPSE, Action.SCATHE, Action.FLARE, Action.HOLY,
                Action.FIRAGA, Action.BLIZZAGA, Action.THUNDAGA, Action.AEROGA, Action.WATERGA, Action.DARKGA,
                Action.FIRA, Action.BLIZZARA, Action.THUNDARA, Action.AERORA, Action.AQUARA, Action.DARKRA,
                Action.FIRE, Action.BLIZZARD, Action.THUNDER, Action.AERO, Action.WATER_MAGIC, Action.DARK
        );
        for (Action spell : offensive) {
            if (canUse(session.player, spell) && spell.mp <= session.mp && spell.element != null
                    && spell.element.equalsIgnoreCase(target.getStats().weakness())) {
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
        var entry = IvaliceBestiary.find(target.getSpeciesId());
        if (entry != null) {
            if (target.isStolenFrom() || entry.steal().isEmpty()) return "Nenhum espólio para roubar.";
            boolean cuffs = player.getOffhandItem().is(IvaliceItems.getItemById("tza_thief_s_cuffs"));
            String id = IvaliceBestiary.steal(entry, cuffs, player.getRandom());
            if (id == null) return "O roubo falhou.";
            String reward = giveLoot(player, id);
            if (reward == null) return "Nenhum espólio para roubar.";
            target.setStolenFrom(true);
            IvalicePlayerData.save(player);
            return "Roubou " + reward + ".";
        }
        Item loot = IvaliceItems.getItemById(target.getStats().stealItemId());
        if (target.isStolenFrom() || loot == null) {
            return "Nenhum espólio para roubar.";
        }
        target.setStolenFrom(true);
        player.getInventory().placeItemBackInInventory(new ItemStack(loot), Prediction.SERVER_ONLY);
        return "Roubou " + target.getStats().stealItemId() + ".";
    }

    static String giveLoot(ServerPlayer player, String id) {
        if (id.startsWith("gil:")) {
            int amount = Integer.parseInt(id.substring(4));
            IvalicePlayerData.get(player).gil += amount;
            return amount + " Gil";
        }
        Item loot = IvaliceItems.getItemById(id);
        if (loot == null) return null;
        ItemStack stack = new ItemStack(loot);
        String name = stack.getHoverName().getString();
        player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
        return name;
    }

    private static void castDamage(ServerPlayer player, IvaliceMonsterEntity target, Action spell, float bonus) {
        if (spell.element != null) {
            float base = spell.damage + (IvalicePlayerData.get(player).getBattleLevel() - 1) * 0.45F;
            if (player.hasEffect(IvaliceStatuses.FAITH)) bonus *= 1.25F;
            if (IvaliceEquipment.has(player, "tza_leather_gorget") && player.getHealth() < player.getMaxHealth() * .25F) bonus *= 2;
            if (IvaliceEquipment.has(player, "tza_magick_gloves") && player.getHealth() >= player.getMaxHealth()) bonus *= 1.25F;
            if ("Fire".equals(spell.element) && target.hasEffect(IvaliceStatuses.OIL)) bonus *= 2;
            float damage = base * bonus * getElementalMultiplier(target.getStats(), spell.element);
            if (damage > 0) strike(player, target, damage, true);
            if (damage < 0) target.heal(-damage);
        }
    }

    static void strike(ServerPlayer player, IvaliceMonsterEntity target, float amount, boolean magic) {
        Session session = BATTLES.get(player.getUUID());
        if (target.isAlive() && target.level() == player.level()
                && (session == null || ENEMIES.get(target.getUUID()) == session)) {
            if (amount <= 0) return;
            if (magic && target.hasEffect(IvaliceStatuses.REFLECT) && !IvaliceEquipment.has(player, "tza_opal_ring")) {
                player.hurtServer(player.level(), player.level().damageSources().indirectMagic(target, target), amount);
                return;
            }
            if (target.hasEffect(IvaliceStatuses.REVERSE)) {
                target.heal(amount);
                return;
            }
            if (magic && target.hasEffect(IvaliceStatuses.SHELL)) amount *= .65F;
            if (!magic && target.hasEffect(IvaliceStatuses.DISEASE)) amount *= 1.20F;
            target.removeEffect(IvaliceStatuses.SLEEP);
            if (session != null) target.damageCooldownTime = 0;
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
        if (spell != null && spell.mp > 0 && player.hasEffect(IvaliceStatuses.SILENCE)) return;
        if (spell == Action.LIBRA) {
            player.sendSystemMessage(Component.literal("Libra • Fraqueza: " + target.getStats().weakness()));
        } else if (spell == Action.STEAL) {
            player.sendSystemMessage(Component.literal(steal(player, target)));
        } else if (spell != null) {
            BattleVisual.begin(player.level(), player, style(spell));
            BattleVisual.impact(player.level(), target, style(spell));
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

    public static int restoreMp(ServerPlayer player, int amount) {
        Session session = BATTLES.get(player.getUUID());
        if (session == null) return 0;
        int restored = Math.min(amount, session.maxMp - session.mp);
        session.mp += restored;
        return restored;
    }

    static void exhaustMp(ServerPlayer player) {
        drainMp(player);
    }

    static int drainMp(ServerPlayer player) {
        Session session = BATTLES.get(player.getUUID());
        if (session == null) return 0;
        int mp = session.mp;
        session.mp = 0;
        return mp;
    }

    public static boolean castFieldAction(ServerPlayer player, Action action) {
        if (isInBattle(player) || IvaliceStatuses.unableToAct(player)
                || (action.mp > 0 && player.hasEffect(IvaliceStatuses.SILENCE))) return false;
        var target = aimedEnemy(player);
        if (!action.targetsSelf() && target == null) return false;
        ServerLevel level = player.level();
        BattleVisual.Style style = style(action);
        BattleVisual.begin(level, player, style);
        BattleVisual.impact(level, action.targetsSelf() ? player : target, style);
        player.sendOverlayMessage(Component.literal(IvaliceActionEffects.apply(player, target, action)));
        return true;
    }

    public static IvaliceMonsterEntity aimedEnemy(ServerPlayer player) {
        var eye = player.getEyePosition();
        var direction = player.getLookAngle();
        return player.level().getEntitiesOfClass(IvaliceMonsterEntity.class, player.getBoundingBox().inflate(12),
                        mob -> mob.isAlive() && !mob.isSummoned() && !isEngaged(mob) && player.hasLineOfSight(mob)).stream()
                .filter(mob -> {
                    var offset = mob.position().add(0, mob.getBbHeight() * .6, 0).subtract(eye);
                    return offset.lengthSqr() <= 144 && offset.normalize().dot(direction) > .90;
                }).min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
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
        state.putBoolean("black", ZodiacJobs.permits(session.player, IvaliceItemCategory.MAGICK_BLACK));
        state.putBoolean("white", ZodiacJobs.permits(session.player, IvaliceItemCategory.MAGICK_WHITE));
        state.putBoolean("time", ZodiacJobs.permits(session.player, IvaliceItemCategory.MAGICK_TIME_ARCANE));
        ListTag availableSpells = new ListTag();
        for (Action act : Action.values()) {
            if ((act.school != null || act == Action.LIBRA || act == Action.STEAL) && canUse(session.player, act)) {
                CompoundTag sp = new CompoundTag();
                sp.putString("id", act.id());
                sp.putString("label", act.label);
                sp.putInt("mp", act.mp);
                sp.putInt("tier", act.tier);
                sp.putBoolean("self", act.targetsSelf());
                sp.putBoolean("areaOnly", act.isInnateArea());
                sp.putBoolean("area", act.offersArea());
                sp.putBoolean("technique", act.school == IvaliceItemCategory.TECHNICK || act == Action.LIBRA || act == Action.STEAL);
                availableSpells.add(sp);
            }
        }
        state.put("availableSpells", availableSpells);
        ListTag availableItems = new ListTag();
        var seen = new HashSet<Item>();
        var inventory = session.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof IvaliceUseItem consumable) || !seen.add(consumable)) continue;
            var item = new CompoundTag();
            item.putString("id", BuiltInRegistries.ITEM.getKey(consumable).getPath());
            item.putString("label", stack.getHoverName().getString());
            item.putInt("count", inventory.countItem(consumable));
            item.putBoolean("self", !consumable.effect().needsEnemy(session.player) || consumable.effect().area());
            availableItems.add(item);
        }
        state.put("availableItems", availableItems);
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
        var entry = IvaliceBestiary.find(stats.id());
        if (entry != null && !entry.original()) return IvaliceBestiary.multiplier(stats.id(), element, 1.0F);
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

        int prevLevel = profile.getBattleLevel();
        int rewardGil = stats.gilReward();
        int rewardLp = stats.lpReward();
        if (IvaliceEquipment.has(player, "golden_amulet")) rewardLp *= 2;
        if (IvaliceEquipment.has(player, "tza_cat_ear_hood")) {
            rewardGil += rewardLp * 10;
            rewardLp = 0;
        }
        if (IvaliceEquipment.has(player, "tza_firefly")) profile.adjustedExperience -= stats.xpReward();
        if (IvaliceEquipment.has(player, "tza_embroidered_tippet")) profile.adjustedExperience += stats.xpReward();
        profile.recordKill(stats.id(), rewardGil, rewardLp);
        int newLevel = profile.getBattleLevel();
        int kills = profile.getKills(stats.id());

        if (newLevel > prevLevel) {
            IvalicePlayerData.applyLicenseAttributes(player);
            player.heal(player.getMaxHealth());
            player.sendSystemMessage(Component.literal(String.format(
                    "✦ [LEVEL UP! Nível %d -> Nível %d / 99] Seus atributos de Vida, Ataque, Defesa e MP aumentaram!",
                    prevLevel, newLevel
            )).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }

        player.sendSystemMessage(Component.literal(String.format(
                "★ VITÓRIA! %s derrotado! Recompensas: +%d Gil | +%d LP | Bestiário: %d abate(s)",
                stats.displayName(), rewardGil, rewardLp, kills
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
