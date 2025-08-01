# 檢舉監控除錯指南

## 概述

本指南說明如何使用新增的除錯功能來監控和測試檢舉系統的運作狀況。

## 新增的除錯功能

### 1. 增強的日誌輸出

所有相關組件現在都會輸出詳細的除錯資訊，包括：

- **API 請求詳情**：URL、參數、認證狀態
- **響應資料**：完整的後端回應
- **錯誤詳情**：HTTP 狀態碼、錯誤訊息、時間戳
- **狀態變化**：監控狀態的即時更新

### 2. 檢舉監控 Composable 增強

`useCommentReportMonitor.js` 現在提供：

- **詳細的輪詢統計**：成功/失敗次數、執行時間
- **API 使用追蹤**：記錄使用哪個 API 端點
- **狀態變化監控**：即時追蹤留言狀態變化
- **智能停止機制**：當留言被處理後自動停止監控

### 3. 除錯面板組件

新增 `DebugPanel.vue` 組件，提供：

- **即時監控狀態**：輪詢狀態、監控中的留言數量
- **測試控制**：手動開始/停止監控特定留言
- **事件日誌**：即時顯示所有相關事件
- **日誌匯出**：可匯出除錯日誌供分析

## 使用方法

### 1. 查看控制台日誌

打開瀏覽器開發者工具的控制台，您會看到詳細的除錯資訊：

```
🔍 開始載入檢舉列表: {page: 0, size: 20, filterStatus: "", timestamp: "上午9:45:30"}
📤 發送檢舉列表請求: {url: "/api/comment-reports/admin", params: {...}, token: "存在"}
✅ 檢舉列表載入成功: {totalReports: 5, totalItems: 5, currentPage: 0, ...}
```

### 2. 使用除錯面板

在需要測試的頁面中加入除錯面板：

```vue
<template>
  <div>
    <!-- 您的頁面內容 -->
    <DebugPanel />
  </div>
</template>

<script setup>
import DebugPanel from '@/components/comments/DebugPanel.vue'
</script>
```

### 3. 測試檢舉流程

1. **開始檢舉**：
   - 在留言上點擊檢舉按鈕
   - 選擇檢舉原因並提交
   - 觀察控制台輸出檢舉過程

2. **監控狀態變化**：
   - 檢舉成功後會自動開始監控該留言
   - 每 5 秒檢查一次留言狀態
   - 當管理員處理檢舉後，會即時更新狀態

3. **管理員處理**：
   - 管理員在檢舉列表頁面處理檢舉
   - 選擇處理或駁回
   - 觀察控制台輸出處理過程

## 除錯資訊說明

### 日誌類型

- 🔍 **查詢**：API 請求開始
- 📤 **發送**：請求發送詳情
- ✅ **成功**：操作成功完成
- ❌ **錯誤**：操作失敗
- 🔄 **輪詢**：輪詢機制運作
- ⏹️ **停止**：監控停止
- 🛑 **處理**：留言被處理

### 重要欄位

- **timestamp**：操作時間戳
- **commentId**：留言 ID
- **apiUsed**：使用的 API 端點
- **status**：HTTP 狀態碼
- **duration**：執行時間
- **error**：錯誤詳情

## 常見問題除錯

### 1. API 請求失敗

檢查控制台輸出：
```
❌ 載入檢舉列表失敗: {
  error: {status: 403, statusText: "Forbidden", ...},
  params: {...},
  timestamp: "..."
}
```

可能原因：
- 認證問題（token 無效）
- 權限不足
- 後端服務未啟動

### 2. 輪詢不工作

檢查：
- 是否有留言在監控中
- 輪詢間隔是否正確（5秒）
- 網路連線是否正常

### 3. 狀態更新不及時

檢查：
- 後端 API 是否正確回應
- 留言狀態是否確實變化
- 事件監聽器是否正常工作

## 效能監控

### 輪詢統計

每次輪詢都會顯示統計資訊：
```
📊 輪詢完成統計: {
  totalComments: 2,
  successCount: 2,
  errorCount: 0,
  duration: "150ms",
  timestamp: "上午9:45:35"
}
```

### 建議

- 監控 `duration` 確保 API 回應時間合理
- 檢查 `errorCount` 確保錯誤率在可接受範圍
- 觀察 `totalComments` 確保不會監控過多留言

## 開發模式

在開發環境中，系統會：

1. **模擬認證**：如果沒有 token，會創建模擬 token
2. **詳細日誌**：輸出所有操作詳情
3. **錯誤寬容**：提供更多錯誤資訊

## 生產環境

在生產環境中，建議：

1. **關閉詳細日誌**：只保留必要的錯誤日誌
2. **調整輪詢間隔**：根據實際需求調整
3. **監控效能**：定期檢查輪詢統計

## 故障排除

如果遇到問題：

1. 檢查瀏覽器控制台是否有錯誤
2. 確認後端 API 是否正常運作
3. 驗證認證狀態是否正確
4. 檢查網路連線是否穩定
5. 使用除錯面板進行即時監控

## 聯絡支援

如果問題持續存在，請提供：

1. 瀏覽器控制台的完整日誌
2. 除錯面板的事件日誌
3. 後端 API 的錯誤日誌
4. 重現問題的步驟說明 