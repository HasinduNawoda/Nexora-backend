package com.nexora.backend;

import com.nexora.backend.entity.Admin;
import com.nexora.backend.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default-username}")
    private String defaultUsername;

    @Value("${admin.default-password}")
    private String defaultPassword;

    public DataSeeder(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminRepository.findByUsername(defaultUsername).isEmpty()) {
            Admin admin = new Admin();
            admin.setUsername(defaultUsername);
            admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
            adminRepository.save(admin);
            System.out.println(">>> Default admin created: username='" + defaultUsername + "'");
        }
    }
}