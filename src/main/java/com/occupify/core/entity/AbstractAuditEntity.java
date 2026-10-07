package com.occupify.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public class AbstractAuditEntity extends AbstractBaseEntity {

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
