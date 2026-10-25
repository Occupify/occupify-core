package com.occupify.core.repository.contract;

import com.occupify.core.entity.contract.Contract;
import com.occupify.core.enums.ContractStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ContractRepository
    extends JpaRepository<Contract, UUID>, JpaSpecificationExecutor<Contract> {
  List<Contract> findByJobId(UUID jobId);

  List<Contract> findByEmployerId(UUID employerId);

  List<Contract> findByEmployeeId(UUID employeeId);

  Page<Contract> findByEmployerId(UUID employerId, Pageable pageable);

  Page<Contract> findByEmployeeId(UUID employeeId, Pageable pageable);

  Page<Contract> findByJobId(UUID jobId, Pageable pageable);

  Page<Contract> findByStatus(ContractStatus status, Pageable pageable);

  Page<Contract> findByEmployerIdAndStatus(
      UUID employerId, ContractStatus status, Pageable pageable);

  Page<Contract> findByEmployeeIdAndStatus(
      UUID employeeId, ContractStatus status, Pageable pageable);
}
