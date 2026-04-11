package controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import models.MentalHealthAssessmentSubmission;
import services.MentalHealthSubmissionService;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Coach window: list assessments that have recommendations — edit or remove.
 */
public final class CoachRecommendationsManageDialog {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private CoachRecommendationsManageDialog() {
    }

    public static void show(Window owner, Runnable afterChange) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Manage recommendations");

        MentalHealthSubmissionService svc = MentalHealthSubmissionService.getInstance();
        FilteredList<MentalHealthAssessmentSubmission> filtered =
                new FilteredList<>(svc.getSubmissions(), MentalHealthAssessmentSubmission::hasRecommendationContent);

        TableView<MentalHealthAssessmentSubmission> table = new TableView<>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No saved recommendations yet."));
        table.getStyleClass().add("rec-manage-table");
        table.setPrefHeight(360);

        TableColumn<MentalHealthAssessmentSubmission, String> colUser = new TableColumn<>("Athlete");
        colUser.setCellValueFactory(cdf -> new SimpleStringProperty(safe(cdf.getValue().getUserFullName())));
        colUser.setPrefWidth(160);

        TableColumn<MentalHealthAssessmentSubmission, String> colDate = new TableColumn<>("Assessment");
        colDate.setCellValueFactory(cdf -> {
            var t = cdf.getValue().getTestedAt();
            return new SimpleStringProperty(t == null ? "—" : DT.format(t));
        });
        colDate.setPrefWidth(130);

        TableColumn<MentalHealthAssessmentSubmission, String> colPreview = new TableColumn<>("Summary");
        colPreview.setCellValueFactory(cdf -> new SimpleStringProperty(cdf.getValue().getRecommendationPreview()));
        colPreview.setPrefWidth(220);

        TableColumn<MentalHealthAssessmentSubmission, Void> colAct = new TableColumn<>("Actions");
        colAct.setPrefWidth(200);
        colAct.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button("Edit");
            private final Button del = new Button("Remove");
            private final HBox box = new HBox(8, edit, del);

            {
                edit.getStyleClass().addAll("rec-manage-action", "rec-manage-edit");
                del.getStyleClass().addAll("rec-manage-action", "rec-manage-delete");
                edit.setOnAction(e -> {
                    TableRow<MentalHealthAssessmentSubmission> tr = getTableRow();
                    MentalHealthAssessmentSubmission row = tr != null ? tr.getItem() : null;
                    if (row != null && RecommendationEditorDialog.showEditor(stage, row)) {
                        table.refresh();
                        if (afterChange != null) {
                            afterChange.run();
                        }
                    }
                });
                del.setOnAction(e -> {
                    TableRow<MentalHealthAssessmentSubmission> tr = getTableRow();
                    MentalHealthAssessmentSubmission row = tr != null ? tr.getItem() : null;
                    if (row == null) {
                        return;
                    }
                    Alert c = new Alert(Alert.AlertType.CONFIRMATION);
                    c.setTitle("Remove recommendation");
                    c.setHeaderText(null);
                    c.setContentText("Remove this recommendation for " + safe(row.getUserFullName()) + "?");
                    Optional<ButtonType> ans = c.showAndWait();
                    if (ans.isPresent() && ans.get() == ButtonType.OK) {
                        MentalHealthSubmissionService.getInstance().clearRecommendationByCoach(row.getId());
                        table.refresh();
                        if (afterChange != null) {
                            afterChange.run();
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().setAll(colUser, colDate, colPreview, colAct);

        Label title = new Label("Saved recommendations");
        title.setStyle("-fx-text-fill: #f4f7ff; -fx-font-size: 18px; -fx-font-weight: 800;");

        Button closeTop = new Button("✕");
        closeTop.getStyleClass().add("rec-close-btn");
        closeTop.setOnAction(e -> stage.close());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox head = new HBox(12, title, sp, closeTop);
        head.setAlignment(Pos.CENTER_LEFT);
        head.getStyleClass().add("rec-dialog-header");
        head.setPadding(new Insets(0));

        Button closeBottom = new Button("Close");
        closeBottom.getStyleClass().add("rec-cancel-btn");
        closeBottom.setOnAction(e -> stage.close());
        HBox foot = new HBox(closeBottom);
        foot.setAlignment(Pos.CENTER_RIGHT);
        foot.getStyleClass().add("rec-footer");

        VBox root = new VBox(12, head, table, foot);
        root.getStyleClass().add("rec-dialog-root");
        root.setPadding(new Insets(0));
        VBox.setVgrow(table, Priority.ALWAYS);

        Scene scene = new Scene(root, 720, 480);
        var css = CoachRecommendationsManageDialog.class.getResource("/css/recommendation-ui.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.show();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
