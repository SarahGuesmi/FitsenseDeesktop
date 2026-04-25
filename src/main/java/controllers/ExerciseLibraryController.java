package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.GifProxyServer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ExerciseLibraryController {

    private static final String RAPIDAPI_KEY  = "2c19914553mshcf25f09126d1868p19781bjsn23bfb6716cf3";
    private static final String RAPIDAPI_HOST = "exercisedb.p.rapidapi.com";
    private static final String BASE_URL      = "https://exercisedb.p.rapidapi.com";
    private static final int    PAGE_SIZE     = 12;

    @FXML private TextField searchField;
    @FXML private Button resetBtn;
    @FXML private FlowPane bodyPartPane;
    @FXML private FlowPane targetPane;
    @FXML private ComboBox<String> equipmentCombo;
    @FXML private FlowPane activeFiltersPane;
    @FXML private Label countLabel;
    @FXML private HBox loadingBox;
    @FXML private VBox resultsContainer;
    @FXML private HBox paginationBox;
    @FXML private Button prevBtn;
    @FXML private Button nextBtn;
    @FXML private Label pageLabel;

    private String activeBodyPart  = null;
    private String activeTarget    = null;
    private String activeEquipment = null;
    private String activeSearch    = null;
    private int    currentPage     = 0;

    @FXML
    private void initialize() {
        loadFilters();
        loadExercises();
    }

    // ── Load filter chips ──────────────────────────────────
    private void loadFilters() {
        new Thread(() -> {
            try {
                JSONArray bodyParts  = fetchArray("/exercises/bodyPartList");
                JSONArray targets    = fetchArray("/exercises/targetList");
                JSONArray equipments = fetchArray("/exercises/equipmentList");

                Platform.runLater(() -> {
                    // Body part chips
                    bodyPartPane.getChildren().clear();
                    for (int i = 0; i < bodyParts.length(); i++) {
                        String val = bodyParts.getString(i);
                        Label chip = buildChip(val, "body");
                        chip.setOnMouseClicked(ev -> onBodyPartClick(val, chip));
                        bodyPartPane.getChildren().add(chip);
                    }

                    // Target chips
                    targetPane.getChildren().clear();
                    for (int i = 0; i < targets.length(); i++) {
                        String val = targets.getString(i);
                        Label chip = buildChip(val, "target");
                        chip.setOnMouseClicked(ev -> onTargetClick(val, chip));
                        targetPane.getChildren().add(chip);
                    }

                    // Equipment combo
                    equipmentCombo.getItems().clear();
                    equipmentCombo.getItems().add("All equipment");
                    for (int i = 0; i < equipments.length(); i++) {
                        equipmentCombo.getItems().add(equipments.getString(i));
                    }
                    equipmentCombo.setValue("All equipment");
                });
            } catch (Exception e) {
                System.err.println("ExerciseLibrary loadFilters error: " + e.getMessage());
            }
        }).start();
    }

    // ── Load exercises ─────────────────────────────────────
    private void loadExercises() {
        setLoading(true);
        new Thread(() -> {
            try {
                JSONArray data;
                int limit  = PAGE_SIZE;
                int offset = currentPage * PAGE_SIZE;

                if (activeSearch != null && !activeSearch.isBlank()) {
                    data = fetchArray("/exercises/name/" + URLEncoder.encode(activeSearch, StandardCharsets.UTF_8)
                            + "?limit=" + limit);
                } else if (activeBodyPart != null) {
                    data = fetchArray("/exercises/bodyPart/" + URLEncoder.encode(activeBodyPart, StandardCharsets.UTF_8)
                            + "?limit=" + limit + "&offset=" + offset);
                } else if (activeTarget != null) {
                    data = fetchArray("/exercises/target/" + URLEncoder.encode(activeTarget, StandardCharsets.UTF_8)
                            + "?limit=" + limit + "&offset=" + offset);
                } else if (activeEquipment != null && !activeEquipment.equals("All equipment")) {
                    data = fetchArray("/exercises/equipment/" + URLEncoder.encode(activeEquipment, StandardCharsets.UTF_8)
                            + "?limit=" + limit + "&offset=" + offset);
                } else {
                    data = fetchArray("/exercises?limit=" + limit + "&offset=" + offset);
                }

                final JSONArray result = data;
                // Log first item to see actual JSON structure
                if (result.length() > 0) {
                    System.out.println("First exercise JSON: " + result.getJSONObject(0).toString(2));
                }
                Platform.runLater(() -> {
                    setLoading(false);
                    renderResults(result);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showError("Failed to load exercises: " + e.getMessage());
                });
            }
        }).start();
    }

    // ── Render grid ────────────────────────────────────────
    private void renderResults(JSONArray data) {
        resultsContainer.getChildren().clear();
        countLabel.setText(data.length() + " exercise(s) found");

        if (data.isEmpty()) {
            Label empty = new Label("No exercises found. Try adjusting your filters.");
            empty.setStyle("-fx-text-fill:#6B7280;-fx-font-size:14px;-fx-padding:40;");
            resultsContainer.getChildren().add(empty);
            paginationBox.setVisible(false);
            paginationBox.setManaged(false);
            return;
        }

        // Build rows of 4
        List<JSONObject> items = new ArrayList<>();
        for (int i = 0; i < data.length(); i++) items.add(data.getJSONObject(i));

        for (int i = 0; i < items.size(); i += 4) {
            HBox row = new HBox(12);
            row.setPadding(new Insets(0, 0, 12, 0));
            for (int j = i; j < Math.min(i + 4, items.size()); j++) {
                VBox card = buildExerciseCard(items.get(j));
                HBox.setHgrow(card, Priority.ALWAYS);
                row.getChildren().add(card);
            }
            // Fill empty slots
            while (row.getChildren().size() < 4) {
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                row.getChildren().add(spacer);
            }
            resultsContainer.getChildren().add(row);
        }

        // Pagination (only when no search/filter)
        boolean showPagination = activeSearch == null && activeBodyPart == null
                && activeTarget == null && activeEquipment == null;
        paginationBox.setVisible(showPagination);
        paginationBox.setManaged(showPagination);
        if (showPagination) {
            pageLabel.setText("Page " + (currentPage + 1));
            prevBtn.setDisable(currentPage == 0);
        }
    }

    private VBox buildExerciseCard(JSONObject ex) {
        String name      = ex.optString("name", "Unknown");
        String bodyPart  = ex.optString("bodyPart", "");
        String target    = ex.optString("target", "");
        String equipment = ex.optString("equipment", "");
        // v2.exercisedb.io est l'URL publique directe — pas besoin de headers RapidAPI
        String exId  = ex.optString("id", "");
        String gifUrl = ex.optString("gifUrl", "");
        if (gifUrl.isBlank() && !exId.isBlank()) {
            gifUrl = "https://v2.exercisedb.io/image/" + exId;
        }

        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:#111827;-fx-border-color:#1F2937;-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;");
        card.setOnMouseEntered(ev -> card.setStyle(
                "-fx-background-color:#1F2937;-fx-border-color:rgba(59,130,246,0.5);-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;"));
        card.setOnMouseExited(ev -> card.setStyle(
                "-fx-background-color:#111827;-fx-border-color:#1F2937;-fx-border-radius:14;-fx-background-radius:14;-fx-cursor:hand;"));

        // Image area
        StackPane imgPane = new StackPane();
        imgPane.setMinHeight(160); imgPane.setMaxHeight(160);
        imgPane.setStyle("-fx-background-color:#0D1117;-fx-background-radius:14 14 0 0;");

        // Use WebView to display animated GIF
        javafx.scene.web.WebView gifView = new javafx.scene.web.WebView();
        gifView.setPrefWidth(220); gifView.setPrefHeight(160);
        gifView.setContextMenuEnabled(false);

        if (!gifUrl.isBlank()) {
            String proxied = GifProxyServer.proxyUrl(gifUrl);
            System.out.println("GIF proxied URL: " + proxied); // debug
            String html = "<!DOCTYPE html><html><head><style>*{margin:0;padding:0;background:#0D1117;overflow:hidden;}"
                    + "body{width:220px;height:160px;display:flex;align-items:center;justify-content:center;}"
                    + "img{width:100%;height:100%;object-fit:cover;}"
                    + "</style></head><body><img src='" + proxied + "'/></body></html>";
            gifView.getEngine().loadContent(html);
        } else {
            String html = "<!DOCTYPE html><html><head><style>*{margin:0;padding:0;background:#0D1117;}"
                    + "body{width:220px;height:160px;display:flex;align-items:center;justify-content:center;"
                    + "color:#4B5563;font-family:sans-serif;font-size:11px;font-weight:700;letter-spacing:1px;}"
                    + "</style></head><body>NO PREVIEW</body></html>";
            gifView.getEngine().loadContent(html);
        }

        // Body part badge
        Label bpBadge = new Label(bodyPart.toUpperCase());
        bpBadge.setStyle("-fx-background-color:rgba(59,130,246,0.8);-fx-text-fill:white;"
                + "-fx-font-size:10px;-fx-font-weight:800;-fx-background-radius:6;-fx-padding:3 8;");
        StackPane.setAlignment(bpBadge, Pos.BOTTOM_LEFT);
        StackPane.setMargin(bpBadge, new Insets(0, 0, 8, 8));

        imgPane.getChildren().addAll(gifView, bpBadge);

        // Card body
        VBox body = new VBox(8);
        body.setPadding(new Insets(12));

        Label nameLabel = new Label(capitalize(name));
        nameLabel.setStyle("-fx-text-fill:white;-fx-font-size:13px;-fx-font-weight:700;");
        nameLabel.setWrapText(false);
        nameLabel.setMaxWidth(200);

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label targetBadge = new Label(target);
        targetBadge.setStyle("-fx-background-color:rgba(16,185,129,0.15);-fx-border-color:rgba(16,185,129,0.2);"
                + "-fx-border-radius:999;-fx-background-radius:999;-fx-text-fill:#34D399;"
                + "-fx-font-size:10px;-fx-font-weight:800;-fx-padding:2 8;");

        Label eqBadge = new Label(equipment);
        eqBadge.setStyle("-fx-background-color:rgba(139,92,246,0.15);-fx-border-color:rgba(139,92,246,0.2);"
                + "-fx-border-radius:999;-fx-background-radius:999;-fx-text-fill:#A78BFA;"
                + "-fx-font-size:10px;-fx-font-weight:800;-fx-padding:2 8;");

        badges.getChildren().addAll(targetBadge, eqBadge);

        Button detailBtn = new Button("View Details →");
        detailBtn.setMaxWidth(Double.MAX_VALUE);
        detailBtn.setStyle("-fx-background-color:#1F2937;-fx-text-fill:white;-fx-font-weight:700;"
                + "-fx-font-size:12px;-fx-background-radius:10;-fx-padding:8 0;-fx-cursor:hand;");
        detailBtn.setOnMouseEntered(ev -> detailBtn.setStyle(
                "-fx-background-color:white;-fx-text-fill:black;-fx-font-weight:700;"
                + "-fx-font-size:12px;-fx-background-radius:10;-fx-padding:8 0;-fx-cursor:hand;"));
        detailBtn.setOnMouseExited(ev -> detailBtn.setStyle(
                "-fx-background-color:#1F2937;-fx-text-fill:white;-fx-font-weight:700;"
                + "-fx-font-size:12px;-fx-background-radius:10;-fx-padding:8 0;-fx-cursor:hand;"));
        detailBtn.setOnAction(ev -> openDetail(ex));

        body.getChildren().addAll(nameLabel, badges, detailBtn);
        card.getChildren().addAll(imgPane, body);
        return card;
    }

    private void openDetail(JSONObject ex) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/ExerciseLibraryDetailView.fxml")));
            javafx.scene.Node view = loader.load();
            ExerciseLibraryDetailController ctrl = loader.getController();
            ctrl.setExercise(ex);
            DashboardController dash = DashboardController.getInstance();
            if (dash != null) dash.showLibraryView(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Filter actions ─────────────────────────────────────
    private void onBodyPartClick(String val, Label chip) {
        activeBodyPart = val.equals(activeBodyPart) ? null : val;
        activeTarget = null; activeSearch = null;
        currentPage = 0;
        refreshChipStyles(bodyPartPane, activeBodyPart, "body");
        refreshChipStyles(targetPane, null, "target");
        updateActiveFilters();
        loadExercises();
    }

    private void onTargetClick(String val, Label chip) {
        activeTarget = val.equals(activeTarget) ? null : val;
        activeBodyPart = null; activeSearch = null;
        currentPage = 0;
        refreshChipStyles(targetPane, activeTarget, "target");
        refreshChipStyles(bodyPartPane, null, "body");
        updateActiveFilters();
        loadExercises();
    }

    @FXML
    private void onEquipmentFilter() {
        String val = equipmentCombo.getValue();
        activeEquipment = (val == null || val.equals("All equipment")) ? null : val;
        activeBodyPart = null; activeTarget = null; activeSearch = null;
        currentPage = 0;
        updateActiveFilters();
        loadExercises();
    }

    @FXML
    private void onSearch() {
        activeSearch = searchField.getText() == null ? null : searchField.getText().trim();
        if (activeSearch != null && activeSearch.isBlank()) activeSearch = null;
        activeBodyPart = null; activeTarget = null; activeEquipment = null;
        currentPage = 0;
        resetBtn.setVisible(activeSearch != null);
        resetBtn.setManaged(activeSearch != null);
        refreshChipStyles(bodyPartPane, null, "body");
        refreshChipStyles(targetPane, null, "target");
        updateActiveFilters();
        loadExercises();
    }

    @FXML
    private void onReset() {
        searchField.clear();
        activeSearch = null; activeBodyPart = null; activeTarget = null; activeEquipment = null;
        currentPage = 0;
        resetBtn.setVisible(false); resetBtn.setManaged(false);
        equipmentCombo.setValue("All equipment");
        refreshChipStyles(bodyPartPane, null, "body");
        refreshChipStyles(targetPane, null, "target");
        updateActiveFilters();
        loadExercises();
    }

    @FXML private void onPrev() { if (currentPage > 0) { currentPage--; loadExercises(); } }
    @FXML private void onNext() { currentPage++; loadExercises(); }

    @FXML
    private void onBack() {
        DashboardController dash = DashboardController.getInstance();
        if (dash != null) {
            dash.showWorkoutsList();
        }
    }

    // ── Helpers ────────────────────────────────────────────
    private Label buildChip(String text, String type) {
        Label chip = new Label(text);
        chip.setStyle(chipStyle(false, type));
        chip.setCursor(javafx.scene.Cursor.HAND);
        return chip;
    }

    private String chipStyle(boolean active, String type) {
        if (active) {
            String color = type.equals("body") ? "#60A5FA" : "#34D399";
            String bg    = type.equals("body") ? "rgba(59,130,246,0.2)" : "rgba(16,185,129,0.2)";
            String border= type.equals("body") ? "rgba(96,165,250,0.6)" : "rgba(52,211,153,0.6)";
            return "-fx-background-color:" + bg + ";-fx-border-color:" + border
                    + ";-fx-border-radius:999;-fx-background-radius:999;-fx-text-fill:" + color
                    + ";-fx-font-size:11px;-fx-font-weight:700;-fx-padding:4 12;-fx-cursor:hand;";
        }
        return "-fx-background-color:rgba(255,255,255,0.05);-fx-border-color:rgba(255,255,255,0.1);"
                + "-fx-border-radius:999;-fx-background-radius:999;-fx-text-fill:#9CA3AF;"
                + "-fx-font-size:11px;-fx-font-weight:600;-fx-padding:4 12;-fx-cursor:hand;";
    }

    private void refreshChipStyles(FlowPane pane, String active, String type) {
        for (javafx.scene.Node node : pane.getChildren()) {
            if (node instanceof Label chip) {
                boolean isActive = chip.getText().equals(active);
                chip.setStyle(chipStyle(isActive, type));
            }
        }
    }

    private void updateActiveFilters() {
        activeFiltersPane.getChildren().clear();
        if (activeBodyPart != null)  addActiveBadge(activeBodyPart, "#60A5FA", "rgba(59,130,246,0.2)");
        if (activeTarget != null)    addActiveBadge(activeTarget, "#34D399", "rgba(16,185,129,0.2)");
        if (activeEquipment != null) addActiveBadge(activeEquipment, "#A78BFA", "rgba(139,92,246,0.2)");
        if (activeSearch != null)    addActiveBadge("\"" + activeSearch + "\"", "#FCD34D", "rgba(245,158,11,0.2)");
    }

    private void addActiveBadge(String text, String color, String bg) {
        Label badge = new Label(text);
        badge.setStyle("-fx-background-color:" + bg + ";-fx-border-color:" + color
                + ";-fx-border-radius:999;-fx-background-radius:999;-fx-text-fill:" + color
                + ";-fx-font-size:11px;-fx-font-weight:700;-fx-padding:3 10;");
        activeFiltersPane.getChildren().add(badge);
    }

    private void setLoading(boolean loading) {
        loadingBox.setVisible(loading);
        loadingBox.setManaged(loading);
        resultsContainer.setVisible(!loading);
        resultsContainer.setManaged(!loading);
    }

    private void showError(String msg) {
        resultsContainer.getChildren().clear();
        Label err = new Label("⚠ " + msg);
        err.setStyle("-fx-text-fill:#F87171;-fx-font-size:13px;-fx-padding:20;");
        resultsContainer.getChildren().add(err);
    }

    private JSONArray fetchArray(String path) throws Exception {
        String url = BASE_URL + path;
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(java.net.http.HttpClient.Redirect.ALWAYS)
                .build();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-RapidAPI-Key", RAPIDAPI_KEY)
                .header("X-RapidAPI-Host", RAPIDAPI_HOST)
                .header("User-Agent", "Mozilla/5.0")
                .GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("API response status: " + resp.statusCode() + " for " + url);
        String body = resp.body();
        // Log first 300 chars to see structure
        System.out.println("API response preview: " + body.substring(0, Math.min(300, body.length())));
        return new JSONArray(body);
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
