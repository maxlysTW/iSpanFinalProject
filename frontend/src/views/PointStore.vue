<template>
  <div class="gift-mall-page">
    <Header />

    <div v-if="isPageLoading" class="loading-overlay">
      <img src="/image/full_blue.png" alt="Loading Logo" class="blinking-logo" />
      <p class="loading-text">載入中...</p>
    </div>

    <main v-else class="main-content">
      <div class="page-container">
        <div class="gift-mall-header">
          <h1>點數商城</h1>
          <p>在這裡，您可以將辛苦累積的點數兌換成各種旅遊優惠券或精選好物！</p>
          <div class="current-points">
            您的當前點數：<strong>{{ userPoints }}</strong> 點
          </div>
        </div>

        <div class="gift-categories">
          <h2>兌換類別</h2>
          <div class="categories-wrapper">
            <button v-for="category in categories" :key="category.value"
              :class="{ active: currentCategory === category.value }" @click="filterItems(category.value)">
              {{ category.label }}
            </button>
          </div>
        </div>

        <div v-if="currentCategory !== 'gacha'" class="gift-items-grid">
          <div class="gift-item-card" v-for="item in paginatedItems" :key="item.id">
            <img :src="item.image" :alt="item.name" class="item-image" />
            <div class="item-details">
              <h3>{{ item.name }}</h3>
              <p class="item-description">{{ item.description }}</p>
              <div class="item-price">
                <span class="item-stock">尚有庫存</span> <span class="points-cost">{{
                  item.pointsCost
                }}
                  點</span>
              </div>
              <button class="exchange-button" @click="confirmExchange(item)" :disabled="userPoints < item.pointsCost">
                {{ userPoints < item.pointsCost ? '點數不足' : '立即兌換' }} </button>
            </div>
          </div>
          <div v-if="paginatedItems.length === 0" class="no-items-message">
            目前沒有符合條件的兌換品項。
          </div>
        </div>

        <div v-else class="gacha-page-content">
          <h2>試試手氣！</h2>
          <p class="gacha-description">每次抽獎花費 <strong>{{ gachaCost }}</strong> 點，有機會獲得商城任一品項！</p>

          <div class="gacha-machine-section">
            <div class="gacha-machine-container">
              <!-- 背景光暈效果 -->
              <div class="gacha-glow" :class="{ 'active': isSpinning }"></div>

              <!-- 移除粒子效果容器 -->

              <!-- 抽獎機主體 -->
              <div class="gacha-machine" :class="{ 'winning': winningItem }">
                <div class="machine-inner">
                  <!-- 機器頂部裝飾 -->
                  <div class="machine-top"></div>

                  <!-- 顯示窗口 -->
                  <div class="machine-window">
                    <div class="window-shine"></div>
                    <div class="slot-items" v-if="isSpinning">
                      <div class="slot-item" v-for="item in slotItems" :key="item.id">
                        <img :src="item.image" :alt="item.name">
                      </div>
                    </div>
                    <div class="machine-logo" v-else>🎰</div>
                  </div>

                  <!-- 按鈕區域 -->
                  <div class="machine-button-area">
                    <div class="machine-button" :class="{ 'pressed': isSpinning }"></div>
                  </div>

                  <!-- 機器底部裝飾 -->
                  <div class="machine-bottom"></div>
                </div>

                <!-- 成功動畫 -->
                <div class="success-burst" v-if="winningItem && !isSpinning">
                  <div class="burst-ray" v-for="n in 8" :key="n" :style="{ '--ray-angle': (n * 45) + 'deg' }"></div>
                </div>
              </div>

              <!-- 提示文字 -->
              <!-- <div class="gacha-pull-hint" v-if="!isSpinning && !winningItem && userPoints >= gachaCost">
                                點擊下方按鈕開始抽獎 -->
              <!-- </div> -->
              <!-- <div class="gacha-pull-hint disabled"
                                v-if="!isSpinning && !winningItem && userPoints < gachaCost">
                                點數不足
                            </div> -->
            </div>

            <!-- 抽獎按鈕 -->
            <button class="spin-button" @click="startGacha" :disabled="userPoints < gachaCost || isSpinning">
              <span class="button-text">
                {{ isSpinning ? '抽獎中...' : (userPoints < gachaCost ? '點數不足' : '立即抽獎') }} </span>
                  <div class="button-shine" v-if="!isSpinning && userPoints >= gachaCost"></div>
            </button>
          </div>

          <!-- 中獎結果顯示 -->
          <div class="gacha-result" v-if="winningItem">
            <div class="result-animation">
              <h3>🎉 恭喜您獲得 🎉</h3>
              <div class="winning-item-display">
                <div class="item-image-container">
                  <img :src="winningItem.image" :alt="winningItem.name" class="winning-item-image">
                  <div class="image-glow"></div>
                </div>
                <div class="winning-item-details">
                  <h4>{{ winningItem.name }}</h4>
                  <p>{{ winningItem.description }}</p>
                </div>
              </div>
              <!-- 慶祝動畫 -->
              <div class="celebration-confetti">
                <div class="confetti" v-for="n in 30" :key="n" :style="{
                  '--delay': (Math.random() * 3) + 's',
                  '--x': (Math.random() * 100) + '%',
                  '--rotation': (Math.random() * 360) + 'deg',
                  '--color': ['#ff6b6b', '#4ecdc4', '#45b7d1', '#f9ca24', '#f0932b', '#eb4d4b'][Math.floor(Math.random() * 6)]
                }"></div>
              </div>
            </div>
          </div>

          <!-- 點數不足提示 -->
          <div v-if="!winningItem && isSpinning === false && userPoints < gachaCost && currentCategory === 'gacha'"
            class="gacha-not-enough-points">
            <i class="warning-icon">⚠️</i>
            您的點數不足，無法進行抽獎。
          </div>
        </div>

        <div class="pagination" v-if="totalPages > 1 && currentCategory !== 'gacha'">
          <button class="pagination-button" @click="prevPage" :disabled="currentPage === 1">
            上一頁
          </button>
          <button class="pagination-button" v-for="page in totalPages" :key="page"
            :class="{ active: currentPage === page }" @click="goToPage(page)">
            {{ page }}
          </button>
          <button class="pagination-button" @click="nextPage" :disabled="currentPage === totalPages">
            下一頁
          </button>
        </div>

      </div>
    </main>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import Swal from 'sweetalert2';
import Header from '../components/Header.vue';
import axios from 'axios';

const isPageLoading = ref(true); // 整體頁面載入狀態，初始為 true
const MIN_LOADING_TIME = 500; // 例如，設定為 0.5 秒

// 初始點數(第一次註冊時的點數)，這邊會等候端送資料進來以後覆寫掉
const userPoints = ref(0);


// 獲取使用者點數的函數
const fetchUserPoints = async () => {
  try {
    // 發送 GET 請求到後端獲取用戶總點數
    const response = await axios.get('/api/points/total');
    // 將後端返回的點數賦值給 userPoints.value
    userPoints.value = response.data.totalPoints; // 已根據後端回傳格式調整
    // console.log('使用者點數已從後端同步:', userPoints.value);
  } catch (error) {
    // console.error('獲取使用者點數失敗:', error);
    Swal.fire({
      icon: 'error',
      title: '載入點數失敗',
      text: '無法從伺服器獲取您的點數，請稍後再試。',
    });
  }
};

// 分頁相關數據
const itemsPerPage = 9; // 每頁顯示9筆
const currentPage = ref(1); // 當前頁碼

// 抽獎相關狀態
const gachaCost = ref(2000); // 每次抽獎花費點數
const isSpinning = ref(false); // 抽獎動畫狀態
const winningItem = ref(null); // 中獎品項

// 商城的兌換品項
const giftItems = ref([
  {
    id: 'coupon-hotel-100',
    name: '飯店住宿 $100 折價券',
    description: '預訂飯店住宿可折抵 $100，無最低消費限制。',
    image: '/image/coupon_gift.png',
    pointsCost: 1000,
    category: 'coupon',
  },
  {
    id: 'coupon-flight-200',
    name: '機票 $200 折價券',
    description: '預訂機票可折抵 $200，適用於所有航空公司。',
    image: '/image/coupon_gift.png',
    pointsCost: 2000,
    category: 'coupon',
  },
  {
    id: 'coupon-attraction-50',
    name: '景點門票 $50 折價券',
    description: '購買景點門票可折抵 $50，部分熱門景點不適用。',
    image: '/image/coupon_gift.png',
    pointsCost: 500,
    category: 'coupon',
  },
  {
    id: 'item-travel-kit',
    name: '豪華旅行組',
    description: '包含旅行枕、眼罩、耳塞等，讓您的旅途更舒適。',
    image: '/image/gifts/gift_item1.png',
    pointsCost: 3500,
    category: 'item',
  },
  {
    id: 'coupon-all-purpose-300',
    name: '全站通用 $300 折價券',
    description: '全站商品適用，消費滿 $3000 可使用。',
    image: '/image/coupon_gift.png',
    pointsCost: 3000,
    category: 'coupon',
  },
  {
    id: 'item-luggage-tag',
    name: '設計款行李吊牌',
    description: '獨家設計款行李吊牌，讓您的行李與眾不同。',
    image: '/image/gifts/gift_item2.png',
    pointsCost: 800,
    category: 'item',
  },
  {
    id: 'coupon-transport-100',
    name: '交通票券 $100 折價券',
    description: '適用於高鐵、火車、客運等交通票券預訂。',
    image: '/image/coupon_gift.png',
    pointsCost: 1200,
    category: 'coupon',
  },
  {
    id: 'item-power-bank',
    name: '迷你行動電源',
    description: '輕巧便攜，解決您旅途中的充電煩惱。',
    image: '/image/gifts/gift_item3.png',
    pointsCost: 4000,
    category: 'item',
  },
  {
    id: 'item-umbrella',
    name: '輕巧摺疊傘',
    description: '防潑水抗UV，旅途中的必備小物。',
    image: '/image/gifts/gift_item4.png',
    pointsCost: 1500,
    category: 'item',
  },
  {
    id: 'coupon-spa-500',
    name: 'SPA按摩 $500 折價券',
    description: '指定合作SPA館所使用。',
    image: '/image/coupon_gift.png',
    pointsCost: 5000,
    category: 'coupon',
  },
  {
    id: 'item-headphone',
    name: '降噪藍牙耳機',
    description: '讓您在飛機或火車上享受寧靜的旅程。',
    image: '/image/gifts/gift_item5.png',
    pointsCost: 8000,
    category: 'item',
  },
  {
    id: 'coupon-taxi-50',
    name: '計程車 $50 折價券',
    description: '適用於指定叫車平台。',
    image: '/image/coupon_gift.png',
    pointsCost: 600,
    category: 'coupon',
  },
  {
    id: 'coupon-all-10-percent',
    name: '全站 9 折優惠券',
    description: '全站商品適用，不限金額，最高折抵 $500。',
    image: '/image/coupon_gift.png',
    pointsCost: 4500,
    category: 'coupon',
  },
  {
    id: 'coupon-hotel-percent-15',
    name: '飯店住宿 85 折優惠券',
    description: '預訂飯店住宿享 85 折，最高折抵 $1000。',
    image: '/image/coupon_gift.png',
    pointsCost: 7000,
    category: 'coupon',
  },
  {
    id: 'coupon-flight-percent-5',
    name: '機票 95 折優惠券',
    description: '預訂機票享 95 折，最高折抵 $300。',
    image: '/image/coupon_gift.png',
    pointsCost: 2500,
    category: 'coupon',
  },
  {
    id: 'coupon-attraction-buyonegetone',
    name: '景點門票買一送一券',
    description: '指定熱門景點門票買一送一。',
    image: '/image/coupon_gift.png',
    pointsCost: 6000,
    category: 'coupon',
  },
  {
    id: 'coupon-transport-airport-pickup',
    name: '機場接送 $150 折價券',
    description: '適用於機場接送服務，讓您輕鬆往返機場。',
    image: '/image/coupon_gift.png',
    pointsCost: 1800,
    category: 'coupon',
  },
  {
    id: 'coupon-hotel-free-breakfast',
    name: '飯店早餐兌換券',
    description: '預訂飯店時可兌換免費早餐一份。',
    image: '/image/coupon_gift.png',
    pointsCost: 1500,
    category: 'coupon',
  },
  {
    id: 'coupon-wifi-rental-day',
    name: '行動 WiFi 租用一日券',
    description: '免費租用行動 WiFi 一天，保持旅途連線。',
    image: '/image/coupon_gift.png',
    pointsCost: 1000,
    category: 'coupon',
  },
  {
    id: 'coupon-all-purpose-500',
    name: '全站通用 $500 折價券',
    description: '全站商品適用，消費滿 $5000 可使用。',
    image: '/image/coupon_gift.png',
    pointsCost: 4800,
    category: 'coupon',
  },
  {
    id: 'coupon-all-purpose-800',
    name: '全站通用 $800 折價券',
    description: '全站商品適用，消費滿 $8000 可使用。',
    image: '/image/coupon_gift.png',
    pointsCost: 7500,
    category: 'coupon',
  },
  {
    id: 'coupon-all-20-percent',
    name: '全站 8 折優惠券',
    description: '全站商品適用，不限金額，最高折抵 $800。',
    image: '/image/coupon_gift.png',
    pointsCost: 6500,
    category: 'coupon',
  },
  {
    id: 'coupon-hotel-night-free',
    name: '飯店免費住宿一晚兌換券',
    description: '指定合作飯店免費住宿一晚（限平日）。',
    image: '/image/coupon_gift.png',
    pointsCost: 15000,
    category: 'coupon',
  },
  {
    id: 'coupon-flight-upgrade',
    name: '機票艙等升級券',
    description: '預訂機票時可免費升等至商務艙（限短程航班）。',
    image: '/image/coupon_gift.png',
    pointsCost: 10000,
    category: 'coupon',
  },
  {
    id: 'coupon-attraction-vip',
    name: '景點快速通關券',
    description: '指定熱門景點免排隊快速通關一次。',
    image: '/image/coupon_gift.png',
    pointsCost: 4000,
    category: 'coupon',
  },
  {
    id: 'coupon-transport-rental-car',
    name: '租車 $300 折價券',
    description: '適用於合作租車平台。',
    image: '/image/coupon_gift.png',
    pointsCost: 2800,
    category: 'coupon',
  },
  {
    id: 'coupon-luggage-storage',
    name: '行李寄存免費券',
    description: '指定車站或機場行李寄存服務免費一次。',
    image: '/image/coupon_gift.png',
    pointsCost: 800,
    category: 'coupon',
  },
  {
    id: 'coupon-tour-guide',
    name: '在地導覽 $200 折價券',
    description: '預訂在地私人導覽服務可折抵 $200。',
    image: '/image/coupon_gift.png',
    pointsCost: 2000,
    category: 'coupon',
  },
  {
    id: 'coupon-hotel-discount-200',
    name: '飯店住宿 $200 折價券',
    description: '預訂飯店住宿可折抵 $200，消費滿 $2500 可使用。',
    image: '/image/coupon_gift.png',
    pointsCost: 2000,
    category: 'coupon',
  },
  {
    id: 'coupon-flight-discount-500',
    name: '機票 $500 折價券',
    description: '預訂國際機票可折抵 $500，消費滿 $8000 可使用。',
    image: '/image/coupon_gift.png',
    pointsCost: 4500,
    category: 'coupon',
  },
  {
    id: 'coupon-attraction-discount-100',
    name: '景點門票 $100 折價券',
    description: '購買指定熱門景點門票可折抵 $100。',
    image: '/image/coupon_gift.png',
    pointsCost: 1000,
    category: 'coupon',
  },
  {
    id: 'coupon-transport-discount-50',
    name: '大眾交通 $50 折價券',
    description: '適用於捷運、公車一日票或儲值卡。',
    image: '/image/coupon_gift.png',
    pointsCost: 400,
    category: 'coupon',
  },
  {
    id: 'coupon-all-purpose-1000',
    name: '全站通用 $1000 折價券',
    description: '全站商品適用，消費滿 $10000 可使用。',
    image: '/image/coupon_gift.png',
    pointsCost: 9500,
    category: 'coupon',
  },
  {
    id: 'coupon-hotel-late-checkout',
    name: '飯店延遲退房券',
    description: '指定合作飯店可延遲退房至下午 2 點。',
    image: '/image/coupon_gift.png',
    pointsCost: 1800,
    category: 'coupon',
  },
  {
    id: 'coupon-flight-extra-baggage',
    name: '機票額外行李券',
    description: '可免費加購一件託運行李（限經濟艙）。',
    image: '/image/coupon_gift.png',
    pointsCost: 3000,
    category: 'coupon',
  },
  {
    id: 'coupon-attraction-combo',
    name: '景點套票 $150 折價券',
    description: '購買指定景點套票可折抵 $150。',
    image: '/image/coupon_gift.png',
    pointsCost: 1500,
    category: 'coupon',
  },
  {
    id: 'coupon-transport-private-car',
    name: '包車服務 $200 折價券',
    description: '預訂私人包車服務可折抵 $200。',
    image: '/image/coupon_gift.png',
    pointsCost: 2500,
    category: 'coupon',
  },
  {
    id: 'coupon-travel-insurance',
    name: '旅遊保險 $100 折價券',
    description: '購買旅遊平安險或不便險可折抵 $100。',
    image: '/image/coupon_gift.png',
    pointsCost: 900,
    category: 'coupon',
  },
  {
    id: 'item-travel-backpack',
    name: '多功能旅行背包',
    description: '大容量設計，多層收納，適合長途旅行。',
    image: '/image/gifts/gift_item6.png',
    pointsCost: 7500,
    category: 'item',
  },
  {
    id: 'item-portable-fan',
    name: '迷你手持電風扇',
    description: '炎熱天氣下的消暑神器，小巧便攜。',
    image: '/image/gifts/gift_item7.png',
    pointsCost: 1000,
    category: 'item',
  },
  {
    id: 'item-travel-journal',
    name: '旅行手帳本',
    description: '記錄旅途中的點滴回憶。',
    image: '/image/gifts/gift_item8.png',
    pointsCost: 1200,
    category: 'item',
  },
  {
    id: 'item-bluetooth-tracker',
    name: '藍牙防丟器',
    description: '追蹤行李或鑰匙，避免遺失，旅途更安心。',
    image: '/image/gifts/gift_item9.png',
    pointsCost: 1800,
    category: 'item',
  },
]);

// 兌換類別 應該會再增加(根據table)
const categories = ref([
  { label: '所有品項', value: 'all' },
  { label: '折價券', value: 'coupon' },
  { label: '實體商品', value: 'item' },
  { label: '試試手氣', value: 'gacha' },
]);

// 當前選中的類別
const currentCategory = ref('all');

// 根據類別過濾品項
const filteredItems = computed(() => {
  if (currentCategory.value === 'all') {
    return giftItems.value;
  }
  return giftItems.value.filter(
    (item) => item.category === currentCategory.value
  );
});

// 計算總頁數
const totalPages = computed(() => {
  return Math.ceil(filteredItems.value.length / itemsPerPage);
});

// 計算當前頁面顯示的品項
const paginatedItems = computed(() => {
  const startIndex = (currentPage.value - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  return filteredItems.value.slice(startIndex, endIndex);
});


// 過濾品項 (當類別改變時，重置頁碼到第一頁，並重置抽獎狀態)
const filterItems = (category) => {
  currentCategory.value = category;
  currentPage.value = 1; // 重置到第一頁
  // 如果切換到抽獎頁籤，重置抽獎狀態
  if (category === 'gacha') {
    winningItem.value = null;
    isSpinning.value = false;
  }
};

// 分頁方法
const goToPage = (page) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
  }
};

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--;
  }
};

const nextPage = () => {
  if (currentPage.value < totalPages.value) {
    currentPage.value++;
  }
};


// 確認兌換
const confirmExchange = async (item) => {
  const result = await Swal.fire({
    title: '確認兌換？',
    html: `您將花費 <strong>${item.pointsCost}</strong> 點兌換「<strong>${item.name}</strong>」`,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: '確認兌換',
    cancelButtonText: '取消',
    confirmButtonColor: '#3085d6',
    cancelButtonColor: '#d33',
  });

  if (result.isConfirmed) {
    exchangeItem(item);
  }
};

// 執行兌換邏輯
const exchangeItem = async (item) => {
  if (userPoints.value < item.pointsCost) {
    Swal.fire({
      icon: 'error',
      title: '點數不足',
      text: `您的點數不足以兌換此商品。`,
      confirmButtonText: '了解',
    });
    return;
  }

  try {
    // 發送 POST 請求到後端，要求扣除點數並記錄
    const response = await axios.post('/api/points/deduct', {
      pointsToDeduct: item.pointsCost, // 要扣除的點數
      reason: `兌換商品: ${item.name}`, // 點數異動原因
      itemId: item.id // 兌換的商品 ID
    });

    // console.log('兌換操作後後端回傳的完整資料:', response); // 保持用於除錯

    // 判斷後端回傳的字串是否為成功的訊息
    if (response.data === '點數扣除成功。') {
      // console.log('點數已成功扣除並同步到後端');
      // 扣點成功後，重新從後端獲取最新的點數，確保顯示正確
      await fetchUserPoints();
      Swal.fire({
        icon: 'success',
        title: '兌換成功！',
        html: `<span style="font-size: 18px;">您已成功兌換「<strong>${item.name}</strong>」。<br>剩餘點數：<strong>${userPoints.value}</strong> 點。</span>`,
        showConfirmButton: false,
        timer: 2500,
      });
    } else {
      // 如果後端回傳的不是成功的字串，則視為失敗。
      // console.error('後端更新點數失敗:', response.data);
      Swal.fire('錯誤', `點數扣除失敗: ${response.data || '未知錯誤'}`, 'error');
    }
  } catch (error) {
    // console.error('更新點數到後端失敗:', error);
    if (error.response) {
      // console.error('網路錯誤時後端回傳的詳細錯誤:', error.response);
    }
    let errorMessage = '無法更新您的點數，請檢查網路連線或稍後再試。';
    if (error.response && error.response.data && typeof error.response.data === 'string') {
      errorMessage = error.response.data; // 如果後端直接回傳錯誤字串
    } else if (error.response && error.response.data && error.response.data.message) {
      errorMessage = error.response.data.message; // 如果後端回傳錯誤物件有 message 屬性
    }
    Swal.fire('網路錯誤', errorMessage, 'error');
  }
};


// 抽獎功能相關方法
const startGacha = async () => {
  if (isSpinning.value) return; // 避免重複點擊

  if (userPoints.value < gachaCost.value) {
    Swal.fire({
      icon: 'error',
      title: '點數不足',
      text: `您的點數不足以進行抽獎。每次抽獎需要 ${gachaCost.value} 點。`,
      confirmButtonText: '了解',
    });
    return;
  }

  // 加入確認抽獎的確認彈窗 (已補齊完整程式碼)
  const result = await Swal.fire({
    title: '確認抽獎？',
    html: `您將花費 <strong>${gachaCost.value}</strong> 點進行抽獎。`,
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: '確認抽獎',
    cancelButtonText: '取消',
    confirmButtonColor: '#3085d6',
    cancelButtonColor: '#d33',
  });

  if (result.isConfirmed) {
    try {
      // 發送 POST 請求到後端，要求扣除抽獎點數並記錄
      const deductResponse = await axios.post('/api/points/deduct', {
        pointsToDeduct: gachaCost.value,
        reason: '點數商城抽獎', // 點數異動原因
        itemId: 'gacha_draw' // 抽獎的通用 ID
      });

      // console.log('抽獎扣點後後端回傳的完整資料:', deductResponse); // 保持用於除錯

      // 判斷後端回傳的字串是否為成功的訊息 (與兌換邏輯保持一致)
      if (deductResponse.data !== '點數扣除成功。') {
        Swal.fire('錯誤', `點數扣除失敗: ${deductResponse.data || '未知錯誤'}`, 'error');
        return; // 如果扣點失敗，則不進行抽獎動畫
      }

      // 扣點成功，開始抽獎動畫和邏輯
      winningItem.value = null; // 重置中獎品項
      isSpinning.value = true; // 開始抽獎動畫
      await fetchUserPoints(); // 扣點成功後，重新從後端獲取最新的點數

      // 模擬抽獎延遲和結果 (前端隨機選擇獎品)
      setTimeout(() => {
        isSpinning.value = false; // 停止動畫

        // 步驟 1: 過濾出所有 'coupon' 類別的品項
        const couponItems = giftItems.value.filter(item => item.category === 'coupon');

        let prize = null;
        if (couponItems.length > 0) {
          // 步驟 2: 從折價券品項中隨機選擇一個獎品
          const randomIndex = Math.floor(Math.random() * couponItems.length);
          prize = couponItems[randomIndex];
        } else {
          // 如果沒有可抽的折價券，可以設定一個預設的「銘謝惠顧」或錯誤處理
          // console.warn('沒有可供抽獎的折價券品項！');
          // 這裡可以選擇不設定 winningItem，或者設定一個特定的「銘謝惠顧」品項
          // winningItem.value = { name: '銘謝惠顧', image: '/image/no_prize.png' }; 
        }

        winningItem.value = prize; // 設定中獎品項

        if (prize) {
          Swal.fire({
            icon: 'success',
            title: '恭喜您中獎！',
            html: `<span style="font-size: 18px;">您抽到了：<strong>${prize.name}</strong>！</span>`,
            confirmButtonText: '太棒了',
          });
        } else {
          Swal.fire({
            icon: 'info',
            title: '哎呀',
            text: '這次沒有中獎，再接再厲！',
            confirmButtonText: '了解',
          });
        }
      }, 1500); // 這裡的時間可以保持您設定的秒數

    } catch (error) {
      // console.error('抽獎失敗:', error);
      if (error.response) {
        // console.error('網路錯誤時後端回傳的詳細錯誤:', error.response);
      }
      let errorMessage = '無法進行抽獎，請檢查網路連線或稍後再試。';
      if (error.response && error.response.data && typeof error.response.data === 'string') {
        errorMessage = error.response.data;
      } else if (error.response && error.response.data && error.response.data.message) {
        errorMessage = error.response.data.message;
      }
      Swal.fire('網路錯誤', errorMessage, 'error');
      // 即使發生錯誤，也嘗試重新獲取點數以反映真實狀態
      await fetchUserPoints();
    }
  }
};

// 重置抽獎狀態，以便再次抽獎
const resetGacha = () => {
  winningItem.value = null;
  isSpinning.value = false;
};

const gachaSlotImages = ref([
  '/image/gift.png', // 替換成你的佔位圖路徑
  '/image/gift.png',
  '/image/gift.png',
  '/image/gift.png',
  '/image/gift.png',
  '/image/gift.png',
]);

// 添加滾動物品數據
const slotItems = computed(() => {
  // 創建一個循環的物品列表用於滾動效果
  // 現在使用 gachaSlotImages 而不是 giftItems.value
  const items = [...gachaSlotImages.value, ...gachaSlotImages.value];
  return items.map((image, index) => ({ id: `slot-item-${index}`, image }));
});

// 在組件掛載後從後端獲取使用者點數
onMounted(async () => {
  // 同時執行數據載入和最小載入時間的等待
  await Promise.all([
    fetchUserPoints(), // 獲取使用者點數
    new Promise(resolve => setTimeout(resolve, MIN_LOADING_TIME)) // 確保載入畫面至少顯示 MIN_LOADING_TIME
  ]);
  isPageLoading.value = false; // 所有載入完成後，隱藏載入疊層
});
</script>

<style scoped>
/* 載入中疊層的樣式 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.9);
  /* 半透明白色背景 */
  display: flex;
  flex-direction: column;
  /* 讓內容垂直堆疊：Logo 在上，文字在下 */
  justify-content: center;
  /* 垂直居中 */
  align-items: center;
  /* 水平居中 */
  z-index: 9999;
  /* 確保它在最上層 */
  backdrop-filter: blur(5px);
  /* 添加模糊效果 */
}

.blinking-logo {
  width: 150px;
  /* 根據您的需求調整 Logo 大小 */
  height: auto;
  margin-bottom: 20px;
  /* Logo 和文字之間的間距 */
  animation: blink-animation 1.5s infinite alternate;
  /* 正確的動畫名稱 */
}

.loading-text {
  color: #333;
  font-size: 1.8em;
  font-weight: bold;
}

/* 確保 @keyframes 的名稱與 .blinking-logo 中的 animation 屬性匹配 */
@keyframes blink-animation {
  0% {
    opacity: 1;
  }

  50% {
    opacity: 0.3;
  }

  100% {
    opacity: 1;
  }
}

.gift-mall-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding-top: 100px;
  padding-bottom: 40px;
  background-color: #f5f7fa;
}

.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  box-sizing: border-box;
}

.gift-mall-header {
  text-align: center;
  margin-bottom: 40px;
  position: relative;
}

.gift-mall-header h1 {
  font-size: 36px;
  color: #333;
  margin-bottom: 10px;
}

.gift-mall-header p {
  font-size: 18px;
  color: #666;
  margin-bottom: 20px;
}

.current-points {
  font-size: 24px;
  color: #007bff;
  font-weight: bold;
}


/* 兌換類別 */
.gift-categories {
  margin-bottom: 30px;
  text-align: center;
}

.gift-categories h2 {
  font-size: 24px;
  color: #333;
  margin-bottom: 15px;
}

.categories-wrapper {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.categories-wrapper button {
  padding: 10px 25px;
  border: 1px solid #ccc;
  border-radius: 25px;
  background-color: #f0f0f0;
  color: #555;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.categories-wrapper button:hover {
  background-color: #e0e0e0;
  border-color: #a0a0a0;
}

.categories-wrapper button.active {
  background-color: #007bff;
  color: #fff;
  border-color: #007bff;
  box-shadow: 0 2px 5px rgba(0, 123, 255, 0.3);
}

/* 禮物品項 */
.gift-items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 30px;
  justify-content: center;
}

.gift-item-card {
  background-color: #fff;
  border: 1px solid #eee;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  display: flex;
  flex-direction: column;
}

.gift-item-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.item-image {
  width: 100%;
  height: 300px;
  /* Adjusted height to extend further down */
  object-fit: cover;
  border-bottom: 1px solid #eee;
}

.item-details {
  padding: 20px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}

.item-details h3 {
  font-size: 22px;
  color: #333;
  margin-bottom: 10px;
  min-height: 50px;
}

.item-description {
  font-size: 15px;
  color: #777;
  line-height: 1.5;
  margin-bottom: 15px;
  flex-grow: 1;
  display: -webkit-box;
  /* -webkit-line-clamp: 3; */
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-price {
  display: flex;
  justify-content: flex-start;
  /* 將此行從 space-between 改為 flex-start */
  align-items: baseline;
  margin-top: 15px;
  margin-bottom: 20px;
  gap: 147px;
  /* 為兩個 span 之間增加間距 */
}

.item-stock {
  font-size: 18px;
  /* Adjust font size as needed */
  color: #28a745;
  /* Green color for "in stock" */
  font-weight: bold;
  margin-right: 10px;
  /* Add some space between "尚有庫存" and points */
}

.points-cost {
  font-size: 26px;
  color: #ff5722;
  font-weight: bold;
}

.exchange-button {
  width: 100%;
  padding: 12px 0;
  background-color: #28a745;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.exchange-button:hover {
  background-color: #218838;
}

.exchange-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.no-items-message {
  grid-column: 1 / -1;
  text-align: center;
  font-size: 20px;
  color: #888;
  padding: 50px;
}

/* 分頁樣式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 40px;
  gap: 10px;
}

.pagination-button {
  padding: 10px 18px;
  border: 1px solid #007bff;
  border-radius: 5px;
  background-color: #fff;
  color: #007bff;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  min-width: 40px;
}

.pagination-button:hover:not(:disabled) {
  background-color: #e7f1ff;
}

.pagination-button.active {
  background-color: #007bff;
  color: #fff;
}

.pagination-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background-color: #f0f0f0;
  color: #888;
  border-color: #ccc;
}


/* 抽獎頁面內容樣式 */
.gacha-page-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  text-align: center;
  margin-top: 30px;
  position: relative;
  overflow: hidden;
}

.gacha-page-content::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  animation: rotate-bg 20s linear infinite;
  z-index: 1;
}

.gacha-page-content>* {
  position: relative;
  z-index: 2;
}

.gacha-page-content h2 {
  font-size: 36px;
  color: #fff;
  margin-bottom: 15px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
  font-weight: 800;
}

.gacha-description {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 40px;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.3);
}

.gacha-machine-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
}

.gacha-machine-container {
  width: 300px;
  height: 300px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 30px;
  position: relative;
}

/* 背景光暈效果 - 移除 active 狀態的變化 */
.gacha-glow {
  position: absolute;
  width: 250px;
  height: 250px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 193, 7, 0.3) 0%, transparent 70%);
  animation: pulse-glow 2s ease-in-out infinite;
  z-index: 1;
}

/* 移除粒子效果相關樣式 */

/* 抽獎機主體 - 移除震動效果 */
.gacha-machine {
  width: 220px;
  height: 280px;
  background: linear-gradient(145deg, #e6e6e6, #ffffff);
  border-radius: 20px;
  position: relative;
  z-index: 2;
  box-shadow:
    0 20px 40px rgba(0, 0, 0, 0.2),
    inset 0 -5px 10px rgba(0, 0, 0, 0.1),
    inset 0 5px 10px rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.gacha-machine.winning {
  animation: machine-celebrate 0.6s ease-in-out;
}

.machine-inner {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

/* 機器頂部 */
.machine-top {
  height: 30px;
  background: linear-gradient(145deg, #ff6b6b, #ee5a52);
  border-radius: 15px;
  position: relative;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.2);
}

.machine-top::after {
  content: '';
  position: absolute;
  top: 5px;
  left: 10px;
  right: 10px;
  height: 5px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 3px;
}

/* 顯示窗口 */
.machine-window {
  height: 140px;
  background: linear-gradient(145deg, #1a1a1a, #2d2d2d);
  border-radius: 15px;
  position: relative;
  overflow: hidden;
  border: 3px solid #333;
  display: flex;
  align-items: center;
  justify-content: center;
}

.window-shine {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  animation: window-shine 3s ease-in-out infinite;
}

.machine-logo {
  font-size: 60px;
  color: #ffc107;
  text-shadow: 0 0 20px rgba(255, 193, 7, 0.5);
  animation: logo-glow 2s ease-in-out infinite;
}

/* 滾動物品 */
.slot-items {
  display: flex;
  flex-direction: column;
  position: absolute;
  /* 相對於父層 .machine-window 定位 */
  top: 0;
  /* 從頂部開始滾動 */
  animation: slot-roll 1.1s linear infinite;
  flex-shrink: 0;
}

.slot-item {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 8px 0;
}

.slot-item img {
  width: 45px;
  height: 45px;
  object-fit: cover;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

/* 按鈕區域 */
.machine-button-area {
  display: flex;
  justify-content: center;
  margin: 15px 0;
}

.machine-button {
  width: 50px;
  height: 50px;
  background: linear-gradient(145deg, #ff6b6b, #ee5a52);
  border-radius: 50%;
  box-shadow:
    0 5px 15px rgba(0, 0, 0, 0.3),
    inset 0 -2px 5px rgba(0, 0, 0, 0.2),
    inset 0 2px 5px rgba(255, 255, 255, 0.3);
  transition: all 0.1s ease;
  position: relative;
}

.machine-button::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
}

.machine-button.pressed {
  transform: translateY(3px);
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.3),
    inset 0 -1px 3px rgba(0, 0, 0, 0.2),
    inset 0 1px 3px rgba(255, 255, 255, 0.3);
}

/* 機器底部 */
.machine-bottom {
  height: 20px;
  background: linear-gradient(145deg, #ccc, #ddd);
  border-radius: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 成功爆炸效果 */
.success-burst {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 4;
}

.burst-ray {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 4px;
  height: 60px;
  background: linear-gradient(to top, transparent, #ffc107, transparent);
  transform-origin: 0 0;
  transform: rotate(var(--ray-angle)) translateX(-2px);
  animation: burst-ray 0.6s ease-out;
}

/* 抽獎按鈕 */
.spin-button {
  padding: 18px 40px;
  background: linear-gradient(145deg, #ffc107, #ffb300);
  color: #333;
  border: none;
  border-radius: 50px;
  font-size: 22px;
  font-weight: bold;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  box-shadow:
    0 8px 20px rgba(255, 193, 7, 0.4),
    inset 0 -3px 8px rgba(0, 0, 0, 0.1),
    inset 0 3px 8px rgba(255, 255, 255, 0.3);
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.spin-button:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow:
    0 12px 30px rgba(255, 193, 7, 0.6),
    inset 0 -3px 8px rgba(0, 0, 0, 0.1),
    inset 0 3px 8px rgba(255, 255, 255, 0.3);
}

.spin-button:active:not(:disabled) {
  transform: translateY(0);
}

.spin-button:disabled {
  background: linear-gradient(145deg, #ccc, #999);
  cursor: not-allowed;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
}

.button-text {
  position: relative;
  z-index: 2;
}

.button-shine {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent);
  animation: button-shine 2s ease-in-out infinite;
  z-index: 1;
}

/* 提示文字 */
.gacha-pull-hint {
  position: absolute;
  bottom: -40px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  background: rgba(0, 0, 0, 0.2);
  padding: 8px 16px;
  border-radius: 20px;
  white-space: nowrap;
}

.gacha-pull-hint.disabled {
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.2);
}

/* 中獎結果 */
.gacha-result {
  margin-top: 40px;
  background: linear-gradient(145deg, #d4edda, #c3e6cb);
  padding: 30px;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  border: 3px solid #28a745;
  position: relative;
  overflow: hidden;
}

.result-animation {
  animation: result-appear 0.8s ease-out;
}

.gacha-result h3 {
  font-size: 32px;
  color: #28a745;
  margin-bottom: 25px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
  animation: title-bounce 0.6s ease-out;
}

.winning-item-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 25px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 15px;
  box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);
  max-width: 400px;
  margin: 0 auto 30px auto;
  position: relative;
}

.item-image-container {
  position: relative;
  animation: item-float 2s ease-in-out infinite;
}

.winning-item-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 15px;
  border: 3px solid #28a745;
  box-shadow: 0 5px 20px rgba(40, 167, 69, 0.3);
  position: relative;
  z-index: 2;
}

.image-glow {
  position: absolute;
  top: -5px;
  left: -5px;
  right: -5px;
  bottom: -5px;
  background: linear-gradient(45deg, #28a745, #20c997, #17a2b8, #28a745);
  border-radius: 20px;
  z-index: 1;
  animation: glow-rotate 2s linear infinite;
}

.winning-item-details h4 {
  font-size: 26px;
  color: #333;
  margin-bottom: 10px;
  font-weight: 700;
}

.winning-item-details p {
  font-size: 16px;
  color: #555;
  margin-bottom: 0;
  line-height: 1.5;
}

/* 慶祝彩帶 */
.celebration-confetti {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 3;
}

.confetti {
  position: absolute;
  width: 10px;
  height: 10px;
  background: var(--color);
  top: -20px;
  left: var(--x);
  transform: rotate(var(--rotation));
  animation: confetti-fall 3s ease-out infinite;
  animation-delay: var(--delay);
}

/* 點數不足提示 */
.gacha-not-enough-points {
  color: #dc3545;
  font-size: 18px;
  font-weight: bold;
  margin-top: 30px;
  padding: 20px;
  border: 2px solid #dc3545;
  background: linear-gradient(145deg, #f8d7da, #f5c6cb);
  border-radius: 15px;
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
  box-shadow: 0 5px 15px rgba(220, 53, 69, 0.2);
  animation: warning-pulse 1s ease-in-out infinite;
}

.warning-icon {
  font-size: 24px;
  margin-right: 10px;
}

/* 動畫定義 */
@keyframes rotate-bg {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

@keyframes pulse-glow {

  0%,
  100% {
    transform: scale(1);
    opacity: 0.3;
  }

  50% {
    transform: scale(1.1);
    opacity: 0.5;
  }
}

/* 移除 active-glow 和 particle-fly 動畫 */
/* 移除 machine-shake 動畫 */

@keyframes machine-celebrate {

  0%,
  100% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.05);
  }
}

@keyframes window-shine {
  0% {
    left: -100%;
  }

  100% {
    left: 100%;
  }
}

@keyframes logo-glow {

  0%,
  100% {
    text-shadow: 0 0 20px rgba(255, 193, 7, 0.5);
  }

  50% {
    text-shadow: 0 0 30px rgba(255, 193, 7, 0.8);
  }
}

@keyframes slot-roll {
  0% {
    transform: translateY(0);
  }

  100% {
    /* 精確移動一個完整品項組的高度 (6 個品項 * 66px/品項 = 396px) */
    transform: translateY(-396px);
  }
}

@keyframes burst-ray {
  0% {
    height: 0;
    opacity: 0;
  }

  50% {
    height: 60px;
    opacity: 1;
  }

  100% {
    height: 80px;
    opacity: 0;
  }
}

@keyframes button-shine {
  0% {
    left: -100%;
  }

  100% {
    left: 100%;
  }
}

@keyframes result-appear {
  0% {
    opacity: 0;
    transform: translateY(30px) scale(0.8);
  }

  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes title-bounce {

  0%,
  20%,
  50%,
  80%,
  100% {
    transform: translateY(0);
  }

  40% {
    transform: translateY(-10px);
  }

  60% {
    transform: translateY(-5px);
  }
}

@keyframes item-float {

  0%,
  100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-10px);
  }
}

@keyframes glow-rotate {
  0% {
    transform: rotate(0deg);
  }

  100% {
    transform: rotate(360deg);
  }
}

@keyframes confetti-fall {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 1;
  }

  100% {
    transform: translateY(400px) rotate(720deg);
    opacity: 0;
  }
}

@keyframes warning-pulse {

  0%,
  100% {
    transform: scale(1);
  }

  50% {
    transform: scale(1.02);
  }
}

/* 響應式設計 */
@media (max-width: 768px) {
  .gacha-machine-container {
    width: 250px;
    height: 250px;
  }

  .gacha-machine {
    width: 180px;
    height: 230px;
  }

  .machine-window {
    height: 120px;
  }

  .spin-button {
    padding: 15px 30px;
    font-size: 18px;
  }
}
</style>
