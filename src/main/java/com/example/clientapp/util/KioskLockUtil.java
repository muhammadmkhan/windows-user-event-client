package com.example.clientapp.util;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class KioskLockUtil {

    private static boolean explorerKilled = false;
    private static Timer focusEnforcer;

    /**
     * Initialize full kiosk mode:
     * - Kills Explorer.exe (removes taskbar and desktop)
     * - Blocks key combos (Alt+Tab, Win, Ctrl+Esc, Alt+F4)
     * - Forces focus back to stage continuously
     */
    public static void enableKioskMode(Stage stage, Scene scene) {
        killExplorer();

        stage.setOnCloseRequest(Event::consume);
        stage.setAlwaysOnTop(true);
        stage.setFullScreen(true);
        stage.setResizable(false);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);

        // Force back into fullscreen if something tries to minimize or exit
        stage.fullScreenProperty().addListener((obs, wasFull, isFull) -> {
            if (!isFull) Platform.runLater(() -> stage.setFullScreen(true));
        });

        // Maintain focus always
        stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) Platform.runLater(stage::requestFocus);
        });

        // Keep enforcing focus every second
        focusEnforcer = new Timer(true);
        focusEnforcer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (!stage.isFocused()) {
                        stage.toFront();
                        stage.requestFocus();
                    }
                });
            }
        }, 2000, 1000);

        // Block all dangerous keys including ESC before stage processes them
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();

            if ((event.isAltDown() && code == KeyCode.TAB) ||    // Alt+Tab
                    (event.isAltDown() && code == KeyCode.F4) ||     // Alt+F4
                    (code == KeyCode.WINDOWS) ||                     // Windows key
                    (code == KeyCode.META) ||                        // Windows key (meta)
                    (event.isControlDown() && code == KeyCode.ESCAPE) || // Ctrl+Esc
                    (code == KeyCode.ESCAPE)) {                      // Plain ESC
                event.consume();
            }
        });

        // 🔒 Also block ESC at scene level after bubbling
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) event.consume();
        });

        System.out.println("🔒 Kiosk mode enabled — Escape fully disabled.");
    }


    /**
     * Restores Windows Explorer when the user unlocks
     */
    public static void disableKioskMode() {
        if (focusEnforcer != null) {
            focusEnforcer.cancel();
        }
        startExplorer();
        System.out.println("🔓 Kiosk mode disabled.");
    }

    private static void killExplorer() {
        if (explorerKilled) return;
        try {
            Runtime.getRuntime().exec("taskkill /f /im explorer.exe");
            explorerKilled = true;
            System.out.println("🔒 Explorer.exe killed — full lock mode active.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startExplorer() {
        try {
            // ✅ Start Windows Explorer (desktop + taskbar)
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start explorer.exe");
            pb.inheritIO();
            pb.start();

            explorerKilled = false;
            System.out.println("🔓 Explorer.exe restarted — desktop and taskbar restored.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
