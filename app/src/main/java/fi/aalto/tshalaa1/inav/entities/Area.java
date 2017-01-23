package fi.aalto.tshalaa1.inav.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;

/**
 * Created by tshalaa1 on 7/22/16.
 */

public class Area {

    public String areaID, description, name, type, alias;
    public Point3D location;
    public String floorOrder;
    public List tags;
   // @JsonProperty("bounds")
    public Bounds bounds;
    public Point3D center;

}
