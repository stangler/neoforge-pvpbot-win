package net.nekometa.pvpbot.fight;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * code:spawn の移植。
 * TODO: %beast sign matches 1/2 (ネザライト装備の強化ボット)は未移植。
 *       まず beast=0(基本ティア)のみ。
 */
public final class BotSpawner {

    private BotSpawner() {
    }

    /** 強さティアごとの基礎攻撃力(base ATTACK_DAMAGE)。 */
    private static double attackDamageForTier(int strengthTier) {
        return switch (strengthTier) {
            case 0 -> 0.5D;
            case 1 -> 0.75D;
            case 3 -> 1.5D;
            case 4 -> 2.5D;
            default -> 1.0D; // 2 = 普通
        };
    }

    /** @return spawnに成功したエンティティ。addFreshEntity失敗時はnull。 */
    public static Zombie spawn(ServerLevel level, Vec3 pos, int armorTier, boolean beastMode, int strengthTier) {
        Zombie bot = EntityType.ZOMBIE.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        if (bot == null) {
            return null;
        }

        // yaw=180: プレイヤー側（-Z方向）を向かせる（アリーナ配置と整合）
        bot.snapTo(pos.x, pos.y, pos.z, 180.0F, 0.0F);
        bot.finalizeSpawn(level, level.getCurrentDifficultyAt(bot.blockPosition()),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, null);
        // finalizeSpawn がランダムでちびゾンビにするのを防ぐ（当たり判定が小さくヒットしづらい）
        bot.setBaby(false);
        bot.setSilent(true);
        bot.setCustomNameVisible(true);
        bot.setCustomName(Component.translatable("pvpbot.bot.name"));
        bot.setPersistenceRequired();
        bot.setCanPickUpLoot(false);
        bot.addTag("bot");
        // 召喚直後の無敵・無反応を避ける
        bot.setNoAi(false);
        bot.setAggressive(true);

        // 装備(TODO: custom_model_data=1 のリソースパック見た目は未対応)
        if (beastMode) {
            ArmorSets.applyBeastGear(bot);
        } else {
            ArmorSets.applyFullSet(bot, armorTier);
            bot.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
            bot.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }

        var followRange = bot.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRange != null) followRange.setBaseValue(net.nekometa.pvpbot.Config.BOT_FOLLOW_RANGE.get());
        var kb = bot.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) kb.setBaseValue(0.187D);
        var speed = bot.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(net.nekometa.pvpbot.Config.BOT_BASE_MOVEMENT_SPEED.get());
        // 元データパックは極小値だが、LivingDamageEvent が発火しないとヒット数も増えないため
        // 最低限のダメージを入れる（クリット補正は AI 側で追加）。難易度(strengthTier)で調整。
        var dmg = bot.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(attackDamageForTier(strengthTier));
        var atkKb = bot.getAttribute(Attributes.ATTACK_KNOCKBACK);
        if (atkKb != null) atkKb.setBaseValue(3.5D);
        // 攻撃リーチをプレイヤー相当に近づける
        var atkRange = bot.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (atkRange != null) atkRange.setBaseValue(3.0D);

        boolean added = level.addFreshEntity(bot);
        if (!added) {
            return null;
        }

        return bot;
    }
}
