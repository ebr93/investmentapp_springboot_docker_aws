package org.perscholas.investmentapp;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.perscholas.investmentapp.dao.*;
import org.perscholas.investmentapp.models.*;
import org.perscholas.investmentapp.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev") // seed data only when dev profile is active
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MyCommandLineRunner implements CommandLineRunner {

    UserRepoI userRepoI;
    AddressRepoI addressRepoI;
    AuthGroupRepoI authGroupRepoI;
    UserServices userServices;

    @Autowired
    public MyCommandLineRunner(
            UserRepoI userRepoI,
            AddressRepoI addressRepoI,
            AuthGroupRepoI authGroupRepoI,
            UserServices userServices
    ) {
        this.userRepoI = userRepoI;
        this.addressRepoI = addressRepoI;
        this.authGroupRepoI = authGroupRepoI;
        this.userServices = userServices;
    }

    @PostConstruct
    void created() {
        log.warn("=============== MyCommandLineRunner CREATED (dev only) ===============");
    }

    @Override
    public void run(String... args) {

        // Seed users only if they don't already exist (prevents duplicate email errors)
        seedUserIfMissing(
                "email@email.com",
                "Edward", "Barcenas", "Hello1234!",
                new Address("123 Main St", "CA", 12345),
                "ROLE_ADMIN"
        );

        seedUserIfMissing(
                "janedoe@example.com",
                "Jane", "Doe", "Hello1234!",
                new Address("456 Elm St", "NY", 10001),
                "ROLE_ADMIN"
        );

        seedUserIfMissing(
                "bobsmith@example.com",
                "Bob", "Smith", "Hello1234!",
                new Address("789 Oak St", "TX", 75001),
                "ROLE_USER"
        );

        seedUserIfMissing(
                "alicejohnson@example.com",
                "Alice", "Johnson", "Hello1234!",
                new Address("987 Pine St", "FL", 33428),
                "ROLE_USER"
        );

        seedUserIfMissing(
                "sambrown@example.com",
                "Sam", "Brown", "Hello1234!",
                new Address("654 Cedar Ave", "IL", 60601),
                "ROLE_USER"
        );

        log.warn("Dev user/role seed completed (stocks are seeded by Flyway V2).");
    }

    private void seedUserIfMissing(
            String email,
            String firstName,
            String lastName,
            String rawPassword,
            Address address,
            String role
    ) {

        if (userRepoI.findByEmail(email).isPresent()) {
            log.warn("Seed skipped for user {} (already exists).", email);
            return;
        }

        // Save address first so it has an ID
        Address savedAddress = addressRepoI.saveAndFlush(address);

        // Create user (your constructor hashes password)
        User user = new User(firstName, lastName, email, rawPassword);

        // Persist user (use service if it applies defaults/roles)
        try {
                userServices.createOrUpdate(user);
        } catch (Exception e) {
                log.debug("User: " + user.getEmail() + ", failed to be created at MyCommandLineRunner");
                e.printStackTrace();
        }

        // Attach address via service helper
        userServices.addOrUpdateAddress(savedAddress, user);

        // Ensure role exists for this email
        authGroupRepoI.save(new AuthGroup(email, role));

        log.warn("Seeded user {} with role {}.", email, role);
    }
}
