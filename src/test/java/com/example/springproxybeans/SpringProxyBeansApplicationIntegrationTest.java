package com.example.springproxybeans;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringProxyBeansApplicationIntegrationTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    @Test
    public void testRequestIsolation() {
        String baseUrl = "http://localhost:" + port + "/api/data";

        // First request: GET data (should be empty)
        ResponseEntity<String> response1 = restTemplate.getForEntity(baseUrl, String.class);
        assertThat(response1.getBody()).isEqualTo("No data set for current request");

        // Second request: POST data
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>("{\"data\":\"test-data-123\"}", headers);
        ResponseEntity<String> response2 = restTemplate.postForEntity(baseUrl, request, String.class);
        assertThat(response2.getBody()).isEqualTo("Data set for current request: test-data-123");

        // Third request: GET data (should be empty again due to request isolation)
        ResponseEntity<String> response3 = restTemplate.getForEntity(baseUrl, String.class);
        assertThat(response3.getBody()).isEqualTo("No data set for current request");
    }

    @Test
    public void testConcurrentRequestIsolation() throws Exception {
        String baseUrl = "http://localhost:" + port + "/api/data";
        int numberOfConcurrentRequests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentRequests);
        
        try {
            // Create a list to store all the futures
            List<CompletableFuture<String>> futures = new ArrayList<>();
            
            // Launch concurrent requests - each should set different data and get it back
            for (int i = 0; i < numberOfConcurrentRequests; i++) {
                final int requestId = i;
                final String testData = "test-data-" + requestId;
                
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        // First, POST data to the request-scoped bean
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<String> postRequest = new HttpEntity<>("{\"data\":\"" + testData + "\"}", headers);
                        
                        TestRestTemplate localRestTemplate = new TestRestTemplate();
                        ResponseEntity<String> postResponse = localRestTemplate.postForEntity(baseUrl, postRequest, String.class);
                        
                        // Verify POST response
                        String expectedPostResponse = "Data set for current request: " + testData;
                        if (!expectedPostResponse.equals(postResponse.getBody())) {
                            return "POST_FAILED_" + requestId + ": expected=" + expectedPostResponse + ", actual=" + postResponse.getBody();
                        }
                        
                        // Then immediately GET the data in a separate request (which should show no data due to request isolation)
                        ResponseEntity<String> getResponse = localRestTemplate.getForEntity(baseUrl, String.class);
                        
                        // Each GET request should show "No data set for current request" 
                        // because it's a separate HTTP request with its own request-scoped bean instance
                        if (!"No data set for current request".equals(getResponse.getBody())) {
                            return "GET_ISOLATION_FAILED_" + requestId + ": " + getResponse.getBody();
                        }
                        
                        return "SUCCESS_" + requestId;
                        
                    } catch (Exception e) {
                        return "EXCEPTION_" + requestId + ": " + e.getMessage();
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // Wait for all requests to complete
            CompletableFuture<Void> allRequests = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            allRequests.get(30, TimeUnit.SECONDS);
            
            // Verify results
            Set<String> results = new HashSet<>();
            for (CompletableFuture<String> future : futures) {
                String result = future.get();
                results.add(result);
                
                // Each request should succeed
                assertThat(result).startsWith("SUCCESS_");
            }
            
            // Should have exactly numberOfConcurrentRequests different success results
            assertThat(results).hasSize(numberOfConcurrentRequests);
            
            // Verify that we get all different request IDs
            for (int i = 0; i < numberOfConcurrentRequests; i++) {
                assertThat(results).contains("SUCCESS_" + i);
            }
            
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void testHighConcurrencyRequestIsolation() throws Exception {
        String baseUrl = "http://localhost:" + port + "/api/data";
        int numberOfConcurrentRequests = 50; // Higher concurrency test
        ExecutorService executor = Executors.newFixedThreadPool(20); // More threads for higher concurrency
        
        try {
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            
            // Launch many concurrent requests to stress test the request scoping
            for (int i = 0; i < numberOfConcurrentRequests; i++) {
                final int requestId = i;
                final String testData = "concurrent-test-" + requestId;
                
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        TestRestTemplate localRestTemplate = new TestRestTemplate();
                        
                        // POST data
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<String> postRequest = new HttpEntity<>("{\"data\":\"" + testData + "\"}", headers);
                        
                        ResponseEntity<String> postResponse = localRestTemplate.postForEntity(baseUrl, postRequest, String.class);
                        String expectedPostResponse = "Data set for current request: " + testData;
                        
                        // Verify POST worked correctly
                        boolean postSuccess = expectedPostResponse.equals(postResponse.getBody());
                        
                        // GET in separate request (should be isolated)
                        ResponseEntity<String> getResponse = localRestTemplate.getForEntity(baseUrl, String.class);
                        boolean getSuccess = "No data set for current request".equals(getResponse.getBody());
                        
                        return postSuccess && getSuccess;
                        
                    } catch (Exception e) {
                        return false;
                    }
                }, executor);
                
                futures.add(future);
            }
            
            // Wait for all requests to complete
            CompletableFuture<Void> allRequests = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            
            allRequests.get(60, TimeUnit.SECONDS); // Give more time for higher concurrency
            
            // Verify all requests succeeded
            int successCount = 0;
            for (CompletableFuture<Boolean> future : futures) {
                if (future.get()) {
                    successCount++;
                }
            }
            
            // All requests should have succeeded, demonstrating proper request isolation under high concurrency
            assertThat(successCount).isEqualTo(numberOfConcurrentRequests);
            
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void testConcurrentRequestTimestampIsolation() throws Exception {
        String baseUrl = "http://localhost:" + port + "/api/data";
        int numberOfConcurrentRequests = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfConcurrentRequests);
        
        try {
            List<CompletableFuture<String>> timestampFutures = new ArrayList<>();
            
            // Launch concurrent requests with staggered timing
            for (int i = 0; i < numberOfConcurrentRequests; i++) {
                final int requestId = i;
                final String testData = "timestamp-test-" + requestId;
                
                // Add a small delay before each request to ensure different timestamps
                Thread.sleep(50); // 50ms delay between starting each concurrent request
                
                CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        TestRestTemplate localRestTemplate = new TestRestTemplate();
                        
                        // POST data 
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        HttpEntity<String> postRequest = new HttpEntity<>("{\"data\":\"" + testData + "\"}", headers);
                        
                        // Set data - this should create a request-scoped bean with timestamp
                        ResponseEntity<String> postResponse = localRestTemplate.postForEntity(baseUrl, postRequest, String.class);
                        
                        // Verify the POST was successful
                        String expectedResponse = "Data set for current request: " + testData;
                        assertThat(postResponse.getBody()).isEqualTo(expectedResponse);
                        
                        // Return success with request ID to show this completed
                        return "TIMESTAMP_SUCCESS_" + requestId;
                        
                    } catch (Exception e) {
                        return "TIMESTAMP_FAILED_" + requestId + ": " + e.getMessage();
                    }
                }, executor);
                
                timestampFutures.add(future);
            }
            
            // Wait for all requests to complete
            CompletableFuture<Void> allRequests = CompletableFuture.allOf(
                timestampFutures.toArray(new CompletableFuture[0])
            );
            
            allRequests.get(30, TimeUnit.SECONDS);
            
            // Verify all requests succeeded
            Set<String> results = new HashSet<>();
            for (CompletableFuture<String> future : timestampFutures) {
                String result = future.get();
                results.add(result);
                assertThat(result).startsWith("TIMESTAMP_SUCCESS_");
            }
            
            // Verify that we have the expected number of unique results
            assertThat(results).hasSize(numberOfConcurrentRequests);
            
            // Verify that we get all different request IDs
            for (int i = 0; i < numberOfConcurrentRequests; i++) {
                assertThat(results).contains("TIMESTAMP_SUCCESS_" + i);
            }
            
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }
}