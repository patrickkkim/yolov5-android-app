package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class DirectionActivity extends AppCompatActivity {
    private DirectionDetector directionDetector;
    private static TextView orientation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction);
        orientation = findViewById(R.id.orientation);
        directionDetector = DirectionDetector.getInstance(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        directionDetector.stopDirectionDetect();
    }
}