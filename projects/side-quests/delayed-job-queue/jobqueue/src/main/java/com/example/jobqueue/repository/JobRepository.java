package com.example.jobqueue.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.jobqueue.model.Job;

public interface JobRepository extends JpaRepository<Job, UUID> {
    
}
