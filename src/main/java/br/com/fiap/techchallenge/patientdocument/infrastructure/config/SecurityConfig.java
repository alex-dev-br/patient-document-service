package br.com.fiap.techchallenge.patientdocument.infrastructure.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;

import java.util.ArrayList;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String KEYCLOAK_CLIENT_SSL_BUNDLE =
            "keycloak-client";

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
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
                        ).hasAuthority(
                                "SCOPE_documents:write"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/patients/*/documents",
                                "/patients/*/timeline"
                        ).hasAuthority(
                                "SCOPE_documents:read"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/documents/*/file"
                        ).hasAuthority(
                                "SCOPE_documents:file:read"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/documents/*",
                                "/documents/*/processed-results"
                        ).hasAuthority(
                                "SCOPE_documents:read"
                        )

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/documents/*/ai-result"
                        ).hasAuthority(
                                "SCOPE_documents:ai-result:write"
                        )

                        .requestMatchers(
                                HttpMethod.POST,
                                "/patients"
                        ).hasAuthority(
                                "SCOPE_patients:write"
                        )

                        .requestMatchers(
                                HttpMethod.GET,
                                "/patients",
                                "/patients/*"
                        ).hasAuthority(
                                "SCOPE_patients:read"
                        )

                        .anyRequest()
                        .authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                )
                .build();
    }

    @Bean
    @Profile("prod")
    JwtDecoder keycloakJwtDecoder(
            RestTemplateBuilder restTemplateBuilder,
            SslBundles sslBundles,
            OAuth2ResourceServerProperties resourceServerProperties
    ) {
        OAuth2ResourceServerProperties.Jwt jwtProperties =
                resourceServerProperties.getJwt();

        String jwkSetUri = requireProperty(
                jwtProperties.getJwkSetUri(),
                "spring.security.oauth2.resourceserver.jwt.jwk-set-uri"
        );

        String issuerUri = requireProperty(
                jwtProperties.getIssuerUri(),
                "spring.security.oauth2.resourceserver.jwt.issuer-uri"
        );

        if (jwtProperties.getAudiences().isEmpty()) {
            throw new IllegalStateException(
                    "A propriedade " +
                            "'spring.security.oauth2.resourceserver.jwt.audiences' " +
                            "deve possuir ao menos um valor."
            );
        }

        RestOperations keycloakRestOperations =
                restTemplateBuilder
                        .sslBundle(
                                sslBundles.getBundle(
                                        KEYCLOAK_CLIENT_SSL_BUNDLE
                                )
                        )
                        .build();

        NimbusJwtDecoder jwtDecoder =
                NimbusJwtDecoder
                        .withJwkSetUri(jwkSetUri)
                        .restOperations(keycloakRestOperations)
                        .build();

        ArrayList<OAuth2TokenValidator<Jwt>> validators =
                new ArrayList<>();

        validators.add(
                new JwtIssuerValidator(issuerUri)
        );

        jwtProperties.getAudiences()
                .forEach(audience ->
                        validators.add(
                                new JwtAudienceValidator(audience)
                        )
                );

        jwtDecoder.setJwtValidator(
                JwtValidators.createDefaultWithValidators(
                        validators
                )
        );

        return jwtDecoder;
    }

    private static String requireProperty(
            String value,
            String propertyName
    ) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "A propriedade '" +
                            propertyName +
                            "' deve estar preenchida."
            );
        }

        return value;
    }
}
