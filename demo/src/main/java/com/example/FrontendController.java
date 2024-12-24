package com.example;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.security.core.Authentication;

@Controller
public class FrontendController {

    @Autowired
    private FrontendService apiService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/directlogin")
    public String redirectToOAuth() {
        return "redirect:/oauth2/authorization/keycloak";
    }

    // @GetMapping("/login")
    // public String getIndex(Model model, Authentication auth) {
    // if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal()
    // instanceof OidcUser oidc) {
    // // Extract username
    // String username = oidc.getPreferredUsername();

    // // Retrieve the access token
    // OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
    // oauth.getAuthorizedClientRegistrationId(),
    // oauth.getName());

    // String accessToken = client.getAccessToken().getTokenValue();

    // List<String> roles = extractRoleFromAccessToken(accessToken);
    // String location = extractLocationFromAccessToken(accessToken);

    // // Add data to the model
    // model.addAttribute("name", username);
    // model.addAttribute("accessToken", accessToken);
    // model.addAttribute("isAuthenticated", auth.isAuthenticated());

    // // accessPermission(roles, location);

    // // logger.info("Username: " + username);
    // // logger.info("Access Token: " + accessToken);
    // // logger.info("Roles: " + roles);
    // // logger.info("Location: " + location);
    // } else {
    // model.addAttribute("name", "");
    // model.addAttribute("accessToken", "");
    // model.addAttribute("isAuthenticated", false);
    // }

    // return "index.html";
    // }

    @GetMapping("/")
    public String getIndex(Model model, Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc) {
            // Extract username
            String username = oidc.getPreferredUsername();

            // Retrieve the access token
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauth.getAuthorizedClientRegistrationId(),
                    oauth.getName());

            String accessToken = client.getAccessToken().getTokenValue();

            List<String> roles = extractRoleFromAccessToken(accessToken);
            String location = extractLocationFromAccessToken(accessToken);

            // Add data to the model
            model.addAttribute("name", username);
            model.addAttribute("accessToken", accessToken);
            model.addAttribute("isAuthenticated", auth.isAuthenticated());

            // accessPermission(roles, location);

            // logger.info("Username: " + username);
            // logger.info("Access Token: " + accessToken);
            // logger.info("Roles: " + roles);
            // logger.info("Location: " + location);
        } else {
            model.addAttribute("name", "");
            model.addAttribute("accessToken", "");
            model.addAttribute("isAuthenticated", false);
        }

        return "index.html";
    }

    // Helper method to extract roles
    private List<String> extractRoleFromAccessToken(String accessToken) {
        try {
            String[] parts = accessToken.split("\\.");
            String payload = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            // Parse the payload as a JSON object
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(payload);

            // Extract roles from resource_access
            JsonNode resourceAccessNode = jsonNode.get("resource_access");
            if (resourceAccessNode != null) {
                JsonNode clientRolesNode = resourceAccessNode.get("login-app"); // Replace "your-client-id"
                if (clientRolesNode != null) {
                    JsonNode rolesNode = clientRolesNode.get("roles");
                    if (rolesNode != null && rolesNode.isArray()) {
                        List<String> roles = new ArrayList<>();
                        rolesNode.forEach(roleNode -> roles.add(roleNode.asText()));
                        return roles;
                    }
                }
            }
        } catch (Exception e) {
            // logger.error("Failed to parse roles from access token", e);
        }
        return List.of(); // Return an empty list if roles cannot be extracted
    }

    // Helper method to extract roles
    private String extractLocationFromAccessToken(String accessToken) {

        String location = "";
        try {
            String[] parts = accessToken.split("\\.");
            String payload = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            // Parse the payload as a JSON object
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(payload);

            // Extract roles from resource_access
            JsonNode locationNode = jsonNode.get("location");
            if (locationNode != null) {
                location = locationNode.asText();
                System.out.println("Location: " + location);
            } else {
                System.out.println("Location claim is missing in the token.");
            }
        } catch (Exception e) {
            // logger.error("Failed to parse roles from access token", e);
        }
        return location; // Return an empty list if roles cannot be extracted
    }

    @GetMapping("/login-success")
    public String loginSuccess(@RequestParam String user) {
        // Process the received user information
        return "Welcome, " + user + "! Login was successful.";
    }

}
