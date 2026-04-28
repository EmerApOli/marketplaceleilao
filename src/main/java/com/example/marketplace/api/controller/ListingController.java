package com.example.marketplace.api.controller;

import com.example.marketplace.api.dto.AcceptBidResponse;
import com.example.marketplace.api.dto.ContactInfoResponse;
import com.example.marketplace.api.dto.CreateBidRequest;
import com.example.marketplace.api.dto.CreateListingRequest;
import com.example.marketplace.domain.enums.ProductCondition;
import com.example.marketplace.domain.model.Bid;
import com.example.marketplace.domain.model.Listing;
import com.example.marketplace.domain.model.Sale;
import com.example.marketplace.service.BidService;
import com.example.marketplace.service.BuyNowService;
import com.example.marketplace.service.ContactService;
import com.example.marketplace.service.ListingService;
import com.example.marketplace.service.SaleService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;
    private final BidService bidService;
    private final BuyNowService buyNowService;
    private final ContactService contactService;
    private final SaleService saleService;
    private final Validator validator;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Listing create(@RequestParam String nome,
                          @RequestParam String descricao,
                          @RequestParam(required = false) String categoria,
                          @RequestParam ProductCondition condicao,
                          @RequestParam BigDecimal initialPrice,
                          @RequestParam BigDecimal buyNowPrice,
                          @RequestParam Integer durationInDays,
                          @RequestParam(required = false) MultipartFile image,
                          @RequestParam(required = false) MultipartFile image2,
                          @RequestParam(required = false) MultipartFile image3,
                          Authentication authentication) {
        CreateListingRequest request = new CreateListingRequest(
                nome,
                descricao,
                categoria,
                condicao,
                initialPrice,
                buyNowPrice,
                durationInDays,
                null
        );
        validate(request);
        List<MultipartFile> images = new ArrayList<>();
        images.add(image);
        images.add(image2);
        images.add(image3);
        return listingService.create(request, images, authentication.getName());
    }


    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Listing update(@PathVariable Long id,
                          @RequestParam String nome,
                          @RequestParam String descricao,
                          @RequestParam(required = false) String categoria,
                          @RequestParam ProductCondition condicao,
                          @RequestParam BigDecimal initialPrice,
                          @RequestParam BigDecimal buyNowPrice,
                          @RequestParam Integer durationInDays,
                          @RequestParam(required = false) MultipartFile image,
                          @RequestParam(required = false) MultipartFile image2,
                          @RequestParam(required = false) MultipartFile image3,
                          Authentication authentication) {
        CreateListingRequest request = new CreateListingRequest(nome, descricao, categoria, condicao, initialPrice, buyNowPrice, durationInDays, id);
        validate(request);
        List<MultipartFile> images = new ArrayList<>();
        images.add(image);
        images.add(image2);
        images.add(image3);
        return listingService.update(id, request, images, authentication.getName());
    }

    @GetMapping
    public List<Listing> findAll() {
        return listingService.findAll();
    }

    @GetMapping("/public")
    public Page<Listing> publicListings(@RequestParam(required = false) String q,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "12") int size) {
        return listingService.findPublicPage(q, page, size);
    }

    @GetMapping("/me")
    public List<Listing> findMine(Authentication authentication) {
        return listingService.findMyListings(authentication.getName());
    }

    @GetMapping("/me/bids")
    public List<Bid> listMyBids(Authentication authentication) {
        return bidService.listMyBids(authentication.getName());
    }

    @GetMapping("/me/received-bids")
    public List<Bid> listReceivedBids(Authentication authentication) {
        return bidService.listReceivedBids(authentication.getName());
    }

    @GetMapping("/{id}")
    public Listing findById(@PathVariable Long id) {
        return listingService.findById(id);
    }

    @GetMapping("/{id}/sale")
    public Sale findSale(@PathVariable Long id) {
        return saleService.findByListingId(id);
    }

    @PostMapping("/{id}/bids")
    public Bid createBid(@PathVariable Long id,
                         @RequestBody CreateBidRequest request,
                         Authentication authentication) {
        return bidService.placeBid(id, authentication.getName(), request.amount());
    }

    @GetMapping("/{id}/bids")
    public List<Bid> listBids(@PathVariable Long id) {
        return bidService.listBids(id);
    }

    @PostMapping("/{id}/accept-bid")
    public AcceptBidResponse acceptBid(@PathVariable Long id, Authentication authentication) {
        return bidService.acceptHighestBid(id, authentication.getName());
    }

    @PostMapping("/bids/{bidId}/reject")
    public Bid rejectBid(@PathVariable Long bidId, Authentication authentication) {
        return bidService.rejectBid(bidId, authentication.getName());
    }

    @PostMapping("/{id}/reactivate")
    public Listing reactivateListing(@PathVariable Long id, Authentication authentication) {
        return listingService.reactivateExpiredListing(id, authentication.getName());
    }

    @PostMapping("/{id}/buy-now")
    public Sale buyNow(@PathVariable Long id, Authentication authentication) {
        return buyNowService.buyNow(id, authentication.getName());
    }

    @GetMapping("/{id}/contact")
    public ContactInfoResponse contact(@PathVariable Long id, Authentication authentication) {
        return contactService.getContactForListing(id, authentication.getName());
    }

    private void validate(CreateListingRequest request) {
        Set<ConstraintViolation<CreateListingRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(message);
        }
    }
}
