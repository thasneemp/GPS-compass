package test.launcher.mummu.gpscompassdemon;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    private LocationRequest locationRequest;
    private GoogleApiClient googleMapClient;
    private ImageView mCompassImageView;
    private ImageView mNeedleImageView;
    private SensorManager mSensorManager;
    // angle, distance between destination and current location
    private float targetAngle = 0;
    private float targetDistance = 0;

    // record the compass picture angle turned
    private float currentDegree_compass = 0;
    private float currentDegree_needle = 0;
    private DatabaseReference myRef;
    private FirebaseDatabase database;
    private Location currentLocation;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    private String deviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCompassImageView = (ImageView) findViewById(R.id.imageView);
        mNeedleImageView = (ImageView) findViewById(R.id.imageNeedle);
        deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        myRef.child("users").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getKey().equalsIgnoreCase(getDeviceId())) {
                    Log.d("TAG", "onChildChanged: " + " user same");
                } else {

                    try {
                        JSONObject jsonObject = new JSONObject(dataSnapshot.getValue().toString());
                        Location destinLocation = new Location("");
                        destinLocation.setLatitude(Double.parseDouble(jsonObject.getString("latitude")));
                        destinLocation.setLongitude(Double.parseDouble(jsonObject.getString("longitude")));
                        targetAngle = (360 + currentLocation.bearingTo(destinLocation)) % 360;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getKey().equalsIgnoreCase(getDeviceId())) {
                    Log.d("TAG", "onChildChanged: " + " user same");
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(dataSnapshot.getValue().toString());
                        Location destinLocation = new Location("");
                        destinLocation.setLatitude(Double.parseDouble(jsonObject.getString("latitude")));
                        destinLocation.setLongitude(Double.parseDouble(jsonObject.getString("longitude")));
                        targetAngle = (360 + currentLocation.bearingTo(destinLocation)) % 360;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    @Override
    protected void onStart() {
        googleMapClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleMapClient.connect();
        createLocationRequest();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        List<Sensor> sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            Log.d("TAG", "onStart: " + sensor.getName());
        }
        super.onStart();
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                googleMapClient, locationRequest, this);

    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        myRef.child("users").child(getDeviceId()).child("latitude").setValue(location.getLatitude());
        myRef.child("users").child(getDeviceId()).child("longitude").setValue(location.getLongitude());

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // get the angle around the z-axis rotated (azimuth)
        float degree_compass = Math.round(event.values.clone()[0]);
//        rotate_compass(degree_compass);
        // degree_compass: counterclockwise, targetAngle: clockwise
        rotate_needle((degree_compass - targetAngle) % 360);
    }

    private void rotate_needle(float degree) {
// create a rotation animation (reverse turn degree <degrees>)
        RotateAnimation ra = new RotateAnimation(
                currentDegree_needle, -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setInterpolator(new LinearInterpolator());
        // how long the animation will take place
        ra.setDuration(5000);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        mNeedleImageView.startAnimation(ra);
        currentDegree_needle = -degree;
    }

    private void rotate_compass(float degree_compass) {
// create a rotation animation (reverse turn degree <degrees>)
        RotateAnimation ra = new RotateAnimation(
                currentDegree_compass, -degree_compass,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setInterpolator(new LinearInterpolator());
        // how long the animation will take place
        ra.setDuration(5000);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        mCompassImageView.startAnimation(ra);
        currentDegree_compass = -degree_compass;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        super.onPause();
    }
}
