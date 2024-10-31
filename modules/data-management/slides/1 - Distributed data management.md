# Distributed data management

Distributed transactions play a crucial role in microservice architectures by enabling coordinated operations across multiple, often heterogeneous, data stores and services. Unlike traditional transactions that occur within a single database, distributed transactions involve multiple participants, necessitating strategies to ensure that all operations either complete successfully or are rolled back in the event of failure.

The challenge of distributed transactions lies in balancing the conflicting demands of consistency, availability, and partition tolerance, as articulated by the CAP theorem. As systems scale, it requires sophisticated protocols and patterns to manage the intricacies of network communication, data integrity, and error handling. Protocols such as the **Two-Phase Commit (2PC)** and the **SAGA pattern** have emerged as solutions to navigate these challenges, each with its own strengths and weaknesses.

As organizations adopt microservices architectures and cloud-based solutions, understanding distributed transactions is essential for developers and architects who must design resilient systems capable of handling failures while maintaining data consistency. 

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


## ACID Properties in Transactions

The **ACID** properties—**Atomicity, Consistency, Isolation, and Durability**—define the reliability requirements for transactions in traditional database systems. These properties ensure that transactions are processed in a predictable, reliable manner, even in the event of system failures.

- **Atomicity**: This property ensures that a transaction is treated as a single, indivisible unit. If any part of the transaction fails, the entire transaction is rolled back, leaving the system in its original state. This "all-or-nothing" approach guarantees that either all steps of a transaction are completed successfully, or none are applied at all.

- **Consistency**: Consistency guarantees that a transaction brings the database from one valid state to another. Each transaction must comply with all predefined rules, constraints, and triggers to preserve the integrity of the data. This means that a completed transaction should leave the database in a valid state, reflecting the system’s business rules and constraints.

- **Isolation**: Isolation ensures that transactions are executed independently, without interference. Each transaction should act as if it is the only one interacting with the database, even if others are running concurrently. Isolation is essential for preventing issues such as **dirty reads** (reading uncommitted data), **non-repeatable reads** (different data returned in repeated reads within a transaction), and **phantom reads** (new data appears in repeated reads within a transaction).

- **Durability**: This property ensures that once a transaction is committed, its results are permanent, even in case of power loss, crashes, or other failures. Changes made by a committed transaction are stored in persistent storage and are guaranteed to survive any subsequent system failures.

Together, these ACID properties are essential in maintaining data accuracy, consistency, and reliability in traditional, centralized databases, enabling them to handle complex, critical operations reliably. In distributed systems, however, achieving strict ACID properties becomes challenging, especially for Isolation, leading to the need for alternative approaches.

## Faulty Transaction Due to Missing Isolation

**Scenario**: Imagine an e-commerce platform that consists of several microservices, including an **Order Service**, a **Consumer Service**, a **Ticket Service**, and an **Accounting Service**. When a customer places an order, the Order Service needs to perform the following actions in a distributed transaction:

1. **Update the Consumer Service** to mark the consumer’s account as having a new order.
2. **Create a ticket** in the Ticket Service for the purchased item.
3. **Deduct the order amount** from the consumer's account in the Accounting Service.

To maintain data consistency, the Order Service attempts to execute this transaction across the three services. The dashed box around these services indicates the need for data consistency when performing these operations. The figure emphasizes that the `createOrder()` operation must update data across several services, requiring a mechanism to maintain consistency.

![](images/transactions.webp)

### Step-by-Step Breakdown of the Faulty Transaction

1. **Initial Order Request**: A customer places an order for a concert ticket costing $100. The Order Service initiates a distributed transaction to update the Consumer, Ticket, and Accounting Services.

2. **Consumer Service Update**:
  - The Order Service updates the Consumer Service to reflect that the customer has placed a new order.
  - This update successfully records the order in the consumer's profile.

3. **Ticket Service Creation**:
  - The Order Service sends a request to the Ticket Service to create a new ticket for the customer.
  - This operation also succeeds, and the ticket is generated.

4. **Accounting Service Deduction**:
  - The Order Service attempts to deduct $100 from the consumer's account in the Accounting Service.
  - Before this operation completes, a concurrent transaction occurs: the customer, unaware of the ongoing order process, decides to purchase another item for $50, leading the Accounting Service to deduct $50 from the same account.

5. **Failure to Deduct**:
  - When the Order Service finally processes the deduction of $100, it checks the consumer's account balance, which is now $50 (after the concurrent deduction).
  - Since the balance is insufficient, the Accounting Service raises an error and rolls back the $100 deduction.

6. **Inconsistent State**:
  - At this point, we have an inconsistent state:
    - The Consumer Service shows that an order has been placed.
    - The Ticket Service has generated a ticket.
    - The Accounting Service has not deducted the $100 for the order due to the insufficient balance.
  - The system's state is now inconsistent because the order exists in the system, but the payment has not been processed.

### Why Isolation Failed

In this example, isolation failed for several reasons:

- **Concurrent Modifications**: The lack of isolation allowed another transaction to modify the consumer's account balance while the Order Service was still processing the transaction. As a result, the Order Service was unable to accurately reflect the real-time state of the account.

- **No Locking Mechanism**: The system did not implement any form of locking to prevent concurrent transactions from interfering with one another. This lack of locking leads to race conditions where multiple transactions access shared resources simultaneously without proper coordination.

- **Error Propagation**: The failure in the Accounting Service did not trigger compensating actions in the previous services (Consumer and Ticket), leaving the overall system in an inconsistent state.


## Two-Phase Commit (2PC) Protocol

The Two-Phase Commit (2PC) protocol is a distributed algorithm that ensures all participants in a distributed transaction agree on whether to commit or abort the transaction. It is designed to achieve atomicity in scenarios where multiple databases or services are involved. Here’s a more detailed breakdown of the protocol, its phases, advantages, disadvantages, and some practical considerations.

2PC consists of two main phases: the **Prepare Phase** and the **Commit Phase**.

1. **Prepare Phase**:
  - **Initiation**: A designated coordinator initiates the transaction and sends a `prepare` request to all participating nodes (also known as participants). This request includes details about the transaction.
  - **Response from Participants**: Each participant processes the request and checks if it can commit the transaction. This typically involves checking local conditions, such as resource availability and data integrity.
  - **Voting**: Each participant responds to the coordinator with either a `Yes` (indicating readiness to commit) or a `No` (indicating inability to commit). If a participant votes `No`, it may also provide reasons for its decision.

2. **Commit Phase**:
  - **Decision**: If all participants respond with `Yes`, the coordinator moves to the commit phase. If any participant responds with `No`, the coordinator issues a `rollback` command to all participants.
  - **Final Actions**: Upon receiving a commit command, each participant commits the transaction, making all changes permanent. In case of a rollback, participants undo any changes made in anticipation of the transaction.

**Advantages of 2PC**

- **Atomicity**: 2PC guarantees that a transaction will either be fully committed or fully aborted, ensuring data consistency across all participants.
- **Simplicity**: The protocol is straightforward to understand and implement, which makes it a popular choice for systems requiring strong consistency.

**Disadvantages of 2PC**

- **Blocking Nature**: During the prepare phase, if a participant crashes or becomes unresponsive, the remaining participants cannot make progress, leading to a blocking situation. This can severely impact system availability.
- **Single Point of Failure**: The coordinator is a single point of failure. If the coordinator fails during the transaction, participants may be left in an uncertain state, unsure whether to commit or rollback.
- **Performance Overhead**: The synchronous communication required between the coordinator and participants can introduce latency, especially in high-latency networks or systems with many participants.
- **Limited Scalability**: In large distributed systems, the overhead and blocking nature of 2PC can limit scalability, as the protocol requires coordination across multiple nodes.


## The SAGA Pattern

The Saga pattern is an architectural pattern for managing long-lived transactions in distributed systems. Unlike 2PC, which attempts to ensure atomicity across all participants, the Saga pattern breaks a transaction into a series of smaller, isolated transactions that are executed sequentially. Each step in the Saga can be independently committed, and if any step fails, compensating transactions are used to undo the effects of previous steps.

### Types of Sagas

**Choreography**

Each service involved in the saga performs its local transaction and then publishes an event to notify other services that the next step can begin. This approach is decentralized: each service reacts to events and performs its designated task. While this simplifies design by removing the need for a central controller, overall complexity usually increases making the saga harder to track and manage.

- **Tight Coupling**: Services are directly connected, meaning changes in one service can impact others, complicating upgrades.
- **Distributed State**: Managing state across microservices complicates process tracking and may require additional infrastructure.
- **Troubleshooting Complexity**: Debugging is harder with dispersed service flows, requiring centralized logging and deep code knowledge.
- **Testing Challenges**: Interconnected microservices make testing more complex for developers.
- **Maintenance Difficulty**: As services evolve, adding new versions can reintroduce complexity, resembling a distributed monolith.

![Choreography Example](images/choreography.webp)

**Orchestration**

A centralized **saga orchestrator** coordinates the transaction steps. The orchestrator sends commands to each service, instructing them to execute their part of the saga. If any step fails, the orchestrator initiates compensating actions to roll back prior steps, ensuring consistency. Orchestration provides better control and visibility, though it introduces a potential single point of failure and can make the orchestrator’s logic more complex.

- **Coordinated Transactions**: A central coordinator manages the execution of microservices, ensuring consistent transactions across the system.
- **Compensation**: Supports rollback through compensating transactions in case of failures, maintaining system consistency.
- **Asynchronous Processing**: Microservices operate independently, with the orchestrator managing communication and sequencing.
- **Scalability**: Easily scale by adding or modifying services without major impact on the overall application.
- **Visibility and Monitoring**: Centralized visibility enables quicker issue detection and resolution, improving system reliability.
- **Faster Time to Market**: Simplifies service integration and flow creation, speeding up adaptation and reducing the time to market.

![Orchestration Example](images/orchestration.webp)

**Advantages of SAGA**

- **Non-Blocking**: The Saga pattern allows for asynchronous execution, meaning that if one step fails, others can still proceed, preventing the blocking issues associated with 2PC.
- **Decoupled Services**: The choreography-based approach allows services to operate independently, making it easier to evolve and deploy them without affecting the overall system.
- **Compensating Actions**: Each transaction step has a corresponding compensating transaction, allowing for more granular control over state changes and rollback behavior.

**Disadvantages of SAGA**

- **Complexity**: Implementing compensating transactions can be complex and requires careful consideration to avoid inconsistencies.
- **Eventual Consistency**: Sagas typically achieve eventual consistency rather than strong consistency, which may not be acceptable for all applications.
- **Error Handling**: Managing failures and compensations can complicate the logic and flow of applications, requiring robust monitoring and error-handling mechanisms.


## References

* [Microservices Patterns - O'Reilly](https://www.oreilly.com/library/view/microservices-patterns/9781617294549/)
* [Microservices Pattern - SAGA](https://microservices.io/patterns/data/saga.html)
