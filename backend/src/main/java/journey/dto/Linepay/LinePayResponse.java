package journey.dto.Linepay;

// Line Pay Response DTO (Request API)
public class LinePayResponse {
    private String returnCode;
    private String returnMessage;
    private Info info;

    public LinePayResponse() {
    }

    public String getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(String returnCode) {
        this.returnCode = returnCode;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    public void setReturnMessage(String returnMessage) {
        this.returnMessage = returnMessage;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public static class Info {
        private PaymentUrl paymentUrl; // LINE Pay 返回的 "paymentUrl" 是一個物件，直接映射到此
        private String transactionId; // LINE Pay 交易 ID

        public Info() {
        }

        public PaymentUrl getPaymentUrl() { // 將這個 getter 的名字改為 getPaymentUrl，直接返回 PaymentUrl 物件
            return paymentUrl;
        }

        public void setPaymentUrl(PaymentUrl paymentUrl) { // 這裡的 setter 應該接收 PaymentUrl 物件
            this.paymentUrl = paymentUrl;
        }

        public String getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(String transactionId) {
            this.transactionId = transactionId;
        }

        public String getWebPaymentUrl() {
            return paymentUrl != null ? paymentUrl.getWeb() : null;
        }

        public static class PaymentUrl {
            private String web; // 網頁支付 URL
            private String app; // App 支付 URL QRcode掃碼

            public String getWeb() {
                return web;
            }

            public void setWeb(String web) {
                this.web = web;
            }

            public String getApp() {
                return app;
            }

            public void setApp(String app) {
                this.app = app;
            }

            public PaymentUrl() {
            }
        }
    }
}
