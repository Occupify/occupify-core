package com.occupify.core.repository.bid;

import com.occupify.core.entity.bid.Bid;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<Bid, UUID>, JpaSpecificationExecutor<Bid> {

  List<Bid> findByJobId(UUID jobId);

  List<Bid> findByFreelancerId(UUID freelancerId);

  boolean existsByJobIdAndFreelancerId(UUID jobId, UUID freelancerId);

  @EntityGraph(attributePaths = {"job", "job.project", "job.project.employer", "freelancer"})
  Optional<Bid> findByJobIdAndFreelancerId(UUID jobId, UUID freelancerId);

  @EntityGraph(attributePaths = {"job", "job.project", "job.project.employer", "freelancer"})
  @Query("SELECT b FROM Bid b WHERE b.id = :id")
  Optional<Bid> findWithDetailsById(@Param("id") UUID id);

  @Query("SELECT b.job.id, COUNT(b.id) FROM Bid b WHERE b.job.id IN :jobIds GROUP BY b.job.id")
  List<Object[]> countBidsByJobIdIn(@Param("jobIds") Collection<UUID> jobIds);
}
