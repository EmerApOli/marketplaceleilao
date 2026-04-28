package com.example.marketplace.api.controller;

import com.example.marketplace.api.dto.AdminDashboardResponse;
import com.example.marketplace.domain.model.AdminNotification;
import com.example.marketplace.domain.model.Sale;
import com.example.marketplace.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SaleService saleService;

    @GetMapping("/dashboard")
    public AdminDashboardResponse dashboard() {
        return saleService.getAdminDashboard();
    }

    @GetMapping("/sales")
    public List<Sale> sales() {
        return saleService.listAllSales();
    }

    @GetMapping("/notifications")
    public List<AdminNotification> notifications() {
        return saleService.listNotifications();
    }
}
