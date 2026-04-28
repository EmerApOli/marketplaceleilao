package com.example.marketplace.domain.model;

import com.example.marketplace.domain.enums.ListingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "listings")
@Getter
@Setter
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal initialPrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal buyNowPrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBid;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatus status;

    @Column(nullable = false)
    private Long views = 0L;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;
}
