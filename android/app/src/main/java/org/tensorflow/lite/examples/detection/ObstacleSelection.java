package org.tensorflow.lite.examples.detection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ObstacleSelection extends AppCompatActivity {
    private Map<Integer, String> labelTable = new HashMap<>();
    private TextView textView;
    private Button Reset, SaveButton;
    private String checkedResult="";
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obstacle_selection);
        
        String checked = null;
        boolean obsChecked[]= new boolean[21];
        Arrays.fill(obsChecked,false);

        textView=(TextView)findViewById(R.id.textView);
        Reset=(Button)findViewById(R.id.allchoicebutton);
        SaveButton=(Button)findViewById(R.id.savebutton);

        SharedPreferences sharedPreferences = getSharedPreferences("obstacle_list",MODE_PRIVATE); //저장을 하기위해 editor를 이용하여 값을 저장시켜준다.
        SharedPreferences.Editor editor = sharedPreferences.edit();


        ArrayList<String> obstacles = new ArrayList<String>();
        ArrayList<String> checkedObs = new ArrayList<String>();
        // ArrayAdapter 생성. 아이템 View를 선택(multiple choice)가능하도록 만듦.
        obstacles.add("트럭");
        obstacles.add("가로수");
        obstacles.add("신호등");
        obstacles.add("스쿠터");
        obstacles.add("화분");
        obstacles.add("기둥");
        obstacles.add("사람");
        obstacles.add("안내판");
        obstacles.add("오토바이");
        obstacles.add("점포");
        obstacles.add("소화전");
        obstacles.add("의자");
        obstacles.add("리어카");
        obstacles.add("차");
        obstacles.add("버스");
        obstacles.add("주의구역");
        obstacles.add("자전거도로");
        obstacles.add("횡단보도");
        obstacles.add("찻길");
        obstacles.add("점자블록");
        obstacles.add("인도");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, obstacles)
        {

            @Override

            public View getView(int position, View convertView, ViewGroup parent)

            {

                View view = super.getView(position, convertView, parent);

                TextView tv = (TextView) view.findViewById(android.R.id.text1);

                tv.setTextColor(Color.BLACK);

                return view;

            }

        };

        // listview 생성 및 adapter 지정.
        ListView listview = (ListView) findViewById(R.id.listView1);
        listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listview.setAdapter(adapter);
        setListViewHeightBasedOnChildren(listview);
        Intent intent = new Intent(this, DetectorActivity.class);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // 콜백매개변수는 순서대로 어댑터뷰, 해당 아이템의 뷰, 클릭한 순번, 항목의 아이디
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //intent.putExtra(obstacles.get(i), obstacles.get(i));


                if(obsChecked[i]==false)
                {
                    checkedObs.add(obstacles.get(i));
                    obsChecked[i]=true;
                }
                else {
                    checkedObs.remove(obstacles.get(i));
                    obsChecked[i] = false;
                }


            }
        });

        Reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){


                Toast.makeText(getApplicationContext(), "초기화 되었습니다.", Toast.LENGTH_SHORT).show();
                editor.clear();
                editor.commit();
                finish();

            }
        });

        SaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){

                checkedResult="";   //저장이 되기 전 초기화

                for (String item : checkedObs) {    //체크된 obstacle 수만큼 추가
                    checkedResult += item + ",";
                }

                //Toast.makeText(getApplicationContext(), Integer.toString(checkedObs.size()), Toast.LENGTH_SHORT).show();
                if(checkedResult=="")
                    Toast.makeText(getApplicationContext(), "체크가 되지 않았습니다.", Toast.LENGTH_SHORT).show();
                else
                {

                    checkedResult = checkedResult.substring(0, checkedResult.length()-1);

                    Toast.makeText(getApplicationContext(), checkedResult, Toast.LENGTH_SHORT).show();
                    //intent.putExtra("obstacle",checkedResult);
                    //startActivity(intent);


                    editor.putString("obstacle",checkedResult);// key, value를 이용하여 저장하는 형태
                    editor.commit();
                }



            }
        });



    }

        @Override
        protected void onDestroy ()
        {
            super.onDestroy();

        }
        //ScrollView에서 ListView의 높이를 설정 (그렇지 않으면 리스트 뷰는 0을 반환)
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }



}
