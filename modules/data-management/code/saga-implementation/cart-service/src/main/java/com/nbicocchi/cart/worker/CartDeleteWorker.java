package com.nbicocchi.cart.worker;

import com.nbicocchi.cart.persistence.model.Product;
import com.nbicocchi.cart.persistence.repository.ProductRepository;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

public class CartDeleteWorker implements Worker {

    private final String taskDefName;
    private final ProductRepository productRepository;

    public CartDeleteWorker(@Value("taskDefName") String taskDefName, ProductRepository productWarehouseRepository) {
        System.out.println("TaskDefName: " + taskDefName);
        this.taskDefName = taskDefName;
        this.productRepository = productWarehouseRepository;
    }

    @Override
    public String getTaskDefName() {
        return taskDefName;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String code = (String) task.getInputData().get("productCode");

        Optional<Product> productWarehouse = productRepository.findByCode(code);

        if(productWarehouse.isPresent()) {
            Product product = productWarehouse.get();
            result.addOutputData("name", product.getName());
            result.addOutputData("description", product.getDescription());

            productRepository.delete(product);

            System.out.println("Delete product from warehouse");
            result.setStatus(TaskResult.Status.COMPLETED);
        }
        else {
            result.setStatus(TaskResult.Status.FAILED);
        }
        return result;
    }
}
