package journey.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import journey.domain.comments.CommentImagesBean;
import journey.domain.comments.CommentsBean;
import journey.domain.lodgings.LodgingImagesBean;
import journey.domain.lodgings.LodgingsBean;
import journey.domain.lodgings.RoomTypesBean;
import journey.dto.BaseImageDTO;
import journey.repository.comments.CommentImagesRepository;
import journey.repository.comments.CommentsRepository;
import journey.repository.lodgings.LodgingImagesRepository;
import journey.repository.lodgings.LodgingsRepository;
import journey.repository.lodgings.RoomTypesRepository;
import journey.utils.FileNameGenerator;
import journey.utils.ImageProcessor;

/**
 * 統一圖片管理服務
 * 整合檔案儲存和資料庫元資料管理功能
 * 
 * @author Journey Team
 * @version 2.0
 * @since 2025-07-08
 */
@Service
public class UnifiedImageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Autowired
    private LodgingImagesRepository lodgingImagesRepository;

    @Autowired
    private CommentImagesRepository commentImagesRepository;

    @Autowired
    private LodgingsRepository lodgingsRepository;

    @Autowired
    private RoomTypesRepository roomTypesRepository;

    @Autowired
    private CommentsRepository commentsRepository;

    @Autowired
    private ImageProcessor imageProcessor;

    /**
     * 驗證圖片數量限制
     * 
     * @param images     圖片 DTO 列表
     * @param maxCount   最大允許的圖片數量
     * @param targetType 目標類型
     * @throws IllegalArgumentException 當圖片數量超過限制時拋出
     */
    public void validateImageCount(List<BaseImageDTO> images, int maxCount, String targetType) {
        if (images != null && images.size() > maxCount) {
            throw new IllegalArgumentException(targetType + "圖片數量不能超過" + maxCount + "張");
        }
    }

    /**
     * 驗證圖片資料完整性
     * 
     * @param imageDto 圖片 DTO
     * @throws IllegalArgumentException 當目標不存在時拋出
     */
    public void validateImageData(BaseImageDTO imageDto) {
        if (imageDto.getTargetId() == null) {
            throw new IllegalArgumentException("目標ID不能為空");
        }

        switch (imageDto.getTargetType()) {
            case "lodging":
                if (!lodgingsRepository.existsById(imageDto.getTargetId())) {
                    throw new IllegalArgumentException("住宿不存在");
                }
                break;
            case "room_type":
                if (!roomTypesRepository.existsById(imageDto.getTargetId())) {
                    throw new IllegalArgumentException("房型不存在");
                }
                break;
            case "comment":
                if (!commentsRepository.existsById(imageDto.getTargetId())) {
                    throw new IllegalArgumentException("留言不存在");
                }
                break;
            default:
                throw new IllegalArgumentException("不支援的目標類型: " + imageDto.getTargetType());
        }
    }

    /**
     * 將 BaseImageDTO 轉換為對應的資料庫實體
     * 
     * @param imageDto 圖片 DTO
     * @return 對應的資料庫實體
     */
    public Object convertToBean(BaseImageDTO imageDto) {
        switch (imageDto.getTargetType()) {
            case "lodging":
            case "room_type":
                return convertToLodgingImageBean(imageDto);
            case "comment":
                return convertToCommentImageBean(imageDto);
            default:
                throw new IllegalArgumentException("不支援的目標類型: " + imageDto.getTargetType());
        }
    }

    /**
     * 轉換為 LodgingImagesBean
     */
    private LodgingImagesBean convertToLodgingImageBean(BaseImageDTO imageDto) {
        LodgingImagesBean image = new LodgingImagesBean();

        if (imageDto.getImageId() != null) {
            image.setImageId(imageDto.getImageId());
        }

        image.setImageType(imageDto.getImageType());
        image.setImageUrl(imageDto.getImageUrl());
        image.setMimeType(imageDto.getMimeType());
        image.setSortOrder(imageDto.getSortOrder());
        image.setIsActive(imageDto.getIsActive() != null ? imageDto.getIsActive() : true);
        image.setDeleteStatus(1);

        // 設置住宿關聯
        if ("lodging".equals(imageDto.getTargetType())) {
            LodgingsBean lodging = lodgingsRepository.findById(imageDto.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("住宿不存在"));
            image.setLodging(lodging);
        }

        // 設置房型關聯
        if ("room_type".equals(imageDto.getTargetType())) {
            RoomTypesBean roomType = roomTypesRepository.findById(imageDto.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("房型不存在"));
            image.setRoomType(roomType);
        }

        return image;
    }

    /**
     * 轉換為 CommentImagesBean
     */
    private CommentImagesBean convertToCommentImageBean(BaseImageDTO imageDto) {
        CommentImagesBean image = new CommentImagesBean();

        if (imageDto.getImageId() != null) {
            image.setId(imageDto.getImageId());
        }

        // 設置留言關聯
        CommentsBean comment = commentsRepository.findById(imageDto.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("留言不存在"));
        image.setComment(comment);

        image.setImageUrl(imageDto.getImageUrl());
        image.setMimeType(imageDto.getMimeType());
        image.setSortOrder(imageDto.getSortOrder());
        image.setIsActive(imageDto.getIsActive() != null ? imageDto.getIsActive() : true);
        image.setDeleteStatus(1);

        return image;
    }

    /**
     * 將資料庫實體轉換為 BaseImageDTO
     * 
     * @param image 資料庫實體
     * @return BaseImageDTO
     */
    public BaseImageDTO convertToDTO(Object image) {
        if (image instanceof LodgingImagesBean) {
            return convertLodgingImageToDTO((LodgingImagesBean) image);
        } else if (image instanceof CommentImagesBean) {
            return convertCommentImageToDTO((CommentImagesBean) image);
        } else {
            throw new IllegalArgumentException("不支援的圖片實體類型");
        }
    }

    /**
     * 轉換 LodgingImagesBean 為 BaseImageDTO
     */
    private BaseImageDTO convertLodgingImageToDTO(LodgingImagesBean image) {
        BaseImageDTO dto = new BaseImageDTO();
        dto.setImageId(image.getImageId());
        dto.setImageType(image.getImageType());
        dto.setImageUrl(image.getImageUrl());
        dto.setMimeType(image.getMimeType());
        dto.setSortOrder(image.getSortOrder());
        dto.setIsActive(image.getIsActive());
        dto.setUploadedAt(image.getUploadedAt());

        if (image.getLodging() != null) {
            dto.setTargetId(image.getLodging().getId());
            dto.setTargetType("lodging");
            dto.setLodgingId(image.getLodging().getId());
        } else if (image.getRoomType() != null) {
            dto.setTargetId(image.getRoomType().getId());
            dto.setTargetType("room_type");
            dto.setRoomTypeId(image.getRoomType().getId());
        }

        return dto;
    }

    /**
     * 轉換 CommentImagesBean 為 BaseImageDTO
     */
    private BaseImageDTO convertCommentImageToDTO(CommentImagesBean image) {
        BaseImageDTO dto = new BaseImageDTO();
        dto.setImageId(image.getId());
        dto.setImageType("comment");
        dto.setTargetId(image.getComment().getId());
        dto.setTargetType("comment");
        dto.setImageUrl(image.getImageUrl());
        dto.setMimeType(image.getMimeType());
        dto.setSortOrder(image.getSortOrder());
        dto.setIsActive(image.getIsActive());
        dto.setUploadedAt(image.getUploadedAt());
        return dto;
    }

    /**
     * 批次儲存圖片
     * 
     * @param imageDtos  圖片 DTO 列表
     * @param targetType 目標類型
     * @return 儲存成功的圖片 DTO 列表
     */
    @Transactional
    public List<BaseImageDTO> saveImages(List<BaseImageDTO> imageDtos, String targetType) {
        if (imageDtos == null || imageDtos.isEmpty()) {
            return new ArrayList<>();
        }

        List<Object> imagesToSave = new ArrayList<>();

        for (BaseImageDTO imageDto : imageDtos) {
            // 跳過軟刪除的圖片
            if (imageDto.getIsActive() != null && !imageDto.getIsActive()) {
                continue;
            }

            validateImageData(imageDto);
            Object image = convertToBean(imageDto);
            imagesToSave.add(image);
        }

        // 根據目標類型選擇對應的 Repository 進行批次儲存
        List<BaseImageDTO> result = new ArrayList<>();
        switch (targetType) {
            case "lodging":
            case "room_type":
                List<LodgingImagesBean> lodgingImages = imagesToSave.stream()
                        .map(img -> (LodgingImagesBean) img)
                        .collect(Collectors.toList());
                List<LodgingImagesBean> savedLodgingImages = lodgingImagesRepository.saveAll(lodgingImages);
                for (LodgingImagesBean savedImage : savedLodgingImages) {
                    result.add(convertToDTO(savedImage));
                }
                break;
            case "comment":
                List<CommentImagesBean> commentImages = imagesToSave.stream()
                        .map(img -> (CommentImagesBean) img)
                        .collect(Collectors.toList());
                List<CommentImagesBean> savedCommentImages = commentImagesRepository.saveAll(commentImages);
                for (CommentImagesBean savedImage : savedCommentImages) {
                    result.add(convertToDTO(savedImage));
                }
                break;
            default:
                throw new IllegalArgumentException("不支援的目標類型: " + targetType);
        }

        return result;
    }

    /**
     * 更新圖片資料
     * 
     * @param imageDtos  圖片 DTO 列表
     * @param targetType 目標類型
     * @return 更新後的圖片 DTO 列表
     */
    @Transactional
    public List<BaseImageDTO> updateImages(List<BaseImageDTO> imageDtos, String targetType) {
        if (imageDtos == null || imageDtos.isEmpty()) {
            return new ArrayList<>();
        }

        List<Object> imagesToUpdate = new ArrayList<>();
        List<Object> imagesToSave = new ArrayList<>();

        for (BaseImageDTO imageDto : imageDtos) {
            if (imageDto.getImageId() != null) {
                // 更新現有圖片
                switch (targetType) {
                    case "lodging":
                    case "room_type":
                        Optional<LodgingImagesBean> existingLodgingImage = lodgingImagesRepository
                                .findById(imageDto.getImageId());
                        if (existingLodgingImage.isPresent()) {
                            LodgingImagesBean image = existingLodgingImage.get();
                            updateLodgingImage(image, imageDto);
                            imagesToUpdate.add(image);
                        } else {
                            System.out.println("⚠️ [WARNING] 找不到 lodging image ID: " + imageDto.getImageId());
                        }
                        break;
                    case "comment":
                        Optional<CommentImagesBean> existingCommentImage = commentImagesRepository
                                .findById(imageDto.getImageId());
                        if (existingCommentImage.isPresent()) {
                            CommentImagesBean image = existingCommentImage.get();
                            updateCommentImage(image, imageDto);
                            imagesToUpdate.add(image);
                        } else {
                            System.out.println("⚠️ [WARNING] 找不到 comment image ID: " + imageDto.getImageId());
                        }
                        break;
                }
            } else {
                // 新增圖片
                validateImageData(imageDto);
                Object image = convertToBean(imageDto);
                imagesToSave.add(image);
            }
        }

        // 執行更新和新增操作
        List<BaseImageDTO> result = new ArrayList<>();

        // 更新現有圖片
        for (Object image : imagesToUpdate) {
            Object savedImage = null;
            if (image instanceof LodgingImagesBean) {
                savedImage = lodgingImagesRepository.save((LodgingImagesBean) image);
            } else if (image instanceof CommentImagesBean) {
                savedImage = commentImagesRepository.save((CommentImagesBean) image);
            }
            if (savedImage != null) {
                result.add(convertToDTO(savedImage));
            }
        }

        // 新增圖片
        if (!imagesToSave.isEmpty()) {
            List<BaseImageDTO> savedImages = saveImages(
                    imagesToSave.stream()
                            .map(this::convertToDTO)
                            .collect(Collectors.toList()),
                    targetType);
            result.addAll(savedImages);
        }

        return result;
    }

    /**
     * 更新 LodgingImagesBean
     */
    private void updateLodgingImage(LodgingImagesBean image, BaseImageDTO imageDto) {
        if (imageDto.getIsActive() != null && !imageDto.getIsActive()) {
            // 軟刪除圖片
            image.setIsActive(false);
            image.setDeleteStatus(0);
            image.setDeletedTime(LocalDateTime.now());
        } else {
            // 更新圖片資料 - 只有有值才更新，避免寫入 null
            if (imageDto.getImageType() != null) {
                image.setImageType(imageDto.getImageType());
            }
            if (imageDto.getImageUrl() != null) {
                image.setImageUrl(imageDto.getImageUrl());
            }
            if (imageDto.getMimeType() != null) {
                image.setMimeType(imageDto.getMimeType());
            }
            // sortOrder 一定要更新（前端拖曳排序時會傳送）
            image.setSortOrder(imageDto.getSortOrder());
            image.setIsActive(imageDto.getIsActive() != null ? imageDto.getIsActive() : true);
            // 如果恢復圖片，清除刪除時間
            if (imageDto.getIsActive() != null && imageDto.getIsActive()) {
                image.setDeletedTime(null);
            }
        }
    }

    /**
     * 更新 CommentImagesBean
     */
    private void updateCommentImage(CommentImagesBean image, BaseImageDTO imageDto) {
        if (imageDto.getIsActive() != null && !imageDto.getIsActive()) {
            // 軟刪除圖片
            image.setIsActive(false);
            image.setDeleteStatus(0);
            image.setDeletedTime(LocalDateTime.now());
        } else {
            // 更新圖片資料 - 只有有值才更新，避免寫入 null
            if (imageDto.getImageUrl() != null) {
                image.setImageUrl(imageDto.getImageUrl());
            }
            if (imageDto.getMimeType() != null) {
                image.setMimeType(imageDto.getMimeType());
            }
            // sortOrder 一定要更新（前端拖曳排序時會傳送）
            image.setSortOrder(imageDto.getSortOrder());
            image.setIsActive(imageDto.getIsActive() != null ? imageDto.getIsActive() : true);
            // 如果恢復圖片，清除刪除時間
            if (imageDto.getIsActive() != null && imageDto.getIsActive()) {
                image.setDeletedTime(null);
            }
        }
    }

    /**
     * 儲存 MultipartFile 到本機並返回檔案路徑
     * 
     * @param file       上傳的檔案
     * @param targetType 目標類型 (lodging, room_type, comment)
     * @param targetId   目標ID
     * @return 儲存結果包含檔案路徑和MIME類型
     * @throws IOException 檔案儲存失敗時拋出
     */
    public FileUploadResult storeFile(MultipartFile file, String targetType, Integer targetId) throws IOException {
        System.out.println("📤 [DEBUG] storeFile 被呼叫:");
        System.out.println("   - targetType: " + targetType);
        System.out.println("   - targetId: " + targetId);
        System.out.println("   - 檔案名稱: " + (file != null ? file.getOriginalFilename() : "null"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("檔案不能為空");
        }

        // 建立目錄路徑
        String directoryPath = buildDirectoryPath(targetType, targetId);
        System.out.println("   - 建立的目錄路徑: " + directoryPath);
        Path directory = Paths.get(directoryPath);

        // 建立目錄（如果不存在）
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            System.out.println("   - 建立新目錄: " + directoryPath);
        } else {
            System.out.println("   - 目錄已存在: " + directoryPath);
        }

        // 生成檔案名
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new IOException("上傳的檔案沒有原始檔名");
        }
        String fileExtension = StringUtils.getFilenameExtension(originalName);
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            throw new IOException("無法取得檔案副檔名: " + originalName);
        }

        String fileName = FileNameGenerator.getNewFileName(fileExtension);
        String fullPath = directoryPath + "/" + fileName;

        // 處理圖片（壓縮和調整尺寸）
        byte[] processedImageBytes = processImageIfNeeded(file, targetType);

        // 儲存處理後的檔案
        try (InputStream inputStream = new ByteArrayInputStream(processedImageBytes)) {
            Files.copy(inputStream, Paths.get(fullPath), StandardCopyOption.REPLACE_EXISTING);
        }

        // 返回結果
        FileUploadResult result = new FileUploadResult();
        result.setFilePath(fullPath);
        result.setMimeType("image/jpeg"); // 統一轉換為 JPEG 格式
        result.setOriginalFileName(originalName);
        result.setFileSize(processedImageBytes.length);

        System.out.println("   - 原始檔案大小: " + file.getSize() + " bytes");
        System.out.println("   - 處理後檔案大小: " + processedImageBytes.length + " bytes");
        System.out.println(
                "   - 壓縮比例: " + String.format("%.1f%%", (double) processedImageBytes.length / file.getSize() * 100));

        return result;
    }

    /**
     * 儲存檔案（使用傳入的 lodgingId）
     * 
     * @param file       上傳的檔案
     * @param targetType 目標類型
     * @param targetId   目標ID
     * @param lodgingId  住宿ID（用於留言圖片）
     * @return 檔案上傳結果
     * @throws IOException 檔案儲存失敗時拋出
     */
    public FileUploadResult storeFile(MultipartFile file, String targetType, Integer targetId, Integer lodgingId)
            throws IOException {
        System.out.println("📤 [DEBUG] storeFile (with lodgingId) 被呼叫:");
        System.out.println("   - targetType: " + targetType);
        System.out.println("   - targetId: " + targetId);
        System.out.println("   - lodgingId: " + lodgingId);
        System.out.println("   - 檔案名稱: " + (file != null ? file.getOriginalFilename() : "null"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("檔案不能為空");
        }

        // 建立目錄路徑
        String directoryPath = buildDirectoryPath(targetType, targetId, lodgingId);
        System.out.println("   - 建立的目錄路徑: " + directoryPath);
        Path directory = Paths.get(directoryPath);

        // 建立目錄（如果不存在）
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            System.out.println("   - 建立新目錄: " + directoryPath);
        } else {
            System.out.println("   - 目錄已存在: " + directoryPath);
        }

        // 生成檔案名
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new IOException("上傳的檔案沒有原始檔名");
        }
        String fileExtension = StringUtils.getFilenameExtension(originalName);
        if (fileExtension == null || fileExtension.trim().isEmpty()) {
            throw new IOException("無法取得檔案副檔名: " + originalName);
        }

        String fileName = FileNameGenerator.getNewFileName(fileExtension);
        String fullPath = directoryPath + "/" + fileName;

        // 處理圖片（壓縮和調整尺寸）
        byte[] processedImageBytes = processImageIfNeeded(file, targetType);

        // 儲存處理後的檔案
        try (InputStream inputStream = new ByteArrayInputStream(processedImageBytes)) {
            Files.copy(inputStream, Paths.get(fullPath), StandardCopyOption.REPLACE_EXISTING);
        }

        // 返回結果
        FileUploadResult result = new FileUploadResult();
        result.setFilePath(fullPath);
        result.setMimeType("image/jpeg"); // 統一轉換為 JPEG 格式
        result.setOriginalFileName(originalName);
        result.setFileSize(processedImageBytes.length);

        System.out.println("   - 原始檔案大小: " + file.getSize() + " bytes");
        System.out.println("   - 處理後檔案大小: " + processedImageBytes.length + " bytes");
        System.out.println(
                "   - 壓縮比例: " + String.format("%.1f%%", (double) processedImageBytes.length / file.getSize() * 100));

        return result;
    }

    /**
     * 處理圖片（如果需要）
     * 
     * @param file       上傳的檔案
     * @param targetType 目標類型
     * @return 處理後的圖片位元組陣列
     * @throws IOException 處理失敗時拋出
     */
    private byte[] processImageIfNeeded(MultipartFile file, String targetType) throws IOException {
        // 讀取原始圖片
        BufferedImage originalImage = imageProcessor.createBufferedImage(file.getBytes());

        System.out.println("   - 原始圖片資訊: " + imageProcessor.getImageInfo(originalImage));

        // 檢查是否需要處理
        if (imageProcessor.needsProcessing(originalImage, targetType)) {
            System.out.println("   - 圖片需要處理，開始壓縮和調整尺寸...");
            byte[] processedBytes = imageProcessor.processImage(originalImage, targetType);

            // 驗證處理後的圖片
            BufferedImage processedImage = imageProcessor.createBufferedImage(processedBytes);
            System.out.println("   - 處理後圖片資訊: " + imageProcessor.getImageInfo(processedImage));

            return processedBytes;
        } else {
            System.out.println("   - 圖片不需要處理，直接使用原始檔案");
            return file.getBytes();
        }
    }

    /**
     * 批次儲存檔案並建立資料庫記錄
     * 
     * @param files      檔案列表
     * @param targetType 目標類型
     * @param targetId   目標ID
     * @param imageType  圖片類型
     * @return 儲存成功的圖片 DTO 列表
     * @throws IOException 檔案儲存失敗時拋出
     */
    @Transactional
    public List<BaseImageDTO> saveFiles(List<MultipartFile> files, String targetType, Integer targetId,
            String imageType) throws IOException {
        System.out.println("📤 [DEBUG] saveFiles 被呼叫:");
        System.out.println("   - targetType: " + targetType);
        System.out.println("   - targetId: " + targetId);
        System.out.println("   - imageType: " + imageType);
        System.out.println("   - 檔案數量: " + (files != null ? files.size() : 0));

        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<BaseImageDTO> imageDtos = new ArrayList<>();
        int sortOrder = 0;

        for (MultipartFile file : files) {
            System.out.println("   - 處理檔案: " + file.getOriginalFilename());

            // 儲存檔案到本機
            FileUploadResult uploadResult = storeFile(file, targetType, targetId);

            // 建立 BaseImageDTO
            BaseImageDTO imageDto = new BaseImageDTO();
            imageDto.setImageType(imageType);
            imageDto.setTargetId(targetId);
            imageDto.setTargetType(targetType);

            // 修正：設置正確的URL路徑而不是檔案系統路徑
            String fileName = uploadResult.getFilePath().substring(uploadResult.getFilePath().lastIndexOf("/") + 1);
            System.out.println("   - 提取的檔案名: " + fileName);
            String urlPath = buildUrlPath(targetType, targetId, fileName);
            System.out.println("   - 建立的URL路徑: " + urlPath);
            imageDto.setImageUrl(urlPath);

            imageDto.setMimeType(uploadResult.getMimeType());
            imageDto.setSortOrder(sortOrder++);
            imageDto.setIsActive(true);

            imageDtos.add(imageDto);
        }

        // 儲存到資料庫
        return saveImages(imageDtos, targetType);
    }

    /**
     * 儲存檔案（用於留言創建，需要傳入 lodgingId）
     * 
     * @param files      檔案列表
     * @param targetType 目標類型
     * @param targetId   目標ID
     * @param imageType  圖片類型
     * @param lodgingId  住宿ID（用於留言圖片）
     * @return 儲存的圖片 DTO 列表
     * @throws IOException
     */
    @Transactional
    public List<BaseImageDTO> saveFiles(List<MultipartFile> files, String targetType, Integer targetId,
            String imageType, Integer lodgingId) throws IOException {
        System.out.println("📤 [DEBUG] saveFiles (with lodgingId) 被呼叫:");
        System.out.println("   - targetType: " + targetType);
        System.out.println("   - targetId: " + targetId);
        System.out.println("   - imageType: " + imageType);
        System.out.println("   - lodgingId: " + lodgingId);
        System.out.println("   - 檔案數量: " + (files != null ? files.size() : 0));

        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<BaseImageDTO> imageDtos = new ArrayList<>();
        int sortOrder = 0;

        for (MultipartFile file : files) {
            System.out.println("   - 處理檔案: " + file.getOriginalFilename());

            // 儲存檔案到本機
            FileUploadResult uploadResult = storeFile(file, targetType, targetId, lodgingId);

            // 建立 BaseImageDTO
            BaseImageDTO imageDto = new BaseImageDTO();
            imageDto.setImageType(imageType);
            imageDto.setTargetId(targetId);
            imageDto.setTargetType(targetType);

            // 修正：設置正確的URL路徑而不是檔案系統路徑
            String fileName = uploadResult.getFilePath().substring(uploadResult.getFilePath().lastIndexOf("/") + 1);
            System.out.println("   - 提取的檔案名: " + fileName);
            String urlPath = buildUrlPathWithLodgingId(targetType, targetId, fileName, lodgingId);
            System.out.println("   - 建立的URL路徑: " + urlPath);
            imageDto.setImageUrl(urlPath);

            imageDto.setMimeType(uploadResult.getMimeType());
            imageDto.setSortOrder(sortOrder++);
            imageDto.setIsActive(true);

            imageDtos.add(imageDto);
        }

        // 儲存到資料庫
        return saveImages(imageDtos, targetType);
    }

    /**
     * 建立目錄路徑
     * 
     * @param targetType 目標類型
     * @param targetId   目標ID
     * @return 目錄路徑
     */
    private String buildDirectoryPath(String targetType, Integer targetId) {
        System.out.println("🔍 [DEBUG] buildDirectoryPath 被呼叫:");
        System.out.println("   - targetType: " + targetType);
        System.out.println("   - targetId: " + targetId);

        switch (targetType) {
            case "lodging":
                String lodgingPath = uploadDir + "lodgings/" + targetId;
                System.out.println("   - 建立 lodging 路徑: " + lodgingPath);
                return lodgingPath;
            case "room_type":
                String roomPath = uploadDir + "room_types/" + targetId;
                System.out.println("   - 建立 room_type 路徑: " + roomPath);
                return roomPath;
            case "comment":
                System.out.println("   - 處理 comment 類型，查詢留言 ID: " + targetId);
                // 留言圖片按照 lodgingId 組織目錄
                try {
                    CommentsBean comment = commentsRepository.findWithLodgingById(targetId)
                            .orElseThrow(() -> new IllegalArgumentException("找不到留言 ID: " + targetId));

                    System.out.println("   - 查詢到的 comment 物件: " + comment);
                    System.out.println("   - comment.getLodging(): " + comment.getLodging());
                    System.out.println("   - comment.getRoomType(): " + comment.getRoomType());
                    System.out.println("   - comment.getUser(): " + comment.getUser());

                    Integer lodgingId = null;
                    if (comment.getLodging() != null) {
                        lodgingId = comment.getLodging().getId();
                        System.out.println("   - 從 comment.lodging 取得 lodgingId: " + lodgingId);
                    } else if (comment.getRoomType() != null) {
                        lodgingId = comment.getRoomType().getLodging().getId();
                        System.out.println("   - 從 comment.roomType.lodging 取得 lodgingId: " + lodgingId);
                    }

                    if (lodgingId == null) {
                        System.out.println("   ❌ 錯誤：留言沒有關聯的住宿");
                        throw new IllegalArgumentException("留言沒有關聯的住宿");
                    }

                    String commentPath = uploadDir + "comments/" + lodgingId;
                    System.out.println("   - 建立 comment 路徑: " + commentPath);
                    return commentPath;
                } catch (Exception e) {
                    System.out.println("   ❌ 查詢留言失敗: " + e.getMessage());
                    // 如果查詢失敗，可能是新留言還沒有保存到資料庫
                    // 這種情況下應該使用 buildDirectoryPath(targetType, targetId, lodgingId) 方法
                    throw new IllegalArgumentException("無法處理 comment 類型，請使用帶 lodgingId 的方法", e);
                }
            default:
                System.out.println("   ❌ 錯誤：不支援的目標類型: " + targetType);
                throw new IllegalArgumentException("不支援的目標類型: " + targetType);
        }
    }

    /**
     * 建立目錄路徑（使用傳入的 lodgingId）
     * 
     * @param targetType 目標類型
     * @param targetId   目標ID
     * @param lodgingId  住宿ID
     * @return 目錄路徑
     */
    private String buildDirectoryPath(String targetType, Integer targetId, Integer lodgingId) {
        System.out.println("🔍 [DEBUG] buildDirectoryPath (with lodgingId) 被呼叫:");
        System.out.println("   - targetType: " + targetType);
        System.out.println("   - targetId: " + targetId);
        System.out.println("   - lodgingId: " + lodgingId);

        switch (targetType) {
            case "lodging":
                String lodgingPath = uploadDir + "lodgings/" + targetId;
                System.out.println("   - 建立 lodging 路徑: " + lodgingPath);
                return lodgingPath;
            case "room_type":
                String roomPath = uploadDir + "room_types/" + targetId;
                System.out.println("   - 建立 room_type 路徑: " + roomPath);
                return roomPath;
            case "comment":
                if (lodgingId == null) {
                    System.out.println("   ❌ 錯誤：留言圖片需要 lodgingId");
                    throw new IllegalArgumentException("留言圖片需要 lodgingId");
                }
                String commentPath = uploadDir + "comments/" + lodgingId;
                System.out.println("   - 建立 comment 路徑: " + commentPath);
                return commentPath;
            default:
                System.out.println("   ❌ 錯誤：不支援的目標類型: " + targetType);
                throw new IllegalArgumentException("不支援的目標類型: " + targetType);
        }
    }

    /**
     * 建立URL路徑
     * 
     * @param targetType 目標類型
     * @param targetId   目標ID
     * @param filePath   檔案系統路徑
     * @return URL路徑
     */
    private String buildUrlPath(String targetType, Integer targetId, String fileName) {
        System.out.println("🔍 [DEBUG] buildUrlPath 被呼叫:");
        System.out.println("   - targetType: " + targetType);
        System.out.println("   - targetId: " + targetId);
        System.out.println("   - fileName: " + fileName);

        String baseUrl = "https://192.168.36.96:8080/uploads/"; // 修正：返回完整的後端URL
        switch (targetType) {
            case "lodging":
                String lodgingUrl = baseUrl + "lodgings/" + targetId + "/" + fileName;
                System.out.println("   - 建立 lodging URL: " + lodgingUrl);
                return lodgingUrl;
            case "room_type":
                String roomUrl = baseUrl + "room_types/" + targetId + "/" + fileName;
                System.out.println("   - 建立 room_type URL: " + roomUrl);
                return roomUrl;
            case "comment":
                System.out.println("   - 處理 comment 類型，查詢留言 ID: " + targetId);
                // 留言圖片URL按照 lodgingId 組織
                CommentsBean comment = commentsRepository.findWithLodgingById(targetId)
                        .orElseThrow(() -> new IllegalArgumentException("找不到留言 ID: " + targetId));

                Integer lodgingId = null;
                if (comment.getLodging() != null) {
                    lodgingId = comment.getLodging().getId();
                    System.out.println("   - 從 comment.lodging 取得 lodgingId: " + lodgingId);
                } else if (comment.getRoomType() != null) {
                    lodgingId = comment.getRoomType().getLodging().getId();
                    System.out.println("   - 從 comment.roomType.lodging 取得 lodgingId: " + lodgingId);
                }

                if (lodgingId == null) {
                    System.out.println("   ❌ 錯誤：留言沒有關聯的住宿");
                    throw new IllegalArgumentException("留言沒有關聯的住宿");
                }

                String commentUrl = baseUrl + "comments/" + lodgingId + "/" + fileName;
                System.out.println("   - 建立 comment URL: " + commentUrl);
                return commentUrl;
            default:
                System.out.println("   ❌ 錯誤：不支援的目標類型: " + targetType);
                throw new IllegalArgumentException("不支援的目標類型: " + targetType);
        }
    }

    /**
     * 建立URL路徑（使用傳入的 lodgingId）
     * 
     * @param targetType 目標類型
     * @param targetId   目標ID
     * @param fileName   檔案名稱
     * @param lodgingId  住宿ID
     * @return URL路徑
     */
    private String buildUrlPathWithLodgingId(String targetType, Integer targetId, String fileName, Integer lodgingId) {
        System.out.println("🔍 [DEBUG] buildUrlPathWithLodgingId 被呼叫:");
        System.out.println("   - targetType: " + targetType);
        System.out.println("   - targetId: " + targetId);
        System.out.println("   - fileName: " + fileName);
        System.out.println("   - lodgingId: " + lodgingId);

        String baseUrl = "https://192.168.36.96:8080/uploads/"; // 修正：返回完整的後端URL
        switch (targetType) {
            case "lodging":
                String lodgingUrl = baseUrl + "lodgings/" + targetId + "/" + fileName;
                System.out.println("   - 建立 lodging URL: " + lodgingUrl);
                return lodgingUrl;
            case "room_type":
                String roomUrl = baseUrl + "room_types/" + targetId + "/" + fileName;
                System.out.println("   - 建立 room_type URL: " + roomUrl);
                return roomUrl;
            case "comment":
                if (lodgingId == null) {
                    System.out.println("   ❌ 錯誤：留言圖片需要 lodgingId");
                    throw new IllegalArgumentException("留言圖片需要 lodgingId");
                }
                String commentUrl = baseUrl + "comments/" + lodgingId + "/" + fileName;
                System.out.println("   - 建立 comment URL: " + commentUrl);
                return commentUrl;
            default:
                System.out.println("   ❌ 錯誤：不支援的目標類型: " + targetType);
                throw new IllegalArgumentException("不支援的目標類型: " + targetType);
        }
    }

    /**
     * 刪除檔案
     * 
     * @param filePath 檔案路徑
     * @return 是否刪除成功
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                return true;
            }
            return false;
        } catch (IOException e) {
            System.err.println("刪除檔案失敗: " + filePath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 檔案上傳結果類別
     */
    public static class FileUploadResult {
        private String filePath;
        private String mimeType;
        private String originalFileName;
        private long fileSize;

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getMimeType() {
            return mimeType;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public void setOriginalFileName(String originalFileName) {
            this.originalFileName = originalFileName;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }
    }

    /**
     * 根據目標ID和類型查詢有效圖片
     * 
     * @param targetId   目標ID
     * @param targetType 目標類型
     * @return 有效圖片列表
     */
    public List<BaseImageDTO> getActiveImagesByTarget(Integer targetId, String targetType) {
        List<BaseImageDTO> result = new ArrayList<>();

        switch (targetType) {
            case "lodging":
                List<LodgingImagesBean> lodgingImages = lodgingImagesRepository
                        .findByLodgingIdAndIsActiveTrueAndDeleteStatus(targetId, 1);
                for (LodgingImagesBean image : lodgingImages) {
                    result.add(convertToDTO(image));
                }
                break;
            case "room_type":
                List<LodgingImagesBean> roomImages = lodgingImagesRepository
                        .findByRoomTypeIdAndIsActiveTrueAndDeleteStatus(targetId, 1);
                for (LodgingImagesBean image : roomImages) {
                    result.add(convertToDTO(image));
                }
                break;
            case "comment":
                List<CommentImagesBean> commentImages = commentImagesRepository.findActiveImagesByCommentId(targetId);
                for (CommentImagesBean image : commentImages) {
                    result.add(convertToDTO(image));
                }
                break;
            default:
                throw new IllegalArgumentException("不支援的目標類型: " + targetType);
        }

        return result;
    }

    /**
     * 根據目標ID和類型查詢所有圖片（包括無效的）
     * 
     * @param targetId   目標ID
     * @param targetType 目標類型
     * @return 所有圖片列表
     */
    public List<BaseImageDTO> getAllImagesByTarget(Integer targetId, String targetType) {
        List<BaseImageDTO> result = new ArrayList<>();

        switch (targetType) {
            case "lodging":
                List<LodgingImagesBean> lodgingImages = lodgingImagesRepository
                        .findByLodgingId(targetId); // 查詢所有圖片，包括已軟刪除的
                for (LodgingImagesBean image : lodgingImages) {
                    result.add(convertToDTO(image));
                }
                break;
            case "room_type":
                List<LodgingImagesBean> roomImages = lodgingImagesRepository
                        .findByRoomTypeId(targetId); // 查詢所有圖片，包括已軟刪除的
                for (LodgingImagesBean image : roomImages) {
                    result.add(convertToDTO(image));
                }
                break;
            case "comment":
                List<CommentImagesBean> commentImages = commentImagesRepository.findAllImagesByCommentId(targetId);
                for (CommentImagesBean image : commentImages) {
                    result.add(convertToDTO(image));
                }
                break;
            default:
                throw new IllegalArgumentException("不支援的目標類型: " + targetType);
        }

        return result;
    }
}