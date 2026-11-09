package com.ivalice.job;

import com.ivalice.item.IvaliceItemCategory;

import java.util.Locale;
import java.util.Set;

import static com.ivalice.item.IvaliceItemCategory.*;

public enum ZodiacJob {
    KNIGHT("Cavaleiro", "Espadas, escudos e armadura pesada", 2, 1, 0, 0,
            WEAPON_SWORD, WEAPON_GREATSWORD, DEFENSE_SHIELD, DEFENSE_HEAVY_ARMOR),
    UHLAN("Uhlan", "Lanças e armadura pesada", 1, 2, 0, 0,
            WEAPON_SPEAR, DEFENSE_HEAVY_ARMOR),
    MACHINIST("Maquinista", "Armas de fogo, medidas e armadura leve", 0, 1, 0.01, 0,
            WEAPON_GUN, WEAPON_MEASURE, DEFENSE_LIGHT_ARMOR),
    RED_BATTLEMAGE("Mago de Batalha Vermelho", "Magia branca e negra, escudos e vestes místicas", 1, 0, 0, 1,
            WEAPON_GREATSWORD, WEAPON_STAFF, MAGICK_WHITE, MAGICK_BLACK, DEFENSE_MYSTIC_ARMOR, DEFENSE_SHIELD),
    WHITE_MAGE("Mago Branco", "Cetros, vestes místicas e magia branca", 0, 0, 0, 2,
            WEAPON_ROD, DEFENSE_MYSTIC_ARMOR, MAGICK_WHITE),
    BLACK_MAGE("Mago Negro", "Cajados, vestes místicas e magia negra", 0, 0, 0, 3,
            WEAPON_STAFF, DEFENSE_MYSTIC_ARMOR, MAGICK_BLACK),
    MONK("Monge", "Bastões e armadura leve", 0, 2, 0.01, 0,
            WEAPON_POLE, DEFENSE_LIGHT_ARMOR),
    TIME_BATTLEMAGE("Mago do Tempo", "Bestas, armadura pesada e magia do tempo", 1, 0, 0.01, 1,
            WEAPON_CROSSBOW, DEFENSE_HEAVY_ARMOR, MAGICK_TIME_ARCANE),
    FOEBREAKER("Quebrador", "Machados, bombas de mão, escudos e armadura pesada", 2, 2, 0, 0,
            WEAPON_AXE_HAMMER, WEAPON_HAND_BOMB, DEFENSE_SHIELD, DEFENSE_HEAVY_ARMOR),
    ARCHER("Arqueiro", "Arcos e armadura leve", 0, 1, 0.02, 0,
            WEAPON_BOW, DEFENSE_LIGHT_ARMOR),
    BUSHI("Bushi", "Katanas e vestes místicas", 0, 2, 0, 1,
            WEAPON_KATANA, DEFENSE_MYSTIC_ARMOR),
    SHIKARI("Shikari", "Adagas, escudos e armadura leve", 1, 1, 0.02, 0,
            WEAPON_DAGGER, DEFENSE_SHIELD, DEFENSE_LIGHT_ARMOR);

    public final String label;
    public final String specialty;
    public final double armor;
    public final double attack;
    public final double speed;
    public final int magick;
    private final Set<IvaliceItemCategory> categories;

    ZodiacJob(String label, String specialty, double armor, double attack, double speed, int magick,
              IvaliceItemCategory... categories) {
        this.label = label;
        this.specialty = specialty;
        this.armor = armor;
        this.attack = attack;
        this.speed = speed;
        this.magick = magick;
        this.categories = Set.of(categories);
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean permits(IvaliceItemCategory category) {
        return categories.contains(category);
    }

    public static ZodiacJob byId(String id) {
        for (ZodiacJob job : values()) {
            if (job.id().equals(id)) return job;
        }
        return null;
    }
}
