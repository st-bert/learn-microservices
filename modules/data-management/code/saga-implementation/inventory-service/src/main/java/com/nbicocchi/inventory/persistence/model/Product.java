package com.nbicocchi.inventory.persistence.model;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@NoArgsConstructor
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private String code;
    private String name;
    private String description;

    public Product(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
