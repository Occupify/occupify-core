package com.occupify.core.service;

import java.util.UUID;

public interface JobLifecycleService {

  void recompute(UUID jobId);

  void closeProjectJobs(UUID projectId);
}
