package com.nbicocchi.order.persistence.repository;

import com.nbicocchi.order.persistence.model.Product;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<Product, Long> {
}
