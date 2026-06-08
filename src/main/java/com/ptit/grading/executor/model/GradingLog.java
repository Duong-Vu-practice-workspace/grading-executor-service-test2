package com.ptit.grading.executor.model;

import com.ptit.grading.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "grading_logs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GradingLog extends BaseEntity {

    @Column(nullable = false)
    private UUID submissionId;

    @Column(nullable = false)
    private String step;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private String level = "INFO";
}
