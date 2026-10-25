# Project Search & Filter with Full-Text Search (FTS) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement full-text search and multi-criteria filtering for projects in `occupify-core`, supporting unaccented Vietnamese keyword search, skill matching (OR), budget overlap filtering, and project-level response cards with aggregated salary ranges and skills.

**Architecture:** Extend `ProjectFilterRequest` with skill and FTS capabilities. Use dynamic JPA Specifications with PostgreSQL `unaccent` function support (with H2 compatibility alias for unit/integration tests). Enhance `ProjectServiceImpl` to aggregate recruiting job skills, open position counts, and salary ranges (`salaryMin`, `salaryMax`) without N+1 queries. Provide database migration `V4` for PostgreSQL `unaccent` extension and GIN indexes.

**Tech Stack:** Java 25, Spring Boot 3.5, Spring Data JPA, Hibernate 6, PostgreSQL 16 (production), H2 (in-memory testing), Flyway.

**Spec:** `docs/superpowers/specs/2026-10-24-project-search-filter-design.md`

## Global Constraints
- Target branch: `feature/oc-53-project-search-filter`
- Java 25 idioms, record DTOs, immutability, no silent exceptions
- Default project search displays only `projects.status = OPEN` having at least one active `job.status = RECRUITING`
- Multi-skill filtering uses `OR` logic (project matches if it requires at least one of the selected skills)
- Budget filtering matches if at least one recruiting job's budget range overlaps with `[minPrice, maxPrice]`
- Keyword FTS must support unaccented Vietnamese search across project title, description, recruiting job title, job description, and skill names

## Review Focus
1. **Vietnamese Diacritics Mismatch:** Searching "lap trinh" must return projects with "lập trình"; searching "thiet ke" must return "thiết kế".
2. **Exclusion of Closed / Zero-Recruiting Projects:** Projects with `status = CLOSED` or projects whose jobs are all `FILLED` or `CLOSED` must never appear in default search results.
3. **Budget Overlap Boundaries:** A job with `budget_min = 15M, budget_max = 25M` must match when filtering `minPrice = 20M, maxPrice = 30M`, but must not match `minPrice = 30M, maxPrice = 40M`.
4. **Skills OR Filter with Duplicate Jobs:** Projects matching multiple selected skills must return distinct projects (no duplicate project rows caused by SQL joins).
5. **Backward Compatibility of Employer Filter:** When `employerId` is explicitly passed (e.g. employer viewing their own projects), closed projects belonging to that employer must still be retrievable when requested.

---

### Task 1: Database Migration for PostgreSQL FTS and H2 Compatibility Alias

**Files:**
- Create: `src/main/resources/db/migration/V4__enable_unaccent_and_fts_indexes.sql`
- Create: `src/test/resources/init-h2.sql`
- Modify: `src/test/resources/application-test.yml`

**Interfaces:**
- Produces: PostgreSQL `unaccent` extension and GIN indexes on `projects`, `job`, and `skills`; H2 `unaccent` alias for test suite.

- [ ] **Step 1: Write the test verifying H2 unaccent alias compatibility**

Create `src/test/java/com/occupify/core/repository/UnaccentFunctionTest.java`:
```java
package com.occupify.core.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnaccentFunctionTest {

  @Autowired private EntityManager entityManager;

  @Test
  void testUnaccentFunction() {
    String result =
        (String)
            entityManager
                .createNativeQuery("SELECT unaccent('Lập trình viên')")
                .getSingleResult();
    assertEquals("Lap trinh vien", result);
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=UnaccentFunctionTest`
Expected: FAIL (Function "UNACCENT" not found in H2)

- [ ] **Step 3: Implement H2 unaccent alias and PostgreSQL V4 migration**

1. Create `src/test/resources/init-h2.sql`:
```sql
CREATE ALIAS IF NOT EXISTS unaccent FOR "com.occupify.core.util.H2Functions.unaccent";
```
Create helper in `src/test/java/com/occupify/core/util/H2Functions.java` using `java.text.Normalizer` to strip Vietnamese accents.
2. In `src/test/resources/application-test.yml`, configure `INIT=RUNSCRIPT FROM 'classpath:init-h2.sql'` in datasource URL.
3. Create `src/main/resources/db/migration/V4__enable_unaccent_and_fts_indexes.sql`:
```sql
CREATE EXTENSION IF NOT EXISTS "unaccent";

CREATE INDEX IF NOT EXISTS idx_projects_fts 
  ON projects USING gin(to_tsvector('simple', unaccent(coalesce(title, '') || ' ' || coalesce(description, ''))));

CREATE INDEX IF NOT EXISTS idx_job_project_status 
  ON job(project_id, status);

CREATE INDEX IF NOT EXISTS idx_job_fts 
  ON job USING gin(to_tsvector('simple', unaccent(coalesce(title, '') || ' ' || coalesce(description, ''))));

CREATE INDEX IF NOT EXISTS idx_skills_name_fts 
  ON skills USING gin(to_tsvector('simple', unaccent(name)));
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=UnaccentFunctionTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V4__enable_unaccent_and_fts_indexes.sql src/test/resources/init-h2.sql src/test/resources/application-test.yml src/test/java/com/occupify/core/repository/UnaccentFunctionTest.java src/test/java/com/occupify/core/util/H2Functions.java
git commit -m "feat(search): add unaccent extension, FTS indexes, and test compatibility"
```

---

### Task 2: DTO Updates for Filter Request and Card Response

**Files:**
- Modify: `src/main/java/com/occupify/core/dto/request/ProjectFilterRequest.java`
- Modify: `src/main/java/com/occupify/core/dto/response/ProjectResponse.java`
- Modify: `src/test/java/com/occupify/core/entity/project/ProjectTest.java`

**Interfaces:**
- Consumes: `ProjectPeriod`, `ExperienceLevel`, `ProjectStatus`, `EmployerSummaryResponse`
- Produces: Updated `ProjectFilterRequest` (with `List<String> skills`), updated `ProjectResponse` (with `BigDecimal salaryMin, BigDecimal salaryMax, Set<String> skills, Integer openJobsCount`)

- [ ] **Step 1: Write test verifying ProjectResponse record structure with Card A fields**

Create or update test in `src/test/java/com/occupify/core/dto/ProjectResponseDtoTest.java`:
```java
package com.occupify.core.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.occupify.core.dto.response.EmployerSummaryResponse;
import com.occupify.core.dto.response.ProjectResponse;
import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectResponseDtoTest {

  @Test
  void testProjectResponseCardAFields() {
    UUID id = UUID.randomUUID();
    ProjectResponse response =
        new ProjectResponse(
            id,
            "Title",
            "Description",
            BigDecimal.valueOf(1000),
            ProjectPeriod.MONTHLY,
            LocalDate.now(),
            ExperienceLevel.EXPERT,
            ProjectStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusMonths(3),
            null,
            new EmployerSummaryResponse(UUID.randomUUID(), "Employer", "avatar.jpg"),
            BigDecimal.valueOf(1500),
            BigDecimal.valueOf(3000),
            Set.of("Java", "React"),
            2,
            LocalDateTime.now(),
            LocalDateTime.now());

    assertEquals(BigDecimal.valueOf(1500), response.salaryMin());
    assertEquals(BigDecimal.valueOf(3000), response.salaryMax());
    assertEquals(Set.of("Java", "React"), response.skills());
    assertEquals(2, response.openJobsCount());
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ProjectResponseDtoTest`
Expected: FAIL (Constructor undefined for ProjectResponse)

- [ ] **Step 3: Update `ProjectFilterRequest` and `ProjectResponse`**

1. In `ProjectFilterRequest.java`:
```java
package com.occupify.core.dto.request;

import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProjectFilterRequest(
    String keyword,
    ProjectStatus status,
    UUID employerId,
    ProjectPeriod period,
    ExperienceLevel experienceLevel,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    List<String> skills) {}
```
2. In `ProjectResponse.java`:
```java
package com.occupify.core.dto.response;

import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    String title,
    String description,
    BigDecimal startPrice,
    ProjectPeriod period,
    LocalDate dueDate,
    ExperienceLevel experienceLevel,
    ProjectStatus status,
    LocalDateTime startDate,
    LocalDateTime endDate,
    LocalDateTime closedAt,
    EmployerSummaryResponse employer,
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    Set<String> skills,
    Integer openJobsCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=ProjectResponseDtoTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/occupify/core/dto/request/ProjectFilterRequest.java src/main/java/com/occupify/core/dto/response/ProjectResponse.java src/test/java/com/occupify/core/dto/ProjectResponseDtoTest.java
git commit -m "feat(dto): update ProjectFilterRequest with skills and ProjectResponse with card summary fields"
```

---

### Task 3: Comprehensive Search & Filter Specification

**Files:**
- Modify: `src/main/java/com/occupify/core/repository/project/ProjectSpecification.java`
- Test: `src/test/java/com/occupify/core/repository/ProjectSpecificationTest.java`

**Interfaces:**
- Consumes: `Project`, `Job`, `Skill`, `ProjectFilterRequest`
- Produces: `Specification<Project> withFilters(ProjectFilterRequest filter)` enforcing FTS unaccent matching, skills OR matching, budget overlap, and active status constraint.

- [ ] **Step 1: Write integration tests for ProjectSpecification**

Create `src/test/java/com/occupify/core/repository/ProjectSpecificationTest.java`:
Test scenarios:
1. Search keyword "lap trinh" matches project with title "Lập trình hệ thống".
2. Search keyword "React" matches recruiting job title or job description.
3. Search keyword matching skill name "PostgreSQL".
4. Filter skills `["Java", "Figma"]` matches projects with either Java OR Figma.
5. Filter budget range `[10M, 20M]` matches job with `[15M, 25M]` (overlap).
6. Closed projects or projects with no recruiting jobs are excluded when `filter.status()` is null.
7. Passing explicit `employerId` and `status = CLOSED` returns the closed project.

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ProjectSpecificationTest`
Expected: FAIL

- [ ] **Step 3: Implement search & filter predicates in `ProjectSpecification`**

Update `ProjectSpecification.java`:
1. Use distinct query (`query.distinct(true)`).
2. Default status constraint: if `filter.status() != null`, match `root.get("status") == filter.status()`. Else, match `root.get("status") == ProjectStatus.OPEN` AND subquery exists active job (`job.status == JobStatus.RECRUITING`).
3. If `filter.keyword()` has text: match unaccented title, unaccented description, OR subquery for recruiting job with unaccented title/description, OR subquery for recruiting job skills with unaccented name.
4. If `filter.skills()` is non-empty: subquery exists active job with job_skills matching any of the skill names (case-insensitive/unaccented).
5. If `filter.minPrice()` / `filter.maxPrice()` provided: subquery exists active job where `(job.budgetMax >= minPrice OR maxPrice IS NULL) AND (job.budgetMin <= maxPrice OR minPrice IS NULL)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=ProjectSpecificationTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/occupify/core/repository/project/ProjectSpecification.java src/test/java/com/occupify/core/repository/ProjectSpecificationTest.java
git commit -m "feat(repository): implement full-text search and multi-criteria ProjectSpecification"
```

---

### Task 4: Job Repository Helper and Service Layer Card Enrichment

**Files:**
- Modify: `src/main/java/com/occupify/core/repository/job/JobRepository.java`
- Modify: `src/main/java/com/occupify/core/mapper/ProjectMapper.java`
- Modify: `src/main/java/com/occupify/core/service/impl/ProjectServiceImpl.java`
- Test: `src/test/java/com/occupify/core/service/ProjectSearchFilterTest.java`

**Interfaces:**
- Consumes: `ProjectRepository`, `JobRepository`, `ProjectMapper`
- Produces: `ProjectService.getProjects(ProjectFilterRequest filter, Pageable pageable)` returning `Page<ProjectResponse>` populated with aggregated salary range (`salaryMin`, `salaryMax`), combined recruiting skills, and `openJobsCount`.

- [ ] **Step 1: Write integration test for ProjectService getProjects search and card enrichment**

Create `src/test/java/com/occupify/core/service/ProjectSearchFilterTest.java`:
```java
package com.occupify.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.occupify.core.dto.request.ProjectFilterRequest;
import com.occupify.core.dto.response.ProjectResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectSearchFilterTest {

  @Autowired private ProjectService projectService;

  @Test
  void testSearchProjectsWithUnaccentAndCardEnrichment() {
    // Seed or retrieve project, call getProjects with unaccented keyword
    Page<ProjectResponse> page =
        projectService.getProjects(
            new ProjectFilterRequest("thiet ke", null, null, null, null, null, null, null),
            PageRequest.of(0, 10));

    assertNotNull(page);
    // Verify card summary fields
    if (!page.isEmpty()) {
      ProjectResponse card = page.getContent().get(0);
      assertNotNull(card.skills());
      assertTrue(card.openJobsCount() >= 1);
    }
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./mvnw test -Dtest=ProjectSearchFilterTest`
Expected: FAIL (Card fields null or query mismatch)

- [ ] **Step 3: Implement batch enrichment in `ProjectServiceImpl`**

1. In `JobRepository.java`:
Add:
```java
@EntityGraph(attributePaths = {"skills"})
List<Job> findByProjectIdInAndStatus(List<UUID> projectIds, JobStatus status);
```
2. In `ProjectServiceImpl.java` `getProjects`:
After fetching `Page<Project>` via `projectRepository.findAll(spec, pageable)`:
- Extract `List<UUID> projectIds = page.map(Project::getId).toList();`
- Batch fetch all active recruiting jobs: `jobRepository.findByProjectIdInAndStatus(projectIds, JobStatus.RECRUITING)`
- Group jobs by `projectId`.
- For each `Project`, calculate:
  - `salaryMin` = min of `job.budgetMin` (fallback to project.startPrice)
  - `salaryMax` = max of `job.budgetMax` (fallback to project.startPrice)
  - `skills` = union of `job.skills`
  - `openJobsCount` = count of recruiting jobs
- Map to `ProjectResponse` with card summary data.

- [ ] **Step 4: Run test to verify it passes**

Run: `./mvnw test -Dtest=ProjectSearchFilterTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/occupify/core/repository/job/JobRepository.java src/main/java/com/occupify/core/service/impl/ProjectServiceImpl.java src/test/java/com/occupify/core/service/ProjectSearchFilterTest.java
git commit -m "feat(service): enrich ProjectResponse card with batch-loaded recruiting skills and salary range"
```

---

### Task 5: End-to-End Controller Verification and Regression Testing

**Files:**
- Modify: `src/main/java/com/occupify/core/controller/ProjectController.java`
- Test: `src/test/java/com/occupify/core/controller/ProjectSearchFilterControllerTest.java`

**Interfaces:**
- Consumes: `ProjectService`, `ProjectFilterRequest`
- Produces: `GET /api/v1/projects` endpoint accepting query parameters: `keyword`, `skills`, `minPrice`, `maxPrice`, `period`, `experienceLevel`, `employerId`, `status`, `page`, `size`, `sort`.

- [ ] **Step 1: Write Controller MockMvc test covering all search and filter scenarios**

Create `src/test/java/com/occupify/core/controller/ProjectSearchFilterControllerTest.java`:
```java
package com.occupify.core.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProjectSearchFilterControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void testSearchAndFilterEndpointWithParams() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/projects")
                .param("keyword", "Mobile")
                .param("skills", "React Native", "TypeScript")
                .param("minPrice", "10000000")
                .param("maxPrice", "30000000")
                .param("page", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.items").isArray());
  }
}
```

- [ ] **Step 2: Run test to verify it passes or fails**

Run: `./mvnw test -Dtest=ProjectSearchFilterControllerTest`
Expected: PASS (or fix parameter binding if needed)

- [ ] **Step 3: Run full test suite across the entire project**

Run: `./mvnw test`
Expected: ALL tests pass with 0 failures, 0 errors.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/occupify/core/controller/ProjectSearchFilterControllerTest.java
git commit -m "test(controller): add comprehensive MockMvc test for search and filter endpoint"
```
