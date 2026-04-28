package com.example.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaleDeadlineJob {

    private final SaleService saleService;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expirePendingSales() {
        saleService.expirePendingSales();
    }
}
