package com.kalyan.advtaskmanager.config;

import com.kalyan.advtaskmanager.entity.Role;
import com.kalyan.advtaskmanager.entity.User;
import com.kalyan.advtaskmanager.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner init(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            var existing = userRepository.findByEmail("admin@gmail.com");

            if (existing.isEmpty()) {
                // First run — create admin
                User admin = User.builder()
                        .name("Admin")
                        .email("admin@gmail.com")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .build();
                userRepository.save(admin);
                System.out.println("✅ Admin created → admin@gmail.com / admin123");
            } else {
                // Already exists — re-encode password if it's still plain-text
                User admin = existing.get();
                if (admin.getPassword() == null || !admin.getPassword().startsWith("$2a$")) {
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    userRepository.save(admin);
                    System.out.println("🔐 Admin password upgraded to BCrypt.");
                } else {
                    System.out.println("ℹ️  Admin already exists with BCrypt password — skipping.");
                }
            }
        };
    }
}
