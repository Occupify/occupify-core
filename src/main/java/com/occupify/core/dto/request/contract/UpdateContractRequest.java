package com.occupify.core.dto.request.contract;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateContractRequest(
    UUID jobId,
    String message,
    @JsonProperty("freelancer_gmail")
    @JsonAlias({"freelancerEmail", "freelancer_email", "freelancerGmail"})
    @Email(message = "Freelancer email must be a valid email format")
    String freelancerGmail) {

  public String freelancerEmail() {
    return freelancerGmail;
  }
}
