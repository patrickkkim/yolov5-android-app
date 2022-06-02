package org.tensorflow.lite.examples.detection;

import android.app.Application;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class DirectionDetector extends Application{
    private static DirectionDetector instance;
    private static Sensor magneticSensor;
    private static Sensor accelerometerSensor;
    private static Sensor stepDetector;

    private static boolean detectMode;



    private static SensorManager directionDetectSensorManager;
    private static SensorEventListener directionDetectListener;

    private static int orientation;


    private static float[] rotation = new float[9];
    private static float[] result_data = new float[3];
    private static float[] mag_data = new float[3];
    private static float[] acc_data = new float[3];

    private TextToSpeech tts;

    private long lastMovedTime = System.currentTimeMillis();

    private boolean movement = true; // 정지 여부

    private static int prevOri = 0;
    private static int currentOri = 0;
    private static boolean isLoading = false;

    private DirectionDetector(Context context) {
        directionDetectSensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = directionDetectSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = directionDetectSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        stepDetector = directionDetectSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        tts = TextToSpeech.getInstance(this);
        directionDetectListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    mag_data = sensorEvent.values.clone();
                }
                else if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                    acc_data = sensorEvent.values.clone();
                }
                if(mag_data != null && acc_data != null){
                    SensorManager.getRotationMatrix(rotation, null, acc_data, mag_data);
                    SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, rotation);
                    SensorManager.getOrientation(rotation, result_data);
                    orientation = (int) Math.toDegrees(result_data[0]);
                    if(orientation < 0){
                        orientation += 360;
                    }
                }
                if(sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                    if(!isLoading){
                        prevOri = orientation;
                        currentOri = orientation;
                        isLoading = true;
                    }
                    else{
                        prevOri = currentOri;
                        currentOri = orientation;
                    }
                    if(Math.abs(currentOri - prevOri) > 10 && Math.abs(currentOri - prevOri) < 50){
                        tts.readBreakAway();
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        directionDetectSensorManager.registerListener(directionDetectListener, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        directionDetectSensorManager.registerListener(directionDetectListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        directionDetectSensorManager.registerListener(directionDetectListener, stepDetector, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public static DirectionDetector getInstance(Context context) {
        if (instance == null) {
            instance = new DirectionDetector(context);
        }
        return instance;
    }

    public long getLastDetectTimePassed() {
        return Math.abs(System.currentTimeMillis() - lastMovedTime);
    }

    public void stopDirectionDetect() {
        directionDetectSensorManager.unregisterListener(directionDetectListener);}

    public static boolean isDetectMode() {
        return detectMode;
    }
    public static void setDetectMode(boolean detectMode) { DirectionDetector.detectMode = detectMode;}
    public boolean isMoving() { return movement; }
    public void setMovement(boolean stopMode) { this.movement = stopMode; }
    public long getLastMovedTime() {
        return lastMovedTime;
    }
    public void setLastMovedTime(long lastMovedTime) {
        this.lastMovedTime = lastMovedTime;
    }

}
