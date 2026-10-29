package com.occupify.core.repository.contract;

import com.occupify.core.entity.contract.Contract;
import com.occupify.core.enums.ContractStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository
    extends JpaRepository<Contract, UUID>, JpaSpecificationExecutor<Contract> {
  List<Contract> findByJobId(UUID jobId);

  long countByJobIdAndStatusIn(UUID jobId, Collection<ContractStatus> statuses);

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

  List<Contract> findByStatusAndUpdatedAtBefore(ContractStatus status, LocalDateTime threshold);

  @Query(
      "SELECT c FROM Contract c "
          + "LEFT JOIN FETCH c.job j "
          + "LEFT JOIN FETCH c.employer e "
          + "LEFT JOIN FETCH c.employee emp "
          + "WHERE c.id = :id")
  Optional<Contract> findByIdWithDetails(@Param("id") UUID id);
}
