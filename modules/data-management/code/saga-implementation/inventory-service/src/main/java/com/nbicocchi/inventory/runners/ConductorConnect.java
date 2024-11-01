package com.nbicocchi.inventory.runners;

import com.nbicocchi.inventory.persistence.repository.InventoryRepository;
import com.nbicocchi.inventory.worker.DeleteProductWorker;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ConductorConnect implements ApplicationRunner {
    InventoryRepository inventoryRepository;

    public ConductorConnect(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        TaskClient taskClient = new TaskClient();
        taskClient.setRootURI("http://conductor:5000/api/"); // Point this to the server API

        Worker worker1 = new DeleteProductWorker("product_sale", inventoryRepository);
        List<Worker> workerArrayList = new ArrayList<>(List.of(worker1));

        // Start the polling and execution of tasks
        int threadCount = 1; // number of threads used to execute workers.  To avoid starvation, should be
        TaskRunnerConfigurer configurer =
                new TaskRunnerConfigurer.Builder(taskClient, workerArrayList)
                        .withThreadCount(threadCount)
                        .build();
        configurer.init();
    }
}
