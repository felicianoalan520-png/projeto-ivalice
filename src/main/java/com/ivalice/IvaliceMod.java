package com.ivalice;

import com.ivalice.combat.BattleManager;
import com.ivalice.combat.IvaliceEquipment;
import com.ivalice.combat.IvaliceStatuses;
import com.ivalice.command.IvaliceCommands;
import com.ivalice.config.IvaliceConfig;
import com.ivalice.data.IvalicePlayerData;
import com.ivalice.network.IvaliceStatePayload;
import com.ivalice.job.ZodiacJobs;
import com.ivalice.registry.IvaliceBlocks;
import com.ivalice.registry.IvaliceCreativeTabs;
import com.ivalice.registry.IvaliceEntities;
import com.ivalice.registry.IvaliceItems;
import com.ivalice.registry.IvaliceVillagers;
import com.ivalice.worldgen.biome.IvaliceBiomes;
import com.ivalice.worldgen.biome.IvaliceOverworldRegion;
import com.ivalice.worldgen.biome.IvaliceOverworldSpawns;
import com.ivalice.worldgen.season.GizaSeasonManager;
import com.ivalice.worldgen.surface.IvaliceSurfaceRules;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import terrablender.api.MaterialRuleManager;
import terrablender.api.Regions;
import terrablender.worldgen.DefaultOverworldRegion;

/**
 * Classe principal do Projeto Ivalice (Final Fantasy XII: The Zodiac Age para Minecraft Java 26.3 / NeoForge).
 */
@Mod(IvaliceMod.MODID)
public class IvaliceMod {
    public static final String MODID = "ivalice";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IvaliceMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(IvaliceStatePayload::register);

        // Registro modular de blocos, itens, entidades e abas criativas
        IvaliceBlocks.register(modEventBus);
        IvaliceStatuses.register(modEventBus);
        IvaliceItems.register(modEventBus);
        IvaliceEntities.register(modEventBus);
        IvaliceVillagers.register(modEventBus);
        IvaliceCreativeTabs.register(modEventBus);

        // Eventos do servidor e sistema sazonal de Giza Plains
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.addListener(BattleManager::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(IvaliceEquipment::onTick);
        NeoForge.EVENT_BUS.addListener(BattleManager::onLogout);
        NeoForge.EVENT_BUS.addListener(BattleManager::onDamage);
        NeoForge.EVENT_BUS.addListener(IvaliceStatuses::onDeath);
        NeoForge.EVENT_BUS.addListener(ZodiacJobs::onAttack);
        NeoForge.EVENT_BUS.addListener(ZodiacJobs::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(GizaSeasonManager::onLevelTickPost);
        NeoForge.EVENT_BUS.addListener(IvaliceOverworldSpawns::onPotentialSpawns);
        NeoForge.EVENT_BUS.addListener(IvaliceOverworldSpawns::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(IvaliceVillagers::onEntityInteract);

        // Registro do arquivo de configuração
        modContainer.registerConfig(ModConfig.Type.LOCAL, IvaliceConfig.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (IvaliceConfig.ENABLE_OVERWORLD_BIOMES.getAsBoolean()) {
                int weight = IvaliceConfig.OVERWORLD_REGION_WEIGHT.getAsInt();
                Identifier regionId = Identifier.fromNamespaceAndPath(MODID, "overworld_ivalice");
                // TerraBlender usa a região vanilla no índice zero como fallback interno.
                // O peso zero mantém esse índice sem selecionar a região na geração normal.
                Regions.register(new DefaultOverworldRegion(0));
                Regions.register(new IvaliceOverworldRegion(regionId, weight));

                MaterialRuleManager.addRules(
                        MaterialRuleManager.RuleCategory.OVERWORLD,
                        MODID,
                        IvaliceSurfaceRules::buildOverworldRules
                );

                LOGGER.info("[Projeto Ivalice] TerraBlender Overworld Region '{}' registrada com peso {} e MaterialRules ativas para {} biomas de Final Fantasy XII.",
                        regionId, weight, IvaliceBiomes.ALL_BIOMES.size());
            }
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        IvaliceCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            IvalicePlayerData.applyLicenseAttributes(serverPlayer);
            IvaliceStatePayload.sendProfile(serverPlayer);
            // Se o jogador não tiver o Teleport Stone nem o Clan Primer no inventário, entrega o guia inicial
            if (serverPlayer.getInventory().countItem(IvaliceItems.TELEPORT_STONE.get()) == 0
                    && serverPlayer.getInventory().countItem(IvaliceItems.CLAN_PRIMER.get()) == 0) {
                serverPlayer.getInventory().add(new ItemStack(IvaliceItems.TELEPORT_STONE.get(), 1));
                serverPlayer.getInventory().add(new ItemStack(IvaliceItems.CLAN_PRIMER.get(), 1));
            }
            serverPlayer.sendSystemMessage(Component.literal(
                    "✦ [PROJETO IVALICE — FINAL FANTASY XII] Bem-vindo a Ivalice! Use 1 no teclado numérico para abrir o menu e 2 mirando uma criatura para batalhar. Use Num 5 para escolher suas profissões e /ivalice kit para explorar as 15 Regiões, Bestiário, Caçadas, Licenças e Bazaar!"
            ).withStyle(ChatFormatting.GOLD));
        }
    }

    @SubscribeEvent
    public void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        String destination;
        ChatFormatting color;
        if (event.getTo().equals(Level.NETHER)) {
            destination = "ivalice.dimension.ruin";
            color = ChatFormatting.GOLD;
        } else if (event.getTo().equals(Level.END)) {
            destination = "ivalice.dimension.ancient";
            color = ChatFormatting.AQUA;
        } else {
            return;
        }
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable(destination + ".subtitle")));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(destination).withStyle(color)));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        Registry<Biome> biomeRegistry = event.getServer().registryAccess().lookupOrThrow(Registries.BIOME);
        int loadedCount = 0;
        for (ResourceKey<Biome> key : IvaliceBiomes.ALL_BIOMES) {
            if (biomeRegistry.containsKey(key)) {
                loadedCount++;
            } else {
                LOGGER.warn("[Projeto Ivalice] Bioma ausente no registro do servidor: {}", key.identifier());
            }
        }
        LOGGER.info("[Projeto Ivalice] Servidor iniciando — {}/{} biomas canônicos de Ivalice validados no registro.",
                loadedCount, IvaliceBiomes.ALL_BIOMES.size());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        ServerLevel overworld = event.getServer().overworld();
        if (overworld == null) {
            return;
        }
        long seed = overworld.getSeed();
        LOGGER.info("[Projeto Ivalice] Verificando geração natural dos biomas de Final Fantasy XII no Overworld (Seed: {})...", seed);

        int foundBiomes = 0;
        BlockPos origin = new BlockPos(0, 64, 0);
        for (ResourceKey<Biome> biomeKey : IvaliceBiomes.ALL_BIOMES) {
            Pair<BlockPos, Holder<Biome>> result = overworld.findClosestBiome3d(
                    holder -> holder.is(biomeKey),
                    origin,
                    9600,
                    64,
                    64
            );
            if (result != null) {
                foundBiomes++;
                BlockPos pos = result.getFirst();
                ChunkAccess chunk = overworld.getChunk(pos);
                int surfaceY = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX() & 15, pos.getZ() & 15);
                BlockPos surfacePos = new BlockPos(pos.getX(), surfaceY, pos.getZ());
                BlockState topState = chunk.getBlockState(surfacePos);
                BlockState underState = chunk.getBlockState(surfacePos.below());
                Holder<Biome> actualBiome = overworld.getBiome(surfacePos);
                LOGGER.info("[Projeto Ivalice] Bioma '{}' localizado em [X={}, Y={}, Z={}] (dist: {}m) | Real={} | Topo={} | Sub={}",
                        biomeKey.identifier(), pos.getX(), surfaceY, pos.getZ(),
                        (int) Math.sqrt(pos.distSqr(origin)),
                        actualBiome.unwrapKey().map(k -> k.identifier().toString()).orElse("?"),
                        topState.getBlock(),
                        underState.getBlock());
            } else {
                LOGGER.warn("[Projeto Ivalice] Bioma '{}' não encontrado em um raio de 9600 blocos da origem nesta seed.",
                        biomeKey.identifier());
            }
        }
        LOGGER.info("[Projeto Ivalice] Validação de geração concluída: {}/{} biomas de Ivalice gerados ao redor da origem (Seed: {}).",
                foundBiomes, IvaliceBiomes.ALL_BIOMES.size(), seed);

        if ("true".equalsIgnoreCase(System.getProperty("ivalice.autoStopServer"))) {
            LOGGER.info("[Projeto Ivalice] Flag ivalice.autoStopServer detectada — encerrando servidor de teste de forma limpa.");
            event.getServer().halt(false);
        }
    }
}
