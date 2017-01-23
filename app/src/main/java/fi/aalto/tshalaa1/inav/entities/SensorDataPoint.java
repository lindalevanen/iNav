package fi.aalto.tshalaa1.inav.entities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kreutze1 on 10/21/14.
 */

/**
 * A data type for storing temporal data collected from specific sensors.
 *
 * Example: a data point from an accelerometer, which has 3 floating point values (the 3 axes), as well as a corresponding timestamp when the values where collected.
 */
public class SensorDataPoint extends DataPoint {

    public float x;
    public float y;
    public float z;
    public long timeStamp;

    public SensorDataPoint(float x, float y, float z, long time) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.timeStamp = time;
    }

    /**
     * toString method.
     * @return a concise string representation of the data point.
     */
    @Override
    public String toString() {
        return this.timeStamp + " " + this.x + " " + this.y + " " + this.z;
    }


    /**
     * Method for returning a JSON object representation of the SensorDataPoint
     *
     * The method will return an object such as { "optionalName":[x,y,z] }, where x,y,z are the floats of the data point.
     *
     * @param optionalName: the key, the name of the JSON object to be returned.
     * @returns a JSON object with a JSON Array value
     */
    @Override
    public JSONObject getJSONObject(String optionalName) {
        JSONObject o = new JSONObject();
        JSONArray a = new JSONArray();

        try {
            a.put(x);
            a.put(y);
            a.put(z);
            o.put(optionalName,a);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return o;
    }
}
