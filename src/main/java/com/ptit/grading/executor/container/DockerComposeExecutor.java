package com.ptit.grading.executor.container;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DockerComposeExecutor {

    private final Path workDir;
    private final String projectName;
    private final int exposedPort;
    private GenericContainer<?> appContainer;

    public DockerComposeExecutor(Path workDir, String projectName, int exposedPort) {
        this.workDir = workDir;
        this.projectName = projectName;
        this.exposedPort = exposedPort;
    }

    /**
     * Build và start student's Docker Compose project bằng Testcontainers.
     * Each service in docker-compose.yml becomes a container.
     */
    public String start() throws IOException, InterruptedException {
        log.info("Starting Docker Compose for project {} in {}", projectName, workDir);

        // Read docker-compose.yml to find services
        Path composeFile = workDir.resolve("docker-compose.yml");
        if (!Files.exists(composeFile)) {
            composeFile = workDir.resolve("docker-compose.yaml");
        }

        if (!Files.exists(composeFile)) {
            throw new RuntimeException("docker-compose.yml not found in " + workDir);
        }

        // Find Dockerfile in the work directory
        Path dockerfile = workDir.resolve("Dockerfile");
        if (!Files.exists(dockerfile)) {
            throw new RuntimeException("Dockerfile not found in " + workDir);
        }

        // Build image using Testcontainers' ImageFromDockerfile
        String imageTag = "grading/" + projectName + ":latest";

        // Build the Docker image from the student's Dockerfile
        appContainer = new GenericContainer<>(
            new ImageFromDockerfile(imageTag)
                .withDockerfile(dockerfile)
        )
            .withExposedPorts(exposedPort)
            .withLogConsumer(new Slf4jLogConsumer(log))
            .waitingFor(org.testcontainers.containers.wait.Wait.forHttp("/actuator/health")
                .forPort(exposedPort)
                .withStartupTimeout(Duration.ofSeconds(30)));

        appContainer.start();

        String mappedPort = String.valueOf(appContainer.getMappedPort(exposedPort));
        log.info("Container started. Mapped port: {}", mappedPort);

        return mappedPort;
    }

    /**
     * Stop and remove the container
     */
    public void stop() {
        if (appContainer != null && appContainer.isRunning()) {
            appContainer.stop();
            log.info("Container stopped for project {}", projectName);
        }
    }

    public GenericContainer<?> getContainer() {
        return appContainer;
    }

    public int getMappedPort(int exposedPort) {
        return appContainer.getMappedPort(exposedPort);
    }

    public String getContainerId() {
        return appContainer != null ? appContainer.getContainerId() : null;
    }

    private static class Duration {
        private final long seconds;

        private Duration(long seconds) {
            this.seconds = seconds;
        }

        public static Duration ofSeconds(long seconds) {
            return new Duration(seconds);
        }
    }
}
