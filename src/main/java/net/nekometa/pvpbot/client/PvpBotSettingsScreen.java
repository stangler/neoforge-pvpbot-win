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

    public PvpBotSettingsScreen() {
        super(Component.translatable("pvpbot.screen.title"));
        this.enemyArmorTier = ClientSettings.getEnemyArmorTier();
        this.playerArmorTier = ClientSettings.getPlayerArmorTier();
        this.boxingMode = ClientSettings.getBoxingMode();
        this.strengthTier = ClientSettings.getStrengthTier();
        this.playerSpeedAmplifier = ClientSettings.getPlayerSpeedAmplifier();
        this.botSpeedAmplifier = ClientSettings.getBotSpeedAmplifier();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 90;

        // 敵の防具設定ボタン
        addRenderableWidget(Button.builder(enemyArmorLabel(), b -> {
            enemyArmorTier = (enemyArmorTier + 1) % 4;
            ClientSettings.setEnemyArmorTier(enemyArmorTier);
            b.setMessage(enemyArmorLabel());
            sendCommand("pvpbot armor enemy " + enemyArmorTier);
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 24;
        // プレイヤーの防具設定ボタン
        addRenderableWidget(Button.builder(playerArmorLabel(), b -> {
            playerArmorTier = (playerArmorTier + 1) % 4;
            ClientSettings.setPlayerArmorTier(playerArmorTier);
            b.setMessage(playerArmorLabel());
            sendCommand("pvpbot armor player " + playerArmorTier);
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 24;
        // ボクシングモード設定ボタン
        addRenderableWidget(Button.builder(boxingLabel(), b -> {
            boxingMode = (boxingMode + 1) % 5;
            ClientSettings.setBoxingMode(boxingMode);
            b.setMessage(boxingLabel());
            sendCommand("pvpbot boxing " + boxingMode);
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 24;
        // 敵の強さ設定ボタン
        addRenderableWidget(Button.builder(strengthLabel(), b -> {
            strengthTier = (strengthTier + 1) % 5;
            ClientSettings.setStrengthTier(strengthTier);
            b.setMessage(strengthLabel());
            sendCommand("pvpbot strength " + strengthTier);
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 24;
        // プレイヤー速度設定ボタン
        addRenderableWidget(Button.builder(playerSpeedLabel(), b -> {
            playerSpeedAmplifier = (playerSpeedAmplifier + 1) % 10;
            ClientSettings.setPlayerSpeedAmplifier(playerSpeedAmplifier);
            b.setMessage(playerSpeedLabel());
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 24;
        // ボット速度設定ボタン
        addRenderableWidget(Button.builder(botSpeedLabel(), b -> {
            botSpeedAmplifier = (botSpeedAmplifier + 1) % 10;
            ClientSettings.setBotSpeedAmplifier(botSpeedAmplifier);
            b.setMessage(botSpeedLabel());
        }).bounds(centerX - 100, y, 200, 20).build());

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
            return Component.translatable("pvpbot.screen.speed.none");
        }
        return Component.translatable("pvpbot.screen.speed", "Speed " + romanNumerals(playerSpeedAmplifier));
    }

    private Component botSpeedLabel() {
        if (botSpeedAmplifier == 0) {
            return Component.translatable("pvpbot.screen.speed.none");
        }
        return Component.translatable("pvpbot.screen.speed", "Speed " + romanNumerals(botSpeedAmplifier));
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