package vn.courses.ut.edu.javaprogramming.bicap.entity;

/**
 * Product visibility/state status (BICAP-5 / SRS-ADM-004).
 * ACTIVE         - product is visible and available on the platform
 * INACTIVE       - admin-disabled product (hidden from marketplace)
 * PENDING_REVIEW - flagged by admin for review (violation, data issue)
 */
public enum ProductStatus {
    ACTIVE,
    INACTIVE,
    PENDING_REVIEW
}
