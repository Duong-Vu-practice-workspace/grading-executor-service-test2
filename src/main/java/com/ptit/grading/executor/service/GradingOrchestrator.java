package com.ptit.grading.executor.service;

import com.ptit.grading.common.client.FeignClientFactory;
import com.ptit.grading.common.dto.GradingJob;
import com.ptit.grading.executor.container.DockerComposeExecutor;
import com.ptit.grading.executor.model.GradingLog;
import com.ptit.grading.executor.repository.GradingLogRepository;
import com.google.gson.Gson;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradingOrchestrator {

    private final DockerComposeExecutor dockerExecutor;
    private final TestExecutor testExecutor;
    private final SubmissionClientFactory submissionClientFactory;
    private final GradingLogRepository logRepository;
    private final Gson gson;

    @Value("${grading.temp-dir:/tmp/grading}")
    private String tempDir;

    @Value("${grading.container.startup-timeout-seconds:60}")
    private int startupTimeoutSeconds;

    public void execute(GradingJob job) {
        String submissionId = job.getSubmissionId().toString();
        Path workDir = Paths.get(tempDir, submissionId);
        String projectName = "sub-" + submissionId;

        try {
            // 1. Create working directory
            Files.createDirectories(workDir);
            log("INFO", submissionId, "Created working directory");

            // 2. Download zip from MinIO (skip for now - assume already extracted)
            // For now, assume the zip is already extracted in workDir
            log("INFO", submissionId, "Working in: " + workDir);

            // 3. Find Dockerfile
            Path dockerfile = workDir.resolve("Dockerfile");
            if (!Files.exists(dockerfile)) {
                fail(job, "Dockerfile not found");
                return;
            }

            // 4. Build and start container using Testcontainers
            log("INFO", submissionId, "Building and starting container...");
            DockerComposeExecutor executor = new DockerComposeExecutor(
                workDir, projectName, 8080);
            executor.start();

            // 5. Wait for container to be ready
            log("INFO", submissionId, "Waiting for container to be ready...");
            Thread.sleep(5000);  // Simple wait - in production use health check

            // 6. Get mapped port
            int port = executor.getMappedPort(8080);
            log("INFO", submissionId, "Container ready on port " + port);

            // 7. Fetch scenarios from Assignment Service (via HTTP call)
            // For now, use mock scenarios from the job
            List<ScenarioData> scenarios = fetchScenarios(job.getAssignmentId());

            // 8. Execute each scenario
            List<ScenarioResult> results = new ArrayList<>();
            for (ScenarioData scenario : scenarios) {
                ScenarioResult result = testExecutor.execute(port, scenario, submissionClientFactory);
                results.add(result);
                log("INFO", submissionId,
                    "Scenario '{}': {} (status={})",
                    scenario.getName(),
                    result.isPassed() ? "PASSED" : "FAILED",
                    result.getActualStatus());
            }

            // 9. Calculate score
            int totalWeight = scenarios.stream().mapToInt(s -> s.getWeight()).sum();
            int earnedWeight = results.stream()
                .filter(ScenarioResult::isPassed)
                .mapToInt(ScenarioResult::getWeight)
                .sum();

            double score = totalWeight > 0
                ? Math.round((double) earnedWeight / totalWeight * 100.0) / 10.0
                : 0;

            // 10. Save results (call Result Service)
            log("INFO", submissionId,
                "Grading completed: {}/{} scenarios, Score: {}/10",
                earnedWeight, totalWeight, score);

            // 11. Cleanup
            executor.stop();

            log("INFO", submissionId, "Grading finished successfully");

        } catch (Exception e) {
            log.error("Grading failed for submission {}", submissionId, e);
            fail(job, "Error: " + e.getMessage());
        }
    }

    private List<ScenarioData> fetchScenarios(UUID assignmentId) {
        // TODO: Call Assignment Service to get scenarios
        // For now, return empty list
        log.warn("fetchScenarios not implemented - returning empty list");
        return List.of();
    }

    private void fail(GradingJob job, String reason) {
        log.error("Grading failed for submission {}: {}", job.getSubmissionId(), reason);
        log("ERROR", job.getSubmissionId().toString(), reason);

        // Save failure result
        GradingLog logEntry = GradingLog.builder()
            .submissionId(job.getSubmissionId())
            .step("orchestrator")
            .message(reason)
            .level("ERROR")
            .build();
        logRepository.save(logEntry);
    }

    private void log(String level, String submissionId, String message, Object... args) {
        GradingLog logEntry = GradingLog.builder()
            .submissionId(UUID.fromString(submissionId))
            .step("orchestrator")
            .message(String.format(message, args))
            .level(level)
            .build();
        logRepository.save(logEntry);
    }
}
