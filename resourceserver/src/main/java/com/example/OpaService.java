package com.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

//improve OPA service
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

        ResponseEntity<OpaResponse> response = restTemplate.postForEntity(opaUrl, request, OpaResponse.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().getResult();
        } else {
            // Handle error response
            System.out.println("Error response from OPA: " + response.getStatusCode());
            return false; // or handle accordingly
        }
    }

    public class OpaResponse {
        private Boolean result;
    
        public Boolean getResult() {
            return result;
        }
    
        public void setResult(Boolean result) {
            this.result = result;
        }
    }
}
