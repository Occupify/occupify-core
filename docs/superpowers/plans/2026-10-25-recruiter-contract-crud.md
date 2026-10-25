# Recruiter Contract CRUD, Oracle S3 Presigned Terms Storage & ERD Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the complete backend lifecycle for employment contracts in `occupify-core` (Task OC-76), including contract creation with direct terms file attachment to Oracle OCI S3, presigned GET URL generation, update locks, cancellation tracking, candidate acceptance, and ERD documentation synchronization.

**Architecture:** A layered Spring Boot REST architecture where `ContractController` receives `multipart/form-data` payloads, delegating to `ContractServiceImpl` for state machine validation, cascading `bids`/`offers` updates, and `S3StorageServiceImpl` for confidential document uploads via AWS S3 SDK v2 to OCI Object Storage. On retrieval, `S3Presigner` dynamically generates short-lived presigned GET URLs for authorized contract participants.

**Tech Stack:** Java 25, Spring Boot 3.3.4, Spring Data JPA, PostgreSQL / Flyway, H2 In-Memory DB (testing), AWS SDK v2 (`software.amazon.awssdk:s3:2.25.40`), MapStruct 1.6.2, Lombok 1.18.48, JUnit 5, Mockito.

**Spec:** `docs/superpowers/specs/2026-10-25-recruiter-contract-crud-design.md`

## Global Constraints
- Target Framework: Spring Boot 3.3.4, Java release 25.
- Controller response envelopes MUST extend `AbstractBaseController` using `created()`, `successSingle()`, `paging()`.
- Error codes MUST follow domain prefix `CONTRACT_xxx` and `STORAGE_xxx` extending `AppErrorCode`.
- S3 SDK dependencies MUST use `software.amazon.awssdk:s3:2.25.40` matching `occupify-identity`.
- Contracts MUST NOT have permanent public URLs; terms files are private and accessed via 15-minute presigned URLs.
- Contracts MUST NOT fall back to default template files if no file is uploaded (`terms_file_key = NULL`).
- Database migrations MUST follow Flyway naming `V5__create_cancellation_contracts_and_alter_contracts.sql`.
- Unit and integration tests must run clean via `./mvnw clean test` without external cloud dependencies.

## Review Focus
1. **Unattached File on Create/Update:** When a contract is created or updated without a file, `terms_file_key` remains `null` and response `termsFileUrl` is `null` without throwing `NullPointerException`.
2. **Invalid State Mutation:** Attempting to update rate/dates via `PUT /contracts/{id}` when `status` is `IN_PROGRESS`, `COMPLETED`, or `CANCELLED` must be blocked with `400 BAD_REQUEST (CONTRACT_ALREADY_ACTIVE)`.
3. **Invalid Cancellation on Completed Contract:** Attempting to cancel a contract with `status == COMPLETED` must be rejected with `400 BAD_REQUEST (CONTRACT_ALREADY_COMPLETED)`.
4. **Short Cancellation Reason:** Cancelling with a reason under 10 characters or blank must be rejected with `400 BAD_REQUEST (CONTRACT_005)`.
5. **Unauthorized File Presigning:** Querying contract details by a user who is neither employer, employee, nor admin must be blocked with `403 FORBIDDEN`.

---

### Task 1: Database Migration, Entity Models & ERD Sync

**Files:**
- Create: `src/main/resources/db/migration/V5__create_cancellation_contracts_and_alter_contracts.sql`
- Create: `src/main/java/com/occupify/core/entity/contract/CancellationContract.java`
- Create: `src/main/java/com/occupify/core/repository/contract/CancellationContractRepository.java`
- Modify: `src/main/resources/init-h2.sql:95-125`
- Modify: `src/main/java/com/occupify/core/entity/contract/Contract.java:30-70`
- Modify: `src/main/java/com/occupify/core/repository/contract/ContractRepository.java:8-16`
- Modify: `docs/CORE-ERD.md:150-170,300-340`
- Modify: `docs/ECOSYSTEM-ERD.md:470-500`
- Test: `src/test/java/com/occupify/core/entity/contract/ContractEntityTest.java`

**Interfaces:**
- Consumes: Existing `Contract`, `User`, `Job` entities.
- Produces: Updated `Contract` with `title`, `termsFileKey`, `jobDescription`, `salaryDueDate`; new `CancellationContract` entity; `CancellationContractRepository` for auditing cancel requests.

- [ ] **Step 1: Write the failing entity and repository test**

```java
// src/test/java/com/occupify/core/entity/contract/ContractEntityTest.java
package com.occupify.core.entity.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.repository.contract.CancellationContractRepository;
import com.occupify.core.repository.contract.ContractRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ContractEntityTest {

  @Autowired private ContractRepository contractRepository;
  @Autowired private CancellationContractRepository cancellationContractRepository;

  @Test
  @DisplayName("Should persist contract with new fields and save cancellation request")
  void shouldPersistContractAndCancellation() {
    User employer = User.builder().email("employer@test.local").fullName("Employer Test").build();
    Job job = Job.builder().title("Backend Dev").quantity(1).build();

    Contract contract = Contract.builder()
        .employer(employer)
        .job(job)
        .title("Hợp đồng Backend Dev")
        .agreedRate(new BigDecimal("25000000.00"))
        .salaryType("MONTHLY")
        .salaryDueDate("Ngày 05 hàng tháng")
        .jobDescription("Mô tả công việc chi tiết")
        .termsFileKey("contracts/sample-key.pdf")
        .status(ContractStatus.PENDING)
        .build();

    Contract savedContract = contractRepository.save(contract);
    assertThat(savedContract.getId()).isNotNull();
    assertThat(savedContract.getTitle()).isEqualTo("Hợp đồng Backend Dev");
    assertThat(savedContract.getTermsFileKey()).isEqualTo("contracts/sample-key.pdf");

    CancellationContract cancelReq = CancellationContract.builder()
        .contract(savedContract)
        .requestedByUser(employer)
        .reason("Freelancer không phản hồi thỏa thuận sau 7 ngày.")
        .status("ACCEPTED")
        .build();

    CancellationContract savedCancel = cancellationContractRepository.save(cancelReq);
    assertThat(savedCancel.getId()).isNotNull();
    assertThat(savedCancel.getReason()).contains("không phản hồi");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ContractEntityTest`
Expected: FAIL compilation with "cannot find symbol CancellationContract" or missing getters on `Contract`.

- [ ] **Step 3: Implement Flyway migration, H2 schema, entities, repositories, and update ERD docs**

1. Write `src/main/resources/db/migration/V5__create_cancellation_contracts_and_alter_contracts.sql`:
   - `ALTER TABLE contracts ADD COLUMN title VARCHAR(255), ADD COLUMN terms_file_key VARCHAR(500), ADD COLUMN job_description TEXT, ADD COLUMN salary_due_date VARCHAR(100);`
   - `CREATE TABLE IF NOT EXISTS cancellation_contracts (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE, requested_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE, reason TEXT NOT NULL, status VARCHAR(50) NOT NULL DEFAULT 'ACCEPTED', created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP);`
   - Add indexes on `contract_id`, `requested_by_user_id`, `status`.
2. Add corresponding table and columns into `src/main/resources/init-h2.sql`.
3. Add fields to `Contract.java`:
   - `private @Nullable String title;`
   - `private @Nullable String termsFileKey;`
   - `private @Nullable String jobDescription;`
   - `private @Nullable String salaryDueDate;`
4. Create `CancellationContract.java` in `src/main/java/com/occupify/core/entity/contract/`:
   - Extends `AbstractBaseEntity`, `@ManyToOne Contract contract`, `@ManyToOne User requestedByUser`, `String reason`, `String status`.
5. Create `CancellationContractRepository.java` in `src/main/java/com/occupify/core/repository/contract/`:
   - `List<CancellationContract> findByContractIdOrderByCreatedAtDesc(UUID contractId);`
   - `Optional<CancellationContract> findFirstByContractIdOrderByCreatedAtDesc(UUID contractId);`
6. Add filter queries to `ContractRepository.java`:
   - `Page<Contract> findByEmployerId(UUID employerId, Pageable pageable);`
   - `Page<Contract> findByEmployeeId(UUID employeeId, Pageable pageable);`
   - Specification/Query for `job_id`, `status`.
7. Synchronize `docs/CORE-ERD.md` and `docs/ECOSYSTEM-ERD.md` with `terms_file_key`, `title`, `job_description`, `salary_due_date`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=ContractEntityTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V5__create_cancellation_contracts_and_alter_contracts.sql \
        src/main/resources/init-h2.sql \
        src/main/java/com/occupify/core/entity/contract/ \
        src/main/java/com/occupify/core/repository/contract/ \
        docs/CORE-ERD.md docs/ECOSYSTEM-ERD.md \
        src/test/java/com/occupify/core/entity/contract/ContractEntityTest.java
git commit -m "feat(contract): add cancellation entity, alter contracts table, and sync erd docs"
```

---

### Task 2: Oracle OCI S3 Storage Layer & Presigner Service

**Files:**
- Modify: `pom.xml` (add AWS SDK v2 S3 dependency)
- Create: `src/main/java/com/occupify/core/config/S3Properties.java`
- Create: `src/main/java/com/occupify/core/config/S3Config.java`
- Create: `src/main/java/com/occupify/core/exception/storage/StorageErrorCode.java`
- Create: `src/main/java/com/occupify/core/exception/storage/StorageException.java`
- Create: `src/main/java/com/occupify/core/service/storage/StorageService.java`
- Create: `src/main/java/com/occupify/core/service/storage/impl/S3StorageServiceImpl.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/test/resources/application-test.yml`
- Test: `src/test/java/com/occupify/core/service/storage/S3StorageServiceTest.java`

**Interfaces:**
- Consumes: AWS SDK S3 `S3Client`, `S3Presigner`, Spring `MultipartFile`.
- Produces: `StorageService` interface with `uploadPrivateDocument(String directory, MultipartFile file)` returning `objectKey`, and `generatePresignedDownloadUrl(String objectKey, Duration duration)` returning time-limited signed URL string.

- [ ] **Step 1: Write the failing storage service unit test**

```java
// src/test/java/com/occupify/core/service/storage/S3StorageServiceTest.java
package com.occupify.core.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.occupify.core.config.S3Properties;
import com.occupify.core.exception.storage.StorageException;
import com.occupify.core.service.storage.impl.S3StorageServiceImpl;
import java.net.URL;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3StorageServiceTest {

  private S3Client s3Client;
  private S3Presigner s3Presigner;
  private S3Properties s3Properties;
  private StorageService storageService;

  @BeforeEach
  void setUp() {
    s3Client = mock(S3Client.class);
    s3Presigner = mock(S3Presigner.class);
    s3Properties = new S3Properties();
    s3Properties.setBucketName("test-bucket");
    s3Properties.setRegion("ap-singapore-1");
    storageService = new S3StorageServiceImpl(s3Client, s3Presigner, s3Properties);
  }

  @Test
  @DisplayName("Should upload private document and return object key")
  void shouldUploadPrivateDocument() {
    MockMultipartFile file = new MockMultipartFile(
        "file", "contract.pdf", "application/pdf", "dummy pdf content".getBytes());

    when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    String objectKey = storageService.uploadPrivateDocument("contracts/123", file);
    assertThat(objectKey).startsWith("contracts/123/").endsWith(".pdf");
    verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  @DisplayName("Should reject empty or oversize files")
  void shouldRejectInvalidFile() {
    MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
    assertThatThrownBy(() -> storageService.uploadPrivateDocument("contracts/123", emptyFile))
        .isInstanceOf(StorageException.class);
  }

  @Test
  @DisplayName("Should generate presigned download URL")
  throws Exception {
    PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
    when(presignedRequest.url()).thenReturn(new URL("https://test.oraclecloud.com/signed-url"));
    when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

    String signedUrl = storageService.generatePresignedDownloadUrl("contracts/123/file.pdf", Duration.ofMinutes(15));
    assertThat(signedUrl).isEqualTo("https://test.oraclecloud.com/signed-url");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=S3StorageServiceTest`
Expected: FAIL compilation with "cannot find symbol S3Properties / StorageService".

- [ ] **Step 3: Implement S3 dependencies, configuration, properties, and S3StorageServiceImpl**

1. In `pom.xml`, add property `<aws.sdk.version>2.25.40</aws.sdk.version>` and dependency:
   ```xml
   <dependency>
       <groupId>software.amazon.awssdk</groupId>
       <artifactId>s3</artifactId>
       <version>${aws.sdk.version}</version>
       <exclusions>
           <exclusion>
               <groupId>commons-logging</groupId>
               <artifactId>commons-logging</artifactId>
           </exclusion>
       </exclusions>
   </dependency>
   ```
2. Create `S3Properties.java` in `src/main/java/com/occupify/core/config/` (mapped to `app.storage.s3`):
   - `bucketNamespace`, `region` (default `ap-singapore-1`), `endpoint`, `bucketName` (default `occupify-storage`), `accessKey`, `secretKey`, `pathStyleAccessEnabled` (default `true`), `presignedDurationMinutes` (default `15`).
3. Create `S3Config.java` in `src/main/java/com/occupify/core/config/`:
   - `@Bean public S3Client s3Client()`: configured with OCI endpoint / credentials.
   - `@Bean public S3Presigner s3Presigner()`: configured for generating presigned GET URLs.
4. Create `StorageErrorCode.java` and `StorageException.java` in `src/main/java/com/occupify/core/exception/storage/`:
   - `FILE_EMPTY`, `FILE_TOO_LARGE`, `INVALID_FILE_TYPE`, `UPLOAD_FAILED`, `PRESIGN_FAILED`, `DELETE_FAILED`.
5. Create `StorageService.java` and `S3StorageServiceImpl.java` in `src/main/java/com/occupify/core/service/storage/`:
   - `uploadPrivateDocument(String directory, MultipartFile file)`: validates size $\le 10$MB and mime types (`application/pdf`, `msword`, `vnd.openxmlformats-officedocument.wordprocessingml.document`, `image/png`, `image/jpeg`). Generates key: `{cleanDirectory}/{UUID}_{originalFilename}`. Uploads with `s3Client.putObject`.
   - `generatePresignedDownloadUrl(String objectKey, Duration duration)`: calls `s3Presigner.presignGetObject`.
   - `deleteFile(String objectKey)`: calls `s3Client.deleteObject`.
6. Update `application.yml` and `application-test.yml` with default S3 properties.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=S3StorageServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add pom.xml \
        src/main/java/com/occupify/core/config/S3* \
        src/main/java/com/occupify/core/exception/storage/ \
        src/main/java/com/occupify/core/service/storage/ \
        src/main/resources/application.yml \
        src/test/resources/application-test.yml \
        src/test/java/com/occupify/core/service/storage/S3StorageServiceTest.java
git commit -m "feat(storage): implement oracle oci s3 storage service with presigned url generation"
```

---

### Task 3: Contract DTOs, Mappers, Exception Hierarchy & Constants

**Files:**
- Create: `src/main/java/com/occupify/core/exception/contract/ContractErrorCode.java`
- Create: `src/main/java/com/occupify/core/exception/contract/ContractException.java`
- Create: `src/main/java/com/occupify/core/dto/request/contract/CreateContractRequest.java`
- Create: `src/main/java/com/occupify/core/dto/request/contract/UpdateContractRequest.java`
- Create: `src/main/java/com/occupify/core/dto/request/contract/CancelContractRequest.java`
- Create: `src/main/java/com/occupify/core/dto/request/contract/CompleteContractRequest.java`
- Create: `src/main/java/com/occupify/core/dto/request/contract/ContractFilterRequest.java`
- Create: `src/main/java/com/occupify/core/dto/response/contract/ContractSummaryResponse.java`
- Create: `src/main/java/com/occupify/core/dto/response/contract/ContractDetailResponse.java`
- Create: `src/main/java/com/occupify/core/dto/response/contract/CancellationContractResponse.java`
- Create: `src/main/java/com/occupify/core/mapper/ContractMapper.java`
- Modify: `src/main/java/com/occupify/core/constant/RouteConstants.java:7-12`
- Modify: `src/main/java/com/occupify/core/constant/MessageConstants.java:10-20`
- Test: `src/test/java/com/occupify/core/mapper/ContractMapperTest.java`

**Interfaces:**
- Consumes: `Contract`, `CancellationContract`, `User`, `Job`.
- Produces: Type-safe request/response DTOs, route constants (`RouteConstants.CONTRACTS`), message constants, and MapStruct `ContractMapper`.

- [ ] **Step 1: Write the failing mapper test**

```java
// src/test/java/com/occupify/core/mapper/ContractMapperTest.java
package com.occupify.core.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.occupify.core.dto.response.contract.ContractDetailResponse;
import com.occupify.core.dto.response.contract.ContractSummaryResponse;
import com.occupify.core.entity.contract.Contract;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ContractMapperTest {

  private final ContractMapper mapper = Mappers.getMapper(ContractMapper.class);

  @Test
  @DisplayName("Should map Contract to ContractSummaryResponse and ContractDetailResponse")
  void shouldMapContractToDto() {
    User employer = User.builder().fullName("Employer Name").email("emp@test.local").build();
    User employee = User.builder().fullName("Freelancer Name").email("free@test.local").build();
    Project project = Project.builder().title("Redesign Mobile App").build();
    Job job = Job.builder().title("Backend Dev").project(project).build();

    Contract contract = Contract.builder()
        .employer(employer)
        .employee(employee)
        .job(job)
        .title("Hợp đồng Backend Dev")
        .agreedRate(new BigDecimal("38000000.00"))
        .salaryType("MONTHLY")
        .salaryDueDate("Ngày 05 hàng tháng")
        .status(ContractStatus.PENDING)
        .termsFileKey("contracts/sample.pdf")
        .startedAt(LocalDateTime.of(2026, 11, 1, 0, 0))
        .finishedAt(LocalDateTime.of(2027, 4, 30, 0, 0))
        .build();

    ContractSummaryResponse summary = mapper.toSummaryResponse(contract);
    assertThat(summary.getTitle()).isEqualTo("Hợp đồng Backend Dev");
    assertThat(summary.getEmployerName()).isEqualTo("Employer Name");
    assertThat(summary.getEmployeeName()).isEqualTo("Freelancer Name");
    assertThat(summary.getProjectTitle()).isEqualTo("Redesign Mobile App");

    String signedUrl = "https://objectstorage.signed.url";
    ContractDetailResponse detail = mapper.toDetailResponse(contract, signedUrl, null);
    assertThat(detail.getTitle()).isEqualTo("Hợp đồng Backend Dev");
    assertThat(detail.getTermsFileUrl()).isEqualTo(signedUrl);
    assertThat(detail.getJob().getTitle()).isEqualTo("Backend Dev");
    assertThat(detail.getProject().getTitle()).isEqualTo("Redesign Mobile App");
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ContractMapperTest`
Expected: FAIL compilation with "cannot find symbol ContractMapper / ContractSummaryResponse".

- [ ] **Step 3: Implement contract exceptions, request/response DTOs, constants, and MapStruct mapper**

1. Create `ContractErrorCode.java` in `src/main/java/com/occupify/core/exception/contract/`:
   - `CONTRACT_NOT_FOUND (404)`, `CONTRACT_ALREADY_ACTIVE (400)`, `JOB_NOT_OWNED (400)`, `USER_NOT_FOUND (400)`, `INVALID_CANCELLATION_REASON (400)`, `CONTRACT_ALREADY_COMPLETED (400)`, `CONTRACT_ALREADY_CANCELLED (400)`.
2. Create `ContractException.java`.
3. Create Request DTOs with Bean Validation annotations (`@NotNull`, `@NotBlank`, `@Size`):
   - `CreateContractRequest.java`: `jobId`, `employeeId`, `freelancerEmail`, `sourceType`, `sourceId`, `title`, `agreedRate`, `salaryType`, `salaryDueDate`, `startedAt`, `finishedAt`, `jobDescription`, `invitationMessage`.
   - `UpdateContractRequest.java`: `title`, `agreedRate`, `salaryType`, `salaryDueDate`, `startedAt`, `finishedAt`, `jobDescription`.
   - `CancelContractRequest.java`: `@NotBlank @Size(min = 10, message = "Lý do hủy hợp đồng phải có ít nhất 10 ký tự") String reason`.
   - `CompleteContractRequest.java`: `@NotNull @Min(1) @Max(5) Integer rating`, `String comment`.
   - `ContractFilterRequest.java`: `UUID projectId`, `UUID jobId`, `ContractStatus status`.
4. Create Response DTOs:
   - `ContractSummaryResponse.java`, `ContractDetailResponse.java`, `CancellationContractResponse.java`.
5. Create `ContractMapper.java`:
   - `@Mapper(componentModel = "spring")`: mappings for summary, detail with presigned URL parameter, and cancellation response.
6. Add constants to `RouteConstants.java` and `MessageConstants.java`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=ContractMapperTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/occupify/core/exception/contract/ \
        src/main/java/com/occupify/core/dto/request/contract/ \
        src/main/java/com/occupify/core/dto/response/contract/ \
        src/main/java/com/occupify/core/mapper/ContractMapper.java \
        src/main/java/com/occupify/core/constant/RouteConstants.java \
        src/main/java/com/occupify/core/constant/MessageConstants.java \
        src/test/java/com/occupify/core/mapper/ContractMapperTest.java
git commit -m "feat(contract): define contract dtos, exceptions, constants, and mapstruct mapper"
```

---

### Task 4: Contract Service Business Logic & Cascading Rules

**Files:**
- Create: `src/main/java/com/occupify/core/service/ContractService.java`
- Create: `src/main/java/com/occupify/core/service/impl/ContractServiceImpl.java`
- Test: `src/test/java/com/occupify/core/service/ContractServiceTest.java`

**Interfaces:**
- Consumes: `ContractRepository`, `CancellationContractRepository`, `JobRepository`, `UserRepository`, `BidRepository`, `OfferRepository`, `ReviewRepository`, `StorageService`, `ContractMapper`.
- Produces: `ContractService` with methods:
  - `ContractDetailResponse createContract(CreateContractRequest request, MultipartFile file, UserContext userContext)`
  - `ContractDetailResponse updateContract(UUID id, UpdateContractRequest request, MultipartFile file, UserContext userContext)`
  - `PageResponse<ContractSummaryResponse> getContracts(ContractFilterRequest filter, Pageable pageable, UserContext userContext)`
  - `ContractDetailResponse getContractDetail(UUID id, UserContext userContext)`
  - `ContractDetailResponse cancelContract(UUID id, CancelContractRequest request, UserContext userContext)`
  - `ContractDetailResponse acceptContract(UUID id, UserContext userContext)`
  - `ContractDetailResponse completeContract(UUID id, CompleteContractRequest request, UserContext userContext)`

- [ ] **Step 1: Write the failing service test exercising business invariants**

```java
// src/test/java/com/occupify/core/service/ContractServiceTest.java
package com.occupify.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.occupify.core.dto.request.contract.CancelContractRequest;
import com.occupify.core.dto.request.contract.CreateContractRequest;
import com.occupify.core.dto.response.contract.ContractDetailResponse;
import com.occupify.core.entity.bid.Bid;
import com.occupify.core.entity.contract.Contract;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.offer.Offer;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.BidStatus;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.enums.OfferStatus;
import com.occupify.core.enums.UserRole;
import com.occupify.core.exception.contract.ContractException;
import com.occupify.core.mapper.ContractMapper;
import com.occupify.core.repository.bid.BidRepository;
import com.occupify.core.repository.contract.CancellationContractRepository;
import com.occupify.core.repository.contract.ContractRepository;
import com.occupify.core.repository.job.JobRepository;
import com.occupify.core.repository.offer.OfferRepository;
import com.occupify.core.repository.review.ReviewRepository;
import com.occupify.core.repository.user.UserRepository;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.impl.ContractServiceImpl;
import com.occupify.core.service.storage.StorageService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ContractServiceTest {

  private ContractRepository contractRepository;
  private CancellationContractRepository cancellationContractRepository;
  private JobRepository jobRepository;
  private UserRepository userRepository;
  private BidRepository bidRepository;
  private OfferRepository offerRepository;
  private ReviewRepository reviewRepository;
  private StorageService storageService;
  private ContractMapper contractMapper;

  private ContractService contractService;

  private UUID employerId;
  private UserContext employerContext;
  private User employer;
  private User employee;
  private Job job;

  @BeforeEach
  void setUp() {
    contractRepository = mock(ContractRepository.class);
    cancellationContractRepository = mock(CancellationContractRepository.class);
    jobRepository = mock(JobRepository.class);
    userRepository = mock(UserRepository.class);
    bidRepository = mock(BidRepository.class);
    offerRepository = mock(OfferRepository.class);
    reviewRepository = mock(ReviewRepository.class);
    storageService = mock(StorageService.class);
    contractMapper = mock(ContractMapper.class);

    contractService = new ContractServiceImpl(
        contractRepository, cancellationContractRepository, jobRepository,
        userRepository, bidRepository, offerRepository, reviewRepository,
        storageService, contractMapper);

    employerId = UUID.randomUUID();
    employerContext = new UserContext(employerId, UserRole.USER, "employer@test.local");
    employer = User.builder().email("employer@test.local").build();
    employer.setId(employerId);

    employee = User.builder().email("freelancer@test.local").build();
    employee.setId(UUID.randomUUID());

    Project project = Project.builder().employer(employer).title("Project A").build();
    job = Job.builder().project(project).title("Job Role").build();
    job.setId(UUID.randomUUID());
  }

  @Test
  @DisplayName("Should create contract from Bid, cascade bid status to ACCEPTED and create Offer")
  void shouldCreateContractFromBid() {
    UUID bidId = UUID.randomUUID();
    Bid bid = Bid.builder().job(job).freelancer(employee).status(BidStatus.PENDING).build();
    bid.setId(bidId);

    CreateContractRequest request = CreateContractRequest.builder()
        .jobId(job.getId())
        .employeeId(employee.getId())
        .sourceType("JOB_BID")
        .sourceId(bidId)
        .title("Contract Backend Dev")
        .agreedRate(new BigDecimal("35000000.00"))
        .salaryType("MONTHLY")
        .build();

    when(userRepository.findById(employerId)).thenReturn(Optional.of(employer));
    when(jobRepository.findById(job.getId())).thenReturn(Optional.of(job));
    when(userRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
    when(bidRepository.findById(bidId)).thenReturn(Optional.of(bid));
    when(contractRepository.save(any(Contract.class))).thenAnswer(inv -> inv.getArgument(0));

    ContractDetailResponse mockResponse = ContractDetailResponse.builder().title("Contract Backend Dev").build();
    when(contractMapper.toDetailResponse(any(), any(), any())).thenReturn(mockResponse);

    ContractDetailResponse response = contractService.createContract(request, null, employerContext);

    assertThat(response.getTitle()).isEqualTo("Contract Backend Dev");
    assertThat(bid.getStatus()).isEqualTo(BidStatus.ACCEPTED);
    verify(offerRepository).save(any(Offer.class));
  }

  @Test
  @DisplayName("Should reject contract update if status is IN_PROGRESS")
  void shouldRejectUpdateWhenActive() {
    Contract contract = Contract.builder()
        .employer(employer)
        .job(job)
        .status(ContractStatus.IN_PROGRESS)
        .build();

    UUID contractId = UUID.randomUUID();
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

    assertThatThrownBy(() -> contractService.updateContract(contractId, null, null, employerContext))
        .isInstanceOf(ContractException.class)
        .hasMessageContaining("chờ duyệt");
  }

  @Test
  @DisplayName("Should reject contract cancellation if already COMPLETED")
  void shouldRejectCancellationWhenCompleted() {
    Contract contract = Contract.builder()
        .employer(employer)
        .job(job)
        .status(ContractStatus.COMPLETED)
        .build();

    UUID contractId = UUID.randomUUID();
    when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));

    CancelContractRequest cancelReq = new CancelContractRequest("Lý do hủy hợp đồng đã đủ dài");
    assertThatThrownBy(() -> contractService.cancelContract(contractId, cancelReq, employerContext))
        .isInstanceOf(ContractException.class);
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ContractServiceTest`
Expected: FAIL compilation with "cannot find symbol ContractService / ContractServiceImpl".

- [ ] **Step 3: Implement ContractService and ContractServiceImpl**

1. Define `ContractService` interface in `src/main/java/com/occupify/core/service/`.
2. Implement `ContractServiceImpl` with `@Transactional`:
   - `createContract`:
     - Resolve `employer` from `userContext.userId()`.
     - Find `job`, verify `job.project.employer.id == employer.id`.
     - Resolve `employee`: from `employeeId` or by `freelancerEmail`.
     - If `sourceType == 'JOB_BID'`: load `bid`, verify `bid.job.id == job.id`, set `bid.setStatus(BidStatus.ACCEPTED)`.
     - If `file != null && !file.isEmpty()`: call `storageService.uploadPrivateDocument("contracts/" + tempId, file)` to set `termsFileKey`; else `termsFileKey = null`.
     - Save `Contract` (`status = PENDING`).
     - Create & save `Offer` (`contract`, `freelancer = employee`, `proposedRate = agreedRate`, `offerMessage = invitationMessage`, `status = PENDING`).
     - Generate presigned download URL if `termsFileKey != null`. Return DTO.
   - `updateContract`:
     - Load contract, verify ownership, verify `status == PENDING`.
     - Update title, rate, salaryType, dates, jd. If file attached, upload & update `termsFileKey`.
   - `getContracts`:
     - Enforce scoping: if not admin, apply specification/filter where `employer.id == userContext.userId()` or `employee.id == userContext.userId()`.
     - Support filtering by `projectId`, `jobId`, `status`. Return paginated summary.
   - `getContractDetail`:
     - Verify user is employer, employee, or admin.
     - Generate presigned GET URL (15m) if `termsFileKey != null`. Return detail DTO.
   - `cancelContract`:
     - Validate cancellation matrix: reject if `COMPLETED` or `CANCELLED`.
     - Save `CancellationContract` (`reason`, `requestedByUser`, `status = ACCEPTED`).
     - Set `contract.status = CANCELLED`.
     - If `offers` exist with `PENDING`, set to `EXPIRED`.
   - `acceptContract`:
     - Verify `userContext.userId() == contract.employee.id`.
     - Set `contract.status = IN_PROGRESS`, `startedAt = LocalDateTime.now()`.
     - Update corresponding `Offer.status = ACCEPTED`.
   - `completeContract`:
     - Verify `userContext.userId() == contract.employer.id`.
     - Set `contract.status = COMPLETED`, `finishedAt = LocalDateTime.now()`.
     - Save `Review` (`rating`, `comment`, `reviewer = employer`, `reviewee = employee`, `contract = contract`).

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=ContractServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/occupify/core/service/ContractService.java \
        src/main/java/com/occupify/core/service/impl/ContractServiceImpl.java \
        src/test/java/com/occupify/core/service/ContractServiceTest.java
git commit -m "feat(contract): implement contract service with invariants, cascades, and storage integration"
```

---

### Task 5: Contract REST Controller & Full Integration Tests

**Files:**
- Create: `src/main/java/com/occupify/core/controller/ContractController.java`
- Create: `src/test/java/com/occupify/core/controller/ContractControllerTest.java`

**Interfaces:**
- Consumes: `ContractService`, `AbstractBaseController`.
- Produces: REST endpoints under `/contracts` consuming `multipart/form-data` and `application/json`.

- [ ] **Step 1: Write the failing MockMvc controller test**

```java
// src/test/java/com/occupify/core/controller/ContractControllerTest.java
package com.occupify.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.occupify.core.constant.RouteConstants;
import com.occupify.core.dto.base.PageResponse;
import com.occupify.core.dto.request.contract.CancelContractRequest;
import com.occupify.core.dto.request.contract.CompleteContractRequest;
import com.occupify.core.dto.request.contract.CreateContractRequest;
import com.occupify.core.dto.response.contract.ContractDetailResponse;
import com.occupify.core.dto.response.contract.ContractSummaryResponse;
import com.occupify.core.enums.ContractStatus;
import com.occupify.core.enums.UserRole;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.ContractService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ContractController.class)
class ContractControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockBean private ContractService contractService;

  @Test
  @DisplayName("POST /contracts should create contract with multipart data")
  void shouldCreateContract() throws Exception {
    CreateContractRequest request = CreateContractRequest.builder()
        .jobId(UUID.randomUUID())
        .title("Hợp đồng Backend Dev")
        .agreedRate(new BigDecimal("35000000.00"))
        .salaryType("MONTHLY")
        .build();

    MockMultipartFile dataPart = new MockMultipartFile(
        "data", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));
    MockMultipartFile filePart = new MockMultipartFile(
        "file", "terms.pdf", MediaType.APPLICATION_PDF_VALUE, "dummy content".getBytes());

    ContractDetailResponse response = ContractDetailResponse.builder()
        .id(UUID.randomUUID())
        .title("Hợp đồng Backend Dev")
        .termsFileUrl("https://signed.url/terms.pdf")
        .status(ContractStatus.PENDING)
        .build();

    when(contractService.createContract(any(), any(), any())).thenReturn(response);

    mockMvc.perform(multipart(RouteConstants.CONTRACTS)
            .file(dataPart)
            .file(filePart))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.title").value("Hợp đồng Backend Dev"))
        .andExpect(jsonPath("$.data.termsFileUrl").value("https://signed.url/terms.pdf"));
  }

  @Test
  @DisplayName("POST /contracts/{id}/cancel should cancel contract")
  void shouldCancelContract() throws Exception {
    UUID contractId = UUID.randomUUID();
    CancelContractRequest cancelReq = new CancelContractRequest("Lý do hủy hợp đồng đã đủ dài");

    ContractDetailResponse response = ContractDetailResponse.builder()
        .id(contractId)
        .status(ContractStatus.CANCELLED)
        .build();

    when(contractService.cancelContract(eq(contractId), any(), any())).thenReturn(response);

    mockMvc.perform(post(RouteConstants.CONTRACTS + "/" + contractId + "/cancel")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(cancelReq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CANCELLED"));
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ContractControllerTest`
Expected: FAIL compilation with "cannot find symbol ContractController".

- [ ] **Step 3: Implement ContractController**

Create `ContractController.java` in `src/main/java/com/occupify/core/controller/`:
- Extends `AbstractBaseController`.
- `@RestController @RequestMapping(RouteConstants.CONTRACTS)`.
- `@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)`:
  - `@RequestPart("data") @Valid CreateContractRequest request`
  - `@RequestPart(value = "file", required = false) MultipartFile file`
  - `@Parameter(hidden = true) @Authenticated UserContext userContext`
  - Returns `created(contractService.createContract(request, file, userContext), MessageConstants.CONTRACT_CREATED)`.
- `@PutMapping(value = RouteConstants.CONTRACT_ID, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)`:
  - `@PathVariable UUID id`, `@RequestPart("data") @Valid UpdateContractRequest request`, `@RequestPart(value = "file", required = false) MultipartFile file`.
  - Returns `successSingle(contractService.updateContract(id, request, file, userContext), MessageConstants.CONTRACT_UPDATED)`.
- `@GetMapping`:
  - Query params via `@ParameterObject ContractFilterRequest filter`, `@PageableDefault Pageable pageable`.
  - Returns `paging(contractService.getContracts(filter, pageable, userContext), MessageConstants.CONTRACT_LIST_RETRIEVED)`.
- `@GetMapping(RouteConstants.CONTRACT_ID)`:
  - Returns `successSingle(contractService.getContractDetail(id, userContext), MessageConstants.CONTRACT_RETRIEVED)`.
- `@PostMapping(RouteConstants.CONTRACT_CANCEL)`:
  - `@Valid @RequestBody CancelContractRequest request`.
  - Returns `successSingle(contractService.cancelContract(id, request, userContext), MessageConstants.CONTRACT_CANCELLED)`.
- `@PostMapping(RouteConstants.CONTRACT_ACCEPT)`:
  - Returns `successSingle(contractService.acceptContract(id, userContext), MessageConstants.CONTRACT_ACCEPTED)`.
- `@PostMapping(RouteConstants.CONTRACT_COMPLETE)`:
  - `@Valid @RequestBody CompleteContractRequest request`.
  - Returns `successSingle(contractService.completeContract(id, request, userContext), MessageConstants.CONTRACT_COMPLETED)`.

- [ ] **Step 4: Run controller test and full test suite to verify everything passes**

Run: `./mvnw clean test`
Expected: All tests pass with 0 failures, 0 errors.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/occupify/core/controller/ContractController.java \
        src/test/java/com/occupify/core/controller/ContractControllerTest.java
git commit -m "feat(contract): implement contract rest controller and integration tests"
```
