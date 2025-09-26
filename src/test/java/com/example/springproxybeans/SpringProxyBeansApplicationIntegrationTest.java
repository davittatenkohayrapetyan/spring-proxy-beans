package com.example.springproxybeans;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
}