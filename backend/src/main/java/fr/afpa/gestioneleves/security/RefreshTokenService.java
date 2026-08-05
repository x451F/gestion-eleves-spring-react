package fr.afpa.gestioneleves.security;

import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {
    private final OpaqueTokenService opaqueTokenService;

    public RefreshTokenService(OpaqueTokenService opaqueTokenService) {
        this.opaqueTokenService = opaqueTokenService;
    }

    public String generate() {
        return opaqueTokenService.generate();
    }

    public String hash(String rawToken) {
        return opaqueTokenService.hash(rawToken);
    }
}
