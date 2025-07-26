import { computed } from 'vue'
import { jwtDecode } from 'jwt-decode'

export function useCurrentUser() {
  // 取得 localStorage 的用戶資訊
  const getCurrentUser = () => {
    // 統一處理多種儲存方式
    const userType = localStorage.getItem('userRole') || localStorage.getItem('userType')
    const userId = localStorage.getItem('userId')
    const userName = localStorage.getItem('userName')

    // 檢查是否為商家登入
    const isVendorLoggedIn = localStorage.getItem('isVendorLoggedIn') === 'true'
    const vendorId = localStorage.getItem('vendorId')
    const vendorType = localStorage.getItem('vendorType')
    const vendorName = localStorage.getItem('vendorName')

    const result = {
      userId: userId ? parseInt(userId) : (isVendorLoggedIn && vendorId ? parseInt(vendorId) : null),
      userType: userType ? userType.toUpperCase() : (isVendorLoggedIn ? 'VENDOR' : null),
      userName: userName || vendorName || '',
      // 商家相關資訊
      isVendorLoggedIn,
      vendorId: vendorId ? parseInt(vendorId) : null,
      vendorType,
      vendorName: vendorName || ''
    }

    // 除錯資訊
    console.log('🔍 useCurrentUser 除錯資訊:', {
      localStorage: {
        userRole: localStorage.getItem('userRole'),
        userType: localStorage.getItem('userType'),
        userId: localStorage.getItem('userId'),
        userName: localStorage.getItem('userName'),
        isVendorLoggedIn: localStorage.getItem('isVendorLoggedIn'),
        vendorId: localStorage.getItem('vendorId'),
        vendorType: localStorage.getItem('vendorType'),
        vendorName: localStorage.getItem('vendorName')
      },
      result: result
    })

    return result
  }

  // 取得用戶資訊的 computed
  const currentUser = computed(() => getCurrentUser())

  // 統一的身分判定 (支援大小寫)
  const isAdmin = computed(() => {
    const userType = currentUser.value.userType
    return userType === 'ADMIN' || userType === 'admin'
  })

  const isVendor = computed(() => {
    const userType = currentUser.value.userType
    // 檢查 userType 是否為 VENDOR 或 vendor，或者 isVendorLoggedIn 為 true
    return (userType === 'VENDOR' || userType === 'vendor' || currentUser.value.isVendorLoggedIn)
  })

  const isUser = computed(() => {
    const userType = currentUser.value.userType
    return userType === 'USER' || userType === 'user'
  })

  // 複合判斷
  const isAdminOrVendor = computed(() => isAdmin.value || isVendor.value)

  // 商家類型判斷
  const isHotelVendor = computed(() => currentUser.value.vendorType === 'hotel')
  const isFlightVendor = computed(() => currentUser.value.vendorType === 'flight')
  const isAttractionVendor = computed(() => currentUser.value.vendorType === 'attraction')
  const isTrafficVendor = computed(() => currentUser.value.vendorType === 'traffic')

  // 用戶頭像
  const userAvatar = computed(() => {
    try {
      const token = localStorage.getItem('token');

      if (token) {
        const payload = jwtDecode(token);
        const userId = payload.id || payload.userId;

        // 如果有 icon 欄位
        if (payload.icon) {
          // 判斷是否為完整 URL
          if (payload.icon.startsWith('http://') || payload.icon.startsWith('https://')) {
            // 如果是完整 URL，直接使用
            return payload.icon;
          } else {
            // 如果是相對路徑，加上後端 URL
            return `/${payload.icon}`;
          }
        }

        // 如果沒有 icon，根據用戶 ID 使用預設頭像
        if (userId !== 1) {
          return `https://i.pravatar.cc/150?u=${userId}`;
        } else {
          return "https://i.pravatar.cc/150?u=6"; // 管理員預設頭像
        }
      }

      // 如果沒有 token，返回預設頭像
      return 'https://cdn-icons-png.flaticon.com/512/149/149071.png';
    } catch (error) {
      console.error('Error parsing token for avatar:', error);
      return 'https://cdn-icons-png.flaticon.com/512/149/149071.png';
    }
  })

  // 統一的登出函數
  const logout = () => {
    // 清除所有用戶相關的 localStorage 項目
    localStorage.removeItem('userRole');
    localStorage.removeItem('userType');
    localStorage.removeItem('userName');
    localStorage.removeItem('userId');
    localStorage.removeItem('userAvatar');
    localStorage.removeItem('token');

    // 清除商家相關資訊
    localStorage.removeItem('vendorType');
    localStorage.removeItem('vendorId');
    localStorage.removeItem('isVendorLoggedIn');
    localStorage.removeItem('vendorName');

    // 清除用戶詳細資訊 (Checkout.vue 使用的 userInfo)
    localStorage.removeItem('userInfo');

    // 清除購物相關資訊
    localStorage.removeItem('favorites');
    localStorage.removeItem('journey');
    localStorage.removeItem('checkoutData');

    // 清除航班相關資訊
    localStorage.removeItem('checkoutFlight');
    localStorage.removeItem('roundTripCheckout');
    localStorage.removeItem('multiTripCheckout');
    localStorage.removeItem('flightSearchForm');

    // 清除其他可能遺漏的項目
    localStorage.removeItem('username'); // HotelVendor.vue 設置的
    // localStorage.removeItem('roomTypes'); // 房型相關
    localStorage.removeItem('bookingOrder'); // 訂房訂單
    localStorage.removeItem('recentViews'); // 最近瀏覽
    localStorage.removeItem('searchHistory'); // 搜尋歷史

    console.log('🧹 已清除所有用戶相關的 localStorage 項目');
  }

  // 檢查是否已登入
  const isLoggedIn = computed(() => {
    const token = localStorage.getItem('token');
    const userRole = localStorage.getItem('userRole');
    const userType = localStorage.getItem('userType');
    const isVendorLoggedIn = localStorage.getItem('isVendorLoggedIn');

    // 更嚴格的檢查：必須有 token 且至少有一種身分資訊
    const hasToken = !!token;
    const hasUserRole = !!(userRole || userType || isVendorLoggedIn === 'true');

    return hasToken && hasUserRole;
  })

  return {
    currentUser,
    isAdmin,
    isVendor,
    isUser,
    isAdminOrVendor,
    isVendorLoggedIn: currentUser.value.isVendorLoggedIn,
    isHotelVendor,
    isFlightVendor,
    isAttractionVendor,
    isTrafficVendor,
    userAvatar,
    logout,
    isLoggedIn
  }
} 