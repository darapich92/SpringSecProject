package com.example;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);
    private final PasswordEncoder passwordEncoder;
    private final OpaService opaService;

    public EmployeeController(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder,
            OpaService opaService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.opaService = opaService;
    }

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @GetMapping("/employees")
    public Iterable<Employee> findAllEmployees() {
        logger.info("GetMethod");
        // System.out.print("GetMethod");
        return this.employeeRepository.findAll();
    }

    @PostMapping("/employees")
    public Employee addOneEmployee(@RequestBody Employee employee) {
        logger.info("PostMethod");
        // System.out.print("PostMethod");
        return this.employeeRepository.save(employee);
    }

    @DeleteMapping("/employee/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Integer id) {
        if (this.employeeRepository.existsById(id)) {
            this.employeeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }

    }

    @PutMapping("/employee/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Integer id, @RequestBody Employee updatedEmployee) {
        java.util.Optional<Employee> existingEmployee = this.employeeRepository.findById(id);

        if (existingEmployee.isPresent()) {
            Employee employee = existingEmployee.get();

            // Update employee properties here
            employee.setFirstName(updatedEmployee.getFirstName());
            employee.setLastName(updatedEmployee.getLastName());
            employee.setRole(updatedEmployee.getRole());
            employee.setUsername(updatedEmployee.getUsername());
            // Add other fields as needed

            // Save the updated employee
            employeeRepository.save(employee);

            return ResponseEntity.ok(employee);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/register")
    public Employee registerEmployee(@RequestBody Employee employee) {
        logger.info("PostMethod");
        employee.setPassword(passwordEncoder.encode(employee.getPassword())); // Encode password

        return this.employeeRepository.save(employee); // Save employee to DB
    }

    @PostMapping("/newlogin")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        logger.info("here" + username);
        Employee employee = employeeRepository.findByUsername(username);
        if (employee == null || !passwordEncoder.matches(password, employee.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        // If the username and password are correct, you can return a success message or
        // a token (e.g., JWT)
        // if (accessPermission(username)) {
        // return ResponseEntity.ok("Login successful");
        // } else {
        // return ResponseEntity.ok("Login unsuccessful");
        // }

        return ResponseEntity.ok("Login successful");

    }

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

            accessPermission(roles,location);

            logger.info("Username: " + username);
            logger.info("Access Token: " + accessToken);
            logger.info("Roles: " + roles);
            logger.info("Location: " + location);
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
            logger.error("Failed to parse roles from access token", e);
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
            logger.error("Failed to parse roles from access token", e);
        }
        return location; // Return an empty list if roles cannot be extracted
    }

    @GetMapping("/nice")
    public String getNice(Model model, Authentication auth) {

        return "nice.html";
    }

    private boolean accessPermission(List<String> role, String location) {

        // Check access permission using OPA
        boolean isAllowed = opaService.isAllowed("GET", role, location);

        System.out.println("Admin Access: " + isAllowed);

        return isAllowed;

    }

}
