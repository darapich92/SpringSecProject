package com.example.admin;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.convert.converter.Converter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .csrf(csrf -> csrf.disable())
                .addFilterBefore((request, response, chain) -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    logger.info("Current Authentication: " + auth);
                    if (request instanceof HttpServletRequest) {
                        HttpServletRequest httpRequest = (HttpServletRequest) request;
                        logger.info("Request URI: " + httpRequest.getRequestURI());
                    }
                    chain.doFilter(request, response);
                }, SecurityContextHolderFilter.class)
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/", "/home", "/login", "/api/login", "/debug/authentication", "/api/logout",
                                "/custom-logout")
                        .permitAll()
                        .requestMatchers("/api/logout", "/custom-logout").authenticated()
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll())
                        .logout(logout -> logout
                        .logoutUrl("/logout") // Default logout URL (Spring Security)
                        .addLogoutHandler((request, response, authentication) -> {
                            // Log here before Spring Security processes logout
                            logger.info("SecurityContext Authentication before logout handler: "
                                    + SecurityContextHolder.getContext().getAuthentication());
        
                            if (authentication != null) {
                                logger.info("Authentication in logout handler: " + authentication.getName());
                            } else {
                                logger.info("Authentication is null during logout handler.");
                            }
                        })
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // Log after the SecurityContext is cleared by Spring Security
                            logger.info("SecurityContext Authentication in logoutSuccessHandler: "
                                    + SecurityContextHolder.getContext().getAuthentication());
                            
                            // Custom handling for logout, if needed
                        })
                        .logoutSuccessUrl("/login") // Redirect after successful logout
                        .permitAll()
                    );

        return http.build();
    }

}
