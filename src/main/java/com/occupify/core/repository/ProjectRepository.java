package com.occupify.core.repository;

import com.occupify.core.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByEmployerId(UUID employerId);
}
