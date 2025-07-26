# 前端 SSE 實作說明

## 1. 概述
本文件說明如何在 Vue.js 前端專案中整合 SSE (Server-Sent Events) 實現即時通知，支援檢舉、留言、系統等多種通知場景。

## 2. 技術架構
- **框架**：Vue.js 3 (Composition API)
- **UI**：Bootstrap 5 + Bootstrap Icons
- **狀態管理**：Vue 3 原生狀態管理 (ref, reactive, computed)
- **HTTP**：Axios
- **通知庫**：SweetAlert2
- **SSE**：原生 EventSource

## 3. 安裝與設定

### 3.1 複製服務檔案
```bash
# 複製到專案目錄
cp sse-notification-service.js src/services/
cp vue-sse-plugin.js src/plugins/
```

### 3.2 在 main.js 註冊插件
```js
// src/main.js
import { createApp } from 'vue'
import App from './App.vue'
import { SSEPlugin } from './plugins/vue-sse-plugin.js'

const app = createApp(App)
app.use(SSEPlugin)
app.mount('#app')
```

## 4. 核心 SSE 服務

```js
// src/services/sse-notification-service.js
class SSENotificationService {
  constructor() {
    this.eventSource = null;
    this.isConnected = false;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 10;
    this.reconnectDelay = 3000;
    this.baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://192.168.36.96:8080';
  }

  connect(token, userType, callbacks = {}) {
    if (!token) {
      console.error('❌ SSE 連接失敗: 缺少 JWT Token');
      return;
    }

    if (this.isConnected) {
      console.log('⚠️ SSE 連接已存在，先斷開舊連接');
      this.disconnect();
    }

    try {
      console.log('🔗 正在建立 SSE 連接...');
      
      // 使用 URL 參數傳遞 token（符合需求文件）
      const url = `${this.baseUrl}/api/sse/connect?token=${encodeURIComponent(token)}`;
      this.eventSource = new EventSource(url);

      // 連接建立成功
      this.eventSource.onopen = (event) => {
        console.log('✅ SSE 連接建立成功');
        this.isConnected = true;
        this.reconnectAttempts = 0;
        callbacks.onConnect?.();
      };

      // 連接錯誤
      this.eventSource.onerror = (error) => {
        console.error('❌ SSE 連接錯誤:', error);
        this.isConnected = false;
        callbacks.onError?.(error);
        this.handleReconnect(token, userType, callbacks);
      };

      // 設定事件監聽器
      this.setupEventListeners(callbacks);

    } catch (error) {
      console.error('❌ 建立 SSE 連接失敗:', error);
      callbacks.onError?.(error);
    }
  }

  setupEventListeners(callbacks) {
    // 監聽所有事件類型（符合需求文件）
    const eventTypes = [
      'connect', 'report_update', 'new_report', 'comment_update', 
      'notification', 'heartbeat', 'disconnect', 'error'
    ];

    eventTypes.forEach(eventType => {
      this.eventSource.addEventListener(eventType, (event) => {
        try {
          const data = JSON.parse(event.data);
          console.log(`📨 收到 ${eventType} 事件:`, data);
          callbacks.onMessage?.(eventType, data);
        } catch (error) {
          console.error('❌ 解析事件資料失敗:', error);
        }
      });
    });

    // 通用訊息處理
    this.eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        callbacks.onMessage?.('message', data);
      } catch (error) {
        console.error('❌ 解析訊息失敗:', error);
      }
    };
  }

  handleReconnect(token, userType, callbacks) {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = Math.min(3000 * Math.pow(2, this.reconnectAttempts - 1), 30000);
      
      console.log(`🔄 嘗試重連 (${this.reconnectAttempts}/${this.maxReconnectAttempts})，${delay}ms 後重試...`);
      
      setTimeout(() => {
        this.connect(token, userType, callbacks);
      }, delay);
    } else {
      console.error('❌ 重連次數已達上限，停止重連');
    }
  }

  disconnect() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      this.isConnected = false;
      this.reconnectAttempts = 0;
      console.log('🔌 SSE 連接已斷開');
    }
  }

  getConnectionStatus() {
    return {
      isConnected: this.isConnected,
      reconnectAttempts: this.reconnectAttempts,
      maxReconnectAttempts: this.maxReconnectAttempts
    };
  }
}

export default new SSENotificationService();
```

## 5. Vue.js 整合方式

### 5.1 Composition API (推薦)
```js
// src/composables/useSSE.js
import { ref, onMounted, onUnmounted } from 'vue'
import sseService from '@/services/sse-notification-service.js'
import Swal from 'sweetalert2'

export function useSSE() {
  const isConnected = ref(false)
  const notifications = ref([])

  const connectSSE = () => {
    const token = localStorage.getItem('token')
    const userType = localStorage.getItem('userType')?.toLowerCase()
    
    if (!token || !userType) {
      console.warn('缺少 token 或 userType，無法建立 SSE 連接')
      return
    }

    sseService.connect(token, userType, {
      onConnect: () => {
        isConnected.value = true
        console.log('✅ SSE 連接成功')
      },
      onError: (error) => {
        isConnected.value = false
        console.error('❌ SSE 連接錯誤:', error)
      },
      onMessage: (eventType, data) => {
        handleNotification(eventType, data)
      }
    })
  }

  const handleNotification = (eventType, data) => {
    // 儲存通知歷史
    const notification = {
      id: Date.now(),
      type: eventType,
      data: data,
      timestamp: new Date().toISOString(),
      read: false
    }
    
    notifications.value.unshift(notification)
    
    // 限制通知數量（最多保留 50 個）
    if (notifications.value.length > 50) {
      notifications.value = notifications.value.slice(0, 50)
    }
    
    // 儲存到 localStorage
    localStorage.setItem('notifications', JSON.stringify(notifications.value))
    
    // 顯示即時通知
    showToastNotification(eventType, data)
  }

  const showToastNotification = (eventType, data) => {
    const config = {
      position: 'top-end',
      timer: 5000,
      timerProgressBar: true,
      toast: true
    }

    switch (eventType) {
      case 'new_report':
        Swal.fire({
          ...config,
          icon: 'warning',
          title: '新檢舉通知',
          text: data.message || '有新的檢舉需要處理'
        })
        break
        
      case 'report_update':
        Swal.fire({
          ...config,
          icon: 'info',
          title: '檢舉狀態更新',
          text: data.message || '檢舉狀態已更新'
        })
        break
        
      case 'comment_update':
        Swal.fire({
          ...config,
          icon: 'success',
          title: '留言更新',
          text: data.message || '有新的留言活動'
        })
        break
        
      case 'notification':
        Swal.fire({
          ...config,
          icon: data.priority === 'URGENT' ? 'error' : 'info',
          title: data.title || '系統通知',
          text: data.message
        })
        break
        
      default:
        console.log('未處理的通知類型:', eventType, data)
    }
  }

  const disconnectSSE = () => {
    sseService.disconnect()
    isConnected.value = false
  }

  const getConnectionStatus = () => {
    return sseService.getConnectionStatus()
  }

  onMounted(() => {
    connectSSE()
  })

  onUnmounted(() => {
    disconnectSSE()
  })

  return {
    isConnected,
    notifications,
    connectSSE,
    disconnectSSE,
    getConnectionStatus
  }
}
```

### 5.2 在組件中使用
```vue
<!-- src/components/ReportManagement.vue -->
<template>
  <div class="report-management">
    <!-- 連接狀態指示器 -->
    <div class="connection-status mb-3">
      <div class="d-flex align-items-center">
        <div 
          class="status-indicator me-2" 
          :class="{ 'connected': isConnected, 'disconnected': !isConnected }"
        ></div>
        <span class="status-text">
          {{ isConnected ? '即時通知已連接' : '即時通知未連接' }}
        </span>
        <button 
          v-if="!isConnected" 
          @click="connectSSE" 
          class="btn btn-sm btn-outline-primary ms-2"
        >
          重新連接
        </button>
      </div>
    </div>

    <!-- 檢舉列表 -->
    <div class="reports-list">
      <!-- 檢舉內容 -->
    </div>

    <!-- 通知歷史 -->
    <div class="notifications-panel">
      <h5>通知歷史</h5>
      <div v-for="notification in notifications" :key="notification.id" class="notification-item">
        <div class="notification-header">
          <span class="notification-type">{{ notification.type }}</span>
          <small class="notification-time">{{ formatTime(notification.timestamp) }}</small>
        </div>
        <div class="notification-content">
          {{ notification.data.message }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useSSE } from '@/composables/useSSE.js'

const { isConnected, notifications, connectSSE } = useSSE()

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString('zh-TW')
}
</script>

<style scoped>
.connection-status {
  padding: 0.75rem;
  border-radius: 0.375rem;
  background-color: #f8f9fa;
  border: 1px solid #dee2e6;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #dc3545;
  transition: background-color 0.3s ease;
}

.status-indicator.connected {
  background-color: #198754;
}

.notification-item {
  padding: 0.75rem;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  margin-bottom: 0.5rem;
  background-color: white;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.notification-type {
  font-weight: 600;
  color: #0d6efd;
}

.notification-time {
  color: #6c757d;
}
</style>
```

## 6. 測試與常見問題

### 6.1 測試步驟
1. **登入測試**：登入後檢查瀏覽器控制台是否有 "SSE 連接成功" 訊息
2. **通知測試**：在另一個瀏覽器視窗提交檢舉，觀察是否收到即時通知
3. **重連測試**：斷開網路後重新連接，檢查是否自動重連
4. **權限測試**：使用不同用戶類型登入，檢查接收的通知類型

### 6.2 常見問題
- **連接失敗**：檢查 token 是否有效、CORS 設定、後端服務狀態
- **收不到通知**：檢查事件監聽器設定、後端推送邏輯
- **頻繁斷線**：檢查網路穩定性、重連機制設定
- **通知不顯示**：檢查 SweetAlert2 是否正確引入、瀏覽器通知權限

## 7. API 參考

### 7.1 服務方法
- `connect(token, userType, callbacks)`：建立 SSE 連接
- `disconnect()`：斷開 SSE 連接
- `getConnectionStatus()`：獲取連接狀態

### 7.2 回調函數
- `onConnect()`：連接建立成功
- `onError(error)`：連接錯誤
- `onMessage(eventType, data)`：收到通知

### 7.3 事件類型
- `connect`：連接建立
- `report_update`：檢舉狀態更新
- `new_report`：新檢舉通知
- `comment_update`：留言更新
- `notification`：一般通知
- `heartbeat`：心跳
- `disconnect`：連接斷開
- `error`：錯誤

## 8. 部署注意事項

### 8.1 生產環境配置
```js
// .env.production
VITE_API_BASE_URL=https://api.yourdomain.com
```

### 8.2 Nginx 代理設定
```nginx
location /api/sse/ {
    proxy_pass http://backend:8080;
    proxy_set_header Connection '';
    proxy_http_version 1.1;
    proxy_buffering off;
    proxy_cache off;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header Host $http_host;
    proxy_read_timeout 86400;
}
```

### 8.3 HTTPS 要求
生產環境必須使用 HTTPS，因為 SSE 在某些瀏覽器中需要安全連接。

---

**文件版本**：1.0  
**最後更新**：2025-01-15  
**適用版本**：Vue.js 3.x, Bootstrap 5.x, SweetAlert2 