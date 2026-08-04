package net.nekometa.pvpbot.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 元データパックの看板UI(data/sign/function/*)の移植先GUI。
 *
 * 設定は ClientSettings / Config に記憶され、再表示・再起動後も維持される。
 * サーバー側には既存の /pvpbot コマンドで反映する（新規ネットワーク不要）。
 */
public class PvpBotSettingsScreen extends Screen {

    private int enemyArmorTier;
    private int playerArmorTier;
    private int boxingMode;
    private int strengthTier;
    private int playerSpeedAmplifier;
    private int botSpeedAmplifier;
    private int arenaY;
    private int arenaRadius;
    private int arenaWallHeight;
    private double botOffsetZ;
    private double voidFallMargin;
    private double aiStrafeSpeed;
    private double aiChaseSpeed;
    private double aiErraticChance;
    private double aiRandomDodgeChance;

    public PvpBotSettingsScreen() {
        super(Component.translatable("pvpbot.screen.title"));
        this.enemyArmorTier = ClientSettings.getEnemyArmorTier();
        this.playerArmorTier = ClientSettings.getPlayerArmorTier();
        this.boxingMode = ClientSettings.getBoxingMode();
        this.strengthTier = ClientSettings.getStrengthTier();
        this.playerSpeedAmplifier = ClientSettings.getPlayerSpeedAmplifier();
        this.botSpeedAmplifier = ClientSettings.getBotSpeedAmplifier();
        this.arenaY = ClientSettings.getArenaY();
        this.arenaRadius = ClientSettings.getArenaRadius();
        this.arenaWallHeight = ClientSettings.getArenaWallHeight();
        this.botOffsetZ = ClientSettings.getBotOffsetZ();
        this.voidFallMargin = ClientSettings.getVoidFallMargin();
        this.aiStrafeSpeed = (int) (ClientSettings.getAiStrafeSpeed() * 100);
        this.aiChaseSpeed = (int) (ClientSettings.getAiChaseSpeed() * 100);
        this.aiErraticChance = (int) (ClientSettings.getAiErraticChance() * 100);
        this.aiRandomDodgeChance = (int) (ClientSettings.getAiRandomDodgeChance() * 100);
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 90;
        int leftX = centerX - 105;
        int rightX = centerX + 5;

        // 左列: 既存の設定
        // 敵の防具設定ボタン
        addRenderableWidget(Button.builder(enemyArmorLabel(), b -> {
            enemyArmorTier = (enemyArmorTier + 1) % 4;
            ClientSettings.setEnemyArmorTier(enemyArmorTier);
            b.setMessage(enemyArmorLabel());
            sendCommand("pvpbot armor enemy " + enemyArmorTier);
        }).bounds(leftX, y, 200, 20).build());

        y += 24;
        // プレイヤーの防具設定ボタン
        addRenderableWidget(Button.builder(playerArmorLabel(), b -> {
            playerArmorTier = (playerArmorTier + 1) % 4;
            ClientSettings.setPlayerArmorTier(playerArmorTier);
            b.setMessage(playerArmorLabel());
            sendCommand("pvpbot armor player " + playerArmorTier);
        }).bounds(leftX, y, 200, 20).build());

        y += 24;
        // ボクシングモード設定ボタン
        addRenderableWidget(Button.builder(boxingLabel(), b -> {
            boxingMode = (boxingMode + 1) % 5;
            ClientSettings.setBoxingMode(boxingMode);
            b.setMessage(boxingLabel());
            sendCommand("pvpbot boxing " + boxingMode);
        }).bounds(leftX, y, 200, 20).build());

        y += 24;
        // 敵の強さ設定ボタン
        addRenderableWidget(Button.builder(strengthLabel(), b -> {
            strengthTier = (strengthTier + 1) % 5;
            ClientSettings.setStrengthTier(strengthTier);
            b.setMessage(strengthLabel());
            sendCommand("pvpbot strength " + strengthTier);
        }).bounds(leftX, y, 200, 20).build());

        y += 24;
        // プレイヤー速度設定ボタン
        addRenderableWidget(Button.builder(playerSpeedLabel(), b -> {
            playerSpeedAmplifier = (playerSpeedAmplifier + 1) % 10;
            ClientSettings.setPlayerSpeedAmplifier(playerSpeedAmplifier);
            b.setMessage(playerSpeedLabel());
        }).bounds(leftX, y, 200, 20).build());

        y += 24;
        // ボット速度設定ボタン
        addRenderableWidget(Button.builder(botSpeedLabel(), b -> {
            botSpeedAmplifier = (botSpeedAmplifier + 1) % 10;
            ClientSettings.setBotSpeedAmplifier(botSpeedAmplifier);
            b.setMessage(botSpeedLabel());
        }).bounds(leftX, y, 200, 20).build());

        // 右列: アリーナ設定
        y = this.height / 2 - 90;
        // アリーナ高さ設定ボタン
        addRenderableWidget(Button.builder(arenaYLabel(), b -> {
            arenaY = cycleInt(arenaY, -64, 2032, 16);
            ClientSettings.setArenaY(arenaY);
            b.setMessage(arenaYLabel());
        }).bounds(rightX, y, 200, 20).build());

        y += 24;
        // アリーナ半径設定ボタン
        addRenderableWidget(Button.builder(arenaRadiusLabel(), b -> {
            arenaRadius = cycleInt(arenaRadius, 4, 64, 2);
            ClientSettings.setArenaRadius(arenaRadius);
            b.setMessage(arenaRadiusLabel());
        }).bounds(rightX, y, 200, 20).build());

        y += 24;
        // 壁の高さ設定ボタン
        addRenderableWidget(Button.builder(arenaWallHeightLabel(), b -> {
            arenaWallHeight = cycleInt(arenaWallHeight, 3, 12, 1);
            ClientSettings.setArenaWallHeight(arenaWallHeight);
            b.setMessage(arenaWallHeightLabel());
        }).bounds(rightX, y, 200, 20).build());

        y += 24;
        // ボット配置距離設定ボタン
        addRenderableWidget(Button.builder(botOffsetZLabel(), b -> {
            botOffsetZ = cycleDouble(botOffsetZ, 2.0, 32.0, 1.0);
            ClientSettings.setBotOffsetZ(botOffsetZ);
            b.setMessage(botOffsetZLabel());
        }).bounds(rightX, y, 200, 20).build());

        y += 24;
        // 奈落落下マージン設定ボタン
        addRenderableWidget(Button.builder(voidFallMarginLabel(), b -> {
            voidFallMargin = cycleDouble(voidFallMargin, 1.0, 64.0, 2.0);
            ClientSettings.setVoidFallMargin(voidFallMargin);
            b.setMessage(voidFallMarginLabel());
        }).bounds(rightX, y, 200, 20).build());

        y += 24;
        // AI設定セクション
        // AI ストレーフ速度設定ボタン
        addRenderableWidget(Button.builder(aiStrafeSpeedLabel(), b -> {
            aiStrafeSpeed = cycleDouble(aiStrafeSpeed, 5, 80, 5);
            ClientSettings.setAiStrafeSpeed(aiStrafeSpeed / 100);
            b.setMessage(aiStrafeSpeedLabel());
        }).bounds(rightX, y, 200, 20).build());

        y += 24;
        // AI チェイス速度設定ボタン
        addRenderableWidget(Button.builder(aiChaseSpeedLabel(), b -> {
            aiChaseSpeed = cycleDouble(aiChaseSpeed, 0, 90, 5);
            ClientSettings.setAiChaseSpeed(aiChaseSpeed / 100);
            b.setMessage(aiChaseSpeedLabel());
        }).bounds(rightX, y, 200, 20).build());

        y += 24;
        // AI 不規則行動確率設定ボタン
        addRenderableWidget(Button.builder(aiErraticChanceLabel(), b -> {
            aiErraticChance = cycleDouble(aiErraticChance, 0, 80, 5);
            ClientSettings.setAiErraticChance(aiErraticChance / 100);
            b.setMessage(aiErraticChanceLabel());
        }).bounds(rightX, y, 200, 20).build());

        y += 24;
        // AI ランダム回避確率設定ボタン
        addRenderableWidget(Button.builder(aiRandomDodgeChanceLabel(), b -> {
            aiRandomDodgeChance = cycleDouble(aiRandomDodgeChance, 0, 80, 5);
            ClientSettings.setAiRandomDodgeChance(aiRandomDodgeChance / 100);
            b.setMessage(aiRandomDodgeChanceLabel());
        }).bounds(rightX, y, 200, 20).build());

        y += 28;
        // ステータス確認ボタン
        addRenderableWidget(Button.builder(Component.translatable("pvpbot.screen.status"), b -> {
            sendCommand("pvpbot status");
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 28;
        // 開始ボタン
        addRenderableWidget(Button.builder(Component.translatable("pvpbot.screen.start"), b -> {
            sendCommand("pvpbot armor enemy " + enemyArmorTier);
            sendCommand("pvpbot armor player " + playerArmorTier);
            sendCommand("pvpbot boxing " + boxingMode);
            sendCommand("pvpbot strength " + strengthTier);
            sendCommand("pvpbot start");
            onClose();
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 24;
        // 閉じるボタン
        addRenderableWidget(Button.builder(Component.translatable("pvpbot.screen.close"), b -> onClose())
                .bounds(centerX - 100, y, 200, 20).build());
    }

    private Component enemyArmorLabel() {
        String key = switch (enemyArmorTier) {
            case 0 -> "pvpbot.screen.armor.leather";
            case 1 -> "pvpbot.screen.armor.iron";
            case 3 -> "pvpbot.screen.armor.netherite";
            default -> "pvpbot.screen.armor.diamond";
        };
        return Component.translatable("pvpbot.screen.enemy.armor", Component.translatable(key));
    }

    private Component playerArmorLabel() {
        String key = switch (playerArmorTier) {
            case 0 -> "pvpbot.screen.armor.leather";
            case 1 -> "pvpbot.screen.armor.iron";
            case 3 -> "pvpbot.screen.armor.netherite";
            default -> "pvpbot.screen.armor.diamond";
        };
        return Component.translatable("pvpbot.screen.player.armor", Component.translatable(key));
    }

    private Component boxingLabel() {
        String key = switch (boxingMode) {
            case 1 -> "pvpbot.screen.boxing.50";
            case 2 -> "pvpbot.screen.boxing.100";
            case 3 -> "pvpbot.screen.boxing.500";
            case 4 -> "pvpbot.screen.boxing.1000";
            default -> "pvpbot.screen.boxing.off";
        };
        return Component.translatable("pvpbot.screen.boxing", Component.translatable(key));
    }

    private Component strengthLabel() {
        String key = switch (strengthTier) {
            case 0 -> "pvpbot.screen.strength.weak";
            case 1 -> "pvpbot.screen.strength.easy";
            case 3 -> "pvpbot.screen.strength.strong";
            case 4 -> "pvpbot.screen.strength.nightmare";
            default -> "pvpbot.screen.strength.normal";
        };
        return Component.translatable("pvpbot.screen.strength", Component.translatable(key));
    }

    private Component playerSpeedLabel() {
        if (playerSpeedAmplifier == 0) {
            return Component.translatable("pvpbot.screen.player.speed", Component.translatable("pvpbot.screen.speed.none"));
        }
        return Component.translatable("pvpbot.screen.player.speed", "Speed " + romanNumerals(playerSpeedAmplifier));
    }

    private Component botSpeedLabel() {
        if (botSpeedAmplifier == 0) {
            return Component.translatable("pvpbot.screen.bot.speed", Component.translatable("pvpbot.screen.speed.none"));
        }
        return Component.translatable("pvpbot.screen.bot.speed", "Speed " + romanNumerals(botSpeedAmplifier));
    }

    private String romanNumerals(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            default -> String.valueOf(number);
        };
    }

    private int cycleInt(int current, int min, int max, int step) {
        int next = current + step;
        if (next > max) {
            return min;
        }
        return next;
    }

    private double cycleDouble(double current, double min, double max, double step) {
        double next = current + step;
        if (next > max) {
            return min;
        }
        return next;
    }

    private Component arenaYLabel() {
        return Component.translatable("pvpbot.screen.arena.y", arenaY);
    }

    private Component arenaRadiusLabel() {
        return Component.translatable("pvpbot.screen.arena.radius", arenaRadius);
    }

    private Component arenaWallHeightLabel() {
        return Component.translatable("pvpbot.screen.arena.wall", arenaWallHeight);
    }

    private Component botOffsetZLabel() {
        return Component.translatable("pvpbot.screen.arena.offset", String.format("%.1f", botOffsetZ));
    }

    private Component voidFallMarginLabel() {
        return Component.translatable("pvpbot.screen.arena.void", String.format("%.1f", voidFallMargin));
    }

    private Component aiStrafeSpeedLabel() {
        return Component.translatable("pvpbot.screen.ai.strafe", String.format("%.0f", aiStrafeSpeed));
    }

    private Component aiChaseSpeedLabel() {
        return Component.translatable("pvpbot.screen.ai.chase", String.format("%.0f", aiChaseSpeed));
    }

    private Component aiErraticChanceLabel() {
        return Component.translatable("pvpbot.screen.ai.erratic", String.format("%.0f", aiErraticChance));
    }

    private Component aiRandomDodgeChanceLabel() {
        return Component.translatable("pvpbot.screen.ai.dodge", String.format("%.0f", aiRandomDodgeChance));
    }

    private void sendCommand(String command) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player != null && player.connection != null) {
            player.connection.sendCommand(command);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}