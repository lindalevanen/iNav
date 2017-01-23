package fi.aalto.tshalaa1.inav.entities;

public class PathPoint {

	private double x, y, lastX, lastY;

	public PathPoint() {
	}
	
	public PathPoint(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}
	
	public void setX(double x) {
		this.x = x;
	}
	
	public void setY(double y) {
		this.y = y;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}

    public void setLastX(double x) {
        this.lastX = x;
    }

    public void setLastY(double y) {
        this.lastY = y;
    }

	public double getLastX() {return lastX;}

    public double getLastY() {return lastY;}

	@Override
	public String toString() {
		return "x: " + x + "y (aka z): " + y;
	}
	
}
