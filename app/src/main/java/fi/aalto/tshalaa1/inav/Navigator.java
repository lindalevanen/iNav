package fi.aalto.tshalaa1.inav;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fi.aalto.tshalaa1.inav.entities.Bounds;
import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.Minimap;
import fi.aalto.tshalaa1.inav.entities.Point3D;
import fi.aalto.tshalaa1.inav.entities.Route;
import fi.aalto.tshalaa1.inav.server.LandmarkRequest;
import fi.aalto.tshalaa1.inav.server.LocationRequest;
import fi.aalto.tshalaa1.inav.server.MinimapRequest;
import fi.aalto.tshalaa1.inav.server.RouteRequest;
import fi.aalto.tshalaa1.inav.server.ServerInterface;

/**
 * Navigator-class is for most of the requests like location, landmark and minimap request.
 */
public class Navigator {

    private NavigatorListener listener;
    private Bounds[] minimapBounds;
    public int buildingID;
    public ToggleButton toggleNavigate;
    List<Particle> partics;
    AutoCompleteTextView searchField;
    DrawerLayout drawerLO;
    public Minimap[] minimaps = null;

    public Navigator() {}

    public void setToggleNavigate(ToggleButton toggleNavigate) {
        this.toggleNavigate = toggleNavigate;
    }

    public interface NavigatorListener {
        void onRouteUpdated(Route route);
        void onLocated(Point3D location);
    }

    public interface ImageUpdateListener {
        //void onImageUpdated(Instruction instruction, Bitmap image);
    }

    private ServerInterface server;
    private Route route;
    private Context context;
    private int areaAmount;

    private Point3D startPosition;
    private Landmark destination;

    private MinimapFragment minimapFragment;

    private byte[] startImage;

    private static Navigator INSTANCE;
    ProgressBar pBar;
    Fragment camFrag;

    public static final synchronized Navigator getInstance(Context c) {
        if (INSTANCE == null) {
            INSTANCE = new Navigator(c);
        }
        if (INSTANCE.context == null) {
            INSTANCE.context = c;
        }
        return INSTANCE;
    }

    private Navigator(Context context) {
        server = new ServerInterface();
        this.context = context;
    }

    public void initMinimapArray(int length) {
        minimaps = new Minimap[length];
    }
    public void initMinimapBounds(int length) {
        this.minimapBounds = new Bounds[length];
    }

    public void setMinimapBounds(Bounds b, int i) {
        this.minimapBounds[i] = b;
    }
    public void setMiniMap(MinimapFragment m) {
        minimapFragment = m;
    }

    public MinimapFragment getMiniMapFragment() { return minimapFragment; }

    public void passCamFrag(Fragment camF) {
        this.camFrag = camF;
    }
    public void passAreaAmount(int amount) {
        this.areaAmount = amount;
    }

    public void removeContext() {
        context = null;
    }

    public void destroy() {
        server.disconnect();
        INSTANCE = null;
    }

    public void setNavigatorListener(NavigatorListener l) {
        this.listener = l;
    }

    public void setNavDrawer(DrawerLayout newDrawerLayout) {
        drawerLO = newDrawerLayout;
        minimapFragment.setDrawer(newDrawerLayout);
    }

    public void navigateToDestination(Landmark landmark) {
        destination = landmark;
        tryNavigate();
    }

    /**
     * Toggle the progress circle's visibility
     * @param show true -> show, false -> hide
     */

    private void showProgress(boolean show) {
        if (show) {
            System.out.println("progressbar set visible");
            pBar.setVisibility(View.VISIBLE);
        }
        else {
            System.out.println("progressbar set invisible");
            pBar.setVisibility(View.GONE);
        }
    }

    private void startParticleFilter(Route route){
        ParticleFilter pFilter = new ParticleFilter();
        pFilter.setNavigator(this);
        pFilter.setMinimapViews(getMiniMapFragment().allMinimapViews, getMiniMapFragment().areaID);
        pFilter.setDetectors();
        pFilter.setTestPath(route);
        //spFilter.start();
    }

    private void stopParticleFilter(){
        //disable detectors
//        pFilter.disableDetectors();
        //set route to null in minimapview
        getMiniMapFragment().getMinimapView().setPath(null);
    }

    /**
     * Opens a dialog that tells the user that they are off course during navigation.
     */

    public void openOffCourseDialog() {
        LayoutInflater li = LayoutInflater.from(Utils.getContext());
        final View dialogLO = li.inflate(R.layout.dialog_off_course, null);

        // Creates the dialog
        final AlertDialog d = new AlertDialog.Builder(Utils.getContext())
                .setView(dialogLO)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {

                Button p = d.getButton(AlertDialog.BUTTON_POSITIVE);
                Button n = d.getButton(AlertDialog.BUTTON_NEGATIVE);

                p.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getMiniMapFragment().openCamFrag(camFrag);
                        d.dismiss();
                    }
                });
                n.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        d.dismiss();
                    }
                });
            }
        });
        d.show();
    }

    /**
     * Navigates from start to destination if they are defined.
     */

    private void tryNavigate() {
        if(startPosition == null) {
            Toast.makeText(Utils.getContext(), "Locate yourself first!", Toast.LENGTH_SHORT).show();
            return;
        }

        if(destination == null) {
            Toast.makeText(Utils.getContext(), "Set a destination!", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        server.process(new RouteRequest(startPosition, destination.getLocation(), startPosition.areaID, destination.getAreaID(), buildingID), new ServerInterface.ServerCallback<Route>() {
            @Override
            public void onResult(Route response) {
                showProgress(false);
                if (response == null) {
                    Toast.makeText(context,"Unable to navigate.",Toast.LENGTH_SHORT);
                    return;
                }
                processRoute(response);
                if (listener != null) listener.onRouteUpdated(route);
            }
        });
    }

    public boolean recalculate = false;

    public void setStart(byte[] image, final String jsonobject) {

        startImage = image;

        showProgress(true);
        final long startTime = System.currentTimeMillis();

        //Used to adjust the user orientation while location is being requested
        minimapFragment.getMinimapView().userPos = null;
        minimapFragment.getMinimapView().offSetAngle = 0;

        server.process(new LocationRequest(startImage, jsonobject, buildingID), new ServerInterface.ServerCallback<Point3D>() {
            @Override
            public void onResult(Point3D response) {
                showProgress(false);

                System.out.println("params for location request: "+startImage.length + " " + jsonobject.toString());

                if (response == null) {

                    System.out.println("response 3D point is null");

                    if (context != null) {
                        Toast t = Toast.makeText(context.getApplicationContext(), R.string.location_not_found, Toast.LENGTH_SHORT);
                        t.setGravity(Gravity.CENTER, 0, 0);
                        t.show();
                        listener.onLocated(null);
                    }
                    //Creating a hard coded point for testing when response from server is null
                    //System.out.println("Creating a test response");
                    //response = new Point3D(1.071,0.0,11.61); //subway's location

                    return; //UNCOMMENT WHEN SERVER WORKS
                }

                startPosition = response;

                int index = minimapFragment.areaID.get(Integer.toString(response.areaID));
                minimapFragment.allMinimapViews[index].showUserPos(response);
                minimapFragment.setNewMinimapView(index);

                System.out.println("Minimap request took: "+(System.currentTimeMillis()-startTime)+" milliseconds.");

                if (listener != null) {
                    listener.onLocated(response);
                }

                if(recalculate){
                    tryNavigate();
                }
            }
        });

    }

    private static final Minimap DUMMY_MINIMAP = new Minimap();

    /**
     * Sets the route that was gotten from the RouteRequest on the minimapviews
     * @param response the route
     */

    public void processRoute(Route response) {
        this.route = response;

        Toast.makeText(Utils.getContext()," Turn to face the path and press Start to begin navigating.", Toast.LENGTH_LONG).show();

        toggleNavigate.setChecked(false);

        //go through all the path's point3d's and add them to correct indices to areas based on their areaID.
        List<List<Point3D>> areas = new ArrayList<List<Point3D>>();
        for(int i = 0; i < areaAmount; i++) {
            areas.add(new ArrayList());
        }
        for(Point3D p : route.getPath()) {
            int index = minimapFragment.areaID.get(Integer.toString(p.areaID));
            areas.get(index).add(p);
        }
        if(startPosition.areaID != destination.areaID) {
            // Adds the changing point to starting area TODO: make this support more than 2 floors.. this is just a temporary solution
            int startindex = minimapFragment.areaID.get(Integer.toString(startPosition.areaID));
            int lastindex = minimapFragment.areaID.get(Integer.toString(destination.areaID));
            areas.get(startindex).add(areas.get(lastindex).get(0));
        }

        // Sets the paths to correct floors
        for(int i = 0; i < areaAmount; i++) {
            minimapFragment.allMinimapViews[i].setPath(areas.get(i));
        }

        //minimapFragment.getMinimapView().setPath(route.getPath());

        toggleNavigate.setVisibility(View.VISIBLE);

        toggleNavigate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    //init particles, sensors etc
                    startParticleFilter(route);
                    //userPos only used for finding initial direction
                    getMiniMapFragment().getMinimapView().userPos = null;
                } else {
                    // The toggle is disabled

                    startPosition = null;


                    for(int i = 0; i < areaAmount; i++) {
                        minimapFragment.allMinimapViews[i].setPath(null);
                        minimapFragment.allMinimapViews[i].updateUserParticle(null);
                        minimapFragment.allMinimapViews[i].setVisitedPath(null);
                    }

                    if(!recalculate) {
                        destination = null;
                        toggleNavigate.setVisibility(View.GONE);
                    }
                    recalculate = false;

                }
            }
        });
    }

    /**
     * Requests minimap with area's id.
     * @param ID area's id
     * @param i the index of the minimap
     * @return the minimap gotten from the response
     */

    public Minimap requestMinimap(String ID, final int i) {
        Log.d("Navigator", "Inside requestMinimap");
        if (minimaps[i] == DUMMY_MINIMAP) {
            return null;    //already scheduled
        }
        if(i == 0) minimapFragment.areaID.clear();

        minimapFragment.areaID.put(ID, i);
        minimapFragment.areaIDArray[i] = ID;
        showProgress(true);
        server.process(new MinimapRequest(ID), new ServerInterface.ServerCallback<Minimap>() {
            @Override
            public void onResult(Minimap response) {
                minimaps[i] = response;
                System.out.println("minimap request response: "+response);
                minimaps[i].setBounds(minimapBounds[i]);
                if (response != null) {
                    minimapFragment.updateMinimap(minimaps[i], i);
                }

                showProgress(false);
            }
        });
        minimaps[i] = DUMMY_MINIMAP;
        return null;    //scheduled update
    }

    private String previousSearchWords = null;
    final List<Landmark> list = new ArrayList<Landmark>();
    static boolean noLandmarks = true;

    /**
     * Requests landmarks with keywords.
     * @param keywords the keywords
     * @param buildID the building id
     */

    public void requestLandmarks(String keywords, int buildID) {
        System.out.println("Keywords are correct " + keywords);
        if ((previousSearchWords != null && keywords.equals(previousSearchWords)) || keywords.trim().equals("")) {
            return;
        } else {
            previousSearchWords = keywords;
        }

        final List<Landmark> list = new ArrayList<Landmark>();
        server.process(new LandmarkRequest(keywords, buildID), new ServerInterface.ServerCallback<List<Landmark>>() {
            @Override
            public void onResult(List<Landmark> response) {
                if (response != null) {
                    noLandmarks = false;
                    list.clear();
                    list.addAll(response);
                    System.out.println("response from landmarkRequest = [" + response + "]");
                    minimapFragment.setLandmarks(response, false);
                } else {
                    Toast.makeText(context ,"Nothing to show based on your search query.",Toast.LENGTH_LONG).show();
                }
                System.out.println("end of LandmarkRequest!!");
            }
        });

    }

    List<String> nameList = new ArrayList<String>();

    /**
     * Requests all landmarks for initializing the landmark searching suggestions list.
     */

    public void requestAllLandmarks(int buildID) {
        buildingID = buildID;
        final List<Landmark> list = new ArrayList<Landmark>();
        server.process(new LandmarkRequest("", buildID), new ServerInterface.ServerCallback<List<Landmark>>() {
            @Override
            public void onResult(List<Landmark> response) {
                if (response != null) {
                    list.clear();
                    list.addAll(response);

                    for (Landmark lm : list) {
                        for(Object tag : lm.tags) {
                            String nicerTag = Character.toUpperCase(tag.toString().charAt(0)) + tag.toString().substring(1);
                            if(!nameList.contains(nicerTag)) {
                                nameList.add(nicerTag);
                            }
                        }
                    }

                    setSearchfieldAdapter(nameList);

                } else {
                    //Toast.makeText(Utils.getContext() ,"Nothing to show based on your search query.",Toast.LENGTH_LONG).show();
                }
                System.out.println("end of LandmarkRequest!!");
            }
        });
    }

    /**
     * Initializes an adapter that contains all landmarks for landmark searchfield.
     * @param landmarks all of the landmarks in the building
     */

    public void setSearchfieldAdapter(List<String> landmarks) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(Utils.getContext(),
                android.R.layout.simple_dropdown_item_1line, landmarks);
        searchField.setAdapter(adapter);
    }
}
