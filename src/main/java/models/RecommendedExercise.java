package models;

/**
 * One exercise suggested by a coach as part of a mental-health recommendation.
 */
public class RecommendedExercise {

    private String name = "";
    private String durationMinutes = "";
    private String description = "";

    public RecommendedExercise() {
    }

    public RecommendedExercise(String name, String durationMinutes, String description) {
        this.name = name == null ? "" : name;
        this.durationMinutes = durationMinutes == null ? "" : durationMinutes;
        this.description = description == null ? "" : description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name;
    }

    public String getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(String durationMinutes) {
        this.durationMinutes = durationMinutes == null ? "" : durationMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public boolean hasContent() {
        return !name.isBlank() || !durationMinutes.isBlank() || !description.isBlank();
    }

    public RecommendedExercise copy() {
        return new RecommendedExercise(name, durationMinutes, description);
    }
}
