package com.ivalice.client.gui;

import com.ivalice.client.ClientScreenHelper;
import com.ivalice.data.IvalicePlayerData;
import com.ivalice.entity.IvaliceMonsterStats;
import com.ivalice.job.ZodiacJob;
import com.ivalice.job.ZodiacJobs;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;

import java.util.ArrayList;
import java.util.List;

public final class IvaliceHubScreen extends IvalicePanelScreen {
    private static final String[][] BIOMES = {
            {"dalmasca_estersand", "Dalmasca Estersand"}, {"dalmasca_westersand", "Dalmasca Westersand"},
            {"giza_plains_dry", "Giza Plains • Seca"}, {"giza_plains_rains", "Giza Plains • Chuvas"},
            {"ozmone_plain", "Ozmone Plain"}, {"golmore_jungle", "Golmore Jungle"},
            {"phon_coast", "Phon Coast"}, {"tchita_uplands", "Tchita Uplands"},
            {"paramina_rift", "Paramina Rift"}, {"salikawood", "Salikawood"},
            {"feywood", "Feywood"}, {"mosphoran_highwaste", "Mosphoran Highwaste"},
            {"nabreus_deadlands", "Nabreus Deadlands"}, {"ogir_yensa_sandsea", "Ogir-Yensa Sandsea"},
            {"cerobi_steppe", "Cerobi Steppe"}
    };

    private enum Page { ROOT, TRAVEL, BESTIARY, JOURNAL, CHARACTER, BAZAAR, LICENSES, JOBS, JOB, RESET_JOB, MONSTER, HUNT, GUIDE, GUIDE_TOPIC }
    private Page page;
    private int listPage;
    private IvaliceMonsterStats monster;
    private ZodiacJob selectedJob;
    private GuideTopic guideTopic;
    private record GuideTopic(String title, String text) {}
    private static final List<GuideTopic> GUIDE = List.of(
            new GuideTopic("Primeiros passos", "Explore Ivalice, o Nether e o Fim. Use Num 1 para abrir este menu. Os shaders com relevo 3D e POM estão em shaderpacks/Ivalice-Realism-PBR.zip."),
            new GuideTopic("Níveis 1 ao 99", "Sua progressão vai do Nível 1 ao Nível 99 (máximo). Cada nível aumenta Vida, Ataque, Defesa e MP, libera a 2ª profissão no Nv.5 e destrava magias progressivas."),
            new GuideTopic("Magias e raridades", "Aldeões vendem magias apenas até o Tier 2 (Nv.1-28). Magias Tier 3 -ga (Raras, Nv.36-56) e Tier 4 Cataclísmicas (Épicas, Nv.68-95) devem ser fabricadas na Alquimia!"),
            new GuideTopic("Alquimia e ingredientes", "Cada magia exige ingredientes de regiões específicas: Fire/Water/Wind Stones nos biomas de Ivalice, Gemsteel/Dark Magicite no Nether e Serpentarius/Empyreal Soul no Fim."),
            new GuideTopic("Ruínas do Nether e do End", "No Nether, vasculhe Mobliz e os destroços Magitek em busca do Fragmento do Julgamento. Use-o no altar da Torre de Kefka. No End, procure Esferas Negras e o Selo do Cristal nos cofres de Ridorana e Giruvegan."),
            new GuideTopic("Bestiário e caçadas", "Num 4 exibe as 42 espécies 3D (FFXII, FFIV, FFV, FFVI e FFXV). Num 3 abre o Quadro do Clã Centurio para invocar Marks, Espers e Chefes Interdimensionais.")
    );
    private IvalicePlayerData.MarkHuntEntry hunt;
    private record Entry(String label, Runnable action) {}

    public IvaliceHubScreen(int initialTab) {
        super("Crônicas de Ivalice");
        page = switch (initialTab) {
            case 0 -> Page.TRAVEL;
            case 1 -> Page.BESTIARY;
            case 2 -> Page.JOURNAL;
            case 3 -> Page.CHARACTER;
            case 4 -> Page.BAZAAR;
            default -> Page.ROOT;
        };
    }

    private CompoundTag profile() {
        return ClientScreenHelper.profile();
    }

    private boolean listed(String key, String value) {
        return profile().getListOrEmpty(key).contains(StringTag.valueOf(value));
    }

    @Override
    protected void init() {
        layout(278, 133, false);
        List<Entry> entries = entries();
        int count = Math.max(1, (entries.size() + 5) / 6);
        listPage = Math.clamp(listPage, 0, count - 1);
        for (int i = listPage * 6; i < Math.min(entries.size(), listPage * 6 + 6); i++) {
            Entry entry = entries.get(i);
            int slot = i % 6;
            option(entry.label(), slot % 2, slot / 2, entry.action());
        }
        if (page == Page.HUNT) {
            option("Aceitar caçada", 0, 2, () -> run("ivalice hunt spawn " + hunt.id()));
        }
        if (page == Page.JOB && canChoose(selectedJob)) {
            option("Confirmar profissão", 0, 2, () -> run("ivalice job choose " + selectedJob.id()));
        } else if (page == Page.RESET_JOB) {
            option("Usar Selo Zodiacal", 0, 2, () -> run("ivalice job reset"));
        }
        int foot = panelY + panelHeight - 23;
        if (page != Page.ROOT) {
            button("Voltar", panelX + 9, foot, 62, this::onClose);
        }
        if (listPage > 0) {
            button("‹ Anterior", panelX + panelWidth - 144, foot, 64, () -> turnPage(-1));
        }
        if (listPage + 1 < count) {
            button("Próxima ›", panelX + panelWidth - 73, foot, 64, () -> turnPage(1));
        }
    }

    private List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        switch (page) {
            case ROOT -> {
                entries.add(new Entry("Glossário", () -> open(Page.JOURNAL)));
                entries.add(new Entry("Bestiário", () -> open(Page.BESTIARY)));
                entries.add(new Entry("Personagem", () -> open(Page.CHARACTER)));
                entries.add(new Entry("Viajar", () -> open(Page.TRAVEL)));
                entries.add(new Entry("Alquimia & Bazaar", () -> open(Page.BAZAAR)));
                entries.add(new Entry("Guia de iniciante", () -> open(Page.GUIDE)));
            }
            case GUIDE -> {
                for (GuideTopic topic : GUIDE) {
                    entries.add(new Entry(topic.title(), () -> {
                        guideTopic = topic;
                        open(Page.GUIDE_TOPIC);
                    }));
                }
            }
            case TRAVEL -> {
                for (String[] biome : BIOMES) {
                    entries.add(new Entry(biome[1], () -> run("ivalice tp " + biome[0])));
                }
            }
            case BESTIARY -> {
                for (IvaliceMonsterStats species : IvaliceMonsterStats.ALL_MONSTERS) {
                    if (listed("encountered", species.id())) {
                        entries.add(new Entry(shortName(species), () -> {
                            monster = species;
                            open(Page.MONSTER);
                        }));
                    }
                }
            }
            case JOURNAL -> {
                for (var entry : IvalicePlayerData.MARK_HUNTS) {
                    String mark = listed("hunts", entry.id()) ? "✓ " : "";
                    entries.add(new Entry(mark + entry.markName(), () -> {
                        hunt = entry;
                        open(Page.HUNT);
                    }));
                }
            }
            case CHARACTER -> {
                entries.add(new Entry("Profissões", () -> open(Page.JOBS)));
                entries.add(new Entry("Licenças e atributos", () -> open(Page.LICENSES)));
                entries.add(new Entry("Equipar itens", ClientScreenHelper::openEquipment));
                entries.add(new Entry(profile().getBooleanOr("gambits", false) ? "Desativar Gambits" : "Ativar Gambits",
                        () -> run("ivalice gambit toggle")));
            }
            case JOBS -> {
                for (ZodiacJob job : ZodiacJob.values()) {
                    String chosen = listed("jobs", job.id()) ? "✓ " : "";
                    entries.add(new Entry(chosen + job.label, () -> {
                        selectedJob = job;
                        open(Page.JOB);
                    }));
                }
                if (!profile().getListOrEmpty("jobs").isEmpty()) {
                    entries.add(new Entry("Reiniciar profissões", () -> open(Page.RESET_JOB)));
                }
            }
            case LICENSES -> {
                for (var node : IvalicePlayerData.LICENSE_CATALOG) {
                    boolean unlocked = listed("licenses", node.id());
                    entries.add(new Entry(unlocked ? "✓ " + node.name() : node.lpCost() + " LP • " + node.name(),
                            () -> { if (!unlocked) run("ivalice license unlock " + node.id()); }));
                }
            }
            case BAZAAR -> {
                for (var pack : IvalicePlayerData.BAZAAR_PACKAGES) {
                    String cost = pack.requiresLoot() ? "Espólios" : pack.gilCost() + " Gil";
                    entries.add(new Entry(pack.rewardItemId().replace('_', ' ') + " • " + cost,
                            () -> run("ivalice bazaar buy " + pack.id())));
                }
            }
            default -> {}
        }
        return entries;
    }

    private boolean canChoose(ZodiacJob job) {
        int slots = profile().getIntOr("level", 1) >= ZodiacJobs.SECOND_JOB_LEVEL ? 2 : 1;
        return !listed("jobs", job.id()) && profile().getListOrEmpty("jobs").size() < slots;
    }

    private String jobSummary() {
        List<String> names = new ArrayList<>();
        for (ZodiacJob job : ZodiacJob.values()) {
            if (listed("jobs", job.id())) names.add(job.label);
        }
        return names.isEmpty() ? "Escolha sua primeira profissão" : String.join(" + ", names);
    }

    private void open(Page next) {
        page = next;
        listPage = 0;
        rebuildWidgets();
    }

    private void turnPage(int offset) {
        listPage += offset;
        rebuildWidgets();
    }

    private void run(String command) {
        super.onClose();
        ClientScreenHelper.command(command);
    }

    @Override
    public void onClose() {
        switch (page) {
            case ROOT -> super.onClose();
            case MONSTER -> open(Page.BESTIARY);
            case HUNT -> open(Page.JOURNAL);
            case GUIDE_TOPIC -> open(Page.GUIDE);
            case LICENSES, JOBS -> open(Page.CHARACTER);
            case JOB, RESET_JOB -> open(Page.JOBS);
            default -> open(Page.ROOT);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        CompoundTag data = profile();
        String heading = switch (page) {
            case ROOT -> "Crônicas de Ivalice";
            case TRAVEL -> "Cristais de viagem";
            case BESTIARY -> "Bestiário do Clã";
            case JOURNAL -> "Glossário • Quadro de caçadas";
            case CHARACTER -> "Personagem";
            case LICENSES -> "Licenças e atributos";
            case JOBS -> "Profissões do Zodíaco";
            case JOB -> selectedJob.label;
            case RESET_JOB -> "Reiniciar profissões";
            case BAZAAR -> "Alquimia & Bazaar de Rabanastre";
            case MONSTER -> shortName(monster);
            case HUNT -> hunt.markName();
            case GUIDE -> "Guia de iniciante";
            case GUIDE_TOPIC -> guideTopic.title();
        };
        String stats = "Nível " + data.getIntOr("level", 1) + "/99   •   "
                + data.getIntOr("gil", 0) + " Gil   •   " + data.getIntOr("lp", 0) + " LP";
        String subtitle = switch (page) {
            case ROOT, CHARACTER, LICENSES -> stats;
            case JOBS -> jobSummary();
            case JOB -> listed("jobs", selectedJob.id()) ? "Profissão ativa" : "Especialidade";
            case RESET_JOB -> "Consome um Selo Zodiacal";
            case TRAVEL -> "Escolha uma região";
            case BESTIARY -> data.getListOrEmpty("encountered").size() + " espécies encontradas";
            case JOURNAL -> data.getListOrEmpty("hunts").size() + " caçadas concluídas";
            case BAZAAR -> data.getIntOr("gil", 0) + " Gil • Também aceita espólios";
            case MONSTER -> monster.family() + " • Nível " + monster.level();
            case HUNT -> hunt.rank() + " • " + hunt.petitioner();
            case GUIDE -> "Escolha um assunto";
            case GUIDE_TOPIC -> "Guia de iniciante";
        };
        drawPanel(graphics, heading, subtitle);
        if (page == Page.GUIDE_TOPIC) {
            int y = 42;
            for (var part : font.split(net.minecraft.network.chat.Component.literal(guideTopic.text()), panelWidth - 18)) {
                graphics.text(font, part, panelX + 9, panelY + y, TEXT);
                y += 11;
            }
        } else if (page == Page.JOB) {
            int y = 42;
            for (var part : font.split(net.minecraft.network.chat.Component.literal(selectedJob.specialty), panelWidth - 18)) {
                graphics.text(font, part, panelX + 9, panelY + y, TEXT);
                y += 11;
            }
            if (!canChoose(selectedJob) && !listed("jobs", selectedJob.id())) {
                line(graphics, "Segunda vaga no nível " + ZodiacJobs.SECOND_JOB_LEVEL + " • Escolhas fixas.", 72, MUTED);
            }
        } else if (page == Page.RESET_JOB) {
            line(graphics, "Reinicia as profissões e o tabuleiro de licenças.", 42, TEXT);
            line(graphics, "Mantém nível, Gil, LP atuais e Bestiário.", 55, MUTED);
        } else if (page == Page.MONSTER) {
            int kills = data.getCompoundOrEmpty("kills").getIntOr(monster.id(), 0);
            line(graphics, "Habitat: " + monster.habitat(), 41, TEXT);
            line(graphics, "Abates: " + kills + "   •   HP base: " + (int) monster.maxHealth(), 56, TEXT);
            line(graphics, "Fraqueza: " + monster.weakness() + "   •   Resistência: " + monster.resistance(), 71, TEXT);
        } else if (page == Page.HUNT) {
            line(graphics, "Região: " + hunt.biomeId().replace('_', ' '), 39, TEXT);
            line(graphics, "+" + hunt.gilReward() + " Gil   •   +" + hunt.lpReward() + " LP", 52, ACCENT);
            line(graphics, listed("hunts", hunt.id()) ? "Concluída" : "Disponível", 65, TEXT);
        } else if (page == Page.BESTIARY && data.getListOrEmpty("encountered").isEmpty()) {
            line(graphics, "Explore os biomas para registrar suas primeiras criaturas.", 49, TEXT);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    public void refresh() {
        rebuildWidgets();
    }

    private static String shortName(IvaliceMonsterStats monster) {
        String name = monster.displayName();
        int annotation = name.indexOf(" (");
        return annotation < 0 ? name : name.substring(0, annotation);
    }
}
