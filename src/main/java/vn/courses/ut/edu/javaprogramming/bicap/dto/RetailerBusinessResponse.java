package vn.courses.ut.edu.javaprogramming.bicap.dto;

import vn.courses.ut.edu.javaprogramming.bicap.entity.BusinessType;
import vn.courses.ut.edu.javaprogramming.bicap.entity.RetailerBusinessProfile;

public record RetailerBusinessResponse(
        Long id,
        String businessName,
        String address,
        BusinessType businessType,
        String licenseUrl
) {
    public static RetailerBusinessResponse from(RetailerBusinessProfile profile) {
        return new RetailerBusinessResponse(
                profile.getId(), profile.getBusinessName(), profile.getAddress(),
                profile.getBusinessType(), profile.getLicenseUrl()
        );
    }
}
