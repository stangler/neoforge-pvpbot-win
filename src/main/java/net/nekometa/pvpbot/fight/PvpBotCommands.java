package net.nekometa.pvpbot.fight;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.nekometa.pvpbot.Config;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /pvpbot コマンド群。
 * 設定変更は FightSession と Config の両方に書き、再起動後も維持する。
 */
@EventBusSubscriber(modid = "pvpbot")
public final class PvpBotCommands {

    private PvpBotCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("pvpbot")
                        .then(Commands.literal("start").executes(PvpBotCommands::executeStart))
                        .then(Commands.literal("quit").executes(PvpBotCommands::executeQuit))
                        .then(Commands.literal("status").executes(PvpBotCommands::executeStatus))
                        .then(Commands.literal("beast")
                                .then(Commands.argument("on", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                        .executes(PvpBotCommands::executeBeast)))
                        .then(Commands.literal("armor")
                                .then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 3))
                                        .executes(PvpBotCommands::executeArmor)))
                        .then(Commands.literal("boxing")
                                .then(Commands.argument("mode", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 4))
                                        .executes(PvpBotCommands::executeBoxing)))
                        .then(Commands.literal("strength")
                                .then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 4))
                                        .executes(PvpBotCommands::executeStrength)))
                        .then(Commands.literal("hitsdebug")
                                .then(Commands.argument("count", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                        .executes(PvpBotCommands::executeHitsDebug)))
                        .then(Commands.literal("menu").executes(PvpBotCommands::executeMenu))
        );
    }

    private static int executeBeast(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean on = com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "on");
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.beastMode = on;
        Config.BEAST_MODE.set(on);
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.beast", on));
        return 1;
    }

    private static int executeArmor(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int tier = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "tier");
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.enemyArmorTier = tier;
        session.playerArmorTier = tier;
        Config.ARMOR_TIER.set(tier);
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.armor", tier));
        return 1;
    }

    private static int executeBoxing(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int mode = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "mode");
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.boxingMode = mode;
        session.hitThresholdOverride = 0;
        Config.BOXING_MODE.set(mode);
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.boxing", mode));
        return 1;
    }

    private static int executeStrength(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int tier = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "tier");
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.enemyStrengthTier = tier;
        Config.STRENGTH_TIER.set(tier);
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.strength", tier));
        return 1;
    }

    /** 現在のサーバー側設定を表示（看板の常時確認相当）。 */
    private static int executeStatus(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        String armor = switch (session.enemyArmorTier) {
            case 0 -> "革";
            case 1 -> "鉄";
            case 3 -> "ネザライト";
            default -> "ダイヤ";
        };
        String boxing = switch (session.boxingMode) {
            case 1 -> "50ヒット";
            case 2 -> "100ヒット";
            case 3 -> "500ヒット";
            case 4 -> "1000ヒット";
            default -> "無効(死亡)";
        };
        String strength = switch (session.enemyStrengthTier) {
            case 0 -> "弱";
            case 1 -> "易";
            case 3 -> "強";
            case 4 -> "激強";
            default -> "普通";
        };
        player.sendSystemMessage(Component.translatable(
                "pvpbot.cmd.status",
                armor,
                boxing,
                session.beastMode ? "ON" : "OFF",
                strength,
                session.state.name()));
        return 1;
    }

    private static int executeHitsDebug(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int count = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "count");
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.hitThresholdOverride = count;
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.hitsdebug", count));
        return 1;
    }

    private static int executeMenu(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        // Config の記憶値をセッションへ反映してからメニュー付与
        applyConfigToSession(player);
        player.getInventory().add(new net.minecraft.world.item.ItemStack(
                net.nekometa.pvpbot.PvPBotMod.PVPBOT_MENU_ITEM.get()));
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.menu_given"));
        return 1;
    }

    private static int executeStart(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        applyConfigToSession(player);
        FightController.start(player);
        return 1;
    }

    private static int executeQuit(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        FightController.quit(player);
        return 1;
    }

    /** Config に保存された設定をプレイヤーの FightSession へ適用。 */
    private static void applyConfigToSession(ServerPlayer player) {
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.enemyArmorTier = Config.ARMOR_TIER.getAsInt();
        session.playerArmorTier = Config.ARMOR_TIER.getAsInt();
        session.boxingMode = Config.BOXING_MODE.getAsInt();
        session.beastMode = Config.BEAST_MODE.getAsBoolean();
        session.enemyStrengthTier = Config.STRENGTH_TIER.getAsInt();
    }
}
