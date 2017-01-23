package fi.aalto.tshalaa1.inav;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import fi.aalto.tshalaa1.inav.entities.Point3D;
import fi.aalto.tshalaa1.inav.entities.SensorRunnable;

/**
 * Created by tshalaa1 on 7/20/16.
 */
public class Particle  {

    public Particle(){

    }
    public Point3D location = new Point3D();
    public double particleStride = 0;
    public int passedWaypointIdx = 0;
    public double nextWaypointDist = 0;

    public void setPassedWaypointIdx(int idx){

//        if(idx > passedWaypointIdx)
//            System.out.println("passed index is larger");
//        else if(idx == passedWaypointIdx)
//            System.out.println("passed index is same");
//        else
//            System.out.println("tried to set smaller passedwaypoint idx");
//
//
//        System.out.println("passed vs set idx" + passedWaypointIdx + " , " + idx);
        passedWaypointIdx = idx;

    }


}
