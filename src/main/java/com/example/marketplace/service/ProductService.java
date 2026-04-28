package com.example.marketplace.service;

import com.example.marketplace.api.dto.CreateProductRequest;
import com.example.marketplace.domain.model.Product;
import com.example.marketplace.domain.model.User;
import com.example.marketplace.domain.repository.ProductRepository;
import com.example.marketplace.service.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserService userService;
    private final ImageStorageService imageStorageService;

    public Product create(CreateProductRequest request, MultipartFile image, String sellerEmail) {
        User seller = userService.findByEmail(sellerEmail);

        Product product = new Product();
        product.setNome(request.nome());
        product.setDescricao(request.descricao());
        product.setCategoria(request.categoria());
        product.setCondicao(request.condicao());

        String resolvedImageUrl = request.imageUrl();
        if (image != null && !image.isEmpty()) {
            resolvedImageUrl = imageStorageService.storeProductImage(image);
        }

        product.setImageUrl(resolvedImageUrl);
        product.setVendedor(seller);

        return productRepository.save(product);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> findMyProducts(String sellerEmail) {
        return productRepository.findByVendedorId(userService.findUserIdByEmail(sellerEmail));
    }
}
