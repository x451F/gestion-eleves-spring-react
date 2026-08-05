package fr.afpa.gestioneleves.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.config.Customizer;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {
    private static final Duration CLOCK_SKEW = Duration.ofSeconds(30);

    @Bean
    SecretKey jwtSecretKey(JwtProperties properties) {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(properties.secretBase64());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT secret must be Base64 encoded", ex);
        }
        if (key.length < 32) {
            throw new IllegalStateException("JWT secret must decode to at least 32 bytes");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties properties, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer()), requiredClaimsValidator(clock)));
        return decoder;
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository(RefreshCookieProperties refreshCookieProperties) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.path("/")
                .sameSite("Strict")
                .secure(refreshCookieProperties.secure()));
        return repository;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
                                            AccessJwtAuthenticationConverter jwtAuthenticationConverter,
                                            ProblemDetailAuthenticationEntryPoint authenticationEntryPoint,
                                            ProblemDetailAccessDeniedHandler accessDeniedHandler,
                                            CookieCsrfTokenRepository csrfTokenRepository,
                                            BearerCsrfProtectionFilter bearerCsrfProtectionFilter) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .requireCsrfProtectionMatcher(new AndRequestMatcher(
                                CsrfFilter.DEFAULT_CSRF_MATCHER,
                                request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/auth/"),
                                new NegatedRequestMatcher(new OrRequestMatcher(
                                        new AntPathRequestMatcher("/api/auth/activate"),
                                        new AntPathRequestMatcher("/api/auth/forgot-password"),
                                        new AntPathRequestMatcher("/api/auth/reset-password"))))))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterAfter(bearerCsrfProtectionFilter, CsrfFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login", "/api/auth/csrf", "/api/auth/refresh", "/api/auth/logout",
                                "/api/auth/activate", "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/logout-all").authenticated()
                        .requestMatchers("/api/admin/accounts/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .jwt(jwt -> jwt.decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    private OAuth2TokenValidator<Jwt> requiredClaimsValidator(Clock clock) {
        return jwt -> {
            try {
                long subject = Long.parseLong(jwt.getSubject());
                Number version = jwt.getClaim("ver");
                String role = jwt.getClaimAsString("role");
                Instant issuedAt = jwt.getIssuedAt();
                Instant expiresAt = jwt.getExpiresAt();
                if (subject <= 0 || version == null || version.longValue() < 0
                        || !isIntegral(version) || jwt.getId() == null || jwt.getId().isBlank()
                        || issuedAt == null || expiresAt == null || !expiresAt.isAfter(clock.instant())
                        || expiresAt.isBefore(issuedAt)
                        || issuedAt.isAfter(clock.instant().plus(CLOCK_SKEW))) {
                    return invalidToken();
                }
                fr.afpa.gestioneleves.enumtype.Role.valueOf(role);
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException ex) {
                return invalidToken();
            }
        };
    }

    private boolean isIntegral(Number number) {
        return number instanceof Byte || number instanceof Short || number instanceof Integer || number instanceof Long;
    }

    private OAuth2TokenValidatorResult invalidToken() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid access token", null));
    }
}
