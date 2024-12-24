package com.example.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.PostConstruct;

// @Component
public class KeycloakUserCreator {

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

    private String[] location = { "Brussels", "Antwerp", "Paris", "USA", "Namur", "New_York", "Brazil", "Mons"};

    private String adminToken;

    int i = 0;

    @PostConstruct
    public void createUsers() throws IOException {
        // adminToken = getAdminToken();
        // // deleteAllUsers();
        // // Randomly assign roles to users
        // Random random = new Random();
        // List<Map<String, Object>> roles = listClientRoles(appId);

        // if (adminToken != null) {
        //     for (int i = 1863; i <= 2000; i++) {
        //         String randomlocation = location[random.nextInt(location.length)];
        //         String userId = createUser("user" + i, "123", "user" + i + "@example.com", "user" + i, "user" + i + 1,
        //                 randomlocation);

        //         Map<String, Object> randomRole = roles.get(random.nextInt(roles.size()));
        //         String roleName = (String) randomRole.get("name");
        //         if (userId != null) {
        //             // Assign role to the user
        //             assignClientRoleToUser(userId, appId, roleName); // Replace "user-role" with your desired role name
        //         }
        //     }
        // }
        // // Fetch all users from Keycloak
        // List<Map<String, Object>> users = fetchAllUsers();
        
        // // Export to CSV
        // exportUsersToCSV(users, appId);

    }

    // Fetch users from Keycloak API
    private List<Map<String, Object>> fetchAllUsers() {
        RestTemplate restTemplate = new RestTemplate();
        String url = keycloakAuthServerUrl + "/admin/realms/" + realm + "/users";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        List<Map<String, Object>> allUsers = new ArrayList<>();
        int page = 0;
        int pageSize = 100;
        
        while (true) {
            // Paginate the user request
            String paginatedUrl = url + "?first=" + (page * pageSize) + "&max=" + pageSize;
            ResponseEntity<List> response = restTemplate.exchange(paginatedUrl, HttpMethod.GET, request, List.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, Object>> users = (List<Map<String, Object>>) response.getBody();
                allUsers.addAll(users);
                
                // If the response contains fewer users than the page size, break out of the loop
                if (users.size() < pageSize) {
                    break;
                }
                
                // Move to the next page
                page++;
            } else {
                System.err.println("Failed to fetch users: " + response.getStatusCode());
                break;
            }
        }
        return allUsers;
    }

    // Export user data to CSV file
    private void exportUsersToCSV(List<Map<String, Object>> users, String clientID) throws IOException {
        // Create a FileWriter and BufferedWriter
        FileWriter fileWriter = new FileWriter("users_with_roles_and_location.csv");
        BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

        // Write header row
        bufferedWriter.write("ID,Username,Email,First Name,Last Name,Enabled,Roles,Location\n");

        // Write user data rows
        for (Map<String, Object> user : users) {
            String userId = (String) user.get("id");
            String username = (String) user.get("username");
            String email = (String) user.get("email");
            String firstName = (String) user.get("firstName");
            String lastName = (String) user.get("lastName");
            boolean enabled = (boolean) user.get("enabled");

            // Fetch the location from user attributes (assuming it's stored under 'attributes')
            String location = null;
            Map<String, Object> attributes = (Map<String, Object>) user.get("attributes");
            if (attributes != null && attributes.containsKey("location")) {
                location = (String) attributes.get("location").toString();
            }

            // Fetch the roles for this user
            String roles = fetchUserRolesForLoginApp(userId);

            // Write each user's data to the CSV file
            bufferedWriter.write(String.format("%s,%s,%s,%s,%s,%b,%s,%s\n", userId, username, email, firstName, lastName, enabled, roles, location));
        }

        // Close the writer
        bufferedWriter.close();

        System.out.println("Exported users to users_with_roles_and_location.csv");
    }

    private String fetchUserRolesForLoginApp(String userId) {
        RestTemplate restTemplate = new RestTemplate();
        String clientsUrl = keycloakAuthServerUrl + "/admin/realms/" + realm + "/clients";
    
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
    
        // Fetch all clients
        ResponseEntity<List> clientsResponse = restTemplate.exchange(clientsUrl, HttpMethod.GET, request, List.class);
        if (clientsResponse.getStatusCode() != HttpStatus.OK) {
            System.err.println("Failed to fetch clients: " + clientsResponse.getStatusCode());
            return "";
        }
    
        List<Map<String, Object>> clients = (List<Map<String, Object>>) clientsResponse.getBody();
    
        // Find the client with clientId = "login-app"
        Map<String, Object> loginAppClient = clients.stream()
                .filter(client -> "login-app".equals(client.get("clientId")))
                .findFirst()
                .orElse(null);
    
        if (loginAppClient == null) {
            System.err.println("Client with clientId 'login-app' not found.");
            return "";
        }
    
        String clientUUID = (String) loginAppClient.get("id");
    
        // Fetch roles for this user and client
        String rolesUrl = keycloakAuthServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/clients/" + clientUUID;
        ResponseEntity<List> rolesResponse = restTemplate.exchange(rolesUrl, HttpMethod.GET, request, List.class);
    
        if (rolesResponse.getStatusCode() == HttpStatus.OK) {
            List<Map<String, Object>> roles = (List<Map<String, Object>>) rolesResponse.getBody();
            if (!roles.isEmpty()) {
                StringBuilder rolesString = new StringBuilder();
                for (Map<String, Object> role : roles) {
                    rolesString.append(role.get("name")).append("; ");
                }
                return rolesString.toString();
            } else {
                return "User ID: " + userId + ", Client ID: login-app, Roles: No Roles\n";
            }
        } else {
            System.err.println("Failed to fetch roles for user " + userId + " and client 'login-app': " + rolesResponse.getStatusCode());
            return "";
        }
    }
    

    public void deleteAllUsers() {
        RestTemplate restTemplate = new RestTemplate();

        int start = 0;
        int batchSize = 100; // Fetch users in batches of 100
        boolean hasMoreUsers = true;

        while (hasMoreUsers) {
            // Step 1: Fetch a batch of users
            String usersUrl = keycloakAuthServerUrl + "/admin/realms/" + realm + "/users?first=" + start + "&max="
                    + batchSize;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(usersUrl, HttpMethod.GET, request, List.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, Object>> users = response.getBody();

                if (users != null && !users.isEmpty()) {
                    for (Map<String, Object> user : users) {
                        String userId = (String) user.get("id");
                        deleteUser(userId);
                        i++;
                    }
                    start += batchSize; // Move to the next batch
                } else {
                    hasMoreUsers = false; // No more users to fetch
                }
            } else {
                System.err.println("Failed to fetch users: " + response.getStatusCode());
                break; // Exit the loop in case of an error
            }
        }
        System.out.println(i + "All users have been deleted.");
    }

    private void deleteUser(String userId) {
        RestTemplate restTemplate = new RestTemplate();
        String deleteUrl = keycloakAuthServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Void> response = restTemplate.exchange(deleteUrl, HttpMethod.DELETE, request, Void.class);
        if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
            System.out.println("Deleted user with ID: " + userId);
        } else {
            System.err.println("Failed to delete user with ID " + userId + ": " + response.getStatusCode());
        }
    }

    private String getAdminToken() {
        RestTemplate restTemplate = new RestTemplate();
        String url = keycloakAuthServerUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=" + clientId +
                "&grant_type=password" +
                "&username=" + adminUsername +
                "&client_secret=" + clientSecret +
                "&password=" + adminPassword;

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return (String) response.getBody().get("access_token");
        } else {
            System.err.println("Failed to get admin token: " + response.getBody());
            return null;
        }
    }

    private String createUser(String username, String password, String email, String firstName, String lastName,
            String location) {
        RestTemplate restTemplate = new RestTemplate();
        String url = keycloakAuthServerUrl + "/admin/realms/" + realm + "/users";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("enabled", true);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("location", location);
        user.put("attributes", attributes);

        Map<String, Object> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", password);
        credentials.put("temporary", false);

        user.put("credentials", new Map[] { credentials });

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        if (response.getStatusCode() == HttpStatus.CREATED) {
            System.out.println("Created user: " + username);
            // Extract user ID from the Location header
            String locationHeader = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            if (locationHeader != null) {
                return locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
            }
        } else {
            System.err.println("Failed to create user: " + response.getBody());
        }
        return null;
    }

    private List<Map<String, Object>> listClientRoles(String clientId) {
        RestTemplate restTemplate = new RestTemplate();

        // Step 1: Get the Client UUID
        String clientUrl = keycloakAuthServerUrl + "/admin/realms/" + realm + "/clients";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List> clientResponse = restTemplate.exchange(clientUrl, HttpMethod.GET, request, List.class);
        if (clientResponse.getStatusCode() == HttpStatus.OK) {
            // Find the client with the given clientId
            List<Map<String, Object>> clients = clientResponse.getBody();
            Map<String, Object> client = clients.stream()
                    .filter(c -> clientId.equals(c.get("clientId")))
                    .findFirst()
                    .orElse(null);

            if (client == null) {
                System.err.println("Client with clientId '" + clientId + "' not found.");
                return Collections.emptyList();
            }

            String clientUUID = (String) client.get("id");

            // Step 2: Fetch Roles for the Client
            String rolesUrl = keycloakAuthServerUrl + "/admin/realms/" + realm + "/clients/" + clientUUID + "/roles";
            ResponseEntity<List> rolesResponse = restTemplate.exchange(rolesUrl, HttpMethod.GET, request, List.class);
            if (rolesResponse.getStatusCode() == HttpStatus.OK) {
                return rolesResponse.getBody();
            } else {
                System.err.println("Failed to fetch roles for client '" + clientId + "'.");
            }
        } else {
            System.err.println("Failed to fetch clients: " + clientResponse.getStatusCode());
        }
        return Collections.emptyList();
    }

    private void assignClientRoleToUser(String userId, String clientId, String roleName) {
        RestTemplate restTemplate = new RestTemplate();

        // Step 1: Get the Client UUID
        String clientUrl = keycloakAuthServerUrl + "/admin/realms/" + realm + "/clients";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List> clientResponse = restTemplate.exchange(clientUrl, HttpMethod.GET, request, List.class);
        if (clientResponse.getStatusCode() == HttpStatus.OK) {
            // Find the client with the given clientId
            List<Map<String, Object>> clients = clientResponse.getBody();
            Map<String, Object> client = clients.stream()
                    .filter(c -> clientId.equals(c.get("clientId")))
                    .findFirst()
                    .orElse(null);

            if (client == null) {
                System.err.println("Client with clientId '" + clientId + "' not found.");
                return;
            }

            String clientUUID = (String) client.get("id");

            // Step 2: Fetch Role Representation
            String roleUrl = keycloakAuthServerUrl + "/admin/realms/" + realm + "/clients/" + clientUUID + "/roles/"
                    + roleName;
            ResponseEntity<Map> roleResponse = restTemplate.exchange(roleUrl, HttpMethod.GET, request, Map.class);
            if (roleResponse.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> role = roleResponse.getBody();

                // Step 3: Assign the Role to the User
                String roleMappingUrl = keycloakAuthServerUrl + "/admin/realms/" + realm + "/users/" + userId
                        + "/role-mappings/clients/" + clientUUID;
                HttpEntity<Map<String, Object>[]> assignRequest = new HttpEntity<>(new Map[] { role }, headers);

                ResponseEntity<Void> assignResponse = restTemplate.exchange(roleMappingUrl, HttpMethod.POST,
                        assignRequest, Void.class);
                if (assignResponse.getStatusCode() == HttpStatus.NO_CONTENT) {
                    System.out.println("Assigned client role '" + roleName + "' for client '" + clientId
                            + "' to user with ID: " + userId);
                } else {
                    System.err.println("Failed to assign client role: " + assignResponse.getStatusCode());
                }
            } else {
                System.err.println("Role '" + roleName + "' not found for client '" + clientId + "'.");
            }
        } else {
            System.err.println("Failed to fetch clients: " + clientResponse.getStatusCode());
        }
    }

}