package com.ivalice.registry;

import com.ivalice.IvaliceMod;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registro central de blocos naturais e vegetação das regiões de Ivalice (Final Fantasy XII).
 */
public class IvaliceBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IvaliceMod.MODID);
    private static final List<DeferredItem<BlockItem>> BLOCK_ITEMS = new ArrayList<>();

    // --- 1. Dalmasca Estersand & Westersand ---
    public static final DeferredBlock<Block> DALMASCA_SAND = registerBlock("dalmasca_sand",
            props -> new SandBlock(new ColorRGBA(0xE3C178), props),
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .instrument(NoteBlockInstrument.SNARE)
                    .strength(0.5F)
                    .sound(SoundType.SAND));

    public static final DeferredBlock<Block> DALMASCA_SANDSTONE = registerBlock("dalmasca_sandstone",
            Block::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SAND)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(0.8F)
                    .sound(SoundType.STONE));

    // --- 2. Ogir-Yensa Sandsea ---
    public static final DeferredBlock<Block> YENSA_FINE_SAND = registerBlock("yensa_fine_sand",
            props -> new SandBlock(new ColorRGBA(0xD89B52), props),
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .instrument(NoteBlockInstrument.SNARE)
                    .strength(0.5F)
                    .sound(SoundType.SAND));

    // --- 3. Giza Plains (Sunstone Deposit) ---
    public static final DeferredBlock<Block> SUNSTONE_ORE = registerBlock("sunstone_ore",
            Block::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.GOLD)
                    .instrument(NoteBlockInstrument.PLING)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F, 4.0F)
                    .lightLevel(state -> 9)
                    .sound(SoundType.AMETHYST));

    // --- 4. Golmore Jungle ---
    public static final DeferredBlock<Block> GOLMORE_DARKWOOD_LOG = registerBlock("golmore_darkwood_log",
            RotatedPillarBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F)
                    .sound(SoundType.WOOD));

    public static final DeferredBlock<Block> GOLMORE_MOSS_STONE = registerBlock("golmore_moss_stone",
            Block::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GREEN)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.STONE));

    // --- 5. Feywood ---
    public static final DeferredBlock<Block> FEYWOOD_MIST_SOIL = registerBlock("feywood_mist_soil",
            Block::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_WHITE)
                    .strength(0.6F)
                    .sound(SoundType.ROOTED_DIRT));

    // --- 6. Paramina Rift ---
    public static final DeferredBlock<Block> PARAMINA_PERMAFROST = registerBlock("paramina_permafrost",
            Block::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .instrument(NoteBlockInstrument.CHIME)
                    .requiresCorrectToolForDrops()
                    .friction(0.95F)
                    .strength(1.2F, 3.0F)
                    .sound(SoundType.GLASS));

    // --- 7. Mosphoran Highwaste ---
    public static final DeferredBlock<Block> MOSPHORAN_SLATE = registerBlock("mosphoran_slate",
            Block::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.DEEPSLATE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(1.8F, 6.0F)
                    .sound(SoundType.DEEPSLATE));

    // --- 8. Nabreus Deadlands ---
    public static final DeferredBlock<Block> NABREUS_MIRE = registerBlock("nabreus_mire",
            Block::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_GRAY)
                    .speedFactor(0.85F)
                    .strength(0.6F)
                    .sound(SoundType.MUD));

    // --- 9. Vegetação e Flora Canônica de Ivalice ---
    public static final DeferredBlock<Block> ESTERSAND_SCRUB = registerBlock("estersand_scrub",
            BushBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .replaceable()
                    .noCollision()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .ignitedByLava()
                    .pushReaction(PushReaction.POPPED));

    public static final DeferredBlock<Block> GYSahl_GREENS_WILD = registerBlock("gysahl_greens_wild",
            TallGrassBlock::new,
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .replaceable()
                    .noCollision()
                    .instabreak()
                    .sound(SoundType.GRASS)
                    .ignitedByLava()
                    .pushReaction(PushReaction.POPPED));

    public static final DeferredBlock<Block> FEYWOOD_WHITE_LILY = registerBlock("feywood_white_lily",
            props -> new FlowerBlock(MobEffects.REGENERATION, 6.0F, props),
            () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .noCollision()
                    .instabreak()
                    .lightLevel(state -> 5)
                    .sound(SoundType.GRASS)
                    .pushReaction(PushReaction.POPPED));

    public static final DeferredBlock<Block> DALMASCA_CARVED_BRICKS = masonry("dalmasca_carved_bricks", MapColor.SAND);
    public static final DeferredBlock<Block> GIZA_PAINTED_ADOBE = masonry("giza_painted_adobe", MapColor.TERRACOTTA_ORANGE);

    public static final DeferredBlock<Block> DALMASCA_EMBERBLOOM = regionalPlant("dalmasca_emberbloom");
    public static final DeferredBlock<Block> GIZA_SUNBLOOM = regionalPlant("giza_sunbloom");
    public static final DeferredBlock<Block> GOLMORE_ORCHID = regionalPlant("golmore_orchid");
    public static final DeferredBlock<Block> PHON_SEA_LILY = regionalPlant("phon_sea_lily");

    private static DeferredBlock<Block> masonry(String id, MapColor color) {
        return registerBlock(id, Block::new, () -> BlockBehaviour.Properties.of().mapColor(color)
                .strength(1.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE));
    }

    private static DeferredBlock<Block> regionalPlant(String id) {
        return registerBlock(id, properties -> new BushBlock(properties, 12) {
            @Override
            protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
                return super.mayPlaceOn(state, level, pos) || state.is(BlockTags.SAND)
                        || state.is(DALMASCA_SAND.get()) || state.is(YENSA_FINE_SAND.get());
            }
        }, () -> BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision()
                .instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.POPPED));
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(
            String name,
            Function<BlockBehaviour.Properties, T> factory,
            Supplier<BlockBehaviour.Properties> propertiesSupplier) {
        DeferredBlock<T> block = BLOCKS.registerBlock(name, factory, propertiesSupplier);
        DeferredItem<BlockItem> item = IvaliceItems.ITEMS.registerSimpleBlockItem(block);
        BLOCK_ITEMS.add(item);
        return block;
    }

    public static List<DeferredItem<BlockItem>> getRegisteredBlockItems() {
        return Collections.unmodifiableList(BLOCK_ITEMS);
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
