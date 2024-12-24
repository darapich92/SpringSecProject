package com.example;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class Controller {

    private final ResourceServerClient resourceServerClient;
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    public Controller(ResourceServerClient resourceServerClient) {
        this.resourceServerClient = resourceServerClient;
    }

    @GetMapping("/resource")
    public ResponseEntity<String> getResource() {
        return ResponseEntity.ok("Secure resource accessed!");
    }

    @GetMapping("/public/test")
    public String publicEndpoint() {
        return "This is a public endpoint.";
    }

    @GetMapping("/admin/test")
    public String adminEndpoint() {
        return "This is an admin-protected endpoint.";
    }

    @GetMapping("/user/test")
    public String userEndpoint() {
        return "This is an authenticated user endpoint.";
    }
}
