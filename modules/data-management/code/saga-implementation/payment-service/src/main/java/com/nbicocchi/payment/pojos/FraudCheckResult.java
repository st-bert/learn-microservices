package com.nbicocchi.payment.pojos;

public record FraudCheckResult(Result result, String reason) {

    public enum Result {
        PASS, FAIL
    }

}
