package com.occupify.core.dto.request;

import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CreateProjectRequest(
    @NotBlank(message = "Project title cannot be blank")
        @Size(max = 255, message = "Project title cannot exceed 255 characters")
        String title,
    @Size(max = 5000, message = "Project description cannot exceed 5000 characters")
        String description,
    @DecimalMin(value = "0.0", inclusive = false, message = "Start price must be greater than 0")
        BigDecimal startPrice,
    @NotNull(message = "Project period is required") ProjectPeriod period,
    @FutureOrPresent(message = "Due date must be in the present or future") LocalDate dueDate,
    ExperienceLevel experienceLevel,
    LocalDateTime startDate,
    LocalDateTime endDate) {}
