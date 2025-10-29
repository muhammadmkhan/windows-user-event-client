package com.example.clientapp.styling;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;
import javafx.scene.input.MouseEvent;
import javafx.geometry.Insets;
import javafx.stage.Window;


public class CustomPopUp {

    // call this to show a themed alert
    public static void showThemedConfirm(Alert alert) {

        // IMPORTANT: set the stage style before the dialog is shown so the OS decoration is removed
        alert.initStyle(StageStyle.TRANSPARENT); // or StageStyle.UNDECORATED

        // Remove default header text (we will create our own header node)
        alert.setHeaderText(null);
        alert.setTitle(null);

        DialogPane dialogPane = alert.getDialogPane();

        // Make the scene background transparent so rounded corners & shadow are visible
        dialogPane.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #1a0033, #330066);" +
                        "-fx-border-color: rgba(160, 32, 240, 0.85);" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 12;"
        );

        // Create a custom title bar inside the dialog (so it looks like a themed title bar)
        Label titleLabel = new Label("🔒 Logout Confirmation");
        titleLabel.setStyle("-fx-text-fill: #e0caff; -fx-font-size: 16px; -fx-font-family: 'Poppins'; -fx-font-weight:bold;");
        titleLabel.setPadding(new Insets(8, 8, 8, 8));

        // Optionally add a small close button in the header (that behaves like window close)
        Button fauxClose = new Button("✕");
        fauxClose.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #f5c6d6;" +
                        "-fx-font-size: 12px;" +
                        "-fx-cursor: hand;"
        );
        fauxClose.setOnAction(ev -> alert.close());

        // Place title and close button in an HBox and set as graphic (or place in a VBox above content)
        HBox headerBox = new HBox();
        headerBox.setSpacing(10);
        headerBox.setPadding(new Insets(4, 4, 8, 12));
        headerBox.getChildren().addAll(titleLabel);
        // align to left; if you want the close on the right you can use Region spacer and add fauxClose
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerBox.getChildren().addAll(spacer, fauxClose);

        // Insert headerBox into dialogPane by using setHeader (dialogPane.setHeader was removed),
        // instead use header text area replacement by placing header inside content wrapper:
        // We'll create a container VBox with headerBox + content area
        Label contentLabel = new Label(alert.getContentText());
        contentLabel.setStyle("-fx-text-fill: #c2b0f9; -fx-font-size: 14px; -fx-font-family:'Poppins';");
        contentLabel.setWrapText(true);
        contentLabel.setPadding(new Insets(6, 12, 12, 12));

        VBox container = new VBox();
        container.getChildren().addAll(headerBox, contentLabel);
        dialogPane.setContent(container);

        // Style the buttons
        dialogPane.getButtonTypes().forEach(type -> {
            Button btn = (Button) dialogPane.lookupButton(type);
            btn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #b388ff, #7c4dff);" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 8;" +
                            "-fx-font-weight: bold;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 6 16;" +
                            "-fx-font-size: 13px;"
            );
            btn.setOnMouseEntered(e -> btn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #7c4dff, #b388ff);" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 8;" +
                            "-fx-font-weight: bold;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 6 16;" +
                            "-fx-font-size: 13px;"
            ));
            btn.setOnMouseExited(e -> btn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #b388ff, #7c4dff);" +
                            "-fx-text-fill: white;" +
                            "-fx-background-radius: 8;" +
                            "-fx-font-weight: bold;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 6 16;" +
                            "-fx-font-size: 13px;"
            ));
        });

        // Make the dialog draggable (so when OS titlebar removed, user can still move it)
        // We haven't shown the dialog yet — we can add drag listeners to dialogPane itself
        final Delta dragDelta = new Delta();
        headerBox.setOnMousePressed((MouseEvent me) -> {
            Window w = dialogPane.getScene().getWindow();
            dragDelta.x = w.getX() - me.getScreenX();
            dragDelta.y = w.getY() - me.getScreenY();
        });
        headerBox.setOnMouseDragged((MouseEvent me) -> {
            Window w = dialogPane.getScene().getWindow();
            w.setX(me.getScreenX() + dragDelta.x);
            w.setY(me.getScreenY() + dragDelta.y);
        });

        // After configuration, show the dialog
        // We want the stage's scene background to be transparent so rounded corners look right
        alert.showingProperty().addListener((obs, wasShowing, isNowShowing) -> {
            if (isNowShowing) {
                Stage stage = (Stage) dialogPane.getScene().getWindow();
                stage.getScene().setFill(Color.TRANSPARENT); // allow rounded corners to show
                // optional: add a subtle drop shadow to the stage
                // Stage-level decorations/shadow require extra work; the CSS drop shadow on dialogPane works fine
            }
        });
    }

    // small helper for drag delta
    private static class Delta { double x, y; }

}
