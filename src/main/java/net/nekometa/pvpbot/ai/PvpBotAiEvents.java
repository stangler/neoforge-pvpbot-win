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

        // 基本はプレイヤーを追跡。各種戦術動作は追跡の上で追加される。
        tickChase(bot, nearestPlayer, state);

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
     * プレイヤーの方を向き、積極的に距離を詰める。
     * 攻撃間合い内でも小さく左右にステップし、プレイヤーの照準を外させる。
     * 離れているほど加速し、空中でも少しだけ前進する。
     */
    private static void tickChase(LivingEntity bot, Player player, BotAiState state) {
        double distSqr = bot.distanceToSqr(player);
        facePlayer(bot, player);

        if (distSqr <= MELEE_RANGE_SQR) {
            // 間合い内：激しく小さく左右ステップ（照準を外させ、連打を避ける）
            if (bot.onGround() && state.hitCount < 1
                    && bot.getRandom().nextDouble() < net.nekometa.pvpbot.Config.AI_RANDOM_DODGE_CHANCE.get()) {
                double strafeSpeed = net.nekometa.pvpbot.Config.AI_STRAFE_SPEED.get() * 0.7D;
                double direction = bot.getRandom().nextBoolean() ? strafeSpeed : -strafeSpeed;
                sideStepTowards(bot, player, direction);
            }
            return;
        }

        // 間合い外：プレイヤー方向へ前進して距離を詰める
        Vec3 forward = forwardVector(bot.getYRot());
        double baseChaseSpeed = net.nekometa.pvpbot.Config.AI_CHASE_SPEED.get();
        // 離れているほど加速（最大1.5倍）
        double accel = distSqr > CHASE_ACCEL_RANGE_SQR ? 1.5D
                : 1.0D + 0.5D * (distSqr - MELEE_RANGE_SQR) / (CHASE_ACCEL_RANGE_SQR - MELEE_RANGE_SQR);
        double chaseSpeed = Math.min(baseChaseSpeed * accel, MAX_CHASE_SPEED);

        double destX = bot.getX() + forward.x * chaseSpeed;
        double destZ = bot.getZ() + forward.z * chaseSpeed;
        if (isSafeSideStepTarget(bot.level(), destX, bot.getY(), destZ)) {
            Vec3 motion = bot.getDeltaMovement();
            if (bot.onGround()) {
                // 地上：スムーズに加速
                bot.setDeltaMovement(forward.x * chaseSpeed, motion.y, forward.z * chaseSpeed);
            } else {
                // 空中でも少し前進（完全に止まらない）
                double airControl = 0.15D;
                bot.setDeltaMovement(
                        motion.x + forward.x * airControl,
                        motion.y,
                        motion.z + forward.z * airControl);
            }
        }
    }

    private static void facePlayer(LivingEntity bot, Player player) {
        double dx = player.getX() - bot.getX();
        double dz = player.getZ() - bot.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        bot.setYRot(yaw);
        bot.setYHeadRot(yaw);
    }

    /**
     * ai:strafe + ai:strafe-c
     * 65tickで一周するタイマー。前半(0-32)は左、後半(32-64)は右へサイドステップする。
     * プレイヤーが空中の時は大きく、地上の時でも小さくステップする。
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
        double distSqr = bot.distanceToSqr(player);

        if (!botOnGround || !noRecentHit) {
            return;
        }

        double strafeSpeed = net.nekometa.pvpbot.Config.AI_STRAFE_SPEED.get();
        if (distSqr <= MELEE_RANGE_SQR) {
            // 間合い内：小刻みに動いて照準を外す
            if (bot.getRandom().nextDouble() < 0.35D) {
                boolean forward = state.strafeTimer <= 32;
                sideStepTowards(bot, player, forward ? -strafeSpeed * 0.5D : strafeSpeed * 0.5D);
            }
            return;
        }

        // 間合い外：プレイヤーが空中なら大きく、地上でも小さく動く
        if (playerAirborne) {
            boolean forward = state.strafeTimer <= 32;
            sideStepTowards(bot, player, forward ? -strafeSpeed : strafeSpeed);
        } else if (bot.getRandom().nextDouble() < 0.25D) {
            boolean forward = state.strafeTimer <= 32;
            sideStepTowards(bot, player, forward ? -strafeSpeed * 0.35D : strafeSpeed * 0.35D);
        }
    }

    /**
     * プレイヤーの滞空状態に関係なく、一定確率でランダムな向きへ小さく回避ステップする。
     * ai:strafe はプレイヤーが空中の時しか発動しないため、これを補い常に多少の
     * 予測不能な動きを出すための追加処理(元データパックには存在しない拡張)。
     *
     * 間合い内では動きを小さくして攻撃を妨害しない。
     */
    private static final double MELEE_RANGE_SQR = 2.5D * 2.5D;
    private static final double CHASE_ACCEL_RANGE_SQR = 6.0D * 6.0D;
    private static final double MAX_CHASE_SPEED = 0.9D;

    private static void tickRandomDodge(LivingEntity bot, Player player, BotAiState state) {
        double dodgeChance = net.nekometa.pvpbot.Config.AI_RANDOM_DODGE_CHANCE.get();
        if (dodgeChance <= 0.0D) {
            return;
        }
        if (!bot.onGround() || state.hitCount >= 1) {
            return;
        }
        if (bot.getRandom().nextDouble() >= dodgeChance) {
            return;
        }
        double strafeSpeed = net.nekometa.pvpbot.Config.AI_STRAFE_SPEED.get();
        double distSqr = bot.distanceToSqr(player);
        if (distSqr <= MELEE_RANGE_SQR) {
            // 間合い内: 小さく動くだけ（攻撃を妨げない）
            strafeSpeed *= 0.35D;
        } else if (distSqr <= CHASE_ACCEL_RANGE_SQR) {
            // 中距離: やや小さく
            strafeSpeed *= 0.7D;
        }
        double direction = bot.getRandom().nextBoolean() ? strafeSpeed : -strafeSpeed;
        sideStepTowards(bot, player, direction);
    }

    /**
     * ai:wtap
     * プレイヤーが滞空中なら移動量をゼロ化(その場停止)し、
     * 距離に応じて素早く距離を詰める。地上でもプレイヤーが後退している場合は追撃する。
     */
    private static void tickWtap(LivingEntity bot, Player player, BotAiState state) {
        boolean playerAirborne = !player.onGround();
        double dist = bot.distanceTo(player);

        if (playerAirborne) {
            // プレイヤーが空中の時：WTAP。近接間合い内だけ一瞬止まり、
            // それ以外では前進して距離を詰め続ける。
            if (dist <= 2.5D) {
                bot.setDeltaMovement(Vec3.ZERO);
            } else {
                double wtapClose = Math.min(dist, 6.0D);
                double step = 0.12D * wtapClose;
                Vec3 forward = forwardVector(bot.getYRot());
                double destX = bot.getX() + forward.x * step;
                double destY = bot.getY();
                double destZ = bot.getZ() + forward.z * step;
                if (isSafeSideStepTarget(bot.level(), destX, destY, destZ)) {
                    bot.teleportTo(destX, destY, destZ);
                }
            }
        } else if (bot.onGround() && dist > 2.5D && dist <= 5.0D) {
            // プレイヤーが地上で少し離れている：すぐに追いつく
            Vec3 forward = forwardVector(bot.getYRot());
            double step = 0.25D;
            double destX = bot.getX() + forward.x * step;
            double destY = bot.getY();
            double destZ = bot.getZ() + forward.z * step;
            if (isSafeSideStepTarget(bot.level(), destX, destY, destZ)) {
                bot.teleportTo(destX, destY, destZ);
            }
        }
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
     * プレイヤーが近接〜中距離(0.8〜4.0ブロック)以内、自分が地上、一定確率で
     * 上方向へ跳ねてクリティカル判定を狙う。距離が近いほど発動しやすい。
     */
    private static void tickCritJumpTrigger(LivingEntity bot, Player player, BotAiState state, ServerLevel level) {
        double distSqr = bot.distanceToSqr(player);
        boolean inRange = distSqr >= (0.8D * 0.8D) && distSqr <= (4.0D * 4.0D);
        if (!bot.onGround() || !inRange) {
            return;
        }
        // 近いほどクリティカルを狙いやすい
        double distanceFactor = 1.0D - (Math.sqrt(distSqr) - 0.8D) / 3.2D;
        double adjustedChance = state.critChance * distanceFactor;
        if (bot.getRandom().nextDouble() < adjustedChance) {
            Vec3 motion = bot.getDeltaMovement();
            bot.setDeltaMovement(motion.x, 0.42D, motion.z);
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
