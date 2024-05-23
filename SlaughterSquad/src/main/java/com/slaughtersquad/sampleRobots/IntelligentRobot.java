package com.slaughtersquad.sampleRobots;

import robocode.*;

import java.awt.geom.*;
import java.awt.*;
import java.io.IOException;
import java.util.Random;

import hex.genmodel.MojoModel;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.prediction.*;

import com.slaughtersquad.utils.*;
import robocode.Robot;

/**
 * This Robot uses the model provided to guess whether it will hit or miss an enemy.
 * This is a very basic model, trained specifically on the following enemies: Corners, Crazy, SittingDuck, Walls.
 * It is not expected to do great...
 */
public class IntelligentRobot extends AdvancedRobot {
    private EnemyBot enemy;

    /**
     * Run the robot
     */
    @Override
    public void run() {
        super.run();

        //load the model
        //EasyPredictModelWrapper model = null;
        //try {
        //    model = new EasyPredictModelWrapper(MojoModel.load("com/slaughtersquad/sampleRobots/IntelligentRobot_model.zip"));
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}

        // Allow the gun and radar to turn independently
        setAdjustRadarForRobotTurn(true);
        setAdjustGunForRobotTurn(true);

        enemy = new EnemyBot();

        while (true) {
            turnRadarRight(360);
            setAhead(100);
            setTurnLeft(100);
            Random rand = new Random();
            setAllColors(new Color(rand.nextInt(3), rand.nextInt(3), rand.nextInt(3)));
            execute();
        }
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        // track if we have no enemy, the one we found is significantly
        // closer, or we scanned the one we've been tracking.
        if (enemy.isReset() || event.getDistance() < enemy.getDistance() - 70 ||
                event.getName().equals(enemy.getName())) {

            // track him using the NEW update method
            enemy.update(event, this);
        }

        double firePower = Math.min(500 / enemy.getDistance(), 3);

        double bulletSpeed = 20 - firePower * 3;

        long time = (long)(event.getDistance() / bulletSpeed);

        double futureX = enemy.getFutureX(time);
        double futureY = enemy.getFutureY(time);
        double absDeg = Utils.absoluteBearing(getX(), getY(), futureX, futureY);

        setTurnGunRight(Utils.normalizeBearing(absDeg - getGunHeading()));

        // Prevent premature firing
        if (getGunHeat() == 0 && Math.abs(getGunTurnRemaining()) < 5) {
            setFire(firePower);
        }
    }

    @Override
    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(enemy.getName())) {
            enemy.reset();
        }
    }
}
