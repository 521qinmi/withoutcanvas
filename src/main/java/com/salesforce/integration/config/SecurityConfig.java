package com.salesforce.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Configure CORS
        CorsConfigurationSource corsConfigurationSource = getCorsConfigurationSource();
        
        http
            // Enable CORS configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // Disable CSRF as we are using stateless API calls and iframe embedding
            .csrf(csrf -> csrf.disable())
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Allow anonymous access to form APIs
                .requestMatchers("/form/**").permitAll()
                // Explicitly allow test-save endpoint (added per request)
                .requestMatchers("/form/test-save").permitAll()
                // Allow anonymous access to embed pages and root
                .requestMatchers("/embed", "/", "/index.html", "/css/**", "/js/**").permitAll()
                // Allow all other requests for now (adjust based on production needs)
                .anyRequest().permitAll()
            )
            // Set session management to stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Disable default login page
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    /**
     * Define CORS configuration to allow all origins for specific paths
     */
    private CorsConfigurationSource getCorsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization", "X-Requested-With", "Accept", "Origin"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply CORS to form endpoints
        source.registerCorsConfiguration("/form/**", configuration);
        // Apply CORS to embed page
        source.registerCorsConfiguration("/embed", configuration);
        
        return source;
    }
}

