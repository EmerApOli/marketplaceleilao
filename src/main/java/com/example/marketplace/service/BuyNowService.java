package com.example.marketplace.service;

import com.example.marketplace.domain.enums.ListingStatus;
import com.example.marketplace.domain.enums.SaleStatus;
import com.example.marketplace.domain.enums.SaleType;
import com.example.marketplace.domain.model.Listing;
import com.example.marketplace.domain.model.Sale;
import com.example.marketplace.domain.model.User;
import com.example.marketplace.domain.repository.ListingRepository;
import com.example.marketplace.domain.repository.SaleRepository;
import com.example.marketplace.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BuyNowService {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.06");

    private final ListingRepository listingRepository;
    private final SaleRepository saleRepository;
    private final UserService userService;

    @Transactional
    public Sale buyNow(Long listingId, String buyerEmail) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessException("Anúncio não encontrado"));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessException("Anúncio indisponível para compra");
        }

        if (listing.getEndAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Anúncio expirado");
        }

        User buyer = userService.findByEmail(buyerEmail);

        if (listing.getProduct().getVendedor().getId().equals(buyer.getId())) {
            throw new BusinessException("O vendedor não pode comprar o próprio produto");
        }

        Sale sale = new Sale();
        sale.setListing(listing);
        sale.setBuyer(buyer);
        sale.setSaleType(SaleType.BUY_NOW);
        sale.setFinalAmount(listing.getBuyNowPrice());
        sale.setAdminCommissionAmount(listing.getBuyNowPrice().multiply(COMMISSION_RATE));
        sale.setStatus(SaleStatus.PENDING_CONTACT);
        sale.setCreatedAt(LocalDateTime.now());
        sale.setClosingDeadline(LocalDateTime.now().plusDays(7));
        sale.setAdminNotified(false);
        sale.setPlatformPaid(false);

        listing.setCurrentBid(listing.getBuyNowPrice());
        listing.setWinner(buyer);
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);

        return saleRepository.save(sale);
    }
}
