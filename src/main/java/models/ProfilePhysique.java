package models;

import java.util.UUID;

public class ProfilePhysique {
    private UUID id;
    private Float weight;
    private Float height;
    private String gender;
    private UUID userId;

    public ProfilePhysique() {
    }

    public ProfilePhysique(UUID id, Float weight, Float height, String gender, UUID userId) {
        this.id = id;
        this.weight = weight;
        this.height = height;
        this.gender = gender;
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "ProfilePhysique{" + "id=" + id + ", weight=" + weight + ", height=" + height + ", gender='" + gender
                + '\'' + ", userId=" + userId + '}';
    }
}
