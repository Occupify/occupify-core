package com.occupify.core.repository.job;

import com.occupify.core.entity.job.Job;
import java.util.List;
import com.occupify.core.enums.JobStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, UUID> {

  @EntityGraph(attributePaths = {"skills"})
  List<Job> findWithSkillsByProjectId(UUID projectId);
  @EntityGraph(attributePaths = {"skills"})
  List<Job> findWithSkillsByProjectIdInAndStatus(List<UUID> projectIds, JobStatus status);


  List<Job> findByProjectId(UUID projectId);
}
