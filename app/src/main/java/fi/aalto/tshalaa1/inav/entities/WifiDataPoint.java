package fi.aalto.tshalaa1.inav.entities;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kreutze1 on 11/19/14.
 */
public class WifiDataPoint extends DataPoint {

    public int signalStrength;
    public String macAddress, networkName;
    public long timestamp;

    public WifiDataPoint(long timestamp, String networkName, String macAddress, int signalStrength) {
        this.timestamp = timestamp;
        this.signalStrength = signalStrength;
        this.macAddress = macAddress;
        this.networkName = networkName;
    }

    @Override
    public String toString() {
        return timestamp+" "+networkName+" "+macAddress+" "+signalStrength;
    }


    @Override
    public JSONObject getJSONObject(String optionalName) {
        JSONObject o = new JSONObject();

        try {
            o.put("signal_strength",signalStrength);
            o.put("mac",macAddress);
            o.put("ssid",networkName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return o;
    }
}
