package vn.courses.ut.edu.javaprogramming.bicap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Retailer xác nhận nhận hàng (BICAP-51 / SRS-RT-016).
 *
 * <p>{@code accepted=true} (mặc định) hoàn tất đơn; {@code false} gửi khiếu nại
 * (đơn giữ {@code DELIVERED}). Rating và nhận xét là tuỳ chọn.
 */
public class CompleteOrderRequest {

    /** true = chấp nhận hàng, false = khiếu nại. Null → true. */
    private Boolean accepted = true;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;

    public CompleteOrderRequest() {}

    public CompleteOrderRequest(Boolean accepted, Integer rating, String comment) {
        this.accepted = accepted;
        this.rating = rating;
        this.comment = comment;
    }

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public boolean isAccepted() {
        return accepted == null || Boolean.TRUE.equals(accepted);
    }
}
