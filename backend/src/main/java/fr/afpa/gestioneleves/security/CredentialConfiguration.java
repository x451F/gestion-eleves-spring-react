package fr.afpa.gestioneleves.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class CredentialConfiguration {
    @Bean Clock clock() { return Clock.systemUTC(); }

    @Bean
    PasswordEncoder passwordEncoder() {
        Map<String, PasswordEncoder> encoders = new LinkedHashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder(12));
        DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder("bcrypt", encoders);
        encoder.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder(12));
        return encoder;
    }
}
