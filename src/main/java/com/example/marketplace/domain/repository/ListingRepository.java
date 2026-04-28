package com.example.marketplace.domain.repository;

import com.example.marketplace.domain.enums.ListingStatus;
import com.example.marketplace.domain.model.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    List<Listing> findByStatusAndEndAtBefore(ListingStatus status, LocalDateTime endAt);
    List<Listing> findByProductVendedorId(Long vendedorId);
    Page<Listing> findByStatusOrderByStartAtDesc(ListingStatus status, Pageable pageable);
    Page<Listing> findByStatusAndProductNomeContainingIgnoreCaseOrderByStartAtDesc(ListingStatus status, String productName, Pageable pageable);
    long countByProductVendedorIdAndStartAtAfter(Long vendedorId, LocalDateTime startAt);
}
