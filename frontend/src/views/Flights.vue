<template>
  <div class="flights-page">
    <Header />
    <!-- 搜尋區域 -->
    <div class="container my-5 pt-4">
      <FlightsSearch :form="searchForm" :showResetButton="true" @search="updateFormAndSearch" @reset="loadAllFlights" />
      <div class="row mt-4 flight-results">
        <!-- 篩選區塊：左側 -->
        <div class="col-md-3">
          <FlightsFilter v-model:selectedAirlines="selectedAirlines" v-model:priceMax="priceMax"
            v-model:selectedDepartureAirports="selectedDepartureAirports"
            v-model:selectedArrivalAirports="selectedArrivalAirports" v-model:departureTimeRange="departureTimeRange"
            v-model:returnTimeRange="returnTimeRange" />
        </div>
        <!-- 卡片區塊：右側 -->
        <div class="col-md-9">
          <FlightCard v-for="flightDetail in paginatedFlights" :key="flightDetail.flightsDetailsId"
            :flightDetail="flightDetail" :tripType="searchForm.tripType" :selectedRoundFlights="selectedRoundFlights"
            :selectedMultiFlights="selectedMultiFlights" @flight-select="handleFlightSelect" />
        </div>
      </div>
    </div>
  </div>
  <!-- 分頁按鈕 -->
  <div class="d-flex justify-content-center mt-4">
    <nav>
      <ul class="pagination">
        <li class="page-item" :class="{ disabled: currentPage === 1 }">
          <a class="page-link" href="#" @click.prevent="currentPage--">上一頁</a>
        </li>
        <li class="page-item" v-for="page in totalPages" :key="page" :class="{ active: currentPage === page }">
          <a class="page-link" href="#" @click.prevent="currentPage = page">{{ page }}</a>
        </li>
        <li class="page-item" :class="{ disabled: currentPage === totalPages }">
          <a class="page-link" href="#" @click.prevent="currentPage++">下一頁</a>
        </li>
      </ul>
    </nav>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { useRouter } from 'vue-router';
import Header from '../components/Header.vue';
import FlightsSearch from '../components/flights/FlightsSearch.vue';
import FlightsFilter from '../components/flights/FlightsFilter.vue';
import FlightCard from '../components/flights/FlightCard.vue';
import axios from 'axios';

const route = useRoute();
const router = useRouter();
const flights = ref([]);
const rawFlights = ref([]);
const selectedAirlines = ref([]);
const priceMax = ref(30000);
const selectedDepartureAirports = ref([]);
const selectedArrivalAirports = ref([]);
const selectedRoundFlights = ref([null, null]); // [去程, 回程]
const selectedMultiFlights = ref([]);// 用於多行程搜尋
// 艙等優先順序對照表
const cabinClassPriority = {
  '經濟艙': 1,
  '商務艙': 2,
  '頭等艙': 3
};


// 排序航班列表
const sortFlightsByDepartureDateTime = (flightList) => {
  return flightList
    .filter(f => f && f.departureTime)
    .sort((a, b) => {
      const aDate = new Date(a.departureTime.replace(' ', 'T'));
      const bDate = new Date(b.departureTime.replace(' ', 'T'));
      if (aDate.getTime() !== bDate.getTime()) {
        return aDate - bDate; // 先比出發時間
      }
      // 出發時間相同時，依照艙等排序
      const classA = cabinClassPriority[a.flightclass] || 99;
      const classB = cabinClassPriority[b.flightclass] || 99;
      return classA - classB;
    });
};

//時間範圍
const departureTimeRange = ref([0, 1439]); // 起飛時間範圍：00:00 - 23:59
const returnTimeRange = ref([0, 1439]);    // 到達時間範圍：00:00 - 23:59

// 格式化時間顯示
const getMinutesFromTime = (datetimeStr) => {
  if (!datetimeStr) return 0;
  const timePart = datetimeStr.split('T')[1] || datetimeStr.split(' ')[1];
  const [hourStr, minuteStr] = timePart.split(':');
  const hour = parseInt(hourStr) || 0;
  const minute = parseInt(minuteStr) || 0;
  return hour * 60 + minute;
};


// 分頁相關
const currentPage = ref(1);
const flightsPerPage = 10;
const filteredFlights = computed(() => {
  return flights.value.filter((f) => {
    const airlineMatch =
      selectedAirlines.value.length === 0 ||
      selectedAirlines.value.includes(f.airline);
    const priceMatch = f.classPrice <= priceMax.value;
    const departureMatch =
      selectedDepartureAirports.value.length === 0 ||
      selectedDepartureAirports.value.includes(f.departureAirport);
    const arrivalMatch =
      selectedArrivalAirports.value.length === 0 ||
      selectedArrivalAirports.value.includes(f.arrivalAirport);
    // 判斷起飛時間是否在範圍內
    const departureMinutes = getMinutesFromTime(f.departureTime);
    const inDepartureTimeRange =
      departureMinutes >= departureTimeRange.value[0] &&
      departureMinutes <= departureTimeRange.value[1];
    // 判斷到達時間是否在範圍內
    const arrivalMinutes = getMinutesFromTime(f.arrivalTime);
    const inReturnTimeRange =
      arrivalMinutes >= returnTimeRange.value[0] &&
      arrivalMinutes <= returnTimeRange.value[1];
    return airlineMatch && priceMatch && departureMatch && arrivalMatch &&
      inDepartureTimeRange &&
      inReturnTimeRange;
  });
});

const paginatedFlights = computed(() => {
  const start = (currentPage.value - 1) * flightsPerPage;
  return filteredFlights.value.slice(start, start + flightsPerPage);
});

const totalPages = computed(() => {
  return Math.ceil(filteredFlights.value.length / flightsPerPage);
});

const searchForm = ref({
  tripType: 'one',
  departure: '',
  destination: '',
  departDate: '',
  returnDate: '',
  passengers: '1',
  cabinClass: '經濟艙',
  trips: [] // 用於多行程搜尋
});

const formatDate = (dateStr) => {
  if (!dateStr) return null;
  const date = new Date(dateStr);
  const yyyy = date.getFullYear();
  const mm = String(date.getMonth() + 1).padStart(2, '0');
  const dd = String(date.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
};

const handleSearch = async (searchParams) => {
  console.log('搜尋條件：', searchParams);

  try {
    let res;

    if (searchParams.tripType === 'multi') {
      res = await axios.post('/api/flights/search/multi', {
        trips: searchParams.trips,
        cabinClass: searchParams.cabinClass
      });
    } else {
      res = await axios.get('/api/flights/search', {
        params: {
          departure: searchParams.departure,
          destination: searchParams.destination,
          departDate: formatDate(searchParams.departDate),
          returnDate: formatDate(searchParams.returnDate),
          cabinClass: searchParams.cabinClass
        }
      });
    }

    flights.value = sortFlightsByDepartureDateTime(res.data);
    currentPage.value = 1;
    router.replace({ path: '/flights' });
  } catch (error) {
    console.error('搜尋航班失敗：', error);
  }
};



const updateFormAndSearch = (searchParams) => {
  localStorage.removeItem('checkoutFlight');
  localStorage.removeItem('roundTripCheckout');
  selectedRoundFlights.value = [null, null];
  // 更新整份 form，包括多行程 trips 陣列
  searchForm.value = {
    ...searchForm.value, // 原本的預設值（避免漏掉欄位）
    ...searchParams, // 使用者輸入的內容（包含 tripType、departure、trips 等）
    trips: searchParams.trips || [] // ✅ 額外確保 trips 正確補進
  };
  if (searchParams.tripType === 'multi') {
    selectedMultiFlights.value = new Array(searchParams.trips.length).fill(null);
  }
  handleSearch(searchParams);
};

watch(selectedAirlines, () => {
  currentPage.value = 1;
});
watch(priceMax, () => {
  currentPage.value = 1;
});
watch(selectedDepartureAirports, () => {
  currentPage.value = 1;
});
watch(selectedArrivalAirports, () => {
  currentPage.value = 1;
});
onMounted(async () => {
  localStorage.removeItem('checkoutFlight');
  localStorage.removeItem('roundTripCheckout');
  localStorage.removeItem('multiTripCheckout');
  // localStorage.removeItem('flightSearchForm');
  const q = route.query;
  const formatDateOnly = (str) => {
    if (!str) return '';
    const date = new Date(str);
    return date.toISOString().split('T')[0];
  };
  let searchTriggered = false; // ✅ 判斷是否成功做了搜尋
  // 判斷是否是多行程查詢
  if (q.tripType === 'multi' && q.trips) {
    try {
      const parsed = JSON.parse(q.trips);
      const trips = parsed.map(t => ({
        ...t,
        date: formatDateOnly(t.date)
      }));

      const searchParams = {
        tripType: 'multi',
        cabinClass: q.cabinClass || '經濟艙',
        passengers: {
          adult: Number(q.adult || 1),
          child: Number(q.child || 0),
          infant: Number(q.infant || 0)
        },
        trips
      };
      console.log('➡️ 多行程查詢', searchParams);

      updateFormAndSearch(searchParams);
      searchTriggered = true;
    } catch (err) {
      console.warn('❌ 多行程 JSON 格式錯誤：', err);
    }
  }
  // 判斷是否是單程 / 來回查詢
  if (!searchTriggered && q.tripType && q.departure && q.destination && q.departDate) {
    const searchParams = {
      tripType: q.tripType,
      departure: q.departure,
      destination: q.destination,
      departDate: formatDateOnly(q.departDate),
      returnDate: formatDateOnly(q.returnDate),
      cabinClass: q.cabinClass || '經濟艙',
      passengers: {
        adult: Number(q.adult || 1),
        child: Number(q.child || 0),
        infant: Number(q.infant || 0)
      },
      trips: []
    };
    console.log('➡️ 單程／來回查詢', searchParams);

    updateFormAndSearch(searchParams);
    searchTriggered = true;
  }
  //沒有 query，但 localStorage 有 flightSearchForm
  // if (!searchTriggered) {
  //   const saved = localStorage.getItem('flightSearchForm');
  //   if (saved) {
  //     try {
  //       const parsedForm = JSON.parse(saved);
  //       updateFormAndSearch(parsedForm);
  //       searchTriggered = true;
  //       console.log('📦 從 localStorage 還原搜尋條件');
  //     } catch (err) {
  //       console.warn('❌ localStorage 資料格式錯誤：', err);
  //     }
  //   }
  // }
  // 若沒有任何搜尋條件，顯示所有航班
  if (!searchTriggered) {
    try {
      const res = await axios.get('/api/flights/all');
      if (Array.isArray(res.data)) {
        flights.value = sortFlightsByDepartureDateTime(res.data);
        console.log('📦 載入所有航班');
      } else {
        flights.value = [];
        console.warn('⚠️ /flights/all 回傳不是陣列：', res.data);
      }
    } catch (err) {
      console.error('❌ 取得航班失敗：', err);
    }
  }
});

// 重新載入所有航班
const loadAllFlights = async () => {
  try {
    const res = await axios.get('/api/flights/all');
    if (Array.isArray(res.data)) {
      flights.value = sortFlightsByDepartureDateTime(res.data);
    } else {
      flights.value = [];
      console.warn('⚠️ /flights/all 回傳不是陣列：', res.data);
    }
    rawFlights.value = []; // 可視情況保留或清空
    currentPage.value = 1;
    // ✅ 清空搜尋條件，回復預設 form 狀態
    searchForm.value = {
      tripType: 'one',
      departure: '',
      destination: '',
      departDate: '',
      returnDate: '',
      passengers: '1',
      cabinClass: '經濟艙',
      trips: []
    };
    // ✅ 清除 localStorage 中儲存的查詢
    localStorage.removeItem('checkoutFlight');
    localStorage.removeItem('roundTripCheckout');
    localStorage.removeItem('multiTripCheckout');
    selectedRoundFlights.value = [null, null];
    selectedMultiFlights.value = [];
    // ✅ 重設篩選器
    selectedAirlines.value = [];
    selectedDepartureAirports.value = [];
    selectedArrivalAirports.value = [];
    priceMax.value = 30000;
    console.log('🔁 重新搜尋：所有航班重新載入');
  } catch (err) {
    console.error('❌ 無法重新載入航班：', err);
  }
};

const handleFlightSelect = (flightDetail) => {
  const tripType = searchForm.value.tripType;
  if (tripType === 'one') {
    // 若未透過搜尋，補上預設乘客資訊
    if (!localStorage.getItem('flightSearchForm')) {
      const defaultPassengerForm = {
        tripType: 'one',
        departure: flightDetail.departureAirport,
        destination: flightDetail.arrivalAirport,
        departDate: flightDetail.departureTime?.split('T')[0] || '',
        returnDate: '',
        cabinClass: flightDetail.flightclass || '經濟艙',
        passengers: {
          adult: 1,
          child: 0,
          infant: 0
        }
      };
      localStorage.setItem('flightSearchForm', JSON.stringify(defaultPassengerForm));
    }
    // ✅ 單程直接跳轉
    localStorage.setItem('checkoutFlight', JSON.stringify(flightDetail));
    router.push({ path: '/Checkout', query: { type: 'one' } });
  } else if (tripType === 'round') {
    // ✅ 來回選兩段
    if (!selectedRoundFlights.value[0]) {
      localStorage.removeItem('roundTripCheckout');
      selectedRoundFlights.value[0] = flightDetail;
    } else if (!selectedRoundFlights.value[1]) {
      selectedRoundFlights.value[1] = flightDetail;

      const roundTripData = {
        outbound: selectedRoundFlights.value[0],
        return: selectedRoundFlights.value[1]
      };
      localStorage.setItem('roundTripCheckout', JSON.stringify(roundTripData));
      router.push({ path: '/Checkout', query: { type: 'round' } });
    }
  } else if (tripType === 'multi') {
    // 找出第一個尚未選擇的段數
    const nextIndex = selectedMultiFlights.value.findIndex(f => f === null);

    if (nextIndex !== -1) {
      selectedMultiFlights.value[nextIndex] = flightDetail;
    }

    // 如果都選完了，就跳轉到 checkout
    const allSelected = selectedMultiFlights.value.every(f => f !== null);
    if (allSelected) {
      localStorage.setItem('multiTripCheckout', JSON.stringify(selectedMultiFlights.value));
      router.push({ path: '/Checkout', query: { type: 'multi' } });
    }
  }
};



</script>