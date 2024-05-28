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
    private static class Data {
        double currentPositionX; // Current X coordinate of the writer robot
        double currentPositionY; // Current Y coordinate of the writer robot
        double distance; // Distance between the enemy robot and the writer robot
        double velocity; // Velocity of the enemy robot
        double bearing; // Angle between the writer robot's heading and the direction to the enemy
        double futureBearing; // Angle between the writer robot's heading and the direction to the future
        // position of the enemy
        double enemyPositionX; // X coordinate of the enemy robot
        double enemyPositionY; // Y coordinate of the enemy robot
        double predictedEnemyPositionX; // predicted X coordinate of the enemy robot
        double predictedEnemyPositionY; // predicted Y coordinate of the enemy robot
        double gunTurnRemaining;
        double gunHeat; // Gun heat of the writer robot

        public Data(
                double currentPositionX, double currentPositionY, double distance,
                double velocity, double bearing, double futureBearing,
                double enemyPositionX, double enemyPositionY,
                double predictedEnemyPositionX, double predictedEnemyPositionY,
                double gunTurnRemaining, double gunHeat) {
            this.currentPositionX = currentPositionX;
            this.currentPositionY = currentPositionY;
            this.distance = distance;
            this.velocity = velocity;
            this.bearing = bearing;
            this.futureBearing = futureBearing;
            this.enemyPositionX = enemyPositionX;
            this.enemyPositionY = enemyPositionY;
            this.predictedEnemyPositionX = predictedEnemyPositionX;
            this.predictedEnemyPositionY = predictedEnemyPositionY;
            this.gunTurnRemaining = gunTurnRemaining;
            this.gunHeat = gunHeat;
        }
    }

    // Method to calculate predicted position of the enemy robot
    private Point2D.Double predictEnemyPosition(ScannedRobotEvent event, double bulletPower) {
        // Implement your prediction logic here based on the opponent's current
        // position,
        // velocity, and other relevant factors.
        // This is just a placeholder method.
        Point2D.Double enemyPosition = Utils.getEnemyCoordinates(this, event.getBearing(), event.getDistance());

        // Get the enemy's heading in radians
        double enemyHeading = event.getHeadingRadians();

        // Get the enemy's velocity
        double enemyVelocity = event.getVelocity();

        // Calculate the bullet speed
        double bulletSpeed = 20 - 3 * bulletPower;

        // Calculate the time it will take for the bullet to reach the enemy
        double distance = event.getDistance();
        double timeToImpact = distance / bulletSpeed;

        // Predict the enemy's future position using the timeToImpact
        double futureX = enemyPosition.x + Math.sin(enemyHeading) * enemyVelocity * timeToImpact;
        double futureY = enemyPosition.y + Math.cos(enemyHeading) * enemyVelocity * timeToImpact;

        // Return the predicted position
        return new Point2D.Double(futureX, futureY);
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

            // Close the reader
            br.close();
            fr.close();

            // Empty the log file
            fw = new RobocodeFileOutputStream(getDataFile("log_robocode.txt").getCanonicalPath(), false);
            fw.write("".getBytes());

            closeFileStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that writes the data to the log_robocode.txt file
     *
     * @param fw File output stream
     * @param d  Data to write to the file
     */
    private void writeHitsToFile(RobocodeFileOutputStream fw, Data d) {
        try {
            fw.write((d.currentPositionX + ";" + d.currentPositionY + ";" + d.distance + ";" + d.velocity + ";"
                    + d.bearing + ";" + d.futureBearing + ";" + d.enemyPositionX + ";"
                    + d.enemyPositionY + ";" + d.predictedEnemyPositionX + ";" + d.predictedEnemyPositionY + ";"
                    + d.gunTurnRemaining + ";" + d.gunHeat + ";" + "hit\n")
                    .getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that writes the data to the log_robocode.txt file
     *
     * @param fw File output stream
     * @param d  Data to write to the file
     */
    private void writeNoHitsToFile(RobocodeFileOutputStream fw, Data d) {
        try {
            fw.write((d.currentPositionX + ";" + d.currentPositionY + ";" + d.distance + ";" + d.velocity + ";"
                    + d.bearing + ";" + d.futureBearing + ";" + d.enemyPositionX + ";"
                    + d.enemyPositionY + ";" + d.predictedEnemyPositionX + ";" + d.predictedEnemyPositionY + ";"
                    + d.gunTurnRemaining + ";" + d.gunHeat + ";" + "no_hit\n")
                    .getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Escreve para um ficheiro para indicar que a ronda terminou
     */
    private void writeSignalRoundEnded() {
        // Write a signal to a file to indicate the round has ended
        try (RobocodeFileOutputStream signalFile = new RobocodeFileOutputStream(
                getDataFile("round_finished_signal.txt"))) {
            signalFile.write("".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Escreve para um ficheiro para indicar que a batalha terminou
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

        setAdjustGunForRobotTurn(true);

        while (true) {
            setAhead(70);
            setTurnLeft(90);
            Random rand = new Random();
            setAllColors(new Color(rand.nextInt(3), rand.nextInt(3), rand.nextInt(3)));
            execute();
        }

    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        super.onScannedRobot(event);

        Point2D.Double enemyCoordinates = Utils.getEnemyCoordinates(this, event.getBearing(), event.getDistance());
        System.out.println(
                "Enemy " + event.getName() + " spotted at " + enemyCoordinates.x + "," + enemyCoordinates.y + "\n");

        double firePower = Math.min(500 / event.getDistance(), 3);

        // Turn towards the predicted enemy position
        Point2D.Double predictedEnemyCoordinates = predictEnemyPosition(event, firePower);

        // Calculate the angle to the predicted enemy position
        double absDeg = Utils.absoluteBearing(getX(), getY(), predictedEnemyCoordinates.x, predictedEnemyCoordinates.y);
        double normalizedAbsDeg = Utils.normalizeBearing(absDeg - getGunHeading());

        setTurnGunRight(normalizedAbsDeg);

        System.out.println("Setting the gun to " + absDeg + " degrees\n");

        Bullet b = null;

        // Fire at the predicted enemy position
        if (Math.abs(getGunTurnRemaining()) < 5) {
            b = setFireBullet(firePower);
        }

        if (b != null) {
            System.out.println("Firing at " + event.getName());

            bulletsOnAir.put(b,
                    new Data(this.getX(), this.getY(), Utils.getDistance(this, enemyCoordinates.x, enemyCoordinates.y),
                            event.getVelocity(),
                            event.getBearing(), normalizedAbsDeg, enemyCoordinates.x, enemyCoordinates.y,
                            predictedEnemyCoordinates.x, predictedEnemyCoordinates.y, getGunTurnRemaining(), getGunHeat()));
        } else
            System.out.println("Cannot fire right now...");

    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        super.onBulletHit(event);
        Data d = bulletsOnAir.get(event.getBullet());
        if (event.getName().equals(event.getBullet().getVictim())) {
            if (fw != null) {
                writeHitsToFile(fw, d);
            }
        } else {
            if (fw != null) {
                writeNoHitsToFile(fw, d);
            }
        }

        bulletsOnAir.remove(event.getBullet());
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        super.onBulletMissed(event);
        Data d = bulletsOnAir.get(event.getBullet());

        if (fw != null) {
            writeNoHitsToFile(fw, d);
        }

        bulletsOnAir.remove(event.getBullet());
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
        super.onBulletHitBullet(event);
        Data d = bulletsOnAir.get(event.getBullet());

        if (fw != null) {
            writeNoHitsToFile(fw, d);
        }

        bulletsOnAir.remove(event.getBullet());
    }

    @Override
    public void onDeath(DeathEvent event) {
        super.onDeath(event);

        closeFileStream();
    }

    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        super.onRoundEnded(event);

        writeToCsvFile();

        writeSignalRoundEnded();

    }

    @Override
    public void onBattleEnded(BattleEndedEvent event) {
        super.onBattleEnded(event);

        writeToCsvFile();

        writeSignalRoundEnded();

        writeSignalBattleEnded();
    }
}
