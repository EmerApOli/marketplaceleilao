package com.example.marketplace.service;

import com.example.marketplace.api.dto.AcceptBidResponse;
import com.example.marketplace.domain.enums.BidStatus;
import com.example.marketplace.domain.enums.ListingStatus;
import com.example.marketplace.domain.enums.SaleStatus;
import com.example.marketplace.domain.enums.SaleType;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BidService {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.06");
    private static final long MAX_BIDS_PER_USER_PER_LISTING = 4L;

    private final ListingRepository listingRepository;
    private final BidRepository bidRepository;
    private final SaleRepository saleRepository;
    private final UserService userService;

    @Transactional
    public Bid placeBid(Long listingId, String bidderEmail, BigDecimal amount) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessException("Anúncio não encontrado"));

        if (listing.getStatus() != ListingStatus.ACTIVE) throw new BusinessException("Anúncio não está ativo");
        if (listing.getEndAt().isBefore(LocalDateTime.now())) throw new BusinessException("Leilão expirado");

        User bidder = userService.findByEmail(bidderEmail);
        if (listing.getProduct().getVendedor().getId().equals(bidder.getId())) throw new BusinessException("O vendedor não pode dar lance no próprio produto");

        long bidsForThisListing = bidRepository.countByBidderIdAndListingId(bidder.getId(), listingId);
        if (bidsForThisListing >= MAX_BIDS_PER_USER_PER_LISTING) throw new BusinessException("Limite atingido: cada usuário pode fazer no máximo 4 lances por anúncio");
        if (amount.compareTo(listing.getCurrentBid()) <= 0) throw new BusinessException("O lance deve ser maior que o lance atual");
        if (amount.compareTo(listing.getBuyNowPrice()) >= 0) throw new BusinessException("O lance manual deve ficar abaixo do valor de compra imediata");

        Bid bid = new Bid();
        bid.setListing(listing);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        bid.setCreatedAt(LocalDateTime.now());
        bid.setStatus(BidStatus.ACTIVE);

        listing.setCurrentBid(amount);
        listingRepository.save(listing);
        return bidRepository.save(bid);
    }

    @Transactional
    public Bid rejectBid(Long bidId, String sellerEmail) {
        Bid bid = bidRepository.findById(bidId).orElseThrow(() -> new BusinessException("Lance não encontrado"));
        Listing listing = bid.getListing();
        User seller = userService.findByEmail(sellerEmail);
        if (!listing.getProduct().getVendedor().getId().equals(seller.getId())) throw new BusinessException("Somente o anunciante pode rejeitar a proposta");
        if (listing.getStatus() != ListingStatus.ACTIVE) throw new BusinessException("Somente anúncios ativos podem ter propostas rejeitadas");
        bid.setStatus(BidStatus.REJECTED);
        bidRepository.save(bid);
        Bid topBid = bidRepository.findTopByListingIdOrderByAmountDescCreatedAtAsc(listing.getId()).orElse(null);
        if (topBid != null && topBid.getId().equals(bid.getId())) {
            BigDecimal newCurrent = bidRepository.findByListingIdOrderByAmountDescCreatedAtAsc(listing.getId()).stream()
                    .filter(existing -> existing.getStatus() != BidStatus.REJECTED)
                    .map(Bid::getAmount)
                    .findFirst()
                    .orElse(listing.getInitialPrice());
            listing.setCurrentBid(newCurrent);
            listingRepository.save(listing);
        }
        return bid;
    }

    @Transactional
    public AcceptBidResponse acceptHighestBid(Long listingId, String sellerEmail) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new BusinessException("Anúncio não encontrado"));

        User seller = userService.findByEmail(sellerEmail);
        if (!listing.getProduct().getVendedor().getId().equals(seller.getId())) throw new BusinessException("Somente o vendedor pode aceitar um lance neste anúncio");
        if (listing.getStatus() != ListingStatus.ACTIVE) throw new BusinessException("Somente anúncios ativos podem ter lance aceito");
        if (!listing.getEndAt().isAfter(LocalDateTime.now())) throw new BusinessException("Este aceite antecipado só pode ser feito antes do fim do leilão");
        if (saleRepository.findByListingId(listingId).isPresent()) throw new BusinessException("Já existe uma negociação aberta para este anúncio");

        Bid topBid = bidRepository.findByListingIdOrderByAmountDescCreatedAtAsc(listingId).stream()
                .filter(b -> b.getStatus() != BidStatus.REJECTED)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Ainda não há lances para aceitar"));

        bidRepository.findByListingIdOrderByAmountDescCreatedAtAsc(listingId).forEach(existing -> {
            if (existing.getId().equals(topBid.getId())) existing.setStatus(BidStatus.ACCEPTED);
            else if (existing.getStatus() != BidStatus.REJECTED) existing.setStatus(BidStatus.OUTBID);
            bidRepository.save(existing);
        });

        listing.setWinner(topBid.getBidder());
        listing.setCurrentBid(topBid.getAmount());
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);

        Sale sale = new Sale();
        sale.setListing(listing);
        sale.setBuyer(topBid.getBidder());
        sale.setSaleType(SaleType.AUCTION_WIN);
        sale.setFinalAmount(topBid.getAmount());
        sale.setAdminCommissionAmount(topBid.getAmount().multiply(COMMISSION_RATE));
        sale.setStatus(SaleStatus.PENDING_CONTACT);
        sale.setCreatedAt(LocalDateTime.now());
        sale.setClosingDeadline(LocalDateTime.now().plusDays(7));
        sale.setAdminNotified(false);
        sale.setPlatformPaid(false);
        sale = saleRepository.save(sale);

        String message = "Olá, " + topBid.getBidder().getNome() + ". Seu lance foi aceito no anúncio '" + listing.getProduct().getNome() + "'. Vamos concluir a negociação no marketplace.";
        String whatsappLink = "https://wa.me/55" + userService.sanitizeWhatsapp(topBid.getBidder().getWhatsapp()) + "?text=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

        return new AcceptBidResponse(sale.getId(), listing.getId(), topBid.getId(), topBid.getBidder().getNome(), topBid.getBidder().getAvatarUrl(), topBid.getAmount(), whatsappLink, message, sale.getClosingDeadline());
    }

    public List<Bid> listBids(Long listingId) {
        return bidRepository.findByListingIdOrderByAmountDescCreatedAtAsc(listingId);
    }
    public List<Bid> listMyBids(String bidderEmail) {
        return bidRepository.findByBidderIdOrderByCreatedAtDesc(userService.findUserIdByEmail(bidderEmail));
    }
    public List<Bid> listReceivedBids(String sellerEmail) {
        return bidRepository.findByListingProductVendedorIdOrderByCreatedAtDesc(userService.findUserIdByEmail(sellerEmail));
    }
}
