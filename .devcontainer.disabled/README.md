# DevContainer 設定（無効化中）

このディレクトリには DevContainer の設定ファイルが含まれていますが、**現在は無効化されています**。

## 無効化の理由

DevContainer を使わずにローカル環境で開発するため、ディレクトリ名を `.devcontainer` から `.devcontainer.disabled` に変更し、VS Code の自動検知を回避しています。

## 含まれるファイル

| ファイル | 説明 |
|---|---|
| `devcontainer.json` | DevContainer のメイン設定 |
| `Dockerfile` | コンテナイメージ定義（eclipse-temurin:25-jdk ベース） |
| `devcontainer-lock.json` | Features のバージョンロック |

## 再び有効化する手順

ディレクトリ名を `.devcontainer` に戻すだけで、いつでも DevContainer を再開できます。

```bash
# Windows (cmd)
move .devcontainer.disabled .devcontainer

# Windows (PowerShell)
Move-Item .devcontainer.disabled .devcontainer

# Linux / macOS
mv .devcontainer.disabled .devcontainer
```

リネーム後、VS Code でプロジェクトを開き直すと「Dev Containers: Reopen in Container」が再び利用可能になります。