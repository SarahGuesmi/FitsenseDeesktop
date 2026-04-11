package models;

import java.util.UUID;

/**
 * One question on a coach-defined mental test. Members answer 1–5 each; score = sum across questions.
 */
public class CoachMentalTestQuestion {

    private final UUID id;
    private int orderIndex;
    private String prompt;

    public CoachMentalTestQuestion(UUID id, int orderIndex, String prompt) {
        this.id = id;
        this.orderIndex = orderIndex;
        this.prompt = prompt;
    }

    public UUID getId() {
        return id;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
