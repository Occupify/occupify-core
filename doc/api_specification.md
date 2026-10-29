# Occupify Core - API Specification

> [!NOTE]
> **Base Port & Gateway Routing:**
> - Direct Service Port: `8182` (Internal)
> - Gateway Port: `8180` (External ingress entry point)
> - Gateway Routes: `/projects/**`, `/applications/**`, `/reviews/**`, `/bids/**`
> - Database Port: PostgreSQL `5531` (`occupify_core`), RabbitMQ `5672`

---

## Standard Response Envelopes

Every API endpoint returns a standardized response envelope:

### Single Data Response (`SingleResponse<T>`)
```json
{
  "statusCode": 200,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

### Resource Created Response (`CreatedResponse<T>`)
```json
{
  "statusCode": 201,
  "message": "Resource created successfully",
  "data": { ... }
}
```

### Paginated Data Response (`PageResponse<T>`)
```json
{
  "statusCode": 200,
  "message": "Operation completed successfully",
  "data": [ ... ],
  "paging": {
    "page": 0,
    "limit": 20,
    "total": 128,
    "totalPages": 7
  }
}
```

### Generic Action Success Response (`SuccessResponse`)
```json
{
  "statusCode": 200,
  "message": "Action completed successfully"
}
```

### Error Response (`ErrorResponse`)
```json
{
  "statusCode": 404,
  "errorCode": "CORE_001",
  "message": "Project not found",
  "timestamp": "2026-10-18T22:30:00Z"
}
```

---

## 1. Project Management (`/projects`)

### 1.1 Create Project
- **Endpoint:** `POST /projects`
- **Description:** Creates a new project owned by the authenticated employer. Emits `project.created` event to RabbitMQ.
- **Authorization:** Authenticated (`X-User-Role: USER` / Employer)
- **Headers:** `Content-Type: application/json`, `Authorization: Bearer <token>` or `X-User-Id`

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `title` | String | Yes | Max 255 chars, not blank | Project title |
| `description` | String | No | Max 5000 chars | Detailed project specification |
| `startPrice` | BigDecimal | No | Must be $> 0$ | Starting budget / price |
| `period` | String (Enum) | Yes | `FIXED`, `HOURLY`, `MONTHLY` | Payment / delivery timeline |
| `dueDate` | String (Date) | No | Present or future (`YYYY-MM-DD`) | Completion deadline |
| `experienceLevel`| String (Enum) | No | `ENTRY`, `INTERMEDIATE`, `EXPERT` | Desired freelancer experience |
| `skillIds` | Array[UUID] | No | Valid skill UUIDs | Required technical skills |

#### Request Body Example
```json
{
  "title": "Build Distributed Notification Microservice with Spring Boot 3",
  "description": "Looking for an experienced backend engineer to architect and implement an event-driven notification service with RabbitMQ, SMTP retry, and SSE stream.",
  "startPrice": 1500.00,
  "period": "FIXED",
  "dueDate": "2026-12-15",
  "experienceLevel": "EXPERT",
  "skillIds": [
    "44444444-5555-6666-7777-888888888888",
    "55555555-6666-7778-8888-999999999999"
  ]
}
```

#### Response Example (`201 CREATED`)
```json
{
  "statusCode": 201,
  "message": "Project created successfully",
  "data": {
    "id": "77777777-8888-9999-aaaa-bbbbbbbbbbbb",
    "title": "Build Distributed Notification Microservice with Spring Boot 3",
    "description": "Looking for an experienced backend engineer to architect and implement an event-driven notification service with RabbitMQ, SMTP retry, and SSE stream.",
    "startPrice": 1500.00,
    "period": "FIXED",
    "dueDate": "2026-12-15",
    "experienceLevel": "EXPERT",
    "status": "OPEN",
    "closedAt": null,
    "employer": {
      "id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
      "email": "employer@techcorp.com",
      "fullName": "TechCorp Global",
      "avatarUrl": "https://storage.occupify.work/avatars/techcorp.png"
    },
    "createdAt": "2026-10-18T22:30:00Z",
    "updatedAt": "2026-10-18T22:30:00Z"
  }
}
```

---

### 1.2 Get Paginated Projects
- **Endpoint:** `GET /projects`
- **Description:** Retrieves a paginated and filtered list of open projects for public discovery.
- **Authorization:** Public

#### Query Parameters
| Parameter | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `keyword` | String | No | - | Substring search on title and description |
| `status` | String (Enum) | No | `OPEN` | `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `employerId` | UUID | No | - | Filter by employer UUID |
| `period` | String (Enum) | No | - | `FIXED`, `HOURLY`, `MONTHLY` |
| `experienceLevel`| String (Enum) | No | - | `ENTRY`, `INTERMEDIATE`, `EXPERT` |
| `minPrice` | BigDecimal | No | - | Minimum budget filter |
| `maxPrice` | BigDecimal | No | - | Maximum budget filter |
| `page` | Integer | No | `0` | Zero-based page index |
| `limit` | Integer | No | `20` | Page size |
| `sort` | String | No | `createdAt,desc` | Sort field and direction (`field,asc\|desc`) |

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Projects retrieved successfully",
  "data": [
    {
      "id": "77777777-8888-9999-aaaa-bbbbbbbbbbbb",
      "title": "Build Distributed Notification Microservice with Spring Boot 3",
      "description": "Looking for an experienced backend engineer...",
      "startPrice": 1500.00,
      "period": "FIXED",
      "dueDate": "2026-12-15",
      "experienceLevel": "EXPERT",
      "status": "OPEN",
      "employer": {
        "id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
        "email": "employer@techcorp.com",
        "fullName": "TechCorp Global"
      },
      "createdAt": "2026-10-18T22:30:00Z"
    }
  ],
  "paging": {
    "page": 0,
    "limit": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

---

### 1.3 Get Project Details
- **Endpoint:** `GET /projects/{id}`
- **Description:** Retrieves full project details, including required jobs, skills, and owner profile.
- **Authorization:** Public
- **Path Parameter:** `id` (UUID of project)

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Project detail retrieved successfully",
  "data": {
    "id": "77777777-8888-9999-aaaa-bbbbbbbbbbbb",
    "title": "Build Distributed Notification Microservice with Spring Boot 3",
    "description": "Looking for an experienced backend engineer...",
    "startPrice": 1500.00,
    "period": "FIXED",
    "dueDate": "2026-12-15",
    "experienceLevel": "EXPERT",
    "status": "OPEN",
    "closedAt": null,
    "employer": {
      "id": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
      "email": "employer@techcorp.com",
      "fullName": "TechCorp Global",
      "avatarUrl": "https://storage.occupify.work/avatars/techcorp.png"
    },
    "jobs": [
      {
        "id": "88888888-9999-aaaa-bbbb-cccccccccccc",
        "title": "Spring Boot AMQP Specialist",
        "description": "Implement RabbitMQ consumers with DLQ and Retry.",
        "budget": 1500.00,
        "headcount": 1,
        "skills": [
          { "id": "44444444-5555-6666-7777-888888888888", "name": "Java" },
          { "id": "55555555-6666-7777-8888-999999999999", "name": "Spring Boot" }
        ]
      }
    ],
    "createdAt": "2026-10-18T22:30:00Z",
    "updatedAt": "2026-10-18T22:30:00Z"
  }
}
```

---

### 1.4 Update Project
- **Endpoint:** `PUT /projects/{id}`
- **Description:** Updates project specification. Permitted exclusively by the project owner.
- **Authorization:** Authenticated (Project Owner)
- **Path Parameter:** `id` (UUID of project)

#### Request Body Example
```json
{
  "title": "Build Distributed Notification Microservice (Spring Boot 3 + RabbitMQ)",
  "description": "Updated project description with expanded scope for SSE stream.",
  "startPrice": 1800.00,
  "period": "FIXED",
  "dueDate": "2026-12-30",
  "experienceLevel": "EXPERT"
}
```

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Project updated successfully",
  "data": {
    "id": "77777777-8888-9999-aaaa-bbbbbbbbbbbb",
    "title": "Build Distributed Notification Microservice (Spring Boot 3 + RabbitMQ)",
    "startPrice": 1800.00,
    "status": "OPEN",
    "updatedAt": "2026-10-18T23:00:00Z"
  }
}
```

---

### 1.5 Close Project
- **Endpoint:** `PATCH /projects/{id}/close`
- **Description:** Sets project status to `COMPLETED`, records `closedAt`, and emits `project.closed` event.
- **Authorization:** Authenticated (Project Owner or Admin)
- **Path Parameter:** `id` (UUID of project)

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Project closed successfully",
  "data": {
    "id": "77777777-8888-9999-aaaa-bbbbbbbbbbbb",
    "status": "COMPLETED",
    "closedAt": "2026-10-18T23:15:00Z"
  }
}
```

---

## 2. Applications & Bids (`/applications`, `/bids`)

### 2.1 Submit Project Bid / Proposal
- **Endpoint:** `POST /applications` or `POST /bids`
- **Description:** Submits a freelancer proposal for an open project job.
- **Authorization:** Authenticated (Freelancer)

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `jobId` | UUID | Yes | Must belong to an OPEN project | Targeted job listing |
| `bidAmount` | BigDecimal | Yes | Must be $> 0$ | Proposed cost / rate |
| `coverLetter`| String | Yes | Max 2000 chars, not blank | Pitch to hiring employer |
| `estimatedDays` | Integer | Yes | Must be $> 0$ | Estimated delivery time in days |

#### Request Body Example
```json
{
  "jobId": "88888888-9999-aaaa-bbbb-cccccccccccc",
  "bidAmount": 1400.00,
  "coverLetter": "Hello! I have over 5 years experience with Spring Boot, AMQP, and Resilience4j. I can deliver this service with 100% test coverage in 10 days.",
  "estimatedDays": 10
}
```

#### Response Example (`201 CREATED`)
```json
{
  "statusCode": 201,
  "message": "Proposal submitted successfully",
  "data": {
    "id": "99999999-aaaa-bbbb-cccc-dddddddddddd",
    "jobId": "88888888-9999-aaaa-bbbb-cccccccccccc",
    "freelancerId": "c1a2b3c4-d5e6-7890-abcd-ef1234567890",
    "bidAmount": 1400.00,
    "status": "SUBMITTED",
    "estimatedDays": 10,
    "createdAt": "2026-10-18T23:20:00Z"
  }
}
```

---

### 2.2 Accept Proposal & Initiate Contract
- **Endpoint:** `PATCH /applications/{id}/accept`
- **Description:** Employer accepts a freelancer proposal, advancing status to `ACCEPTED` and triggering contract creation in `occupify-payment`.
- **Authorization:** Authenticated (Project Owner)
- **Path Parameter:** `id` (UUID of application/bid)

#### Response Example (`200 OK`)
```json
{
  "statusCode": 200,
  "message": "Proposal accepted successfully. Contract generation initiated."
}
```

---

## 3. Reviews & Ratings (`/reviews`)

### 3.1 Submit Review
- **Endpoint:** `POST /reviews`
- **Description:** Submits feedback and 1-to-5 star rating upon contract completion.
- **Authorization:** Authenticated (Contract Participant)

#### Request Body
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `contractId` | UUID | Yes | Completed contract UUID | Associated contract |
| `rating` | Integer | Yes | Between 1 and 5 | Rating score |
| `comment` | String | No | Max 1000 chars | Review comment |

#### Request Body Example
```json
{
  "contractId": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
  "rating": 5,
  "comment": "Outstanding engineer! Delivered high-quality code with excellent communication."
}
```

#### Response Example (`201 CREATED`)
```json
{
  "statusCode": 201,
  "message": "Review submitted successfully",
  "data": {
    "id": "bbbbbbbb-cccc-dddd-eeee-ffffffffffff",
    "rating": 5,
    "comment": "Outstanding engineer! Delivered high-quality code with excellent communication.",
    "createdAt": "2026-10-18T23:25:00Z"
  }
}
```

---

## 4. Asynchronous Event Contracts (RabbitMQ)

### Published Events
| Event | Exchange | Routing Key | Description |
| :--- | :--- | :--- | :--- |
| `ProjectCreatedEvent` | `occupify.notification.exchange` | `project.created` | Emitted when a new project is created to alert matching candidates. |
| `ProjectClosedEvent` | `occupify.notification.exchange` | `project.closed` | Emitted when a project is closed to notify active bidders. |

### Consumed Events
| Event | Exchange | Routing Key | Queue | Action |
| :--- | :--- | :--- | :--- | :--- |
| `UserActivatedEvent` | `occupify.user.exchange` | `user.activated` | `occupify.core.user.queue` | Synchronizes activated user profile and role into core database. |
