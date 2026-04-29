package services;

import models.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * UserService — service CRUD pour la table app_user.
 *
 * C'est le seul endroit où le code SQL lié aux utilisateurs est écrit.
 * Les contrôleurs (SignUpController, SignInController, AdminDashboardController)
 * utilisent ce service sans jamais écrire de SQL directement.
 *
 * Architecture :
 *   Controller → UserService → PreparedStatement → MySQL (table app_user)
 *
 * Implémente CRUD<User> : oblige à définir create, read, update, delete, createPrepared.
 */
public class UserService implements CRUD<User> {

    /** Connexion JDBC récupérée depuis le Singleton DbConnection */
    public Connection cnx;

    /** Encodeur BCrypt pour hasher et vérifier les mots de passe */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Constructeur : récupère la connexion unique à la base de données.
     * Appelé dans chaque contrôleur qui a besoin de gérer les utilisateurs.
     * Ex: userService = new UserService();
     */
    public UserService() {
        cnx = DbConnection.getInstance().getCnx();
    }

    /**
     * mapRow — convertit une ligne de ResultSet en objet User.
     *
     * Appelé après chaque SELECT pour transformer les données brutes SQL
     * en objet Java utilisable dans les contrôleurs.
     *
     * Exemple : une ligne de app_user avec email_email="test@test.com"
     * devient un objet User avec user.getEmail() = "test@test.com"
     */
    private static User mapRow(ResultSet rs) throws SQLException {
        // UuidUtil.fromResultSet lit le BINARY(16) et le convertit en UUID Java
        UUID id = UuidUtil.fromResultSet(rs, "id");

        // Convertit le DATETIME MySQL en LocalDateTime Java
        Timestamp ts = rs.getTimestamp("date_creation");
        LocalDateTime dateCreation =
                ts != null ? ts.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime() : null;

        User user = new User(
                id,
                rs.getString("email_email"),
                rs.getString("password"),
                rs.getString("roles"),
                rs.getString("name_firstname"),
                rs.getString("name_lastname"),
                rs.getString("account_status"),
                dateCreation,
                rs.getString("google_authenticator_secret"),
                rs.getString("phone_number"),
                rs.getString("photo"),
                rs.getString("username"));

        // Ces colonnes peuvent ne pas exister dans certaines requêtes → on ignore l'erreur
        user.setAccountObjective(safeGetString(rs, "objective"));
        user.setAccountGender(safeGetString(rs, "gender"));
        return user;
    }

    /** Lit une colonne sans planter si elle n'existe pas dans le ResultSet */
    private static String safeGetString(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (SQLException ignored) { return null; }
    }

    /**
     * hashPasswordIfPlain — hache le mot de passe si ce n'est pas déjà un hash BCrypt.
     *
     * BCrypt produit des hashes qui commencent par "$2a$", "$2y$" ou "$2b$".
     * Si le mot de passe commence déjà par ces préfixes, il est déjà hashé → on ne re-hache pas.
     * Sinon, on le hache avant de l'insérer en base.
     *
     * Exemple :
     *   "123456"        → "$2a$10$xyz..." (hashé)
     *   "$2a$10$xyz..." → "$2a$10$xyz..." (déjà hashé, inchangé)
     */
    private String hashPasswordIfPlain(String password) {
        if (password == null) return null;
        if (password.startsWith("$2a$") || password.startsWith("$2y$") || password.startsWith("$2b$")) {
            return password; // déjà un hash BCrypt
        }
        return passwordEncoder.encode(password); // hache le mot de passe en clair
    }

    // ── Méthodes de recherche ─────────────────────────────────────────────────

    /**
     * findByEmail — cherche un utilisateur par son email.
     *
     * Utilisé dans :
     *   - SignInController : pour récupérer l'utilisateur lors de la connexion
     *   - SignUpController : pour vérifier qu'un email n'est pas déjà utilisé
     *   - AdminDashboardController : pour vérifier les doublons avant création/modification
     *
     * Utilise un PreparedStatement avec "?" pour éviter les injections SQL.
     * Retourne null si aucun utilisateur trouvé.
     */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM `app_user` WHERE `email_email` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, email); // remplace le "?" par l'email
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs); // convertit la ligne en objet User
            }
        }
        return null; // aucun utilisateur trouvé avec cet email
    }

    /**
     * findByUsername — cherche un utilisateur par son nom d'utilisateur.
     *
     * Utilisé dans SignUpController.uniqueUsernameFromEmail() pour vérifier
     * qu'un username généré n'est pas déjà pris avant de l'assigner.
     */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM `app_user` WHERE `username` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ── Implémentation de l'interface CRUD<User> ──────────────────────────────

    /**
     * CREATE — délègue à createPrepared() qui est la version sécurisée.
     */
    @Override
    public void create(User user) throws SQLException { createPrepared(user); }

    /**
     * READ — récupère tous les utilisateurs de la base.
     *
     * Utilise un Statement simple (pas de paramètres variables).
     * Appelé dans AdminDashboardController.refreshData() pour afficher
     * la liste des utilisateurs dans le TableView.
     *
     * Retourne : SELECT * FROM app_user → List<User>
     */
    @Override
    public List<User> read() throws SQLException {
        String sql = "SELECT * FROM `app_user`";
        // try-with-resources : ferme automatiquement stmt et rs après utilisation
        try (Statement stmt = cnx.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            List<User> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs)); // chaque ligne → un objet User
            return list;
        }
    }

    /**
     * UPDATE — met à jour les informations d'un utilisateur existant.
     *
     * Utilise un PreparedStatement avec "?" pour chaque valeur modifiable.
     * Le WHERE id = ? cible uniquement la ligne de cet utilisateur.
     * Appelé dans AdminDashboardController.onConfirmEdit() et updateStatusForRow().
     *
     * Note : le mot de passe est re-hashé si nécessaire via hashPasswordIfPlain().
     */
    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE `app_user` SET `email_email` = ?, `password` = ?, `roles` = ?, "
                + "`name_firstname` = ?, `name_lastname` = ?, `account_status` = ? "
                + "WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, hashPasswordIfPlain(user.getPassword()));
            stmt.setString(3, user.getRolesJson() != null ? user.getRolesJson() : "[\"ROLE_USER\"]");
            stmt.setString(4, user.getFirstname());
            stmt.setString(5, user.getLastname());
            stmt.setString(6, user.getAccountStatus());
            // UuidUtil.toBytes16 convertit l'UUID Java en BINARY(16) pour MySQL
            stmt.setBytes(7, UuidUtil.toBytes16(user.getId()));
            stmt.executeUpdate(); // exécute le UPDATE
        }
    }

    /**
     * DELETE — supprime un utilisateur de la base par son id.
     *
     * Appelé dans AdminDashboardController.onConfirmDelete().
     * Le PreparedStatement avec "?" évite toute injection SQL.
     */
    @Override
    public void delete(User user) throws SQLException {
        String sql = "DELETE FROM `app_user` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            stmt.setBytes(1, UuidUtil.toBytes16(user.getId()));
            stmt.executeUpdate(); // exécute le DELETE
        }
    }

    /**
     * deleteByIds — supprime plusieurs utilisateurs en une seule opération.
     *
     * Utilise addBatch() / executeBatch() pour envoyer plusieurs DELETE
     * en une seule transaction, plus efficace que des DELETE un par un.
     */
    public void deleteByIds(List<UUID> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) return;
        String sql = "DELETE FROM `app_user` WHERE `id` = ?";
        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            for (UUID id : ids) {
                stmt.setBytes(1, UuidUtil.toBytes16(id));
                stmt.addBatch(); // ajoute ce DELETE à la file d'attente
            }
            stmt.executeBatch(); // envoie tous les DELETE d'un coup
        }
    }

    /**
     * createPrepared — insère un nouvel utilisateur en base de données.
     *
     * C'est la méthode principale d'inscription. Elle :
     *   1. Assigne le rôle par défaut "ROLE_USER" si non défini
     *   2. Assigne le statut "active" si non défini
     *   3. Génère un UUID aléatoire pour l'id
     *   4. Hache le mot de passe avec BCrypt avant insertion
     *   5. Insère toutes les données avec un PreparedStatement sécurisé
     *   6. Assigne l'UUID généré à l'objet user (pour l'utiliser après l'appel)
     *
     * Appelé dans :
     *   - SignUpController.onSignUp() : inscription d'un nouvel utilisateur
     *   - AdminDashboardController.onActivateCoach() : création d'un coach par l'admin
     *   - FitSenseApp.seedCoach() : création du coach par défaut au démarrage
     */
    @Override
    public void createPrepared(User user) throws SQLException {
        // Valeurs par défaut si non renseignées
        if (user.getRolesJson() == null || user.getRolesJson().isBlank()) {
            user.setRolesJson("[\"ROLE_USER\"]");
        }
        if (user.getAccountStatus() == null || user.getAccountStatus().isBlank()) {
            user.setAccountStatus("active");
        }

        // Génère un identifiant unique pour ce nouvel utilisateur
        UUID newId = UUID.randomUUID();

        String sql = "INSERT INTO `app_user` "
                + "(`id`, `email_email`, `password`, `roles`, `name_firstname`, `name_lastname`, "
                + "`account_status`, `date_creation`, `username`, `phone_number`, `photo`, "
                + "`google_authenticator_secret`) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?)";

        try (PreparedStatement stmt = cnx.prepareStatement(sql)) {
            // Chaque setXxx(position, valeur) remplace un "?" dans la requête
            stmt.setBytes(1, UuidUtil.toBytes16(newId));          // id en BINARY(16)
            stmt.setString(2, user.getEmail());                    // email
            stmt.setString(3, hashPasswordIfPlain(user.getPassword())); // mot de passe hashé
            stmt.setString(4, user.getRolesJson());                // ex: ["ROLE_USER"]
            stmt.setString(5, user.getFirstname());                // prénom
            stmt.setString(6, user.getLastname());                 // nom
            stmt.setString(7, user.getAccountStatus());            // "active"
            // date_creation → NOW() géré directement par MySQL
            stmt.setString(8, user.getUsername());                 // username unique
            stmt.setString(9, user.getPhoneNumber());              // téléphone (peut être null)
            stmt.setString(10, user.getPhoto());                   // photo (peut être null)
            stmt.setString(11, user.getGoogleAuthenticatorSecret()); // 2FA (peut être null)

            stmt.executeUpdate(); // exécute l'INSERT

            // Assigne l'UUID généré à l'objet pour pouvoir l'utiliser après l'appel
            // Ex: AppSession.setCurrentUser(user) → user.getId() sera disponible
            user.setId(newId);
        }
    }
}
