# 🛒 Online Marketplace (Akka + Spring Boot + Docker)

A distributed **online marketplace** where the **Marketplace service** is reimplemented using **Akka actors**, while the **Account** and **Wallet services** are reused from the **Spring Boot + Docker** setup.  

Supports user accounts, product catalog, orders, payments via wallet, and order cancellation/refunds.  

---

## ✨ Features
- User signup & account management (**Spring Boot**)  
- Product catalog & order handling (**Akka**)  
- Wallet balance management with debit/credit (**Spring Boot**)  
- Real-time concurrency handling with **Akka actors**  
- Order cancellation restores stock & refunds wallet  
- Dockerized services for portability  
- Akka Cluster scaling in Phase 2  

---

## 🛠 Tech Stack
- **Akka (Java)** – Marketplace microservice  
- **Spring Boot (Java)** – Account & Wallet microservices  
- **Database:** H2 (in-memory)  
- **Build Tool:** Maven  
- **Containerization:** Docker  
- **Orchestration (Phase 2):** Akka Cluster + Routers  

---

## 🚀 Getting Started

### 📌 Prerequisites
- Java 17+  
- Maven 3.9+  
- Docker  

---

### ▶️ Run the Services

| Service             | Port | Docker Image          | URL                       |
|--------------------|------|---------------------|---------------------------|
| Account Service     | 8080 | `account-service`    | http://localhost:8080     |
| Wallet Service      | 8082 | `wallet-service`     | http://localhost:8082     |
| Marketplace Service | 8081 | `marketplace-service`| http://localhost:8081     |

#### 1. Build Docker images
```bash
# Account Service
cd account-service
docker build -t account-service .

# Wallet Service
cd wallet-service
docker build -t wallet-service .

# Marketplace (Akka) Service
cd marketplace-akka
docker build -t marketplace-service .

# Run Account Service → http://localhost:8080
docker run -p 8080:8080 --name account account-service

# Run Wallet Service → http://localhost:8082
docker run -p 8082:8080 --name wallet wallet-service

# Run Marketplace Service (Akka) → http://localhost:8081
docker run -p 8081:8080 --rm --name marketplace \
  --add-host=host.docker.internal:host-gateway marketplace-service
