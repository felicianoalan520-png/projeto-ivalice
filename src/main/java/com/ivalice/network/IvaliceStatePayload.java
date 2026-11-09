package com.ivalice.network;

import com.ivalice.IvaliceMod;
import com.ivalice.data.IvalicePlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record IvaliceStatePayload(CompoundTag state) implements CustomPacketPayload {
    public static final Type<IvaliceStatePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(IvaliceMod.MODID, "state"));

    @Override
    public Type<IvaliceStatePayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("2").playToClient(TYPE, ByteBufCodecs.COMPOUND_TAG.map(IvaliceStatePayload::new, IvaliceStatePayload::state));
    }

    public static void sendProfile(ServerPlayer player) {
        var profile = IvalicePlayerData.get(player);
        CompoundTag state = new CompoundTag();
        state.putString("kind", "profile");
        state.putInt("gil", profile.gil);
        state.putInt("lp", profile.lp);
        state.putInt("level", profile.getBattleLevel());
        state.putString("rank", profile.getClanRank());
        state.putString("hunt", profile.activeHuntId);
        state.putBoolean("gambits", !profile.activeGambits.isEmpty());
        state.put("encountered", strings(profile.encounteredMonsters));
        state.put("hunts", strings(profile.completedHunts));
        state.put("licenses", strings(profile.unlockedLicenses));
        state.put("jobs", strings(profile.jobs));
        CompoundTag kills = new CompoundTag();
        profile.bestiaryKills.forEach(kills::putInt);
        state.put("kills", kills);
        send(player, state);
    }

    private static ListTag strings(Iterable<String> entries) {
        ListTag values = new ListTag();
        for (String entry : entries) {
            values.add(StringTag.valueOf(entry));
        }
        return values;
    }

    public static void send(ServerPlayer player, CompoundTag state) {
        PacketDistributor.sendToPlayer(player, new IvaliceStatePayload(state));
    }
}
