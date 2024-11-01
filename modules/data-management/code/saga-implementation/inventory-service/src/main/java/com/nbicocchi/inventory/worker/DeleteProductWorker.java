package com.nbicocchi.inventory.worker;

import com.nbicocchi.inventory.persistence.model.Product;
import com.nbicocchi.inventory.persistence.repository.InventoryRepository;
import com.nbicocchi.inventory.persistence.repository.ProductRepository;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

public class DeleteProductWorker implements Worker {

    private final String taskDefName;
    private final InventoryRepository inventoryRepository;

    public DeleteProductWorker(@Value("taskDefName") String taskDefName, InventoryRepository inventoryRepository) {
        System.out.println("TaskDefName: " + taskDefName);
        this.taskDefName = taskDefName;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public String getTaskDefName() {
        return taskDefName;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String code = (String) task.getInputData().get("productCode");

        return result;
    }
}
