package org.tensorflow.lite.examples.detection;

import static android.util.Log.ERROR;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;


public class VoiceOption extends AppCompatActivity {

    private TextToSpeech tts;
    private EditText editText;
    private TextView random, random2;
    private Button voicebutton,voicehigh,voicelow,voicefast,voiceslow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_option);
        editText=(EditText)  findViewById(R.id.editText);
        voicebutton=(Button) findViewById(R.id.voicebutton);
        voicehigh=(Button) findViewById(R.id.voicehigh);
        voicelow=(Button) findViewById(R.id.voicelow);
        voicefast=(Button) findViewById(R.id.voicefast);
        voiceslow=(Button) findViewById(R.id.voiceslow);

        random=(TextView) findViewById(R.id.random);
        random2=(TextView) findViewById(R.id.random2);

        Map<Integer, String> obstacles= new HashMap<Integer, String>(){{ // Obstcacle dictionary
            put(0,"트럭");
            put(1,"가로수");
            put(2,"신호등");
            put(3,"이륜차");
            put(4,"화분");
            put(5,"기둥");
            put(6,"사람");
            put(7,"안내판");
            put(8,"오토바이");
            put(9,"점포");
            put(10,"소화전");
            put(11,"의자");
            put(12,"리어카");
            put(13,"차");
            put(14,"버스");
            put(15,"주의구역");
            put(16,"자전거도로");
            put(17,"횡단보도");
            put(18,"찻길");
            put(19,"점자블록");
            put(20,"인도");
        }};

        Random randnumber = new Random();
        randnumber.setSeed(System.currentTimeMillis());
        double x1=randnumber.nextDouble()*640;
        double x2=x1+randnumber.nextDouble()*(640-x1);
        double y1=randnumber.nextDouble()*640;
        double y2=y1+randnumber.nextDouble()*(640-y1);

        int obsNumber=randnumber.nextInt(20);


        random.setText("x1 "+Double.toString(x1)+"\n"+"y1 "+ Double.toString(y1));
        random2.setText("x2 "+Double.toString(x2)+"\n"+"y2 "+ Double.toString(y2)+ "\n" +"장애물:"+ obstacles.get(obsNumber));
        String Temp="";
        if(x1< 212) {
            if (x2 < 212) {
                Temp = "왼쪽";
            } else if (x2 >= 212 && x2 < 426)
                Temp = "왼쪽과 중앙";
            else
                Temp = "중앙";
        }
        else if(x1 >=212 && x1< 426) {
            Temp= (x2 >= 212 && x2 < 426) ? "중앙":"오른쪽과 중앙";
        }
        else
            Temp ="오른쪽";
        String direction=Temp;



        //TTS 생성후, OnInitListener로 초기화
        tts= new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i != ERROR){

                    tts.setLanguage(Locale.KOREAN);
                }
            }
        });

        voicebutton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                //editText 문장 읽기
                tts.speak(obstacles.get(obsNumber)+" "+direction+"에 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        voicehigh.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                tts.setPitch(2.0f); //음성 톤 두배로 올림
                tts.setSpeechRate(1.0f);    //읽는 속도 기본 설정
                //editText 문장 읽기
                tts.speak(obstacles.get(obsNumber)+" "+direction+"에 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        voicelow.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                tts.setPitch(0.5f); //음성 톤 절반으로 낮춤
                tts.setSpeechRate(1.0f);    //읽는 속도 기본 설정
                //editText 문장 읽기
                tts.speak(obstacles.get(obsNumber)+" "+direction+"에 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        voicefast.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                tts.setPitch(1.0f); //음성 톤 기본 설정
                tts.setSpeechRate(2.0f);    //읽는 속도 2배 빠르게
                //editText 문장 읽기
                tts.speak(obstacles.get(obsNumber)+" "+direction+"에 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        voiceslow.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                tts.setPitch(1.0f); //음성 톤 기본 설정
                tts.setSpeechRate(0.5f);    //읽는 속도 2배 느리게
                //editText 문장 읽기
                tts.speak(obstacles.get(obsNumber)+" "+direction+"에 있습니다.", TextToSpeech.QUEUE_FLUSH, null);
            }
        });

    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        //TTs 객체가 남아있다면 메모리에서 제거
        if(tts != null){
            tts.stop();
            tts.shutdown();
            tts=null;
        }
    }
}