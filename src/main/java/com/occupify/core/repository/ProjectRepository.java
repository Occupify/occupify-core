package com.occupify.core.repository;

import com.occupify.core.entity.Project;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
  List<Project> findByEmployerId(UUID employerId);
}
