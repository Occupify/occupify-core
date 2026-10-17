# Occupify Core - API Specification

For business and system error codes, refer to [error_code.md](error_code.md).

> [!NOTE]
> **Local Authentication (`DEV_MOCK_AUTH_ENABLED=true`):**
> Enabled by default when running locally to bypass Gateway headers using the following mock credentials:
> - **User ID:** `00000000-0000-0000-0000-000000000001`
> - **Role:** `CLIENT`
> - **Email:** `dev-employer@occupify.local`
> 
> *(Note: GitHub Secrets will disable this in CI/CD and production environments by setting `DEV_MOCK_AUTH_ENABLED=false`).*

---

## 1. Create Project
**Endpoint:** `POST /projects`  
**Description:** Creates a new project owned by the authenticated user.  
**Authorization:** Authorized for users (not permitted for admins).

### Parameters

#### Headers
| Name | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `Authorization` | String | Conditional | `Bearer <JWT>` (when calling directly via Scalar/Swagger) |
| `X-User-Id` | UUID | Conditional | User UUID (injected automatically by Gateway) |
| `X-User-Role` | String | Optional | User role (e.g. `CLIENT`, `FREELANCER`) |
| `X-User-Email` | String | Optional | User email address |

#### Request Body (`application/json`)
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `title` | String | Yes | Max 255 chars, not blank | Project title |
| `description` | String | No | Max 5000 chars | Project description |
| `startPrice` | BigDecimal | No | Must be > 0 | Starting budget / price |
| `period` | String (Enum) | Yes | `FIXED`, `HOURLY`, `MONTHLY` | Payment / timeline period |
| `dueDate` | String (Date) | No | Present or future (`YYYY-MM-DD`) | Deadline for project delivery |
| `experienceLevel` | String (Enum) | No | `ENTRY`, `INTERMEDIATE`, `EXPERT` | Desired freelancer experience |

#### Response Data (`ProjectResponse`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | UUID | Project ID |
| `title` | String | Project title |
| `description` | String | Project description |
| `startPrice` | BigDecimal | Starting price |
| `period` | String (Enum) | `FIXED`, `HOURLY`, `MONTHLY` |
| `dueDate` | String (Date) | Deadline date |
| `experienceLevel` | String (Enum) | `ENTRY`, `INTERMEDIATE`, `EXPERT` |
| `status` | String (Enum) | Project status (`OPEN`) |
| `closedAt` | String (DateTime) | Timestamp when project was closed (null on create) |
| `employer` | Object | Employer summary (`id`, `fullName`, `email`, `avatarUrl`) |
| `createdAt` | String (DateTime) | Creation timestamp |
| `updatedAt` | String (DateTime) | Last update timestamp |

---

## 2. Get Paginated Projects
**Endpoint:** `GET /projects`  
**Description:** Retrieves a paginated and filtered list of projects.  
**Authorization:** Public (No authentication required).

### Parameters

#### Query Parameters
| Parameter | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `keyword` | String | No | - | Case-insensitive search on title and description |
| `status` | String (Enum) | No | - | Filter by status: `OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| `employerId` | UUID | No | - | Filter by employer UUID |
| `period` | String (Enum) | No | - | Filter by period: `FIXED`, `HOURLY`, `MONTHLY` |
| `experienceLevel` | String (Enum) | No | - | Filter by level: `ENTRY`, `INTERMEDIATE`, `EXPERT` |
| `minPrice` | BigDecimal | No | - | Minimum starting price |
| `maxPrice` | BigDecimal | No | - | Maximum starting price |
| `page` | Integer | No | `0` | Zero-based page index (must be >= 0) |
| `size` | Integer | No | `20` | Page size (must be >= 1) |
| `sort` | String | No | `createdAt,desc` | Sorting criteria (`field,asc\|desc`) |

#### Response Data (`Page<ProjectResponse>`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `items` | Array[`ProjectResponse`] | List of projects on the current page |
| `pageNumber` | Integer | Current page index |
| `pageSize` | Integer | Requested page size |
| `totalElements` | Long | Total number of matched projects |
| `totalPages` | Integer | Total pages available |
| `isFirst` | Boolean | True if first page |
| `isLast` | Boolean | True if last page |

---

## 3. Get Project Details
**Endpoint:** `GET /projects/{id}`  
**Description:** Retrieves full details of a specific project, including associated jobs and skills.  
**Authorization:** Public (No authentication required).

### Parameters

#### Path Parameters
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | Yes | Unique identifier of the project |

#### Response Data (`ProjectDetailResponse`)
| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | UUID | Project ID |
| `title` | String | Project title |
| `description` | String | Project description |
| `startPrice` | BigDecimal | Starting price |
| `period` | String (Enum) | `FIXED`, `HOURLY`, `MONTHLY` |
| `dueDate` | String (Date) | Deadline date |
| `experienceLevel` | String (Enum) | `ENTRY`, `INTERMEDIATE`, `EXPERT` |
| `status` | String (Enum) | Project status |
| `closedAt` | String (DateTime) | Timestamp when project was closed |
| `employer` | Object | Employer summary (`id`, `fullName`, `email`, `avatarUrl`) |
| `jobs` | Array[`JobSummaryResponse`] | List of jobs with skills (`id`, `title`, `description`, `budget`, `headcount`, `skills`) |
| `createdAt` | String (DateTime) | Creation timestamp |
| `updatedAt` | String (DateTime) | Last update timestamp |

---

## 4. Update Project
**Endpoint:** `PUT /projects/{id}`  
**Description:** Updates project details.  
**Authorization:** Authorized for users (only permitted by the project owner user; admins not permitted).

### Parameters

#### Headers
| Name | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `Authorization` | String | Conditional | `Bearer <JWT>` |
| `X-User-Id` | UUID | Conditional | User UUID (injected by Gateway) |
| `X-User-Role` | String | Optional | User role |
| `X-User-Email` | String | Optional | User email |

#### Path Parameters
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | Yes | Unique identifier of the project to update |

#### Request Body (`application/json`)
| Field | Type | Required | Constraints | Description |
| :--- | :--- | :--- | :--- | :--- |
| `title` | String | No | Max 255 chars | Updated title |
| `description` | String | No | Max 5000 chars | Updated description |
| `startPrice` | BigDecimal | No | Must be > 0 | Updated starting budget |
| `period` | String (Enum) | No | `FIXED`, `HOURLY`, `MONTHLY` | Updated period |
| `dueDate` | String (Date) | No | Present or future (`YYYY-MM-DD`) | Updated deadline |
| `experienceLevel` | String (Enum) | No | `ENTRY`, `INTERMEDIATE`, `EXPERT` | Updated experience level |

#### Response Data (`ProjectResponse`)
Returns updated `ProjectResponse` object.

---

## 5. Close Project
**Endpoint:** `PATCH /projects/{id}/close`  
**Description:** Closes a project (sets status to `COMPLETED`, records `closedAt`, and emits RabbitMQ `project.closed` event).  
**Authorization:** Allowed by users (project owner) and admins.

### Parameters

#### Headers
| Name | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `Authorization` | String | Conditional | `Bearer <JWT>` |
| `X-User-Id` | UUID | Conditional | User UUID (injected by Gateway) |
| `X-User-Role` | String | Optional | User role |
| `X-User-Email` | String | Optional | User email |

#### Path Parameters
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `id` | UUID | Yes | Unique identifier of the project to close |

#### Response Data (`ProjectResponse`)
Returns updated `ProjectResponse` object with `status = "COMPLETED"` and populated `closedAt`.
