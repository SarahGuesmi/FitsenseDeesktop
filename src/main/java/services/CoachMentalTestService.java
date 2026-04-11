package services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import models.CoachMentalTest;

import java.util.Optional;
import java.util.UUID;

/**
 * Coach mental tests: kept in memory and persisted to disk so they survive restarts.
 * Entries are removed only via {@link #remove(UUID)} (coach delete).
 */
public final class CoachMentalTestService {

    private static final CoachMentalTestService INSTANCE = new CoachMentalTestService();

    private final ObservableList<CoachMentalTest> tests = FXCollections.observableArrayList();

    private CoachMentalTestService() {
        CoachMentalTestPersistence.load(tests);
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
        tests.removeIf(t -> t.getId().equals(test.getId()));
        tests.add(0, test);
        persist();
    }

    public void remove(UUID id) {
        tests.removeIf(t -> t.getId().equals(id));
        persist();
    }

    private void persist() {
        CoachMentalTestPersistence.save(tests);
    }
}
