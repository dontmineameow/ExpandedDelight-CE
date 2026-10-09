<div align="center">

# Expanded Delight

Folia not been tested！！！
if it show some bug to u
just tell me to let me know

[English](README.md) | 繁體中文

### 農夫樂事 Paper / Bukkit 附屬插件移植版

Minecraft 知名農夫樂事附屬模組 **[Expanded Delight](https://github.com/ianm1647/ExpandedDelight)** 的原生伺服器插件移植版本。  
基於 **[CraftEngine](https://github.com/Momirealms/CraftEngine)** 驅動，完整重現模組內的作物、果汁機、起司製作、肉桂樹、各式料理與特色工具。

---

## 功能特色

### 作物與野生植物
- **新增作物**：蘆筍 (Asparagus)、甜薯 (Sweet Potato)、辣椒 (Chili Pepper)、花生 (Peanut)、蔓越莓 (Cranberry)。
- **完整生態**：支援骨粉催熟、自然生長週期、耕地保水以及世界野生作物自然生成。

### 廚房與加工設備
- **果汁機 (Juicer)**：放入各類水果、冰塊或牛奶壓榨，支援專屬 GUI 互動介面與加工進度動畫。
- **陳釀桶 (Cask)**：使用牛奶或山羊奶熟成起司輪，熟成後可切塊食用或分裝。
- **烹飪與料理**：完整對接農夫樂事料理鍋 (Cooking Pot) 與砧板 (Cutting Board) 切片機制。

### 肉桂樹與林業生態
- **世界生成**：叢林與稀疏叢林群系中自然生成肉桂樹。
- **剝皮機制**：手持刀具直接右鍵點擊肉桂原木即可剝取肉桂樹皮。
- **培育繁殖**：破壞樹葉可取得肉桂樹苗，支援骨粉催熟成長。

### 其他內容
- **鹽礦與調味**：海中生成鹽礦，提供烹飪調味料。
- **多語言支援**：內建繁體中文 (`zh_tw`) 與簡體中文 (`zh_cn`)。

---

## 安裝與前置需求

> [!NOTE]
> **當前測試環境與相容性**
> - **已實測環境**：Paper 1.21.x（配合 CraftEngine 26.9.x / 核心版本 26.1.2）。
> - **Folia 相容性**：底層架構已考量區塊執行序安全，但**尚未經過完整的 Folia 多執行緒實機壓力測試**。
> - **注意事項**：若要應用於生產或大型伺服器，**強烈建議請先在個人的測試伺服器進行實機測試與配置微調**，確認與其他插件無衝突後再正式上線。

### 前置依賴需求
1. **Java**：Java 21 或更高版本
2. **伺服器核心**：Paper 1.21.x
3. **前置插件**：
   - **CraftEngine** (v26.9+)
   - **FarmersDelight-Plugin**

---

## 安裝指南

1. 下載最新發布的 `ExpandedDelight-1.0.0.jar`。
2. 將檔案放入伺服器的 `plugins/` 資料夾內。
3. 確保已安裝 `CraftEngine` 與 `FarmersDelight` 主插件。
4. 啟動伺服器，插件將自動向 CraftEngine 註冊資源包與自訂方塊/物品。

---

## 專案編譯

本專案使用 Gradle 進行建置：

```bash
git clone https://github.com/your-username/ExpandedDelight.git
cd ExpandedDelight
./gradlew build
```

編譯完成的插件 Jar 檔案位於：`build/libs/ExpandedDelight-1.0.0.jar`

---

## 開源授權與致謝

- 本專案採用 **[MIT License](LICENSE)** 條款開源。
- 原作模組 **Expanded Delight** 由 **[ianm1647](https://github.com/ianm1647)** 設計與開發，原模組亦採用 **MIT License**。
- 所有原創美術資源（貼圖、模型）之版權皆屬原作者所有，詳細授權說明請參閱 [NOTICE.md](NOTICE.md)。
