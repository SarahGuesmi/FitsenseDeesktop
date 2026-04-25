package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImagePickerController {

    private static final String PIDEVASSETS_URL  = "http://localhost/pidevassets/";
    private static final String PIDEVASSETS_PATH = "C:/xampp/htdocs/pidevassets";

    @FXML private TextField searchField;
    @FXML private Label loadingLabel;
    @FXML private FlowPane imageGrid;
    @FXML private HBox selectionBar;
    @FXML private ImageView selectedPreview;
    @FXML private Label selectedNameLabel;

    private List<String> allFiles = new ArrayList<>();
    private String selectedFilename = null;
    private Consumer<String> onSelectCallback;
    private Stage stage;

    public void init(Stage stage, Consumer<String> onSelect) {
        this.stage = stage;
        this.onSelectCallback = onSelect;
        loadImages();
    }

    private void loadImages() {
        loadingLabel.setVisible(true);
        loadingLabel.setManaged(true);
        imageGrid.getChildren().clear();

        new Thread(() -> {
            try {
                // Fetch directory listing from Apache
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(PIDEVASSETS_URL))
                        .GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

                // Parse Apache directory listing HTML for image files
                List<String> files = parseApacheListing(resp.body());

                Platform.runLater(() -> {
                    allFiles = files;
                    loadingLabel.setVisible(false);
                    loadingLabel.setManaged(false);
                    renderGrid(files);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingLabel.setText("⚠ Could not load: " + e.getMessage());
                });
            }
        }).start();
    }

    private List<String> parseApacheListing(String html) {
        List<String> files = new ArrayList<>();
        // Match href="filename.ext" for image files
        Pattern p = Pattern.compile("href=\"([^\"]+\\.(png|jpg|jpeg|gif|webp))\"",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        while (m.find()) {
            String name = m.group(1);
            if (!name.startsWith("?") && !name.startsWith("/")) {
                files.add(name);
            }
        }
        return files;
    }

    private void renderGrid(List<String> files) {
        imageGrid.getChildren().clear();
        if (files.isEmpty()) {
            Label empty = new Label("No images found. Upload one first.");
            empty.setStyle("-fx-text-fill:#6B7280;-fx-font-size:13px;-fx-padding:20;");
            imageGrid.getChildren().add(empty);
            return;
        }

        String filter = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        for (String filename : files) {
            if (!filter.isEmpty() && !filename.toLowerCase().contains(filter)) continue;

            String url = PIDEVASSETS_URL + filename;

            StackPane card = new StackPane();
            card.setStyle("-fx-background-color:#1F2937;-fx-border-color:#374151;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
            card.setPrefWidth(140); card.setPrefHeight(120);
            card.setMaxWidth(140); card.setMaxHeight(120);

            ImageView img = new ImageView();
            img.setFitWidth(138); img.setFitHeight(100);
            img.setPreserveRatio(true);
            img.setSmooth(true);
            try {
                img.setImage(new Image(url, 138, 100, true, true, true));
            } catch (Exception ignored) {}

            Label nameLbl = new Label(filename.length() > 18 ? filename.substring(0, 15) + "..." : filename);
            nameLbl.setStyle("-fx-text-fill:#9CA3AF;-fx-font-size:10px;-fx-background-color:rgba(0,0,0,0.6);-fx-padding:2 6;");
            StackPane.setAlignment(nameLbl, javafx.geometry.Pos.BOTTOM_CENTER);

            card.getChildren().addAll(img, nameLbl);

            // Hover
            card.setOnMouseEntered(ev -> card.setStyle(
                    "-fx-background-color:#374151;-fx-border-color:#22C55E;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;"));
            card.setOnMouseExited(ev -> {
                boolean sel = filename.equals(selectedFilename);
                card.setStyle(sel
                        ? "-fx-background-color:rgba(34,197,94,0.15);-fx-border-color:#22C55E;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;"
                        : "-fx-background-color:#1F2937;-fx-border-color:#374151;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
            });

            card.setOnMouseClicked(ev -> selectImage(filename, url, card));
            imageGrid.getChildren().add(card);
        }
    }

    private void selectImage(String filename, String url, StackPane card) {
        selectedFilename = filename;

        // Reset all cards
        for (javafx.scene.Node node : imageGrid.getChildren()) {
            node.setStyle("-fx-background-color:#1F2937;-fx-border-color:#374151;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");
        }
        card.setStyle("-fx-background-color:rgba(34,197,94,0.15);-fx-border-color:#22C55E;-fx-border-radius:10;-fx-background-radius:10;-fx-cursor:hand;");

        // Show selection bar
        try { selectedPreview.setImage(new Image(url, 48, 48, true, true, true)); } catch (Exception ignored) {}
        selectedNameLabel.setText(filename);
        selectionBar.setVisible(true);
        selectionBar.setManaged(true);
    }

    @FXML
    private void onSearch() {
        renderGrid(allFiles);
    }

    @FXML
    private void onRefresh() {
        selectedFilename = null;
        selectionBar.setVisible(false);
        selectionBar.setManaged(false);
        loadImages();
    }

    @FXML
    private void onUploadNew() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Upload Image to pidevassets");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        try {
            Path dest = Path.of(PIDEVASSETS_PATH);
            if (!Files.exists(dest)) Files.createDirectories(dest);
            String name = System.currentTimeMillis() + "_" + file.getName();
            Files.copy(file.toPath(), dest.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            loadImages(); // refresh
        } catch (IOException e) {
            System.err.println("Upload error: " + e.getMessage());
        }
    }

    @FXML
    private void onConfirm() {
        if (selectedFilename != null && onSelectCallback != null) {
            onSelectCallback.accept(selectedFilename);
        }
        if (stage != null) stage.close();
    }

    @FXML
    private void onClose() {
        if (stage != null) stage.close();
    }
}
