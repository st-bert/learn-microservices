package com.nbicocchi.order.persistence.repository;

import com.nbicocchi.order.persistence.model.Customer;
import org.springframework.data.repository.CrudRepository;

public interface CustomerRepository extends CrudRepository<Customer, Long> {
}
