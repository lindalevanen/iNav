package fi.aalto.tshalaa1.inav;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import fi.aalto.tshalaa1.inav.entities.Landmark;

/**
 * This ListAdapter class is for determining the view of an item in FavoritesActivity.
 */

public class ListAdapter extends ArrayAdapter<Landmark> {

    Context context;
    int layoutResourceId;
    List<Landmark> data = null;

    public ListAdapter(Context context, int layoutResourceId, List<Landmark> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        LandmarkHolder holder = null;

        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new LandmarkHolder();
            holder.txtTitle = (TextView) row.findViewById(R.id.fav_text);

            row.setTag(holder);

        } else {
            holder = (LandmarkHolder)row.getTag();
        }

        Landmark lm = data.get(position);
        holder.txtTitle.setText(lm.getNicerTitle());

        return row;
    }

    static class LandmarkHolder {
        ImageButton favMenu;
        TextView txtTitle;
    }

}