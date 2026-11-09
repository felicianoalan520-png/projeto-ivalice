package com.ivalice.combat;

import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.entity.IvaliceMonsterStats;
import com.ivalice.item.IvaliceCatalogItem;
import com.ivalice.registry.IvaliceItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador central do Sistema de Combate por Turnos de Final Fantasy XII (Etapa 3.3).
 * Suporta:
 * - Ordem de turnos baseada em Velocidade (Speed) e Haste
 * - Comandos: Attack, Magicks & Technicks (Fire, Blizzard, Thunder, Cure, Cura, Hastega, Libra, Steal),
 *   Items (Potion, Hi-Potion, X-Potion, Elixir, Remedy) e Flee
 * - Fraquezas e Resistências Elementais (2.0x dano na fraqueza, 0.5x na resistência)
 * - Gambits automáticos (Etapa 5)
 * - Integração direta com o Bestiário (Clan Primer), Caçadas (Mark Hunts), Gil e LP (Etapa 4)
 */
public class BattleManager {
    private static final Map<UUID, BattleSession> ACTIVE_BATTLES = new ConcurrentHashMap<>();

    // Estado sincronizado no cliente para renderização da BattleScreen
    private static IvaliceMonsterEntity clientTargetMonster = null;
    private static boolean clientLibraActive = false;
    private static final List<String> clientBattleLog = new ArrayList<>();

    public static class BattleSession {
        public final UUID playerId;
        public final IvaliceMonsterEntity monster;
        public int turnNumber = 1;
        public boolean libraRevealed = false;
        public int playerMp = 100;
        public int maxPlayerMp = 100;
        public final List<String> log = new ArrayList<>();

        public BattleSession(ServerPlayer player, IvaliceMonsterEntity monster) {
            this.playerId = player.getUUID();
            this.monster = monster;
            IvalicePlayerData.PlayerProfile profile = IvalicePlayerData.get(player);
            if (profile.unlockedLicenses.contains("MAGICK_LORE_1")) {
                this.maxPlayerMp = 150;
                this.playerMp = 150;
            }
            // Se o jogador já abateu 3+ dessa espécie no Bestiário, Libra já inicia ativo!
            if (profile.getKills(monster.getSpeciesId()) >= 3) {
                this.libraRevealed = true;
            }
            addLog("Engajado contra " + monster.getStats().displayName() + " [Lv." + monster.getStats().level() + "]!");
        }

        public void addLog(String entry) {
            log.add(entry);
            while (log.size() > 8) {
                log.removeFirst();
            }
        }
    }

    public static BattleSession startOrResumeBattle(ServerPlayer player, IvaliceMonsterEntity monster) {
        BattleSession session = ACTIVE_BATTLES.get(player.getUUID());
        if (session == null || session.monster != monster || !monster.isAlive()) {
            session = new BattleSession(player, monster);
            ACTIVE_BATTLES.put(player.getUUID(), session);
        }
        IvaliceMonsterStats stats = monster.getStats();
        player.sendSystemMessage(Component.literal("═══ [COMBATE IVALICE — ATB PRONTO] ═══").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal(String.format("Alvo: %s (Lv.%d | HP: %d/%d | Família: %s)",
                stats.displayName(), stats.level(), (int) Math.ceil(monster.getHealth()), (int) Math.ceil(monster.getMaxHealth()), stats.family()))
                .withStyle(ChatFormatting.YELLOW));
        if (session.libraRevealed) {
            player.sendSystemMessage(Component.literal(String.format("[Libra] Fraqueza: %s (2x Dano) | Resistência: %s | Espólio: %s",
                    stats.weakness(), stats.resistance(), stats.primaryLootId())).withStyle(ChatFormatting.AQUA));
        }
        return session;
    }

    /**
     * Executa uma ação de turno escolhida pelo jogador na BattleScreen ou via comando `/ivalice battle <action>`.
     */
    public static void executeTurnAction(ServerPlayer player, IvaliceMonsterEntity monster, String actionId) {
        if (monster == null || !monster.isAlive()) {
            player.sendSystemMessage(Component.literal("[Ivalice] O alvo já foi derrotado!").withStyle(ChatFormatting.GRAY));
            return;
        }
        BattleSession session = startOrResumeBattle(player, monster);
        IvaliceMonsterStats stats = monster.getStats();
        IvalicePlayerData.PlayerProfile profile = IvalicePlayerData.get(player);
        ServerLevel level = (ServerLevel) player.level();

        double baseAtk = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        if (baseAtk < 4.0) baseAtk = 6.0;
        double magBonus = profile.unlockedLicenses.contains("MAGICK_LORE_1") ? 1.25 : 1.0;

        switch (actionId.toLowerCase()) {
            case "attack" -> {
                float dmg = calculateFieldDamage(player, monster, (float) (baseAtk + 4.0));
                session.addLog(String.format("Turno %d: Você atacou %s causando %.1f de dano!", session.turnNumber, stats.displayName(), dmg));
                addClientLog(String.format("Turno %d: Ataque físico causou %.1f de dano!", session.turnNumber, dmg));
                monster.hurtWithCalculatedDamage(level, level.damageSources().playerAttack(player), dmg);
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            case "fire" -> {
                float mult = getElementalMultiplier(stats, "Fire");
                float dmg = (float) (12.0 * magBonus * mult);
                String note = mult > 1.0F ? " [FRAQUEZA ELEMENTAL 2x!]" : (mult < 1.0F ? " [Resistiu!]" : "");
                session.addLog(String.format("Black Magick [Fire]: %.1f de dano de Fogo!%s", dmg, note));
                addClientLog(String.format("Black Magick [Fire]: %.1f de dano!%s", dmg, note));
                monster.setRemainingFireTicks(80);
                monster.hurtWithCalculatedDamage(level, level.damageSources().indirectMagic(player, player), dmg);
                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.9F, 1.1F);
            }
            case "blizzard" -> {
                float mult = getElementalMultiplier(stats, "Ice");
                float dmg = (float) (12.0 * magBonus * mult);
                String note = mult > 1.0F ? " [FRAQUEZA ELEMENTAL 2x!]" : (mult < 1.0F ? " [Resistiu!]" : "");
                session.addLog(String.format("Black Magick [Blizzard]: %.1f de dano de Gelo!%s", dmg, note));
                addClientLog(String.format("Black Magick [Blizzard]: %.1f de dano!%s", dmg, note));
                monster.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                monster.hurtWithCalculatedDamage(level, level.damageSources().indirectMagic(player, player), dmg);
                level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.9F, 1.3F);
            }
            case "thunder" -> {
                float mult = getElementalMultiplier(stats, "Thunder");
                float dmg = (float) (13.0 * magBonus * mult);
                String note = mult > 1.0F ? " [FRAQUEZA ELEMENTAL 2x!]" : (mult < 1.0F ? " [Resistiu!]" : "");
                session.addLog(String.format("Black Magick [Thunder]: %.1f de dano de Trovão!%s", dmg, note));
                addClientLog(String.format("Black Magick [Thunder]: %.1f de dano!%s", dmg, note));
                monster.hurtWithCalculatedDamage(level, level.damageSources().indirectMagic(player, player), dmg);
                level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.7F, 1.4F);
            }
            case "water_magic" -> {
                float mult = getElementalMultiplier(stats, "Water");
                float dmg = (float) (12.0 * magBonus * mult);
                String note = mult > 1.0F ? " [FRAQUEZA ELEMENTAL 2x!]" : (mult < 1.0F ? " [Resistiu!]" : "");
                session.addLog(String.format("Green Magick [Aqua]: %.1f de dano de Água!%s", dmg, note));
                addClientLog(String.format("Green Magick [Aqua]: %.1f de dano!%s", dmg, note));
                monster.hurtWithCalculatedDamage(level, level.damageSources().indirectMagic(player, player), dmg);
            }
            case "aero" -> {
                float mult = getElementalMultiplier(stats, "Wind");
                float dmg = (float) (12.0 * magBonus * mult);
                String note = mult > 1.0F ? " [FRAQUEZA ELEMENTAL 2x!]" : (mult < 1.0F ? " [Resistiu!]" : "");
                session.addLog(String.format("Time Magick [Aero]: %.1f de dano de Vento!%s", dmg, note));
                addClientLog(String.format("Time Magick [Aero]: %.1f de dano!%s", dmg, note));
                monster.hurtWithCalculatedDamage(level, level.damageSources().indirectMagic(player, player), dmg);
            }
            case "cure" -> {
                float heal = (float) (10.0 * magBonus);
                player.heal(heal);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                session.addLog(String.format("White Magick [Cura]: Recuperou +%.0f HP!", heal));
                addClientLog(String.format("White Magick [Cura]: +%.0f HP recuperado!", heal));
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9F, 1.2F);
            }
            case "libra" -> {
                session.libraRevealed = true;
                clientLibraActive = true;
                session.addLog(String.format("Technick [Libra]: Fraqueza a %s revelada! Resistência: %s.", stats.weakness(), stats.resistance()));
                addClientLog(String.format("Libra: Fraqueza=%s | Resist=%s | Roubo=%s", stats.weakness(), stats.resistance(), stats.stealItemId()));
                player.sendSystemMessage(Component.literal(String.format("[Libra] %s -> Fraqueza: %s | Resistência: %s | Roubo: %s",
                        stats.displayName(), stats.weakness(), stats.resistance(), stats.stealItemId())).withStyle(ChatFormatting.AQUA));
            }
            case "steal" -> {
                if (monster.isStolenFrom()) {
                    session.addLog("Technick [Steal]: O inimigo não possui mais itens para roubar!");
                    addClientLog("Steal: Inimigo já teve seu espólio roubado!");
                } else {
                    monster.setStolenFrom(true);
                    Item stolen = IvaliceItems.getItemById(stats.stealItemId());
                    if (stolen != null) {
                        player.getInventory().add(new ItemStack(stolen, 1));
                    }
                    session.addLog("Technick [Steal]: Roubou 1x " + stats.stealItemId() + " com sucesso!");
                    addClientLog("Steal: Roubou 1x " + stats.stealItemId() + "!");
                    player.sendSystemMessage(Component.literal("[Technick: Steal] Você roubou 1x " + stats.stealItemId() + " de " + stats.displayName() + "!")
                            .withStyle(ChatFormatting.GREEN));
                }
            }
            case "potion" -> {
                player.heal(12.0F);
                session.addLog("Item [Hi-Potion]: Recuperou +12 HP instantaneamente!");
                addClientLog("Item [Hi-Potion]: +12 HP recuperado!");
            }
            case "gambit" -> {
                // Executa automaticamente o Gambit mais prioritário do jogador!
                executeGambitAutoTurn(player, monster, session);
                return;
            }
            case "flee" -> {
                ACTIVE_BATTLES.remove(player.getUUID());
                player.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 2));
                addClientLog("Fleeing... Você recuou da batalha!");
                player.sendSystemMessage(Component.literal("[Ivalice] Você escapou da batalha (Fleeing...)!").withStyle(ChatFormatting.GRAY));
                return;
            }
        }

        // Contra-ataque de turno da criatura se ela continuar viva
        if (monster.isAlive()) {
            session.turnNumber++;
            executeMonsterCounterTurn(player, monster, session);
            // Verifica Gambit automático de emergência (Self: HP < 50% -> Cure/Potion)
            checkEmergencyGambit(player, session);
        }
    }

    private static void executeGambitAutoTurn(ServerPlayer player, IvaliceMonsterEntity monster, BattleSession session) {
        IvaliceMonsterStats stats = monster.getStats();
        if (player.getHealth() < player.getMaxHealth() * 0.55F) {
            addClientLog("[Gambit: Self HP < 50%] -> Cast Cura!");
            executeTurnAction(player, monster, "cure");
            return;
        }
        String weakness = stats.weakness().toLowerCase();
        if (weakness.contains("fire")) {
            addClientLog("[Gambit: Foe Fire-Weak] -> Cast Fire!");
            executeTurnAction(player, monster, "fire");
        } else if (weakness.contains("ice")) {
            addClientLog("[Gambit: Foe Ice-Weak] -> Cast Blizzard!");
            executeTurnAction(player, monster, "blizzard");
        } else if (weakness.contains("thunder")) {
            addClientLog("[Gambit: Foe Thunder-Weak] -> Cast Thunder!");
            executeTurnAction(player, monster, "thunder");
        } else if (weakness.contains("wind")) {
            addClientLog("[Gambit: Foe Wind-Weak] -> Cast Aero!");
            executeTurnAction(player, monster, "aero");
        } else if (weakness.contains("water")) {
            addClientLog("[Gambit: Foe Water-Weak] -> Cast Aqua!");
            executeTurnAction(player, monster, "water_magic");
        } else {
            addClientLog("[Gambit: Foe Nearest] -> Attack!");
            executeTurnAction(player, monster, "attack");
        }
    }

    private static void executeMonsterCounterTurn(ServerPlayer player, IvaliceMonsterEntity monster, BattleSession session) {
        IvaliceMonsterStats stats = monster.getStats();
        ServerLevel level = (ServerLevel) player.level();
        float rawDmg = (float) Math.max(1.5, stats.attackDamage() * 0.45);
        String skillName = switch (stats.id()) {
            case "flowering_cactoid" -> "1,000 Needles (Espinhos Milenares)";
            case "rogue_tomato" -> "Headbutt Furioso";
            case "thextera" -> "Presas do Alfa";
            case "white_mousse" -> "Waterga Glacial";
            case "belias" -> "Hellfire (Chamas do Inferno)";
            case "mateus" -> "Frostwave (Onda Glacial)";
            default -> "Investida";
        };
        player.hurtServer(level, level.damageSources().mobAttack(monster), rawDmg);
        String msg = String.format("%s usou [%s] causando %.1f de dano!", stats.displayName(), skillName, rawDmg);
        session.addLog(msg);
        addClientLog(msg);
    }

    private static void checkEmergencyGambit(ServerPlayer player, BattleSession session) {
        IvalicePlayerData.PlayerProfile profile = IvalicePlayerData.get(player);
        if (profile.activeGambits.contains("SELF_HP_50_POTION") && player.isAlive() && player.getHealth() < player.getMaxHealth() * 0.45F) {
            player.heal(6.0F);
            String msg = "[Gambit Auto: Self HP < 50%] -> Auto-Potion (+6 HP)!";
            session.addLog(msg);
            addClientLog(msg);
            player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.GREEN));
        }
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

    public static void onPlayerFieldStrike(ServerPlayer player, IvaliceMonsterEntity monster, float dealtDamage) {
        checkEmergencyGambit(player, startOrResumeBattle(player, monster));
        IvaliceMonsterStats stats = monster.getStats();
        player.sendOverlayMessage(Component.literal(String.format(
                "⚔ %s [Lv.%d] — HP: %d/%d (-%.1f) | Fraqueza: %s | Clique Direito p/ Menu ATB",
                stats.displayName(), stats.level(), (int) Math.ceil(monster.getHealth()), (int) Math.ceil(monster.getMaxHealth()),
                dealtDamage, stats.weakness()
        )).withStyle(ChatFormatting.GOLD));
    }

    public static void onMonsterDefeated(ServerPlayer player, IvaliceMonsterEntity monster) {
        ACTIVE_BATTLES.remove(player.getUUID());
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

    // Métodos auxiliares para a GUI de Batalha no Cliente
    public static void openClientBattleScreen(IvaliceMonsterEntity monster) {
        clientTargetMonster = monster;
        clientBattleLog.clear();
        clientBattleLog.add("ATB Pronto contra " + monster.getStats().displayName() + "!");
        clientBattleLog.add("Escolha um comando na janela de batalha de Ivalice.");
        com.ivalice.client.ClientScreenHelper.openBattleScreen(monster);
    }

    public static IvaliceMonsterEntity getClientTargetMonster() {
        return clientTargetMonster;
    }

    public static boolean isClientLibraActive() {
        return clientLibraActive;
    }

    public static void setClientLibraActive(boolean active) {
        clientLibraActive = active;
    }

    public static List<String> getClientBattleLog() {
        return clientBattleLog;
    }

    public static void addClientLog(String line) {
        clientBattleLog.add(line);
        while (clientBattleLog.size() > 7) {
            clientBattleLog.removeFirst();
        }
    }
}
