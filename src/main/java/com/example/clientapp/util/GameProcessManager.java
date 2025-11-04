package com.example.clientapp.util;

import com.example.clientapp.tests.WindowsShortcut;
import javafx.application.Platform;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GameProcessManager {

    private static File currentGame;
    private static final Map<File, String> runningGames = new ConcurrentHashMap<>();

    public static void launchGame(File gameFile, Stage mainStage, Consumer<File> onGameExit) throws IOException, ParseException {
        if (gameFile == null || !gameFile.exists()) {
            System.err.println("❌ Game file not found: " + (gameFile != null ? gameFile.getAbsolutePath() : "null"));
            return;
        }

        currentGame = gameFile;
        WindowsShortcut windowsShortcut = new WindowsShortcut(gameFile);
        new Thread(() -> {
            try {
                String exePath = gameFile.getAbsolutePath();
                String exeName = windowsShortcut.getDescription()!=null?windowsShortcut.getDescription():"";
                System.out.println("🎮 Launching: " + exePath);

                // ✅ Suspend kiosk focus safely before launching
                Platform.runLater(KioskLockUtil::suspendKioskFocus);

                // ✅ Launch the game in a new process
                Process process = new ProcessBuilder("cmd", "/c", "start", "\"Game\"", "\"" + exePath + "\"")
                        .directory(gameFile.getParentFile())
                        .start();

                synchronized (runningGames) {
                    if(gameFile.getName().contains("Counter") || gameFile.getName().contains("Strike")){exeName="hl";};
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
                System.out.println(file.getName() +" : "+ exeName);
                // PowerShell command to find and kill any process whose name contains exeName (case-insensitive)
                String command = "taskkill /F /IM " + exeName + ".exe";
                // Run the PowerShell command as administrator
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
                pb.redirectErrorStream(true);
                Process kill = pb.start();
                kill.waitFor();

                // Log the process of forcefully closing the game
                System.out.println("⚠️ Game forcefully closed: " + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Clear the runningGames list after closing all tasks
        runningGames.clear();

        // Ensure the kiosk resumes its focus
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
