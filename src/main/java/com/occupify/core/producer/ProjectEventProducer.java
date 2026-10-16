package com.occupify.core.producer;

import com.occupify.core.event.ProjectClosedEvent;
import com.occupify.core.event.ProjectCreatedEvent;

public interface ProjectEventProducer {

  void publishProjectCreated(ProjectCreatedEvent event);

  void publishProjectClosed(ProjectClosedEvent event);
}
