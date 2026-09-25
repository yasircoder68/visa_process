package mca.visareg.dashboard.security;

import mca.visareg.dashboard.entity.User;
import mca.visareg.dashboard.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UserSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            String visaPassword = System.getenv("VISA_OFFICER_PASSWORD");
            if (visaPassword == null || visaPassword.isBlank()) {
                visaPassword = "password123";
            }

            String frroPassword = System.getenv("FRRO_OFFICER_PASSWORD");
            if (frroPassword == null || frroPassword.isBlank()) {
                frroPassword = "password123";
            }

            // Seed required officers
            userRepository.save(new User(
                    "visaofficer01",
                    passwordEncoder.encode(visaPassword),
                    "VISA_OFFICER",
                    "VO01"
            ));

            userRepository.save(new User(
                    "frroofficer01",
                    passwordEncoder.encode(frroPassword),
                    "FRRO_OFFICER",
                    "FRRO01"
            ));

            userRepository.save(new User(
                    "frroofficer02",
                    passwordEncoder.encode(frroPassword),
                    "FRRO_OFFICER",
                    "FRRO02"
            ));

            userRepository.save(new User(
                    "frroofficer03",
                    passwordEncoder.encode(frroPassword),
                    "FRRO_OFFICER",
                    "FRRO03"
            ));

            log.info("Default users seeded successfully into database (BCrypt encrypted)");
        }
    }
}
