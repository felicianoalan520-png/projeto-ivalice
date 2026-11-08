package com.ivalice.worldgen.season;

import com.ivalice.IvaliceMod;
import com.ivalice.config.IvaliceConfig;
import com.ivalice.worldgen.biome.IvaliceBiomes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador climático sazonal de Giza Plains (Final Fantasy XII).
 * Em FFXII, Giza Plains alterna ciclicamente entre a Estação Seca (The Dry) e a Estação das Chuvas (The Rains).
 * Este sistema calcula a estação atual de forma determinística a partir do tempo do mundo, sem corromper ou regenerar chunks.
 * Também será consultado na Etapa 3 (surgimento condicional de criaturas como Ichthon/Toads/Silicon Tortoise)
 * e na Etapa 4 (Caçadas do Clã Centurio como Gil Snapper).
 */
public class GizaSeasonManager {

    public enum GizaSeason {
        THE_DRY("season.ivalice.giza_dry"),
        THE_RAINS("season.ivalice.giza_rains");

        private final String translationKey;

        GizaSeason(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    private static GizaSeason lastBroadcastedSeason = null;
    private static final Map<UUID, Identifier> LAST_PLAYER_BIOME = new ConcurrentHashMap<>();

    /**
     * Retorna a estação atual de Giza Plains com base no dayTime do mundo.
     */
    public static GizaSeason getCurrentSeason(long dayTime) {
        int dryDays = IvaliceConfig.GIZA_DRY_SEASON_DAYS.getAsInt();
        int rainDays = IvaliceConfig.GIZA_RAINS_SEASON_DAYS.getAsInt();
        int totalCycleDays = Math.max(2, dryDays + rainDays);

        long currentDay = (dayTime / 24000L) % totalCycleDays;
        return currentDay < dryDays ? GizaSeason.THE_DRY : GizaSeason.THE_RAINS;
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.dimension().equals(ServerLevel.OVERWORLD)) {
            return;
        }
        if (!IvaliceConfig.ENABLE_GIZA_SEASONAL_CYCLE.getAsBoolean()) {
            return;
        }

        long gameTime = serverLevel.getGameTime();
        GizaSeason currentSeason = getCurrentSeason(serverLevel.getOverworldClockTime());

        // Verifica entrada em regiões de Ivalice a cada 20 ticks (1 segundo)
        if (gameTime % 20L == 0L) {
            for (ServerPlayer player : serverLevel.players()) {
                BlockPos pos = player.blockPosition();
                Holder<Biome> biomeHolder = serverLevel.getBiome(pos);
                biomeHolder.unwrapKey().ifPresent(biomeKey -> {
                    Identifier id = biomeKey.identifier();
                    Identifier prev = LAST_PLAYER_BIOME.put(player.getUUID(), id);
                    if (IvaliceMod.MODID.equals(id.getNamespace()) && !id.equals(prev)) {
                        String langKey = "biome.ivalice." + id.getPath();
                        Component title = Component.translatable(langKey)
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                        Component subtitle = Component.literal("— REGIÃO DE IVALICE (FFXII) —")
                                .withStyle(ChatFormatting.YELLOW);
                        if (biomeKey.equals(IvaliceBiomes.GIZA_PLAINS_DRY) || biomeKey.equals(IvaliceBiomes.GIZA_PLAINS_RAINS)) {
                            subtitle = Component.translatable(currentSeason.getTranslationKey())
                                    .withStyle(ChatFormatting.AQUA);
                        }
                        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
                        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
                        player.connection.send(new ClientboundSetTitleTextPacket(title));
                        player.sendOverlayMessage(Component.literal("✦ ").withStyle(ChatFormatting.GOLD)
                                .append(Component.translatable(langKey).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                                .append(Component.literal(" ✦").withStyle(ChatFormatting.GOLD)));
                    }
                });
            }
        }

        // Verifica transição sazonal de Giza Plains a cada 200 ticks (10 segundos)
        if (gameTime % 200L != 0L) {
            return;
        }

        if (lastBroadcastedSeason != currentSeason) {
            lastBroadcastedSeason = currentSeason;
            IvaliceMod.LOGGER.debug("Giza Plains seasonal transition -> {}", currentSeason.name());

            for (ServerPlayer player : serverLevel.players()) {
                BlockPos pos = player.blockPosition();
                Holder<Biome> biomeHolder = serverLevel.getBiome(pos);
                if (biomeHolder.is(IvaliceBiomes.GIZA_PLAINS_DRY) || biomeHolder.is(IvaliceBiomes.GIZA_PLAINS_RAINS)) {
                    player.sendSystemMessage(Component.translatable(currentSeason.getTranslationKey()));
                }
            }
        }
    }
}
