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
    private Button mBtnSettings;
    private ImageView mImgArrow;       // Arrow indicating current direction of the ball
    private TextView mTxtHeight;       // The text updating throughout the throw
    private TextView mTxtHighestThrow; // The top text with the high score

    private SensorManager mSensorManager;
    private Sensor mAccelerationSensor;
    private SharedPreferences mPreferences;   // Holds threshold and high score
    private MediaPlayer mHighestPointSound;   // The sound player

    private float mAccelerationThreshold; // From the settings
    private boolean mBallMoving; // Used to make sure a throw can't be initiated when one is in progress

    private ArrayList<Double> mAccelerations; // This holds the temporary accelerations to find the max
    private Timer mBallTimer; // Used to find the highest acceleration

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

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mPreferences = getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, 0);
        mAccelerationThreshold = mPreferences.getInt(SETTINGS_THRESHOLD, 10);

        mHighestPointSound = MediaPlayer.create(this, R.raw.hiko_are_you_kidding_me);
        mBallMoving = false;
        mAccelerations = new ArrayList<>();
        mBallTimer = new Timer();

        this.updateHighestThrowText();


        mBtnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mHighestPointSound.start();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        // Long clicking on the high score text resets the high score
        // Lets call this an easter egg, so it sounds fun :)
        mTxtHighestThrow.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mPreferences.edit().putFloat(HIGHSCORE, 0f).apply();
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
        if(!mBallMoving) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];

                    // EARTH_GRAVITY is negative, so for it to be correct we need to add instead of subtract
                    final double acceleration = Math.sqrt(x*x + y*y + z*z) + EARTH_GRAVITY;

                    if(acceleration >= mAccelerationThreshold) {
                        mAccelerations.add(acceleration);

                        // First acceleration above the threshold, schedule the ball to move
                        // with the highest acceleration found in a span of 250 milliseconds
                        if(mAccelerations.size() == 1) {
                            mBallTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    double highest = Collections.max(mAccelerations);

                                    // Needs to be cleared first or else it can run multiple times
                                    mAccelerations.clear();

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
        mSensorManager.registerListener(this, mAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL);

        mAccelerationThreshold = mPreferences.getInt(SETTINGS_THRESHOLD, 10);
    }

    /**
     * Initializes all the UI views
     */
    private void initViews() {
        mBtnSettings = findViewById(R.id.btn_settings);
        mImgArrow = findViewById(R.id.img_arrow);
        mTxtHeight = findViewById(R.id.txt_height);
        mTxtHighestThrow = findViewById(R.id.txt_highestThrow);
    }

    /**
     * Moves the non existent ball
     * @param velocity The starting velocity
     */
    private void moveBall(final double velocity) {
        mBallMoving = true;

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
                            mHighestPointSound.start();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() { // Point the arrow downwards
                                    mImgArrow.setRotation(180);
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
                                mTxtHeight.setText(heightText);
                            }
                        });
                    }
                }

                runOnUiThread(new Runnable() { // Reset text and image to default
                    @Override
                    public void run() {
                        mTxtHeight.setText(R.string.txt_height);
                        mImgArrow.setRotation(0);
                    }
                });

                mBallMoving = false;
                v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));

                if(highestPos > mPreferences.getFloat(HIGHSCORE, 0f)) { // This throw was the highest
                    mPreferences.edit().putFloat(HIGHSCORE, (float)highestPos).apply();
                    updateHighestThrowText();
                }
            }
        }).start();
    }

    /**
     * Updates txt_highestThrow to be the highest recorded throw
     */
    private void updateHighestThrowText() {
        float highestThrow = mPreferences.getFloat(HIGHSCORE, 0f);

        if(Float.compare(highestThrow, 0f) == 0) {
            mTxtHighestThrow.setText(R.string.txt_highscoreDefault);
        } else {
            mTxtHighestThrow.setText(String.format(Locale.getDefault(),
                    "Highest throw: %d", (int)highestThrow));
        }
    }
}