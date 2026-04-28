package com.example.marketplace.domain.repository;

import com.example.marketplace.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByVendedorId(Long vendedorId);
}
