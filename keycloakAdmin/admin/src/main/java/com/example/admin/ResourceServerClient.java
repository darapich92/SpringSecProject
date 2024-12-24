package com.example.admin;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Service
public class ResourceServerClient {

    private final WebClient webClient;

    public ResourceServerClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    // Step 1: Obtain Access Token
    public TokenResponse getTokens(String username, String password) {
        String tokenEndpoint = "http://localhost/realms/Spring_App/protocol/openid-connect/token";

        // Keycloak client credentials
        String clientId = "login-app";
        String clientSecret = "ykZsEsUarnXGas8HaBJJzUNpss6f17BS";

        return webClient.post()
            .uri(tokenEndpoint)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .bodyValue("client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&grant_type=password" +
                    "&scope=openid" +
                    "&username=" + username +
                    "&password=" + password)
            .retrieve()
            .bodyToMono(String.class) // Capture the response as a String
            .flatMap(response -> {
                try {
                    // Deserialize the JSON response to TokenResponse
                    TokenResponse tokenResponse = new ObjectMapper().readValue(response, TokenResponse.class);
                    return Mono.just(tokenResponse); // Return the whole TokenResponse
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return Mono.error(new RuntimeException("Error parsing JSON response"));
                }
            })
            .block(); // Blocking for simplicity
    }

    // Step 2: Call Resource Server
    public String callResourceServer(String accessToken) {
        String resourceServerEndpoint = "http://localhost:8183/api/admin/test";

        return webClient.get()
                .uri(resourceServerEndpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // Blocking for simplicity
    }

}
