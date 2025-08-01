# Journey.com 旅遊網站項目摘要

## 項目概述

Journey.com 是一個綜合性旅遊預訂平台，提供機票、飯店、景點門票和交通服務的預訂功能。網站採用現代化的響應式設計，支援多種用戶角色和完整的購物體驗。

## 技術架構

- **前端框架**: Vue 3 + Composition API
- **路由管理**: Vue Router 4
- **UI 框架**: Bootstrap 5 + Bootstrap Icons
- **構建工具**: Vite
- **狀態管理**: localStorage + 事件驅動
- **後端**: Spring Boot (Java)

## 主要功能

### 1. 用戶認證與角色管理

- **登入/註冊系統**: 簡化的本地存儲認證
- **角色權限**:
  - 一般用戶: 瀏覽、收藏、購物車
  - 管理員: 後台管理、數據統計
  - 商家: 商品管理、訂單處理
- **測試帳戶**:
  - 管理員: admin/admin123
  - 一般用戶: user/user123

### 2. 商品瀏覽與搜尋

- **多類別商品**: 機票、飯店、景點門票、交通服務
- **進階搜尋**: 目的地、日期、價格範圍篩選
- **商品詳情**: 詳細資訊、圖片、評價系統
- **響應式設計**: 適配桌面和移動設備

### 3. 購物車與收藏系統

- **旅程管理**: 添加商品到購物車，數量控制
- **收藏功能**: 收藏喜愛的商品
- **即時同步**: 跨頁面數據同步
- **未登入支援**: 使用 localStorage 暫存數據

### 4. 會員中心 (新增)

- **個人資料管理**: 基本資料編輯、頭像上傳
- **訂單管理**: 查看訂單歷史、狀態追蹤
- **收藏管理**: 管理收藏的商品
- **旅程管理**: 查看和編輯購物車內容
- **帳戶設定**: 密碼修改、通知偏好設定
- **響應式設計**: 側邊欄導航，適配移動設備

### 5. 結帳系統

- **折價券功能**:
  - 多種類型: 百分比折扣、固定金額折抵
  - 使用條件: 最低消費金額、適用商品類型
  - 即時計算: 自動計算折扣後總價
- **聯絡資訊**: 姓名、電話、Email
- **乘客資訊**: 多人資料管理
- **付款方式**: 信用卡、LINE Pay、銀行轉帳
- **發票設定**: 個人、公司、捐贈發票

### 6. 管理後台

- **數據統計**: 銷售數據、用戶統計
- **商品管理**: 新增、編輯、刪除商品
- **訂單管理**: 訂單狀態更新、處理
- **用戶管理**: 用戶資料、權限管理
- **評價管理**: 評論審核、回覆

### 7. 客服系統

- **即時聊天**: 客服對話介面
- **常見問題**: FAQ 分類展示
- **聯絡方式**: 多種聯絡管道

## 頁面結構

### 主要頁面

1. **首頁** (`/home`): 輪播圖、熱門商品、搜尋功能
2. **機票頁面** (`/flights`): 航班搜尋、篩選、預訂
3. **飯店頁面** (`/hotels`): 飯店搜尋、房型選擇
4. **門票頁面** (`/tickets`): 景點門票、套票選擇
5. **交通頁面** (`/traffic`): 租車、接送服務
6. **搜尋結果** (`/search`): 進階搜尋結果展示

### 用戶頁面

1. **會員中心** (`/profile`): 個人資料、訂單、收藏管理
2. **我的旅程** (`/my-journey`): 購物車管理、結帳
3. **訂單頁面** (`/orders`): 訂單歷史查看
4. **結帳頁面** (`/checkout`): 訂單確認、付款
5. **登入頁面** (`/login`): 用戶認證
6. **註冊頁面** (`/register`): 新用戶註冊

### 管理頁面

1. **管理後台** (`/admin`): 數據統計、商品管理
2. **客服頁面** (`/customer-service`): 客服對話、FAQ

## 組件架構

### 核心組件

- **Header**: 導航欄、用戶選單、搜尋功能
- **Footer**: 網站資訊、連結
- **Banner**: 輪播圖展示
- **SearchBar**: 搜尋功能
- **AdvancedSearch**: 進階搜尋表單

### 商品組件

- **HotelCard**: 飯店卡片，包含收藏和購物車按鈕
- **TicketCard**: 門票卡片，包含收藏和購物車按鈕
- **TrafficCard**: 交通服務卡片，包含收藏和購物車按鈕
- **FlightCard**: 航班卡片

### 功能組件

- **OrderTable**: 訂單列表展示
- **Comment**: 評論展示
- **CommentForm**: 評論發表表單
- **ChatRoom**: 客服聊天室
- **CustomerService**: 客服介面
- **RecommendationSystem**: 推薦系統

## 數據管理

### 本地存儲

- **用戶狀態**: 登入狀態、角色、用戶名
- **購物車**: 旅程商品、數量
- **收藏**: 收藏的商品列表
- **結帳數據**: 傳遞到結帳頁面的數據

### 事件驅動更新

- **journeyUpdated**: 購物車數據更新事件
- **favoritesUpdated**: 收藏數據更新事件
- **實時同步**: 跨組件數據同步

## 折價券系統

### 折價券類型

1. **新用戶專享**: 首次購買享 9 折優惠
2. **滿額折抵**: 消費滿 5000 元折抵 500 元
3. **飯店專用券**: 飯店預訂享 8 折優惠
4. **門票優惠券**: 景點門票折抵 200 元
5. **交通優惠券**: 交通服務享 85 折優惠

### 使用規則

- **最低消費**: 每張折價券都有最低消費金額限制
- **適用商品**: 指定適用於特定商品類型
- **有效期**: 折價券有效期管理
- **自動計算**: 即時計算折扣金額

## 響應式設計

### 桌面版

- 完整功能展示
- 側邊欄導航
- 多欄位佈局

### 移動版

- 漢堡選單
- 單欄位佈局
- 觸控優化

## 未來規劃

### 短期目標

1. **支付整合**: 整合真實支付系統
2. **後端 API**: 完善後端 API 接口
3. **數據庫**: 實現持久化數據存儲
4. **郵件通知**: 訂單確認郵件發送

### 中期目標

1. **多語言支援**: 英文、日文等多語言版本
2. **移動應用**: 開發原生移動應用
3. **社交功能**: 用戶評價、分享功能
4. **推薦算法**: 智能商品推薦

### 長期目標

1. **AI 客服**: 智能客服機器人
2. **虛擬旅遊**: VR/AR 旅遊體驗
3. **區塊鏈**: 去中心化預訂系統
4. **生態系統**: 完整的旅遊服務生態

## 部署說明

### 開發環境

```bash
# 安裝依賴
npm install

# 啟動開發服務器
npm run dev

# 構建生產版本
npm run build
```

### 生產部署

- 使用 Nginx 作為 Web 服務器
- 配置 HTTPS 證書
- 設置 CDN 加速
- 數據庫備份策略

## 維護與支援

### 定期維護

- 依賴包更新
- 安全漏洞修復
- 性能優化
- 用戶體驗改進

### 技術支援

- 文檔更新
- 問題追蹤
- 用戶反饋處理
- 功能擴展支援

---

**最後更新**: 2024 年 1 月
**版本**: 1.0.0
**開發團隊**: iSpan Team EE199
