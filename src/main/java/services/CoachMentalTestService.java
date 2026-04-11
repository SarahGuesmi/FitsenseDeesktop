package services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.CoachMentalTest;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Coach mental tests: persisted in MySQL.
 */
public final class CoachMentalTestService {

    private static final CoachMentalTestService INSTANCE = new CoachMentalTestService();

    private final ObservableList<CoachMentalTest> tests = FXCollections.observableArrayList();
    private final CoachMentalTestRepository repository = new CoachMentalTestRepository();

    private CoachMentalTestService() {
        try {
            tests.setAll(repository.findAll());
        } catch (SQLException e) {
            System.err.println("CoachMentalTestService load failed: " + e.getMessage());
        }
    }

    public static CoachMentalTestService getInstance() {
        return INSTANCE;
    }

    public ObservableList<CoachMentalTest> getTests() {
        return tests;
    }

    public Optional<CoachMentalTest> findById(UUID id) {
        return tests.stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    public void add(CoachMentalTest test) {
        try {
            repository.save(test);
            tests.removeIf(t -> t.getId().equals(test.getId()));
            tests.add(0, test);
        } catch (SQLException e) {
            System.err.println("CoachMentalTestService save failed: " + e.getMessage());
        }
    }

    public void remove(UUID id) {
        try {
            repository.delete(id);
            tests.removeIf(t -> t.getId().equals(id));
        } catch (SQLException e) {
            System.err.println("CoachMentalTestService delete failed: " + e.getMessage());
        }
    }
}
