package fi.aalto.tshalaa1.inav.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.List;

/**
 * Created by tshalaa1 on 6/20/16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Route {
    public List<HashMap> waypoints;
    private int id;
    private String minimapId;
    private List<Instruction> instructions;
    private List<Point3D> path;

    public Route() {

    }

    public Route(List<Instruction> ins) {
        instructions = ins;
        id = instructions.hashCode();
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<Instruction> instructions) {
        this.instructions = instructions;
    }

    public int getId() {
        return id;
    }

    public String getMinimapId() {
        return minimapId;
    }

    public void setMinimapId(String minimapId) {
        this.minimapId = minimapId;
    }

    public void setPath(List<Point3D> path) {
        this.path = path;
    }

    public List<Point3D> getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "Path: "+this.getPath()+"\nWaypoints: "+this.waypoints;
    }

}
