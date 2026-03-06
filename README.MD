# 🚀 Esign API

Digital Signature Service API for document signing

---

# 🧩 Architecture Overview

This project supports two runtime modes:

| Mode        | Spring Boot Runtime | Database                 | Configuration Source          |
|-------------|--------------------|---------------------------|------------------------------|
| Development | Run from IDE       | PostgreSQL (Docker)      | IDE Environment Variables     |


---

# 🛠 DEVELOPMENT MODE (Hybrid Setup)

Development Mode uses:

- 🐳 Docker → PostgreSQL
- 💻 IDE → Spring Boot Application

---

## 📦 1. Prerequisites

Ensure the following tools are installed on your machine:

- **JDK 21** (Microsoft OpenJDK ms-21.0.10)
- **Docker Desktop**
- **DBeaver** (Optional, for database management)

---

## 🧬 2. Clone Repository

```bash
git clone https://github.com/haetamm/esign-api.git
cd esign-api
```

---

## 🔐 3. Configure Required Environment Variables

### 3.1 Spring Boot (IDE)

Configure in:

IntelliJ → Run → Edit Configurations → Environment Variables

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/esign-db
SPRING_DATASOURCE_USERNAME=developer
SPRING_DATASOURCE_PASSWORD=rahasia
```

---

## 🐳 4. Start Infrastructure (Docker)

```bash
docker compose -f docker-compose.dev.yml up -d
```

Check:

```bash
docker ps
```

Expected:

- esign-postgres-dev

---

## ▶ 5. Run Spring Boot Application

Run:

```
EsignApplication.java
```

App runs at:

```
http://localhost:3000
```
---

## 📚 6. API Documentation (Swagger)

Open the following URL in your browser to access the API documentation:

http://localhost:3000/swagger-ui/index.html#/

---

## 🗄 7. Configure Database via DBeaver

Open DBeaver → Create New PostgreSQL Connection

Use the following configuration:

- Host: `localhost`
- Port: `5432`
- Database: `esign-db`
- Username: `developer`
- Password: `rahasia`

Click **Test Connection** → then Finish.

Hibernate will automatically create tables on first application startup.

---

## 🔁 Development Workflow

1. Start Docker infrastructure
2. Run Spring Boot from IDE

---

## 🛑 Stop Development

```bash
docker compose -f docker-compose.dev.yml down
```