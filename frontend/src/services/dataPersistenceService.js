/**
 * 數據持久化服務
 * 用於管理商家上傳的數據，提供更長的存儲時間
 */
class DataPersistenceService {
  constructor() {
    this.vendorDataKeys = ['roomTypes', 'hotelsData', 'flightsData', 'ticketsData', 'trafficData'];

    this.userDataKeys = [
      'userRole',
      'userType',
      'userName',
      'userId',
      'userAvatar',
      'token',
      'vendorType',
      'vendorId',
      'isVendorLoggedIn',
      'vendorName',
      'userInfo',
      'favorites',
      'journey',
      'checkoutData',
      'checkoutFlight',
      'roundTripCheckout',
      'multiTripCheckout',
      'flightSearchForm',
      'username',
      'bookingOrder',
      'recentViews',
      'searchHistory'
    ];
  }

  /**
   * 保存商家數據到持久化存儲
   */
  saveVendorData() {
    const vendorData = {};

    this.vendorDataKeys.forEach((key) => {
      const data = localStorage.getItem(key);
      if (data) {
        vendorData[key] = data;
      }
    });

    // 保存到 sessionStorage 作為備份
    sessionStorage.setItem('vendorDataBackup', JSON.stringify(vendorData));

    // 同時保存到 localStorage 的持久化鍵
    localStorage.setItem('persistentVendorData', JSON.stringify(vendorData));

    console.log('💾 商家數據已保存到持久化存儲');
  }

  /**
   * 恢復商家數據
   */
  restoreVendorData() {
    // 優先從 sessionStorage 恢復
    const sessionBackup = sessionStorage.getItem('vendorDataBackup');
    if (sessionBackup) {
      try {
        const vendorData = JSON.parse(sessionBackup);
        Object.entries(vendorData).forEach(([key, value]) => {
          localStorage.setItem(key, value);
        });
        console.log('🔄 從 sessionStorage 恢復商家數據');
        return true;
      } catch (error) {
        console.error('🔴 從 sessionStorage 恢復數據失敗:', error);
      }
    }

    // 從 localStorage 持久化鍵恢復
    const persistentData = localStorage.getItem('persistentVendorData');
    if (persistentData) {
      try {
        const vendorData = JSON.parse(persistentData);
        Object.entries(vendorData).forEach(([key, value]) => {
          localStorage.setItem(key, value);
        });
        console.log('🔄 從持久化存儲恢復商家數據');
        return true;
      } catch (error) {
        console.error('🔴 從持久化存儲恢復數據失敗:', error);
      }
    }

    return false;
  }

  /**
   * 清除用戶數據但保留商家數據
   */
  clearUserDataOnly() {
    // 先保存商家數據
    this.saveVendorData();

    // 清除用戶相關數據
    this.userDataKeys.forEach((key) => {
      localStorage.removeItem(key);
    });

    console.log('🧹 已清除用戶數據，保留商家數據');
  }

  /**
   * 完全清除所有數據（包括商家數據）
   */
  clearAllData() {
    // 清除所有數據
    this.userDataKeys.forEach((key) => {
      localStorage.removeItem(key);
    });

    this.vendorDataKeys.forEach((key) => {
      localStorage.removeItem(key);
    });

    // 清除備份數據
    sessionStorage.removeItem('vendorDataBackup');
    localStorage.removeItem('persistentVendorData');

    console.log('🧹 已清除所有數據');
  }

  /**
   * 檢查是否有商家數據
   */
  hasVendorData() {
    return this.vendorDataKeys.some((key) => localStorage.getItem(key));
  }

  /**
   * 獲取商家數據統計
   */
  getVendorDataStats() {
    const stats = {};

    this.vendorDataKeys.forEach((key) => {
      const data = localStorage.getItem(key);
      if (data) {
        try {
          const parsed = JSON.parse(data);
          stats[key] = Array.isArray(parsed) ? parsed.length : 'N/A';
        } catch (error) {
          stats[key] = 'Invalid';
        }
      } else {
        stats[key] = 0;
      }
    });

    return stats;
  }

  /**
   * 導出商家數據
   */
  exportVendorData() {
    const vendorData = {};

    this.vendorDataKeys.forEach((key) => {
      const data = localStorage.getItem(key);
      if (data) {
        vendorData[key] = data;
      }
    });

    const dataStr = JSON.stringify(vendorData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `vendor_data_${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
  }

  /**
   * 導入商家數據
   */
  importVendorData(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const vendorData = JSON.parse(e.target.result);

          // 驗證數據格式
          const isValid = this.vendorDataKeys.some((key) => vendorData.hasOwnProperty(key));
          if (!isValid) {
            reject(new Error('無效的商家數據格式'));
            return;
          }

          // 導入數據
          Object.entries(vendorData).forEach(([key, value]) => {
            localStorage.setItem(key, value);
          });

          // 保存到持久化存儲
          this.saveVendorData();

          resolve(true);
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
const dataPersistenceService = new DataPersistenceService();

export default dataPersistenceService;
