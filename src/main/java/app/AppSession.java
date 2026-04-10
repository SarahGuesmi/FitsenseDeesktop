package app;

import models.User;

/**
 * Small in-memory session shared by JavaFX controllers.
 */
public final class AppSession {

    private static User currentUser;
    private static final OnboardingData onboardingData = new OnboardingData();

    private AppSession() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static OnboardingData getOnboardingData() {
        return onboardingData;
    }

    public static void resetOnboarding() {
        onboardingData.heightCm = 170f;
        onboardingData.weightKg = 75f;
        onboardingData.gender = null;
        onboardingData.objectiveName = null;
    }

    public static final class OnboardingData {
        private float heightCm = 170f;
        private float weightKg = 75f;
        private String gender;
        private String objectiveName;

        public float getHeightCm() {
            return heightCm;
        }

        public void setHeightCm(float heightCm) {
            this.heightCm = heightCm;
        }

        public float getWeightKg() {
            return weightKg;
        }

        public void setWeightKg(float weightKg) {
            this.weightKg = weightKg;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getObjectiveName() {
            return objectiveName;
        }

        public void setObjectiveName(String objectiveName) {
            this.objectiveName = objectiveName;
        }
    }
}
