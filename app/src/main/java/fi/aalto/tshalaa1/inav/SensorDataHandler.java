package fi.aalto.tshalaa1.inav;

import android.app.Activity;
import android.content.Context;
import android.graphics.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Arrays;

import fi.aalto.tshalaa1.inav.entities.SensorRunnable;

/**
 * Created by kreutze1 on 9/23/14.
 */

public class SensorDataHandler {

    SensorManager sensorManager = (SensorManager) Utils.getContext().getSystemService(Context.SENSOR_SERVICE);
    SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    long lastInterval = gyroInterval;
                    gyroInterval = System.currentTimeMillis();
                    if (lastInterval > 0 && gyroInterval - lastInterval > 0)
                        gyroRunnable.processValues(x, y, z, (int) (gyroInterval - lastInterval));
                } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                    long lastInterval = accInterval;
                    accInterval = System.currentTimeMillis();

                    if (lastInterval > 0 && accInterval - lastInterval > 0)
                        accRunnable.processValues(x, y, z, (int) (accInterval - lastInterval));
                } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    //Used to calculate pitch of device
                    System.arraycopy(event.values, 0, mMagnetometerReading,
                            0, mMagnetometerReading.length);

                    long lastInterval = magnInterval;
                    magnInterval = System.currentTimeMillis();

                    if (lastInterval > 0 && magnInterval - lastInterval > 0)
                        magnRunnable.processValues(x, y, z, (int) (magnInterval - lastInterval));

                } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                    //Used to calculate pitch of device

                    System.arraycopy(event.values, 0, mAccelerometerReading,
                            0, mAccelerometerReading.length);
                }

            if(iCam != null && iCam.isResumed()) {
//                System.out.println("accelero reading: " + Arrays.toString(mAccelerometerReading));
                updateOrientationAngles();
//                System.out.println("picTaken " + picTaken);
                if((mOrientationAngles[1] *-100) > 140 && picTaken){
//                    iCam.takePictureButton.callOnClick();
//                    iCam.takePictureButton.performClick();
                    int accX = Math.round(mAccelerometerReading[0]);
                    int accY = Math.round(mAccelerometerReading[1]);
                    int accZ = Math.round(mAccelerometerReading[2]);

                    if(accX == 0 && accY >= 9 && accZ < 2) {
                        preview = true;
//                        iCam.takePicture();
                        picTaken = false;
                        Log.d("SensorDataHandler", "picTaken: " + picTaken);
                    }


                }

//         iCam.takePicture();
            }


        };

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        };
    };

    public boolean picTaken = true;
    public boolean preview = false;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    public final float[] mOrientationAngles = new float[3];

    SensorRunnable gyroRunnable,accRunnable,magnRunnable, acceleroRunnable;

    private Sensor sensorAcc, sensorGyro, sensorMagn, sensorAccelero;

    private long gyroInterval = 0;
    private long accInterval = 0;
    private long magnInterval = 0;
    private long acceleroInterval = 0;


    //these are set in MinimapView.java, in the init method.
    public void setGyroRunnable(SensorRunnable r) {
        gyroRunnable = r;
    }
    public void setAccRunnable(SensorRunnable r) {
        accRunnable = r;
    }
    public void setMagnRunnable(SensorRunnable r) {
        magnRunnable = r;
    }
    public void setAcceleroRunnable(SensorRunnable r) {
        acceleroRunnable = r;
    }

    //the method init needs to be called before start, and both runnables have to be set.
    public void start(boolean acc, boolean gyro, boolean magn, boolean accelero) {
        if (sensorManager != null) {
            if (accRunnable != null && acc)
                startAcc();
            if (gyroRunnable != null && gyro)
               startGyro();
            if (magnRunnable != null && magn)
                startMagn();
            if(acceleroRunnable != null && accelero)
                startAccelero();
        }
    }

    public void stop() {
        if (sensorManager != null) {
            if (sensorAcc != null)
                stopSensor(sensorAcc);
            if (sensorGyro != null)
                stopSensor(sensorGyro);
            if (sensorMagn != null)
                stopSensor(sensorMagn);
            if(sensorAccelero != null)
                stopSensor(sensorAccelero);
        }
    }

    private void startGyro() {
        sensorGyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(sensorListener, sensorGyro, SensorManager.SENSOR_DELAY_UI);
    }
    private void startAcc() {
        sensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(sensorListener, sensorAcc, SensorManager.SENSOR_DELAY_UI);
    }

    private void startMagn() {
        sensorMagn = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(sensorListener, sensorMagn, SensorManager.SENSOR_DELAY_UI);
    }

    private void startAccelero() {
        sensorAccelero = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(sensorListener, sensorAccelero, SensorManager.SENSOR_DELAY_UI);
    }


    private void stopSensor(Sensor sensor) {
        if (sensor != null) {
            sensorManager.unregisterListener(sensorListener, sensor);

            if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
                sensorAcc = null;
            else if (sensor.getType() == Sensor.TYPE_GYROSCOPE)
                sensorGyro = null;
            else if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                sensorMagn = null;
            else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                sensorAccelero = null;
        }
    }

//    private InternalCamera iCam;
    private Camera2Fragment iCam;
    //called inside internalCam
    public void setICam(Camera2Fragment cam) {
        iCam = cam;
    }
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        sensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);

        // "mRotationMatrix" now has up-to-date information.
        //System.out.println("pitch before : " + Arrays.toString(mOrientationAngles));
        sensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

        //trigger to take picture when above 130 degrees
        // "mOrientationAngles" now has up-to-date information.


    }

}
