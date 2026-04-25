package models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Workout {

    private Integer id;
    private UUID uuid; // BINARY(16) primary key in DB
    private UUID coachId;
    private String nom;
    private String niveau;
    private Integer duree;
    private String description;
    private String status;

    // 🔗 Relations
    private List<Exercise> exercises = new ArrayList<>();
    private List<ObjectifSportif> objectifs = new ArrayList<>();

    // 🔧 Constructor
    public Workout() {}

    // ================= GETTERS & SETTERS =================

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public UUID getCoachId() { return coachId; }
    public void setCoachId(UUID coachId) { this.coachId = coachId; }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public Integer getDuree() {
        return duree;
    }

    public void setDuree(Integer duree) {
        this.duree = duree;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ================= EXERCISES =================

    public List<Exercise> getExercises() {
        return exercises;
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
    }

    public void addExercise(Exercise exercise) {
        if (!exercises.contains(exercise)) {
            exercises.add(exercise);
        }
    }

    public void removeExercise(Exercise exercise) {
        exercises.remove(exercise);
    }

    // ================= OBJECTIFS =================

    public List<ObjectifSportif> getObjectifs() {
        return objectifs;
    }

    public void setObjectifs(List<ObjectifSportif> objectifs) {
        this.objectifs = objectifs;
    }

    public void addObjectif(ObjectifSportif objectif) {
        if (!objectifs.contains(objectif)) {
            objectifs.add(objectif);
        }
    }

    public void removeObjectif(ObjectifSportif objectif) {
        objectifs.remove(objectif);
    }
}
