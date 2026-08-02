package net.nekometa.pvpbot.fight;

/** code:main/tick の %start var に対応。 */
public enum FightState {
    IDLE,       // 0: 待機中
    SPAWNING,   // テレポート直後、チャンク安定待ち(ボット未召喚)
    FIGHTING,   // 2: 戦闘中
    QUIT,       // 3: リタイア
    WIN,        // 4: 勝利(ボット撃破)
    LOSE,       // 5: 敗北(プレイヤー死亡)
    LOSE_VOID   // 6: 敗北(ボイス/ビースト戦特殊敗北。code:end/beast相当)
}
