package fi.aalto.tshalaa1.inav.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Bounds {

	@JsonIgnore
	public float pxTopLeftX, pxTopLeftY, pxBottomRightX, pxBottomRightY;
	@JsonIgnore
	public float topLeftX, topLeftY, bottomRightX, bottomRightY;

	public Point3D topLeft, bottomRight;

	@Override
	public String toString(){
		return String.format("Bounds topLeft: %s, bottomRight: %s ", topLeft.toString(), bottomRight.toString());
	}

	
}
