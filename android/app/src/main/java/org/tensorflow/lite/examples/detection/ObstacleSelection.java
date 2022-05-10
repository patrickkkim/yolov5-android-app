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
    private Button AllChoiceButton, SaveButton;
    private String checkedResult="";
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obstacle_selection);
        
        String checked = null;
        boolean obsChecked[]= new boolean[20];
        Arrays.fill(obsChecked,false);

        textView=(TextView)findViewById(R.id.textView);
        AllChoiceButton=(Button)findViewById(R.id.allchoicebutton);
        SaveButton=(Button)findViewById(R.id.savebutton);


        ArrayList<String> obstacles = new ArrayList<String>();
        ArrayList<String> checkedObs = new ArrayList<String>();
        // ArrayAdapter 생성. 아이템 View를 선택(multiple choice)가능하도록 만듦.
        obstacles.add("truck");
        obstacles.add("tree_trunk");
        obstacles.add("traffic_light");
        obstacles.add("scooter");
        obstacles.add("potted_plant");
        obstacles.add("pole");
        obstacles.add("person");
        obstacles.add("movable_signage");
        obstacles.add("motorcycle");
        obstacles.add("kiosk");
        obstacles.add("fire_hydrant");
        obstacles.add("chair");
        obstacles.add("carrier");
        obstacles.add("car");
        obstacles.add("bus");
        obstacles.add("caution_zone");
        obstacles.add("bike_lane");
        obstacles.add("alley");
        obstacles.add("roadway");
        obstacles.add("braille_guide_blocks");
        obstacles.add("sidewalk");

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

        AllChoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){

                for(int i=0; i<obsChecked.length; i++)
                {
                    obsChecked[i]=true;
                }
                Toast.makeText(getApplicationContext(), "전체 선택", Toast.LENGTH_SHORT).show();

            }
        });

        SaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){

                //Toast.makeText(getApplicationContext(), Integer.toString(checkedObs.size()), Toast.LENGTH_SHORT).show();


                for (String item : checkedObs) {
                    checkedResult += item + ",";
                }
                checkedResult = checkedResult.substring(0, checkedResult.length()-1);

                Toast.makeText(getApplicationContext(), checkedResult, Toast.LENGTH_SHORT).show();
                //intent.putExtra("obstacle",checkedResult);
                //startActivity(intent);
                SharedPreferences sharedPreferences = getSharedPreferences("obstacle_list",MODE_PRIVATE); //저장을 하기위해 editor를 이용하여 값을 저장시켜준다.
                SharedPreferences.Editor editor = sharedPreferences.edit();

                editor.putString("obstacle",checkedResult);// key, value를 이용하여 저장하는 형태
                editor.commit();

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
