package com.ptit.grading.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan({"com.ptit.grading.common.model", "com.ptit.grading.executor.model"})
public class GradingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradingServiceApplication.class, args);
    }
}
