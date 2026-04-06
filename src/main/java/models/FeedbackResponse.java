package models;

import java.time.Instant;

public class FeedbackResponse {

    private Integer id;
    private User user;
    private Workout workout;
    private String rating;
    private String comment;
    private Instant createdAt;
    private User coach;
    private String sentiment;
    private String keywords;
    private String aiSummary;

    public FeedbackResponse() {
        this.createdAt = Instant.now();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Workout getWorkout() { return workout; }
    public void setWorkout(Workout workout) { this.workout = workout; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public User getCoach() { return coach; }
    public void setCoach(User coach) { this.coach = coach; }

    public String getUserName() {
        String firstname = user != null ? user.getFirstname() : "";
        String lastname = user != null ? user.getLastname() : "";
        return (firstname + " " + lastname).trim();
    }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    @Override
    public String toString() {
        return "FeedbackResponse{id=" + id + ", rating='" + rating + "', sentiment='" + sentiment + "', user=" + user + "}";
    }
}
