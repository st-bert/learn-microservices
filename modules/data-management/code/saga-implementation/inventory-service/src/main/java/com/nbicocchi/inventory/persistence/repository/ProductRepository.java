package com.nbicocchi.inventory.persistence.repository;

import com.nbicocchi.inventory.persistence.model.Product;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ProductRepository extends CrudRepository<Product, Long> {
    Optional<Product> findByCode(String code);
}
