package com.ivalice.worldgen.season;

import com.ivalice.IvaliceMod;
import com.ivalice.config.IvaliceConfig;
import com.ivalice.worldgen.biome.IvaliceBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

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

        // Verifica uma vez a cada 200 ticks (10 segundos)
        long gameTime = serverLevel.getGameTime();
        if (gameTime % 200L != 0L) {
            return;
        }

        GizaSeason currentSeason = getCurrentSeason(serverLevel.getOverworldClockTime());
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
