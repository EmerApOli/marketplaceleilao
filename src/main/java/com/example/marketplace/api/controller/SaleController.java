package com.example.marketplace.api.controller;

import com.example.marketplace.api.dto.SaleConfirmationResponse;
import com.example.marketplace.domain.model.Listing;
import com.example.marketplace.domain.model.Sale;
import com.example.marketplace.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping("/{id}/confirm")
    public SaleConfirmationResponse confirm(@PathVariable Long id, Authentication authentication) {
        return saleService.confirmSale(id, authentication.getName());
    }

    @PostMapping("/{id}/pay-platform")
    public SaleConfirmationResponse payPlatform(@PathVariable Long id, Authentication authentication) {
        return saleService.payPlatform(id, authentication.getName());
    }

    @PostMapping("/{id}/reactivate")
    public Listing reactivate(@PathVariable Long id, Authentication authentication) {
        return saleService.reactivateListing(id, authentication.getName());
    }

    @GetMapping("/me/seller")
    public List<Sale> listSellerSales(Authentication authentication) {
        return saleService.listSalesBySeller(authentication.getName());
    }

    @GetMapping("/me/buyer")
    public List<Sale> listBuyerSales(Authentication authentication) {
        return saleService.listSalesByBuyer(authentication.getName());
    }
}
