package fi.aalto.tshalaa1.inav;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.Minimap;
import fi.aalto.tshalaa1.inav.server.SendNewLandmark;
import fi.aalto.tshalaa1.inav.server.ServerInterface;

/**
 * The fragment that is opened when the user wants to create a new landmark to be sent to server.
 */
public class NewLandmarkFragment extends android.support.v4.app.Fragment {

    onNewLandmarkCreate mCallback;
    View view;

    private static Landmark mLandmark;
    private static String[] mTypeString;
    private static Bitmap mLandmarkBtmp;
    private static Minimap mMinimap;

    private ImageView landmarkLocation;
    private ImageView landmarkPhoto;

    private EditText nameSection;
    private EditText descriptionSection;

    String type;

    private TextView typeError;
    private TextView photoError;

    ServerInterface server;

    public boolean okayToClose = false;
    private boolean hasNewLocation = false;
    private boolean hasNewPhoto = false;

    public interface onNewLandmarkCreate {
        public void onDiscardPressed(String where);
        public void openMarkerEdit(Minimap minimap, Landmark lm, String where);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mCallback = (onNewLandmarkCreate) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement onNewLandmarkCreate");
        }
    }

    public static NewLandmarkFragment newInstance(Landmark lm, String[] types, Bitmap landmarkBtmp, Minimap minimapClone) {
        //Bundle args = new Bundle();

        mLandmark = lm;
        mTypeString = types;
        mLandmarkBtmp = landmarkBtmp;
        mMinimap = minimapClone;

        NewLandmarkFragment fragment = new NewLandmarkFragment();
        //fragment.setArguments(args);
        return fragment;
    }

    public NewLandmarkFragment() {}

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_make_new_landmark, container, false);

        ImageButton discardB = (ImageButton) view.findViewById(R.id.back_button);
        ImageButton sendB = (ImageButton) view.findViewById(R.id.send_button);
        nameSection = (EditText) view.findViewById(R.id.lm_name);
        descriptionSection = (EditText) view.findViewById(R.id.lm_description);
        typeError = (TextView) view.findViewById(R.id.type_error);
        photoError = (TextView) view.findViewById(R.id.photo_error);

        server = new ServerInterface();

        initSpinner();
        initLocationView();
        initPhotoView();

        discardB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.onDiscardPressed("newLandmark");
            }
        });

        sendB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(hasImportantData()) {
                    String newName = nameSection.getText().toString().trim();
                    String newDescription = descriptionSection.getText().toString().trim();

                    if(!newName.isEmpty()) mLandmark.setTitle(newName);
                    if(!newDescription.isEmpty()) mLandmark.setDescription(newDescription);

                    sendNewLandmark();
                }
            }
        });

        return view;
    }

    /**
     * Checks if the user has chosen a type and taken a picture of the landmark when pressing send.
     * @return true if the user has, false if even one of the attributes are missing.
     */

    public boolean hasImportantData() {
        boolean hasTheData = true;

        if(type == null) {
            typeError.setVisibility(View.VISIBLE);
            hasTheData = false;
        }

        if(mLandmark.getImageData() == null) {
            photoError.setVisibility(View.VISIBLE);
            hasTheData = false;
        }

        return hasTheData;
    }

    /**
     * Initializes the location thumbnail view that shows where the landmark is on the map.
     */

    public void initLocationView() {
        landmarkLocation = (ImageView) view.findViewById(R.id.lm_location);
        landmarkLocation.setImageBitmap(mLandmarkBtmp);

        landmarkLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallback.openMarkerEdit(mMinimap, mLandmark, "newLandmarkFrag");
            }
        });
    }

    /**
     * Initializes the photo thumbnail view that shows the photo of the landmark.
     * In the beginning it's just a grey box with a camera icon in the middle of it.
     */

    public void initPhotoView() {
        landmarkPhoto = (ImageView) view.findViewById(R.id.lm_photo);

        landmarkPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // create Intent to take a picture and return control to the calling application
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 1);
            }
        });
    }

    /**
     * Inits the landmark type dropdown menu and what happens when they are pressed.
     */

    public void initSpinner() {
        Spinner spinner = (Spinner) view.findViewById(R.id.type_spinner);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(getContext(),
                android.R.layout.simple_spinner_item, mTypeString);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                switch (selectedItem) {
                    case "Choose a type":
                        type = null;
                        break;
                    default:
                        type = selectedItem;
                        typeError.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                System.out.println("nothing selected");
            }
        });
    }

    static RelativeLayout progressView;
    /**
     * Sends the newly created landmark to server.
     */
    public void sendNewLandmark() {
        progressView = (RelativeLayout) view.findViewById(R.id.progressView);
        progressView.setVisibility(View.VISIBLE);
        server.process(new SendNewLandmark(mLandmark, type), new ServerInterface.ServerCallback<String>() {
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
     * Passes the landmark location data to this fragment.
     * @param lm the new landmark with the new user defined location
     */

    public void passMarkerData(Landmark lm) {
        hasNewLocation = true;
        mLandmark.setPosition(lm.getLocation());
    }

    /**
     * Passes a new bitmap for the location thumbnail view.
     * @param newBitmap the new bitmap
     */

    public void passNewBitmap(Bitmap newBitmap) {
        landmarkLocation.setImageBitmap(newBitmap);
    }

    /**
     * Opens up a dialog that tells the user that the landmark sending was successful and
     * closes the fragment when the user presses "ok".
     */

    public void openReportSuccess() {
        LayoutInflater li = LayoutInflater.from(getContext());
        final View dialogLO = li.inflate(R.layout.dialog_new_landmark_success, null);

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
                        mCallback.onDiscardPressed("newLandmark");
                    }
                });
            }
        });
        d.show();
    }

    /**
     * Checks whether the user has made any changes.
     * @return true if has, false if hasn't
     */

    public boolean madeChanges() {
        return (!nameSection.getText().toString().trim().isEmpty() ||
                !descriptionSection.getText().toString().trim().isEmpty() ||
                type != null || hasNewLocation || hasNewPhoto);
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
                        mCallback.onDiscardPressed("newLandmark");
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
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            mLandmark.setImageData(stream.toByteArray());
            hasNewPhoto = true;
            ImageView plus = (ImageView) view.findViewById(R.id.plus);
            photoError.setVisibility(View.GONE);
            plus.setVisibility(View.GONE);
        }
    }
}
