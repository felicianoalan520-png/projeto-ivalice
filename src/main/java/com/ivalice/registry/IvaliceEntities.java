package com.ivalice.registry;

import com.ivalice.IvaliceMod;
import com.ivalice.entity.IvaliceMonsterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registro central de todas as 12 criaturas, Elite Marks e Espers de Final Fantasy XII (Etapa 3 e Etapa 5).
 */
public class IvaliceEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(IvaliceMod.MODID);

    // === 6 Criaturas Canônicas Iniciais (Etapa 3.2) ===
    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> WOLF =
            ENTITIES.registerEntityType("wolf", (type, level) -> new IvaliceMonsterEntity(type, level, "wolf"),
                    MobCategory.MONSTER, b -> b.sized(0.85F, 0.95F).eyeHeight(0.75F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> CACTOID =
            ENTITIES.registerEntityType("cactoid", (type, level) -> new IvaliceMonsterEntity(type, level, "cactoid"),
                    MobCategory.CREATURE, b -> b.sized(0.65F, 1.15F).eyeHeight(0.9F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> HYENA =
            ENTITIES.registerEntityType("hyena", (type, level) -> new IvaliceMonsterEntity(type, level, "hyena"),
                    MobCategory.MONSTER, b -> b.sized(0.95F, 1.05F).eyeHeight(0.85F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> GIZA_RABBIT =
            ENTITIES.registerEntityType("giza_rabbit", (type, level) -> new IvaliceMonsterEntity(type, level, "giza_rabbit"),
                    MobCategory.CREATURE, b -> b.sized(0.55F, 0.70F).eyeHeight(0.50F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> COCKATRICE =
            ENTITIES.registerEntityType("cockatrice", (type, level) -> new IvaliceMonsterEntity(type, level, "cockatrice"),
                    MobCategory.MONSTER, b -> b.sized(0.90F, 1.0F).eyeHeight(0.80F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> ICHTHON =
            ENTITIES.registerEntityType("ichthon", (type, level) -> new IvaliceMonsterEntity(type, level, "ichthon"),
                    MobCategory.MONSTER, b -> b.sized(0.75F, 0.75F).eyeHeight(0.55F));

    // === 4 Elite Marks — Caçadas do Clã Centurio (Etapa 4.2) ===
    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> ROGUE_TOMATO =
            ENTITIES.registerEntityType("rogue_tomato", (type, level) -> new IvaliceMonsterEntity(type, level, "rogue_tomato"),
                    MobCategory.MONSTER, b -> b.sized(0.95F, 1.35F).eyeHeight(1.05F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> THEXTERA =
            ENTITIES.registerEntityType("thextera", (type, level) -> new IvaliceMonsterEntity(type, level, "thextera"),
                    MobCategory.MONSTER, b -> b.sized(1.30F, 1.35F).eyeHeight(1.10F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> FLOWERING_CACTOID =
            ENTITIES.registerEntityType("flowering_cactoid", (type, level) -> new IvaliceMonsterEntity(type, level, "flowering_cactoid"),
                    MobCategory.MONSTER, b -> b.sized(0.80F, 1.45F).eyeHeight(1.15F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> WHITE_MOUSSE =
            ENTITIES.registerEntityType("white_mousse", (type, level) -> new IvaliceMonsterEntity(type, level, "white_mousse"),
                    MobCategory.MONSTER, b -> b.sized(1.50F, 1.40F).eyeHeight(1.0F));

    // === 2 Espers — Scions of Darkness (Etapa 5) ===
    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> BELIAS =
            ENTITIES.registerEntityType("belias", (type, level) -> new IvaliceMonsterEntity(type, level, "belias"),
                    MobCategory.MONSTER, b -> b.sized(1.80F, 2.80F).eyeHeight(2.30F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> MATEUS =
            ENTITIES.registerEntityType("mateus", (type, level) -> new IvaliceMonsterEntity(type, level, "mateus"),
                    MobCategory.MONSTER, b -> b.sized(1.60F, 2.60F).eyeHeight(2.15F));

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(IvaliceEntities::registerAttributes);
        modEventBus.addListener(IvaliceEntities::registerSpawnPlacements);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(WOLF.get(), IvaliceMonsterEntity.createAttributes("wolf").build());
        event.put(CACTOID.get(), IvaliceMonsterEntity.createAttributes("cactoid").build());
        event.put(HYENA.get(), IvaliceMonsterEntity.createAttributes("hyena").build());
        event.put(GIZA_RABBIT.get(), IvaliceMonsterEntity.createAttributes("giza_rabbit").build());
        event.put(COCKATRICE.get(), IvaliceMonsterEntity.createAttributes("cockatrice").build());
        event.put(ICHTHON.get(), IvaliceMonsterEntity.createAttributes("ichthon").build());

        event.put(ROGUE_TOMATO.get(), IvaliceMonsterEntity.createAttributes("rogue_tomato").build());
        event.put(THEXTERA.get(), IvaliceMonsterEntity.createAttributes("thextera").build());
        event.put(FLOWERING_CACTOID.get(), IvaliceMonsterEntity.createAttributes("flowering_cactoid").build());
        event.put(WHITE_MOUSSE.get(), IvaliceMonsterEntity.createAttributes("white_mousse").build());

        event.put(BELIAS.get(), IvaliceMonsterEntity.createAttributes("belias").build());
        event.put(MATEUS.get(), IvaliceMonsterEntity.createAttributes("mateus").build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        registerGroundSpawn(event, WOLF.get());
        registerGroundSpawn(event, CACTOID.get());
        registerGroundSpawn(event, HYENA.get());
        registerGroundSpawn(event, GIZA_RABBIT.get());
        registerGroundSpawn(event, COCKATRICE.get());
        registerGroundSpawn(event, ICHTHON.get());
        registerGroundSpawn(event, ROGUE_TOMATO.get());
        registerGroundSpawn(event, THEXTERA.get());
        registerGroundSpawn(event, FLOWERING_CACTOID.get());
        registerGroundSpawn(event, WHITE_MOUSSE.get());
        registerGroundSpawn(event, BELIAS.get());
        registerGroundSpawn(event, MATEUS.get());
    }

    private static void registerGroundSpawn(RegisterSpawnPlacementsEvent event, EntityType<IvaliceMonsterEntity> entityType) {
        event.register(
                entityType,
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                IvaliceEntities::canIvaliceCreatureSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    private static boolean canIvaliceCreatureSpawn(
            EntityType<IvaliceMonsterEntity> type,
            ServerLevelAccessor level,
            EntitySpawnReason reason,
            BlockPos pos,
            RandomSource random
    ) {
        if (reason == EntitySpawnReason.NATURAL && type.getCategory() == MobCategory.MONSTER && !level.canSeeSky(pos)) {
            return false;
        }
        return !level.getBlockState(pos.below()).isAir();
    }

    public static EntityType<IvaliceMonsterEntity> getById(String id) {
        return switch (id.toLowerCase()) {
            case "cactoid" -> CACTOID.get();
            case "hyena" -> HYENA.get();
            case "giza_rabbit" -> GIZA_RABBIT.get();
            case "cockatrice" -> COCKATRICE.get();
            case "ichthon" -> ICHTHON.get();
            case "rogue_tomato" -> ROGUE_TOMATO.get();
            case "thextera" -> THEXTERA.get();
            case "flowering_cactoid" -> FLOWERING_CACTOID.get();
            case "white_mousse" -> WHITE_MOUSSE.get();
            case "belias" -> BELIAS.get();
            case "mateus" -> MATEUS.get();
            default -> WOLF.get();
        };
    }
}
