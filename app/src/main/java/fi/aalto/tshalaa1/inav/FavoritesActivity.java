package fi.aalto.tshalaa1.inav;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import fi.aalto.tshalaa1.inav.entities.Landmark;

/**
 * Activity that shows the user all of their favorite landmarks and gives the possibility to remove them.
 */
public class FavoritesActivity extends AppCompatActivity {

    String prefsName;
    SharedPreferences favPrefs;
    ListAdapter adapter;
    ListView listview;
    List<Landmark> landmarklist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        prefsName = MainActivity.favPrefsName;
        favPrefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE);
        landmarklist = getLandMarkList(favPrefs);

        if(landmarklist.isEmpty()) {
            TextView noLandmarksText = (TextView) findViewById(R.id.no_landmarks);
            noLandmarksText.setText(R.string.no_landmarks);
        } else {
            listview = (ListView) findViewById(R.id.listview);

            adapter = new ListAdapter(this,
                    R.layout.favorite_list_item, landmarklist);
            listview.setAdapter(adapter);

            registerForContextMenu(listview);

            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, final View view,
                                        int position, long id) {
                    Landmark item = (Landmark) parent.getItemAtPosition(position);
                    Intent intent = new Intent();
                    Gson gson = new Gson();
                    String json = gson.toJson(item);
                    intent.putExtra("newlandmark",json);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });
        }
    }

    /**
     * Creates the context menu for the landmarks that opens up when the landmark is longpressed.
     * @param menu
     * @param v
     * @param menuInfo
     */

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fav_context_menu, menu);
    }

    /**
     * Determines what happens when the context menu items are pressed.
     * @param item the item pressed
     * @return true if selection was successful, otherwise false
     */

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.delete_item:
                Landmark lm = landmarklist.get(info.position);
                String lmTitle = lm.getNicerTitle();
                landmarklist.remove(lm);

                favPrefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = favPrefs.edit();
                String json = convertToJson(landmarklist);
                editor.putString("landmarks", json);
                editor.apply();

                adapter.notifyDataSetChanged();
                Toast.makeText(Utils.getContext(), "Deleted "+lmTitle, Toast.LENGTH_SHORT).show();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Converts a list of landmarks to json String.
     * @param landmarks the list of landmarks
     * @return the json String
     */

    public String convertToJson(List<Landmark> landmarks) {
        Gson gson = new Gson();
        return gson.toJson(landmarks);
    }

    /**
     * Fetches the favorite landmarklist from Shared Preferences
     * @param prefs the Shared Preferences
     * @return the favorite landmarklist
     */

    public List<Landmark> getLandMarkList(SharedPreferences prefs) {
        Gson gson = new Gson();
        String jsonPreferences = prefs.getString("landmarks", "");
        Type type = new TypeToken<List<Landmark>>() {}.getType();
        List<Landmark> productFromShared = gson.fromJson(jsonPreferences, type);
        if(productFromShared == null) return new ArrayList();
        return productFromShared;
    }

    // The following method is for options menu in the toolbar. There could be actions that the user
    // might want to perform to multiple favorite landmarks, like delete.

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fav_menu, menu);
        return true;
    }*/

    /**
     * Handles the toolbar selections, like back and options menu selections.
     * @param item the item pressed
     * @return true if selection was successful, otherwise false
     */

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
