# Expanded Delight (Bukkit / Paper / Folia Addon)

[English](README.md) | 繁體中文

Minecraft 知名農夫樂事附屬模組 **[Expanded Delight](https://github.com/ianm1647/ExpandedDelight)** 的 Bukkit / Paper / Folia 原生插件移植版本！

本專案利用 [CraftEngine](https://github.com/Momirealms/CraftEngine) 作為底層驅動，完整重現了模組中的作物、果汁機、起司製作、肉桂樹、各式料理與特色工具。

---

## ✨ 功能特色

- 🌾 **全新作物與野生植物**：
  - 蘆筍 (Asparagus)、甜薯 (Sweet Potato)、辣椒 (Chili Pepper)、花生 (Peanut)、蔓越莓 (Cranberry)。
  - 完整支援原版骨粉催熟、自然生長、耕地保水以及世界自然生成。
- 🍹 **果汁機 (Juicer)**：
  - 支援各種水果與牛奶壓榨合成特色果汁與冰沙。
  - 具備完整 GUI 互動介面與工作進度動畫。
- 🧀 **陳釀桶 (Cask) 與起司發酵**：
  - 支援山羊奶與牛奶熟成起司輪、可分塊切食。
- 🌳 **肉桂樹與剝皮機制**：
  - 肉桂樹世界生成，支援使用刀具直接右鍵剝皮取得肉桂樹皮。
- 🍳 **農夫樂事聯動**：
  - 支援砧板切片（切蔬菜、切起司、切派）與料理鍋烹飪配方。
- 🌍 **多語言支援**：
  - 內建 `zh_tw` (繁體中文) 與 `zh_cn` (簡體中文)。

---

## 📦 安裝與前置需求

1. **伺服器核心**：Paper 1.21.x 或 Folia 1.21.x（Java 21+）
2. **前置插件**：
   - [CraftEngine](https://github.com/Momirealms/CraftEngine) (v26.9+)
   - [FarmersDelight-Plugin](https://github.com/huidu)
3. **安裝步驟**：
   - 將編譯後的 `expandeddelight-1.0.0.jar` 放入伺服器的 `plugins/` 目錄。
   - 啟動伺服器，插件會自動將資源包與配置加載至 CraftEngine。

---

## 🛠️ 編譯專案

```bash
git clone https://github.com/your-username/ExpandedDelight.git
cd ExpandedDelight
./gradlew build
```
編譯產物將生成於 `build/libs/expandeddelight-1.0.0.jar`。

---

## 📄 授權條款 (License & Credits)

- 本專案採用 **[MIT License](LICENSE)** 開源授權。
- 原作模組 **Expanded Delight** 由 [ianm1647](https://github.com/ianm1647) 設計與開發，亦採用 **MIT License**。
- 詳情請參閱 [NOTICE.md](NOTICE.md)。
