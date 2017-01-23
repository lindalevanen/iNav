package fi.aalto.tshalaa1.inav;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SlidingPaneLayout;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import fi.aalto.tshalaa1.inav.entities.Building;
import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.Point3D;
import fi.aalto.tshalaa1.inav.entities.Route;
import fi.aalto.tshalaa1.inav.server.LandmarkRequest;
import fi.aalto.tshalaa1.inav.server.ServerInterface;

/**
 * NavigationFragment initializes handles most of the actions that are performed to
 * all the other MainActivity components but the minimap. These components include e.g. the search bar,
 * floor buttons and the locate button.
 */
public class NavigationFragment extends Fragment implements Navigator.NavigatorListener {

    private static final String TAG = "NavigationFragment";

    // the main folder in which the app stores data
    private final File NAVIGATION_CLIENT_MAIN_FOLDER = new File(Environment.getExternalStorageDirectory(), "Navigation Client");

    Navigator navigator;
    private MinimapFragment minimapFragment;
    public List<Particle> partics;

    // current photo file used by the app to temporarily store a photo before it is actually saved into the gallery
    private File currentPhoto = null;

    // the last successful photo
    private File lastPhoto = null;

    /**
     * a variable for storing the JSON data sent along with images in location requests.
     * This variable might be set by reading JSON from a file (gallery request), or by collecting new data when taking fresh pictures.
     * Either way, it will be passed to the navigator at a new request.
     */
    private String lastJSON = null;

    /**
     * will be set to true whenever the user gets a location with the camera, or false if it was the gallery
     * Used for the development version of the app, to determine whether to save (when taken by camera) or not to save (when taken form gallery) a picture after it was located
     */
    private Boolean lastRequestWasCamera = false;

    private LinearLayout searchLayout;
    private AutoCompleteTextView searchField;

    /**
     * request codes for determining (on the intent result) what action was just taken:
     * did the user get a new location with the camera (code 0)
     * or did she get a new location from the gallery (code 2), or set a destination from the gallery (code 3)
     */
    private int REQUEST_CODE_CAMERA_START = 0;
    private int REQUEST_CODE_GALLERY_START = 2;
    private int REQUEST_CODE_GALLERY_END = 3;

    ProgressBar progressBar;
    DrawerLayout drawerLO;
    public String areaID;
    View rootView;
    LinearLayout floorLO;
    public ToggleButton navToggle;
    public InternalCamera camFragment;

        /**
         * Standard Fragment onCreateView method.
         * <p/>
         * Used by the NavigationFragment to initialize a lot of different View elements and other data.
         */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.content_main, container, false);
        super.onCreateView(inflater, container, savedInstanceState);

        this.verifyStoragePermissions(getActivity());

        progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

        navigator = Navigator.getInstance(this.getActivity());
        navigator.setNavigatorListener(this);

        navigator.pBar = progressBar;

        minimapFragment = new MinimapFragment();
        camFragment = new InternalCamera();
        minimapFragment.partics = this.partics;

        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.add(R.id.minimap_fragment, minimapFragment, "minimapFrag").commit();
        navigator.setMiniMap(minimapFragment);

        navToggle = (ToggleButton) rootView.findViewById(R.id.toggleNavigate);
        navigator.setToggleNavigate(navToggle);
        camFragment.setNavigator(navigator);
        navigator.passCamFrag(camFragment);

        searchLayout = (LinearLayout) rootView.findViewById(R.id.search_layout);
        searchField = (AutoCompleteTextView) rootView.findViewById(R.id.search_field);

        searchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                /** If the user pressed the search button on the keyboard, the app will try to search for landmarks with the search terms entered */
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {

                    /** search terms handed to the navigator. This will find available (if any) landmarks for the given terms */
                    String formattedText = searchField.getText().toString().toLowerCase().trim();
                    navigator.noLandmarks = true;
                    navigator.requestLandmarks(formattedText, navigator.buildingID);

                    searchField.setText("");
                    navigator.getMiniMapFragment().getMinimapView()
                            .slideLO.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                    hideKeyboard();
                    return true;
                }
                return false;
            }

        });

        navigator.searchField = this.searchField;

        rootView.findViewById(R.id.search_field_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //System.out.println("the areaID should be something" + areaID);
                String formattedText = searchField.getText().toString().toLowerCase().trim();
                navigator.noLandmarks = true;
//                for(int i = 0; i < navigator.minimap.length; i++) {
                    navigator.requestLandmarks(formattedText, navigator.buildingID);
//                }
                searchField.setText("");
                navigator.getMiniMapFragment().getMinimapView()
                        .slideLO.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                hideKeyboard();
            }
        });

        rootView.findViewById(R.id.navOpener).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLO.openDrawer(GravityCompat.START);
            }
        });

        Button LButton = (Button) rootView.findViewById(R.id.locateFromGallery);
        LButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                galleryIntent(true);
            }
        });

        FloatingActionButton CamButton = (FloatingActionButton) rootView.findViewById(R.id.locateButton);
        CamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                navigator.getMiniMapFragment().openCamFrag(camFragment);

//                Navigator item = navigator;
//                Gson gson = new Gson();
//                String navJson = gson.toJson(item);

//                ft.addToBackStack(null);
//                Intent intent = new Intent(Utils.getContext(), InternalCamera.class); //REMOVE

//                intent.putExtra("navigator", navJson);

//                startActivityForResult(intent, 7); //REMOVE

//                cameraIntent(0);
            }
        });

//        //For testing purposes
//        Button dialogButton = (Button) rootView.findViewById(R.id.dialogbutton);
//        dialogButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                navigator.openOffCourseDialog();
//            }
//        });

        /* For initializing the  */
        FloatingActionButton floors = (FloatingActionButton) rootView.findViewById(R.id.floors);
        floorLO = (LinearLayout) rootView.findViewById(R.id.floorLO);
        minimapFragment.passFloorLO(floorLO);

        floors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //toggleFloorButton(floorLO.getChildAt(navigator.getMiniMapFragment().currentMinimapView), true);
                if(floorLO.getVisibility() == View.VISIBLE) { floorLO.setVisibility(View.GONE);}
                else { floorLO.setVisibility(View.VISIBLE); }
            }
        });


        panelSlideInit();

        /** at each start of the app, clean the folder of leftover files (eg. JSON text files that haven't been deleted, etc) */
        this.cleanNavigationFolder();

        navigator.setNavDrawer(drawerLO);

        return rootView;
    }

    int floorAmount;

    public void setUpFloorButtons(final int amount) {
        floorAmount = amount;

        // Stores drawables to array
        List<Drawable> icons = new ArrayList<Drawable>();
        icons.add(getResources().getDrawable(R.drawable.icon1));
        icons.add(getResources().getDrawable(R.drawable.icon2));
        icons.add(getResources().getDrawable(R.drawable.icon3));

        // Inits buttons
        for(int i = 0; i < amount; i++) {
            final int index = i;
            LayoutInflater inflater = LayoutInflater.from(getContext());
            final FloatingActionButton layout = (FloatingActionButton) inflater.inflate(R.layout.floor_button, null, false);

            final FloatingActionButton floorB = (FloatingActionButton) layout.findViewById(R.id.floorButton);
            floorB.setTag("floorB"+(amount-1-i));     //amount-1-i so that the order of the floor is correct
            floorB.setImageDrawable(icons.get((amount-1-i)));
            LinearLayout.LayoutParams buttonparams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            buttonparams.setMargins(toPx(20d), 0, toPx(5d), toPx(10d));
            floorB.setLayoutParams(buttonparams);

            if(i == 0) {
                toggleFloorButton(floorB, true);
            }

            floorLO.addView(layout);

            floorB.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    minimapFragment.setNewMinimapView(amount-1-index);
                    resetFloorIconColors();
                }
            });
        }
    }

    /**
     * Sets all the floor number buttons to light blue but the one that the user has selected.
     */

    public void resetFloorIconColors() {
        for(int i = 0; i < floorLO.getChildCount(); i++) {
            toggleFloorButton(floorLO.getChildAt(i), false);
        }
        toggleFloorButton(floorLO.getChildAt(floorAmount - 1 - navigator.getMiniMapFragment().currentMinimapView), true);
    }

    /**
     * Changes the floor button's background according to the pressed-boolean.
     * @param floorB the floor button
     * @param pressed true -> dark green, false -> light green
     */

    public void toggleFloorButton(View floorB, boolean pressed) {
        if(pressed) {
           floorB.getBackground()
                    .setColorFilter(getResources().getColor(R.color.colorPrimaryDarker),
                            PorterDuff.Mode.SRC_ATOP);
        } else {
            floorB.getBackground()
                    .setColorFilter(getResources().getColor(R.color.colorPrimaryDark),
                            PorterDuff.Mode.SRC_ATOP);
        }
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQUEST_CAMERA = 1;
    private static String[] PERMISSIONS_CAMERA = {Manifest.permission.CAMERA};
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001;
    /*For API 23+ you need to request the read/write permissions even if they are already in your manifest.
    http://stackoverflow.com/questions/23527767/open-failed-eacces-permission-denied
    */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission1 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        int permission3 = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        if (permission2 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_CAMERA,
                    REQUEST_CAMERA
            );
        }
        if( permission3 != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION);
        }
    }

    SlidingUpPanelLayout slideLO;

    /**
     * Inits the panel slider for landmark info fragment.
     */

    public void panelSlideInit() {

        slideLO = (SlidingUpPanelLayout) rootView.findViewById(R.id.sliding_layout);
        slideLO.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);

        slideLO.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {

            // slideOffset is a float between 0 and 1, 0 indicates a collapsed panel, 1 expanded.
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                FragmentManager manager = ((FragmentActivity) getContext()).getSupportFragmentManager();
                Fragment frag = manager.findFragmentByTag("landmarkInfo");

                // Determines what happenes when the panel is being dragged.
                if(frag != null && slideOffset >= 0)  {
                    LinearLayout eButtons = (LinearLayout) frag.getView().findViewById(R.id.e_buttons);
                    ImageButton naviB = (ImageButton) frag.getView().findViewById(R.id.button_navigate);
                    eButtons.setAlpha(slideOffset);
                    moveNavigateButton(naviB, slideOffset);

                    Bundle b = frag.getArguments();
                    String wholeDesc = b.getString("wholeDescription");

                    TextView description = (TextView) frag.getView().findViewById(R.id.description);
                    Layout descLO = description.getLayout();
                    description.setText(wholeDesc);

                    // If there are more than 3 lines of description, this adds the rest slowly when the panel is being dragged
                    if(descLO.getLineCount() > 3) {
                        int charsAmount = descLO.getLineEnd(2);

                        // If the panel is collapsed, this adds "..."  to the end if needed
                        if(slideOffset == 0) {
                            description.setMaxLines(3);
                            String subS = wholeDesc.substring(0, charsAmount).trim();
                            String shortDesc;
                            char lastchar = subS.charAt(subS.length()-1);
                            if(lastchar == '.' || lastchar == '!') shortDesc = subS;
                            else shortDesc = subS + "...";

                            description.setText(shortDesc);
                            return;
                        }

                        // Changes the opacity of the rest of the description
                        Spannable wordtoSpan = new SpannableString(wholeDesc);
                        String colorString = colorWithOpacity(slideOffset, "9c9c9c");
                        int color = Color.parseColor(colorString);

                        wordtoSpan.setSpan(new ForegroundColorSpan(color), charsAmount, wholeDesc.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        description.setText(wordtoSpan);
                        description.setMaxLines(8);
                    }
                }
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {}

        });
    }

    /**
     * Enlarges the info panel.
     */

    public void enlargeInfoPanel() {
        if(slideLO.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            slideLO.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        } else if (slideLO.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            slideLO.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
    }

    /**
     * Moves the navigate button (changes its topmargin) when the landmark info is slided.
     * @param naviB the navigation button
     * @param offset float that tells how much the info has been slided (0 - not at all, 1 - all the way)
     */

    public void moveNavigateButton(ImageButton naviB, float offset) {
        double newMargin = 8 + ((double) (40 * offset));
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) naviB.getLayoutParams();
        params.setMargins(0, toPx(newMargin),0,0);
        naviB.setLayoutParams(params);
    }

    /**
     * Gives the color given as a parameter an opacity given as a parameter (float from 0 to 1)
     * @param opacity a float from 0 to 1, 0 being transparent and 1 being opaque
     * @param color the color used
     * @return a color with the opacity given as a parameter
     */

    public String colorWithOpacity(float opacity, String color) {
        int inAlpha = (int) (opacity * 255);
        String hex = Integer.toHexString(inAlpha);
        String rColor;
        if(hex.length() != 2) {
            rColor = "#0"+hex+color;
        } else {
            rColor = "#"+hex+color;
        }
        return rColor;
    }

    /**
     * Converts dps to pixels
     * @param dps the density-independent pixels
     * @return the pixel amount
     */

    public int toPx(Double dps) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }

    public void passNavDrawer(DrawerLayout dlo) { drawerLO = dlo; }

    /**
     * Method for hiding the keyboard, if it was previously visible.
     */
    public void hideKeyboard() {
        searchField.clearFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(searchLayout.getWindowToken(), 0);
    }

    /**
     * Ask the navigator to get a new route, to a specified destination.
     *
     * @param landmark: the point on the map which will be the route's destination.
     */
    public void navigateToDestination(Landmark landmark) {
       navigator.navigateToDestination(landmark);
    }

    /**
     * destroy navigator once we're done with it. eg on application destroy
     */
    public void navigatorDestroy() {
        navigator.destroy();
    }

    public void navigatorRemoveContext() {
        navigator.removeContext();
    }

    /**
     * Method to start the camera.
     *
     * @param code: the requestCode for this intent. the code will be used after the Intent produces a result, to determine what action was taken and how it should act.
     *              Code zero (0) is standard (getting a new location).
     */
    public void cameraIntent(int code) {
        // create Intent to take a picture and return control to the calling application
        //TODO: toasti jostain syystä näkyy horizontaalisesti, voi johtua tosin vain lindan htc onen kamera appista.
//        Toast.makeText(Utils.getContext(), "Take a photo to locate yourself.", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        /** if the folder for this app doesn't exist, create it. */
        if (!NAVIGATION_CLIENT_MAIN_FOLDER.exists())
            NAVIGATION_CLIENT_MAIN_FOLDER.mkdirs();

        currentPhoto = new File(NAVIGATION_CLIENT_MAIN_FOLDER, "img.jpg");

        //currentPhoto = new File(currentPhoto.getAbsolutePath());
        Uri fileUri = Uri.fromFile(currentPhoto); // create a file to save the image
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name

        lastRequestWasCamera = true;

        // start the image capture intent
        startActivityForResult(intent, code);

    }

    public void galleryIntent(boolean start) {
        lastRequestWasCamera = false;

        int code = (start) ? REQUEST_CODE_GALLERY_START : REQUEST_CODE_GALLERY_END;

        Intent gallery = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(gallery, code);
    }

    /**
     * Whenever an Intent returns with a result, this method picks up the result.
     * This happens (for instance) after the camera intent is opened, and returns with a picture taken!
     *
     * Notice that both Gallery and Camera intent results are handled in the same onActivityResult method!
     *
     * @param requestCode: the specified requestCode that was sent with the intent (request codes explained further up in this file)
     * @param resultCode:  was the result OK, or was the request CANCELED?
     * @param data:        the data returned. eg file path
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode: " + requestCode + " resultCode: " + resultCode);
        /** Good responses (OK) are handled here. The user did NOT cancel */
        if (resultCode == Activity.RESULT_OK) {
            byte[] bytes = null;
            try {
                /** Responses from the gallery intent are handled here */
                if (requestCode == REQUEST_CODE_GALLERY_END || requestCode == REQUEST_CODE_GALLERY_START) {
                    //this is the procedure for gallery intent results
                    InputStream is = getActivity().getContentResolver().openInputStream(data.getData());
                    bytes = Utils.bytesFromStream(is);
                    String path = getRealPathFromURI(data.getData());
                    /**
                     * The app will try to read JSON data that was saved from a camera request.
                     * The JSON have the same name as the pictures, except for the different extension.
                     */
                    File jsonFile = new File(path.substring(0, path.length() - 3) + "txt");

                    if (jsonFile.exists())
                        lastJSON = Utils.readFile(jsonFile);
                    else {
                        /**
                         * If no JSON data exists for the selected image, send a default JSON Object with three empty objects.
                         */
                        JSONObject o = new JSONObject();
                        try {
                            o.put("wifi", "");
                            o.put("cell", "");
                            o.put("magn", "");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        lastJSON = o.toString();
                        Log.d(TAG, "onActivityResult: empty JSON: " + lastJSON);
                    }
                }
//                else if(requestCode == 7) {
//                    System.out.println("bs: " + data.getByteArrayExtra("bs"));
//                    System.out.println("last: " + data.getStringExtra("lastJSON"));
//                    navigator.setStart(data.getByteArrayExtra("bs"), data.getStringExtra("lastJSON"));
//                }
                /**
                 * Responses from the camera intent are handled in the below "else" statement.
                 */
                else {
                    navigator.setStart(data.getByteArrayExtra("bs"), data.getStringExtra("lastJSON"));
                    Log.d(TAG, "onActivityResult: new photo collected");
                    FileInputStream ios = new FileInputStream(currentPhoto);
                    bytes = Utils.bytesFromStream(ios);
                    ios.close();
                    String imgId = Utils.getDeviceId() + "_" + System.currentTimeMillis();
                    //this process saves the image to the gallery with a unique name so that it's visible later, if need be.
                    lastPhoto = Utils.renameFile(currentPhoto, imgId + ".jpg");
                    final String sensorFileName =imgId + ".txt";
                    Log.d(TAG, "onActivityResult: lastPhoto:" + lastPhoto);

                    final byte[] bs = bytes;
//                    lastJSON = data.getStringExtra("lastJSON");


                    //((MainActivity) getActivity()).setLoading(true);

                    /**
                     * a new JSONDataCollector is initiated to collect the necessary data to be sent to the server along with the image
                     * read JSONDataCollector.java for specifics.
                     *
                     * A callback method sends the data to the server once it's all collected.
                     */
                    new JSONDataCollector(false, false, false, true, false) {
                        @Override
                        void onSuccessCallback(JSONObject data) {
                            lastJSON = data.toString();
                            Log.d(TAG, "onActivityResult: got sensor data while taking a photo:" + lastJSON);
                            //save lastJSON to file
                            Utils.saveStringToFolder(Utils.getContext(), sensorFileName, lastJSON);
                            Log.d(TAG, "sensors info passed onto Location request from NavFragm " + lastJSON);
//                            Toast.makeText(Utils.getContext(), "Wi-Fi data collected! " + lastJSON, Toast.LENGTH_SHORT).show();
//                            System.out.println("not good but printing image bytes from cam intent: " + bs.length );
//                            navigator.setStart(bs, lastJSON); UNCOMMENT WHEN USING cameraIntent
                            //Toast.makeText(Utils.getContext(), "Wi-Fi data collected!", Toast.LENGTH_SHORT).show();
                        }
                    };

                }

            } catch (FileNotFoundException e) {
                //e.printStackTrace();
            } catch (IOException e) {
                //e.printStackTrace();
            }

            /*
             * Here, responses from the gallery intent are passed to the navigator to get a location or set a destination.
            */

            if (requestCode == REQUEST_CODE_GALLERY_END) {
               // navigator.setEnd(bytes, lastJSON);

            } else if (requestCode == REQUEST_CODE_GALLERY_START) {
                navigator.setStart(bytes, lastJSON);
            }

        } else {
            Toast.makeText(getActivity(), "No image was selected", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "onActivityResult: Image was not selected");

            if (currentPhoto != null && currentPhoto.exists())
                currentPhoto.delete();
        }
    }

    /** android haxing. convert a content location URI to a path String */
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getActivity().getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }

    @Override
    public void onDestroy() {
        navigatorDestroy();
        Log.d(TAG, "onDestroy is called");
        super.onDestroy();
    }

    /**
     * Callback method that fires after a succesful location request.
     * NOT needed when internal cam used
     * @param location: the received location.
     */
    @Override
    public void onLocated(Point3D location) {

        /**
         * image will be added to the gallery if it was indeed located,
         * and if it was shot by the camera (we don't want it to ba added to the gallery multiple times)
         *
         * Also, we write the image's corresponding json data to a text file.
         */

        if (lastRequestWasCamera) {
            Utils.galleryAddPic(getActivity(), lastPhoto);
            Utils.writeFile(new File(lastPhoto.getAbsolutePath().replace(".jpg", ".txt")), lastJSON);
        }
    }

    @Override
    public void onRouteUpdated(Route route) {
        //mSectionsPagerAdapter.onRouteUpdated(route);
        //locateButton.setText(getString(R.string.action_relocate));
        //elsewhereButton.setText(getString(R.string.action_new_destination));
        System.out.print(" ");
    }

    /**
     * For each located image, the app saves images in the gallery along with JSON data.
     * However, when an image is deleted by the user, the JSON data text file remains on the disk.
     * This method cleans up and removes those redundant text files.
     *
     * To be called each time the app is started.
     */
    private void cleanNavigationFolder() {
        File[] files = NAVIGATION_CLIENT_MAIN_FOLDER.listFiles();
        if (files == null) return;
        int count = 0;
        for (File file : files) {
            String p = file.getAbsolutePath();
            if (p.endsWith(".txt") && !(new File(p.replace(".txt", ".jpg")).exists())) {
                file.delete();
                count++;
            }
        }

        System.out.println("FileCleaner: " + count + " textfiles (JSON data files for previous requests) were removed.");
    }

}
