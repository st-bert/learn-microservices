package com.nbicocchi.purchase.persistence.repository;

import com.nbicocchi.purchase.persistence.model.Product;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<Product, Long> {
}
