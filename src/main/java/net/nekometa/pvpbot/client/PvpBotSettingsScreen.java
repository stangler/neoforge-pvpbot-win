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

    private int armorTier;
    private int boxingMode;
    private boolean beastMode;

    public PvpBotSettingsScreen() {
        super(Component.translatable("pvpbot.screen.title"));
        this.armorTier = ClientSettings.getArmorTier();
        this.boxingMode = ClientSettings.getBoxingMode();
        this.beastMode = ClientSettings.isBeastMode();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 70;

        // サーバー側セッションにも現在の記憶値を同期
        sendCommand("pvpbot armor " + armorTier);
        sendCommand("pvpbot boxing " + boxingMode);
        sendCommand("pvpbot beast " + beastMode);

        addRenderableWidget(Button.builder(armorLabel(), b -> {
            armorTier = (armorTier + 1) % 4;
            ClientSettings.setArmorTier(armorTier);
            b.setMessage(armorLabel());
            sendCommand("pvpbot armor " + armorTier);
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 24;
        addRenderableWidget(Button.builder(boxingLabel(), b -> {
            boxingMode = (boxingMode + 1) % 5;
            ClientSettings.setBoxingMode(boxingMode);
            b.setMessage(boxingLabel());
            sendCommand("pvpbot boxing " + boxingMode);
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 24;
        addRenderableWidget(Button.builder(beastLabel(), b -> {
            beastMode = !beastMode;
            ClientSettings.setBeastMode(beastMode);
            b.setMessage(beastLabel());
            sendCommand("pvpbot beast " + beastMode);
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 28;
        addRenderableWidget(Button.builder(Component.translatable("pvpbot.screen.status"), b -> {
            sendCommand("pvpbot status");
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 28;
        addRenderableWidget(Button.builder(Component.translatable("pvpbot.screen.start"), b -> {
            sendCommand("pvpbot armor " + armorTier);
            sendCommand("pvpbot boxing " + boxingMode);
            sendCommand("pvpbot beast " + beastMode);
            sendCommand("pvpbot start");
            onClose();
        }).bounds(centerX - 100, y, 200, 20).build());

        y += 24;
        addRenderableWidget(Button.builder(Component.translatable("pvpbot.screen.close"), b -> onClose())
                .bounds(centerX - 100, y, 200, 20).build());
    }

    private Component armorLabel() {
        String key = switch (armorTier) {
            case 0 -> "pvpbot.screen.armor.leather";
            case 1 -> "pvpbot.screen.armor.iron";
            case 3 -> "pvpbot.screen.armor.netherite";
            default -> "pvpbot.screen.armor.diamond";
        };
        return Component.translatable("pvpbot.screen.armor", Component.translatable(key));
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

    private Component beastLabel() {
        Component state = Component.translatable(beastMode ? "pvpbot.screen.on" : "pvpbot.screen.off");
        return Component.translatable("pvpbot.screen.beast", state);
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
