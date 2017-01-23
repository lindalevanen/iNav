package fi.aalto.tshalaa1.inav.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by tshalaa1 on 7/26/16.
 */
public class SimplePoint {

    @JsonProperty
    private final double x,y,z;

    //SimplePoint() {}

    @JsonCreator
    public SimplePoint(@JsonProperty("x") double x, @JsonProperty("y") double y, @JsonProperty("z") double z) {
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
}
