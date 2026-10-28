package com.occupify.core.seed;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

public class SeedProfileGuardTest {

  @Test
  void refusesWithProd() {
    MockEnvironment env = new MockEnvironment();
    env.setActiveProfiles("seed", "prod");

    SeedCatalogLoader loader = new SeedCatalogLoader(null, null, env, null);
    assertThatThrownBy(loader::checkProdProfile)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Seed profile is strictly forbidden when prod profile is active");
  }
}
