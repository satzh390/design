package com.example.jobqueue.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.jobqueue.dtos.Job;
import com.example.jobqueue.services.JobService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/jobs")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<com.example.jobqueue.model.Job> createJob(@Valid @RequestBody Job job) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.createJob(job));
    }

    @GetMapping
    public List<com.example.jobqueue.model.Job> getAllJobs() {
        return jobService.getAllJobs();
    }

    @PatchMapping("/{id}/cancel")
    public com.example.jobqueue.model.Job cancelJob(@PathVariable UUID id) {
        return jobService.cancelJob(id);
    }
}