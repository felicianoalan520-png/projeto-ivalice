package com.ivalice.item;

import net.minecraft.ChatFormatting;

/**
 * Categorias canônicas de itens e equipamentos de Final Fantasy XII: The Zodiac Age.
 */
public enum IvaliceItemCategory {
    WEAPON_SWORD("Armas: Espada (Sword)", ChatFormatting.GOLD),
    WEAPON_GREATSWORD("Armas: Espada Longa (Greatsword)", ChatFormatting.GOLD),
    WEAPON_DAGGER("Armas: Adaga / Ninja (Dagger)", ChatFormatting.YELLOW),
    WEAPON_SPEAR("Armas: Lança (Spear)", ChatFormatting.GOLD),
    WEAPON_POLE("Armas: Bastão de Monge (Pole)", ChatFormatting.GOLD),
    WEAPON_KATANA("Armas: Katana", ChatFormatting.RED),
    WEAPON_BOW("Armas: Arco (Bow)", ChatFormatting.GREEN),
    WEAPON_CROSSBOW("Armas: Besta (Crossbow)", ChatFormatting.GREEN),
    WEAPON_GUN("Armas: Arma de Fogo (Gun)", ChatFormatting.GRAY),
    WEAPON_AXE_HAMMER("Armas: Machado / Martelo (Axe & Hammer)", ChatFormatting.GOLD),
    WEAPON_STAFF("Armas: Cajado Arcano (Staff)", ChatFormatting.AQUA),
    WEAPON_ROD("Armas: Cetro Místico (Rod)", ChatFormatting.LIGHT_PURPLE),
    WEAPON_MACE("Armas: Maça (Mace)", ChatFormatting.GOLD),
    WEAPON_MEASURE("Armas: Medida Tática (Measure)", ChatFormatting.AQUA),
    WEAPON_HAND_BOMB("Armas: Bomba de Mão (Hand-bomb)", ChatFormatting.RED),

    DEFENSE_SHIELD("Defesa: Escudo (Shield)", ChatFormatting.BLUE),
    DEFENSE_LIGHT_ARMOR("Defesa: Armadura Leve (Light Armor)", ChatFormatting.GREEN),
    DEFENSE_HEAVY_ARMOR("Defesa: Armadura Pesada (Heavy Armor)", ChatFormatting.BLUE),
    DEFENSE_MYSTIC_ARMOR("Defesa: Armadura Mística (Mystic Armor)", ChatFormatting.LIGHT_PURPLE),
    DEFENSE_ACCESSORY("Defesa: Acessório (Accessory)", ChatFormatting.AQUA),

    AMMUNITION("Munição", ChatFormatting.GRAY),
    CONSUMABLE("Consumível de Ivalice (Consumable)", ChatFormatting.GREEN),
    LOOT_MATERIAL("Espólio / Material de Monstro (Loot)", ChatFormatting.YELLOW),
    KEY_ITEM("Item Especial / Relíquia do Clã (Key Item)", ChatFormatting.GOLD),

    MAGICK_WHITE("Magia Branca (White Magick)", ChatFormatting.AQUA),
    MAGICK_BLACK("Magia Negra (Black Magick)", ChatFormatting.RED),
    MAGICK_TIME_ARCANE("Magia do Tempo / Arcana (Time Magick)", ChatFormatting.LIGHT_PURPLE),
    TECHNICK("Técnica Tática (Technick)", ChatFormatting.GOLD),
    ESPER_GLYPH("Glifo de Esper (Scion of Darkness)", ChatFormatting.RED);

    private final String displayName;
    private final ChatFormatting color;

    IvaliceItemCategory(String displayName, ChatFormatting color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatFormatting getColor() {
        return color;
    }
}
