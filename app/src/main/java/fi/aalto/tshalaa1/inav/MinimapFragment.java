package fi.aalto.tshalaa1.inav;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.Minimap;

/**
 * The MinimapFragment-fragment is for initializing and editing minimapviews it contains.
 */
public class MinimapFragment extends Fragment implements MinimapView.MinimapViewListener {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String ARG_MINIMAP_ID = "param1";
    private static final String ARG_PARAM2 = "param2";
    public boolean forMarkerEdit = false;
    Minimap minimapClone;
    Landmark markerToBeEdited;
    public DrawerLayout drawer;
    SlidingUpPanelLayout slideLO;

    public HashMap<String, Integer> areaID;
    public String[] areaIDArray;
    public int areaAmount;
    public int buildingID;
    public String where;

    private String mParam1;
    List<Particle> partics;

    markerEditing mCallback;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MinimapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MinimapFragment newInstance(String minimapId) {
        MinimapFragment fragment = new MinimapFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MINIMAP_ID, minimapId);
        fragment.setArguments(args);
        return fragment;
    }

    public MinimapFragment() {}

    public interface markerEditing {
        public void onDiscardPressed(String fragment);
        public void onFinishPressed(Landmark newLandmark, String where);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_MINIMAP_ID);
        }
    }

    // private View progress;
    private ViewGroup vg;
    public MinimapView[] allMinimapViews;
    int currentMinimapView;
    DrawerLayout drawerLO;
    LinearLayout floorLO;

    /**
     * Returns the current mininmapview shown to the user.
     * @return current minimapview
     */

    public MinimapView getMinimapView() {
        if(allMinimapViews != null) return allMinimapViews[currentMinimapView];
        else return null;
    }

    /**
     * Changes the floor.
     * @param minimapView the index of the floor
     */

    public void setNewMinimapView(int minimapView) {
        getMinimapView().setVisibility(View.GONE);
        MinimapView mmv = (MinimapView) vg.findViewWithTag("minimapView"+minimapView);
        mmv.setVisibility(View.VISIBLE);
        currentMinimapView = minimapView;
        resetFloorIconColors();
        getMinimapView().invalidate();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This method is merely for the landmark location edit view.
        if(forMarkerEdit) {
            try {
                mCallback = (markerEditing) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString()
                        + " must implement markerEditing");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment according to on which situation the fragment is used (main minimap view or marker editing)
        if(forMarkerEdit) {
            vg = (ViewGroup)inflater.inflate(R.layout.content_edit_marker_location, container, false);
        } else {
            vg = (ViewGroup)inflater.inflate(R.layout.fragment_minimap, container, false);
        }

        if(forMarkerEdit) {
            MinimapView newMinimapview = new MinimapView(getContext());

            FrameLayout.LayoutParams newParams =
                    new FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT);
            newMinimapview.setLayoutParams(newParams);

            allMinimapViews = new MinimapView[1];
            allMinimapViews[0] = newMinimapview;

            vg.addView(newMinimapview);
        }

        return vg;
    }

    /**
     * Initializes the allMinimapViews-array that contains all of the minimapviews in the building.
     * @param length the amount of the views.
     */

    public void initMinimapArray(int length) {
        allMinimapViews = new MinimapView[length];
    }

    /**
     * Initializes all minimapviews.
     */

    public void initMinimapViews() {
        for(int i = 0; i < allMinimapViews.length; i++) {
            initNewMinimapView(i);
            allMinimapViews[i] = (MinimapView) vg.findViewWithTag("minimapView"+i);
            allMinimapViews[i].setLongClickable(true);
            registerForContextMenu(allMinimapViews[i]);
            allMinimapViews[i].setDrawerLO(drawerLO);
        }
    }

    /**
     * Initializes the minimapview at specific index.
     * @param index the index
     */

    public void initNewMinimapView(int index) {
        MinimapView newMinimapview;
        newMinimapview = new MinimapView(getContext());
        newMinimapview.setTag("minimapView"+index);
        FrameLayout.LayoutParams newParams =
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
        newMinimapview.setLayoutParams(newParams);
        newMinimapview.passSlideLO(slideLO);
        newMinimapview.passAreaID(Integer.parseInt(areaIDArray[index]));
        newMinimapview.passBuildingID(buildingID);
        newMinimapview.setMinimapViewListener(this);
        if(index != 0) newMinimapview.setVisibility(View.GONE);
        //set the floorLO to all minimapviews (it's static)
        if(index == allMinimapViews.length-1) newMinimapview.passFloorLO(floorLO);
        vg.addView(newMinimapview, newParams);
    }

    public void openCamFrag(Fragment camFragment) {
        final FragmentTransaction ft =((FragmentActivity) this.getContext()).getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.camera_view_layout, camFragment, "CamFragmentTag");
        ft.addToBackStack(null);
        ft.commit();
    }

    /**
     * Sets all the floor number buttons to light blue but the one that the user has selected.
     */

    public void resetFloorIconColors() {
        for(int i = 0; i < floorLO.getChildCount(); i++) {
            toggleFloorButton(floorLO.getChildAt(i), false);
        }
        toggleFloorButton(floorLO.getChildAt(allMinimapViews.length - 1 - currentMinimapView), true);
    }

    /**
     * Changes the floor button's background according to the pressed-boolean.
     * @param floorB the floor button
     * @param pressed true -> dark green, false -> light green
     */

    public void toggleFloorButton(View floorB, boolean pressed) {
        // The isAdded fixes a bug that causes the app to crash when pressing back-button quickly after opening MainActivity.
        if(isAdded()) {
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
    }

    public void passSlideLO(SlidingUpPanelLayout sLO) {
        this.slideLO = sLO;
    }

    public void passFloorLO(LinearLayout fLO){
        floorLO = fLO;
    }

    public void passAreaIDs(HashMap<String, Integer> areaidmap, String[] areaidarray) {
        areaID = areaidmap;
        areaIDArray = areaidarray;
        areaAmount = areaidarray.length;
    }

    public void passBuildingID(int buildingID) {
        this.buildingID = buildingID;
    }

    /**
     * Sets the navigation drawer to this fragment.
     * @param dLO the drawer layout
     */

    public void setDrawer(DrawerLayout dLO) {
        drawerLO = dLO;
    }

    /**
     * Sets all the important parameters for minimapview based on the minimap given as parameter
     * @param minimap the minimap that contains the bitmap and bounds that are given to minimapview
     * @param i the index of the minimapview that is wanted to be changed
     */

    public void updateMinimap(final Minimap minimap, int i) {
        if(allMinimapViews != null) {
            if (vg == null) { System.out.println("Viewgroup is null"); return; }

            if (allMinimapViews[i]==null) System.out.println("view is null");

            System.out.println("Minimap is"+((minimap!=null) ? " not " : " ") +"null.");

            if (!forMarkerEdit) allMinimapViews[i].setMinimapClone(minimap.copy());

            allMinimapViews[i].setMiniMap(minimap.getBitmap());
            //minimapView.setMiniMap(minimap.getDrawable());
            allMinimapViews[i].setMinimapBounds(minimap.getBounds());
            if(i == 0 && !forMarkerEdit) setNewMinimapView(i);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {

        if(forMarkerEdit) {
            updateMinimap(minimapClone, 0);
            initButtonListeners();
            getMinimapView().setLandmarks(Arrays.asList(markerToBeEdited), false);
            centerMap(700);
        }

        super.onActivityCreated(savedInstanceState);
    }

    /**
     * These are for the landmark location editing. Defines what happens when
     * the cancel- and finish buttons are pressed.
     */

    public void initButtonListeners() {
        Button cancel = (Button) vg.findViewById(R.id.cancelB);
        Button finish = (Button) vg.findViewById(R.id.finishB);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMinimapView().stepper.cancel();
                mCallback.onDiscardPressed("markerEdit");
            }
        });

        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMinimapView().stepper.cancel();
                if(where.equals("errorFrag")) {
                    mCallback.onFinishPressed(getMinimapView().updatedLandmark, "errorFrag");
                } else if(where.equals("newLandmarkFrag")) {
                    mCallback.onFinishPressed(getMinimapView().updatedLandmark, "newLandmarkFrag");
                } else {
                    System.err.println("The caller of openMarkerEdit still has a typo in parameter 'where'.");
                }

            }
        });
    }

    /**
     * The context menu that opens when the user presses the landmark that they have placed.
     * @param menu the contextmenu
     * @param v the view
     * @param menuInfo the menu infrmation
     */

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        removeOtherLocalLMs(getMinimapView());
        MenuInflater inflat = getActivity().getMenuInflater();
        inflat.inflate(R.menu.landmark_popup,menu);
    }

    /**
     * Defines what happens when an item in the context menu is pressed.
     * @param item the item pressed
     * @return true if the selection was successful, false otherwise
     */

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {

            case R.id.action_setdestination: {
//                System.out.println("new landmark needs an areaID " + getKey(areaID, currentMinimapView));
                getMinimapView().getLocalLandmark().areaID = Integer.parseInt(getKey(areaID, currentMinimapView));
                ((MainActivity) getActivity()).navigateToDestination(getMinimapView().getLocalLandmark()); break;
            }
            case R.id.action_save_landmark:
                getMinimapView().onEditButtonPressed(getMinimapView().getLocalLandmark(), true); break;
            case R.id.action_add_new_landmark:
                openNewLandmarkFrag(getMinimapView().getLocalLandmark());
            default:
                System.out.println("dest: "+item.getItemId()); break;
        }

        return super.onContextItemSelected(item);
    }

    /**
     * Opens a fragment that lets the user to create and send a new landmark to server.
     * @param newLandmark the new landmark that the user placed on map
     */

    public void openNewLandmarkFrag(Landmark newLandmark) {
        String[] tempTypes = {"Choose a type","Room","Entrance","Toilet","Restaurant","Fire alarm","Other"};        //TODO: get these from server when ready
        Bitmap bmp = getMinimapView().overlay(getMinimapView().map, newLandmark);
        Landmark newLM = newLandmark.copy();
        newLM.setTitle("New landmark");
        newLM.setDescription("");
        NewLandmarkFragment lmFrag = NewLandmarkFragment.newInstance(newLM, tempTypes, bmp, getMinimapView().minimapClone);

        final FragmentTransaction ft = ((FragmentActivity) this.getContext()).getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.new_landmark_layout, lmFrag, "newLandmarkFrag");
        ft.addToBackStack(null);
        ft.commit();
    }

    static String getKey(HashMap<String, Integer> map, Integer value) {
        String key = null;
        for(Map.Entry<String, Integer> entry : map.entrySet()) {
            if((value == null && entry.getValue() == null) || (value != null && value.equals(entry.getValue()))) {
                key = entry.getKey();
                break;
            }
        }
        return key;
    }

    /**
     * Removes all other user placed landmarks except the one on the current floor.
     * @param mmView
     */

    public void removeOtherLocalLMs(MinimapView mmView) {
        for (MinimapView mmv : allMinimapViews) {
            if(mmv != mmView) {
                mmv.newLocalLM = null;
            }
        }
    }

    /**
     * A bit confusing setlandmarks method
     * @param l the list of landmarks wanted to be set
     * @param fromFavs are the landmarks to be set from the FavoritesActivity
     */

    public void setLandmarks(final List<Landmark> l, boolean fromFavs) {
        int firstIndex = -1;
        int userFloor = -1;
        List<List<Landmark>> landmarks = new ArrayList<List<Landmark>>();

        for(int i = 0; i < areaAmount; i++) {
            landmarks.add(new ArrayList());
            allMinimapViews[i].newLocalLM = null;
        }

        for(Landmark lm : l) {
            System.out.println("areaID"+areaID+" lm areaid: "+lm.areaID);
            int index = areaID.get(Integer.toString(lm.areaID));

            if(firstIndex == -1) firstIndex = index;
            if(userFloor == -1 && allMinimapViews[index].userPos != null) userFloor = index;

            landmarks.get(index).add(lm);
        }

        for(int i = 0; i < areaAmount; i++) {
            allMinimapViews[i].setLandmarks(landmarks.get(i), fromFavs);
        }

        // The algorithm takes into account if some of the landmarks are on the same floor than the user
        // and if so, this changes the floor to the one that the user is on. If not, it shows the lowest
        // floor that has the landmark.
        if(userFloor != -1) setNewMinimapView(userFloor);
        else if(firstIndex != -1) setNewMinimapView(firstIndex);
    }

    public void reset() {
        getMinimapView().reset();
    }

    public void centerMap(int time) {
        getMinimapView().alignCenter(time);
    }

}
