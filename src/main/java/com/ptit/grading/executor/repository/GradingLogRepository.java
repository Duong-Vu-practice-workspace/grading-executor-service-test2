package com.ptit.grading.executor.repository;

import com.ptit.grading.executor.model.GradingLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GradingLogRepository extends JpaRepository<GradingLog, UUID> {
}
