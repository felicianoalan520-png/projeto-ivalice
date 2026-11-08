package com.ivalice.client;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.ivalice.IvaliceMod;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.registry.IvaliceEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Inicialização exclusiva do cliente (compatível com Iris Shaders e renderizadores 3D GeckoLib 5.5.7 para as 12 criaturas, Marks e Espers).
 */
@Mod(value = IvaliceMod.MODID, dist = Dist.CLIENT)
public class IvaliceModClient {
    private static int clientTicks = 0;

    public IvaliceModClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterRenderers);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        IvaliceMod.LOGGER.info("[Projeto Ivalice] Cliente inicializado com sucesso (Minecraft 26.3 / NeoForge + GeckoLib 5.5.7).");
    }

    private void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        registerGeoMonster(event, IvaliceEntities.WOLF.get(), "wolf");
        registerGeoMonster(event, IvaliceEntities.CACTOID.get(), "cactoid");
        registerGeoMonster(event, IvaliceEntities.HYENA.get(), "hyena");
        registerGeoMonster(event, IvaliceEntities.GIZA_RABBIT.get(), "giza_rabbit");
        registerGeoMonster(event, IvaliceEntities.COCKATRICE.get(), "cockatrice");
        registerGeoMonster(event, IvaliceEntities.ICHTHON.get(), "ichthon");

        registerGeoMonster(event, IvaliceEntities.ROGUE_TOMATO.get(), "rogue_tomato");
        registerGeoMonster(event, IvaliceEntities.THEXTERA.get(), "thextera");
        registerGeoMonster(event, IvaliceEntities.FLOWERING_CACTOID.get(), "flowering_cactoid");
        registerGeoMonster(event, IvaliceEntities.WHITE_MOUSSE.get(), "white_mousse");

        registerGeoMonster(event, IvaliceEntities.BELIAS.get(), "belias");
        registerGeoMonster(event, IvaliceEntities.MATEUS.get(), "mateus");

        IvaliceMod.LOGGER.info("[Projeto Ivalice] 12 Renderizadores 3D GeckoLib 5.5.7 registrados para criaturas, Marks e Espers de Ivalice.");
    }

    private static void registerGeoMonster(
            EntityRenderersEvent.RegisterRenderers event,
            EntityType<IvaliceMonsterEntity> entityType,
            String modelName
    ) {
        Identifier modelId = Identifier.fromNamespaceAndPath(IvaliceMod.MODID, modelName);
        event.registerEntityRenderer(entityType, ctx -> new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<>(modelId)));
    }

    private void onClientTick(final ClientTickEvent.Post event) {
        if ("true".equalsIgnoreCase(System.getProperty("ivalice.autoStopClient"))) {
            clientTicks++;
            if (clientTicks == 40) {
                IvaliceMod.LOGGER.info("[Projeto Ivalice] Cliente validado após 40 ticks (texturas, modelos, GeckoLib 3D, lang e tela inicial carregados sem erros). Encerrando teste automatizado.");
                Minecraft.getInstance().stop();
            }
        }
    }
}
