# Order Processor E-commerce Simulation using Spring Boot Microservices

## Project Demonstration

Since this is a backend project that isn't publicly hosted, a video demonstration is provided to showcase its functionality:

**[Watch Project Demo on YouTube](https://youtu.be/IiWhYAXe8Bk)**

*(The video demonstrates starting the services via Docker Compose/Kuberenetes Kind, placing an order that triggers inventory updates and a notification, and a brief look at the Eureka and Prometheus dashboards.)*

## Project Description

This project is an **e-commerce simulation backend** built with Spring Boot microservices. It demonstrates a distributed system handling core e-commerce functionalities like product catalog management, order processing, inventory updates, and real-time notifications. The architecture leverages Spring Cloud components for service discovery, API Gateway routing, and inter-service communication, along with advanced concepts like circuit breakers, distributed tracing, and centralized configuration. The entire application is designed for containerization with Docker and can be deployed on Kubernetes.

## Features

* **Product Management:** CRUD operations for product listings, interacting with MongoDB.
* **Order Creation:** Users can create new orders, checking inventory availability.
* **Inventory Management:** Updates stock levels upon order creation, interacting with PostgreSQL.
* **Service Discovery:** Services register with and discover each other via Eureka Server.
* **API Gateway:** Centralized entry point for all client requests with routing, load balancing, and security.
* **Inter-service Communication:** RESTful communication between microservices.
* **Fault Tolerance:** Implemented with Resilience4j (Circuit Breaker pattern) for order service.
* **Distributed Tracing:** Integrated with Micrometer and Zipkin for observability.
* **Centralized Configuration:** Managed via Spring Cloud Config Server for externalized configuration.
* **Real-time Notifications:** Utilizes Apache Kafka for asynchronous, real-time order notifications.
* **Containerization & Orchestration:** Packaged with Docker and orchestrated using Docker Compose, with Kubernetes manifests for deployment.
* **Authentication & Authorization:** Integrated with Keycloak for OAuth2/OIDC.
* **Observability:** Integrated with Prometheus and Grafana for monitoring application metrics.

## Technologies Used

* **Core:** Java 17, Spring Boot 3.x
* **Frameworks:** Spring Cloud (Eureka, Gateway, Config Server, Resilience4j, Kafka Stream)
* **Databases:** PostgreSQL (for Order, Inventory), MongoDB (for Product)
* **Messaging:** Apache Kafka, Apache Zookeeper
* **Build Tool:** Maven
* **Containerization:** Docker, Docker Compose, Jib
* **Orchestration:** Kubernetes
* **Observability:** Actuator, Prometheus, Grafana, Zipkin (Tracing)
* **Security:** Spring Security
* **Authentication:** Keycloak

## Architecture Overview

The project follows a microservices architecture, orchestrated by Docker Compose and deployable on Kubernetes. Key components include:

* **`discovery-server`**: The central Eureka Server for service registration and client-side load balancing.
* **`api-gateway`**: A Spring Cloud Gateway that acts as the single entry point for all external requests, handling routing to various microservices, security, and load balancing.
* **`product-service`**: Manages the product catalog, persisting data in MongoDB.
* **`order-service`**: Core service for handling order creation, interacting with Inventory Service, and publishing order events to Kafka. It includes a Circuit Breaker for resilience.
* **`inventory-service`**: Manages product stock levels, persisting data in PostgreSQL.
* **`notification-service`**: Consumes order events from Kafka to simulate sending real-time notifications.
* **`config-server`**: Provides centralized configuration management for all microservices.
* **Keycloak**: An identity and access management solution for OAuth2/OIDC.
* **Kafka**: Message broker for asynchronous communication, particularly for notifications.
* **Prometheus & Grafana**: For collecting and visualizing application and system metrics.

## How to Run Locally

This project is designed to be easily run locally using Docker Compose, which orchestrates all microservices and their dependencies.

### Prerequisites

* Java 17 Development Kit (JDK 17)
* Apache Maven (compatible with Java 17)
* Docker Desktop (or Docker Engine and Docker Compose)

### Steps (Docker Compose)

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/adeel-015/order-processor.git
    cd order-processor
    ```

2.  **Build the Microservices:**
    Navigate to the root `order-processor` directory (where the parent `pom.xml` is located) and build all services using Maven:
    ```bash
    mvn clean install
    ```
    This command will also build Docker images for each service using Jib.

3.  **Start the Services using Docker Compose:**
    From the root `order-processor` directory, execute:
    ```bash
    docker compose up -d
    ```
    This will spin up all the services including Eureka Server, API Gateway, individual microservices, databases (PostgreSQL, MongoDB), Kafka, Zookeeper, Keycloak, Prometheus, and Grafana.

4.  **Verify Services (Optional):**
    You can check the status of your running containers:
    ```bash
    docker compose ps
    ```

5.  **Access Dashboards and API Gateway:**
    Once services are up (this may take a few minutes), you can access the following:
    * **Eureka Dashboard:** `http://localhost:8761`
    * **Keycloak Admin Console:** `http://localhost:8080/admin` (default user: `admin`, pass: `admin`)
    * **Grafana Dashboard:** `http://localhost:3000` (default user: `admin`, pass: `admin`)
    * **Prometheus Dashboard:** `http://localhost:9090`
    * **API Gateway (Main Entry Point):** `http://localhost:8080`

    You can then use tools like Postman or Insomnia to interact with the API Gateway endpoints.

## API Documentation / Testing

* **Swagger UI:** Individual services within the API Gateway might expose Swagger UI for API exploration (e.g., `http://localhost:8080/swagger-ui.html` or specific service paths like `http://localhost:8080/api/products/swagger-ui.html`). You may need to verify the exact path by checking the logs or configuration after running the application.
* **Postman Collection:** (Highly Recommended!) If you have a Postman collection for your APIs, link it here: `[Download Postman Collection](link-to-your-postman-collection.json)`

## How to Deploy to Kubernetes

This project includes Kubernetes manifests to facilitate deployment to a Kubernetes cluster.

### Prerequisites (Kubernetes)

* A running Kubernetes cluster (e.g., Minikube, Kind, or a cloud-managed cluster).
* `kubectl` command-line tool configured to interact with your cluster.
* Docker images of your microservices pushed to a Docker registry accessible by your cluster (e.g., Docker Hub, Google Container Registry). Alternatively, if using Minikube/Kind, you can load images directly.

### Steps (Kubernetes)

1.  **Build Docker Images:**
    Ensure you have built the Docker images for your services using Maven (this is done by `mvn clean install` if Jib is configured):
    ```bash
    mvn clean install
    ```

2.  **Load Images into Cluster (for Minikube/Kind):**
    If you are using Minikube or Kind and have not pushed your images to a registry, you need to load them into the cluster's Docker daemon:
    ```bash
    # For Minikube:
    eval $(minikube docker-env)
    # Then for each image (e.g., product-service, order-service, etc.):
    docker build -t your-image-name:latest . # Build if not already done by Jib
    minikube image load your-image-name:latest

    # For Kind:
    kind load docker-image your-image-name:latest --name your-kind-cluster-name
    ```
    *(Note: If you push to a public/private registry, skip this step and ensure image names in `k8s/` manifests point to your registry path, e.g., `your-registry/your-image-name:latest`)*

3.  **Deploy Core Infrastructure (if not using Docker Compose for dependencies):**
    If your Kubernetes manifests include deployments for Kafka, MongoDB, PostgreSQL, Keycloak, Prometheus, Grafana etc., apply them first:
    ```bash
    kubectl apply -f k8s/infrastructure/
    ```
    *(Adjust path as per your `k8s` subdirectories)*

4.  **Deploy Microservices:**
    Apply the Kubernetes manifests for your microservices:
    ```bash
    kubectl apply -f k8s/services/
    ```
    *(Adjust path as per your `k8s` subdirectories)*

5.  **Verify Deployment:**
    Check the status of your pods and services:
    ```bash
    kubectl get pods
    kubectl get services
    ```

6.  **Access Services in Kubernetes:**
    You might need to use `kubectl port-forward` or configure Ingress to expose your API Gateway and other services externally.
    * Example for API Gateway (if service name is `api-gateway-service` and port is `8080`):
        ```bash
        kubectl port-forward service/api-gateway-service 8080:8080
        ```
        Then access via `http://localhost:8080`.

## Author

* **Adeel Javed**
    * [GitHub Profile](https://github.com/adeel-015)
    * [LinkedIn Profile](https://www.linkedin.com/in/adeel-javed-lnkdn)
