package journey.controller.comments;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import journey.dto.Comments.CommentReportRequestDTO;
import journey.dto.Comments.CommentReportResponseDTO;
import journey.dto.Comments.CommentReportUpdateDTO;
import journey.dto.Comments.CommentSimpleResponseDTO;
import journey.dto.user.JwtDTO;
import journey.enums.UserTypeEnum;
import journey.service.comments.CommentReportsService;
import journey.utils.JwtUtil;

/**
 * 檢舉管理員端點
 * 
 * @author Fan
 * @version 1.0
 * @since 07/11/2025
 */
@RestController
@RequestMapping("/api/comment-reports")
public class CommentReportsController {

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    private CommentReportsService service;

    /**
     * 管理員查詢所有檢舉（包含留言詳細資訊）
     * 
     * @param status 檢舉狀態篩選（可選）
     * @param page   頁碼（預設 0）
     * @param size   每頁大小（預設 20）
     * @return 檢舉列表
     */
    @GetMapping("/admin")
    public ResponseEntity<?> getReportsForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") String authHeader) {

        try {

            // 驗證並解析 token
            String token = jwtUtil.extractToken(authHeader);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new CommentSimpleResponseDTO("缺少或格式錯誤的 Authorization Header"));
            }

            JwtDTO jwt = jwtUtil.verfiy(token);
            if (jwt == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new CommentSimpleResponseDTO("Token 驗證失敗"));
            }

            System.out.println("👨‍💼 管理員查詢檢舉列表:");
            System.out.println("   - 查詢者: " + jwt.getUsername() + " (ID: " + jwt.getId() + ")");
            System.out.println("   - 狀態篩選: " + (status != null ? status : "全部"));
            System.out.println("   - 頁碼: " + page);
            System.out.println("   - 每頁大小: " + size);

            // 權限檢查
            if (jwt.getUserType() != UserTypeEnum.ADMIN) {
                System.err.println("❌ 權限不足: 只有管理員可以查詢檢舉列表");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new CommentSimpleResponseDTO("權限不足: 只有管理員可以查詢檢舉列表"));
            }

            List<CommentReportResponseDTO> reports = service.getReportsForAdmin(status, page, size);

            System.out.println("✅ 管理員查詢檢舉成功，共 " + reports.size() + " 筆");
            return ResponseEntity.ok(reports);

        } catch (Exception e) {
            System.err.println("❌ 管理員查詢檢舉失敗: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommentSimpleResponseDTO("系統錯誤，請稍後再試"));
        }
    }

    /**
     * 管理員處理檢舉（確認/駁回）
     * 
     * @param reportId  檢舉 ID
     * @param updateDto 更新資料
     * @return 處理結果
     */
    @PutMapping("/admin/{reportId}")
    public ResponseEntity<?> processReport(
            @PathVariable Integer reportId,
            @RequestBody CommentReportUpdateDTO updateDto,
            @RequestHeader("Authorization") String authHeader) {

        try {

            // 驗證並解析 token
            String token = jwtUtil.extractToken(authHeader);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new CommentSimpleResponseDTO("缺少或格式錯誤的 Authorization Header"));
            }

            JwtDTO jwt = jwtUtil.verfiy(token);
            if (jwt == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new CommentSimpleResponseDTO("Token 驗證失敗"));
            }

            System.out.println("👨‍💼 管理員處理檢舉:");
            System.out.println("   - 檢舉ID: " + reportId);
            System.out.println("   - 新狀態: " + updateDto.status());
            System.out.println("   - 備註: " + updateDto.note());

            // 權限檢查
            if (jwt.getUserType() != UserTypeEnum.ADMIN) {
                System.err.println("❌ 權限不足: 只有管理員可以處理檢舉");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new CommentSimpleResponseDTO("權限不足: 只有管理員可以處理檢舉"));
            }

            service.processReport(reportId, updateDto, jwt.getId());

            System.out.println("✅ 檢舉處理成功");
            return ResponseEntity.ok(new CommentSimpleResponseDTO("檢舉處理成功"));

        } catch (IllegalArgumentException e) {
            System.err.println("❌ 檢舉處理失敗 - 參數錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommentSimpleResponseDTO("參數錯誤: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ 檢舉處理失敗 - 系統錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommentSimpleResponseDTO("系統錯誤，請稍後再試"));
        }
    }

    /**
     * 提交檢舉
     * 
     * @param req       檢舉請求
     * @param principal 當前用戶
     * @return 檢舉結果
     */
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody CommentReportRequestDTO req,
            @RequestHeader("Authorization") String authHeader) {

        try {

            // 驗證並解析 token
            String token = jwtUtil.extractToken(authHeader);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new CommentSimpleResponseDTO("缺少或格式錯誤的 Authorization Header"));
            }

            JwtDTO jwt = jwtUtil.verfiy(token);
            if (jwt == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new CommentSimpleResponseDTO("Token 驗證失敗"));
            }

            System.out.println("🚨 收到檢舉請求:");
            System.out.println("   - 留言ID: " + req.commentId());
            System.out.println("   - 檢舉原因: " + req.reasonIds());
            System.out.println("   - 檢舉者: " + jwt.getId() + " (" + jwt.getUsername() + ")");

            // 權限檢查
            if (jwt.getUserType() == UserTypeEnum.ADMIN) {
                System.out.println("❌ 管理員不能檢舉留言");
                throw new AccessDeniedException("管理員不能檢舉留言");
            }

            service.create(req, jwt.getId(), jwt.getUserType());

            System.out.println("✅ 檢舉提交成功");
            return ResponseEntity.ok(new CommentSimpleResponseDTO("檢舉已提交"));

        } catch (AccessDeniedException e) {
            System.err.println("❌ 檢舉失敗 - 權限不足: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommentSimpleResponseDTO("權限不足: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            System.err.println("❌ 檢舉失敗 - 參數錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new CommentSimpleResponseDTO("參數錯誤: " + e.getMessage()));
        } catch (IllegalStateException e) {
            System.err.println("❌ 檢舉失敗 - 狀態錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new CommentSimpleResponseDTO("狀態錯誤: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("❌ 檢舉失敗 - 系統錯誤: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommentSimpleResponseDTO("系統錯誤，請稍後再試"));
        }
    }
}
