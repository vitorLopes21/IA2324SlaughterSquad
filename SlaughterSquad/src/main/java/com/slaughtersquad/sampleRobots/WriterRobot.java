package com.slaughtersquad.sampleRobots;

import com.slaughtersquad.utils.*;
import robocode.*;
import java.awt.geom.*;
import java.awt.*;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.util.Random;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * This robot writes data to a file for later analysis
 */
public class WriterRobot extends AdvancedRobot {

    // Constants
    private static final int CELL_SIZE = 50;
    private static final int HIGH_RISK_SCORE = 50;
    private static final int MEDIUM_RISK_SCORE = 5;
    private static final int LOW_RISK_SCORE = 3;
    private static final int MIN_RISK_SCORE = 0;

    // Grid dimensions
    private int gridWidth;
    private int gridHeight;
    private int[][] riskGrid;

    // Data collection
    List<Data> data = new ArrayList<Data>();

    // Enemy positions
    private Map<String, Point2D.Double> enemyPositions = new HashMap<>();

    // File writer
    private volatile RobocodeFileOutputStream fw;

    /**
     * Data class to store the data
     */
    private static class Data {
        double myPositionX; // Current X coordinate of the writer robot
        double myPositionY;
        double myFuturePositionX;
        double myFuturePositionY;
        double myVelocity; // Velocity of the writer robot
        double myHeading; // Heading of the writer robot
        double enemyPositionX; // X coordinate of the enemy robot
        double enemyPositionY;
        double enemyPredictedPositionX;
        double enemyPredictedPositionY;
        double enemyDistance; // Distance between the enemy robot and the writer robot
        double enemyBearing; // Angle between the enemy robot's heading and the direction to the writer robot
        double enemyPredictedBearing;
        LocalDateTime time; // Last time the scan was made, to indicate time of the event
        double destinationCellX; // X coordinate of the cell the writer robot is heading to
        double destinationCellY;
        int risk; // Risk score of the cell

        /**
         * Constructor for the Data class
         */
        public Data(
                double myPositionX, double myPositionY, double myFuturePositionX, double myFuturePositionY,
                double myVelocity,
                double myHeading, double enemyPositionX, double enemyPositionY,
                double enemyPredictedPositionX, double enemyPredictedPositionY,
                double enemyDistance, double enemyBearing,
                double enemyPredictedBearing, LocalDateTime time, double destinationCellX, double destinationCellY,
                int risk, String isRisky) {
            this.myPositionX = myPositionX;
            this.myPositionY = myPositionY;
            this.myFuturePositionX = myFuturePositionX;
            this.myFuturePositionY = myFuturePositionY;
            this.myVelocity = myVelocity;
            this.myHeading = myHeading;
            this.enemyPositionX = enemyPositionX;
            this.enemyPositionY = enemyPositionY;
            this.enemyPredictedPositionX = enemyPositionX;
            this.enemyPositionY = enemyPredictedPositionY;
            this.enemyDistance = enemyDistance;
            this.enemyBearing = enemyBearing;
            this.enemyPredictedBearing = enemyPredictedBearing;
            this.time = time;
            this.destinationCellX = destinationCellX;
            this.destinationCellY = destinationCellY;
            this.risk = risk;
        }
    }

    /**
     * Method that predicts the enemy's future position based on the bullet power
     * 
     * @param event
     * @param bulletPower
     * @return
     */
    private Point2D.Double predictEnemyPosition(ScannedRobotEvent event, double bulletPower) {

        // Get the enemy's coordinates
        Point2D.Double enemyPosition = Utils.getEnemyCoordinates(this, event);

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
     * Method that predicts the enemy's future bearing based on the future positions
     * 
     * @param event            The event containing information about the scanned
     *                         robot
     * @param bulletPower      The power of the bullet to be fired
     * @param enemyCoordinates The current coordinates of the enemy robot
     * @return The predicted bearing of the enemy robot
     */
    private double predictEnemyBearing(ScannedRobotEvent event, double bulletPower, Point2D.Double enemyCoordinates) {

        // Calculate the bullet speed
        double bulletSpeed = 20 - 3 * bulletPower;

        // Calculate the time it will take for the bullet to reach the enemy
        double distance = event.getDistance();
        double timeToImpact = distance / bulletSpeed;

        // Predict enemy position based on their current heading and velocity
        double enemyHeading = event.getHeadingRadians();
        double enemyVelocity = event.getVelocity();
        double enemyFutureX = enemyCoordinates.x + Math.sin(enemyHeading) * enemyVelocity * timeToImpact;
        double enemyFutureY = enemyCoordinates.y + Math.cos(enemyHeading) * enemyVelocity * timeToImpact;

        // Predict my future position
        double futureX = this.getX() + Math.sin(getHeadingRadians()) * getVelocity() * timeToImpact;
        double futureY = this.getY() + Math.cos(getHeadingRadians()) * getVelocity() * timeToImpact;

        // Calculate the enemy's future bearing to my future position
        double absDeg = Utils.absoluteBearing(enemyFutureX, enemyFutureY, futureX, futureY);

        return absDeg;
    }

    /**
     * Method that calculates the destination cell based on the risk grid
     * 
     * @param event The event containing information about the scanned robot
     * @return Point2D.Double The destination cell coordinates
     */
    private Point2D.Double getDestinationCell(ScannedRobotEvent event) {
        // Current position of the robot
        Point2D.Double currentPosition = new Point2D.Double(getX(), getY());

        // Initialize variables to track the best cell
        Point2D.Double bestCell = null;
        int lowestRisk = Integer.MAX_VALUE;

        // Iterate over the risk grid to find the cell with the lowest risk
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                System.out.print(riskGrid[x][y] + " ");
                int risk = riskGrid[x][y];
                if (risk < lowestRisk) {
                    lowestRisk = risk;
                    bestCell = new Point2D.Double(x * CELL_SIZE, y * CELL_SIZE);
                } else if (risk == lowestRisk) {
                    // If the risk is the same, choose the closer cell to the current position
                    Point2D.Double cell = new Point2D.Double(x * CELL_SIZE, y * CELL_SIZE);
                    if (currentPosition.distance(cell) < currentPosition.distance(bestCell)) {
                        bestCell = cell;
                    }
                }
            }

            System.out.println("");
        }

        // Add randomness to the selected destination cell
        Random random = new Random();
        double offsetX = (random.nextDouble() - 0.5) * CELL_SIZE * 2; // Increased randomness
        double offsetY = (random.nextDouble() - 0.5) * CELL_SIZE * 2; // Increased randomness

        // Calculate new destination with randomness
        double newDestX = bestCell.x + offsetX;
        double newDestY = bestCell.y + offsetY;

        // Ensure the destination is within the battlefield boundaries
        newDestX = Math.max(Math.min(newDestX, getBattleFieldWidth() - CELL_SIZE), CELL_SIZE);
        newDestY = Math.max(Math.min(newDestY, getBattleFieldHeight() - CELL_SIZE), CELL_SIZE);

        System.out.println("Destination cell: " + newDestX + "," + newDestY + "\n");

        // Return the best cell found
        return bestCell != null ? new Point2D.Double(newDestX, newDestY) : new Point2D.Double(0, 0);
    }

    private void updateRiskGrid() {
        // Reset the risk grid to initial state
        assignInitialRiskScores();

        // Update risks based on known enemy positions
        for (Point2D.Double enemyPos : enemyPositions.values()) {
            int gridX = (int) (enemyPos.x / CELL_SIZE);
            int gridY = (int) (enemyPos.y / CELL_SIZE);

            if (gridX >= 0 && gridX < gridWidth && gridY >= 0 && gridY < gridHeight) {
                // Assign high risk to the enemy's current cell
                riskGrid[gridX][gridY] += 10;

                // Assign high risk to surrounding cells in a circular pattern
                int radius = 8;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {

                        int adjX = gridX + dx;
                        int adjY = gridY + dy;

                        if (adjX >= 0 && adjX < gridWidth && adjY >= 0 && adjY < gridHeight) {

                            double distance = Math.sqrt(dx * dx + dy * dy);

                            if (distance <= radius) {

                                // ((int) 10 * (1 - distance / radius)));
                                riskGrid[adjX][adjY] += (int) (6 * (1 - distance / radius));
                            }
                        }
                    }
                }
            }
        }

        // Update risks around my current position
        int myGridX = (int) (getX() / CELL_SIZE);
        int myGridY = (int) (getY() / CELL_SIZE);

        if (myGridX >= 0 && myGridX < gridWidth && myGridY >= 0 && myGridY < gridHeight) {
            // Assign high risk to my current cell
            riskGrid[myGridX][myGridY] += 5;

            // Assign medium risk to surrounding cells in a circular pattern
            int myRadius = 8;
            for (int dx = -myRadius; dx <= myRadius; dx++) {
                for (int dy = -myRadius; dy <= myRadius; dy++) {
                    int adjX = myGridX + dx;
                    int adjY = myGridY + dy;
                    if (adjX >= 0 && adjX < gridWidth && adjY >= 0 && adjY < gridHeight) {

                        double distance = Math.sqrt(dx * dx + dy * dy);

                        if (distance <= myRadius) {
                            riskGrid[adjX][adjY] += (int) (3 * (1 - distance / myRadius));
                        }
                    }
                }
            }
        }

        // Loop through risk grid so we can see the values after we made changes
        System.out.println("Risk grid after update:");
        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                System.out.print(riskGrid[i][j] + " ");
            }
            System.out.println();
        }
    }

    /**
     * Method that assigns initial risk scores to the cells in the grid
     */
    private void assignInitialRiskScores() {
        // Assign medium risk to all cells

        for (int i = 0; i < gridWidth; i++) {
            for (int j = 0; j < gridHeight; j++) {
                riskGrid[i][j] = HIGH_RISK_SCORE;
            }
        }

        /*
         * // Assign high risk to a circular area in the center of the map, 2/4 of the
         * map
         * int centerX = gridWidth / 2;
         * int centerY = gridHeight / 2;
         * int radius = Math.min(gridWidth, gridHeight) / 4; // Adjust the radius as
         * needed
         * 
         * for (int i = 0; i < gridWidth; i++) {
         * for (int j = 0; j < gridHeight; j++) {
         * if (Math.sqrt(Math.pow(i - centerX, 2) + Math.pow(j - centerY, 2)) <= radius)
         * {
         * riskGrid[i][j] = HIGH_RISK_SCORE;
         * }
         * }
         * }
         */

        // Assign minimum risk to corners, triangles occupying 1/12 of the map each
        int triangleHeight = gridHeight * 2 / 3;
        int triangleWidth = gridWidth * 2 / 3;

        for (int i = 0; i < triangleWidth; i++) {
            for (int j = 0; j < triangleHeight - i; j++) {
                // Bottom-left corner
                riskGrid[i][j] = MIN_RISK_SCORE;
                // Bottom-right corner
                riskGrid[gridWidth - 1 - i][j] = MIN_RISK_SCORE;
                // Top-left corner
                riskGrid[i][gridHeight - 1 - j] = MIN_RISK_SCORE;
                // Top-right corner
                riskGrid[gridWidth - 1 - i][gridHeight - 1 - j] = MIN_RISK_SCORE;
            }
        }

        // Assign high risk to the cells adjacent to walls
        for (int i = 0; i < gridWidth; i++) {
            riskGrid[i][0] = MIN_RISK_SCORE;
            riskGrid[i][gridHeight - 1] = MIN_RISK_SCORE;

            riskGrid[i][1] = MIN_RISK_SCORE;
            riskGrid[i][gridHeight - 2] = MIN_RISK_SCORE;
        }
        for (int j = 0; j < gridHeight; j++) {
            riskGrid[0][j] = MIN_RISK_SCORE;
            riskGrid[gridWidth - 1][j] = MIN_RISK_SCORE;

            riskGrid[1][j] = MIN_RISK_SCORE;
            riskGrid[gridWidth - 2][j] = MIN_RISK_SCORE;
        }

    }

    private void assignRiskBasedOnFutureEnemyShot(ScannedRobotEvent event, double myFutureX, double myFutureY,
            Point2D.Double enemyFuturePosition, double futureEnemyGunBearing) {
        // My future position
        Point2D.Double myFuturePosition = new Point2D.Double(myFutureX, myFutureY);

        // Calculate the enemy's gun bearing in radians
        double enemyGunBearingRadians = Math.toRadians(futureEnemyGunBearing);

        // Calculate the distance between the enemy and my future position
        double distance = Utils.getDistance(myFutureX, myFutureY, enemyFuturePosition.x, enemyFuturePosition.y);
        System.out.println("Distance: " + distance);

        // Calculate the angle between the enemy's gun bearing and the line to my future
        // position
        double deltaX = myFuturePosition.x - enemyFuturePosition.x;
        double deltaY = myFuturePosition.y - enemyFuturePosition.y;
        double angleToMyFuturePosition = Math.atan2(deltaY, deltaX);
        double angleDifference = Math.abs(enemyGunBearingRadians - angleToMyFuturePosition);

        // Normalize the angle difference to the range [0, π]
        angleDifference = Math.min(angleDifference, 2 * Math.PI - angleDifference);

        // If the distance is within a certain threshold and the angle difference is
        // small, mark it as risky
        double distanceThreshold = 250; // Adjust this threshold as needed
        double angleThreshold = Math.toRadians(90); // 10 degrees tolerance, adjust as needed

        if (distance < distanceThreshold && angleDifference < angleThreshold) {
            // Mark the future position as risky
            int futureGridX = (int) (myFuturePosition.x / CELL_SIZE);
            int futureGridY = (int) (myFuturePosition.y / CELL_SIZE);

            if (futureGridX >= 0 && futureGridX < gridWidth && futureGridY >= 0 && futureGridY < gridHeight) {
                riskGrid[futureGridX][futureGridY] += 0.1;
            }
        }
    }

    @Override
    public void run() {
        super.run();

        // Initialize grid based on dynamic arena size
        gridWidth = (int) Math.ceil(getBattleFieldWidth() / CELL_SIZE);
        gridHeight = (int) Math.ceil(getBattleFieldHeight() / CELL_SIZE);
        riskGrid = new int[gridWidth][gridHeight];

        // Open the file stream
        try {
            if (fw == null) {
                File logRobocodeTxt = this.getDataFile("log_robocode.txt");
                fw = new RobocodeFileOutputStream(logRobocodeTxt.getCanonicalPath(), true);
                /*
                 * // Write the column names to the file
                 * if (logRobocodeTxt.length() == 0) {
                 * fw.write(
                 * "myPositionX;myPositionY;myFuturePositionX;myFuturePositionY;myVelocity;myHeading;enemyPositionX;enemyPositionY;enemyPredictedPositionX;enemyPredictedPositionY;enemyDistance;enemyBearing;enemyPredictedBearing;time;destinationCellX;destinationCellY;risk;isRisky;"
                 * .getBytes());
                 * for (int i = 0; i < gridWidth; i++) {
                 * for (int j = 0; j < gridHeight; j++) {
                 * fw.write((i + "," + j + ";").getBytes());
                 * }
                 * fw.write("\n".getBytes());
                 * }
                 * 
                 * // System.out.println("Writing to: " + fw.getName());
                 * }
                 */
            }
        } catch (IOException e) {
            // e.printStackTrace();
        }

        // Allow the gun and radar to turn independently
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForRobotTurn(true);

        // Start scanning continuously
        setTurnRadarRight(Double.POSITIVE_INFINITY);

        // Assign initial risk scores
        assignInitialRiskScores();

        // Main loop
        while (true) {
            // Move towards the safest destination
            Point2D.Double destinationCell = getDestinationCell(null);
            Utils.advancedRobotGoTo(this, destinationCell.x, destinationCell.y);
            execute();
        }

    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        super.onScannedRobot(event);

        // Get enemy coordinates
        Point2D.Double enemyCoordinates = Utils.getEnemyCoordinates(this, event);

        // Update enemy position
        enemyPositions.put(event.getName(), enemyCoordinates);

        // Update the risk grid based on all known enemy positions
        updateRiskGrid();

        // Predict enemy position
        double firePower = Math.min(500 / event.getDistance(), 3);
        Point2D.Double predictedEnemyCoordinates = predictEnemyPosition(event, firePower);

        // Predict enemy bearing
        double predictedEnemyBearing = predictEnemyBearing(event, firePower, enemyCoordinates);

        // Predict my future position
        double futureX = this.getX() + Math.sin(getHeadingRadians()) * getVelocity();
        double futureY = this.getY() + Math.cos(getHeadingRadians()) * getVelocity();

        // Assign risk based on enemy bearing, based on his future position and bearing,
        // if it intercepts with my future position then it's risky
        assignRiskBasedOnFutureEnemyShot(event, futureX, futureY, predictedEnemyCoordinates, predictedEnemyBearing);

        // Calculate the destination cell and get the risk score
        Point2D.Double destinationCell = getDestinationCell(event);
        int riskScore = riskGrid[(int) (destinationCell.x / CELL_SIZE)][(int) (destinationCell.y / CELL_SIZE)];

        // Save into list
        if (riskScore >= 8) {
            data.add(new Data(
                    this.getX(), this.getY(), futureX, futureY, this.getVelocity(), this.getHeading(),
                    enemyCoordinates.x, enemyCoordinates.y, predictedEnemyCoordinates.x, predictedEnemyCoordinates.y,
                    event.getDistance(), event.getBearing(), predictedEnemyBearing, LocalDateTime.now(),
                    destinationCell.x,
                    destinationCell.y, riskScore,
                    "risky"));
        } else {
            data.add(new Data(
                    this.getX(), this.getY(), futureX, futureY, this.getVelocity(), this.getHeading(),
                    enemyCoordinates.x, enemyCoordinates.y, predictedEnemyCoordinates.x, predictedEnemyCoordinates.y,
                    event.getDistance(), event.getBearing(), predictedEnemyBearing, LocalDateTime.now(),
                    destinationCell.x,
                    destinationCell.y, riskScore,
                    "notRisky"));
        }
    }

    /**
     * Method that is called when the robot is hit by a bullet
     */
    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        super.onHitByBullet(event);

        // System.out.println("Hit by bullet! " + event.getBullet().getName() + " at " +
        // event.getBullet().getX() + ","
        // + event.getBullet().getY() + "\n");

        // Get current position
        int currentGridX = (int) (getX() / CELL_SIZE);
        int currentGridY = (int) (getY() / CELL_SIZE);

        // Predict future position
        double futureX = this.getX() + Math.sin(getHeadingRadians()) * getVelocity();
        double futureY = this.getY() + Math.cos(getHeadingRadians()) * getVelocity();
        int futureGridX = (int) (futureX / CELL_SIZE);
        int futureGridY = (int) (futureY / CELL_SIZE);

        // Update risk in a circular pattern around current position
        int radius = 5;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int adjX = currentGridX + dx;
                int adjY = currentGridY + dy;

                if (adjX >= 0 && adjX < gridWidth && adjY >= 0 && adjY < gridHeight) {

                    double distance = Math.sqrt(dx * dx + dy * dy);

                    if (distance <= radius) {
                        System.out.println("Risk: "
                                + ((int) 10 * (1 - distance / radius)));
                        riskGrid[adjX][adjY] += (int) (10 * (1 - distance / radius));
                    }
                }

            }
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {

                int adjX = futureGridX + dx;
                int adjY = futureGridY + dy;
                if (adjX >= 0 && adjX < gridWidth && adjY >= 0 && adjY < gridHeight) {
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= radius) {
                        System.out.println("Risk: " + ((int) 8 * (1 - distance / radius)));
                        riskGrid[adjX][adjY] += (int) (8 * (1 - distance / radius));
                    }
                }
            }
        }

        Data d = data.get(data.size() - 1);

        if (event.getName().equals(this.getName())) {
            if (fw != null) {
                writeColumnsToFile(d, riskGrid);
            }
        }

    }

    /**
     * Method that is called when the robot hits a wall
     */
    @Override
    public void onHitWall(HitWallEvent event) {
        super.onHitWall(event);

        System.out.println("Someone hit a wall!\n");

    }

    /**
     * Method that is called when the robot hits another robot
     */
    @Override
    public void onHitRobot(HitRobotEvent event) {
        super.onHitRobot(event);

        // System.out.println("Hit the robot! " + event.getName() + " at " +
        // event.getEnergy() + "\n");

        // Get current position
        int currentGridX = (int) (getX() / CELL_SIZE);
        int currentGridY = (int) (getY() / CELL_SIZE);

        // Predict future position
        double futureX = this.getX() + Math.sin(getHeadingRadians()) * getVelocity();
        double futureY = this.getY() + Math.cos(getHeadingRadians()) * getVelocity();
        int futureGridX = (int) (futureX / CELL_SIZE);
        int futureGridY = (int) (futureY / CELL_SIZE);

        // Update risk in a circular pattern around current position
        int radius = 5;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int adjX = currentGridX + dx;
                int adjY = currentGridY + dy;

                if (adjX >= 0 && adjX < gridWidth && adjY >= 0 && adjY < gridHeight) {

                    double distance = Math.sqrt(dx * dx + dy * dy);

                    if (distance <= radius) {
                        System.out.println("Risk: "
                                + ((int) 10 * (1 - distance / radius)));
                        riskGrid[adjX][adjY] += (int) (10 * (1 - distance / radius));
                    }
                }

            }
        }

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {

                int adjX = futureGridX + dx;
                int adjY = futureGridY + dy;
                if (adjX >= 0 && adjX < gridWidth && adjY >= 0 && adjY < gridHeight) {
                    double distance = Math.sqrt(dx * dx + dy * dy);
                    if (distance <= radius) {
                        System.out.println("Risk: " + ((int) 8 * (1 - distance / radius)));
                        riskGrid[adjX][adjY] += (int) (8 * (1 - distance / radius));
                    }
                }
            }
        }

        // Save the data to the file
        Data d = data.get(data.size() - 1);

        writeColumnsToFile(d, riskGrid);

        if (event.getName().equals(this.getName())) {
            if (fw != null) {
                writeColumnsToFile(d, riskGrid);
            }
        }

    }

    /**
     * Method that is called when the robot dies
     */
    @Override
    public void onDeath(DeathEvent event) {
        super.onDeath(event);

        System.out.println("I am become death!\n");

        closeFileStream();
    }

    /**
     * Method that is called when the round ends
     */
    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        super.onRoundEnded(event);

        writeToCsvFile();

        writeSignalRoundEnded();

    }

    /**
     * Method that is called when the battle ends
     */
    @Override
    public void onBattleEnded(BattleEndedEvent event) {
        super.onBattleEnded(event);

        writeToCsvFile();

        writeSignalRoundEnded();

        writeSignalBattleEnded();
    }

    /**
     * Method that writes the grid to a file
     * 
     * @param riskGrid
     */
    private void writeColumnsToFile(Data d, int[][] riskGrid) {

        try {
            // Write the data to the file
            if (d.risk > 5) {
                fw.write((d.myPositionX + ";" + d.myPositionY + ";" + d.myFuturePositionX + ";" + d.myFuturePositionY
                        + ";"
                        + d.myVelocity + ";" + d.myHeading + ";" + d.enemyPositionX + ";" + d.enemyPositionY + ";"
                        + d.enemyPredictedPositionX + ";" + d.enemyPredictedPositionY + ";" + d.enemyDistance + ";"
                        + d.enemyBearing + ";" + d.enemyPredictedBearing + ";" + d.time + ";" + d.destinationCellX + ";"
                        + d.destinationCellY + ";" + d.risk + ";" + "risky;")
                        .getBytes());
            } else {
                fw.write((d.myPositionX + ";" + d.myPositionY + ";" + d.myFuturePositionX + ";" + d.myFuturePositionY
                        + ";"
                        + d.myVelocity + ";" + d.myHeading + ";" + d.enemyPositionX + ";" + d.enemyPositionY + ";"
                        + d.enemyPredictedPositionX + ";" + d.enemyPredictedPositionY + ";" + d.enemyDistance + ";"
                        + d.enemyBearing + ";" + d.enemyPredictedBearing + ";" + d.time + ";" + d.destinationCellX + ";"
                        + d.destinationCellY + ";" + d.risk + ";" + "notRisky")
                        .getBytes());
            }

            // Write the risk grid to the file
            for (int i = 0; i < gridWidth; i++) {
                for (int j = 0; j < gridHeight; j++) {
                    fw.write((";" + riskGrid[i][j]).getBytes());
                }
            }
            fw.write("\n".getBytes());

        } catch (IOException e) {
            // e.printStackTrace();
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
                // e.printStackTrace();
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
            // e.printStackTrace();
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
            // e.printStackTrace();
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
            // e.printStackTrace();
        }
    }

}
