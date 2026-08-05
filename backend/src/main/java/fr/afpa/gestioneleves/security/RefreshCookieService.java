package fr.afpa.gestioneleves.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class RefreshCookieService {
    private final RefreshCookieProperties properties;

    public RefreshCookieService(RefreshCookieProperties properties) {
        this.properties = properties;
    }

    public void addIssuedCookie(HttpHeaders headers, String rawToken) {
        headers.add(HttpHeaders.SET_COOKIE, cookie(rawToken, properties.lifetime().toSeconds()).toString());
    }

    public void addClearedCookie(HttpHeaders headers) {
        headers.add(HttpHeaders.SET_COOKIE, cookie("", 0).toString());
    }

    private ResponseCookie cookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(properties.name(), value)
                .httpOnly(true)
                .secure(properties.secure())
                .path(properties.path())
                .sameSite(properties.sameSite())
                .maxAge(maxAgeSeconds)
                .build();
    }

    public String name() {
        return properties.name();
    }
}
