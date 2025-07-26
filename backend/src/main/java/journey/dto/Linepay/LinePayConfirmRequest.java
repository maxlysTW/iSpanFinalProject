package journey.dto.Linepay;

// Line Pay Confirm Request DTO
public class LinePayConfirmRequest {
    private int amount;
    private String currency;

    public LinePayConfirmRequest() {
    }

    public LinePayConfirmRequest(int amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
