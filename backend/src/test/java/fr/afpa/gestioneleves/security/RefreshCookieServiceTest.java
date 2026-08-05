package fr.afpa.gestioneleves.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshCookieServiceTest {
    @Test
    void productionStyleCookieIsSecureAndHostOnly() {
        RefreshCookieService service = new RefreshCookieService(
                new RefreshCookieProperties("refresh_token", "/api/auth", Duration.ofDays(7), true, "Strict"));
        HttpHeaders headers = new HttpHeaders();

        service.addIssuedCookie(headers, "opaque-value");
        String cookie = headers.getFirst(HttpHeaders.SET_COOKIE);

        assertThat(cookie).contains("HttpOnly").contains("Secure").contains("SameSite=Strict")
                .contains("Path=/api/auth").contains("Max-Age=604800").doesNotContain("Domain=");
    }
}
