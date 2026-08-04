package net.nekometa.pvpbot.fight;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.nekometa.pvpbot.Config;
import net.nekometa.pvpbot.PvPBotMod;
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
                        .then(Commands.literal("armor")
                                .then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 3))
                                        .executes(PvpBotCommands::executeArmor))
                                .then(Commands.literal("enemy")
                                        .then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 3))
                                                .executes(PvpBotCommands::executeEnemyArmor)))
                                .then(Commands.literal("player")
                                        .then(Commands.argument("tier", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 3))
                                                .executes(PvpBotCommands::executePlayerArmor))))
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

    private static int executeArmor(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int tier = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "tier");
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.enemyArmorTier = tier;
        session.playerArmorTier = tier;
        Config.ENEMY_ARMOR_TIER.set(tier);
        Config.PLAYER_ARMOR_TIER.set(tier);
        PvPBotMod.saveConfig();
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.armor", tier));
        return 1;
    }

    private static int executeEnemyArmor(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int tier = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "tier");
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.enemyArmorTier = tier;
        Config.ENEMY_ARMOR_TIER.set(tier);
        PvPBotMod.saveConfig();
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.enemyarmor", tier));
        return 1;
    }

    private static int executePlayerArmor(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int tier = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "tier");
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.playerArmorTier = tier;
        Config.PLAYER_ARMOR_TIER.set(tier);
        PvPBotMod.saveConfig();
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.playerarmor", tier));
        return 1;
    }

    private static int executeBoxing(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int mode = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "mode");
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.boxingMode = mode;
        session.hitThresholdOverride = 0;
        Config.BOXING_MODE.set(mode);
        PvPBotMod.saveConfig();
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.boxing", mode));
        return 1;
    }

    private static int executeStrength(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int tier = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "tier");
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.enemyStrengthTier = tier;
        Config.STRENGTH_TIER.set(tier);
        PvPBotMod.saveConfig();
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.strength", tier));
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
        giveMenu(player);
        return 1;
    }

    /**
     * /pvpbot menu 相当の処理本体。ワールド参加時の自動実行(onPlayerLoggedIn)からも呼ぶ。
     * Config の記憶値をセッションへ反映してからメニューアイテムを付与する。
     */
    static void giveMenu(ServerPlayer player) {
        applyConfigToSession(player);
        player.getInventory().add(new net.minecraft.world.item.ItemStack(
                net.nekometa.pvpbot.PvPBotMod.PVPBOT_MENU_ITEM.get()));
        player.sendSystemMessage(Component.translatable("pvpbot.cmd.menu_given"));
    }

    /**
     * ワールド参加時に /pvpbot menu を自動実行する（毎回コマンドを打つ手間を省く）。
     * 既にインベントリにメニューアイテムを持っている場合は重複付与しない。
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (hasMenuItem(player)) {
            return;
        }
        giveMenu(player);
    }

    /** インベントリ内に既にメニューアイテムを持っているかを確認する。 */
    private static boolean hasMenuItem(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(net.nekometa.pvpbot.PvPBotMod.PVPBOT_MENU_ITEM.get())) {
                return true;
            }
        }
        return false;
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
        session.enemyArmorTier = Config.ENEMY_ARMOR_TIER.getAsInt();
        session.playerArmorTier = Config.PLAYER_ARMOR_TIER.getAsInt();
        session.boxingMode = Config.BOXING_MODE.getAsInt();
        session.enemyStrengthTier = Config.STRENGTH_TIER.getAsInt();
        // アリーナ設定
        session.arenaY = Config.ARENA_Y.getAsInt();
        session.arenaRadius = Config.ARENA_RADIUS.getAsInt();
        session.arenaWallHeight = Config.ARENA_WALL_HEIGHT.getAsInt();
        session.botOffsetZ = Config.BOT_OFFSET_Z.getAsDouble();
        session.voidFallMargin = Config.VOID_FALL_MARGIN.getAsDouble();
        // ボット行動設定
        session.aiStrafeSpeed = Config.AI_STRAFE_SPEED.getAsDouble();
        session.aiChaseSpeed = Config.AI_CHASE_SPEED.getAsDouble();
        session.aiErraticChance = Config.AI_ERRATIC_CHANCE.getAsDouble();
        session.aiRandomDodgeChance = Config.AI_RANDOM_DODGE_CHANCE.getAsDouble();
    }
}
