package tests;

import models.ObjectifSportif;
import models.ProfilePhysique;
import models.User;
import services.ObjectifSportifService;
import services.ProfilePhysiqueService;
import services.UserService;
import utils.DbConnection;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manual JDBC smoke test (same role as your course {@code Main} in {@code tests}).
 * Requires MySQL {@code fitsense} running with Symfony schema and existing data optional.
 */
public class Main {

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(Main::shutdownMysqlCleanup, "mysql-cleanup"));

        DbConnection db = DbConnection.getInstance();
        if (db.getCnx() == null || !db.isAlive()) {
            System.err.println("No working connection — start MySQL, create database `fitsense`, and check");
            System.err.println("URL / user / password in utils.DbConnection (must match Symfony .env).");
            return;
        }
        System.out.println("Database connection OK (isValid check passed).");

        UserService userService = new UserService();
        ProfilePhysiqueService profileService = new ProfilePhysiqueService();
        ObjectifSportifService objectifService = new ObjectifSportifService();

        try {
            System.out.println("--- READ users ---");
            List<User> users = userService.read();
            users.forEach(u -> System.out.println(u));

            Map<UUID, User> userById = new HashMap<>();
            for (User u : users) {
                userById.put(u.getId(), u);
            }

            if (!users.isEmpty()) {
                /*
                 * users.get(0) is only “whoever the DB returns first” (no ORDER BY on read()).
                 * It is not “admin vs ROLE_USER” — roles are not used here at all.
                 */
                User arbitraryFirst = users.get(0);
                System.out.println("\n--- findByEmail (same as first row in list above) ---");
                System.out.println(userService.findByEmail(arbitraryFirst.getEmail()));

                System.out.println("\n--- Profile(s) for that one user only (user id=" + arbitraryFirst.getId() + ") ---");
                List<ProfilePhysique> profilesOfFirst = profileService.findByUserId(arbitraryFirst.getId());
                if (profilesOfFirst.isEmpty()) {
                    System.out.println("(none — this account has no row in profile_physique; other users may still have one.)");
                } else {
                    profilesOfFirst.forEach(System.out::println);
                }
            } else {
                System.out.println("(No rows in app_user — create users via the web app or uncomment demo insert below.)");
            }

            System.out.println("\n--- ALL rows in profile_physique (entire table) ---");
            List<ProfilePhysique> allProfiles = profileService.read();
            if (allProfiles.isEmpty()) {
                System.out.println("(table is empty)");
            } else {
                for (ProfilePhysique p : allProfiles) {
                    User owner = userById.get(p.getUserId());
                    String label = owner != null ? owner.getEmail() : "user_id not in loaded app_user list";
                    System.out.println(p + " -> " + label);
                }
            }

            System.out.println("\n--- Objectifs per profile (objectif_sportif) ---");
            for (ProfilePhysique p : allProfiles) {
                List<ObjectifSportif> objectifs = objectifService.findByProfilePhysiqueId(p.getId());
                User owner = userById.get(p.getUserId());
                String label = owner != null ? owner.getEmail() : String.valueOf(p.getUserId());
                System.out.println("Profile " + p.getId() + " (" + label + "):");
                if (objectifs.isEmpty()) {
                    System.out.println("  (no objectifs for this profile)");
                } else {
                    objectifs.forEach(o -> System.out.println("  " + o));
                }
            }

            // Demo CRUD (uncomment to test insert/update/delete on a throwaway email)
            // demoCrud(userService, profileService, objectifService);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closeQuietly();
            shutdownMysqlCleanup();
        }
    }

    @SuppressWarnings("unused")
    private static void demoCrud(UserService users, ProfilePhysiqueService profiles, ObjectifSportifService objectifs)
            throws SQLException {
        User u = new User();
        u.setEmail("javafx.demo+" + System.currentTimeMillis() + "@example.com");
        u.setPassword("demo123456");
        u.setRolesJson("[\"ROLE_USER\"]");
        u.setFirstname("Demo");
        u.setLastname("Java");
        u.setAccountStatus("active");
        u.setPhoneNumber("+33000000000");
        users.createPrepared(u);

        ProfilePhysique p = new ProfilePhysique();
        p.setUserId(u.getId());
        p.setHeight(1.75f);
        p.setWeight(70f);
        p.setGender("M");
        profiles.createPrepared(p);

        ObjectifSportif o = new ObjectifSportif();
        o.setProfilePhysiqueId(p.getId());
        o.setName("Perte de poids");
        objectifs.createPrepared(o);

        o.setName("Perte de poids (maj)");
        objectifs.update(o);

        objectifs.delete(o);
        profiles.delete(p);
        users.delete(u);
        System.out.println("Demo CRUD round-trip finished.");
    }

    /** Stops MySQL Connector/J’s cleanup thread so {@code exec:java} exits without linger warnings. */
    private static void shutdownMysqlCleanup() {
        try {
            com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.checkedShutdown();
        } catch (Throwable ignored) {
            // Older drivers or no MySQL on classpath — ignore
        }
    }
}
