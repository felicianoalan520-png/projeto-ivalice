package com.ivalice.item;

import com.ivalice.combat.BattleManager;
import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceBestiary;
import com.ivalice.registry.IvaliceEntities;
import com.ivalice.registry.IvaliceTzaCatalog;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;

public final class IvaliceReferenceItem extends IvaliceCatalogItem {
    private static final Map<String, String> REGIONS = Map.ofEntries(
            Map.entry("nalbina", "dalmasca_estersand"), Map.entry("jahara", "ozmone_plain"),
            Map.entry("eruyt", "golmore_jungle"), Map.entry("bur_omisace", "paramina_rift"),
            Map.entry("giza_plains", "giza_plains_dry"), Map.entry("nam_yensa", "ogir_yensa_sandsea"),
            Map.entry("ridorana", "phon_coast"), Map.entry("garamsythe", "giza_plains_rains"),
            Map.entry("barheim", "dalmasca_estersand"), Map.entry("zertinan", "dalmasca_westersand"),
            Map.entry("lhusu", "mosphoran_highwaste"), Map.entry("raithwall", "ogir_yensa_sandsea"),
            Map.entry("necrohol", "nabreus_deadlands"), Map.entry("henne", "ozmone_plain"),
            Map.entry("giruvegan", "feywood"), Map.entry("stilshrine", "paramina_rift"),
            Map.entry("sochen", "tchita_uplands"), Map.entry("draklor", "tchita_uplands"),
            Map.entry("leviathan", "ogir_yensa_sandsea"), Map.entry("bahamut", "dalmasca_westersand"),
            Map.entry("pharos", "phon_coast"), Map.entry("the_port_at_balfonheim", "phon_coast"));
    private static final Map<String, String> CITIES = Map.of(
            "rabanastre", "rabanastre", "bhujerba", "bhujerba", "archades", "archades", "balfonheim", "balfonheim_port");

    private final IvaliceTzaCatalog.Entry entry;

    public IvaliceReferenceItem(Properties properties, IvaliceTzaCatalog.Entry entry, Rarity rarity, String source) {
        super(properties, entry.category(), describe(entry.kind()), entry.price(), source, entry.name(), rarity);
        this.entry = entry;
    }

    private static String describe(String kind) {
        return switch (kind) {
            case "map" -> "Use para consultar a região correspondente nesta semente.";
            case "gambit" -> "Use para aprender a condição. Depois configure /ivalice gambit set.";
            case "esper" -> "Convoque o Esper em campo depois de vencê-lo.";
            default -> "Use para consultar o registro do item-chave.";
        };
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        switch (entry.kind()) {
            case "gambit" -> {
                var profile = IvalicePlayerData.get(serverPlayer);
                if (profile.learnedGambits.add(entry.id())) IvalicePlayerData.save(serverPlayer);
                player.sendSystemMessage(Component.literal(entry.name() + " • /ivalice gambit set " + entry.id() + " <ação>"));
            }
            case "map" -> {
                locate(serverPlayer);
                player.getCooldowns().addCooldown(player.getItemInHand(hand), 80);
            }
            case "esper" -> {
                String esper = entry.id().replaceFirst("^tza_", "").replaceFirst("_s_weapon$", "");
                if (!summon(serverPlayer, esper)) return InteractionResult.FAIL;
                player.getCooldowns().addCooldown(player.getItemInHand(hand), 600);
            }
            default -> player.sendSystemMessage(Component.literal(entry.name() + " • "
                    + (entry.id().endsWith("_monograph") || entry.id().equals("tza_canopic_jar")
                    ? "Leve no inventário para descobrir espólios especiais." : "Registro de Ivalice " + entry.sourceId())));
        }
        return InteractionResult.SUCCESS;
    }

    private void locate(ServerPlayer player) {
        if (!player.level().dimension().equals(Level.OVERWORLD)) {
            player.sendSystemMessage(Component.literal("Consulte este mapa no Overworld."));
            return;
        }
        String id = entry.id();
        for (var city : CITIES.entrySet()) {
            if (id.contains(city.getKey())) {
                player.sendSystemMessage(Component.literal(entry.name() + " • Cidade ivalice:" + city.getValue()
                        + " • /locate structure ivalice:" + city.getValue()));
                return;
            }
        }
        String region = IvaliceBestiary.regionIds().stream().filter(id::contains).findFirst().orElseGet(() ->
                REGIONS.entrySet().stream().filter(mapping -> id.contains(mapping.getKey())).map(Map.Entry::getValue)
                        .findFirst().orElse(null));
        if (region == null) {
            player.sendSystemMessage(Component.literal(entry.name() + " • Área registrada no atlas de Ivalice."));
            return;
        }
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("ivalice", region));
        var found = player.level().findClosestBiome3d(biome -> biome.is(key), player.blockPosition(), 6400, 32, 64);
        if (found == null) {
            player.sendSystemMessage(Component.literal(entry.name() + " • Região " + region + " fora do alcance do mapa."));
        } else {
            var pos = found.getFirst();
            player.sendSystemMessage(Component.literal(entry.name() + " • " + region + " • X " + pos.getX() + " Z " + pos.getZ()));
        }
    }

    public static boolean summon(ServerPlayer player, String id) {
        if (BattleManager.isInBattle(player)) {
            player.sendOverlayMessage(Component.literal("Convoque o Esper antes de iniciar a batalha."));
            return false;
        }
        if (IvalicePlayerData.get(player).getKills(id) == 0 && !player.getAbilities().instabuild) {
            player.sendOverlayMessage(Component.literal("Derrote este Esper antes de convocá-lo."));
            return false;
        }
        var level = player.level();
        var type = IvaliceEntities.getById(id);
        var monster = type.create(level, EntitySpawnReason.EVENT);
        if (monster == null) return false;
        var spawn = player.blockPosition().relative(player.getDirection(), 2);
        monster.setPos(spawn.getX() + .5, spawn.getY(), spawn.getZ() + .5);
        if (!level.noCollision(monster)) return false;
        if (level.getEntitiesOfClass(com.ivalice.entity.IvaliceMonsterEntity.class, player.getBoundingBox().inflate(48),
                mob -> mob.isSummonedBy(player)).size() > 0) {
            player.sendOverlayMessage(Component.literal("Você já tem um Esper em campo."));
            return false;
        }
        monster.bindSummoner(player);
        if (!level.addFreshEntity(monster)) return false;
        player.sendOverlayMessage(Component.literal("Invocou " + monster.getStats().displayName() + "."));
        return true;
    }
}
