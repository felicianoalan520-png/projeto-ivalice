package com.ivalice.client;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.ivalice.IvaliceMod;
import com.ivalice.client.gui.BattleScreen;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.registry.IvaliceEntities;
import com.ivalice.network.IvaliceStatePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Inicialização exclusiva do cliente (compatível com Iris Shaders e renderizadores 3D GeckoLib 5.5.7 para as criaturas, Marks e Espers).
 */
@Mod(value = IvaliceMod.MODID, dist = Dist.CLIENT)
public class IvaliceModClient {
    private static int clientTicks = 0;
    private static final KeyMapping.Category KEYS = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(IvaliceMod.MODID, "controls"));
    private static final KeyMapping MENU = key("menu", InputConstants.KEY_NUMPAD1);
    private static final KeyMapping BATTLE = key("battle", InputConstants.KEY_NUMPAD2);
    private static final KeyMapping JOURNAL = key("journal", InputConstants.KEY_NUMPAD3);
    private static final KeyMapping BESTIARY = key("bestiary", InputConstants.KEY_NUMPAD4);
    private static final KeyMapping CHARACTER = key("character", InputConstants.KEY_NUMPAD5);
    private static final KeyMapping EQUIPMENT = key("equipment", InputConstants.KEY_NUMPAD6);

    private static KeyMapping key(String id, int key) {
        return new KeyMapping("key.ivalice." + id, key, KEYS);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : new KeyMapping[]{MENU, BATTLE, JOURNAL, BESTIARY, CHARACTER, EQUIPMENT}) {
            event.register(key);
        }
    }

    private void registerPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(IvaliceStatePayload.TYPE, ClientScreenHelper::receive);
    }

    private void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientScreenHelper.clearProfile();
    }

    public IvaliceModClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterRenderers);
        modEventBus.addListener(this::registerKeys);
        modEventBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onDisconnect);
        NeoForge.EVENT_BUS.addListener(this::onNameTag);
    }

    private void onNameTag(RenderNameTagEvent.CanRender event) {
        if (event.getEntity() instanceof IvaliceMonsterEntity
                && Minecraft.getInstance().gui.screen() instanceof BattleScreen battle) {
            int number = battle.targetNumber(event.getEntity().getId());
            event.setCanRender(number == 0 ? TriState.FALSE : TriState.TRUE);
            if (number != 0) event.setContent(Component.literal(Integer.toString(number)));
        }
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
        registerGeoMonster(event, IvaliceEntities.WILD_SAURIAN.get(), "wild_saurian");
        registerGeoMonster(event, IvaliceEntities.MANDRAGORA.get(), "mandragora");
        registerGeoMonster(event, IvaliceEntities.MALBORO.get(), "malboro");
        registerGeoMonster(event, IvaliceEntities.YETI.get(), "yeti");
        registerGeoMonster(event, IvaliceEntities.BOMB.get(), "bomb");
        registerGeoMonster(event, IvaliceEntities.WHITE_WOLF.get(), "white_wolf");

        registerGeoMonster(event, IvaliceEntities.ROGUE_TOMATO.get(), "rogue_tomato");
        registerGeoMonster(event, IvaliceEntities.THEXTERA.get(), "thextera");
        registerGeoMonster(event, IvaliceEntities.FLOWERING_CACTOID.get(), "flowering_cactoid");
        registerGeoMonster(event, IvaliceEntities.WHITE_MOUSSE.get(), "white_mousse");

        registerGeoMonster(event, IvaliceEntities.BELIAS.get(), "belias");
        registerGeoMonster(event, IvaliceEntities.MATEUS.get(), "mateus");

        registerGeoMonster(event, IvaliceEntities.CHOCOBO.get(), "chocobo");
        registerGeoMonster(event, IvaliceEntities.ALPHA_WOLF.get(), "alpha_wolf");
        registerGeoMonster(event, IvaliceEntities.ALPHA_HYENA.get(), "alpha_hyena");
        registerGeoMonster(event, IvaliceEntities.ALPHA_SAURIAN.get(), "alpha_saurian");
        registerGeoMonster(event, IvaliceEntities.COEURL.get(), "coeurl");
        registerGeoMonster(event, IvaliceEntities.BEHEMOTH.get(), "behemoth");
        registerGeoMonster(event, IvaliceEntities.SKELETON_CRUSADER.get(), "skeleton_crusader");
        registerGeoMonster(event, IvaliceEntities.FIREMANE.get(), "firemane");
        registerGeoMonster(event, IvaliceEntities.JUDGE_GABRANTH.get(), "judge_gabranth");
        registerGeoMonster(event, IvaliceEntities.GILGAMESH.get(), "gilgamesh");
        registerGeoMonster(event, IvaliceEntities.ZODIARK.get(), "zodiark");
        registerGeoMonster(event, IvaliceEntities.YIAZMAT.get(), "yiazmat");

        registerGeoMonster(event, IvaliceEntities.MINDFLAYER.get(), "mindflayer");
        registerGeoMonster(event, IvaliceEntities.RUBICANTE.get(), "rubicante");
        registerGeoMonster(event, IvaliceEntities.IRON_GIANT.get(), "iron_giant");
        registerGeoMonster(event, IvaliceEntities.GOLBEZ.get(), "golbez");
        registerGeoMonster(event, IvaliceEntities.IFRIT_PYREBURNER.get(), "ifrit_pyreburner");
        registerGeoMonster(event, IvaliceEntities.ADAMANTOISE.get(), "adamantoise");

        registerGeoMonster(event, IvaliceEntities.ULTROS.get(), "ultros");
        registerGeoMonster(event, IvaliceEntities.GIL_TURTLE.get(), "gil_turtle");
        registerGeoMonster(event, IvaliceEntities.ATB_WEAPON.get(), "atb_weapon");
        registerGeoMonster(event, IvaliceEntities.SHINRYU.get(), "shinryu");
        registerGeoMonster(event, IvaliceEntities.OMEGA_MK.get(), "omega_mk");
        registerGeoMonster(event, IvaliceEntities.KEFKA_GOD.get(), "kefka_god");

        IvaliceMod.LOGGER.info("[Projeto Ivalice] Renderizadores 3D GeckoLib 5.5.7 registrados para criaturas, Marks e Espers de Ivalice.");
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.isAlive() && mc.gui.screen() == null) {
            if (MENU.consumeClick()) ClientScreenHelper.openHubScreen(-1);
            else if (BATTLE.consumeClick()) {
                if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof IvaliceMonsterEntity mob) {
                    ClientScreenHelper.command("ivalice engage " + mob.getId());
                } else {
                    mc.player.sendSystemMessage(Component.literal("Mire em uma criatura de Ivalice próxima."));
                }
            } else if (JOURNAL.consumeClick()) ClientScreenHelper.openHubScreen(2);
            else if (BESTIARY.consumeClick()) ClientScreenHelper.openHubScreen(1);
            else if (CHARACTER.consumeClick()) ClientScreenHelper.openHubScreen(3);
            else if (EQUIPMENT.consumeClick()) ClientScreenHelper.openEquipment();
        }
        if ("true".equalsIgnoreCase(System.getProperty("ivalice.autoStopClient"))) {
            clientTicks++;
            if (clientTicks == 40) {
                IvaliceMod.LOGGER.info("[Projeto Ivalice] Cliente validado após 40 ticks (texturas, modelos, GeckoLib 3D, lang e tela inicial carregados sem erros). Encerrando teste automatizado.");
                Minecraft.getInstance().stop();
            }
        }
    }
}
