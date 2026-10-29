package com.occupify.core.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("seed")
public class SeedCatalogLoader implements ApplicationRunner {

  private final NamedParameterJdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final Environment environment;
  private final Resource defaultCatalogResource;

  public SeedCatalogLoader(
      NamedParameterJdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      Environment environment,
      @Value("${app.seed.catalog-path:classpath:seed/sample-catalog.json}")
          Resource defaultCatalogResource) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.environment = environment;
    this.defaultCatalogResource = defaultCatalogResource;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    checkProdProfile();
    loadDefaultCatalog();
  }

  public void checkProdProfile() {
    if (environment.acceptsProfiles(Profiles.of("prod"))) {
      throw new IllegalStateException(
          "Seed profile is strictly forbidden when prod profile is active");
    }
  }

  public void loadDefaultCatalog() {
    checkProdProfile();
    if (defaultCatalogResource != null && defaultCatalogResource.exists()) {
      try (InputStream is = defaultCatalogResource.getInputStream()) {
        List<SeedProject> projects =
            objectMapper.readValue(is, new TypeReference<List<SeedProject>>() {});
        loadCatalog(projects);
      } catch (Exception e) {
        throw new RuntimeException("Failed to load default seed catalog", e);
      }
    } else {
      log.warn("Default seed catalog resource does not exist: {}", defaultCatalogResource);
    }
  }

  public void loadCatalogFromPath(Path path) {
    checkProdProfile();
    try {
      String json = Files.readString(path, StandardCharsets.UTF_8);
      loadCatalogFromJson(json);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Failed to read catalog from path: " + path, e);
    }
  }

  public void loadCatalogFromJson(String json) {
    checkProdProfile();
    try {
      List<SeedProject> projects =
          objectMapper.readValue(json, new TypeReference<List<SeedProject>>() {});
      loadCatalog(projects);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse catalog JSON", e);
    }
  }

  @Transactional
  public void loadCatalog(List<SeedProject> projects) {
    checkProdProfile();
    validateCatalog(projects);
    writeSyntheticFreelancers();
    writeProjects(projects);
  }

  private void validateCatalog(List<SeedProject> projects) {
    for (SeedProject project : projects) {
      try {
        UUID.fromString(project.id());
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid project UUID id: " + project.id(), e);
      }

      if (project.hoursPerDay() != null) {
        if (project.hoursPerDay().compareTo(BigDecimal.valueOf(0.5)) < 0
            || project.hoursPerDay().compareTo(BigDecimal.valueOf(24.0)) > 0) {
          throw new IllegalArgumentException(
              "hoursPerDay must be between 0.5 and 24.0: " + project.hoursPerDay());
        }
      }

      if (project.location() != null && project.location().length() > 120) {
        throw new IllegalArgumentException(
            "location exceeds 120 characters: " + project.location().length());
      }

      if (project.duration() != null && project.duration().length() > 60) {
        throw new IllegalArgumentException(
            "duration exceeds 60 characters: " + project.duration().length());
      }

      if (project.tags() != null) {
        if (project.tags().size() > 10) {
          throw new IllegalArgumentException("tags count exceeds 10: " + project.tags().size());
        }
        for (String tag : project.tags()) {
          if (tag != null && tag.length() > 40) {
            throw new IllegalArgumentException("tag exceeds 40 characters: " + tag);
          }
        }
      }

      if (project.jobs() != null) {
        for (SeedJob job : project.jobs()) {
          try {
            UUID.fromString(job.id());
          } catch (Exception e) {
            throw new IllegalArgumentException("Invalid job UUID id: " + job.id(), e);
          }

          if (job.requirements() != null) {
            if (job.requirements().size() > 20) {
              throw new IllegalArgumentException(
                  "requirements count exceeds 20: " + job.requirements().size());
            }
            for (String req : job.requirements()) {
              if (req != null && req.length() > 300) {
                throw new IllegalArgumentException(
                    "requirement exceeds 300 characters: " + req.length());
              }
            }
          }
        }
      }
    }
  }

  private void writeSyntheticFreelancers() {
    for (int i = 0; i < 20; i++) {
      UUID freelancerId =
          UUID.nameUUIDFromBytes(
              ("occupify-seed-freelancer-" + i).getBytes(StandardCharsets.UTF_8));
      String email = "seed-freelancer-" + i + "@example.com";
      String fullName = "Seed Freelancer " + i;

      MapSqlParameterSource params =
          new MapSqlParameterSource()
              .addValue("id", freelancerId)
              .addValue("email", email)
              .addValue("fullName", fullName);

      jdbcTemplate.update(
          """
          INSERT INTO users (id, email, full_name, role, status, created_at, updated_at)
          VALUES (:id, :email, :fullName, 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          ON CONFLICT (id) DO UPDATE SET
            email = EXCLUDED.email,
            full_name = EXCLUDED.full_name,
            updated_at = CURRENT_TIMESTAMP
          WHERE (users.email, users.full_name) IS DISTINCT FROM (EXCLUDED.email, EXCLUDED.full_name)
          """,
          params);
    }
  }

  private void writeProjects(List<SeedProject> projects) {
    int totalJobs = 0;

    for (int i = 0; i < projects.size(); i++) {
      SeedProject project = projects.get(i);
      int employerIndex = i / 10;
      UUID employerId =
          UUID.nameUUIDFromBytes(
              ("occupify-seed-employer-" + employerIndex).getBytes(StandardCharsets.UTF_8));
      String employerEmail = "seed-employer-" + employerIndex + "@example.com";
      String employerName = "Seed Employer " + employerIndex;

      MapSqlParameterSource empParams =
          new MapSqlParameterSource()
              .addValue("id", employerId)
              .addValue("email", employerEmail)
              .addValue("fullName", employerName);

      jdbcTemplate.update(
          """
          INSERT INTO users (id, email, full_name, role, status, created_at, updated_at)
          VALUES (:id, :email, :fullName, 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          ON CONFLICT (id) DO UPDATE SET
            email = EXCLUDED.email,
            full_name = EXCLUDED.full_name,
            updated_at = CURRENT_TIMESTAMP
          WHERE (users.email, users.full_name) IS DISTINCT FROM (EXCLUDED.email, EXCLUDED.full_name)
          """,
          empParams);

      UUID projectId = UUID.fromString(project.id());
      MapSqlParameterSource projParams =
          new MapSqlParameterSource()
              .addValue("id", projectId)
              .addValue("employerId", employerId)
              .addValue("title", project.title())
              .addValue("description", project.description())
              .addValue("startPrice", project.startPrice())
              .addValue("period", project.period() != null ? project.period().name() : null)
              .addValue("dueDate", project.dueDate())
              .addValue(
                  "experienceLevel",
                  project.experienceLevel() != null ? project.experienceLevel().name() : null)
              .addValue("status", project.status() != null ? project.status().name() : "OPEN")
              .addValue("location", project.location())
              .addValue("hoursPerDay", project.hoursPerDay())
              .addValue("duration", project.duration());

      jdbcTemplate.update(
          """
          INSERT INTO projects (
            id, employer_id, title, description, start_price, period, due_date,
            experience_level, status, location, hours_per_day, duration, version,
            created_at, updated_at
          ) VALUES (
            :id, :employerId, :title, :description, :startPrice, :period, :dueDate,
            :experienceLevel, :status, :location, :hoursPerDay, :duration, 0,
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
          )
          ON CONFLICT (id) DO UPDATE SET
            employer_id = EXCLUDED.employer_id,
            title = EXCLUDED.title,
            description = EXCLUDED.description,
            start_price = EXCLUDED.start_price,
            period = EXCLUDED.period,
            due_date = EXCLUDED.due_date,
            experience_level = EXCLUDED.experience_level,
            status = EXCLUDED.status,
            location = EXCLUDED.location,
            hours_per_day = EXCLUDED.hours_per_day,
            duration = EXCLUDED.duration,
            updated_at = CURRENT_TIMESTAMP
          WHERE (projects.employer_id, projects.title, projects.description, projects.start_price, projects.period, projects.due_date,
                 projects.experience_level, projects.status, projects.location, projects.hours_per_day, projects.duration)
            IS DISTINCT FROM
                (EXCLUDED.employer_id, EXCLUDED.title, EXCLUDED.description, EXCLUDED.start_price, EXCLUDED.period, EXCLUDED.due_date,
                 EXCLUDED.experience_level, EXCLUDED.status, EXCLUDED.location, EXCLUDED.hours_per_day, EXCLUDED.duration)
          """,
          projParams);

      // Project tags
      List<String> existingTags =
          jdbcTemplate.queryForList(
              "SELECT tag FROM project_tag WHERE project_id = :projectId",
              new MapSqlParameterSource("projectId", projectId),
              String.class);
      Set<String> existingTagSet = new HashSet<>(existingTags);
      Set<String> newTagSet = new HashSet<>(project.tags() != null ? project.tags() : List.of());

      if (!existingTagSet.equals(newTagSet)) {
        jdbcTemplate.update(
            "DELETE FROM project_tag WHERE project_id = :projectId",
            new MapSqlParameterSource("projectId", projectId));
        for (String tag : newTagSet) {
          jdbcTemplate.update(
              "INSERT INTO project_tag (project_id, tag) VALUES (:projectId, :tag)",
              new MapSqlParameterSource("projectId", projectId).addValue("tag", tag));
        }
      }

      // Jobs
      if (project.jobs() != null) {
        for (SeedJob job : project.jobs()) {
          totalJobs++;
          UUID jobId = UUID.fromString(job.id());
          MapSqlParameterSource jobParams =
              new MapSqlParameterSource()
                  .addValue("id", jobId)
                  .addValue("projectId", projectId)
                  .addValue("title", job.title())
                  .addValue("description", job.description())
                  .addValue("quantity", job.quantity() != null ? job.quantity() : 1)
                  .addValue("salaryType", job.salaryType() != null ? job.salaryType().name() : null)
                  .addValue("budgetMin", job.budgetMin())
                  .addValue("budgetMax", job.budgetMax())
                  .addValue("status", job.status() != null ? job.status().name() : "RECRUITING");

          jdbcTemplate.update(
              """
              INSERT INTO job (
                id, project_id, title, description, quantity, salary_type,
                budget_min, budget_max, status, version, created_at, updated_at
              ) VALUES (
                :id, :projectId, :title, :description, :quantity, :salaryType,
                :budgetMin, :budgetMax, :status, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
              )
              ON CONFLICT (id) DO UPDATE SET
                project_id = EXCLUDED.project_id,
                title = EXCLUDED.title,
                description = EXCLUDED.description,
                quantity = EXCLUDED.quantity,
                salary_type = EXCLUDED.salary_type,
                budget_min = EXCLUDED.budget_min,
                budget_max = EXCLUDED.budget_max,
                status = EXCLUDED.status,
                updated_at = CURRENT_TIMESTAMP
              WHERE (job.project_id, job.title, job.description, job.quantity, job.salary_type, job.budget_min, job.budget_max, job.status)
                IS DISTINCT FROM
                    (EXCLUDED.project_id, EXCLUDED.title, EXCLUDED.description, EXCLUDED.quantity, EXCLUDED.salary_type, EXCLUDED.budget_min, EXCLUDED.budget_max, EXCLUDED.status)
              """,
              jobParams);

          // Job requirements
          List<String> existingReqs =
              jdbcTemplate.queryForList(
                  "SELECT text FROM job_requirement WHERE job_id = :jobId ORDER BY position",
                  new MapSqlParameterSource("jobId", jobId),
                  String.class);
          List<String> newReqs = job.requirements() != null ? job.requirements() : List.of();

          if (!existingReqs.equals(newReqs)) {
            jdbcTemplate.update(
                "DELETE FROM job_requirement WHERE job_id = :jobId",
                new MapSqlParameterSource("jobId", jobId));
            for (int pos = 0; pos < newReqs.size(); pos++) {
              jdbcTemplate.update(
                  "INSERT INTO job_requirement (job_id, position, text) VALUES (:jobId, :pos, :text)",
                  new MapSqlParameterSource("jobId", jobId)
                      .addValue("pos", pos)
                      .addValue("text", newReqs.get(pos)));
            }
          }

          // Job skills
          if (job.skills() != null && !job.skills().isEmpty()) {
            List<UUID> resolvedSkillIds = new ArrayList<>();
            for (String skillName : job.skills()) {
              if (skillName == null || skillName.isBlank()) continue;
              List<Map<String, Object>> skillRows =
                  jdbcTemplate.queryForList(
                      "SELECT id FROM skills WHERE lower(name) = lower(:skillName)",
                      new MapSqlParameterSource("skillName", skillName.trim()));

              UUID skillId;
              if (!skillRows.isEmpty()) {
                skillId = (UUID) skillRows.get(0).get("id");
              } else {
                skillId =
                    UUID.nameUUIDFromBytes(
                        skillName.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
                jdbcTemplate.update(
                    """
                    INSERT INTO skills (id, name, created_at, updated_at)
                    VALUES (:id, :name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    ON CONFLICT (id) DO NOTHING
                    """,
                    new MapSqlParameterSource("id", skillId).addValue("name", skillName.trim()));
              }
              resolvedSkillIds.add(skillId);
            }

            List<UUID> existingSkillIds =
                jdbcTemplate.queryForList(
                    "SELECT skill_id FROM job_skills WHERE job_id = :jobId",
                    new MapSqlParameterSource("jobId", jobId),
                    UUID.class);
            Set<UUID> existingSkillSet = new HashSet<>(existingSkillIds);
            Set<UUID> newSkillSet = new HashSet<>(resolvedSkillIds);

            if (!existingSkillSet.equals(newSkillSet)) {
              jdbcTemplate.update(
                  "DELETE FROM job_skills WHERE job_id = :jobId",
                  new MapSqlParameterSource("jobId", jobId));
              for (UUID sId : resolvedSkillIds) {
                jdbcTemplate.update(
                    "INSERT INTO job_skills (job_id, skill_id) VALUES (:jobId, :skillId) ON CONFLICT DO NOTHING",
                    new MapSqlParameterSource("jobId", jobId).addValue("skillId", sId));
              }
            }
          }
        }
      }
    }

    log.info(
        "Seeding completed successfully: {} projects, {} jobs seeded", projects.size(), totalJobs);
  }
}
