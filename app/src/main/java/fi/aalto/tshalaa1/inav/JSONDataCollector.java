package fi.aalto.tshalaa1.inav;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fi.aalto.tshalaa1.inav.entities.DataPoint;
import fi.aalto.tshalaa1.inav.entities.SensorDataPoint;
import fi.aalto.tshalaa1.inav.entities.SensorRunnable;
import fi.aalto.tshalaa1.inav.entities.WifiDataPoint;

/**
 * Created by Emil on 5.1.2015.
 *
 * Class for collecting one (1) data point of JSON data, containing a specified set of data from various sensors on the phone.
 * The JSONDataCollector can collect data such as: accelerometer, gyroscope, magnetic field sensor, wifi fingerprint, cellid fingerprint.
 *
 * The JSONDataColletor is abstract: any use of it requires defining a callback method onSuccessCallback() in which can be specified what happens with the collected data.
 *
 * Example use: (notice how the instance of JSONDataCollector isn't saved. We just instantiate a new collector, that collects data and runs the callback.
 *
 * new JSONDataCollector(true, false, true, true, true) {
        @Override
        void onSuccessCallback(JSONObject data) {
            lastJSON = data.toString();
            navigator.setStart(bs, lastJSON);
            Toast.makeText(Utils.getContext(), "JSON received!", Toast.LENGTH_SHORT).show();
        }
 * };
 *
 *
 * In the current version of JSONDataCollector, Cell and Wifi methods are implemented in the class itself.
 * The sensordata is handled by a separate SensorDataHandler, since sensor data is used by other parts of the application,
 * and Wifi and Cell info is only needed by this class to collect single data points for each location request.
 */
public abstract class JSONDataCollector {
    private static final String TAG = "JSONDataCollector";
    DataPoint accData, gyroData, magnData, cellData;
    SensorRunnable accRunnable = new SensorRunnable() {
        @Override
        public void run() {
            accData = new SensorDataPoint(this.x,this.y,this.z,this.interval);

            checkFinished();
        }
    };
    ArrayList<DataPoint> wifiData;
    Thread t;
    SensorRunnable gyroRunnable = new SensorRunnable() {
        @Override
        public void run() {
            gyroData = new SensorDataPoint(this.x,this.y,this.z,this.interval);

            checkFinished();
        }
    };
    SensorRunnable magnRunnable = new SensorRunnable() {
        @Override
        public void run() {
            magnData = new SensorDataPoint(this.x,this.y,this.z,this.interval);

            checkFinished();
        }
    };
    WifiManager wifiManager;
    private SensorDataHandler sensorDataHandler = new SensorDataHandler();
    private boolean collectAcc = false;
    private boolean collectGyro = false;
    private boolean collectMagn = false;
    private boolean collectWifi = false;
    private boolean collectCell = false;
    Runnable startCollectingRunnable = new Runnable() {
        @Override
        public void run() {
            if (collectAcc || collectGyro || collectMagn) {

                sensorDataHandler.setAccRunnable(accRunnable);
                sensorDataHandler.setGyroRunnable(gyroRunnable);
                sensorDataHandler.setMagnRunnable(magnRunnable);
                //TODO: ADD ACCELEROMETER RUNNABLE
                sensorDataHandler.start(collectAcc, collectGyro, collectMagn, false);
            }

            if (collectWifi)
                scanWifiNow();
        }
    };
    /** This receiver will fire every time the system fires the SCAN_RESULTS_AVAILABLE action. Notice that nothing will fire unless we start the scan ourselves */
    private BroadcastReceiver wifiBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                handleWiFiScanResult();
            }

        }};
    private JSONObject responseObject;
    Runnable callbackRunnable = new Runnable() {
        @Override
        public void run() {
            onSuccessCallback(responseObject);
        }
    };

    public JSONDataCollector(boolean collectAcc, boolean collectGyro, boolean collectMagn, boolean collectWifi, boolean collectCell) {
        this.collectAcc = collectAcc;
        this.collectGyro = collectGyro;
        this.collectCell = collectCell;
        this.collectMagn = collectMagn;
        this.collectWifi = collectWifi;

        t = new Thread(startCollectingRunnable);
        t.start();
    }

    abstract void onSuccessCallback(JSONObject data);


    /**
     * METHOD FOR SCANNING WIFI NETWORKS.
     */
    private void scanWifiNow() {
        wifiManager = (WifiManager) Utils.getContext().getSystemService(Context.WIFI_SERVICE);

        /** Tell the system to fire our wifiBroadcastReceiver whenever there is wifi scan results available, or when the network changes state. */

        Utils.getContext().registerReceiver(wifiBroadcastReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        Utils.getContext().registerReceiver(wifiBroadcastReceiver, new IntentFilter(
                WifiManager.NETWORK_STATE_CHANGED_ACTION));

        if (wifiManager.isWifiEnabled() && wifiManager.startScan()) {
            Log.d(TAG, "starting wifi scan.");
        }
    }

    public void unregisterReceiverWifi(){
//        if(wifiBroadcastReceiver != null)
           try {
               Utils.getContext().unregisterReceiver(wifiBroadcastReceiver);
           } catch (IllegalArgumentException e) {
               Log.e(TAG, "receiver not reg");
           }


    }

    /** Whenever a new set of datapoints are available, this method is to be called for storing them in a list */
    private void handleWiFiScanResult() {

        // list scan result
        List<ScanResult> list = wifiManager.getScanResults();
        Log.d(TAG, "wifi scan results" + Arrays.toString(list.toArray()));

        StringBuffer scanList = new StringBuffer();

        /** Store the approximate time when this scan was done. This time will be stored in the data file produced */
        long timestamp = System.currentTimeMillis();

        ArrayList<DataPoint> tempData = new ArrayList();

        if (list != null && !list.isEmpty()) {
            for (int i = list.size() - 1; i >= 0; i--) {
                final ScanResult scan = list.get(i);

                if (scan == null)
                    continue;

                tempData.add(new WifiDataPoint(timestamp,scan.SSID.toLowerCase().replace(' ', '_'),scan.BSSID,scan.level));
            }
        }

        wifiData = tempData;

        checkFinished();
    }


    /**
     * When each sensor (acc, gyro, magn, wifi, cell) has collected the data it needs, they will all separately run this method.
     * The main if statement in checkFinished() is only passed when all sensors have collected their data.
     *
     * When all sensors have finished (wifi usually slowest!), this method will bundle up the data in a nice JSON object and run the callback method.
     */
    private void checkFinished() {
        if (((collectWifi && wifiData != null) || !collectWifi) &&
                ((collectAcc && accData != null) || !collectAcc) &&
                ((collectGyro && gyroData != null) || !collectGyro) &&
                ((collectMagn && magnData != null) || !collectMagn) &&
                ((collectCell && cellData != null) || !collectCell)) {

            sensorDataHandler.stop();

            if (collectWifi)
                Utils.getContext().unregisterReceiver(wifiBroadcastReceiver);

            //TODO: set the response object to something useful!!

            responseObject = new JSONObject();

            try {
                if (collectAcc)
                    responseObject.put("acc", accData.getJSONObject("find").getJSONArray("find"));

                if (collectGyro)
                    responseObject.put("gyro", gyroData.getJSONObject("find").getJSONArray("find"));

                if (collectMagn)
                    responseObject.put("magn", magnData.getJSONObject("find").getJSONArray("find"));

                if (collectWifi) {

                    JSONArray wifiArray = new JSONArray();
                    for (DataPoint w : wifiData) {
                        wifiArray.put(w.getJSONObject(""));
                    }

                    responseObject.put("wifi",wifiArray);
                }

                if (collectCell) {
                    responseObject.put("cell",cellData.getJSONObject(""));
                }

            } catch (JSONException e) {
                //nice catch
            }


            Utils.runOnUiThread(callbackRunnable);
        }

    }


}
