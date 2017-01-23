package fi.aalto.tshalaa1.inav;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import fi.aalto.tshalaa1.inav.entities.Building;

/**
 * Created by tshalaa1 on 7/22/16.
 */
public class BuildingsAdapter extends ArrayAdapter<Building> {

    Context context;
    int layoutResourceId;
    List<Building> data = null;

    public BuildingsAdapter(Context context, int layoutResourceId, List<Building> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        BuildingHolder holder = null;

        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new BuildingHolder();
            holder.txtTitle = (TextView) row.findViewById(R.id.building_label);

            row.setTag(holder);
            System.out.println("holder1: " + holder.txtTitle.getText());
        } else {
            holder = (BuildingHolder)row.getTag();
            System.out.println("holder: " + holder.txtTitle.getText());
        }

        if(data != null) {
            Building build = data.get(position);
            holder.txtTitle.setText(build.name);
        }


        return row;
    }

    static class BuildingHolder {
        ImageButton favMenu;
        TextView txtTitle;
    }

}
