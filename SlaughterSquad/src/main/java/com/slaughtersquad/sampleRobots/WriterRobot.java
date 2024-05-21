package com.slaughtersquad.sampleRobots;

import robocode.*;

import java.awt.geom.*;
import java.awt.*;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Random;
import com.slaughtersquad.utils.*;

public class WriterRobot extends AdvancedRobot {

    // Object used to write to the log_robocode.txt file
    private volatile RobocodeFileOutputStream fw;

    // Structure to keep the information of the bullets
    // while they don't hit a target, a wall or another bullet
    // This is done since we don't know if the bullet hit the target or not until it
    // disappears
    HashMap<Bullet, Data> bulletsOnAir = new HashMap<>();

    /**
     * Helper class to store the data of the enemy robot
     */
    private class Data {
        String name; // Name of the enemy robot
        Double distance; // Distance between the enemy robot and the writer robot
        Double velocity; // Velocity of the enemy robot

        public Data(String name, Double distance, Double velocity) {
            this.name = name;
            this.distance = distance;
            this.velocity = velocity;
        }
    }

    /**
     * Method that flushes and closes the file stream
     */
    private void closeFileStream() {
        if (fw != null) {
            try {
                fw.flush();
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                fw = null; // Reset fw to null for the next round
            }
        }
    }

    /**
     * Method to write the data to the dataset.csv file
     */
    private void writeToCsvFile() {
        try {
            // Close the file stream
            closeFileStream();

            // Read the log_robocode.txt file
            FileReader fr = new FileReader(getDataFile("log_robocode.txt").getCanonicalPath());
            BufferedReader br = new BufferedReader(fr);

            // Write the data to the dataset.csv file
            RobocodeFileOutputStream pw = new RobocodeFileOutputStream(getDataFile("dataset.csv").getCanonicalPath(),
                    true);

            // Read the file line by line and store it in a string
            String line;

            // Read line by line and print it
            while ((line = br.readLine()) != null) {
                // Write each line to the dataset.csv file
                pw.write((line + "\n").getBytes());
            }

            // Close the file
            pw.flush();
            pw.close();

            // Empty the log file
            fw = new RobocodeFileOutputStream(getDataFile("log_robocode.txt").getCanonicalPath(), false);
            fw.write("".getBytes());

            // Close the reader
            br.close();
            fr.close();

            closeFileStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Executa o método main da classe Main
     */
    private void writeSignalBattleEnded() {
        // Write a signal to a file to indicate the battle has ended
        try (RobocodeFileOutputStream signalFile = new RobocodeFileOutputStream(
                getDataFile("battle_finished_signal.txt"))) {
            signalFile.write("".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();

        try {
            if (fw == null) {
                fw = new RobocodeFileOutputStream(this.getDataFile("log_robocode.txt").getCanonicalPath(), true);
            }

            System.out.println("Writing to: " + fw.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            setAhead(100);
            setTurnLeft(100);
            Random rand = new Random();
            setAllColors(new Color(rand.nextInt(3), rand.nextInt(3), rand.nextInt(3)));
            execute();
        }

    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        super.onScannedRobot(event);

        Point2D.Double coordinates = Utils.getEnemyCoordinates(this, event.getBearing(), event.getDistance());
        System.out.println("Enemy " + event.getName() + " spotted at " + coordinates.x + "," + coordinates.y + "\n");
        Bullet b = fireBullet(3);

        if (b != null) {
            System.out.println("Firing at " + event.getName());
            // guardar os dados do inimigo temporariamente, até que a bala chegue ao
            // destino, para depois os escrever em ficheiro
            bulletsOnAir.put(b, new Data(event.getName(), event.getDistance(), event.getVelocity()));
        } else
            System.out.println("Cannot fire right now...");

    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        super.onBulletHit(event);
        Data d = bulletsOnAir.get(event.getBullet());
        try {
            // testar se acertei em quem era suposto
            // testar se acertei em quem era suposto
            if (event.getName().equals(event.getBullet().getVictim())) {
                if (fw != null) {
                    fw.write((d.name + "," + d.distance + "," + d.velocity + ",hit\n").getBytes());
                }
            } else {
                if (fw != null) {
                    fw.write((d.name + "," + d.distance + "," + d.velocity + ",no_hit\n").getBytes());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        bulletsOnAir.remove(event.getBullet());
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        super.onBulletMissed(event);
        Data d = bulletsOnAir.get(event.getBullet());
        try {
            if (fw != null) {
                fw.write((d.name + "," + d.distance + "," + d.velocity + ",no_hit\n").getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        bulletsOnAir.remove(event.getBullet());
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
        super.onBulletHitBullet(event);
        Data d = bulletsOnAir.get(event.getBullet());
        try {
            if (fw != null) {
                fw.write((d.name + "," + d.distance + "," + d.velocity + ",no_hit\n").getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        bulletsOnAir.remove(event.getBullet());
    }

    @Override
    public void onDeath(DeathEvent event) {
        super.onDeath(event);
    }

    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        super.onRoundEnded(event);

        closeFileStream();
    }

    @Override
    public void onBattleEnded(BattleEndedEvent event) {
        super.onBattleEnded(event);

        writeToCsvFile();

        writeSignalBattleEnded();
    }
}
