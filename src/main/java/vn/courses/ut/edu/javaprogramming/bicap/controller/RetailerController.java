package vn.courses.ut.edu.javaprogramming.bicap.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import vn.courses.ut.edu.javaprogramming.bicap.dto.*;
import vn.courses.ut.edu.javaprogramming.bicap.service.RetailerProfileService;

@RestController
@RequestMapping("/api/retailer")
@PreAuthorize("hasRole('RETAILER')")
public class RetailerController {
    private final RetailerProfileService retailerProfileService;

    public RetailerController(RetailerProfileService retailerProfileService) {
        this.retailerProfileService = retailerProfileService;
    }

    @GetMapping("/profile")
    public ResponseEntity<RetailerProfileResponse> getProfile() {
        return ResponseEntity.ok(retailerProfileService.getProfile());
    }

    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RetailerProfileResponse> updateProfile(
            @Valid @ModelAttribute RetailerProfileRequest request) {
        return ResponseEntity.ok(retailerProfileService.updateProfile(request));
    }

    @GetMapping("/business-profile")
    public ResponseEntity<RetailerBusinessResponse> getBusinessProfile() {
        return ResponseEntity.ok(retailerProfileService.getBusinessProfile());
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RetailerBusinessResponse> updateBusinessProfile(
            @Valid @ModelAttribute RetailerBusinessRequest request) {
        return ResponseEntity.ok(retailerProfileService.updateBusinessProfile(request));
    }
}
