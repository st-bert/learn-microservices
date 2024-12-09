# Distributed data management

From its core principles and true context, a [microservice](https://www.baeldung.com/spring-microservices-guide)-based application is a distributed system. The overall system consists of multiple smaller services, and together these services provide the overall application functionality.

Although this architectural style provides numerous benefits, it has several limitations as well. One of the major problems in a microservice architecture is how to handle a [transaction that spans multiple services](https://www.baeldung.com/transactions-across-microservices).


## Database per Service Pattern

One of the benefits of microservice architecture is that we can choose the technology stack per service. For instance, we can decide to use a relational database for service A and a NoSQL database for service B.

This model lets the service manage domain data independently on a data store that best suites its data types and schema. Further, it also lets the service scale its data stores on demand and insulates it from the failures of other services.

However, at times a [transaction](https://www.baeldung.com/transactions-intro) can span across multiple services, and ensuring data consistency across the service database is a challenge. 

## Challenges of Distributed Transaction

To demonstrate the use of distributed transactions, we'll take an example of an e-commerce application that processes online orders and is implemented with microservice architecture.

There is a microservice to create the orders, one that processes the payment, another that updates the inventory and the last one that delivers the order. Each of these microservices performs a local transaction to implement the individual functionalities:

![distributed transaction](https://www.baeldung.com/wp-content/uploads/sites/4/2021/04/distributed-transaction.png)

This is an example of a distributed transaction as the transaction boundary crosses multiple services and databases.

To ensure a successful order processing service, all four microservices must complete the individual local transactions. If any of the microservices fail to complete its local transaction, all the completed preceding transactions should roll back to ensure data integrity.

**The first challenge is maintaining ACID**. To ensure the correctness of a transaction, it must be Atomic, Consistent, Isolated and Durable (ACID).
* *Atomicity* ensures that all or none of the steps of a transaction should complete.
* *Consistency* takes data from one valid state to another valid state. 
* *Isolation* guarantees that concurrent transactions should produce the same result that sequentially transactions would have produced. 
* *Durability* means that committed transactions remain committed irrespective of any type of system failure.

**The second challenge is managing the transaction isolation level**. It specifies the amount of data that is visible in a transaction when the other services access the same data simultaneously. In other words, if one object in one of the microservices is persisted in the database while another request reads the data, should the service return the old or new data?

## CAP Theorem
The [CAP theorem](https://mwhittaker.github.io/blog/an_illustrated_proof_of_the_cap_theorem/), is a fundamental principle in distributed systems that states it is impossible for a distributed data store to simultaneously provide all three of the following guarantees:

1. **Consistency (C)**: Every read operation receives the most recent write result or an error. In other words, all nodes in the system return the same data at the same time, ensuring that every client sees the same information, regardless of which node they connect to. This requires coordination among nodes to ensure that updates are propagated correctly.

2. **Availability (A)**: Every request (read or write) receives a response, either with the requested data or an acknowledgment of the write operation, regardless of the state of any node in the system. This means that the system remains operational and responsive, even when some nodes are down or experiencing issues.

3. **Partition Tolerance (P)**: The system continues to operate despite arbitrary network partitions that prevent some nodes from communicating with others. This ensures that even in the face of network failures, the system can still function and respond to requests, albeit possibly sacrificing consistency or availability.

The CAP theorem posits that a distributed system can only guarantee two out of the three properties at any given time. This trade-off arises from the inherent challenges of managing data across multiple nodes, particularly in the presence of network failures or latencies.

![](images/cap-theorem.webp)

### CA Systems (Consistency and Availability)
- **Definition**: CA systems ensure **Consistency** and **Availability** but do not provide **Partition Tolerance**.
- **Characteristics**:
    - Every request (read or write) gets a consistent response, meaning that all nodes in the system have the same data at the same time.
    - The system is available, meaning every request receives a response (even if it’s an error).
    - However, these systems assume a reliable network with minimal or no network partitions. In case of a network partition, the system may fail to provide both consistency and availability.
- **Examples**:
    - **Relational databases in a single data center** (e.g., MySQL, PostgreSQL).
    - **Centralized, tightly coupled distributed systems** within stable network environments.
- **Use Cases**: CA systems are ideal for environments where network reliability is high, and both consistency and availability are critical. Examples include:
    - **Banking systems within a single data center** where transactions need to be consistent and highly available.
    - **Enterprise Resource Planning (ERP) systems** with minimal network failure risks.

### CP Systems (Consistency and Partition Tolerance)
- **Definition**: CP systems ensure **Consistency** and **Partition Tolerance** but do not guarantee **Availability** during network partitions.
- **Characteristics**:
    - They prioritize consistency, meaning all nodes have the same, up-to-date data.
    - They tolerate network partitions, meaning the system can still function even if parts of it cannot communicate with others.
    - If a network partition occurs, CP systems may sacrifice availability by blocking some requests to maintain consistency across nodes.
- **Examples**:
    - **Zookeeper**: A coordination and configuration management service that prioritizes consistency.
    - **HBase** and **Redis** (in specific configurations): Systems that prioritize strong consistency and may block requests if partitioned to ensure all nodes stay synchronized.
- **Use Cases**: CP systems are suitable for applications where data accuracy and consistency are critical, even at the cost of availability during network issues. Examples include:
    - **Banking and financial transactions**, where inconsistency can lead to major errors.
    - **Configuration management systems** where all nodes need to read consistent settings (e.g., for distributed services).

### AP Systems (Availability and Partition Tolerance)
- **Definition**: AP systems ensure **Availability** and **Partition Tolerance** but do not guarantee **Consistency**.
- **Characteristics**:
    - The system remains available during network partitions, meaning it can continue to serve requests even if parts of the system are isolated.
    - Partition tolerance ensures that the system can handle network failures without going offline.
    - Since consistency is not guaranteed, AP systems may allow different parts of the system to return different data during partitions, resulting in eventual consistency once the partition is resolved.
- **Examples**:
    - **Cassandra** and **DynamoDB**: NoSQL databases that prioritize availability and partition tolerance, offering eventual consistency.
    - **Couchbase** and **Riak**: AP databases that are often used in large-scale, distributed applications.
- **Use Cases**: AP systems are suitable for applications that require high availability and can tolerate temporary inconsistencies. Examples include:
    - **Social media platforms** where users can tolerate minor delays in seeing consistent data.
    - **Content delivery networks (CDNs)** where availability is critical to serve content globally despite network partitions.



## Two-Phase Commit (2PC)

The Two-Phase Commit protocol (2PC) is a widely used pattern to implement distributed transactions. We can use this pattern in a microservice architecture to implement distributed transactions.

In a two-phase commit protocol there are two key components: 
* the coordinator component that is responsible for controlling the transaction and contains the logic to manage the transaction.
* the participating nodes (e.g., the microservices) that run their local transactions.

As the name indicates, the two-phase commit protocol runs a distributed transaction in two phases:

1.  **Phase 1 (Prepare)** -- The coordinator asks the participating nodes whether they are ready to commit the transaction. The participants returned with a *yes* or *no*.
2.  **Phase 2 (Commit)** -- If all the participating nodes respond affirmatively in phase 1, the coordinator asks all of them to commit. If at least one node returns negative, the coordinator asks all participants to roll back their local transactions.

![two phase commit](https://www.baeldung.com/wp-content/uploads/sites/4/2021/04/two-phase-commit.png)


### Problems With 2PC

Although 2PC is useful to implement a distributed transaction, it has the following shortcomings:

* The coordinator node can become the single point of failure. 
* All other services need to wait until the slowest service finishes its confirmation. So, the overall performance of the transaction is bound by the slowest service. 
* The two-phase commit protocol is slow by design due to the chattiness and dependency on the coordinator. So, it can lead to scalability and performance issues in a microservice-based architecture involving multiple services. 
* Two-phase commit protocol is not supported in NoSQL databases. Therefore, in a microservice architecture where one or more services use NoSQL databases, we can't apply a two-phase commit.

### Two-Phase Commit (2PC) and CAP Theorem

2PC aligns with CP in the CAP theorem, prioritizing Consistency and Partition Tolerance over Availability. It is suitable for systems where strong consistency is crucial and where network reliability is high, such as financial systems or databases requiring strict data integrity.

* **Consistency (C)**: 2PC ensures that all participants in a transaction have a consistent state by coordinating a commit or rollback across all nodes. All nodes either commit the transaction or roll it back, achieving atomicity and ensuring data consistency at the expense of availability.

* **Availability (A)**: Because 2PC requires each participant to wait for all others to reach a decision, it is inherently blocking. In the event of a failure or timeout, nodes may be left in an uncertain state, which can limit availability. During a network partition, if the coordinator or any participant is unavailable, 2PC will block the transaction, reducing availability in favor of consistency.

* **Partition Tolerance (P)**: 2PC does not handle network partitions well, as the protocol relies on synchronous responses from all participants. A network failure during the transaction can leave nodes in a blocked or uncertain state. Consequently, 2PC does not prioritize partition tolerance and struggles to function effectively in environments where partitions are common.


## The Saga Architecture Pattern

The Saga pattern, [introduced in 1987 by Hector Garcia Molina & Kenneth Salem](https://www.cs.cornell.edu/andru/cs711/2002fa/reading/sagas.pdf), defines a saga as a sequence of transactions that can be interleaved with one another.

* A local transaction is the unit of work performed by a Saga participant. 
* Every operation that is part of the Saga can be rolled back by a compensating transaction.
* The Saga pattern guarantees that either all operations complete successfully or the corresponding compensation transactions are run to undo the work previously completed.

In the Saga pattern, a compensating transaction must be *idempotent* and *retryable*. These two principles ensure that we can manage transactions without any manual intervention.

![saga pattern](https://www.baeldung.com/wp-content/uploads/sites/4/2021/04/saga-pattern.png)


### Saga Pattern and CAP Theorem

The Saga pattern aligns with **AP** in the CAP theorem, favoring **Availability** and **Partition Tolerance** while accepting **eventual consistency**. It is ideal for systems where high availability and partition tolerance are priorities, and where eventual consistency is acceptable, such as e-commerce order processing, travel bookings, and other long-running workflows.

- **Consistency (C)**: Sagas achieve **eventual consistency** rather than strong consistency. If a failure occurs during one of the steps, compensating transactions are executed to reverse the effects of previous steps. While this approach ensures that the system will reach a consistent state eventually, it does not guarantee immediate consistency across all nodes.

- **Availability (A)**: Sagas are non-blocking, allowing other parts of the system to proceed even if some steps are still being executed or a step fails. In a network partition, steps can continue on available nodes, improving the system's availability compared to 2PC.

- **Partition Tolerance (P)**: The Saga pattern is well-suited for partition tolerance since it does not require synchronous communication across all nodes. Steps in a Saga can be executed asynchronously, and nodes do not need to coordinate a global commit. This enables the system to handle partitions gracefully, with compensating actions mitigating inconsistencies after the partition is resolved.





### The Saga Execution Coordinator

The Saga Execution Coordinator is the central component to implement a Saga flow. It contains a Saga log that captures the sequence of events of a distributed transaction.

* For any failure, the SEC component inspects the Saga log to identify the impacted components and the sequence in which the compensating transactions should run.

* For any failure in the SEC component, it can read the Saga log once it's coming back up. It can then identify the transactions successfully rolled back, which ones are pending, and can take appropriate actions:

![saga execution](https://www.baeldung.com/wp-content/uploads/sites/4/2021/04/saga-execution.png)


### Saga Orchestration Pattern

In the Orchestration pattern, a single orchestrator is responsible for managing the overall transaction status. If any of the microservices encounter a failure, the orchestrator is responsible for invoking the necessary compensating transactions.

The following diagram demonstrates the successful Saga flow for the online order processing application:

![saga coreography](https://www.baeldung.com/wp-content/uploads/sites/4/2021/04/saga-coreography.png)

In the event of a failure, the microservice reports the failure to SEC, and it is the SEC's responsibility to invoke the relevant compensation transactions:

![saga coreography 2](https://www.baeldung.com/wp-content/uploads/sites/4/2021/04/saga-coreography-2.png)

In this example, the Payment microservice reports a failure, and the SEC invokes the compensating transaction to unblock the seat. If the call to the compensating transaction fails, it is the SEC's responsibility to retry it until it is successfully completed. Recall that in Saga, a compensating transaction must be *idempotent* and *retryable*.

The Saga orchestration pattern is useful for brownfield microservice application development architecture. In other words, this pattern works when we already have a set of microservices and would like to implement the Saga pattern in the application. We need to define the appropriate compensating transactions to proceed with this pattern.

### Saga Choreography Pattern

In the Saga Choreography pattern, each microservice that is part of the transaction publishes an event that is processed by the next microservice. In the Saga, choreography flow is successful if all the microservices complete their local transaction, and none of the microservices reported any failure.

![saga orchestration](https://www.baeldung.com/wp-content/uploads/sites/4/2021/04/saga-orchestration.png)

The Choreography pattern works well when there are fewer participants in the transaction.

### Saga Frameworks
Here are a few frameworks available to implement the Saga pattern:
* [Orkes Conductor](https://www.orkes.io/what-is-conductor)
* [Eclipse MicroProfile LRA](https://github.com/eclipse/microprofile-lra)
* [Eventuate Tram Saga](https://eventuate.io/docs/manual/eventuate-tram/latest/getting-started-eventuate-tram-sagas.html)
* [Seata](https://www.seata.io/docs/dev/mode/saga-mode/)
* [Camunda](https://camunda.com/)
* [Apache Camel](https://camel.apache.org/components/latest/eips/saga-eip.html) 


## References