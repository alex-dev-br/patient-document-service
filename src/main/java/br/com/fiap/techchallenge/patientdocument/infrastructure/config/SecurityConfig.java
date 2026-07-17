package br.com.fiap.techchallenge.patientdocument.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/patients/*/documents"
                        ).hasAuthority("SCOPE_documents:write")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/patients/*/documents",
                                "/patients/*/timeline"
                        ).hasAuthority("SCOPE_documents:read")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/documents/*/file"
                        ).hasAuthority("SCOPE_documents:file:read")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/documents/*",
                                "/documents/*/processed-results"
                        ).hasAuthority("SCOPE_documents:read")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/documents/*/ai-result"
                        ).hasAuthority("SCOPE_documents:ai-result:write")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/patients"
                        ).hasAuthority("SCOPE_patients:write")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/patients",
                                "/patients/*"
                        ).hasAuthority("SCOPE_patients:read")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                )
                .build();
    }
}
