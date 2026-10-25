package com.occupify.core.constant;

public final class RouteConstants {

  private RouteConstants() {}

  public static final String PROJECTS = "/projects";
  public static final String PROJECT_ID = "/{id}";
  public static final String PROJECT_CLOSE = "/{id}/close";

  public static final String CONTRACTS = "/contracts";
  public static final String CONTRACT_ID = "/{id}";
  public static final String CONTRACT_CANCEL = "/{id}/cancel";
  public static final String CONTRACT_ACCEPT = "/{id}/accept";
  public static final String CONTRACT_COMPLETE = "/{id}/complete";
}
