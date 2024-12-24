package com.example;

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
    public String getAccessToken() {
        String tokenEndpoint = "http://localhost/realms/Spring_App/protocol/openid-connect/token";

        // Keycloak client credentials
        String clientId = "login-app";
        String clientSecret = "ykZsEsUarnXGas8HaBJJzUNpss6f17BS";
        String username = "user1";
        String password = "123";

        return webClient.post()
            .uri(tokenEndpoint)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .bodyValue("client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&grant_type=password" +
                    "&username=" + username +
                    "&password=" + password)
            .retrieve()
            .bodyToMono(String.class) // Capture the response as a String
            .flatMap(response -> {
                try {
                    // Use ObjectMapper to convert JSON to TokenResponse
                    TokenResponse tokenResponse = new ObjectMapper().readValue(response, TokenResponse.class);
                    return Mono.just(tokenResponse.getAccessToken());
                } catch (JsonProcessingException e) {
                    // Log and handle the exception here
                    e.printStackTrace();
                    return Mono.error(new RuntimeException("Error parsing JSON response"));
                }
            })
            .block();    // Blocking for simplicity
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

    // DTO for token response
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TokenResponse {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("refresh_expires_in")
    private Long refreshExpiresIn;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("scope")
    private String scope;

    // Getters and Setters
    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public void setRefreshExpiresIn(Long refreshExpiresIn) {
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
}
