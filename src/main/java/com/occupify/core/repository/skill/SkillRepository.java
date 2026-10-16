package com.occupify.core.repository.skill;

import com.occupify.core.entity.skill.Skill;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
  Optional<Skill> findByName(String name);
}
