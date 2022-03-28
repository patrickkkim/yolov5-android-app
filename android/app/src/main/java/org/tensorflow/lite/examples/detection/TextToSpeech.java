package org.tensorflow.lite.examples.detection;

import static android.util.Log.ERROR;

import android.content.Context;
import android.content.Intent;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class TextToSpeech {
    private static android.speech.tts.TextToSpeech tts;
    private final double border_Left = 212;
    private final double border_Lower = 212;
    private final double border_Right = 426;
    private final double border_Top = 426;


    private boolean isLoading = true;
    private long lastSpokeTime;
    private static float speed = 1;
    private static float frequency = 1;

    public TextToSpeech(Context context) {
        //TTS 생성후, OnInitListener로 초기화
        tts = new android.speech.tts.TextToSpeech(context, new android.speech.tts.TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i != ERROR){
                    tts.setLanguage(Locale.KOREAN);
                    tts.setSpeechRate(speed);
                }
            }
        });
    }

    public void readText(String text) {
        tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, text);

    }

    // Speak label and location
    public void readLocation(String Location, String label) {
        // 빈도 검사
        long timePassed = Math.abs(System.currentTimeMillis() - getLastSpokeTime());
        if (timePassed < (1000 * getFrequency())) return;

        tts.speak(Location+"에 "+label+" 있습니다.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null);
        // 시간 기록
        while (tts.isSpeaking()) {
            this.setLastSpokeTime(System.currentTimeMillis());
        }
    }

    public void readDelay() {
        tts.playSilence(2000, android.speech.tts.TextToSpeech.QUEUE_ADD,null);
    }

    public boolean IsSpeaking(){
        return tts.isSpeaking();
    }

    public void readGPs(String text){
        tts.speak("현재 위치는"+ text +"입니다.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null);
    }

    // Return the location of object
    public String inputLocation(ArrayList<Double> location) {
        ArrayList<Double> i = location;

        double x_median = i.get(0);
        double y_median = i.get(1);
        double height = i.get(2);
        double width = i.get(3);

        System.out.println("x test:" + x_median);
        System.out.println("y test:" + y_median);

//        String Temp = String.valueOf((int) x_median);
        String Temp = "";

        if(x_median >= border_Right) { //x 중앙값이 왼쪽 경계에 있을때
            Temp += "왼쪽";
        }
        else if(x_median > border_Left && x_median < border_Right) { //x 중앙값이 중앙 경계에 있을때
            Temp += "중앙";
        }
        else if(x_median <= border_Left){
            Temp += "오른쪽";
        }
        else System.out.println("label value error");

        return Temp;
    }

    public boolean isLoading() {
        return isLoading;
    }
    public void setLoading(boolean loading) {
        isLoading = loading;
    }

    public static float getSpeed(){return speed;}
    public static void setSpeed(float speed){
        TextToSpeech.speed = speed;
        tts.setSpeechRate(speed);
    }
    public void incrementSpeed() {
        TextToSpeech.setSpeed(speed + (float) 0.2);
    }
    public void decrementSpeed() {
        TextToSpeech.setSpeed(speed - (float) 0.2);
    }

    public static float getFrequency() {
        return frequency;
    }
    public static void setFrequency(float frequency) {
        TextToSpeech.frequency = frequency;
    }
    public void incrementFreq() {
        TextToSpeech.setFrequency(frequency + (float) 1);
    }
    public void decrementFreq() {
        TextToSpeech.setFrequency(frequency - (float) 1);
    }

    public long getLastSpokeTime() {
        return lastSpokeTime;
    }
    public void setLastSpokeTime(long lastSpokeTime) {
        this.lastSpokeTime = lastSpokeTime;
    }
}