package net.nekometa.pvpbot.client;

import net.nekometa.pvpbot.Config;

/**
 * クライアント側で最後に選んだ設定を保持する（看板の「常時表示・記憶」相当）。
 * Config と同期し、GUI を閉じても・再起動しても値を維持する。
 */
public final class ClientSettings {

    private ClientSettings() {
    }

    public static int getEnemyArmorTier() {
        return Config.ENEMY_ARMOR_TIER.getAsInt();
    }

    public static void setEnemyArmorTier(int tier) {
        Config.ENEMY_ARMOR_TIER.set(Math.clamp(tier, 0, 3));
    }

    public static int getPlayerArmorTier() {
        return Config.PLAYER_ARMOR_TIER.getAsInt();
    }

    public static void setPlayerArmorTier(int tier) {
        Config.PLAYER_ARMOR_TIER.set(Math.clamp(tier, 0, 3));
    }

    public static int getBoxingMode() {
        return Config.BOXING_MODE.getAsInt();
    }

    public static void setBoxingMode(int mode) {
        Config.BOXING_MODE.set(Math.clamp(mode, 0, 4));
    }

    public static int getStrengthTier() {
        return Config.STRENGTH_TIER.getAsInt();
    }

    public static void setStrengthTier(int tier) {
        Config.STRENGTH_TIER.set(Math.clamp(tier, 0, 4));
    }

    public static int getPlayerSpeedAmplifier() {
        return Config.PLAYER_SPEED_AMPLIFIER.getAsInt();
    }

    public static void setPlayerSpeedAmplifier(int amplifier) {
        Config.PLAYER_SPEED_AMPLIFIER.set(Math.clamp(amplifier, 0, 9));
    }

    public static int getBotSpeedAmplifier() {
        return Config.BOT_SPEED_AMPLIFIER.getAsInt();
    }

    public static void setBotSpeedAmplifier(int amplifier) {
        Config.BOT_SPEED_AMPLIFIER.set(Math.clamp(amplifier, 0, 9));
    }
}
