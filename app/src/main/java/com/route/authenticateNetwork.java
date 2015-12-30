package com.route;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class authenticateNetwork extends Activity {
    protected static WifiManager mainWifi = getNearbyNetworks.mainWifi;
    private String ssid;
    private String bssid;
    private String capabilities;
    private WifiConfiguration wfc;
    private EditText pswrdEditText;
    private TextView ssidName;
    private TextView statusMessage;
    private ProgressBar progressBar;
    private ImageView successImage;
    private Button validateButton;
    private WifiManager wifiManager;
    private ConnectivityChangeReceiver connChangeRec;
    public static Boolean handlerActive;
    public static Boolean networkAuthenticated;
    private Handler myHandler;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authenticate_network);
        handlerActive = false;
        networkAuthenticated = false;
        pswrdEditText = (EditText) findViewById(R.id.ssid_pswrd);
        ssidName = (TextView) findViewById(R.id.networkSSID);
        statusMessage = (TextView) findViewById(R.id.message_result);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        validateButton = (Button) findViewById(R.id.submit_pswrd);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        // Bind listener to disable authenticate button until field is populated
        if(pswrdEditText.length() == 0) {
            validateButton.setEnabled(false);
        }
        // create click listener on the validate button
        View.OnClickListener testConnectionSubmit = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                validateButton.setEnabled(false);
                pswrdEditText.setEnabled(false);
                // Verify that the wifi is disconnected before trying to authenticate
                wifiManager.disconnect();
                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (mWifi.isConnected()) {
                    statusMessage.setVisibility(View.VISIBLE);
                    statusMessage.setText("Disconnecting from active network...");
                    Handler disconnectNet = new Handler();
                    disconnectNet.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            testConnection();
                        }
                    },5000);
                }
                else {
                    testConnection();
                }
            }
        };
        validateButton.setOnClickListener(testConnectionSubmit);

        pswrdEditText.addTextChangedListener(textWatcher);

        // Enable wifi if disabled
        enableWifi();

        Intent myIntent = getIntent();
        if (myIntent.hasExtra(getNearbyNetworks.NETWORK)) {
            String scanResultParts = myIntent.getStringExtra(getNearbyNetworks.NETWORK);
            String[] splitParts = scanResultParts.split(",");
            ssid = splitParts[0];
            ssidName.setText(ssid);
            bssid = splitParts[1];
            capabilities = splitParts[2];
        }


    }
    protected void onPause() {
        try {
            unregisterReceiver(connChangeRec);
        } catch(IllegalArgumentException e) {
            Log.w(this.toString(), "Attempted to unregister a reciever but failed");
        }
        super.onPause();
    }
    protected void onResume() {
        registerReceiver(connChangeRec, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        // Enable wifi if disabled
        enableWifi();

        super.onResume();
    }

    private void testConnection() {
        wfc = null;
        statusMessage.setVisibility(View.INVISIBLE);
        generateWifiConf();
        // remove the network first, Connect to the network, Activate the waiting icon, and Disable the Button
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for(WifiConfiguration k : list)
        {
            Log.w(this.toString(), "SSID: " + k.SSID);
            if(k.SSID.equals("\"" + ssid + "\""))
            {
                wifiManager.removeNetwork(k.networkId);
                Log.w(this.toString(),"Removed SSID: " + ssid);
            }
        }
        try {
            wifiManager.saveConfiguration();
            int netId = wifiManager.addNetwork(wfc);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
            // Wait until the status of the wifi connection changes
            connChangeRec = new ConnectivityChangeReceiver();
            registerReceiver(connChangeRec, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            Log.i(this.toString(), "Created the receiver");

            // Use this to set max timeout to authenticate of 10 seconds
            myHandler = new Handler();
            myHandler.postDelayed(checkAuthenticated, 10000);

        }
        catch (Exception e) {
            Log.e(this.toString(), this.getLocalClassName() + "; " + e.toString());
        }
    }

    private void generateWifiConf() {
        try {
            wfc = new WifiConfiguration();
            EditText ssid_pswrd = (EditText)findViewById(R.id.ssid_pswrd);
            wfc.SSID = "\"".concat(ssid).concat("\"");
            wfc.status = WifiConfiguration.Status.DISABLED;
            wfc.priority = 999;

            wfc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            wfc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wfc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

            if (capabilities.contains("WPA2") || (capabilities.contains("WPA"))) {
                wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                wfc.preSharedKey = "\"".concat(ssid_pswrd.getText().toString()).concat("\"");
                Log.i(this.toString(), "Capability matched: WPA2 or WPA");
            }
            else if (capabilities.contains("WEP")) {
                wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wfc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);

                wfc.wepKeys[0] = "\"".concat(ssid_pswrd.getText().toString()).concat("\"");
                wfc.wepTxKeyIndex = 0;
                Log.i(this.toString(), "Capability matched: WEP");
            }
            else {
                wfc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wfc.allowedAuthAlgorithms.clear();
                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wfc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                Log.i(this.toString(), "Capability matched neither WPA2, WPA, or WEP");
            }
        }
        catch(NullPointerException e) {
            Log.e(this.toString(), this.getLocalClassName() + "; " + e.toString());
        }
        catch(Exception e) {
            Log.e(this.toString(), this.getLocalClassName() + "; " + e.toString());
        }
    }

    //TODO: This class is not using probable better practice of getAllNetworks due to API min 21 constraint
    public class ConnectivityChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                ConnectivityManager cm =
                        (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                Bundle extras = intent.getExtras();
                NetworkInfo networkInfo;

                Log.w("INTENT", "action: " + intent.getAction());
                Log.w("INTENT", "component: " + intent.getComponent());
                if (extras != null) {
                    for (String key: extras.keySet()) {
                        if (key.toString().toLowerCase().contains("networkinfo")) {
                           //networkInfo = (NetworkInfo) extras.get(key);
                            String compareString = extras.get(key).toString().toLowerCase();
                            Log.w("networkInfo", "Comparing with: " + compareString);
                            //if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                            if (compareString.contains("type: wifi")) {
                                //if (networkInfo.getDetailedState() == NetworkInfo.DetailedState.FAILED) {
                                if (compareString.contains("state: failed")) {
                                    Log.w("Inetify","WIFI failed to authenticate");
                                    authenticationFailed();
                                }
                                //else if(networkInfo.getDetailedState() == NetworkInfo.DetailedState.AUTHENTICATING ||
                                //        networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTING) {
                                else if(compareString.contains("state: authenticating") ||
                                        compareString.contains("state: connecting")) {
                                    Log.i("Inetify","WIFI is authenticating");
                                }
                                //else if ((networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) && networkInfo.isConnected())  {
                                else if (compareString.contains("state: connected"))  {
                                    Log.i("Inetify","WIFI is connected and validated");
                                    networkAuthenticated = true;
                                    authenticationSuccess();
                                }
                           }
                       }
                    }
                } else Log.w("INTENT", "no extras");
                //if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {

                /*
                }
                else {
                    Log.w("Inetify", "Connection change not for WIFI");
                } */
            }
            else {
                Log.w(this.toString(), "Wrong Intent type, got: " + intent.getAction());
            }
        }
    }

    // Enable and Disable button depending on password textfield
    private TextWatcher textWatcher = new TextWatcher() {
        public void afterTextChanged(Editable s) {
            if(pswrdEditText.length() == 0) {
                //disable send button if no text entered
                validateButton.setEnabled(false);
            }
            else {
                validateButton.setEnabled(true);
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
        public void onTextChanged(CharSequence s, int start, int before,
                                  int count) {
        }
    };

   private Runnable checkAuthenticated = new Runnable() {
        public void run() {
            Boolean networkAuthenticated = hasNetworkAuthenticated();
            if (networkAuthenticated != null && !networkAuthenticated) {
                authenticationFailed();
            }
        }
    };

    // Unregister receiver, Reset password textField, hide the spinner,
    // make sure that the button is enabled, Set the message to visible and error type
    private void authenticationFailed() {
        // Double check that wifi is not connected"
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (mWifi.isConnected()) {
            authenticationSuccess();
        }
        Log.w(this.toString(), this.getLocalClassName() + " ; Failed to authenticate");
        try {
            unregisterReceiver(connChangeRec);
        } catch(IllegalArgumentException e) {
            Log.w(this.toString(), "Attempted to unregister a receiver but failed");
        }
        progressBar.setVisibility(View.INVISIBLE);
        pswrdEditText.setText("");
        pswrdEditText.setEnabled(true);
        statusMessage.setText("Timed out to authenticate.\nVerify the password entered.");
        statusMessage.setTextColor(Color.RED);
        statusMessage.setVisibility(View.VISIBLE);
        if(validateButton.isEnabled()) {
            validateButton.setEnabled(false);
        }
    }

    //TODO: Pass the network profile to DB, encryption should be H(SSID | BBSID) for network name, password H(password | uniqueUserPin)
    // Unregister receiver, hide the spinner, set the message to success type
    // Cancel the handler
    private void authenticationSuccess() {
        Log.w(this.toString(), this.getLocalClassName() + " ; Success to authenticate");
        myHandler.removeCallbacks(checkAuthenticated);
        try {
            unregisterReceiver(connChangeRec);
        } catch(IllegalArgumentException e) {
            Log.w(this.toString(), "Attempted to unregister a receiver but failed");
        }
        successImage = (ImageView) findViewById(R.id.successAuthImage);
        successImage.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        statusMessage.setText("Successfully authenticated!\nReturning to your home page...");
        statusMessage.setTextColor(Color.BLACK);
        statusMessage.setVisibility(View.VISIBLE);
    }

    private Boolean hasNetworkAuthenticated() {
        return this.networkAuthenticated;
    }

    private void enableWifi() {
// Enable wifi if disabled
        if (!wifiManager.isWifiEnabled()) {
            // If wifi disabled then enable it
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled",
                    Toast.LENGTH_LONG).show();

            wifiManager.setWifiEnabled(true);
        }
    }

}
