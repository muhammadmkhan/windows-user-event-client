package com.example.clientapp.controller;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import com.example.clientapp.config.PropertiesInfo;
import com.example.clientapp.styling.CustomPopUp;
import com.example.clientapp.util.GameProcessManager;
import com.example.clientapp.util.KioskLockUtil;
import com.sun.jna.platform.win32.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.Objects;

@Component
public class MainScreenController {

    @FXML
    private GridPane gameGrid;

    private Stage mainStage;

    @FXML
    private Label userLabel;

    @FXML
    private Label creditLabel;

    private double remainingMinutes;
    private Timeline countdownTimeline;


    private final String GAMES_DIR = "C:\\Games";
    private final int GRID_COLUMNS = 4;
    private final String DEFAULT_IMAGE = "/images/cs.png";

    @Autowired
    private PropertiesInfo propertiesInfo;

    private String username;

    public void setStage(Stage stage) {
        this.mainStage = stage;
    }

    @FXML
    public void initialize() {
        loadGames(new File(GAMES_DIR));
    }

    public void setCreditMinutes(double minutes) {
        this.remainingMinutes = minutes;
        updateCreditDisplay();
        startCountdown();
    }


    public void setUsername(String username) {
        this.username = username;
        if (userLabel != null) {
            userLabel.setText("Welcome, " + username);
        }
    }

    private void loadGames(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) return;

        gameGrid.getChildren().clear();
        gameGrid.getColumnConstraints().clear();

        for (int i = 0; i < GRID_COLUMNS; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / GRID_COLUMNS);
            gameGrid.getColumnConstraints().add(cc);
        }

        File[] files = folder.listFiles(f -> {
            if (f.isHidden()) return false;
            String name = f.getName().toLowerCase();
            return name.endsWith(".exe") || name.endsWith(".bat") || name.endsWith(".lnk");
        });

        if (files == null || files.length == 0) return;

        for (int i = 0; i < files.length; i++) addGameCard(files[i], i);
    }

    private void addGameCard(File shortcutFile, int index) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #2b0066, #4d0099);" +
                        "-fx-border-color: rgba(180, 32, 255, 0.8);" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-radius: 15;" +
                        "-fx-padding: 15;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(200, 32, 255, 0.8), 25, 0, 0, 8);"
        ));

        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #1a0033, #330066);" +
                        "-fx-border-color: rgba(160, 32, 240, 0.5);" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-radius: 15;" +
                        "-fx-padding: 15;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(180, 32, 255, 0.6), 15, 0, 0, 5);"
        ));


        ImageView imageView = new ImageView();
        imageView.setFitWidth(72);
        imageView.setFitHeight(72);

        // Try to extract the icon from the shortcut target
        try {
            String targetExe = "";/*ShortcutResolver.resolveShortcutTarget(shortcutFile);*/

            WinDef.HICON[] largeIcon = new WinDef.HICON[1];
            Shell32.INSTANCE.ExtractIconEx(targetExe, 0, largeIcon, null, 1);

            BufferedImage img = hIconToBufferedImage(largeIcon[0]);
            if (img != null) {
                byte[] bytes = toByteArray(img, "png");
                imageView.setImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(bytes)));
            } else {
                imageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(DEFAULT_IMAGE))));
            }
        } catch (Exception e) {
            imageView.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(DEFAULT_IMAGE))));
            e.printStackTrace();
        }

        Label nameLabel = new Label(shortcutFile.getName().replace(".lnk", ""));
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(120);

        card.getChildren().addAll(imageView, nameLabel);

        card.setOnMouseClicked(evt -> {
            if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2) {
                try {
                    GameProcessManager.launchGame(shortcutFile, mainStage, f -> {});
                } catch (IOException | ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        int col = index % GRID_COLUMNS;
        int row = index / GRID_COLUMNS;
        gameGrid.add(card, col, row);
    }
    public static BufferedImage hIconToBufferedImage(WinDef.HICON hIcon) {
        if (hIcon == null) return null;

        WinGDI.ICONINFO iconInfo = new WinGDI.ICONINFO();
        if (!User32.INSTANCE.GetIconInfo(hIcon, iconInfo)) return null;

        // Get icon width/height from hbmColor
        java.awt.image.BufferedImage img = null;
        try {
            GDI32 gdi = GDI32.INSTANCE;
            int width = 32;  // default
            int height = 32; // default

            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, width, height);
            g.dispose();
        } finally {
            if (iconInfo.hbmColor != null) GDI32.INSTANCE.DeleteObject(iconInfo.hbmColor);
            if (iconInfo.hbmMask != null) GDI32.INSTANCE.DeleteObject(iconInfo.hbmMask);
        }

        return img;
    }

    public static byte[] toByteArray(BufferedImage image, String format) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
    // Converts BufferedImage to ICO-format byte array
    public static byte[] getICO(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos); // JavaFX Image can read PNG
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    @FXML
    public void logOut(){

    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to logout?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.setTitle("Confirm Logout");
        CustomPopUp.showThemedConfirm(confirm);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                performLogout();
            }
        });
    }


    private void performLogout() {
        System.out.println("🔒 Logging out user: " + username);

        // Close all games
        GameProcessManager.closeAllGames();
        WebClient webClient = WebClient.builder()
                .baseUrl("http://"+propertiesInfo.getMainServerIp()+":"+propertiesInfo.getServerPort()) // POS API endpoint
                .build();
        // Call logout API
        webClient.post()
                .uri("/api/customer/logout")
                .bodyValue(Map.of("username", username))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(java.time.Duration.ofSeconds(5))
                .onErrorResume(e -> {
                    System.out.println("⚠️ Logout API failed: " + e.getMessage());
                    return Mono.empty();
                })
                .subscribe(response -> {
                    Platform.runLater(() -> {
                        System.out.println("✅ Logout API response: " + response);
                        returnToLoginScreen();
                    });
                });
    }

    private void returnToLoginScreen() {
        try {
            KioskLockUtil.showLoginScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void updateCreditDisplay() {
        long minutes = (long) remainingMinutes;
        long seconds = Math.round((remainingMinutes - minutes) * 60);
        creditLabel.setText(String.format("⏱ %02d:%02d mins left", minutes, seconds));
    }

    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        countdownTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> {
                    remainingMinutes -= 1.0 / 60.0; // reduce 1 second
                    if (remainingMinutes <= 0) {
                        remainingMinutes = 0;
                        creditLabel.setText("⏱ 00:00 mins left");
                        countdownTimeline.stop();
                        showTimeExpiredPopup();
                    } else {
                        updateCreditDisplay();
                    }
                })
        );
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void showTimeExpiredPopup() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Session Time Over");
            alert.setHeaderText(null);
            alert.setContentText("Your time has expired. Logging out...");
            CustomPopUp.showThemedConfirm(alert);
            alert.show(); // ✅ use show() not showAndWait()

            // Delay the logout slightly to let user see popup
            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(event -> {
                alert.close();
                performLogout();
/*                GameProcessManager.closeAllGames();
                KioskLockUtil.showLoginScreen();*/
            });
            delay.play();
        });
    }

}

