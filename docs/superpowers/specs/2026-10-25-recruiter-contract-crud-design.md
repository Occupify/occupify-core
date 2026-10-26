# Design Specification: Recruiter Contract CRUD, Oracle S3 Presigned Terms Storage & ERD Sync

- **Feature Key:** `OC-76`
- **Topic:** Recruiter Contract Lifecycle Management & Private Terms File Storage with Oracle S3 Presigned URLs
- **Target Repository:** `occupify-core`
- **Frontend Target:** `occupify-web` (Workspace / Project Detail view)
- **Reference Implementation:** `occupify-identity` (`com.occupify.identity.service.storage.impl.S3StorageServiceImpl`)
- **Author:** Antigravity / Gem
- **Date:** 2026-10-25
- **Status:** APPROVED DRAFT

---

## 1. Intent & Context

### 1.1. The "Why"
Occupify connects employers/recruiters with freelancers. Currently, recruiters can post projects, create job roles, and receive bids from applicants. However, recruiters have no backend mechanism to:
1. Formally establish an employment contract (`Contract`) with a candidate either from an existing bid (`JOB_BID`) or via a direct invitation (`DIRECT_OFFER`).
2. Directly attach/upload private agreement terms documents (`.pdf`, `.docx`) upon contract creation or modification.
3. Review, modify, cancel, or complete contracts with reason tracking and rating feedback.

In `occupify-web`, these interactions exist as UI prototypes with in-memory mocks. Task `OC-76` implements the complete backend lifecycle for contracts, synchronizes database schemas with architecture docs, and delivers private document storage using **Oracle Cloud Infrastructure (OCI) Object Storage** with **S3 Presigned URLs**.

### 1.2. Direct User Decisions
- **No System Template Fallback:** If the recruiter does not attach a terms file, `terms_file_key` is simply `NULL`. No default template file is attached.
- **Clear Distinction between Bid and Offer Sources:**
  - **Path A — From Job Application (`source_type = 'JOB_BID'`):** Candidate already applied via a `Bid`. When the recruiter accepts and creates a contract, `bids.status` transitions from `pending` $\rightarrow$ `accepted`. No redundant `Offer` record is created.
  - **Path B — Direct Invitation (`source_type = 'DIRECT_OFFER'`):** Recruiter directly invites a freelancer who hasn't applied. Creates a contract and an associated `Offer` record (`status = 'PENDING'`). Candidate accepts/declines via this offer.
- **Strict Cancellation Authority Matrix:**
  - `PENDING`: Recruiter has the unilateral right to cancel/withdraw anytime.
  - `IN_PROGRESS`: Recruiter has the right to cancel, but MUST provide a valid reason ($\ge 10$ chars) stored in `cancellation_contracts`.
  - `COMPLETED`: Recruiter CANNOT cancel (`400 BAD_REQUEST: CONTRACT_ALREADY_COMPLETED`).
  - `CANCELLED`: Already cancelled (`400 BAD_REQUEST: CONTRACT_ALREADY_CANCELLED`).
  - Ownership: Only the contract's employer (or employee, or admin) can request cancellation. Unauthorized users receive `403 FORBIDDEN`.
- **Cloud Storage:** Use Oracle Cloud Infrastructure (OCI) Object Storage via S3-compatible API, adopting the pattern and dependencies (`software.amazon.awssdk:s3:2.25.40`) from `occupify-identity`.
- **Private Legal Documents:** Contract terms are confidential documents, NOT public assets. The database stores the private **S3 Object Key** (`terms_file_key`). The backend generates temporary **Presigned GET URLs** (TTL: 15 minutes) only for authenticated parties to the contract.
- **Direct Multipart Attachment:** The terms file is attached directly during contract creation (`POST /api/v1/contracts`) or update (`PUT /api/v1/contracts/{id}`) via `multipart/form-data`.
- **ERD & Code Sync:** The database schema (`contracts`, `cancellation_contracts`) and documentation (`docs/CORE-ERD.md`, `docs/ECOSYSTEM-ERD.md`) MUST be updated and synchronized simultaneously.

---

## 2. Goals, Non-Goals & Anti-Goals

### 2.1. Goals
1. **Schema & Doc Synchronization:**
   - Add missing columns to `contracts`: `title VARCHAR(255)`, `terms_file_key VARCHAR(500) NULL`, `job_description TEXT`, `salary_due_date VARCHAR(100)`.
   - Create table `cancellation_contracts` with constraints and foreign keys (`contract_id`, `requested_by_user_id`, `reason`, `status`).
   - Synchronize `docs/CORE-ERD.md` and `docs/ECOSYSTEM-ERD.md`.
2. **Oracle S3 Storage & Presigner Integration:**
   - Add `software.amazon.awssdk:s3:2.25.40` dependency to `pom.xml`.
   - Implement `S3Properties` and `S3Config` supporting OCI Object Storage auto-derived endpoints, credentials, and path-style access.
   - Configure `S3Client` for private upload (`putObject`) and `S3Presigner` for time-limited signed URLs (`presignGetObject` with 15-minute TTL).
   - Create `StorageService` interface handling `uploadPrivateDocument(directory, file)` and `generatePresignedDownloadUrl(objectKey, duration)`.
3. **Contract RESTful API:**
   - `POST /api/v1/contracts` (`multipart/form-data`): Create contract, handle optional file upload, handle `JOB_BID` vs `DIRECT_OFFER` logic, return response with presigned terms URL (if file exists).
   - `GET /api/v1/contracts`: Paginated list of contracts strictly scoped to current user (`employer_id == currentUser` or `employee_id == currentUser`, unless Admin), with filters (`projectId`, `jobId`, `status`).
   - `GET /api/v1/contracts/{id}`: Detailed contract response including employer, employee, job, project, and fresh presigned terms download URL.
   - `PUT /api/v1/contracts/{id}` (`multipart/form-data`): Edit contract details and optional terms file replacement (STRICT: allowed ONLY when `status == PENDING`).
   - `POST /api/v1/contracts/{id}/cancel`: Cancel contract based on the cancellation matrix with required reason ($\ge 10$ chars), insert row into `cancellation_contracts` (`status = 'ACCEPTED'`), update status to `CANCELLED`.
   - `POST /api/v1/contracts/{id}/accept`: Candidate accepts the contract, transitioning status from `PENDING` to `IN_PROGRESS` and setting `started_at = CURRENT_TIMESTAMP`.
   - `POST /api/v1/contracts/{id}/complete`: Recruiter completes contract upon deliverable acceptance with 1-5 star rating and review.

### 2.2. Non-Goals
- **Payment Escrow & Wallet Integration:** Payment ledger and wallet balance locks belong to `occupify-payment`. Contract status reflects `payment_status` locally.
- **Hard Deleting Contracts:** Contracts must never be deleted with SQL `DELETE`.

### 2.3. Anti-Goals
- **No System Template Fallback:** Do not inject system fallback files if recruiter uploaded nothing.
- **Permanent Public URLs for Contracts:** Never expose public URLs for confidential contract files.
- **Separate Standalone Upload Endpoint:** Rejecting detached upload endpoints. File attachment is atomic with contract payload.
- **Unrestricted In-Progress Editing:** Never allow arbitrary rate/deadline modifications when status is `IN_PROGRESS`.

---

## 3. Domain Model & State Lifecycle

### 3.1. Contract State Machine

```mermaid
stateDiagram-v2
    [*] --> PENDING: Recruiter creates contract (from Bid or Direct Offer)
    PENDING --> IN_PROGRESS: Candidate accepts contract (POST /accept)
    PENDING --> CANCELLED: Recruiter withdraws/cancels (POST /cancel)
    IN_PROGRESS --> COMPLETED: Recruiter accepts deliverables & rates freelancer (POST /complete)
    IN_PROGRESS --> CANCELLED: Recruiter cancels with reason (POST /cancel)
    COMPLETED --> [*]
    CANCELLED --> [*]
```

### 3.2. Two Creation Paths (Bid vs Offer)

```mermaid
flowchart TD
    Start([Recruiter initiates contract creation]) --> Choice{Source Type?}
    
    Choice -->|From Bid: JOB_BID| BidFlow[1. Link source_id = bid.id<br/>2. Update bid.status = 'ACCEPTED'<br/>3. Employee = bid.freelancer_id]
    Choice -->|Direct Invite: DIRECT_OFFER| OfferFlow[1. Create Offer record with status = 'PENDING'<br/>2. Link contract.source_id = offer.id<br/>3. Employee = resolved freelancer_id]
    
    BidFlow --> CreateContract[Create Contract with status = 'PENDING']
    OfferFlow --> CreateContract
    CreateContract --> FileCheck{File uploaded?}
    FileCheck -->|Yes| S3Upload[Upload to OCI S3 -> save terms_file_key]
    FileCheck -->|No| NoFile[terms_file_key = NULL]
    S3Upload --> Finish([Contract Created])
    NoFile --> Finish
```

### 3.3. Cancellation Authority Matrix

| Current Contract Status | Can Recruiter Cancel? | Conditions & Side Effects |
|---|---|---|
| **`PENDING`** | **YES** | Unilateral withdrawal. Optional/brief reason. Updates `contracts.status = CANCELLED`. If `DIRECT_OFFER`, sets `offers.status = EXPIRED`. |
| **`IN_PROGRESS`** | **YES** | Permitted, but MUST provide a detailed reason ($\ge 10$ chars). Creates row in `cancellation_contracts` with `status = 'ACCEPTED'` for audit trail. Updates `contracts.status = CANCELLED`. |
| **`COMPLETED`** | **NO** | Forbidden (`400 BAD_REQUEST: CONTRACT_ALREADY_COMPLETED`). Work has concluded and deliverables accepted. |
| **`CANCELLED`** | **NO** | Forbidden (`400 BAD_REQUEST: CONTRACT_ALREADY_CANCELLED`). |

---

## 4. Technical Architecture

### 4.1. Oracle S3 Storage Service Architecture

```
[Create / Update Contract Request (multipart/form-data)]
                     │
                     ▼
             [ContractService]
                     │
      If file attached -> uploadPrivateDocument(file)
                     │
                     ▼
       [Oracle OCI S3-Compatible Bucket]
          Object Key: contracts/{contractId}/{timestamp}_{filename}.pdf
                     │
         Key saved in DB: contracts.terms_file_key
```

When contract details are queried (`GET /api/v1/contracts/{id}`):
```
[Client GET /api/v1/contracts/{id}]
                     │
                     ▼
             [ContractService]
                     │
          If terms_file_key != null -> Generate Presigned GET URL (TTL: 15m)
                     │
                     ▼
             [S3Presigner] ───> Returns signed temporary URL
                     │
                     ▼
  Response DTO includes `termsFileUrl` (null if no file was uploaded)
```

**Interface Definition:**
```java
public interface StorageService {
  String uploadPrivateDocument(String directory, MultipartFile file);
  String generatePresignedDownloadUrl(String objectKey, Duration duration);
  void deleteFile(String objectKey);
}
```

**Configuration Properties (`S3Properties.java`):**
```yaml
app:
  storage:
    s3:
      bucket-namespace: ${S3_BUCKET_NAMESPACE:}
      region: ${S3_REGION:ap-singapore-1}
      endpoint: ${S3_ENDPOINT:}
      bucket-name: ${S3_BUCKET_NAME:occupify-storage}
      access-key: ${S3_ACCESS_KEY:}
      secret-key: ${S3_SECRET_KEY:}
      path-style-access-enabled: ${S3_PATH_STYLE_ACCESS_ENABLED:true}
      presigned-url-duration-minutes: ${S3_PRESIGNED_DURATION_MINUTES:15}
```

---

### 4.2. Database Migration (`V5__create_cancellation_contracts_and_alter_contracts.sql`)

```sql
-- 1. Alter contracts table with UI-aligned attributes
ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS terms_file_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS job_description TEXT,
    ADD COLUMN IF NOT EXISTS salary_due_date VARCHAR(100);

-- 2. Create cancellation_contracts table
CREATE TABLE IF NOT EXISTS cancellation_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    requested_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACCEPTED' CHECK (UPPER(status) IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cancellation_contracts_contract_id ON cancellation_contracts(contract_id);
CREATE INDEX IF NOT EXISTS idx_cancellation_contracts_user_id ON cancellation_contracts(requested_by_user_id);
CREATE INDEX IF NOT EXISTS idx_cancellation_contracts_status ON cancellation_contracts(status);
```

---

### 4.3. API Specifications

#### Endpoint 1: Create Contract (`POST /api/v1/contracts`)
- **Content-Type:** `multipart/form-data`
- **Request Parts:**
  - `data` (`application/json`): `CreateContractRequest`
  - `file` (`MultipartFile`, optional): Contract terms file (.pdf, .doc, .docx, $\le 10$MB).
- **Request DTO:**
  ```json
  {
    "jobId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "employeeId": "3fa85f64-5717-4562-b3fc-2c963f66afa7",
    "freelancerEmail": "freelancer@gmail.com",
    "sourceType": "JOB_BID",
    "sourceId": "3fa85f64-5717-4562-b3fc-2c963f66afa8",
    "title": "Hợp đồng Backend Developer — Redesign Mobile App",
    "agreedRate": 38000000.00,
    "salaryType": "MONTHLY",
    "salaryDueDate": "Ngày 05 hàng tháng",
    "startedAt": "2026-11-01T00:00:00",
    "finishedAt": "2027-04-30T00:00:00",
    "jobDescription": "Đảm nhiệm vị trí Backend Developer cho dự án...",
    "invitationMessage": "Chào bạn, chúng tôi gửi dự thảo hợp đồng..."
  }
  ```
- **Response:** `ApiResponse<ContractDetailResponse>` (`201 CREATED`). If file was attached, `termsFileUrl` contains the freshly generated Presigned GET URL (15-min TTL); otherwise `termsFileUrl` is `null`.

#### Endpoint 2: Get Contract List (`GET /api/v1/contracts`)
- **Query Params:** `projectId`, `jobId`, `status`, `page` (default 0), `size` (default 10), `sort` (default `createdAt,desc`).
- **Scoping:** Results are automatically restricted to contracts where `employer_id == currentUser.id` or `employee_id == currentUser.id` (unless Admin).
- **Response:** `ApiResponse<PageResponse<ContractSummaryResponse>>` (`200 OK`).

#### Endpoint 3: Get Contract Detail (`GET /api/v1/contracts/{id}`)
- **Authorization:** Only the contract's employer, employee, or Admin can access.
- **Response:** `ApiResponse<ContractDetailResponse>` (`200 OK`). Includes fresh 15-minute Presigned URL if `terms_file_key != null`.

#### Endpoint 4: Update Contract (`PUT /api/v1/contracts/{id}`)
- **Content-Type:** `multipart/form-data`
- **Request Parts:**
  - `data` (`application/json`): `UpdateContractRequest`
  - `file` (`MultipartFile`, optional): Replacement terms file.
- **Condition:** Allowed ONLY if `status == ContractStatus.PENDING`.
- **Response:** `ApiResponse<ContractDetailResponse>` (`200 OK`).

#### Endpoint 5: Cancel Contract (`POST /api/v1/contracts/{id}/cancel`)
- **Content-Type:** `application/json`
- **Request Body:**
  ```json
  {
    "reason": "Freelancer không phản hồi thỏa thuận sau 7 ngày làm việc."
  }
  ```
- **Behavior:**
  - Validates cancellation matrix (disallows cancellation if `COMPLETED` or `CANCELLED`).
  - Inserts row into `cancellation_contracts` with `status = 'ACCEPTED'`.
  - Sets `contracts.status = 'CANCELLED'`.
  - If `sourceType == 'DIRECT_OFFER'`, cascades `offers.status = 'EXPIRED'`.
- **Response:** `ApiResponse<ContractDetailResponse>` (`200 OK`).

#### Endpoint 6: Candidate Accept Contract (`POST /api/v1/contracts/{id}/accept`)
- **Authorization:** Only the designated employee can accept.
- **Behavior:**
  - Sets `contracts.status = 'IN_PROGRESS'`.
  - Sets `contracts.started_at = CURRENT_TIMESTAMP`.
  - If `sourceType == 'DIRECT_OFFER'`, sets `offers.status = 'ACCEPTED'`.
- **Response:** `ApiResponse<ContractDetailResponse>` (`200 OK`).

#### Endpoint 7: Complete Contract (`POST /api/v1/contracts/{id}/complete`)
- **Authorization:** Only the employer can complete.
- **Content-Type:** `application/json`
- **Request Body:**
  ```json
  {
    "rating": 5,
    "comment": "Chất lượng bàn giao xuất sắc, giao diện chuẩn chỉ đúng hạn."
  }
  ```
- **Behavior:**
  - Sets `contracts.status = 'COMPLETED'`.
  - Sets `contracts.finished_at = CURRENT_TIMESTAMP`.
  - Creates review record in `reviews` table (`rating`, `comment`, `reviewer_user_id = employer.id`, `reviewee_user_id = employee.id`, `contract_id = contract.id`).
- **Response:** `ApiResponse<ContractDetailResponse>` (`200 OK`).

---

## 5. Error Codes & Boundaries

| Code | HTTP Status | Description |
|---|---|---|
| `CONTRACT_001` | 404 NOT_FOUND | Contract not found with the given ID. |
| `CONTRACT_002` | 400 BAD_REQUEST | Cannot modify an active or completed contract (`status != PENDING`). |
| `CONTRACT_003` | 400 BAD_REQUEST | Contract job does not belong to the employer's project. |
| `CONTRACT_004` | 400 BAD_REQUEST | Freelancer user not found with provided ID or email. |
| `CONTRACT_005` | 400 BAD_REQUEST | Cancellation reason is required (min 10 characters). |
| `CONTRACT_006` | 400 BAD_REQUEST | Contract already completed (`CONTRACT_ALREADY_COMPLETED`). |
| `CONTRACT_007` | 400 BAD_REQUEST | Contract already cancelled (`CONTRACT_ALREADY_CANCELLED`). |
| `STORAGE_001`  | 400 BAD_REQUEST | Invalid file type. Allowed: PDF, DOC, DOCX, PNG, JPG. |
| `STORAGE_002`  | 400 BAD_REQUEST | File size exceeds limit (10MB). |
| `STORAGE_003`  | 500 INTERNAL_ERROR | Failed to upload or presign object on Oracle S3. |

---

## 6. What's Left to the Builder
- Unit and integration tests with Mockito mocking `S3Client` and `S3Presigner`.
- MapStruct mapping configurations between `Contract`, DTOs, and Presigned URL generation.
- Graceful fallback for local development if S3 credentials are not configured (dummy presigned URL generation).
