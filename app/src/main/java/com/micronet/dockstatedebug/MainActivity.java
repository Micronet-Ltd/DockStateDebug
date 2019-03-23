package com.micronet.dockstatedebug;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;
import micronet.hardware.MicronetHardware;
import micronet.hardware.exception.MicronetHardwareException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DockStateDebug";

    private Handler handler;
    private AtomicInteger currentInternalDockState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Dock State Debugging");
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        handler = new Handler();
        currentInternalDockState = new AtomicInteger();
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.postDelayed(uiUpdate, 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private synchronized String getInternalDockState(){
        StringBuilder sb = new StringBuilder();

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/sys/class/switch/dock/state"));

            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) {
                sb.append(inputLine);
            }

            if(!sb.toString().equalsIgnoreCase("")) {
                int dockState = Integer.parseInt(sb.toString());

                if(dockState != currentInternalDockState.get()){
                    int oldDockState = currentInternalDockState.get();
                    currentInternalDockState.set(dockState);
                    Log.i(TAG, "Internal dock state changed from " + oldDockState + " to " + dockState);
                }

                switch (dockState) {
                    case 0:
                        sb.setLength(0);
                        sb.append(dockState + " - Undocked");
                        break;
                    case 1:
                        sb.setLength(0);
                        sb.append(dockState + " - Basic Cradle, Docked Ign Low");
                        break;
                    case 3:
                        sb.setLength(0);
                        sb.append(dockState + " - Basic Cradle, Docked Ign High");
                        break;
                    case 5:
                        sb.setLength(0);
                        sb.append(dockState + " - Smart Cradle, Docked Ign Low");
                        break;
                    case 7:
                        sb.setLength(0);
                        sb.append(dockState + " - Smart Cradle, Docked Ign High");
                        break;
                    case 8:
                        sb.setLength(0);
                        sb.append(dockState + " - Smarthub, Docked Ign Low");
                        break;
                    case 10:
                        sb.setLength(0);
                        sb.append(dockState + " - Smarthub, Docked Ign High");
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }

        return sb.toString();
    }

    private synchronized String getDockState(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_DOCK_EVENT);
        Intent dockStatus = this.registerReceiver(null, ifilter);

        if(dockStatus != null){
            int dockState = dockStatus.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);

            return String.valueOf(dockState);
        }else{
            return "";
        }
    }

    private synchronized String getChargingState(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);

        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        if(status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL){
            return "Charging";
        }else if(status == -1) {
            return "";
        }else{
            return "Not Charging";
        }
    }

    private Runnable uiUpdate = new Runnable() {
        @Override
        public void run() {
            String dockState = getDockState();
            String internalDockState = getInternalDockState();
            String chargingState = getChargingState();

//            Log.d(TAG, "Updating the dock state to " + dockState + " and internal dock state to " + internalDockState);

            updateDockState(dockState);
            updateInternalDockState(internalDockState);
            updateChargingState(chargingState);

            handler.postDelayed(uiUpdate, 1000);
        }
    };

    private void updateDockState(final String dockState){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = findViewById(R.id.dockState);
                textView.setText(dockState);
            }
        });
    }

    private void updateInternalDockState(final String dockState){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = findViewById(R.id.internalDockState);
                textView.setText(dockState);
            }
        });
    }

    private void updateChargingState(final String chargingState){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = findViewById(R.id.chargingState);
                textView.setText(chargingState);
            }
        });
    }
}
