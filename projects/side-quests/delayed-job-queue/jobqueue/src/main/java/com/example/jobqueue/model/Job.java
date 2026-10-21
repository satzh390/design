package com.example.jobqueue.model;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Data;

@Data 
@Entity 
public class Job {
    @Id 
    private UUID id;
    private String name;
    @Enumerated(EnumType.STRING)
    private JobType type;
    private String script;
    private boolean retry;
    private Instant scheduled;
    private long timeoutInseconds;
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    private String assingedWorker;
    @CreationTimestamp 
    private Instant createdAt;
    @UpdateTimestamp 
    private Instant updatedAt;
    private Instant completedAt;
    private Instant failedAt;
    private int retryCount;
    private String errorMessage;
    private String errorStackTrace;
    private Instant leasedTill;

    public static Job fromDto(com.example.jobqueue.dtos.Job request) {
        Instant now = Instant.now();

        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setName(request.name());
        job.setType(request.type());
        job.setScript(request.script());
        job.setRetry(request.retry());
        job.setScheduled(request.scheduled());
        job.setTimeoutInseconds(request.timeoutInseconds());
        job.setStatus(JobStatus.SCHEDULED);
        job.setCreatedAt(now);
        job.setUpdatedAt(now);

        return job;
    }
}
