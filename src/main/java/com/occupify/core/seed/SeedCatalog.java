package com.occupify.core.seed;

import java.util.List;

public record SeedCatalog(List<SeedProject> projects) {
  public SeedCatalog {
    if (projects == null) {
      projects = List.of();
    }
  }
}
