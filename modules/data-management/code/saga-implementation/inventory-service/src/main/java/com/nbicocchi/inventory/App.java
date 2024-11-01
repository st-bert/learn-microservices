package com.nbicocchi.inventory;

import com.nbicocchi.inventory.persistence.repository.ProductRepository;
import com.nbicocchi.inventory.worker.DeleteProductWorker;
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
public class App {
    public static void main(final String... args) {
        SpringApplication.run(App.class, args);
    }
}
