package com.nbicocchi.payment.persistence.repository;

import com.nbicocchi.payment.persistence.model.PaymentDetails;
import org.springframework.data.repository.CrudRepository;

public interface PaymentDetailsRepository extends CrudRepository<PaymentDetails, Long> {

}
