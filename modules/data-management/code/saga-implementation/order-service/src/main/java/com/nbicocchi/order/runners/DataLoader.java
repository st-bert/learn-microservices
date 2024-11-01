package com.nbicocchi.order.runners;

import com.nbicocchi.order.persistence.model.Customer;
import com.nbicocchi.order.persistence.model.Order;
import com.nbicocchi.order.persistence.model.Product;
import com.nbicocchi.order.persistence.repository.CustomerRepository;
import com.nbicocchi.order.persistence.repository.OrderRepository;
import com.nbicocchi.order.persistence.repository.ProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataLoader implements ApplicationRunner {
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public DataLoader(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<Product> products = Set.of(
                new Product("P-001", "Bmx Bike", "A small bike for jumping"),
                new Product("P-002", "Led Light", "A led light for bikes"));

        Customer customer = new Customer("BCCNCL", "Nicola", "123-456-789");
        Order order1 = new Order("O-001", products, customer);

        customerRepository.save(customer);
        orderRepository.save(order1);
    }
}
