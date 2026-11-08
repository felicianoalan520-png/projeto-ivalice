package com.ivalice.client.gui;

import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceMonsterStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Interface Gráfica Principal do Projeto Ivalice (Etapas 1, 4 e 5).
 * Reúne 5 abas interativas em estilo Final Fantasy XII:
 * 0 - Cristal de Teleporte (Teleporte direto para qualquer um dos 15 Biomas Canônicos de Ivalice)
 * 1 - Bestiário (Clan Primer: Catálogo das 12 Criaturas, Fraquezas Elementais, Habitats e Sage Knowledge)
 * 2 - Quadro de Caçadas (Clan Centurio Hunt Board: Aceitar e Invocar Marks Rank I, V e Espers Rank S)
 * 3 - Tabuleiro de Licenças & Gambits (License Board: Gastar LP em bônus permanentes de HP, Ataque, Velocidade e Defesa)
 * 4 - Bazaar de Rabanastre (Trocar espólios de monstros por pacotes especiais como Tournesol, Masamune e Hi-Potions)
 */
public class IvaliceHubScreen extends Screen {
    private int currentTab;
    private int selectedMonsterIndex = 0;

    private static final String[][] BIOME_BUTTONS = {
            {"dalmasca_estersand", "1. Dalmasca Estersand"},
            {"dalmasca_westersand", "2. Dalmasca Westersand"},
            {"giza_plains_dry", "3. Giza Plains (Dry)"},
            {"giza_plains_rains", "4. Giza Plains (Rains)"},
            {"ozmone_plain", "5. Ozmone Plain"},
            {"golmore_jungle", "6. Golmore Jungle"},
            {"phon_coast", "7. Phon Coast"},
            {"tchita_uplands", "8. Tchita Uplands"},
            {"paramina_rift", "9. Paramina Rift"},
            {"salikawood", "10. Salikawood"},
            {"feywood", "11. Feywood"},
            {"mosphoran_highwaste", "12. Mosphoran Highwaste"},
            {"nabreus_deadlands", "13. Nabreus Deadlands"},
            {"ogir_yensa_sandsea", "14. Ogir-Yensa Sandsea"},
            {"nam_yensa_sandsea", "15. Nam-Yensa / Cerobi"}
    };

    public IvaliceHubScreen(int initialTab) {
        super(Component.literal("Crônicas de Ivalice — Clan Centurio & Cristal de Teleporte"));
        this.currentTab = initialTab;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int topY = this.height / 2 - 115;

        // Barra de Abas no Topo
        this.addRenderableWidget(Button.builder(Component.literal("💎 Teleporte (Biomas)"), b -> switchTab(0))
                .bounds(centerX - 205, topY + 22, 80, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("📖 Bestiário"), b -> switchTab(1))
                .bounds(centerX - 122, topY + 22, 75, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("🎯 Caçadas (Hunts)"), b -> switchTab(2))
                .bounds(centerX - 44, topY + 22, 88, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("🛡 Licenças & Gambits"), b -> switchTab(3))
                .bounds(centerX + 47, topY + 22, 92, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal("⚖ Bazaar"), b -> switchTab(4))
                .bounds(centerX + 142, topY + 22, 63, 18).build());

        int contentY = topY + 48;

        if (currentTab == 0) {
            // Aba 0: 15 Botões de Teleporte Direto para os Biomas de Ivalice
            for (int i = 0; i < BIOME_BUTTONS.length; i++) {
                int col = i % 3;
                int row = i / 3;
                String biomeId = BIOME_BUTTONS[i][0];
                String label = BIOME_BUTTONS[i][1];
                this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                    sendServerCommand("ivalice tp " + biomeId);
                    this.onClose();
                }).bounds(centerX - 195 + col * 132, contentY + 18 + row * 26, 126, 22).build());
            }
        } else if (currentTab == 1) {
            // Aba 1: Bestiário (Clan Primer) — Seletor das 12 criaturas à esquerda e detalhes à direita
            List<IvaliceMonsterStats> monsters = IvaliceMonsterStats.ALL_MONSTERS;
            for (int i = 0; i < monsters.size(); i++) {
                final int idx = i;
                int col = i / 6;
                int row = i % 6;
                String shortName = (i + 1) + ". " + monsters.get(i).id();
                this.addRenderableWidget(Button.builder(Component.literal(shortName), btn -> {
                    this.selectedMonsterIndex = idx;
                }).bounds(centerX - 198 + col * 92, contentY + 16 + row * 24, 88, 20).build());
            }
        } else if (currentTab == 2) {
            // Aba 2: Quadro de Caçadas do Clã Centurio (6 Marks & Espers)
            List<IvalicePlayerData.MarkHuntEntry> hunts = IvalicePlayerData.MARK_HUNTS;
            for (int i = 0; i < hunts.size(); i++) {
                IvalicePlayerData.MarkHuntEntry hunt = hunts.get(i);
                int rowY = contentY + 14 + i * 26;
                this.addRenderableWidget(Button.builder(Component.literal("⚔ Invocar/Caçar: " + hunt.id()), btn -> {
                    sendServerCommand("ivalice hunt spawn " + hunt.id());
                    this.onClose();
                }).bounds(centerX + 55, rowY, 140, 20).build());
            }
        } else if (currentTab == 3) {
            // Aba 3: Tabuleiro de Licenças (License Board) & Gambits
            List<IvalicePlayerData.LicenseNode> nodes = IvalicePlayerData.LICENSE_CATALOG;
            for (int i = 0; i < Math.min(nodes.size(), 10); i++) {
                IvalicePlayerData.LicenseNode node = nodes.get(i);
                int col = i % 2;
                int row = i / 2;
                String btnLabel = String.format("%s [%d LP]", node.name(), node.lpCost());
                this.addRenderableWidget(Button.builder(Component.literal(btnLabel), btn -> {
                    sendServerCommand("ivalice license unlock " + node.id());
                }).bounds(centerX - 195 + col * 198, contentY + 16 + row * 25, 192, 21).build());
            }
            this.addRenderableWidget(Button.builder(Component.literal("⚙ Alternar Preset de Gambits Automáticos"), btn -> {
                sendServerCommand("ivalice gambit toggle");
            }).bounds(centerX - 130, contentY + 144, 260, 20).build());
        } else if (currentTab == 4) {
            // Aba 4: Bazaar de Rabanastre
            List<IvalicePlayerData.BazaarPackage> packs = IvalicePlayerData.BAZAAR_PACKAGES;
            for (int i = 0; i < packs.size(); i++) {
                IvalicePlayerData.BazaarPackage pack = packs.get(i);
                int rowY = contentY + 18 + i * 34;
                this.addRenderableWidget(Button.builder(Component.literal("💰 Adquirir (" + pack.gilCost() + " Gil)"), btn -> {
                    sendServerCommand("ivalice bazaar buy " + pack.id());
                }).bounds(centerX + 65, rowY, 130, 22).build());
            }
        }
    }

    private void switchTab(int tab) {
        this.currentTab = tab;
        this.rebuildWidgets();
    }

    private void sendServerCommand(String cmd) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendCommand(cmd);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int topY = this.height / 2 - 115;

        // Janela Principal FFXII
        graphics.fillGradient(centerX - 212, topY, centerX + 212, topY + 230, 0xEC091326, 0xF2050914);
        graphics.outline(centerX - 212, topY, 424, 230, 0xFFD4AF37);
        graphics.outline(centerX - 210, topY + 2, 420, 226, 0x88AA8822);

        graphics.centeredText(this.font, Component.literal("══ PROJETO IVALICE — FINAL FANTASY XII: THE ZODIAC AGE ══"), centerX, topY + 8, 0xFFFFD700);

        int contentY = topY + 48;

        if (currentTab == 0) {
            graphics.centeredText(this.font, Component.literal("Clique em qualquer Região Canônica para Teleportar imediatamente até lá:"), centerX, contentY + 2, 0xFF55FFFF);
        } else if (currentTab == 1) {
            graphics.text(this.font, Component.literal("Catálogo do Clã:"), centerX - 195, contentY + 2, 0xFFFFD700);
            IvaliceMonsterStats m = IvaliceMonsterStats.ALL_MONSTERS.get(selectedMonsterIndex);
            int infoX = centerX - 8;
            graphics.fill(infoX, contentY + 12, centerX + 202, contentY + 168, 0xAA0D1B30);
            graphics.outline(infoX, contentY + 12, 210, 156, 0xFFD4AF37);

            graphics.text(this.font, Component.literal(m.displayName()), infoX + 6, contentY + 18, 0xFFFFE066);
            graphics.text(this.font, Component.literal("Família: " + m.family() + " | Lv." + m.level()), infoX + 6, contentY + 31, 0xFFDDDDDD);
            graphics.text(this.font, Component.literal("Classificação: " + m.rankType()), infoX + 6, contentY + 43, 0xFFFFAA55);
            graphics.text(this.font, Component.literal("HP Base: " + (int) m.maxHealth() + " | ATQ: " + m.attackDamage()), infoX + 6, contentY + 55, 0xFF88FF88);
            graphics.text(this.font, Component.literal("Fraqueza (2x): " + m.weakness() + " | Resist: " + m.resistance()), infoX + 6, contentY + 67, 0xFF55FFFF);
            graphics.text(this.font, Component.literal("Espólios: " + m.primaryLootId() + ", " + m.rareLootId()), infoX + 6, contentY + 79, 0xFFFFCC88);
            graphics.text(this.font, Component.literal("Recompensa: " + m.gilReward() + " Gil & " + m.lpReward() + " LP"), infoX + 6, contentY + 91, 0xFFFFD700);
            graphics.text(this.font, Component.literal("Habitat: " + clip(m.habitat(), 30)), infoX + 6, contentY + 105, 0xFFAAAAAA);
            graphics.text(this.font, Component.literal("Nota: " + clip(m.bestiaryLore1(), 32)), infoX + 6, contentY + 120, 0xFFCCCCCC);
            graphics.text(this.font, Component.literal(clip(m.bestiaryLore2(), 35)), infoX + 6, contentY + 133, 0xFFDDA0DD);
        } else if (currentTab == 2) {
            graphics.text(this.font, Component.literal("Quadro de Caçadas do Clã Centurio (Montblanc):"), centerX - 195, contentY + 1, 0xFFFFD700);
            List<IvalicePlayerData.MarkHuntEntry> hunts = IvalicePlayerData.MARK_HUNTS;
            for (int i = 0; i < hunts.size(); i++) {
                IvalicePlayerData.MarkHuntEntry h = hunts.get(i);
                int rowY = contentY + 16 + i * 26;
                graphics.text(this.font, Component.literal("[" + h.rank() + "] " + h.markName()), centerX - 195, rowY, 0xFFFF8866);
                graphics.text(this.font, Component.literal("Região: " + h.biomeId() + " | +" + h.gilReward() + " Gil, +" + h.lpReward() + " LP"), centerX - 195, rowY + 10, 0xFFBBBBBB);
            }
        } else if (currentTab == 3) {
            graphics.centeredText(this.font, Component.literal("Tabuleiro de Licenças (License Board) — Clique para desbloquear bônus permanentes com LP:"), centerX, contentY + 2, 0xFF88FF88);
        } else if (currentTab == 4) {
            graphics.text(this.font, Component.literal("Bazaar de Rabanastre — Mercadorias Especiais Desbloqueáveis:"), centerX - 195, contentY + 2, 0xFFFFD700);
            List<IvalicePlayerData.BazaarPackage> packs = IvalicePlayerData.BAZAAR_PACKAGES;
            for (int i = 0; i < packs.size(); i++) {
                IvalicePlayerData.BazaarPackage p = packs.get(i);
                int rowY = contentY + 19 + i * 34;
                graphics.text(this.font, Component.literal("★ " + p.packageName()), centerX - 195, rowY, 0xFFFFE066);
                graphics.text(this.font, Component.literal(clip(p.description(), 44)), centerX - 195, rowY + 11, 0xFFCCCCCC);
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static String clip(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
