package com.occupify.core.repository.outbox;

import com.occupify.core.entity.outbox.OutboxEvent;
import com.occupify.core.entity.outbox.OutboxStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
  List<OutboxEvent> findTop50ByStatusAndRetryCountLessThanOrderByCreatedAtAsc(
      OutboxStatus status, int maxRetryCount);
}
