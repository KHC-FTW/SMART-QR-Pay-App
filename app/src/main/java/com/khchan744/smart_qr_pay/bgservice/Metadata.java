package com.khchan744.smart_qr_pay.bgservice;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.concurrent.atomic.AtomicReference;

public class Metadata implements SensorEventListener {

    public static final int TIME_MS_TOL = 10000 * 2;
    public static final boolean TIME_IS_TWO_SIDED = false;
    public static final double LATITUDE_TOL = 0.000066 * 2;
    public static final double LONGITUDE_TOL = 0.00025 * 2;
    public static final boolean LOCATION_IS_TWO_SIDED = true;
    public static final float X_AXIS_TOL = 1.2f * 2;
    public static final float Y_AXIS_TOL = 0.96f * 2;
    public static final float Z_AXIS_TOL = 0.97f * 2;
    public static final boolean ACCEL_IS_TWO_SIDED = true;

    private static final int REQUEST_LOCATION_PERM = 1001;
    private static volatile Metadata INSTANCE;

    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final SensorManager sensorManager;
    private final Sensor accelerometer;

    private final Context context;

    private final AtomicReference<Location> latestLocation = new AtomicReference<>();
    private final AtomicReference<float[]> latestAccelerometer = new AtomicReference<>();

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (location != null) {
                    latestLocation.set(location);
                }
            }
        }
    };

    public interface MetadataCallback {
        void onMetadataCollected(long timeMs, Location location, float[] accelerometer);
        void onMetadataCollectionFailure(String errorMessage);
    }

    private Metadata(Context context) {
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
        this.context = context;
        startLocationUpdates(context);
        startAccelerometerUpdates();
    }

    public static Metadata getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (Metadata.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Metadata(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void startCollection(Activity activity, MetadataCallback callback) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERM
            );
            // The calling activity must handle the permission result and recall this method.
            return;
        }

        Location location = latestLocation.get();
        float[] accel = latestAccelerometer.get();

        if (location == null || accel == null) {
            callback.onMetadataCollectionFailure("Metadata not available yet. Please try again shortly.");
        } else {
            callback.onMetadataCollected(System.currentTimeMillis(), location, accel);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults, Activity activity, MetadataCallback callback) {
        if (requestCode == REQUEST_LOCATION_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates(activity.getApplicationContext());
                // Retry collection after permission is granted
                startCollection(activity, callback);
            } else {
                callback.onMetadataCollectionFailure("Location permission denied");
            }
        }
    }

    private void startLocationUpdates(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Cannot start updates without permission. The calling activity must request it.
            return;
        }
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setWaitForAccurateLocation(true)
                .build();
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void startAccelerometerUpdates() {
        if (accelerometer != null && sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            latestAccelerometer.set(event.values.clone());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used but required by the interface
    }

    public void stopUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    public void startUpdates(){
        startLocationUpdates(context);
        startAccelerometerUpdates();
    }
}
