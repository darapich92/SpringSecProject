package com.example;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);
    private final PasswordEncoder passwordEncoder;

    public EmployeeController(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
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

    @PostMapping("/register")
    public Employee registerEmployee(@RequestBody Employee employee) {
        logger.info("PostMethod");
        employee.setPassword(passwordEncoder.encode(employee.getPassword())); // Encode password

        return this.employeeRepository.save(employee); // Save employee to DB
    }

    @PostMapping("/newlogin")
    public ResponseEntity<String> login(@RequestBody Employee loginRequest) {
        Employee employee = employeeRepository.findByUsername(loginRequest.getUsername());
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        // Check if the provided password matches the one in the database
        if (!passwordEncoder.matches(loginRequest.getPassword(), employee.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
        }

        logger.info("GetPassword" + employee.getPassword());

        // If the username and password are correct, you can return a success message or a token (e.g., JWT)
        return ResponseEntity.ok("Login successful");
    }

}
