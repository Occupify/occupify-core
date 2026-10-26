package com.occupify.core.repository.contract;

import com.occupify.core.entity.contract.Contract;
import com.occupify.core.enums.ContractStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, UUID> {
  List<Contract> findByJobId(UUID jobId);

  List<Contract> findByFreelancerId(UUID freelancerId);

  List<Contract> findByStatusAndUpdatedAtBefore(ContractStatus status, LocalDateTime threshold);

  @Query(
      "SELECT c FROM Contract c "
          + "LEFT JOIN FETCH c.job j "
          + "LEFT JOIN FETCH j.project p "
          + "LEFT JOIN FETCH p.employer "
          + "LEFT JOIN FETCH c.freelancer "
          + "WHERE c.id = :id")
  Optional<Contract> findByIdWithDetails(@Param("id") UUID id);
}
