package com.example.admin;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.HttpHeaders;
import reactor.core.publisher.Mono;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

@Controller
public class EmployeeController {

    private final ResourceServerClient resourceServerClient;
    private static final Logger logger = LoggerFactory.getLogger(EmployeeController.class);
    private final WebClient webClient;

    @Value("${keycloak.logout-url}")
    private String keycloakLogoutUrl;

    @Value("${keycloak.post-logout-redirect-uri}")
    private String postLogoutRedirectUri;

    public EmployeeController(ResourceServerClient resourceServerClient,
            WebClient.Builder webClientBuilder) {
        this.resourceServerClient = resourceServerClient;
        this.webClient = webClientBuilder.build();
    }

    @GetMapping("/home")
    public String redirectToOAuth(Model model) {
        return "login.html";
    }

    @PostMapping("/api/login")
    public String login(@RequestParam String username,
            @RequestParam String password,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request,
            HttpServletResponse response) {
        String resourceServerUrl = "http://localhost:8183/api/resource";
        boolean isAuthenticated = false;

        try {
            // Retrieve tokens (access_token and id_token)
            TokenResponse tokenResponse = resourceServerClient.getTokens(username, password);

            if (tokenResponse == null) {
                throw new RuntimeException("Token response is null");
            }

            String accessToken = tokenResponse.getAccessToken();
            String idToken = tokenResponse.getIdToken();

            logger.info("tokenResponse: "+tokenResponse);
            logger.info("Access Token "+ accessToken);
            logger.info("Id Token "+idToken);

            // Verify authentication with resource server
            webClient.get()
                    .uri(resourceServerUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .toBodilessEntity()
                    .block(); // Ensure authentication by resource server succeeds

            // Decode the token and set Authentication
            OidcUser oidcUser = getOidcUserFromKeycloakToken(idToken); // Use idToken for OIDC user
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    oidcUser, null, oidcUser.getAuthorities());

            // Set the Authentication in SecurityContextHolder
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // Save the SecurityContext
            HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
            securityContextRepository.saveContext(context, request, response);

            logger.info("While login " + SecurityContextHolder.getContext().getAuthentication());

            // Authentication successful
            isAuthenticated = true;
            model.addAttribute("name", username);
            model.addAttribute("isAuthenticated", isAuthenticated);

        } catch (Exception e) {
            logger.error("Error during authentication: ", e);

            // Authentication failed
            isAuthenticated = false;
            model.addAttribute("isAuthenticated", isAuthenticated);
        }
        return "index.html"; // Ensure this is the correct path for your template
    }

    private OidcUser getOidcUserFromKeycloakToken(String token) {
        try {
            // Fetch the public key from Keycloak's JWKS endpoint
            String jwksUrl = "http://localhost/realms/Spring_App/protocol/openid-connect/certs";
            URI jwksUri = URI.create(jwksUrl); // Create a URI from the string
            JWKSet jwkSet = JWKSet.load(jwksUri.toURL());
            RSAKey rsaKey = (RSAKey) jwkSet.getKeys().get(0); // Assumes the first key is used

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(rsaKey.toPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            logger.info("Claims: {}", claims);
            // Extract username
            // Extract username (subject) and expiration
            String username = claims.getSubject(); // "sub" field
            Instant expiration = claims.getExpiration().toInstant();

            // Extract roles from "resource_access"
            List<GrantedAuthority> authorities = new ArrayList<>();
            Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");
            if (resourceAccess != null && resourceAccess.get("login-app") != null) {
                Map<String, Object> loginApp = (Map<String, Object>) resourceAccess.get("login-app");
                if (loginApp.get("roles") != null) {
                    authorities = ((List<String>) loginApp.get("roles"))
                            .stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                }
            } else {
                logger.warn("No roles found in resource_access for login-app");
            }

            // Use the claims directly if you don't have an id_token claim
            OidcIdToken idToken = new OidcIdToken(token, Instant.now(), expiration, claims);

            // Create OidcUserInfo from the claims
            OidcUserInfo oidcUserInfo = new OidcUserInfo(claims);

            // Return the OidcUser, combining the IdToken and UserInfo
            return new DefaultOidcUser(authorities, idToken, oidcUserInfo);

        } catch (IOException | ParseException e) {
            throw new RuntimeException("Failed to load JWK set or parse token", e);
        } catch (JOSEException e) {
            throw new RuntimeException("Failed to convert JWK to RSA public key", e);
        }
    }

    @GetMapping("/debug/authentication")
    public ResponseEntity<String> debugAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Authentication set for user: {}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.ok("User is not authenticated");
        } else {
            return ResponseEntity.ok("Authenticated user: " + authentication.getName());
        }
    }

    @PostMapping("/custom-logout")
    public String logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        logger.info("Logout endpoint reached!");

        // Log the current authentication details
        logger.info("Authentication: " + authentication);

        // Check if session exists and log session details
        HttpSession session = request.getSession(false);
        logger.info("Session ID: " + (session != null ? session.getId() : "No session"));

        if (authentication.getPrincipal() instanceof OidcUser) {
            logger.info("OIDCUser here");
        }

        // Handle the logout process if the user is authenticated
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String idToken = oidcUser.getIdToken().getTokenValue();

            // Construct the Keycloak logout URL
            String keycloakLogoutUrl = "http://localhost/realms/Spring_App/protocol/openid-connect/logout";
            String postLogoutRedirectUri = "http://localhost:8082/"; // Adjust based on your frontend URL

            String logoutUrl = keycloakLogoutUrl + "?post_logout_redirect_uri=" + postLogoutRedirectUri + "&id_token_hint=" + idToken;

            logger.info("Constructed Keycloak logout URL: " + logoutUrl);

            // Invalidate session and clear SecurityContext
            SecurityContextHolder.clearContext();
            if (session != null) {
                logger.info("Invalidating session with ID: " + session.getId());
                session.invalidate();
            } else {
                logger.info("No session found to invalidate.");
            }

            // Redirect to Keycloak logout URL
            return "redirect:" + logoutUrl;
        }

        // If no authenticated user, redirect to login
        logger.info("User is not authenticated.");
        return "redirect:/login";
    }

    @GetMapping("/")
    public String getIndex(Model model, Authentication auth) {
        return "login.html";
    }
}
