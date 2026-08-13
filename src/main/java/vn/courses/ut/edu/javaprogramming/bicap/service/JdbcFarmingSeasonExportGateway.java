package vn.courses.ut.edu.javaprogramming.bicap.service;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.courses.ut.edu.javaprogramming.bicap.exception.BadRequestException;
import vn.courses.ut.edu.javaprogramming.bicap.exception.ResourceNotFoundException;

@Component
public class JdbcFarmingSeasonExportGateway implements FarmingSeasonExportGateway {
    private final JdbcTemplate jdbc;
    public JdbcFarmingSeasonExportGateway(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public SeasonSnapshot requireHarvested(Long farmId, Long seasonId) {
        try {
            var values = jdbc.query("""
                    select id, name, harvested_quantity, harvest_unit, status
                    from farming_seasons where id = ? and farm_id = ? for update
                    """, (rs, row) -> new Object[] { rs.getLong("id"), rs.getString("name"),
                            rs.getBigDecimal("harvested_quantity"), rs.getString("harvest_unit"),
                            rs.getString("status") }, seasonId, farmId);
            if (values.isEmpty()) throw new ResourceNotFoundException("Harvested farming season not found");
            Object[] value = values.getFirst();
            if (!"HARVESTED".equals(value[4])) throw new BadRequestException("Only HARVESTED seasons can be exported");
            if (value[2] == null) throw new BadRequestException("Harvest quantity is not available for this season");
            return new SeasonSnapshot((Long) value[0], (String) value[1],
                    (java.math.BigDecimal) value[2], (String) value[3]);
        } catch (DataAccessException ex) {
            throw new BadRequestException("Farming season export data is not available yet");
        }
    }
}
