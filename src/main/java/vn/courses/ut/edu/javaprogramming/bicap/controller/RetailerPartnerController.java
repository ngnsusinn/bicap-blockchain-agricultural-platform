package vn.courses.ut.edu.javaprogramming.bicap.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.courses.ut.edu.javaprogramming.bicap.dto.RetailerPartnerDetailResponse;
import vn.courses.ut.edu.javaprogramming.bicap.dto.RetailerPartnerResponse;
import vn.courses.ut.edu.javaprogramming.bicap.service.RetailerPartnerService;

import java.util.List;

/**
 * Thông tin Nhà bán lẻ đã ký hợp đồng với nông trại của Farm Manager (BICAP-21 / SRS-FM-015,
 * detail-design §4.3 farm-web → Retailers).
 *
 * <p>Portal endpoints — xác thực qua JWT ({@code CurrentUser}) và giới hạn FARM_MANAGER
 * trong service. Trả về danh sách đối tác hoặc chi tiết kèm lịch sử giao dịch.
 */
@RestController
@RequestMapping("/api/retailers")
public class RetailerPartnerController {

    private final RetailerPartnerService retailerPartnerService;

    public RetailerPartnerController(RetailerPartnerService retailerPartnerService) {
        this.retailerPartnerService = retailerPartnerService;
    }

    /** Danh sách Nhà bán lẻ đã có giao dịch trên nông trại của Farm Manager. */
    @GetMapping
    public ResponseEntity<List<RetailerPartnerResponse>> getContractRetailers() {
        return ResponseEntity.ok(retailerPartnerService.getContractRetailers());
    }

    /** Chi tiết đối tác: thông tin kinh doanh + lịch sử giao dịch. */
    @GetMapping("/{id}")
    public ResponseEntity<RetailerPartnerDetailResponse> getContractRetailerDetail(@PathVariable Long id) {
        return ResponseEntity.ok(retailerPartnerService.getContractRetailerDetail(id));
    }
}
