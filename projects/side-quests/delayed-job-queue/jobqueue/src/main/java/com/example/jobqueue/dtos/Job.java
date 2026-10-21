package com.example.jobqueue.dtos;

import java.time.Instant;

import com.example.jobqueue.model.JobType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Job(
    @NotBlank String name,
    @NotNull JobType type,
    @NotBlank String script,
    boolean retry,
    @NotNull Instant scheduled,
    @Positive long timeoutInseconds
) {

}
