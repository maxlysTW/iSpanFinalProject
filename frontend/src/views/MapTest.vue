<template>
  <div class="map-test-page">
    <Header />

    <div class="container my-5 pt-4">
      <div class="row">
        <div class="col-12">
          <div class="card">
            <div class="card-header">
              <h5>Google Map 測試</h5>
            </div>
            <div class="card-body">
              <div class="row">
                <div class="col-md-8">
                  <div class="map-container">
                    <GoogleMap :favorites="testFavorites" :center="mapCenter" :zoom="12" />
                  </div>
                </div>
                <div class="col-md-4">
                  <div class="map-sidebar">
                    <h6 class="mb-3">測試地點</h6>
                    <div class="favorites-list">
                      <div
                        v-for="item in testFavorites"
                        :key="`${item.type}-${item.id}`"
                        class="favorite-item mb-2 p-2 border rounded"
                      >
                        <div class="d-flex align-items-center">
                          <div class="marker-icon me-2" :class="`marker-${item.type}`">
                            <i class="bi" :class="getMarkerIcon(item.type)"></i>
                          </div>
                          <div class="flex-grow-1">
                            <h6 class="mb-1">{{ item.name }}</h6>
                            <small class="text-muted">
                              <i class="bi bi-geo-alt"></i>
                              {{ item.location }}
                            </small>
                            <div class="mt-1">
                              <span class="badge bg-primary">{{ getTypeLabel(item.type) }}</span>
                              <span class="text-primary ms-2"
                                >NT$ {{ item.price?.toLocaleString() }}</span
                              >
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref } from 'vue';
  import Header from '../components/Header.vue';
  import GoogleMap from '../components/common/GoogleMap.vue';

  const mapCenter = ref({
    lat: 25.04477709780432, // 台北市中心
    lng: 121.52201134530966
  });

  // 測試數據
  const testFavorites = ref([
    {
      id: 1,
      type: 'hotel',
      name: '台北喜來登大飯店',
      location: '台北市忠孝東路一段12號',
      price: 8000,
      lat: 25.04477709780432,
      lng: 121.52201134530966
    },
    {
      id: 2,
      type: 'ticket',
      name: '台北101觀景台',
      location: '台北市信義區信義路五段7號',
      price: 1200,
      lat: 25.033964,
      lng: 121.564472
    },
    {
      id: 3,
      type: 'traffic',
      name: '台北捷運',
      location: '台北市各區',
      price: 30,
      lat: 25.033964,
      lng: 121.564472
    }
  ]);

  const getTypeLabel = (type) => {
    const labels = {
      hotel: '旅館',
      ticket: '景點',
      traffic: '交通'
    };
    return labels[type] || '其他';
  };

  const getMarkerIcon = (type) => {
    const icons = {
      hotel: 'bi-house-fill',
      ticket: 'bi-ticket-perforated-fill',
      traffic: 'bi-train-front-fill'
    };
    return icons[type] || 'bi-geo-alt-fill';
  };
</script>

<style scoped>
  .map-test-page {
    min-height: 100vh;
    background-color: #f8f9fa;
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
</style>
