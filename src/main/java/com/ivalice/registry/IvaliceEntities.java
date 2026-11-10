package com.ivalice.registry;

import com.ivalice.IvaliceMod;
import com.ivalice.entity.IvaliceBestiary;
import com.ivalice.entity.IvaliceFlyingEntity;
import com.ivalice.entity.IvaliceGuardEntity;
import com.ivalice.entity.IvaliceIchthonEntity;
import com.ivalice.entity.IvaliceMonsterEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registro central de criaturas, Elite Marks e Espers de Final Fantasy XII (Etapa 3 e Etapa 5).
 */
public class IvaliceEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(IvaliceMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceGuardEntity>> DALMASCA_GUARD =
            ENTITIES.registerEntityType("dalmasca_guard", IvaliceGuardEntity::new, MobCategory.CREATURE,
                    b -> b.sized(0.62F, 1.82F).eyeHeight(1.64F));

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
            ENTITIES.registerEntityType("ichthon", IvaliceIchthonEntity::new,
                    MobCategory.WATER_CREATURE, b -> b.sized(0.75F, 0.75F).eyeHeight(0.55F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> WILD_SAURIAN =
            ENTITIES.registerEntityType("wild_saurian", (type, level) -> new IvaliceMonsterEntity(type, level, "wild_saurian"),
                    MobCategory.MONSTER, b -> b.sized(1.25F, 1.65F).eyeHeight(1.35F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> MANDRAGORA =
            ENTITIES.registerEntityType("mandragora", (type, level) -> new IvaliceMonsterEntity(type, level, "mandragora"),
                    MobCategory.CREATURE, b -> b.sized(0.55F, 0.85F).eyeHeight(0.66F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> MALBORO =
            ENTITIES.registerEntityType("malboro", (type, level) -> new IvaliceMonsterEntity(type, level, "malboro"),
                    MobCategory.MONSTER, b -> b.sized(1.10F, 1.15F).eyeHeight(0.90F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> YETI =
            ENTITIES.registerEntityType("yeti", (type, level) -> new IvaliceMonsterEntity(type, level, "yeti"),
                    MobCategory.MONSTER, b -> b.sized(0.95F, 1.55F).eyeHeight(1.28F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> BOMB =
            ENTITIES.registerEntityType("bomb", (type, level) -> new IvaliceMonsterEntity(type, level, "bomb"),
                    MobCategory.MONSTER, b -> b.sized(0.85F, 1.10F).eyeHeight(0.85F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> WHITE_WOLF =
            ENTITIES.registerEntityType("white_wolf", (type, level) -> new IvaliceMonsterEntity(type, level, "white_wolf"),
                    MobCategory.MONSTER, b -> b.sized(0.86F, 0.96F).eyeHeight(0.76F));

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

    // === 12 Novas Criaturas, Variantes Alfa, Bosses Imperiais e Superbosses ===
    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> CHOCOBO =
            ENTITIES.registerEntityType("chocobo", (type, level) -> new IvaliceMonsterEntity(type, level, "chocobo"),
                    MobCategory.CREATURE, b -> b.sized(1.10F, 1.85F).eyeHeight(1.55F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> ALPHA_WOLF =
            ENTITIES.registerEntityType("alpha_wolf", (type, level) -> new IvaliceMonsterEntity(type, level, "alpha_wolf"),
                    MobCategory.MONSTER, b -> b.sized(1.20F, 1.30F).eyeHeight(1.05F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> ALPHA_HYENA =
            ENTITIES.registerEntityType("alpha_hyena", (type, level) -> new IvaliceMonsterEntity(type, level, "alpha_hyena"),
                    MobCategory.MONSTER, b -> b.sized(1.25F, 1.40F).eyeHeight(1.15F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> ALPHA_SAURIAN =
            ENTITIES.registerEntityType("alpha_saurian", (type, level) -> new IvaliceMonsterEntity(type, level, "alpha_saurian"),
                    MobCategory.MONSTER, b -> b.sized(1.65F, 2.20F).eyeHeight(1.85F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> COEURL =
            ENTITIES.registerEntityType("coeurl", (type, level) -> new IvaliceMonsterEntity(type, level, "coeurl"),
                    MobCategory.MONSTER, b -> b.sized(1.05F, 1.15F).eyeHeight(0.95F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> BEHEMOTH =
            ENTITIES.registerEntityType("behemoth", (type, level) -> new IvaliceMonsterEntity(type, level, "behemoth"),
                    MobCategory.MONSTER, b -> b.sized(1.55F, 2.10F).eyeHeight(1.75F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> SKELETON_CRUSADER =
            ENTITIES.registerEntityType("skeleton_crusader", (type, level) -> new IvaliceMonsterEntity(type, level, "skeleton_crusader"),
                    MobCategory.MONSTER, b -> b.sized(0.85F, 1.95F).eyeHeight(1.70F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> FIREMANE =
            ENTITIES.registerEntityType("firemane", (type, level) -> new IvaliceMonsterEntity(type, level, "firemane"),
                    MobCategory.MONSTER, b -> b.sized(1.45F, 2.15F).eyeHeight(1.80F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> JUDGE_GABRANTH =
            ENTITIES.registerEntityType("judge_gabranth", (type, level) -> new IvaliceMonsterEntity(type, level, "judge_gabranth"),
                    MobCategory.MONSTER, b -> b.sized(1.10F, 2.35F).eyeHeight(2.00F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> GILGAMESH =
            ENTITIES.registerEntityType("gilgamesh", (type, level) -> new IvaliceMonsterEntity(type, level, "gilgamesh"),
                    MobCategory.MONSTER, b -> b.sized(1.35F, 2.45F).eyeHeight(2.10F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> ZODIARK =
            ENTITIES.registerEntityType("zodiark", (type, level) -> new IvaliceMonsterEntity(type, level, "zodiark"),
                    MobCategory.MONSTER, b -> b.sized(2.10F, 3.10F).eyeHeight(2.55F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> YIAZMAT =
            ENTITIES.registerEntityType("yiazmat", (type, level) -> new IvaliceMonsterEntity(type, level, "yiazmat"),
                    MobCategory.MONSTER, b -> b.sized(2.50F, 3.40F).eyeHeight(2.85F).fireImmune());

    // === 6 Criaturas do Nether (Final Fantasy IV & Final Fantasy XV) ===
    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> MINDFLAYER =
            ENTITIES.registerEntityType("mindflayer", (type, level) -> new IvaliceMonsterEntity(type, level, "mindflayer"),
                    MobCategory.MONSTER, b -> b.sized(0.95F, 2.05F).eyeHeight(1.75F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> RUBICANTE =
            ENTITIES.registerEntityType("rubicante", (type, level) -> new IvaliceMonsterEntity(type, level, "rubicante"),
                    MobCategory.MONSTER, b -> b.sized(1.30F, 2.55F).eyeHeight(2.15F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> IRON_GIANT =
            ENTITIES.registerEntityType("iron_giant", (type, level) -> new IvaliceMonsterEntity(type, level, "iron_giant"),
                    MobCategory.MONSTER, b -> b.sized(1.65F, 2.85F).eyeHeight(2.40F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> GOLBEZ =
            ENTITIES.registerEntityType("golbez", (type, level) -> new IvaliceMonsterEntity(type, level, "golbez"),
                    MobCategory.MONSTER, b -> b.sized(1.40F, 2.65F).eyeHeight(2.25F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> IFRIT_PYREBURNER =
            ENTITIES.registerEntityType("ifrit_pyreburner", (type, level) -> new IvaliceMonsterEntity(type, level, "ifrit_pyreburner"),
                    MobCategory.MONSTER, b -> b.sized(1.90F, 3.00F).eyeHeight(2.50F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> ADAMANTOISE =
            ENTITIES.registerEntityType("adamantoise", (type, level) -> new IvaliceMonsterEntity(type, level, "adamantoise"),
                    MobCategory.MONSTER, b -> b.sized(2.60F, 2.40F).eyeHeight(1.80F).fireImmune());

    // === 6 Criaturas da Dimensão do Fim / The End (Final Fantasy V & Final Fantasy VI) ===
    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> ULTROS =
            ENTITIES.registerEntityType("ultros", (type, level) -> new IvaliceMonsterEntity(type, level, "ultros"),
                    MobCategory.MONSTER, b -> b.sized(1.50F, 1.75F).eyeHeight(1.30F));

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> GIL_TURTLE =
            ENTITIES.registerEntityType("gil_turtle", (type, level) -> new IvaliceMonsterEntity(type, level, "gil_turtle"),
                    MobCategory.MONSTER, b -> b.sized(1.65F, 1.60F).eyeHeight(1.20F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> ATB_WEAPON =
            ENTITIES.registerEntityType("atb_weapon", (type, level) -> new IvaliceMonsterEntity(type, level, "atb_weapon"),
                    MobCategory.MONSTER, b -> b.sized(2.00F, 3.05F).eyeHeight(2.50F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> SHINRYU =
            ENTITIES.registerEntityType("shinryu", (type, level) -> new IvaliceMonsterEntity(type, level, "shinryu"),
                    MobCategory.MONSTER, b -> b.sized(2.20F, 3.10F).eyeHeight(2.60F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> OMEGA_MK =
            ENTITIES.registerEntityType("omega_mk", (type, level) -> new IvaliceMonsterEntity(type, level, "omega_mk"),
                    MobCategory.MONSTER, b -> b.sized(2.15F, 2.25F).eyeHeight(1.75F).fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>> KEFKA_GOD =
            ENTITIES.registerEntityType("kefka_god", (type, level) -> new IvaliceMonsterEntity(type, level, "kefka_god"),
                    MobCategory.MONSTER, b -> b.sized(2.25F, 3.45F).eyeHeight(2.90F).fireImmune());

    private static final Map<String, DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>>> BESTIARY = registerBestiary();

    private static Map<String, DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>>> registerBestiary() {
        var entries = new LinkedHashMap<String, DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>>>();
        IvaliceBestiary.entries().values().stream().filter(entry -> !entry.original()).forEach(entry -> {
            MobCategory category = entry.aquatic() ? MobCategory.WATER_CREATURE
                    : entry.neutral() ? MobCategory.CREATURE : MobCategory.MONSTER;
            var entity = ENTITIES.<IvaliceMonsterEntity>registerEntityType(entry.id(),
                    (type, level) -> {
                        if (entry.aquatic()) return new IvaliceIchthonEntity(type, level, entry.id());
                        if (entry.flying()) return new IvaliceFlyingEntity(type, level, entry.id());
                        return new IvaliceMonsterEntity(type, level, entry.id());
                    },
                    category, builder -> {
                        builder.sized(entry.width(), entry.height()).eyeHeight(entry.height() * 0.8F);
                        return entry.id().startsWith("ruin_") ? builder.fireImmune() : builder;
                    });
            entries.put(entry.id(), entity);
        });
        return Collections.unmodifiableMap(entries);
    }

    public static Map<String, DeferredHolder<EntityType<?>, EntityType<IvaliceMonsterEntity>>> bestiaryTypes() {
        return BESTIARY;
    }

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(IvaliceEntities::registerAttributes);
        modEventBus.addListener(IvaliceEntities::registerSpawnPlacements);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(DALMASCA_GUARD.get(), IvaliceGuardEntity.createAttributes().build());
        event.put(WOLF.get(), IvaliceMonsterEntity.createAttributes("wolf").build());
        event.put(CACTOID.get(), IvaliceMonsterEntity.createAttributes("cactoid").build());
        event.put(HYENA.get(), IvaliceMonsterEntity.createAttributes("hyena").build());
        event.put(GIZA_RABBIT.get(), IvaliceMonsterEntity.createAttributes("giza_rabbit").build());
        event.put(COCKATRICE.get(), IvaliceMonsterEntity.createAttributes("cockatrice").build());
        event.put(ICHTHON.get(), IvaliceMonsterEntity.createAttributes("ichthon").build());
        event.put(WILD_SAURIAN.get(), IvaliceMonsterEntity.createAttributes("wild_saurian").build());
        event.put(MANDRAGORA.get(), IvaliceMonsterEntity.createAttributes("mandragora").build());
        event.put(MALBORO.get(), IvaliceMonsterEntity.createAttributes("malboro").build());
        event.put(YETI.get(), IvaliceMonsterEntity.createAttributes("yeti").build());
        event.put(BOMB.get(), IvaliceMonsterEntity.createAttributes("bomb").build());
        event.put(WHITE_WOLF.get(), IvaliceMonsterEntity.createAttributes("white_wolf").build());

        event.put(ROGUE_TOMATO.get(), IvaliceMonsterEntity.createAttributes("rogue_tomato").build());
        event.put(THEXTERA.get(), IvaliceMonsterEntity.createAttributes("thextera").build());
        event.put(FLOWERING_CACTOID.get(), IvaliceMonsterEntity.createAttributes("flowering_cactoid").build());
        event.put(WHITE_MOUSSE.get(), IvaliceMonsterEntity.createAttributes("white_mousse").build());

        event.put(BELIAS.get(), IvaliceMonsterEntity.createAttributes("belias").build());
        event.put(MATEUS.get(), IvaliceMonsterEntity.createAttributes("mateus").build());

        event.put(CHOCOBO.get(), IvaliceMonsterEntity.createAttributes("chocobo").build());
        event.put(ALPHA_WOLF.get(), IvaliceMonsterEntity.createAttributes("alpha_wolf").build());
        event.put(ALPHA_HYENA.get(), IvaliceMonsterEntity.createAttributes("alpha_hyena").build());
        event.put(ALPHA_SAURIAN.get(), IvaliceMonsterEntity.createAttributes("alpha_saurian").build());
        event.put(COEURL.get(), IvaliceMonsterEntity.createAttributes("coeurl").build());
        event.put(BEHEMOTH.get(), IvaliceMonsterEntity.createAttributes("behemoth").build());
        event.put(SKELETON_CRUSADER.get(), IvaliceMonsterEntity.createAttributes("skeleton_crusader").build());
        event.put(FIREMANE.get(), IvaliceMonsterEntity.createAttributes("firemane").build());
        event.put(JUDGE_GABRANTH.get(), IvaliceMonsterEntity.createAttributes("judge_gabranth").build());
        event.put(GILGAMESH.get(), IvaliceMonsterEntity.createAttributes("gilgamesh").build());
        event.put(ZODIARK.get(), IvaliceMonsterEntity.createAttributes("zodiark").build());
        event.put(YIAZMAT.get(), IvaliceMonsterEntity.createAttributes("yiazmat").build());

        event.put(MINDFLAYER.get(), IvaliceMonsterEntity.createAttributes("mindflayer").build());
        event.put(RUBICANTE.get(), IvaliceMonsterEntity.createAttributes("rubicante").build());
        event.put(IRON_GIANT.get(), IvaliceMonsterEntity.createAttributes("iron_giant").build());
        event.put(GOLBEZ.get(), IvaliceMonsterEntity.createAttributes("golbez").build());
        event.put(IFRIT_PYREBURNER.get(), IvaliceMonsterEntity.createAttributes("ifrit_pyreburner").build());
        event.put(ADAMANTOISE.get(), IvaliceMonsterEntity.createAttributes("adamantoise").build());

        event.put(ULTROS.get(), IvaliceMonsterEntity.createAttributes("ultros").build());
        event.put(GIL_TURTLE.get(), IvaliceMonsterEntity.createAttributes("gil_turtle").build());
        event.put(ATB_WEAPON.get(), IvaliceMonsterEntity.createAttributes("atb_weapon").build());
        event.put(SHINRYU.get(), IvaliceMonsterEntity.createAttributes("shinryu").build());
        event.put(OMEGA_MK.get(), IvaliceMonsterEntity.createAttributes("omega_mk").build());
        event.put(KEFKA_GOD.get(), IvaliceMonsterEntity.createAttributes("kefka_god").build());
        BESTIARY.forEach((id, holder) -> event.put(holder.get(), IvaliceMonsterEntity.createAttributes(id).build()));
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        registerGroundSpawn(event, WOLF.get());
        registerGroundSpawn(event, CACTOID.get());
        registerGroundSpawn(event, HYENA.get());
        registerGroundSpawn(event, GIZA_RABBIT.get());
        registerGroundSpawn(event, COCKATRICE.get());
        event.register(ICHTHON.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, reason, pos, random) -> level.getFluidState(pos.below()).is(FluidTags.WATER),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        registerGroundSpawn(event, WILD_SAURIAN.get());
        registerGroundSpawn(event, MANDRAGORA.get());
        registerGroundSpawn(event, MALBORO.get());
        registerGroundSpawn(event, YETI.get());
        registerGroundSpawn(event, BOMB.get());
        registerGroundSpawn(event, WHITE_WOLF.get());
        registerGroundSpawn(event, ROGUE_TOMATO.get());
        registerGroundSpawn(event, THEXTERA.get());
        registerGroundSpawn(event, FLOWERING_CACTOID.get());
        registerGroundSpawn(event, WHITE_MOUSSE.get());
        registerGroundSpawn(event, BELIAS.get());
        registerGroundSpawn(event, MATEUS.get());
        registerGroundSpawn(event, CHOCOBO.get());
        registerGroundSpawn(event, ALPHA_WOLF.get());
        registerGroundSpawn(event, ALPHA_HYENA.get());
        registerGroundSpawn(event, ALPHA_SAURIAN.get());
        registerGroundSpawn(event, COEURL.get());
        registerGroundSpawn(event, BEHEMOTH.get());
        registerGroundSpawn(event, SKELETON_CRUSADER.get());
        registerGroundSpawn(event, FIREMANE.get());
        registerGroundSpawn(event, JUDGE_GABRANTH.get());
        registerGroundSpawn(event, GILGAMESH.get());
        registerGroundSpawn(event, ZODIARK.get());
        registerGroundSpawn(event, YIAZMAT.get());

        registerGroundSpawn(event, MINDFLAYER.get());
        registerGroundSpawn(event, RUBICANTE.get());
        registerGroundSpawn(event, IRON_GIANT.get());
        registerGroundSpawn(event, GOLBEZ.get());
        registerGroundSpawn(event, IFRIT_PYREBURNER.get());
        registerGroundSpawn(event, ADAMANTOISE.get());

        registerGroundSpawn(event, ULTROS.get());
        registerGroundSpawn(event, GIL_TURTLE.get());
        registerGroundSpawn(event, ATB_WEAPON.get());
        registerGroundSpawn(event, SHINRYU.get());
        registerGroundSpawn(event, OMEGA_MK.get());
        registerGroundSpawn(event, KEFKA_GOD.get());
        BESTIARY.forEach((id, holder) -> {
            if (IvaliceBestiary.find(id).aquatic()) {
                event.register(holder.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        (type, level, reason, pos, random) -> level.getFluidState(pos).is(FluidTags.WATER)
                                && level.getFluidState(pos.below()).is(FluidTags.WATER),
                        RegisterSpawnPlacementsEvent.Operation.REPLACE);
            } else {
                registerGroundSpawn(event, holder.get());
            }
        });
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
        if (reason == EntitySpawnReason.NATURAL && type.getCategory() == MobCategory.MONSTER
                && level instanceof ServerLevel sl && sl.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
                && pos.getY() < level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ())) {
            return false;
        }
        if (reason == EntitySpawnReason.NATURAL && level instanceof ServerLevel serverLevel
                && type.getCategory() == MobCategory.MONSTER) {
            ChunkPos chunk = ChunkPos.containing(pos);
            AABB bounds = new AABB(chunk.getMinBlockX(), level.getMinY(), chunk.getMinBlockZ(),
                    chunk.getMaxBlockX() + 1, level.getMaxY() + 1, chunk.getMaxBlockZ() + 1);
            int hostiles = 0;
            int hyenas = 0;
            for (IvaliceMonsterEntity mob : serverLevel.getEntitiesOfClass(IvaliceMonsterEntity.class, bounds,
                    entity -> entity.isAlive() && entity.getType().getCategory() == MobCategory.MONSTER
                            && entity.chunkPosition().equals(chunk))) {
                hostiles++;
                if (mob.getType() == HYENA.get()) {
                    hyenas++;
                }
            }
            if (hostiles >= 7 || (type == HYENA.get() && hyenas >= 3)) {
                return false;
            }
        }
        return !level.getBlockState(pos.below()).isAir();
    }

    public static EntityType<IvaliceMonsterEntity> getById(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        var entry = BESTIARY.get(normalized);
        if (entry != null) return entry.get();
        return switch (normalized) {
            case "cactoid" -> CACTOID.get();
            case "hyena" -> HYENA.get();
            case "giza_rabbit" -> GIZA_RABBIT.get();
            case "cockatrice" -> COCKATRICE.get();
            case "ichthon" -> ICHTHON.get();
            case "wild_saurian" -> WILD_SAURIAN.get();
            case "mandragora" -> MANDRAGORA.get();
            case "malboro" -> MALBORO.get();
            case "yeti" -> YETI.get();
            case "bomb" -> BOMB.get();
            case "white_wolf" -> WHITE_WOLF.get();
            case "rogue_tomato" -> ROGUE_TOMATO.get();
            case "thextera" -> THEXTERA.get();
            case "flowering_cactoid" -> FLOWERING_CACTOID.get();
            case "white_mousse" -> WHITE_MOUSSE.get();
            case "belias" -> BELIAS.get();
            case "mateus" -> MATEUS.get();
            case "chocobo" -> CHOCOBO.get();
            case "alpha_wolf" -> ALPHA_WOLF.get();
            case "alpha_hyena" -> ALPHA_HYENA.get();
            case "alpha_saurian" -> ALPHA_SAURIAN.get();
            case "coeurl" -> COEURL.get();
            case "behemoth" -> BEHEMOTH.get();
            case "skeleton_crusader" -> SKELETON_CRUSADER.get();
            case "firemane" -> FIREMANE.get();
            case "judge_gabranth" -> JUDGE_GABRANTH.get();
            case "gilgamesh" -> GILGAMESH.get();
            case "zodiark" -> ZODIARK.get();
            case "yiazmat" -> YIAZMAT.get();
            case "mindflayer" -> MINDFLAYER.get();
            case "rubicante" -> RUBICANTE.get();
            case "iron_giant" -> IRON_GIANT.get();
            case "golbez" -> GOLBEZ.get();
            case "ifrit_pyreburner" -> IFRIT_PYREBURNER.get();
            case "adamantoise" -> ADAMANTOISE.get();
            case "ultros" -> ULTROS.get();
            case "gil_turtle" -> GIL_TURTLE.get();
            case "atb_weapon" -> ATB_WEAPON.get();
            case "shinryu" -> SHINRYU.get();
            case "omega_mk" -> OMEGA_MK.get();
            case "kefka_god" -> KEFKA_GOD.get();
            default -> WOLF.get();
        };
    }
}
