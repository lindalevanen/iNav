package fi.aalto.tshalaa1.inav.entities;

import java.util.List;

public class Instruction {

	private String text;
	private String imageId;
	private List<PathPoint> path;

    public Instruction() {

    }

	public Instruction(String text, String imageId, List<PathPoint> path) {
		this.path = path;
		this.text = text;
		this.imageId = imageId;
	}

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setPath(List<PathPoint> path) {
        this.path = path;
    }

    public String getText() {
		return text;
	}

	public String getImageId() {
		return imageId;
	}
	
	public List<PathPoint> getPath() {
		return path;
	}

    public PathPoint getTurningPoint() {
        if (path.size() == 3) return path.get(1);
        return path.get(0);
    }
	
}
