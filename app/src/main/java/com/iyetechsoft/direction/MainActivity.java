package com.iyetechsoft.direction;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements LocationListener {
    private static final int PERMISSION_CODE = 100;
    private static final String DIRECTION_API_KEY = "5b3ce3597851110001cf62488a3c4b8bc8d4442481bd61fd654ce8e4";
    private static final String serverURL = "https://api.openrouteservice.org/v2/directions";
    private static final String[] modes = {"driving-car", "driving-hgv", "cycling-regular", "cycling-road", "cycling-mountain", "cycling-electric", "foot-walking", "foot-hiking", "wheelchair"};
    LocationManager locationManager;
    private boolean gpsEnabled = false;
    private String modeOfTransport;
    private String[] destinationLatLong = new String[2];
    private TextView latLong, direction;
    private EditText destLatLong;
    private double currentLatitude, currentLongitude;
    private Spinner modesSpinner;
    private FloatingActionButton currentLocation, showDirection;


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // checking if gps module is available or not
        if (hasGPS()) {
            Toast.makeText(MainActivity.this, "GPS Module found", Toast.LENGTH_SHORT).show();

            latLong = findViewById(R.id.latLong);
            direction = findViewById(R.id.direction);
            destLatLong = findViewById(R.id.destLatLong);
            showDirection = findViewById(R.id.getDirection);
            modesSpinner = findViewById(R.id.modesSpinner);
            currentLocation = findViewById(R.id.current_location);

            ArrayAdapter ad = new ArrayAdapter(this, android.R.layout.simple_spinner_item, modes);
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            modesSpinner.setAdapter(ad);
            modesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    destLatLong.clearFocus();
                    modeOfTransport = modes[position];
                    if (!destLatLong.getText().toString().isEmpty() && destLatLong.getText().toString().contains(":")) {
                        destinationLatLong = destLatLong.getText().toString().split(":");
                        getDirection();
                    } else {
                        Toast.makeText(getApplicationContext(), "Please enter destination coordinates", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    modeOfTransport = modes[0];
                    Toast.makeText(getApplicationContext(), "Modes not selected", Toast.LENGTH_SHORT).show();
                }
            });

            currentLocation.setOnClickListener(v -> {
                latLong.setText("Fetching...");
                getLocation();
            });

            showDirection.setOnClickListener(v -> {
                destLatLong.clearFocus();
                if (!destLatLong.getText().toString().isEmpty() && destLatLong.getText().toString().contains(":")) {
                    destinationLatLong = destLatLong.getText().toString().split(":");
                    getDirection();
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter destination coordinates", Toast.LENGTH_SHORT).show();
                }
            });
        } else
            Toast.makeText(MainActivity.this, "No GPS Module.", Toast.LENGTH_SHORT).show();
    }

    public void getDirection() {
        Direction dir = new Direction();
        if (currentLatitude > 0 && currentLongitude > 0) {
            dir.execute();
        } else
            Toast.makeText(MainActivity.this, "No current Location found\nClick on location button below direction button", Toast.LENGTH_SHORT).show();
    }


    // switch on the gps service
    private void switchOnGPS() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enable GPS to use this feature")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("No", (dialog, which) -> dialog.cancel());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // responsible for retrieving current location using gps
    public void getLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_CODE);
        } else {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }
    }

    public boolean hasGPS() {
        return getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (location != null) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            currentLatitude = lat;
            currentLongitude = lon;
            latLong.setText("Latitude: " + currentLatitude + "\n" + "Longitude: " + currentLongitude);

        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Toast.makeText(this, "GPS Disabled ", Toast.LENGTH_SHORT).show();
        gpsEnabled = false;
        switchOnGPS();
        currentLongitude = 0;
        currentLatitude = 0;
        latLong.setText("Latitude: " + currentLatitude + "\n" + "Longitude: " + currentLongitude);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Toast.makeText(this, "GPS Enabled ", Toast.LENGTH_SHORT).show();

    }


    public void getSteps(JSONObject jsonObject, String targetKey) {
        try {
            if (!jsonObject.has(targetKey)) {
                Iterator<?> keys = jsonObject.keys();
                String nextKeys;
                while (keys.hasNext()) {
                    nextKeys = (String) keys.next();
                    //Log.i("keys", nextKeys);

                    if (jsonObject.get(nextKeys) instanceof JSONObject) {
                        if (!jsonObject.has(targetKey))
                            getSteps(jsonObject.getJSONObject(nextKeys), targetKey);

                    } else if (jsonObject.get(nextKeys) instanceof JSONArray) {
                        JSONArray array = jsonObject.getJSONArray(nextKeys);
                        for (int i = 0; i < array.length(); i++) {
                            String arrayString = array.get(i).toString();

                            try {
                                JSONObject nestedJSON = new JSONObject(arrayString);
                                if (!jsonObject.has(targetKey))
                                    getSteps(nestedJSON, targetKey);

                            } catch (JSONException e) {
                                continue;
                            }
                        }
                    }
                }
            } else {
                String output = jsonObject.get(targetKey).toString();
                String steps = new JSONArray(output).toString(2);
                arrangeDirection(steps);
            }
        } catch (JSONException e) {
            direction.setText("GetStepsException: " + e.getMessage());
        }
    }

    public void arrangeDirection(String steps) {
        try {
            JSONArray array = new JSONArray(steps);
            StringBuffer step = new StringBuffer();
            for (int i = 0; i < array.length(); i++) {
                step.append("\t\t" + new JSONObject(array.get(i).toString()).get("instruction") + "\n");
                step.append("\t\t" + new JSONObject(array.get(i).toString()).get("distance") + " m\n\t\t\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n");
                direction.setText(step);
            }
        } catch (JSONException e) {
            Log.i("ArrangeDirectionExcep", e.getMessage());
        }
    }

    class Direction extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            direction.setText("\t\tPlease wait, While fetching direction data...");
        }

        @Override
        protected String doInBackground(String... strings) {
            String result = null;
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.connectTimeout(1, TimeUnit.MINUTES).writeTimeout(1, TimeUnit.MINUTES).readTimeout(1, TimeUnit.MINUTES);    // socket timeout
            OkHttpClient client = builder.build();
            Request request = new Request.Builder()
                    .url(serverURL + "/" + modeOfTransport + "?api_key=" + DIRECTION_API_KEY + "&start=" + currentLongitude + "," + currentLatitude + "&end=" + Double.parseDouble(destinationLatLong[0]) + "," + Double.parseDouble(destinationLatLong[1]))
                    .build();


            try {
                Log.i("Request", request.toString());
                Response response = client.newCall(request).execute();
                result = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s != null) {
                try {
                    //Log.i("Response", s);
                    if (!s.contains("error")) {
                        JSONObject json = new JSONObject(s);
                        getSteps(json, "steps");
                    } else {
                        String steps = new JSONObject(new JSONObject(s).get("error").toString()).get("message").toString();
                        direction.setText(steps);
                    }
                } catch (JSONException e) {
                    Log.i("Parsing Exception", e.getMessage());
                }
            }
        }
    }
}