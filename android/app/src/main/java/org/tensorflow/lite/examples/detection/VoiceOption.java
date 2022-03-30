//2022-02-12
package org.tensorflow.lite.examples.detection;

import static android.util.Log.ERROR;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;


public class VoiceOption extends AppCompatActivity {

    private TextToSpeech tts;

    //private EditText editText;
    //private TextView random, random2;
    private Button voicefrequency,voicefrequency2,voicefast,voicelow, stopmode;
    /*
    private final double border_Left=(double)1/640*212;
    private final double border_Lower=(double)1/640*212;
    private final double border_Right=(double)1/640*426;
    private final double border_Top=(double)1/640*426; */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_option);
        //editText=(EditText)  findViewById(R.id.editText);
        voicefrequency = (Button) findViewById(R.id.voicefrequency);
        voicefrequency2 = (Button) findViewById(R.id.voicefrequency2);
        voicefast = (Button) findViewById(R.id.voicefast);
        voicelow = (Button) findViewById(R.id.voicelow);
        stopmode = (Button) findViewById(R.id.stopmode);

        tts = new TextToSpeech(this);


        voicefrequency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                tts.incrementFreq();
            }
        });

        voicefrequency2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                tts.decrementFreq();
            }
        });

        voicefast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                // speed += 0.2;
                tts.incrementSpeed();
                //tts.getSpeed(2.0);   //읽는 속도 2배 빠르게
                //editText 문장 읽기
            }
        });

        voicelow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                tts.decrementSpeed();
            }
        });

        stopmode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MotionDetector motionDetector = MotionDetector.getInstance(VoiceOption.this, tts);
                motionDetector.setDetectMode(!MotionDetector.isDetectMode());
                String output = "";
                if (MotionDetector.isDetectMode()) {
                    output = "정지 모드 활성";
                } else {
                    output = "정지 모드 비활성";
                }
                Toast.makeText(VoiceOption.this, output, Toast.LENGTH_SHORT).show();
                tts.readText(output);
            }
        });
    }


 /*
    private String inputLocation(double x_median, double y_median, double height, double width) {
        String Temp="";
        if(x_median< border_Left) { //x 중앙값이 왼쪽 경계에 있을때
            if(y_median <border_Lower) {
                Temp = "왼쪽 하단";
            }
            else {
                Temp= (y_median >= border_Lower && y_median < border_Top) ? "왼쪽":"왼쪽 상단";
            }
        }
        else if(x_median >= border_Left && x_median< border_Right ) { //x 중앙값이 중앙 경계에 있을때
            if (y_median < border_Lower) {
                Temp = "중앙 하단";
            } else {
                Temp = (y_median >= border_Lower && y_median < border_Top) ? "중앙" : "중앙 상단";
            }
        }
        else if(x_median > border_Right){
            if (y_median < border_Lower) {
                Temp = "오른쪽 하단";
            } else {
                Temp = (y_median >= border_Lower && y_median < border_Top) ? "오른쪽" : "오른쪽 상단";
            }
        }
        else System.out.println("label value error");
        return Temp;
    }
 */
   /* @Override
    protected void onDestroy() {
        super.onDestroy();
        //TTs 객체가 남아있다면 메모리에서 제거
        if(tts != null){
            tts.stop();
            tts.shutdown();
            tts=null;
        }
    }
    */
}