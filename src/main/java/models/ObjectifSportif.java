package models;

import java.util.UUID;

public class ObjectifSportif {
    private UUID id;
    private String name;
    private UUID profilePhysiqueId;

    public ObjectifSportif() {
    }

    public ObjectifSportif(UUID id, String name, UUID profilePhysiqueId) {
        this.id = id;
        this.name = name;
        this.profilePhysiqueId = profilePhysiqueId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getProfilePhysiqueId() {
        return profilePhysiqueId;
    }

    public void setProfilePhysiqueId(UUID profilePhysiqueId) {
        this.profilePhysiqueId = profilePhysiqueId;
    }

    @Override
    public String toString() {
        return "ObjectifSportif{" + "id=" + id + ", name='" + name + '\'' + ", profilePhysiqueId=" + profilePhysiqueId
                + '}';
    }
}
