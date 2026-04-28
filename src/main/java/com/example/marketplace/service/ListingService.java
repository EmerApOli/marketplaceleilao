package com.example.marketplace.service;

import com.example.marketplace.api.dto.CreateListingRequest;
import com.example.marketplace.domain.enums.ListingStatus;
import com.example.marketplace.domain.model.Listing;
import com.example.marketplace.domain.model.Product;
import com.example.marketplace.domain.model.User;
import com.example.marketplace.domain.repository.BidRepository;
import com.example.marketplace.domain.repository.ListingRepository;
import com.example.marketplace.domain.repository.ProductRepository;
import com.example.marketplace.domain.repository.SaleRepository;
import com.example.marketplace.exception.BusinessException;
import com.example.marketplace.service.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ListingService {

    private static final int MAX_LISTINGS_PER_WEEK = 3;
    private static final int FIXED_DURATION_DAYS = 10;

    private final ListingRepository listingRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final ImageStorageService imageStorageService;
    private final BidRepository bidRepository;
    private final SaleRepository saleRepository;

    public Listing create(CreateListingRequest request, List<MultipartFile> images, String sellerEmail) {
        validatePrices(request);
        User seller = userService.findByEmail(sellerEmail);
        validateWeeklyCreationLimit(seller.getId());

        Product product = new Product();
        applyProductData(product, request, images, false);
        product.setVendedor(seller);
        product = productRepository.save(product);

        Listing listing = new Listing();
        listing.setProduct(product);
        listing.setInitialPrice(request.initialPrice());
        listing.setBuyNowPrice(request.buyNowPrice());
        listing.setCurrentBid(request.initialPrice());
        listing.setStartAt(LocalDateTime.now());
        listing.setEndAt(LocalDateTime.now().plusDays(FIXED_DURATION_DAYS));
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setViews(0L);

        return listingRepository.save(listing);
    }

    @Transactional
    public Listing update(Long listingId, CreateListingRequest request, List<MultipartFile> images, String sellerEmail) {
        validatePrices(request);
        Listing listing = findById(listingId);
        User seller = userService.findByEmail(sellerEmail);

        if (!listing.getProduct().getVendedor().getId().equals(seller.getId())) {
            throw new BusinessException("Somente o anunciante pode editar o anúncio");
        }
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BusinessException("Somente anúncios ativos podem ser editados");
        }
        if (bidRepository.countByListingId(listingId) > 0) {
            throw new BusinessException("Não é possível editar um anúncio que já possui lances");
        }

        applyProductData(listing.getProduct(), request, List.of(), true);
        productRepository.save(listing.getProduct());
        listing.setInitialPrice(request.initialPrice());
        listing.setBuyNowPrice(request.buyNowPrice());
        listing.setCurrentBid(request.initialPrice());
        return listingRepository.save(listing);
    }

    @Transactional
    public Listing reactivateExpiredListing(Long listingId, String sellerEmail) {
        Listing listing = findById(listingId);
        User seller = userService.findByEmail(sellerEmail);
        if (!listing.getProduct().getVendedor().getId().equals(seller.getId())) {
            throw new BusinessException("Somente o anunciante pode reativar o anúncio");
        }
        if (saleRepository.findByListingId(listingId).isPresent()) {
            throw new BusinessException("Este anúncio possui negociação vinculada e não pode ser reativado por aqui");
        }
        if (listing.getStatus() == ListingStatus.ACTIVE) {
            throw new BusinessException("O anúncio já está ativo");
        }
        listing.setStatus(ListingStatus.ACTIVE);
        listing.setStartAt(LocalDateTime.now());
        listing.setEndAt(LocalDateTime.now().plusDays(FIXED_DURATION_DAYS));
        listing.setCurrentBid(listing.getInitialPrice());
        listing.setWinner(null);
        return listingRepository.save(listing);
    }

    private void validatePrices(CreateListingRequest request) {
        if (request.buyNowPrice().compareTo(request.initialPrice()) < 0) {
            throw new BusinessException("Preço final não pode ser menor que o preço inicial");
        }
        if (request.durationInDays() == null || request.durationInDays() < 1) {
            throw new BusinessException("Duração do anúncio inválida");
        }
    }

    private void validateWeeklyCreationLimit(Long sellerId) {
        long createdLastWeek = listingRepository.countByProductVendedorIdAndStartAtAfter(sellerId, LocalDateTime.now().minusDays(7));
        if (createdLastWeek >= MAX_LISTINGS_PER_WEEK) {
            throw new BusinessException("Limite atingido: cada usuário pode criar no máximo 3 anúncios por semana");
        }
    }

    private void applyProductData(Product product, CreateListingRequest request, List<MultipartFile> images, boolean keepExistingImagesWhenNone) {
        product.setNome(request.nome());
        product.setDescricao(request.descricao());
        String categoria = request.categoria() == null || request.categoria().isBlank() ? "Sem categoria" : request.categoria().trim();
        product.setCategoria(categoria);
        product.setCondicao(request.condicao());

        LinkedHashSet<String> resolvedImages = new LinkedHashSet<>();
        if (keepExistingImagesWhenNone && product.getImageUrls() != null) {
            resolvedImages.addAll(product.getImageUrls());
        }

        if (images != null) {
            for (MultipartFile image : images) {
                if (image != null && !image.isEmpty()) {
                    resolvedImages.add(imageStorageService.storeProductImage(image));
                }
            }
        }

        List<String> limitedImages = new ArrayList<>(resolvedImages).stream().limit(3).toList();
        if (limitedImages.isEmpty()) {
            if (!keepExistingImagesWhenNone) {
                throw new BusinessException("Cadastre pelo menos uma foto para criar o anúncio");
            }
            product.setImageUrl(null);
            product.setImageUrls(new ArrayList<>());
            return;
        }

        product.setImageUrl(limitedImages.get(0));
        product.setImageUrls(new ArrayList<>(limitedImages));
    }

    public List<Listing> findAll() {
        return listingRepository.findAll();
    }

    public Page<Listing> findPublicPage(String query, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        if (query == null || query.isBlank()) {
            return listingRepository.findByStatusOrderByStartAtDesc(ListingStatus.ACTIVE, pageRequest);
        }
        return listingRepository.findByStatusAndProductNomeContainingIgnoreCaseOrderByStartAtDesc(ListingStatus.ACTIVE, query.trim(), pageRequest);
    }

    public List<Listing> findMyListings(String sellerEmail) {
        return listingRepository.findByProductVendedorId(userService.findUserIdByEmail(sellerEmail));
    }

    @Transactional
    public Listing findById(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Anúncio não encontrado"));
        listing.setViews((listing.getViews() == null ? 0L : listing.getViews()) + 1);
        return listingRepository.save(listing);
    }
}
