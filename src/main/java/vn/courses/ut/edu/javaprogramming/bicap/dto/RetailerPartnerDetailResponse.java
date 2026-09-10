package vn.courses.ut.edu.javaprogramming.bicap.dto;

import java.util.List;

/**
 * Chi tiết một Nhà bán lẻ đã ký hợp đồng với nông trại của Farm Manager (BICAP-21 /
 * SRS-FM-015): thông tin tóm tắt ({@link RetailerPartnerResponse}) kèm lịch sử giao
 * dịch ({@link RetailerTransactionResponse}) để Farm Manager đánh giá đối tác.
 */
public class RetailerPartnerDetailResponse {
    private RetailerPartnerResponse retailer;
    private List<RetailerTransactionResponse> transactions;

    public RetailerPartnerDetailResponse() {
    }

    public RetailerPartnerDetailResponse(RetailerPartnerResponse retailer,
                                         List<RetailerTransactionResponse> transactions) {
        this.retailer = retailer;
        this.transactions = transactions;
    }

    public RetailerPartnerResponse getRetailer() { return retailer; }
    public void setRetailer(RetailerPartnerResponse retailer) { this.retailer = retailer; }
    public List<RetailerTransactionResponse> getTransactions() { return transactions; }
    public void setTransactions(List<RetailerTransactionResponse> transactions) { this.transactions = transactions; }
}
