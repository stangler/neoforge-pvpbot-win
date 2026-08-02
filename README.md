# PvP Practice Bot

Minecraft 26.1.2 / NeoForge 26.1.2 向けの練習用PvPボットモッドです。
元データパック（mcfunction）で実装されていたPvP練習ボットを、JavaのNeoForgeモッドへ移植したものです。

## 機能

- `/pvpbot start` で高所アリーナへ移動し、Zombieベースの練習ボットと1対1で戦闘
- 防具ティア、勝敗方式（死亡 / ヒット数先取）、Beastモード、敵の強さを `/pvpbot` コマンドまたはGUIで設定
- ボットはストレーフ、WTAP、ジャンプリセット、クリットを組み合わせたAIで動作
- 終了時は開始前の位置・視点・ゲームモードへ復帰

## 開発環境

- Java 25
- Gradle 9.2.1
- VS Code + Dev Container（推奨）

## 主なコマンド

```bash
./gradlew build         # モッドJARをビルド
./gradlew runClient     # デベロッパークライアント起動
./gradlew runServer     # デベロッパーサーバー起動
./gradlew data          # データ生成
./gradlew clean         # ビルド成果物をクリア
```

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

## Dev Containerでの開発

1. VS Codeでこのリポジトリを開く
2. コマンドパレット → `Dev Containers: Reopen in Container`
3. コンテナ内ターミナルで `./gradlew build`

初回はMinecraft本体・マッピング・NeoForgeライブラリのダウンロードで数分〜数十分かかります。
Gradleキャッシュはnamed volumeで永続化されるため、コンテナ再作成後も再ビルドが高速化されます。

## ドキュメント

- CLAUDE.md — コードベースの構造と主要設計
- SETUP_NOTES.md — セットアップ手順と受け入れ基準
- 公式NeoForgeドキュメント: https://docs.neoforged.net/

## ライセンス

All Rights Reserved

Mapping Names: 本モッドはMojang公式マッピングを使用しています。詳細は以下を参照してください。
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md
