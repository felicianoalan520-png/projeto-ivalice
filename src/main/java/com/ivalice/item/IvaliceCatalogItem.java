package com.ivalice.item;

import com.ivalice.combat.IvaliceEquipment;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Classe base para todos os itens do catálogo de Final Fantasy XII: The Zodiac Age (Etapa 2).
 * Exibe categoria canônica, descrição da obra original, fonte de obtenção (espólio/Bazaar/caçada) e valor em Gil.
 */
public class IvaliceCatalogItem extends Item {
    private final IvaliceItemCategory category;
    private final String descriptionText;
    private final int gilValue;
    private final String obtainSource;
    private final String canonicalRef;
    private final Rarity itemRarity;

    public IvaliceCatalogItem(
            Properties properties,
            IvaliceItemCategory category,
            String descriptionText,
            int gilValue,
            String obtainSource,
            String canonicalRef,
            Rarity itemRarity
    ) {
        super(properties);
        this.category = category;
        this.descriptionText = descriptionText;
        this.gilValue = gilValue;
        this.obtainSource = obtainSource;
        this.canonicalRef = canonicalRef;
        this.itemRarity = itemRarity;
    }

    public IvaliceItemCategory getCategory() {
        return category;
    }

    public String getDescriptionText() {
        return descriptionText;
    }

    public int getGilValue() {
        return gilValue;
    }

    public String getObtainSource() {
        return obtainSource;
    }

    public String getCanonicalRef() {
        return canonicalRef;
    }

    public Rarity getItemRarity() {
        return itemRarity;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!IvaliceEquipment.ranged(stack)) return super.use(level, player, hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (!IvaliceEquipment.useRanged(serverPlayer, stack)) return InteractionResult.FAIL;
            player.getCooldowns().addCooldown(stack, 18);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipAdder,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, flag);
        tooltipAdder.accept(Component.literal("❖ " + category.getDisplayName()).withStyle(category.getColor()));
        if (descriptionText != null && !descriptionText.isEmpty()) {
            tooltipAdder.accept(Component.literal(descriptionText).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        if (obtainSource != null && !obtainSource.isEmpty()) {
            tooltipAdder.accept(Component.literal("Obtenção: " + obtainSource).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (category == IvaliceItemCategory.DEFENSE_ACCESSORY || category == IvaliceItemCategory.DEFENSE_SHIELD) {
            tooltipAdder.accept(Component.literal("Equipe na mão secundária.").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (!IvaliceEquipment.ammoFamily(category).isEmpty()) {
            tooltipAdder.accept(Component.literal("Use com munição compatível no inventário.").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (gilValue > 0) {
            tooltipAdder.accept(Component.literal("Valor em Ivalice: " + gilValue + " Gil").withStyle(ChatFormatting.GOLD));
        }
        if (canonicalRef != null && !canonicalRef.isEmpty()) {
            tooltipAdder.accept(Component.literal("[" + canonicalRef + "]").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
