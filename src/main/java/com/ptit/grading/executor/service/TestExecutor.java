package com.ptit.grading.executor.service;

import com.ptit.grading.common.client.SubmissionClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import feign.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestExecutor {

    private final Gson gson;
    private final ResponseComparator responseComparator;

    public ScenarioResult execute(int port, ScenarioData scenario, SubmissionClientFactory clientFactory) {
        // Resolve path variables
        String resolvedPath = resolvePath(scenario.getEndpoint(), scenario.getPathVariables());
        if (!resolvedPath.startsWith("/")) {
            resolvedPath = "/" + resolvedPath;
        }

        // Parse query params
        Map<String, Object> queryParams = Map.of();
        if (scenario.getQueryParams() != null && !scenario.getQueryParams().isBlank()) {
            queryParams = gson.fromJson(scenario.getQueryParams(),
                new TypeToken<Map<String, Object>>(){}.getType());
        }

        // Parse request body
        Object requestBody = null;
        if (scenario.getRequestBody() != null && !scenario.getRequestBody().isBlank()) {
            requestBody = gson.fromJson(scenario.getRequestBody(), Object.class);
        }

        try {
            Response response;
            SubmissionClient client = clientFactory.create(port);

            response = switch (scenario.getHttpMethod().toUpperCase()) {
                case "GET" -> client.get(resolvedPath, queryParams);
                case "POST" -> client.post(resolvedPath, requestBody);
                case "PUT" -> client.put(resolvedPath, requestBody);
                case "DELETE" -> client.delete(resolvedPath);
                case "PATCH" -> client.patch(resolvedPath, requestBody);
                default -> throw new IllegalArgumentException(
                    "Unsupported method: " + scenario.getHttpMethod());
            };

            int actualStatus = response.status();
            String actualBody = "";
            if (response.body() != null) {
                actualBody = new String(
                    response.body().asInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
                );
            }

            boolean statusMatch = actualStatus == scenario.getExpectedStatus();
            boolean bodyMatch = responseComparator.matches(
                scenario.getExpectedResponseBody(), actualBody);

            boolean passed = statusMatch && bodyMatch;

            return ScenarioResult.builder()
                .scenarioId(scenario.getId())
                .scenarioName(scenario.getName())
                .passed(passed)
                .actualStatus(actualStatus)
                .actualBody(actualBody)
                .errorMessage(buildError(scenario, actualStatus, statusMatch, bodyMatch))
                .weight(scenario.getWeight())
                .build();

        } catch (Exception e) {
            log.error("Failed to execute scenario: {}", scenario.getName(), e);
            return ScenarioResult.builder()
                .scenarioId(scenario.getId())
                .scenarioName(scenario.getName())
                .passed(false)
                .actualStatus(0)
                .errorMessage("Exception: " + e.getMessage())
                .weight(scenario.getWeight())
                .build();
        }
    }

    private String resolvePath(String endpoint, Object pathVariables) {
        String path = endpoint;
        if (pathVariables == null) return path;

        // Parse pathVariables from JSON to list
        if (pathVariables instanceof String pvJson && !pvJson.isBlank()) {
            var pvList = gson.fromJson(pvJson,
                new TypeToken<Map<String, String>[]>(){}.getType());
            for (Map<String, String> pv : pvList) {
                String name = pv.get("name");
                String placeholder = "{" + name + "}";
                if (path.contains(placeholder)) {
                    path = path.replace(placeholder, generateValue(pv.get("type")));
                }
            }
        }

        // Fallback: replace any remaining {xxx}
        path = path.replaceAll("\\{[^}]+\\}", UUID.randomUUID().toString());
        return path;
    }

    private String generateValue(String type) {
        if (type == null) return "test-value";
        return switch (type.toUpperCase()) {
            case "UUID" -> UUID.randomUUID().toString();
            case "INTEGER", "LONG", "INT" -> "1";
            case "STRING" -> "test";
            default -> "test-value";
        };
    }

    private String buildError(ScenarioData scenario, int actualStatus,
                               boolean statusMatch, boolean bodyMatch) {
        if (statusMatch && bodyMatch) return null;
        StringBuilder sb = new StringBuilder();
        if (!statusMatch) {
            sb.append("Expected status ")
              .append(scenario.getExpectedStatus())
              .append(", got ")
              .append(actualStatus);
        }
        if (!statusMatch && !bodyMatch) sb.append("; ");
        if (!bodyMatch) {
            sb.append("Response body does not match expected structure");
        }
        return sb.toString();
    }
}
