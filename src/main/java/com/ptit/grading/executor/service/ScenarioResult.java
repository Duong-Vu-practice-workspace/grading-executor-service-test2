package com.ptit.grading.executor.service;

import com.ptit.grading.common.dto.GradingJob;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScenarioResult {
    private String scenarioId;
    private String scenarioName;
    private boolean passed;
    private int actualStatus;
    private String actualBody;
    private String errorMessage;
    private int weight;
}
