package org.tensorflow.lite.examples.detection;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;


public class Manual extends AppCompatActivity {
    private TextToSpeech tts;
    private AppCompatButton play;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual);


        play = (AppCompatButton) findViewById(R.id.play);

        tts = new TextToSpeech(this);

        ReadManual();
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                ReadManual();

            }
        });


    }
    @Override
    protected void onStart() {
        super.onStart();
        ReadManual();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(tts!=null)
            tts.stop();
    }

    public void ReadManual()
    {
        tts.readText("상단에서 첫번째 버튼은 안내 시작 버튼입니다.\n" +
                "실시간으로 보행중에 카메라를 통해 장애물을 식별하고 위치를 알려줍니다.\n" +
                "\n" +
                "두번째 버튼은 내 위치 버튼입니다.\n" +
                "내 위치의 주소를 실시간으로 알려줍니다.\n" +
                "\n" +
                "세번째 버튼은 음성 설정 버튼입니다.\n" +
                "안내하는 목소리의 빠르기와 목소리 지연시간을 조절할 수 있습니다.\n" +
                "그리고 정지모드를 활성화할 수 있습니다."+
                "\n" +
                "다시듣기를 원하시면 가운데 버튼을 눌러주세요.");
    }


}