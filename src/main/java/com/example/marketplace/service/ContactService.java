package com.example.marketplace.service;

import com.example.marketplace.api.dto.ContactInfoResponse;
import com.example.marketplace.domain.model.Bid;
import com.example.marketplace.domain.model.Listing;
import com.example.marketplace.domain.model.Sale;
import com.example.marketplace.domain.model.User;
import com.example.marketplace.domain.repository.BidRepository;
import com.example.marketplace.domain.repository.ListingRepository;
import com.example.marketplace.domain.repository.SaleRepository;
import com.example.marketplace.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ListingRepository listingRepository;
    private final BidRepository bidRepository;
    private final SaleRepository saleRepository;
    private final UserService userService;

    public ContactInfoResponse getContactForListing(Long listingId, String requesterEmail) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessException("Anúncio não encontrado"));

        User requester = userService.findByEmail(requesterEmail);
        User seller = listing.getProduct().getVendedor();
        LocalDateTime availableUntil;
        User counterpart;
        String context;

        Sale sale = saleRepository.findByListingId(listingId).orElse(null);
        if (sale != null) {
            if (sale.getClosingDeadline().isBefore(LocalDateTime.now())) {
                throw new BusinessException("Prazo de contato expirado");
            }
            availableUntil = sale.getClosingDeadline();
            if (requester.getId().equals(seller.getId())) {
                counterpart = sale.getBuyer();
                context = "Contato do comprador para concluir a venda";
            } else if (requester.getId().equals(sale.getBuyer().getId())) {
                counterpart = seller;
                context = "Contato do vendedor para concluir a compra";
            } else {
                throw new BusinessException("Você não participa desta negociação");
            }
        } else {
            Bid topBid = bidRepository.findTopByListingIdOrderByAmountDescCreatedAtAsc(listingId)
                    .orElseThrow(() -> new BusinessException("Ainda não há lances para contato"));
            availableUntil = topBid.getCreatedAt().plusDays(2);
            if (availableUntil.isBefore(LocalDateTime.now())) {
                throw new BusinessException("Prazo de contato expirado");
            }
            if (requester.getId().equals(seller.getId())) {
                counterpart = topBid.getBidder();
                context = "Contato do maior lance atual";
            } else if (requester.getId().equals(topBid.getBidder().getId())) {
                counterpart = seller;
                context = "Contato do vendedor do anúncio";
            } else {
                throw new BusinessException("Você não participa deste lance");
            }
        }

        String sanitized = userService.sanitizeWhatsapp(counterpart.getWhatsapp());
        String text = "Olá, vim pelo marketplace para concluir a negociação do anúncio " + listing.getProduct().getNome();
        String link = "https://wa.me/55" + sanitized + "?text=" + text.replace(" ", "%20");
        return new ContactInfoResponse(
                counterpart.getNome(),
                userService.maskWhatsapp(counterpart.getWhatsapp()),
                link,
                context,
                availableUntil
        );
    }
}
