package net.nekometa.pvpbot;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * PvP Bot 設定（看板相当の記憶）。
 * 防具 / 勝敗方式 / Beast を config に保存し、再起動後も維持する。
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.push("fight");
    }

    /** 0革 1鉄 2ダイヤ 3ネザライト */
    public static final ModConfigSpec.IntValue ARMOR_TIER = BUILDER
            .comment("Default armor tier: 0=leather, 1=iron, 2=diamond, 3=netherite")
            .defineInRange("armorTier", 2, 0, 3);

    /** 0無効 1:50 2:100 3:500 4:1000 */
    public static final ModConfigSpec.IntValue BOXING_MODE = BUILDER
            .comment("Win condition: 0=death, 1=50hits, 2=100, 3=500, 4=1000")
            .defineInRange("boxingMode", 0, 0, 4);

    public static final ModConfigSpec.BooleanValue BEAST_MODE = BUILDER
            .comment("Beast mode (diamond + unbreakable wooden sword)")
            .define("beastMode", false);

    /** 0弱 1易 2普通 3強 4激強。攻撃力・クリット率・ジャンプリセット頻度に影響。 */
    public static final ModConfigSpec.IntValue STRENGTH_TIER = BUILDER
            .comment("Enemy strength tier: 0=weak, 1=easy, 2=normal, 3=strong, 4=nightmare. Affects bot attack damage, crit chance, and knockback-reset frequency")
            .defineInRange("strengthTier", 2, 0, 4);

    static {
        BUILDER.pop();
        BUILDER.push("arena");
    }

    /** アリーナ床の高さ（ワールド上限-16でクランプされる）。 */
    public static final ModConfigSpec.IntValue ARENA_Y = BUILDER
            .comment("Arena floor height (clamped to world max height - 16)")
            .defineInRange("arenaY", 200, -64, 2032);

    /** プラットフォーム半径（床）。 */
    public static final ModConfigSpec.IntValue ARENA_RADIUS = BUILDER
            .comment("Arena platform radius in blocks")
            .defineInRange("arenaRadius", 12, 4, 64);

    /** 外周ガラス壁＋天井の高さ。ノックバックで壁を越えて奈落落下するのを防ぐ。 */
    public static final ModConfigSpec.IntValue ARENA_WALL_HEIGHT = BUILDER
            .comment("Arena wall/ceiling height (glass blocks). A ceiling is added at this height to prevent knockback from launching players over the walls")
            .defineInRange("arenaWallHeight", 5, 3, 12);

    /** プレイヤーとボットの対峙距離（Z オフセット）。 */
    public static final ModConfigSpec.DoubleValue BOT_OFFSET_Z = BUILDER
            .comment("Distance between player and bot spawn point (Z offset)")
            .defineInRange("botOffsetZ", 8.0D, 2.0D, 32.0D);

    /** voidbug: 床よりこの値以上下がったら落下扱い。 */
    public static final ModConfigSpec.DoubleValue VOID_FALL_MARGIN = BUILDER
            .comment("How far below the arena floor counts as falling into the void")
            .defineInRange("voidFallMargin", 10.0D, 1.0D, 64.0D);

    /** effects: プレイヤー速度レベル（0=Speed I）。 */
    public static final ModConfigSpec.IntValue PLAYER_SPEED_AMPLIFIER = BUILDER
            .comment("Player speed effect amplifier during fight (0=Speed I)")
            .defineInRange("playerSpeedAmplifier", 1, 0, 4);

    /** effects: ボット速度レベル（0=Speed I）。 */
    public static final ModConfigSpec.IntValue BOT_SPEED_AMPLIFIER = BUILDER
            .comment("Bot speed effect amplifier during fight (0=Speed I)")
            .defineInRange("botSpeedAmplifier", 1, 0, 4);

    /** ボットの基礎移動速度属性値。 */
    public static final ModConfigSpec.DoubleValue BOT_BASE_MOVEMENT_SPEED = BUILDER
            .comment("Bot base movement speed attribute value")
            .defineInRange("botBaseMovementSpeed", 0.3969D, 0.1D, 1.0D);

    /** ボットのフォロー範囲。 */
    public static final ModConfigSpec.DoubleValue BOT_FOLLOW_RANGE = BUILDER
            .comment("Bot follow range attribute value")
            .defineInRange("botFollowRange", 200.0D, 16.0D, 512.0D);

    static {
        BUILDER.pop();
        BUILDER.push("ai");
    }

    /** ai:strafe の1tickあたりの横移動量。大きいほど機敏に見える。 */
    public static final ModConfigSpec.DoubleValue AI_STRAFE_SPEED = BUILDER
            .comment("Bot sidestep distance per tick during strafing. Higher = more agile-looking movement")
            .defineInRange("aiStrafeSpeed", 0.2D, 0.05D, 0.6D);

    /** プレイヤーとの距離を詰める前進速度（1tickあたり）。 */
    public static final ModConfigSpec.DoubleValue AI_CHASE_SPEED = BUILDER
            .comment("Forward chase distance per tick when the bot is outside melee range. Higher = bot closes distance faster")
            .defineInRange("aiChaseSpeed", 0.18D, 0.0D, 0.6D);

    /** 毎tickの確率でストレーフの周期タイマーをランダムな値へ飛ばし、方向転換を不規則にする。 */
    public static final ModConfigSpec.DoubleValue AI_ERRATIC_CHANCE = BUILDER
            .comment("Per-tick chance to randomly jump the strafe cycle timer, making direction changes irregular instead of a clean left-right pattern")
            .defineInRange("aiErraticChance", 0.08D, 0.0D, 0.5D);

    /** プレイヤーの滞空状態に関係なく、毎tickの確率でランダムな向きへ小さく回避ステップする。 */
    public static final ModConfigSpec.DoubleValue AI_RANDOM_DODGE_CHANCE = BUILDER
            .comment("Per-tick chance for a small random dodge step, independent of whether the player is airborne. Makes the bot feel active even when the player stays grounded")
            .defineInRange("aiRandomDodgeChance", 0.05D, 0.0D, 0.5D);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
