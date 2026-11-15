package com.dzfashion.identity.config;

import com.dzfashion.identity.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitialOwnerSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialOwnerSeeder.class);

    private final UserService userService;

    @Value("${INITIAL_OWNER_USERNAME:}")
    private String initialOwnerUsername;

    @Value("${INITIAL_OWNER_EMAIL:}")
    private String initialOwnerEmail;

    @Value("${INITIAL_OWNER_PASSWORD:}")
    private String initialOwnerPassword;

    public InitialOwnerSeeder(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (initialOwnerUsername != null && !initialOwnerUsername.isBlank()
                && initialOwnerEmail != null && !initialOwnerEmail.isBlank()
                && initialOwnerPassword != null && !initialOwnerPassword.isBlank()) {
            try {
                var owner = userService.bootstrapOwnerIfAbsent(
                        initialOwnerUsername,
                        initialOwnerEmail,
                        initialOwnerPassword,
                        "D & Z Owner"
                );
                if (owner != null) {
                    log.info("Initial Owner account successfully bootstrapped for {}", initialOwnerUsername);
                }
            } catch (Exception e) {
                log.warn("Initial owner account bootstrap skipped: {}", e.getMessage());
            }
        }
    }
}
