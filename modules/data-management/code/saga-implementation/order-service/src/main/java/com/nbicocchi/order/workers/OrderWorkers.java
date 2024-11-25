package com.nbicocchi.order.workers;

import com.nbicocchi.order.persistence.model.Order;
import com.nbicocchi.order.persistence.repository.OrderRepository;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@AllArgsConstructor
@Component
@Slf4j
public class OrderWorkers {
    private final OrderRepository orderRepository;

    @WorkerTask(value = "persist-pending-order", threadCount = 1, pollingInterval = 200)
    public void placeOrder(Order order) {
        log.info("persisting {}...", order);
        orderRepository.save(order);
    }
}
