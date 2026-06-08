package com.ptit.grading.executor.service;

import com.ptit.grading.common.client.FeignClientFactory;
import com.ptit.grading.common.client.SubmissionClient;
import feign.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmissionClientFactory {

    private final FeignClientFactory feignClientFactory;

    public SubmissionClient create(int port) {
        String baseUrl = "http://localhost:" + port;
        return feignClientFactory.createClient(SubmissionClient.class, baseUrl);
    }

    public Response executeGet(int port, String path, Map<String, Object> queryParams) throws Exception {
        SubmissionClient client = create(port);
        return client.get(path, queryParams);
    }

    public Response executePost(int port, String path, Object body) throws Exception {
        SubmissionClient client = create(port);
        return client.post(path, body);
    }
}
