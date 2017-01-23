package fi.aalto.tshalaa1.inav;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import fi.aalto.tshalaa1.inav.entities.Building;
import fi.aalto.tshalaa1.inav.entities.Landmark;
import fi.aalto.tshalaa1.inav.entities.Route;
import fi.aalto.tshalaa1.inav.server.BuildingsRequest;
import fi.aalto.tshalaa1.inav.server.RouteRequest;
import fi.aalto.tshalaa1.inav.server.ServerInterface;

/**
 * The Activity that's first opened when the app starts. Shows the list of all buildings.
 */
public class StartingActivity extends AppCompatActivity {

    private ServerInterface server = new ServerInterface();
    List<Building> buildingList;
    final int MY_PERMISSION_ACCESS_COURSE_LOCATION = 1;
    SlidingUpPanelLayout sliderLO;
    BroadcastReceiver networkStateReceiver;
    ProgressBar pBar;
    TextView chooseLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_starting);

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        ImageView banner = (ImageView) findViewById(R.id.banner);
        banner.setMaxHeight(screenHeight/3);

        ImageView logo = (ImageView) findViewById(R.id.logo);
        logo.setMaxHeight(200);

        pBar = (ProgressBar) findViewById(R.id.progressBarS);
        chooseLocation = (TextView) findViewById(R.id.chooseLocation);

        initConnectionListener();
        initSlider();
        initRetry();

        if(isNetworkAvailable()) {
            chooseLocation.setVisibility(View.VISIBLE);
            if ( ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION ) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  },
                        MY_PERMISSION_ACCESS_COURSE_LOCATION );
            } else {
                requestBuildings();
            }
        } else {
            //this corresponds to opening the "no connection" view
            sliderLO.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
    }

    /**
     * Inits the building list.
     */

    public void initList(){
        if (ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // The user has granted permission for coarse location -> find closest building.
            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            double longitude = location.getLongitude();
            double latitude = location.getLatitude();

            Building closestBuilding = findClosestBuilding(buildingList, longitude, latitude);
            int index = findBuildingIndex(buildingList, closestBuilding);
            if(index != -1) Collections.swap(buildingList, index, 0);
        }

        ListView listView = (ListView) findViewById(R.id.building_list);
        BuildingsAdapter adapter = new BuildingsAdapter(this, R.layout.starting_listview, buildingList);
        listView.setAdapter(adapter);

        //Starts the MainActivity with building info if the user presses one of the buildings.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                Building item = (Building) parent.getItemAtPosition(position);
                Gson gson = new Gson();
                String json = gson.toJson(item);
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra("buildingInfo",json);
                startActivity(intent);

            }
        });
    }

    /**
     * Finds the closest building to the user.
     * @param bList the building list
     * @param longtitude user's longtitude
     * @param latitude user's latitude
     * @return the closest building
     */

    public Building findClosestBuilding(List<Building> bList, double longtitude, double latitude) {
        if(!bList.isEmpty()) {
            Building closestB = bList.get(0);
            double closestDistance = findDistance(closestB, longtitude, latitude);

            for(int i = 1; i < bList.size(); i++) {
                Building compare = bList.get(i);
                double newDistance = findDistance(compare, longtitude, latitude);
                if(newDistance < closestDistance) {
                    closestDistance = newDistance;
                    closestB = compare;
                }
            }
            return closestB;
        }
        return null;
    }

    /**
     * Finds the distance between the user and a building.
     * @param b the building
     * @param longitude user's longtitude
     * @param latitude user's latitude
     * @return the distance
     */

    public double findDistance(Building b, double longitude, double latitude) {
        double longDif = (double) b.location.get("lng") - longitude;
        double longDifPower2 = Math.pow(longDif, 2);
        double latDif = (double) b.location.get("lat") - latitude;
        double latDifPower2 = Math.pow(latDif, 2);

        double distance = Math.sqrt(longDifPower2 + latDifPower2);
        return distance;
    }

    /**
     * Checks if the user has internet connection.
     * @return true if they have, false if not
     */

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    /**
     * Inits a network connection listener. This requests buildings when the user has connected to network.
     */

    public void initConnectionListener() {
        networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(isNetworkAvailable()) {
                    chooseLocation.setVisibility(View.VISIBLE);
                    sliderLO.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                    requestBuildings();
                } else {
                    sliderLO.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
            }
        };

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);
    }

    /**
     * Inits the "no connection" view.
     */

    public void initSlider() {
        sliderLO = (SlidingUpPanelLayout) findViewById(R.id.no_connection);
        sliderLO.setTouchEnabled(false);
        sliderLO.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
    }

    /**
     * Inits the retry textButton in "no internet connection" view.
     * When clicked, the "no connection" view goes down, requests buildings if there is now connection and
     * if not, waits 700ms and puts the "no connection" view back up.
     */

    public void initRetry() {
        TextView retry = (TextView)findViewById(R.id.retry_button);
        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sliderLO.setPanelState(SlidingUpPanelLayout.PanelState.HIDDEN);
                if(isNetworkAvailable()) {
                    chooseLocation.setVisibility(View.VISIBLE);
                    requestBuildings();
                }
                else {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sliderLO.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                        }
                    }, 700);
                }
            }
        });
    }

    /**
     * Returns the index of the building if it is in the list given as a parameter (otherwise -1)
     * @param bList list of buildings
     * @param building the building needed to be found
     * @return the index of the building
     */

    public int findBuildingIndex(List<Building> bList, Building building) {
        for(int i = 0; i < bList.size(); i++) {
            if(bList.get(i).buildingID == (building.buildingID)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Request all the buildings and initializes the building list.
     */

    public void requestBuildings(){
        pBar.setVisibility(View.VISIBLE);
        server.process(new BuildingsRequest(), new ServerInterface.ServerCallback<Building[]>() {
            @Override
            public void onResult(Building[] response) {
                if (response == null || response.length == 0) {
                    Toast.makeText(getApplicationContext(),"Unable to find any buildings.",Toast.LENGTH_SHORT);
                    pBar.setVisibility(View.GONE);
                    return;
                }
                buildingList = Arrays.asList(response);
                initList();
                pBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroy() {
        if(networkStateReceiver != null) unregisterReceiver(networkStateReceiver);

        super.onDestroy();
    }


    /**
     * Just call requestBuildings-method when the user has chosen whether or not they want the app
     * to access user's coarse location.
     * @param requestCode
     * @param permissions
     * @param grantResults
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSION_ACCESS_COURSE_LOCATION) {
            requestBuildings();
        }
    }

}
