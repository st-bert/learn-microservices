# Distributed data management

## Introduction

The **SAGA pattern** is an architectural approach designed to manage complex, long-running transactions across distributed services, making it particularly valuable in microservices environments where a single, monolithic transaction is unfeasible.

In a SAGA, transactions are broken down into a sequence of smaller, isolated steps:

- Each step is managed by a separate microservice, often with its own database.
- If any step fails, compensating transactions are triggered to undo previous operations, ensuring data consistency.

In microservices architectures, maintaining traditional ACID (Atomicity, Consistency, Isolation, Durability) properties becomes challenging, especially for isolation, which prevents transactions from interfering with one another. This is due to each service having its own distributed data store. The SAGA pattern helps address these challenges by using countermeasures to minimize the impact of concurrency anomalies, providing a practical alternative to ACID transactions in distributed systems.

### ACID Properties in Transactions

The **ACID** properties—**Atomicity, Consistency, Isolation, and Durability**—define the reliability requirements for transactions in traditional database systems. These properties ensure that transactions are processed in a predictable, reliable manner, even in the event of system failures.

- **Atomicity**: This property ensures that a transaction is treated as a single, indivisible unit. If any part of the transaction fails, the entire transaction is rolled back, leaving the system in its original state. This "all-or-nothing" approach guarantees that either all steps of a transaction are completed successfully, or none are applied at all.

- **Consistency**: Consistency guarantees that a transaction brings the database from one valid state to another. Each transaction must comply with all predefined rules, constraints, and triggers to preserve the integrity of the data. This means that a completed transaction should leave the database in a valid state, reflecting the system’s business rules and constraints.

- **Isolation**: Isolation ensures that transactions are executed independently, without interference. Each transaction should act as if it is the only one interacting with the database, even if others are running concurrently. Isolation is essential for preventing issues such as **dirty reads** (reading uncommitted data), **non-repeatable reads** (different data returned in repeated reads within a transaction), and **phantom reads** (new data appears in repeated reads within a transaction).

- **Durability**: This property ensures that once a transaction is committed, its results are permanent, even in case of power loss, crashes, or other failures. Changes made by a committed transaction are stored in persistent storage and are guaranteed to survive any subsequent system failures.

Together, these ACID properties are essential in maintaining data accuracy, consistency, and reliability in traditional, centralized databases, enabling them to handle complex, critical operations reliably. In distributed systems, however, achieving strict ACID properties becomes challenging, especially for Isolation, leading to the need for alternative approaches like the **SAGA Pattern** in microservices architectures.

### Anomalies Caused by Lack of Isolation

In a distributed system with limited isolation, several anomalies can disrupt data consistency:

- **Lost Updates**: This anomaly occurs when one saga overwrites changes made by another saga without reading them first. This results in critical updates being lost, leading to inconsistent or incorrect data within the system.

- **Dirty Reads**: This issue arises when a saga reads data that is still being modified by another saga that hasn’t completed its transaction. Such reads can lead to decisions based on incomplete or incorrect data, potentially resulting in problems like exceeding credit limits or applying unauthorized changes.

- **Fuzzy/Non-repeatable Reads**: This happens when different steps within the same saga read the same data but receive inconsistent results due to updates from another saga. This lack of stability can lead to unreliable outcomes in transaction processing.

These anomalies highlight the challenges of maintaining data integrity in a distributed environment without strict isolation controls.

### Addressing the Lack of Isolation in Saga Transactions

In the saga model, transactions are **ACD-compliant** (Atomicity, Consistency, Durability) but lack **Isolation**, leading to possible anomalies that can affect business operations. To mitigate these issues, developers can employ several countermeasures:

- **Semantic Lock**: This method involves placing a temporary flag on a record during a compensatable transaction to signal that the data may still change. This flag serves as either a hard lock, preventing other transactions from accessing the record, or a soft warning, advising caution for other transactions. The flag is removed once the saga completes (either through a successful retriable transaction or a compensating rollback).

- **Commutative Updates**: Ensures that updates are designed to be executed in any order, meaning they are **commutative**. This reduces the potential impact of concurrent operations and helps maintain data consistency.

- **Pessimistic View**: By reordering the steps in a saga, this countermeasure reduces business risks associated with dirty reads. Critical steps are sequenced to occur in a way that limits the effect of inconsistent data, reducing the likelihood of errors from concurrent transactions.

- **Reread Value**: This method prevents dirty writes by verifying the data before making an update. If the data has been modified, the saga aborts and may restart. Acting as a type of **Optimistic Offline Lock**, this countermeasure ensures that the saga uses consistent data, minimizing conflicts or overwrites from other operations.

- **Version File**: This technique logs each update to maintain the correct order of operations. By recording each transaction, the system can reorder actions to maintain sequence integrity. This countermeasure effectively transforms non-commutative actions into commutative ones, supporting data consistency even when transactions are processed out of order.

- **By Value**: This approach dynamically selects a concurrency mechanism based on the business risk associated with each request. For low-risk actions, sagas with countermeasures may suffice. However, for high-risk transactions (e.g., financial transfers), distributed transactions ensure more rigorous consistency. This strategy allows a flexible balance between business risk, availability, and scalability.

These countermeasures collectively help maintain data integrity and minimize concurrency anomalies in systems where strict isolation is unfeasible.


## Why cannot we use a distributed transaction?

Distributed transactions, managed through the X/Open Distributed Transaction Processing (DTP) Model and typically implemented with two-phase commit (2PC), ensure that all participants in a transaction either commit or roll back together. While this approach may seem straightforward, it has significant limitations.

One major issue is that many modern technologies, including NoSQL databases like MongoDB and Cassandra, as well as modern message brokers like RabbitMQ and Apache Kafka, do not support distributed transactions. Additionally, distributed transactions are synchronous, meaning that all participating services must be available for the transaction to complete. This requirement reduces the overall availability of the system because the availability of the entire transaction is the product of the availability of each service involved. According to the CAP theorem, systems can only achieve two out of three properties: consistency, availability, and partition tolerance. Modern architectures often prioritize availability over consistency.

Although distributed transactions offer a familiar programming model similar to local transactions, these challenges make them unsuitable for modern applications. Instead, to maintain data consistency in a microservices architecture, a different approach is needed one that leverages loosely coupled, asynchronous services. This is where the SAGA pattern comes into play.

![](images/transactions.webp)

In the image there is an example of what we have mentioned at the beginning of the paragraph. It illustrates `createOrder()` operation in a microservices architecture. This operation involves multiple services and must ensure data consistency across them. The diagram shows the `Order Service`, `Consumer Service`, `Kitchen Service`, and `Accounting Service`, each represented by hexagons.

- The `Order controller` initiates the `createOrder()` process.
- The `Order Service` reads data from the `Consumer Service`, which manages consumer information.
- The `Order Service` then writes data to both the `Kitchen Service`, which handles ticket information, and the `Accounting Service`, which manages account details.

The dashed box around these services indicates the need for data consistency when performing these operations. The figure emphasizes that the `createOrder()` operation must update data across several services, requiring a mechanism to maintain consistency.

### Implementing the SAGA Pattern

To replace traditional distributed transactions, the **SAGA Pattern** offers a solution for coordinating transactions across services in a microservices architecture. Two primary approaches can be used to implement SAGA:

- **Choreography**: Here, each service involved in the saga performs its local transaction and then publishes an event to notify other services that the next step can begin. This approach is decentralized: each service reacts to events and performs its designated task. While this simplifies design by removing the need for a central controller, complexity can increase with more interactions, making the saga harder to track and manage.

  ![Choreography Example](images/choreography.webp)

- **Orchestration**: This approach relies on a centralized **saga orchestrator** to coordinate the transaction steps. The orchestrator sends commands to each service, instructing them to execute their part of the saga. If any step fails, the orchestrator initiates compensating actions to roll back prior steps, ensuring consistency. Orchestration provides better control and visibility, though it introduces a potential single point of failure and can make the orchestrator’s logic more complex.

  ![Orchestration Example](images/orchestration.webp)

Both approaches aim to achieve data consistency across distributed services without relying on traditional ACID transactions. The choice between choreography and orchestration depends on the complexity and control requirements of the system.

## Why is orchestration more widespread?

### Limitations of Choreography

- **Tight Coupling**: Services are directly connected, meaning changes in one service can impact others, complicating upgrades.
- **Distributed State**: Managing state across microservices complicates process tracking and may require additional infrastructure.
- **Troubleshooting Complexity**: Debugging is harder with dispersed service flows, requiring centralized logging and deep code knowledge.
- **Testing Challenges**: Interconnected microservices make testing more complex for developers.
- **Maintenance Difficulty**: As services evolve, adding new versions can reintroduce complexity, resembling a distributed monolith.

### Advantages of Orchestration

- **Coordinated Transactions**: A central coordinator manages the execution of microservices, ensuring consistent transactions across the system.
- **Compensation**: Supports rollback through compensating transactions in case of failures, maintaining system consistency.
- **Asynchronous Processing**: Microservices operate independently, with the orchestrator managing communication and sequencing.
- **Scalability**: Easily scale by adding or modifying services without major impact on the overall application.
- **Visibility and Monitoring**: Centralized visibility enables quicker issue detection and resolution, improving system reliability.
- **Faster Time to Market**: Simplifies service integration and flow creation, speeding up adaptation and reducing the time to market.

## References

* [Microservices Patterns - O'Reilly](https://www.oreilly.com/library/view/microservices-patterns/9781617294549/)
* [Microservices Pattern - SAGA](https://microservices.io/patterns/data/saga.html)
