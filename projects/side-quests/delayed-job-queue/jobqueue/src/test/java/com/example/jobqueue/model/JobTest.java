package com.example.jobqueue.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class JobTest {
    @Test
    void fromDtoCopiesRequestFieldsAndInitializesJob() {
        Instant scheduled = Instant.parse("2026-10-22T12:00:00Z");
        com.example.jobqueue.dtos.Job request = new com.example.jobqueue.dtos.Job(
                "daily-report",
                JobType.CMD,
                "generate-report",
                true,
                scheduled,
                300);

        Job job = Job.fromDto(request);

        assertNotNull(job.getId());
        assertEquals(request.name(), job.getName());
        assertEquals(request.type(), job.getType());
        assertEquals(request.script(), job.getScript());
        assertEquals(request.retry(), job.isRetry());
        assertEquals(request.scheduled(), job.getScheduled());
        assertEquals(request.timeoutInseconds(), job.getTimeoutInseconds());
        assertEquals(JobStatus.SCHEDULED, job.getStatus());
        assertNotNull(job.getCreatedAt());
        assertNotNull(job.getUpdatedAt());
        assertEquals(job.getCreatedAt(), job.getUpdatedAt());
    }
}
