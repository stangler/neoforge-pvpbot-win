package net.nekometa.pvpbot;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * 右クリックでPvP Bot設定GUIを開くアイテム(サインUIの代替入口)。
 */
public class PvpBotMenuItem extends Item {

    public PvpBotMenuItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            openScreen();
        }
        return InteractionResult.SUCCESS;
    }

    // クライアント専用コードを呼び出し元(use, サーバー側でも実行される)から
    // 分離するためのヘルパー。DistExecutor等を使わずシンプルにするため、
    // ここでは level.isClientSide() のガードのみで対応している。
    private void openScreen() {
        net.minecraft.client.Minecraft.getInstance().setScreen(
                new net.nekometa.pvpbot.client.PvpBotSettingsScreen());
    }
}
