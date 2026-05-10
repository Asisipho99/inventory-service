package com.shopping.inventory_service.repository;

import com.shopping.inventory_service.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Standard read — no lock
    Optional<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    // Find all products with stock below their alert threshold
    List<Inventory> findByQuantityLessThanEqual(Integer threshold);

    // Pessimistic write lock — used during stock deduction and restoration
    // to prevent race conditions when multiple orders arrive simultaneously
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);
}
