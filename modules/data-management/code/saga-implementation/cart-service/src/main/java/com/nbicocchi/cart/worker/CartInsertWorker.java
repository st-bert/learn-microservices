package com.nbicocchi.cart.worker;

import com.nbicocchi.cart.persistence.model.Product;
import com.nbicocchi.cart.persistence.repository.ProductRepository;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.beans.factory.annotation.Value;

public class CartInsertWorker implements Worker {
    private final String taskDefName;
    private final ProductRepository productCartRepository;

    public CartInsertWorker(@Value("taskDefName") String taskDefName, ProductRepository productCartRepository) {
        System.out.println("TaskDefName: " + taskDefName);
        this.taskDefName = taskDefName;
        this.productCartRepository = productCartRepository;
    }

    @Override
    public String getTaskDefName() {
        return taskDefName;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String code = (String) task.getInputData().get("productCode");
        String name = (String) task.getInputData().get("name");
        String description = (String) task.getInputData().get("description");

        System.out.println("Code: " + code);
        System.out.println("Name: " + name);
        System.out.println("Description: " + description);

        Product product = new Product(code, name, description);
        productCartRepository.save(product);
        System.out.println("Add product to chart db");
        result.setStatus(TaskResult.Status.COMPLETED);
        return result;
    }

}
