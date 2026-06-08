package com.ptit.grading.executor.service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResponseComparator {

    private final Gson gson;

    /**
     * Compare expected response body with actual response body.
     * Checks structure (keys) rather than exact values.
     * Returns true if they match.
     */
    public boolean matches(String expectedJson, String actualJson) {
        if (expectedJson == null || expectedJson.isBlank()) {
            return true;  // No expected body = skip check
        }
        if (actualJson == null || actualJson.isBlank()) {
            return false;
        }

        try {
            Object expected = gson.fromJson(expectedJson, Object.class);
            Object actual = gson.fromJson(actualJson, Object.class);
            return deepEquals(expected, actual);
        } catch (Exception e) {
            log.warn("Failed to parse JSON for comparison", e);
            return expectedJson.trim().equals(actualJson.trim());
        }
    }

    @SuppressWarnings("unchecked")
    private boolean deepEquals(Object expected, Object actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;

        if (expected instanceof Map && actual instanceof Map) {
            Map<String, Object> expMap = (Map<String, Object>) expected;
            Map<String, Object> actMap = (Map<String, Object>) actual;

            // Check same keys
            if (!expMap.keySet().equals(actMap.keySet())) {
                return false;
            }

            // Recursively check values
            return expMap.keySet().stream()
                .allMatch(k -> deepEquals(expMap.get(k), actMap.get(k)));
        }

        if (expected instanceof java.util.List && actual instanceof java.util.List) {
            java.util.List<Object> expList = (java.util.List<Object>) expected;
            java.util.List<Object> actList = (java.util.List<Object>) actual;
            if (expList.size() != actList.size()) return false;
            for (int i = 0; i < expList.size(); i++) {
                if (!deepEquals(expList.get(i), actList.get(i))) return false;
            }
            return true;
        }

        if (expected instanceof Number && actual instanceof Number) {
            return ((Number) expected).doubleValue() == ((Number) actual).doubleValue();
        }

        return expected.getClass().equals(actual.getClass());
    }
}
