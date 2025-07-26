<template>
  <div class="image-uploader">
    <!-- 上傳區域 -->
    <div class="upload-area" :class="{ 'drag-over': isDragOver, 'disabled': disabled }" @drop="handleDrop"
      @dragover="handleDragOver" @dragleave="handleDragLeave" @click="triggerFileInput">

      <div class="upload-content">
        <i class="bi bi-cloud-upload upload-icon"></i>
        <p class="upload-text">
          {{ uploadText }}
        </p>
        <p class="upload-hint">
          支援 {{ supportedFormatsText }}，最多 {{ maxCount }} 張
        </p>
        <p class="upload-size-hint">
          單張圖片最大 {{ maxSizeText }}
        </p>
      </div>

      <input ref="fileInput" type="file" multiple :accept="acceptTypes" @change="handleFileChange" @click.stop
        class="file-input" :disabled="disabled" />
    </div>

    <!-- 圖片預覽區域 -->
    <div v-if="images.length > 0" class="image-preview-area">
      <div class="preview-header">
        <h6 class="preview-title">
          <i class="bi bi-images me-2"></i>
          已選擇 {{ images.length }} 張圖片
        </h6>
        <button v-if="images.length > 1" @click="clearAll" class="btn btn-sm btn-outline-danger" :disabled="disabled">
          <i class="bi bi-trash me-1"></i>
          清空全部
        </button>
      </div>

      <draggable v-model="images" class="image-grid" :item-key="getItemKey" @end="handleReorder">
        <template #item="{ element, index }">
          <div class="image-item" :class="{
            'processing': element.processing,
            'deleted': element.isDeleted
          }">
            <!-- 圖片預覽 -->
            <div class="image-preview">
              <img :src="element.previewUrl" :alt="`圖片 ${index + 1}`" @error="handleImageError" class="preview-image"
                :class="{ 'deleted-image': element.isDeleted }" />

              <!-- 處理中遮罩 -->
              <div v-if="element.processing" class="processing-overlay">
                <div class="spinner-border spinner-border-sm text-light" role="status">
                  <span class="visually-hidden">處理中...</span>
                </div>
                <p class="processing-text">處理中...</p>
              </div>

              <!-- 錯誤遮罩 -->
              <div v-if="element.error" class="error-overlay">
                <i class="bi bi-exclamation-triangle text-danger"></i>
                <p class="error-text">{{ element.error }}</p>
              </div>

              <!-- 已刪除遮罩 -->
              <div v-if="element.isDeleted" class="deleted-overlay">
                <i class="bi bi-trash text-danger"></i>
                <p class="deleted-text">已標記刪除</p>
              </div>
            </div>

            <!-- 操作按鈕 -->
            <div class="image-actions">
              <button v-if="!element.isDeleted" @click="removeImage(index)" class="btn btn-sm btn-danger remove-btn"
                :disabled="disabled || element.processing" title="移除圖片">
                <i class="bi bi-x"></i>
              </button>

              <button v-else @click="restoreImage(index)" class="btn btn-sm btn-success restore-btn"
                :disabled="disabled" title="恢復圖片">
                <i class="bi bi-arrow-clockwise"></i>
              </button>

              <div v-if="element.processing" class="progress-indicator">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: element.progress + '%' }"></div>
                </div>
              </div>
            </div>

            <!-- 圖片資訊 -->
            <div class="image-info">
              <small class="image-name">{{ element.name }}</small>
              <small class="image-size">{{ formatFileSize(element.size) }}</small>
              <small v-if="element.isDeleted" class="deleted-status text-danger">
                <i class="bi bi-trash me-1"></i>已刪除
              </small>
            </div>
          </div>
        </template>
      </draggable>
    </div>

    <!-- 錯誤提示 -->
    <div v-if="errors.length > 0" class="error-messages">
      <div v-for="(error, index) in errors" :key="index" class="alert alert-danger alert-sm">
        <i class="bi bi-exclamation-triangle me-2"></i>
        {{ error }}
      </div>
    </div>
  </div>
</template>

<script setup>
  import { ref, computed, watch, onUnmounted, toRaw } from 'vue'
  import draggable from 'vuedraggable'
  import ImageService, { ImageValidator } from '@/services/imageService'
  import { imageAPI } from '@/services/api'

  // Props
  const props = defineProps({
    // 圖片類型：'COMMENT' | 'VENDOR'
    type: {
      type: String,
      default: 'COMMENT',
      validator: (value) => ['COMMENT', 'VENDOR'].includes(value)
    },
    // 是否禁用
    disabled: {
      type: Boolean,
      default: false
    },
    // 圖片陣列
    images: {
      type: Array,
      default: () => []
    },
    // 是否允許拖拽排序
    allowReorder: {
      type: Boolean,
      default: true
    },
    // 評論 ID（用於排序 API）
    commentId: {
      type: Number,
      default: null
    },
    // 是否啟用自動排序同步
    autoSyncOrder: {
      type: Boolean,
      default: false
    }
  })

  // Emits
  const emit = defineEmits(['update:images', 'change', 'error', 'orderUpdated'])

  // 響應式數據
  const fileInput = ref(null)
  const isDragOver = ref(false)
  const errors = ref([])
  const images = ref([])
  const watchEnabled = ref(true) // 控制 watch 的啟用狀態

  // 防抖變數
  let changeTimeout = null

  // 清理函數
  onUnmounted(() => {
    // 清理防抖 timeout
    if (changeTimeout) {
      clearTimeout(changeTimeout)
      changeTimeout = null
    }



    // 清理 blob URLs
    images.value.forEach(image => {
      if (image.previewUrl && image.previewUrl.startsWith('blob:')) {
        ImageService.revokePreviewUrl(image.previewUrl)
      }
    })

    console.log('🧹 ImageUploader 組件已清理')

    // 重置循環檢測計數器
    lastProcessedImages = null;
  })

  // 計算屬性
  const config = computed(() => ImageService.getConfig(props.type))
  const maxCount = computed(() => config.value.maxCount)
  const maxSizeText = computed(() => {
    const size = config.value.maxFileSize / (1024 * 1024)
    return size >= 1 ? `${size}MB` : `${config.value.maxFileSize / 1024}KB`
  })
  const supportedFormatsText = computed(() => {
    const formats = ImageService.getSupportedFormats()
    return formats.map(f => f.split('/')[1].toUpperCase()).join('、')
  })
  const acceptTypes = computed(() => ImageService.getSupportedFormats().join(','))

  const uploadText = computed(() => {
    if (props.disabled) return '上傳功能已禁用'
    return '點擊或拖拽圖片到此處上傳'
  })

  // 取得 draggable 的 item key
  const getItemKey = (item) => {
    // 防止遞迴問題
    if (!item || typeof item !== 'object') {
      return `temp-${Date.now()}-${Math.random()}`
    }

    // 優先使用數字 imageId
    if (item.originalImg?.imageId) {
      return item.originalImg.imageId
    }
    if (item.originalImg?.id && !isNaN(Number(item.originalImg.id))) {
      return item.originalImg.id
    }
    if (item.imageId && !isNaN(Number(item.imageId))) {
      return item.imageId
    }
    // 如果是新圖片，使用臨時 ID
    if (item.id && item.id.toString().startsWith('new-')) {
      return item.id
    }
    // 最後才使用字串 ID
    return item.id || `temp-${Date.now()}-${Math.random()}`
  }



  // 圖片陣列比較函數 - 不比對物件引用，只比對關鍵欄位
  function areImageArraysEqual(arr1, arr2) {
    if (!Array.isArray(arr1) || !Array.isArray(arr2)) {
      return arr1 === arr2;
    }

    if (arr1.length !== arr2.length) return false;

    return arr1.every((img1, i) => {
      const img2 = arr2[i];
      if (!img1 || !img2) return img1 === img2;

      return (
        img1.imageId === img2.imageId &&
        img1.id === img2.id &&
        img1.isActive === img2.isActive &&
        img1.isDeleted === img2.isDeleted &&
        img1.sortOrder === img2.sortOrder
      );
    });
  }

  // 同步圖片到父組件 - 只在明確操作時觸發
  const syncImagesToParent = () => {
    console.log('🔄 同步圖片到父組件:', {
      imagesCount: images.value.length,
      activeImages: images.value.filter(img => img.isActive !== false).length
    });

    // ✅ 檢查是否為用戶操作（如刪除、排序、上傳新圖片）
    const hasUserAction = images.value.some(img =>
      img.isNew || // 新上傳的圖片
      img.isDeleted !== undefined || // 刪除操作
      img.sortOrder !== undefined || // 排序操作
      img.sortOrder !== img.originalImg?.sortOrder // 排序已改變
    );

    // ✅ 額外檢查：確保不是來自後端的資料更新
    const isFromBackend = images.value.every(img =>
      img.imageId && (img.commentImageUrl || img.imageUrl) && !img.isNew
    );

    if (hasUserAction && !isFromBackend) {
      console.log('🔄 檢測到用戶操作，發送圖片變化事件');

      // 使用深拷貝避免引用共享
      const clonedImages = images.value.map(img => ({
        id: img.id,
        imageId: img.imageId,
        name: img.name,
        previewUrl: img.previewUrl,
        commentImageUrl: img.commentImageUrl,
        imageUrl: img.imageUrl,
        isActive: img.isActive,
        isDeleted: img.isDeleted,
        sortOrder: img.sortOrder,
        mimeType: img.mimeType,
        size: img.size,
        isNew: img.isNew || false,
        file: img.file // 保留檔案引用
      }));

      emit('update:images', clonedImages);

      // 如果有 commentId，觸發詳細的圖片變化事件
      if (props.commentId && props.type === 'COMMENT') {
        console.log('🔄 圖片陣列變化，觸發詳細事件:', {
          commentId: props.commentId,
          imagesCount: clonedImages.length,
          activeImages: clonedImages.filter(img => img.isActive !== false).length
        });
      }
    } else {
      console.log('🔄 跳過同步：沒有用戶操作');
    }
  };

  // 監聽 images prop 變化 - 只接收完整後端圖片，不 emit
  let lastProcessedImages = null;

  watch(() => props.images, (newImages) => {
    // ✅ 檢查 watch 是否啟用
    if (!watchEnabled.value) {
      console.log('🔄 watch 已禁用，跳過處理');
      return;
    }

    console.log('🖼️ 接收圖片資料:', {
      count: newImages?.length || 0,
      images: newImages?.map(img => ({
        id: img.id,
        imageId: img.imageId,
        name: img.name,
        previewUrl: img.previewUrl,
        commentImageUrl: img.commentImageUrl,
        imageUrl: img.imageUrl
      }))
    });

    // 檢查是否為重複處理
    if (lastProcessedImages === newImages) {
      console.log('🔄 檢測到重複的圖片資料，跳過處理');
      return;
    }

    // ✅ 只接收完整後端圖片，不 emit
    const isAllFromBackend = newImages && Array.isArray(newImages) &&
      newImages.every(img => img && img.imageId && (img.commentImageUrl || img.imageUrl));

    if (isAllFromBackend && !areImageArraysEqual(newImages, images.value)) {
      console.log('[ImageUploader] 接收後端圖片，更新 localImages');

      // ✅ 過濾掉已刪除的圖片
      const cleanImages = newImages.filter(img => !img.isDeleted);
      console.log(`✅ 過濾後保留 ${cleanImages.length} 張圖片（原本 ${newImages.length} 張）`);

      // ✅ 轉成前端格式並更新 localImages
      if (cleanImages && Array.isArray(cleanImages) && cleanImages.length > 0) {
        images.value = cleanImages.map((img, index) => {
          // 確保圖片是普通物件，不是響應式物件
          let plainImg;
          if (img && typeof img === 'object' && img.__v_isRef) {
            plainImg = toRaw(img);
          } else {
            plainImg = img;
          }

          // 處理圖片 URL，確保在部署環境下能正確顯示
          let processedUrl = plainImg.commentImageUrl || plainImg.imageUrl;
          if (processedUrl && processedUrl.startsWith('https://192.168.36.96:8080/uploads/')) {
            processedUrl = processedUrl.replace('https://192.168.36.96:8080/uploads/', '/api/uploads/');
          } else if (processedUrl && processedUrl.startsWith('/uploads/')) {
            processedUrl = '/api' + processedUrl;
          }


          console.log(`🔍 處理圖片 ${index} 的原始資料:`, plainImg);

          return {
            id: plainImg.imageId || plainImg.id,
            imageId: plainImg.imageId,
            name: plainImg.name || `圖片 ${index + 1}`,
            previewUrl: processedUrl || plainImg.previewUrl,
            commentImageUrl: processedUrl,
            imageUrl: processedUrl, // 確保各欄位一致性
            sortOrder: plainImg.sortOrder || index,
            isActive: plainImg.isActive !== undefined ? plainImg.isActive : true,
            isDeleted: false, // 確保不為刪除狀態
            mimeType: plainImg.mimeType,
            size: plainImg.size || 0,
            progress: 100,
            isNew: false,
            processing: false,
            error: null,
            // 保存原始資料避免遞迴
            originalImg: {
              imageId: plainImg.imageId,
              commentImageUrl: plainImg.commentImageUrl,
              imageUrl: plainImg.imageUrl,
              sortOrder: plainImg.sortOrder,
              isActive: plainImg.isActive,
              isDeleted: plainImg.isDeleted,
              mimeType: plainImg.mimeType
            }
          }
        });

        console.log('✅ 圖片資料處理完成，總數:', images.value.length);
      } else {
        console.log('📝 沒有圖片或為空陣列');
        images.value = [];
      }

      // 記錄本次處理的圖片
      lastProcessedImages = newImages;
    } else {
      console.log('🔄 跳過處理：不是完整後端圖片或資料相同');
    }
  }, { immediate: true, deep: true })

  // 方法
  const triggerFileInput = () => {
    if (!props.disabled && fileInput.value) {
      // 確保 input 值已清空
      fileInput.value.value = ''
      fileInput.value.click()
    }
  }

  const handleFileChange = async (event) => {
    const files = Array.from(event.target.files || [])
    if (files.length === 0) return

    // 立即清空 input 值，避免重複觸發
    event.target.value = ''

    // 防止重複處理
    if (props.disabled) return

    console.log('📁 處理檔案:', files.length, '個檔案')
    await processFiles(files)
  }

  const handleDrop = async (event) => {
    event.preventDefault()
    isDragOver.value = false

    if (props.disabled) return

    const files = Array.from(event.dataTransfer.files || [])
    if (files.length === 0) return

    await processFiles(files)
  }

  const handleDragOver = (event) => {
    event.preventDefault()
    if (!props.disabled) {
      isDragOver.value = true
    }
  }

  const handleDragLeave = (event) => {
    event.preventDefault()
    isDragOver.value = false
  }

  const processFiles = async (files) => {
    console.log('🔄 開始處理檔案...')

    // 驗證檔案
    const validation = ImageValidator.validateMultiple(files, props.type)
    if (!validation.isValid) {
      errors.value = validation.errors
      emit('error', validation.errors)
      return
    }

    // 檢查總數量限制
    const totalCount = images.value.length + files.length
    if (totalCount > maxCount.value) {
      const error = `圖片數量超過限制: ${totalCount} > ${maxCount.value}`
      errors.value = [error]
      emit('error', [error])
      return
    }

    // 清空錯誤
    errors.value = []

    // 處理每個檔案
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      const imageId = `new-${Date.now()}-${i}`

      // 添加臨時圖片項目
      const tempImage = {
        id: imageId,
        name: file.name,
        size: file.size,
        previewUrl: ImageService.createPreviewUrl(file),
        processing: true,
        error: null,
        progress: 0,
        file: file
      }

      images.value.push(tempImage)

      try {
        // 只在前端處理圖片（壓縮、驗證等），不立即上傳
        const processedImage = await ImageService.processImageLocally(file, props.type)

        // 更新圖片資料
        const index = images.value.findIndex(img => img.id === imageId)
        if (index !== -1) {
          images.value[index] = {
            ...tempImage,
            ...processedImage,
            processing: false,
            progress: 100,
            isNew: true // 標記為新圖片，需要上傳
          }
        }
      } catch (error) {
        // 處理錯誤
        const index = images.value.findIndex(img => img.id === imageId)
        if (index !== -1) {
          images.value[index] = {
            ...tempImage,
            processing: false,
            error: error.message
          }
        }

        errors.value.push(`處理圖片 ${file.name} 失敗: ${error.message}`)
        emit('error', [error.message])
      }
    }

    // 檔案處理完成後，同步到父組件
    syncImagesToParent();
  }

  const removeImage = async (index) => {
    const image = images.value[index]

    console.log('🗑️ 移除圖片:', {
      index,
      image,
      hasImageId: !!(image.originalImg?.imageId || image.originalImg?.id),
      isNewImage: image.id.toString().startsWith('new-')
    })

    // 檢查是否為已存在的圖片（有 imageId）
    const hasImageId = !!(image.originalImg?.imageId || image.originalImg?.id)
    const isNewImage = image.id.toString().startsWith('new-')

    if (hasImageId && !isNewImage) {
      // 已存在的圖片：軟刪除（設置 isActive = false）
      console.log('🔄 軟刪除已存在的圖片:', image.originalImg?.imageId || image.originalImg?.id)

      // 使用 splice 產生新物件，確保 Vue 3 能正確追蹤變化
      const updatedImage = {
        ...image,
        isActive: false,
        isDeleted: true // 額外標記，方便 UI 處理
      }

      images.value.splice(index, 1, updatedImage)

      console.log('✅ 圖片已標記為軟刪除，保留在陣列中')
      console.log('🔄 圖片狀態變化:', {
        imageId: image.originalImg?.imageId || image.originalImg?.id,
        isActive: updatedImage.isActive,
        isDeleted: updatedImage.isDeleted
      })

      // 立即同步到後端
      if (props.commentId && props.type === 'COMMENT') {
        try {
          console.log('🔄 開始同步軟刪除到後端...')
          await syncImagesToBackend()
          console.log('✅ 軟刪除已同步到後端')
          // 同步到父組件
          syncImagesToParent();
        } catch (error) {
          console.error('❌ 同步軟刪除到後端失敗:', error)
          // 可以選擇恢復圖片狀態或顯示錯誤訊息
          console.log('🔄 嘗試恢復圖片狀態...')
          const restoredImage = {
            ...updatedImage,
            isActive: true,
            isDeleted: false
          }
          images.value.splice(index, 1, restoredImage)
          console.log('✅ 圖片狀態已恢復')
        }
      } else {
        // 如果不是 COMMENT 類型，直接同步到父組件
        syncImagesToParent();
      }
    } else {
      // 新上傳的圖片：直接移除
      console.log('🗑️ 直接移除新上傳的圖片')

      // 釋放預覽 URL
      if (image.previewUrl && image.previewUrl.startsWith('blob:')) {
        ImageService.revokePreviewUrl(image.previewUrl)
      }

      // 直接從陣列中移除
      images.value.splice(index, 1)

      // 同步到父組件
      syncImagesToParent();
    }
  }

  const restoreImage = async (index) => {
    const image = images.value[index]

    console.log('🔄 恢復圖片:', {
      index,
      image,
      imageId: image.originalImg?.imageId || image.originalImg?.id
    })

    // 使用 splice 產生新物件，確保 Vue 3 能正確追蹤變化
    const updatedImage = {
      ...image,
      isActive: true,
      isDeleted: false
    }

    images.value.splice(index, 1, updatedImage)

    console.log('✅ 圖片已恢復')

    // 立即同步到後端
    if (props.commentId && props.type === 'COMMENT') {
      try {
        await syncImagesToBackend()
        console.log('✅ 圖片恢復已同步到後端')
        // 同步到父組件
        syncImagesToParent();
      } catch (error) {
        console.error('❌ 同步圖片恢復到後端失敗:', error)
      }
    } else {
      // 如果不是 COMMENT 類型，直接同步到父組件
      syncImagesToParent();
    }
  }

  const clearAll = async () => {
    console.log('🗑️ 清空所有圖片')

    // 處理每個圖片
    images.value.forEach((image, index) => {
      const hasImageId = !!(image.originalImg?.imageId || image.originalImg?.id)
      const isNewImage = image.id.toString().startsWith('new-')

      if (hasImageId && !isNewImage) {
        // 已存在的圖片：軟刪除
        console.log(`🔄 軟刪除圖片 ${index}:`, image.originalImg?.imageId || image.originalImg?.id)

        // 使用 splice 產生新物件
        const updatedImage = {
          ...image,
          isActive: false,
          isDeleted: true
        }

        images.value.splice(index, 1, updatedImage)
      } else {
        // 新上傳的圖片：釋放資源
        if (image.previewUrl && image.previewUrl.startsWith('blob:')) {
          ImageService.revokePreviewUrl(image.previewUrl)
        }
      }
    })

    // 移除新上傳的圖片，保留已刪除的圖片
    images.value = images.value.filter(image => {
      const hasImageId = !!(image.originalImg?.imageId || image.originalImg?.id)
      const isNewImage = image.id.toString().startsWith('new-')

      // 保留已存在的圖片（即使已標記刪除），移除新上傳的圖片
      return hasImageId && !isNewImage
    })

    console.log('✅ 清空完成，保留已標記刪除的圖片')

    // 立即同步到後端
    if (props.commentId && props.type === 'COMMENT') {
      try {
        await syncImagesToBackend()
        console.log('✅ 清空操作已同步到後端')
        // 同步到父組件
        syncImagesToParent();
      } catch (error) {
        console.error('❌ 同步清空操作到後端失敗:', error)
      }
    } else {
      // 如果不是 COMMENT 類型，直接同步到父組件
      syncImagesToParent();
    }
  }

  // 同步圖片狀態到後端
  const syncImagesToBackend = async () => {
    if (!props.commentId || props.type !== 'COMMENT') {
      console.log('⚠️ 不需要同步：缺少 commentId 或不是 COMMENT 類型')
      return
    }

    try {
      console.log('🔄 同步圖片狀態到後端...')

      // 準備同步資料 - 包含所有已存在的圖片（包括已刪除的）
      const syncData = images.value
        .filter(image => {
          // 排除新上傳的圖片（以 "new-" 開頭）
          if (image.id.toString().startsWith('new-')) {
            return false
          }

          // 處理 "existing-X" 格式
          if (typeof image.id === 'string' && image.id.startsWith('existing-')) {
            const numPart = image.id.substring('existing-'.length())
            return !isNaN(Number(numPart))
          }

          // 處理數字格式
          return !isNaN(Number(image.id))
        })
        .map((image, index) => {
          let imageId

          // 優先使用資料庫中的真正 imageId
          if (image.originalImg && image.originalImg.imageId) {
            imageId = Number(image.originalImg.imageId)
          } else if (image.originalImg && image.originalImg.id) {
            imageId = Number(image.originalImg.id)
          } else if (typeof image.id === 'string' && image.id.startsWith('existing-')) {
            // 處理 "existing-X" 格式
            const numPart = image.id.substring('existing-'.length())
            imageId = Number(numPart)
          } else if (!isNaN(Number(image.id))) {
            // 處理數字格式
            imageId = Number(image.id)
          } else {
            console.warn('⚠️ 無法找到有效的 imageId:', image)
            return null
          }

          return {
            imageId: imageId,
            sortOrder: index,
            isActive: image.isActive !== false, // 使用圖片的 isActive 狀態，預設為 true
            isDeleted: image.isDeleted === true // 明確標記刪除狀態
          }
        })
        .filter(item => item !== null) // 過濾掉無效的項目

      console.log('📦 同步資料（包含刪除狀態）:', syncData)
      console.log('🔍 原始圖片資料:', images.value.map(img => ({
        id: img.id,
        type: typeof img.id,
        originalImg: img.originalImg,
        imageId: img.originalImg?.imageId || img.originalImg?.id,
        isActive: img.isActive,
        isDeleted: img.isDeleted
      })))
      console.log('🌐 API 端點:', `/api/comments/${props.commentId}/images`)

      // 發送到後端
      const response = await imageAPI.updateImageOrder(props.commentId, syncData)
      console.log('✅ 圖片狀態同步成功:', response.data)

      // 如果後端回傳了更新後的圖片資料，直接覆蓋前端陣列
      if (response.data && Array.isArray(response.data)) {
        console.log('🔄 使用後端回傳的圖片資料更新前端陣列')

        // ✅ 暫時禁用 watch 避免無限迴圈
        const wasWatching = watchEnabled.value;
        watchEnabled.value = false;

        try {
          // ✅ 過濾掉已刪除的圖片
          const cleanImages = response.data.filter(img => !img.isDeleted);
          console.log(`✅ 過濾後保留 ${cleanImages.length} 張圖片（原本 ${response.data.length} 張）`);

          images.value = cleanImages.map((img, index) => {
            // 處理圖片 URL，確保在部署環境下能正確顯示
            let processedUrl = img.commentImageUrl;
            if (processedUrl && processedUrl.startsWith('https://192.168.36.96:8080/uploads/')) {
              processedUrl = processedUrl.replace('https://192.168.36.96:8080/uploads/', '/api/uploads/');
            } else if (processedUrl && processedUrl.startsWith('/uploads/')) {
              processedUrl = '/api' + processedUrl;
            }

            return {
              id: img.imageId,
              imageId: img.imageId,
              name: `圖片 ${index + 1}`,
              previewUrl: processedUrl,
              commentImageUrl: processedUrl,
              imageUrl: processedUrl, // 確保各欄位一致性
              sortOrder: img.sortOrder,
              isActive: img.isActive,
              isDeleted: false, // 確保不為刪除狀態
              mimeType: img.mimeType,
              size: 0,
              progress: 100,
              isNew: false,
              processing: false,
              error: null,
              // 保存原始資料避免遞迴
              originalImg: {
                imageId: img.imageId,
                commentImageUrl: img.commentImageUrl,
                sortOrder: img.sortOrder,
                isActive: img.isActive,
                isDeleted: img.isDeleted,
                mimeType: img.mimeType
              }
            };
          });

          console.log('✅ 前端圖片已依後端結果更新')
        } finally {
          // 恢復 watch
          watchEnabled.value = wasWatching;
        }
      }

      // 觸發成功事件
      emit('orderUpdated', {
        commentId: props.commentId,
        images: syncData,
        success: true
      })

      return response.data
    } catch (error) {
      console.error('❌ 圖片狀態同步失敗:', error)
      console.error('❌ 錯誤詳情:', {
        message: error.message,
        status: error.response?.status,
        statusText: error.response?.statusText,
        data: error.response?.data,
        url: error.config?.url,
        method: error.config?.method
      })

      // 構建詳細錯誤信息
      let errorMessage = error.message
      if (error.response?.status) {
        errorMessage += ` (HTTP ${error.response.status})`
      }
      if (error.response?.data?.message) {
        errorMessage += `: ${error.response.data.message}`
      }

      // 觸發錯誤事件
      emit('orderUpdated', {
        commentId: props.commentId,
        images: images.value,
        success: false,
        error: errorMessage,
        details: {
          status: error.response?.status,
          data: error.response?.data
        }
      })

      throw error
    }
  }

  const handleReorder = async () => {
    console.log('🔄 開始處理圖片排序...');

    // 更新排序
    images.value.forEach((image, index) => {
      const oldSortOrder = image.sortOrder;
      image.sortOrder = index;

      // ✅ 標記為排序操作
      if (oldSortOrder !== index) {
        console.log(`🔄 圖片 ${image.id} 排序從 ${oldSortOrder} 變更為 ${index}`);
      }
    })

    // 如果啟用自動同步且有評論 ID，則發送到後端
    if (props.autoSyncOrder && props.commentId && props.type === 'COMMENT') {
      try {
        await syncImagesToBackend()
        console.log('✅ 排序已同步到後端')
        // 同步到父組件
        syncImagesToParent();
      } catch (error) {
        console.error('❌ 同步排序到後端失敗:', error)
      }
    } else {
      // 如果不是自動同步或不是 COMMENT 類型，直接同步到父組件
      syncImagesToParent();
    }
  }

  const handleImageError = (event) => {
    const img = event.target
    console.error('❌ 圖片載入失敗:', {
      src: img.src,
      alt: img.alt,
      naturalWidth: img.naturalWidth,
      naturalHeight: img.naturalHeight
    })

    // 設置預設圖片
    img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iODAiIGhlaWdodD0iODAiIHZpZXdCb3g9IjAgMCA4MCA4MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiBmaWxsPSIjRjVGNUY1Ii8+CjxwYXRoIGQ9Ik0yMCAyMEg2MFY2MEgyMFYyMFoiIGZpbGw9IiNEN0Q3RDciLz4KPHBhdGggZD0iTTI1IDI1SDU1VjU1SDI1VjI1WiIgZmlsbD0iI0Y1RjVGNSIvPgo8L3N2Zz4K'
  }

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }
</script>

<style scoped>
  .image-uploader {
    width: 100%;
  }

  .upload-area {
    border: 2px dashed #dee2e6;
    border-radius: 8px;
    padding: 2rem;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s ease;
    background-color: #f8f9fa;
    position: relative;
  }

  .upload-area:hover:not(.disabled) {
    border-color: #007bff;
    background-color: #e3f2fd;
  }

  .upload-area.drag-over {
    border-color: #007bff;
    background-color: #e3f2fd;
    transform: scale(1.02);
  }

  .upload-area.disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }

  .upload-content {
    pointer-events: none;
  }

  .upload-icon {
    font-size: 3rem;
    color: #6c757d;
    margin-bottom: 1rem;
  }

  .upload-text {
    font-size: 1.1rem;
    font-weight: 500;
    margin-bottom: 0.5rem;
    color: #495057;
  }

  .upload-hint,
  .upload-size-hint {
    font-size: 0.9rem;
    color: #6c757d;
    margin-bottom: 0.25rem;
  }

  .file-input {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    opacity: 0;
    cursor: pointer;
  }

  .file-input:disabled {
    cursor: not-allowed;
  }

  .image-preview-area {
    margin-top: 1.5rem;
  }

  .preview-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 1rem;
  }

  .preview-title {
    margin: 0;
    color: #495057;
  }

  .image-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
    gap: 1rem;
  }

  .image-item {
    position: relative;
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    transition: transform 0.2s ease;
  }

  .image-item:hover {
    transform: translateY(-2px);
  }

  .image-item.processing {
    opacity: 0.7;
  }

  .image-item.deleted {
    opacity: 0.5;
  }

  .image-item.deleted .preview-image {
    filter: grayscale(100%);
  }

  .deleted-image {
    filter: grayscale(100%) brightness(0.7);
  }

  .deleted-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(220, 53, 69, 0.3);
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: #dc3545;
  }

  .deleted-text {
    margin: 0.5rem 0 0 0;
    font-size: 0.8rem;
    text-align: center;
    font-weight: 500;
  }

  .restore-btn {
    width: 24px;
    height: 24px;
    padding: 0;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.75rem;
  }

  .deleted-status {
    display: block;
    font-size: 0.7rem;
    margin-top: 0.25rem;
  }

  .image-preview {
    position: relative;
    width: 100%;
    height: 120px;
    overflow: hidden;
  }

  .preview-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .processing-overlay,
  .error-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.7);
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    color: white;
  }

  .processing-text,
  .error-text {
    margin: 0.5rem 0 0 0;
    font-size: 0.8rem;
    text-align: center;
  }

  .error-overlay {
    background-color: rgba(220, 53, 69, 0.8);
  }

  .image-actions {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .remove-btn {
    width: 24px;
    height: 24px;
    padding: 0;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 0.75rem;
  }

  .progress-indicator {
    width: 24px;
    height: 24px;
    background-color: rgba(0, 0, 0, 0.5);
    border-radius: 50%;
    overflow: hidden;
  }

  .progress-bar {
    width: 100%;
    height: 100%;
    background-color: rgba(255, 255, 255, 0.3);
  }

  .progress-fill {
    height: 100%;
    background-color: #007bff;
    transition: width 0.3s ease;
  }

  .image-info {
    padding: 0.5rem;
    background-color: white;
  }

  .image-name {
    display: block;
    font-size: 0.75rem;
    color: #495057;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .image-size {
    display: block;
    font-size: 0.7rem;
    color: #6c757d;
  }

  .error-messages {
    margin-top: 1rem;
  }

  .alert-sm {
    padding: 0.5rem 0.75rem;
    font-size: 0.875rem;
    margin-bottom: 0.5rem;
  }

  /* 響應式設計 */
  @media (max-width: 768px) {
    .image-grid {
      grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
      gap: 0.75rem;
    }

    .image-preview {
      height: 100px;
    }

    .upload-area {
      padding: 1.5rem;
    }

    .upload-icon {
      font-size: 2.5rem;
    }
  }
</style>