package com.example.marketplace.domain.repository;

import com.example.marketplace.domain.enums.SaleStatus;
import com.example.marketplace.domain.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    Optional<Sale> findByListingId(Long listingId);
    List<Sale> findByStatusOrderByCreatedAtDesc(SaleStatus status);
    List<Sale> findAllByOrderByCreatedAtDesc();
    List<Sale> findByListingProductVendedorIdOrderByCreatedAtDesc(Long sellerId);
    List<Sale> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
}
