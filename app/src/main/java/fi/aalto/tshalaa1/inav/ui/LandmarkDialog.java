package fi.aalto.tshalaa1.inav.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Created by kreutze1 on 8/22/14.
 */
public class LandmarkDialog extends DialogFragment {

    private Bitmap img;
    private AlertDialog.Builder builder;

    public void setMessage(AlertDialog.Builder b) {
        this.builder = b;
    }

    public void setImage(Bitmap b) {
        this.img = b;
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction


        // Create the AlertDialog object and return it
        return builder.create();
    }

}
