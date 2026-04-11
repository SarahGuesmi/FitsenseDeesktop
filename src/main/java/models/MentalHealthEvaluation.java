package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * One mental health self-assessment row (persisted in {@code mental_health_evaluation}).
 */
public class MentalHealthEvaluation {

    private final UUID id;
    /** Owning member (app_user.id); set when persisting or loading from DB. */
    private UUID userId;
    private UUID coachTestId;
    private String coachTestTitle;
    /** Ordered answers (1–5) for each question; empty for legacy rows. */
    private final List<Integer> questionScores = new ArrayList<>();
    /** Prompts copied at save time so edits work if the coach test is removed or changed. */
    private final List<String> questionPrompts = new ArrayList<>();
    private LocalDateTime testedAt;
    private int mood;
    private int stress;
    private int sleep;
    private int motivation;
    private int mentalTired;
    private int score;
    private String status;
    private String notes;

    public MentalHealthEvaluation(UUID id) {
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

    public UUID getCoachTestId() {
        return coachTestId;
    }

    public void setCoachTestId(UUID coachTestId) {
        this.coachTestId = coachTestId;
    }

    public String getCoachTestTitle() {
        return coachTestTitle;
    }

    public void setCoachTestTitle(String coachTestTitle) {
        this.coachTestTitle = coachTestTitle;
    }

    public List<Integer> getQuestionScores() {
        return questionScores;
    }

    public void setQuestionScores(List<Integer> scores) {
        questionScores.clear();
        if (scores != null) {
            questionScores.addAll(scores);
        }
    }

    public List<String> getQuestionPrompts() {
        return Collections.unmodifiableList(questionPrompts);
    }

    public void setQuestionPrompts(List<String> prompts) {
        questionPrompts.clear();
        if (prompts != null) {
            questionPrompts.addAll(prompts);
        }
    }

    /** Display title in history table; legacy rows without a coach test. */
    public String getTestTitleForDisplay() {
        if (coachTestTitle != null && !coachTestTitle.isBlank()) {
            return coachTestTitle;
        }
        return "—";
    }

    /** Max points for this row (for score display and bands). */
    public int getMaxScoreForDisplay() {
        if (!questionScores.isEmpty()) {
            return questionScores.size() * 5;
        }
        return 25;
    }

    public LocalDateTime getTestedAt() {
        return testedAt;
    }

    public void setTestedAt(LocalDateTime testedAt) {
        this.testedAt = testedAt;
    }

    public int getMood() {
        return mood;
    }

    public void setMood(int mood) {
        this.mood = mood;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
