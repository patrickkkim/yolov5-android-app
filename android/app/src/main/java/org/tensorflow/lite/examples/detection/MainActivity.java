package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button setting_voice,sunglass_connect,googlemap_connect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setting_voice=(Button) findViewById(R.id.setting_voice);
        sunglass_connect=(Button) findViewById(R.id.sunglass_connect);
        googlemap_connect=(Button) findViewById(R.id.googlemap_connect);


        setting_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, VoiceOption.class);
                startActivity(intent);
            }
        });
    }



}