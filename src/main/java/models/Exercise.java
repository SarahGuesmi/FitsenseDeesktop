package models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Exercise {

    private UUID id;
    private String nom;
    private String type;
    private Integer duree;
    private String description;
    private Integer sets;
    private Integer reps;
    private String imageName;
    private byte[] imageData;
    private LocalDateTime updatedAt;
    private String youtubeVideoId;

    private List<Workout> workouts = new ArrayList<>();

    public Exercise() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getDuree() { return duree; }
    public void setDuree(Integer duree) { this.duree = duree; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSets() { return sets; }
    public void setSets(Integer sets) { this.sets = sets; }

    public Integer getReps() { return reps; }
    public void setReps(Integer reps) { this.reps = reps; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getYoutubeVideoId() { return youtubeVideoId; }
    public void setYoutubeVideoId(String youtubeVideoId) { this.youtubeVideoId = youtubeVideoId; }

    public List<Workout> getWorkouts() { return workouts; }
    public void setWorkouts(List<Workout> workouts) { this.workouts = workouts; }

    public void addWorkout(Workout workout) {
        if (!workouts.contains(workout)) workouts.add(workout);
    }
    public void removeWorkout(Workout workout) { workouts.remove(workout); }
}
