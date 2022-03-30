package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.Arrays;

public class MotionDetector {
    private static MotionDetector instance;
    private static SensorManager sensorManager;
    private static Sensor stepSensor;
    private static SensorEventListener sensorEventListener;
    private static boolean detectMode;

    private static final float frequency = 1000;
    private long lastMovedTime = System.currentTimeMillis();
    private boolean movement = true; // 정지 여부

    private MotionDetector(Context context, TextToSpeech tts) {
        detectMode = false;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        stepSensor = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                movement = true;
                lastMovedTime = System.currentTimeMillis();
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {}
        };
        sensorManager.registerListener(sensorEventListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public static MotionDetector getInstance(Context context, TextToSpeech tts) {
        if (instance == null) {
            instance = new MotionDetector(context, tts);
        }
        return instance;
    }

    public long getLastMovedTimePassed() {
        return Math.abs(System.currentTimeMillis() - lastMovedTime);
    }

    public static boolean isDetectMode() {
        return detectMode;
    }
    public static void setDetectMode(boolean detectMode) {
        MotionDetector.detectMode = detectMode;
    }
    public static float getFrequency() {
        return frequency;
    }
    public boolean isMoving() { return movement; }
    public void setMovement(boolean stopMode) { this.movement = stopMode; }
    public long getLastMovedTime() {
        return lastMovedTime;
    }
    public void setLastMovedTime(long lastMovedTime) {
        this.lastMovedTime = lastMovedTime;
    }
}
