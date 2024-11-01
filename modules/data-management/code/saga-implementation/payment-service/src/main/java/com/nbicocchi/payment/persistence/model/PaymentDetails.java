package com.nbicocchi.payment.persistence.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@NoArgsConstructor
@Entity
public class PaymentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private String orderId;
    private LocalDateTime createdAt;
    private Boolean success;

    public PaymentDetails(String orderId, Boolean success) {
        this.orderId = orderId;
        this.createdAt = LocalDateTime.now();
        this.success = success;
    }
}
