package fi.aalto.tshalaa1.inav.entities;

/**
 * Created by kreutze1 on 9/11/14.
 *
 * A Runnable class for acting on sensor data.
 * For each instance of SensorRunnable, a run() method is defined.
 *
 * This method might use or manipulate sensor data that is output in real time.
 *
 * Example use case: The UI (an Activity, for instance) defines a SensorRunnable whose run() method displays x,y,z components of
 * an accelerometer on the screen.
 * Then, the SensorRunnable is passed to another class which deals with the sensor itsels. Each time the sensor gets new data, it calls upon the
 * processValues() method of the SensorRunnable. --> a nice abstraction to keep the sensor data away from the UI!
 *
 * In this software, the SensorRunnable is utilized for the experimental feature "dynamic positioning"
 */
public abstract class SensorRunnable implements Runnable {

    /** the x,y,z components from the sensor (eg. acceleration data in 3D space) */
    public float x,y,z;

    /** the interval (in milliseconds probably) between this and the last data point collected. */
    public int interval;

    /** Method for updating sensor values, and processing them, each time the sensor fires and gets new values */
    public void processValues(float x, float y, float z, int i) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.interval = i;

        this.run();
    }
}
