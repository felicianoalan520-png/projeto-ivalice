package com.ivalice.worldgen.biome;

import com.ivalice.config.IvaliceConfig;
import com.ivalice.registry.IvaliceEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
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

        // Dimensão do Nether: Popula com criaturas de Final Fantasy IV & Final Fantasy XV
        if (level.dimension().equals(Level.NETHER) && event.getMobCategory() == MobCategory.MONSTER) {
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.MINDFLAYER.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 28));
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.IRON_GIANT.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 22));
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.RUBICANTE.get(), net.minecraft.util.valueproviders.ConstantInt.of(1)), 12));
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.GOLBEZ.get(), net.minecraft.util.valueproviders.ConstantInt.of(1)), 8));
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.IFRIT_PYREBURNER.get(), net.minecraft.util.valueproviders.ConstantInt.of(1)), 5));
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.ADAMANTOISE.get(), net.minecraft.util.valueproviders.ConstantInt.of(1)), 3));
            return;
        }

        // Dimensão do Fim (The End): Popula com criaturas de Final Fantasy V & Final Fantasy VI
        if (level.dimension().equals(Level.END) && event.getMobCategory() == MobCategory.MONSTER) {
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.ULTROS.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 26));
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.GIL_TURTLE.get(), net.minecraft.util.valueproviders.UniformInt.of(1, 2)), 22));
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.ATB_WEAPON.get(), net.minecraft.util.valueproviders.ConstantInt.of(1)), 10));
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.SHINRYU.get(), net.minecraft.util.valueproviders.ConstantInt.of(1)), 7));
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.OMEGA_MK.get(), net.minecraft.util.valueproviders.ConstantInt.of(1)), 5));
            event.addSpawnerData(new Weighted<>(new MobSpawnSettings.SpawnerData(IvaliceEntities.KEFKA_GOD.get(), net.minecraft.util.valueproviders.ConstantInt.of(1)), 3));
            return;
        }

        if (!usesIvaliceFauna(level)) {
            return;
        }

        for (var entry : List.copyOf(event.getSpawnerDataList())) {
            if (isVanillaFauna(entry.value().type())) {
                event.removeSpawnerData(entry);
            }
        }
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
        if (event.getLevel() instanceof ServerLevel level && usesIvaliceFauna(level)
                && event.getEntity() instanceof Mob mob && isVanillaFauna(mob.getType())
                && !mob.hasCustomName() && !(mob instanceof TamableAnimal pet && pet.isTame())) {
            event.setCanceled(true);
        }
    }

    private static boolean usesIvaliceFauna(ServerLevel level) {
        return level.dimension().equals(Level.OVERWORLD) && IvaliceConfig.ENABLE_OVERWORLD_BIOMES.getAsBoolean();
    }

    private static boolean isVanillaFauna(EntityType<?> type) {
        return type.getCategory() != MobCategory.MISC
                && BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals("minecraft");
    }
}
