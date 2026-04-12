package tests;

import models.User;
import services.UserService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class CreateAdmin {
    public static void main(String[] args) {
        try {
            UserService userService = new UserService();

            User admin = new User();

            admin.setId(UUID.randomUUID());
            admin.setEmail("admin@gmail.com");
            admin.setPassword("admin123");
            admin.setRolesJson("[\"ROLE_ADMIN\"]"); // 🔥 IMPORTANT
            admin.setFirstname("Admin");
            admin.setLastname("FitSense");
            admin.setAccountStatus("active");
            admin.setDateCreation(LocalDateTime.now(ZoneOffset.UTC));
            admin.setUsername("admin");

            userService.create(admin);

            System.out.println("✅ Admin créé !");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}