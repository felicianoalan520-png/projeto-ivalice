package com.ivalice.client;

import com.ivalice.client.gui.BattleScreen;
import com.ivalice.client.gui.IvaliceHubScreen;
import com.ivalice.network.IvaliceStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientScreenHelper {
    private static CompoundTag profile = new CompoundTag();

    private ClientScreenHelper() {}

    public static CompoundTag profile() {
        return profile;
    }

    public static void clearProfile() {
        profile = new CompoundTag();
    }

    public static void receive(IvaliceStatePayload payload, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        CompoundTag data = payload.state();
        switch (data.getStringOr("kind", "")) {
            case "profile" -> {
                profile = data;
                if (mc.gui.screen() instanceof IvaliceHubScreen hub) hub.refresh();
            }
            case "battle" -> {
                if (mc.gui.screen() instanceof BattleScreen battle) battle.acceptState(data);
                else if (mc.player != null && mc.player.isAlive()) mc.gui.setScreen(new BattleScreen(data));
            }
            case "closed" -> {
                if (mc.gui.screen() instanceof BattleScreen battle) battle.finish();
                mc.player.sendSystemMessage(Component.literal(data.getStringOr("message", "")));
            }
        }
    }

    public static void command(String command) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.connection.sendCommand(command);
    }

    public static void openHubScreen(int initialTab) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.gui.setScreen(new IvaliceHubScreen(initialTab));
    }

    public static void openEquipment() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.gui.setScreen(new InventoryScreen(mc.player));
    }
}
