package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

public class ExerciseLibraryDetailController {

    @FXML private WebView gifWebView;
    @FXML private Label nameLabel;
    @FXML private Label bodyPartBadge;
    @FXML private Label targetBadge;
    @FXML private Label equipBadge;
    @FXML private Label equipLabel;
    @FXML private VBox stepsBox;
    @FXML private Label stepsCount;
    @FXML private VBox musclesBox;
    @FXML private Label musclesCount;
    @FXML private VBox secondaryBox;
    @FXML private FlowPane secondaryPane;
    @FXML private VBox instructionsBox;
    @FXML private VBox instructionsList;

    public void setExercise(JSONObject ex) {
        nameLabel.setText(capitalize(ex.optString("name", "Unknown")));

        String bodyPart  = ex.optString("bodyPart", "");
        String target    = ex.optString("target", "");
        String equipment = ex.optString("equipment", "");

        bodyPartBadge.setText("🏃 " + bodyPart);
        targetBadge.setText("🎯 " + target);
        equipBadge.setText("🏋 " + equipment);
        equipLabel.setText(capitalize(equipment));

        // GIF URL — v2.exercisedb.io est l'URL publique directe (pas besoin de RapidAPI headers)
        String exId = ex.optString("id", "");
        String gifUrlRaw = ex.optString("gifUrl", "");
        final String gifUrl = (gifUrlRaw.isBlank() && !exId.isBlank())
                ? "https://v2.exercisedb.io/image/" + exId
                : gifUrlRaw;
        System.out.println("GIF URL = " + gifUrl);
        Platform.runLater(() -> loadGif(gifUrl));

        // ── Secondary muscles ────────────────────────────────
        JSONArray secondary = ex.optJSONArray("secondaryMuscles");
        if (secondary != null && !secondary.isEmpty()) {
            secondaryPane.getChildren().clear();
            for (int i = 0; i < secondary.length(); i++) {
                Label badge = new Label(secondary.getString(i));
                badge.setStyle(
                        "-fx-background-color:rgba(107,114,128,0.2);" +
                                "-fx-border-color:rgba(107,114,128,0.3);" +
                                "-fx-border-radius:999;-fx-background-radius:999;" +
                                "-fx-text-fill:#9CA3AF;-fx-font-size:11px;" +
                                "-fx-font-weight:700;-fx-padding:3 10;"
                );
                secondaryPane.getChildren().add(badge);
            }
            secondaryBox.setVisible(true);
            secondaryBox.setManaged(true);

            musclesCount.setText(String.valueOf(secondary.length()));
            musclesBox.setVisible(true);
            musclesBox.setManaged(true);
        }

        // ── Instructions ─────────────────────────────────────
        JSONArray instructions = ex.optJSONArray("instructions");
        if (instructions != null && !instructions.isEmpty()) {
            instructionsList.getChildren().clear();
            stepsCount.setText(String.valueOf(instructions.length()));
            stepsBox.setVisible(true);
            stepsBox.setManaged(true);

            for (int i = 0; i < instructions.length(); i++) {
                HBox stepRow = new HBox(12);
                stepRow.setStyle(
                        "-fx-background-color:#0D1117;-fx-border-color:#1F2937;" +
                                "-fx-border-radius:10;-fx-background-radius:10;-fx-padding:12 16;"
                );
                stepRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label num = new Label(String.valueOf(i + 1));
                num.setStyle(
                        "-fx-background-color:#3B82F6;-fx-text-fill:white;" +
                                "-fx-font-size:11px;-fx-font-weight:900;" +
                                "-fx-background-radius:999;" +
                                "-fx-min-width:24;-fx-min-height:24;" +
                                "-fx-max-width:24;-fx-max-height:24;" +
                                "-fx-alignment:center;"
                );
                num.setAlignment(javafx.geometry.Pos.CENTER);

                Label text = new Label(instructions.getString(i));
                text.setStyle("-fx-text-fill:#D1D5DB;-fx-font-size:13px;");
                text.setWrapText(true);
                HBox.setHgrow(text, javafx.scene.layout.Priority.ALWAYS);

                stepRow.getChildren().addAll(num, text);
                instructionsList.getChildren().add(stepRow);
            }
            instructionsBox.setVisible(true);
            instructionsBox.setManaged(true);
        }
    }

    // ── GIF loader ───────────────────────────────────────────
    private void loadGif(String gifUrl) {
        if (gifUrl == null || gifUrl.isBlank()) {
            gifWebView.getEngine().loadContent(noPreviewHtml());
            return;
        }

        gifWebView.getEngine().setJavaScriptEnabled(true);

        gifWebView.getEngine().setUserAgent(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
        );

        gifWebView.getEngine().setOnError(event ->
                System.err.println("WebView error: " + event.getMessage())
        );

        String html =
                "<!DOCTYPE html><html>" +
                        "<head><meta charset='UTF-8'>" +
                        "<style>" +
                        "* { margin:0; padding:0; box-sizing:border-box; }" +
                        "html, body {" +
                        "  width:320px; height:320px;" +
                        "  background:#111827; overflow:hidden;" +
                        "  display:flex; align-items:center; justify-content:center;" +
                        "}" +
                        "img {" +
                        "  max-width:100%; max-height:100%;" +
                        "  object-fit:contain; display:block;" +
                        "}" +
                        "#err {" +
                        "  color:#4B5563; font-family:sans-serif;" +
                        "  font-size:13px; font-weight:700;" +
                        "  text-align:center; display:none;" +
                        "}" +
                        "</style></head>" +
                        "<body>" +
                        "<img id='gif' src='" + gifUrl + "' " +
                        "  onload=\"this.style.display='block';document.getElementById('err').style.display='none';\" " +
                        "  onerror=\"this.style.display='none';document.getElementById('err').style.display='block';\" />" +
                        "<div id='err'>NO PREVIEW</div>" +
                        "</body></html>";

        gifWebView.getEngine().loadContent(html);
        gifWebView.setContextMenuEnabled(false);
    }

    private String noPreviewHtml() {
        return
                "<!DOCTYPE html><html>" +
                        "<head><style>" +
                        "* { margin:0; padding:0; background:#111827; }" +
                        "body {" +
                        "  display:flex; align-items:center; justify-content:center;" +
                        "  width:320px; height:320px;" +
                        "  color:#4B5563; font-family:sans-serif;" +
                        "  font-size:13px; font-weight:700; letter-spacing:1px;" +
                        "}" +
                        "</style></head>" +
                        "<body>NO PREVIEW</body></html>";
    }

    // ── Navigation ───────────────────────────────────────────
    @FXML
    private void onBack() {
        DashboardController dash = DashboardController.getInstance();
        if (dash != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        Objects.requireNonNull(getClass().getResource("/fxml/ExerciseLibraryView.fxml")));
                javafx.scene.Node view = loader.load();
                dash.showLibraryView(view);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}