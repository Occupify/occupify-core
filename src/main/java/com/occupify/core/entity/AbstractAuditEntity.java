package com.occupify.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@MappedSuperclass
public class AbstractAuditEntity extends AbstractBaseEntity {

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;
}
