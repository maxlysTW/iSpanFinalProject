<template>
  <div class="card h-100 room-card position-relative">
    <router-link :to="getHotelDetailPath()" class="text-decoration-none">
      <!-- 圖片顯示優先順序：coverImageUrl（API/本地）、imageUrl（本地）、image（API/本地）、images[0]（本地陣列）。無圖則不顯示 img 標籤 -->
      <img v-if="
        hotel.coverImageUrl || hotel.imageUrl || hotel.image || (hotel.images && hotel.images[0])
      " :src="hotel.coverImageUrl || hotel.imageUrl || hotel.image || (hotel.images && hotel.images[0])
        " class="card-img-top" :alt="hotel.name" />
      <div class="card-body">
        <h5 class="card-title d-flex justify-content-between align-items-center">
          <span>{{ hotel.name }}</span>
          <!-- 收藏按鈕 - 與名稱同一排 -->
          <button class="btn btn-link p-0 border-0 shadow-none" @click.stop.prevent="toggleFavorite"
            :class="{ 'text-danger': localIsFavorited, 'text-muted': !localIsFavorited }" type="button"
            style="min-width: auto; line-height: 1;">
            <i class="bi" :class="localIsFavorited ? 'bi-heart-fill' : 'bi-heart'" style="font-size: 1.2rem;"></i>
          </button>
        </h5>
        <p class="card-text text-muted">{{ hotel.location }}</p>

        <!-- 房型資訊 -->
        <div v-if="hotel.roomTypeName" class="mb-2">
          <small class="text-primary fw-bold">{{ hotel.roomTypeName }}</small>
          <div v-if="hotel.maxGuests" class="text-muted">
            <small>可住 {{ hotel.maxGuests }} 人</small>
          </div>
        </div>

        <div class="d-flex justify-content-between align-items-center">
          <div class="rating">
            <span class="text-warning">★</span>
            <span>{{ hotel.rating }}</span>
            <small class="text-muted ms-1">({{ typeof hotel.reviewCount === 'number' ? hotel.reviewCount : 0
            }}則評價)</small>
          </div>
          <div class="price">
            <span v-if="hotel.pricePerNight || hotel.price" class="text-primary fw-bold">
              NT$ {{ hotel.pricePerNight || hotel.price }}
            </span>
            <span v-else class="text-muted">價格請洽詢</span>
            <small v-if="hotel.pricePerNight || hotel.price" class="text-muted">/ 晚</small>
          </div>
        </div>

        <!-- 設施標籤 -->
        <div v-if="hotel.facilities && hotel.facilities.length > 0" class="mt-2">
          <div class="d-flex flex-wrap gap-1">
            <span v-for="facility in hotel.facilities.slice(0, 3)" :key="facility.id" class="badge bg-light text-dark"
              style="font-size: 0.7rem">
              {{ facility.name }}
            </span>
            <span v-if="hotel.facilities.length > 3" class="badge bg-secondary" style="font-size: 0.7rem">
              +{{ hotel.facilities.length - 3 }}
            </span>
          </div>
        </div>
      </div>
    </router-link>
    <div class="card-footer bg-white border-top-0">
      <!-- <button class="btn btn-primary w-100 mb-2" @click="addToJourney">
        加入我的旅程
      </button> -->
      <button class="btn btn-primary w-100" @click="handleBook">
        查看房型情況
      </button>
    </div>
  </div>
</template>

<script setup>
  import { ref, onMounted, onUnmounted } from 'vue'
  import { useRouter } from 'vue-router'

  /* ---------- props / emits ---------- */
  const props = defineProps({
    hotel: {
      type: Object,
      required: true
    }
  });

  const router = useRouter();

  /* ---------- 本地收藏狀態 ---------- */
  const localIsFavorited = ref(false);

  /* ---------- 生命週期：進畫面先檢查收藏 ---------- */
  onMounted(() => {
    checkFavoriteStatus()
    window.addEventListener('favoritesUpdated', checkFavoriteStatus)
  })

  onUnmounted(() => {
    window.removeEventListener('favoritesUpdated', checkFavoriteStatus)
  })


  function handleBook() {
    const path = getHotelDetailPath();
    router.push(path);
  }

  function getHotelDetailPath() {
    // 如果有 roomTypeId，使用房型詳情路徑
    if (props.hotel.roomTypeId !== undefined && props.hotel.roomTypeId !== null) {
      return `/hotel/${props.hotel.id}/room/${props.hotel.roomTypeId}`
    }

    // 如果沒有 roomTypeId（熱門旅宿），使用基本住宿詳情路徑
    // 注意：這需要後端支援 /api/lodgings/{id} 端點
    console.warn('熱門旅宿缺少房型 ID，嘗試使用基本住宿詳情');
    return `/hotel/${props.hotel.id}`
  }

  function addToJourney() {
    try {
      const journey = JSON.parse(localStorage.getItem('journey') || '[]')
      const existingIndex = journey.findIndex(
        (item) => item.id === props.hotel.id && item.type === 'hotel'
      )

      if (existingIndex > -1) {
        // 已存在 → 數量 +1
        journey[existingIndex].quantity = (journey[existingIndex].quantity || 1) + 1
      } else {
        // 不存在 → 加入
        journey.push({
          id: props.hotel.id,
          type: 'hotel',
          name: props.hotel.name,
          image: props.hotel.coverImageUrl || props.hotel.image,
          price: props.hotel.pricePerNight || props.hotel.price,
          location: props.hotel.location,
          rating: props.hotel.rating,
          roomTypeName: props.hotel.roomTypeName,
          maxGuests: props.hotel.maxGuests,
          facilities: props.hotel.facilities,
          quantity: 1,
        })
      }

      // 更新 localStorage 並通知其他元件
      localStorage.setItem('journey', JSON.stringify(journey))
      window.dispatchEvent(new Event('journeyUpdated'))

      alert('已加入我的旅程！')
    } catch (error) {
      console.error('🔴 加入旅程失敗', error)
      alert('加入旅程失敗，請稍後再試')
    }
  }

  function checkFavoriteStatus() {
    try {
      const favorites = JSON.parse(localStorage.getItem('favorites') || '[]')
      localIsFavorited.value = favorites.some(
        (item) => item.id === props.hotel.id && item.type === 'hotel'
      )
    } catch (error) {
      console.error('🔴 無法讀取收藏清單', error)
      localIsFavorited.value = false
    }
  }

  function toggleFavorite() {
    try {
      const favorites = JSON.parse(localStorage.getItem('favorites') || '[]')
      const existingIndex = favorites.findIndex(
        (fav) => fav.id === props.hotel.id && fav.type === 'hotel'
      )

      if (existingIndex > -1) {
        // 已存在 → 取消收藏
        favorites.splice(existingIndex, 1)
        localIsFavorited.value = false
      } else {
        // 不存在 → 加入收藏
        favorites.push({
          id: props.hotel.id,
          type: 'hotel',
          name: props.hotel.name,
          image: props.hotel.coverImageUrl || props.hotel.image,
          price: props.hotel.pricePerNight || props.hotel.price,
          location: props.hotel.location,
          rating: props.hotel.rating,
          roomTypeName: props.hotel.roomTypeName,
          maxGuests: props.hotel.maxGuests,
          facilities: props.hotel.facilities,
          createdAt: new Date().toISOString()
        })
        localIsFavorited.value = true
      }

      // 更新 localStorage
      localStorage.setItem('favorites', JSON.stringify(favorites))

      // 通知其他元件收藏狀態變更
      window.dispatchEvent(new Event('favoritesUpdated'))
    } catch (error) {
      console.error('🔴 收藏操作錯誤', error)
    }
  }
</script>

<style scoped>
  .card-img-top {
    height: 200px;
    object-fit: cover;
  }

  .rating {
    font-size: 1.1rem;
  }

  .cursor-pointer {
    cursor: pointer;
  }

  .card-body {
    color: inherit;
  }

  .room-card:hover {
    box-shadow: var(--shadow-lg);
    transform: translateY(-2px);
  }
</style>