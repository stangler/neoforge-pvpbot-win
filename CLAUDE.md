# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 概要

Minecraft 26.1.2 / NeoForge 26.1.2 モード「PvP Practice Bot」。
元データパック(mcfunction)のPvP練習ボット機能をJava移植したものです。
`data/ai/function/*.mcfunction` や `code:main`, `code:armorsets`, `code:spawn` 等の mcfunction 命名に、ソース内のコメントで対応関係が記録されています。

## 開発コマンド

| コマンド | 説明 |
|---|---|
| `./gradlew build` | モードのビルド（JAR生成） |
| `./gradlew runClient` | デベロッパークライアント起動 |
| `./gradlew runServer` | デベロッパーサーバー起動（--nogui） |
| `./gradlew gameTestServer` | GameTest サーバー起動 |
| `./gradlew data` | データ生成（`src/generated/resources/` へ出力） |
| `./gradlew --refresh-dependencies` | キャッシュを破棄して依存解決をやり直し |
| `./gradlew clean` | build/ を削除（コードは影響なし） |

VS Code 上で直接デバッグ起動する場合は `.vscode/launch.json` の Client / Server / Data / GameTestServer 設定を使います。

## Dev Container（無効化中）

> **現在は DevContainer を使用せず、ローカル環境で開発しています。**
> 設定は `.devcontainer.disabled/` に保持しており、いつでも復元可能です。

- `devcontainer.json` + `Dockerfile(eclipse-temurin:25-jdk)` ベース
- 初回リオープン: `Dev Containers: Reopen in Container`
- `postCreateCommand` が `gradlew --version` を実行
- Gradle キャッシュは named volume `neoforge-gradle-cache` で永続化
- WSL2/gRPC 上の Linux GUI(X11/Wayland/PulseAudio)へレンダリング

### 再び有効化する手順

```bash
# ディレクトリ名を戻すだけで再開可能
mv .devcontainer.disabled .devcontainer
```

リネーム後、VS Code で「Dev Containers: Reopen in Container」が再び利用可能になります。詳細は `.devcontainer.disabled/README.md` を参照してください。

## Java アーキテクチャ

```
net.nekometa.pvpbot
├── ExampleMod.java              # モードエントリーポイント
│   └── ITEMS / CREATIVE_MODE_TABS / DataAttachment 登録
├── Config.java                  # ModConfigSpec — 防具・勝敗方式・アリーナ・AIパラメータ
├── PvpBotMenuItem.java          # 右クリックでGUIを開くアイテム
│
├── fight/                       # 戦闘管理(サーバー側)
│   ├── FightController.java     # code:main/start/quit/end のコア実装
│   │   - buildArena() 高所アリーナ生成
│   │   - clearIntruders() 侵入Mob除去
│   │   - tickSpawning/FIGHTING/ENDING() 状態機械
│   │   - onPlayerClone 死亡セッションクリーンアップ
│   │   - onLivingDamage ヒット数カウント
│   │   - onEntityJoin / onSpawnPositionCheck 自然湧きブロック
│   ├── FightSession.java        # プレイヤー単位セッション状態(状態機械,ヒット数,セット等)
│   ├── FightState.java          # 列挙型: IDLE→SPAWNING→FIGHTING→WIN/LOSE/QUIT/LOSE_VOID
│   ├── FightAttachments.java    # NeoForge DataAttachment (FightSession)
│   ├── BotSpawner.java          # code:spawn — Zombieボット生成・属性設定
│   ├── ArmorSets.java           # 防具セット適用 (革/鉄/ダイヤ/ネザライト + BeastGear)
│   └── PvpBotCommands.java      # /pvpbot {start,quit,beast,armor,boxing,strength,hitsdebug,menu}（menuはログイン時にも自動実行）
│
├── ai/                          # ボットAI(サーバー側)
│   ├── PvpBotAiEvents.java      # コアメイン: ストレーフ/WTAP/ジャンプリセット/クリット
│   │   - tickStrafe() 65tick一周の左右サイドステップ
│   │   - tickWtap() プレイヤー滞空時に接近
│   │   - tickJumpReset() 被hit→knockback_resistance上昇
│   │   - tickCritJumpTrigger() クリティカル用ジャンプ
│   │   - onLivingDamage ボット被ダメージ/プレイヤー被ダメージ
│   ├── BotAiState.java          # ボット1体分のAI状態(DataAttachment保存)
│   └── BotAiAttachments.java    # NeoForge DataAttachment (BotAiState)
│
└── client/                      # クライアント側
    ├── ClientSettings.java      # 最後に選んだ設定の永続記憶(Config経由)
    └── PvpBotSettingsScreen.java# 看板UI代替GUI — 防具/勝敗方式/Beast/Toggle/開始
```

### 重要な設計ポイント

1. **DataAttachment パターン**: バニラエンティティ(PiglinBrute 等)にカスタムフィールドを持たせるため、NeoForge の DataAttachment API を使用。`BotAiState` はボットエンティティに、`FightSession` はプレイヤーにアタッチ。

2. **状態機械**: `FightState` 列挙型で戦闘の全体状態を管理。`FightController.tickEnding()` 等、tickベースの状態遷移。

3. **Config ↔ Session 同期**: `Config`(ModConfigSpec) に保存し、再起動後も維持。セッション開始時に `applyConfigToSession()` で反映。

4. **アリーナ構築**: 現在の X/Z を維持したまま高所 Y へテレポートし、石プラットフォーム＋シーランタン＋ガラス壁＋天井の完全密閉アリーナを生成。地上の地形は破壊しない。

5. **クライアント分離**: `@Mod(dist = Dist.CLIENT)` で `ExampleModClient` を分離。`PvpBotMenuItem.use()` で `level.isClientSide()` ガード後、クライアント画面を開く。

## 移植元の mcfunction 対応関係（主要）

| mcfunction | Java |
|---|---|
| code:main/main, start, quit, end/* | FightController |
| ai:strafe, ai:strafe-c | PvpBotAiEvents.tickStrafe() |
| ai:wtap | PvpBotAiEvents.tickWtap() |
| ai:jumpreset, ai:botas | PvpBotAiEvents.tickJumpReset() |
| ai:crit | PvpBotAiEvents.tickCritJumpTrigger() + applyCritEffects() |
| code:spawn | BotSpawner |
| code:armorsets/apply_enemy, apply_player | ArmorSets.applyFullSet() |
| code:beastgear | ArmorSets.applyBeastGear() |

## 未実装・TODO

- `beast=1/2`(ネザライト装備の強化ボット) — BotSpawner に TODO あり
- サインUI → GUI 移植後の設定値接続 — BotAiState / FightSession の TODO コメント参照
- `code:on-air` predicate の厳密なブロック判定 — `onGround()` 否定で近似
- `jumptest2` フラグの正確な移植 — `player.onGround()` で近似
- `code:vsfx/playerhurt` の crit 非ヒット時ダメージ減衰 — バグのため移植せず

## 設定値 (Config.java)

- **fight**: armorTier(0-3), boxingMode(0-4), beastMode, strengthTier(0-4)
- **arena**: arenaY, arenaRadius, arenaWallHeight, botOffsetZ, voidFallMargin
- **effects**: playerSpeedAmplifier, botSpeedAmplifier, botBaseMovementSpeed, botFollowRange
- **ai**: aiStrafeSpeed, aiErraticChance, aiRandomDodgeChance