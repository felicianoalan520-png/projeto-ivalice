package com.ivalice.client.gui;

import com.ivalice.combat.BattleManager;
import com.ivalice.entity.IvaliceMonsterEntity;
import com.ivalice.entity.IvaliceMonsterStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Interface de Combate por Turnos de Final Fantasy XII (Etapa 3.3).
 * Apresenta a janela clássica azul-marinho e dourada de Ivalice com:
 * - Painel de Status do Monstro / Mark / Esper (HP, Nível, Família, Fraqueza via Libra)
 * - Barra de HP e ATB do Jogador
 * - Menu de Comandos: Attack, Magicks (Fire, Blizzard, Thunder, Cure), Technicks (Libra, Steal), Items (Hi-Potion), Gambit Auto e Flee
 * - Registro de Batalha (Combat Log) em tempo real
 */
public class BattleScreen extends Screen {
    private final IvaliceMonsterEntity monster;

    public BattleScreen(IvaliceMonsterEntity monster) {
        super(Component.literal("Final Fantasy XII — Active Dimension Battle"));
        this.monster = monster;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int baseY = this.height / 2 - 20;

        // Coluna 1: Comandos Principais (Attack, Gambit Auto, Cure, Hi-Potion, Flee)
        int col1X = centerX - 185;
        this.addRenderableWidget(Button.builder(Component.literal("⚔ Attack (Ataque)"), btn -> sendBattleCommand("attack"))
                .bounds(col1X, baseY, 115, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("⚙ Gambit Auto-Turn"), btn -> sendBattleCommand("gambit"))
                .bounds(col1X, baseY + 24, 115, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("✚ White Magick: Cura"), btn -> sendBattleCommand("cure"))
                .bounds(col1X, baseY + 48, 115, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("🧪 Item: Hi-Potion"), btn -> sendBattleCommand("potion"))
                .bounds(col1X, baseY + 72, 115, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("🏃 Flee (Recuar)"), btn -> {
            sendBattleCommand("flee");
            this.onClose();
        }).bounds(col1X, baseY + 96, 115, 20).build());

        // Coluna 2: Black/Time Magicks & Technicks (Fire, Blizzard, Thunder, Aero, Libra, Steal)
        int col2X = centerX - 62;
        this.addRenderableWidget(Button.builder(Component.literal("🔥 Black Magick: Fire"), btn -> sendBattleCommand("fire"))
                .bounds(col2X, baseY, 125, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("❄ Black Magick: Blizzard"), btn -> sendBattleCommand("blizzard"))
                .bounds(col2X, baseY + 24, 125, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("⚡ Black Magick: Thunder"), btn -> sendBattleCommand("thunder"))
                .bounds(col2X, baseY + 48, 125, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("👁 Technick: Libra"), btn -> {
            BattleManager.setClientLibraActive(true);
            sendBattleCommand("libra");
        }).bounds(col2X, baseY + 72, 125, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("✋ Technick: Steal"), btn -> sendBattleCommand("steal"))
                .bounds(col2X, baseY + 96, 125, 20).build());
    }

    private void sendBattleCommand(String actionId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null && monster != null) {
            mc.player.connection.sendCommand("ivalice battle " + monster.getId() + " " + actionId);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        //Se o monstro morreu, fecha a tela automaticamente após mostrar vitória
        if (monster == null || !monster.isAlive()) {
            this.onClose();
            return;
        }

        int centerX = this.width / 2;
        int topY = this.height / 2 - 110;

        // Fundo translúcido estilo FFXII Menu Window (Azul-Marinho Escuro com borda Dourada)
        graphics.fillGradient(centerX - 195, topY, centerX + 195, topY + 218, 0xE60B1528, 0xEE060B16);
        graphics.outline(centerX - 195, topY, 390, 218, 0xFFD4AF37);
        graphics.outline(centerX - 193, topY + 2, 386, 214, 0x88AA8822);

        IvaliceMonsterStats stats = monster.getStats();

        // Cabeçalho da Batalha
        graphics.centeredText(this.font, Component.literal("══ FINAL FANTASY XII — COMBATE EM IVALICE ══"), centerX, topY + 8, 0xFFFFD700);

        // Painel do Inimigo (Topo)
        graphics.fill(centerX - 185, topY + 22, centerX + 185, topY + 82, 0x9910203A);
        graphics.outline(centerX - 185, topY + 22, 370, 60, 0xFF887020);

        String titleLine = String.format("Alvo: %s   [Lv.%d | %s | %s]", stats.displayName(), stats.level(), stats.family(), stats.rankType());
        graphics.text(this.font, Component.literal(titleLine), centerX - 178, topY + 27, 0xFFFFE066);

        // Barra de HP do Monstro
        float hpRatio = Math.clamp(monster.getHealth() / monster.getMaxHealth(), 0.0F, 1.0F);
        int barWidth = 220;
        int filledWidth = (int) (barWidth * hpRatio);
        graphics.fill(centerX - 178, topY + 41, centerX - 178 + barWidth, topY + 51, 0xFF221111);
        graphics.fill(centerX - 178, topY + 41, centerX - 178 + filledWidth, topY + 51, 0xFFCC2222);
        graphics.outline(centerX - 178, topY + 41, barWidth, 10, 0xFFD4AF37);

        String hpText = String.format("HP: %d / %d", (int) Math.ceil(monster.getHealth()), (int) Math.ceil(monster.getMaxHealth()));
        graphics.text(this.font, Component.literal(hpText), centerX + 50, topY + 42, 0xFFFFFFFF);

        // Linha Libra (Fraqueza Elemental, Resistência e Roubo)
        String libraLine = String.format("Fraqueza Elemental (2x Dano): %s   |   Resistência: %s   |   Roubo: %s",
                stats.weakness(), stats.resistance(), stats.stealItemId());
        graphics.text(this.font, Component.literal(libraLine), centerX - 178, topY + 56, 0xFF55FFFF);

        // Status do Jogador
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            String playerLine = String.format("Herói: %s   |   HP: %d/%d   |   ATB: PRONTO [100%%]",
                    mc.player.getName().getString(), (int) Math.ceil(mc.player.getHealth()), (int) Math.ceil(mc.player.getMaxHealth()));
            graphics.text(this.font, Component.literal(playerLine), centerX - 178, topY + 69, 0xFF88FF88);
        }

        // Painel de Log de Combate (Direita)
        int logX = centerX + 70;
        int logY = this.height / 2 - 20;
        graphics.fill(logX, logY, centerX + 185, logY + 116, 0xAA091220);
        graphics.outline(logX, logY, 115, 116, 0xFF887020);
        graphics.text(this.font, Component.literal("Registro de Turnos:"), logX + 5, logY + 5, 0xFFFFD700);

        List<String> logs = BattleManager.getClientBattleLog();
        int lineY = logY + 18;
        for (String line : logs) {
            String clipped = line.length() > 22 ? line.substring(0, 22) + "…" : line;
            graphics.text(this.font, Component.literal("• " + clipped), logX + 4, lineY, 0xFFDDDDDD);
            lineY += 13;
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
