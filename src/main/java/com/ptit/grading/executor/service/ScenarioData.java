package com.ptit.grading.executor.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioData {
    private String id;
    private String assignmentId;
    private Integer sequenceOrder;
    private String name;
    private String httpMethod;
    private String endpoint;
    private String queryParams;
    private String requestBody;
    private String expectedResponseBody;
    private Integer expectedStatus;
    private Integer weight;
    private String pathVariables;  // JSON string
}
