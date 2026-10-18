package com.occupify.core.entity.skill;

import com.occupify.core.entity.base.AbstractBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "skills")
public class Skill extends AbstractBaseEntity {

  @Column(name = "name", nullable = false, unique = true, length = 100)
  private String name;

  @Column(name = "category", length = 50)
  private @Nullable String category;
}
