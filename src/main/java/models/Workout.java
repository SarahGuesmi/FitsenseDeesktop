package models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Workout {

    private UUID id;
    private String nom;
    private String niveau;
    private Integer duree;
    private String description;
    private String status;

    private List<Exercise> exercises = new ArrayList<>();
    private List<ObjectifSportif> objectifs = new ArrayList<>();

    public Workout() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public Integer getDuree() { return duree; }
    public void setDuree(Integer duree) { this.duree = duree; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Exercise> getExercises() { return exercises; }
    public void setExercises(List<Exercise> exercises) { this.exercises = exercises; }

    public void addExercise(Exercise exercise) {
        if (!exercises.contains(exercise)) exercises.add(exercise);
    }
    public void removeExercise(Exercise exercise) { exercises.remove(exercise); }

    public List<ObjectifSportif> getObjectifs() { return objectifs; }
    public void setObjectifs(List<ObjectifSportif> objectifs) { this.objectifs = objectifs; }

    public void addObjectif(ObjectifSportif objectif) {
        if (!objectifs.contains(objectif)) objectifs.add(objectif);
    }
    public void removeObjectif(ObjectifSportif objectif) { objectifs.remove(objectif); }
}
