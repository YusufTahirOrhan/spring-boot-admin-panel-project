# The Master Prompt: OptiMaxx Management System

## Project Overview

Develop a modern, high-performance Web Application for an optical store (optimaxx.com).  
The system consists of a public landing page and two distinct management subdomains.

---

## Core Architecture & Subdomains

### Public Site (optimaxx.com)
A clean landing page showcasing glasses, frames, lens services, and store information.

### Admin Portal (admin.optimaxx.com)
Accessible only by OWNER and ADMIN roles.

Features:
- Staff management (CRUD employees)
- Global activity tracking (who did what and when)
- Dynamic configuration of transaction types (e.g., Glass Sale, Lens Order, Frame Repair)
- Future-ready for multi-store support

### Sales Portal (sales.optimaxx.com)
Accessible by STAFF (and optionally ADMIN for monitoring).

Features:
- Recording sales
- Customer CRM
- Managing repairs
- Managing lens prescriptions
- Inventory updates
- Transaction type selection dynamically defined by Admin

---

## Technical Stack (MANDATORY)

- Backend: Spring Boot 4
- Java 25
- Utilize Virtual Threads where appropriate for performance
- Primary Database: PostgreSQL (relational business data)
- Audit Log Engine: ClickHouse (immutable append-only logs)
- Cache & Session Store: Redis
- Security: Spring Security with JWT + RBAC
- API Documentation & Testing: OpenAPI / Swagger UI enabled
- Actuator enabled (health endpoint available)

---

## Global Data Rules

### Soft Delete Policy (PostgreSQL)

No data should ever be permanently removed.

Every entity must include:
- is_deleted (boolean)
- deleted_at (timestamp)
- deleted_by (user_id reference)

Repository/query design must ensure deleted records are excluded by default.

---

### Strict Audit Logging (ClickHouse)

Every staff action must be logged to ClickHouse as an immutable INSERT-only event.

Especially:
- CREATE
- UPDATE
- DELETE
- Any critical configuration change

Minimum audit fields:

- event_id
- timestamp
- actor_user_id
- actor_role
- action
- resource_type
- resource_id
- before_json
- after_json
- request_id
- ip_address
- user_agent
- store_id (future multi-store support)

ClickHouse must never perform UPDATE or DELETE operations.

---

## Multi-Store Readiness

Prepare domain models to support future multi-store architecture.

Core entities should include:
- store_id

Even if single-store is used initially.

---

## RBAC Model

Roles:
- OWNER
- ADMIN
- STAFF

JWT must include role claims.
Admin portal endpoints must be protected accordingly.

---

## STRICT LANGUAGE CONSTRAINTS

Backend & Development must be 100% English:

- Class names
- Variable names
- Database schemas
- API endpoints
- Comments
- Git commit messages
- PR titles and descriptions

Forbidden:
- Turkish identifiers
- Turklish words (e.g., satis, gozluk, musteri)

Frontend exception:
UI labels and buttons must be Turkish (or support i18n with Turkish default).

---

# Initial Task (FOUNDATION ONLY)

This task is strictly infrastructure and foundation setup.  
Do NOT implement full business logic yet.

### Deliverables:

1. Project structure following Clean Architecture:
   - domain
   - application
   - infrastructure
   - interfaces (controllers)
   - security
   - config

2. Provide a fully configured:
   - pom.xml (Spring Boot 4 + Java 25)
   - Virtual Threads enabled
   - Swagger/OpenAPI configuration
   - Actuator health endpoint

3. docker-compose.yml including:
   - PostgreSQL
   - ClickHouse
   - Redis
   - Named volumes
   - Sensible exposed ports

4. Configuration files:
   - application-dev.yml (uses docker services)
   - application-test.yml (prepared for Testcontainers)
   - application-prod.yml (placeholders only, no secrets)

5. Initial domain/entity designs (English only):
   - BaseEntity (soft delete + audit metadata)
   - User (RBAC-ready, multi-store ready)
   - ActivityLog (if stored in Postgres)
   - ClickHouse event schema definition

6. Security foundation:
   - JWT configuration skeleton
   - Role-based access configuration
   - Basic secured sample endpoint (no full auth flow yet)

7. Provide README section:
   - How to start docker
   - How to run the application
   - Swagger UI URL
   - Health endpoint URL

---

## Git Rules

- Create feature branch:
  feat/optimaxx-foundation
- Small logical commits
- Add minimal context-load test
- Run tests
- Open PR
- Enable auto-merge (squash)
- Never push directly to main

---

## Reporting

Send milestone updates only:
- Plan
- Implementation summary
- Test result
- PR opened (with link)
- Merged

Start now.
