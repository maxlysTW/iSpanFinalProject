import { onMounted } from 'vue';
import favoriteService from '@/services/favoriteService';

export function useAuthSync() {
  // 監聽登入事件（現在只記錄，不做同步）
  const handleLogin = () => {
    console.log('🔵 用戶登入，收藏系統使用本地存儲');
  };

  // 監聽登出事件
  const handleLogout = () => {
    console.log('🔵 用戶登出，收藏系統繼續使用本地存儲');
  };

  // 監聽登入狀態變化
  const watchAuthState = () => {
    // 監聽 localStorage 中的 token 變化
    const originalSetItem = localStorage.setItem;
    const originalRemoveItem = localStorage.removeItem;

    localStorage.setItem = function (key, value) {
      originalSetItem.apply(this, arguments);
      if (key === 'token' && value) {
        // token 被設置，表示登入
        handleLogin();
      }
    };

    localStorage.removeItem = function (key) {
      originalRemoveItem.apply(this, arguments);
      if (key === 'token') {
        // token 被移除，表示登出
        handleLogout();
      }
    };
  };

  onMounted(() => {
    watchAuthState();
  });

  return {
    handleLogin,
    handleLogout
  };
}