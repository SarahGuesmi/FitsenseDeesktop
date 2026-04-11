package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CoachLatestAssessmentsFragmentController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private TableView<AssessmentDTO> assessmentsTable;
    @FXML
    private TableColumn<AssessmentDTO, String> colUser;
    @FXML
    private TableColumn<AssessmentDTO, String> colDate;
    @FXML
    private TableColumn<AssessmentDTO, String> colStress;
    @FXML
    private TableColumn<AssessmentDTO, String> colSleep;
    @FXML
    private TableColumn<AssessmentDTO, String> colMood;
    @FXML
    private TableColumn<AssessmentDTO, String> colMotivation;
    @FXML
    private TableColumn<AssessmentDTO, String> colScore;
    @FXML
    private TableColumn<AssessmentDTO, String> colStatus;
    @FXML
    private TableColumn<AssessmentDTO, Void> colAction;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ObservableList<AssessmentDTO> masterData = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList("All Statuses", "Good", "Moderate", "Low"));
        statusCombo.getSelectionModel().selectFirst();

        setupColumns();
        loadDummyData();

        // Reactive Filtering
        FilteredList<AssessmentDTO> filteredData = new FilteredList<>(masterData, p -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(dto -> applyFilter(dto, newValue, statusCombo.getValue()));
        });

        statusCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(dto -> applyFilter(dto, searchField.getText(), newValue));
        });

        assessmentsTable.setItems(filteredData);
    }

    private boolean applyFilter(AssessmentDTO dto, String searchText, String statusFilter) {
        // 1. Status Filter
        if (statusFilter != null && !"All Statuses".equals(statusFilter)) {
            if (!dto.getStatus().equalsIgnoreCase(statusFilter)) {
                return false;
            }
        }

        // 2. Search Text Filter
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }

        String lowerCaseFilter = searchText.toLowerCase();
        return dto.getUserName().toLowerCase().contains(lowerCaseFilter) ||
               dto.getRole().toLowerCase().contains(lowerCaseFilter);
    }

    private void setupColumns() {
        // (Existing setupColumns content)
        // ...
    }

    private void loadDummyData() {
        masterData.setAll(
                new AssessmentDTO("omar guesmi", "Athlete", LocalDateTime.of(2026, 3, 5, 6, 1), 3, 4, 2, 3, 15, "Moderate"),
                new AssessmentDTO("omar guesmi", "Athlete", LocalDateTime.of(2026, 3, 5, 5, 58), 3, 2, 3, 2, 13, "Moderate"),
                new AssessmentDTO("omar guesmi", "Athlete", LocalDateTime.of(2026, 3, 5, 5, 57), 2, 2, 1, 1, 8, "Low"),
                new AssessmentDTO("sarah guesmi", "Athlete", LocalDateTime.of(2026, 3, 6, 10, 0), 5, 5, 5, 5, 25, "Good")
        );
    }

    public static class AssessmentDTO {
        private final String userName;
        private final String role;
        private final LocalDateTime testedAt;
        private final int stress;
        private final int sleep;
        private final int mood;
        private final int motivation;
        private final int score;
        private final String status;

        public AssessmentDTO(String userName, String role, LocalDateTime testedAt, int stress, int sleep, int mood, int motivation, int score, String status) {
            this.userName = userName;
            this.role = role;
            this.testedAt = testedAt;
            this.stress = stress;
            this.sleep = sleep;
            this.mood = mood;
            this.motivation = motivation;
            this.score = score;
            this.status = status;
        }

        public String getUserName() { return userName; }
        public String getRole() { return role; }
        public LocalDateTime getTestedAt() { return testedAt; }
        public int getStress() { return stress; }
        public int getSleep() { return sleep; }
        public int getMood() { return mood; }
        public int getMotivation() { return motivation; }
        public int getScore() { return score; }
        public String getStatus() { return status; }

        public String getInitials() {
            if (userName == null || userName.isBlank()) return "??";
            String[] parts = userName.split(" ");
            if (parts.length >= 2) {
                return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
            }
            return (userName.charAt(0) + "").toUpperCase();
        }
    }
}
