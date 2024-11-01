package com.nbicocchi.payment.runners;

import com.nbicocchi.payment.persistence.model.PaymentDetails;
import com.nbicocchi.payment.persistence.repository.PaymentDetailsRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataLoader implements ApplicationRunner {
    private final PaymentDetailsRepository paymentDetailsRepository;

    public DataLoader(PaymentDetailsRepository paymentDetailsRepository) {
        this.paymentDetailsRepository = paymentDetailsRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Set<PaymentDetails> paymentDetails = Set.of(
                new PaymentDetails("O-345", true),
                new PaymentDetails("O-123", false),
                new PaymentDetails("O-005", true));
        paymentDetailsRepository.saveAll(paymentDetails);
    }
}
