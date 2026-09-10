package vn.courses.ut.edu.javaprogramming.bicap.entity;

/**
 * Status of a farm registration (SRS-ADM-002 / BICAP-3).
 * PENDING  - awaiting admin approval
 * APPROVED - registration accepted by admin
 * REJECTED - registration declined by admin (farm may resubmit later)
 * SUSPENDED- temporarily disabled by admin (management flow, BICAP-4)
 * INACTIVE - permanently disabled
 */
public enum FarmStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED,
    INACTIVE
}
