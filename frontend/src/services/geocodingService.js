class GeocodingService {
  constructor() {
    this.geocoder = null;
    this.initGeocoder();
  }

  // 初始化地理編碼器
  initGeocoder() {
    if (window.google && window.google.maps) {
      this.geocoder = new google.maps.Geocoder();
    } else {
      // 等待 Google Maps API 載入
      window.addEventListener('load', () => {
        setTimeout(() => {
          if (window.google && window.google.maps) {
            this.geocoder = new google.maps.Geocoder();
          }
        }, 1000);
      });
    }
  }

  // 將地址轉換為座標
  async geocodeAddress(address) {
    return new Promise((resolve, reject) => {
      if (!this.geocoder) {
        reject(new Error('地理編碼器未初始化'));
        return;
      }

      this.geocoder.geocode({ address: address }, (results, status) => {
        if (status === 'OK' && results[0]) {
          const location = results[0].geometry.location;
          resolve({
            lat: location.lat(),
            lng: location.lng()
          });
        } else {
          reject(new Error(`地理編碼失敗: ${status}`));
        }
      });
    });
  }

  // 批量處理收藏項目的座標
  async processFavoritesCoordinates(favorites) {
    const processedFavorites = [];

    for (const favorite of favorites) {
      try {
        if (favorite.location && !favorite.lat && !favorite.lng) {
          // 嘗試獲取座標
          const coordinates = await this.geocodeAddress(favorite.location);
          processedFavorites.push({
            ...favorite,
            lat: coordinates.lat,
            lng: coordinates.lng
          });
        } else {
          // 已有座標或沒有位置信息
          processedFavorites.push(favorite);
        }
      } catch (error) {
        console.warn(`無法獲取 ${favorite.name} 的座標:`, error.message);
        // 使用預設座標（台北市中心）
        processedFavorites.push({
          ...favorite,
          lat: 25.04477709780432,
          lng: 121.52201134530966
        });
      }
    }

    return processedFavorites;
  }

  // 獲取兩個點之間的距離（公里）
  calculateDistance(lat1, lng1, lat2, lng2) {
    const R = 6371; // 地球半徑（公里）
    const dLat = this.deg2rad(lat2 - lat1);
    const dLng = this.deg2rad(lng2 - lng1);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(this.deg2rad(lat1)) *
      Math.cos(this.deg2rad(lat2)) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const distance = R * c;
    return distance;
  }

  deg2rad(deg) {
    return deg * (Math.PI / 180);
  }
}

export default new GeocodingService();