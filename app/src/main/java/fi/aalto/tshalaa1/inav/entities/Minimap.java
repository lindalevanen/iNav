package fi.aalto.tshalaa1.inav.entities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Minimap {

	private Bounds bounds;
	private byte[] imageData;

    @JsonIgnore
    private Bitmap bitmap;

	private Drawable drawable;

    public Bounds getBounds() {
		return bounds;
	}

	public void setBounds(Bounds bounds) {
		this.bounds = bounds;
	}

	public byte[] getImageData() {
		return imageData;
	}

	public void setImageData(byte[] imageData) {
		this.imageData = imageData;
	}

 	public Drawable getDrawable(){
		if(drawable == null)
			drawable = new BitmapDrawable(null,BitmapFactory.decodeByteArray(getImageData(), 0, getImageData().length));

		return drawable;
	}

	public void setDrawable(Drawable d){
		drawable = d;
	}

    @JsonIgnore
    public Bitmap getBitmap() {
        if (bitmap == null && imageData != null) {
            bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        }
        return bitmap;
    }

	public Minimap copy() {
		Minimap minimap = new Minimap();
		minimap.setBounds(this.bounds);
		minimap.setImageData(this.imageData);
		minimap.bitmap = this.getBitmap();
		return minimap;
	}
}
