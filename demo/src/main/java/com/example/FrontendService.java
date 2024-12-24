package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Service
public class FrontendService {

    private final WebClient webClient;

    @Autowired
    public FrontendService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8082").build(); // Set your backend API URL here
    }

    public Mono<String> getIndex(String accessToken) {
        return webClient.get()
                .uri("/") // You can adjust the URI as needed
                .header("Authorization", "Bearer " + accessToken) // Pass the access token in the Authorization header
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), 
                          clientResponse -> Mono.error(new RuntimeException("API Error")))
                .bodyToMono(String.class); // Convert the response body to a String
    }
}
