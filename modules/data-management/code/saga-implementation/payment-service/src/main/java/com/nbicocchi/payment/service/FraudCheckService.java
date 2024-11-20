package com.nbicocchi.payment.service;

import com.nbicocchi.payment.pojos.DepositDetail;
import com.nbicocchi.payment.pojos.FraudCheckResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.nbicocchi.payment.pojos.FraudCheckResult.Result.FAIL;
import static com.nbicocchi.payment.pojos.FraudCheckResult.Result.PASS;

@Service
public class FraudCheckService {

    public FraudCheckResult checkForFraud(DepositDetail depositDetail) {
        if (depositDetail.getAmount().compareTo(BigDecimal.valueOf(100000)) > 0) {
            return new FraudCheckResult(FAIL, "Amount too large");
        } else {
            return new FraudCheckResult(PASS, "All good");
        }
    }
}
