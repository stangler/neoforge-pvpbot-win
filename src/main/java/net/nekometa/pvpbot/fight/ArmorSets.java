package net.nekometa.pvpbot.fight;

import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * code:armorsets/apply_enemy, code:armorsets/apply_player, code:beastgear の移植。
 *
 * 元は看板UIで選ばれた %enemy_armor_set / %player_armor_set (0〜3) に応じて
 * フルセット装備。ここでは整数tier引数として受け取る形にした
 * (サインUI→GUI移植までは呼び出し側で固定値を渡す)。
 */
public final class ArmorSets {

    private ArmorSets() {
    }

    /** 0:革 1:鉄 2:ダイヤ 3:ネザライト */
    public static void applyFullSet(net.minecraft.world.entity.LivingEntity entity, int tier) {
        Item helmet, chest, legs, boots;
        switch (tier) {
            case 0 -> {
                helmet = Items.LEATHER_HELMET; chest = Items.LEATHER_CHESTPLATE;
                legs = Items.LEATHER_LEGGINGS; boots = Items.LEATHER_BOOTS;
            }
            case 1 -> {
                helmet = Items.IRON_HELMET; chest = Items.IRON_CHESTPLATE;
                legs = Items.IRON_LEGGINGS; boots = Items.IRON_BOOTS;
            }
            case 3 -> {
                helmet = Items.NETHERITE_HELMET; chest = Items.NETHERITE_CHESTPLATE;
                legs = Items.NETHERITE_LEGGINGS; boots = Items.NETHERITE_BOOTS;
            }
            default -> { // 2: ダイヤ(未知の値もここにフォールバック)
                helmet = Items.DIAMOND_HELMET; chest = Items.DIAMOND_CHESTPLATE;
                legs = Items.DIAMOND_LEGGINGS; boots = Items.DIAMOND_BOOTS;
            }
        }

        equip(entity, EquipmentSlot.HEAD, helmet);
        equip(entity, EquipmentSlot.CHEST, chest);
        equip(entity, EquipmentSlot.LEGS, legs);
        equip(entity, EquipmentSlot.FEET, boots);
    }

    /** code:beastgear: ダイヤフルセット + 壊れない木の剣。 */
    public static void applyBeastGear(LivingEntity entity) {
        applyFullSet(entity, 2);
        equip(entity, EquipmentSlot.MAINHAND, Items.WOODEN_SWORD);
    }

    private static void equip(LivingEntity entity, EquipmentSlot slot, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        entity.setItemSlot(slot, stack);
        if (entity instanceof Mob mob) {
            mob.setDropChance(slot, 0.0F);
        }
    }
}
