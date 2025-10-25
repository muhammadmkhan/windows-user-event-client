package com.example.clientapp.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Component
public class MainScreenController {

    @FXML private TilePane gameContainer;
    @FXML private Button logoutBtn;

    private final File gamesDir = new File("C:/Games"); // configurable later

    @FXML
    public void initialize() {
        loadGames();

        logoutBtn.setOnAction(e -> handleLogout());
    }

    private void loadGames() {
        if (!gamesDir.exists()) {
            System.err.println("❌ Game directory not found: " + gamesDir.getAbsolutePath());
            return;
        }

        File[] gameFolders = gamesDir.listFiles(File::isDirectory);
        if (gameFolders == null) return;

        for (File folder : gameFolders) {
            File[] exes = folder.listFiles(f -> f.getName().endsWith(".exe"));
            if (exes != null && exes.length > 0) {
                addGameCard(folder.getName(), exes[0]);
            }
        }
    }

    private void addGameCard(String gameName, File exeFile) {

        Image image;
        try {
            image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/img/cstrike.png")));
        } catch (Exception e) {
            System.err.println("⚠️ Could not load game icon, using blank placeholder.");
            image = new Image("https://via.placeholder.com/100x100.png?text=Game");
        }

        ImageView icon = new ImageView(image);
        icon.setFitHeight(100);
        icon.setFitWidth(100);

        Label label = new Label(gameName);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        VBox card = new VBox(10, icon, label);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: #2d2d30; -fx-border-color: #444; -fx-padding: 10; -fx-background-radius: 8; -fx-border-radius: 8;");
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #3a3a3d; -fx-border-color: #666; -fx-padding: 10; -fx-background-radius: 8; -fx-border-radius: 8;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #2d2d30; -fx-border-color: #444; -fx-padding: 10; -fx-background-radius: 8; -fx-border-radius: 8;"));

        card.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                launchGame(exeFile, (Stage) card.getScene().getWindow());
            }
        });

        Platform.runLater(() -> gameContainer.getChildren().add(card));
    }

    private void launchGame(File exeFile, Stage stage) {
        try {
            System.out.println("🎮 Launching: " + exeFile.getAbsolutePath());

            // ✅ set working directory to the game folder
            ProcessBuilder pb = new ProcessBuilder(exeFile.getAbsolutePath());
            pb.directory(exeFile.getParentFile());

            pb.inheritIO(); // optional: attach output to console for debugging
            pb.start();

            // Hide launcher while game runs
            stage.hide();

            // Wait for the game to exit, then show launcher again
            new Thread(() -> {
                try {
                    pb.start().waitFor(); // wait until game closes
                } catch (Exception ignored) {}
                Platform.runLater(stage::show);
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void handleLogout() {
        System.out.println("🔒 Logging out user...");
        // later you’ll re-enable kiosk lock here and return to lock screen
    }
}
