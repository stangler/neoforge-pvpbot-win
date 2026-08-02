package net.nekometa.pvpbot.ai;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 元データパック `data/ai/function/*.mcfunction` の移植。
 *
 * 対応関係:
 *   ai:strafe, ai:strafe-c        -> tickStrafe()
 *   ai:wtap                       -> tickWtap()
 *   ai:jumpreset, ai:botas,
 *   ai:jumpresetact               -> tickJumpReset()
 *   code:main/asbot の crit判定    -> tickCritJumpTrigger()
 *   ai:crit                       -> applyCritEffects()
 *   code:bothurt                  -> onBotHurtByPlayer()
 *   code:vsfx/playerhurt          -> onPlayerHurtByBot()
 *
 * 注意点(元mcfunctionのまま移植できず判断が必要だった箇所):
 *   - "on-air" predicate は本来「足元ブロックがair」の判定だが、Java側では
 *     LivingEntity#onGround() の否定で近似している。地面判定の細かい挙動
 *     (半ブロックの上に立っている等)が完全に一致しない可能性がある。
 *   - "jumptest2"(custom:jump統計をplayer着地時に0リセットする自作フラグ)は
 *     「プレイヤーが直近でジャンプしてから着地していない」を表す。ここでは
 *     Player#onGround() の否定で近似している(通常のジャンプ以外での滞空も
 *     含んでしまう点は元実装との差異)。
 *   - vsfx/playerhurt の「crit sign有効時、非クリティカルヒットは
 *     attack_damage base を 0.0001 に下げる」処理は、元に戻す仕組みが
 *     元データパック内に見当たらず、一度発動すると攻撃力が恒久的にほぼ0に
 *     なる致命的なバグだったため、Java版では移植せず削除した。
 */
@EventBusSubscriber(modid = "pvpbot")
public final class PvpBotAiEvents {

    private static final Identifier CRIT_DAMAGE_MODIFIER_ID =
            Identifier.fromNamespaceAndPath("pvpbot", "crit_beast_bonus");

    private PvpBotAiEvents() {
    }

    // ================================================================
    // 毎tick処理 (code:main/main -> code:main/asbot に相当)
    // ================================================================

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity bot)) {
            return;
        }
        if (!isBot(bot) || !(bot.level() instanceof ServerLevel level)) {
            return;
        }

        BotAiState state = bot.getData(BotAiAttachments.BOT_AI_STATE.get());

        Player nearestPlayer = level.getNearestPlayer(bot, 32.0D);
        if (nearestPlayer == null) {
            return;
        }

        if (state.strafesEnabled) {
            tickStrafe(bot, nearestPlayer, state);
            tickRandomDodge(bot, nearestPlayer, state);
        }
        if (state.wtapEnabled) {
            tickWtap(bot, nearestPlayer, state);
        }
        if (state.jumpresLevel > 0) {
            tickJumpReset(bot, state);
        }
        if (state.critEnabled) {
            tickCritJumpTrigger(bot, nearestPlayer, state, level);
        }
    }

    /**
     * ai:strafe + ai:strafe-c
     * 65tickで一周するタイマー。前半(0-32)は左、後半(32-64)は右へ0.2ブロックずつ
     * サイドステップする。ただし「自分が地上」「直近ヒットなし」
     * 「プレイヤーが滞空中」「プレイヤーが直近ジャンプ済でない」の全条件を満たす時のみ。
     */
    private static void tickStrafe(LivingEntity bot, Player player, BotAiState state) {
        state.strafeTimer++;
        if (state.strafeTimer >= 65) {
            state.strafeTimer = 0;
        }
        // 不規則な方向転換: 一定確率でタイマーをランダムな値へ飛ばし、
        // 綺麗な左右交互パターンを崩して予測しづらくする。
        double erraticChance = net.nekometa.pvpbot.Config.AI_ERRATIC_CHANCE.get();
        if (erraticChance > 0.0D && bot.getRandom().nextDouble() < erraticChance) {
            state.strafeTimer = bot.getRandom().nextInt(65);
        }

        boolean botOnGround = bot.onGround();
        boolean noRecentHit = state.hitCount < 1;
        boolean playerAirborne = !player.onGround();
        boolean playerNotFreshlyJumped = player.onGround(); // jumptest2近似(下記コメント参照)

        // 元: unless predicate code:on-air unless score %hitcount var matches 1..
        //     if entity @p[predicate=code:on-air] unless score @p jumptest2 matches 1
        // "unless jumptest2 matches 1" は「ジャンプ直後でない」の意だが、
        // Java版では簡略化のため player.onGround() で近似している(要調整)。
        if (botOnGround && noRecentHit && playerAirborne && bot.distanceToSqr(player) > MELEE_RANGE_SQR) {
            boolean forward = state.strafeTimer <= 32;
            double strafeSpeed = net.nekometa.pvpbot.Config.AI_STRAFE_SPEED.get();
            sideStepTowards(bot, player, forward ? -strafeSpeed : strafeSpeed);
        }
    }

    /**
     * プレイヤーの滞空状態に関係なく、一定確率でランダムな向きへ小さく回避ステップする。
     * ai:strafe はプレイヤーが空中の時しか発動しないため、これを補い常に多少の
     * 予測不能な動きを出すための追加処理(元データパックには存在しない拡張)。
     *
     * 近接攻撃の間合い内では発動させない。sideStepTowards は瞬間移動(teleportTo)
     * のため、攻撃直前にワープすると近接リーチから外れてヒットしなくなるバグを防ぐ。
     */
    private static final double MELEE_RANGE_SQR = 2.5D * 2.5D;

    private static void tickRandomDodge(LivingEntity bot, Player player, BotAiState state) {
        double dodgeChance = net.nekometa.pvpbot.Config.AI_RANDOM_DODGE_CHANCE.get();
        if (dodgeChance <= 0.0D) {
            return;
        }
        if (!bot.onGround() || state.hitCount >= 1) {
            return;
        }
        if (bot.distanceToSqr(player) <= MELEE_RANGE_SQR) {
            return; // 攻撃間合い内: 回避で攻撃を妨げない
        }
        if (bot.getRandom().nextDouble() >= dodgeChance) {
            return;
        }
        double strafeSpeed = net.nekometa.pvpbot.Config.AI_STRAFE_SPEED.get();
        double direction = bot.getRandom().nextBoolean() ? strafeSpeed : -strafeSpeed;
        sideStepTowards(bot, player, direction);
    }

    /**
     * ai:wtap
     * プレイヤーが滞空中なら移動量をゼロ化(その場停止)し、
     * 距離4以内ならさらに少し前進(^ ^ ^0.17)して距離を詰める。
     * 加えて、ブロックにめり込んで動けなくなった場合の脱出処理と、
     * プレイヤーのjumptest2フラグの着地時リセットを行う。
     */
    private static void tickWtap(LivingEntity bot, Player player, BotAiState state) {
        boolean playerAirborne = !player.onGround();

        if (playerAirborne) {
            bot.setDeltaMovement(Vec3.ZERO);
            if (bot.distanceTo(player) <= 4.0D) {
                Vec3 forward = forwardVector(bot.getYRot());
                double destX = bot.getX() + forward.x * 0.17D;
                double destY = bot.getY();
                double destZ = bot.getZ() + forward.z * 0.17D;
                if (isSafeSideStepTarget(bot.level(), destX, destY, destZ)) {
                    bot.teleportTo(destX, destY, destZ);
                }
            }
        }

        // アンチ・埋まり込み(元: unless block ~ ~ ~ air run tp @s ~ ~0.1 ~)
        // Java側では「立っていない(空中でもない=ブロック内にいる)」ケースの厳密な
        // ブロック判定が煩雑なため、ひとまず省略。詰まる不具合が確認された場合に追加する。

        // jumptest2の着地時リセットは player.onGround() を直接使う近似のため、
        // Java側では専用のリセット処理は不要(状態を保持していないため)。
    }

    /**
     * ai:jumpreset, ai:botas, ai:jumpresetact
     * 直近でヒットを受けていれば knockback_resistance を一時的に大きく上げて
     * ノックバックを無効化し(=ジャンプリセット)、そのあとさらにヒットを受けたら
     * 通常よりやや高い値まで戻す。発動確率は難易度(jumpresLevel)に応じて変化。
     */
    private static void tickJumpReset(LivingEntity bot, BotAiState state) {
        double chance = switch (state.jumpresLevel) {
            case 1 -> 0.15D;
            case 2 -> 0.30D;
            case 3 -> 0.50D;
            case 4 -> 0.70D;
            case 5 -> 0.85D;
            default -> 1.0D; // 6 = 常時
        };
        if (bot.getRandom().nextDouble() > chance) {
            return;
        }

        // ai:jumpresetact
        state.jumpResetActive = state.hitCount >= 1;
        if (!state.jumpResetActive) {
            state.jumpResetHitCount = 0;
        }

        var attr = bot.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attr == null) {
            return;
        }
        if (state.jumpResetActive) {
            attr.setBaseValue(3.9D);
        }
        if (state.jumpResetHitCount >= 1) {
            attr.setBaseValue(0.187D);
        }
    }

    /**
     * code:main/asbot のcrit判定部分:
     * プレイヤーが1.2〜2.8ブロック以内、自分が地上、15%抽選に当たったら
     * 上方向へ跳ねて(Motion.y=0.4)クリティカル判定を狙う。
     */
    private static void tickCritJumpTrigger(LivingEntity bot, Player player, BotAiState state, ServerLevel level) {
        double distSqr = bot.distanceToSqr(player);
        boolean inRange = distSqr >= (1.2D * 1.2D) && distSqr <= (2.8D * 2.8D);
        if (bot.onGround() && inRange && bot.getRandom().nextDouble() < state.critChance) {
            Vec3 motion = bot.getDeltaMovement();
            bot.setDeltaMovement(motion.x, 0.4D, motion.z);
        }
    }

    // ================================================================
    // ダメージイベント (code:bothurt / code:vsfx/playerhurt に相当)
    // ================================================================

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();

        if (isBot(victim) && source.getEntity() instanceof Player) {
            onBotHurtByPlayer(victim);
        } else if (victim instanceof ServerPlayer player && isBot(source.getEntity())) {
            onPlayerHurtByBot(player, (LivingEntity) source.getEntity());
        }
    }

    /** code:bothurt: プレイヤーがボットを殴った瞬間の処理。 */
    private static void onBotHurtByPlayer(LivingEntity bot) {
        BotAiState state = bot.getData(BotAiAttachments.BOT_AI_STATE.get());
        state.hitCount++;
        if (state.jumpResetActive) {
            state.jumpResetHitCount++;
        }
    }

    /** code:vsfx/playerhurt: ボットがプレイヤーを殴った瞬間の処理。 */
    private static void onPlayerHurtByBot(ServerPlayer player, LivingEntity bot) {
        BotAiState state = bot.getData(BotAiAttachments.BOT_AI_STATE.get());
        // ヒットが成立した = 直近ヒットカウントをリセット
        state.hitCount = 0;
        state.jumpResetHitCount = 0;

        if (!state.critEnabled) {
            return;
        }

        boolean botAirborne = !bot.onGround();
        if (botAirborne) {
            applyCritEffects(bot, player, state);
        }
        // 元データパックには「非クリティカルヒット時に attack_damage base を
        // 0.0001まで下げる」処理があったが、それを元に戻す仕組みがどこにも
        // 存在せず、一度非クリティカルヒットが発生すると以降ずっとほぼ0ダメージ
        // になってしまう致命的なバグだったため、この減衰処理は移植しない。
    }

    /** ai:crit: クリティカルヒットの演出とダメージ補正。 */
    private static void applyCritEffects(LivingEntity bot, ServerPlayer player, BotAiState state) {
        if (bot.level() instanceof ServerLevel level) {
            level.playSound(null, bot.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_CRIT,
                    net.minecraft.sounds.SoundSource.MASTER, 2.0F, 1.0F);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                    bot.getX(), bot.getY() + 1.0D, bot.getZ(),
                    35, 0.4D, 0.8D, 0.4D, 0.06D);
        }

        var attr = bot.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr == null) {
            return;
        }

        // クリティット発生時にダメージ+2.0(ビーストモード時)を付与。
        // 毎回 remove → add する方式で、critDamageApplied フラグの
        // バグ(通常モードで2回目で付与が止まる、ビースト→通常で残る等)を回避する。
        attr.removeModifier(CRIT_DAMAGE_MODIFIER_ID);
        if (state.beastMode) {
            attr.addPermanentModifier(new AttributeModifier(
                    CRIT_DAMAGE_MODIFIER_ID, 2.0D, AttributeModifier.Operation.ADD_VALUE));
        }
        state.critDamageApplied = true;
    }

    // ================================================================
    // ユーティリティ
    // ================================================================

    private static boolean isBot(net.minecraft.world.entity.Entity entity) {
        return entity instanceof LivingEntity le && le.entityTags().contains("bot");
    }

    /** Minecraftのyaw(度)から水平方向の前方ベクトルを求める。 */
    private static Vec3 forwardVector(float yawDegrees) {
        double yawRad = Math.toRadians(yawDegrees);
        double x = -Mth.sin((float) yawRad);
        double z = Mth.cos((float) yawRad);
        return new Vec3(x, 0.0D, z);
    }

    /**
     * `tp @s ^-.2 ^ ^ facing entity @p` に相当:
     * まずプレイヤーの方を向かせ、その向きを基準にした左右(側面)方向へ
     * offset分だけテレポートする。前後・上下は変化させない。
     *
     * teleportTo は当たり判定を無視するため、壁際でそのまま使うとガラス壁を
     * すり抜けてアリーナ外の空中に出てしまうバグがあった。移動先が
     * 「床がある・障害物がない」安全な位置かどうかを事前に確認し、
     * 危険な場合は移動をキャンセルする。
     */
    private static void sideStepTowards(LivingEntity bot, Player player, double sidewaysOffset) {
        double dx = player.getX() - bot.getX();
        double dz = player.getZ() - bot.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        bot.setYRot(yaw);
        bot.setYHeadRot(yaw);

        Vec3 forward = forwardVector(yaw);
        // 前方ベクトルを90度回転させて右方向ベクトルを得る
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);

        double destX = bot.getX() + right.x * sidewaysOffset;
        double destY = bot.getY();
        double destZ = bot.getZ() + right.z * sidewaysOffset;

        if (!isSafeSideStepTarget(bot.level(), destX, destY, destZ)) {
            return; // 壁の中・アリーナ外(床なし)には移動させない
        }

        bot.teleportTo(destX, destY, destZ);
    }

    /** 移動先の足元が床あり・体の位置が空気(通行可能)であることを確認する。 */
    private static boolean isSafeSideStepTarget(net.minecraft.world.level.Level level, double x, double y, double z) {
        net.minecraft.core.BlockPos feet = net.minecraft.core.BlockPos.containing(x, y, z);
        net.minecraft.core.BlockPos below = feet.below();
        return level.getBlockState(feet).isAir()
                && level.getBlockState(feet.above()).isAir()
                && !level.getBlockState(below).isAir();
    }
}
