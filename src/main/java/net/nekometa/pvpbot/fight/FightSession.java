package net.nekometa.pvpbot.fight;

/**
 * 元mcfunctionのグローバルfake player var(%start, %finaltimer, %hitcount,
 * %overall_hits, %bothits等)を、プレイヤー単位のセッション状態として保持。
 */
public class FightSession {
    public FightState state = FightState.IDLE;

    /** code:end/finish の %finaltimer。終了演出〜リセットまでのtickカウント。 */
    public int finalTimer = 0;

    /** code:cdisplay の %hitcount(現在のコンボ数)。 */
    public int comboCount = 0;

    /** %overall_hits(自分の総ヒット数)、%bothits(ボットの総ヒット数)。 */
    public int playerHits = 0;
    public int botHits = 0;

    /** spawn()した対戦相手ボットのUUID。距離に依存せず直接参照するため。 */
    public java.util.UUID botUuid = null;

    /** start()直後、チャンク/エンティティが安定するまでの猶予tick数。 */
    public int graceTicks = 0;

    /** SPAWNING状態でのボット召喚待ちtick数。 */
    public int spawnDelayTicks = 0;

    /** SPAWNING状態で使う、ボット召喚予定座標。 */
    public net.minecraft.world.phys.Vec3 pendingBotPos = null;

    /**
     * 戦闘開始前のプレイヤー位置。終了時にここへテレポートして戻す。
     * null の場合は復帰しない。
     */
    public net.minecraft.world.phys.Vec3 returnPos = null;

    /** 戦闘開始前の視点(yaw/pitch)。復帰時に復元する。 */
    public float returnYRot = 0.0F;
    public float returnXRot = 0.0F;

    /** 戦闘開始前のゲームモード。終了時に復元する（未取得時は SURVIVAL）。 */
    public net.minecraft.world.level.GameType returnGameMode = null;

    /**
     * アリーナ床の Y 座標。voidbug（奈落セーフティ）判定に使う。
     * start() で設定される。
     */
    public double arenaFloorY = 200.0D;

    /** アリーナ中心 X/Z（ボット復帰テレポート用）。 */
    public double arenaCenterX = 0.0D;
    public double arenaCenterZ = 0.0D;

    // --- サインUI(armorsets)相当。移植までは固定値。 ---
    // TODO: サインUI→GUI移植後、プレイヤーの選択値に置き換える。
    /** %enemy_armor_set: 0革 1鉄 2ダイヤ 3ネザライト */
    public int enemyArmorTier = 2;
    /** %player_armor_set: 0革 1鉄 2ダイヤ 3ネザライト */
    public int playerArmorTier = 2;

    /** %boxing sign: 0=無効(死亡で決着) 1=50 2=100 3=500 4=1000 ヒット先取で決着(code:hitrace) */
    public int boxingMode = 0;

    /** 敵の強さ設定: 0弱 1易 2普通 3強 4激強。攻撃力・クリット率・ジャンプリセット頻度に影響。 */
    public int enemyStrengthTier = 2;

    /** 検証用: 0以外なら上記固定値を無視してこの数値をヒット閾値として使う。 */
    public int hitThresholdOverride = 0;

    // --- アリーナ設定（GUI/Configから反映） ---
    /** アリーナ床の高さ */
    public int arenaY = 200;
    /** アリーナ半径 */
    public int arenaRadius = 12;
    /** 壁の高さ */
    public int arenaWallHeight = 5;
    /** ボット配置距離（Zオフセット） */
    public double botOffsetZ = 8.0D;
    /** 奈落落下マージン */
    public double voidFallMargin = 10.0D;

    // --- ボット行動設定（GUI/Configから反映） ---
    /** ストレーフ速度 */
    public double aiStrafeSpeed = 0.35D;
    /** チェイス速度 */
    public double aiChaseSpeed = 0.35D;
    /** 不規則行動確率 */
    public double aiErraticChance = 0.18D;
    /** ランダム回避確率 */
    public double aiRandomDodgeChance = 0.15D;
}
