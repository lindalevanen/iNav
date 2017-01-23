package fi.aalto.tshalaa1.inav;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.aalto.tshalaa1.inav.entities.Area;
import fi.aalto.tshalaa1.inav.entities.Building;
import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.Minimap;
import fi.aalto.tshalaa1.inav.entities.Point3D;
import fi.aalto.tshalaa1.inav.server.AreaRequest;
import fi.aalto.tshalaa1.inav.server.ServerInterface;

/**
 * MainActivity initializes one of the most important fragments, NavigationFragment, and passes
 * important data to other classes and fragments.
 * It also handles fragment activity from many important fragments.
 */
public class MainActivity extends FragmentActivity
        implements InfoFragment.OnInfoButtonPressed, ErrorFragment.onErrorPressed, MinimapFragment.markerEditing, NewLandmarkFragment.onNewLandmarkCreate {

    /** the main fragment of the app. most of the Navigation Client's functionality is in the NavigationFragment, rather than in the MainActivity */
    private NavigationFragment navigationFragment = new NavigationFragment();
    public DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ArrayAdapter<String> mAdapter;
    public static String favPrefsName;
    public SharedPreferences favPrefs;
    private ParticleFilter pFilter;
    private ServerInterface server = new ServerInterface();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_layout);
        /**
         * set this Activity as the main Context of the whole app.
         * This allows other classes in the application to access Android features via the globally available getContext() method in Utils.
         * */
        Utils.setContext(this);

        // Collects the Extras sent from StartingActivity that contains the building information.
        String jsonBuild = (String) getIntent().getExtras().get("buildingInfo");
        Gson gson = new Gson();
        Type type = new TypeToken<Building>() {}.getType();
        final Building buildObj = gson.fromJson(jsonBuild, type);

        // Set up Shared Preferences
        favPrefsName = buildObj.alias;
        favPrefs = getSharedPreferences(favPrefsName, Context.MODE_PRIVATE);

        // Add the navigation fragment to this activity
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_frame, navigationFragment)
                    .commit();
        }

        initDrawer();
        navigationFragment.passNavDrawer(mDrawerLayout);

        minimapInit(buildObj);

        /**
         * set this Activity as the main Context of the whole app.
         * This allows other classes in the application to access Android features via the globally available getContext() method in Utils.
         * */

//        pFilter = new ParticleFilter();
//        pFilter.setNavigationFragment(navigationFragment);
//        pFilter.setDetectors();
//        pFilter.setTestPath();
//        navigationFragment.partics = pFilter.particles;


    }

    /**
     * Asks for the area id from the server with the building id
     * that was gotten from the Starting Activiy and initializes the minimap.
     */

    public void minimapInit(final Building buildObj) {

        server.process(new AreaRequest(buildObj.buildingID), new ServerInterface.ServerCallback<Area[]>() {
            @Override
            public void onResult(Area[] response) {
                if (response == null) {
                    Toast.makeText(getApplicationContext(),"Unable to find building areas!.",Toast.LENGTH_SHORT);
                    return;
                }

                initFragments(response, buildObj.buildingID);

                for(int i = 0; i < response.length; i++) {
                    navigationFragment.navigator.setMinimapBounds(response[i].bounds, i);
                    navigationFragment.navigator.requestMinimap(response[i].areaID, i);
                }
            }
        });
    }

    /**
     * Initializes navigationfragment, navigator and minimapfragment.
     * @param response The building's areas
     * @param buildingID the building's id
     */

    public void initFragments(Area[] response, int buildingID) {
        int len = response.length;

        String[] areaIDarray = new String[len];
        HashMap<String, Integer> areaID = new HashMap<String, Integer>();
        for(int i = 0; i < len; i++) {
            areaIDarray[i] = response[i].areaID;
            areaID.put(response[i].areaID, i);
        }

        Navigator navi = navigationFragment.navigator;
        navi.initMinimapArray(len);
        navi.initMinimapBounds(len);
        navi.passAreaAmount(len);
        navi.requestAllLandmarks(buildingID);

        MinimapFragment mmf = navigationFragment.navigator.getMiniMapFragment();
        mmf.passAreaIDs(areaID, areaIDarray);
        mmf.passBuildingID(buildingID);
        mmf.passSlideLO(navigationFragment.slideLO);
        mmf.initMinimapArray(len);
        mmf.initMinimapViews();

        navigationFragment.setUpFloorButtons(len);
        navigationFragment.resetFloorIconColors();
    }

    /**
     * Set up the navigation drawer
     */

    public void initDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.root_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        this.addDrawerItems();

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });
    }

    /**
     * Determine what happens when the items in the navi drawer are pressed.
     * @param position the position of the item in the list
     */

    public void selectItem(int position) {
        switch (position) {
            case 0:
                Intent intent = new Intent(getApplicationContext(), FavoritesActivity.class);
                startActivityForResult(intent, 1);
                mDrawerLayout.closeDrawer(GravityCompat.START);
                break;
            case 1:
                Toast.makeText(MainActivity.this, "Settings not yet implemented!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * Init the drawer items and add them to Drawer list.
     */

    private void addDrawerItems() {
        String[] items = getResources().getStringArray(R.array.drawer_items);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
        mDrawerList.setAdapter(mAdapter);
    }

    /**
     * Initiates a navigation request. The navigator (on a deeper level) will attempt to navigate from the current position to another
     * position and show navigation information on the screen.
     * @param landmark: the landmark to navigate to
     */
    public void navigateToDestination(Landmark landmark) {
        navigationFragment.navigateToDestination(landmark);
    }

    /**
     * This method gets called when the user presses the landmark info fragment (not any buttons in it but the view).
     */

    public void onInfoPressed() {
        navigationFragment.enlargeInfoPanel();
    }

    /**
     * Determines what happens when a button is pressed on the landmark info.
     * If a minimapclone hasn't been set, this sets it.
     * The clone is for landmark location editing in Error Reporting.
     */

    boolean cloneSet = false;

    /**
     * Determines what happens when the user presses a button on landmark info fragment. Specific actions to
     * different buttons are defined in Minimapview.
     * @param button the button pressed
     * @param landmark the landmark that the info was about
     */

    public void onButtonPressed(View button, Landmark landmark) {
        if(!cloneSet) {
            cloneSet = true;
        }
        getMinimap().onButtonPressed(button, landmark);
    }

    /**
     * Fetches the minimap. (Only to be called and used when the minimap has been set!)
     * @return the minimapview of this activity.
     */

    public MinimapView getMinimap() {
        if(navigationFragment.navigator == null) {
            return null;
        } else {
            return navigationFragment.navigator.getMiniMapFragment().getMinimapView();
        }
    }

    /**
     * Sets a new landmark to the correct minimapview defined by the landmark's areaID.
     * @param lm the landmark to be set
     */

    public void setLandmark(Landmark lm) {
        List<Landmark> lmlist = Arrays.asList(lm);
        navigationFragment.navigator
                .getMiniMapFragment()
                .setLandmarks(lmlist, true);
    }

    @Override
    public void onBackPressed() {
        if(exitConfirmationShowed()) return;
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else if (getMinimap() != null) {
            getMinimap().onBackPressed();
            super.onBackPressed();
        }
    }

    /**
     * Show the exit confirmation in either Error Report View or Create New Landmark View if the
     * user has made changes to those views.
     * @return true if confirmation was shown, false if not
     */

    public boolean exitConfirmationShowed() {
        android.support.v4.app.FragmentManager manager = getSupportFragmentManager();

        ErrorFragment errorFrag = (ErrorFragment) manager.findFragmentByTag("errorFragment");
        NewLandmarkFragment newLMFrag = (NewLandmarkFragment) manager.findFragmentByTag("newLandmarkFrag");

        if(errorFrag != null) {
            if(!errorFrag.okayToClose && errorFrag.madeChanges()) {
                errorFrag.askExitConfirmation();
                return true;
            }
        }

        if(newLMFrag != null) {
            if(!newLMFrag.okayToClose && newLMFrag.madeChanges()) {
                newLMFrag.askExitConfirmation();
                return true;
            }
        }
        return false;
    }

    /**
     * Updates all landmarks in all minimapviews on resume so that landmark info shows correctly
     * when coming back from FavoritesActivity.
     */

    @Override
    protected void onResume() {
        if(getMinimap() != null) {
            MinimapView[] allminimapviews = navigationFragment.navigator.getMiniMapFragment().allMinimapViews;
            for(MinimapView mmv : allminimapviews) {
                mmv.updateLandmarks();
            }
        }
        super.onResume();
    }

    /**
     * Defines what happens when the user presses back button in toolbar in error reporting fragment,
     * landmark location editing or in creating a new landmark.
     * @param fragment the fragment where this method gets called
     */

    public void onDiscardPressed(String fragment) {
        if(!fragment.equals("markerEdit") && exitConfirmationShowed()) return;
        android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
        switch (fragment) {
            case "error":
                getMinimap().errorOpen = false;
                navigationFragment.hideKeyboard();
                break;
            case "markerEdit":
                getMinimap().markerEditOpened(false);
                break;
            case "newLandmark":
                break;
        }
        manager.popBackStack();
    }

    /**
     * Passes landmark with new location to Errorfragment or NewLandmarkFragment
     * when the user is finished marking the new location in Landmark location editor.
     * @param newLandmark the edited landmark
     */

    public void onFinishPressed(Landmark newLandmark, String where) {
        android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
        // Pass landmark with new location to correct fragment:
        switch (where) {
            case "errorFrag":
                ErrorFragment possibleFrag1 = (ErrorFragment) manager.findFragmentByTag("errorFragment");
                possibleFrag1.passMarkerData(newLandmark);
                getMarkerBitmap(newLandmark, "errorFrag");
                break;
            case "newLandmarkFrag":
                NewLandmarkFragment possibleFrag2 = (NewLandmarkFragment) manager.findFragmentByTag("newLandmarkFrag");
                possibleFrag2.passMarkerData(newLandmark);
                getMarkerBitmap(newLandmark, "newLandmarkFrag");
                break;
            default:
                System.err.println("The caller of openMarkerEdit STILL has a typo in parameter 'where'.");
        }
        // In the end close the marker editing fragment
        onDiscardPressed("markerEdit");
    }

    /**
     * Passes the new bitmap of the minimap and the landmark marker to Errorfragment
     * so that it can be used in viewing the thumbnail of the location of the landmark.
     * @param newlm the landmark to be shown on the thumbnail
     */

    public void getMarkerBitmap(Landmark newlm, String where) {
        Bitmap map = getMinimap().map;
        Bitmap newBitmap = getMinimap().overlay(map, newlm);
        android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
        switch (where) {
            case "errorFrag":
                ErrorFragment possibleFrag1 = (ErrorFragment) manager.findFragmentByTag("errorFragment");
                possibleFrag1.passNewBitmap(newBitmap);
                break;
            case "newLandmarkFrag":
                NewLandmarkFragment possibleFrag2 = (NewLandmarkFragment) manager.findFragmentByTag("newLandmarkFrag");
                possibleFrag2.passNewBitmap(newBitmap);
                break;
            default:
                System.err.println("The caller of openMarkerEdit has a typo in parameter 'where'.");
        }


    }

    /**
     * Opens the marker editing view in Error fragment.
     * @param minimap the clone of the original minimap
     * @param landmark the landmark to be edited
     */

    public void openMarkerEdit(Minimap minimap, Landmark landmark, String where) {
        MinimapFragment minimapFragment = new MinimapFragment();
        minimapFragment.forMarkerEdit = true;
        minimapFragment.minimapClone = minimap;
        minimapFragment.markerToBeEdited = landmark;

        if(where.equals("errorFrag")) { minimapFragment.where = "errorFrag"; }
        else if(where.equals("newLandmarkFrag")) { minimapFragment.where = "newLandmarkFrag"; }
        else System.err.println("The caller of openMarkerEdit has a typo in parameter 'where'.");

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.new_minimap_fragment, minimapFragment, "editMarkerLocation");
        transaction.addToBackStack(null);
        transaction.commit();
        getMinimap().markerEditOpened(true);
    }

    /**
     * Activity result method for the return value from FavoritesActivity atm.
     * Sets the landmark on the minimap that was selected in the FavoritesActivity.
     * @param requestCode
     * @param resultCode
     * @param data
     */

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if(resultCode == RESULT_OK){
                Bundle b = data.getExtras();
                if (b != null) {
                    String lmjson = b.getString("newlandmark");
                    Type type = new TypeToken<Landmark>() {}.getType();
                    Gson gson = new Gson();
                    Landmark lm = gson.fromJson(lmjson, type);
                    setLandmark(lm);
                }
            }
        }
    }
}
