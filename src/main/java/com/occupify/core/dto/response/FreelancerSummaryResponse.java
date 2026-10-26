package com.occupify.core.dto.response;

import java.util.UUID;

public record FreelancerSummaryResponse(UUID id, String email, String fullName, String avatarUrl) {}
