package com.example;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class LoginAndPermissionTest {
    private static final String KEYCLOAK_URL = "http://localhost:2020/realms/Spring_App/protocol/openid-connect/token";
    private static final String PERMISSION_CHECK_URL = "http://localhost:8181/v1/data/example/authz/allow";

    private static final int THREAD_COUNT = 200; // Number of concurrent users
    static String filePath = "users_with_roles_and_location.csv";
    static int i, j = 0;

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<String>> results = new ArrayList<>();
        List<User> users = loadUsersFromCsv(filePath);

        // for (User user : users) {
        // // System.out.println(user);
        // i++;
        // }

        // System.out.println(i);

        for (User user : users) {
            results.add(executorService.submit(() -> loginAndCheckPermission(user)));
        }

        for (Future<String> result : results) {
            try {
                result.get();
            } catch (ExecutionException e) {
                j++;
                Throwable cause = e.getCause();
                System.err.println("Task failed due to: " + cause);
                // System.err.println("Error during execution: " + e.getMessage());
            }
        }

        System.out.println("i = " + i + "\n" + "j=" + j);

        executorService.shutdown();
    }

    public static List<User> loadUsersFromCsv(String filePath) {
        List<User> users = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true; // Skip header
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length == 8) {
                    String id = parts[0].trim();
                    String username = parts[1].trim();
                    String email = parts[2].trim();
                    String firstName = parts[3].trim();
                    String lastName = parts[4].trim();
                    boolean enabled = Boolean.parseBoolean(parts[5].trim());
                    List<String> roles = Arrays.asList(parts[6].trim().split("\\|")); // Split roles by pipe "|"
                    String location = parts[7].trim();

                    users.add(new User(id, username, email, firstName, lastName, enabled, roles, location, 123));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }

        return users;
    }

    private static String loginAndCheckPermission(User user) {
        RestTemplate restTemplate = new RestTemplate();

        // Step 1: Login to Keycloak
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String loginBody = "client_id=login-app&grant_type=password&username=" + user.getUsername() +
                "&password=" + user.getPassword() + "&client_secret=ykZsEsUarnXGas8HaBJJzUNpss6f17BS";

        HttpEntity<String> loginRequest = new HttpEntity<>(loginBody, headers);

        ResponseEntity<String> loginResponse = restTemplate.exchange(KEYCLOAK_URL, HttpMethod.POST, loginRequest,
                String.class);
        if (loginResponse.getStatusCode() == HttpStatus.OK) {
            // Extract token
            String token = extractTokenFromResponse(loginResponse.getBody());

            // Step 2: Check Permission
            List<String> roles = extractRoleFromAccessToken(token);
            String location = extractLocationFromAccessToken(token);

            Map<String, Object> request = new HashMap<>();
            Map<String, Object> input = new HashMap<>();
            input.put("method", "LOGIN");
            input.put("role", roles);
            input.put("location", location);
            request.put("input", input);

            // System.out.println("Request payload: " + user.getUsername() + request);

            try {
                // Send request to OPA
                ResponseEntity<Map> response = restTemplate.postForEntity(PERMISSION_CHECK_URL, request, Map.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    // Extract and process the response
                    Map<String, Object> result = response.getBody();
                    if (result != null && result.get("result") instanceof Boolean) {
                        // System.out.println("Response for user " + user.getUsername() + ": " + result);
                        return String.valueOf(result.get("result")); // Convert Boolean to String
                    } else {
                        i++;
                        return "Unexpected response structure from OPA: " + result;
                    }
                } else {
                    i++;
                    return "Error response from OPA: HTTP Status " + response.getStatusCode();
                }
            } catch (Exception e) {
                // Handle exceptions (e.g., network errors)
                i++;
                return "Exception during OPA request: " + e.getMessage();
            }
        } else {
            i++;
            return "Login failed for user: " + user.getUsername();
        }
    }

    private static String extractTokenFromResponse(String responseBody) {
        // Parse the token from the response body (use a library like Jackson or Gson if
        // necessary)
        // Example assumes responseBody contains a JSON object with an "access_token"
        // field
        return responseBody.split("\"access_token\":\"")[1].split("\"")[0];
    }

    // Helper method to extract roles
    private static List<String> extractRoleFromAccessToken(String accessToken) {
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
            System.out.println("Failed to parse roles from access token" + e);
        }
        return List.of(); // Return an empty list if roles cannot be extracted
    }

    // Helper method to extract roles
    private static String extractLocationFromAccessToken(String accessToken) {

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
            } else {
                System.out.println("Location claim is missing in the token.");
            }
        } catch (Exception e) {
            System.out.println("Failed to parse roles from access token" + e);
        }
        return location; // Return an empty list if roles cannot be extracted
    }

    static class User {
        private String id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private boolean enabled;
        private List<String> roles; // Multiple roles can be stored as a list
        private String location;
        private int password;

        public User(String id, String username, String email, String firstName, String lastName, boolean enabled,
                List<String> roles, String location, int password) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.enabled = enabled;
            this.roles = roles;
            this.location = location;
            this.password = password;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public int getPassword() {
            return password;
        }

        public void setPassword(int password) {
            this.password = password;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", username='" + username + '\'' +
                    ", email='" + email + '\'' +
                    ", firstName='" + firstName + '\'' +
                    ", lastName='" + lastName + '\'' +
                    ", enabled=" + enabled +
                    ", roles=" + roles +
                    ", location='" + location + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }
}
