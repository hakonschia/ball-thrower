package com.hakon.ball_thrower;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

public class SettingsActivity extends AppCompatActivity {

    private TextView txtThreshold;
    private SeekBar seekBarThreshold;
    private Button btnApply;

    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        this.initViews();

        final SharedPreferences preferences = getSharedPreferences(MainActivity.PREFERENCES_SETTINGS, 0);

        final int currentThreshold = preferences.getInt(MainActivity.SETTINGS_THRESHOLD, 10);

        txtThreshold.setText("Acceleration threshold - " + currentThreshold);

        seekBarThreshold.setProgress(currentThreshold);
        seekBarThreshold.setMin(10);
        seekBarThreshold.setMax(30);

        seekBarThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txtThreshold.setText("Acceleration threshold - " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final SharedPreferences.Editor preferencesEditor = preferences.edit();

                preferencesEditor.putInt(MainActivity.SETTINGS_THRESHOLD, seekBarThreshold.getProgress());
                preferencesEditor.apply();

                finish();
            }
        });
    }

    private void initViews() {
        txtThreshold = findViewById(R.id.txtThreshold);
        seekBarThreshold = findViewById(R.id.seekBarThreshold);
        btnApply = findViewById(R.id.btnApply);
    }
}
