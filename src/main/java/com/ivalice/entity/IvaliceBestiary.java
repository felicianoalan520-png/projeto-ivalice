package com.ivalice.entity;

import com.google.gson.Gson;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class IvaliceBestiary {
    public record Roll(float chance, List<String> items) {
        public String pick(RandomSource random) {
            return items.get(random.nextInt(items.size()));
        }
    }

    public record Entry(String id, String name, String family, String kind, String model, boolean original,
                        String habitat, List<String> regions, String mode, boolean aquatic, boolean neutral,
                        float width, float height, int level, double hp, double attack, double speed, int gil,
                        List<List<Roll>> drops, boolean vanillaLoot, List<List<Roll>> steal, List<Roll> poach,
                        Map<String, Float> affinities, String move) {
        public ResourceKey<Biome> regionKey(String region) {
            return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("ivalice", region));
        }

        public String displayHabitat() {
            return habitat.isBlank() ? "Encontro regional do Clã" : habitat;
        }

        public boolean flying() {
            return switch (model) {
                case "bat", "bird", "wyvern", "elemental", "ghost", "ruin_deathgaze", "ruin_ray" -> true;
                default -> false;
            };
        }

        public boolean isContract() {
            return mode.equals("contract");
        }

        public boolean isHunt() {
            return kind.contains("Mark") || kind.equals("Esper") || kind.equals("Boss") || kind.equals("Rare Game");
        }

        public int minimumLevel() {
            return Math.max(1, level - 12);
        }

        public IvaliceMonsterStats stats() {
            String weakness = affinities.entrySet().stream().filter(e -> e.getValue() > 1)
                    .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("—");
            String resistance = affinities.entrySet().stream().filter(e -> e.getValue() < 1)
                    .min(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("—");
            return new IvaliceMonsterStats(id, name, family, kind, level, hp, attack, speed, weakness, resistance,
                    neutral ? "Neutral" : "Hostile", displayHabitat(), isContract() ? "Contrato" : "Natural",
                    Math.max(12, (int) (5 * Math.pow(level, 1.45))), Math.max(1, level / 8),
                    gil, first(drops), "", first(steal), "Família: " + family + ".", "Habitat: " + displayHabitat() + ".");
        }
    }

    private static final Map<String, Entry> BY_ID = read();

    private IvaliceBestiary() {}

    private static Map<String, Entry> read() {
        var entries = new LinkedHashMap<String, Entry>();
        readEntries(entries, "/ivalice/tza_bestiary.json");
        readEntries(entries, "/ivalice/ruin_bestiary.json");
        return Collections.unmodifiableMap(entries);
    }

    private static void readEntries(Map<String, Entry> entries, String resource) {
        var stream = Objects.requireNonNull(IvaliceBestiary.class.getResourceAsStream(resource));
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            for (Entry entry : new Gson().fromJson(reader, Entry[].class)) {
                if (entries.putIfAbsent(entry.id, entry) != null) {
                    throw new IllegalStateException("Duplicate Ivalice bestiary ID " + entry.id);
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot load " + resource, e);
        }
    }

    public static Map<String, Entry> entries() {
        return BY_ID;
    }

    public static Entry find(String id) {
        return BY_ID.get(id);
    }

    public static float multiplier(String id, String element, float fallback) {
        Entry entry = find(id);
        if (entry == null) return fallback;
        return entry.affinities.getOrDefault(element.toLowerCase(Locale.ROOT), 1F);
    }

    private static String first(List<List<Roll>> pools) {
        return pools.isEmpty() || pools.getFirst().isEmpty() ? "" : pools.getFirst().getFirst().items.getFirst();
    }

    public static String steal(Entry entry, boolean cuffs, RandomSource random) {
        if (entry.steal.isEmpty()) return null;
        List<Roll> pool = entry.steal.get(cuffs && entry.steal.size() > 1 ? 1 : 0);
        for (Roll roll : pool.reversed()) {
            if (random.nextFloat() < roll.chance) return roll.pick(random);
        }
        return null;
    }

    public static String poach(Entry entry, RandomSource random) {
        float selection = random.nextFloat();
        for (Roll roll : entry.poach) {
            selection -= roll.chance;
            if (selection < 0) return roll.pick(random);
        }
        return null;
    }

    private static final List<String> REGION_IDS = BY_ID.values().stream().flatMap(entry -> entry.regions.stream())
            .distinct().sorted().toList();

    public static List<String> regionIds() {
        return REGION_IDS;
    }
}
