package com.nbicocchi.cart;

import com.nbicocchi.cart.persistence.repository.ProductRepository;
import com.nbicocchi.cart.worker.CartDeleteWorker;
import com.nbicocchi.cart.worker.CartInsertWorker;
import com.nbicocchi.cart.worker.CreditCardWorker;
import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.worker.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class App implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    private final ProductRepository productCartRepository;

    public App(ProductRepository productCartRepository) {
        this.productCartRepository = productCartRepository;
    }

    public static void main(final String... args) {
        SpringApplication.run(App.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        TaskClient taskClient = new TaskClient();
        taskClient.setRootURI("http://conductor:5000/api/"); // Point this to the server API

        Worker worker1 = new CartInsertWorker("insert_product_in_the_cart", productCartRepository);
        Worker worker2 = new CreditCardWorker("check_credit_card");
        Worker worker3 = new CartDeleteWorker("cart_delete_product", productCartRepository);
        List<Worker> workerArrayList = new ArrayList<>(List.of(worker1, worker2, worker3));

        // Start the polling and execution of tasks
        int threadCount = 1; // number of threads used to execute workers.  To avoid starvation, should be
        TaskRunnerConfigurer configurer =
                new TaskRunnerConfigurer.Builder(taskClient, workerArrayList)
                        .withThreadCount(threadCount)
                        .build();
        configurer.init();
    }
}
