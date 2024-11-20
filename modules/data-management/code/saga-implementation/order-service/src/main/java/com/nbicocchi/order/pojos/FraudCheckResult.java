package com.nbicocchi.order.pojos;

public record FraudCheckResult(FraudCheckResult.Result result, String reason) {

    public enum Result {
        PASS, FAIL
    }

}
