package fi.aalto.tshalaa1.inav.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.springframework.util.MultiValueMap;

import java.util.HashMap;

/**
 * Created by noreikm1 on 4/15/14.
 */

public class Point3D {

    private double x, y, z;

    private double rotX, rotY, rotZ;
    public int areaID;

    public long timestamp;

    public Point3D position;

    public HashMap<String, Double> direction;

    public Point3D() {

    }

    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getRotX() {
        return rotX;
    }

    public void setRotX(double rotX) {
        this.rotX = rotX;
    }

    public double getRotY() {
        return rotY;
    }

    public void setRotY(double rotY) {
        this.rotY = rotY;
    }

    public double getRotZ() {
        return rotZ;
    }

    public void setRotZ(double rotZ) {
        this.rotZ = rotZ;
    }

    public double distance (Point3D other) {
        Point3D result = new Point3D();
        result.setZ(Math.abs(getZ() - other.getZ()));
        result.setX(Math.abs (getX()- other.getX()));
        double distance = Math.sqrt((result.getZ())*(result.getZ()) +(result.getX())*(result.getX()));
        //System.out.println(result.distance);
        return distance;
    }

    public double userPosDistance (Point3D other) {
        Point3D result = new Point3D();
        result.setY(Math.abs(getY() - other.getY()));
        result.setX(Math.abs (getX()- other.getX()));
        double distance = Math.sqrt((result.getY())*(result.getY()) +(result.getX())*(result.getX()));
        //System.out.println(result.distance);
        return distance;
    }

    public SimplePoint parameters() {
//        HashMap<String, Double> position = new HashMap<>();
//        position.put("z", getZ());
//        position.put("y", getY());
//        position.put("x", getX());
//        return position;
        return new SimplePoint(x, y, z);
    }

    public Point3D copy(){
        return new Point3D(this.x, this.y, this.z);
    }

    @Override
    public String toString() {
        return "x: " + x + " y: " + y + " z: " + z; //+ " (rotation: "+rotX+", "+rotY+", "+rotZ+")";
    }
}
