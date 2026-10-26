package com.occupify.core.service.impl;

import com.occupify.core.constant.StorageConstants;
import com.occupify.core.dto.request.BidFilterRequest;
import com.occupify.core.dto.request.CreateBidRequest;
import com.occupify.core.dto.request.UpdateBidRequest;
import com.occupify.core.dto.response.BidResponse;
import com.occupify.core.entity.bid.Bid;
import com.occupify.core.entity.job.Job;
import com.occupify.core.entity.project.Project;
import com.occupify.core.entity.user.User;
import com.occupify.core.enums.BidStatus;
import com.occupify.core.enums.JobStatus;
import com.occupify.core.enums.ProjectStatus;
import com.occupify.core.event.BidCancelledEvent;
import com.occupify.core.event.BidCreatedEvent;
import com.occupify.core.exception.bid.BidErrorCode;
import com.occupify.core.exception.bid.BidException;
import com.occupify.core.exception.system.SystemErrorCode;
import com.occupify.core.exception.system.SystemException;
import com.occupify.core.mapper.BidMapper;
import com.occupify.core.producer.BidEventProducer;
import com.occupify.core.repository.bid.BidRepository;
import com.occupify.core.repository.bid.BidSpecification;
import com.occupify.core.repository.job.JobRepository;
import com.occupify.core.repository.project.ProjectRepository;
import com.occupify.core.repository.user.UserRepository;
import com.occupify.core.security.UserContext;
import com.occupify.core.service.BidService;
import com.occupify.core.service.StorageService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BidServiceImpl implements BidService {

  private final BidRepository bidRepository;
  private final JobRepository jobRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final BidMapper bidMapper;
  private final BidEventProducer bidEventProducer;
  private final StorageService storageService;

  @Override
  public BidResponse createBid(CreateBidRequest request, UserContext userContext) {
    log.info(
        "[BidService] User [{}] is submitting bid for job [{}]",
        userContext.userId(),
        request.jobId());

    if (!userContext.isUser()) {
      throw new SystemException(
          SystemErrorCode.FORBIDDEN, "Only users are permitted to submit bids");
    }

    User freelancer =
        userRepository
            .findById(userContext.userId())
            .orElseThrow(
                () ->
                    new BidException(
                        BidErrorCode.FREELANCER_NOT_FOUND,
                        "Freelancer not found with ID: " + userContext.userId()));

    Job job =
        jobRepository
            .findWithProjectById(request.jobId())
            .orElseThrow(
                () ->
                    new BidException(
                        BidErrorCode.JOB_NOT_FOUND, "Job not found with ID: " + request.jobId()));

    if (job.getStatus() != JobStatus.RECRUITING) {
      throw new BidException(
          BidErrorCode.JOB_NOT_RECRUITING,
          "Job " + request.jobId() + " is not recruiting (status: " + job.getStatus() + ")");
    }

    Project project = job.getProject();
    if (project.getStatus() != ProjectStatus.OPEN) {
      throw new BidException(
          BidErrorCode.PROJECT_NOT_OPEN, "Project " + project.getId() + " is closed");
    }

    if (project.getDueDate() != null && LocalDate.now().isAfter(project.getDueDate())) {
      throw new BidException(
          BidErrorCode.PROJECT_NOT_OPEN,
          "Project deadline " + project.getDueDate() + " has passed");
    }

    if (project.getEmployer().getId().equals(userContext.userId())) {
      throw new BidException(
          BidErrorCode.CANNOT_BID_OWN_PROJECT, "You cannot submit a bid to your own project");
    }

    if (bidRepository.existsByJobIdAndFreelancerId(request.jobId(), userContext.userId())) {
      throw new BidException(
          BidErrorCode.DUPLICATE_BID, "You have already submitted a proposal for this job");
    }

    Bid bid = bidMapper.toEntity(request);
    bid.setJob(job);
    bid.setFreelancer(freelancer);
    bid.setStatus(BidStatus.PENDING);

    if (request.proposedSalaryType() == null) {
      bid.setProposedSalaryType(job.getSalaryType());
    }

    String uploadedCvUrl = null;
    if (request.attachmentCvUrl() != null && !request.attachmentCvUrl().isEmpty()) {
      uploadedCvUrl =
          storageService.uploadDocument(
              StorageConstants.getResumePath(userContext.userId()), request.attachmentCvUrl());
      bid.setAttachmentCvUrl(uploadedCvUrl);
    }

    Bid saved;
    try {
      saved = bidRepository.save(bid);
    } catch (Exception ex) {
      if (uploadedCvUrl != null) {
        try {
          storageService.deleteFile(uploadedCvUrl);
        } catch (Exception deleteEx) {
          log.error(
              "[BidService] Failed to cleanup uploaded CV [{}] after DB save failure",
              uploadedCvUrl,
              deleteEx);
        }
      }
      throw ex;
    }

    bidEventProducer.publishBidCreated(
        new BidCreatedEvent(
            saved.getId(),
            job.getId(),
            job.getTitle(),
            project.getId(),
            project.getTitle(),
            project.getEmployer().getId(),
            freelancer.getId(),
            freelancer.getFullName(),
            saved.getProposedRate(),
            saved.getCreatedAt()));

    return bidMapper.toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BidResponse> getMyBids(
      BidFilterRequest filter, Pageable pageable, UserContext userContext) {
    log.info("[BidService] Retrieving bids submitted by user [{}]", userContext.userId());
    Specification<Bid> spec = BidSpecification.withFilters(filter, userContext.userId());
    Page<Bid> page = bidRepository.findAll(spec, pageable);
    return page.map(bidMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<BidResponse> getBids(
      BidFilterRequest filter, Pageable pageable, UserContext userContext) {
    log.info(
        "[BidService] Querying bids with filter: {} by user [{}]", filter, userContext.userId());

    if (filter != null && filter.projectId() != null) {
      Project project =
          projectRepository
              .findById(filter.projectId())
              .orElseThrow(
                  () ->
                      new BidException(
                          BidErrorCode.PROJECT_NOT_OPEN,
                          "Project not found with ID: " + filter.projectId()));
      if (!project.getEmployer().getId().equals(userContext.userId()) && !userContext.isAdmin()) {
        throw new BidException(
            BidErrorCode.FORBIDDEN_OPERATION,
            "You do not have permission to view bids for project " + filter.projectId());
      }
    }

    if (filter != null && filter.jobId() != null) {
      Job job =
          jobRepository
              .findById(filter.jobId())
              .orElseThrow(
                  () ->
                      new BidException(
                          BidErrorCode.JOB_NOT_FOUND, "Job not found with ID: " + filter.jobId()));
      if (!job.getProject().getEmployer().getId().equals(userContext.userId())
          && !userContext.isAdmin()) {
        throw new BidException(
            BidErrorCode.FORBIDDEN_OPERATION,
            "You do not have permission to view bids for job " + filter.jobId());
      }
    }

    UUID defaultFreelancerId =
        (filter != null && (filter.projectId() != null || filter.jobId() != null))
            ? null
            : userContext.userId();

    Specification<Bid> spec = BidSpecification.withFilters(filter, defaultFreelancerId);
    Page<Bid> page = bidRepository.findAll(spec, pageable);
    return page.map(bidMapper::toResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public BidResponse getBidDetail(UUID id, UserContext userContext) {
    log.info("[BidService] Retrieving bid [{}] by user [{}]", id, userContext.userId());
    Bid bid =
        bidRepository
            .findWithDetailsById(id)
            .orElseThrow(
                () -> new BidException(BidErrorCode.BID_NOT_FOUND, "Bid not found with ID: " + id));

    boolean isApplicant = bid.getFreelancer().getId().equals(userContext.userId());
    boolean isEmployer =
        bid.getJob().getProject().getEmployer().getId().equals(userContext.userId());
    boolean isAdmin = userContext.isAdmin();

    if (!isApplicant && !isEmployer && !isAdmin) {
      throw new BidException(
          BidErrorCode.FORBIDDEN_OPERATION, "You do not have permission to access bid " + id);
    }

    return bidMapper.toResponse(bid);
  }

  @Override
  @Transactional(readOnly = true)
  public BidResponse getMyBidForJob(UUID jobId, UserContext userContext) {
    log.info(
        "[BidService] Retrieving user [{}] proposal for job [{}]", userContext.userId(), jobId);
    Bid bid =
        bidRepository
            .findByJobIdAndFreelancerId(jobId, userContext.userId())
            .orElseThrow(
                () ->
                    new BidException(
                        BidErrorCode.BID_NOT_FOUND,
                        "No bid proposal found for job " + jobId + " by current user"));

    return bidMapper.toResponse(bid);
  }

  @Override
  public BidResponse updateBid(UUID id, UpdateBidRequest request, UserContext userContext) {
    log.info("[BidService] Updating bid [{}] by user [{}]", id, userContext.userId());
    Bid bid =
        bidRepository
            .findWithDetailsById(id)
            .orElseThrow(
                () -> new BidException(BidErrorCode.BID_NOT_FOUND, "Bid not found with ID: " + id));

    if (!bid.getFreelancer().getId().equals(userContext.userId())) {
      throw new BidException(
          BidErrorCode.FORBIDDEN_OPERATION,
          "Only the applicant is permitted to update this bid proposal");
    }

    if (bid.getStatus() != BidStatus.PENDING) {
      throw new BidException(
          BidErrorCode.BID_NOT_PENDING,
          "Only pending bids can be updated (current status: " + bid.getStatus() + ")");
    }

    if (bid.getJob().getProject().getStatus() != ProjectStatus.OPEN) {
      throw new BidException(
          BidErrorCode.PROJECT_NOT_OPEN, "Cannot update bid because the parent project is closed");
    }

    String oldCvUrl = bid.getAttachmentCvUrl();
    String newCvUrl = null;
    boolean hasNewFile = request.attachmentCvUrl() != null && !request.attachmentCvUrl().isEmpty();
    boolean shouldRemoveFile = Boolean.TRUE.equals(request.removeAttachmentCv());

    if (hasNewFile) {
      newCvUrl =
          storageService.uploadDocument(
              StorageConstants.getResumePath(userContext.userId()), request.attachmentCvUrl());
      bid.setAttachmentCvUrl(newCvUrl);
    } else if (shouldRemoveFile) {
      bid.setAttachmentCvUrl(null);
    }

    bidMapper.updateEntity(bid, request);

    Bid updated;
    try {
      updated = bidRepository.save(bid);
    } catch (Exception ex) {
      if (newCvUrl != null) {
        try {
          storageService.deleteFile(newCvUrl);
        } catch (Exception deleteEx) {
          log.error(
              "[BidService] Failed to cleanup newly uploaded CV [{}] after DB update failure",
              newCvUrl,
              deleteEx);
        }
      }
      throw ex;
    }

    if ((hasNewFile || shouldRemoveFile) && oldCvUrl != null && !oldCvUrl.isBlank()) {
      try {
        storageService.deleteFile(oldCvUrl);
      } catch (Exception ex) {
        log.warn("[BidService] Failed to delete obsolete CV [{}] from storage", oldCvUrl, ex);
      }
    }

    return bidMapper.toResponse(updated);
  }

  @Override
  public void cancelBid(UUID id, UserContext userContext) {
    log.info("[BidService] Cancelling bid [{}] by user [{}]", id, userContext.userId());
    Bid bid =
        bidRepository
            .findWithDetailsById(id)
            .orElseThrow(
                () -> new BidException(BidErrorCode.BID_NOT_FOUND, "Bid not found with ID: " + id));

    if (!bid.getFreelancer().getId().equals(userContext.userId()) && !userContext.isAdmin()) {
      throw new BidException(
          BidErrorCode.FORBIDDEN_OPERATION, "Only the applicant is permitted to cancel this bid");
    }

    if (bid.getStatus() != BidStatus.PENDING) {
      throw new BidException(
          BidErrorCode.BID_NOT_PENDING,
          "Only pending bids can be cancelled (current status: " + bid.getStatus() + ")");
    }

    String cvUrl = bid.getAttachmentCvUrl();

    bidRepository.delete(bid);

    bidEventProducer.publishBidCancelled(
        new BidCancelledEvent(
            bid.getId(),
            bid.getJob().getId(),
            bid.getJob().getProject().getId(),
            bid.getJob().getProject().getEmployer().getId(),
            bid.getFreelancer().getId(),
            LocalDateTime.now()));

    if (cvUrl != null && !cvUrl.isBlank()) {
      try {
        storageService.deleteFile(cvUrl);
      } catch (Exception ex) {
        log.warn("[BidService] Failed to delete CV [{}] after bid cancellation", cvUrl, ex);
      }
    }
  }
}
