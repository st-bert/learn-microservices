package com.nbicocchi.payment.persistence.repository;

import com.nbicocchi.payment.persistence.model.Product;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ProductRepository extends CrudRepository<Product, Long> {
    Optional<Product> findByCode(String code);
}
