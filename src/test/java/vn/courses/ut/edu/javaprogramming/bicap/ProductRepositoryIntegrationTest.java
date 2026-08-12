package vn.courses.ut.edu.javaprogramming.bicap;

import vn.courses.ut.edu.javaprogramming.bicap.entity.Farm;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmStatus;
import vn.courses.ut.edu.javaprogramming.bicap.entity.FarmingSeason;
import vn.courses.ut.edu.javaprogramming.bicap.entity.Product;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.FarmingSeasonRepository;
import vn.courses.ut.edu.javaprogramming.bicap.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the BICAP-5 product query (findProductsFiltered) actually executes against
 * H2, including the explicit JOIN ... ON across scalar id columns (Product → season → farm)
 * and the LIKE search covering both product and farm names.
 */
@SpringBootTest
class ProductRepositoryIntegrationTest {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private FarmingSeasonRepository seasonRepository;
    @Autowired
    private FarmRepository farmRepository;

    @Test
    void findProductsFiltered_shouldSupportSearchAndFilters() {
        // Arrange: one approved farm → one harvested season → one ACTIVE product.
        Farm farm = farmRepository.save(Farm.builder()
                .userId(1L).name("Trang Trại Tích Hợp")
                .address("Đồng Nai").area(5.0)
                .status(FarmStatus.APPROVED)
                .build());

        FarmingSeason season = seasonRepository.save(new FarmingSeason(
                null, farm.getId(), "Mùa Cải Đông", "Rau ăn lá", "Cải xanh",
                1.5, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1),
                "HARVESTED", null, null));

        Product product = productRepository.save(Product.builder()
                .seasonId(season.getId()).categoryId(1L).name("Cải xanh sạch")
                .description("VietGAP").price(new BigDecimal("15000"))
                .quantity(200.0).status("ACTIVE")
                .build());

        // Filter by status + category
        Page<Product> byStatus = productRepository.findProductsFiltered(
                "ACTIVE", 1L, null, PageRequest.of(0, 10));
        assertTrue(byStatus.getContent().stream().anyMatch(p -> p.getId().equals(product.getId())),
                "ACTIVE product should match status+category filter");

        // Search by product name
        Page<Product> byName = productRepository.findProductsFiltered(
                null, null, "cải", PageRequest.of(0, 10));
        assertTrue(byName.getContent().stream().anyMatch(p -> p.getId().equals(product.getId())),
                "Product should be found by its name");

        // Search by farm name (through season → farm join)
        Page<Product> byFarm = productRepository.findProductsFiltered(
                null, null, "tích hợp", PageRequest.of(0, 10));
        assertTrue(byFarm.getContent().stream().anyMatch(p -> p.getId().equals(product.getId())),
                "Product should be found by its owning farm name");

        // Non-matching search → empty page
        Page<Product> none = productRepository.findProductsFiltered(
                null, null, "không-tồn-tại", PageRequest.of(0, 10));
        assertEquals(0, none.getTotalElements());

        // Wrong status → excluded
        Page<Product> wrongStatus = productRepository.findProductsFiltered(
                "INACTIVE", null, null, PageRequest.of(0, 10));
        assertEquals(0, wrongStatus.getTotalElements());

        // Cleanup so the shared H2 context stays deterministic for other tests.
        productRepository.delete(product);
        seasonRepository.delete(season);
        farmRepository.delete(farm);
    }
}
