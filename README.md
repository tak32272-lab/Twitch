# MonstSwitch

モンスト v33.0.0 Mod APK 用のフローティング・コントローラー。

モンストプロセス内の libhook (Frida script) とソケット通信 (127.0.0.1:28900) して、各 Mod 機能を画面上のトグルで ON/OFF できる。

## ビルド

GitHub にプッシュすると Actions が自動で APK をビルド & Release にアップロード。

```
git init
git add -A
git commit -m "init"
git branch -M main
git remote add origin https://github.com/<user>/MonstSwitch.git
git push -u origin main
```

Actions タブ → 最新 run → Artifacts または Releases から `MonstSwitch.apk` をダウンロード。

## 使い方

1. モンスト Mod APK (`monst_v18_mod.apks`) をインストール
2. `MonstSwitch.apk` をインストール
3. MonstSwitch 起動 → 「Switch起動」ボタン → オーバーレイ権限を許可
4. モンストに切り替え → 画面に ⚙ ボタンが出現
5. タップでトグルパネル展開、各機能を ON/OFF

## 機能

- 👊 ワンパン (ダメージ50倍)
- 🛡 無敵 + HP0操作可
- ⚡ SS無限
- 💥 クリティカル常時
- ✨ Fantastics/ラックスキル常時
- 🎯 弱点ヒット常時
- 🗡 キラー常時
- 🚫 ギミック無効 (DW/地雷/ワープ)
- 🏃 速度2倍
- 🍀 ドロップ率MAX
- 🏁 自動クリア
- 🔢 ターン偽装

## 構成

- `app/src/main/java/jp/co/monst/switchpanel/`
  - `MainActivity.java` - 起動/停止UI
  - `OverlayService.java` - フローティングUI + ソケット送信
- `app/src/main/res/` - レイアウト・リソース
- `.github/workflows/build.yml` - GitHub Actions ビルド定義
