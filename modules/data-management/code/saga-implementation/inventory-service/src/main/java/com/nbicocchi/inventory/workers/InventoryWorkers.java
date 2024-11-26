package com.nbicocchi.inventory.workers;

import com.nbicocchi.inventory.persistence.model.Inventory;
import com.nbicocchi.inventory.persistence.repository.InventoryRepository;
import com.nbicocchi.inventory.pojos.Order;
import com.netflix.conductor.sdk.workflow.task.WorkerTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Component
@Slf4j
public class InventoryWorkers {
    private final InventoryRepository inventoryRepository;
    /**
     * Note: Using this setting, up to 5 tasks will run in parallel, with tasks being polled every 200ms
     */
    @WorkerTask(value = "inventory-check", threadCount = 1, pollingInterval = 200)
    public void inventoryCheck(Order order) {;
        List<String> productIds = Arrays.stream(order.getProductIds().split(",")).toList();
        log.info("Verifying inventory {}...", productIds);
        for (String id : productIds) {
            Optional<Inventory> inventoryOptional = inventoryRepository.findInventoriesByProductId(id);
            if (inventoryOptional.isPresent()) {
                // product found
                Inventory inventory = inventoryOptional.get();
                if (inventory.getQuantity() > 0) {
                    inventory.setQuantity(inventory.getQuantity() - 1);
                    inventoryRepository.save(inventory);
                } else {
                    // inventory empty
                    throw new RuntimeException("Inventory empty: " + id);
                }
            } else {
                // missing product
                throw new RuntimeException("Inventory not found: " + id);
            }
        }
    }
}
