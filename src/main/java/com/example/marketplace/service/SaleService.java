package com.example.marketplace.service;

import com.example.marketplace.api.dto.AdminDashboardResponse;
import com.example.marketplace.api.dto.SaleConfirmationResponse;
import com.example.marketplace.domain.enums.ListingStatus;
import com.example.marketplace.domain.enums.SaleStatus;
import com.example.marketplace.domain.model.AdminNotification;
import com.example.marketplace.domain.model.Listing;
import com.example.marketplace.domain.model.Sale;
import com.example.marketplace.domain.model.User;
import com.example.marketplace.domain.repository.AdminNotificationRepository;
import com.example.marketplace.domain.repository.ListingRepository;
import com.example.marketplace.domain.repository.SaleRepository;
import com.example.marketplace.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ListingRepository listingRepository;
    private final AdminNotificationRepository adminNotificationRepository;
    private final UserService userService;

    @Transactional
    public SaleConfirmationResponse confirmSale(Long saleId, String userEmail) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new BusinessException("Venda não encontrada"));

        if (sale.getClosingDeadline().isBefore(LocalDateTime.now())) {
            sale.setStatus(SaleStatus.EXPIRED);
            saleRepository.save(sale);
            throw new BusinessException("Prazo de 7 dias expirado para concluir a venda");
        }

        User user = userService.findByEmail(userEmail);
        Long sellerId = sale.getListing().getProduct().getVendedor().getId();
        Long buyerId = sale.getBuyer().getId();
        if (!user.getId().equals(sellerId) && !user.getId().equals(buyerId)) {
            throw new BusinessException("Somente vendedor ou comprador podem validar a venda");
        }

        sale.setStatus(SaleStatus.COMPLETED);
        sale.getListing().setStatus(ListingStatus.FINISHED);
        listingRepository.save(sale.getListing());
        if (sale.getCompletedAt() == null) {
            sale.setCompletedAt(LocalDateTime.now());
        }
        sale.setAdminNotified(true);
        saleRepository.save(sale);

        createNotification(sale, "Venda validada no anúncio " + sale.getListing().getId()
                + ". Comissão da plataforma (6%): R$ " + sale.getAdminCommissionAmount());

        return mapConfirmation(sale);
    }

    @Transactional
    public SaleConfirmationResponse payPlatform(Long saleId, String sellerEmail) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new BusinessException("Venda não encontrada"));

        User seller = userService.findByEmail(sellerEmail);
        if (!sale.getListing().getProduct().getVendedor().getId().equals(seller.getId())) {
            throw new BusinessException("Somente o vendedor pode marcar o pagamento da plataforma");
        }

        if (sale.getStatus() != SaleStatus.COMPLETED) {
            throw new BusinessException("Valide a venda antes de pagar a plataforma");
        }

        sale.setPlatformPaid(true);
        if (sale.getPlatformPaidAt() == null) {
            sale.setPlatformPaidAt(LocalDateTime.now());
        }
        saleRepository.save(sale);

        createNotification(sale, "Pagamento da plataforma confirmado para a venda " + sale.getId()
                + ". Comissão paga: R$ " + sale.getAdminCommissionAmount());

        return mapConfirmation(sale);
    }

    @Transactional
    public Listing reactivateListing(Long saleId, String sellerEmail) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new BusinessException("Venda não encontrada"));

        User seller = userService.findByEmail(sellerEmail);
        Listing listing = sale.getListing();

        if (!listing.getProduct().getVendedor().getId().equals(seller.getId())) {
            throw new BusinessException("Somente o vendedor pode reativar o anúncio");
        }

        if (sale.getStatus() == SaleStatus.COMPLETED) {
            throw new BusinessException("Venda já validada. Não é possível reativar o anúncio");
        }

        if (!listing.getEndAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("O prazo original da publicação já terminou. Não é possível reativar");
        }

        listing.setStatus(ListingStatus.ACTIVE);
        listing.setWinner(null);
        listingRepository.save(listing);
        saleRepository.delete(sale);
        return listing;
    }

    @Transactional(readOnly = true)
    public Sale findByListingId(Long listingId) {
        return saleRepository.findByListingId(listingId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Sale> listSalesBySeller(String sellerEmail) {
        return saleRepository.findByListingProductVendedorIdOrderByCreatedAtDesc(userService.findUserIdByEmail(sellerEmail));
    }

    @Transactional
    public int expirePendingSales() {
        List<Sale> pendingSales = saleRepository.findByStatusOrderByCreatedAtDesc(SaleStatus.PENDING_CONTACT);
        LocalDateTime now = LocalDateTime.now();
        int expiredCount = 0;

        for (Sale sale : pendingSales) {
            if (sale.getClosingDeadline() != null && sale.getClosingDeadline().isBefore(now)) {
                sale.setStatus(SaleStatus.EXPIRED);
                saleRepository.save(sale);
                expiredCount++;
            }
        }

        return expiredCount;
    }

    public AdminDashboardResponse getAdminDashboard() {
        List<Sale> sales = saleRepository.findAll();

        BigDecimal totalSalesAmount = BigDecimal.ZERO;
        BigDecimal totalCommissionAmount = BigDecimal.ZERO;
        BigDecimal totalCompletedCommissionAmount = BigDecimal.ZERO;
        BigDecimal totalPendingCommissionAmount = BigDecimal.ZERO;
        BigDecimal totalPaidCommissionAmount = BigDecimal.ZERO;

        long completedSales = 0;
        long pendingSales = 0;
        long expiredSales = 0;
        long sellerPendingPayments = 0;

        for (Sale sale : sales) {
            BigDecimal finalAmount = sale.getFinalAmount() != null ? sale.getFinalAmount() : BigDecimal.ZERO;
            BigDecimal commission = sale.getAdminCommissionAmount() != null ? sale.getAdminCommissionAmount() : BigDecimal.ZERO;

            totalSalesAmount = totalSalesAmount.add(finalAmount);
            totalCommissionAmount = totalCommissionAmount.add(commission);

            if (sale.getStatus() == SaleStatus.COMPLETED) {
                completedSales++;
                totalCompletedCommissionAmount = totalCompletedCommissionAmount.add(commission);
                if (sale.isPlatformPaid()) {
                    totalPaidCommissionAmount = totalPaidCommissionAmount.add(commission);
                } else {
                    sellerPendingPayments++;
                }
            } else if (sale.getStatus() == SaleStatus.PENDING_CONTACT) {
                pendingSales++;
                totalPendingCommissionAmount = totalPendingCommissionAmount.add(commission);
            } else if (sale.getStatus() == SaleStatus.EXPIRED) {
                expiredSales++;
            }
        }

        return new AdminDashboardResponse(
                totalSalesAmount,
                totalCommissionAmount,
                totalCompletedCommissionAmount,
                totalPendingCommissionAmount,
                totalPaidCommissionAmount,
                sales.size(),
                completedSales,
                pendingSales,
                expiredSales,
                sellerPendingPayments
        );
    }

    @Transactional(readOnly = true)
    public List<Sale> listSalesByBuyer(String buyerEmail) {
        return saleRepository.findByBuyerIdOrderByCreatedAtDesc(userService.findUserIdByEmail(buyerEmail));
    }

    public List<Sale> listAllSales() {
        return saleRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<AdminNotification> listNotifications() {
        return adminNotificationRepository.findAllByOrderByCreatedAtDesc();
    }

    private void createNotification(Sale sale, String message) {
        AdminNotification notification = new AdminNotification();
        notification.setSale(sale);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        notification.setMessage(message);
        adminNotificationRepository.save(notification);
    }

    private SaleConfirmationResponse mapConfirmation(Sale sale) {
        return new SaleConfirmationResponse(
                sale.getId(),
                sale.getStatus().name(),
                sale.getFinalAmount(),
                sale.getAdminCommissionAmount(),
                sale.getCompletedAt(),
                sale.isAdminNotified(),
                sale.isPlatformPaid(),
                sale.getPlatformPaidAt()
        );
    }
}
