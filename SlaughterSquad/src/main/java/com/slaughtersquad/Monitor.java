package com.slaughtersquad;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;

public class Monitor {
    private static final String SIGNAL_ROUND_ENDED_FILE_PATH = "SlaughterSquad/target/classes/com/slaughtersquad/sampleRobots/WriterRobot.data/battle_finished_signal.txt";
    private static final String SIGNAL_BATTLE_ENDED_FILE_PATH = "SlaughterSquad/target/classes/com/slaughtersquad/sampleRobots/WriterRobot.data/battle_finished_signal.txt";
    private static final String CLASS_PATH = "SlaughterSquad/target/classes";
    private static final String MAIN_CLASS = "com.slaughtersquad.Main";

    private static FileTime lastModifiedTimeRoundEnded = FileTime.fromMillis(0);
    private static FileTime lastModifiedTimeBattleEnded = FileTime.fromMillis(0);

    public static void main(String[] args) {
        Path roundEndedFilePath = Paths.get(SIGNAL_ROUND_ENDED_FILE_PATH);
        Path battleEndedFilePath = Paths.get(SIGNAL_BATTLE_ENDED_FILE_PATH);

        Thread roundEndedThread = new Thread(
                () -> watchFile(roundEndedFilePath, Monitor::runMainClass, lastModifiedTimeRoundEnded));
        Thread battleEndedThread = new Thread(
                () -> watchFile(battleEndedFilePath, Monitor::runNewBattle, lastModifiedTimeBattleEnded));

        roundEndedThread.start();
        battleEndedThread.start();
    }

    private static void watchFile(Path signalFilePath, Runnable action, FileTime lastModifiedTime) {
        try (WatchService watchService = signalFilePath.getParent().getFileSystem().newWatchService()) {
            signalFilePath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE
                            || event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Path changed = (Path) event.context();
                        if (changed.endsWith(signalFilePath.getFileName())) {
                            FileTime currentModifiedTime = Files.getLastModifiedTime(signalFilePath);
                            if (!currentModifiedTime.equals(lastModifiedTime)) {
                                // Run the corresponding action
                                action.run();

                                // Update the last modified time
                                lastModifiedTime = currentModifiedTime;

                                // Reset the watch key
                                key.reset();
                                break;
                            }
                        }
                    }
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void runMainClass() {
        ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", CLASS_PATH, MAIN_CLASS);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runNewBattle() {
        try {
            Robot robot = new Robot();
            // Press Ctrl+N to start a new battle
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_N);
            robot.keyRelease(KeyEvent.VK_N);
            robot.keyRelease(KeyEvent.VK_CONTROL);
            // Wait for the UI to update
            Thread.sleep(500);
            // Click at the position where the "Start New Battle" button appears
            robot.mouseMove(100, 100); // Adjust coordinates as needed
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } catch (AWTException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
