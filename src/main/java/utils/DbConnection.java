package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DbConnection — connexion unique à la base de données MySQL.
 *
 * Utilise le pattern Singleton : une seule instance de connexion est créée
 * pour toute l'application. Tous les services (UserService, WorkoutService...)
 * récupèrent cette même connexion via getInstance().getCnx().
 *
 * Flux :
 *   DbConnection.getInstance()  →  retourne l'instance unique
 *   .getCnx()                   →  retourne l'objet Connection JDBC
 *   service.executeQuery(...)   →  exécute la requête SQL
 */
public class DbConnection {

    /**
     * URL de connexion JDBC à MySQL.
     * Format : jdbc:mysql://hôte:port/nomBase?options
     *
     * On peut surcharger cette valeur au lancement avec :
     *   -Dfitsense.db.url=jdbc:mysql://autrehost:3306/autrebase?serverTimezone=UTC
     *
     * serverTimezone=UTC     : évite les erreurs de fuseau horaire avec MySQL 8
     * characterEncoding=UTF-8 : force l'encodage des caractères (accents, etc.)
     */
    public static final String DB_URL = System.getProperty(
            "fitsense.db.url",
            "jdbc:mysql://127.0.0.1:3306/fitsense?serverTimezone=GMT%2B1&characterEncoding=UTF-8");

    // Identifiants de connexion MySQL (à ne pas mettre en dur en production)
    public static final String DB_USER = "root";
    public static final String DB_PASSWORD = "root";

    /** L'objet Connection JDBC utilisé pour exécuter les requêtes SQL */
    public Connection cnx;

    /** L'instance unique du Singleton — null au départ, créée au premier appel */
    public static DbConnection instance;

    /**
     * Constructeur privé : empêche de faire "new DbConnection()" depuis l'extérieur.
     * Ouvre la connexion MySQL une seule fois au démarrage.
     * Lance une RuntimeException si MySQL n'est pas accessible.
     */
    private DbConnection() {
        try {
            cnx = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("FitSense connected: " + DB_URL);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            throw new RuntimeException("Cannot connect to database: " + e.getMessage(), e);
        }
    }

    /**
     * Retourne l'objet Connection JDBC.
     * Utilisé dans chaque service : cnx = DbConnection.getInstance().getCnx()
     */
    public Connection getCnx() {
        return cnx;
    }

    /**
     * Point d'accès unique au Singleton.
     *
     * Première fois : crée la connexion et la stocke dans "instance".
     * Fois suivantes : retourne directement "instance" sans se reconnecter.
     *
     * Exemple d'utilisation dans UserService :
     *   cnx = DbConnection.getInstance().getCnx();
     */
    public static DbConnection getInstance() {
        if (instance == null)           // si pas encore connecté
            instance = new DbConnection(); // crée la connexion une seule fois
        return instance;                // retourne toujours la même instance
    }
}
