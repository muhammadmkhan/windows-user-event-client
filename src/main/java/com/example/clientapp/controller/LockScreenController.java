package com.example.clientapp.controller;

import com.example.clientapp.util.KioskLockUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class LockScreenController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private final WebClient webClient;

    public LockScreenController() {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8989") // POS API endpoint
                .build();
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both fields.");
            return;
        }

        webClient.post()
                .uri("/api/customer/auth")
                .bodyValue(Map.of("username", username, "password", password))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorResume(e -> {
                    Platform.runLater(() ->
                            messageLabel.setText("Connection failed: " + e.getMessage()));
                    return Mono.empty();
                })
                .subscribe(response -> Platform.runLater(() -> {
                    if (response != null && response.contains("success")) {
                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("Access Granted!");
                        openMainScreen();
                    } else {
                        messageLabel.setStyle("-fx-text-fill: red;");
                        messageLabel.setText("Invalid credentials.");
                    }
                }));
    }

    private void openMainScreen() {
        try {
            KioskLockUtil.disableKioskMode();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainScreen.fxml"));
            Scene mainScene = new Scene(loader.load());
            stage.setScene(mainScene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
