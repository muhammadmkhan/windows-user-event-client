package com.example.clientapp.util;

import com.example.clientapp.config.ApplicationContextProvider;
import com.example.clientapp.controller.MainScreenController;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class KioskLockUtil {

    private static boolean explorerKilled = false;
    private static Timer focusEnforcer;
    private static boolean suspendFocus = false;
    private static Stage launcherStage;
    private static boolean kioskActive = false;

    // ✅ Initialize stage once at app start
    public static void initializeLauncher(Stage stage) {
        launcherStage = stage;
    }

    public static void enableKioskMode(Stage stage, Scene scene) {
        launcherStage = stage;
        killExplorer();


        stage.initStyle(StageStyle.UNDECORATED);
        stage.setAlwaysOnTop(true);
        stage.setFullScreen(true);
        stage.setResizable(false);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setOnCloseRequest(Event::consume);


        // Enforce focus
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
            if ((event.isAltDown() && code == KeyCode.TAB)
                    || (event.isAltDown() && code == KeyCode.F4)
                    || code == KeyCode.WINDOWS
                    || code == KeyCode.META
                    || (event.isControlDown() && code == KeyCode.ESCAPE)
                    || code == KeyCode.ESCAPE) {
                event.consume();
            }
        });

        System.out.println("🔒 Kiosk mode enabled");
    }

    public static void disableLauncher() {
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
            System.out.println("🔒 Explorer.exe killed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startExplorer() {
        try {
            new ProcessBuilder("cmd", "/c", "start explorer.exe").inheritIO().start();
            explorerKilled = false;
            System.out.println("🔓 Explorer.exe restarted");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showLoginScreen() {
        if (launcherStage == null) {
            System.err.println("❌ Launcher stage not initialized. Call initializeLauncher(stage) first.");
            return;
        }

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(KioskLockUtil.class.getResource("/fxml/LockScreen.fxml"));
                loader.setControllerFactory(ApplicationContextProvider.getContext()::getBean);

                Scene scene = new Scene(loader.load());
                launcherStage.setScene(scene);

                enableKioskMode(launcherStage, scene);
                launcherStage.show();

                System.out.println("🔐 Lock screen shown again.");

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void showMainScreen(String username) {
        if (launcherStage == null) {
            System.err.println("❌ Launcher stage not initialized. Call initializeLauncher(stage) first.");
            return;
        }

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(KioskLockUtil.class.getResource("/fxml/MainScreen.fxml"));
                loader.setControllerFactory(ApplicationContextProvider.getContext()::getBean);

                Scene scene = new Scene(loader.load());

                MainScreenController controller = loader.getController();
                controller.setUsername(username);

                launcherStage.setScene(scene);
                enableKioskMode(launcherStage, scene);
                launcherStage.show();

                System.out.println("🖥️ Main screen shown.");

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Enable kiosk mode (disable window decorations, fullscreen, etc.)
     */
    public static void enableKioskMode(Stage stage) {
        if (stage == null) return;
        launcherStage = stage;
        kioskActive = true;

        Platform.runLater(() -> {
            stage.setFullScreen(true);
            stage.setAlwaysOnTop(true);
            stage.setResizable(false);
            stage.setFullScreenExitHint("");
            stage.setFullScreenExitKeyCombination(null);
        });

        System.out.println("🟢 Kiosk mode enabled.");
    }

    /**
     * Disable kiosk mode (restore normal behavior)
     */
    public static void disableKioskMode() {
        if (launcherStage == null) return;
        kioskActive = false;

        Platform.runLater(() -> {
            launcherStage.setFullScreen(false);
            launcherStage.setAlwaysOnTop(false);
        });

        System.out.println("🔴 Kiosk mode disabled.");
    }

    /**
     * Temporarily suspend kiosk focus (for launching external apps/games)
     */
    public static void suspendKioskFocus() {
        if (!kioskActive || launcherStage == null) return;

        Platform.runLater(() -> {
            launcherStage.setAlwaysOnTop(false);
            System.out.println("🟡 Kiosk focus suspended (allowing external app).");
        });
    }

    /**
     * Restore kiosk focus after a game or external app closes
     */
    public static void resumeKioskFocus() {
        if (!kioskActive || launcherStage == null) return;

        Platform.runLater(() -> {
            launcherStage.setAlwaysOnTop(true);
            launcherStage.toFront();
            launcherStage.requestFocus();
            System.out.println("🟢 Kiosk focus resumed.");
        });
    }

    /**
     * Minimize the main launcher (used when launching a game)
     */
    public static void minimizeLauncherStage() {
        if (launcherStage == null) return;

        Platform.runLater(() -> {
            launcherStage.setIconified(true);
            System.out.println("🟠 Launcher minimized.");
        });
    }

    /**
     * Bring back the main launcher when a game closes
     */
    public static void restoreLauncherStage() {
        if (launcherStage == null) return;

        Platform.runLater(() -> {
            launcherStage.setIconified(false);
            launcherStage.toFront();
            launcherStage.requestFocus();
            System.out.println("🔵 Launcher restored.");
        });
    }


}
