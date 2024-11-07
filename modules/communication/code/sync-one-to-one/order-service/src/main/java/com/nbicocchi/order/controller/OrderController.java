package com.nbicocchi.order.controller;

import com.nbicocchi.order.integration.ProductIntegration;
import com.nbicocchi.order.persistence.model.Order;
import com.nbicocchi.order.persistence.repository.OrderRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {
    OrderRepository orderRepository;
    ProductIntegration productIntegration;

    public OrderController(OrderRepository orderRepository, ProductIntegration productIntegration) {
        this.orderRepository = orderRepository;
        this.productIntegration = productIntegration;
    }

    @GetMapping(value = "")
    public Iterable<Order> findAll() {
        return orderRepository.findAll();
    }

    @GetMapping(value = "/{id}")
    public Order findById(@PathVariable Long id) {
        return orderRepository.findById(id).orElseThrow();
    }
}