package models;

import java.util.ArrayList;
import java.util.List;

public class Workout {

    private Integer id;
    private User coach;
    private String nom;
    private String niveau;
    private Integer duree;
    private String description;
    private String status;
    private List<Workout> exercises = new ArrayList<>();
    private List<Workout> objectifs = new ArrayList<>();

    public Workout() {}

    public Workout(Integer id, String nom, String niveau, Integer duree, String description, String status, User coach) {
        this.id = id;
        this.nom = nom;
        this.niveau = niveau;
        this.duree = duree;
        this.description = description;
        this.status = status;
        this.coach = coach;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getCoach() { return coach; }
    public void setCoach(User coach) { this.coach = coach; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    // getName() alias for compatibility
    public String getName() { return nom; }
    public void setName(String name) { this.nom = name; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public Integer getDuree() { return duree; }
    public void setDuree(Integer duree) { this.duree = duree; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Workout{id=" + id + ", nom='" + nom + "', niveau='" + niveau + "', duree=" + duree + "}";
    }
}
