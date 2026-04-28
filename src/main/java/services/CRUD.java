package services;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface CRUD — contrat de base pour tous les services de l'application.
 *
 * CRUD = Create, Read, Update, Delete : les 4 opérations fondamentales
 * sur une base de données.
 *
 * Chaque service (UserService, WorkoutService, NotificationService...)
 * implémente cette interface pour la classe qu'il gère.
 *
 * Le type générique <X> représente le modèle concerné.
 * Exemple : CRUD<User> dans UserService, CRUD<Workout> dans WorkoutService.
 *
 * Architecture :
 *   Controller → Service (implémente CRUD<X>) → Base de données
 *   Ex: AdminDashboardController → UserService → MySQL table app_user
 */
public interface CRUD<X> {

    /**
     * CREATE — insère un nouvel enregistrement en base.
     * Dans UserService, délègue à createPrepared() qui utilise un PreparedStatement.
     */
    void create(X x) throws SQLException;

    /**
     * READ — récupère tous les enregistrements de la table.
     * Retourne une List<X> avec tous les objets mappés depuis la base.
     * Exemple : userService.read() → SELECT * FROM app_user
     */
    List<X> read() throws SQLException;

    /**
     * UPDATE — met à jour un enregistrement existant en base.
     * L'objet X doit avoir un identifiant (id) pour cibler la bonne ligne.
     * Exemple : userService.update(user) → UPDATE app_user SET ... WHERE id = ?
     */
    void update(X x) throws SQLException;

    /**
     * DELETE — supprime un enregistrement de la base.
     * Exemple : userService.delete(user) → DELETE FROM app_user WHERE id = ?
     */
    void delete(X x) throws SQLException;

    /**
     * CREATE avec PreparedStatement — version sécurisée de create().
     *
     * Utilise un PreparedStatement avec des "?" pour éviter les injections SQL.
     * C'est cette méthode qui est utilisée partout dans le projet pour les insertions.
     *
     * Différence avec create() :
     *   Statement     → requête fixe, pas de paramètres → risque d'injection SQL
     *   PreparedStatement → paramètres séparés de la requête → sécurisé
     */
    void createPrepared(X x) throws SQLException;
}
