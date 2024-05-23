package com.slaughtersquad.sampleRobots;

import robocode.*;
import com.slaughtersquad.utils.*;

import java.awt.geom.*;
import java.awt.*;
import java.util.Random;

public class LoggerRobot extends AdvancedRobot {
    @Override
    public void run() {
        super.run();

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
        } else
            System.out.println("Cannot fire right now...");

    }

    @Override
    public void onBulletHit(BulletHitEvent event) {
        super.onBulletHit(event);
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        super.onBulletMissed(event);
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
        super.onBulletHitBullet(event);
    }

    @Override
    public void onDeath(DeathEvent event) {
        super.onDeath(event);
    }

    @Override
    public void onRoundEnded(RoundEndedEvent event) {
        super.onRoundEnded(event);
    }
}
