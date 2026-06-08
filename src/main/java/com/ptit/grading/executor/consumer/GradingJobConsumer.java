package com.ptit.grading.executor.consumer;

import com.ptit.grading.common.dto.GradingJob;
import com.ptit.grading.executor.service.GradingOrchestrator;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GradingJobConsumer {

    private final GradingOrchestrator orchestrator;
    private final Gson gson;

    @KafkaListener(
        topics = "${kafka.topic.grading-jobs:grading-jobs}",
        groupId = "${spring.kafka.consumer.group-id:grading-group}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        log.info("Received grading job: key={}, partition={}, offset={}",
            record.key(), record.partition(), record.offset());

        try {
            GradingJob job = gson.fromJson(record.value(), GradingJob.class);
            orchestrator.execute(job);
        } catch (Exception e) {
            log.error("Failed to process grading job", e);
        }
    }
}
