package com.occupify.core.constant;

public final class RouteConstants {

  private RouteConstants() {}

  public static final String PROJECTS = "/projects";
  public static final String PROJECT_ID = "/{id}";
  public static final String PROJECT_CLOSE = "/{id}/close";

  public static final String JOBS = "/jobs";
  public static final String JOB_ID = "/{id}";
  public static final String JOB_CLOSE = "/{id}/close";

  public static final String CONTRACTS = "/contracts";
  public static final String CONTRACT_ID = "/{id}";
  public static final String CONTRACT_CANCEL = "/{id}/cancel";
  public static final String CONTRACT_ACCEPT = "/{id}/accept";
  public static final String CONTRACT_REJECT = "/{id}/reject";
  public static final String CONTRACT_COMPLETE = "/{id}/complete";
  public static final String BIDS = "/bids";
  public static final String BID_ID = "/{id}";
  public static final String BIDS_MY = "/my";
  public static final String BID_JOB = "/job/{jobId}";

  public static final String REVIEWS = "/reviews";
  public static final String REVIEW_CONTRACT = "/contract/{contractId}";
  public static final String REVIEW_USER = "/user/{userId}";
  public static final String REVIEW_MY = "/my";
}
