<template>
  <div class="favorites-page">
    <!-- Header -->
    <Header />

    <div class="container my-5 pt-4">
      <div class="row">
        <div class="col-12">
          <div class="card">
            <div class="card-header bg-white">
              <div class="d-flex justify-content-between align-items-center">
                <h5 class="mb-0">
                  <i class="bi bi-heart-fill text-danger me-2"></i>
                  我的收藏
                </h5>
                <div class="d-flex gap-2">
                  <!-- 視圖切換按鈕 -->
                  <div class="btn-group me-3" role="group">
                    <button class="btn btn-outline-secondary btn-sm" @click="viewMode = 'list'"
                      :class="{ active: viewMode === 'list' }" title="列表視圖">
                      <i class="bi bi-list-ul"></i>
                    </button>
                    <button class="btn btn-outline-secondary btn-sm" @click="viewMode = 'map'"
                      :class="{ active: viewMode === 'map' }" title="地圖視圖">
                      <i class="bi bi-geo-alt"></i>
                    </button>
                  </div>

                  <!-- 過濾按鈕 -->
                  <button class="btn btn-outline-primary btn-sm" @click="filterByType('all')"
                    :class="{ active: currentFilter === 'all' }">
                    全部
                  </button>
                  <button class="btn btn-outline-primary btn-sm" @click="filterByType('hotel')"
                    :class="{ active: currentFilter === 'hotel' }">
                    飯店
                  </button>
                  <button class="btn btn-outline-primary btn-sm" @click="filterByType('ticket')"
                    :class="{ active: currentFilter === 'ticket' }">
                    景點
                  </button>
                  <button class="btn btn-outline-primary btn-sm" @click="filterByType('traffic')"
                    :class="{ active: currentFilter === 'traffic' }">
                    交通
                  </button>
                </div>
              </div>
            </div>
            <div class="card-body">
              <!-- 載入中 -->
              <div v-if="loading" class="text-center py-5">
                <div class="loading"></div>
                <p class="mt-3 text-muted">載入中...</p>
              </div>

              <!-- 空狀態 -->
              <div v-else-if="filteredFavorites.length === 0" class="text-center py-5">
                <i class="bi bi-heart fs-1 text-muted"></i>
                <p class="mt-3 text-muted">
                  {{
                    currentFilter === 'all'
                      ? '您還沒有收藏任何項目'
                      : `您還沒有收藏${getFilterName(currentFilter)}`
                  }}
                </p>
                <router-link to="/" class="btn btn-primary"> 開始探索 </router-link>
              </div>

              <!-- 地圖視圖 -->
              <div v-else-if="viewMode === 'map'" class="map-view">
                <div class="row">
                  <div class="col-md-8">
                    <div class="map-container">
                      <GoogleMap :favorites="processedFavorites" :center="mapCenter" :zoom="17"
                        :streetDisplay="streetDisplay" />
                    </div>
                  </div>
                  <div class="col-md-4">
                    <div class="map-sidebar">
                      <h6 class="mb-3">
                        <i class="bi bi-geo-alt text-primary"></i>
                        收藏地點 ({{ processedFavorites.length }})
                      </h6>
                      <div class="favorites-list">
                        <div v-for="item in processedFavorites" :key="`${item.type}-${item.id}`"
                          class="favorite-item mb-2 p-2 border rounded" @click="focusOnMap(item)">
                          <div class="d-flex align-items-center">
                            <div class="marker-icon me-2" :class="`marker-${item.type}`">
                              <i class="bi" :class="getMarkerIcon(item.type)"></i>
                            </div>
                            <div class="flex-grow-1">
                              <h6 class="mb-1">{{ item.name }}</h6>
                              <small class="text-muted">
                                <i class="bi bi-geo-alt"></i>
                                {{ item.location || item.route }}
                              </small>
                              <div class="mt-1">
                                <span class="badge bg-primary">{{ getTypeLabel(item.type) }}</span>
                                <span class="text-primary ms-2">NT$ {{ (item.price || 0).toLocaleString() }}</span>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 列表視圖 -->
              <div v-else class="row">
                <div v-for="item in filteredFavorites" :key="`${item.type}-${item.id}`" class="col-md-6 col-lg-4 mb-4">
                  <div class="card h-100 favorite-card">
                    <div class="position-relative">
                      <img :src="item.image" class="card-img-top" :alt="item.name" />
                      <button class="btn btn-sm btn-light position-absolute top-0 end-0 m-2"
                        @click="removeFavorite(item)" title="取消收藏">
                        <i class="bi bi-heart-fill text-danger"></i>
                      </button>
                    </div>
                    <div class="card-body">
                      <h6 class="card-title">{{ item.name }}</h6>
                      <p class="card-text">
                        <small class="text-muted">
                          <i class="bi bi-geo-alt"></i>
                          {{ item.location || item.route }}
                        </small>
                      </p>

                      <!-- 旅館專屬資訊 -->
                      <div v-if="item.type === 'hotel'" class="mb-2">
                        <small class="text-primary" v-if="item.roomTypeName">
                          <i class="bi bi-house"></i> {{ item.roomTypeName }}
                        </small>
                        <small class="text-muted ms-2" v-if="item.maxGuests">
                          <i class="bi bi-people"></i> 可住 {{ item.maxGuests }} 人
                        </small>
                      </div>

                      <!-- 景點專屬資訊 -->
                      <div v-if="item.type === 'ticket'" class="mb-2">
                        <small class="text-success" v-if="item.features">
                          <i class="bi bi-tags"></i> {{ item.features.join(', ') }}
                        </small>
                      </div>

                      <!-- 交通專屬資訊 -->
                      <div v-if="item.type === 'traffic'" class="mb-2">
                        <small class="text-info" v-if="item.provider">
                          <i class="bi bi-train-front"></i> {{ item.provider }}
                        </small>
                        <small class="text-muted ms-2" v-if="item.duration">
                          <i class="bi bi-clock"></i> {{ item.duration }}
                        </small>
                      </div>

                      <div class="d-flex justify-content-between align-items-center">
                        <div class="price">
                          <span class="text-primary fw-bold">
                            NT$ {{ (item.price || 0).toLocaleString() }}
                          </span>
                        </div>
                        <div class="d-flex gap-2">
                          <button class="btn btn-outline-primary btn-sm" @click="addToJourney(item)" title="加入旅程">
                            <i class="bi bi-plus-circle me-1"></i>加入旅程
                          </button>
                          <button class="btn btn-primary btn-sm" @click="viewDetail(item)" title="查看詳情">
                            詳情
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 分頁信息 -->
              <div v-if="filteredFavorites.length > 0" class="text-center mt-4">
                <small class="text-muted"> 共 {{ filteredFavorites.length }} 個收藏項目 </small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
  import { useRouter } from 'vue-router';
  import Header from '../components/Header.vue';
  import GoogleMap from '../components/common/GoogleMap.vue';
  import favoriteService from '@/services/favoriteService';
  import geocodingService from '@/services/geocodingService';

  const router = useRouter();
  const favorites = ref([]);
  const processedFavorites = ref([]);
  const loading = ref(false);
  const currentFilter = ref('all');
  const viewMode = ref('list'); // 'list' 或 'map'
  const streetDisplay = ref('normal'); // 'minimal', 'normal', 'detailed'

  // 地圖中心點
  const mapCenter = ref({
    lat: 25.04477709780432, // 台北市中心
    lng: 121.52201134530966
  });

  // 過濾後的收藏列表
  const filteredFavorites = computed(() => {
    if (currentFilter.value === 'all') {
      return favorites.value;
    }
    return favorites.value.filter((item) => item.type === currentFilter.value);
  });

  // 獲取過濾器名稱
  const getFilterName = (filter) => {
    const filterNames = {
      hotel: '飯店',
      ticket: '景點',
      traffic: '交通'
    };
    return filterNames[filter] || '';
  };

  // 獲取類型標籤
  const getTypeLabel = (type) => {
    const labels = {
      hotel: '飯店',
      ticket: '景點',
      traffic: '交通'
    };
    return labels[type] || '其他';
  };

  // 獲取標記圖標
  const getMarkerIcon = (type) => {
    const icons = {
      hotel: {
        url: 'https://maps.google.com/mapfiles/ms/icons/red-dot.png', // 👈 這裡是紅色標記
        scaledSize: new google.maps.Size(32, 32)
      },
      ticket: {
        url: 'https://maps.google.com/mapfiles/ms/icons/blue-dot.png', // 藍色標記
        scaledSize: new google.maps.Size(32, 32)
      },
      traffic: {
        url: 'https://maps.google.com/mapfiles/ms/icons/green-dot.png', // 綠色標記
        scaledSize: new google.maps.Size(32, 32)
      }
    };
    return icons[type] || icons.hotel; // 預設使用紅色標記
  };

  // 載入收藏列表
  const loadFavorites = async () => {
    loading.value = true;
    try {
      favorites.value = favoriteService.getFavorites();

      // 處理座標
      if (viewMode.value === 'map') {
        processedFavorites.value = await geocodingService.processFavoritesCoordinates(
          favorites.value
        );
      }
    } catch (error) {
      console.error('🔴 載入收藏失敗:', error);
      favorites.value = [];
    } finally {
      loading.value = false;
    }
  };

  // 過濾收藏
  const filterByType = (type) => {
    currentFilter.value = type;
  };

  // 移除收藏
  const removeFavorite = (item) => {
    try {
      favoriteService.toggleFavorite(item);
      loadFavorites(); // 重新載入列表
    } catch (error) {
      console.error('🔴 移除收藏失敗:', error);
    }
  };

  // 加入旅程
  const addToJourney = (item) => {
    try {
      const journey = JSON.parse(localStorage.getItem('journey') || '[]');
      const existingIndex = journey.findIndex((i) => i.id === item.id && i.type === item.type);

      if (existingIndex > -1) {
        // 已存在，增加數量
        journey[existingIndex].quantity = (journey[existingIndex].quantity || 1) + 1;
      } else {
        // 新增到旅程
        journey.push({
          ...item,
          quantity: 1
        });
      }

      localStorage.setItem('journey', JSON.stringify(journey));
      window.dispatchEvent(new Event('journeyUpdated'));

      // 顯示成功訊息
      alert('已加入旅程！');
    } catch (error) {
      console.error('🔴 加入旅程失敗:', error);
      alert('加入旅程失敗，請稍後再試');
    }
  };

  // 查看詳情
  const viewDetail = (item) => {
    const routes = {
      hotel: `/hotel/${item.id}`,
      ticket: `/ticket/${item.id}`,
      traffic: `/traffic/${item.id}`
    };

    const route = routes[item.type];
    if (route) {
      router.push(route);
    }
  };

  // 地圖聚焦功能
  const focusOnMap = (item) => {
    if (item.lat && item.lng) {
      mapCenter.value = {
        lat: item.lat,
        lng: item.lng
      };
    }
  };

  // 監聽視圖模式變化
  watch(viewMode, async (newMode) => {
    if (newMode === 'map' && favorites.value.length > 0) {
      // 切換到地圖視圖時處理座標
      processedFavorites.value = await geocodingService.processFavoritesCoordinates(
        favorites.value
      );
    }
  });

  // 監聽收藏更新事件
  const handleFavoritesUpdate = () => {
    loadFavorites();
  };

  onMounted(() => {
    loadFavorites();
    window.addEventListener('favoritesUpdated', handleFavoritesUpdate);
  });

  onUnmounted(() => {
    window.removeEventListener('favoritesUpdated', handleFavoritesUpdate);
  });
</script>

<style scoped>
  .favorites-page {
    min-height: 100vh;
    background-color: #f8f9fa;
  }

  .favorite-card {
    transition: transform 0.2s ease, box-shadow 0.2s ease;
    border: none;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  .favorite-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  }

  .favorite-card img {
    height: 200px;
    object-fit: cover;
  }

  .btn.active {
    background-color: #0d6efd;
    color: white;
  }

  .loading {
    display: inline-block;
    width: 40px;
    height: 40px;
    border: 4px solid #f3f3f3;
    border-top: 4px solid #0d6efd;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    0% {
      transform: rotate(0deg);
    }

    100% {
      transform: rotate(360deg);
    }
  }

  .card-header {
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  }

  .price {
    font-size: 1.1rem;
  }

  /* 地圖視圖樣式 */
  .map-view {
    min-height: 600px;
  }

  .map-container {
    height: 600px;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  }

  .map-sidebar {
    height: 600px;
    overflow-y: auto;
    padding: 1rem;
    background: white;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  }

  .favorites-list {
    max-height: 500px;
    overflow-y: auto;
  }

  .favorite-item {
    cursor: pointer;
    transition: all 0.2s ease;
    background: white;
  }

  .favorite-item:hover {
    background: #f8f9fa;
    transform: translateX(2px);
  }

  .marker-icon {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 14px;
  }

  .marker-hotel {
    background: #dc3545;
  }

  .marker-ticket {
    background: #0d6efd;
  }

  .marker-traffic {
    background: #198754;
  }

  /* 響應式設計 */
  @media (max-width: 768px) {
    .map-container {
      height: 400px;
      margin-bottom: 1rem;
    }

    .map-sidebar {
      height: auto;
      max-height: 300px;
    }
  }
</style>