package com.ivalice.command;

import com.ivalice.IvaliceMod;
import com.ivalice.combat.BattleManager;
import com.ivalice.data.IvalicePlayerData;
import com.ivalice.job.ZodiacJobs;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.entity.IvaliceMonsterStats;
import com.ivalice.registry.IvaliceEntities;
import com.ivalice.registry.IvaliceItems;
import com.ivalice.registry.IvaliceTzaCatalog;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;
import java.util.Set;

/**
 * Comandos oficiais `/ivalice` para navegação instantânea entre biomas, Combate por Turnos,
 * Bestiário, Caçadas do Clã Centurio, Tabuleiro de Licenças, Gambits e Bazaar de Rabanastre.
 */
public class IvaliceCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ivalice")
                // 1. /ivalice tp <biome> — Localiza o bioma em qualquer seed e teleporta o jogador para a superfície segura!
                .then(Commands.literal("tp")
                        .then(Commands.argument("biome", StringArgumentType.greedyString())
                                .executes(ctx -> teleportToBiome(ctx.getSource(), StringArgumentType.getString(ctx, "biome")))))

                // 2. /ivalice status — Mostra Gil, LP, Rank do Clã Centurio, Licenças e Bestiário
                .then(Commands.literal("status")
                        .executes(ctx -> showPlayerStatus(ctx.getSource())))

                // 3. /ivalice bestiary — Mostra progresso completo do Bestiário (Clan Primer)
                .then(Commands.literal("bestiary")
                        .executes(ctx -> showBestiary(ctx.getSource())))

                .then(Commands.literal("catalog")
                        .then(Commands.literal("search")
                                .then(Commands.argument("query", StringArgumentType.greedyString())
                                        .executes(ctx -> IvaliceTzaCatalog.search(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "query")))))
                        .then(Commands.literal("buy")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> IvaliceTzaCatalog.buy(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("recipe")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogOrder(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "id"), false))))
                        .then(Commands.literal("order")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> catalogOrder(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "id"), true)))))
                .then(Commands.literal("job")
                        .then(Commands.literal("choose")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> ZodiacJobs.choose(ctx.getSource().getPlayerOrException(),
                                                StringArgumentType.getString(ctx, "id")) ? 1 : 0)))
                        .then(Commands.literal("reset")
                                .executes(ctx -> ZodiacJobs.reset(ctx.getSource().getPlayerOrException()) ? 1 : 0)))
                .then(Commands.literal("engage")
                        .then(Commands.argument("entityId", IntegerArgumentType.integer())
                                .executes(ctx -> engage(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "entityId")))))

                // /ivalice battle <entityId> <action> [revision]
                .then(Commands.literal("battle")
                        .then(Commands.argument("entityId", IntegerArgumentType.integer())
                                .then(Commands.argument("action", StringArgumentType.word())
                                        .executes(ctx -> executeBattleTurn(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "entityId"),
                                                StringArgumentType.getString(ctx, "action"), 0))
                                        .then(Commands.argument("revision", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeBattleTurn(ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "entityId"),
                                                        StringArgumentType.getString(ctx, "action"),
                                                        IntegerArgumentType.getInteger(ctx, "revision")))))))

                // 5. /ivalice hunt spawn <markId> — Aceita e invoca a Mark/Esper da Caçada do Clã Centurio
                .then(Commands.literal("hunt")
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("markId", StringArgumentType.word())
                                        .executes(ctx -> spawnHuntMark(ctx.getSource(), StringArgumentType.getString(ctx, "markId"))))))

                // 6. /ivalice license unlock <licenseId> — Desbloqueia licença no Tabuleiro de Licenças
                .then(Commands.literal("license")
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("licenseId", StringArgumentType.word())
                                        .executes(ctx -> unlockLicense(ctx.getSource(), StringArgumentType.getString(ctx, "licenseId"))))))

                // 7. /ivalice gambit toggle — Alterna Gambits automáticos
                .then(Commands.literal("gambit")
                        .then(Commands.literal("toggle")
                                .executes(ctx -> toggleGambits(ctx.getSource()))))

                // 8. /ivalice bazaar buy <packageId> — Compra pacote no Bazaar de Rabanastre
                .then(Commands.literal("bazaar")
                        .then(Commands.literal("buy")
                                .then(Commands.argument("packageId", StringArgumentType.word())
                                        .executes(ctx -> buyBazaarPackage(ctx.getSource(), StringArgumentType.getString(ctx, "packageId"))))))

                // 9. /ivalice kit — Entrega o Kit de Aventureiro de Ivalice (Teleport Stone, Clan Primer, License Tome, Gambit Scroll, Armas)
                .then(Commands.literal("kit")
                        .executes(ctx -> giveStarterKit(ctx.getSource())))
        );
    }

    private static int teleportToBiome(CommandSourceStack source, String rawBiomeName) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        String biomeName = rawBiomeName.strip();
        Identifier biomeId = Identifier.tryParse(biomeName.contains(":")
                ? biomeName : IvaliceMod.MODID + ":" + biomeName);
        ServerLevel level = player.level();
        if (biomeId == null || !level.registryAccess().lookupOrThrow(Registries.BIOME).containsKey(biomeId)) {
            source.sendFailure(Component.literal("[Gate Crystal] Bioma desconhecido: " + biomeName));
            return 0;
        }

        ResourceKey<Biome> targetKey = ResourceKey.create(Registries.BIOME, biomeId);
        BlockPos origin = player.blockPosition();

        player.sendSystemMessage(Component.literal("[Gate Crystal] Sintonizando Mist para localizar a região '" + biomeId + "'...")
                .withStyle(ChatFormatting.AQUA));

        Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(
                holder -> holder.is(targetKey),
                origin,
                6400,
                32,
                64
        );

        if (found == null) {
            player.sendSystemMessage(Component.literal("[Gate Crystal] Região não encontrada num raio de 6.400 blocos a partir daqui. Tente listar biomas válidos de Ivalice!")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        BlockPos pos = found.getFirst();
        // Carrega o chunk de destino e calcula a altura segura da superfície
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        if (surfaceY <= level.getMinY()) {
            surfaceY = 72;
        }

        player.teleportTo(level, pos.getX() + 0.5, surfaceY + 1.0, pos.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.1F);

        player.sendSystemMessage(Component.literal(String.format(
                "✦ [GATE CRYSTAL] Teleportado para %s em [X=%d, Y=%d, Z=%d]!",
                biomeId, pos.getX(), surfaceY + 1, pos.getZ()
        )).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        return 1;
    }

    private static int showPlayerStatus(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        IvalicePlayerData.PlayerProfile profile = IvalicePlayerData.get(player);
        IvalicePlayerData.applyLicenseAttributes(player);

        player.sendSystemMessage(Component.literal("═══ [PERFIL DO CLÃ CENTURIO — IVALICE] ═══").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("• Nível " + profile.getBattleLevel() + " | " + profile.getClanRank()).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.literal("• Fundos: " + profile.gil + " Gil   |   License Points: " + profile.lp + " LP").withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal("• Caçadas Concluídas: " + profile.completedHunts.size() + " / " + IvalicePlayerData.MARK_HUNTS.size()).withStyle(ChatFormatting.AQUA));
        player.sendSystemMessage(Component.literal("• Licenças Ativas: " + profile.unlockedLicenses).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.sendSystemMessage(Component.literal("• Gambits Ativos: " + profile.activeGambits).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int showBestiary(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        IvalicePlayerData.PlayerProfile profile = IvalicePlayerData.get(player);

        player.sendSystemMessage(Component.literal("═══ [CLAN PRIMER — BESTIÁRIO DE IVALICE] ═══").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (IvaliceMonsterStats m : IvaliceMonsterStats.ALL_MONSTERS) {
            int kills = profile.getKills(m.id());
            ChatFormatting color = kills > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY;
            player.sendSystemMessage(Component.literal(String.format(
                    "• %s [Lv.%d | %s] — Abates: %d | Fraqueza: %s | Habitat: %s",
                    m.displayName(), m.level(), m.family(), kills, m.weakness(), m.habitat()
            )).withStyle(color));
        }
        return 1;
    }

    private static int engage(CommandSourceStack source, int entityId) {
        ServerPlayer player = source.getPlayer();
        return player != null && player.level().getEntity(entityId) instanceof IvaliceMonsterEntity monster
                && BattleManager.engage(player, monster) ? 1 : 0;
    }

    private static int executeBattleTurn(CommandSourceStack source, int entityId, String action, int revision) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return 0;
        }
        BattleManager.executeTurnAction(player, entityId, action, revision);
        return 1;
    }

    private static int spawnHuntMark(CommandSourceStack source, String markId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        ServerLevel level = (ServerLevel) player.level();
        IvalicePlayerData.PlayerProfile profile = IvalicePlayerData.get(player);

        for (IvalicePlayerData.MarkHuntEntry hunt : IvalicePlayerData.MARK_HUNTS) {
            if (hunt.id().equalsIgnoreCase(markId) || hunt.entityId().equalsIgnoreCase(markId)) {
                profile.activeHuntId = hunt.id();
                EntityType<IvaliceMonsterEntity> type = IvaliceEntities.getById(hunt.entityId());
                BlockPos spawnPos = player.blockPosition().relative(player.getDirection(), 5);
                IvaliceMonsterEntity mark = type.create(level, EntitySpawnReason.EVENT);
                if (mark != null) {
                    mark.setPos(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5);
                    level.addFreshEntity(mark);
                    level.playSound(null, spawnPos, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.0F, 1.0F);
                    player.sendSystemMessage(Component.literal(String.format(
                            "🎯 [CAÇADA ACEITA — %s] O alvo '%s' (Cliente: %s) surgiu diante de você!\nDerrote-o para receber +%d Gil, +%d LP e 1x %s!",
                            hunt.rank(), hunt.markName(), hunt.petitioner(), hunt.gilReward(), hunt.lpReward(), hunt.bountyItemId()
                    )).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    IvalicePlayerData.save(player);
                    return 1;
                }
            }
        }
        player.sendSystemMessage(Component.literal("[Clã Centurio] Caçada não encontrada: " + markId).withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int unlockLicense(CommandSourceStack source, String licenseId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        IvalicePlayerData.PlayerProfile profile = IvalicePlayerData.get(player);

        for (IvalicePlayerData.LicenseNode node : IvalicePlayerData.LICENSE_CATALOG) {
            if (node.id().equalsIgnoreCase(licenseId)) {
                if (profile.unlockedLicenses.contains(node.id())) {
                    player.sendSystemMessage(Component.literal("[License Board] Você já desbloqueou a licença '" + node.name() + "'!")
                            .withStyle(ChatFormatting.YELLOW));
                    return 1;
                }
                if (profile.lp < node.lpCost()) {
                    player.sendSystemMessage(Component.literal(String.format(
                            "[License Board] LP insuficiente para '%s'! Custo: %d LP (Você possui: %d LP). Derrote monstros ou Marks para ganhar LP!",
                            node.name(), node.lpCost(), profile.lp
                    )).withStyle(ChatFormatting.RED));
                    return 0;
                }
                profile.lp -= node.lpCost();
                profile.unlockedLicenses.add(node.id());
                IvalicePlayerData.applyLicenseAttributes(player);
                IvalicePlayerData.save(player);
                player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
                player.sendSystemMessage(Component.literal(String.format(
                        "🛡 [LICENSE BOARD] Licença '%s' desbloqueada! (%s) | LP restante: %d",
                        node.name(), node.description(), profile.lp
                )).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                return 1;
            }
        }
        return 0;
    }

    private static int toggleGambits(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        IvalicePlayerData.PlayerProfile profile = IvalicePlayerData.get(player);

        if (profile.activeGambits.contains("SELF_HP_50_POTION")) {
            profile.activeGambits.clear();
            player.sendSystemMessage(Component.literal("⚙ [GAMBITS] Sistema de Gambits Automáticos: DESATIVADO (Controle 100% Manual).")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            profile.activeGambits.clear();
            profile.activeGambits.add("SELF_HP_50_POTION");
            profile.activeGambits.add("FOE_WEAKNESS_MAGIC");
            profile.activeGambits.add("FOE_NEAREST_ATTACK");
            player.sendSystemMessage(Component.literal("⚙ [GAMBITS] Sistema de Gambits Automáticos: ATIVADO!\n1. Self: HP < 50% -> Auto-Potion/Cura\n2. Foe: Elemental Weakness -> Cast Magick\n3. Foe: Nearest Visible -> Attack")
                    .withStyle(ChatFormatting.GREEN));
        }
        IvalicePlayerData.save(player);
        return 1;
    }


    private static int catalogOrder(ServerPlayer player, String id, boolean placeOrder) {
        var entry = IvaliceTzaCatalog.getEntry(id);
        Item item = IvaliceItems.getItemById(id);
        if (entry == null || item == null) {
            player.sendSystemMessage(Component.literal("Item desconhecido. Use /ivalice catalog search <nome>."));
            return 0;
        }
        if (entry.isSoldInRegionalShops()) {
            player.sendSystemMessage(Component.literal(entry.name() + " custa " + entry.price()
                    + " Gil. Use /ivalice catalog buy " + id + "."));
            return 1;
        }
        var pack = IvaliceTzaCatalog.bazaarPackage(id);
        if (pack != null) {
            player.sendSystemMessage(Component.literal(pack.description() + " Use /ivalice bazaar buy " + pack.id() + "."));
            return 1;
        }

        var order = IvaliceTzaCatalog.orderFor(entry);
        Item ingredient = IvaliceItems.getItemById(order.ingredient());
        var profile = IvalicePlayerData.get(player);
        String requirements = entry.name() + " • Nível " + order.minimumLevel() + " • " + order.gil() + " Gil • "
                + order.quantity() + "× " + new ItemStack(ingredient).getHoverName().getString();
        if (!placeOrder || profile.getBattleLevel() < order.minimumLevel() || profile.gil < order.gil()
                || player.getInventory().countItem(ingredient) < order.quantity()) {
            player.sendSystemMessage(Component.literal(requirements + " • /ivalice catalog order " + id));
            return placeOrder ? 0 : 1;
        }

        removeItemsFromInventory(player, ingredient, order.quantity());
        profile.gil -= order.gil();
        player.getInventory().placeItemBackInInventory(new ItemStack(item), Prediction.SERVER_ONLY);
        IvalicePlayerData.save(player);
        player.sendSystemMessage(Component.literal("Encomenda concluída • " + entry.name()).withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static int buyBazaarPackage(CommandSourceStack source, String packageId) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        IvalicePlayerData.PlayerProfile profile = IvalicePlayerData.get(player);

        for (IvalicePlayerData.BazaarPackage pack : IvalicePlayerData.BAZAAR_PACKAGES) {
            if (pack.id().equalsIgnoreCase(packageId)) {
                boolean hasLoot = true;
                for (Map.Entry<String, Integer> req : pack.requiredLoot().entrySet()) {
                    Item reqItem = IvaliceItems.getItemById(req.getKey());
                    if (reqItem == null || player.getInventory().countItem(reqItem) < req.getValue()) {
                        hasLoot = false;
                        break;
                    }
                }

                if (!hasLoot && pack.requiresLoot()) {
                    player.sendSystemMessage(Component.literal(pack.description()).withStyle(ChatFormatting.RED));
                    return 0;
                }

                if (!hasLoot && profile.gil < pack.gilCost()) {
                    player.sendSystemMessage(Component.literal(String.format(
                            "[Bazaar de Rabanastre] Para adquirir '%s', você precisa dos espólios (%s) OU de %d Gil (Você tem %d Gil)!",
                            pack.packageName(), pack.description(), pack.gilCost(), profile.gil
                    )).withStyle(ChatFormatting.RED));
                    return 0;
                }

                if (hasLoot) {
                    for (Map.Entry<String, Integer> req : pack.requiredLoot().entrySet()) {
                        Item reqItem = IvaliceItems.getItemById(req.getKey());
                        if (reqItem != null) {
                            removeItemsFromInventory(player, reqItem, req.getValue());
                        }
                    }
                } else {
                    profile.gil -= pack.gilCost();
                }

                Item reward = IvaliceItems.getItemById(pack.rewardItemId());
                if (reward != null) {
                    player.getInventory().placeItemBackInInventory(new ItemStack(reward, pack.rewardCount()), Prediction.SERVER_ONLY);
                }
                profile.bazaarPurchased.add(pack.id());
                IvalicePlayerData.save(player);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.9F, 1.2F);
                player.sendSystemMessage(Component.literal(String.format(
                        "⚖ [BAZAAR DE RABANASTRE] Pacote '%s' desbloqueado! Você recebeu %dx %s!",
                        pack.packageName(), pack.rewardCount(), pack.rewardItemId()
                )).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                return 1;
            }
        }
        return 0;
    }

    private static void removeItemsFromInventory(ServerPlayer player, Item targetItem, int countToRemove) {
        int remaining = countToRemove;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(targetItem)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    private static int giveStarterKit(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        player.getInventory().add(new ItemStack(IvaliceItems.TELEPORT_STONE.get(), 1));
        player.getInventory().add(new ItemStack(IvaliceItems.CLAN_PRIMER.get(), 1));
        player.getInventory().add(new ItemStack(IvaliceItems.LICENSE_TOME.get(), 1));
        player.getInventory().add(new ItemStack(IvaliceItems.GAMBIT_SCROLL.get(), 1));
        player.getInventory().add(new ItemStack(IvaliceItems.MYTHRIL_SWORD.get(), 1));
        player.getInventory().add(new ItemStack(IvaliceItems.HI_POTION.get(), 5));
        player.getInventory().add(new ItemStack(IvaliceItems.TECHNICK_LIBRA.get(), 1));

        player.sendSystemMessage(Component.literal(
                "✦ [CLÃ CENTURIO] Você recebeu o Kit de Aventureiro de Ivalice (Teleport Stone, Clan Primer, License Tome, Gambit Scroll, Mythril Sword, Hi-Potions e Libra)!"
        ).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        return 1;
    }
}
