# WebChat

A real-time chat application with group and direct messaging, built on Spring Boot WebFlux + Reactor Kafka on the backend and React + Vite on the frontend.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Server runtime | Spring Boot 4.x (Netty / WebFlux) |
| Messaging | Apache Kafka + Reactor Kafka |
| Persistence | MySQL + Spring Data JPA (Hibernate) |
| Authentication | JWT (jjwt 0.12) |
| Client | React 18 + Vite |
| WebSocket | Native browser WebSocket API |

---

## Project Structure

```
webchat1/
├── webchat-server/   # Spring Boot backend
└── webchat-client/   # React frontend
```

---

## Prerequisites

- Java 21
- Maven (wrapper included — `mvnw`)
- Node.js 18+ and npm
- MySQL 8
- Apache Kafka (with Zookeeper or KRaft mode)

---

## Server Setup (`webchat-server`)

### 1. MySQL

Create a database (the app will create tables automatically via Hibernate):

```sql
CREATE DATABASE webchat;
```

Or simply let the datasource URL do it — `createDatabaseIfNotExist=true` is already set.

### 2. Kafka

Start a local Kafka broker on the default port `9092`.
The topic `webchat-messages` is created automatically when the first message is published.

### 3. Configuration — `application.yml`

```yaml
server:
  port: 9876

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/webchat?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
    username: root
    password: root

  kafka:
    bootstrap-servers: localhost:9092

jwt:
  secret: <your-secret-key-at-least-32-chars>
  expiration: 86400000   # 24 hours

cors:
  allowed-origins: http://localhost:3000
```

> **JWT secret** — replace the default value with a long random string before deploying.

> **CORS** — add your production frontend URL to `cors.allowed-origins`.

### 4. Running the server

```bash
cd webchat-server
./mvnw spring-boot:run
```

The server starts on **http://localhost:9876**.
WebSocket endpoint: `ws://localhost:9876/ws/chat`

---

## Client Setup (`webchat-client`)

### 1. Install dependencies

```bash
cd webchat-client
npm install
```

### 2. Configuration — `vite.config.js`

The dev server proxies both API and WebSocket calls to the backend automatically. No extra configuration needed during development.

```js
proxy: {
  '/api': { target: 'http://localhost:9876', changeOrigin: true },
  '/ws':  { target: 'ws://localhost:9876',  ws: true, changeOrigin: true },
}
```

If the backend runs on a different host/port, update the `target` values accordingly.

### 3. Running the client

```bash
npm run dev
```

The app opens on **http://localhost:3000**.

---

## REST API

All endpoints are prefixed with `/api`.

### Auth — `/api/auth`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/signup` | No | Register a new user |
| `POST` | `/login` | No | Login and receive a JWT |
| `POST` | `/logout` | Yes | Revoke all tokens for the user |
| `POST` | `/forgot-password/request` | No | Request a password-reset OTP |
| `POST` | `/forgot-password/reset` | No | Reset password using OTP |

### Contacts — `/api/contacts` *(JWT required)*

| Method | Path | Description |
|--------|------|-------------|
| `GET`  | `/` | List all contacts |
| `POST` | `/add?phone=<number>` | Add a contact by phone number |

### Messages — `/api/messages` *(JWT required)*

| Method | Path | Description |
|--------|------|-------------|
| `GET`  | `/group/{groupId}` | Fetch message history for a group |
| `GET`  | `/dm/{phone}` | Fetch DM history with a user |

---

## WebSocket Protocol

Connect to `ws://<host>/ws/chat?token=<JWT>`.

After connecting, send a `REGISTER` message. All subsequent messages are JSON objects with a `type` field.

### Client → Server message types

| Type | Required fields | Description |
|------|----------------|-------------|
| `REGISTER` | — | Register the session with the server |
| `CREATE_GROUP` | `content` (name) | Create a new group |
| `JOIN` | `groupId` | Join a group room |
| `LEAVE` | `groupId` | Leave a group room |
| `TEXT` | `groupId`, `content` | Send a group message |
| `DIRECT_MESSAGE` | `recipientId`, `content` | Send a direct message |
| `LIST_GROUPS` | — | List groups the user belongs to |
| `LIST_USERS` | `groupId` | List members of a group |
| `LIST_ONLINE_USERS` | — | List currently online users |
| `ADD_GROUP_MEMBER` | `groupId`, `content` (phone) | Add a member (admin/creator only) |
| `REMOVE_GROUP_MEMBER` | `groupId`, `content` (phone) | Remove a member |
| `PROMOTE_MEMBER` | `groupId`, `content` (phone) | Promote to admin (creator only) |
| `DEMOTE_MEMBER` | `groupId`, `content` (phone) | Demote admin (creator only) |
| `DELETE_GROUP` | `groupId` | Delete a group (creator only) |
| `DELETE_DM` | `recipientId` | Delete a DM thread |

### Server → Client message types

| Type | Description |
|------|-------------|
| `TEXT` | Incoming group message |
| `DIRECT_MESSAGE` | Incoming direct message |
| `JOIN` / `LEAVE` | Member joined or left a group |
| `GROUP_INVITATION` | You were added to a group |
| `GROUP_KICK` | You were removed from a group |
| `DELETE_GROUP` | A group was deleted |
| `LIST_GROUPS` | Response to LIST_GROUPS request |
| `LIST_USERS` | Response to LIST_USERS request |
| `LIST_ONLINE_USERS` | Online user list update |

---

## Group Roles

| Role | Permissions |
|------|------------|
| **Member** | Send messages, leave group |
| **Admin** | Add/remove regular members |
| **Creator** | All admin rights + promote/demote admins, delete group |

When a creator leaves, ownership is transferred to an existing admin, or the oldest remaining member if no admins exist. If the group is empty after the creator leaves, the group is deleted.

---

## Environment Variables

The server reads these from the environment (with defaults):

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |

---

## Notes

- The forgot-password flow uses a **mock OTP** (`123456`). Integrate a real SMS/email provider before going to production.
- JWT tokens are stored in the database and revoked on logout. A cleanup job removes expired tokens every hour.
- Messages are delivered directly to connected WebSocket sessions for zero-latency delivery. Kafka is used for cross-server fan-out and message durability.