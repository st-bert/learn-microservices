# SAGA Pattern (Conductor)

## Project structure

A common example of a microservices architecture is an e-commerce platform where users can make purchases. In such an architecture, each microservice is responsible for a specific task, working in coordination to ensure that data remains accurate and consistent across the system. This project implements a simplified e-commerce platform using three microservices built with Spring Boot, each serving a distinct role:

- **`warehouse-service`**: Manages product inventory in the warehouse.
- **`cart-service`**: Manages products added to the shopping cart.
- **`purchase-service`**: Manages completed purchases.

Since the SAGA pattern requires an orchestrator to handle distributed transactions, we’ve chosen **Conductor**, a lightweight orchestrator that can be easily deployed as a Docker container. Conductor provides an intuitive graphical interface, which allows us to define workflows, manage task invocations, and configure interactions between services. We will delve further into these capabilities as we proceed.

In our implementation, each microservice has its own dedicated database for storing products information. The key class, shared among all three services, is `Product`:

```java
package com.nbicocchi.cart.persistence.model;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@NoArgsConstructor
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private String code;
    private String name;
    private String description;

    public Product(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
}
```

In summary, the architecture consists of three microservices, each with its own database to store product details, along with an orchestrator to manage interactions and ensure consistency across the platform.

## Docker configuration

Before delving into implementation details, reviewing the Docker container configuration will help clarify the architecture:

- **postgres**: This container manages the databases for the microservices. While each microservice would ideally have its own database, we simulate this by creating separate tables within a single Postgres container to simplify the setup and reduce resource consumption.

- **conductor**: This container runs the Conductor image, handling orchestration tasks and executing workflows. It is the only container that exposes two ports: port 5000 for accessing the UI and port 8080 for allowing services to register their tasks.

- **cart**: This container hosts the cart-service.

- **warehouse**: This container hosts the warehouse-service.

- **purchase**: This container hosts the purchase-service.

```yaml
services:
  postgres:
    image: postgres:latest
    restart: always
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: user
      POSTGRES_PASSWORD: secret
      POSTGRES_DB: jdbc_schema
    volumes:
      - pg-data:/var/lib/postgresql/data
    healthcheck:
      test: [ "CMD-SHELL", "pg_isready -U user -d jdbc_schema" ]
      interval: 30s
      timeout: 10s
      retries: 5

  conductor:
    image: orkesio/orkes-conductor-community-standalone:latest
    init: true
    ports:
      - "8080:8080"
      - "5000:5000"
    volumes:
      - redis:/redis
      - postgres:/pgdata
  
  cart:
    build: cart-service
    mem_limit: 512m
    ports:
      - "9000:9000"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      postgres:
        condition: service_healthy

  purchase:
    build: purchase-service
    mem_limit: 512m
    ports:
      - "9001:9001"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      postgres:
        condition: service_healthy

  warehouse:
    build: warehouse-service
    mem_limit: 512m
    ports:
      - "9002:9002"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  pg-data:
  pgadmin-data:
  redis:
  postgres:
```


## Conductor Overview

**Conductor** is a workflow orchestration framework developed by Netflix that facilitates the management of complex processes within microservices architectures. Conductor enables workflows to be defined as a sequence of independent tasks, each handled by different microservices, which promotes scalability, resilience, and centralized control. With support for multiple programming languages and integration with a wide array of technologies, Conductor is well-suited for automating and orchestrating distributed processes efficiently.

In our project, we need to add the necessary dependencies in each microservice’s `pom.xml` file to allow seamless interaction with Conductor:

```xml
<dependency>
   <groupId>io.orkes.conductor</groupId>
   <artifactId>orkes-conductor-client-spring</artifactId>
   <version>4.0.0</version>
</dependency>
```

## Workflow definition

To ensure that the orchestrator can manage transactions across each microservice’s database and maintain data consistency, workflows need to be defined. This enables the orchestrator to handle various scenarios effectively.

![](images/workflow.webp)

The workflow diagram shown above outlines the steps a product must go through before being sold. In our project, designed to simulate an e-commerce platform, each product must pass certain checks prior to sale. For example, verifying the validity of a credit card number is crucial before completing a purchase. Essentially, a workflow in Conductor represents an activity diagram, structured as a sequence of tasks to be executed in order. Specifically, our workflow models the product sale process and includes the following steps:

1. Removing the product from the warehouse.
2. Adding the product to the cart.
3. Verifying the credit card number to determine whether to proceed with removing the item from the cart and completing the sale, or to leave the item in the cart if payment cannot be processed.

The workflow also takes two input parameters:

- **productCode**: the primary key of the product.
- **creditCard**: the credit card number intended for payment.

By executing the workflow, we ensure consistent synchronization across databases. For example, if a product is purchased and the provided credit card number is valid, the workflow results in the product being transferred from the warehouse database to the purchases database.


### Workflow definition (JSON)

Once the theoretical aspects of the workflow are defined (including tasks, input and output parameters, and other details), implementation in Conductor can begin. Conductor uses a JSON file to define a workflow, which serves as a direct translation of the theoretical design, with additional information provided as metadata. This metadata is crucial, as it specifies aspects like the task lifecycle, retry limits in case of failures, and other essential configurations.

The basic structure of the JSON file is as follows:

```text
{
  "name": "worflow_name",
  "description": "Workflow for handling product purchases",
  "version": 1,
  "tasks": [
    {
      "name": "task1_name",
      "type": "SIMPLE",
      "inputParameters": {
        "id": "...",
        ...
      },
      "outputParameters": {
        ...
      }
    },
    {
      "name": "task2_name",
      "type": "SIMPLE",
      "inputParameters": {
        ...
      },
      "outputParameters": {
        ...
      }
    },

    ....
  ]
}
```

In some cases, as in our example, more complex constructs are required, involving specific operators like `SWITCH`, `DO-WHILE`, and others. For details on the syntax, refer to the official Conductor reference.

The full implementation of our workflow can be found in the file `buy_product_workflow.json`.

## Task definition and implementation

Each workflow consists of a set of tasks, and each task must be defined. To accomplish this, two main steps should be followed:

1. **Define Task Characteristics**: Use a JSON file to specify the attributes and configurations of each task.

2. **Implement Tasks in Java**: In the various microservices, implement the tasks by utilizing the [Worker](https://conductor-oss.github.io/conductor/devguide/how-tos/Workers/build-a-java-task-worker.html) interface from Conductor OSS.

Following these steps ensures that each task is correctly defined and implemented, allowing the workflow to function seamlessly.

### 1. Task definition (JSON)

For each task, as with each workflow, it is essential to define not only the name but also any associated **metadata**. This metadata typically adheres to a standard configuration that requires minimal modifications. Below is an example corresponding to the `check_credit_card` task in our workflow:

```json
{
  "createdBy": "",
  "updatedBy": "",
  "name": "check_credit_card",
  "description": "Check number of credit card",
  "retryCount": 3,
  "timeoutSeconds": 1200,
  "inputKeys": [
    "integer"
  ],
  "outputKeys": [
    "sum"
  ],
  "timeoutPolicy": "TIME_OUT_WF",
  "retryLogic": "FIXED",
  "retryDelaySeconds": 60,
  "responseTimeoutSeconds": 600,
  "inputTemplate": {},
  "rateLimitPerFrequency": 0,
  "rateLimitFrequencyInSeconds": 1,
  "ownerEmail": "yes.in.a.jiffy@gmail.com",
  "backoffScaleFactor": 1
}
```

The JSON files for each task present in the considered microservice can be found in the `/resources/task` directory.

### 2. Task implementation (Java)
When it comes to defining the task in Java, we simply need to implement a class following this structure:

```java
package com.baeldung.lsd.worker;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.springframework.beans.factory.annotation.Value;

public class CreditCardWorker implements Worker {

    private final String taskDefName;

    public CreditCardWorker(@Value("taskDefName") String taskDefName) {
        this.taskDefName = taskDefName;
    }

    // ...

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        String creditCard = (String) task.getInputData().get("creditCard");

        System.out.println("Credit card: " + creditCard);

        if (creditCard != null && creditCard.matches("^\\d{16}$")) {
            result.addOutputData("status", "valid");
        } else {
            result.addOutputData("status", "Invalid credit card");
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("Controllo numero di carta di credito");
        return result;
    }

}

```
Essentially, each Worker is associated with a Task, and the task's logic is implemented within the `execute()` method. The example provided is the implementation of the `check_credit_card` task from our workflow.

Typically, it is considered a best practice to place the implementation of Workers in a dedicated package named `worker`.

It is also necessary to instantiate the Worker when the service starts. So, every *main* class must be as follows:

```java
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
```


## Trying out the ecosystem

Now that we have defined the workflow (both theoretically and practically), as well as the tasks and written the code to manage the JPA `@Entity` classes, we can move on to see how to actually execute the workflow (the database implementation isn't treated in this notebook because it is already known - see JPA reference or the project code).

For first, it is necessary to generate the .jar files to start Docker:

```sh
$ mvn clean package -Dmaven.test.skip=true
$ docker compose build
$ docker compose up --detach
```
At this point navigate to
[http://localhost:5000/](http://localhost:5000/): here there is the **Conductor UI** as shown in the image below.

![](images/conductor-ui.webp)

At the firt access, we need to save the workflow and tasks definitions. To do this click on *Definitions->Workflows->New Workflow Definition* and *Definitions->Tasks->New Task Defintion*. The only things to do is copy and paste the JSON files defined before and click *Save*. See the image below:

![](images/def-workflow.webp)

To run the workflow, there is a section called ***Workbech***.

![](images/workbench.webp)

Here, you can select which workflow execute and the set the input parameter. Once the configuration is ready click *Play*.

In our case, there are three main cases we can run to verify how workflow works:

1. Insert a correct *productCode* and *creditCard*:
    ```text
    {
      "productCode": "P7",
      "creditCard": "1234567891234567"
    }
    ```

2. Insert a correct *productCode* and incorrect *creditCard*:
   ```text
    {
      "productCode": "P7",
      "creditCard": "123456789"
    }
    ```

4. Insert a *pruductCode* that does not exist:
   ```text
    {
      "productCode": "False Code",
      "creditCard": "1234567891234567"
    }
    ```

**Note**: In this simplified implementation, the credit card number is considered invalid if it is not 16 digits long.

For the first case, we will see that the workflow successed moving the product "P7" from the warehouse product tablet to the purchase product table. For the second case, the product "P7" remains in cart product table because the credit card is invalid. In the end, in the third case the workflow failed.

### How to see the execution results

In Conductor UI there is also another section called ***Executions***. Inside it is possible to see the results of every workflow executed and analyze the input/output parameters. It is very useful for debugging stuff.

![](images/executions.webp)

### Execute a workflow with curl

In Netflix Conductor, you can trigger the execution of a workflow not only through the Conductor UI but also via an HTTP request. This capability allows you to programmatically start workflows, making it easier to integrate Conductor into automated processes or other applications.

Here's an example of how to trigger a workflow using an HTTP request with `curl`:

```bash
curl -X POST http://localhost:8080/api/workflow/<example-workflow> \
     -H "Content-Type: application/json" \
     -d '{
           "name": "<example-workflow>",
           "version": 1,
           "input": {
             "productCode": "P7",
             "creditCard": "1234567891234567"
           }
         }'
```

Replace `<example-workflow>` with the name of the workflow you want to execute. This command sends a `POST` request to start the specified workflow, including any necessary input parameters in the JSON payload.

## References
* [Saga Pattern with Conductor - Baeldung](https://www.baeldung.com/orkes-conductor-saga-pattern-spring-boot)
* [Conductor OSS site](https://conductor-oss.github.io/conductor/devguide/concepts/index.html)
* [Conductor OSS Documentation]()
* [Operators in Conductor](https://conductor-oss.github.io/conductor/documentation/configuration/workflowdef/operators/index.html)
* [Basic Example of how to use Conductor](https://github.com/crisandolindesmanrumahorbo/conductor-netflix-demo)
* [Java JPA Documentation](https://docs.spring.io/spring-data/jpa/reference/jpa.html)
