package com.example.marketplace.api.controller;

import com.example.marketplace.api.dto.CreateProductRequest;
import com.example.marketplace.domain.enums.ProductCondition;
import com.example.marketplace.domain.model.Product;
import com.example.marketplace.service.ProductService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final Validator validator;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Product create(@RequestParam String nome,
                          @RequestParam String descricao,
                          @RequestParam String categoria,
                          @RequestParam ProductCondition condicao,
                          @RequestParam(required = false) String imageUrl,
                          @RequestParam(required = false) MultipartFile image,
                          Authentication authentication) {
        CreateProductRequest request = new CreateProductRequest(nome, descricao, categoria, condicao, imageUrl);
        validate(request);
        return productService.create(request, image, authentication.getName());
    }

    @GetMapping
    public List<Product> findAll() {
        return productService.findAll();
    }

    @GetMapping("/me")
    public List<Product> findMine(Authentication authentication) {
        return productService.findMyProducts(authentication.getName());
    }

    private void validate(CreateProductRequest request) {
        Set<ConstraintViolation<CreateProductRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(message);
        }
    }
}
