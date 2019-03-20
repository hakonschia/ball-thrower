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

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelSensors;
    private Button btnSettings;
    private SharedPreferences preferences;

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

        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.accelSensors = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        this.preferences = getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, 0);
        this.accelerationThreshold = preferences.getInt(SETTINGS_THRESHOLD, 10);

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Timer timer = new Timer();
        // TODO: make so you cant throw when a ball is moving

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                final double acceleration = Math.sqrt(x*x + y*y + z*z) - EARTH_GRAVITY;

                // TODO: Sliding window, whatever that is :)
                if(acceleration >= this.accelerationThreshold) { // Super exact values aren't compared, so who cares about rounding errors really
                    Log.d(TAG, String.format("onSensorChanged: (%f, %f, %f). Acceleration: %f", x, y, z, acceleration));

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
                               // moveBall(acceleration);
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

        //TODO: Probably run on new thread? xd
        double currentAcceleration = acceleration;

        long lastTime = System.nanoTime();

        while(currentAcceleration > 0d) {
            long time = System.nanoTime();
            int deltaTime = (int) ((time - lastTime) / 1000000);

            currentAcceleration -= EARTH_GRAVITY / deltaTime;

            Log.d(TAG, "moveBall: " + currentAcceleration);

            lastTime = time;
        }
    }
}