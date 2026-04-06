package models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Questionnaire {

    private Integer id;
    private User user;
    private List<Workout> workouts = new ArrayList<>();
    private Integer noteGlobale;
    private Integer satisfaction;
    private String intensite;
    private String exercicesCompris;
    private String duree;
    private String ressentiPhysique;
    private String stress;
    private String motivation;
    private String progression;
    private Integer rapprocheObjectifs;
    private String commentaire;
    private String titre;
    private String options;
    private User coach;
    private String type = "response";
    private Instant dateSoumission;
    private String userName;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public List<Workout> getWorkouts() { return workouts; }
    public void addWorkout(Workout workout) {
        if (!workouts.contains(workout)) workouts.add(workout);
    }
    public void removeWorkout(Workout workout) { workouts.remove(workout); }

    public Integer getNoteGlobale() { return noteGlobale; }
    public void setNoteGlobale(Integer noteGlobale) { this.noteGlobale = noteGlobale; }

    public Integer getSatisfaction() { return satisfaction; }
    public void setSatisfaction(Integer satisfaction) { this.satisfaction = satisfaction; }

    public String getIntensite() { return intensite; }
    public void setIntensite(String intensite) { this.intensite = intensite; }

    public String getExercicesCompris() { return exercicesCompris; }
    public void setExercicesCompris(String exercicesCompris) { this.exercicesCompris = exercicesCompris; }

    public String getDuree() { return duree; }
    public void setDuree(String duree) { this.duree = duree; }

    public String getRessentiPhysique() { return ressentiPhysique; }
    public void setRessentiPhysique(String ressentiPhysique) { this.ressentiPhysique = ressentiPhysique; }

    public String getStress() { return stress; }
    public void setStress(String stress) { this.stress = stress; }

    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }

    public String getProgression() { return progression; }
    public void setProgression(String progression) { this.progression = progression; }

    public Integer getRapprocheObjectifs() { return rapprocheObjectifs; }
    public void setRapprocheObjectifs(Integer rapprocheObjectifs) { this.rapprocheObjectifs = rapprocheObjectifs; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }

    public User getCoach() { return coach; }
    public void setCoach(User coach) { this.coach = coach; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Instant getDateSoumission() { return dateSoumission; }
    public void setDateSoumission(Instant dateSoumission) { this.dateSoumission = dateSoumission; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    @Override
    public String toString() {
        return "Questionnaire{id=" + id + ", titre='" + titre + "', type='" + type + "', user=" + user + "}";
    }
}
