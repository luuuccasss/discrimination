package fr.luuuccasss.discrimination;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Ce que les villageois devinent de ta richesse en te regardant : ton armure, et les émeraudes que tu portes. */
public enum SocialClass {
    MISEREUX("miséreux", ChatFormatting.DARK_GRAY),
    MODESTE("modeste", ChatFormatting.GRAY),
    AISE("aisé", ChatFormatting.YELLOW),
    RICHE("riche", ChatFormatting.GOLD);

    public final String label;
    public final ChatFormatting color;

    SocialClass(String label, ChatFormatting color) {
        this.label = label;
        this.color = color;
    }

    public static SocialClass of(ServerPlayer player) {
        int score = wealth(player);
        if (score < 2) return MISEREUX;
        if (score < 10) return MODESTE;
        if (score < 24) return AISE;
        return RICHE;
    }

    /** Une armure complète en cuir vaut 4, en fer 12, en diamant 24, en netherite 40. Plus une par 16 émeraudes. */
    public static int wealth(ServerPlayer player) {
        int score = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) score += armorValue(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
        }
        int emeralds = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.EMERALD)) emeralds += stack.getCount();
            else if (stack.is(Items.EMERALD_BLOCK)) emeralds += stack.getCount() * 9;
        }
        return score + emeralds / 16;
    }

    /** D'après le nom de la pièce : netherite_chestplate, diamond_helmet… */
    private static int armorValue(String id) {
        if (id.startsWith("netherite_")) return 10;
        if (id.startsWith("diamond_")) return 6;
        if (id.startsWith("iron_") || id.startsWith("golden_")) return 3;
        if (id.startsWith("chainmail_")) return 2;
        return 1;
    }
}
