package com.example.clientapp.util;


import com.sun.jna.platform.win32.Shell32Util;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
                String path = gameFile.getAbsolutePath();
                System.out.println("🎮 Launching: " + path);

                // Suspend kiosk focus while game is running
                KioskLockUtil.suspendKioskFocus();

                // Use cmd /c start to handle .exe, .bat, .lnk
                Process process =new ProcessBuilder("cmd", "/c", "start", "\"Game\"", "\"" + exePath + "\"")
                        .directory(gameFile.getParentFile())
                        .start();


                synchronized (runningGames) {
                    runningGames.put(gameFile, exeName);
                }

                // Minimize the launcher stage AFTER game starts
                Platform.runLater(() -> KioskLockUtil.minimizeLauncherStage());

                // Monitor game exit (polling by process name)
                if (onGameExit != null) {
                    waitForGameToClose(gameFile, process ,onGameExit);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void waitForGameToClose(File gameFile, Process process, Consumer<File> onGameExit) {
        new Thread(() -> {
            try {
                process.waitFor(); // Wait for this game to exit
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                synchronized (runningGames) {
                    runningGames.remove(process);
                }
                Platform.runLater(() -> {
                    KioskLockUtil.resumeKioskFocus();
                    onGameExit.accept(gameFile);
                });
            }
        }).start();
    }
    public static void closeAllGames() {
        runningGames.forEach((file, exeName) -> {
            try {
                // Force kill via taskkill
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

/*    private static void waitForGameToClose(File gameFile, Consumer<File> onGameExit) {
        String exeName = gameFile.getName();
        boolean running;

        do {
            running = isProcessRunning(exeName);
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
        } while (running);

        System.out.println("🛑 Game closed: " + exeName);
        Platform.runLater(() -> {
            KioskLockUtil.resumeKioskFocus();
            onGameExit.accept(gameFile);
        });
    }*/

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
