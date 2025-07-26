package journey.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import journey.domain.comments.CommentImagesBean;
import journey.repository.comments.CommentImagesRepository;

/**
 * 圖片URL修復服務
 * 用於修復資料庫中現有的圖片URL格式問題
 * 
 * @author Journey Team
 * @version 1.0
 * @since 2025-07-09
 */
@Service
public class ImageUrlFixService {

    @Autowired
    private CommentImagesRepository commentImagesRepository;

    /**
     * 修復所有留言圖片的URL格式
     * 將相對路徑轉換為完整的後端URL
     */
    @Transactional
    public void fixAllCommentImageUrls() {
        System.out.println("🔧 開始修復留言圖片URL...");

        // 獲取所有有效的留言圖片
        List<CommentImagesBean> allImages = commentImagesRepository.findAll();

        int fixedCount = 0;
        int skippedCount = 0;

        for (CommentImagesBean image : allImages) {
            String originalUrl = image.getImageUrl();

            // 檢查是否需要修復
            if (needsUrlFix(originalUrl)) {
                String fixedUrl = fixImageUrl(originalUrl, image.getComment().getId());
                image.setImageUrl(fixedUrl);
                commentImagesRepository.save(image);

                System.out.println("✅ 修復圖片URL: " + originalUrl + " -> " + fixedUrl);
                fixedCount++;
            } else {
                System.out.println("⏭️ 跳過已正確的URL: " + originalUrl);
                skippedCount++;
            }
        }

        System.out.println("🎯 修復完成:");
        System.out.println("   - 修復數量: " + fixedCount);
        System.out.println("   - 跳過數量: " + skippedCount);
        System.out.println("   - 總計: " + allImages.size());
    }

    /**
     * 檢查URL是否需要修復
     */
    private boolean needsUrlFix(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        // 如果已經是完整URL，不需要修復
        if (url.startsWith("http://192.168.36.96:8080/")) {
            return false;
        }

        // 如果是相對路徑，需要修復
        if (url.startsWith("/uploads/") || url.startsWith("uploads/")) {
            return true;
        }

        // 如果是其他格式的錯誤URL，也需要修復
        if (url.contains("192.168.36.96:5173") || url.contains("/hotel/")) {
            return true;
        }

        return false;
    }

    /**
     * 修復圖片URL
     */
    private String fixImageUrl(String originalUrl, Integer commentId) {
        // 提取檔案名
        String fileName = extractFileName(originalUrl);

        // 構建正確的URL
        return "http://192.168.36.96:8080/uploads/comments/" + commentId + "/" + fileName;
    }

    /**
     * 從URL中提取檔案名
     */
    private String extractFileName(String url) {
        if (url == null || url.isEmpty()) {
            return "";
        }

        // 移除查詢參數
        String cleanUrl = url.split("\\?")[0];

        // 提取檔案名
        String[] parts = cleanUrl.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }

        return "";
    }

    /**
     * 修復指定留言的圖片URL
     */
    @Transactional
    public void fixCommentImageUrls(Integer commentId) {
        System.out.println("🔧 開始修復留言 " + commentId + " 的圖片URL...");

        List<CommentImagesBean> images = commentImagesRepository.findActiveImagesByCommentId(commentId);

        int fixedCount = 0;

        for (CommentImagesBean image : images) {
            String originalUrl = image.getImageUrl();

            if (needsUrlFix(originalUrl)) {
                String fixedUrl = fixImageUrl(originalUrl, commentId);
                image.setImageUrl(fixedUrl);
                commentImagesRepository.save(image);

                System.out.println("✅ 修復圖片URL: " + originalUrl + " -> " + fixedUrl);
                fixedCount++;
            }
        }

        System.out.println("🎯 留言 " + commentId + " 修復完成，共修復 " + fixedCount + " 張圖片");
    }
}