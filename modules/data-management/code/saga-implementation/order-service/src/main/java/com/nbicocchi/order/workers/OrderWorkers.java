package com.nbicocchi.order.workers;

import com.nbicocchi.order.persistence.model.Order;
import com.nbicocchi.order.persistence.repository.OrderRepository;
import com.nbicocchi.order.pojos.TaskResult;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@AllArgsConstructor
@Component
@Slf4j
public class OrderWorkers {
    private final OrderRepository orderRepository;

    @WorkerTask(value = "persist-pending-order", threadCount = 1, pollingInterval = 200)
    public TaskResult placeOrder(Order order) {
        log.info("persisting {}...", order);
        Optional<Order> existingOrder = orderRepository.findByCode(order.getCode());
        if (existingOrder.isPresent()) {
            log.info("persisting Order(not valid)");
            return new TaskResult(TaskResult.Result.FAIL, "Duplicate order");
        }
        log.info("persisting Order(valid)");
        orderRepository.save(order);
        return new TaskResult(TaskResult.Result.PASS, "");
    }

    @WorkerTask(value = "delete-pending-order", threadCount = 1, pollingInterval = 200)
    public TaskResult deleteOrder(Order order) {
        log.info("deleting {}...", order);
        log.info("deleting Order(SAGA aborted)");
        Optional<Order> existingOrder = orderRepository.findByCode(order.getCode());
        existingOrder.ifPresent(orderRepository::delete);
        return new TaskResult(TaskResult.Result.PASS, "");
    }
}
