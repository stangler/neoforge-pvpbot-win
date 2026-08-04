# PvP Practice Bot

Minecraft 26.1.2 / NeoForge 26.1.2 向けの練習用PvPボットモッドです。
元データパック（mcfunction）で実装されていたPvP練習ボットを、JavaのNeoForgeモッドへ移植したものです。

このモッドは [CurseForge: PvP Bot Practice World](https://www.curseforge.com/minecraft/worlds/pvp-bot-practice-world) にインスパイアされて作成されました。

## 機能

- `/pvpbot start` で高所アリーナへ移動し、Zombieベースの練習ボットと1対1で戦闘
- 防具ティア、勝敗方式（死亡 / ヒット数先取）、敵の強さを `/pvpbot` コマンドまたはGUIで設定
- **アリーナ設定**: 高さ、半径、壁の高さ、ボットとの距離、奈落マージンをGUIで調整可能
- **ボット行動設定**: ストレーフ速度、チェイス速度、不規則行動確率、回避確率をGUIで調整可能
- 設定GUI（`/pvpbot menu`）は2列レイアウト＋マウスホイールでの縦スクロールに対応。下段の「開始」「閉じる」ボタンはスクロールに関わらず常に固定表示
- ワールド参加時、`/pvpbot menu` が自動実行され設定メニューアイテムが自動付与される（毎回コマンドを打つ必要なし。既に持っている場合は付与しない）
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

ローカル環境で `./gradlew build` などを直接実行できます。`.devcontainer.disabled/` に設定を残したうえで Dev Container を無効化しています。再有効化の手順は[後述](#dev-containerについて)を参照してください。

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
| `/pvpbot menu` | 設定GUIを開くアイテムを付与（ワールド参加時に自動実行済み） |
| `/pvpbot armor <0-3>` | 防具ティア（0=革, 1=鉄, 2=ダイヤ, 3=ネザライト） |
| `/pvpbot boxing <0-4>` | 勝敗方式（0=死亡, 1=50, 2=100, 3=500, 4=1000ヒット） |
| `/pvpbot strength <0-4>` | 敵の強さ（0=弱, 1=易, 2=普通, 3=強, 4=激強） |
| `/pvpbot hitsdebug <count>` | ヒット数決着の閾値を一時変更（検証用） |

## 設定GUI (`/pvpbot menu`)

右クリックで開く設定メニューでは、以下の項目をGUI上で変更できます。変更は即座に `pvpbot-common.toml` に保存され、次回起動時も保持されます。戦闘設定は「開始」ボタン押下時にサーバー側のコマンドへ反映されます。

### 戦闘設定（左列）

| ボタン | 選択肢 | 説明 |
|---|---|---|
| **Enemy Armor** | 革 / 鉄 / ダイヤ / ネザライト | ボットが装備する防具のティア。ティアが高いほど防御力が上がり、プレイヤーの与ダメージが減少します。 |
| **Player Armor** | 革 / 鉄 / ダイヤ / ネザライト | プレイヤーに自動装備される防具のティア。自分自身の被ダメージ量を調整する用途で使います。 |
| **Boxing Mode** | Off / 50 / 100 / 500 / 1000 Hits | 勝敗の判定方式。`Off` で死亡決着、それ以外では先に指定ヒット数を与えた側の勝利になります。 |
| **Strength** | 弱 / 易 / 普通 / 強 / 激強 | ボットの攻撃力と体力をまとめて調整。高いほどボットの与ダメージと最大HPが増加します。 |
| **Player Speed** | None / Speed I〜IX | プレイヤーに付与する移動速度上昇効果。素早い立ち回い練習や追い打ち練習に使います。 |
| **Bot Speed** | None / Speed I〜IX | ボットに付与する移動速度上昇効果。素早いボットを相手にしたい場合に上げます。 |

### アリーナ設定

| ボタン | 初期値 | 範囲 | 説明 |
|---|---|---|---|
| **Arena Y** | 260 | -64 〜 2032（16刻み） | 高所アリーナの生成高さ。地上の地形を破壊せずに戦闘するために使用します。 |
| **Arena Radius** | 8 | 4 〜 64（2刻み） | アリーナ床の半径。小さいと近接戦が強制され、大きいと走位の幅が広がります。 |
| **Wall Height** | 4 | 3 〜 12（1刻み） | アリーナのガラス壁の高さ。低いと飛び降りやすく、高いと囲いが強くなります。 |
| **Bot Offset Z** | 6.0 | 2.0 〜 32.0（1.0刻み） | 開始時にボットをプレイヤー正面からどれだけ離すか。大きいほど間合いが離れます。 |
| **Void Fall Margin** | 8.0 | 1.0 〜 64.0（2.0刻み） | 奈落判定までの余白。床よりこの距離だけ下に落ちると敗北扱いになります。 |

### ボット行動設定

| ボタン | 初期値 | 範囲 | 説明 |
|---|---|---|---|
| **Strafe Speed** | 65 | 5 〜 80（5刻み） | 左右サイドステップの強さ。高いほどボットが横に大きく動き、照準を外しやすくなります。 |
| **Chase Speed** | 65 | 0 〜 90（5刻み） | プレイヤーへの接近速度。高いほど距離を詰める動きが速くなります。 |
| **Erratic Chance** | 35 | 0 〜 80（5刻み） | サイドステップの方向を不規則に切り替える確率。高いほど左右の動きがランダムになり、予測しにくくなります。 |
| **Dodge Chance** | 35 | 0 〜 80（5刻み） | ランダムに回避ステップを踏む確率。高いほど被弾を避ける動きが増えます。 |

### 下段ボタン

| ボタン | 説明 |
|---|---|
| **開始** | 現在のGUI設定を `/pvpbot` コマンドでサーバーに送信し、`/pvpbot start` を実行して戦闘を開始します。 |
| **閉じる** | 設定GUIを閉じます。変更済みの戦闘設定はまだサーバーに反映されていない場合があるため、開始する場合は「開始」を押してください。 |

### GUIの操作

- 設定ボタンは2列レイアウトで、マウスホイールで縦スクロールできます。
- 下段の「開始」「閉じる」ボタンはスクロールの影響を受けず、常に固定表示されます。
- 各数値ボタンをクリックするたびに、表の範囲内で次の値へ循環します。

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

現在は Dev Container を無効化し、ローカル環境で開発しています。設定ファイルは `.devcontainer.disabled/` に残しており、いつでも復元可能です。Gradle ラッパーが Java を自動的にダウンロードするため、Dev Container なしでローカル開発が可能です。

### 再有効化する手順

ディレクトリ名を `.devcontainer` に戻すだけで、VS Code の「Dev Containers: Reopen in Container」が再び利用可能になります。

```bash
# Windows (cmd)
move .devcontainer.disabled .devcontainer

# Linux / macOS
mv .devcontainer.disabled .devcontainer
```

詳細は `.devcontainer.disabled/README.md` を参照してください。

### 不要な Docker リソースの削除

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