package com.tcs.SmartBancsApp.config;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.core.*;
import org.springframework.web.cors.*;
import com.tcs.SmartBancsApp.repositories.RepositoryUsers;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.DispatcherType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    public static final String ISSUER = "smartbancs";

    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    SecretKey jwtKey(@Value("${JWT_SECRET:}") String configuredSecret) {
        byte[] bytes;
        if (configuredSecret.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            LoggerFactory.getLogger(SecurityConfig.class).info(
                    "Clave JWT temporal para demo: reiniciar Java requiere iniciar sesion otra vez");
        } else {
            bytes = Base64.getDecoder().decode(configuredSecret);
            if (bytes.length < 32) {
                throw new IllegalArgumentException("JWT_SECRET debe contener al menos 32 bytes en Base64");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtKey, RepositoryUsers users) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER), token -> {
                    try {
                        var user = users.findById(UUID.fromString(token.getSubject()));
                        String account = token.getClaimAsString("account_number");
                        if (account != null && account.matches("[0-9]{8}") && user.isPresent()
                                && CredentialVersion.of(user.get().getPassword())
                                    .equals(token.getClaimAsString("credentials"))) {
                            return OAuth2TokenValidatorResult.success();
                        }
                    } catch (IllegalArgumentException exception) {
                        // Un subject que no sea UUID no es una identidad emitida por esta API.
                    }
                    return OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_token", "Sesion expirada o credenciales actualizadas", null));
                }));
        return decoder;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
        config.setExposedHeaders(List.of("Location", "Idempotency-Replayed"));
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // Solo Bearer en Authorization; no se autentica mediante cookies del navegador.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/users", "/users/", "/auth/login").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {}))
                .build();
    }
}
