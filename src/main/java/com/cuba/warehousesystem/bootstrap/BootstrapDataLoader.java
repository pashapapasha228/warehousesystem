package com.cuba.warehousesystem.bootstrap;

import com.cuba.warehousesystem.model.User;
import com.cuba.warehousesystem.model.UserRole;
import com.cuba.warehousesystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapDataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting bootstrap data loading...");

        // Проверяем, есть ли уже пользователи
        if (userRepository.count() > 0) {
            log.info("Users already exist in the database. Skipping bootstrap.");
            return;
        }

        log.info("Creating initial users...");

        // Создаем пользователей с разными ролями
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin123")); // Используем encoder
        admin.setRole(UserRole.ADMIN);

        User manager = new User();
        manager.setUsername("manager");
        manager.setPasswordHash(passwordEncoder.encode("manager123"));
        manager.setRole(UserRole.MANAGER);

        User storekeeper = new User();
        storekeeper.setUsername("storekeeper");
        storekeeper.setPasswordHash(passwordEncoder.encode("storekeeper123"));
        storekeeper.setRole(UserRole.STOREKEEPER);

        // Сохраняем в базу
        userRepository.save(admin);
        userRepository.save(manager);
        userRepository.save(storekeeper);

        log.info("Bootstrap data loaded successfully.");
    }
}
