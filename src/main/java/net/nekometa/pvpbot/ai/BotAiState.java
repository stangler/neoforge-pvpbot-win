package net.nekometa.pvpbot.ai;

/**
 * 元データパックの fake player scoreboard 変数に対応する、ボット1体分のAI状態。
 *
 * 対応表:
 *   %aistrafetime var -> strafeTimer   (ai:strafe / ai:strafe-c)
 *   %hitcount     var -> hitCount      (ai:jumpresetact / code:bothurt / code:vsfx/playerhurt)
 *   %js           var -> jumpResetActive   (ai:jumpresetact)
 *   %js-hit       var -> jumpResetHitCount (ai:jumpresetact / code:bothurt)
 *   %bu-damage    var -> critDamageBackedUp (ai:crit)
 *   %beast        sign -> beastMode (ai:crit; 現状は未使用、beastgear移植時に接続)
 */
public class BotAiState {

    /** ai:strafe / ai:strafe-c 用ストレーフタイマー。0〜64でループ。 */
    public int strafeTimer = 0;

    /** 直近でプレイヤーからヒットを受けた回数(0にリセットされるまでの間の値)。
     *  code:bothurt で +1、code:vsfx/playerhurt(=プレイヤーがボットを殴った瞬間)で 0 にリセット。 */
    public int hitCount = 0;

    /** ai:jumpresetact の %js。knockback_resistance を一時的に上げている状態かどうか。 */
    public boolean jumpResetActive = false;

    /** ai:jumpresetact の %js-hit。jumpResetActive中にさらにヒットを受けた回数。 */
    public int jumpResetHitCount = 0;

    /** ai:crit の %bu-damage。既にattack_damageのcrit補正が入っているかどうか(二重付与防止)。 */
    public boolean critDamageApplied = false;

    // --- サインUIに相当する有効/無効フラグ。現時点では固定値(全ON)。 ---
    // TODO: sign UI -> GUI/Screen 移植後、ここをプレイヤーの設定値に置き換える。
    public boolean wtapEnabled = true;
    public boolean strafesEnabled = true;
    /** ai:botas の %jumpres sign (0=無効, 1〜5=確率段階, 6=常時)。まずは常時(6)固定。 */
    public int jumpresLevel = 6;
    public boolean critEnabled = true;
    /** ai:crit の発動確率。難易度(strengthTier)に応じて FightController 側で設定される。 */
    public double critChance = 0.15D;
}
