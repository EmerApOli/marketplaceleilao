package com.example.marketplace.domain.repository;

import com.example.marketplace.domain.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.marketplace.domain.enums.BidStatus;
import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByListingIdOrderByAmountDescCreatedAtAsc(Long listingId);
    List<Bid> findByBidderIdOrderByCreatedAtDesc(Long bidderId);
    Optional<Bid> findTopByListingIdOrderByAmountDescCreatedAtAsc(Long listingId);
    long countByBidderIdAndListingId(Long bidderId, Long listingId);
    List<Bid> findByListingProductVendedorIdOrderByCreatedAtDesc(Long vendedorId);
    long countByListingId(Long listingId);
    long countByListingIdAndStatus(Long listingId, BidStatus status);
}
