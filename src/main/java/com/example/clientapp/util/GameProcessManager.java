package com.example.clientapp.util;

import javafx.application.Platform;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GameProcessManager {

    private static File currentGame;
    private static final Map<File, String> runningGames = new ConcurrentHashMap<>();

    public static void launchGame(File gameFile, Stage mainStage, Consumer<File> onGameExit) {
        if (gameFile == null || !gameFile.exists()) {
            System.err.println("❌ Game file not found: " + (gameFile != null ? gameFile.getAbsolutePath() : "null"));
            return;
        }

        currentGame = gameFile;

        new Thread(() -> {
            try {
                String exePath = gameFile.getAbsolutePath();
                String exeName = gameFile.getName();
                System.out.println("🎮 Launching: " + exePath);

                // ✅ Suspend kiosk focus safely before launching
                Platform.runLater(KioskLockUtil::suspendKioskFocus);

                // ✅ Launch the game in a new process
                Process process = new ProcessBuilder("cmd", "/c", "start", "\"Game\"", "\"" + exePath + "\"")
                        .directory(gameFile.getParentFile())
                        .start();

                synchronized (runningGames) {
                    runningGames.put(gameFile, exeName);
                }

                // ✅ Minimize kiosk stage *after* the game is successfully launched
                Platform.runLater(() -> {
                    try {
                        KioskLockUtil.minimizeLauncherStage();
                    } catch (Exception e) {
                        System.err.println("⚠️ Could not minimize launcher stage: " + e.getMessage());
                    }
                });

                // ✅ Monitor game closure
                if (onGameExit != null) {
                    waitForGameToClose(gameFile, process, onGameExit);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void waitForGameToClose(File gameFile, Process process, Consumer<File> onGameExit) {
        new Thread(() -> {
            try {
                process.waitFor(); // Wait for game to close
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                synchronized (runningGames) {
                    runningGames.remove(gameFile);
                }

                Platform.runLater(() -> {
                    // ✅ Restore kiosk focus when game exits
                    KioskLockUtil.resumeKioskFocus();
                    onGameExit.accept(gameFile);
                });
            }
        }).start();
    }

    public static void closeAllGames() {
        runningGames.forEach((file, exeName) -> {
            try {
                Process kill = new ProcessBuilder("taskkill", "/f", "/im", exeName).start();
                kill.waitFor();
                System.out.println("⚠️ Game forcefully closed: " + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        runningGames.clear();

        Platform.runLater(KioskLockUtil::resumeKioskFocus);
    }

    private static boolean isProcessRunning(String processName) {
        try {
            Process p = new ProcessBuilder(
                    "cmd", "/c", "tasklist /FI \"IMAGENAME eq " + processName + "\""
            ).start();
            String output = new String(p.getInputStream().readAllBytes());
            return output.toLowerCase().contains(processName.toLowerCase());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
