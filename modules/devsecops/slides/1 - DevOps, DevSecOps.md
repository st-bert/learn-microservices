# DevOps, DevSecOps

## Introduction

Once we have complete the development of our microservice, we want to make it available to other microservices in a production environment.

So, we need to push it into a **Container Registry**, that store all version of our container image.

There are many **Container Registry** that we can use, such us **Docker Hub**, **GitLab Container Registry**, **GitHub Packages Registry** or we can also host our **personal Docker Registry**.

To **build** and **publish** a new version of our container image we need first to **login** to our container registry:

```bash
docker login registry.gitlab.com -u myusername --password-stdin
```

**Build** a new image with the **latest** tag:

```bash
docker build -t registry.gitlab.com/michelemosca/cloudedgecomputing:latest .
```

And **push** it into the container registry:

```bash
docker push registry.gitlab.com/michelemosca/cloudedgecomputing --all-tags
```

Once the new version of our container image is available inside the container registry, other microservices can interact with this new version by adding it as **image** inside the **docker-compose** file:

```yml
web:
    image: registry.gitlab.com/michelemosca/cloudedgecomputing:latest
    stop_signal: SIGINT
    environment:
      - SQLALCHEMY_DATABASE_URI=${SQLALCHEMY_DATABASE_URI}
    ports:
      - '80:5000'
    depends_on:
      postgres:
        condition: service_healthy
```

Force docker to **pull** the new version of the container image with the following command:

```bash
docker compose pull
```

And after all, **restart** the microservice in order to use the latest image:

```bash
docker compose up -d --build --no-deps web
```

All this operations needs to be performed **every time** we develop a new feature to our microservice.

To help us in these operations we can use **DevOps**'s practices. 

## DevOps

**DevOps** is a set of adaptable **practices** and **processes** that organizations use to build and deliver applications and services, aligning software development closely with IT operations.

Adopting DevOps is essential for organizations to keep up with the ever-increasing speed of development demanded by both customers and internal stakeholders. Leveraging cloud-native technologies, open-source solutions, and agile APIs, teams can now deliver and manage code more **effectively** than ever before.

![DevOps](images/devops-devops.webp)

### How does DevOps works?

In the DevOps model, **development** and **operations** teams no longer work in isolation.

Instead, they often integrate into a single unit, where engineers participate throughout the application lifecycle — from **development** and **testing** to **deployment** and **production** — and gain a versatile skill set that spans multiple functions.

Teams use **automation** to accelerate traditionally slow, manual processes. DevOps tools and technologies enable faster, more **reliable** deployment and evolution of applications.

These tools also empower teams to handle tasks that once required support from other departments, such as **code deployment** or **infrastructure provisioning**, boosting overall **efficiency**.

### Continuous Integration (CI), Delivery (CD), Deployment (CD)
![](images/continuous-delivery-and-continuous-deployment.webp)

**Continuous Integration (CI)**

Continuous Integration (CI) is a development practice where team members frequently commit code to a shared repository, often multiple times a day. Each code update triggers automated testing and builds, allowing developers to quickly **identify and resolve integration issues** or bugs. CI helps maintain a stable codebase and encourages collaboration, speeding up the overall development cycle.

**Continuous Delivery (CD)**

Continuous Delivery (CD) builds on CI by automating the testing and preparation of code changes for release. Once code passes all automated tests, it is integrated into the main branch, where it becomes **ready for deployment** at any time. This practice enables teams to release new features, fixes, or updates more frequently and reliably, as the code is always in a deployable state.

**Continuous Deployment (CD)**

Continuous Deployment is the final step in the DevOps pipeline, automating the release of every code change that passes all tests directly into the **production environment**. By eliminating the need for manual deployment steps, continuous deployment enables a rapid feedback loop with users, providing immediate updates and ensuring that customers have access to the latest features without delays.

### DevOps Pipeline

A typical **DevOps pipeline** consists of five key **stages**:

- **Lint**: Conducts **static code analysis** to identify programming errors, bugs, stylistic issues, and suspicious constructs.
- **Test**: Executes **functional tests** or **security analyses** on the code to ensure quality and safety.
- **Build**: Compiles all the application’s **artifacts**.
- **Package**: Uses the built artifacts to create a new **container image** and pushes it to the **container registry**.
- **Deploy**: Updates the currently running microservice to the **latest version**.

A DevOps pipeline can be further optimized by adding stages, such as additional security checks, to ensure application code is robust before release. In this case, **DevSecOps** practices can be integrated throughout these operations.

## DevSecOps

**DevSecOps** is a strategic approach that integrates three key disciplines: **development**, **security**, and **operations**. Its primary goal is to embed security practices into the continuous integration and continuous delivery (CI/CD) pipeline across both **pre-production** (development, testing, staging) and **production** (operations) environments.

By adopting DevSecOps, teams can **release higher-quality software more quickly** and **detect and respond to software vulnerabilities** in production with greater efficiency.

![](images/devops-devsecops.webp)

### Historically

Application security has been addressed after development is completed, and by a separate team of people, separate from both the development team and the operations team.
This approach **slowed down** the development process and the reaction time.

Also, security tools themselves have historically been isolated. Each application security test looked only at that application, and often only at the source code of that application.

This made it hard for anyone to have an organization-wide view of security issues, or to understand any of the software risks in the context of the production environment.

By making **application security** part of a unified **DevSecOps** process, from initial design to eventual implementation, organizations can align the three most important components of software creation and delivery.

### Challenges in implementing DevSecOps

The first challenge lies in addressing people and culture: it may be necessary to retrain members of your DevOps teams to understand security best practices and effectively utilize new security tools.

The second challenge involves selecting the right security tools and integrating them into your DevOps workflow. The more automated and seamlessly integrated your DevSecOps tooling is with your CI/CD pipeline, the less training and cultural adjustment will be required.

However, simply opting for a more automated version of the security tools you’ve been using for years may not be the best solution. Your development environment has likely undergone significant changes in recent years, necessitating a reevaluation of the tools and practices that best fit your current needs.

### Top traits of successful DevSecOps practices

Here are the **key performance indicators** (**KPIs**) to measure the effectiveness of your DevSecOps initiatives:

- **Security Awareness and Ownership**: Cultivate a culture where "security is everyone’s responsibility," encouraging all team members to prioritize security in their roles.
- **Automated Operations**: Implement automation to streamline security processes, reducing manual efforts and enhancing efficiency.
- **Fast Results**: Aim for quick feedback and results from security initiatives, enabling rapid response to vulnerabilities.
- **Wide Scope**: Ensure that security measures are applicable across all types of environments, from development to production.
- **Shift-Left and Shift-Right**: Monitor applications throughout the **software development lifecycle** ("**shift left**") as well as in **production environments** ("**shift right**") to maintain comprehensive security oversight.
- **Accuracy**: Achieve DevSecOps efficiency by utilizing security tests that minimize false positives and false negatives, delivering actionable insights to your remediation team.
- **Developer Acceptance**: Foster acceptance and collaboration among developers, ensuring they are engaged in the security process and understand its importance.
