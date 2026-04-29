package app;

import models.User;

/**
 * AppSession — session utilisateur en memoire, partagee par tous les controleurs.
 *
 * Joue le role de "memoire globale" de l'application : une fois l'utilisateur
 * connecte, n'importe quel controleur peut recuperer ses informations via
 * AppSession.getCurrentUser() sans repasser par la base de donnees.
 *
 * Utilise des variables statiques : il n'y a qu'une seule instance de ces
 * donnees pour toute l'application (comme une variable globale).
 *
 * Utilise dans :
 *   - SignInController : AppSession.setCurrentUser(user) apres connexion reussie
 *   - SignUpController : AppSession.setCurrentUser(user) apres inscription
 *   - AdminDashboardController : AppSession.getCurrentUser() pour afficher le nom
 *   - DashboardController : AppSession.getCurrentUser() pour personnaliser la vue
 *   - Tout controleur qui a besoin de savoir qui est connecte
 *
 * Note : cette session est perdue si l'application est fermee (pas de persistance).
 * Pour une vraie persistance, il faudrait utiliser un fichier ou une DB.
 */
public final class AppSession {

    /** L'utilisateur actuellement connecte. null si personne n'est connecte. */
    private static User currentUser;

    /**
     * Donnees temporaires collectees pendant l'onboarding (inscription).
     * Stocke la taille, le poids, le genre et l'objectif avant de les
     * sauvegarder en base a la fin du processus d'onboarding.
     */
    private static final OnboardingData onboardingData = new OnboardingData();

    /** Constructeur prive : empeche d'instancier cette classe (tout est statique) */
    private AppSession() {
    }

    /**
     * getCurrentUser — retourne l'utilisateur connecte.
     * Retourne null si personne n'est connecte.
     * Utilise dans les controleurs pour personnaliser l'interface.
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * setCurrentUser — definit l'utilisateur connecte.
     * Appele dans SignInController et SignUpController apres authentification.
     * Appele avec null lors de la deconnexion (logout).
     */
    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    /**
     * getOnboardingData — retourne les donnees d'onboarding en cours.
     * Utilise par les controleurs HeightController, WeightController,
     * GenderController, ObjectiveController pour stocker les valeurs
     * saisies etape par etape avant l'insertion finale en base.
     */
    public static OnboardingData getOnboardingData() {
        return onboardingData;
    }

    /**
     * resetOnboarding — reinitialise les donnees d'onboarding aux valeurs par defaut.
     * Appele avant de demarrer un nouvel onboarding pour repartir de zero.
     */
    public static void resetOnboarding() {
        onboardingData.heightCm = 170f;
        onboardingData.weightKg = 75f;
        onboardingData.gender = null;
        onboardingData.objectiveName = null;
    }

    /**
     * OnboardingData — classe interne pour stocker les donnees d'onboarding.
     *
     * L'onboarding est le processus de configuration du profil apres l'inscription.
     * L'utilisateur saisit sa taille, son poids, son genre et son objectif
     * sur plusieurs ecrans successifs. Ces donnees sont stockees ici temporairement
     * puis inserees en base a la fin du processus.
     *
     * Flux :
     *   SignUpController → HeightView → WeightView → GenderView → ObjectiveView
     *   Chaque controleur lit/ecrit dans AppSession.getOnboardingData()
     */
    public static final class OnboardingData {

        /** Taille en centimetres — valeur par defaut : 170 cm */
        private float heightCm = 170f;

        /** Poids en kilogrammes — valeur par defaut : 75 kg */
        private float weightKg = 75f;

        /** Genre de l'utilisateur : "male", "female" ou null si non renseigne */
        private String gender;

        /** Objectif sportif : "perte de poids", "prise de masse", etc. ou null */
        private String objectiveName;

        public float getHeightCm() { return heightCm; }
        public void setHeightCm(float heightCm) { this.heightCm = heightCm; }

        public float getWeightKg() { return weightKg; }
        public void setWeightKg(float weightKg) { this.weightKg = weightKg; }

        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }

        public String getObjectiveName() { return objectiveName; }
        public void setObjectiveName(String objectiveName) { this.objectiveName = objectiveName; }
    }
}
