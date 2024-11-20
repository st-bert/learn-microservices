package com.nbicocchi.payment.worker;

import com.nbicocchi.payment.pojos.DepositDetail;
import com.nbicocchi.payment.pojos.FraudCheckResult;
import com.nbicocchi.payment.service.FraudCheckService;
import com.netflix.conductor.sdk.workflow.task.InputParam;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

@AllArgsConstructor
@Component
@Slf4j
public class PaymentWorkers {

    private final FraudCheckService fraudCheckService;
    private final Random random = new Random();

    // docs-marker-start-1

    /**
     * Note: Using this setting, up to 5 tasks will run in parallel, with tasks being polled every 200ms
     */
    @WorkerTask(value = "payment-check-nb", threadCount = 1, pollingInterval = 200)
    public FraudCheckResult checkForFraudTask(DepositDetail depositDetail) {
        log.info("ok");
        return fraudCheckService.checkForFraud(depositDetail);
    }

    // docs-marker-end-1
}
