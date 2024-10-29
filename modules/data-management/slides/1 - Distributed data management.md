# Distributed data management

## ACID Properties in Transactions

The **ACID** properties—**Atomicity, Consistency, Isolation, and Durability**—define the reliability requirements for transactions in traditional database systems. These properties ensure that transactions are processed in a predictable, reliable manner, even in the event of system failures.

- **Atomicity**: This property ensures that a transaction is treated as a single, indivisible unit. If any part of the transaction fails, the entire transaction is rolled back, leaving the system in its original state. This "all-or-nothing" approach guarantees that either all steps of a transaction are completed successfully, or none are applied at all.

- **Consistency**: Consistency guarantees that a transaction brings the database from one valid state to another. Each transaction must comply with all predefined rules, constraints, and triggers to preserve the integrity of the data. This means that a completed transaction should leave the database in a valid state, reflecting the system’s business rules and constraints.

- **Isolation**: Isolation ensures that transactions are executed independently, without interference. Each transaction should act as if it is the only one interacting with the database, even if others are running concurrently. Isolation is essential for preventing issues such as **dirty reads** (reading uncommitted data), **non-repeatable reads** (different data returned in repeated reads within a transaction), and **phantom reads** (new data appears in repeated reads within a transaction).

- **Durability**: This property ensures that once a transaction is committed, its results are permanent, even in case of power loss, crashes, or other failures. Changes made by a committed transaction are stored in persistent storage and are guaranteed to survive any subsequent system failures.

Together, these ACID properties are essential in maintaining data accuracy, consistency, and reliability in traditional, centralized databases, enabling them to handle complex, critical operations reliably. In distributed systems, however, achieving strict ACID properties becomes challenging, especially for Isolation, leading to the need for alternative approaches like the **SAGA Pattern** in microservices architectures.

In a distributed system, achieving strict **isolation** across services and databases is challenging due to the decentralized nature of data and processing. Here’s an example illustrating why isolation is not guaranteed in a distributed context:

## Faulty Transaction Due to Missing Isolation

**Scenario**: Imagine an e-commerce platform that consists of several microservices, including an **Order Service**, a **Consumer Service**, a **Ticket Service**, and an **Accounting Service**. When a customer places an order, the Order Service needs to perform the following actions in a distributed transaction:

1. **Update the Consumer Service** to mark the consumer’s account as having a new order.
2. **Create a ticket** in the Ticket Service for the purchased item.
3. **Deduct the order amount** from the consumer's account in the Accounting Service.

To maintain data consistency, the Order Service attempts to execute this transaction across the three services. The dashed box around these services indicates the need for data consistency when performing these operations. The figure emphasizes that the `createOrder()` operation must update data across several services, requiring a mechanism to maintain consistency.

![](images/transactions.webp)





#### Step-by-Step Breakdown of the Faulty Transaction

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


### Anomalies Caused by Lack of Isolation

Without adequate isolation, several read anomalies can occur, leading to inconsistent or unexpected results. The three primary types of read anomalies are **Dirty Reads**, **Non-Repeatable Reads**, and **Phantom Reads**.

- **Dirty Reads**: This anomaly occurs when a transaction reads data that has been modified by another transaction but not yet committed. If the other transaction rolls back or fails, the data read by the first transaction becomes invalid. Dirty reads can lead to decisions based on data that might change, resulting in errors or inconsistencies.

  *Example:* Transaction A updates a customer’s credit balance but hasn’t committed. Transaction B reads this uncommitted balance to approve a credit limit, but then Transaction A rolls back. As a result, Transaction B has made a decision based on incorrect information.

- **Non-Repeatable Reads**: In this anomaly, a transaction reads the same data multiple times but receives different values because another transaction has modified the data in between reads. This inconsistency can cause issues when a transaction expects data to remain stable over its duration.

  *Example:* Transaction A reads an account balance. Meanwhile, Transaction B modifies this balance and commits. When Transaction A reads the balance again, it sees a different value, leading to unexpected results if it assumes stability in the data.

- **Phantom Reads**: Phantom reads occur when a transaction reads a set of rows that satisfy a certain condition, but another transaction inserts, updates, or deletes rows that affect the result set during the course of the first transaction. As a result, the first transaction sees different data when it re-executes the same query.

  *Example:* Transaction A reads all accounts with a balance over $10,000. Simultaneously, Transaction B inserts a new account with a balance of $15,000 and commits. If Transaction A re-executes the query, it will include the new account, resulting in a different set of results or “phantom” rows.

These read anomalies highlight the challenges in achieving consistent and isolated transaction behavior, especially in concurrent and distributed environments. Addressing them requires implementing appropriate isolation levels, such as **Serializable** or **Repeatable Read**, though strict isolation can affect system performance and scalability.


## The X/Open Distributed Transaction Processing (DTP) Model

**Distributed transactions**, managed through the **X/Open Distributed Transaction Processing (DTP) Model**, are designed to ensure that all participants in a transaction either commit or roll back their changes together. Typically implemented using the **two-phase commit (2PC)** protocol, this approach provides a familiar programming model that resembles local transactions. However, it comes with significant limitations that make it less suitable for modern applications:

* Many contemporary technologies, including popular **NoSQL databases** like **MongoDB** and **Cassandra**, as well as modern messaging systems such as **RabbitMQ** and **Apache Kafka**, do not inherently support distributed transactions. This lack of support complicates the implementation of 2PC in environments where these technologies are prevalent. 
* Distributed transactions are inherently **synchronous**. This means that all participating services must be available and responsive for the transaction to complete successfully. Consequently, the overall availability of the system is contingent upon the availability of each individual service involved in the transaction. This interdependence can significantly reduce system resilience. According to the **CAP theorem**, which states that a distributed system can only guarantee two out of the following three properties—**Consistency**, **Availability**, and **Partition Tolerance**—modern architectures frequently prioritize availability over strict consistency.

## CAP Theorem

The [CAP theorem](https://mwhittaker.github.io/blog/an_illustrated_proof_of_the_cap_theorem/), also known as Brewer's theorem, is a fundamental principle in distributed systems that states it is impossible for a distributed data store to simultaneously provide all three of the following guarantees:

1. **Consistency (C)**: Every read operation receives the most recent write result or an error. In other words, all nodes in the system return the same data at the same time, ensuring that every client sees the same information, regardless of which node they connect to. This requires coordination among nodes to ensure that updates are propagated correctly.

2. **Availability (A)**: Every request (read or write) receives a response, either with the requested data or an acknowledgment of the write operation, regardless of the state of any node in the system. This means that the system remains operational and responsive, even when some nodes are down or experiencing issues.

3. **Partition Tolerance (P)**: The system continues to operate despite arbitrary network partitions that prevent some nodes from communicating with others. This ensures that even in the face of network failures, the system can still function and respond to requests, albeit possibly sacrificing consistency or availability.

The CAP theorem posits that a distributed system can only guarantee two out of the three properties at any given time. This trade-off arises from the inherent challenges of managing data across multiple nodes, particularly in the presence of network failures or latencies.

### Navigating Database Choices with the CAP Theorem
Different applications demand different priorities in terms of data consistency, system availability, and tolerance to network partitions:

- **CA Systems (Consistency and Availability)**: These systems maintain consistency and availability as long as there are no network partitions. However, if a partition occurs, the system must sacrifice either consistency or availability to maintain operations. An example of this might be a traditional relational database that ensures consistency and availability under normal conditions but may become unavailable during network issues.

- **CP Systems (Consistency and Partition Tolerance)**: These systems prioritize consistency and partition tolerance but may sacrifice availability in the event of a partition. If a network failure occurs, these systems may become unavailable until consistency can be restored across nodes. An example is a distributed database that chooses to block requests during a network partition to ensure that all nodes remain consistent.

- **AP Systems (Availability and Partition Tolerance)**: These systems focus on availability and partition tolerance, accepting that some data may be stale or inconsistent during network partitions. They continue to respond to requests even if some nodes cannot communicate. NoSQL databases like Cassandra or DynamoDB are examples of AP systems, allowing for high availability and responsiveness even in the face of network failures.

![](images/cap-theorem.webp)

Here's how popular databases measure up against the CAP dimensions:

- **Relational Databases (MySQL, PostgreSQL, and SAP Hana)** - Consistency and Availability: Applications requiring transactional integrity and complex queries, such as financial systems and ERP solutions.
- **NoSQL Databases (DynamoDB, Cassandra, Couchbase)** - Availability and Partition Tolerance: Scalable applications with flexible data models and the need for high availability, like social media platforms and real-time analytics systems.
- **Analytical Databases (Vertica, Redshift)** - Consistency and Partition Tolerance: Data warehousing and business intelligence applications requiring fast queries over large datasets.
- **Graph Databases (Neo4j)** - Consistency and Availability: It focuses on maintaining data integrity in highly connected data. Applications with complex relationships and network analysis features, such as recommendation engines and fraud detection systems.
- **Document Stores (MongoDB) and Key-Value Stores (Redis)** - Consistency and Partition Tolerance: Applications needing schema flexibility (MongoDB) or high-performance caching and real-time operations (Redis).
- **Column Stores (HBase)** - Consistency and Partition Tolerance: Big data applications requiring efficient read/write access to large datasets, such as time-series data and event logging.


## Implementing the SAGA Pattern

Given these challenges, the SAGA pattern provides a framework for managing complex transactions in distributed systems by breaking them down into smaller, manageable sub-transactions. Each sub-transaction is executed independently, allowing for greater flexibility and resilience. If a sub-transaction fails, compensating transactions can be triggered to roll back the effects of previously completed steps, thus maintaining overall data consistency without the tight coupling and synchronous constraints of traditional distributed transactions.

Two primary approaches can be used to implement SAGA:

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
