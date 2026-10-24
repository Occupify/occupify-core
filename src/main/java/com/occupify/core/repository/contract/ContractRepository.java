package com.occupify.core.repository.contract;

import com.occupify.core.entity.contract.Contract;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, UUID> {
  List<Contract> findByJobId(UUID jobId);

  List<Contract> findByEmployerId(UUID employerId);

  List<Contract> findByEmployeeId(UUID employeeId);
}
