package com.occupify.core.repository.project;

import com.occupify.core.entity.project.Project;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProjectRepository
    extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

  @EntityGraph(attributePaths = {"employer"})
  Optional<Project> findWithEmployerById(UUID id);

  @Override
  @EntityGraph(attributePaths = {"employer"})
  Page<Project> findAll(Specification<Project> spec, Pageable pageable);

  List<Project> findByEmployerId(UUID employerId);
}
