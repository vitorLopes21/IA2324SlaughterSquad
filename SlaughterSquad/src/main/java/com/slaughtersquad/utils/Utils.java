package com.slaughtersquad.utils;

import robocode.AdvancedRobot;
import robocode.Robot;
import static robocode.util.Utils.normalRelativeAngle;

import java.awt.geom.*;
import java.util.GregorianCalendar;


/**
 * Classe que implementa algumas tarefas frequentes em Robocode
 *
 * @author Davide Carneiro
 * @version 1.0
 */
public final class Utils
{

    private Utils(){}

    /**
     * Devolve a distância às coordenadas dadas
     *
     * @param robot o meu robot
     * @param x coordenada x do alvo
     * @param y coordenada y do alvo
     * @return distância entre o robot e as coordenadas
     * */
    public static double getDistance(Robot robot, double x, double y)
    {
        x -= robot.getX();
        y -= robot.getY();

        return Math.hypot(x, y);
    }

    /**
     * Devolve as coordenadas de um alvo
     *
     * @param robot o meu robot
     * @param bearing ângulo para o alvo, em graus
     * @param distance distância ao alvo
     * @return coordenadas do alvo
     * */
    public static Point2D.Double getEnemyCoordinates(Robot robot, double bearing, double distance){
        double angle = Math.toRadians((robot.getHeading() + bearing) % 360);

        return new Point2D.Double((robot.getX() + Math.sin(angle) * distance), (robot.getY() + Math.cos(angle) * distance));
    }

    /**
     * Foge em linha reta de um determinado alvo
     *
     * @param robot o meu robot
     * @param x coordenada x do alvo
     * @param y coordenada y do alvo
     * @param distance distância que pretende fugir
     * */
    public static void runLineFrom(Robot robot, double x, double y, double distance)
    {
        x -= robot.getX();
        y -= robot.getY();

        double angleToTarget = Math.atan2(x, y);
        double targetAngle = normalRelativeAngle(angleToTarget - Math.toRadians(robot.getHeading()));
        double turnAngle = Math.atan(Math.tan(targetAngle));
        robot.turnRight(Math.toDegrees(turnAngle));

        if (targetAngle == turnAngle)
            robot.back(distance);
        else
            robot.ahead(distance);
    }

    /**
     * Dirige o robot (RobotSimples) para determinadas coordenadas
     *
     * @param robot o meu robot
     * @param x coordenada x do alvo
     * @param y coordenada y do alvo
     * */
    public static void robotGoTo(Robot robot, double x, double y)
    {
        x -= robot.getX();
        y -= robot.getY();

        double angleToTarget = Math.atan2(x, y);
        double targetAngle = normalRelativeAngle(angleToTarget - Math.toRadians(robot.getHeading()));
        double distance = Math.hypot(x, y);
        double turnAngle = Math.atan(Math.tan(targetAngle));
        robot.turnRight(Math.toDegrees(turnAngle));
        if (targetAngle == turnAngle)
            robot.ahead(distance);
        else
            robot.back(distance);
    }

    /**
     * Dirige o robot (AdvancedRobot) para determinadas coordenadas
     *
     * @param robot o meu robot
     * @param x coordenada x do alvo
     * @param y coordenada y do alvo
     * */
    public static void advancedRobotGoTo(AdvancedRobot robot, double x, double y)
    {
        x -= robot.getX();
        y -= robot.getY();

        double angleToTarget = Math.atan2(x, y);
        double targetAngle = normalRelativeAngle(angleToTarget - Math.toRadians(robot.getHeading()));
        double distance = Math.hypot(x, y);
        double turnAngle = Math.atan(Math.tan(targetAngle));
        robot.setTurnRight(Math.toDegrees(turnAngle));
        if (targetAngle == turnAngle)
            robot.setAhead(distance);
        else
            robot.setBack(distance);
        robot.execute();
    }

    /**
     * Devolve a data atual no formato yyyy-MM-dd HH:mm:ss
     *
     * @return a data atual, no formato yyyy-MM-dd HH:mm:ss
     */
    public static String dataAtualFormatada()
    {
        GregorianCalendar gc = new GregorianCalendar();
        String mes = gc.get(GregorianCalendar.MONTH) < 10 ? "0"+gc.get(GregorianCalendar.MONTH) : ""+gc.get(GregorianCalendar.MONTH);
        String dia = gc.get(GregorianCalendar.DAY_OF_MONTH) < 10 ? "0"+gc.get(GregorianCalendar.DAY_OF_MONTH) : ""+gc.get(GregorianCalendar.DAY_OF_MONTH);
        String hora = gc.get(GregorianCalendar.HOUR_OF_DAY) < 10 ? "0"+gc.get(GregorianCalendar.HOUR_OF_DAY) : ""+gc.get(GregorianCalendar.HOUR_OF_DAY);
        String minuto = gc.get(GregorianCalendar.MINUTE) < 10 ? "0"+gc.get(GregorianCalendar.MINUTE) : ""+gc.get(GregorianCalendar.MINUTE);
        String segundo = gc.get(GregorianCalendar.SECOND) < 10 ? "0"+gc.get(GregorianCalendar.SECOND) : ""+gc.get(GregorianCalendar.SECOND);

        return gc.get(GregorianCalendar.YEAR)+"-"+mes+"-"+dia+" "+hora+":"+minuto+":"+segundo;
    }

    /**
     * Normalize the bearing to be between -180 and 180
     * @param angle the angle to normalize
     * @return the normalized angle
     */
    public static double normalizeBearing(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    /**
     * Calculate the absolute bearing between two points
     * @param x1 the x coordinate of the first point
     * @param y1 the y coordinate of the first point
     * @param x2 the x coordinate of the second point
     * @param y2 the y coordinate of the second point
     * @return the absolute bearing between the two points
     */
    public static double absoluteBearing(double x1, double y1, double x2, double y2) {
        double xo = x2-x1;
        double yo = y2-y1;
        double hyp = Point2D.distance(x1, y1, x2, y2);
        double arcSin = Math.toDegrees(Math.asin(xo / hyp));
        double bearing = 0;

        if (xo > 0 && yo > 0) { // both pos: lower-Left
            bearing = arcSin;
        } else if (xo < 0 && yo > 0) { // x neg, y pos: lower-right
            bearing = 360 + arcSin; // arcsin is negative here, actuall 360 - ang
        } else if (xo > 0 && yo < 0) { // x pos, y neg: upper-left
            bearing = 180 - arcSin;
        } else if (xo < 0 && yo < 0) { // both neg: upper-right
            bearing = 180 - arcSin; // arcsin is negative here, actually 180 + ang
        }

        return bearing;
    }
}
