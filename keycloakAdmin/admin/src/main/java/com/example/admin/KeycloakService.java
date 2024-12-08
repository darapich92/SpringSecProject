package com.example.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class KeycloakService {

    private static final String KEYCLOAK_TOKEN_URL = "http://localhost:80/realms/Spring_App/protocol/openid-connect/token";
    private static final String KEYCLOAK_LOGOUT_URL = "http://localhost:80/realms/Spring_App/protocol/openid-connect/logout";

    @Value("${keycloak.auth-server-url}")
    private String keycloakAuthServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-id-app}")
    private String appId;

    @Value("${keycloak.admin-username}")
    private String adminUsername;

    @Value("${keycloak.admin-password}")
    private String adminPassword;

    @Value("${keycloak.client.secret}")
    private String clientSecret;

    @Autowired
    private RestTemplate restTemplate;

    // Login to Keycloak
    public String login(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id="+clientId+"&username=" + username + "&password=" + password +
                "&grant_type=password&client_secret="+clientSecret;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(KEYCLOAK_TOKEN_URL, HttpMethod.POST, request,
                String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            System.out.println(response.getBody());
            return response.getBody(); // Return the token
        } else {
            throw new RuntimeException("Failed to login to Keycloak: " + response.getStatusCode());
        }
    }

    // Logout from Keycloak
    public void logout(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id="+clientId+"&refresh_token=" + refreshToken + "&client_secret="+clientSecret;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response = restTemplate.exchange(KEYCLOAK_LOGOUT_URL, HttpMethod.POST, request,
                Void.class);

        if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
            throw new RuntimeException("Failed to logout from Keycloak: " + response.getStatusCode());
        }
    }
}