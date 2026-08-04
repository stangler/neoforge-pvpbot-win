package net.nekometa.pvpbot.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 元データパックの看板UI(data/sign/function/*)の移植先GUI。
 *
 * 設定は ClientSettings / Config に記憶され、再表示・再起動後も維持される。
 * サーバー側には既存の /pvpbot コマンドで反映する（新規ネットワーク不要）。
 *
 * レイアウト: 設定ボタンは左右2列（旧3列から変更）。
 * ウィンドウが低い場合に備え、2列部分はマウスホイールで縦スクロール可能。
 * ステータス確認/開始/閉じるの下段ボタンはスクロールの影響を受けない固定位置。
 */
public class PvpBotSettingsScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 24;
    private static final int VISIBLE_ROWS = 6; // スクロールviewportに一度に表示する行数
    private static final int COLUMN_GAP = 20;

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

    // スクロール対応の設定ボタン群（下段の固定ボタンとは別管理）
    private final List<AbstractWidget> configWidgets = new ArrayList<>();
    private final List<Integer> configBaseY = new ArrayList<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private int listX1;
    private int listX2;
    private int listTop;
    private int listBottom;

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
        configWidgets.clear();
        configBaseY.clear();
        scrollOffset = 0;

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int leftX = centerX - 210;
        int midX = leftX + BUTTON_WIDTH + COLUMN_GAP; // = centerX + 10 (旧midXと同一)

        listTop = centerY - 90;
        int viewportHeight = VISIBLE_ROWS * ROW_HEIGHT;
        listBottom = listTop + viewportHeight;
        listX1 = leftX - 4;
        listX2 = midX + BUTTON_WIDTH + 4;

        // 左列(col1): 戦闘設定 + アリーナ設定の一部 (8ボタン)
        int col1Row = 0;
        addConfigButton(leftX, col1Row++, enemyArmorLabel(), b -> {
            enemyArmorTier = (enemyArmorTier + 1) % 4;
            ClientSettings.setEnemyArmorTier(enemyArmorTier);
            b.setMessage(enemyArmorLabel());
            sendCommand("pvpbot armor enemy " + enemyArmorTier);
        });
        addConfigButton(leftX, col1Row++, playerArmorLabel(), b -> {
            playerArmorTier = (playerArmorTier + 1) % 4;
            ClientSettings.setPlayerArmorTier(playerArmorTier);
            b.setMessage(playerArmorLabel());
            sendCommand("pvpbot armor player " + playerArmorTier);
        });
        addConfigButton(leftX, col1Row++, boxingLabel(), b -> {
            boxingMode = (boxingMode + 1) % 5;
            ClientSettings.setBoxingMode(boxingMode);
            b.setMessage(boxingLabel());
            sendCommand("pvpbot boxing " + boxingMode);
        });
        addConfigButton(leftX, col1Row++, strengthLabel(), b -> {
            strengthTier = (strengthTier + 1) % 5;
            ClientSettings.setStrengthTier(strengthTier);
            b.setMessage(strengthLabel());
            sendCommand("pvpbot strength " + strengthTier);
        });
        addConfigButton(leftX, col1Row++, playerSpeedLabel(), b -> {
            playerSpeedAmplifier = (playerSpeedAmplifier + 1) % 10;
            ClientSettings.setPlayerSpeedAmplifier(playerSpeedAmplifier);
            b.setMessage(playerSpeedLabel());
        });
        addConfigButton(leftX, col1Row++, botSpeedLabel(), b -> {
            botSpeedAmplifier = (botSpeedAmplifier + 1) % 10;
            ClientSettings.setBotSpeedAmplifier(botSpeedAmplifier);
            b.setMessage(botSpeedLabel());
        });
        addConfigButton(leftX, col1Row++, arenaYLabel(), b -> {
            arenaY = cycleInt(arenaY, -64, 2032, 16);
            ClientSettings.setArenaY(arenaY);
            b.setMessage(arenaYLabel());
        });
        addConfigButton(leftX, col1Row++, arenaRadiusLabel(), b -> {
            arenaRadius = cycleInt(arenaRadius, 4, 64, 2);
            ClientSettings.setArenaRadius(arenaRadius);
            b.setMessage(arenaRadiusLabel());
        });

        // 右列(col2): アリーナ設定の残り + AI設定 (7ボタン)
        int col2Row = 0;
        addConfigButton(midX, col2Row++, arenaWallHeightLabel(), b -> {
            arenaWallHeight = cycleInt(arenaWallHeight, 3, 12, 1);
            ClientSettings.setArenaWallHeight(arenaWallHeight);
            b.setMessage(arenaWallHeightLabel());
        });
        addConfigButton(midX, col2Row++, botOffsetZLabel(), b -> {
            botOffsetZ = cycleDouble(botOffsetZ, 2.0, 32.0, 1.0);
            ClientSettings.setBotOffsetZ(botOffsetZ);
            b.setMessage(botOffsetZLabel());
        });
        addConfigButton(midX, col2Row++, voidFallMarginLabel(), b -> {
            voidFallMargin = cycleDouble(voidFallMargin, 1.0, 64.0, 2.0);
            ClientSettings.setVoidFallMargin(voidFallMargin);
            b.setMessage(voidFallMarginLabel());
        });
        addConfigButton(midX, col2Row++, aiStrafeSpeedLabel(), b -> {
            aiStrafeSpeed = cycleDouble(aiStrafeSpeed, 5, 80, 5);
            ClientSettings.setAiStrafeSpeed(aiStrafeSpeed / 100);
            b.setMessage(aiStrafeSpeedLabel());
        });
        addConfigButton(midX, col2Row++, aiChaseSpeedLabel(), b -> {
            aiChaseSpeed = cycleDouble(aiChaseSpeed, 0, 90, 5);
            ClientSettings.setAiChaseSpeed(aiChaseSpeed / 100);
            b.setMessage(aiChaseSpeedLabel());
        });
        addConfigButton(midX, col2Row++, aiErraticChanceLabel(), b -> {
            aiErraticChance = cycleDouble(aiErraticChance, 0, 80, 5);
            ClientSettings.setAiErraticChance(aiErraticChance / 100);
            b.setMessage(aiErraticChanceLabel());
        });
        addConfigButton(midX, col2Row++, aiRandomDodgeChanceLabel(), b -> {
            aiRandomDodgeChance = cycleDouble(aiRandomDodgeChance, 0, 80, 5);
            ClientSettings.setAiRandomDodgeChance(aiRandomDodgeChance / 100);
            b.setMessage(aiRandomDodgeChanceLabel());
        });

        int maxRows = Math.max(col1Row, col2Row);
        int contentHeight = maxRows * ROW_HEIGHT;
        maxScroll = Math.max(0, contentHeight - viewportHeight);
        repositionConfigWidgets();

        // 下段: 開始・閉じるボタン（スクロールの影響を受けない固定位置）
        int y = listBottom + 16;
        addRenderableWidget(Button.builder(Component.translatable("pvpbot.screen.start"), b -> {
            sendCommand("pvpbot armor enemy " + enemyArmorTier);
            sendCommand("pvpbot armor player " + playerArmorTier);
            sendCommand("pvpbot boxing " + boxingMode);
            sendCommand("pvpbot strength " + strengthTier);
            sendCommand("pvpbot start");
            onClose();
        }).bounds(centerX - 100, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("pvpbot.screen.close"), b -> onClose())
                .bounds(centerX - 100, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    /**
     * スクロール対象の設定ボタンを追加する。addRenderableWidget ではなく addWidget を使うことで
     * クリック判定(children)には含めつつ、自動描画(renderables)からは除外し、
     * render() 内で手動＋シザー(clip)描画する。
     */
    private void addConfigButton(int x, int row, Component label, Button.OnPress onPress) {
        int baseY = listTop + row * ROW_HEIGHT;
        Button button = Button.builder(label, onPress).bounds(x, baseY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addWidget(button);
        configWidgets.add(button);
        configBaseY.add(baseY);
    }

    private void repositionConfigWidgets() {
        for (int i = 0; i < configWidgets.size(); i++) {
            AbstractWidget w = configWidgets.get(i);
            int baseY = configBaseY.get(i);
            int newY = baseY - scrollOffset;
            w.setY(newY);
            // viewport外に完全に出ているボタンはクリック判定を無効化（境界での誤クリック防止）
            w.active = (newY + BUTTON_HEIGHT > listTop) && (newY < listBottom);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (maxScroll > 0 && mouseX >= listX1 && mouseX <= listX2 && mouseY >= listTop && mouseY <= listBottom) {
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (scrollY * (ROW_HEIGHT / 2.0))));
            repositionConfigWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int mouseX, int mouseY, float partialTick) {
        // 背景 + 下段の固定ボタン(renderables)は通常描画
        super.extractRenderState(guiGraphicsExtractor, mouseX, mouseY, partialTick);

        // スクロール対象の設定ボタンはシザーでクリップして手動描画
        guiGraphicsExtractor.enableScissor(listX1, listTop, listX2, listBottom);
        for (AbstractWidget w : configWidgets) {
            w.extractRenderState(guiGraphicsExtractor, mouseX, mouseY, partialTick);
        }
        guiGraphicsExtractor.disableScissor();
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

    // 未使用メソッドの警告を抑制（将来の拡張用）
    @SuppressWarnings("unused")
    private int cycleIntDecrease(int current, int min, int max, int step) {
        int next = current - step;
        if (next < min) {
            return max;
        }
        return next;
    }

    @SuppressWarnings("unused")
    private double cycleDoubleDecrease(double current, double min, double max, double step) {
        double next = current - step;
        if (next < min) {
            return max;
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
