package journey.domain.orders;

import journey.domain.payments.PaymentsRecordBean;

// 定義了一個所有訂單共同實作的界面，當中可以get或set所有訂單都會有的一些共同資料欄位
public interface OrderEntityInterface {
    Integer getOrderIdForPayment();

    String getOrderNoForPayment();

    Boolean getStatusForPayment();

    void setStatusForPayment(String status);

    // // 可以根據需要添加其他所有訂單都共有的方法，例如：
    // Integer getTotalAmountForPayment(); // 如果需要從 OrderEntityInterface 獲取總金額
    // String getItemNameForPayment(); // 如果需要從 OrderEntityInterface 獲取商品名稱
    // String getDescriptionForPayment(); // 如果需要從 OrderEntityInterface 獲取描述
    // String getPaymentMethodForPayment(); // 如果需要從 OrderEntityInterface 獲取支付方式

    // LocalDateTime getCreatedAtForPayment(); // 如果所有訂單都有創建時間

    void setPaymentIdForPayment(PaymentsRecordBean paymentRecord);
}
