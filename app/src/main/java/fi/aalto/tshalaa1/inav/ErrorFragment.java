package fi.aalto.tshalaa1.inav;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.io.ByteArrayOutputStream;

import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.Minimap;
import fi.aalto.tshalaa1.inav.entities.Point3D;
import fi.aalto.tshalaa1.inav.server.SendErrorReport;
import fi.aalto.tshalaa1.inav.server.ServerInterface;

/**
 * ErrorFragment allows the user to send an error report of a landmark to server.
 */
public class ErrorFragment extends android.support.v4.app.Fragment {

    // a lot of static variables cause they've got to be accessed from inner functions (and can't be final)
    onErrorPressed mCallback;
    View view;
    private static Landmark mLandmark;
    private static Minimap mMinimap;
    private static Bitmap mMarkerBitmap;
    private static Bitmap mPhotoBitmap;
    FrameLayout newMarkerWindow;
    private Landmark editedLandmark;
    private EditText newName;
    private EditText notes;
    private CheckBox nameCB;
    private CheckBox markerCB;
    private CheckBox photoCB;

    private byte[] newImageData;
    private Point3D newLocation;
    private ImageView landmarkPhoto;
    private ServerInterface server;
    private static RelativeLayout progressView;

    private static Point3D originalLocation;

    private boolean hasNewLocation = false;
    private boolean hasNewPhoto = false;

    public boolean okayToClose = false;

    // Container Activity must implement this interface (MainActivity)
    public interface onErrorPressed {
        public void onDiscardPressed(String fragment);
        public void openMarkerEdit(Minimap minimap, Landmark landmark, String where);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (onErrorPressed) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement onErrorPressed");
        }
    }

    public static ErrorFragment newInstance(Landmark landmark, Minimap minimapClone, Bitmap markerBmp) {

        mLandmark = landmark;
        mMinimap = minimapClone;
        mMarkerBitmap = markerBmp;
        originalLocation = landmark.getLocation();
        if(landmark.getBitmap() != null) mPhotoBitmap = landmark.getBitmap();

        final ErrorFragment fragment = new ErrorFragment();
        return fragment;
    }

    public ErrorFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_error, container, false);

        ImageButton discardB = (ImageButton) view.findViewById(R.id.back_button);
        ImageButton sendB = (ImageButton) view.findViewById(R.id.send_button);
        newMarkerWindow = (FrameLayout) view.findViewById(R.id.new_minimap_fragment);
        landmarkPhoto = (ImageView) view.findViewById(R.id.landmark_photo);
        notes = (EditText) view.findViewById(R.id.note);
        server = new ServerInterface();

        setEditedLandmark(mLandmark);

        discardB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onDiscardPressed("error");
            }
        });

        // Determines what happenes when the send button is pressed
        sendB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = newName.getText().toString().trim();
                String note = notes.getText().toString().trim();

                if (nameCB.isChecked() && !name.trim().isEmpty()) editedLandmark.setTitle(name);
                else editedLandmark.setTitle(null);

                if (!note.equals("")) editedLandmark.setDescription(note);

                if (photoCB.isChecked() && newImageData != null) editedLandmark.setImageData(newImageData);
                else editedLandmark.setImageData(null);

                if (markerCB.isChecked() && newLocation != null) { hasNewLocation = true; }

                if (newInfoInReport()) {
                    makeErrorReport();
                } else {
                    Toast.makeText(getContext(), "Set the correct information first, please.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        initNameView();
        initMarkerView();
        initPhotoView();

        return view;
    }

    /**
     * Check if user has set anything as incorrect.
     * @return true if has, false if hasn't
     */

    public boolean newInfoInReport() {
        return (editedLandmark.getName() != null ||
                editedLandmark.getDescription() != null ||
                editedLandmark.getBitmap() != null ||
                hasNewLocation);
    }

    /**
     * This is a bit different from newInfoReport since this accepts also the changes to fields
     * that are not checked.
     * @return true if the user has made any changes, false if hasn't
     */

    public boolean madeChanges() {
        return (!newName.getText().toString().trim().isEmpty() ||
                !notes.getText().toString().trim().isEmpty() ||
                hasNewPhoto || hasNewLocation);
    }

    public void setEditedLandmark(Landmark newLandmark) { editedLandmark = newLandmark.copy(); }

    /**
     * Passes the landmark location data to this fragment.
     * @param lm the new landmark with the new user defined location
     */

    public void passMarkerData(Landmark lm) {
        if(originalLocation != editedLandmark.getLocation()) {
            setMarkerLOafterNewMarker();
            newLocation = lm.getLocation();
        }
    }

    /**
     * Passes a new bitmap for the location thumbnail view.
     * @param newBtmp the new bitmap
     */

    public void passNewBitmap(Bitmap newBtmp) {
        mMarkerBitmap = newBtmp;
        ImageView markerPhoto = (ImageView) view.findViewById(R.id.marker_map);
        markerPhoto.setImageBitmap(newBtmp);
    }

    /**
     * Inits name edit view.
     */

    public void initNameView() {
        nameCB = (CheckBox) view.findViewById(R.id.checkbox1);
        final TextView currentName = (TextView) view.findViewById(R.id.current_name);
        newName = (EditText) view.findViewById(R.id.new_name);
        RelativeLayout nameView = (RelativeLayout) view.findViewById(R.id.name_view);

        currentName.setText(mLandmark.getNicerTitle());
        nameCB.setOnCheckedChangeListener(null);

        nameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ( !nameCB.isChecked() ) {
                    newName.setVisibility(View.VISIBLE);
                    currentName.setPaintFlags(currentName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    currentName.setTextColor(Color.parseColor("#bd0000"));
                    nameCB.setChecked(true);
                } else {
                    newName.setVisibility(View.GONE);
                    currentName.setPaintFlags(currentName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                    currentName.setTextColor(Color.parseColor("#000000"));
                    nameCB.setChecked(false);
                }
            }
        });
    }

    /**
     * Inits marker edit view.
     */

    public void initMarkerView() {
        markerCB = (CheckBox) view.findViewById(R.id.checkbox2);
        final ImageView darkBG = (ImageView) view.findViewById(R.id.dark_bg);
        final TextView updateLM = (TextView) view.findViewById(R.id.update_location_text);
        RelativeLayout markerLO = (RelativeLayout) view.findViewById(R.id.marker_location_layout);
        ImageView markerPhoto = (ImageView) view.findViewById(R.id.marker_map);

        markerPhoto.setImageBitmap(mMarkerBitmap);

        markerLO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!markerCB.isChecked() && updateLM.getVisibility() == View.GONE) setMarkerLOCheck(true);
                else setMarkerLOCheck(false);
            }
        });

        darkBG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(markerCB.isChecked()) {
                    if(updateLM.getVisibility() == View.GONE) setMarkerLOCheck(true);
                    else openMarkerEdit();
                }
            }
        });
    }

    /**
     * Updates the marker edit layout when it is pressed (checks it as incorrect).
     * Would be cool if this was made using styles or something, like the view had three styles,
     * checked, unchecked and edited. (the edited one is determined in setMarkerLOafterNewMarker())
     * @param checked true -> checks it, false -> unchecks
     */

    public void setMarkerLOCheck(boolean checked) {
        final ImageView darkBG = (ImageView) view.findViewById(R.id.dark_bg);
        final TextView updateLM = (TextView) view.findViewById(R.id.update_location_text);
        final TextView markerText = (TextView) view.findViewById(R.id.marker_location_text);
        final ImageView landmarkPhoto = (ImageView) view.findViewById(R.id.landmark_photo);

        //landmarkPhoto.setImageBitmap(mPhotoBitmap);       //TODO: uncomment when server sends landmark's image

        if(checked) {
            markerText.setText(R.string.location_incorrect);
            markerText.setTextColor(Color.parseColor("#bd0000"));
            darkBG.setVisibility(View.VISIBLE);
            updateLM.setVisibility(View.VISIBLE);
            markerCB.setChecked(true);

            //The background's gotta be changed according to the android version...
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                darkBG.setBackground(getResources().getDrawable(R.drawable.dark_with_red_borders, getContext().getTheme()));
            } else {
                darkBG.setBackground(getResources().getDrawable(R.drawable.dark_with_red_borders));
            }
        } else {
            markerText.setText(R.string.marker_location);
            markerText.setTextColor(Color.parseColor("#9e9e9e"));
            darkBG.setVisibility(View.GONE);
            updateLM.setVisibility(View.GONE);
            markerCB.setChecked(false);
        }
    }

    /**
     * Updates the landmark location view after it has been edited.
     */

    public void setMarkerLOafterNewMarker() {
        ImageView darkBG = (ImageView) view.findViewById(R.id.dark_bg);
        TextView updateLocationText = (TextView) view.findViewById(R.id.update_location_text);

        updateLocationText.setVisibility(View.GONE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            darkBG.setBackground(getResources().getDrawable(R.drawable.red_borders, getContext().getTheme()));
        } else {
            darkBG.setBackground(getResources().getDrawable(R.drawable.red_borders));
        }
    }

    /**
     * Inits the landmark's photo view.
     */

    public void initPhotoView() {
        photoCB = (CheckBox) view.findViewById(R.id.checkbox3);
        final ImageView darkBG = (ImageView) view.findViewById(R.id.dark_bg2);
        final TextView takePhotoText = (TextView) view.findViewById(R.id.update_photo_text);
        RelativeLayout photoLO = (RelativeLayout) view.findViewById(R.id.photoLO);
        ImageView photo = (ImageView) view.findViewById(R.id.landmark_photo);

        if(mPhotoBitmap != null) photo.setImageBitmap(mPhotoBitmap);

        photoLO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!photoCB.isChecked() && takePhotoText.getVisibility() == View.GONE) {
                    setPhotoLOCheck(true);
                } else {
                    setPhotoLOCheck(false);
                }
            }
        });

        darkBG.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(photoCB.isChecked()) {
                    if(takePhotoText.getVisibility() == View.GONE) setPhotoLOCheck(true);
                    else openPhotoEdit();
                }
            }
        });
    }

    /**
     * Updates the photo layout when it is pressed (checks it as incorrect).
     * Would be cool if this was made using styles or something, like the view had three styles,
     * checked, unchecked and edited. (the edited one is determined in setPhotoLOafterNewPic())
     * @param checked true -> checks it, false -> unchecks
     */

    public void setPhotoLOCheck(boolean checked) {
        photoCB = (CheckBox) view.findViewById(R.id.checkbox3);
        final ImageView darkBG = (ImageView) view.findViewById(R.id.dark_bg2);
        final TextView takePhotoText = (TextView) view.findViewById(R.id.update_photo_text);
        final TextView PhotoText = (TextView) view.findViewById(R.id.photo_text);

        if(checked) {
            PhotoText.setText("Photo incorrect");
            PhotoText.setTextColor(Color.parseColor("#bd0000"));
            darkBG.setVisibility(View.VISIBLE);
            takePhotoText.setVisibility(View.VISIBLE);
            photoCB.setChecked(true);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                darkBG.setBackground(getResources().getDrawable(R.drawable.dark_with_red_borders, getContext().getTheme()));
            } else {
                darkBG.setBackground(getResources().getDrawable(R.drawable.dark_with_red_borders));
            }

        } else {
            PhotoText.setText("Photo");
            PhotoText.setTextColor(Color.parseColor("#9e9e9e"));
            darkBG.setVisibility(View.GONE);
            takePhotoText.setVisibility(View.GONE);
            photoCB.setChecked(false);
        }
    }

    /**
     * Updates the landmark photo view after a new photo has been taken.
     */

    public void setPhotoLOAfterNewPic() {
        ImageView darkBG = (ImageView) view.findViewById(R.id.dark_bg2);
        TextView updatePhotoText = (TextView) view.findViewById(R.id.update_photo_text);

        updatePhotoText.setVisibility(View.GONE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            darkBG.setBackground(getResources().getDrawable(R.drawable.red_borders, getContext().getTheme()));
        } else {
            darkBG.setBackground(getResources().getDrawable(R.drawable.red_borders));
        }
    }

    /**
     * Opens the marker editing view (a new minimap) with the old marker on it
     * and it can be moved somewhere else by pressing on the map.
     */

    public void openMarkerEdit() {
        mCallback.openMarkerEdit(mMinimap, editedLandmark, "errorFrag");
    }

    /**
     * Starts the camera intent for taking a new photo for the landmark.
     */

    public void openPhotoEdit() {
        // create Intent to take a picture and return control to the calling application
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // start the image capture intent
        startActivityForResult(intent, 1);
    }

    /**
     * Sends the error report to server, listens for the response and opens the success dialog
     * if the sending was successful.
     */

    public void makeErrorReport() {
        progressView = (RelativeLayout) view.findViewById(R.id.progressView);
        progressView.setVisibility(View.VISIBLE);
        server.process(new SendErrorReport(editedLandmark, newLocation), new ServerInterface.ServerCallback<String>() {
            @Override
            public void onResult(String response) {
                if(response != null) {
                    if (response.equals("success")) {
                        openReportSuccess();
                    } else {
                        Toast.makeText(getContext() ,"Error sending report.",Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getContext() ,"Error sending report.",Toast.LENGTH_LONG).show();
                }
                progressView.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Opens up the Report successful -dialog. When "ok" is pressed, the fragment is closed.
     */

    public void openReportSuccess() {
        LayoutInflater li = LayoutInflater.from(getContext());
        final View dialogLO = li.inflate(R.layout.dialog_error_success, null);

        // Creates the dialog
        final AlertDialog d = new AlertDialog.Builder(Utils.getContext())
                .setView(dialogLO)
                .setPositiveButton(android.R.string.ok, null)
                .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button p = d.getButton(AlertDialog.BUTTON_POSITIVE);

                p.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        d.dismiss();
                        okayToClose = true;
                        mCallback.onDiscardPressed("error");
                    }
                });
            }
        });
        d.show();
    }

    /**
     * Asks the user for a confirmation to exit the view.
     */

    public void askExitConfirmation() {
        LayoutInflater li = LayoutInflater.from(getContext());
        final View dialogLO = li.inflate(R.layout.dialog_exit_confirmation, null);

        // Creates the dialog
        final AlertDialog d = new AlertDialog.Builder(Utils.getContext())
                .setView(dialogLO)
                .setPositiveButton("DISCARD", null)
                .setNegativeButton("KEEP EDITING", null)
                .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button p = d.getButton(AlertDialog.BUTTON_POSITIVE);
                Button n = d.getButton(AlertDialog.BUTTON_NEGATIVE);

                p.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        d.dismiss();
                        okayToClose = true;
                        mCallback.onDiscardPressed("error");
                    }
                });

                n.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d.dismiss();
                    }
                });
            }
        });
        d.show();
    }

    /**
     * Receives the photo from camera intent, saves it as bitmap and sets it to photo edit thumbnail.
     * @param requestCode
     * @param resultCode
     * @param intent
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(resultCode == Activity.RESULT_OK) {
            Bitmap bmp = (Bitmap) intent.getExtras().get("data");
            landmarkPhoto.setImageBitmap(bmp);
            setPhotoLOAfterNewPic();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            newImageData = stream.toByteArray();
            hasNewPhoto = true;
        }
    }
}
