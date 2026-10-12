package fr.luuuccasss.discrimination;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

/** Ce que les villageois devinent de ta richesse en te regardant : ton armure, et les émeraudes que tu portes. */
public enum SocialClass {
    MISEREUX("miséreux", Formatting.DARK_GRAY),
    MODESTE("modeste", Formatting.GRAY),
    AISE("aisé", Formatting.YELLOW),
    RICHE("riche", Formatting.GOLD);

    public final String label;
    public final Formatting color;

    SocialClass(String label, Formatting color) {
        this.label = label;
        this.color = color;
    }

    public static SocialClass of(ServerPlayerEntity player) {
        int score = wealth(player);
        if (score < 2) return MISEREUX;
        if (score < 10) return MODESTE;
        if (score < 24) return AISE;
        return RICHE;
    }

    /** Une armure complète en cuir vaut 4, en fer 12, en diamant 24, en netherite 40. Plus une par 16 émeraudes. */
    public static int wealth(ServerPlayerEntity player) {
        int score = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.getItem() instanceof ArmorItem armor) score += armorValue(armor.getMaterial());
        }
        int emeralds = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.EMERALD)) emeralds += stack.getCount();
            else if (stack.isOf(Items.EMERALD_BLOCK)) emeralds += stack.getCount() * 9;
        }
        return score + emeralds / 16;
    }

    private static int armorValue(RegistryEntry<net.minecraft.item.ArmorMaterial> material) {
        if (material.equals(ArmorMaterials.NETHERITE)) return 10;
        if (material.equals(ArmorMaterials.DIAMOND)) return 6;
        if (material.equals(ArmorMaterials.IRON) || material.equals(ArmorMaterials.GOLD)) return 3;
        if (material.equals(ArmorMaterials.CHAIN)) return 2;
        return 1;
    }
}
