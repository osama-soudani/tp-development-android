package com.example.mycurrentlocation;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;

import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;


import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements LocationListener {

    TextView textView_location;
    TextView textView_signal;
    TextView textView_battery;
    LocationManager locationManager;
    File logFile;
    FileOutputStream fileOutputStream;
    TelephonyManager telephonyManager;
    SignalStrengthListener signalStrengthListener;
    BatteryReceiver batteryReceiver;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        textView_location = findViewById(R.id.text_location);
        textView_signal = findViewById(R.id.text_signal);
        textView_battery = findViewById(R.id.text_battery);
        try {
            File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "data");

            if (!dir.exists()) {
                boolean dirCreated = dir.mkdirs();
                Log.d("Directory Creation", "Directory created: " + dirCreated);
            }

            logFile = new File(dir, "data_log.txt");
            Log.d("File Path", "Log file path: " + logFile.getAbsolutePath());
            fileOutputStream = new FileOutputStream(logFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        signalStrengthListener = new SignalStrengthListener();
        telephonyManager.listen(signalStrengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);


        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        batteryReceiver = new BatteryReceiver();
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED){


            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE
            }, 100);
            return;
        }


        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                1,
                this
        );

        // Handle insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close(); // Ensure the file output stream is closed properly
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        String locationText = "X: " + location.getLatitude() + "-   Y: " + location.getLongitude();
        textView_location.setText(locationText);
        logToFile("Location Update: " + locationText);
        Log.d("LogToFIleLocation","location sent");
    }

    private void logToFile(String data) {
        try {
            if (fileOutputStream != null) {
                OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
                writer.write(data + "\n");
                writer.flush(); // Ensure data is written immediately
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        LocationListener.super.onStatusChanged(provider, status, extras);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            1000,
                            2,
                            this
                    );
                }
            }
        }
    }

    private class SignalStrengthListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            // Extract signal strength
            int strength = signalStrength.getLevel();
            String signalText = "Signal Strength: " + strength + "/4";
            textView_signal.setText(signalText);

            logToFile(signalText);
        }
    }


    private class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPercentage = (level / (float) scale) * 100;

            String batteryText = "Battery Level: " + Math.round(batteryPercentage) + "%";
            textView_battery.setText(batteryText);

            logToFile(batteryText);
        }
    }
}
