Secure Enterprise Contact System (SECS) — Microservices EditionA complete, distributed system for secure contact and user management, built with Java 23 and Spring Boot 3. The project implements a microservices architecture with a centralized API Gateway, asynchronous communication, and modern DevOps practices.🏗 System ArchitectureThe system consists of specialized microservices coordinated by a Gateway, supported by a robust infrastructure for messaging, data storage, and observability.graph TD
    Client[Desktop Client GUI] -->|REST HTTP| Gateway[API Gateway :8000]
    Client -->|TCP Socket| ContactService
    Client -->|UDP Datagram| ContactService
    Client -->|FTP| FTPServer[FTP Server]
    Client -->|POP3| MailServer[GreenMail SMTP/POP3]
    
    subgraph Microservices
        Gateway -->|Route| AuthService[Auth Service :8082]
        Gateway -->|Route| ContactService[Contact Service :8080]
        Gateway -->|Route| AuditService[Audit Service :8083]
        Gateway -->|Route| NotifService[Notification Service :8084]
    end

    subgraph Storage
        AuthService -->|JDBC| AuthDB[(PostgreSQL :5433)]
        ContactService -->|JDBC| ContactDB[(PostgreSQL :5432)]
        AuditService -->|NoSQL| AuditDB[(MongoDB :27017)]
    end
    
    subgraph Messaging
        AuthService -.->|Event| RabbitMQ
        ContactService -.->|Event| RabbitMQ
        RabbitMQ -.->|Consume| NotifService
        RabbitMQ -.->|Consume| AuditService
    end

    subgraph Monitoring
        Services --> Promtail/Telegraf/cAdvisor
        Promtail/Telegraf/cAdvisor --> Loki/Prometheus
        Loki/Prometheus --> Grafana
    end
🚀 Key Features & Protocols1. Networking Protocols (5 Application Layers)HTTP (REST API): Handles CRUD operations, authentication, and integration with external APIs (Agify.io).TCP: Custom socket server (port 8888) in contact-service for real-time server status monitoring in the GUI footer.UDP: Datagram listener (port 9999) for high-speed, connectionless transmission of audit logs.SMTP/POP3: Asynchronous email dispatch (via RabbitMQ) and direct email retrieval by the client from the GreenMail server.FTP: Integration with the vsftpd server – file transfers, document sharing, and database backups.2. Security (RBAC & Encryption)AES Encryption: Sensitive data (contact addresses) is encrypted/decrypted on the fly before database persistence.RBAC: Defined roles for ADMIN (user management, backups) and USER (limited access).Hashing: Passwords stored securely using BCrypt hashes.3. Advanced Integration & DevOpsAPI Gateway: Central entry point (Spring Cloud Gateway) managing traffic routing.Message Broker: Distributed inter-service communication using RabbitMQ.CI/CD: GitHub Actions pipeline automatically building Docker images and pushing them to the GitHub Container Registry (GHCR).360° Monitoring: Full stack (Grafana, Loki, Prometheus, Promtail, Telegraf, cAdvisor) for application logs and container metrics.🛠 Tech StackComponentTechnologyLanguageJava 23 (JDK 23)FrameworkSpring Boot 3.4 (Cloud Gateway, Data JPA, Security, AMQP)DatabasesPostgreSQL 15 (Auth & Contacts), MongoDB (Audit)MessagingRabbitMQServersvsftpd (FTP), GreenMail (Mail)MonitoringGrafana, Loki, Prometheus, Promtail, Telegraf, cAdvisor⚙️ Installation & SetupPrerequisitesDocker DesktopJava JDK 23Maven1. Start InfrastructureDownload and start all containers (Databases, Queues, Monitoring, Servers):docker-compose up -d
2. Build and CDThe project is configured for GitHub Actions. To build images locally:mvn clean install -DskipTests
docker-compose build
3. Run the ClientRun the ClientApplication.java class located in the desktop-client module.📊 Monitoring and LogsGrafana: http://localhost:3000 (admin/admin) – visualization of logs (Loki) and metrics.RabbitMQ: http://localhost:15672 (guest/guest) – queue management.Prometheus: http://localhost:9090 – insight into system metrics.cAdvisor: http://localhost:8080 – real-time container resource usage statistics.Project developed as the final version of the Secure Contact System.
