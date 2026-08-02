# PvP Practice Bot

Minecraft 26.1.2 / NeoForge 26.1.2 向けの練習用PvPボットモッドです。
元データパック（mcfunction）で実装されていたPvP練習ボットを、JavaのNeoForgeモッドへ移植したものです。

このモッドは [CurseForge: PvP Bot Practice World](https://www.curseforge.com/minecraft/worlds/pvp-bot-practice-world) にインスパイアされて作成されました。

## 機能

- `/pvpbot start` で高所アリーナへ移動し、Zombieベースの練習ボットと1対1で戦闘
- 防具ティア、勝敗方式（死亡 / ヒット数先取）、Beastモード、敵の強さを `/pvpbot` コマンドまたはGUIで設定
- ボットはストレーフ、WTAP、ジャンプリセット、クリットを組み合わせた攻撃的なAIで動作
  - プレイヤーとの距離に応じて加速し、近接時でも小刻みに動いて照準を外させる
  - `pvpbot-common.toml` の `ai.*` / `botBaseMovementSpeed` / `botSpeedAmplifier` で俊敏さを調整可能
- 終了時は開始前の位置・視点・ゲームモードへ復帰
- 設定は再起動後も `pvpbot-common.toml` に自動保存される
- start 連打・死亡リスポーン時のボット2体化を防止

## 開発環境

- Java 25（Gradle Toolchain により自動ダウンロードされるため、手動インストールは不要）
- Gradle 9.2.1
- VS Code

ローカル環境で `./gradlew build` などを直接実行できます。`.devcontainer/` は参考用として残していますが、現在は Dev Container を使わずに開発しています。

## 主なコマンド

```bash
./gradlew build         # モッドJARをビルド
./gradlew runClient     # デベロッパークライアント起動
./gradlew runServer     # デベロッパーサーバー起動
./gradlew data          # データ生成
./gradlew clean         # ビルド成果物をクリア
```

VS Code の `.vscode/launch.json` から Client / Server / Data / GameTestServer の各デバッグ起動も可能です。

## ゲーム内コマンド

| コマンド | 説明 |
|---|---|
| `/pvpbot start` | 戦闘開始 |
| `/pvpbot quit` | 戦闘をリタイア |
| `/pvpbot status` | 現在の設定を表示 |
| `/pvpbot menu` | 設定GUIを開くアイテムを付与 |
| `/pvpbot armor <0-3>` | 防具ティア（0=革, 1=鉄, 2=ダイヤ, 3=ネザライト） |
| `/pvpbot boxing <0-4>` | 勝敗方式（0=死亡, 1=50, 2=100, 3=500, 4=1000ヒット） |
| `/pvpbot beast <true/false>` | Beastモード切替 |
| `/pvpbot strength <0-4>` | 敵の強さ（0=弱, 1=易, 2=普通, 3=強, 4=激強） |
| `/pvpbot hitsdebug <count>` | ヒット数決着の閾値を一時変更（検証用） |

## ボットの動きを調整する

設定ファイル `pvpbot-common.toml` は、実行環境の `config/` フォルダーに自動生成されます。例えば Windows の標準ランチャーであれば以下のようなパスに保存されています。

```
C:\Users\<ユーザー名>\AppData\Roaming\.minecraft\versions\neoforge-26.1.2.84\config\pvpbot-common.toml
```

このファイルで以下の項目を変更できます。

| 項目 | 効果 | 推奨値 |
|---|---|---|
| `ai.aiChaseSpeed` | 間合い外での接近速度 | 0.35〜0.6 |
| `ai.aiStrafeSpeed` | サイドステップの大きさ | 0.35〜0.7 |
| `ai.aiErraticChance` | 方向転換を不規則にする確率 | 0.18〜0.5 |
| `ai.aiRandomDodgeChance` | ランダム回避ステップの確率 | 0.15〜0.5 |
| `botBaseMovementSpeed` | ボットの基礎移動速度 | 0.45〜0.7 |
| `botSpeedAmplifier` | ボットのスピード効果レベル（0=Speed I） | 1〜4 |
| `botFollowRange` | プレイヤーを追跡する最大距離 | 512 |

値を大きくするとボットがより俊敏・攻撃的になります。逆に緩くしたい場合は小さくしてください。

## Dev Containerについて

`.devcontainer/` は参考用に残していますが、現在は使用していません。Gradle ラッパーが Java を自動的にダウンロードするため、Dev Container なしでローカル開発が可能です。

過去に Dev Container を使用した場合、ストレージ容量を節約するために以下のコマンドで不要なコンテナ・イメージ・ボリュームを削除できます。

```bash
# 停止中のコンテナを削除
docker container prune

# 未使用のイメージを削除
docker image prune

# 未使用のボリュームを削除（Gradle キャッシュなど）
docker volume prune
```

上記の `docker volume prune` は `neoforge-gradle-cache` などの未使用ボリュームも削除します。実行する前に削除対象のボリュームを確認してください。

### トラブルシューティング

- **`generateModMetadata` で `Could not set file mode 777` エラーになる**  
  Dev Container 内の `/workspaces` は Windows ホスト側のファイルシステムにマウントされており、Gradle が `chmod` を適用できないことが原因です。  
  `build.gradle` では Linux 環境下で `layout.buildDirectory` を `/tmp/neoforge-pvpbot-build` に移動するよう設定済みです。コンテナ内ではこのディレクトリにビルド出力が作成されるため、権限エラーは回避されます。

## ドキュメント

- CLAUDE.md — コードベースの構造と主要設計
- SETUP_NOTES.md — セットアップ手順と受け入れ基準
- 公式NeoForgeドキュメント: https://docs.neoforged.net/

## ライセンス

All Rights Reserved

Mapping Names: 本モッドはMojang公式マッピングを使用しています。詳細は以下を参照してください。
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md