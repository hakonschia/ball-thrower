package com.hakon.ball_thrower;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {

    private TextView mTxtThreshold;
    private SeekBar mSeekBarThreshold;
    private Button mBtnApply;

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        this.initViews();

        final SharedPreferences preferences = getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, 0);

        final int currentThreshold = preferences.getInt(MainActivity.SETTINGS_THRESHOLD, 10);

        mTxtThreshold.setText("Acceleration threshold - " + currentThreshold);

        mSeekBarThreshold.setProgress(currentThreshold);
        mSeekBarThreshold.setMin(10);
        mSeekBarThreshold.setMax(30);

        mSeekBarThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mTxtThreshold.setText("Acceleration threshold - " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mBtnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final SharedPreferences.Editor preferencesEditor = preferences.edit();

                preferencesEditor.putInt(MainActivity.SETTINGS_THRESHOLD, mSeekBarThreshold.getProgress());
                preferencesEditor.apply();

                finish();
            }
        });
    }

    private void initViews() {
        mTxtThreshold = findViewById(R.id.txtThreshold);
        mSeekBarThreshold = findViewById(R.id.seekBarThreshold);
        mBtnApply = findViewById(R.id.btnApply);
    }
}
