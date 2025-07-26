class FavoriteService {
  constructor() {
    this.localStorageKey = 'favorites';
  }

  /**
   * 獲取本地收藏列表
   */
  getFavorites() {
    try {
      const favorites = localStorage.getItem(this.localStorageKey);
      return favorites ? JSON.parse(favorites) : [];
    } catch (error) {
      console.error('🔴 讀取本地收藏失敗:', error);
      return [];
    }
  }

  /**
   * 保存收藏到本地存儲
   */
  saveFavorites(favorites) {
    try {
      localStorage.setItem(this.localStorageKey, JSON.stringify(favorites));
      // 觸發收藏更新事件
      window.dispatchEvent(new Event('favoritesUpdated'));
    } catch (error) {
      console.error('🔴 保存本地收藏失敗:', error);
    }
  }

  /**
   * 添加收藏
   */
  addFavorite(item) {
    const favorites = this.getFavorites();
    const existingIndex = favorites.findIndex(
      (fav) => fav.id === item.id && fav.type === item.type
    );

    if (existingIndex === -1) {
      favorites.push({
        ...item,
        createdAt: new Date().toISOString()
      });
      this.saveFavorites(favorites);
      return true;
    }
    return false;
  }

  /**
   * 移除收藏
   */
  removeFavorite(item) {
    const favorites = this.getFavorites();
    const existingIndex = favorites.findIndex(
      (fav) => fav.id === item.id && fav.type === item.type
    );

    if (existingIndex > -1) {
      favorites.splice(existingIndex, 1);
      this.saveFavorites(favorites);
      return true;
    }
    return false;
  }

  /**
   * 切換收藏狀態
   */
  toggleFavorite(item) {
    const favorites = this.getFavorites();
    const existingIndex = favorites.findIndex(
      (fav) => fav.id === item.id && fav.type === item.type
    );

    if (existingIndex > -1) {
      // 已收藏，取消收藏
      favorites.splice(existingIndex, 1);
      this.saveFavorites(favorites);
      return false;
    } else {
      // 未收藏，添加收藏
      favorites.push({
        ...item,
        createdAt: new Date().toISOString()
      });
      this.saveFavorites(favorites);
      return true;
    }
  }

  /**
   * 檢查是否已收藏
   */
  isFavorited(item) {
    const favorites = this.getFavorites();
    return favorites.some((fav) => fav.id === item.id && fav.type === item.type);
  }

  /**
   * 獲取收藏數量
   */
  getFavoriteCount() {
    return this.getFavorites().length;
  }

  /**
   * 按類型獲取收藏
   */
  getFavoritesByType(type) {
    const favorites = this.getFavorites();
    return favorites.filter((fav) => fav.type === type);
  }

  /**
   * 清空所有收藏
   */
  clearAllFavorites() {
    this.saveFavorites([]);
  }

  /**
   * 導出收藏數據（用於備份）
   */
  exportFavorites() {
    const favorites = this.getFavorites();
    const dataStr = JSON.stringify(favorites, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `favorites_${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  /**
   * 導入收藏數據（從備份恢復）
   */
  importFavorites(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const favorites = JSON.parse(e.target.result);
          if (Array.isArray(favorites)) {
            this.saveFavorites(favorites);
            resolve(true);
          } else {
            reject(new Error('無效的收藏數據格式'));
          }
        } catch (error) {
          reject(error);
        }
      };
      reader.onerror = () => reject(new Error('讀取文件失敗'));
      reader.readAsText(file);
    });
  }
}

// 創建單例實例
const favoriteService = new FavoriteService();

export default favoriteService;