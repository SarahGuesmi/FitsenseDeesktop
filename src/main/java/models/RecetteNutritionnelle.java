package models;

import java.time.LocalDateTime;
import java.util.List;

public class RecetteNutritionnelle {

    private String id;
    private String title;
    private String description;
    private Integer kcal;
    private Integer proteins;
    private String typeMeal;
    private String ingredients;
    private String preparation;
    private String image;
    private LocalDateTime createdAt;
    private String coachId;
    private List<String> objectifs;

    public RecetteNutritionnelle() {
    }

    public RecetteNutritionnelle(String title, String description, Integer kcal, Integer proteins,
                                 String typeMeal, String ingredients, String preparation, String image) {
        this.title = title;
        this.description = description;
        this.kcal = kcal;
        this.proteins = proteins;
        this.typeMeal = typeMeal;
        this.ingredients = ingredients;
        this.preparation = preparation;
        this.image = image;
    }

    public RecetteNutritionnelle(String id, String title, String description, Integer kcal, Integer proteins,
                                 String typeMeal, String ingredients, String preparation, String image,
                                 LocalDateTime createdAt, String coachId, List<String> objectifs) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.kcal = kcal;
        this.proteins = proteins;
        this.typeMeal = typeMeal;
        this.ingredients = ingredients;
        this.preparation = preparation;
        this.image = image;
        this.createdAt = createdAt;
        this.coachId = coachId;
        this.objectifs = objectifs;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getKcal() {
        return kcal;
    }

    public void setKcal(Integer kcal) {
        this.kcal = kcal;
    }

    public Integer getProteins() {
        return proteins;
    }

    public void setProteins(Integer proteins) {
        this.proteins = proteins;
    }

    public String getTypeMeal() {
        return typeMeal;
    }

    public void setTypeMeal(String typeMeal) {
        this.typeMeal = typeMeal;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getPreparation() {
        return preparation;
    }

    public void setPreparation(String preparation) {
        this.preparation = preparation;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCoachId() {
        return coachId;
    }

    public void setCoachId(String coachId) {
        this.coachId = coachId;
    }

    public List<String> getObjectifs() {
        return objectifs;
    }

    public void setObjectifs(List<String> objectifs) {
        this.objectifs = objectifs;
    }

    @Override
    public String toString() {
        return "RecetteNutritionnelle{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", kcal=" + kcal +
                ", proteins=" + proteins +
                ", typeMeal='" + typeMeal + '\'' +
                ", ingredients='" + ingredients + '\'' +
                ", preparation='" + preparation + '\'' +
                ", image='" + image + '\'' +
                ", createdAt=" + createdAt +
                ", coachId='" + coachId + '\'' +
                ", objectifs=" + objectifs +
                '}';
    }
}