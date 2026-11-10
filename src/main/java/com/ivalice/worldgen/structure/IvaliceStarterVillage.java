package com.ivalice.worldgen.structure;

import com.ivalice.IvaliceMod;
import com.ivalice.combat.BattleManager;
import com.ivalice.entity.IvaliceGuardEntity;
import com.ivalice.registry.IvaliceBlocks;
import com.ivalice.registry.IvaliceEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class IvaliceStarterVillage {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(IvaliceMod.MODID, "starter_village");
    private static final SavedDataType<VillageData> DATA = new SavedDataType<>(ID, VillageData::new,
            BlockPos.CODEC.optionalFieldOf("origin").codec().xmap(VillageData::new, data -> data.origin));
    private static final int HALF_SIZE = 16;
    private static final int APPROACH = 6;
    private static final int BUILD_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS;
    private static final BlockPos PLAZA = new BlockPos(HALF_SIZE, 1, HALF_SIZE);
    private static final List<BlockPos> POSTS = List.of(new BlockPos(16, 1, 4), new BlockPos(16, 1, 28),
            new BlockPos(6, 1, 16), new BlockPos(26, 1, 16));

    private IvaliceStarterVillage() {}

    public static void prepare(ServerLevel level) {
        VillageData data = level.getDataStorage().computeIfAbsent(DATA);
        if (data.origin.isEmpty()) {
            var template = level.getStructureTemplateManager().get(ID);
            if (template.isEmpty()) {
                IvaliceMod.LOGGER.error("Starter village template is missing");
                return;
            }
            BlockPos origin = findSite(level);
            if (origin == null) {
                IvaliceMod.LOGGER.warn("No dry, unused terrain found for the starter village near {}", level.getRespawnData().pos());
                return;
            }
            // Generate all affected chunks before placing the template across chunk borders.
            BlockPos from = origin.offset(0, 0, -APPROACH);
            BlockPos to = origin.offset(2 * HALF_SIZE, 0, 2 * HALF_SIZE + APPROACH);
            ChunkPos.rangeClosed(ChunkPos.containing(from), ChunkPos.containing(to))
                    .forEach(chunk -> level.getChunk(chunk.x(), chunk.z()));

            if (!template.get().placeInWorld(level, origin, origin, new StructurePlaceSettings(),
                    level.getRandom(), BUILD_FLAGS)) {
                IvaliceMod.LOGGER.error("Could not place the starter village at {}", origin);
                return;
            }
            addFoundations(level, origin);
            addApproach(level, origin, Direction.NORTH);
            addApproach(level, origin, Direction.SOUTH);
            data.origin = Optional.of(origin);
            data.setDirty();
            IvaliceMod.LOGGER.info("Starter village Refúgio de Dalmasca placed at {}", origin.offset(PLAZA));
        }
        BlockPos arrival = data.origin.orElseThrow().offset(PLAZA);
        level.setRespawnData(new LevelData.RespawnData(GlobalPos.of(Level.OVERWORLD, arrival), 0, 0));
    }

    private static BlockPos findSite(ServerLevel level) {
        BlockPos start = level.getRespawnData().pos();
        ServerChunkCache chunks = level.getChunkSource();
        for (int ring = 0; ring <= 40; ring++) {
            for (int dz = -ring; dz <= ring; dz++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    int x = start.getX() + 64 * dx;
                    int z = start.getZ() + 64 * dz;
                    BlockPos origin = findDryFootprint(level, chunks, x, z);
                    if (origin == null) continue;
                    BlockPos from = origin.offset(0, 0, -APPROACH);
                    BlockPos to = origin.offset(2 * HALF_SIZE, 0, 2 * HALF_SIZE + APPROACH);
                    if (ChunkPos.rangeClosed(ChunkPos.containing(from), ChunkPos.containing(to))
                            .noneMatch(chunk -> chunks.hasChunk(chunk.x(), chunk.z()) || chunks.chunkMap.read(chunk).join().isPresent())) {
                        return origin;
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos findDryFootprint(ServerLevel level, ServerChunkCache chunks, int x, int z) {
        var generator = chunks.getGenerator();
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int dz = -HALF_SIZE; dz <= HALF_SIZE; dz += 8) {
            for (int dx = -HALF_SIZE; dx <= HALF_SIZE; dx += 8) {
                int surface = generator.getFirstFreeHeight(x + dx, z + dz, Heightmap.Types.WORLD_SURFACE_WG,
                        level, chunks.randomState());
                if (surface <= generator.getSeaLevel() || surface != generator.getFirstFreeHeight(x + dx, z + dz,
                        Heightmap.Types.OCEAN_FLOOR_WG, level, chunks.randomState())) return null;
                low = Math.min(low, surface);
                high = Math.max(high, surface);
                if (high - low > 6) return null;
            }
        }
        BlockPos origin = new BlockPos(x - HALF_SIZE, high - 1, z - HALF_SIZE);
        return level.getWorldBorder().isWithinBounds(origin.offset(0, 0, -APPROACH))
                && level.getWorldBorder().isWithinBounds(origin.offset(2 * HALF_SIZE, 0, 2 * HALF_SIZE + APPROACH))
                ? origin : null;
    }

    private static void addFoundations(ServerLevel level, BlockPos origin) {
        BlockState stone = IvaliceBlocks.DALMASCA_SANDSTONE.get().defaultBlockState();
        for (int z = 0; z <= 2 * HALF_SIZE; z++) {
            for (int x = 0; x <= 2 * HALF_SIZE; x++) {
                BlockPos pos = origin.offset(x, -1, z);
                for (int depth = 0; depth < 10 && !level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP); depth++) {
                    level.setBlock(pos, stone, Block.UPDATE_CLIENTS);
                    pos = pos.below();
                }
            }
        }
    }

    private static void addApproach(ServerLevel level, BlockPos origin, Direction outward) {
        BlockPos edge = origin.offset(HALF_SIZE, 0, outward == Direction.NORTH ? 0 : 2 * HALF_SIZE);
        ServerChunkCache chunks = level.getChunkSource();
        var generator = chunks.getGenerator();
        BlockState brick = IvaliceBlocks.DALMASCA_CARVED_BRICKS.get().defaultBlockState();
        BlockState stairs = Blocks.SANDSTONE_STAIRS.defaultBlockState();
        BlockState stone = IvaliceBlocks.DALMASCA_SANDSTONE.get().defaultBlockState();
        int previousY = edge.getY();
        for (int step = 1; step <= APPROACH; step++) {
            int z = edge.getZ() + outward.getStepZ() * step;
            int surface = generator.getFirstFreeHeight(edge.getX(), z, Heightmap.Types.OCEAN_FLOOR_WG, level, chunks.randomState());
            int floorY = Mth.clamp(surface - 1, previousY - 1, previousY + 1);
            int pathY = Math.max(floorY, previousY);
            BlockState path = floorY == previousY ? brick
                    : stairs.setValue(StairBlock.FACING, floorY > previousY ? outward : outward.getOpposite());
            for (int width = -1; width <= 1; width++) {
                BlockPos pos = new BlockPos(edge.getX() + width, pathY, z);
                for (int headroom = 1; headroom <= 4; headroom++) {
                    level.setBlock(pos.above(headroom), Blocks.AIR.defaultBlockState(), BUILD_FLAGS);
                }
                level.setBlock(pos, path, BUILD_FLAGS);
                BlockPos support = pos.below();
                for (int depth = 0; depth < 6 && !level.getBlockState(support).isFaceSturdy(level, support, Direction.UP); depth++) {
                    level.setBlock(support, stone, BUILD_FLAGS);
                    support = support.below();
                }
            }
            previousY = floorY;
        }
    }

    public static void welcome(ServerPlayer player) {
        if (player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) == 0) travel(player);
    }

    public static boolean travel(ServerPlayer player) {
        if (BattleManager.isInBattle(player)) {
            player.sendSystemMessage(Component.translatable("ivalice.village.in_battle"));
            return false;
        }
        ServerLevel level = player.level().getServer().overworld();
        BlockPos pos = findArrival(level);
        if (pos == null) {
            player.sendSystemMessage(Component.translatable("ivalice.village.unavailable"));
            return false;
        }
        Vec3 arrival = Vec3.atBottomCenterOf(pos);
        if (!player.teleportTo(level, arrival.x, arrival.y, arrival.z, Set.of(), 0, 0, true)) return false;
        player.sendSystemMessage(Component.translatable("ivalice.village.arrival"));
        return true;
    }

    public static void onRespawn(PlayerRespawnPositionEvent event) {
        if (event.isFromEndFight()) return;
        ServerLevel level = event.getEntity().level().getServer().overworld();
        BlockPos pos = findArrival(level);
        if (pos != null) {
            event.setTeleportTransition(new TeleportTransition(level, Vec3.atBottomCenterOf(pos), Vec3.ZERO, 0, 0,
                    event.getTeleportTransition().postTeleportTransition()));
            event.setCopyOriginalSpawnPosition(false);
        }
    }

    private static BlockPos findArrival(ServerLevel level) {
        BlockPos origin = origin(level.getDataStorage());
        if (origin == null) return null;
        BlockPos center = origin.offset(PLAZA);
        for (BlockPos pos : BlockPos.withinManhattan(center, 7)) {
            if (pos.getY() != center.getY()) continue;
            if (canStandAt(level, pos)) return pos.immutable();
        }
        return null;
    }

    private static boolean canStandAt(ServerLevel level, BlockPos pos) {
        BlockPos floor = pos.below();
        return level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    public static boolean protects(ServerLevel level, BlockPos pos) {
        BlockPos origin = origin(level.getDataStorage());
        return origin != null && pos.getX() >= origin.getX() && pos.getX() <= origin.getX() + 2 * HALF_SIZE
                && pos.getZ() >= origin.getZ() && pos.getZ() <= origin.getZ() + 2 * HALF_SIZE
                && pos.getY() >= origin.getY() && pos.getY() <= origin.getY() + 24;
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)
                || level.getGameTime() % 100 != 0) return;
        BlockPos origin = origin(level.getDataStorage());
        if (origin == null) return;
        BlockPos center = origin.offset(PLAZA);
        if (level.getNearestPlayer(center.getX(), center.getY(), center.getZ(), 44, false) == null) return;
        for (BlockPos offset : POSTS) {
            BlockPos post = origin.offset(offset);
            int range = IvaliceGuardEntity.PATROL_RADIUS + 4;
            AABB patrol = new AABB(post).inflate(range);
            boolean loaded = ChunkPos.rangeClosed(ChunkPos.containing(post.offset(-range, 0, -range)),
                    ChunkPos.containing(post.offset(range, 0, range))).allMatch(chunk -> level.areEntitiesLoaded(chunk.pack()));
            if (!loaded || !level.isPositionEntityTicking(post) || !canStandAt(level, post)) continue;
            if (!level.getEntitiesOfClass(IvaliceGuardEntity.class, patrol,
                    guard -> guard.isAlive() && guard.getHomeRadius() > 0 && guard.getHomePosition().equals(post)).isEmpty()) continue;
            var guard = IvaliceEntities.DALMASCA_GUARD.get().create(level, EntitySpawnReason.STRUCTURE);
            if (guard != null) {
                guard.snapTo(Vec3.atBottomCenterOf(post), 0, 0);
                guard.setHomeTo(post, IvaliceGuardEntity.PATROL_RADIUS);
                if (level.noCollision(guard)) level.addFreshEntity(guard);
            }
        }
    }

    private static BlockPos origin(SavedDataStorage storage) {
        VillageData data = storage.get(DATA);
        return data == null ? null : data.origin.orElse(null);
    }

    private static final class VillageData extends SavedData {
        private Optional<BlockPos> origin;

        private VillageData() {
            this(Optional.empty());
        }

        private VillageData(Optional<BlockPos> origin) {
            this.origin = origin;
        }
    }
}
