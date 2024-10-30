package com.nbicocchi.warehouse.persistence.repository;

import com.nbicocchi.warehouse.persistence.model.Product;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ProductRepository extends CrudRepository<Product, Long> {
    Optional<Product> findByCode(String code);
}
