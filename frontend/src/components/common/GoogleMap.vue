<template>
  <div class="google-map-container">
    <div id="map" class="map-canvas"></div>
  </div>
</template>

<script setup>
  import { ref, onMounted, watch } from 'vue';

  const props = defineProps({
    favorites: {
      type: Array,
      default: () => []
    },
    center: {
      type: Object,
      default: () => ({
        lat: 25.04477709780432, // 台北市中心
        lng: 121.52201134530966
      })
    },
    zoom: {
      type: Number,
      default: 13
    }
  });

  const map = ref(null);
  const markers = ref([]);

  // 詳細街道樣式
  const getMapStyles = () => [
    {
      featureType: 'poi',
      elementType: 'labels',
      stylers: [{ visibility: 'off' }]
    },
    {
      featureType: 'road',
      elementType: 'geometry',
      stylers: [{ visibility: 'on' }]
    },
    {
      featureType: 'road',
      elementType: 'labels',
      stylers: [{ visibility: 'on' }]
    },
    {
      featureType: 'transit',
      elementType: 'all',
      stylers: [{ visibility: 'on' }]
    }
  ];

  // 初始化地圖
  const initMap = () => {
    if (!window.google) {
      console.error('Google Maps API 未載入');
      return;
    }
    console.log({ ...props.center });
    // 建立地圖
    map.value = new google.maps.Map(document.getElementById('map'), {
      center: props.center,
      zoom: props.zoom,
      /*
        roadmap 顯示默認道路地圖視圖。
        satellite 顯示 Google 地球衛星圖像。
        hybrid 顯示正常和衛星視圖的混合。
        terrain 顯示基於地形信息的物理地圖。
      */
      mapTypeId: 'terrain', // 地形地圖
      styles: getMapStyles(),
      disableDefaultUI: false,
      zoomControl: true,
      mapTypeControl: true,
      scaleControl: true,
      streetViewControl: true,
      rotateControl: true,
      fullscreenControl: false // 關閉全螢幕按鈕
    });

    // const marker = new google.maps.marker.AdvancedMarkerElement({
    //   position: { ...props.center },
    //   map: map.value,
    //   title: 'test'
    // });
    // 添加收藏項目的標記
    addMarkers();
  };

  // 添加標記
  const addMarkers = () => {
    if (!map.value) return;

    // 清除現有標記
    markers.value.forEach((marker) => marker.setMap(null));
    markers.value = [];

    // 為每個收藏項目添加標記
    props.favorites.forEach((favorite, index) => {
      if (favorite.location && favorite.lat && favorite.lng) {
        const marker = new google.maps.Marker({
          position: { lat: favorite.lat, lng: favorite.lng },
          map: map.value,
          title: favorite.name,
          icon: getMarkerIcon(favorite.type),
          animation: google.maps.Animation.DROP
        });

        // 添加信息窗口
        const infoWindow = new google.maps.InfoWindow({
          content: `
          <div style="padding: 10px; max-width: 200px;">
            <h6 style="margin: 0 0 5px 0; color: #333;">${favorite.name}</h6>
            <p style="margin: 0; font-size: 12px; color: #666;">
              <i class="bi bi-geo-alt"></i> ${favorite.location}
            </p>
            <p style="margin: 5px 0 0 0; font-size: 12px; color: #28a745;">
              NT$ ${favorite.price?.toLocaleString() || 'N/A'}
            </p>
            <div style="margin-top: 8px;">
              <span style="background: #007bff; color: white; padding: 2px 6px; border-radius: 3px; font-size: 10px;">
                ${getTypeLabel(favorite.type)}
              </span>
            </div>
          </div>
        `
        });

        // 點擊標記顯示信息窗口
        marker.addListener('click', () => {
          infoWindow.open(map.value, marker);
        });

        markers.value.push(marker);
      }
    });

    // 如果有標記，調整地圖視圖
    if (markers.value.length > 0) {
      // 使用設定的 zoom 值，不自動調整
      // const bounds = new google.maps.LatLngBounds();
      // markers.value.forEach((marker) => {
      //   bounds.extend(marker.getPosition());
      // });
      // map.value.fitBounds(bounds);
    }
  };

  // 根據類型獲取標記圖標
  const getMarkerIcon = (type) => {
    const icons = {
      hotel: {
        url: 'https://maps.google.com/mapfiles/ms/icons/red-dot.png',
        scaledSize: new google.maps.Size(32, 32)
      },
      ticket: {
        url: 'https://maps.google.com/mapfiles/ms/icons/blue-dot.png',
        scaledSize: new google.maps.Size(32, 32)
      },
      traffic: {
        url: 'https://maps.google.com/mapfiles/ms/icons/green-dot.png',
        scaledSize: new google.maps.Size(32, 32)
      }
    };
    return icons[type] || icons.hotel;
  };

  // 獲取類型標籤
  const getTypeLabel = (type) => {
    const labels = {
      hotel: '旅館',
      ticket: '景點',
      traffic: '交通'
    };
    return labels[type] || '其他';
  };

  // 監聽收藏列表變化
  watch(
    () => props.favorites,
    () => {
      if (map.value) {
        addMarkers();
      }
    },
    { deep: true }
  );

  onMounted(() => {
    // 等待 Google Maps API 載入
    if (window.google) {
      initMap();
    } else {
      // 如果 API 還沒載入，等待載入完成
      window.addEventListener('load', () => {
        setTimeout(initMap, 1000); // 給 API 一點時間初始化
      });
    }
  });
</script>

<style scoped>
  .google-map-container {
    width: 100%;
    height: 100%;
    border-radius: 8px;
    overflow: hidden;
  }

  .map-canvas {
    width: 100%;
    height: 100%;
    min-height: 400px;
  }

  /* 響應式設計 */
  @media (max-width: 768px) {
    .map-canvas {
      min-height: 300px;
    }
  }
</style>