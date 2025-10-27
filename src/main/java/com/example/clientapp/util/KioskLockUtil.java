package com.example.clientapp.util;

import com.example.clientapp.config.ApplicationContextProvider;
import com.example.clientapp.controller.LockScreenController;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class KioskLockUtil {

    private static boolean explorerKilled = false;
    private static Timer focusEnforcer;
    private static boolean suspendFocus = false;
    public static Stage launcherStage;
    @Autowired
    private static LockScreenController lockScreenController;
    public static void enableKioskMode(Stage stage, Scene scene) {

        launcherStage = stage;
        killExplorer();

        stage.setAlwaysOnTop(true);
        stage.setFullScreen(true);
        stage.setResizable(false);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setOnCloseRequest(event -> event.consume());

        // Focus listener
        stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!suspendFocus && !isNowFocused) {
                Platform.runLater(() -> stage.setFullScreen(true));
            }
        });

        // Timer enforcing focus
        focusEnforcer = new Timer(true);
        focusEnforcer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!suspendFocus && !stage.isFocused()) {
                    Platform.runLater(() -> {
                        stage.toFront();
                        stage.requestFocus();
                    });
                }
            }
        }, 2000, 1000);

        // Block dangerous keys
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            KeyCode code = event.getCode();
            if ((event.isAltDown() && code == KeyCode.TAB) ||
                    (event.isAltDown() && code == KeyCode.F4) ||
                    code == KeyCode.WINDOWS ||
                    code == KeyCode.META ||
                    (event.isControlDown() && code == KeyCode.ESCAPE) ||
                    code == KeyCode.ESCAPE) {
                event.consume();
            }
        });

        System.out.println("🔒 Kiosk mode enabled");
    }

    public static void suspendKioskFocus() {
        suspendFocus = true;
    }

    public static void resumeKioskFocus() {
        suspendFocus = false;
        System.out.println("🔓 Kiosk focus resumed");
    }

    private static void killExplorer() {
        if (explorerKilled) return;
        try {
            Runtime.getRuntime().exec("taskkill /f /im explorer.exe");
            explorerKilled = true;
            System.out.println("🔒 Explorer.exe killed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void disableKioskMode() {
        if (focusEnforcer != null) {
            focusEnforcer.cancel();
        }
        startExplorer();
        System.out.println("🔓 Kiosk mode disabled.");
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

    public static void minimizeLauncherStage() {
        if (launcherStage != null) {
            Platform.runLater(() -> launcherStage.setIconified(true));
        }
    }

    public static void showLoginScreen() {
        Platform.runLater(() -> {
            try {


                // Load login FXML
                FXMLLoader loader = new FXMLLoader(KioskLockUtil.class.getResource("/fxml/LockScreen.fxml"));
                loader.setControllerFactory(ApplicationContextProvider.getContext()::getBean);

                Scene scene = new Scene(loader.load());

                launcherStage.setScene(scene);
/*                launcherStage.setFullScreen(true);
                launcherStage.setAlwaysOnTop(true);
                launcherStage.setResizable(false);
                launcherStage.setFullScreenExitHint("");
                launcherStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
                launcherStage.setOnCloseRequest(Event::consume);*/

                //enableKioskMode(launcherStage, scene); // re-enable kiosk
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
