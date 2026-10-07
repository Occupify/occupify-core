package com.occupify.core.repository;

import com.occupify.core.entity.Contract;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, UUID> {
  List<Contract> findByJobId(UUID jobId);

  List<Contract> findByFreelancerId(UUID freelancerId);
}
