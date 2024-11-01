package com.nbicocchi.order.persistence.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@NoArgsConstructor
@Entity
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private String code;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    private Set<Product> products = new HashSet<>();
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    public Order(String code, Set<Product> products, Customer customer) {
        this.code = code;
        this.products = products;
        this.customer = customer;
    }
}
