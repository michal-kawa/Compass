package com.michalkawa.compass;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.SettingsClient;

public class NavigationActivity extends AppCompatActivity {
    private static final String TAG = NavigationActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private static final int REQUEST_DESTINATION_VALUES = 0x2;
    private static String KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates";
    private static String KEY_LOCATION = "location";
    private static String KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string";

    public static final  String DIRECTION_EXTRA_VALUES_LATITUDE = "direction_latitude";
    public static final String DIRECTION_EXTRA_VALUES_LONGITUDE = "direction_longitude";

    private ImageView imageView;
    private ImageView destination_arrow;
    private TextView distanceToDestination;

    private Boolean requestingLocationUpdates;
    private String lastUpdateTime;

    private NavigationPresenter navigationPresenter;
    private SettingsClient settingsClient;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        navigationPresenter = new NavigationPresenter(savedInstanceState, this);

        distanceToDestination = findViewById(R.id.tv_distance_to_destination);

        requestingLocationUpdates = false;
        lastUpdateTime = "";

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        imageView = findViewById(R.id.iv_compass_face);
        destination_arrow = findViewById(R.id.iv_destination_arrow);
    }

    public void setCompassRotation(float rotation) {
        imageView.setRotation(rotation);
    }

    public void setDestinationArrow(float angle) {
        destination_arrow.setRotation(angle);
    }

    public void setDestinationDistance(String text) {
        distanceToDestination.setText("Distance from the destination: " + text + "m");
    }

    public void goToSetDirectionActivity(View view) {
        Intent myIntent = new Intent(this, SetDirectionActivity.class);
        this.startActivityForResult(myIntent, REQUEST_DESTINATION_VALUES);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                if(requestCode == Activity.RESULT_CANCELED) {
                    requestingLocationUpdates = false;
                }
                break;

            case REQUEST_DESTINATION_VALUES:
                if (resultCode == Activity.RESULT_OK) {
                    String setted_latitude = data.getStringExtra(DIRECTION_EXTRA_VALUES_LATITUDE); // PUBLIC_STATIC_STRING_IDENTIFIER
                    String setted_longitude = data.getStringExtra(DIRECTION_EXTRA_VALUES_LONGITUDE); // PUBLIC_STATIC_STRING_IDENTIFIER
                    navigationPresenter.setDestinationCoordinates(setted_latitude, setted_longitude);
                    destination_arrow.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (requestingLocationUpdates && navigationPresenter.checkPermissions()) {
            navigationPresenter.startLocationUpdates();
        } else if (!navigationPresenter.checkPermissions()) {
            navigationPresenter.requestPermissions();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        navigationPresenter.stopLocationUpdates();
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates);
        savedInstanceState.putParcelable(KEY_LOCATION, currentLocation);
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, lastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    navigationPresenter.startLocationUpdates();
                }
            } else {
                navigationPresenter.showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }
}