package org.perscholas.investmentapp.config;

import lombok.extern.slf4j.Slf4j;
import org.perscholas.investmentapp.dao.AuthGroupRepoI;
import org.perscholas.investmentapp.dao.UserRepoI;
import org.perscholas.investmentapp.models.AuthGroup;
import org.perscholas.investmentapp.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
public class AdminBootstrapper implements ApplicationRunner {

    private final UserRepoI userRepoI;
    private final AuthGroupRepoI authGroupRepoI;

    @Value("${BOOTSTRAP_ADMIN:false}")
    private boolean bootstrapAdmin;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    public AdminBootstrapper(UserRepoI userRepoI, AuthGroupRepoI authGroupRepoI) {
        this.userRepoI = userRepoI;
        this.authGroupRepoI = authGroupRepoI;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!bootstrapAdmin) {
            log.info("AdminBootstrapper: BOOTSTRAP_ADMIN=false (skipping).");
            return;
        }

        if (isBlank(adminEmail) || isBlank(adminPassword)) {
            // Fail fast: you explicitly asked for deterministic bootstrap behavior gated by env.
            throw new IllegalStateException(
                    "AdminBootstrapper: BOOTSTRAP_ADMIN=true but ADMIN_EMAIL/ADMIN_PASSWORD is missing."
            );
        }

        // 1) Create admin user if missing
        var existingUser = userRepoI.findByEmailAllIgnoreCase(adminEmail);
        if (existingUser.isEmpty()) {
            // Must satisfy your User validation patterns: letters-only, length 2..30.
            // Address is nullable in schema, so we can omit it safely.
            User admin = new User("Admin", "User", adminEmail, adminPassword);
            userRepoI.save(admin);
            log.warn("AdminBootstrapper: created admin user {}", adminEmail);
        } else {
            log.info("AdminBootstrapper: admin user {} already exists (no changes to password).", adminEmail);
        }

        // 2) Ensure ROLE_ADMIN exists for this email (idempotent)
        List<AuthGroup> roles = authGroupRepoI.findByEmail(adminEmail);
        boolean hasAdmin = roles.stream().anyMatch(r -> "ROLE_ADMIN".equalsIgnoreCase(r.getRole()));

        if (!hasAdmin) {
            authGroupRepoI.saveAndFlush(new AuthGroup(adminEmail, "ROLE_ADMIN"));
            log.warn("AdminBootstrapper: granted ROLE_ADMIN to {}", adminEmail);
        } else {
            log.info("AdminBootstrapper: {} already has ROLE_ADMIN.", adminEmail);
        }

        log.warn("AdminBootstrapper: bootstrap complete for {}", adminEmail);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}