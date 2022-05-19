package org.tensorflow.lite.examples.detection;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class DirectionDitector {
    private static DirectionDitector instance;
    private static SensorManager sensorManager;
    private static Sensor stepSensor;
    private static SensorEventListener sensorEventListener;
    private static boolean detectMode;

    private static float currentDirection;

    private static final int RADIAN_TO_DEGREE = -57;

    private static final float frequency = 1000;
    private long lastMovedTime = System.currentTimeMillis();
    private boolean movement = true; // 정지 여부

    private DirectionDitector(Context context) {
        setLastMovedTime(System.currentTimeMillis());
        detectMode = false;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        stepSensor = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                currentDirection = (sensorEvent.values[0] * RADIAN_TO_DEGREE);
                if(getLastDetectTimePassed() > 3000){
                    Log.d("currentDirection : ", String.valueOf(currentDirection));
                    setLastMovedTime(System.currentTimeMillis());
                }
                movement = true;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {}
        };
        sensorManager.registerListener(sensorEventListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public static DirectionDitector getInstance(Context context) {
        if (instance == null) {
            instance = new DirectionDitector(context);
        }
        return instance;
    }

    public long getLastDetectTimePassed() {
        return Math.abs(System.currentTimeMillis() - lastMovedTime);
    }

    public void log() {
        Log.d("currentDirection : ", String.valueOf(currentDirection));
    }

    public static boolean isDetectMode() {
        return detectMode;
    }
    public static void setDetectMode(boolean detectMode) {DirectionDitector.detectMode = detectMode;}
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
