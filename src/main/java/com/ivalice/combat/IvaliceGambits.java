package com.ivalice.combat;

import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceBestiary;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.registry.IvaliceItems;
import com.ivalice.registry.IvaliceTzaCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class IvaliceGambits {
    private static final Pattern THRESHOLD = Pattern.compile("(hp|mp|count|item amt)\\s*(>=|<|=)\\s*([\\d,]+)(%)?");

    record Choice(BattleManager.Action action, IvaliceMonsterEntity target) {}

    private IvaliceGambits() {}

    public static int configure(ServerPlayer player, String id, String actionId) {
        var profile = IvalicePlayerData.get(player);
        var entry = IvaliceTzaCatalog.getEntry(id);
        var action = BattleManager.Action.fromId(actionId);
        if (entry == null || !entry.kind().equals("gambit") || !profile.learnedGambits.contains(id)) {
            player.sendSystemMessage(Component.literal("Aprenda a condição usando o pergaminho do Gambit."));
            return 0;
        }
        if (action == null || action == BattleManager.Action.GAMBIT || action == BattleManager.Action.FLEE
                || action == BattleManager.Action.ITEM_USE || !BattleManager.canUse(player, action)) {
            player.sendSystemMessage(Component.literal("Escolha uma ação aprendida, como attack, cure, potion ou fire."));
            return 0;
        }
        profile.gambitRules.removeIf(rule -> rule.startsWith(id + ":"));
        if (profile.gambitRules.size() >= 12) {
            player.sendSystemMessage(Component.literal("O quadro comporta doze Gambits. Use /ivalice gambit clear."));
            return 0;
        }
        profile.gambitRules.add(id + ":" + action.id());
        IvalicePlayerData.save(player);
        player.sendSystemMessage(Component.literal("Gambit " + profile.gambitRules.size() + " • " + entry.name() + " → " + action.label));
        return 1;
    }

    public static int list(ServerPlayer player, boolean clear) {
        var profile = IvalicePlayerData.get(player);
        if (clear) {
            profile.gambitRules.clear();
            IvalicePlayerData.save(player);
        }
        if (profile.gambitRules.isEmpty()) player.sendSystemMessage(Component.literal("Quadro vazio. Use um pergaminho de Gambit."));
        for (String rule : profile.gambitRules) {
            String[] parts = rule.split(":", 2);
            var entry = IvaliceTzaCatalog.getEntry(parts[0]);
            if (entry != null) player.sendSystemMessage(Component.literal(entry.name() + " → " + parts[1]));
        }
        return 1;
    }

    static Choice choose(ServerPlayer player, List<IvaliceMonsterEntity> enemies, IvaliceMonsterEntity current, int mp, int maxMp) {
        for (String rule : IvalicePlayerData.get(player).gambitRules) {
            String[] parts = rule.split(":", 2);
            var entry = IvaliceTzaCatalog.getEntry(parts[0]);
            var action = parts.length == 2 ? BattleManager.Action.fromId(parts[1]) : null;
            if (entry == null || action == null || !BattleManager.canUse(player, action) || action.mp > mp) continue;
            if (action == BattleManager.Action.POTION && player.getInventory().countItem(IvaliceItems.HI_POTION.get()) == 0) continue;
            IvaliceMonsterEntity selected = target(player, enemies, current,
                    entry.name().toLowerCase(Locale.ROOT).replaceFirst("^gambit ", ""), mp, maxMp);
            if (selected != null) return new Choice(action, selected);
        }
        return null;
    }

    private static IvaliceMonsterEntity target(ServerPlayer player, List<IvaliceMonsterEntity> enemies,
                                               IvaliceMonsterEntity current, String condition, int mp, int maxMp) {
        if (condition.startsWith("self") || condition.startsWith("ally")) {
            String detail = condition.contains(": ") ? condition.split(": ", 2)[1] : "any";
            if (detail.matches("vaan|ashe|fran|balthier|basch|penelo|guest")) {
                return player.getScoreboardName().equalsIgnoreCase(detail) ? current : null;
            }
            return matches(player, player, enemies, detail, mp, maxMp) ? current : null;
        }
        String detail = condition.replaceFirst("^foe: ", "");
        var order = sort(detail, player);
        if (order != null) return enemies.stream().filter(LivingEntity::isAlive).min(order).orElse(null);
        if (detail.equals("party leader's target") || detail.equals("concentrated attack")) return current;
        return enemies.stream().filter(enemy -> enemy.isAlive() && matches(player, enemy, enemies, detail, mp, maxMp))
                .findFirst().orElse(null);
    }

    private static Comparator<IvaliceMonsterEntity> sort(String detail, ServerPlayer player) {
        if (detail.equals("nearest") || detail.equals("nearest visible") || detail.equals("furthest")) {
            Comparator<IvaliceMonsterEntity> distance = Comparator.comparingDouble(player::distanceToSqr);
            return detail.equals("furthest") ? distance.reversed() : distance;
        }
        if (!detail.startsWith("highest ") && !detail.startsWith("lowest ")) return null;
        String field = detail.substring(detail.indexOf(' ') + 1);
        Comparator<IvaliceMonsterEntity> value = Comparator.comparingDouble(enemy -> switch (field) {
            case "hp" -> enemy.getHealth();
            case "max hp" -> enemy.getMaxHealth();
            case "strength" -> enemy.getAttributeValue(Attributes.ATTACK_DAMAGE);
            case "speed" -> enemy.getAttributeValue(Attributes.MOVEMENT_SPEED);
            case "defence" -> enemy.getArmorValue();
            case "magick resist" -> enemy.hasEffect(IvaliceStatuses.SHELL) ? 1 : 0;
            case "mp", "max mp", "magick power" -> enemy.getStats().attackDamage();
            default -> enemy.getStats().level();
        });
        return detail.startsWith("highest ") ? value.reversed() : value;
    }

    private static boolean matches(ServerPlayer player, LivingEntity entity, List<IvaliceMonsterEntity> enemies,
                                   String detail, int mp, int maxMp) {
        if (detail.startsWith("status = ")) return status(entity, detail.substring(9));
        if (detail.startsWith("character status = ")) return status(player, detail.substring(19));
        var threshold = THRESHOLD.matcher(detail);
        if (threshold.find()) {
            boolean playerStat = entity == player || detail.startsWith("character");
            LivingEntity subject = playerStat ? player : entity;
            String stat = threshold.group(1);
            int boundary = Integer.parseInt(threshold.group(3).replace(",", ""));
            boolean percent = threshold.group(4) != null;
            double value = switch (stat) {
                case "hp" -> percent ? subject.getHealth() / subject.getMaxHealth() * 100 : subject.getHealth();
                case "mp" -> playerStat ? (percent ? 100.0 * mp / Math.max(1, maxMp) : mp) : 0;
                case "count" -> detail.startsWith("ally count") ? player.level().getEntitiesOfClass(ServerPlayer.class,
                        player.getBoundingBox().inflate(10), ally -> ally == player || player.isAlliedTo(ally)).size() : enemies.size();
                default -> player.getInventory().countItem(IvaliceItems.HI_POTION.get());
            };
            return switch (threshold.group(2)) {
                case ">=" -> value >= boundary;
                case "<" -> value < boundary;
                default -> Math.abs(value - boundary) < .01;
            };
        }
        if (entity == player) {
            return !detail.contains("target") || enemies.stream().anyMatch(enemy -> enemy.getTarget() == player);
        }
        IvaliceMonsterEntity enemy = (IvaliceMonsterEntity) entity;
        if (detail.endsWith("-weak") || detail.endsWith("-vulnerable")) {
            String element = detail.split("-", 2)[0].replace("lightning", "thunder");
            float affinity = BattleManager.getElementalMultiplier(enemy.getStats(), element);
            return detail.endsWith("-weak") ? affinity > 1 : affinity > 0;
        }
        if (detail.equals("undead")) return enemy.getStats().family().toLowerCase(Locale.ROOT).contains("undead");
        if (detail.equals("flying")) {
            var entry = IvaliceBestiary.find(enemy.getSpeciesId());
            return entry != null && List.of("bat", "bird", "wyvern", "elemental", "ghost").contains(entry.model());
        }
        if (detail.startsWith("targeting ")) {
            String whom = detail.substring(10);
            return switch (whom) {
                case "self", "leader" -> enemy.getTarget() == null || enemy.getTarget() == player;
                case "ally" -> enemy.getTarget() instanceof ServerPlayer ally && ally != player && player.isAlliedTo(ally);
                default -> enemy.getTarget() instanceof ServerPlayer named && named.getScoreboardName().equalsIgnoreCase(whom);
            };
        }
        if (detail.equals("distributed attack")) return enemy.getHealth() >= enemy.getMaxHealth() * .75;
        if (detail.equals("daring attack")) return enemy.getHealth() < enemy.getMaxHealth() * .5;
        return detail.equals("any");
    }

    private static boolean status(LivingEntity entity, String status) {
        return switch (status) {
            case "ko" -> !entity.isAlive();
            case "stone", "petrify" -> entity.hasEffect(IvaliceStatuses.PETRIFY);
            case "stop" -> entity.hasEffect(IvaliceStatuses.STOP);
            case "sleep" -> entity.hasEffect(IvaliceStatuses.SLEEP);
            case "confuse" -> entity.hasEffect(IvaliceStatuses.CONFUSE);
            case "doom" -> entity.hasEffect(IvaliceStatuses.DOOM);
            case "blind" -> entity.hasEffect(MobEffects.BLINDNESS);
            case "poison" -> entity.hasEffect(MobEffects.POISON);
            case "silence" -> entity.hasEffect(IvaliceStatuses.SILENCE);
            case "sap" -> entity.hasEffect(MobEffects.WITHER);
            case "oil" -> entity.hasEffect(IvaliceStatuses.OIL);
            case "reverse" -> entity.hasEffect(IvaliceStatuses.REVERSE);
            case "disable" -> entity.hasEffect(IvaliceStatuses.DISABLE);
            case "immobilize" -> entity.hasEffect(IvaliceStatuses.IMMOBILIZE);
            case "slow" -> entity.hasEffect(MobEffects.SLOWNESS);
            case "disease" -> entity.hasEffect(IvaliceStatuses.DISEASE);
            case "lure" -> entity.hasEffect(IvaliceStatuses.DECOY);
            case "protect" -> entity.hasEffect(MobEffects.RESISTANCE);
            case "shell" -> entity.hasEffect(IvaliceStatuses.SHELL);
            case "haste" -> entity.hasEffect(MobEffects.SPEED);
            case "bravery", "berserk" -> entity.hasEffect(MobEffects.STRENGTH);
            case "faith" -> entity.hasEffect(IvaliceStatuses.FAITH);
            case "reflect" -> entity.hasEffect(IvaliceStatuses.REFLECT);
            case "invisible" -> entity.hasEffect(MobEffects.INVISIBILITY);
            case "regen" -> entity.hasEffect(MobEffects.REGENERATION);
            case "float" -> entity.hasEffect(MobEffects.SLOW_FALLING);
            case "bubble" -> entity.hasEffect(MobEffects.HEALTH_BOOST);
            case "hp critical" -> entity.getHealth() <= entity.getMaxHealth() * .25F;
            default -> false;
        };
    }
}
