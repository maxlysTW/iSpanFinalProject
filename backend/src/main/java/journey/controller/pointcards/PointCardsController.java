package journey.controller.pointcards;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import journey.service.pointcards.PointCardsService;
import journey.service.pointcards.PointCardsService.InsufficientPointsException;

@RestController
@RequestMapping("/api/points")
public class PointCardsController {

    private final PointCardsService pointCardsService;

    @Autowired
    public PointCardsController(PointCardsService pointCardsService) {
        this.pointCardsService = pointCardsService;
    }

    /**
     * 獲取固定用戶 (user_id = 1) 的總點數。
     * 路徑先不包含 userId 變數。
     */
    @GetMapping("/total") // <--- 之後後面要補上 {userId}
    public ResponseEntity<Map<String, Integer>> getUserTotalPoints() { // 之後要來改成 @PathVariable Integer userId
        Integer totalPoints = pointCardsService.getUserTotalPoints(1); // 先寫死，之後要來修改
        return ResponseEntity.ok(Map.of("totalPoints", totalPoints));
    }

    /**
     * 為固定用戶 (user_id = 1) 新增點數。
     * PointAdditionRequest 中先不需要有 userId 欄位。
     */
    @PostMapping("/add")
    public ResponseEntity<String> addPointsToUser(@RequestBody PointAdditionRequest request) {
        try {
            // PointAdditionRequest 中先不需要有 userId 字段
            pointCardsService.addPoints(1, request.getPointsToAdd(), request.getSource(), request.getPaymentId()); // 先寫死，之後要來修改
            return ResponseEntity.ok("點數新增成功。");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 為固定用戶 (user_id = 1) 扣除點數，用於前端商品兌換或抽獎後更新。
     * PointDeductionRequest 中先不需要有 userId 欄位。
     */
    @PostMapping("/deduct") // 或 /exchange，取決於您的命名偏好
    public ResponseEntity<String> deductPointsFromUser(@RequestBody PointDeductionRequest request) {
        // 先寫死 user_id 為 1，之後需要根據登入使用者動態獲取
        Integer fixedUserId = 1;

        try {
            pointCardsService.deductPoints(fixedUserId, request.getPointsToDeduct(), request.getReason(),
                    request.getItemId());
            return ResponseEntity.ok("點數扣除成功。");
        } catch (InsufficientPointsException e) {
            // 點數不足的自定義異常處理
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("點數不足：" + e.getMessage());
        } catch (RuntimeException e) {
            // 其他運行時錯誤
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("點數扣除失敗：" + e.getMessage());
        }
    }

    static class PointAdditionRequest {
        // private Integer userId; // 這部分有登入功能之後，要解註解加回來

        private Integer pointsToAdd;
        private String source;
        private Integer paymentId;

        // public Integer getUserId() { return userId; } // 這部分有登入功能之後，要解註解加回來
        // public void setUserId(Integer userId) { this.userId = userId; } //
        // 這部分有登入功能之後，要解註解加回來

        public Integer getPointsToAdd() {
            return pointsToAdd;
        }

        public void setPointsToAdd(Integer pointsToAdd) {
            this.pointsToAdd = pointsToAdd;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Integer getPaymentId() {
            return paymentId;
        }

        public void setPaymentId(Integer paymentId) {
            this.paymentId = paymentId;
        }
    }

    // 新增的點數扣除請求體類別
    static class PointDeductionRequest {
        // private Integer userId; // 這部分有登入功能之後，要解註解加回來

        private Integer pointsToDeduct; // 要扣除的點數數量
        private String reason; // 扣除原因，例如 "商品兌換", "抽獎"
        private String itemId; // 兌換的商品ID或抽獎相關ID (可選)

        // public Integer getUserId() { return userId; }
        // public void setUserId(Integer userId) { this.userId = userId; }

        public Integer getPointsToDeduct() {
            return pointsToDeduct;
        }

        public void setPointsToDeduct(Integer pointsToDeduct) {
            this.pointsToDeduct = pointsToDeduct;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }
    }
}