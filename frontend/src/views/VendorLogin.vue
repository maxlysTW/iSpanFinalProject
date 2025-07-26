<!--
  商家登入頁面
  
  @author Max
  @since 2025-07-08
-->
<template>
  <div class="vendor-login-container">
    <!-- 背景裝飾 -->
    <div class="background-decoration">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
    </div>

    <div class="login-wrapper">
      <div class="login-card">
        <!-- Logo 區域 -->
        <div class="logo-section">
          <div class="logo-icon">
            <i class="bi bi-shop"></i>
          </div>
          <h1 class="brand-title">商家管理中心</h1>
          <p class="brand-subtitle">歡迎回到您的商家後台</p>
        </div>

        <!-- 登入表單 -->
        <form @submit.prevent="handleLogin" class="login-form">
          <div class="form-group">
            <label for="username" class="form-label">
              <i class="bi bi-person me-2"></i>商家帳號
            </label>
            <input type="text" class="form-control" id="username" v-model="username" placeholder="請輸入您的商家帳號" required />
          </div>

          <div class="form-group">
            <label for="password" class="form-label"> <i class="bi bi-lock me-2"></i>密碼 </label>
            <div class="input-group">
              <input :type="showPassword ? 'text' : 'password'" v-model="password" class="form-control" id="password"
                placeholder="請輸入密碼" required />
              <span class="input-group-text" @click="showPassword = !showPassword" style="cursor: pointer">
                <i :class="showPassword ? 'bi bi-eye' : 'bi bi-eye-slash'"></i>
              </span>
            </div>
          </div>

          <button type="submit" class="btn btn-primary login-btn" :disabled="loading">
            <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
            <i v-else class="bi bi-box-arrow-in-right me-2"></i>
            {{ loading ? '登入中...' : '登入系統' }}
          </button>

          <div v-if="errorMsg" class="alert alert-danger mt-3">
            <i class="bi bi-exclamation-triangle me-2"></i>
            {{ errorMsg }}
          </div>
        </form>

        <!-- 返回首頁連結 -->
        <div class="back-to-home">
          <router-link to="/" class="back-link">
            <i class="bi bi-arrow-left me-2"></i>
            返回首頁
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';

const username = ref('');
const password = ref('');
const showPassword = ref(false);
const loading = ref(false);
const errorMsg = ref('');
const router = useRouter();

const handleLogin = async () => {
  loading.value = true;
  errorMsg.value = '';

  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: username.value,
        password: password.value
      })
    });

    if (response.ok) {
      const data = await response.json();
      // 檢查 userType
      if (data.userType === 'VENDOR') {
        // 儲存商家資訊到 localStorage
        localStorage.setItem('userType', 'VENDOR');
        localStorage.setItem('userId', data.userId);
        localStorage.setItem('userName', data.vendorName || data.username);
        localStorage.setItem('vendorType', data.username);
        localStorage.setItem('vendorId', data.userId);
        localStorage.setItem('isVendorLoggedIn', 'true');
        localStorage.setItem('token', data.token);
        localStorage.setItem('vendorName', data.vendorName || data.username);

        // 優先使用後端回傳的商家類型，如果沒有則根據商家名稱判斷
        let vendorType = data.vendorType || 'hotel'; // 預設為飯店

        // 如果後端沒有回傳 vendorType，則根據商家名稱判斷（暫時的解決方案）
        if (!data.vendorType && data.vendorName) {
          const vendorName = data.vendorName.toLowerCase();
          if (
            vendorName.includes('airline') ||
            vendorName.includes('flight') ||
            vendorName.includes('機票')
          ) {
            vendorType = 'flight';
          } else if (vendorName.includes('attraction') || vendorName.includes('景點')) {
            vendorType = 'attraction';
          } else if (vendorName.includes('traffic') || vendorName.includes('交通')) {
            vendorType = 'traffic';
          }
        }
        localStorage.setItem('vendorType', vendorType);

        console.log('🔐 商家登入成功，儲存用戶資訊:', {
          userType: 'VENDOR',
          userId: data.userId,
          userName: data.vendorName || data.username,
          vendorType: data.username,
          vendorId: data.userId
        });

        // 根據商家類型導向不同頁面
        router.push(`/vendor/${vendorType}`);
      } else {
        errorMsg.value = '此帳號不是商家身分';
      }
    } else {
      const errorData = await response.text();
      errorMsg.value = errorData || '帳號或密碼錯誤，請重新輸入';
    }
  } catch (error) {
    errorMsg.value = '登入失敗，請稍後再試';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.vendor-login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #0080ff 0%, #005bbd 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.background-decoration {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.floating-shape {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 80px;
  height: 80px;
  top: 20%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 120px;
  height: 120px;
  top: 60%;
  right: 15%;
  animation-delay: 2s;
}

.shape-3 {
  width: 60px;
  height: 60px;
  bottom: 20%;
  left: 20%;
  animation-delay: 4s;
}

@keyframes float {

  0%,
  100% {
    transform: translateY(0px) rotate(0deg);
  }

  50% {
    transform: translateY(-20px) rotate(180deg);
  }
}

.login-wrapper {
  width: 100%;
  max-width: 480px;
  padding: 20px;
  z-index: 1;
}

.login-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  padding: 40px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.logo-section {
  text-align: center;
  margin-bottom: 40px;
}

.logo-icon {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #0080ff, #005bbd);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 10px 20px rgba(0, 128, 255, 0.3);
}

.logo-icon i {
  font-size: 36px;
  color: white;
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  color: #2d3748;
  margin-bottom: 8px;
}

.brand-subtitle {
  color: #718096;
  font-size: 16px;
  margin: 0;
}

.login-form {
  margin-bottom: 30px;
}

.form-group {
  margin-bottom: 24px;
}

.form-label {
  font-weight: 600;
  color: #4a5568;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.form-control {
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px 16px;
  font-size: 16px;
  transition: all 0.3s ease;
  background: #f7fafc;
}

.form-control:focus {
  border-color: #0080ff;
  box-shadow: 0 0 0 3px rgba(0, 128, 255, 0.1);
  background: white;
}

.login-btn {
  width: 100%;
  padding: 16px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #0080ff, #005bbd);
  border: none;
  transition: all 0.3s ease;
  margin-top: 8px;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(0, 128, 255, 0.3);
}

.login-btn:disabled {
  transform: none;
  opacity: 0.7;
}

.demo-accounts {
  background: #f7fafc;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
}

.demo-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  color: #4a5568;
  font-weight: 600;
}

.account-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.account-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background: white;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.account-item:hover {
  border-color: #0080ff;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.account-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  color: white;
  font-size: 18px;
}

.account-icon.flight {
  background: linear-gradient(135deg, #0080ff, #005bbd);
}

.account-icon.hotel {
  background: linear-gradient(135deg, #0080ff, #005bbd);
}

.account-icon.traffic {
  background: linear-gradient(135deg, #0080ff, #005bbd);
}

.account-icon.attraction {
  background: linear-gradient(135deg, #0080ff, #005bbd);
}

.account-info {
  display: flex;
  flex-direction: column;
}

.account-type {
  font-size: 14px;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 2px;
}

.account-details {
  font-size: 12px;
  color: #718096;
}

.back-to-home {
  text-align: center;
}

.back-link {
  color: #0080ff;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.3s ease;
}

.back-link:hover {
  color: #005bbd;
}

.alert {
  border-radius: 12px;
  border: none;
  padding: 16px;
  display: flex;
  align-items: center;
}

@media (max-width: 768px) {
  .login-card {
    padding: 30px 20px;
  }

  .account-grid {
    grid-template-columns: 1fr;
  }

  .brand-title {
    font-size: 24px;
  }
}
</style>