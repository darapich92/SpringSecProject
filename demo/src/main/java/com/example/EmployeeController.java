package com.example;

import org.slf4j.LoggerFactory;
import org.apache.el.stream.Optional;
import org.slf4j.Logger;
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
import org.springframework.web.bind.annotation.RestController;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

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
        if (accessAuthentication(username)) {
            return ResponseEntity.ok("Login successful");
        } else {
            return ResponseEntity.ok("Login unsuccessful");
        }

    }

    @GetMapping("/")
    public String getIndex(Model model, Authentication auth) {
        model.addAttribute(
            "name",
            auth instanceof OAuth2AuthenticationToken oauth && oauth.getPrincipal() instanceof OidcUser oidc ? 
                oidc.getPreferredUsername() : 
                "");
        model.addAttribute("isAuthenticated", auth != null && auth.isAuthenticated());
        model.addAttribute("isNice", auth != null && auth.getAuthorities().stream().anyMatch(authority -> Objects.equals("NICE", authority.getAuthority())));
        return "index.html";
    }

    @GetMapping("/nice")
    public String getNice(Model model, Authentication auth) {
        return "nice.html";
    }

    private boolean accessAuthentication(String username) {

        Employee employee = employeeRepository.findByUsername(username);
        String role = employee.getRole();

        logger.info(role);
        // Check access permission using OPA
        boolean isAdminAllowed = opaService.isAllowed("GET", "/some/path", role);
        boolean isEditorAllowed = opaService.isAllowed("POST", "/some/path", "editor");

        System.out.println("Admin Access: " + isAdminAllowed); 
        System.out.println("Editor Access: " + isEditorAllowed);

        return opaService.isAllowed("GET", "/employees", role);

    }

    private boolean accessPermission(String username) {

        Employee employee = employeeRepository.findByUsername(username);
        String role = employee.getRole();

        logger.info(role);
        // Check access permission using OPA
        boolean isAdminAllowed = opaService.isAllowed("GET", "/some/path", role);
        boolean isEditorAllowed = opaService.isAllowed("POST", "/some/path", "editor");

        System.out.println("Admin Access: " + isAdminAllowed); 
        System.out.println("Editor Access: " + isEditorAllowed);

        return opaService.isAllowed("GET", "/employees", "viewer");

    }

}
