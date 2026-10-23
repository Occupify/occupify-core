package com.occupify.core.repository.job;

import com.occupify.core.entity.job.Job;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, UUID> {

  @EntityGraph(attributePaths = {"skills"})
  List<Job> findWithSkillsByProjectId(UUID projectId);

  List<Job> findByProjectId(UUID projectId);

  @EntityGraph(attributePaths = {"project", "project.employer"})
  Optional<Job> findWithProjectById(UUID id);
}
