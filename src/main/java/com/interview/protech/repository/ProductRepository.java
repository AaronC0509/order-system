package com.interview.protech.repository;

import com.interview.protech.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Product findByProductCode(String productCode);

    @Modifying
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.productCode = :productCode AND p.stockQuantity >= :quantity")
    int decreaseStock(@Param("productCode") String productCode, @Param("quantity") Integer quantity);
}
