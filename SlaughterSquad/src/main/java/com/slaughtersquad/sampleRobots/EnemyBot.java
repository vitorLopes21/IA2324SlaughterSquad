package com.slaughtersquad.sampleRobots;

import robocode.AdvancedRobot;
import robocode.Robot;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;

public class EnemyBot extends AdvancedRobot {
    private String name = "";
    private double bearing = 0;
    private double distance = 0;
    private double energy = 0;
    private double heading = 0;
    private double velocity = 0;
    private double x = 0;
    private double y = 0;

    public EnemyBot() {
        reset();
    }

    public String getName() {
        return name;
    }

    public double getBearing() {
        return bearing;
    }

    public double getDistance() {
        return distance;
    }

    public double getEnergy() {
        return energy;
    }

    public double getHeading() {
        return heading;
    }

    public double getVelocity() {
        return velocity;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getFutureX(long when) {
        return x + Math.sin(Math.toRadians(getHeading())) * getVelocity() * when;
    }

    public double getFutureY(long when) {
        return y + Math.cos(Math.toRadians(getHeading())) * getVelocity() * when;
    }

    public void update(ScannedRobotEvent event, Robot robot) {
        this.name = event.getName();
        this.bearing = event.getBearing();
        this.distance = event.getDistance();
        this.energy = event.getEnergy();
        this.heading = event.getHeading();
        this.velocity = event.getVelocity();

        double absBearingDeg = (robot.getHeading() + event.getBearing());

        if (absBearingDeg < 0) {
            absBearingDeg += 360;
        }

        this.x = robot.getX() + Math.sin(Math.toRadians(absBearingDeg)) * event.getDistance();

        this.y = robot.getY() + Math.cos(Math.toRadians(absBearingDeg)) * event.getDistance();
    }

    public void reset() {
        this.name = "";
        this.bearing = 0;
        this.distance = 0;
        this.energy = 0;
        this.heading = 0;
        this.velocity = 0;
        this.x = 0;
        this.y = 0;
    }

    public boolean isReset() {
        return this.name.equals("");
    }
}
