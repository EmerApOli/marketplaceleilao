package com.example.marketplace.domain.model;

import com.example.marketplace.domain.enums.SaleStatus;
import com.example.marketplace.domain.enums.SaleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales")
@Getter
@Setter
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @ManyToOne(optional = false)
    @JoinColumn(name = "buyer_id")
    private User buyer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleType saleType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal finalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal adminCommissionAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime closingDeadline;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private boolean adminNotified;

    @Column(nullable = false)
    private boolean platformPaid;

    private LocalDateTime platformPaidAt;
}
