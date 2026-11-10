package com.ivalice.worldgen.biome;

import com.ivalice.config.IvaliceConfig;
import com.ivalice.worldgen.structure.IvaliceStarterVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.List;

public final class IvaliceOverworldSpawns {
    private IvaliceOverworldSpawns() {}

    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (level.dimension().equals(Level.OVERWORLD) && event.getMobCategory() == MobCategory.MONSTER
                && IvaliceStarterVillage.protects(level, event.getPos())) {
            List.copyOf(event.getSpawnerDataList()).forEach(event::removeSpawnerData);
            return;
        }

        boolean overworld = usesIvaliceFauna(level);
        if (!overworld && !usesRuinFauna(level)) return;

        for (var entry : List.copyOf(event.getSpawnerDataList())) {
            if (isVanillaFauna(entry.value().type())) {
                event.removeSpawnerData(entry);
            }
        }
        if (!overworld) return;

        BlockPos pos = event.getPos();
        if (level.getBiome(pos).is(IvaliceBiomes.IS_IVALICE_BIOME)) {
            return;
        }

        // Chunks antigos mantêm seu bioma gravado. Use a região que o gerador atual escolheria aqui.
        Holder<Biome> region = level.getUncachedNoiseBiome(
                QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
        if (!region.is(IvaliceBiomes.IS_IVALICE_BIOME)) {
            region = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(IvaliceBiomes.OZMONE_PLAIN);
        }
        MobSpawnSettings spawns = region.value().getAttributes()
                .applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
        for (var spawn : spawns.getMobsToSpawn(event.getMobCategory()).unwrap()) {
            event.addSpawnerData(spawn);
        }
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && (usesIvaliceFauna(level) || usesRuinFauna(level))
                && event.getEntity() instanceof Mob mob && isVanillaFauna(mob.getType())
                && !(mob instanceof Villager) && !mob.hasCustomName() && !(mob instanceof TamableAnimal pet && pet.isTame())) {
            event.setCanceled(true);
        }
    }

    private static boolean usesIvaliceFauna(ServerLevel level) {
        return level.dimension().equals(Level.OVERWORLD) && IvaliceConfig.ENABLE_OVERWORLD_BIOMES.getAsBoolean();
    }

    private static boolean usesRuinFauna(ServerLevel level) {
        return level.dimension().equals(Level.NETHER) || level.dimension().equals(Level.END);
    }

    private static boolean isVanillaFauna(EntityType<?> type) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return type.getCategory() != MobCategory.MISC && id.getNamespace().equals("minecraft")
                && !id.getPath().equals("ender_dragon");
    }
}
