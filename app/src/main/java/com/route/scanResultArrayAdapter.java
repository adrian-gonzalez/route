package com.route;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.net.wifi.ScanResult;

import static com.route.getNearbyNetworks.calculateSignalStrength;

/**
 * Created by Adrian on 12/27/2015.
 */

public class scanResultArrayAdapter extends ArrayAdapter<ScanResult> {

    // declaring our ArrayList of items
    private ArrayList<ScanResult> objects;

    /* here we must override the constructor for ArrayAdapter
    * the only variable we care about now is ArrayList<Item> objects,
    * because it is the list of objects we want to display.
    */
    public scanResultArrayAdapter(Context context, int textViewResourceId, ArrayList<ScanResult> objects) {
        super(context, textViewResourceId, objects);
        this.objects = objects;
    }

    /*
     * we are overriding the getView method here - this is what defines how each
     * list item will look.
     */
    public View getView(int position, View convertView, ViewGroup parent){

        // assign the view we are converting to a local variable
        View v = convertView;

        // first check to see if the view is null. if so, we have to inflate it.
        // to inflate it basically means to render, or show, the view.
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.scan_result_item, null);
        }

		/*
		 * Recall that the variable position is sent in as an argument to this method.
		 * The variable simply refers to the position of the current object in the list. (The ArrayAdapter
		 * iterates through the list we sent it)
		 *
		 * Therefore, i refers to the current Item object.
		 */
        ScanResult i = objects.get(position);

        if (i != null) {

            // This is how you obtain a reference to the TextViews.
            // These TextViews are created in the XML files we defined.

            TextView ssid_name = (TextView) v.findViewById(R.id.toptext);
            TextView ssid = (TextView) v.findViewById(R.id.toptextdata);
            TextView sigstrength_name = (TextView) v.findViewById(R.id.middletext);
            TextView sigstrength = (TextView) v.findViewById(R.id.middletextdata);


            // check to see if each individual textview is null.
            // if not, assign some text!
            if (ssid_name != null){
                ssid_name.setText("SSID: ");
            }
            if (ssid != null){
                ssid.setText(i.SSID);
            }
            if (sigstrength_name != null){
                sigstrength_name.setText("Signal Strength: ");
            }
            if (sigstrength != null){
                int signalStrength = calculateSignalStrength(getNearbyNetworks.mainWifi, i.level);
                sigstrength.setText(Integer.toString(signalStrength));
            }
        }

        // the view must be returned to our activity
        return v;

    }

}
