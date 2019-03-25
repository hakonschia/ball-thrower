package com.hakon.ball_thrower;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelSensors;
    private Button btnSettings;
    private Button fly;
    private SharedPreferences preferences;

    private boolean ballMoving;

    private ArrayList<Double> accelerations;

    private double highestAcceleration;

    private float accelerationThreshold;

    public static final float EARTH_GRAVITY = 9.8f;
    public static final String PREFERENCES_SETTINGS = "SETTINGS";
    public static final String SETTINGS_THRESHOLD = "SETTINGS_THRESHOLD";


    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.initViews();

        this.highestAcceleration = 0d;
        this.ballMoving = false;

        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.accelSensors = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        this.preferences = getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, 0);
        this.accelerationThreshold = preferences.getInt(SETTINGS_THRESHOLD, 10);
        this.accelerations = new ArrayList<>();

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        fly = findViewById(R.id.btn_test);
        fly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveBall(50d);
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Timer timer = new Timer();
        // TODO: make so you cant throw when a ball is moving

        boolean ballMoving = false;

        if(!ballMoving) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    final double acceleration = Math.sqrt(x*x + y*y + z*z) - EARTH_GRAVITY;

                    // TODO: Sliding window, whatever that is :)
                    if(acceleration >= this.accelerationThreshold) { // Super exact values aren't compared, so who cares about rounding errors really
                        Log.d(TAG, String.format("onSensorChanged: (%f, %f, %f). Acceleration: %f", x, y, z, acceleration));

                        /*
                        accelerations.add(acceleration);

                        if(accelerations.size() > 15) {
                            // throw ball etcetc
                            moveBall(Collections.max(accelerations));
                        }
                        */

                        // If first is higher, > 0. Equal = 0, second is higher, <0
                        if(Double.compare(acceleration, this.highestAcceleration) > 0) { // New highest found
                            this.highestAcceleration = acceleration;

                            Log.d(TAG, "onSensorChanged: new highest found " + this.highestAcceleration);
                            timer.cancel(); // Cancel the old timer

                            timer = new Timer(); // Create a new timer
                            timer.schedule(new TimerTask() { // 150 ms without a new high
                                @Override
                                public void run() {
                                    Log.d(TAG, "run: " + acceleration);
                                    highestAcceleration = 0;
                                    moveBall(acceleration);
                                    //  highest = 0;
                                }
                            }, 1000);
                        }
                    }

                    // TODO: Create timer so it looks for the highest within 0.5 seconds or some shit
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        sensorManager.registerListener(this, accelSensors, SensorManager.SENSOR_DELAY_NORMAL);

        this.accelerationThreshold = preferences.getInt(SETTINGS_THRESHOLD, 10);
    }

    /**
     * Initializes all the UI views
     */
    private void initViews() {
        btnSettings = findViewById(R.id.btnSettings);
    }

    /**
     * Moves the ball
     * @param acceleration The starting acceleration
     */
    private void moveBall(double acceleration) {
        double currentAcceleration = acceleration;

        final long lastTime = System.nanoTime();

        Log.d(TAG, "moveBall: MOVING:-D " + acceleration);
        this.ballMoving = true;

        // s = v0 * t + 1/2 * a * t^2
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    float v = fly.getY();
                    long time = System.nanoTime();
                    int t = (int) ((time - lastTime) / 1000000);

                    double y = (v * t) + (1d/2d * EARTH_GRAVITY * Math.pow(t, 2));
                    Log.d(TAG, "moveBall: t(" + t + "), " + y);
                    fly.setY(v + (float)y);
                }
            }
        }).start();
    }
}