package com.example.springproxybeans;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringProxyBeansApplicationIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();
    private static final Logger log = LoggerFactory.getLogger(SpringProxyBeansApplicationIntegrationTest.class);

    @Test
    public void testRequestIsolation() {
        log.info("Starting testRequestIsolation");
        String baseUrl = "http://localhost:" + port + "/api/data";

        log.info("Sending GET request (should be empty)");
        ResponseEntity<String> response1 = restTemplate.getForEntity(baseUrl, String.class);
        log.info("GET response: {}", response1.getBody());
        assertThat(response1.getBody()).isEqualTo("No data set for current request");

        log.info("Sending POST request with data 'test-data-123'");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"data\":\"test-data-123\"}", headers);
        ResponseEntity<String> response2 = restTemplate.postForEntity(baseUrl, request, String.class);
        log.info("POST response: {}", response2.getBody());
        assertThat(response2.getBody()).isEqualTo("Data set for current request: test-data-123");

        log.info("Sending GET request again (should be empty due to isolation)");
        ResponseEntity<String> response3 = restTemplate.getForEntity(baseUrl, String.class);
        log.info("GET response: {}", response3.getBody());
        assertThat(response3.getBody()).isEqualTo("No data set for current request");
        log.info("Finished testRequestIsolation");
    }

    @Test
    public void testConcurrentRequestIsolation() throws Exception {
        log.info("Starting testConcurrentRequestIsolation");
        String baseUrl = "http://localhost:" + port + "/api/data";
        int numRequests = 100;
        ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(numRequests);
        java.util.List<java.util.concurrent.Future<String>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < numRequests; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    String payload = String.format("{\"data\":\"test-data-%d\"}", idx);
                    HttpEntity<String> request = new HttpEntity<>(payload, headers);
                    ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);
                    log.info("Request {} response: {}", idx, response.getBody());
                    return response.getBody();
                } finally {
                    latch.countDown();
                }
            }));
        }
        latch.await();
        executor.shutdown();

        for (int i = 0; i < numRequests; i++) {
            String expected = String.format("Data set for current request: test-data-%d", i);
            String actual = futures.get(i).get();
            log.info("Asserting response for request {}: expected='{}', actual='{}'", i, expected, actual);
            assertThat(actual).isEqualTo(expected);
        }
        log.info("Finished testConcurrentRequestIsolation");
    }
}