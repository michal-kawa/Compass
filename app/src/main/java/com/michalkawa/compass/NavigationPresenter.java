package com.michalkawa.compass;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.material.snackbar.Snackbar;

public class NavigationPresenter  {

    private static final String TAG = NavigationActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 50;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private final static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private final static String KEY_LOCATION = "location";
    private final static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

    private NavigationActivity navigationActivity;

    private SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorMagneticField;

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private Location target;

    private Boolean requestingLocationUpdates;
    private float[] floatGravityMatrix = new float[3];
    private float[] floatGeoMagneticMatrix = new float[3];
    private final float[] floatRotationMatrix = new float[9];
    private final float[] floatOrientationMatrix = new float[3];

    private float destinationLatitude;
    private float destinationLongitude;
    private boolean isTarget;

    public NavigationPresenter(Bundle savedInstanceState, NavigationActivity view) {
        requestingLocationUpdates = false;
        isTarget = false;
        navigationActivity = view;

        updateValuesFromBundle(savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(navigationActivity);
        settingsClient = LocationServices.getSettingsClient(navigationActivity);

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        if (!requestingLocationUpdates) {
            requestingLocationUpdates = true;
            startLocationUpdates();
        }

        sensorManager = (SensorManager) view.getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        SensorEventListener sensorEventListenerAccelrometer = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                floatGravityMatrix = event.values;

                SensorManager.getRotationMatrix(floatRotationMatrix, null, floatGravityMatrix, floatGeoMagneticMatrix);
                SensorManager.getOrientation(floatRotationMatrix, floatOrientationMatrix);

                navigationActivity.setCompassRotation((float) (-floatOrientationMatrix[0]*180/3.14159));

                if (currentLocation != null && isTarget) {

                    float azimuth = (float) Math.toDegrees(floatOrientationMatrix[0]);
                    float bearing = currentLocation.bearingTo(target);
                    float direction = azimuth - bearing;
                    navigationActivity.setDestinationArrow(-direction);
                    int distance = (int) currentLocation.distanceTo(target);
                    navigationActivity.setDestinationDistance(String.valueOf(distance));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        SensorEventListener sensorEventListenerMagneticField = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                floatGeoMagneticMatrix = event.values;

                SensorManager.getRotationMatrix(floatRotationMatrix, null, floatGravityMatrix, floatGeoMagneticMatrix);
                SensorManager.getOrientation(floatRotationMatrix, floatOrientationMatrix);

                navigationActivity.setCompassRotation((float) (-floatOrientationMatrix[0]*180/3.14159));

                if (currentLocation != null && isTarget) {
                    float azimuth = (float) Math.toDegrees(floatOrientationMatrix[0]);
                    float bearing = currentLocation.bearingTo(target);
                    float direction = azimuth - bearing;
                    navigationActivity.setDestinationArrow(-direction);
                    int distance = (int) currentLocation.distanceTo(target);
                    navigationActivity.setDestinationDistance(String.valueOf(distance));
                    Log.i("Distance", String.valueOf(distance));
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        sensorManager.registerListener(sensorEventListenerAccelrometer, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(sensorEventListenerMagneticField, sensorMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void setDestinationCoordinates(String settted_latitude, String setted_longitude) {
        isTarget = true;
        destinationLatitude = Float.parseFloat(settted_latitude);
        destinationLongitude = Float.parseFloat(setted_longitude);
        Log.i("Distance - latitude 2", String.valueOf(destinationLatitude));

        target = new Location("target");
        target.setLatitude(destinationLatitude);
        target.setLongitude(destinationLongitude);
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        switch (requestCode) {
//            case REQUEST_CHECK_SETTINGS:
//                if(requestCode == Activity.RESULT_CANCELED) {
//                    requestingLocationUpdates = false;
//                }
//        }
//    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                requestingLocationUpdates = savedInstanceState.getBoolean(
                        KEY_REQUESTING_LOCATION_UPDATES);
            }

            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                currentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            }
        }
    }

    public void startLocationUpdates() {

        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(navigationActivity, locationSettingsResponse -> {
                    Log.i(TAG, "All location settings are satisfied.");

                    //noinspection MissingPermission
                    fusedLocationClient.requestLocationUpdates(locationRequest,
                            locationCallback, Looper.myLooper());
                })
                .addOnFailureListener(navigationActivity, e -> {
                    int statusCode = ((ApiException) e).getStatusCode();
                    switch (statusCode) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                    "location settings ");
                            try {
                                ResolvableApiException rae = (ResolvableApiException) e;
                                rae.startResolutionForResult(navigationActivity, REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException sie) {
                                Log.i(TAG, "PendingIntent unable to execute request.");
                            }
                            break;

                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            String errorMessage = "Location settings are inadequate, and cannot be " +
                                    "fixed here. Fix in Settings.";
                            Log.e(TAG, errorMessage);
                            Toast.makeText(navigationActivity, errorMessage, Toast.LENGTH_LONG).show();
                            requestingLocationUpdates = false;
                    }
                });
    }

    public void stopLocationUpdates() {
        if (!requestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener(navigationActivity, task -> requestingLocationUpdates = false);
    }

    public void showSnackbar(final int mainTextStringId, final int actionStringId,
                             View.OnClickListener listener) {
        Snackbar.make(
                navigationActivity.findViewById(android.R.id.content),
                navigationActivity.getResources().getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(navigationActivity.getResources().getString(actionStringId), listener).show();
    }

    public boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(navigationActivity,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(navigationActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, view -> {
                        // Request permission
                        ActivityCompat.requestPermissions(navigationActivity,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                REQUEST_PERMISSIONS_REQUEST_CODE);
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            ActivityCompat.requestPermissions(navigationActivity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

}
