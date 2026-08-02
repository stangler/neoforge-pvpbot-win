# セットアップ手順(Windows 11 + Docker Desktop + VS Code)

## 済んでいる作業
- `.devcontainer/Dockerfile` / `devcontainer.json` 作成済み
- `NeoForgeMDKs/MDK-26.1.2-ModDevGradle` 公式テンプレをclone・`.git`除去・展開済み
- バージョン実値 確認済み(2026-07-26時点):
  - `neo_version=26.1.2.84`(仕様書一致)
  - Gradle Wrapper `9.2.1`(要件9.1.0以上 満たす)
  - Java toolchain `25`(仕様書一致)
- `gradle.properties` の `mod_id`/`mod_name`/`mod_group_id` を変更済み
  - `mod_id=pvpbot`
  - `mod_name=PvP Practice Bot`
  - `mod_group_id=net.nekometa.pvpbot`
  - (Javaパッケージ`com.example.examplemod`はスコープ外につき未変更。
    実コード移植時にリネーム要)

## 未実施(このサンドボックスでは実行不可。要ローカル実行)
このプロジェクト構築はClaudeのサンドボックス環境では以下理由により
検証未了:
- サンドボックスJavaが21系(toolchain要求は25)
- サンドボックスのネットワーク許可リストに
  `maven.neoforged.net` / `libraries.minecraft.net` /
  `piston-meta.mojang.com` 等が含まれず、Gradle依存解決不可

**ローカルでの手順:**

1. このZIPを展開し、VS Codeで開く
2. コマンドパレット → `Dev Containers: Reopen in Container`
3. 初回ビルドはコンテナイメージ取得(`eclipse-temurin:25-jdk`)+
   `postCreateCommand`で`chmod +x gradlew && ./gradlew --version`実行
   - タグが見つからない場合は `.devcontainer/Dockerfile` の
     `FROM eclipse-temurin:25-jdk` を `eclipse-temurin:25-jdk-noble` に変更
4. コンテナ内ターミナルで:
   ```
   ./gradlew build
   ```
   初回はMinecraft本体・マッピング・NeoForgeライブラリDLで
   数分〜数十分かかる
5. 続けて:
   ```
   ./gradlew runClient
   ```
   起動後Mod一覧に`PvP Practice Bot`が表示されればOK

## 受け入れ基準チェックリスト(仕様書セクション8)
- [ ] `java -version` が `25` 系
- [ ] `./gradlew build` 成功
- [ ] `./gradlew runClient` でクライアント起動、Mod一覧に表示
- [ ] VS Code上でJavaファイルのコード補完・定義ジャンプが機能
- [ ] コンテナ再作成後もGradleキャッシュ(named volume)が保持され再ビルド高速化

## ネットワーク要件(社内プロキシ等がある場合、許可リストに追加)
- `maven.neoforged.net`
- `libraries.minecraft.net`
- `piston-meta.mojang.com` / `piston-data.mojang.com`
- `repo.maven.apache.org`
- `plugins.gradle.org`
