package com.ivalice.client;

import com.ivalice.entity.IvaliceMonsterEntity;
import net.minecraft.client.Minecraft;

/**
 * Ponte segura para abertura das interfaces gráficas (GUIs) de Final Fantasy XII no cliente.
 */
public class ClientScreenHelper {

    public static void openBattleScreen(IvaliceMonsterEntity monster) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new com.ivalice.client.gui.BattleScreen(monster));
        }
    }

    public static void openHubScreen(int initialTab) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(new com.ivalice.client.gui.IvaliceHubScreen(initialTab));
        }
    }
}
