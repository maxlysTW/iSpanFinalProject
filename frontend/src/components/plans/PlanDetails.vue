<template>
  <div class="plan-detail">
    <!-- Header -->
    <Header></Header>
    <div class="container my-5 pt-4">
      <div class="col-md-12 d-flex justify-content-between align-items-center">
        <div class="mb-2 mt-1 link-secondary" @click="goBackToPlan" style="cursor: pointer;">
          <i class="bi bi-arrow-left-circle me-2"></i>
          <span style="text-decoration: underline;">返回我的旅程</span>
        </div>
        <div class="mb-2 mt-2" style="font-size: 24px;">{{ title }}</div>
      </div>
      <div class="row">
        <!-- 左側導航 -->
        <div class="col-md-3">
          <div class="profile-sidebar">
            <!-- 導航選單 -->
            <div class="nav-menu">
              <div v-for="item in navItems" :key="item.id" class="nav-item" :class="{ active: activeTab === item.id }"
                @click="activeTab = item.id">
                <i :class="item.icon"></i>
                <span>{{ item.title }}</span>
              </div>
            </div>

            <!-- 景點清單：僅在 activeTab === 'activity' 時顯示 -->
            <div v-if="activeTab === 'activity'" class="mt-3 border-top pt-2">
              <h5 class="mb-2">景點清單</h5>
              <div class="card mb-2" draggable="true" @dragstart="(e) => onDragStart(place.title, e)"
                v-for="place in places" :key="place.id" @click="place.schedule && scrollToSchedule(place.schedule)">
                <div class="card-body p-2 d-flex justify-content-between align-items-center">
                  <div>
                    <h6 class="card-title mb-1">{{ place.title }}</h6>
                    <p class="card-text small text-muted mb-0">{{ place.desc }}</p>
                  </div>
                  <div class="text-end" v-if="place.schedule" style="min-width: 80px; font-size: 0.8rem; color: #666;">
                    <div>{{ place.schedule.day }}</div>
                    <div>{{ formatTimeRange(place.schedule) }}</div>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
        <!-- 右側內容 -->
        <div class="col-md-9">
          <div class="profile-content">

            <!-- 項目 -->
            <div v-if="activeTab === 'orders'" class="content-section">
              <div class="row">
                <div class="section-header position-relative">
                  <h4>詳細資訊</h4>
                  <p class="text-muted">檢視本旅程的項目</p>
                  <button class="btn rounded-0 border-0 bg-transparent border-bottom border-dark no-hover new-partner"
                    style="position: absolute; bottom: 10px; right: 0px; z-index: 10" @click="openSearch">
                    <i class="bi bi-plus-lg me-1"></i>新增景點
                  </button>
                </div>

                <!-- 機票 -->
                <div class="accordion mb-4" id="userAccordion">
                  <h5 class="section-header" style="padding-bottom: 5px; margin-bottom: 10px;">機票</h5>
                  <div class="card mb-2" v-for="(f, index) in flights" :key="index">
                    <h2 class="mb-0">
                      <div class="text-start w-100 p-3 plan-container">
                        <div class="d-flex justify-content-between align-items-center">
                          <div class="card-body" style="padding: 0px;">
                            <div class="d-flex flightTitle b-3 mb-3 align-items-center"
                              style="font-size: 18px; font-weight: bold;">
                              <div class="me-3">{{ f.airline }}</div>
                              <div class="me-3">{{ f.flightNumber }}</div>
                              <div class="text-secondary me-4">
                                {{ typeof f.departureTime === 'string' ? f.departureTime.substring(0, 10) : '' }}
                              </div>
                            </div>
                            <div class="mt-2 fs-6 text-primary">
                              <div class="d-flex ">
                                <div class="ms-3" style="flex-basis: 18%;">{{ `出發：${f.departureNameZh}
                                  (${f.departureAirport}),` }}</div>
                                <div class="ml-4" style="flex-basis: 12%;">{{ `${f.departureTerminal},` }}
                                </div>
                                <div class="ml-4" style="flex-basis: 15%;">{{ getTimeStr(f.departureTime) }}
                                </div>
                              </div>
                            </div>
                            <div class="mt-2 fs-6 text-success">
                              <div class="d-flex ">
                                <div class="ms-3" style="flex-basis: 18%;">{{ `抵達：${f.arrivalNameZh}
                                  (${f.arrivalAirport}),` }}</div>
                                <div class="ml-4" style="flex-basis: 12%;">{{ `${f.arrivalTerminal},` }}
                                </div>
                                <div class="ml-4" style="flex-basis: 15%;">{{ getTimeStr(f.arrivalTime) }}
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </h2>
                  </div>
                </div>
                <!-- 機票 -->

                <!-- 住宿 -->
                <div class="accordion mb-4" id="userAccordion">
                  <h5 class="section-header" style="padding-bottom: 5px; margin-bottom: 10px;">住宿</h5>
                  <div class="card mb-3" v-for="(h, index) in hotel">
                    <div class="card-body">
                      <div class="row g-3">
                        <div class="col-md-6">
                          <div>
                            <h5 class="mb-1 text-primary" style="font-weight: bold;">{{ h.name }}</h5>
                            <span class="text-secondary">{{ h.room }}</span>
                          </div>
                        </div>
                        <div class="col-md-6 ">
                          <div class="link-primary lnk mb-1" style="cursor: pointer;"
                            @click.stop.prevent="goToHotelPage(index)">
                            <i class="bi bi-arrow-up-right-square me-2">
                            </i>
                            <span style="font-size: 14px;">點擊前往房型資訊頁面</span>
                          </div>
                        </div>
                        <div class="col-md-6">
                          <div>入住日期：{{ h.checkIn }}</div>
                        </div>
                        <div class="col-md-6">
                          <div>退房日期：{{ h.checkOut }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- 住宿 -->

                <!-- 門票 -->
                <div class="accordion mb-4" id="userAccordion">
                  <h5 class="section-header" style="padding-bottom: 5px; margin-bottom: 10px;">門票</h5>
                  <div class="card mb-3" v-for="(t, index) in tickets">
                    <div class="card-body">
                      <div class="row g-3">
                        <div class="col-md-6">
                          <div>
                            <h5 class="mb-1 text-primary" style="font-weight: bold;">{{ t.title }}</h5>
                          </div>
                        </div>
                        <div class="col-md-6">
                          <div class="link-primary lnk mb-1" style="cursor: pointer;"
                            @click.stop.prevent="goToTicketPage">
                            <i class="bi bi-arrow-up-right-square me-2">
                            </i>
                            <span style="font-size: 14px;">點擊前往門票資訊頁面</span>
                          </div>
                        </div>
                        <div class="col-md-6">
                          <div>票券使用日期：{{ t.date }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                <!-- 門票 -->

                <!-- 自訂 -->
                <div class="accordion mb-4" id="userAccordion">
                  <h5 class="section-header" style="padding-bottom: 5px; margin-bottom: 10px;">景點</h5>
                  <!-- 這裡加 v-for -->
                  <div class="card mb-3" v-for="s in spots.slice().sort((a, b) => b.id - a.id)" :key="s.id">
                    <div class="card-body">
                      <div class="row g-3 d-flex align-item-center">
                        <div class="col-md-6">
                          <h5 class="mb-1 text-primary" style="font-weight: bold;">{{ s.title }}</h5>
                        </div>
                        <div class="col-md-6 d-flex">
                          <div class="link-primary lnk mb-1 ms-auto me-2" style="cursor: pointer;"
                            @click.stop.prevent="openMap(s.title, s.url)">
                            <i class="bi bi-geo-alt-fill me-2 text-danger">
                            </i>
                            <span style="font-size: 14px;">查看 Google Map</span>
                          </div>
                        </div>
                        <div class="d-flex align-items-center mt-0" style="font-size: 14px;">
                          <div class="col-md-3 text-secondary m-0 p-0 mt-0">
                            <span>{{ s.oriName }}</span>
                          </div>
                          <div class="col-md-3 text-secondary m-0 p-0 d-flex align-items-center mt-0">
                            <i class="bi bi-globe-asia-australia secondary me-2"></i>
                            <span>{{ `${s.lat.toFixed(3)}N, ${s.lon.toFixed(3)}E` }}</span>
                          </div>
                          <div class="col-md-6 d-flex justify-content-end m-0 p-0 mt-0">
                            <button class="btn btn-danger"
                              style="font-size: 14px; padding: 2px 2px; line-height: 1.2;">刪除</button>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <!--  v-for end -->
                </div>
              </div>
            </div>

            <!-- 規劃 -->
            <div id="print-section">
              <div v-if="activeTab === 'activity'" class="content-section">
                <div class="row">
                  <div class="section-header position-relative">
                    <h4>規劃每天行程</h4>
                    <button class="btn rounded-0 border-0 bg-transparent border-bottom border-dark no-hover new-partner"
                      style="position: absolute; bottom: 10px; right: 0px; z-index: 10" @click="
                        exportAllDaysToPDF">
                      <i class=" bi bi-file-earmark-arrow-down me-1"></i>匯出至 PDF
                    </button>
                  </div>

                  <!-- 日期選擇按鈕群 -->
                  <div class="mb-3">
                    <button v-for="(day, index) in days" :key="index" class="btn btn-outline-primary me-2"
                      :class="{ active: selectedDay === day }" @click="selectedDay = day">
                      {{ day }}
                    </button>
                  </div>

                  <!-- 每天對應的時間軸行程表 -->
                  <div class="border p-3">
                    <h5>{{ selectedDay }} 的行程</h5>
                    <div class="time-table position-relative" ref="timeTableContainer"
                      style="border:1px solid #ccc; border-radius:0;">
                      <div v-for="h in 24" :key="h" class="d-flex" style="border-bottom:1px solid #ccc;">
                        <!-- 左側時間欄 -->
                        <div class="time-label text-muted d-flex align-items-center justify-content-center"
                          style="width:60px; height:61px; font-size:14px; background:white; border-right:1px solid #ccc;">
                          {{ h - 1 }}:00
                        </div>

                        <!-- 右側時間區 -->
                        <div class="flex-grow-1 d-flex flex-column">
                          <!-- 前半小時 -->
                          <div class="time-slot position-relative" style="height:30px; background:#f6f6f6;"
                            @dragover.prevent @drop="handleTimeSlotDrop(h - 1, 0, $event)">
                            <template v-for="(item, idx) in getItemsAtSlot(h - 1, 0)" :key="idx">
                              <div class="position-absolute text-white rounded px-2 d-flex align-items-start opacity-88"
                                :class="item.color" :style="{
                                  top: '0',
                                  height: getCardHeight(item) + 'px',
                                  left: '0',
                                  right: '0',
                                  cursor: draggingResizeItem === item ? 'ns-resize' : 'grab',
                                  userSelect: 'none'
                                }" @mousedown.prevent="onCardMouseDown(item, $event)">
                                <span class="ms-2 mt-3 fw-bold">
                                  {{ item.title }}
                                </span>

                                <button class="me-2 mt-2 btn-close btn-close-white position-absolute"
                                  style="top: 6px; right: 6px; width: 10px; height: 10px;"
                                  @click.stop="!isDraggingOrResizing && removeScheduleItem(item)" title="移除行程"></button>

                                <div class="resize-handle" @mousedown.stop.prevent="onResizeStart(item, $event)">
                                </div>
                              </div>
                            </template>
                          </div>

                          <!-- 後半小時 -->
                          <div class="time-slot position-relative" style="height:30px;" @dragover.prevent
                            @drop="handleTimeSlotDrop(h - 1, 30, $event)">
                            <template v-for="(item, idx) in getItemsAtSlot(h - 1, 30)" :key="idx">
                              <div class="position-absolute text-white rounded px-2 d-flex align-items-start opacity-88"
                                :class="item.color" :style="{
                                  top: '0',
                                  height: getCardHeight(item) + 'px',
                                  left: '0',
                                  right: '0',
                                  cursor: draggingResizeItem === item ? 'ns-resize' : 'grab',
                                  userSelect: 'none'
                                }" @mousedown.prevent="onCardMouseDown(item, $event)">
                                <span class="mt-3 ms-2 fw-bold">
                                  {{ item.title }}
                                </span>

                                <button class="me-2 mt-2 btn-close btn-close-white position-absolute"
                                  style="top: 6px; right: 6px; width: 10px; height: 10px;"
                                  @click.stop="!isDraggingOrResizing && removeScheduleItem(item)" title="移除行程"></button>

                                <div class="resize-handle" @mousedown.stop.prevent="onResizeStart(item, $event)">
                                </div>
                              </div>
                            </template>
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

  <!-- Modal 元件 -->
  <div class="modal fade" id="myModal" tabindex="-1" aria-labelledby="myModalLabel" aria-hidden="true" ref="modalRef">
    <div class="modal-dialog modal-dialog-centered" style="max-width: 60%;">
      <div class="modal-content">
        <div class="modal-header d-flex justify-content-center">
          <div class="modal-title fs-3 ms-2" id="myModalLabel">{{ modalData.title }}</div>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body d-flex justify-content-center">
          <iframe class="w-100" height="600" style="border:0" loading="lazy" allowfullscreen
            referrerpolicy="no-referrer-when-downgrade" :src="modalData.url">
          </iframe>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">關閉</button>
        </div>
      </div>
    </div>
  </div>

  <!-- 搜尋 Modal 元件 -->
  <div class="modal fade" id="mapSearch" tabindex="-1" aria-labelledby="myModalLabel" aria-hidden="true"
    ref="mapSearchRef">
    <div class="modal-dialog modal-dialog-centered" style="max-width: 60%;">
      <div class="modal-content">
        <div class="modal-header d-flex justify-content-center">
          <div class="modal-title fs-3 ms-2">景點搜尋</div>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body d-flex ">
          <input v-if="!embedUrl" id="autocomplete" type="text" class="form-control w-75 mb-3 mt-3"
            placeholder="請輸入地標或地址" v-model="inputValue" />

          <!-- 顯示嵌入式網址 -->
          <div v-if="embedUrl" class="w-100 text-center">
            <p class="mb-2">已找到地點：{{ placeName }}</p>

            <!-- 地圖預覽 -->
            <iframe class="w-100" height="600" style="border:0" loading="lazy" allowfullscreen
              referrerpolicy="no-referrer-when-downgrade" :src="embedUrl"></iframe>
          </div>


        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-primary"
            @click="addToList(placeName, placeOriName, placeLat, placeLon, embedUrl)">加入景點</button>
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">關閉</button>
        </div>
      </div>
    </div>
  </div>

  <!-- 匯出用專用區塊 -->
  <div v-if="printMode" id="print-area">
    <div class="mt-2 mb-4" style="font-size: 24px;">{{ title }}</div>
    <div v-for="day in days" :key="day" class="mb-3">
      <h5>{{ day }} 的行程</h5>
      <div class="border p-2">
        <div v-for="item in schedule[day].slice().sort(sortByStartTime)" :key="item.title" class="mb-1">
          {{ formatTimeRange(item) }} — {{ item.title }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted, watch, onBeforeUnmount, nextTick, reactive } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import Header from '../Header.vue';
  import { usePlansStore } from '@/stores/plans';
  import { useActivityStore } from '@/stores/activity';
  import { Modal } from 'bootstrap';
  import spotMap from '@/assets/spotMap.json';
  import axios from 'axios';
  import Swal from 'sweetalert2';

  const embedUrl = ref('')
  const placeName = ref('')
  const props = defineProps({
    id: Number,
  })

  const activityStore = useActivityStore();
  const planStore = usePlansStore();
  const router = useRouter();
  const route = useRoute();
  const activeTab = ref('orders');
  const order = ref({});
  const plans = computed(() => planStore.plans);
  const isModalOpen = ref(false);
  const modalRef = ref(null);
  const mapSearchRef = ref(null);

  const modalData = ref({
    title: '',
    // lat: null,
    // lon: null,
    url: '',
  });
  let modalInstance = null;
  let mapSearchInstance = null
  // 導航選單
  const navItems = ref([
    { id: 'orders', title: '詳細資訊', icon: 'bi bi-receipt' },
    { id: 'activity', title: '行程規劃', icon: 'bi bi-geo-alt' },
  ]);

  const goBackToPlan = () => {
    router.push({
      path: '/profile',
      query: { activeTab: 'journey' }
    });
  }


  const title = computed(() => {
    return planStore.plans.find(plan => plan.id === Number(props.id)).title;
  })

  const activities = computed(() => {
    return activityStore.act.filter(a => a.tripId === Number(props.id));
  })

  const flights = computed(() => {
    return activities.value.
      filter(a => a.orderType === 'flight')
      .sort((a, b) => a.departureTime - b.departureTime);
  })

  const hotel = computed(() => {
    return activities.value.filter(a => a.orderType === 'hotel');
  })

  const tickets = computed(() => {
    return activities.value.filter(a => a.orderType === 'ticket');
  })

  const spots = computed(() => {
    return activities.value
      .filter(a => a.orderType === 'spot')
      .map((a, index) => ({
        ...a,
        id: index,
      }));
  })

  const days = ref(['Day 1', 'Day 2', 'Day 3', 'Day 4', 'Day 5'])
  const selectedDay = ref('Day 1')

  const schedule = ref({
    'Day 1': [],
    'Day 2': [],
    'Day 3': [],
    'Day 4': [],
    'Day 5': [],
  })

  let lastColor = null;
  const colors = ['bg-primary', 'bg-success', 'bg-warning', 'bg-danger', 'bg-dark'];

  // --------------------------- //
  const day0 = new Date('2025-08-10');
  const places = ref([]);

  function getTimeStr(datetime) {
    if (typeof datetime !== 'string' || !datetime.includes('T')) return '';
    const parts = datetime.split('T');
    return parts[1]?.substring(0, 5) || '';
  }

  // localStorage.setItem(['travelSchedule'], '');
  watch(activities, () => {
    places.value = [];
    activities.value.forEach(a => {
      if (a.orderType === 'flight') {
        const f = flights.value.find(f => f.flightNumber === a.flightNumber);
        if (!f || !f.departureTime || !f.arrivalTime) {
          console.warn('找不到對應的 flight 或資料不完整：', a);
          return; // ⚠️ 跳過這筆，避免出錯
        }

        const [date, time] = f.departureTime.split('T');
        if (!date || !time) {
          console.warn('departureTime 格式錯誤，無法 split：', f.departureTime);
          return;
        }

        console.log(date, time);

        const hour = time.substring(0, 2);
        const min = time.substring(3, 5);
        const dur = (new Date(f.arrivalTime) - new Date(f.departureTime)) / 1000 / 60;
        const dayIndex = (new Date(date) - day0) / 1000 / 86400;
        places.value.push({
          title: dayIndex === 0 ? `去程航班 ${f.flightNumber}` : `回程航班 ${f.flightNumber}`,
          desc: `${f.departureNameZh}(${f.departureAirport}) -> ${f.arrivalNameZh}(${f.arrivalAirport})`,
          schedule: {
            day: days.value[dayIndex],
            startHour: Number(hour),
            startMinute: Number(min),
            duration: Number(dur),
          }
        })
      } else if (a.orderType === 'hotel') {
        const h = hotel.value.find(h => h.name === a.name);
        const checkInIndex = (new Date(h.checkIn) - day0) / 1000 / 86400;
        const checkOutIndex = (new Date(h.checkOut) - day0) / 1000 / 86400;
        // checkin :00
        places.value.push({
          title: ` Check-In ${h.name}`,
          desc: h.room,
          schedule: {
            day: days.value[checkInIndex],
            startHour: 15,
            startMinute: 0,
            duration: 60,
          }
        })

        // checkout 11:00
        places.value.push({
          title: `Check-Out ${h.name}`,
          desc: h.room,
          schedule: {
            day: days.value[checkOutIndex],
            startHour: 11,
            startMinute: 0,
            duration: 60,
          }
        })
      } else if (a.orderType === 'ticket') {
        const t = tickets.value.find(t => t.title === a.title);
        const dayIndex = (new Date(t.date) - day0) / 86400 / 1000;
        places.value.push({
          title: '東京迪士尼',
          desc: t.title,
          schedule: {
            day: days.value[dayIndex],
            startHour: 9,
            startMinute: 0,
            duration: 540,
          }
        })
      } else {
        places.value.push({
          title: a.title,
          desc: a.oriName,
          schedule: null,
        })
      }
    })
    initScheduleFromPlaces();
  }, { deep: true });

  watch(places, () => {
    places.value.forEach((p, index) => {
      p.id = index;
    })
  }, { deep: true })


  function initScheduleFromPlaces() {
    // 先清空 schedule，避免重複
    for (const day of days.value) {
      schedule.value[day] = [];
    }
    places.value.forEach(place => {
      if (place.schedule) {
        const { day, startHour, startMinute, duration } = place.schedule;
        if (schedule.value[day]) {
          schedule.value[day].push({
            title: place.title,
            startHour,
            startMinute,
            duration,
            color: getRandomColor()  // 你自己的顏色函式
          });
        }
      }
    });
  }


  initScheduleFromPlaces();

  function onDragStart(title, event) {
    event.dataTransfer.setData('text/plain', title)
  }

  function isOverlap(day, startMin, endMin, excludeItem = null) {
    const items = schedule.value[day];
    for (const item of items) {
      if (excludeItem && item === excludeItem) continue;
      const itemStart = item.startHour * 60 + item.startMinute;
      const itemEnd = itemStart + item.duration;
      // 判斷時間區間是否重疊
      if (!(endMin <= itemStart || startMin >= itemEnd)) {
        return true; // 有重疊
      }
    }
    return false;
  }

  function handleTimeSlotDrop(hour, minute, event) {
    const title = event.dataTransfer.getData('text/plain');
    if (!title) return;

    // 預設 duration
    const duration = 60;
    const startMin = hour * 60 + minute;
    const endMin = startMin + duration;

    // 檢查重疊
    if (isOverlap(selectedDay.value, startMin, endMin)) {
      alert('該時段已被佔用，請選擇其他時間');
      return;
    }

    // 移除舊的行程
    for (const day in schedule.value) {
      schedule.value[day] = schedule.value[day].filter(item => item.title !== title);
    }

    // 新增
    schedule.value[selectedDay.value].push({
      title,
      startHour: hour,
      startMinute: minute,
      duration,
      color: getRandomColor()
    });

    // 同步更新 place
    const place = places.value.find(p => p.title === title);
    if (place) {
      place.schedule = {
        day: selectedDay.value,
        startHour: hour,
        startMinute: minute,
        duration,
      };
    }
  }

  function getItemsAtSlot(hour, minute) {
    return schedule.value[selectedDay.value].filter(item => item.startHour === hour && item.startMinute === minute)
  }

  // 拖曳改時間相關
  const draggingItem = ref(null)
  const dragStartY = ref(0)
  const initialTop = ref(0)

  const isDraggingOrResizing = ref(false);

  function onCardMouseDown(item, event) {
    isDraggingOrResizing.value = true;
    draggingItem.value = item;
    dragStartY.value = event.clientY;
    initialTop.value = ((item.startHour * 60 + item.startMinute) / 60) * 50;
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
  }

  // 拖曳改時間時檢查
  function onMouseMove(event) {
    if (!draggingItem.value) return;
    const deltaY = event.clientY - dragStartY.value;
    let newTop = initialTop.value + deltaY;
    if (newTop < 0) newTop = 0;
    if (newTop > 24 * 50) newTop = 24 * 50;
    newTop = Math.round(newTop / 25) * 25;
    const totalMinutes = (newTop / 25) * 30;

    // 檢查是否重疊
    const newStartHour = Math.floor(totalMinutes / 60);
    const newStartMinute = totalMinutes % 60;
    const itemDuration = draggingItem.value.duration;
    if (isOverlap(selectedDay.value, totalMinutes, totalMinutes + itemDuration, draggingItem.value)) {
      // 不更新位置，維持原本位置（也可做其他提示）
      return;
    }

    draggingItem.value.startHour = newStartHour;
    draggingItem.value.startMinute = newStartMinute;
    updatePlaceSchedule(draggingItem.value);
  }


  function onMouseUp() {
    isDraggingOrResizing.value = false;
    draggingItem.value = null;
    window.removeEventListener('mousemove', onMouseMove);
    window.removeEventListener('mouseup', onMouseUp);
  }


  // 拉伸調整時長相關
  const lastValidDuration = ref(0); // 新增變數，記錄上次有效長度
  const draggingResizeItem = ref(null)
  const resizeStartY = ref(0)
  const initialHeight = ref(0)

  function onResizeStart(item, event) {
    isDraggingOrResizing.value = true;
    draggingResizeItem.value = item;
    resizeStartY.value = event.clientY;
    initialHeight.value = getCardHeight(item);
    lastValidDuration.value = item.duration; // 初始有效長度是目前長度
    window.addEventListener('mousemove', onResizeMove);
    window.addEventListener('mouseup', onResizeEnd);
  }

  // 拉伸改長度時檢查
  function onResizeMove(event) {
    if (!draggingResizeItem.value) return;
    const deltaY = event.clientY - resizeStartY.value;
    let newHeight = initialHeight.value + deltaY;

    // 限制最小高度（30 分鐘）
    if (newHeight < 25) newHeight = 25;
    // 限制最大高度（不能超過 24 小時）
    if (newHeight > 24 * 50) newHeight = 24 * 50;

    // 四捨五入到 25px 倍數
    newHeight = Math.round(newHeight / 25) * 25;

    const newDuration = (newHeight / 25) * 30;

    const item = draggingResizeItem.value;
    const startMin = item.startHour * 60 + item.startMinute;
    const endMin = startMin + newDuration;

    // 超過一天結束時間，限制在最大值
    const maxEnd = 24 * 60;
    if (endMin > maxEnd) {
      // 超過就用最大長度
      item.duration = maxEnd - startMin;
      lastValidDuration.value = item.duration;
      updatePlaceSchedule(item);
      return;
    }

    // 檢查是否重疊
    if (isOverlap(selectedDay.value, startMin, endMin, item)) {
      // 重疊就維持上一個有效長度，不更新
      item.duration = lastValidDuration.value;
      return;
    }

    // 不重疊，更新長度並更新 lastValidDuration
    item.duration = newDuration;
    lastValidDuration.value = newDuration;
    updatePlaceSchedule(item);
  }

  function onResizeEnd() {
    isDraggingOrResizing.value = false;
    draggingResizeItem.value = null;
    window.removeEventListener('mousemove', onResizeMove);
    window.removeEventListener('mouseup', onResizeEnd);
  }


  function formatTimeRange(sch) {
    if (!sch) return ''
    const pad = (n) => (n < 10 ? '0' + n : n)
    const startH = pad(sch.startHour)
    const startM = pad(sch.startMinute)
    let endTotalMins = sch.startHour * 60 + sch.startMinute + sch.duration
    const endH = pad(Math.floor(endTotalMins / 60))
    const endM = pad(endTotalMins % 60)
    return `${startH}:${startM} ~ ${endH}:${endM}`
  }

  function updatePlaceSchedule(item) {
    const place = places.value.find(p => p.title === item.title)
    if (!place) return
    place.schedule = {
      day: selectedDay.value,
      startHour: item.startHour,
      startMinute: item.startMinute,
      duration: item.duration,
    }
  }

  watch(
    schedule,
    (newVal) => {
      localStorage.setItem('travelSchedule', JSON.stringify(newVal))
    },
    { deep: true }
  )

  const savedSchedule = localStorage.getItem('travelSchedule');

  if (savedSchedule) {
    schedule.value = JSON.parse(savedSchedule);

    // 讀完後同步 places 中 schedule 狀態
    for (const day in schedule.value) {
      schedule.value[day].forEach((item) => {
        const place = places.value.find((p) => p.title === item.title);
        if (place) {
          place.schedule = {
            day,
            startHour: item.startHour,
            startMinute: item.startMinute,
            duration: item.duration,
          };
        }
      });
    }
  } else {
    // localStorage 沒資料，從 places 初始化 schedule
    // 初始化 schedule
    for (const place of places.value) {
      if (place.schedule) {
        const day = place.schedule.day;
        if (!schedule.value[day].some(item => item.title === place.title)) {
          schedule.value[day].push({
            title: place.title,
            startHour: place.schedule.startHour,
            startMinute: place.schedule.startMinute,
            duration: place.schedule.duration,
            color: place.color || getRandomColor(),
          });
        }
      }
    }
  }

  function getCardHeight(item) {
    // 每 30 分鐘高 30px
    const pxPer30Min = 30

    // 當天結束時間總分鐘
    const dayEndInMinutes = 24 * 60

    // 起始時間總分鐘
    const startInMinutes = item.startHour * 60 + item.startMinute

    // 剩餘時間（分鐘）
    const remaining = dayEndInMinutes - startInMinutes

    // 有效 duration 不可超過剩餘時間
    const effectiveDuration = Math.min(item.duration, remaining)

    // 高度計算：30px / 30分鐘 * duration
    return (effectiveDuration / 30) * pxPer30Min
  }


  function getRandomColor() {
    let idx;
    do {
      idx = Math.floor(Math.random() * colors.length);
    } while (colors[idx] === lastColor);
    lastColor = colors[idx];
    return lastColor;
  }

  function removeScheduleItem(item) {
    // 從 schedule 中移除該 item
    for (const day in schedule.value) {
      schedule.value[day] = schedule.value[day].filter(i => i !== item);
    }
    // 同步更新 places 裡的 schedule 為 null
    const place = places.value.find(p => p.title === item.title);
    if (place) {
      place.schedule = null;
    }
  }

  const timeTableContainer = ref(null);

  function scrollToSchedule(item) {
    if (selectedDay.value === item.day) {
      // 如果是同一天就直接滾動
      scrollToTime(item.startHour, item.startMinute);
    } else {
      // 切換天數，等畫面更新後再滾動
      selectedDay.value = item.day;
      nextTick(() => {
        // 再等一下 DOM 確保已渲染
        setTimeout(() => {
          scrollToTime(item.startHour, item.startMinute);
        }, 50);
      });
    }
  }
  function scrollToTime(hour, minute) {
    const pxPer30Min = 30;
    const totalMinutes = hour * 60 + minute;
    const scrollY = (totalMinutes / 30) * pxPer30Min;

    window.scrollTo({
      top: scrollY,
      behavior: 'smooth',
    });
  }

  const printMode = ref(false)
  function exportAllDaysToPDF() {
    printMode.value = true
    nextTick(() => {
      const element = document.getElementById('print-area')
      import('html2pdf.js').then(html2pdf => {
        html2pdf.default()
          .from(element)
          .set({
            margin: 10,
            filename: `${title.value}_行程表.pdf`,
            html2canvas: { scale: 2 },
            jsPDF: { unit: 'mm', format: 'a4' }
          })
          .save()
          .then(() => {
            printMode.value = false
          })
      })
    })
  }

  function printSchedule() {
    window.print();
  }

  function sortByStartTime(a, b) {
    const aStart = a.startHour * 60 + a.startMinute;
    const bStart = b.startHour * 60 + b.startMinute;
    return aStart - bStart;
  }
  // --------------------------- //


  const goToHotelPage = (index) => {
    const path = '/hotel/' + hotel.value[index].hotelId + '/room/' + hotel.value[index].roomId;
    window.open(path, '_blank');
  }

  // 寫死迪士尼 因為沒抓到後端 tickeId
  const goToTicketPage = () => {
    const path = '/ticket/5';
    window.open(path, '_blank');
  }

  const openMap = (title, url) => {
    if (modalInstance) {
      modalData.value = {
        title: title,
        // lat: lat,
        // lon: lon,
        url: url,
      }
      modalInstance?.show();
    }
    console.log(name);
  }

  let modalEl = null;

  const openSearch = () => {
    if (mapSearchInstance) {
      mapSearchInstance?.show();
    }
  }
  // const apiKey = 'google api' // 🔁 換成你的金鑰
  const fetchPlaceDetails = async (placeId) => {
    const response = await axios.get('/api/place/details', {
      params: { placeId, language: 'ja' }
    });
    return response.data.result.name; // JSON 字串，你可用 JSON.parse 處理
  };


  const inputValue = ref('');
  const placeOriName = ref('');
  const placeLat = ref(0);
  const placeLon = ref(0);

  onMounted(() => {
    window.flights = flights.value

    if (modalRef.value) {
      modalInstance = new Modal(modalRef.value)
    }
    modalRef.value.addEventListener('hide.bs.modal', afterClose);

    if (mapSearchRef.value) {
      mapSearchInstance = new Modal(mapSearchRef.value);
    }
    mapSearchRef.value.addEventListener('hide.bs.modal', afterClose);

    modalEl = document.getElementById('mapSearch')
    modalEl.addEventListener('hidden.bs.modal', () => {
      embedUrl.value = ''
      placeName.value = ''
      // 也可以清空 input 框
      const input = document.getElementById('autocomplete')
      if (input) input.value = ''
    })

    console.log(activities.value);
    const input = document.getElementById('autocomplete')
    const autocomplete = new google.maps.places.Autocomplete(input, {
      types: ['establishment', 'geocode'],
      fields: ['geometry', 'name', 'place_id', 'formatted_address']
    })

    autocomplete.addListener('place_changed', async () => {
      const place = autocomplete.getPlace();
      if (!place.name && !place.formatted_address) return;

      const result = await fetchPlaceDetails(place.place_id, 'ja');

      // 這裡用 name 或 address 都可以
      placeLat.value = place.geometry.location.lat();
      placeLon.value = place.geometry.location.lng();
      placeOriName.value = result;
      placeName.value = place.name;
      const query = place.name || place.formatted_address;
      embedUrl.value = `https://www.google.com/maps/embed/v1/place?key=
    ${apiKey}&q=${encodeURIComponent(query)}`;
    })
    console.log("activityStore.act", activityStore.act);
  })

  onBeforeUnmount(() => {
    modalInstance?.dispose();
    modalRef.value.removeEventListener('hide.bs.modal', afterClose);
  });

  const afterClose = () => {
    if (modalRef.value && modalRef.value.contains(document.activeElement)) {
      if (document.activeElement instanceof HTMLElement) {
        document.activeElement.blur();
      }
    }
    if (mapSearchRef.value && mapSearchRef.value.contains(document.activeElement)) {
      if (document.activeElement instanceof HTMLElement) {
        document.activeElement.blur();
      }
    }
  }

  // 景點假資料
  class mySpot {
    constructor(title, oriName, lat, lon, url) {
      this.title = title;
      this.oriName = oriName;
      this.lat = lat;
      this.lon = lon;
      this.url = url;
    }
  }

  const fakeData = [
    ['明治神宮', '明治神宮', 35.676398, 139.699326],
    ['淺草寺', '浅草寺', 35.714765, 139.796655],
    ['東京都水科學館', '東京都水の科学館', 35.630844, 139.785483],
    ['彩虹大橋', 'レインボーブリッジ', 35.636564, 139.763144],
    ['澀谷', '渋谷スクランブル交差点', 35.659482, 139.70056],
    ['阿美橫丁', '上野アメ横商店街', 35.708978, 139.774716],
    ['Diver City東京廣場', 'ダイバーシティ東京 プラザ', 35.625317, 139.775638],
    ['自由女神像', '自由の女神像', 35.627873, 139.771835],
    ['台場海灘', 'おだいばビーチ', 35.631756, 139.777418],
    ['新宿御苑', '新宿御苑', 35.685176, 139.710052],
    ['表參道', '表参道ヒルズ', 35.667287, 139.708616],
    ['谷中銀座', '谷中ぎんざ商店街', 35.727693, 139.765723],
    ['豐洲市場', '豊洲市場', 35.644091, 139.784281],
    ['上野恩賜公園', '上野恩賜公園', 35.714756, 139.773431],
    // ['東京國立博物館', '東京国立博物館', 35.718835,139.776522],
  ]

  activityStore.clean('spot', 1);
  for (let fd in fakeData) {
    activityStore.addSpot(new mySpot(...fakeData[fd], spotMap[fd].url), 'spot', 1);
  }
  activityStore.$subscribe((mutations, state) => {
    console.log('activtyStore 更新:', state);
  })
  // Odaiba Beach DiverCity Tokyo レインボーブリッジ
  // 新宿御苑 明治神宮 原宿 表參道 shibuya
  // 淺草寺 谷中銀座 上野公園 東京國立博物館 阿美橫町
  // 豊洲市場 東京都水的科學館 自由女神像

  const addToList = (title, oriName, lat, lon, url) => {
    const l = spots.value.length;
    activityStore.act = [...activityStore.act, {
      tripId: 1,
      orderType: 'spot',
      title: title,
      oriName: oriName,
      lat: lat,
      lon: lon,
      url: url,
    }];

    if (mapSearchInstance) {
      mapSearchInstance?.hide();
      afterClose();
    }
    Swal.fire({
      icon: 'success',
      showConfirmButton: true,
      title: '成功加入景點！',
      text: title
    })
    console.log([...activities.value, {
      tripId: 1,
      orderType: 'spot',
      title: title,
      oriName: oriName,
      lat: lat,
      lon: lon,
      url: url,
    }]);
    console.log(spots.value);
    console.log(activities.value);
  }

</script>

<style scoped>
  ::v-deep .pac-container {
    z-index: 2100 !important;
  }

  @media print {
    body * {
      visibility: hidden;
    }

    /* 讓列印時所有 flex 或 grid 佈局都還原成 block */
    * {
      all: unset !important;
      display: block !important;
      width: auto !important;
      height: auto !important;
      margin: 0 !important;
      padding: 0 !important;
      overflow: visible !important;
    }

    /* 讓列印的行程表可見 */
    #print-section,
    #print-section * {
      all: unset !important;
      display: block !important;
      visibility: visible !important;
    }

    /* 把行程表放在頁面頂端 */
    #print-section {
      position: absolute;
      left: 0;
      top: 0;
      width: 100%;
    }
  }

  .card[draggable="true"] {
    cursor: grab;
  }

  .card[draggable="true"]:active {
    cursor: grabbing;
  }

  .card[draggable="true"] {
    user-select: none;
  }

  .time-label {
    width: 60px;
    color: #666;
    line-height: 25px;
    /* 單格高度 */
    padding-top: 10px;
    /* 垂直置中 */
    position: relative;
    z-index: 1;
    /* 比行程區低 */
  }

  .time-slot {
    position: relative;
  }


  .position-relative {
    position: relative;
  }

  .position-absolute {
    position: absolute;
    z-index: 10;
  }

  .resize-handle {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    height: 6px;
    cursor: ns-resize;
    background: rgba(255, 255, 255, 0.7);
    border-top: 1px solid rgba(0, 0, 0, 0.2);
    border-radius: 0 0 6px 6px;
  }

  /* ------------------- */
  .profile-sidebar {
    position: sticky;
    top: 2rem;
  }

  /* 導航選單 */
  .nav-menu {
    background: white;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  .nav-item {
    display: flex;
    align-items: center;
    padding: 1rem 1.5rem;
    cursor: pointer;
    transition: all 0.2s ease;
    border-bottom: 1px solid #f8f9fa;
    position: relative;
  }

  .nav-item:last-child {
    border-bottom: none;
  }

  .nav-item:hover {
    background-color: #f8f9fa;
  }

  .nav-item.active {
    background-color: #e3f2fd;
    color: #007bff;
    border-left: 4px solid #007bff;
  }

  .nav-item i {
    margin-right: 0.75rem;
    font-size: 1.1rem;
  }

  .nav-item .badge {
    margin-left: auto;
    background-color: #dc3545;
    color: white;
    font-size: 0.75rem;
    padding: 0.25rem 0.5rem;
    border-radius: 10px;
  }

  /* 內容區域 */
  .profile-content {
    background: white;
    border-radius: 12px;
    padding: 2rem;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  .section-header {
    margin-bottom: 2rem;
    padding-bottom: 1rem;
    border-bottom: 1px solid #e9ecef;
  }

  .order-card {
    transition: transform 0.2s;
  }

  .order-card:hover {
    transform: translateY(-2px);
  }

  .order-detail {
    animation: fadeIn 0.3s ease-in-out;
  }

  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(-10px);
    }

    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  .table th {
    background: #f8f9fa;
  }

  .lnk:hover {
    text-decoration: underline;
  }

  .no-hover:hover,
  .no-hover:focus,
  .no-hover:active {
    box-shadow: none !important;
    transform: none !important;
  }

  .no-hover:hover {
    background-color: #b9b9b9 !important;
    border-color: #b9b9b9 !important;
    color: white;
  }

  .new-partner {
    padding: 0px 3px;
  }

  @media (max-width: 768px) {
    .btn-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .btn-group .btn {
      width: 100%;
    }
  }
</style>
