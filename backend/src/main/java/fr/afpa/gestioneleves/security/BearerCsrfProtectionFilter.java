package fr.afpa.gestioneleves.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Resource-server defaults exempt Bearer requests from CSRF checks. Logout-all
 * deliberately remains cookie-affecting, so it retains the same CSRF contract.
 */
@Component
public class BearerCsrfProtectionFilter extends OncePerRequestFilter {
    private final CsrfTokenRepository csrfTokenRepository;
    private final ProblemDetailAccessDeniedHandler accessDeniedHandler;

    public BearerCsrfProtectionFilter(CsrfTokenRepository csrfTokenRepository,
                                      ProblemDetailAccessDeniedHandler accessDeniedHandler) {
        this.csrfTokenRepository = csrfTokenRepository;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod())
                || !(request.getContextPath() + "/api/auth/logout-all").equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken token = csrfTokenRepository.loadToken(request);
        String presented = token == null ? null : request.getHeader(token.getHeaderName());
        if (token == null || !StringUtils.hasText(presented)
                || !MessageDigest.isEqual(token.getToken().getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8))) {
            accessDeniedHandler.handle(request, response, new MissingCsrfTokenException(presented));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
