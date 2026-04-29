package controllers;

import app.AppSession;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import models.User;
import utils.DbConnection;
import utils.UuidUtil;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AvatarPickController — shows 6 gender-appropriate avatars generated via DiceBear API.
 * Male users see male avatars, female users see female avatars.
 *
 * DiceBear API: https://api.dicebear.com/7.x/{style}/png?seed={seed}&...
 * No API key required. Free and open source.
 */
public class AvatarPickController {

    // DiceBear styles — adventurer for female, big-smile for male (both sport-themed)
    private static final String STYLE_MALE   = "adventurer";
    private static final String STYLE_FEMALE = "adventurer";

    // Gender-specific seed pools for distinct looks
    private static final String[] MALE_SEEDS = {
        "Atlas", "Titan", "Blaze", "Storm", "Apex",
        "Bolt", "Forge", "Ryder", "Knox", "Zane"
    };
    private static final String[] FEMALE_SEEDS = {
        "Luna", "Nova", "Aria", "Sage", "Ember",
        "Skye", "Zara", "Ivy", "Nyx", "Cleo"
    };

    @FXML private StackPane root;
    @FXML private FlowPane  avatarsPane;
    @FXML private Button    redesignBtn;
    @FXML private Button    confirmBtn;
    @FXML private Label     loadingLabel;
    @FXML private Label     errorLabel;

    private String selectedAvatarUrl = null;
    private int    seedOffset        = 0;

    @FXML
    private void initialize() {
        loadAvatars();
    }

    @FXML
    private void onRedesign() {
        seedOffset += 5;
        selectedAvatarUrl = null;
        loadAvatars();
    }

    @FXML
    private void onBack() {
        switchScene("/fxml/UsernamePickView.fxml", "/css/onboarding.css");
    }

    @FXML
    private void onConfirm() {
        hideError();
        if (selectedAvatarUrl == null) {
            showError("Please select an avatar.");
            return;
        }

        try {
            User user = AppSession.getCurrentUser();
            String sql = "UPDATE `app_user` SET `photo` = ? WHERE `id` = ?";
            try (PreparedStatement stmt = DbConnection.getInstance().getCnx().prepareStatement(sql)) {
                stmt.setString(1, selectedAvatarUrl);
                stmt.setBytes(2, UuidUtil.toBytes16(user.getId()));
                stmt.executeUpdate();
            }
            user.setPhoto(selectedAvatarUrl);
        } catch (Exception e) {
            showError("Failed to save avatar: " + e.getMessage());
            return;
        }

        switchScene("/fxml/DashboardView.fxml", "/css/dashboard.css");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void loadAvatars() {
        setLoading(true);
        avatarsPane.getChildren().clear();

        User user = AppSession.getCurrentUser();
        String gender = AppSession.getOnboardingData().getGender();
        if (gender == null) gender = "male";

        boolean isMale = gender.equalsIgnoreCase("male");
        String style   = isMale ? STYLE_MALE : STYLE_FEMALE;
        String[] seeds = isMale ? MALE_SEEDS : FEMALE_SEEDS;

        // Pick 6 seeds from the pool with offset for "redesign"
        List<String> chosen = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            chosen.add(seeds[(seedOffset + i) % seeds.length]);
        }

        // Build avatar URLs — PNG format, 120x120, with sport background color
        String bgColor = isMale ? "1a3a5c" : "3a1a5c";
        List<String> urls = new ArrayList<>();
        for (String seed : chosen) {
            urls.add("https://api.dicebear.com/7.x/" + style + "/png"
                    + "?seed=" + seed
                    + "&size=120"
                    + "&backgroundColor=" + bgColor);
        }

        new Thread(() -> {
            List<AvatarCard> cards = new ArrayList<>();
            for (String url : urls) {
                try {
                    Image img = new Image(url, 120, 120, true, true, false);
                    cards.add(new AvatarCard(url, img));
                } catch (Exception e) {
                    System.err.println("Avatar load failed: " + e.getMessage());
                }
            }
            Platform.runLater(() -> {
                setLoading(false);
                for (AvatarCard card : cards) {
                    avatarsPane.getChildren().add(buildAvatarNode(card));
                }
            });
        }, "avatar-loader").start();
    }

    private VBox buildAvatarNode(AvatarCard card) {
        ImageView iv = new ImageView(card.image);
        iv.setFitWidth(110);
        iv.setFitHeight(110);
        iv.setPreserveRatio(true);

        // Clip the image to a circle so it never overflows the frame
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(55, 55, 55);
        iv.setClip(clip);

        StackPane frame = new StackPane(iv);
        frame.getStyleClass().add("avatar-frame");
        frame.setPrefSize(130, 130);
        frame.setMaxSize(130, 130);

        Label lbl = new Label("SELECT");
        lbl.getStyleClass().add("avatar-label");

        VBox box = new VBox(8, frame, lbl);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("avatar-card");
        box.setOnMouseClicked(e -> selectAvatar(box, card.url));
        return box;
    }

    private void selectAvatar(VBox box, String url) {
        // Deselect all
        avatarsPane.getChildren().forEach(n -> {
            n.getStyleClass().remove("avatar-card-selected");
            if (n instanceof VBox vb) {
                vb.getChildren().stream()
                  .filter(c -> c instanceof Label)
                  .forEach(c -> ((Label) c).setText("SELECT"));
            }
        });
        box.getStyleClass().add("avatar-card-selected");
        box.getChildren().stream()
           .filter(c -> c instanceof Label)
           .forEach(c -> ((Label) c).setText("✓ SELECTED"));
        selectedAvatarUrl = url;
        hideError();
    }

    private void setLoading(boolean loading) {
        loadingLabel.setManaged(loading);
        loadingLabel.setVisible(loading);
        redesignBtn.setDisable(loading);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private void switchScene(String fxml, String css) {
        try {
            Parent newRoot = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(fxml)));
            Scene scene = root.getScene();
            scene.setRoot(newRoot);
            scene.getStylesheets().setAll(Objects.requireNonNull(getClass().getResource(css)).toExternalForm());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private record AvatarCard(String url, Image image) {}
}
