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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /* UI */
    private Button btnSettings;
    private ImageView imgArrow;       // Arrow indicating current direction of the ball
    private TextView txtHeight;       // The text updating throughout the throw
    private TextView txtHighestThrow; // The top text with the high score

    private SensorManager sensorManager;
    private Sensor accelSensors;
    private SharedPreferences preferences;   // Holds threshold and high score
    private MediaPlayer highestPointSound;   // The sound player

    private float accelerationThreshold; // From the settings
    private boolean ballMoving; // Used to make sure a throw can't be initiated when one is in progress

    private ArrayList<Double> accelerations; // This holds the temporary accelerations to find the max
    private Timer ballTimer; // Used to find the highest acceleration

    /* Constants */
    public static final float EARTH_GRAVITY = -9.81f; // There's an android constant for this but idk what it is :)
    public static final String PREFERENCES_SETTINGS = "SETTINGS";
    public static final String SETTINGS_THRESHOLD = "SETTINGS_THRESHOLD";
    public static final String HIGHSCORE = "HIGHSCORE";

    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.initViews();

        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.accelSensors = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        this.preferences = getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, 0);
        this.accelerationThreshold = preferences.getInt(SETTINGS_THRESHOLD, 10);

        this.highestPointSound = MediaPlayer.create(this, R.raw.hiko_are_you_kidding_me);
        this.ballMoving = false;
        this.accelerations = new ArrayList<>();
        this.ballTimer = new Timer();

        this.updateHighestThrowText();


        this.btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                highestPointSound.start();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        // Long clicking on the high score text resets the high score
        // Lets call this an easter egg, so it sounds fun :)
        this.txtHighestThrow.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                preferences.edit().putFloat(HIGHSCORE, 0f).apply();
                updateHighestThrowText();
                return true;
                // If there is also an onClick listener registered, returning true will make
                // the app only call one of the functions, ie. returning a variable
                // called "isEventHandled" would make sense here
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

                    if(acceleration >= this.accelerationThreshold) {
                        this.accelerations.add(acceleration);

                        // First acceleration above the threshold, schedule the ball to move
                        // with the highest acceleration found in a span of 250 milliseconds
                        if(accelerations.size() == 1) {
                            this.ballTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    double highest = Collections.max(accelerations);

                                    // Needs to be cleared first or else it can run multiple times
                                    accelerations.clear();

                                    moveBall(highest);
                                }
                            }, 250);
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
    private void moveBall(final double velocity) {
        this.ballMoving = true;

        final long startTime = System.currentTimeMillis();

        /* v0 = starting velocity
           v(t) = v0 - a*t
           At the highest point, v(t) = 0
           0 = v0 - at
           -v0 = -a*t
           -v0/a = -t
           t = -(v/a) * -1 */
        final int timeToHighest = (int)(velocity/EARTH_GRAVITY) * -1000; // In milliseconds

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean highestReached = false;
                double position = 0d;   // Position of the ball
                double highestPos = 0d;

                // Small vibration at the start and end of the throw
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));

                while(Double.compare(position, 0d) >= 0) {
                    long dt = (System.currentTimeMillis() - startTime); // Time since start in ms

                    if(dt >= timeToHighest) { // Highest point is reached, ball is falling down
                        if(!highestReached) { // First time we reach this point, play a sound
                            highestReached = true;
                            highestPointSound.start();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() { // Point the arrow downwards
                                    imgArrow.setRotation(180);
                                }
                            });
                        }
                        // Here I would increase the pitch of the sound

                    } else { // Ball is still going upwards
                        // And here decrease the pitch, but this isn't implemented :)
                    }

                    if (dt % 16 == 0) { // Update every 16 ms (~60fps)

                        // This looks pretty nasty, but all the 1000 parts are converting
                        // from m/s to m/ms. It is basically just the formula below
                        // pos = v0*t + a*t^2/2
                        position = (velocity / 1000f) * dt + (EARTH_GRAVITY / 1000f) * Math.pow(dt, 2) / (2d * 1000d);

                        if(position > highestPos) {
                            highestPos = position;
                        }

                        final String heightText = String.format(Locale.getDefault(),
                                "The ball is %dm above the ground (Thrown with velocity: %d)",
                                (int)position,
                                (int)velocity
                        );

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtHeight.setText(heightText);
                            }
                        });
                    }
                }

                runOnUiThread(new Runnable() { // Reset text and image to default
                    @Override
                    public void run() {
                        txtHeight.setText(R.string.txt_height);
                        imgArrow.setRotation(0);
                    }
                });

                ballMoving = false;
                v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));

                if(highestPos > preferences.getFloat(HIGHSCORE, 0f)) { // This throw was the highest
                    preferences.edit().putFloat(HIGHSCORE, (float)highestPos).apply();
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

        // Float.compare returns an integer that says which value is highest (0 = equal)
        // This is probably better than comparing two floats directly (because of imprecision)
        if(Float.compare(highestThrow, 0f) == 0) {
            this.txtHighestThrow.setText(R.string.txt_highscoreDefault);
        } else {
            this.txtHighestThrow.setText(String.format(Locale.getDefault(),
                    "Highest throw: %d", (int)highestThrow));
        }
    }
}