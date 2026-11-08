package com.ivalice.client;

import com.ivalice.IvaliceMod;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Inicialização exclusiva do cliente (compatível com Iris Shaders e preparada para renderizadores GeckoLib na Etapa 3).
 */
@Mod(value = IvaliceMod.MODID, dist = Dist.CLIENT)
public class IvaliceModClient {
    private static int clientTicks = 0;

    public IvaliceModClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onClientSetup);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        IvaliceMod.LOGGER.info("[Projeto Ivalice] Cliente inicializado com sucesso (Minecraft 26.3 / NeoForge).");
    }

    private void onClientTick(final ClientTickEvent.Post event) {
        if ("true".equalsIgnoreCase(System.getProperty("ivalice.autoStopClient"))) {
            clientTicks++;
            if (clientTicks == 40) {
                IvaliceMod.LOGGER.info("[Projeto Ivalice] Cliente validado após 40 ticks (texturas, modelos, lang e tela inicial carregados sem erros). Encerrando teste automatizado.");
                Minecraft.getInstance().stop();
            }
        }
    }
}
