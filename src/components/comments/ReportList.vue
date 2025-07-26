<!--
  檢舉列表管理組件
  
  @author Max
  @since 2025-07-11
-->
<template>
  <div class="report-list-container">
    <!-- 檢舉統計面板 -->
    <div class="row mb-4">
      <div class="col-md-3 mb-3">
        <div class="card bg-warning text-white">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h4 class="mb-0">{{ statistics.pending }}</h4>
                <small>待處理檢舉</small>
              </div>
              <i class="bi bi-exclamation-triangle fs-1 opacity-75"></i>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-3 mb-3">
        <div class="card bg-success text-white">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h4 class="mb-0">{{ statistics.resolved }}</h4>
                <small>已處理檢舉</small>
              </div>
              <i class="bi bi-check-circle fs-1 opacity-75"></i>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-3 mb-3">
        <div class="card bg-danger text-white">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h4 class="mb-0">{{ statistics.rejected }}</h4>
                <small>已駁回檢舉</small>
              </div>
              <i class="bi bi-x-circle fs-1 opacity-75"></i>
            </div>
          </div>
        </div>
      </div>
      <div class="col-md-3 mb-3">
        <div class="card bg-info text-white">
          <div class="card-body">
            <div class="d-flex justify-content-between">
              <div>
                <h4 class="mb-0">{{ statistics.total }}</h4>
                <small>總檢舉數</small>
              </div>
              <i class="bi bi-flag fs-1 opacity-75"></i>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 檢舉列表 -->
    <div class="card shadow-sm">
      <div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">
        <h5 class="mb-0">
          <i class="bi bi-flag me-2"></i>檢舉管理
        </h5>
        <div class="d-flex flex-column flex-md-row gap-2">
          <!-- 狀態篩選 -->
          <select v-model="filterStatus" class="form-select form-select-sm filter-select">
            <option value="">全部狀態</option>
            <option value="PENDING">待處理</option>
            <option value="RESOLVED">已處理</option>
            <option value="REJECTED">已駁回</option>
          </select>
          <!-- 重新整理按鈕 -->
          <button class="btn btn-outline-light btn-sm" @click="fetchReports" :disabled="loading">
            <i class="bi bi-arrow-clockwise" :class="{ 'spinning': loading }"></i>
          </button>
        </div>
      </div>
      <div class="card-body">
        <!-- 載入中 -->
        <div v-if="loading" class="text-center py-4">
          <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">載入中...</span>
          </div>
          <p class="mt-2 text-muted">正在載入檢舉資料...</p>
        </div>

        <!-- 檢舉列表 -->
        <div v-else-if="reports.length > 0">
          <div v-for="report in reports" :key="report.reportId" class="report-item mb-4 border-bottom pb-3">
            <!-- 檢舉標題列 -->
            <div class="d-flex flex-column flex-md-row justify-content-between align-items-start mb-2">
              <div class="d-flex align-items-center gap-2 mb-2 mb-md-0">
                <span class="badge" :class="getStatusBadgeClass(report.status)">
                  {{ report.statusLabel }}
                </span>
                <span class="text-muted small">
                  <i class="bi bi-clock me-1"></i>
                  {{ formatDate(report.createdAt) }}
                </span>
              </div>
              <div class="d-flex flex-wrap gap-1">
                <button v-if="report.status === 'PENDING'" class="btn btn-success btn-sm"
                  @click="processReport(report.reportId, 'RESOLVED')" :disabled="processing">
                  <i class="bi bi-check-lg me-1"></i>處理
                </button>
                <button v-if="report.status === 'PENDING'" class="btn btn-danger btn-sm"
                  @click="processReport(report.reportId, 'REJECTED')" :disabled="processing">
                  <i class="bi bi-x-lg me-1"></i>駁回
                </button>
                <button class="btn btn-outline-primary btn-sm" @click="viewReportDetail(report)">
                  <i class="bi bi-eye me-1"></i>詳情
                </button>
              </div>
            </div>

            <!-- 檢舉內容 -->
            <div class="row">
              <div class="col-lg-8 col-md-12 mb-3">
                <!-- 檢舉原因 -->
                <div class="mb-2">
                  <strong class="text-danger">檢舉原因：</strong>
                  <span class="badge bg-danger">{{ report.reasonText }}</span>
                </div>

                <!-- 被檢舉留言 -->
                <div class="reported-comment bg-light p-3 rounded">
                  <div class="d-flex flex-column flex-md-row justify-content-between align-items-start mb-2">
                    <div class="mb-2 mb-md-0">
                      <strong>{{ report.reportedComment.author?.username || '匿名用戶' }}</strong>
                      <span class="text-muted ms-2">評論於 {{ formatDate(report.reportedComment.createdAt) }}</span>
                    </div>
                    <div class="text-warning" v-if="report.reportedComment.rating != null">
                      <span v-for="n in report.reportedComment.rating" :key="n">★</span>
                      <span v-for="n in 5 - report.reportedComment.rating" :key="'empty' + n">☆</span>
                    </div>
                  </div>
                  <div class="comment-content">{{ report.reportedComment.content }}</div>

                  <!-- 評論圖片 -->
                  <div v-if="getActiveImages(report.reportedComment).length > 0" class="comment-images mt-3">
                    <div class="d-flex flex-wrap gap-2">
                      <img v-for="(img, idx) in getActiveImages(report.reportedComment)"
                        :key="`${report.reportId}-${img.imageId || img.id || idx}`" :src="getImageUrl(img)"
                        class="comment-image" @click="openImageModal(img)" @error="handleImageError"
                        :alt="`檢舉留言圖片 ${idx + 1}`" />
                    </div>
                  </div>
                </div>
              </div>

              <div class="col-lg-4 col-md-12">
                <!-- 檢舉者資訊 -->
                <div class="reporter-info">
                  <h6 class="text-primary mb-2">
                    <i class="bi bi-person-flag me-1"></i>檢舉者
                  </h6>
                  <div class="d-flex align-items-center mb-2">
                    <div class="reporter-avatar me-2">
                      <img :src="getUserAvatar(report.reporter)" :alt="report.reporter.username"
                        @error="handleAvatarError" />
                    </div>
                    <div>
                      <div class="fw-bold">{{ report.reporter.username }}</div>
                      <small class="text-muted">ID: {{ report.reporter.id }}</small>
                    </div>
                  </div>
                </div>

                <!-- 處理資訊 -->
                <div v-if="report.status !== 'PENDING'" class="processing-info mt-3">
                  <h6 class="text-success mb-2">
                    <i class="bi bi-check-circle me-1"></i>處理資訊
                  </h6>
                  <div class="small">
                    <div class="mb-1">
                      <strong>處理時間：</strong>
                      {{ formatDate(report.reviewedAt) }}
                    </div>
                    <div v-if="report.note" class="mb-1">
                      <strong>處理備註：</strong>
                      <div class="text-muted">{{ report.note }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 分頁 -->
          <div class="d-flex flex-column flex-md-row justify-content-between align-items-center mt-4">
            <div class="text-muted mb-3 mb-md-0">
              顯示第 {{ (currentPage * pageSize) + 1 }} - {{ Math.min((currentPage + 1) * pageSize, totalItems) }} 筆，
              共 {{ totalItems }} 筆檢舉
            </div>
            <nav>
              <ul class="pagination pagination-sm mb-0">
                <li class="page-item" :class="{ disabled: currentPage === 0 }">
                  <button class="page-link" @click="changePage(currentPage - 1)">上一頁</button>
                </li>
                <li v-for="page in visiblePages" :key="page" class="page-item"
                  :class="{ active: page === currentPage }">
                  <button class="page-link" @click="changePage(page)">{{ page + 1 }}</button>
                </li>
                <li class="page-item" :class="{ disabled: currentPage === totalPages - 1 }">
                  <button class="page-link" @click="changePage(currentPage + 1)">下一頁</button>
                </li>
              </ul>
            </nav>
          </div>
        </div>

        <!-- 無資料 -->
        <div v-else class="text-center py-5">
          <i class="bi bi-inbox fs-1 text-muted mb-3"></i>
          <h5 class="text-muted">暫無檢舉資料</h5>
          <p class="text-muted">目前沒有待處理的檢舉案件</p>
        </div>
      </div>
    </div>

    <!-- 檢舉詳情 Modal -->
    <div v-if="showDetailModal" class="modal fade show d-block" @click="closeDetailModal">
      <div class="modal-dialog modal-lg modal-dialog-centered" @click.stop>
        <div class="modal-content">
          <div class="modal-header bg-primary text-white">
            <h5 class="modal-title">
              <i class="bi bi-flag me-2"></i>檢舉詳情
            </h5>
            <button type="button" class="btn-close btn-close-white" @click="closeDetailModal"></button>
          </div>
          <div class="modal-body" v-if="selectedReport">
            <div class="row">
              <div class="col-lg-6 col-md-12 mb-3">
                <h6 class="text-primary">檢舉資訊</h6>
                <table class="table table-sm">
                  <tbody>
                    <tr>
                      <td><strong>檢舉 ID：</strong></td>
                      <td>{{ selectedReport.reportId }}</td>
                    </tr>
                    <tr>
                      <td><strong>檢舉原因：</strong></td>
                      <td>{{ selectedReport.reasonText }}</td>
                    </tr>
                    <tr>
                      <td><strong>檢舉時間：</strong></td>
                      <td>{{ formatDate(selectedReport.createdAt) }}</td>
                    </tr>
                    <tr>
                      <td><strong>檢舉狀態：</strong></td>
                      <td>
                        <span class="badge" :class="getStatusBadgeClass(selectedReport.status)">
                          {{ selectedReport.statusLabel }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <div class="col-lg-6 col-md-12">
                <h6 class="text-primary">檢舉者資訊</h6>
                <div class="d-flex align-items-center mb-3">
                  <div class="reporter-avatar me-3">
                    <img :src="getUserAvatar(selectedReport.reporter)" :alt="selectedReport.reporter.username"
                      @error="handleAvatarError" />
                  </div>
                  <div>
                    <div class="fw-bold">{{ selectedReport.reporter.username }}</div>
                    <small class="text-muted">用戶 ID: {{ selectedReport.reporter.id }}</small>
                  </div>
                </div>
              </div>
            </div>

            <hr>

            <h6 class="text-primary">被檢舉留言</h6>
            <div class="reported-comment bg-light p-3 rounded">
              <div class="d-flex justify-content-between align-items-start mb-2">
                <div>
                  <strong>{{ selectedReport.reportedComment.author?.username || '匿名用戶' }}</strong>
                  <span class="text-muted ms-2">{{ formatDate(selectedReport.reportedComment.createdAt) }}</span>
                </div>
                <div class="text-warning">
                  <span v-for="n in selectedReport.reportedComment.rating" :key="n">★</span>
                  <span v-for="n in 5 - selectedReport.reportedComment.rating" :key="'empty' + n">☆</span>
                </div>
              </div>
              <div class="comment-content">{{ selectedReport.reportedComment.content }}</div>

              <!-- 評論圖片 -->
              <div v-if="getActiveImages(selectedReport.reportedComment).length > 0" class="comment-images mt-3">
                <div class="d-flex flex-wrap gap-2">
                  <img v-for="(img, idx) in getActiveImages(selectedReport.reportedComment)"
                    :key="`modal-${selectedReport.reportId}-${img.imageId || img.id || idx}`" :src="getImageUrl(img)"
                    style="width: 80px; height: 80px; object-fit: cover; border-radius: 4px; cursor: pointer;"
                    @click="openImageModal(img)" @error="handleImageError" :alt="`檢舉留言圖片 ${idx + 1}`" />
                </div>
              </div>
            </div>

            <!-- 處理表單 -->
            <div v-if="selectedReport.status === 'PENDING'" class="mt-4">
              <h6 class="text-primary">處理檢舉</h6>
              <div class="mb-3">
                <label class="form-label">處理備註</label>
                <textarea v-model="processingNote" class="form-control" rows="3" placeholder="請輸入處理備註（選填）"></textarea>
              </div>
              <div class="d-flex flex-column flex-md-row gap-2">
                <button class="btn btn-success" @click="processReport(selectedReport.reportId, 'RESOLVED')"
                  :disabled="processing">
                  <i class="bi bi-check-lg me-1"></i>處理檢舉
                </button>
                <button class="btn btn-danger" @click="processReport(selectedReport.reportId, 'REJECTED')"
                  :disabled="processing">
                  <i class="bi bi-x-lg me-1"></i>駁回檢舉
                </button>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="closeDetailModal">關閉</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 圖片放大 Modal -->
    <div v-if="showImageModal" class="modal fade show d-block" @click="closeImageModal">
      <div class="modal-dialog modal-lg modal-dialog-centered" @click.stop>
        <div class="modal-content">
          <div class="modal-header bg-dark text-white">
            <h5 class="modal-title">
              <i class="bi bi-image me-2"></i>檢舉留言圖片
            </h5>
            <button type="button" class="btn-close btn-close-white" @click="closeImageModal"></button>
          </div>
          <div class="modal-body text-center" v-if="selectedImage">
            <img :src="getImageUrl(selectedImage)" class="img-fluid rounded" style="max-height: 70vh; max-width: 100%;"
              :alt="`檢舉留言圖片`" @error="handleImageError" />
            <div class="mt-3">
              <small class="text-muted">
                圖片 ID: {{ selectedImage.imageId || selectedImage.id }} |
                大小: {{ selectedImage.size ? `${Math.round(selectedImage.size / 1024)}KB` : '未知' }}
              </small>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" @click="closeImageModal">關閉</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, onMounted } from 'vue'
  import { apiClient } from '@/services/api'
  import Swal from 'sweetalert2'

  // 響應式資料
  const reports = ref([])
  const loading = ref(false)
  const processing = ref(false)
  const currentPage = ref(0)
  const pageSize = ref(20)
  const totalItems = ref(0)
  const filterStatus = ref('')
  const showDetailModal = ref(false)
  const selectedReport = ref(null)
  const processingNote = ref('')
  const showImageModal = ref(false)
  const selectedImage = ref(null)

  // 統計資料
  const statistics = ref({
    pending: 0,
    resolved: 0,
    rejected: 0,
    total: 0
  })

  // 計算屬性
  const totalPages = computed(() => Math.ceil(totalItems.value / pageSize.value))

  const visiblePages = computed(() => {
    const pages = []
    const start = Math.max(0, currentPage.value - 2)
    const end = Math.min(totalPages.value - 1, currentPage.value + 2)

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }
    return pages
  })

  // 方法
  const fetchReports = async () => {
    loading.value = true

    console.log(`📋 開始載入檢舉列表:`, {
      page: currentPage.value,
      size: pageSize.value,
      filterStatus: filterStatus.value,
      timestamp: new Date().toLocaleTimeString()
    })

    try {
      const params = {
        page: currentPage.value,
        size: pageSize.value
      }

      if (filterStatus.value) {
        params.status = filterStatus.value
      }

      console.log(`📤 發送檢舉列表請求:`, {
        url: '/api/comment-reports/admin',
        params,
        token: localStorage.getItem('token') ? '存在' : '不存在'
      })

      const response = await apiClient.get('/api/comment-reports/admin', {
        params,
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`
        }
      })

      reports.value = response.data.content || response.data
      totalItems.value = response.data.totalElements || response.data.length || 0

      console.log(`✅ 檢舉列表載入成功:`, {
        totalReports: reports.value.length,
        totalItems: totalItems.value,
        currentPage: currentPage.value,
        responseData: response.data,
        timestamp: new Date().toLocaleTimeString()
      })

      // 更新統計資料
      updateStatistics()
    } catch (error) {
      console.error('❌ 載入檢舉列表失敗:', {
        error: {
          status: error.response?.status,
          statusText: error.response?.statusText,
          message: error.message,
          data: error.response?.data
        },
        params: {
          page: currentPage.value,
          size: pageSize.value,
          filterStatus: filterStatus.value
        },
        timestamp: new Date().toLocaleTimeString()
      })

      Swal.fire({
        icon: 'error',
        title: '載入失敗',
        text: '無法載入檢舉列表，請稍後再試',
        confirmButtonText: '確定'
      })
    } finally {
      loading.value = false
      console.log(`🏁 檢舉列表載入完成:`, {
        loading: loading.value,
        timestamp: new Date().toLocaleTimeString()
      })
    }
  }

  const updateStatistics = async () => {
    try {
      // 這裡可以呼叫統計 API，或從現有資料計算
      const pendingCount = reports.value.filter(r => r.status === 'PENDING').length
      const resolvedCount = reports.value.filter(r => r.status === 'RESOLVED').length
      const rejectedCount = reports.value.filter(r => r.status === 'REJECTED').length

      statistics.value = {
        pending: pendingCount,
        resolved: resolvedCount,
        rejected: rejectedCount,
        total: totalItems.value
      }
    } catch (error) {
      console.error('更新統計資料失敗:', error)
    }
  }

  const processReport = async (reportId, status) => {
    processing.value = true

    console.log(`🔧 開始處理檢舉:`, {
      reportId,
      status,
      note: processingNote.value,
      timestamp: new Date().toLocaleTimeString()
    })

    try {
      const payload = {
        status: status,
        note: processingNote.value || null
      }

      console.log(`📤 發送處理請求:`, {
        url: `/api/comment-reports/admin/${reportId}`,
        method: 'PUT',
        payload,
        token: localStorage.getItem('token') ? '存在' : '不存在'
      })

      const response = await apiClient.put(`/api/comment-reports/admin/${reportId}`, payload, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`
        }
      })

      console.log(`✅ 檢舉處理成功:`, {
        reportId,
        status,
        response: response.data,
        timestamp: new Date().toLocaleTimeString()
      })

      Swal.fire({
        icon: 'success',
        title: '處理成功',
        text: `檢舉已${status === 'RESOLVED' ? '處理' : '駁回'}`,
        confirmButtonText: '確定'
      })

      // 重新載入資料
      console.log(`🔄 重新載入檢舉列表`)
      await fetchReports()
      closeDetailModal()
    } catch (error) {
      console.error('❌ 處理檢舉失敗:', {
        reportId,
        status,
        error: {
          status: error.response?.status,
          statusText: error.response?.statusText,
          message: error.message,
          data: error.response?.data
        },
        timestamp: new Date().toLocaleTimeString()
      })

      Swal.fire({
        icon: 'error',
        title: '處理失敗',
        text: error.response?.data?.message || '處理檢舉時發生錯誤',
        confirmButtonText: '確定'
      })
    } finally {
      processing.value = false
      console.log(`🏁 檢舉處理完成:`, {
        reportId,
        status,
        timestamp: new Date().toLocaleTimeString()
      })
    }
  }

  const viewReportDetail = (report) => {
    selectedReport.value = report
    processingNote.value = ''
    showDetailModal.value = true
  }

  const closeDetailModal = () => {
    showDetailModal.value = false
    selectedReport.value = null
    processingNote.value = ''
  }

  const openImageModal = (img) => {
    selectedImage.value = img
    showImageModal.value = true
  }

  const closeImageModal = () => {
    showImageModal.value = false
    selectedImage.value = null
  }

  const changePage = (page) => {
    if (page >= 0 && page < totalPages.value) {
      currentPage.value = page
      fetchReports()
    }
  }

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'PENDING':
        return 'bg-warning text-dark'
      case 'RESOLVED':
        return 'bg-success'
      case 'REJECTED':
        return 'bg-danger'
      default:
        return 'bg-secondary'
    }
  }

  const formatDate = (dateString) => {
    if (!dateString) return '未知時間'
    const date = new Date(dateString)
    return date.toLocaleString('zh-TW', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  // 取得活躍的圖片（過濾已刪除的圖片）
  const getActiveImages = (comment) => {
    if (!comment.images || !Array.isArray(comment.images)) {
      return []
    }

    // 過濾掉已刪除的圖片
    return comment.images.filter(img => {
      // 檢查 isActive 狀態
      if (img.isActive === false) {
        return false
      }

      // 檢查 isDeleted 標記
      if (img.isDeleted === true) {
        return false
      }

      // 檢查 deleteStatus（如果存在）
      if (img.deleteStatus === 0) {
        return false
      }

      return true
    })
  }

  // 取得圖片 URL
  const getImageUrl = (img) => {
    if (img.commentImageUrl) {
      // 如果是 blob URL，檢查是否有效
      if (img.commentImageUrl.startsWith('blob:')) {
        // 回傳預設圖片
        return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iODAiIHZpZXdCb3g9IjAgMCA4MCA4MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0yMCAyMEg2MFY2MEgyMFYyMFoiIGZpbGw9IiNEN0Q3RDciLz4KPHBhdGggZD0iTTI1IDI1SDU1VjU1SDI1VjI1WiIgZmlsbD0iI0Y1RjVGNSIvPgo8L3N2Zz4K'
      }

      const prefix = 'https://192.168.36.96:8080/uploads/';
      if (img.commentImageUrl.startsWith(prefix)) {
        img.commentImageUrl = img.commentImageUrl.replace(prefix, '/api/uploads/');
      }

      // ✅ 若為 /uploads/... → 加上 /api 前綴（避免遺漏）
      if (img.commentImageUrl.startsWith('/uploads/')) {
        img.commentImageUrl = '/api' + img.commentImageUrl;
      }
      return img.commentImageUrl
    } else if (img.imageUrl) {
      return img.imageUrl
    } else {
      console.error('❌ 找不到圖片 URL:', img)
      // 回傳預設圖片
      return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iODAiIHZpZXdCb3g9IjAgMCA4MCA4MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0yMCAyMEg2MFY2MEgyMFYyMFoiIGZpbGw9IiNEN0Q3RDciLz4KPHBhdGggZD0iTTI1IDI1SDU1VjU1SDI1VjI1WiIgZmlsbD0iI0Y1RjVGNSIvPgo8L3N2Zz4K'
    }
  }

  // 處理圖片載入錯誤
  const handleImageError = (event) => {
    const img = event.target
    console.error('🖼️ 圖片載入失敗:', {
      src: img.src,
      naturalWidth: img.naturalWidth,
      naturalHeight: img.naturalHeight
    })

    // 顯示預設圖片
    img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iODAiIHZpZXdCb3g9IjAgMCA4MCA4MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0yMCAyMEg2MFY2MEgyMFYyMFoiIGZpbGw9IiNEN0Q3RDciLz4KPHBhdGggZD0iTTI1IDI1SDU1VjU1SDI1VjI1WiIgZmlsbD0iI0Y1RjVGNSIvPgo8L3N2Zz4K'
  }

  // 處理頭像載入錯誤
  const handleAvatarError = (event) => {
    const img = event.target
    console.error('👤 頭像載入失敗:', {
      src: img.src,
      naturalWidth: img.naturalWidth,
      naturalHeight: img.naturalHeight
    })

    // 顯示預設頭像
    img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjAiIGN5PSIyMCIgcj0iMjAiIGZpbGw9IiM5QzlDOUMiLz4KPHBhdGggZD0iTTIwIDEwQzE2LjEzIDEwIDEzIDEzLjEzIDEzIDE3QzEzIDIwLjg3IDE2LjEzIDI0IDIwIDI0QzIzLjg3IDI0IDI3IDIwLjg3IDI3IDE3QzI3IDEzLjEzIDIzLjg3IDEwIDIwIDEwWiIgZmlsbD0iI0Y1RjVGNSIvPgo8cGF0aCBkPSJNMTAgMzBDMTAgMjUuNTggMTQuNTggMjEgMjAgMjFDMjUuNDIgMjEgMzAgMjUuNTggMzAgMzBIMFYzMFoiIGZpbGw9IiNGNUY1RjUiLz4KPC9zdmc+'
  }

  // 取得檢舉者頭像
  const getReporterAvatar = (reporter) => {
    // 如果有有效的頭像 URL，直接使用
    if (reporter.avatarUrl && reporter.avatarUrl !== null && reporter.avatarUrl !== '') {
      return reporter.avatarUrl
    }

    // 沒有頭像時，回傳預設灰色頭像
    return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjAiIGN5PSIyMCIgcj0iMjAiIGZpbGw9IiM5QzlDOUMiLz4KPHBhdGggZD0iTTIwIDEwQzE2LjEzIDEwIDEzIDEzLjEzIDEzIDE3QzEzIDIwLjg3IDE2LjEzIDI0IDIwIDI0QzIzLjg3IDI0IDI3IDIwLjg3IDI3IDE3QzI3IDEzLjEzIDIzLjg3IDEwIDIwIDEwWiIgZmlsbD0iI0Y1RjVGNSIvPgo8cGF0aCBkPSJNMTAgMzBDMTAgMjUuNTggMTQuNTggMjEgMjAgMjFDMjUuNDIgMjEgMzAgMjUuNTggMzAgMzBIMFYzMFoiIGZpbGw9IiNGNUY1RjUiLz4KPC9zdmc+'
  }

  // 取得用戶頭像（處理錯誤情況）
  const getUserAvatar = (user) => {
    try {
      return getReporterAvatar(user)
    } catch (error) {
      console.error('取得用戶頭像失敗:', error)
      // 回傳預設頭像
      return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjAiIGN5PSIyMCIgcj0iMjAiIGZpbGw9IiM5QzlDOUMiLz4KPHBhdGggZD0iTTIwIDEwQzE2LjEzIDEwIDEzIDEzLjEzIDEzIDE3QzEzIDIwLjg3IDE2LjEzIDI0IDIwIDI0QzIzLjg3IDI0IDI3IDIwLjg3IDI3IDE3QzI3IDEzLjEzIDIzLjg3IDEwIDIwIDEwWiIgZmlsbD0iI0Y1RjVGNSIvPgo8cGF0aCBkPSJNMTAgMzBDMTAgMjUuNTggMTQuNTggMjEgMjAgMjFDMjUuNDIgMjEgMzAgMjUuNTggMzAgMzBIMFYzMFoiIGZpbGw9IiNGNUY1RjUiLz4KPC9zdmc+'
    }
  }

  // 監聽篩選變更
  import { watch } from 'vue'

  watch(filterStatus, () => {
    currentPage.value = 0
    fetchReports()
  })

  // 生命週期
  onMounted(() => {
    fetchReports()
  })
</script>

<style scoped>
  .report-list-container {
    max-width: 1200px;
    margin: 0 auto;
  }

  .report-item {
    transition: all 0.3s ease;
  }

  .report-item:hover {
    background-color: #f8f9fa;
    border-radius: 8px;
    padding: 1rem;
    margin: -1rem;
  }

  .reported-comment {
    border-left: 4px solid #dc3545;
  }

  .comment-content {
    white-space: pre-wrap;
    word-break: break-word;
  }

  .comment-image {
    width: 80px;
    height: 80px;
    object-fit: cover;
    border-radius: 4px;
    cursor: pointer;
    transition: transform 0.2s ease;
  }

  .comment-image:hover {
    transform: scale(1.05);
  }

  .reporter-avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #e9ecef;
    border: 1px solid #dee2e6;
  }

  .reporter-avatar img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .spinning {
    animation: spin 1s linear infinite;
  }

  @keyframes spin {
    from {
      transform: rotate(0deg);
    }

    to {
      transform: rotate(360deg);
    }
  }

  .modal {
    background-color: rgba(0, 0, 0, 0.5);
  }

  .pagination .page-link {
    color: #0d6efd;
  }

  .pagination .page-item.active .page-link {
    background-color: #0d6efd;
    border-color: #0d6efd;
  }

  .pagination .page-item.disabled .page-link {
    color: #6c757d;
  }

  /* 響應式設計 - 與 Admin.vue 保持一致 */
  @media (max-width: 768px) {
    .report-list-container {
      padding: 0 1rem;
    }

    .card-body {
      padding: 1rem;
    }

    .btn-sm {
      font-size: 0.8rem;
      padding: 0.2rem 0.4rem;
    }

    .filter-select {
      min-width: 120px;
    }

    .comment-image {
      width: 60px;
      height: 60px;
    }

    .reporter-avatar {
      width: 32px;
      height: 32px;
    }

    .reporter-avatar img {
      width: 100%;
      height: 100%;
    }

    .modal-dialog {
      margin: 1rem;
    }

    .modal-body {
      padding: 1rem;
    }

    .table {
      font-size: 0.875rem;
    }

    .table td,
    .table th {
      padding: 0.5rem;
    }
  }

  @media (max-width: 576px) {
    .report-list-container {
      padding: 0 0.5rem;
    }

    .card-body {
      padding: 0.75rem;
    }

    .btn-sm {
      font-size: 0.75rem;
      padding: 0.15rem 0.3rem;
    }

    .comment-image {
      width: 50px;
      height: 50px;
    }

    .pagination .page-link {
      padding: 0.25rem 0.5rem;
      font-size: 0.875rem;
    }

    .badge {
      font-size: 0.75rem;
    }

    .text-muted.small {
      font-size: 0.75rem;
    }
  }

  /* 卡片樣式 - 與 Admin.vue 保持一致 */
  .card {
    border-radius: 1rem;
    border: none;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  }

  .card-header {
    font-size: 1.1rem;
    font-weight: 600;
    border-radius: 1rem 1rem 0 0 !important;
  }

  /* 統計卡片樣式 - 與 Admin.vue 保持一致 */
  .card.bg-warning,
  .card.bg-success,
  .card.bg-danger,
  .card.bg-info {
    border-radius: 1rem;
    transition: transform 0.2s ease;
  }

  .card.bg-warning:hover,
  .card.bg-success:hover,
  .card.bg-danger:hover,
  .card.bg-info:hover {
    transform: translateY(-2px);
  }
</style>