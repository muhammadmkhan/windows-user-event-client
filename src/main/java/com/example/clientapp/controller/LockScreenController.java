package com.example.clientapp.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

public class LockScreenController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    private final WebClient webClient = WebClient.create("http://localhost:8989"); // POS system base URL

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both username and password.");
            return;
        }

        webClient.post()
                .uri("/auth/customer")
                .bodyValue(Map.of("username", username, "password", password))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(err -> messageLabel.setText("Connection failed: " + err.getMessage()))
                .subscribe(response -> {
                    if (response.contains("success")) {
                        messageLabel.setStyle("-fx-text-fill: green;");
                        messageLabel.setText("Access Granted!");
                        unlockScreen();
                    } else {
                        messageLabel.setText("Invalid credentials.");
                    }
                });
    }

    private void unlockScreen() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        stage.close(); // hides lock screen
    }
}
