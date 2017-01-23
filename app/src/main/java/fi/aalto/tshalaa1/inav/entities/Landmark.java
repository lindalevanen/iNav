package fi.aalto.tshalaa1.inav.entities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * Created by kreutze1 on 8/19/14.
 */

@JsonIgnoreProperties(ignoreUnknown = true)

/**
 * A class for describing points of interest, landmarks, on the indoor map.
 * This might for instance be a toilet, or an exit.
 */
public class Landmark{

    /** the short descriptive title of the landmark */
    private String title;

    /** a longer description of the landmark, which might be used by the UI to explain better what is shown on the map at the moment */
    public String name, description, type;
    @JsonProperty
    public int landmarkID, areaID, buildingID;
    /** the location of the landmark on the map (not on the screen) */
    private Point3D position;
    public byte[] imageData;
    public List tags;
    public boolean isNewLm = false;

    @JsonIgnore
    private Bitmap bitmap;

    public String getName() {
        return this.name;
    }

    public String getNicerTitle() { return Character.toUpperCase(this.name.charAt(0)) + this.name.substring(1); }

    public int getAreaID() { return this.areaID; }

    public int getID() { return this.landmarkID; }

    public void setTitle(String title) {
        this.name = title;
    }

    public void setDescription(String description) { this.description = description; }

    public void setAreaID(int areaid) { this.areaID = areaid; }

    public void setID(int id) { this.landmarkID = id; }

    public String getDescription() {
        return this.description;
    }

    public byte[] getImageData() {
        return this.imageData;
    }

    public void setImageData(byte[] data) {
        this.imageData = data;
    }

    //TODO: change method name
    public Point3D getLocation() { return this.position; }

    public void setPosition(Point3D p) {
        this.position = p;
    }

    @JsonIgnore
    public Bitmap getBitmap() {
        if (bitmap == null && imageData != null) {
            bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        }
        return bitmap;
    }

    public Landmark() {}    //default constructor

    public Landmark(Point3D startpos) {
        System.out.println("startpos: "+startpos);
        this.position = startpos;
    }

    /**
     * This method is made for error fragment
     * @return the landmark copy
     */

    public Landmark copy() {
        Landmark newLandmark = new Landmark(this.getLocation());
        newLandmark.setAreaID(this.areaID);
        newLandmark.setImageData(this.imageData);
        newLandmark.setTitle(this.getName());
        newLandmark.setDescription(this.getDescription());
        // !! the landmark copy also has the same id so it can't be used normally!
        newLandmark.setID(this.getID());
        return newLandmark;
    }

    @Override
    public String toString() {
        return "Landmark"+
                "\n\tName: "+name+
                "\n\tDescription: "+description+
                "\n\tLandmarkID: "+landmarkID+
                "\n\tAreaID: "+areaID+
                "\n\tImageData: "+imageData+
                "\n\tPosition: "+position;
    }
}

