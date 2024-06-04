package com.slaughtersquad.sampleRobots;

import robocode.*;

import java.awt.geom.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.*;

import com.slaughtersquad.utils.*;
import robocode.Robot;

import static robocode.util.Utils.normalRelativeAngleDegrees;

/**
 * This Robot uses the model provided to guess whether it will hit or miss an enemy.
 * This is a very basic model, trained specifically on the following enemies: Corners, Crazy, SittingDuck, Walls.
 * It is not expected to do great...
 */
public class IntelligentRobot extends AdvancedRobot {
    private EnemyBot enemy;
    private EasyPredictModelWrapper model;
    private byte scanDirection = 2;

    /**
     * Run the robot
     */
    @Override
    public void run() {
        super.run();

        File dir = getDataDirectory(); // Use Robocode's method to get the data directory
        File[] files = dir.listFiles(); // List all files in the directory

        File modelFile = null;
        assert files != null;
        for (File file : files) {
            if (file.getName().endsWith(".zip")) {
                modelFile = file; // Found a matching file
                break;
            }
        }

        if (modelFile != null) {
            try {
                model = new EasyPredictModelWrapper(MojoModel.load(modelFile.getAbsolutePath()));
                System.out.println("Model loaded successfully");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No matching model file found");
        }

        // Allow the gun and radar to turn independently
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        enemy = new EnemyBot();

        while (true) {
            turnRadarRight(360 * scanDirection);
            Random rand = new Random();
            setAllColors(new Color(rand.nextInt(3), rand.nextInt(3), rand.nextInt(3)));
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        // track if we have no enemy, the one we found is significantly
        // closer, or we scanned the one we've been tracking.
        if (enemy.isReset() || event.getDistance() < enemy.getDistance() ||
                event.getName().equals(enemy.getName())) {

            // track him using the NEW update method
            enemy.update(event, this);
        }

        if (enemy.getDistance() < 150) {
            scanDirection *= -1;
        }

        setTurnRadarRight(360 * scanDirection);

        double firePower = Math.min(500 / enemy.getDistance(), 3);

        double bulletSpeed = 20 - firePower * 3;

        long time = (long) (event.getDistance() / bulletSpeed);

        double futureX = enemy.getFutureX(time);
        double futureY = enemy.getFutureY(time);

        double absDeg = Utils.absoluteBearing(getX(), getY(), futureX, futureY);
        double normalizedAbsDeg = Utils.normalizeBearing(absDeg - getGunHeading());

        setTurnGunRight(normalizedAbsDeg);

        RowData row = new RowData();
        row.put("currentPositionX", getX());
        row.put("currentPositionY", getY());
        row.put("distance", event.getDistance());
        row.put("velocity", event.getVelocity());
        row.put("bearing", event.getBearing());
        row.put("futureBearing", normalizedAbsDeg);
        row.put("enemyPositionX", enemy.getX());
        row.put("enemyPositionY", enemy.getY());
        row.put("predictedEnemyPositionX", futureX);
        row.put("predictedEnemyPositionY", futureY);
        row.put("gunHeat", getGunHeat());

        try {
            if (model != null) {
                MultinomialModelPrediction p = model.predictMultinomial(row);
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
