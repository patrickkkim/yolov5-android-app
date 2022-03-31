package org.tensorflow.lite.examples.detection;

import static android.util.Log.ERROR;

import android.content.Context;
import android.content.Intent;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TextToSpeech {
    private static android.speech.tts.TextToSpeech tts;
    private final double border_Left = 212;
    private final double border_Lower = 212;
    private final double border_Right = 426;
    private final double border_Top = 426;


    private boolean isLoading = true;
    private long lastSpokeTime;
    private static float speed = 1;
    private static float frequency = 20000;

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
    public void readLocation(String location, String label) {
        tts.speak(location+"에 "+label+" 있습니다.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null);
        // 시간 기록
        while (tts.isSpeaking()) {}
        lastSpokeTime = System.currentTimeMillis();
    }

    public void readLocations(LinkedHashMap<String, HashSet<String>> locationMap) {
        String speechText = "";
        for (String location : locationMap.keySet()) {
            HashSet<String> labelSet = locationMap.get(location);
            if (labelSet != null && !labelSet.isEmpty()) {
                speechText += location + "에 ";
                for (String label : labelSet) {
                    speechText += label + ". ";
                }
            }
        }
        if (speechText.equals("")) return;

        speechText += " 있습니다.";
        this.readText(speechText);
        while (tts.isSpeaking()) {}
        lastSpokeTime = System.currentTimeMillis();
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

    public long getLastSpokeTimePassed() {
        return Math.abs(System.currentTimeMillis() - lastSpokeTime);
    }
    public void reset() {
        TextToSpeech.setFrequency((float)1.0);
        TextToSpeech.setSpeed((float)1.0);
    }
    public void stop(){
        tts.stop();
    }

    public static float getFrequency() {
        return frequency;
    }
    public static void setFrequency(float frequency) {
        TextToSpeech.frequency = frequency;
    }
    public void incrementFreq() {
        TextToSpeech.setFrequency(frequency + (float) 1000);
    }
    public void decrementFreq() {
        TextToSpeech.setFrequency(frequency - (float) 1000);
    }
    public long getLastSpokeTime() {
        return lastSpokeTime;
    }
    public void setLastSpokeTime(long lastSpokeTime) {
        this.lastSpokeTime = lastSpokeTime;
    }
}