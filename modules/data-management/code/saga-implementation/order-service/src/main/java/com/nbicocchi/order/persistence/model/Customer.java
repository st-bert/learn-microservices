package com.nbicocchi.order.persistence.model;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@NoArgsConstructor
@Entity
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private String socialSecurity;
    private String name;
    @Column(unique = true)
    private String cardNumber;

    public Customer(String socialSecurity, String name, String cardNumber) {
        this.socialSecurity = socialSecurity;
        this.name = name;
        this.cardNumber = cardNumber;
    }
}
