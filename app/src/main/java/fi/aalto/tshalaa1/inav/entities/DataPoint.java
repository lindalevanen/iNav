package fi.aalto.tshalaa1.inav.entities;

import org.json.JSONObject;

/**
 * Created by kreutze1 on 11/19/14.
 */
public abstract class DataPoint {
    public abstract String toString();

    public abstract JSONObject getJSONObject(String optionalName);
}
