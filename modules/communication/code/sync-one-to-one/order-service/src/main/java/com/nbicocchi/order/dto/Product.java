package com.nbicocchi.order.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Product {
    private Long id;
    @EqualsAndHashCode.Include
    private String uuid;
    private String name;
    private Double weight;

    public Product(String uuid, String name, Double weight) {
        this.uuid = uuid;
        this.name = name;
        this.weight = weight;
    }
}
