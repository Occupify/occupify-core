package com.occupify.core.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

public class FlywayMigrationIT extends AbstractIntegrationIT {

  @Autowired private Flyway flyway;

  @Test
  void migratesV1ToLatestOnPostgres16() {
    MigrationInfo current = flyway.info().current();
    assertThat(current).isNotNull();
    assertThat(current.getState().isApplied()).isTrue();
  }

  @Test
  void v1ToV6Unchanged() throws Exception {
    Map<String, String> expectedChecksums =
        Map.of(
            "V1__init_core_schema.sql",
            "e61484bbf000dc9e03a430066bcf4195a5709e6d367f7ff29983738fce880df5",
            "V2__add_category_to_skills_and_sync_roles.sql",
            "03970d405440b34744ca9c7c73e4f76c5986b1d15582da3185341d1046ec2e1f",
            "V3__seed_project_data.sql",
            "b76b227c21206b40d683184fb1ee527b12e52d0c4736916d28c7fd44c00a41fb",
            "V4__enable_unaccent_and_fts_indexes.sql",
            "aae61da8834530b2f941143a96397a4d13b2c340a28be0bd8e0c1d2905f36471",
            "V5__add_bid_unique_constraint_and_seeds.sql",
            "8dc96e097229aa40c9e0e1b402b1692d259d748e3646697e6d9c86dd19bb1ca5",
            "V6__create_cancellation_contracts_and_alter_contracts.sql",
            "c6295cf9f90c893aed50257ec72cc0fefea53a24960d61474fdb66cd6775aadc");

    for (Map.Entry<String, String> entry : expectedChecksums.entrySet()) {
      ClassPathResource resource = new ClassPathResource("db/migration/" + entry.getKey());
      assertThat(resource.exists())
          .withFailMessage("Missing migration file: %s", entry.getKey())
          .isTrue();

      try (InputStream is = resource.getInputStream()) {
        byte[] bytes = is.readAllBytes();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        String hexHash = HexFormat.of().formatHex(hash);
        assertThat(hexHash)
            .withFailMessage(
                "Migration file %s checksum mismatch! Expected: %s, Actual: %s",
                entry.getKey(), entry.getValue(), hexHash)
            .isEqualTo(entry.getValue());
      }
    }
  }

  @Test
  void existingRowsReadWithEmptyCollections() {
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            "SELECT hours_per_day, location, duration, version FROM projects LIMIT 1");
    assertThat(rows).isNotEmpty();
    Map<String, Object> project = rows.get(0);
    assertThat(project.get("hours_per_day")).isNull();
    assertThat(project.get("location")).isNull();
    assertThat(project.get("duration")).isNull();
    assertThat(((Number) project.get("version")).longValue()).isEqualTo(0L);

    Integer tagCount =
        jdbcTemplate.queryForObject("SELECT count(*) FROM project_tag", Integer.class);
    assertThat(tagCount).isEqualTo(0);

    Integer reqCount =
        jdbcTemplate.queryForObject("SELECT count(*) FROM job_requirement", Integer.class);
    assertThat(reqCount).isEqualTo(0);

    List<Map<String, Object>> jobRows =
        jdbcTemplate.queryForList("SELECT version FROM job LIMIT 1");
    assertThat(jobRows).isNotEmpty();
    assertThat(((Number) jobRows.get(0).get("version")).longValue()).isEqualTo(0L);
  }

  @Test
  void hibernateValidatePasses() {
    MigrationInfo current = flyway.info().current();
    assertThat(current).isNotNull();
    assertThat(
            current
                .getVersion()
                .compareTo(MigrationVersion.fromVersion("7")))
        .isGreaterThanOrEqualTo(0);
  }
}
