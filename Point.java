/**
 * Created by Bruce on 7/16/16.
 */
public class Point {
    private double lonX;
    private double latY;

    public Point() {
        lonX = 0;
        latY = 0;
    }

    public Point(double x, double y) {
        lonX = x;
        latY = y;
    }

    public double getLonX() {
        return lonX;
    }

    public double getLatY() {
        return latY;
    }

    public void setPoint(double x, double y) {
        lonX = x;
        latY = y;
    }
}
