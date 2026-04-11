package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Snapshot of a member mental check-in visible to coaches (in-memory feed until DB).
 */
public class MentalHealthAssessmentSubmission {

    private final UUID id;
    private UUID userId;
    private String userFullName;
    private String userEmail;
    private LocalDateTime testedAt;
    private int stress;
    private int sleep;
    private int mood;
    private int motivation;
    private int mentalTired;
    private int score;
    private String status;
    private String memberNotes;
    private String coachRecommendation;
    /** Coach-authored test title, if any. */
    private String coachTestTitle;
    /** Structured exercises suggested for this assessment. */
    private final List<RecommendedExercise> recommendedExercises = new ArrayList<>();
    /** Optional overall note from the coach (separate from legacy {@link #coachRecommendation}). */
    private String recommendationGeneralNote;

    public MentalHealthAssessmentSubmission(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public LocalDateTime getTestedAt() {
        return testedAt;
    }

    public void setTestedAt(LocalDateTime testedAt) {
        this.testedAt = testedAt;
    }

    public int getStress() {
        return stress;
    }

    public void setStress(int stress) {
        this.stress = stress;
    }

    public int getSleep() {
        return sleep;
    }

    public void setSleep(int sleep) {
        this.sleep = sleep;
    }

    public int getMood() {
        return mood;
    }

    public void setMood(int mood) {
        this.mood = mood;
    }

    public int getMotivation() {
        return motivation;
    }

    public void setMotivation(int motivation) {
        this.motivation = motivation;
    }

    public int getMentalTired() {
        return mentalTired;
    }

    public void setMentalTired(int mentalTired) {
        this.mentalTired = mentalTired;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMemberNotes() {
        return memberNotes;
    }

    public void setMemberNotes(String memberNotes) {
        this.memberNotes = memberNotes;
    }

    public String getCoachRecommendation() {
        return coachRecommendation;
    }

    public void setCoachRecommendation(String coachRecommendation) {
        this.coachRecommendation = coachRecommendation;
    }

    public String getCoachTestTitle() {
        return coachTestTitle;
    }

    public void setCoachTestTitle(String coachTestTitle) {
        this.coachTestTitle = coachTestTitle;
    }

    public List<RecommendedExercise> getRecommendedExercises() {
        return recommendedExercises;
    }

    public void clearRecommendedExercises() {
        recommendedExercises.clear();
    }

    public void setRecommendedExercises(List<RecommendedExercise> list) {
        recommendedExercises.clear();
        if (list != null) {
            for (RecommendedExercise e : list) {
                if (e != null) {
                    recommendedExercises.add(e.copy());
                }
            }
        }
    }

    public String getRecommendationGeneralNote() {
        return recommendationGeneralNote;
    }

    public void setRecommendationGeneralNote(String recommendationGeneralNote) {
        this.recommendationGeneralNote = recommendationGeneralNote;
    }

    /** True if the coach left any recommendation (structured, note, or legacy text). */
    public boolean hasRecommendationContent() {
        if (recommendationGeneralNote != null && !recommendationGeneralNote.isBlank()) {
            return true;
        }
        for (RecommendedExercise e : recommendedExercises) {
            if (e != null && e.hasContent()) {
                return true;
            }
        }
        return coachRecommendation != null && !coachRecommendation.isBlank();
    }

    public void clearRecommendationContent() {
        recommendedExercises.clear();
        recommendationGeneralNote = null;
        coachRecommendation = null;
    }

    /** Short line for manage table / summaries. */
    public String getRecommendationPreview() {
        if (recommendationGeneralNote != null && !recommendationGeneralNote.isBlank()) {
            String t = recommendationGeneralNote.trim();
            return t.length() > 80 ? t.substring(0, 77) + "…" : t;
        }
        for (RecommendedExercise e : recommendedExercises) {
            if (e != null && !e.getName().isBlank()) {
                return e.getName();
            }
        }
        if (coachRecommendation != null && !coachRecommendation.isBlank()) {
            String t = coachRecommendation.trim();
            return t.length() > 80 ? t.substring(0, 77) + "…" : t;
        }
        return "—";
    }
}
