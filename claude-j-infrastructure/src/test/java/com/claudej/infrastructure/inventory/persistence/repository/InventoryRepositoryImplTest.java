package com.claudej.infrastructure.inventory.persistence.repository;

import com.claudej.domain.inventory.model.aggregate.Inventory;
import com.claudej.domain.inventory.model.valobj.InventoryId;
import com.claudej.domain.inventory.model.valobj.SkuCode;
import com.claudej.domain.inventory.repository.InventoryRepository;
import com.claudej.infrastructure.inventory.persistence.converter.InventoryConverter;
import com.claudej.infrastructure.inventory.persistence.mapper.InventoryMapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InventoryRepositoryImpl SpringBootTest
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:inventory_repo_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Transactional
class InventoryRepositoryImplTest {

    @SpringBootApplication(scanBasePackageClasses = {
            InventoryRepositoryImpl.class,
            InventoryConverter.class,
            InventoryMapper.class
    })
    @MapperScan(basePackageClasses = InventoryMapper.class)
    static class TestConfig {
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void should_save_and_findById() {
        // Given
        String productId = "PROD-" + UUID.randomUUID().toString().substring(0, 8);
        String skuCode = "SKU-" + UUID.randomUUID().toString().substring(0, 8);
        Inventory inventory = Inventory.create(productId, new SkuCode(skuCode), 100);

        // When
        Inventory saved = inventoryRepository.save(inventory);

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(saved.getAvailableStock()).isEqualTo(100);

        // Verify by finding
        Optional<Inventory> found = inventoryRepository.findByInventoryId(saved.getInventoryId());
        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo(productId);
    }

    @Test
    void should_findByProductId() {
        // Given
        String productId = "PROD-" + UUID.randomUUID().toString().substring(0, 8);
        String skuCode = "SKU-" + UUID.randomUUID().toString().substring(0, 8);
        Inventory inventory = Inventory.create(productId, new SkuCode(skuCode), 100);
        inventoryRepository.save(inventory);

        // When
        Optional<Inventory> found = inventoryRepository.findByProductId(productId);

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getProductId()).isEqualTo(productId);
        assertThat(found.get().getAvailableStock()).isEqualTo(100);
    }

    @Test
    void should_findBySkuCode() {
        // Given
        String productId = "PROD-" + UUID.randomUUID().toString().substring(0, 8);
        String skuCode = "SKU-" + UUID.randomUUID().toString().substring(0, 8);
        Inventory inventory = Inventory.create(productId, new SkuCode(skuCode), 100);
        inventoryRepository.save(inventory);

        // When
        Optional<Inventory> found = inventoryRepository.findBySkuCode(new SkuCode(skuCode));

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getSkuCodeValue()).isEqualTo(skuCode);
    }

    @Test
    void should_return_empty_when_notFound() {
        // When
        Optional<Inventory> found = inventoryRepository.findByInventoryId(new InventoryId("NOT-EXIST"));

        // Then
        assertThat(found).isNotPresent();
    }

    @Test
    void should_existsByInventoryId() {
        // Given
        String productId = "PROD-" + UUID.randomUUID().toString().substring(0, 8);
        String skuCode = "SKU-" + UUID.randomUUID().toString().substring(0, 8);
        Inventory inventory = Inventory.create(productId, new SkuCode(skuCode), 100);
        Inventory saved = inventoryRepository.save(inventory);

        // When
        boolean exists = inventoryRepository.existsByInventoryId(saved.getInventoryId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void should_update_inventory() {
        // Given
        String productId = "PROD-" + UUID.randomUUID().toString().substring(0, 8);
        String skuCode = "SKU-" + UUID.randomUUID().toString().substring(0, 8);
        Inventory inventory = Inventory.create(productId, new SkuCode(skuCode), 100);
        Inventory saved = inventoryRepository.save(inventory);

        // When - reserve and save
        saved.reserve(10);
        inventoryRepository.save(saved);

        // Then
        Optional<Inventory> found = inventoryRepository.findByInventoryId(saved.getInventoryId());
        assertThat(found).isPresent();
        assertThat(found.get().getAvailableStock()).isEqualTo(90);
        assertThat(found.get().getReservedStock()).isEqualTo(10);
    }
}