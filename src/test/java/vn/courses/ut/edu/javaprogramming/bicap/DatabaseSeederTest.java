package vn.courses.ut.edu.javaprogramming.bicap;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmCertification;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmCertificationRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the demo data seeded by DatabaseSeeder (BICAP-3 / BICAP-4).
 * Each unique farm name must be seeded exactly once — the seeder dedupes by name,
 * not by owner, so all 4 farms (2 PENDING, 1 APPROVED, 1 REJECTED) must exist.
 */
@SpringBootTest
class DatabaseSeederTest {

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private FarmCertificationRepository certificationRepository;

    @Test
    void allFourSeedFarms_shouldExist_withCorrectStatuses() {
        Optional<Farm> dongNai = farmRepository.findByName("Trang Trại Xanh Đồng Nai");
        Optional<Farm> lamDong = farmRepository.findByName("HTX Nông Sản Sạch Lâm Đồng");
        Optional<Farm> songHong = farmRepository.findByName("Trang Trại Hữu Cơ Sông Hồng");
        Optional<Farm> tienGiang = farmRepository.findByName("Vườn Sạch Tiền Giang");

        assertTrue(dongNai.isPresent(), "Đồng Nai farm must be seeded");
        assertTrue(lamDong.isPresent(), "Lâm Đồng farm must be seeded");
        assertTrue(songHong.isPresent(), "Sông Hồng farm must be seeded");
        assertTrue(tienGiang.isPresent(), "Tiền Giang farm must be seeded");

        assertEquals(FarmStatus.PENDING, dongNai.get().getStatus());
        assertEquals(FarmStatus.PENDING, lamDong.get().getStatus());
        assertEquals(FarmStatus.APPROVED, songHong.get().getStatus());
        assertEquals(FarmStatus.REJECTED, tienGiang.get().getStatus());

        // One farm per name — no duplicates from repeated seeder runs
        assertEquals(4, farmRepository.count());
    }

    @Test
    void seedFarms_shouldCarryDescriptionAndProductTypes() {
        Optional<Farm> dongNai = farmRepository.findByName("Trang Trại Xanh Đồng Nai");
        assertTrue(dongNai.isPresent());
        assertNotNull(dongNai.get().getDescription());
        assertNotNull(dongNai.get().getProductTypes());
    }

    @Test
    void eachSeedFarm_shouldHaveAtLeastOneCertification() {
        List<Farm> farms = farmRepository.findAll();
        for (Farm farm : farms) {
            List<FarmCertification> certs = certificationRepository.findByFarmId(farm.getId());
            assertFalse(certs.isEmpty(), "Farm " + farm.getName() + " should have a certification document");
        }
    }
}
