package com.occupify.core.repository.contract;

import com.occupify.core.entity.contract.CancellationContract;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationContractRepository extends JpaRepository<CancellationContract, UUID> {

  List<CancellationContract> findByContractIdOrderByCreatedAtDesc(UUID contractId);

  Optional<CancellationContract> findFirstByContractIdOrderByCreatedAtDesc(UUID contractId);
}
