package journey.controller.payment;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import journey.dto.Linepay.LinePayConfirmResponse;
import journey.service.payment.LinePayService;

@Controller
@RequestMapping("/api/linepay")
public class LinePayCallbackController {

    private static final Logger logger = Logger.getLogger(LinePayCallbackController.class.getName());
    private final LinePayService linePayService;

    @Value("${linepay.return-url}")
    private String frontendReturnUrl; // 前端返回URL，通常由 LinePayService 配置並用於 redirect

    public LinePayCallbackController(LinePayService linePayService) {
        this.linePayService = linePayService;
    }

    /*
     * LINE Pay 支付確認 (confirmUrl)
     * 這是 LINE Pay 服務器會呼叫的後端 URL，用於確認支付狀態
     */

    // 現在這邊是寫死的，不會發生驗證行為
    @GetMapping("/confirm")
    public String linePayConfirm(@RequestParam("transactionId") String transactionId,
            @RequestParam("orderId") String orderNo,
            // 移除：@RequestParam("amount") int amount, // 這個參數不再需要(ps: 金額用view表計算)
            @RequestParam(value = "regKey", required = false) String regKey,
            Model model) {
        logger.info("LINE Pay Confirm Callback received. transactionId: " + transactionId + ", orderNo: " + orderNo);

        // 前端支付結果頁面的 URL
        final String FRONTEND_PAYMENT_RESULT_URL = "http://192.168.36.96:5173/payment-result";

        try {
            // 移除 amount 參數(ps: 金額用view表計算)，並將 orderId 參數傳遞為 orderNo (String)
            LinePayConfirmResponse confirmResponse = linePayService.confirmPayment(transactionId, orderNo);

            if ("0000".equals(confirmResponse.getReturnCode())) {
                logger.info("LINE Pay payment confirmed successfully for transactionId: " + transactionId);
                // 支付成功，導向前端的成功頁面
                return "redirect:" + FRONTEND_PAYMENT_RESULT_URL +
                        "?paymentMethod=" + URLEncoder.encode("linepay", StandardCharsets.UTF_8) +
                        "&status=" + URLEncoder.encode("success", StandardCharsets.UTF_8) +
                        "&message=" + URLEncoder.encode("支付成功！感謝您的購買。", StandardCharsets.UTF_8) +
                        "&orderId=" + URLEncoder.encode(orderNo, StandardCharsets.UTF_8) +
                        "&tradeNo=" + URLEncoder.encode(transactionId, StandardCharsets.UTF_8);
            } else {
                logger.warning("LINE Pay payment confirmation failed for transactionId: " + transactionId
                        + " with message: " + confirmResponse.getReturnMessage());
                // 支付失敗，導到前端的失敗頁面
                return "redirect:" + FRONTEND_PAYMENT_RESULT_URL +
                        "?paymentMethod=" + URLEncoder.encode("linepay", StandardCharsets.UTF_8) +
                        "&status=" + URLEncoder.encode("fail", StandardCharsets.UTF_8) +
                        "&message=" + URLEncoder.encode(confirmResponse.getReturnMessage(), StandardCharsets.UTF_8) +
                        "&orderId=" + URLEncoder.encode(orderNo, StandardCharsets.UTF_8) +
                        "&tradeNo=" + URLEncoder.encode(transactionId, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.severe("Error confirming LINE Pay payment: " + e.getMessage());
            e.printStackTrace();
            // 處理異常，導到前端的錯誤頁面
            return "redirect:" + FRONTEND_PAYMENT_RESULT_URL +
                    "?paymentMethod=" + URLEncoder.encode("linepay", StandardCharsets.UTF_8) +
                    "&status=" + URLEncoder.encode("error", StandardCharsets.UTF_8) +
                    "&message=" + URLEncoder.encode("內部服務器錯誤。", StandardCharsets.UTF_8) +
                    "&orderId=" + URLEncoder.encode(orderNo, StandardCharsets.UTF_8);
        }
    }

    /*
     * LINE Pay 支付取消 (cancelUrl) - 用戶在 LINE Pay 頁面取消支付時會被呼叫
     */

    // 以下我也不知道怎麼demo，好像要裝電腦版的才有?手機的不確定會部會有這個狀態顯示，你要取消其實直接返回上一頁就可以了?
    @GetMapping("/cancel")
    public String linePayCancel(@RequestParam("orderId") String orderNo) {
        logger.info("LINE Pay Cancel Callback received for orderNo: " + orderNo);
        final String FRONTEND_PAYMENT_CANCEL_URL = "http://192.168.36.96:5173/payment-cancel";
        return "redirect:" + FRONTEND_PAYMENT_CANCEL_URL + "?orderId="
                + URLEncoder.encode(orderNo, StandardCharsets.UTF_8);
    }
}