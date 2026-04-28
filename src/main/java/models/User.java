package models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User — modèle représentant un utilisateur de l'application.
 *
 * Cette classe est le "M" du pattern MVC pour le module utilisateur.
 * Elle ne contient que des données (attributs + getters/setters), aucune logique SQL.
 * La logique SQL est dans UserService.
 *
 * Correspond à la table MySQL : app_user
 * Colonnes principales :
 *   id               → BINARY(16) UUID stocké en binaire (voir UuidUtil)
 *   email_email      → adresse email (unique)
 *   password         → mot de passe hashé en BCrypt
 *   roles            → JSON ex: ["ROLE_USER"], ["ROLE_ADMIN"], ["ROLE_COACH"]
 *   name_firstname   → prénom
 *   name_lastname    → nom
 *   account_status   → "active" ou "inactive"
 *   date_creation    → date d'inscription
 *   username         → nom d'utilisateur unique généré depuis l'email
 *   phone_number     → numéro de téléphone (optionnel)
 *   photo            → chemin ou URL de la photo de profil (optionnel)
 */
public class User {

    /** Identifiant unique UUID — stocké en BINARY(16) dans MySQL, converti par UuidUtil */
    private UUID id;

    /** Email de l'utilisateur — sert aussi d'identifiant de connexion */
    private String email;

    /**
     * Mot de passe hashé en BCrypt.
     * Jamais stocké en clair. Exemple de hash : "$2a$10$xyz..."
     * Hachage fait dans UserService.hashPasswordIfPlain()
     * Vérification faite dans SignInController avec passwordEncoder.matches()
     */
    private String password;

    /**
     * Rôles de l'utilisateur au format JSON.
     * Exemples :
     *   ["ROLE_USER"]   → utilisateur normal
     *   ["ROLE_ADMIN"]  → administrateur
     *   ["ROLE_COACH"]  → coach
     * Utilisé dans SignInController pour rediriger vers le bon dashboard.
     */
    private String rolesJson;

    /** Prénom de l'utilisateur */
    private String firstname;

    /** Nom de famille de l'utilisateur */
    private String lastname;

    /**
     * Statut du compte : "active" ou "inactive".
     * L'admin peut changer ce statut depuis AdminDashboardController.
     */
    private String accountStatus;

    /** Date et heure d'inscription — remplie automatiquement par NOW() en SQL */
    private LocalDateTime dateCreation;

    /** Secret pour l'authentification à deux facteurs (Google Authenticator) — optionnel */
    private String googleAuthenticatorSecret;

    /** Numéro de téléphone — optionnel */
    private String phoneNumber;

    /** Photo de profil (URL ou chemin) — optionnel */
    private String photo;

    /** Nom d'utilisateur unique généré depuis l'email lors de l'inscription */
    private String username;

    /**
     * Objectif sportif de l'utilisateur (ex: "perte de poids", "prise de masse").
     * Récupéré depuis app_user.objective lors du mapRow dans UserService.
     */
    private String accountObjective;

    /**
     * Genre de l'utilisateur (ex: "male", "female").
     * Récupéré depuis app_user.gender lors du mapRow dans UserService.
     */
    private String accountGender;

    /** Constructeur vide — nécessaire pour créer un User puis remplir ses champs un par un */
    public User() {
    }

    /**
     * Constructeur complet — utilisé dans UserService.mapRow() pour reconstruire
     * un User depuis un ResultSet (ligne de la base de données).
     */
    public User(UUID id, String email, String password, String rolesJson, String firstname, String lastname,
                String accountStatus, LocalDateTime dateCreation, String googleAuthenticatorSecret,
                String phoneNumber, String photo, String username) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.rolesJson = rolesJson;
        this.firstname = firstname;
        this.lastname = lastname;
        this.accountStatus = accountStatus;
        this.dateCreation = dateCreation;
        this.googleAuthenticatorSecret = googleAuthenticatorSecret;
        this.phoneNumber = phoneNumber;
        this.photo = photo;
        this.username = username;
    }

    // ── Getters et Setters ────────────────────────────────────────────────────
    // Permettent aux contrôleurs et services d'accéder aux attributs privés.

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRolesJson() { return rolesJson; }
    public void setRolesJson(String rolesJson) { this.rolesJson = rolesJson; }

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getGoogleAuthenticatorSecret() { return googleAuthenticatorSecret; }
    public void setGoogleAuthenticatorSecret(String googleAuthenticatorSecret) {
        this.googleAuthenticatorSecret = googleAuthenticatorSecret;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAccountObjective() { return accountObjective; }
    public void setAccountObjective(String accountObjective) { this.accountObjective = accountObjective; }

    public String getAccountGender() { return accountGender; }
    public void setAccountGender(String accountGender) { this.accountGender = accountGender; }

    /** Représentation textuelle utile pour le débogage (System.out.println(user)) */
    @Override
    public String toString() {
        return "User{" + "id=" + id + ", email='" + email + '\'' + ", firstname='" + firstname + '\''
                + ", lastname='" + lastname + '\'' + ", accountStatus='" + accountStatus + '\'' + ", username='"
                + username + '\'' + '}';
    }
}
