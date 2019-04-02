package com.hakon.ball_thrower;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelSensors;

    /* UI */
    private Button btnSettings;
    private ImageView imgArrow;
    private TextView txtHeight;
    private TextView txtHighestThrow;
    private Button fly; // Debug

    private SharedPreferences preferences; // Settings (threshold
    private MediaPlayer highestPointSound;  // The sound player

    private float accelerationThreshold; // From the settings
    private boolean ballMoving;
    private Thread ballMover;

    private ArrayList<Double> accelerations;

    /* Constants */
    public static final float EARTH_GRAVITY = -9.81f;
    public static final String PREFERENCES_SETTINGS = "SETTINGS";
    public static final String SETTINGS_THRESHOLD = "SETTINGS_THRESHOLD";
    public static final String HIGHSCORE = "HIGHSCORE";

    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.initViews();

        this.ballMoving = false;

        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.accelSensors = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        this.preferences = getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, 0);
        this.accelerationThreshold = preferences.getInt(SETTINGS_THRESHOLD, 10);

        this.accelerations = new ArrayList<>();

        this.highestPointSound = MediaPlayer.create(this, R.raw.hiko_are_you_kidding_me);

        this.updateHighestThrowText();

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                highestPointSound.start();
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

        // Long clicking on the highscore text resets the highscore
        // Lets call this an easter egg, so it sounds fun
        txtHighestThrow.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                preferences.edit().putFloat(HIGHSCORE, 0f).apply();
                return true;
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(!this.ballMoving) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    // EARTH_GRAVITY is negative, so for it to be correct we need to add instead of subtract
                    final double acceleration = Math.sqrt(x*x + y*y + z*z) + EARTH_GRAVITY;
                    Log.d(TAG, String.format("onSensorChanged: %f. Acceleration: %f", this.accelerationThreshold, acceleration));

                    // TODO: Sliding window, whatever that is :)
                    if(acceleration >= this.accelerationThreshold) {
                        // Just adding to a list and checking when it's above a certain size
                        // doesnt work because it might not add enough accelerations in one throw
                        accelerations.add(acceleration);

                        if(accelerations.size() == 15) {
                            moveBall(Collections.max(accelerations));
                            accelerations.clear();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelSensors, SensorManager.SENSOR_DELAY_NORMAL);

        this.accelerationThreshold = preferences.getInt(SETTINGS_THRESHOLD, 10);
    }

    /**
     * Initializes all the UI views
     */
    private void initViews() {
        this.btnSettings = findViewById(R.id.btn_settings);
        this.imgArrow = findViewById(R.id.img_arrow);
        this.txtHeight = findViewById(R.id.txt_height);
        this.txtHighestThrow = findViewById(R.id.txt_highestThrow);
    }

    /**
     * Moves the non existent ball
     * @param velocity The starting velocity
     */
    private synchronized void moveBall(final double velocity) {
        this.ballMoving = true;

        final long startTime = System.currentTimeMillis();

        Log.d(TAG, "moveBall: velocity is " + velocity);

        // v0 = starting velocity
        // v(t) = v0 - a*t
        // At the highest point, v(t) = 0
        // 0 = v0 - at
        // -v0 = -a*t
        // -v0/a = -t
        // t = (v/a) * -1
        final double timeToHighest = (velocity/EARTH_GRAVITY) * -1;

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean highestReached = false;
                double position = 0; // Position of the ball
                double highestPos = 0;

                while(position >= 0) { // comparing floats bad or something
                    long dt = (System.currentTimeMillis() - startTime) / 1000; // Time since start

                    if(dt >= timeToHighest) { // Highest point is reached, ball is falling down
                        if(!highestReached) {
                            highestReached = true;
                            highestPointSound.start();

                            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    imgArrow.setRotation(180);
                                }
                            });
                        }
                        // Increase pitch of nice sound or whatever

                    } else { // Ball is still going upwards
                        // Decrease pitch
                    }

                    // pos = v0 * t + 1/2 * a * t^2
                    position = (velocity * dt) + (EARTH_GRAVITY / 2d * Math.pow(dt, 2));

                    long dt2 = (System.currentTimeMillis() - startTime); // Time since start
                    double position2 = ((velocity * dt2) + (EARTH_GRAVITY / 2d * Math.pow(dt2, 2)));

                    Log.d(TAG, String.format("other: %d/%d, %s/%s", dt, dt2, position, position2));
                    if(position > highestPos) {
                        highestPos = position;
                    }

                    final String heightText = String.format(Locale.getDefault(),
                            "The ball is %dm above the ground", (int)position);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtHeight.setText(heightText);
                        }
                    });
                }

                runOnUiThread(new Runnable() { // Reset text and image to default
                    @Override
                    public void run() {
                        txtHeight.setText(R.string.txt_height);
                        imgArrow.setRotation(0);
                    }
                });

                Log.d(TAG, "run: highest " + highestPos);

                ballMoving = false;

                if(highestPos > preferences.getFloat(HIGHSCORE, 0f)) { // This throw was the highest
                    preferences.edit().putFloat(HIGHSCORE, (float)highestPos).apply();
                    txtHighestThrow.setText("");
                    updateHighestThrowText();
                }
            }
        }).start();
    }

    /**
     * Updates txt_highestThrow to be the highest recorded throw
     */
    private void updateHighestThrowText() {
        float highestThrow = preferences.getFloat(HIGHSCORE, 0f);

        if(highestThrow == 0f) {
            this.txtHighestThrow.setText("No throw yet recorded");
        } else {
            this.txtHighestThrow.setText(String.format(Locale.getDefault(),
                    "Highest throw: %d", (int)highestThrow));
        }
    }
}