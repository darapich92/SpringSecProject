package com.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OpaService {
    private final RestTemplate restTemplate;

    public OpaService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public boolean isAllowed(String method, List<String> role, String location) {
        String opaUrl = "http://localhost:8181/v1/data/example/authz/allow";
        Map<String, Object> request = new HashMap<>();
        Map<String, Object> input = new HashMap<>();
        input.put("method", method);
        input.put("role", role);
        input.put("location", location);
        request.put("input", input);

        System.out.println("here is input" + input);

        ResponseEntity<Map> response = restTemplate.postForEntity(opaUrl, request, Map.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> result = response.getBody();
            System.out.println("Response from OPA: " + result); // Log response
            return result != null && (Boolean) result.get("result");
        } else {
            // Handle error response
            System.out.println("Error response from OPA: " + response.getStatusCode());
            return false; // or handle accordingly
        }
    }
}
