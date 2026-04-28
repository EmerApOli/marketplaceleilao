package com.example.marketplace.service;

import com.example.marketplace.domain.enums.ListingStatus;
import com.example.marketplace.domain.enums.SaleStatus;
import com.example.marketplace.domain.enums.SaleType;
import com.example.marketplace.domain.model.Bid;
import com.example.marketplace.domain.model.Listing;
import com.example.marketplace.domain.model.Sale;
import com.example.marketplace.domain.repository.BidRepository;
import com.example.marketplace.domain.repository.ListingRepository;
import com.example.marketplace.domain.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuctionClosingJob {

    private final ListingRepository listingRepository;
    private final BidRepository bidRepository;
    private final SaleRepository saleRepository;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void closeExpiredAuctions() {
        List<Listing> expiredListings =
                listingRepository.findByStatusAndEndAtBefore(ListingStatus.ACTIVE, LocalDateTime.now());

        for (Listing listing : expiredListings) {
            Optional<Bid> highestBid = bidRepository.findTopByListingIdOrderByAmountDescCreatedAtAsc(listing.getId());

            if (highestBid.isPresent()) {
                Bid winnerBid = highestBid.get();

                Sale sale = new Sale();
                sale.setListing(listing);
                sale.setBuyer(winnerBid.getBidder());
                sale.setSaleType(SaleType.AUCTION_WIN);
                sale.setFinalAmount(winnerBid.getAmount());
                sale.setAdminCommissionAmount(winnerBid.getAmount().multiply(new BigDecimal("0.06")));
                sale.setStatus(SaleStatus.PENDING_CONTACT);
                sale.setCreatedAt(LocalDateTime.now());
                sale.setClosingDeadline(LocalDateTime.now().plusDays(7));
                sale.setAdminNotified(false);
        sale.setPlatformPaid(false);

                saleRepository.save(sale);

                listing.setWinner(winnerBid.getBidder());
                listing.setCurrentBid(winnerBid.getAmount());
                listing.setStatus(ListingStatus.FINISHED);
            } else {
                listing.setStatus(ListingStatus.EXPIRED);
            }

            listingRepository.save(listing);
        }
    }
}
