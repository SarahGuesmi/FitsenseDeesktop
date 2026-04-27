package controllers;

import app.AppSession;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import models.Exercise;
import models.Workout;
import services.ExerciseRatingService;
import services.MusicService;
import services.ProgressService;
import utils.ActivityTracker;
import utils.WebAssets;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class ExerciseDetailController {

    @FXML private StackPane imagePane;
    @FXML private ImageView exerciseImage;
    @FXML private Label nameLabel;
    @FXML private Label doneBadge;
    @FXML private Label typeBadge;
    @FXML private Label targetBadge;
    @FXML private Label setsBadge;
    @FXML private Label descLabel;
    @FXML private VBox videoPane;
    @FXML private WebView youtubeWebView;
    @FXML private StackPane videoThumbPane;
    @FXML private ImageView videoThumbnail;
    @FXML private Label playPauseIcon;
    @FXML private Button videoPlayBtn;
    @FXML private Button videoPauseBtn;
    @FXML private Label videoStatusLabel;

    private boolean videoPlaying = false;
    private String currentVideoId = null;
    @FXML private Label resultBanner;
    @FXML private Label statusLabel;
    @FXML private Label ringArc;
    @FXML private Label timerLabel;
    @FXML private Label elapsedLabel;
    @FXML private Label overtimeAlert;
    @FXML private Label resultMessage;
    @FXML private Button startBtn;
    @FXML private Button repeatBtn;
    @FXML private Button doneBtn;

    @FXML private Label star1, star2, star3, star4, star5;
    @FXML private Label ratingLabel;
    @FXML private Label avgRatingLabel;

    // Music player
    @FXML private Button moodWorkout, moodHiit, moodRelax, moodEnergy;
    @FXML private Label musicLoadingLabel;
    @FXML private HBox nowPlayingBox;
    @FXML private Label trackTitleLabel;
    @FXML private Label trackArtistLabel;
    @FXML private Button playPauseMusicBtn;
    @FXML private VBox trackListBox;

    private MediaPlayer mediaPlayer;
    private List<MusicService.Track> currentTracks;
    private int currentTrackIndex = 0;
    private boolean musicPlaying = false;
    private final MusicService musicService = new MusicService();

    private int currentRating = 0;
    private final ExerciseRatingService ratingService = new ExerciseRatingService();
    private Exercise exercise;
    private Workout workout;
    private Consumer<Exercise> onDoneCallback;
    private final ProgressService progressService = new ProgressService();

    private Timeline timeline;
    private int elapsedSeconds = 0;
    private boolean running = false;
    private boolean completed = false;
    private boolean overtimeShown = false;

    public void setExercise(Exercise e, Workout w, Consumer<Exercise> onDone) {
        this.exercise = e;
        this.workout = w;
        this.onDoneCallback = onDone;
        populate();
    }

    private void populate() {
        if (exercise == null) return;

        nameLabel.setText(safe(exercise.getNom()));

        // Type badge
        typeBadge.setText("🏷 Type : " + (safe(exercise.getType()).isEmpty() ? "—" : exercise.getType()));

        // Target duration
        int target = exercise.getDuree() != null ? exercise.getDuree() : 0;
        targetBadge.setText("⏱ Target : " + target + "s");

        // Sets / Reps
        if (exercise.getSets() != null) {
            setsBadge.setText("🔁 " + exercise.getSets() + " sets × " + (exercise.getReps() != null ? exercise.getReps() : 12) + " reps");
            setsBadge.setVisible(true);
            setsBadge.setManaged(true);
        }

        // Description
        String desc = safe(exercise.getDescription());
        descLabel.setText(desc.isEmpty() ? "No description available." : desc);

        // Hero image
        if (exercise.getImageName() != null && !exercise.getImageName().isBlank()) {
            try {
                String url = WebAssets.assetUrl("uploads/exercises/" + exercise.getImageName());
                exerciseImage.setImage(new Image(url, true));
                imagePane.setVisible(true);
                imagePane.setManaged(true);
            } catch (Exception ignored) {}
        }

        // YouTube embed
        if (exercise.getYoutubeVideoId() != null && !exercise.getYoutubeVideoId().isBlank()) {
            currentVideoId = exercise.getYoutubeVideoId();
            // Load thumbnail
            String thumbUrl = "https://img.youtube.com/vi/" + currentVideoId + "/hqdefault.jpg";
            try {
                videoThumbnail.setImage(new Image(thumbUrl, true));
            } catch (Exception ignored) {}
            // Click on thumbnail = play
            videoThumbPane.setOnMouseClicked(ev -> onVideoPlay());
            videoPane.setVisible(true);
            videoPane.setManaged(true);
        }

        // Check if already done in DB
        UUID userId = AppSession.getCurrentUser() != null ? AppSession.getCurrentUser().getId() : null;
        if (userId != null && exercise.getUuid() != null) {
            if (progressService.isExerciseDone(userId, exercise.getUuid())) {
                completed = true;
                elapsedSeconds = progressService.getElapsedTime(userId, exercise.getUuid());
                showCompletion(elapsedSeconds);
            }
        }

        updateTimerDisplay(elapsedSeconds);

        // Load existing rating
        loadRating();
    }

    private void loadRating() {
        UUID userId = AppSession.getCurrentUser() != null ? AppSession.getCurrentUser().getId() : null;
        if (userId == null || exercise.getUuid() == null) return;
        currentRating = ratingService.getRating(userId, exercise.getUuid());
        renderStars(currentRating);
        double avg = ratingService.getAverageRating(exercise.getUuid());
        if (avg > 0) avgRatingLabel.setText(String.format("avg %.1f ★", avg));
        if (currentRating > 0) ratingLabel.setText("Your rating: " + currentRating + "/5");
    }

    // ── VIDEO CONTROLS ─────────────────────────────────────
    @FXML
    private void onVideoPlay() {
        if (currentVideoId == null) return;
        videoPlaying = true;

        // Hide thumbnail, show WebView with proper embed
        videoThumbPane.setVisible(false);
        videoThumbPane.setManaged(false);
        youtubeWebView.setPrefHeight(380.0);
        youtubeWebView.setVisible(true);
        youtubeWebView.setManaged(true);

        // Use nocookie domain + enablejsapi to avoid full YouTube page
        String html = "<!DOCTYPE html><html>"
                + "<head><style>*{margin:0;padding:0;overflow:hidden;background:#000;}</style></head>"
                + "<body>"
                + "<iframe id='yt' width='100%' height='100%' "
                + "src='https://www.youtube-nocookie.com/embed/" + currentVideoId
                + "?autoplay=1&rel=0&modestbranding=1&controls=1&enablejsapi=1&fs=0' "
                + "frameborder='0' allow='autoplay;encrypted-media' allowfullscreen='false'>"
                + "</iframe>"
                + "</body></html>";
        youtubeWebView.getEngine().loadContent(html);

        // Disable right-click context menu
        youtubeWebView.setContextMenuEnabled(false);

        videoPlayBtn.setVisible(false);
        videoPlayBtn.setManaged(false);
        videoPauseBtn.setVisible(true);
        videoPauseBtn.setManaged(true);
        videoStatusLabel.setText("Playing...");
    }

    @FXML
    private void onVideoPause() {
        if (currentVideoId == null) return;
        videoPlaying = false;

        // Load same embed but with autoplay=0 (paused state)
        String html = "<!DOCTYPE html><html>"
                + "<head><style>*{margin:0;padding:0;overflow:hidden;background:#000;}</style></head>"
                + "<body>"
                + "<iframe id='yt' width='100%' height='100%' "
                + "src='https://www.youtube-nocookie.com/embed/" + currentVideoId
                + "?autoplay=0&rel=0&modestbranding=1&controls=1&enablejsapi=1&fs=0' "
                + "frameborder='0' allow='autoplay;encrypted-media'>"
                + "</iframe>"
                + "</body></html>";
        youtubeWebView.getEngine().loadContent(html);

        videoPlayBtn.setVisible(true);
        videoPlayBtn.setManaged(true);
        videoPauseBtn.setVisible(false);
        videoPauseBtn.setManaged(false);
        videoStatusLabel.setText("Paused — click Play to resume");
    }

    // ── STAR RATING ────────────────────────────────────────
    @FXML private void onStar1() { saveRating(1); }
    @FXML private void onStar2() { saveRating(2); }
    @FXML private void onStar3() { saveRating(3); }
    @FXML private void onStar4() { saveRating(4); }
    @FXML private void onStar5() { saveRating(5); }

    @FXML private void onStarHover1() { renderStars(1); }
    @FXML private void onStarHover2() { renderStars(2); }
    @FXML private void onStarHover3() { renderStars(3); }
    @FXML private void onStarHover4() { renderStars(4); }
    @FXML private void onStarHover5() { renderStars(5); }
    @FXML private void onStarExit()   { renderStars(currentRating); }

    private void saveRating(int rating) {
        currentRating = rating;
        UUID userId = AppSession.getCurrentUser() != null ? AppSession.getCurrentUser().getId() : null;
        if (userId != null && exercise.getUuid() != null) {
            ratingService.saveRating(userId, exercise.getUuid(), rating);
        }
        renderStars(rating);
        ratingLabel.setText("Your rating: " + rating + "/5");
        double avg = ratingService.getAverageRating(exercise.getUuid());
        if (avg > 0) avgRatingLabel.setText(String.format("avg %.1f ★", avg));
    }

    private void renderStars(int count) {
        Label[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < 5; i++) {
            if (i < count) {
                stars[i].setText("★");
                stars[i].setStyle("-fx-font-size:26px;-fx-cursor:hand;-fx-text-fill:#F59E0B;");
            } else {
                stars[i].setText("☆");
                stars[i].setStyle("-fx-font-size:26px;-fx-cursor:hand;-fx-text-fill:#6B7280;");
            }
        }
    }

    // ── MUSIC PLAYER ───────────────────────────────────────
    @FXML private void onMoodWorkout() { loadMusic(MusicService.Mood.WORKOUT, moodWorkout); }
    @FXML private void onMoodHiit()    { loadMusic(MusicService.Mood.HIIT, moodHiit); }
    @FXML private void onMoodRelax()   { loadMusic(MusicService.Mood.RELAX, moodRelax); }
    @FXML private void onMoodEnergy()  { loadMusic(MusicService.Mood.ENERGY, moodEnergy); }

    private void loadMusic(MusicService.Mood mood, Button activeBtn) {
        // Highlight active mood button
        for (Button b : new Button[]{moodWorkout, moodHiit, moodRelax, moodEnergy}) {
            if (b != null) b.setStyle("-fx-background-color:#1F2937;-fx-border-color:#374151;-fx-border-radius:10;-fx-background-radius:10;-fx-text-fill:#9CA3AF;-fx-font-size:12px;-fx-font-weight:700;-fx-padding:8 14;-fx-cursor:hand;");
        }
        if (activeBtn != null) activeBtn.setStyle("-fx-background-color:rgba(34,197,94,0.2);-fx-border-color:#22C55E;-fx-border-radius:10;-fx-background-radius:10;-fx-text-fill:#22C55E;-fx-font-size:12px;-fx-font-weight:700;-fx-padding:8 14;-fx-cursor:hand;");

        stopMusic();
        musicLoadingLabel.setVisible(true); musicLoadingLabel.setManaged(true);
        nowPlayingBox.setVisible(false); nowPlayingBox.setManaged(false);
        trackListBox.setVisible(false); trackListBox.setManaged(false);

        new Thread(() -> {
            try {
                List<MusicService.Track> tracks = musicService.fetchTracks(mood);
                Platform.runLater(() -> {
                    musicLoadingLabel.setVisible(false); musicLoadingLabel.setManaged(false);
                    if (tracks.isEmpty()) {
                        musicLoadingLabel.setText("No tracks found for this mood.");
                        musicLoadingLabel.setVisible(true); musicLoadingLabel.setManaged(true);
                        return;
                    }
                    currentTracks = tracks;
                    currentTrackIndex = 0;
                    buildTrackList(tracks);
                    playTrack(0);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    musicLoadingLabel.setText("⚠ Could not load music: " + e.getMessage());
                    musicLoadingLabel.setVisible(true); musicLoadingLabel.setManaged(true);
                });
            }
        }).start();
    }

    private void buildTrackList(List<MusicService.Track> tracks) {
        trackListBox.getChildren().clear();
        for (int i = 0; i < tracks.size(); i++) {
            final int idx = i;
            MusicService.Track t = tracks.get(i);
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-padding:6 10;-fx-cursor:hand;-fx-background-radius:8;");
            row.setOnMouseEntered(ev -> row.setStyle("-fx-padding:6 10;-fx-cursor:hand;-fx-background-color:rgba(255,255,255,0.05);-fx-background-radius:8;"));
            row.setOnMouseExited(ev -> row.setStyle("-fx-padding:6 10;-fx-cursor:hand;-fx-background-radius:8;"));
            row.setOnMouseClicked(ev -> playTrack(idx));

            Label num = new Label((i + 1) + ".");
            num.setStyle("-fx-text-fill:#4B5563;-fx-font-size:11px;-fx-min-width:20;");
            Label title = new Label(t.title);
            title.setStyle("-fx-text-fill:#D1D5DB;-fx-font-size:12px;-fx-font-weight:700;");
            Label artist = new Label("— " + t.artist);
            artist.setStyle("-fx-text-fill:#6B7280;-fx-font-size:11px;");
            HBox.setHgrow(title, javafx.scene.layout.Priority.ALWAYS);
            row.getChildren().addAll(num, title, artist);
            trackListBox.getChildren().add(row);
        }
        trackListBox.setVisible(true); trackListBox.setManaged(true);
    }

    private void playTrack(int index) {
        if (currentTracks == null || index < 0 || index >= currentTracks.size()) return;
        stopMusic();
        currentTrackIndex = index;
        MusicService.Track track = currentTracks.get(index);

        try {
            Media media = new Media(track.audioUrl);
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setOnReady(() -> {
                mediaPlayer.play();
                musicPlaying = true;
                Platform.runLater(() -> {
                    trackTitleLabel.setText(track.title);
                    trackArtistLabel.setText(track.artist);
                    playPauseMusicBtn.setText("⏸");
                    nowPlayingBox.setVisible(true); nowPlayingBox.setManaged(true);
                });
            });
            mediaPlayer.setOnEndOfMedia(() -> {
                // Auto-play next
                if (currentTrackIndex + 1 < currentTracks.size()) {
                    Platform.runLater(() -> playTrack(currentTrackIndex + 1));
                }
            });
            mediaPlayer.setOnError(() ->
                System.err.println("MediaPlayer error: " + mediaPlayer.getError()));
        } catch (Exception e) {
            System.err.println("Music playback error: " + e.getMessage());
        }
    }

    @FXML
    private void onPlayPauseMusic() {
        if (mediaPlayer == null) return;
        if (musicPlaying) {
            mediaPlayer.pause();
            musicPlaying = false;
            playPauseMusicBtn.setText("▶");
        } else {
            mediaPlayer.play();
            musicPlaying = true;
            playPauseMusicBtn.setText("⏸");
        }
    }

    @FXML
    private void onPrevTrack() {
        if (currentTracks != null && currentTrackIndex > 0) playTrack(currentTrackIndex - 1);
    }

    @FXML
    private void onNextTrack() {
        if (currentTracks != null && currentTrackIndex + 1 < currentTracks.size()) playTrack(currentTrackIndex + 1);
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
            musicPlaying = false;
        }
    }

    // ── TIMER ──────────────────────────────────────────────
    @FXML
    private void onStartTimer() {
        if (completed) return;

        if (running) {
            // Pause
            timeline.pause();
            running = false;
            startBtn.setText("▶  Resume");
            startBtn.setStyle("-fx-background-color:#1F2937;-fx-text-fill:#9CA3AF;-fx-font-weight:900;-fx-font-size:14px;-fx-background-radius:14;-fx-padding:14 32;-fx-cursor:hand;");
            setStatus("⏸ Paused", "rgba(245,158,11,0.15)", "rgba(245,158,11,0.4)", "#FCD34D");
        } else {
            // Start / Resume
            running = true;
            startBtn.setText("⏸  Pause");
            startBtn.setStyle("-fx-background-color:#1F2937;-fx-text-fill:#9CA3AF;-fx-font-weight:900;-fx-font-size:14px;-fx-background-radius:14;-fx-padding:14 32;-fx-cursor:hand;");
            setStatus("⏱ In progress...", "rgba(59,130,246,0.15)", "rgba(59,130,246,0.4)", "#60A5FA");

            // Enable done button
            doneBtn.setDisable(false);
            doneBtn.setStyle("-fx-background-color:linear-gradient(to right,#3B82F6,#1D4ED8);-fx-text-fill:white;-fx-font-weight:900;-fx-font-size:14px;-fx-background-radius:14;-fx-padding:14 32;-fx-cursor:hand;");

            if (timeline == null) {
                timeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                    elapsedSeconds++;
                    updateTimerDisplay(elapsedSeconds);
                    elapsedLabel.setText("Time spent: " + elapsedSeconds + "s");

                    int target = exercise.getDuree() != null ? exercise.getDuree() : 0;
                    if (target > 0 && elapsedSeconds > target && !overtimeShown) {
                        overtimeShown = true;
                        overtimeAlert.setText("⚠ You have exceeded the target duration of " + target + "s!");
                        overtimeAlert.setVisible(true);
                        overtimeAlert.setManaged(true);
                    }
                }));
                timeline.setCycleCount(Timeline.INDEFINITE);
            }
            timeline.play();
        }
    }

    @FXML
    private void onMarkDone() {
        if (!running && elapsedSeconds == 0) return;
        if (timeline != null) timeline.stop();
        running = false;
        completed = true;

        // Persist to DB
        UUID userId = AppSession.getCurrentUser() != null ? AppSession.getCurrentUser().getId() : null;
        if (userId != null && exercise.getUuid() != null) {
            progressService.markExerciseDone(userId, exercise.getUuid(), elapsedSeconds);
        }

        ActivityTracker.track(ActivityTracker.EventType.COMPLETE_EXERCISE,
                "Completed: " + safe(exercise.getNom()),
                "Time: " + elapsedSeconds + "s" + (exercise.getDuree() != null ? " / target " + exercise.getDuree() + "s" : ""));

        showCompletion(elapsedSeconds);
        if (onDoneCallback != null) onDoneCallback.accept(exercise);
    }

    @FXML
    private void onRepeat() {
        if (timeline != null) { timeline.stop(); timeline = null; }
        elapsedSeconds = 0;
        running = false;
        completed = false;
        overtimeShown = false;

        updateTimerDisplay(0);
        elapsedLabel.setText("Elapsed time");
        overtimeAlert.setVisible(false);
        overtimeAlert.setManaged(false);
        resultMessage.setVisible(false);
        resultMessage.setManaged(false);
        resultBanner.setVisible(false);
        resultBanner.setManaged(false);
        doneBadge.setVisible(false);
        doneBadge.setManaged(false);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
        repeatBtn.setVisible(false);
        repeatBtn.setManaged(false);

        startBtn.setText("▶  Start Exercise");
        startBtn.setStyle("-fx-background-color:linear-gradient(to right,#22C55E,#16A34A);-fx-text-fill:black;-fx-font-weight:900;-fx-font-size:14px;-fx-background-radius:14;-fx-padding:14 32;-fx-cursor:hand;");
        startBtn.setDisable(false);

        doneBtn.setDisable(true);
        doneBtn.setStyle("-fx-background-color:#1F2937;-fx-text-fill:#4B5563;-fx-font-weight:900;-fx-font-size:14px;-fx-background-radius:14;-fx-padding:14 32;");
        doneBtn.setText("✔  Mark Done");

        ringArc.setStyle("-fx-background-color:transparent;-fx-border-color:#22C55E;-fx-border-radius:70;-fx-border-width:7;-fx-min-width:140;-fx-min-height:140;-fx-max-width:140;-fx-max-height:140;-fx-opacity:0.15;");
    }

    private void showCompletion(int elapsed) {
        int target = exercise.getDuree() != null ? exercise.getDuree() : 0;
        boolean over = target > 0 && elapsed > target;

        doneBadge.setVisible(true);
        doneBadge.setManaged(true);

        // Status pill
        if (over) {
            setStatus("❌ Exceeded by " + (elapsed - target) + "s", "rgba(239,68,68,0.15)", "rgba(239,68,68,0.4)", "#F87171");
        } else {
            setStatus("✔ Finished in " + elapsed + "s — Within target!", "rgba(34,197,94,0.15)", "rgba(34,197,94,0.4)", "#4ADE80");
        }

        // Inline result message
        String msg, style;
        if (over) {
            msg = "❌ Completed in " + elapsed + "s — Exceeded by " + (elapsed - target) + "s (target: " + target + "s)";
            style = "-fx-background-color:rgba(239,68,68,0.1);-fx-border-color:rgba(239,68,68,0.3);-fx-border-radius:12;-fx-background-radius:12;-fx-text-fill:#F87171;-fx-font-size:13px;-fx-font-weight:600;-fx-padding:10 16;";
        } else {
            int spare = target - elapsed;
            msg = "✅ Completed in " + elapsed + "s — Within target!" + (spare > 0 ? " (" + spare + "s to spare)" : "");
            style = "-fx-background-color:rgba(34,197,94,0.1);-fx-border-color:rgba(34,197,94,0.3);-fx-border-radius:12;-fx-background-radius:12;-fx-text-fill:#4ADE80;-fx-font-size:13px;-fx-font-weight:600;-fx-padding:10 16;";
        }
        resultMessage.setText(msg);
        resultMessage.setStyle(style);
        resultMessage.setVisible(true);
        resultMessage.setManaged(true);

        // Persistent banner
        resultBanner.setText(msg);
        resultBanner.setStyle(style.replace("-fx-padding:10 16;", "-fx-padding:14 20;"));
        resultBanner.setVisible(true);
        resultBanner.setManaged(true);

        // Ring color
        ringArc.setStyle("-fx-background-color:transparent;-fx-border-color:" + (over ? "#EF4444" : "#22C55E")
                + ";-fx-border-radius:70;-fx-border-width:7;-fx-min-width:140;-fx-min-height:140;-fx-max-width:140;-fx-max-height:140;-fx-opacity:1.0;");

        // Buttons
        startBtn.setDisable(true);
        startBtn.setStyle("-fx-background-color:#1F2937;-fx-text-fill:#4B5563;-fx-font-weight:900;-fx-font-size:14px;-fx-background-radius:14;-fx-padding:14 32;");

        doneBtn.setDisable(true);
        doneBtn.setText("✔  Completed");
        doneBtn.setStyle("-fx-background-color:linear-gradient(to right,#22C55E,#16A34A);-fx-text-fill:black;-fx-font-weight:900;-fx-font-size:14px;-fx-background-radius:14;-fx-padding:14 32;");

        repeatBtn.setVisible(true);
        repeatBtn.setManaged(true);
    }

    private void setStatus(String text, String bg, String border, String color) {
        statusLabel.setText(text);
        statusLabel.setStyle("-fx-background-color:" + bg + ";-fx-border-color:" + border
                + ";-fx-border-radius:999;-fx-background-radius:999;-fx-text-fill:" + color
                + ";-fx-font-size:13px;-fx-font-weight:700;-fx-padding:8 20;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void updateTimerDisplay(int seconds) {
        int mm = seconds / 60;
        int ss = seconds % 60;
        timerLabel.setText(String.format("%02d:%02d", mm, ss));

        int target = exercise != null && exercise.getDuree() != null ? exercise.getDuree() : 0;
        if (target > 0) {
            double pct = Math.min(1.0, (double) seconds / target);
            String color = pct < 0.5 ? "#22C55E" : pct < 0.85 ? "#F59E0B" : "#EF4444";
            double opacity = 0.15 + pct * 0.85;
            ringArc.setStyle("-fx-background-color:transparent;-fx-border-color:" + color
                    + ";-fx-border-radius:70;-fx-border-width:7;"
                    + "-fx-min-width:140;-fx-min-height:140;-fx-max-width:140;-fx-max-height:140;"
                    + "-fx-opacity:" + opacity + ";");
        }
    }

    // ── NAVIGATION ─────────────────────────────────────────
    @FXML
    private void onBack() {
        if (timeline != null) timeline.stop();
        stopMusic();
        navigateBack();
    }

    private void navigateBack() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource("/fxml/WorkoutDetailView.fxml")));
            Parent root = loader.load();
            if (workout != null) {
                WorkoutDetailController ctrl = loader.getController();
                ctrl.setWorkout(workout);
                if (completed && exercise != null) {
                    ctrl.markExerciseDoneUuid(exercise.getUuid(), exercise.getId());
                }
            }
            nameLabel.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
