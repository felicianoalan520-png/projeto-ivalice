package com.ivalice.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;

abstract class IvalicePanelScreen extends Screen {
    protected static final int TEXT = 0xFFF1EAD8;
    protected static final int ACCENT = 0xFFEBC982;
    protected static final int MUTED = 0xFFAAB9CA;
    private final List<Runnable> shortcuts = new ArrayList<>();
    protected int panelX;
    protected int panelY;
    protected int panelWidth;
    protected int panelHeight;

    protected IvalicePanelScreen(String title) {
        super(Component.literal(title));
    }

    protected void layout(int desiredWidth, int desiredHeight, boolean bottom) {
        shortcuts.clear();
        panelWidth = Math.min(width - 12, desiredWidth);
        panelHeight = Math.min(height - 8, desiredHeight);
        panelX = (width - panelWidth) / 2;
        panelY = bottom ? height - panelHeight - 6 : (height - panelHeight) / 2;
    }

    protected void option(String label, int column, int row, Runnable action) {
        int cell = (panelWidth - 25) / 2;
        shortcuts.add(action);
        addRenderableWidget(new PanelButton(label, shortcuts.size(), panelX + 9 + column * (cell + 7),
                panelY + 35 + row * 22, cell, action));
    }

    protected void button(String label, int x, int y, int w, Runnable action) {
        addRenderableWidget(new PanelButton(label, 0, x, y, w, action));
    }

    protected String fit(String text, int maxWidth) {
        return font.width(text) <= maxWidth ? text
                : font.plainSubstrByWidth(text, Math.max(0, maxWidth - font.width("…"))) + "…";
    }

    protected void line(GuiGraphicsExtractor graphics, String text, int y, int color) {
        graphics.text(font, fit(text, panelWidth - 18), panelX + 9, panelY + y, color);
    }

    protected void drawPanel(GuiGraphicsExtractor graphics, String heading, String subtitle) {
        int right = panelX + panelWidth;
        int bottom = panelY + panelHeight;
        graphics.fill(panelX + 3, panelY + 4, right + 3, bottom + 4, 0x7805060B);
        graphics.fillGradient(panelX, panelY, right, bottom, 0xF51A2939, 0xF50D1423);
        graphics.outline(panelX, panelY, panelWidth, panelHeight, 0xFF7B6546);
        graphics.fill(panelX + 1, panelY + 1, right - 1, panelY + 3, 0xFFBC9254);
        graphics.fill(panelX + 10, panelY + 31, right - 10, panelY + 32, 0x706D8595);
        int emblem = panelX + 15;
        for (int offset = -5; offset <= 5; offset++) {
            int span = 5 - Math.abs(offset);
            graphics.fill(emblem - span, panelY + 12 + offset, emblem + span + 1,
                    panelY + 13 + offset, offset <= 0 ? 0xFFE0B96D : 0xFF947448);
        }
        graphics.fill(emblem, panelY + 9, emblem + 1, panelY + 14, 0xFFF4E1A1);
        graphics.text(font, fit(heading, panelWidth - 34), panelX + 26, panelY + 8, TEXT);
        graphics.text(font, fit(subtitle, panelWidth - 34), panelX + 26, panelY + 21, MUTED);
    }

    private final class PanelButton extends Button {
        private final String label;
        private final int number;

        PanelButton(String label, int number, int x, int y, int width, Runnable action) {
            super(Button.builder(Component.literal(label), button -> action.run()).bounds(x, y, width, 18));
            this.label = label;
            this.number = number;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            boolean selected = isHoveredOrFocused();
            int fill = selected ? 0xFF365267 : 0xFF223447;
            int outline = selected ? 0xFFE2B86F : 0xFF475E72;
            graphics.fillGradient(x, y, x + getWidth(), y + 18, fill, selected ? 0xFF243B50 : 0xFF1A2637);
            graphics.outline(x, y, getWidth(), 18, outline);
            if (number != 0) {
                graphics.fill(x + 4, y + 3, x + 17, y + 15, selected ? 0xFFE2B86F : 0xFF121C2C);
                graphics.text(font, Integer.toString(number), x + 8, y + 5, selected ? 0xFF162437 : ACCENT, false);
            }
            int inset = number == 0 ? 6 : 22;
            graphics.text(font, fit(label, getWidth() - inset - 5), x + inset, y + 5, TEXT, false);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Preserve the world view around the compact panel.
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        int index = key >= InputConstants.KEY_NUMPAD1 && key <= InputConstants.KEY_NUMPAD6
                ? key - InputConstants.KEY_NUMPAD1 : key - InputConstants.KEY_1;
        if (index >= 0 && index < shortcuts.size()) {
            shortcuts.get(index).run();
            return true;
        }
        return super.keyPressed(event);
    }
}
