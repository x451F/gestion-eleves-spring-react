package fr.afpa.gestioneleves.security;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 64;

    public boolean isValid(String password) {
        return password != null && password.length() >= MIN_LENGTH && password.length() <= MAX_LENGTH;
    }
}
