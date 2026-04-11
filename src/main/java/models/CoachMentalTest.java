package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Coach-authored mental wellbeing test: title + ordered questions.
 * Scoring (for members): each answer is 1–5; total score = sum; max = 5 × number of questions.
 */
public class CoachMentalTest {

    private final UUID id;
    private String title;
    private final List<CoachMentalTestQuestion> questions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CoachMentalTest(UUID id) {
        this.id = id;
        this.questions = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<CoachMentalTestQuestion> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    public void clearQuestions() {
        questions.clear();
    }

    public void addQuestion(CoachMentalTestQuestion q) {
        questions.add(q);
    }

    public int getQuestionCount() {
        return questions.size();
    }

    public int getMaxScore() {
        return questions.size() * 5;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
