package com.nexora.backend;

import com.nexora.backend.entity.Admin;
import com.nexora.backend.entity.Category;
import com.nexora.backend.repository.AdminRepository;
import com.nexora.backend.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.default-username}")
    private String defaultUsername;

    @Value("${admin.default-password}")
    private String defaultPassword;

    private static final List<String> STARTER_CATEGORIES =
            List.of("AI", "Security", "Dev", "Hardware", "Emerging");

    public DataSeeder(AdminRepository adminRepository, CategoryRepository categoryRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.categoryRepository = categoryRepository;
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

        // The Category table starts empty. Seed the original 5 starter
        // categories once so they're immediately usable, instead of relying
        // on an admin to recreate them by hand.
        if (categoryRepository.count() == 0) {
            for (String name : STARTER_CATEGORIES) {
                Category category = new Category();
                category.setName(name);
                categoryRepository.save(category);
            }
            System.out.println(">>> Seeded starter categories: " + STARTER_CATEGORIES);
        }
    }
}