package com.slaughtersquad.sampleRobots;

import com.amazonaws.services.dynamodbv2.xspec.S;
import com.slaughtersquad.utils.Utils;
import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.BinomialModelPrediction;
import hex.genmodel.easy.prediction.MultinomialModelPrediction;
import robocode.AdvancedRobot;
import robocode.RobotDeathEvent;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;

import java.awt.*;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * This Robot uses the model provided to guess whether it will hit or miss an
 * enemy.
 * This is a very basic model, trained specifically on the following enemies:
 * Corners, Crazy, SittingDuck, Walls.
 * It is not expected to do great...
 */
public class SlaughterSquad extends AdvancedRobot {
    // Constants
    private static final int CELL_SIZE = 50;
    private static final int HIGH_RISK_SCORE = 50;
    private static final int MIN_RISK_SCORE = 0;

    // Grid dimensions
    private int gridWidth;
    private int gridHeight;
    private int[][] riskGrid;

    private EnemyBot enemy;
    private EasyPredictModelWrapper predictShootModel;
    private EasyPredictModelWrapper predictPositionRiskModel;
    private HashMap<String, Point2D.Double> enemies;
    private byte scanDirection = 2;

    /**
     * Calculate the velocity based on the distance to the destination
     *
     * @param distanceToDestination The distance to the destination
     * @return The velocity to use
     */
    private double calculateVelocityBasedOnDistance(
            double distanceToDestination) {
        if (distanceToDestination > 50) {
            return 45; // Large velocity for long distances
        } else {
            return 23; // Small velocity size for short distances
        }
    }

    /**
     * Turn the robot to a specific angle
     *
     * @param targetAngle The angle to turn to
     */
    private void turnTo(double targetAngle) {
        double currentHeading = getHeading();
        double turnAngle = robocode.util.Utils.normalRelativeAngleDegrees(
                targetAngle - currentHeading);
        setTurnRight(turnAngle);
        execute();
    }

    /**
     * Follow the path generated by the genetic algorithm
     *
     * @param bestPath      The best path generated by the genetic algorithm
     * @param totalDistance The total distance to the destination
     */
    private void followPath(char[] bestPath, double totalDistance) {
        for (char move : bestPath) {
            switch (move) {
                case 'N' -> turnTo(0);
                case 'E' -> turnTo(90);
                case 'S' -> turnTo(180);
                case 'W' -> turnTo(270);
            }

            setAhead(calculateVelocityBasedOnDistance(totalDistance));
        }
    }

    /**
     * Solution Class
     */
    private class Solution implements Comparable<Solution> {
        private Random random;
        private double fitness;
        private char[] path;

        public Solution(double fitness, char[] path) {
            this.random = new Random();
            this.path = path;
            this.fitness = fitness;
        }

        public Solution(Solution solution) {
            this.random = new Random();
            this.path = solution.getPath();
            this.fitness = solution.getFitness();
        }

        /**
         * Getter for the solution's fitness
         */
        public double getFitness() {
            return this.fitness;
        }

        /**
         * Getter for the solution's path
         *
         * @return The path
         */
        public char[] getPath() {
            return Arrays.copyOf(this.path, this.path.length);
        }

        /**
         * Getter for the solution's path as a string
         *
         * @return The path as a string
         */
        public String getPathString() {
            StringBuilder sb = new StringBuilder();

            for (char c : this.path) {
                sb.append(c);
            }

            return sb.toString();
        }

        /**
         * Function to apply mutation to 1 solution
         */
        protected void mutate() {
            int point = random.nextInt(this.path.length); // Random index to mutate
            this.path[point] = Population.MOVEMENTS[random.nextInt(Population.MOVEMENTS.length)]; // Mutate at the
            // random index
        }

        /**
         * Function to apply crossover to 1 solution
         *
         * @param mother The mother solution, used to crossover with the father solution
         *               (this solution)
         * @return An array containing the 2 new solutions created by the crossover
         */
        protected Solution[] crossover(Solution mother) {
            int point = random.nextInt(mother.getPath().length); // Random crossover point

            char[] tempPath = Arrays.copyOf(this.path, this.path.length); // Copy of father's path
            System.arraycopy(mother.getPath(), 0, tempPath, point, mother.getPath().length - point); // Perform
            // crossover

            char[] motherPath = Arrays.copyOf(mother.getPath(), mother.getPath().length); // Copy of mother's path
            System.arraycopy(this.path, 0, motherPath, point, this.path.length - point); // Perform crossover

            Solution[] children = new Solution[]{new Solution(this.fitness, tempPath),
                    new Solution(mother.getFitness(), motherPath)}; // Return the children

            return children;
        }

        /**
         * Function to compare two solutions
         *
         * @param o The solution to compare to
         * @return -1 if this solution has a lower fitness, 1 if this solution has a
         * higher fitness, 0 if they are equal
         */
        @Override
        public int compareTo(Solution o) {
            if (this.fitness < o.fitness) {
                return 1;
            } else if (this.fitness > o.fitness) {
                return -1;
            }
            return 0;
        }

        /**
         * Function to return the solution as a string
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            for (char c : this.path) {
                sb.append(c);
            }

            return sb.toString();
        }
    }

    /**
     * Population Class
     */
    private class Population {
        public static final char[] MOVEMENTS = {'N', 'S', 'E', 'W'};
        public static final int POPULATION_SIZE = 200;
        public static final int POPUPLATION_HEREDITARY = 50;
        public static final int GENERATIONS = 10;
        public static final double MUTATION_RATE = 0.05;
        public static final double CROSSOVER_RATE = 0.2;
        public static int PATH_LENGTH;

        private HashMap<String, Point2D.Double> enemies;
        private Point2D.Double start;
        private Point2D.Double end;
        private double totalDistance;

        private ArrayList<Solution> solutions;

        private Random random;

        public Population(
                Point2D.Double start,
                Point2D.Double end,
                double distance,
                HashMap<String, Point2D.Double> enemies) {
            this.random = new Random();
            this.start = start;
            this.end = end;
            this.totalDistance = distance;
            this.enemies = enemies;

            this.solutions = new ArrayList<>();
            Population.PATH_LENGTH = calculatePathLength(distance);

            for (int i = 0; i < POPULATION_SIZE; i++) {
                char[] path = new char[PATH_LENGTH];

                for (int j = 0; j < PATH_LENGTH; j++) {
                    path[j] = MOVEMENTS[random.nextInt(MOVEMENTS.length)];
                }

                this.solutions.add(
                        new Solution(this.fitnessFunction(path, start, end, enemies), path));
            }
        }

        /**
         * Calculate the path length based on the distance to the destination
         *
         * @param distance The distance to the destination
         * @return The path length
         */
        private int calculatePathLength(double distance) {
            return (int) Math.ceil(distance / 42);
        }

        /**
         * Fitness function to evaluate the path
         *
         * @param path    The path to evaluate
         * @param start   The starting point
         * @param end     The ending point
         * @param enemies The enemies on the battlefield
         * @return The fitness of the path
         */
        private double fitnessFunction(
                char[] path,
                Point2D.Double start,
                Point2D.Double end,
                HashMap<String, Point2D.Double> enemies) {
            double x = start.x;
            double y = start.y;
            double battlefieldWidth = getBattleFieldWidth();
            double battlefieldHeight = getBattleFieldHeight();

            double distance = 0;
            double previousDistanceToEnd = start.distance(end);

            for (char move : path) {
                switch (move) {
                    case 'N' -> y += 1;
                    case 'S' -> y -= 1;
                    case 'E' -> x += 1;
                    case 'W' -> x -= 1;
                }

                // Check if the position hits an enemy
                if (enemies.containsValue(new Point2D.Double(x, y))) {
                    return Integer.MAX_VALUE; // Penalize movements that collide with enemies
                }

                // Penalty for hitting walls
                if (x < 0 || x > battlefieldWidth || y < 0 || y > battlefieldHeight) {
                    return Integer.MAX_VALUE; // Penalize movements that hit the walls
                }

                // Penalty for moving away from the end point
                double currentDistanceToEnd = new Point2D.Double(x, y).distance(end);
                if (currentDistanceToEnd > previousDistanceToEnd) {
                    distance += 10; // Penalize moving away from the end point
                }
                previousDistanceToEnd = currentDistanceToEnd;

                distance += 1; // Basic distance increment
            }

            distance += Math.abs(x - end.x) + Math.abs(y - end.y);
            return distance;
        }

        /**
         * Function to iterate over the population
         *
         * @return The best path found
         */
        public char[] iterate() {
            for (int i = 0; i < GENERATIONS; i++) {
                List<Solution> currentSolutions = this.solutions;

                currentSolutions.sort(Collections.reverseOrder());

                // Get POPUPLATION_HEREDITARY number of best solutions
                ArrayList<Solution> nextGen = new ArrayList<>();

                for (int j = 0; j < POPUPLATION_HEREDITARY; j++) {
                    nextGen.add(currentSolutions.get(j));
                }

                // From the best POPUPLATION_HEREDITARY solutions, mutate POPUPLATION_HEREDITARY
                // * MUTATION_RATE solutions
                // to them before adding everything to the next generation
                for (int j = 0; j < POPUPLATION_HEREDITARY * MUTATION_RATE; j++) {
                    Solution solution = new Solution(nextGen.get(j));

                    solution.mutate();
                    nextGen.add(solution);
                }

                // From the best POPUPLATION_HEREDITARY solutions, crossover
                // POPUPLATION_HEREDITARY * CROSSOVER_RATE solutions
                // to them before adding everything to the next generation
                for (int j = 0; j < POPUPLATION_HEREDITARY * CROSSOVER_RATE; j++) {
                    Solution father = new Solution(nextGen.get(j));
                    Solution mother = new Solution(nextGen.get(j + 1));

                    Solution[] children = father.crossover(mother);

                    nextGen.add(children[0]);
                    nextGen.add(children[1]);
                }

                this.solutions = nextGen;

                this.solutions.sort(Collections.reverseOrder());
            }

            return this.solutions.get(0).getPath();
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
        for (int x = 0; x < this.gridWidth; x++) {
            for (int y = 0; y < this.gridHeight; y++) {
                int risk = this.riskGrid[x][y];

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

        // Return the best cell found
        return bestCell != null ? new Point2D.Double(newDestX, newDestY) : new Point2D.Double(0, 0);
    }

    private void updateRiskGrid() {
        // Reset the risk grid to initial state
        assignInitialRiskScores();

        // Update risks based on known enemy positions
        for (Point2D.Double enemyPos : enemies.values()) {
            int gridX = (int) (enemyPos.x / CELL_SIZE);
            int gridY = (int) (enemyPos.y / CELL_SIZE);

            if (gridX >= 0 && gridX < this.gridWidth && gridY >= 0 && gridY < this.gridHeight) {
                // Assign high risk to the enemy's current cell
                this.riskGrid[gridX][gridY] += 10;

                // Assign high risk to surrounding cells in a circular pattern
                int radius = 8;
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {

                        int adjX = gridX + dx;
                        int adjY = gridY + dy;

                        if (adjX >= 0 && adjX < this.gridWidth && adjY >= 0 && adjY < this.gridHeight) {

                            double distance = Math.sqrt(dx * dx + dy * dy);

                            if (distance <= radius) {

                                // ((int) 10 * (1 - distance / radius)));
                                this.riskGrid[adjX][adjY] += (int) (6 * (1 - distance / radius));
                            }
                        }
                    }
                }
            }
        }

        // Update risks around my current position
        int myGridX = (int) (getX() / CELL_SIZE);
        int myGridY = (int) (getY() / CELL_SIZE);

        if (myGridX >= 0 && myGridX < this.gridWidth && myGridY >= 0 && myGridY < this.gridHeight) {
            // Assign high risk to my current cell
            this.riskGrid[myGridX][myGridY] += 5;

            // Assign medium risk to surrounding cells in a circular pattern
            int myRadius = 8;
            for (int dx = -myRadius; dx <= myRadius; dx++) {
                for (int dy = -myRadius; dy <= myRadius; dy++) {
                    int adjX = myGridX + dx;
                    int adjY = myGridY + dy;
                    if (adjX >= 0 && adjX < this.gridWidth && adjY >= 0 && adjY < this.gridHeight) {

                        double distance = Math.sqrt(dx * dx + dy * dy);

                        if (distance <= myRadius) {
                            this.riskGrid[adjX][adjY] += (int) (3 * (1 - distance / myRadius));
                        }
                    }
                }
            }
        }
    }

    /**
     * Method that assigns initial risk scores to the cells in the grid
     */
    private void assignInitialRiskScores() {
        // Assign medium risk to all cells

        for (int i = 0; i < this.gridWidth; i++) {
            for (int j = 0; j < this.gridHeight; j++) {
                this.riskGrid[i][j] = HIGH_RISK_SCORE;
            }
        }

        // Assign minimum risk to corners, triangles occupying 1/12 of the map each
        int triangleHeight = this.gridHeight * 2 / 3;
        int triangleWidth = this.gridWidth * 2 / 3;

        for (int i = 0; i < triangleWidth; i++) {
            for (int j = 0; j < triangleHeight - i; j++) {
                // Bottom-left corner
                this.riskGrid[i][j] = MIN_RISK_SCORE;
                // Bottom-right corner
                this.riskGrid[this.gridWidth - 1 - i][j] = MIN_RISK_SCORE;
                // Top-left corner
                this.riskGrid[i][this.gridHeight - 1 - j] = MIN_RISK_SCORE;
                // Top-right corner
                this.riskGrid[this.gridWidth - 1 - i][this.gridHeight - 1 - j] = MIN_RISK_SCORE;
            }
        }

        // Assign high risk to the cells adjacent to walls
        for (int i = 0; i < gridWidth; i++) {
            this.riskGrid[i][0] = MIN_RISK_SCORE;
            this.riskGrid[i][this.gridHeight - 1] = MIN_RISK_SCORE;

            this.riskGrid[i][1] = MIN_RISK_SCORE;
            this.riskGrid[i][this.gridHeight - 2] = MIN_RISK_SCORE;
        }
        for (int j = 0; j < gridHeight; j++) {
            this.riskGrid[0][j] = MIN_RISK_SCORE;
            this.riskGrid[this.gridWidth - 1][j] = MIN_RISK_SCORE;

            this.riskGrid[1][j] = MIN_RISK_SCORE;
            this.riskGrid[this.gridWidth - 2][j] = MIN_RISK_SCORE;
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

            if (futureGridX >= 0 && futureGridX < this.gridWidth && futureGridY >= 0 && futureGridY < this.gridHeight) {
                this.riskGrid[futureGridX][futureGridY] += 0.1;
            }
        }
    }

    private void initialize() {
        // Initialize grid based on dynamic arena size
        this.gridWidth = (int) Math.ceil(getBattleFieldWidth() / CELL_SIZE);
        this.gridHeight = (int) Math.ceil(getBattleFieldHeight() / CELL_SIZE);
        this.riskGrid = new int[this.gridWidth][this.gridHeight];

        this.enemies = new HashMap<>();

        File dir = getDataDirectory(); // Use Robocode's method to get the data directory
        File[] files = dir.listFiles(); // List all files in the directory

        assert files != null;
        try {
            this.predictPositionRiskModel = new EasyPredictModelWrapper(
                    MojoModel.load(files[0].getAbsolutePath()));
            this.predictShootModel = new EasyPredictModelWrapper(MojoModel.load(files[1].getAbsolutePath()));
            System.out.println("Model loaded successfully");
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.enemy = new EnemyBot();

        assignInitialRiskScores();
    }

    /**
     * Run the robot
     */
    @Override
    public void run() {
        super.run();

        // Initialize all the necessary components for the robot
        initialize();

        // Allow the gun and radar to turn independently
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        while (true) {
            turnRadarRight(360 * scanDirection);
            setAllColors(new Color(3, 3, 3));
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        // Update the enemy's position
        enemies.put(event.getName(), Utils.getEnemyCoordinates(this, event));

        // track if we have no enemy, the one we found is significantly
        // closer, or we scanned the one we've been tracking.
        if (enemy.isReset() || event.getDistance() < enemy.getDistance() ||
                event.getName().equals(enemy.getName())) {

            // track him using the NEW update method
            enemy.update(event, this);
        }

        if (enemy.getDistance() < 300) {
            scanDirection *= -1;
        }

        setTurnRadarRight(360 * scanDirection);

        this.updateRiskGrid();

        double firePower = Math.min(500 / enemy.getDistance(), 3);

        double bulletSpeed = 20 - firePower * 3;

        long time = (long) (event.getDistance() / bulletSpeed);

        // Calculate my future position
        double myFutureX = getX() + Math.sin(getHeadingRadians()) * getVelocity() * time;
        double myFutureY = getY() + Math.cos(getHeadingRadians()) * getVelocity() * time;

        // Ensure the future position is within the battlefield boundaries
        myFutureX = Math.max(Math.min(myFutureX, getBattleFieldWidth() - CELL_SIZE), CELL_SIZE);
        myFutureY = Math.max(Math.min(myFutureY, getBattleFieldHeight() - CELL_SIZE), CELL_SIZE);

        System.out.println("My future position: " + myFutureX + ", " + myFutureY);

        // Calculate the enemies' future position
        double enemyFutureX = enemy.getFutureX(time);
        double enemyFutureY = enemy.getFutureY(time);

        // Calculate the absolute bearing to the enemy's future position
        double absDeg = Utils.absoluteBearing(getX(), getY(), enemyFutureX, enemyFutureY);
        double normalizedAbsDeg = Utils.normalizeBearing(absDeg - getGunHeading());

        int riskScore = riskGrid[(int) (myFutureX / CELL_SIZE)][(int) (myFutureY / CELL_SIZE)];

        Point2D.Double enemyPredictedPosition = predictEnemyPosition(event, firePower);

        double enemyPredictedBearing = predictEnemyBearing(event, firePower, enemies.get(event.getName()));

        Point2D.Double destinationCell = getDestinationCell(event);

        // Assign risk based on enemy bearing, based on his future position and bearing,
        // if it intercepts with my future position then it's risky
        assignRiskBasedOnFutureEnemyShot(event, enemyFutureX, enemyFutureY, enemyPredictedPosition,
                enemyPredictedBearing);

        RowData rowPredictRiskyCoordinates = new RowData();
        rowPredictRiskyCoordinates.put("col1", myFutureX);
        rowPredictRiskyCoordinates.put("col2", myFutureY);
        rowPredictRiskyCoordinates.put("col3", this.getVelocity());
        rowPredictRiskyCoordinates.put("col4", this.getHeading());
        rowPredictRiskyCoordinates.put("col5", enemyFutureX);
        rowPredictRiskyCoordinates.put("col6", enemyFutureY);
        rowPredictRiskyCoordinates.put("col7", event.getDistance());
        rowPredictRiskyCoordinates.put("col8", enemyPredictedBearing);
        rowPredictRiskyCoordinates.put("col9", destinationCell.x);
        rowPredictRiskyCoordinates.put("col10", destinationCell.y);
        rowPredictRiskyCoordinates.put("col11", riskScore);

        boolean isRisky = true;

        // While the algorithm hasn't predicted a safe position, keep predicting
        while (isRisky) {
            try {
                if (predictPositionRiskModel != null) {
                    BinomialModelPrediction p = predictPositionRiskModel.predictBinomial(rowPredictRiskyCoordinates);
                    System.out.println("Is it risky? ->" + p.label);

                    if (p.label.equals("notRisky")) {
                        isRisky = false;
                    } else {
                        // If the position is still risky, update the risk grid and try again
                        this.updateRiskGrid();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Point2D.Double start = new Point2D.Double(getX(), getY());
        Point2D.Double end = getDestinationCell(event);
        Population population = new Population(start, end, start.distance(end), enemies);

        char[] bestPath = population.iterate();

        followPath(bestPath, start.distance(end));

        RowData row = new RowData();
        row.put("currentPositionX", getX());
        row.put("currentPositionY", getY());
        row.put("distance", event.getDistance());
        row.put("velocity", event.getVelocity());
        row.put("bearing", event.getBearing());
        row.put("futureBearing", normalizedAbsDeg);
        row.put("predictedEnemyPositionX", enemyFutureX);
        row.put("predictedEnemyPositionY", enemyFutureY);

        setTurnGunRight(normalizedAbsDeg);

        try {
            if (predictShootModel != null) {
                BinomialModelPrediction p = predictShootModel.predictBinomial(row);
                System.out.println("Will I hit? ->" + p.label);

                if (p.label.equals("hit")) {
                    setFire(firePower);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(enemy.getName())) {
            enemy.reset();
        }
    }

    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        // Reset the enemy
        enemy.reset();
    }
}
