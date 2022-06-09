package org.tensorflow.lite.examples.detection;

import static android.util.Log.ERROR;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Handler;

public class TextToSpeech extends AppCompatActivity {
    private android.speech.tts.TextToSpeech tts;
    private final double border_Left = 212;
    private final double border_Lower = 212;
    private final double border_Right = 426;
    private final double border_Top = 426;

    private int standbyIndex = 0;
    private int lastPlayIndex = 0;
    private static UtteranceProgressListener progressListener;
    private static boolean isRunning = false;

    private static TextToSpeech instance;

    MediaPlayer player;
    private boolean isLoading = true;
    private long lastSpokeTime;
    private static float speed = 1.4f;
    private static float frequency = 1500;

    private long lastBeepTime;
    private boolean isBeeping = false;

    private Context context;

    private TextToSpeech(Context context) {
        //TTS 생성후, OnInitListener로 초기화
        this.context = context;
        tts = new android.speech.tts.TextToSpeech(context, new android.speech.tts.TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i != ERROR){
                    tts.setLanguage(Locale.KOREAN);
                    tts.setSpeechRate(speed);
                }
            }
        });

        // tts에 리스너 추가
        progressListener = new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
            }
            @Override
            public void onError(String s) { }


            // 말 끝났을 때
            @Override
            public void onDone(String s) {
                new Thread() { //말 끝날때마다 쓰레드 생성함(충돌이 있어서, 비동기적 구현)
                    public void run() {
                        TextToSpeech.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                lastSpokeTime = System.currentTimeMillis(); // 시간 기록


                            }
                        });
                    }
                }.start();
                //clearAll();
            }
            @Override
            public void onRangeStart(String utteranceId, int start, int end, int frame) {
                lastPlayIndex = start;
            }
        };
        tts.setOnUtteranceProgressListener(progressListener);
    }

    //while()
    //{
        //tts를 index로 읽다가
        //중간에 beef음이 들어오면 중단
        //라스트 인덱스부터 읽기

    //}
    // 싱글턴 객체 반환 메소드
    public static TextToSpeech getInstance(Context context) {
        if (instance == null) {
            instance = new TextToSpeech(context);
        }
        return instance;
    }

    // 문장 읽기 메소드(말 끊기 불가능)
    public void readText(String text) {
        if (!tts.isSpeaking()) {
            tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, text);
        }
    }

    // 문장 읽기 메소드(말 끊기 가능)
    public void readTextWithInterference(String text) {
        tts.stop();
        tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, text);
    }

    // Speak label and location
    public void readLocation(String location, String label) {
        String text = location+"에 "+label+" 있습니다.";
        readTextWithInterference(text);

    }

    public void readLocations(LinkedHashMap<String, HashSet<String>> locationMap) {
        String speechText = "";
        for (String location : locationMap.keySet()) {
            HashSet<String> labelSet = locationMap.get(location); //(왼쪽,중앙,오른쪽) 중의 장소 hashmap 저장
            if (labelSet != null && !labelSet.isEmpty()) { //위치의 value값이 비지 않은 값만 저장
                speechText += location + "에 ";
                for (String label : labelSet) {
                    speechText += label + ". ";
                }
            }
        }
        if (speechText.equals("")) return;

        speechText += " 있습니다.";
        readText(speechText);
    }

    public void makeBeep() {
        if (isBeeping) {
            if (System.currentTimeMillis() - lastBeepTime > 500) {
                player.stop();
                isBeeping = false;
            }
            else {
                return;
            }
        }
        isBeeping = true;
        player = MediaPlayer.create(context, R.raw.beep_1);
        player.setLooping(false);
        lastBeepTime = System.currentTimeMillis();
        player.start();
//        readText("주의하세요");
    }

    public void alertLeftSide() {
        readTextWithInterference("왼쪽으로 기울어짐");
    }
    public void alertRightSide() {
        readTextWithInterference("오른쪽으로 기울어짐");
    }
  
    public boolean IsSpeaking(){
        return tts.isSpeaking();
    }


    public void readGPs(String text){
        tts.speak("현재 위치는"+ text +"입니다.", android.speech.tts.TextToSpeech.QUEUE_FLUSH, null);
    }

    // Return the location of object
    public String inputLocation(ArrayList<Double> location) {
        double x_median = location.get(0);
        double y_median = location.get(1);
        double height = location.get(2);
        double width = location.get(3);

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

    public float getSpeed(){ return speed; }
    public void setSpeed(float speed){
        TextToSpeech.speed = speed;
        tts.setSpeechRate(speed);
    }
    public void incrementSpeed() {
        setSpeed(speed + (float) 0.2);
    }
    public void decrementSpeed() {
        setSpeed(speed - (float) 0.2);
    }

    public long getLastSpokeTimePassed() {
        return Math.abs(System.currentTimeMillis() - lastSpokeTime);
    }
    public void reset() {
        setFrequency((float)20000.0);
        setSpeed((float)1.0);
    }
    public void stop(){
        tts.stop();
    }

    public static float getFrequency() {
        return frequency;
    }
    public void setFrequency(float frequency) {
        TextToSpeech.frequency = frequency;
    }
    public void incrementFreq() {
        setFrequency(frequency + (float) 1000);
    }
    public void decrementFreq() {
        setFrequency(frequency - (float) 1000);
    }
    public long getLastSpokeTime() {
        return lastSpokeTime;
    }
    public void setLastSpokeTime(long lastSpokeTime) {
        this.lastSpokeTime = lastSpokeTime;
    }
    public void setIsRunning(boolean isRunning) {this.isRunning = isRunning;}
    public boolean getIsRunning() {return isRunning;}
}