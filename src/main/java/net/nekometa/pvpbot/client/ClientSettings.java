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

    // Arena settings
    public static int getArenaY() {
        return Config.ARENA_Y.getAsInt();
    }

    public static void setArenaY(int y) {
        Config.ARENA_Y.set(y);
    }

    public static int getArenaRadius() {
        return Config.ARENA_RADIUS.getAsInt();
    }

    public static void setArenaRadius(int radius) {
        Config.ARENA_RADIUS.set(Math.clamp(radius, 4, 64));
    }

    public static int getArenaWallHeight() {
        return Config.ARENA_WALL_HEIGHT.getAsInt();
    }

    public static void setArenaWallHeight(int height) {
        Config.ARENA_WALL_HEIGHT.set(Math.clamp(height, 3, 12));
    }

    public static double getBotOffsetZ() {
        return Config.BOT_OFFSET_Z.getAsDouble();
    }

    public static void setBotOffsetZ(double offset) {
        Config.BOT_OFFSET_Z.set(offset);
    }

    public static double getVoidFallMargin() {
        return Config.VOID_FALL_MARGIN.getAsDouble();
    }

    public static void setVoidFallMargin(double margin) {
        Config.VOID_FALL_MARGIN.set(margin);
    }

    // AI settings
    public static double getAiStrafeSpeed() {
        return Config.AI_STRAFE_SPEED.getAsDouble();
    }

    public static void setAiStrafeSpeed(double speed) {
        Config.AI_STRAFE_SPEED.set(speed);
    }

    public static double getAiChaseSpeed() {
        return Config.AI_CHASE_SPEED.getAsDouble();
    }

    public static void setAiChaseSpeed(double speed) {
        Config.AI_CHASE_SPEED.set(speed);
    }

    public static double getAiErraticChance() {
        return Config.AI_ERRATIC_CHANCE.getAsDouble();
    }

    public static void setAiErraticChance(double chance) {
        Config.AI_ERRATIC_CHANCE.set(chance);
    }

    public static double getAiRandomDodgeChance() {
        return Config.AI_RANDOM_DODGE_CHANCE.getAsDouble();
    }

    public static void setAiRandomDodgeChance(double chance) {
        Config.AI_RANDOM_DODGE_CHANCE.set(chance);
    }
}
