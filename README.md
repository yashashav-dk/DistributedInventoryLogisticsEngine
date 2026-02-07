# Distributed Inventory Logistics Engine

A full-stack inventory management system built to demonstrate production-grade concurrency handling and database optimization techniques.

## Tech Stack

| Layer     | Technology                          |
|-----------|-------------------------------------|
| Backend   | Java 17, Spring Boot 3.2, JPA       |
| Frontend  | Vue.js 3, Vite                      |
| Database  | PostgreSQL 15                        |
| Infra     | Docker Compose                       |

---

## System Architecture

```mermaid
graph TB
    subgraph Client["Frontend — Vue.js 3 + Vite"]
        UI["Dashboard UI<br/><i>:5173</i>"]
        SIM["Load Simulator"]
    end

    subgraph Server["Backend — Spring Boot 3.2"]
        direction TB
        CTRL["InventoryController<br/><i>REST API :8080</i>"]
        SVC["InventoryService"]
        EXC["GlobalExceptionHandler"]

        subgraph JPA["JPA / Hibernate"]
            ENT_ITEM["InventoryItem<br/><b>@Version</b>"]
            ENT_LOG["InventoryLog<br/><b>@Index</b>"]
        end

        REPO_ITEM["InventoryRepository"]
        REPO_LOG["InventoryLogRepository"]
    end

    subgraph DB["PostgreSQL 15 — Docker"]
        T_ITEMS[("inventory_items<br/>Optimistic Lock<br/><i>version column</i>")]
        T_LOGS[("inventory_logs<br/>Composite Index<br/><i>(warehouse_id, timestamp)</i>")]
    end

    UI -- "HTTP GET/PUT" --> CTRL
    SIM -- "POST /simulate-load<br/>N concurrent requests" --> CTRL
    CTRL --> SVC
    SVC --> EXC
    SVC --> REPO_ITEM
    SVC --> REPO_LOG
    REPO_ITEM --> ENT_ITEM
    REPO_LOG --> ENT_LOG
    ENT_ITEM -- "JDBC" --> T_ITEMS
    ENT_LOG -- "JDBC" --> T_LOGS

    style Client fill:#1a1e2e,stroke:#58a6ff,color:#e1e4e8
    style Server fill:#1a2e1a,stroke:#3fb950,color:#e1e4e8
    style DB fill:#2e1a1a,stroke:#f85149,color:#e1e4e8
    style JPA fill:#0d1117,stroke:#30363d,color:#8b949e
```

---

## Optimistic Locking — Sequence Diagram

Shows what happens when two concurrent transactions attempt to update the same SKU:

```mermaid
sequenceDiagram
    participant C1 as Transaction A
    participant C2 as Transaction B
    participant SVC as InventoryService
    participant DB as PostgreSQL

    Note over C1,C2: Both read the same SKU simultaneously

    C1->>SVC: updateStock(SKU-001, -1)
    C2->>SVC: updateStock(SKU-001, -1)

    SVC->>DB: SELECT * FROM inventory_items<br/>WHERE sku = 'SKU-001'
    DB-->>SVC: {qty: 500, version: 0}

    SVC->>DB: SELECT * FROM inventory_items<br/>WHERE sku = 'SKU-001'
    DB-->>SVC: {qty: 500, version: 0}

    Note over C1,DB: Transaction A commits first

    SVC->>DB: UPDATE SET qty=499, version=1<br/>WHERE id=1 AND version=0
    DB-->>SVC: 1 row updated
    SVC-->>C1: 200 OK {qty: 499, version: 1}

    Note over C2,DB: Transaction B has stale version

    SVC->>DB: UPDATE SET qty=499, version=1<br/>WHERE id=1 AND version=0
    DB-->>SVC: 0 rows updated

    Note over SVC: ObjectOptimisticLockingFailureException

    SVC-->>C2: 409 Conflict<br/>"Concurrent modification detected"
```

---

## Database Schema — ER Diagram

```mermaid
erDiagram
    INVENTORY_ITEMS {
        bigserial id PK
        varchar_50 sku UK "UNIQUE — lookup key"
        varchar_255 product_name
        integer quantity "CHECK >= 0"
        varchar_20 warehouse_id
        bigint version "Optimistic Lock"
        timestamptz created_at
        timestamptz updated_at
    }

    INVENTORY_LOGS {
        bigserial id PK
        varchar_20 warehouse_id "Composite Index (1)"
        timestamptz timestamp "Composite Index (2)"
        varchar_50 sku
        varchar_20 action "RESTOCK | DEDUCT"
        integer quantity_change
        integer resulting_quantity
        text details
    }

    INVENTORY_ITEMS ||--o{ INVENTORY_LOGS : "generates"
```

---

## Request Flow

How a stock update travels through the application layers:

```mermaid
flowchart LR
    A["Vue Dashboard"] -->|"PUT /api/inventory/update-stock<br/>{sku, quantityChange}"| B["InventoryController"]
    B -->|"@Valid"| C["InventoryService<br/>.updateStock()"]
    C --> D{"Find SKU"}
    D -->|"Not found"| E["400 Bad Request"]
    D -->|"Found"| F["Compute new qty"]
    F --> G{"qty < 0?"}
    G -->|"Yes"| H["400 Insufficient Stock"]
    G -->|"No"| I["saveAndFlush()"]
    I --> J{"Version<br/>match?"}
    J -->|"Yes"| K["Write InventoryLog"]
    K --> L["200 OK<br/>{newQty, version}"]
    J -->|"No"| M["ObjectOptimistic<br/>LockingFailureException"]
    M --> N["409 Conflict"]

    style A fill:#1a1e2e,stroke:#58a6ff,color:#e1e4e8
    style L fill:#1a2e1a,stroke:#3fb950,color:#e1e4e8
    style N fill:#2e1a1a,stroke:#f85149,color:#e1e4e8
    style E fill:#2e1a1a,stroke:#f85149,color:#e1e4e8
    style H fill:#2e1a1a,stroke:#f85149,color:#e1e4e8
```

---

## Composite Index — Query Performance

Illustrates how the B-tree composite index avoids full table scans:

```mermaid
flowchart TD
    Q["SELECT * FROM inventory_logs<br/>WHERE warehouse_id = 'WH-EAST'<br/>AND timestamp BETWEEN '2025-01-01' AND '2025-06-01'"]

    Q --> P{"Query Planner"}

    P -->|"Without Index"| FULL["Sequential Scan<br/>Reads ALL rows<br/>O(n)"]
    P -->|"With Composite Index"| IDX["Index Scan on<br/>idx_warehouse_timestamp"]

    IDX --> B1["B-tree narrows to<br/>warehouse_id = 'WH-EAST'"]
    B1 --> B2["Range scan within<br/>timestamp bounds"]
    B2 --> RES["Returns only<br/>matching rows<br/>O(log n + k)"]

    style FULL fill:#2e1a1a,stroke:#f85149,color:#e1e4e8
    style RES fill:#1a2e1a,stroke:#3fb950,color:#e1e4e8
    style IDX fill:#1a1e2e,stroke:#58a6ff,color:#e1e4e8
```

---

## Key Technical Features

### 1. Concurrency Handling — Optimistic Locking

The `InventoryItem` entity uses JPA's `@Version` annotation to implement optimistic locking. When two concurrent transactions attempt to update the same SKU:

1. Both transactions read the entity (including its current `version`).
2. The first transaction to commit increments the version and succeeds.
3. The second transaction detects a version mismatch and throws `ObjectOptimisticLockingFailureException`.
4. The service layer catches this and returns an HTTP `409 Conflict` response.

**Why this matters:** In a distributed warehouse environment, multiple systems may try to adjust stock simultaneously. Optimistic locking prevents silent data corruption (e.g., overselling) without the throughput penalty of pessimistic database locks.

**Try it yourself:** Click **Simulate High Load** in the dashboard to fire 20 concurrent deduction requests against a single SKU. The results panel shows how many succeeded vs. how many were safely rejected.

```
Entity: backend/src/main/java/com/inventory/engine/entity/InventoryItem.java
Service: backend/src/main/java/com/inventory/engine/service/InventoryService.java
```

### 2. High-Performance Querying — Composite Index

The `inventory_logs` table includes a composite B-tree index on `(warehouse_id, timestamp)`:

```sql
CREATE INDEX idx_inventory_logs_warehouse_timestamp
    ON inventory_logs (warehouse_id, timestamp);
```

This accelerates the most common analytical query pattern — fetching all activity for a specific warehouse within a time range. PostgreSQL's query planner uses this index for efficient range scans instead of a full table scan.

**Endpoint:** `GET /api/inventory/logs/warehouse/{warehouseId}?start=...&end=...`

```
Schema: db/init.sql
Entity: backend/src/main/java/com/inventory/engine/entity/InventoryLog.java
```

---

## Project Structure

```
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/inventory/engine/
│       ├── InventoryEngineApplication.java
│       ├── config/
│       │   ├── DataSeeder.java
│       │   └── WebConfig.java
│       ├── controller/
│       │   └── InventoryController.java
│       ├── dto/
│       │   ├── ConcurrencyTestResult.java
│       │   ├── StockUpdateRequest.java
│       │   └── StockUpdateResponse.java
│       ├── entity/
│       │   ├── InventoryItem.java      ← @Version optimistic locking
│       │   └── InventoryLog.java       ← Composite index definition
│       ├── exception/
│       │   ├── GlobalExceptionHandler.java
│       │   └── StockConflictException.java
│       ├── repository/
│       │   ├── InventoryLogRepository.java
│       │   └── InventoryRepository.java
│       └── service/
│           └── InventoryService.java   ← Concurrency test logic
├── frontend/
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js
│   └── src/
│       ├── App.vue
│       ├── main.js
│       └── components/
│           └── Dashboard.vue           ← Stock table + load simulator
├── db/
│   └── init.sql                        ← Schema + composite index + seed data
├── docker-compose.yml
└── README.md
```

---

## Getting Started

### Prerequisites

- Java 17+
- Node.js 18+
- Docker & Docker Compose (for PostgreSQL)

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts a PostgreSQL instance and runs `db/init.sql` to create tables, indexes, and seed data.

### 2. Start the Backend

```bash
cd backend
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### 3. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The dashboard will be available at `http://localhost:5173`.

---

## API Endpoints

| Method | Endpoint                                  | Description                              |
|--------|-------------------------------------------|------------------------------------------|
| GET    | `/api/inventory`                          | List all inventory items                 |
| GET    | `/api/inventory/warehouse/{id}`           | Items by warehouse                       |
| PUT    | `/api/inventory/update-stock`             | Update stock (optimistic locking)        |
| POST   | `/api/inventory/simulate-load`            | Fire concurrent requests for testing     |
| GET    | `/api/inventory/logs`                     | Recent activity logs                     |
| GET    | `/api/inventory/logs/warehouse/{id}`      | Logs by warehouse + time range (indexed) |
| POST   | `/api/inventory/seed`                     | Load seed data                           |

---

## Architecture Decisions

- **Optimistic over pessimistic locking:** Optimistic locking allows high read throughput since no rows are locked during reads. Conflicts are rare in practice and are handled gracefully at the application layer.
- **Composite index column order:** `(warehouse_id, timestamp)` is ordered to match the most selective filter first (warehouse narrows the result set), followed by the range predicate (timestamp).
- **JPA `ddl-auto=update`:** Used for development convenience. In production, schema migrations should use Flyway or Liquibase with the provided `init.sql` as the baseline.
