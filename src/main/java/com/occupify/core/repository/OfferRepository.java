package com.occupify.core.repository;

import com.occupify.core.entity.Offer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, UUID> {
  List<Offer> findByContractId(UUID contractId);

  List<Offer> findByFreelancerId(UUID freelancerId);
}
