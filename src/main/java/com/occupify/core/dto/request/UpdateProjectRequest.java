package com.occupify.core.dto.request;

import com.occupify.core.enums.ExperienceLevel;
import com.occupify.core.enums.ProjectPeriod;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

public record UpdateProjectRequest(
    @Size(max = 255, message = "Project title cannot exceed 255 characters") String title,
    @Size(max = 5000, message = "Project description cannot exceed 5000 characters")
        String description,
    @DecimalMin(value = "0.0", inclusive = false, message = "Start price must be greater than 0")
        BigDecimal startPrice,
    ProjectPeriod period,
    @FutureOrPresent(message = "Due date must be in the present or future") LocalDate dueDate,
    ExperienceLevel experienceLevel,
    LocalDateTime startDate,
    LocalDateTime endDate,
    @DecimalMin(value = "0.0", inclusive = false, message = "Hours per day must be greater than 0")
        @DecimalMax(value = "24.0", inclusive = true, message = "Hours per day cannot exceed 24")
        BigDecimal hoursPerDay,
    @Size(max = 120, message = "Location cannot exceed 120 characters") String location,
    @Size(max = 60, message = "Duration cannot exceed 60 characters") String duration,
    @Size(max = 10, message = "Cannot exceed 10 tags")
        @UniqueElements(message = "Tags must be unique")
        List<@NotBlank @Size(max = 40, message = "Tag cannot exceed 40 characters") String> tags) {}
