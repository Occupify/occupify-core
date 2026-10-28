package com.occupify.core.repository.job;

import com.occupify.core.entity.job.Job;
import com.occupify.core.enums.JobStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, UUID> {

  @EntityGraph(attributePaths = {"skills"})
  List<Job> findWithSkillsByProjectId(UUID projectId);

  @EntityGraph(attributePaths = {"skills"})
  List<Job> findWithSkillsByProjectIdIn(List<UUID> projectIds);

  @EntityGraph(attributePaths = {"skills"})
  List<Job> findWithSkillsByProjectIdInAndStatus(List<UUID> projectIds, JobStatus status);

  List<Job> findByProjectId(UUID projectId);

  List<Job> findByProjectIdIn(List<UUID> projectIds);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT j FROM Job j WHERE j.id = :id")
  Optional<Job> findForUpdateById(@Param("id") UUID id);

  List<Job> findByProjectIdAndDeletedAtIsNull(UUID projectId);

  Page<Job> findByProjectIdAndDeletedAtIsNull(UUID projectId, Pageable pageable);

  Optional<Job> findByIdAndDeletedAtIsNull(UUID id);

  @EntityGraph(attributePaths = {"project", "project.employer", "skills"})
  Optional<Job> findWithProjectById(UUID id);

  @EntityGraph(attributePaths = {"project", "project.employer", "skills"})
  Optional<Job> findWithProjectByIdAndDeletedAtIsNull(UUID id);
}
