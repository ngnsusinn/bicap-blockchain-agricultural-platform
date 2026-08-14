package vn.courses.ut.edu.javaprogramming.bicap.service;

import java.math.BigDecimal;

/** Integration boundary owned by BICAP-16; BICAP-14/15 can replace only this adapter. */
public interface FarmingSeasonExportGateway {
    SeasonSnapshot requireHarvested(Long farmId, Long seasonId);
    record SeasonSnapshot(Long id, String name, BigDecimal harvestedQuantity, String harvestUnit) {}
}
