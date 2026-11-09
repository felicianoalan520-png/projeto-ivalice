package com.ivalice.client.gui;

import com.ivalice.client.ClientScreenHelper;
import com.ivalice.entity.IvaliceMonsterStats;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class BattleScreen extends IvalicePanelScreen {
    private enum Page { COMMANDS, MAGICK, CAST_MODE, TECHNICKS, ITEMS, TARGET }
    private CompoundTag state;
    private Page page = Page.COMMANDS;
    private Page targetBack;
    private Page spellBack;
    private String spellLabel;
    private int spellCost;
    private String action;
    private boolean waiting;
    private boolean finished;
    private int ticksWaiting;
    private String visualCue = "";
    private int targetsPage;
    private int spellsPage;

    public BattleScreen(CompoundTag state) {
        super("Encontro de Ivalice");
        this.state = state;
    }

    public void acceptState(CompoundTag updated) {
        state = updated;
        waiting = false;
        open(Page.COMMANDS);
    }

    public void showAnimation(String cue) {
        visualCue = cue;
        ticksWaiting = 0;
        if (!waiting) {
            waiting = true;
            rebuildWidgets();
        }
    }

    public void finish() {
        finished = true;
        super.onClose();
    }

    @Override
    protected void init() {
        layout(294, 123, true);
        if (waiting) {
            return;
        }
        switch (page) {
            case COMMANDS -> {
                option("Ataque", 0, 0, () -> select("attack"));
                option("Magias", 1, 0, () -> {
                    spellsPage = 0;
                    open(Page.MAGICK);
                });
                option("Itens", 0, 1, () -> {
                    spellsPage = 0;
                    open(Page.ITEMS);
                });
                option("Técnicas", 1, 1, () -> {
                    spellsPage = 0;
                    open(Page.TECHNICKS);
                });
                option("Gambit", 0, 2, () -> select("gambit"));
                option("Fugir", 1, 2, () -> send("flee", firstTarget()));
            }
            case MAGICK, TECHNICKS -> {
                ListTag spells = availableSpells(page == Page.TECHNICKS);
                int count = Math.max(1, (spells.size() + 5) / 6);
                spellsPage = Math.clamp(spellsPage, 0, count - 1);
                for (int i = spellsPage * 6; i < Math.min((spellsPage + 1) * 6, spells.size()); i++) {
                    CompoundTag sp = spells.getCompoundOrEmpty(i);
                    String id = sp.getStringOr("id", "fire");
                    String label = sp.getStringOr("label", "Fire");
                    int mp = sp.getIntOr("mp", 10);
                    boolean self = sp.getBooleanOr("self", false);
                    boolean areaOnly = sp.getBooleanOr("areaOnly", false);
                    int slot = i % 6;
                    String btnText = mp == 0 ? label : label + " • " + mp + " MP";
                    option(btnText, slot % 2, slot / 2, () -> {
                        if (self) {
                            send(id, firstTarget());
                        } else if (areaOnly) {
                            send(id + "_area", firstTarget());
                        } else if (sp.getBooleanOr("area", false)) {
                            spell(id, label, mp);
                        } else {
                            select(id);
                        }
                    });
                }
                if (spells.size() > 6) {
                    button("Mais ›", panelX + panelWidth - 134, panelY + 79, 60, () -> {
                        spellsPage = (spellsPage + 1) % count;
                        rebuildWidgets();
                    });
                }
            }
            case ITEMS -> {
                var items = state.getListOrEmpty("availableItems");
                int count = Math.max(1, (items.size() + 5) / 6);
                spellsPage = Math.clamp(spellsPage, 0, count - 1);
                for (int i = spellsPage * 6; i < Math.min((spellsPage + 1) * 6, items.size()); i++) {
                    var item = items.getCompoundOrEmpty(i);
                    String id = "item_" + item.getStringOr("id", "");
                    int slot = i % 6;
                    option(item.getStringOr("label", "") + " ×" + item.getIntOr("count", 0), slot % 2, slot / 2, () -> {
                        if (item.getBooleanOr("self", false)) send(id, firstTarget());
                        else select(id);
                    });
                }
                if (count > 1) button("Mais ›", panelX + panelWidth - 134, panelY + 79, 60, () -> {
                    spellsPage = (spellsPage + 1) % count;
                    rebuildWidgets();
                });
            }
            case CAST_MODE -> {
                option("Alvo único • " + spellCost + " MP", 0, 0, () -> select(action));
                option("Em área • " + (spellCost + com.ivalice.combat.BattleManager.AREA_MP_SURCHARGE) + " MP",
                        1, 0, () -> send(action + "_area", firstTarget()));
            }
            case TARGET -> {
                for (int i = targetsPage * 6; i < Math.min((targetsPage + 1) * 6, enemies().size()); i++) {
                    CompoundTag target = enemies().getCompoundOrEmpty(i);
                    var stats = IvaliceMonsterStats.byId(target.getStringOr("species", "wolf"));
                    String name = stats.displayName();
                    int annotation = name.indexOf(" (");
                    if (annotation >= 0) name = name.substring(0, annotation);
                    String suffix = " • " + target.getIntOr("hp", 0) + " HP";
                    int slot = i % 6;
                    String label = fit(name, (panelWidth - 25) / 2 - font.width(suffix) - 22) + suffix;
                    option(label, slot % 2, slot / 2, () -> send(action, target.getIntOr("id", 0)));
                }
            }
        }
        if (page != Page.COMMANDS && page != Page.TARGET) {
            button("Voltar", panelX + panelWidth - 68, panelY + 79, 59, this::onClose);
        }
        if (page == Page.TARGET && enemies().size() > 6) {
            button(targetsPage == 0 ? "Alvo 7 ›" : "‹ 1 a 6", panelX + panelWidth - 72, panelY + 100, 63, () -> {
                targetsPage = 1 - targetsPage;
                rebuildWidgets();
            });
        }
    }

    private ListTag enemies() {
        return state.getListOrEmpty("enemies");
    }

    private ListTag availableSpells(boolean techniques) {
        var result = new ListTag();
        var available = state.getListOrEmpty("availableSpells");
        for (int i = 0; i < available.size(); i++) {
            var spell = available.getCompoundOrEmpty(i);
            if (spell.getBooleanOr("technique", false) == techniques) result.add(spell);
        }
        return result;
    }

    public int targetNumber(int entityId) {
        if (page != Page.TARGET || waiting) return 0;
        int start = targetsPage * 6;
        for (int i = start; i < Math.min(start + 6, enemies().size()); i++) {
            if (enemies().getCompoundOrEmpty(i).getIntOr("id", 0) == entityId) {
                return i - start + 1;
            }
        }
        return 0;
    }

    private int firstTarget() {
        return enemies().getCompoundOrEmpty(0).getIntOr("id", 0);
    }

    private void spell(String id, String label, int cost) {
        action = id;
        spellLabel = label;
        spellCost = cost;
        spellBack = page;
        open(Page.CAST_MODE);
    }

    private void select(String choice) {
        if (enemies().size() <= 1) {
            send(choice, firstTarget());
        } else {
            action = choice;
            targetBack = page;
            targetsPage = 0;
            open(Page.TARGET);
        }
    }

    private void open(Page next) {
        page = next;
        rebuildWidgets();
    }

    private void send(String choice, int id) {
        if (waiting) return;
        waiting = true;
        visualCue = "";
        ticksWaiting = 0;
        rebuildWidgets();
        ClientScreenHelper.command("ivalice battle " + id + " " + choice + " " + state.getIntOr("revision", 1));
    }

    @Override
    public void tick() {
        if (waiting && ++ticksWaiting > 220) {
            super.onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (waiting) return;
        switch (page) {
            case COMMANDS -> send("flee", firstTarget());
            case TARGET -> open(targetBack);
            case CAST_MODE -> open(spellBack);
            default -> open(Page.COMMANDS);
        }
    }

    @Override
    public void removed() {
        if (!finished) {
            ClientScreenHelper.command("ivalice battle " + firstTarget() + " flee");
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (waiting) {
            if (!visualCue.isEmpty()) {
                graphics.centeredText(font, fit(visualCue, width - 16), width / 2, 16, ACCENT);
            }
            return;
        }
        String heading = switch (page) {
            case COMMANDS -> "Rodada " + state.getIntOr("round", 1) + "   •   " + enemies().size() + " inimigo(s)";
            case MAGICK -> "Grimório de Magias";
            case CAST_MODE -> spellLabel;
            case TECHNICKS -> "Técnicas";
            case ITEMS -> "Bolsa de batalha";
            case TARGET -> "Escolha o alvo";
        };
        int hp = minecraft.player == null ? 0 : (int) Math.ceil(minecraft.player.getHealth());
        drawPanel(graphics, heading, "HP " + hp + "   •   MP " + state.getIntOr("mp", 0)
                + "/" + state.getIntOr("maxMp", 100));
        if (minecraft.player != null && panelWidth >= 225) {
            drawMeter(graphics, 12, minecraft.player.getHealth() / minecraft.player.getMaxHealth(), 0xFFE4A06D);
            drawMeter(graphics, 21, (float) state.getIntOr("mp", 0) / Math.max(1, state.getIntOr("maxMp", 100)), 0xFF73B9CC);
        }
        if (page == Page.CAST_MODE) {
            line(graphics, "Alvo único: dano completo.", 58, TEXT);
            line(graphics, "Em área: 60% de dano por inimigo.", 70, MUTED);
        } else if (page == Page.MAGICK && availableSpells(false).isEmpty()) {
            line(graphics, "Compre ou use Pergaminhos de Magia com uma profissão mágica.", 52, TEXT);
        } else if (page == Page.TARGET && enemies().size() > 6) {
            line(graphics, "Esc para voltar", 105, MUTED);
        } else {
            line(graphics, state.getStringOr("message", ""), 105, MUTED);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawMeter(GuiGraphicsExtractor graphics, int row, float ratio, int color) {
        int right = panelX + panelWidth - 10;
        int left = right - 58;
        int y = panelY + row;
        graphics.fill(left, y, right, y + 4, 0xFF253B50);
        graphics.fill(left, y, left + Math.round(58 * Math.clamp(ratio, 0, 1)), y + 4, color);
    }
}
