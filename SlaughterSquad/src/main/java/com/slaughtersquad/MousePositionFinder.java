package com.slaughtersquad;

import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;

public class MousePositionFinder {
    public static void main(String[] args) throws InterruptedException {
        while (true) {
            PointerInfo pointerInfo = MouseInfo.getPointerInfo();
            Point point = pointerInfo.getLocation();
            int x = (int) point.getX();
            int y = (int) point.getY();
            System.out.println("Mouse Coordinates: (" + x + ", " + y + ")");
            Thread.sleep(500); // Update every 500 milliseconds
        }
    }
}
