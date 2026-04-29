package models;

import java.time.LocalDateTime;

/**
 * Notification — modèle représentant une notification destinée à l'administrateur.
 *
 * Correspond à la table MySQL : admin_notification
 * Créée automatiquement par NotificationService.ensureTableExists() si elle n'existe pas.
 *
 * Colonnes :
 *   id           → INT AUTO_INCREMENT (clé primaire)
 *   type         → catégorie de la notification, ex: "NEW_USER"
 *   message      → texte affiché dans le panneau notifications de l'admin
 *   target_email → email de l'utilisateur concerné par la notification
 *   is_read      → 0 = non lue, 1 = lue (TINYINT en MySQL, boolean en Java)
 *   created_at   → date et heure de création
 *
 * Utilisée dans :
 *   - SignUpController : crée une notification quand un nouvel utilisateur s'inscrit
 *   - AdminDashboardController : affiche et gère les notifications dans le panneau admin
 *   - NotificationService : gère toutes les opérations SQL sur cette table
 */
public class Notification {

    /** Identifiant auto-incrémenté par MySQL — 0 avant insertion */
    private int id;

    /**
     * Type de la notification.
     * Actuellement utilisé : "NEW_USER" (nouvel utilisateur inscrit).
     * Peut être étendu : "NEW_WORKOUT", "NEW_MESSAGE", etc.
     */
    private String type;

    /** Texte de la notification affiché dans l'interface admin.
     *  Ex: "New athlete registered: John Doe" */
    private String message;

    /** Email de l'utilisateur concerné par la notification */
    private String targetEmail;

    /**
     * Statut de lecture de la notification.
     * false = non lue (affichée en surbrillance dans l'interface)
     * true  = lue (affichée en grisé)
     * Stocké en TINYINT(1) dans MySQL : 0 = false, 1 = true
     */
    private boolean read;

    /** Date et heure de création de la notification */
    private LocalDateTime createdAt;

    /** Constructeur vide — nécessaire pour mapRow() dans NotificationService */
    public Notification() {}

    /**
     * Constructeur utilisé dans SignUpController lors de la création d'une notification.
     * L'id est auto-généré par MySQL, donc pas besoin de le passer ici.
     * read est initialisé à false (non lue par défaut).
     *
     * Exemple d'utilisation dans SignUpController :
     *   Notification notif = new Notification(
     *       "NEW_USER",
     *       "New athlete registered: John Doe",
     *       "john@example.com",
     *       LocalDateTime.now()
     *   );
     *   notificationService.create(notif);
     */
    public Notification(String type, String message, String targetEmail, LocalDateTime createdAt) {
        this.type = type;
        this.message = message;
        this.targetEmail = targetEmail;
        this.read = false; // toute nouvelle notification est non lue
        this.createdAt = createdAt;
    }

    // ── Getters et Setters ────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTargetEmail() { return targetEmail; }
    public void setTargetEmail(String targetEmail) { this.targetEmail = targetEmail; }

    /** isRead() (et non getRead()) car Java génère "is" pour les booléens */
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
