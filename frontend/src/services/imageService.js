/**
 * 統一圖片管理服務
 * 整合評論圖片系統與商家上傳系統
 */

// 圖片壓縮配置
const IMAGE_CONFIG = {
    // 評論圖片配置
    COMMENT: {
        maxSize: 400, // 最大尺寸 (px)
        quality: 0.6, // 壓縮品質 (0.6 = 60%)
        maxFileSize: 500 * 1024, // 最大檔案大小 (500KB)
        maxCount: 5 // 最大圖片數量
    },
    // 商家上傳圖片配置
    VENDOR: {
        maxSize: 800, // 最大尺寸 (px)
        quality: 0.8, // 壓縮品質 (0.8 = 80%)
        maxFileSize: 2 * 1024 * 1024, // 最大檔案大小 (2MB)
        maxCount: 5 // 最大圖片數量（統一為5張）
    }
}

// 支援的圖片格式
const SUPPORTED_FORMATS = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

/**
 * 圖片壓縮工具類
 */
class ImageCompressor {
    /**
     * 壓縮圖片
     * @param {File} file - 原始圖片檔案
     * @param {string} type - 圖片類型 ('COMMENT' | 'VENDOR')
     * @returns {Promise<File>} 壓縮後的圖片檔案
     */
    static async compress(file, type = 'COMMENT') {
        const config = IMAGE_CONFIG[type] || IMAGE_CONFIG.COMMENT

        return new Promise((resolve, reject) => {
            const canvas = document.createElement('canvas')
            const ctx = canvas.getContext('2d')
            const img = new Image()

            img.onload = () => {
                try {
                    // 計算縮放比例
                    let { width, height } = img

                    if (width > config.maxSize || height > config.maxSize) {
                        if (width > height) {
                            height = (height * config.maxSize) / width
                            width = config.maxSize
                        } else {
                            width = (width * config.maxSize) / height
                            height = config.maxSize
                        }
                    }

                    canvas.width = width
                    canvas.height = height

                    // 繪製圖片
                    ctx.drawImage(img, 0, 0, width, height)

                    // 轉換為 Blob
                    canvas.toBlob((blob) => {
                        const compressedFile = new File([blob], file.name, {
                            type: 'image/jpeg',
                            lastModified: Date.now()
                        })
                        resolve(compressedFile)
                    }, 'image/jpeg', config.quality)
                } catch (error) {
                    reject(error)
                }
            }

            img.onerror = () => reject(new Error('圖片載入失敗'))
            img.src = URL.createObjectURL(file)
        })
    }

    /**
     * 檢查圖片大小
     * @param {File} file - 圖片檔案
     * @param {string} type - 圖片類型
     * @returns {boolean} 是否在大小限制內
     */
    static checkFileSize(file, type = 'COMMENT') {
        const config = IMAGE_CONFIG[type] || IMAGE_CONFIG.COMMENT
        return file.size <= config.maxFileSize
    }

    /**
     * 檢查圖片格式
     * @param {File} file - 圖片檔案
     * @returns {boolean} 是否為支援的格式
     */
    static checkFormat(file) {
        return SUPPORTED_FORMATS.includes(file.type)
    }
}

/**
 * 檔案轉換工具類
 */
class FileConverter {
    /**
     * 將檔案轉換為 base64
     * @param {File} file - 檔案
     * @returns {Promise<string>} base64 字串
     */
    static fileToBase64(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader()
            reader.readAsDataURL(file)
            reader.onload = () => resolve(reader.result)
            reader.onerror = error => reject(error)
        })
    }

    /**
     * 將 base64 轉換為 Blob
     * @param {string} base64 - base64 字串
     * @param {string} mimeType - MIME 類型
     * @returns {Blob} Blob 物件
     */
    static base64ToBlob(base64, mimeType = 'image/jpeg') {
        const byteCharacters = atob(base64.split(',')[1])
        const byteNumbers = new Array(byteCharacters.length)

        for (let i = 0; i < byteCharacters.length; i++) {
            byteNumbers[i] = byteCharacters.charCodeAt(i)
        }

        const byteArray = new Uint8Array(byteNumbers)
        return new Blob([byteArray], { type: mimeType })
    }

    /**
     * Blob URL 轉 File
     * @param {string} blobUrl - blob URL
     * @param {string} mimeType - MIME 類型
     * @returns {Promise<File>} File 物件
     */
    static async blobUrlToFile(blobUrl, mimeType) {
        try {
            console.log('🔄 轉換 Blob URL:', blobUrl)

            const response = await fetch(blobUrl)
            if (!response.ok) {
                throw new Error(`Fetch failed: ${response.status}`)
            }

            const blob = await response.blob()
            const fileExtension = mimeType.split('/')[1] || 'jpg'
            const fileName = `image_${Date.now()}.${fileExtension}`

            const file = new File([blob], fileName, { type: mimeType })
            console.log('✅ Blob 轉 File 成功:', {
                name: file.name,
                size: file.size,
                type: file.type
            })

            return file
        } catch (error) {
            console.error('❌ Blob 轉 File 失敗:', error)
            throw error
        }
    }

    /**
     * Base64 轉 File
     * @param {string} base64String - base64 字串
     * @param {string} mimeType - MIME 類型
     * @returns {File} File 物件
     */
    static base64ToFile(base64String, mimeType) {
        try {
            console.log('🔄 轉換 Base64:', base64String.substring(0, 50) + '...')

            // 移除 data:image/xxx;base64, 前綴
            const base64Data = base64String.includes(',')
                ? base64String.split(',')[1]
                : base64String

            const byteString = atob(base64Data)
            const ab = new ArrayBuffer(byteString.length)
            const ia = new Uint8Array(ab)

            for (let i = 0; i < byteString.length; i++) {
                ia[i] = byteString.charCodeAt(i)
            }

            const fileExtension = mimeType.split('/')[1] || 'jpg'
            const fileName = `image_${Date.now()}.${fileExtension}`
            const file = new File([ab], fileName, { type: mimeType })

            console.log('✅ Base64 轉 File 成功:', {
                name: file.name,
                size: file.size,
                type: file.type
            })

            return file
        } catch (error) {
            console.error('❌ Base64 轉 File 失敗:', error)
            throw error
        }
    }
}

/**
 * 圖片驗證工具類
 */
class ImageValidator {
    /**
     * 驗證圖片檔案
     * @param {File} file - 圖片檔案
     * @param {string} type - 圖片類型
     * @returns {Object} 驗證結果
     */
    static validate(file, type = 'COMMENT') {
        const config = IMAGE_CONFIG[type] || IMAGE_CONFIG.COMMENT
        const errors = []

        // 檢查格式
        if (!ImageCompressor.checkFormat(file)) {
            errors.push(`不支援的圖片格式: ${file.type}`)
        }

        // 檢查大小
        if (!ImageCompressor.checkFileSize(file, type)) {
            const maxSizeMB = config.maxFileSize / (1024 * 1024)
            errors.push(`圖片太大: ${(file.size / (1024 * 1024)).toFixed(2)}MB > ${maxSizeMB}MB`)
        }

        return {
            isValid: errors.length === 0,
            errors
        }
    }

    /**
     * 驗證圖片陣列
     * @param {File[]} files - 圖片檔案陣列
     * @param {string} type - 圖片類型
     * @returns {Object} 驗證結果
     */
    static validateMultiple(files, type = 'COMMENT') {
        const config = IMAGE_CONFIG[type] || IMAGE_CONFIG.COMMENT
        const errors = []

        // 檢查數量
        if (files.length > config.maxCount) {
            errors.push(`圖片數量超過限制: ${files.length} > ${config.maxCount}`)
        }

        // 檢查每個檔案
        files.forEach((file, index) => {
            const validation = this.validate(file, type)
            if (!validation.isValid) {
                validation.errors.forEach(error => {
                    errors.push(`圖片 ${index + 1}: ${error}`)
                })
            }
        })

        return {
            isValid: errors.length === 0,
            errors
        }
    }
}

/**
 * 統一圖片管理服務
 */
export class ImageService {
    /**
     * 處理圖片上傳（評論系統）
     * @param {File[]} files - 原始圖片檔案
     * @returns {Promise<Array>} 處理後的圖片資料
     */
    static async processCommentImages(files) {
        const validation = ImageValidator.validateMultiple(files, 'COMMENT')
        if (!validation.isValid) {
            throw new Error(validation.errors.join('\n'))
        }

        const processedImages = []

        for (let i = 0; i < files.length; i++) {
            try {
                const file = files[i]

                // 壓縮圖片
                const compressedFile = await ImageCompressor.compress(file, 'COMMENT')

                // 轉換為 base64
                const base64 = await FileConverter.fileToBase64(compressedFile)

                // 檢查 base64 大小
                const base64Size = Math.ceil((base64.length * 3) / 4)
                if (base64Size > IMAGE_CONFIG.COMMENT.maxFileSize) {
                    throw new Error(`圖片 ${i + 1} 壓縮後仍然太大`)
                }

                processedImages.push({
                    imageUrl: base64,
                    mimeType: 'image/jpeg',
                    sortOrder: i,
                    isNew: true,
                    file: compressedFile
                })
            } catch (error) {
                throw new Error(`處理圖片 ${i + 1} 失敗: ${error.message}`)
            }
        }

        return processedImages
    }

    /**
     * 處理圖片上傳（商家系統）
     * @param {File[]} files - 原始圖片檔案
     * @returns {Promise<Array>} 處理後的圖片資料
     */
    static async processVendorImages(files) {
        const validation = ImageValidator.validateMultiple(files, 'VENDOR')
        if (!validation.isValid) {
            throw new Error(validation.errors.join('\n'))
        }

        const processedImages = []

        for (let i = 0; i < files.length; i++) {
            try {
                const file = files[i]

                // 壓縮圖片
                const compressedFile = await ImageCompressor.compress(file, 'VENDOR')

                // 轉換為 base64
                const base64 = await FileConverter.fileToBase64(compressedFile)

                processedImages.push({
                    imageUrl: base64,
                    mimeType: 'image/jpeg',
                    sortOrder: i,
                    isNew: true,
                    file: compressedFile
                })
            } catch (error) {
                throw new Error(`處理圖片 ${i + 1} 失敗: ${error.message}`)
            }
        }

        return processedImages
    }

    /**
     * 創建圖片預覽 URL
     * @param {File} file - 圖片檔案
     * @returns {string} 預覽 URL
     */
    static createPreviewUrl(file) {
        return URL.createObjectURL(file)
    }

    /**
     * 釋放預覽 URL
     * @param {string} url - 預覽 URL
     */
    static revokePreviewUrl(url) {
        if (url && url.startsWith('blob:')) {
            URL.revokeObjectURL(url)
        }
    }

    /**
     * 獲取圖片配置
     * @param {string} type - 圖片類型
     * @returns {Object} 配置物件
     */
    static getConfig(type = 'COMMENT') {
        return IMAGE_CONFIG[type] || IMAGE_CONFIG.COMMENT
    }

    /**
     * 獲取支援的格式
     * @returns {Array} 支援的格式陣列
     */
    static getSupportedFormats() {
        return [...SUPPORTED_FORMATS]
    }

    /**
     * 調試工具：檢查當前環境配置
     */
    static debugEnvironment() {
        console.log('🔍 環境配置檢查:')
        console.log('  API Base URL:', import.meta.env.VITE_API_BASE_URL || 'http://192.168.36.96:8080')
        console.log('  Token:', localStorage.getItem('token') ? '已設置' : '未設置')
        console.log('  User Agent:', navigator.userAgent)
        console.log('  File API 支援:', typeof File !== 'undefined')
        console.log('  FormData 支援:', typeof FormData !== 'undefined')
        console.log('  Fetch 支援:', typeof fetch !== 'undefined')
    }

    /**
     * 調試工具：測試檔案轉換
     */
    static async testFileConversion() {
        console.log('🧪 檔案轉換測試:')

        // 創建測試檔案
        const testBlob = new Blob(['test'], { type: 'image/jpeg' })
        const testFile = new File([testBlob], 'test.jpg', { type: 'image/jpeg' })

        console.log('  測試檔案:', {
            name: testFile.name,
            size: testFile.size,
            type: testFile.type
        })

        // 測試 FormData
        const formData = new FormData()
        formData.append('file', testFile)
        formData.append('targetType', 'test')
        formData.append('targetId', '123')

        console.log('  FormData 條目:')
        for (let [key, value] of formData.entries()) {
            console.log(`    ${key}:`, value instanceof File ? `File(${value.name}, ${value.size} bytes)` : value)
        }

        return { testFile, formData }
    }

    /**
     * 本地處理圖片（壓縮、驗證等，不上傳）
     * @param {File} file - 原始檔案
     * @param {string} type - 圖片類型 ('COMMENT' | 'VENDOR')
     * @returns {Promise<Object>} 處理後的圖片資料
     */
    static async processImageLocally(file, type) {
        try {
            console.log('🔄 開始本地處理圖片:', {
                fileName: file.name,
                fileSize: file.size,
                fileType: file.type,
                targetType: type
            })

            // 獲取配置
            const config = ImageService.getConfig(type)

            // 壓縮圖片
            const compressedFile = await ImageCompressor.compress(file, type)

            // 轉換為 base64 用於預覽
            const base64Url = await FileConverter.fileToBase64(compressedFile)

            console.log('✅ 本地圖片處理完成:', {
                originalSize: file.size,
                compressedSize: compressedFile.size,
                compressionRatio: ((file.size - compressedFile.size) / file.size * 100).toFixed(2) + '%'
            })

            return {
                file: compressedFile,
                imageUrl: base64Url,
                mimeType: file.type,
                size: compressedFile.size,
                name: file.name,
                sortOrder: 0,
                isNew: true
            }
        } catch (error) {
            console.error('❌ 本地圖片處理失敗:', error)
            throw new Error(`圖片處理失敗: ${error.message}`)
        }
    }

    /**
     * 上傳圖片檔案到後端
     * @param {File} file - 圖片檔案
     * @param {string} targetType - 目標類型 ('comment' | 'vendor')
     * @param {number} targetId - 目標ID
     * @returns {Promise<Object>} 上傳結果
     */
    static async uploadImageFile(file, targetType, targetId) {
        try {
            console.log('🔍 開始上傳圖片:', {
                fileName: file.name,
                fileSize: file.size,
                fileType: file.type,
                targetType: targetType,
                targetId: targetId
            })

            const formData = new FormData()
            formData.append('file', file)
            formData.append('targetType', targetType)
            formData.append('targetId', targetId.toString()) // 確保 targetId 為字串格式

            // 添加調試信息
            console.log('🔍 上傳參數詳情:', {
                fileName: file.name,
                fileSize: file.size,
                fileType: file.type,
                targetType: targetType,
                targetId: targetId,
                targetIdType: typeof targetId,
                targetIdString: targetId.toString()
            })

            // 檢查 FormData 內容
            console.log('📋 FormData 內容檢查:')
            for (let [key, value] of formData.entries()) {
                console.log(`  ${key}:`, value instanceof File ? `File(${value.name}, ${value.size} bytes)` : value)
            }

            // 使用 axios 實例進行上傳
            const { imageAPI } = await import('./api.js')
            const response = await imageAPI.uploadImage(formData)

            console.log('✅ 圖片上傳成功:', response.data)
            return response.data
        } catch (error) {
            console.error('❌ 圖片上傳失敗:', {
                message: error.message,
                status: error.response?.status,
                statusText: error.response?.statusText,
                data: error.response?.data,
                requestData: {
                    url: error.config?.url,
                    method: error.config?.method,
                    headers: error.config?.headers
                }
            })

            // 詳細記錄後端錯誤信息
            if (error.response?.data) {
                console.error('📋 後端錯誤詳情:', {
                    error: error.response.data.error,
                    message: error.response.data.message,
                    timestamp: error.response.data.timestamp,
                    path: error.response.data.path
                })
            }

            // 根據錯誤類型提供具體建議
            if (error.response?.status === 403) {
                console.error('🔐 403 錯誤 - 可能是認證問題，請檢查：')
                console.error('1. 是否已登入')
                console.error('2. Token 是否有效')
                console.error('3. Token 是否正確設置在 Authorization 標頭中')
            } else if (error.response?.status === 400) {
                console.error('📝 400 錯誤 - 可能是參數問題，請檢查：')
                console.error('1. FormData 是否正確設置')
                console.error('2. 檔案是否為空')
                console.error('3. 必要參數是否提供')
            }

            throw new Error(`圖片上傳錯誤: ${error.message}`)
        }
    }

    /**
     * 處理圖片上傳（將 Blob/Base64 轉為檔案路徑）
     * @param {Array} images - 圖片陣列
     * @param {string} targetType - 目標類型
     * @param {number} targetId - 目標ID
     * @returns {Promise<Array>} 處理後的圖片陣列
     */
    static async processImagesForUpload(images, targetType, targetId) {
        const processedImages = []

        console.log('🔄 開始處理圖片上傳...', {
            totalImages: images.length,
            targetType: targetType,
            targetId: targetId
        })

        for (let i = 0; i < images.length; i++) {
            const img = images[i]
            console.log(`📸 處理第 ${i + 1} 張圖片:`, {
                imageUrl: img.imageUrl?.substring(0, 50) + '...',
                mimeType: img.mimeType,
                sortOrder: img.sortOrder,
                isNew: img.isNew
            })

            try {
                let file = null

                // 檢查圖片類型並轉換為 File
                if (img.isNew && img.file) {
                    console.log('🔄 檢測到本地處理的檔案，直接使用...')
                    file = img.file
                } else if (img.imageUrl && img.imageUrl.startsWith('blob:')) {
                    console.log('🔄 檢測到 Blob URL，開始轉換...')
                    file = await FileConverter.blobUrlToFile(img.imageUrl, img.mimeType || 'image/jpeg')
                } else if (img.imageUrl && img.imageUrl.startsWith('data:')) {
                    console.log('🔄 檢測到 Base64，開始轉換...')
                    const mimeType = img.imageUrl.split(';')[0].split(':')[1] || img.mimeType || 'image/jpeg'
                    file = FileConverter.base64ToFile(img.imageUrl, mimeType)
                } else if (img.imageUrl && img.imageUrl.startsWith('uploads/')) {
                    console.log('✅ 圖片已有正確路徑，跳過上傳:', img.imageUrl)
                    processedImages.push(img)
                    continue
                } else {
                    console.warn('⚠️ 圖片 URL 格式不正確，跳過:', img.imageUrl)
                    continue
                }

                if (file) {
                    console.log('📤 開始上傳檔案...')
                    const uploadResult = await ImageService.uploadImageFile(file, targetType, targetId)

                    processedImages.push({
                        imageUrl: uploadResult.filePath || uploadResult.url,
                        mimeType: uploadResult.mimeType || img.mimeType,
                        sortOrder: img.sortOrder || i,
                        isNew: true
                    })

                    console.log('✅ 圖片上傳完成:', uploadResult.filePath || uploadResult.url)
                }
            } catch (error) {
                console.error(`❌ 處理第 ${i + 1} 張圖片失敗:`, error)
                throw new Error(`處理圖片 ${img.name || `第${i + 1}張`} 失敗: ${error.message}`)
            }
        }

        console.log('✅ 所有圖片處理完成:', processedImages.length, '張')
        return processedImages
    }
}

// 導出工具類
export { ImageCompressor, FileConverter, ImageValidator }

// 預設導出
export default ImageService 