package org.tensorflow.lite.examples.detection;

import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class DirectionDitector extends Application {
    private static DirectionDitector instance;
    private static SensorManager sensorManager;
    private static Sensor stepSensor;
    private static SensorEventListener sensorEventListener;
    private static boolean detectMode;

    private static int prevDirectionL;
    private static int currentDirectionL;

    private static boolean isLoading = false;

    private TextToSpeech tts;

    private long lastMovedTime = System.currentTimeMillis();
    private boolean movement = true; // 정지 여부

    private DirectionDitector(Context context) {
        setLastMovedTime(System.currentTimeMillis());
        detectMode = false;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        stepSensor = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        tts = TextToSpeech.getInstance(this);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(getLastDetectTimePassed() > 500){
                    if(!isLoading){
                        currentDirectionL = (int)Math.floor(sensorEvent.values[2]);
                        isLoading = true;
                    }
                    else{
                        prevDirectionL = currentDirectionL;
                        currentDirectionL = (int)Math.floor(sensorEvent.values[2]);
                        if(Math.abs(currentDirectionL - prevDirectionL) > 5 && Math.abs(currentDirectionL - prevDirectionL) <10) tts.readBreakAway();
                    }
                    Log.d("currentDirection : ", String.valueOf(prevDirectionL)+"///"+ String.valueOf(currentDirectionL));
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


    public static boolean isDetectMode() {
        return detectMode;
    }
    public static void setDetectMode(boolean detectMode) {DirectionDitector.detectMode = detectMode;}

    public boolean isMoving() { return movement; }
    public void setMovement(boolean stopMode) { this.movement = stopMode; }
    public long getLastMovedTime() {
        return lastMovedTime;
    }
    public void setLastMovedTime(long lastMovedTime) {
        this.lastMovedTime = lastMovedTime;
    }

}
