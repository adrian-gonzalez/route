package com.route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class getNearbyNetworks extends ListActivity {
    public final static String NETWORK = "com.route.adrian.route.authenticateNetwork";
    protected static WifiManager mainWifi;
    private TextView scanningText;
    private ProgressBar scanningPBar;
    private WifiReceiver receiverWifi;
    private List<ScanResult> wifiList;
    private scanResultArrayAdapter adapter;
    private HashMap<String, Integer> signalStrength = new HashMap<String, Integer>();
    private ArrayList<ScanResult> mScanResults = new ArrayList<ScanResult>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.get_nearby_networks);

        // Enable Wifi if it is disabled
        mainWifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (mainWifi.isWifiEnabled() == false)
        {
            // If wifi disabled then enable it
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled",
                    Toast.LENGTH_LONG).show();

            mainWifi.setWifiEnabled(true);
        }
        scanningText = (TextView) findViewById(R.id.scanning_text);
        scanningPBar = (ProgressBar) findViewById(R.id.scan_progress_bar);
        scanningText.setVisibility(View.VISIBLE);
        scanningPBar.setVisibility(View.VISIBLE);

        //Instantiate the receiver and the adapter
        receiverWifi = new WifiReceiver();
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWifi.startScan();
        adapter = new scanResultArrayAdapter(this, R.layout.scan_result_item, mScanResults);
        setListAdapter(adapter);

        // Listen to which network is selected and unregister the receiver
        try
        {
            // ListView on item selected listener.
            getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    try {
                        unregisterReceiver(receiverWifi);
                    } catch(IllegalArgumentException e) {
                        Log.w(this.toString(), "Attempted to unregister a reciever but failed");
                    }

                    // Pass the network selected to the "authenticateNetwork" activity
                    sendNetwork(mScanResults.get(position));
                }
            });
        }
        catch (Exception e)
        {
            Log.w(this.toString(), e.toString());
        }
    }

    protected void onPause() {
        try {
            unregisterReceiver(receiverWifi);
        } catch(IllegalArgumentException e) {
            Log.w(this.toString(), "Attempted to unregister a reciever but failed");
        }
        mScanResults.clear();
        signalStrength.clear();
        adapter.clear();
        adapter.notifyDataSetChanged();
        scanningText.setVisibility(View.INVISIBLE);
        scanningPBar.setVisibility(View.GONE);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(receiverWifi, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        scanningText.setVisibility(View.VISIBLE);
        scanningPBar.setVisibility(View.VISIBLE);
        mainWifi.startScan();
        super.onResume();
    }

    // Broadcast receiver class called its receive method
    // when number of wifi connections changed
    class WifiReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            scanningText.setVisibility(View.INVISIBLE);
            scanningPBar.setVisibility(View.GONE);
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Log.v(this.toString(), "The onReceive action ran run code block");
                wifiList = mainWifi.getScanResults();

                // Add the network if the SSID and BSSID of a network isn't already listed
                if (wifiList != null) {
                    try {
                        for(int i = 0; i < wifiList.size(); i++){
                            ScanResult scanresult = wifiList.get(i);
                            String ssid = scanresult.SSID;
                            if (!ssid.isEmpty()) {
                                String key = scanresult.SSID + " "
                                        + scanresult.BSSID;
                                if (!signalStrength.containsKey(key)) {
                                    Log.i(this.toString(), "Added - ssid:" + scanresult.SSID + "bssid:"
                                            + scanresult.BSSID);
                                    signalStrength.put(key, i);
                                    mScanResults.add(scanresult);
                                    adapter.notifyDataSetChanged();

                                } else {
                                    int position = signalStrength.get(key);
                                    ScanResult updateItem = mScanResults.get(position);
                                    if (calculateSignalStrength(mainWifi, updateItem.level) !=
                                            calculateSignalStrength(mainWifi, scanresult.level)) {
                                        Log.i(this.toString(), "Changed - ssid:" + scanresult.SSID + "bssid:"
                                                + scanresult.BSSID);
                                        mScanResults.set(position, updateItem);
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                     Log.w(this.toString(), e.toString());
                    }

                }
                else {
                    Log.v(this.toString(), "No networks found within range");
                }
            }
        }
    }

    public static int calculateSignalStrength(WifiManager wifiManager, int level){
        return wifiManager.calculateSignalLevel(level, 5) + 1;
    }

    public void sendNetwork(ScanResult sr) {
        mScanResults.clear();
        signalStrength.clear();

        Intent intent = new Intent(this, authenticateNetwork.class);
        intent.putExtra(NETWORK, sr.SSID + "," + sr.BSSID + "," + sr.capabilities);
        startActivity(intent);
    }

}