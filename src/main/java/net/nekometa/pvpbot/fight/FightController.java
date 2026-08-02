package net.nekometa.pvpbot.fight;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.nekometa.pvpbot.Config;

/**
 * code:main/main, code:start, code:quit, code:end/* の移植。
 *
 * アリーナはプレイヤー現在地の X/Z を維持しつつ、固定の高所 Y（既定値は Config.ARENA_Y）へ
 * テレポートして石のプラットフォームを生成する。地上の地形・建築物を破壊しない。
 * 終了時は開始前の位置・視点・ゲームモードへ復帰する。
 *
 * voidbug: アリーナ床より Config.VOID_FALL_MARGIN 以上落下したらプレイヤーは LOSE_VOID、ボットはアリーナへ復帰。
 * effects: 戦闘中はプレイヤー/ボットに移動速度上昇を付与（終了時に removeAllEffects）。
 */
@EventBusSubscriber(modid = "pvpbot")
public final class FightController {

    /** プレイヤーとボットの対峙距離（Z オフセット）。実行時は Config.BOT_OFFSET_Z を参照。 */
    private static double botOffsetZ() {
        return net.nekometa.pvpbot.Config.BOT_OFFSET_Z.get();
    }
    /** effects: 速度エフェクトの持続（tick）。戦闘終了で removeAllEffects される。 */
    private static final int SPEED_DURATION_TICKS = 20 * 60 * 30; // 30分相当

    private FightController() {
    }

    /**
     * プレイヤー周辺・アリーナ周辺の「bot」タグ付きゾンビを全て除去する。
     * 前回セッションのボットが残っていると2体化するため、start/quit/endingの各所で呼ぶ。
     */
    private static void removeBotEntities(ServerLevel level, FightSession session) {
        if (session.botUuid != null) {
            net.minecraft.world.entity.Entity leftover = level.getEntity(session.botUuid);
            if (leftover != null) {
                leftover.discard();
            }
            session.botUuid = null;
        }
        if (session.arenaCenterX != 0.0D || session.arenaCenterZ != 0.0D) {
            net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                    session.arenaCenterX - 64.0D, session.arenaFloorY - 32.0D, session.arenaCenterZ - 64.0D,
                    session.arenaCenterX + 64.0D, session.arenaFloorY + 32.0D, session.arenaCenterZ + 64.0D);
            level.getEntitiesOfClass(net.minecraft.world.entity.monster.zombie.Zombie.class, box,
                            e -> e.entityTags().contains("bot"))
                    .forEach(net.minecraft.world.entity.Entity::discard);
        }
    }

    /**
     * 高所に石床＋光源＋周囲のガラス壁を構築する。
     * 床は全面シーランタン（夜間スポーン防止）、外周ガラス壁＋天井で完全密閉しモブの侵入を防ぐ。
     */
    private static void buildArena(ServerLevel level, Vec3 center, int radius) {
        int cx = (int) Math.floor(center.x);
        int cy = (int) Math.floor(center.y);
        int cz = (int) Math.floor(center.z);

        var light = net.minecraft.world.level.block.Blocks.SEA_LANTERN.defaultBlockState();
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        var wallBlock = net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState();
        int wallHeight = Config.ARENA_WALL_HEIGHT.get();
        int clearHeight = wallHeight + 1; // 壁の上にもう1段クリアしておく

        // 床＋空間（壁と同じ半径。壁の外側に歩ける床を残すと、ボットが
        // 壁越しにすり抜けて外に出てしまうバグになるため、壁と床の範囲を揃える）
        int floorR = radius;
        for (int dx = -floorR; dx <= floorR; dx++) {
            for (int dz = -floorR; dz <= floorR; dz++) {
                level.setBlockAndUpdate(new net.minecraft.core.BlockPos(cx + dx, cy - 1, cz + dz), light);
                for (int dy = 0; dy < clearHeight; dy++) {
                    level.setBlockAndUpdate(new net.minecraft.core.BlockPos(cx + dx, cy + dy, cz + dz), air);
                }
                // 天井: ノックバックで壁を飛び越えて奈落へ落ちるのを防ぐ
                level.setBlockAndUpdate(new net.minecraft.core.BlockPos(cx + dx, cy + wallHeight, cz + dz), wallBlock);
            }
        }

        // 過去バージョンで壁外側に生成していた「庇」ブロックの残骸が
        // 同じ座標に残っている可能性があるため、壁のすぐ外側1マスも
        // 念のため空気にクリアしておく(ボットが乗り上げて出られなくなる事故防止)。
        int cleanupR = radius + 1;
        for (int dx = -cleanupR; dx <= cleanupR; dx++) {
            for (int dz = -cleanupR; dz <= cleanupR; dz++) {
                boolean insideNewFloor = Math.abs(dx) <= radius && Math.abs(dz) <= radius;
                if (insideNewFloor) {
                    continue; // 新しい床/壁の範囲は上のループで処理済み
                }
                for (int dy = -1; dy < clearHeight; dy++) {
                    level.setBlockAndUpdate(new net.minecraft.core.BlockPos(cx + dx, cy + dy, cz + dz), air);
                }
            }
        }

        // 外周ガラス壁（高さ wallHeight）。
        // 天井で完全密閉しているため、旧来の「クモ登攀防止の庇」(壁外側への
        // はみ出しブロック)は不要かつ、そこにボットが乗り上げて壁の外に
        // 出られなくなるバグの原因になっていたため削除した。
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                boolean edge = Math.abs(dx) == radius || Math.abs(dz) == radius;
                if (!edge) {
                    continue;
                }
                for (int dy = 0; dy < wallHeight; dy++) {
                    level.setBlockAndUpdate(new net.minecraft.core.BlockPos(cx + dx, cy + dy, cz + dz), wallBlock);
                }
            }
        }
    }

    /**
     * アリーナ周辺の「bot」以外の敵対Mobを除去する（クモ・クリーパー等の侵入対策）。
     * あわせて、ドロップアイテム・経験値玉の残骸も掃除する。
     * 中心はアリーナ座標基準。範囲は広めに取る。
     */
    private static void clearIntruders(ServerLevel level, ServerPlayer player, FightSession session) {
        double cx = session.arenaCenterX;
        double cy = session.arenaFloorY;
        double cz = session.arenaCenterZ;
        double range = 48.0D;

        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                cx - range, cy - 32.0D, cz - range,
                cx + range, cy + 32.0D, cz + range);

        // Monster + Enemy の両方を対象（バージョン差で interface 実装が違う場合に備える）
        level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box, e -> {
            if (e.entityTags().contains("bot")) {
                return false;
            }
            if (e instanceof net.minecraft.world.entity.monster.Monster) {
                return true;
            }
            if (e instanceof net.minecraft.world.entity.monster.Enemy) {
                return true;
            }
            // 名前で保険（パッケージ移動時）
            String n = e.getType().toShortString();
            return n.contains("spider") || n.contains("creeper") || n.contains("skeleton")
                    || n.contains("zombie") || n.contains("enderman") || n.contains("witch")
                    || n.contains("slime") || n.contains("phantom");
        }).forEach(e -> {
            e.setRemoved(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            e.discard();
        });

        // ボット討伐時のドロップアイテム・経験値玉の残骸も掃除する
        // (bot本体はドロップ率0で設定済みだが、過去バージョンの残骸や
        // 想定外のドロップが床に残るとアリーナが散らかるため)
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box, e -> true)
                .forEach(net.minecraft.world.entity.Entity::discard);
        level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, box, e -> true)
                .forEach(net.minecraft.world.entity.Entity::discard);
    }

    /**
     * バグ修正: FightSession attachment は死亡時に copyOnDeath されない(デフォルト非永続)ため、
     * 戦闘中にプレイヤーが実際に死亡してリスポーンすると、新しいプレイヤーエンティティは
     * IDLE状態のFightSessionを新規に持つことになり、旧セッションの
     * 「45tick後にボットをkillして復帰させる」処理が実行されないまま迷子になる。
     * これにより次回 /pvpbot start 時、掃除されなかった旧ボット＋新規ボットの
     * 「敵が2体になる」不具合が発生していた。
     *
     * ここでは死亡直前(旧エンティティ)のセッションを見て、戦闘中だった場合は
     * 追跡していたボットを確実に discard する。
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }
        FightSession oldSession = oldPlayer.getData(FightAttachments.FIGHT_SESSION.get());
        if (oldSession.state == FightState.IDLE) {
            return;
        }
        if (oldSession.botUuid != null && oldPlayer.level() instanceof ServerLevel level) {
            net.minecraft.world.entity.Entity bot = level.getEntity(oldSession.botUuid);
            if (bot != null) {
                bot.discard();
            }
        }
    }

    /** code:start */
    public static void start(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        // 保険: 何らかの理由で前回セッションのボットが残っていたら先に消す(2体化防止)
        removeBotEntities(level, session);
        session.state = FightState.SPAWNING;
        session.finalTimer = 0;
        session.comboCount = 0;
        session.playerHits = 0;
        session.botHits = 0;
        session.botUuid = null;
        session.spawnDelayTicks = 20; // 1秒: テレポート後、チャンクが落ち着くのを待つ

        // 復帰用に開始前の状態を保存
        session.returnPos = player.position();
        session.returnYRot = player.getYRot();
        session.returnXRot = player.getXRot();
        session.returnGameMode = player.gameMode.getGameModeForPlayer();

        // 現在の X/Z を保ち、高所 Y へ移動してアリーナを作る（地上破壊を避ける）
        double arenaY = Math.min(Config.ARENA_Y.get(), level.getMaxY() - 16.0D);
        double botOffsetZ = botOffsetZ();
        // ブロック中心に揃える
        double ax = Math.floor(player.getX()) + 0.5D;
        double az = Math.floor(player.getZ()) + 0.5D;
        Vec3 playerArenaPos = new Vec3(ax, arenaY, az);
        Vec3 botPos = playerArenaPos.add(0.0D, 0.0D, botOffsetZ);
        session.pendingBotPos = botPos;
        session.arenaFloorY = arenaY;
        session.arenaCenterX = ax;
        session.arenaCenterZ = az + botOffsetZ / 2.0D;

        // プレイヤー位置とボット位置の中間を中心に広めの床を敷く
        Vec3 arenaCenter = playerArenaPos.add(0.0D, 0.0D, botOffsetZ / 2.0D);
        buildArena(level, arenaCenter, Config.ARENA_RADIUS.get());
        clearIntruders(level, player, session);

        player.teleportTo(playerArenaPos.x, playerArenaPos.y, playerArenaPos.z);
        player.setYRot(0.0F); // ボット方向（+Z）を正面に
        player.setXRot(0.0F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 2.0F, 1.0F);

        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.DIAMOND_SWORD));
        ArmorSets.applyFullSet(player, session.playerArmorTier);
        ItemStack quitStick = new ItemStack(Items.CARROT_ON_A_STICK);
        quitStick.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.translatable("pvpbot.item.ragequit"));
        player.getInventory().setItem(8, quitStick);

        player.setGameMode(GameType.ADVENTURE);
        player.sendSystemMessage(Component.translatable("pvpbot.msg.hint_start"), false);
    }

    /** code:quit */
    public static void quit(ServerPlayer player) {
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        session.state = FightState.QUIT;
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, player.blockPosition(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.MASTER, 2.0F, 1.0F);
            removeBotEntities(level, session);
        }
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    /**
     * 退出用アイテムの右クリック検知。
     * 戦闘中（IDLE以外）に carrot_on_a_stick を右クリックしたら退出。
     * 名前比較は言語依存になるため使わず、セッション状態で判定する。
     */
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!stack.is(Items.CARROT_ON_A_STICK)) {
            return;
        }
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
        if (session.state != FightState.IDLE) {
            quit(player);
        }
    }

    /** code:main/main のプレイヤー側毎tick監視(勝敗判定・コンボ表示)。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());

        switch (session.state) {
            case SPAWNING -> tickSpawning(player, session);
            case FIGHTING -> tickFighting(player, session);
            case QUIT, WIN, LOSE, LOSE_VOID -> tickEnding(player, session);
            case IDLE -> {
                // 何もしない
            }
        }
    }

    /** テレポート後、チャンクが落ち着くのを数tick待ってからボットを召喚する。 */
    private static void tickSpawning(ServerPlayer player, FightSession session) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        session.spawnDelayTicks--;
        if (session.spawnDelayTicks > 0) {
            return;
        }

        var bot = BotSpawner.spawn(level, session.pendingBotPos, session.enemyArmorTier, session.beastMode, session.enemyStrengthTier);
        session.botUuid = bot != null ? bot.getUUID() : null;
        if (bot != null) {
            applyStrengthTierToAi(bot, session);
        }

        // code:effects — plrspeed / botspeed
        applyFightSpeedEffects(player, bot);

        session.state = FightState.FIGHTING;
        session.graceTicks = 20;
    }

    /**
     * 強さティア(session.enemyStrengthTier)とbeastModeを、ボットのBotAiState
     * (jumpresLevel/critChance/beastMode)へ反映する。
     * jumpresLevel: 1〜5=確率段階(小さいほど発動しづらい)、6=常時。
     * critChance: ai:crit の発動確率(高いほどクリットを狙いやすい)。
     */
    private static void applyStrengthTierToAi(LivingEntity bot, FightSession session) {
        net.nekometa.pvpbot.ai.BotAiState state =
                bot.getData(net.nekometa.pvpbot.ai.BotAiAttachments.BOT_AI_STATE.get());
        state.jumpresLevel = switch (session.enemyStrengthTier) {
            case 0 -> 2;
            case 1 -> 4;
            case 3 -> 6;
            case 4 -> 6;
            default -> 6; // 2 = 普通
        };
        state.critChance = switch (session.enemyStrengthTier) {
            case 0 -> 0.05D;
            case 1 -> 0.10D;
            case 3 -> 0.20D;
            case 4 -> 0.30D;
            default -> 0.15D; // 2 = 普通
        };
        state.beastMode = session.beastMode;
    }

    /**
     * code:effects 相当。
     * プレイヤー・ボットに移動速度上昇を付与（終了時の removeAllEffects で消える）。
     */
    private static void applyFightSpeedEffects(ServerPlayer player, LivingEntity bot) {
        // ambient=false, visible=false でパーティクル控えめ
        player.addEffect(new MobEffectInstance(
                MobEffects.SPEED, SPEED_DURATION_TICKS, Config.PLAYER_SPEED_AMPLIFIER.get(), false, false, true));
        if (bot != null) {
            bot.addEffect(new MobEffectInstance(
                    MobEffects.SPEED, SPEED_DURATION_TICKS, Config.BOT_SPEED_AMPLIFIER.get(), false, false, true));
        }
    }

    private static void tickFighting(ServerPlayer player, FightSession session) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        if (session.graceTicks > 0) {
            session.graceTicks--;
            // 猶予中も侵入Mobは掃除
            clearIntruders(level, player, session);
            return; // チャンク/エンティティ安定待ち。この間はWIN/LOSE判定しない
        }

        // クモ・クリーパー等がアリーナに湧いた／入ってきた場合の掃除
        clearIntruders(level, player, session);

        LivingEntity bot = null;
        if (session.botUuid != null) {
            net.minecraft.world.entity.Entity found = level.getEntity(session.botUuid);
            if (found instanceof LivingEntity le) {
                bot = le;
            }
        }

        // code:end/voidbug — アリーナ床より大きく落下したらセーフティ判定
        double voidLine = session.arenaFloorY - Config.VOID_FALL_MARGIN.get();
        if (player.getY() < voidLine) {
            session.state = FightState.LOSE_VOID;
            session.finalTimer = 0;
            return;
        }
        if (bot != null && bot.isAlive() && bot.getY() < voidLine) {
            // ボット落下: アリーナ上に復帰させて継続（ノックバック飛び出し対策）
            if (session.pendingBotPos != null) {
                bot.teleportTo(session.pendingBotPos.x, session.pendingBotPos.y, session.pendingBotPos.z);
                bot.setDeltaMovement(Vec3.ZERO);
            } else {
                bot.teleportTo(session.arenaCenterX, session.arenaFloorY, session.arenaCenterZ);
                bot.setDeltaMovement(Vec3.ZERO);
            }
        }

        // code:hitrace: %boxing sign が有効な場合、死亡ではなくヒット数先取で決着。
        int hitThreshold = session.hitThresholdOverride > 0 ? session.hitThresholdOverride : switch (session.boxingMode) {
            case 1 -> 50;
            case 2 -> 100;
            case 3 -> 500;
            case 4 -> 1000;
            default -> 0; // 0: 無効
        };
        if (hitThreshold > 0) {
            if (session.playerHits >= hitThreshold) {
                session.state = FightState.WIN;
                session.finalTimer = 0;
                return;
            }
            if (session.botHits >= hitThreshold) {
                session.state = FightState.LOSE;
                session.finalTimer = 0;
                return;
            }
        }

        if (bot == null || !bot.isAlive()) {
            session.state = FightState.WIN;
            session.finalTimer = 0;
            return;
        }
        if (!player.isAlive() || player.getHealth() <= 0.0F) {
            session.state = FightState.LOSE;
            session.finalTimer = 0;
            return;
        }

        // code:cdisplay 相当(コンボ・HP表示)
        player.sendSystemMessage(Component.translatable(
                "pvpbot.msg.hud",
                session.playerHits,
                session.botHits,
                session.comboCount,
                Math.round(bot.getHealth())), true);
    }

    /** code:end/finish の終了演出〜リセット。 */
    private static void tickEnding(ServerPlayer player, FightSession session) {
        session.finalTimer++;

        if (session.finalTimer == 1) {
            Component title = switch (session.state) {
                case QUIT -> Component.translatable("pvpbot.msg.title.quit");
                case WIN -> Component.translatable("pvpbot.msg.title.win");
                case LOSE_VOID -> Component.translatable("pvpbot.msg.title.lose_void");
                case LOSE -> Component.translatable("pvpbot.msg.title.lose");
                default -> Component.empty();
            };
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(title));

            if (player.level() instanceof ServerLevel level) {
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_DEATH, SoundSource.MASTER, 2.0F, 1.0F);
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                        player.getX(), player.getY() + 1.0D, player.getZ(),
                        162, 0.0D, 0.0D, 0.0D, 0.85D);
            }
        }

        if (session.finalTimer >= 45) {
            // code:end/timer — botタグ付きボットを全て除去
            if (player.level() instanceof ServerLevel level) {
                removeBotEntities(level, session);
            }
            player.getInventory().clearContent();
            player.removeAllEffects();

            // 開始前の位置・視点・ゲームモードへ復帰
            if (session.returnPos != null) {
                player.teleportTo(session.returnPos.x, session.returnPos.y, session.returnPos.z);
                player.setYRot(session.returnYRot);
                player.setXRot(session.returnXRot);
            }
            GameType restoreMode = session.returnGameMode != null
                    ? session.returnGameMode
                    : GameType.SURVIVAL;
            player.setGameMode(restoreMode);

            session.returnPos = null;
            session.returnGameMode = null;
            session.state = FightState.IDLE;
            session.finalTimer = 0;
        }
    }

    /** code:bothurt / code:vsfx/playerhurt からのヒット数連携用フック。 */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker.entityTags().contains("bot")) {
            FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
            session.botHits++;
            session.comboCount = 0;
        } else if (event.getEntity().entityTags().contains("bot")
                && event.getSource().getEntity() instanceof ServerPlayer player) {
            FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
            session.playerHits++;
            session.comboCount++;
        }
    }

    /**
     * 戦闘中アリーナ付近への敵対Mob湧きをブロックする。
     * bot タグ付きは許可（練習ボット本体）。
     */
    @SubscribeEvent
    public static void onEntityJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var entity = event.getEntity();
        if (!(entity instanceof net.minecraft.world.entity.Mob)) {
            return;
        }
        if (entity.entityTags().contains("bot")) {
            return;
        }
        boolean hostile = entity instanceof net.minecraft.world.entity.monster.Monster
                || entity instanceof net.minecraft.world.entity.monster.Enemy;
        if (!hostile) {
            return;
        }
        double range = 48.0D;
        for (ServerPlayer player : level.players()) {
            FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
            if (session.state == FightState.IDLE) {
                continue;
            }
            double distSq = entity.distanceToSqr(session.arenaCenterX, session.arenaFloorY, session.arenaCenterZ);
            if (distSq <= range * range) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /** 自然湧きの位置チェック段階でも拒否する。 */
    @SubscribeEvent
    public static void onSpawnPositionCheck(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        var pos = event.getEntity().position();
        double range = 48.0D;
        for (ServerPlayer player : level.players()) {
            FightSession session = player.getData(FightAttachments.FIGHT_SESSION.get());
            if (session.state == FightState.IDLE) {
                continue;
            }
            double dx = pos.x - session.arenaCenterX;
            double dy = pos.y - session.arenaFloorY;
            double dz = pos.z - session.arenaCenterZ;
            if (dx * dx + dy * dy + dz * dz <= range * range) {
                event.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
        }
    }
}
