package models;

public class Workout {

    private Integer id;
    private String name;

    public Workout() {}

    public Workout(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "Workout{id=" + id + ", name='" + name + "'}";
    }
}
