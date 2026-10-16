package com.occupify.core.dto.request;

import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import com.occupify.core.enums.ProjectStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record ProjectFilterRequest(
    String keyword,
    ProjectStatus status,
    UUID employerId,
    ProjectPeriod period,
    ExperienceLevel experienceLevel,
    BigDecimal minPrice,
    BigDecimal maxPrice) {}
