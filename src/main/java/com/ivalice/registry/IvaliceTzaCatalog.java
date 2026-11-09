package com.ivalice.registry;

import com.ivalice.data.IvalicePlayerData;
import com.ivalice.item.IvaliceItemCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Catálogo completo dos 1.246 registros canônicos de Final Fantasy XII: The Zodiac Age.
 * Aplica progressão linear estrita: lojas comuns vendem apenas itens básicos/intermediários e pergaminhos iniciais,
 * enquanto equipamentos raros, magias cataclísmicas e espólios exigem Drops de Monstros, Caçadas ou Alquimia no Bazaar.
 */
public final class IvaliceTzaCatalog {
    public record Entry(String sourceId, String id, IvaliceItemCategory category, String kind, int rank, String name) {
        public int price() {
            return switch (kind) {
                case "weapon", "head", "chest", "shield", "accessory" -> Math.min(28000, 120 + rank * rank * 18);
                case "spell", "technick" -> Math.min(12000, 180 + rank * rank * 6);
                case "key", "map", "gambit", "esper" -> 450 + rank * 40;
                default -> Math.min(2400, 35 + rank * 14);
            };
        }

        public boolean isSoldInRegionalShops() {
            if ("loot".equals(kind) || "esper".equals(kind) || "key".equals(kind)) {
                return false;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.contains("tournesol") || lower.contains("zodiac") || lower.contains("wyrmhero")
                    || lower.contains("masamune") || lower.contains("excalibur") || lower.contains("durandal")
                    || lower.contains("fomalhaut") || lower.contains("sagittarius") || lower.contains("scorpion")
                    || lower.contains("grand armor") || lower.contains("maximilian") || lower.contains("lordly")
                    || lower.contains("brave suit") || lower.contains("genji") || lower.contains("ribbon")
                    || lower.contains("firaga") || lower.contains("blizzaga") || lower.contains("thundaga")
                    || lower.contains("waterga") || lower.contains("aeroga") || lower.contains("darkga")
                    || lower.contains("curaga") || lower.contains("curaja") || lower.contains("renew")
                    || lower.contains("hastega") || lower.contains("ardor") || lower.contains("collapse")
                    || lower.contains("scathe") || lower.contains("holy") || lower.contains("flare")
                    || lower.contains("shock") || lower.contains("scourge") || lower.contains("elixir")
                    || lower.contains("megalixir")) {
                return false;
            }
            return switch (kind) {
                case "weapon", "head", "chest", "shield", "accessory" -> rank <= 12;
                case "spell", "technick" -> rank <= 28;
                case "consumable", "ammo", "map", "gambit" -> rank <= 18;
                default -> false;
            };
        }
    }

    private static final Map<String, Entry> ITEMS = new LinkedHashMap<>();
    private static final List<Entry> RECORDS = new ArrayList<>();

    private IvaliceTzaCatalog() {}

    static void register() {
        var stream = Objects.requireNonNull(IvaliceTzaCatalog.class.getResourceAsStream("/ivalice/tza_catalog.tsv"),
                "Missing Ivalice TZA catalog");
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                String[] fields = line.split("\t", 6);
                Entry entry = new Entry(fields[0], fields[1], IvaliceItemCategory.valueOf(fields[2]),
                        fields[3], Integer.parseInt(fields[4]), fields[5]);
                if (ITEMS.putIfAbsent(entry.id, entry) == null) IvaliceItems.registerArchive(entry);
                RECORDS.add(entry);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Ivalice TZA catalog", e);
        }
    }

    public static int search(ServerPlayer player, String query) {
        String term = query.toLowerCase(Locale.ROOT).strip();
        List<Entry> matches = ITEMS.values().stream()
                .filter(entry -> entry.id.contains(term) || entry.name.toLowerCase(Locale.ROOT).contains(term))
                .limit(14).toList();
        if (matches.isEmpty()) {
            player.sendSystemMessage(Component.literal("Nenhum item encontrado para “" + query + "”."));
            return 0;
        }
        for (Entry item : matches) {
            String availability = item.isSoldInRegionalShops()
                    ? item.price() + " Gil [Loja Regional]"
                    : "Exclusivo [Drop de Mob / Alquimia / Boss]";
            player.sendSystemMessage(Component.literal("• " + item.name + " • " + availability + " • ID: " + item.id)
                    .withStyle(item.isSoldInRegionalShops() ? ChatFormatting.YELLOW : ChatFormatting.LIGHT_PURPLE));
        }
        return matches.size();
    }

    public static int buy(ServerPlayer player, String id) {
        Entry entry = ITEMS.get(id);
        Item item = IvaliceItems.getItemById(id);
        if (entry == null || item == null) {
            player.sendSystemMessage(Component.literal("Item desconhecido. Use /ivalice catalog search <nome>."));
            return 0;
        }
        if (!entry.isSoldInRegionalShops()) {
            player.sendSystemMessage(Component.literal(String.format(
                    "⚠ [PROGRESSÃO DE IVALICE] '%s' é um item raro/avançado e NÃO é vendido em mercadores comuns! Obtenha-o derrotando Monstros/Alfas, concluindo Caçadas de Bosses ou criando com Alquimia no Bazaar!",
                    entry.name
            )).withStyle(ChatFormatting.RED));
            return 0;
        }
        var profile = IvalicePlayerData.get(player);
        if (profile.gil < entry.price()) {
            player.sendSystemMessage(Component.literal("Você precisa de " + entry.price() + " Gil (Atual: " + profile.gil + " Gil)."));
            return 0;
        }
        profile.gil -= entry.price();
        player.getInventory().placeItemBackInInventory(new ItemStack(item), Prediction.SERVER_ONLY);
        IvalicePlayerData.save(player);
        player.sendSystemMessage(Component.literal("Comprou " + entry.name + " por " + entry.price() + " Gil.")
                .withStyle(ChatFormatting.GREEN));
        return 1;
    }

    public static Entry getEntry(String id) {
        return ITEMS.get(id);
    }

    public static int count() {
        return RECORDS.size();
    }
}
