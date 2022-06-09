/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.camera2.CameraManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.DetectorFactory;
import org.tensorflow.lite.examples.detection.tflite.YoloV5Classifier;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.3f;
    private static final boolean MAINTAIN_ASPECT = true;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 640);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private YoloV5Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;


    private MultiBoxTracker tracker;

    private BorderedText borderedText;
    MediaPlayer player;

    private BluetoothConnector bluetoothConnector;


    private final Map<Integer, String> labelTable = new HashMap<>();
    private final Map<String, String> koreanLabelTable = new HashMap<>();
    private final Map<String, String> tempTable = new HashMap<>();
    private final Map<Integer, ArrayList<Double>> avgSizeTable = new HashMap<>();
    private TextToSpeech tts;
    private MotionDetector motionDetector;
    private TOFDetector tofDetector;

    // 틀어짐 확인
    private DirectionDetector directionDetector;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tts = TextToSpeech.getInstance(this);
        motionDetector = MotionDetector.getInstance(this);
        directionDetector = DirectionDetector.getInstance(this);
//        bluetoothConnector = BluetoothConnector.getInstance(this);

        this.initLabelTable();
        this.initAvgSizeTable();
        this.recObsTable();

    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        tofDetector = new TOFDetector(this, (CameraManager) getSystemService(CAMERA_SERVICE), size);
        tofDetector.openCamera(tofDetector.getTOFCamera());

        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        final int modelIndex = modelView.getCheckedItemPosition();
        final String modelString = modelStrings.get(modelIndex);

        try {
            detector = DetectorFactory.getDetector(getAssets(), modelString);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        int cropSize = detector.getInputSize();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        Integer changedOrientation = rotation - 270;

        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        changedOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    protected void updateActiveModel() {
        // Get UI information before delegating to background
        final int modelIndex = modelView.getCheckedItemPosition();
        final int deviceIndex = deviceView.getCheckedItemPosition();
        String threads = threadsTextView.getText().toString().trim();
        final int numThreads = Integer.parseInt(threads);

        handler.post(() -> {
            if (modelIndex == currentModel && deviceIndex == currentDevice
                    && numThreads == currentNumThreads) {
                return;
            }
            currentModel = modelIndex;
            currentDevice = deviceIndex;
            currentNumThreads = numThreads;

            // Disable classifier while updating
            if (detector != null) {
                detector.close();
                detector = null;
            }

            // Lookup names of parameters.
            String modelString = modelStrings.get(modelIndex);
            String device = deviceStrings.get(deviceIndex);

            LOGGER.i("Changing model to " + modelString + " device " + device);

            // Try to load model.

            try {
                detector = DetectorFactory.getDetector(getAssets(), modelString);
                // Customize the interpreter to the type of device we want to use.
                if (detector == null) {
                    return;
                }
            }
            catch(IOException e) {
                e.printStackTrace();
                LOGGER.e(e, "Exception in updateActiveModel()");
                Toast toast =
                        Toast.makeText(
                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }


            if (device.equals("CPU")) {
                detector.useCPU();
            } else if (device.equals("GPU")) {
                detector.useGpu();
            } else if (device.equals("NNAPI")) {
                detector.useNNAPI();
            }
            detector.setNumThreads(numThreads);

            int cropSize = detector.getInputSize();
            croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

            frameToCropTransform =
                    ImageUtils.getTransformationMatrix(
                            previewWidth, previewHeight,
                            cropSize, cropSize,
                            sensorOrientation, MAINTAIN_ASPECT);

            cropToFrameTransform = new Matrix();
            frameToCropTransform.invert(cropToFrameTransform);
        });
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
            new Runnable() {
                @Override
                public void run() {
                    if (tts != null && tts.isLoading()) {
                        tts.readText("로딩이 끝났습니다.");
                        tts.setLoading(false);
                    }

                    LOGGER.i("Running detection on image " + currTimestamp);
                    final long startTime = SystemClock.uptimeMillis();
                    final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                    lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                    Log.d("ProcessTime", Long.toString(lastProcessingTimeMs) + "ms");

                    Log.e("CHECK", "run: " + results.size());

                    cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                    final Canvas canvas = new Canvas(cropCopyBitmap);
                    final Paint paint = new Paint();
                    paint.setColor(Color.RED);
                    paint.setStyle(Style.STROKE);
                    paint.setStrokeWidth(2.0f);

                    float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                    switch (MODE) {
                        case TF_OD_API:
                            minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                            break;
                    }

                    final List<Classifier.Recognition> mappedRecognitions =
                            new LinkedList<Classifier.Recognition>();

                    /** Custom depth map **/
                    final List<Integer> mappedDepth =
                            new LinkedList<>();

                    for (final Classifier.Recognition result : results) {
                        final RectF location = result.getLocation();
                        if (location != null && result.getConfidence() >= minimumConfidence) {
                            canvas.drawRect(location, paint);

                            cropToFrameTransform.mapRect(location);

                            result.setLocation(location);
                            mappedRecognitions.add(result);

                            /** Custom depth map **/
                            int y = (int) location.centerY();
                            int x = (int) location.centerX();
                            int depth = getDetectedDepth(x, y);
//                            List<Integer> coord = new LinkedList<>();
//                            coord.add(x);
//                            coord.add(y);
                            mappedDepth.add(depth);
                        }
                    }

                    tracker.trackResults(mappedRecognitions, mappedDepth, currTimestamp);
                    trackingOverlay.postInvalidate();

                    computingDetection = false;

                    runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                showFrameInfo(previewWidth + "x" + previewHeight);
                                showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                                showInference(lastProcessingTimeMs + "ms");
                            }
                        }
                    );

                    /** Custom **/
                    //tts stop() 로직 구현
                    //tts.stop();

                    if ((tts != null && !tts.IsSpeaking())){
                        if (MotionDetector.isDetectMode() && !motionDetector.isMoving() && (tts.getLastSpokeTimePassed() > MotionDetector.getFrequency())) {
                            // *--정지 모드 안내 실행
                            readDetectedData(mappedRecognitions);

                        }
                        else if (tts.getLastSpokeTimePassed() > TextToSpeech.getFrequency()) {
                            // 일반 안내 실행
                              readDetectedData(mappedRecognitions);
                            // alertClosestObj(mappedRecognitions);
                        }

                        // 움직임 감지 및 수정(3초 간격으로 감지)
                        if (motionDetector.getLastMovedTimePassed() > 3000) {
                            motionDetector.setMovement(false);
                        }

                    }
                }
            }
        );
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
//        directionDetector.stopDirectionDetect();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }



    /** Custom Methods **/
    // 라벨 테이블(맵) 초기화 및 생성
    private void initLabelTable() {
        ArrayList<String> lineList = this.getFileLines("labels.txt");
        int index = 0;
        for (String line : lineList) {
            labelTable.put(index, line);
            index++;
        }

        lineList = this.getFileLines("korean_labels.txt");
        for (String line : lineList) {
            String[] labels = line.split(",");
            String englishLabel = labels[0];
            String koreanLabel = labels[1];
            koreanLabelTable.put(englishLabel, koreanLabel);
        }
    }

    // 사물 평균 크기 테이블 초기화 및 생성
    private void initAvgSizeTable() {
        ArrayList<String> lineList = this.getFileLines("avgsize.txt");
        for (String line : lineList) {
            String[] sizeInfo = line.split(" ");
            int classIndex = Integer.parseInt(sizeInfo[0]);
            double avgWidth = Double.parseDouble(sizeInfo[1]);
            double avgHeight = Double.parseDouble(sizeInfo[2]);
            avgSizeTable.put(classIndex, new ArrayList<Double>(Arrays.asList(avgWidth, avgHeight)));
        }
    }
    // 장애물 체크된 테이블 초기화 및 생성
    private void recObsTable(){
        SharedPreferences sf = getSharedPreferences("obstacle_list",MODE_PRIVATE); //obstacle key에 저장된 값이 있는지 확인. 아무값도 들어있지 않으면 ""를 반환
        String obstacle = sf.getString("obstacle","");
        String[] array = obstacle.split(",");

        //장애물 체크를 하지 않았을경우 전체 장애물 식별
        if(obstacle =="")
        {
            for (String key : koreanLabelTable.keySet()) {
                String value=koreanLabelTable.get(key);
                for (int i = 0; i < koreanLabelTable.size(); i++) {
                    tempTable.put(key, value);
                }
            }
        }
        else {

            for (String key : koreanLabelTable.keySet()) {
                String value = koreanLabelTable.get(key);
                for (int i = 0; i < array.length; i++) {
                    if (value.equals(array[i]))
                        tempTable.put(key, value);
                }
            }
        }
    }

    // 파일을 읽고 각 줄을 String 형태로 리스트에 담아서 반환하는 메소드
    private ArrayList<String> getFileLines(String fileName) {
        try {
            AssetManager am = getResources().getAssets();
            InputStream is = am.open(fileName);
            BufferedReader bf = new BufferedReader(new InputStreamReader(is));
            ArrayList<String> lineList = new ArrayList<>();
            String line;
            while ((line = bf.readLine()) != null) {
                lineList.add(line);
            }
            is.close();
            bf.close();
            return lineList;
        } catch (Exception e) {
            Log.d("File", fileName + " file not found.");
            e.printStackTrace();
            return null;
        }
    }

    // Speak label and location
    private void readDetectedData(List<Classifier.Recognition> recognitions) {
        List<Classifier.Recognition> sortedRecognition  = getCloseObj(recognitions);
        ArrayList<ArrayList<Double>> detectedLocations = getDetectedDataLocation(sortedRecognition);

        LinkedHashMap<String, HashSet<String>> map = new LinkedHashMap<>();
        map.put("왼쪽", new HashSet<>());
        map.put("중앙", new HashSet<>());
        map.put("오른쪽", new HashSet<>());
        for (int i = 0; i < sortedRecognition.size(); i++) {
            int labelIndex = sortedRecognition.get(i).getDetectedClass();
            String englishLabel = labelTable.get(labelIndex);

            if(tempTable.containsKey(englishLabel))
            {

                String koreanLabel = koreanLabelTable.get(englishLabel);

                ArrayList<Double> detectedLocation = detectedLocations.get(i);
                //Log.d("detectedLocation", detectedLocation.get(i) + " file not found.");

                String location = tts.inputLocation(detectedLocation);

                map.get(location).add(koreanLabel); //해당 위치의 hashmap 장소 추가

                }
            }


        tts.readLocations(map);
    }

    private boolean readDetectedDepth(List<Classifier.Recognition> recognitions){

        boolean state=true;

        List<Classifier.Recognition> sortedRecognition  =
                getSortedDetectedDataList(getFilteredDetectedDataBySize(recognitions));
        ArrayList<ArrayList<Double>> detectedLocations = getDetectedDataLocation(sortedRecognition);

        for (int i = 0; i < sortedRecognition.size(); i++) {

                ArrayList<Double> detectedLocation = detectedLocations.get(i);

                double y = detectedLocation.get(0);
                double x = detectedLocation.get(1);
                int depth = getDetectedDepth((int) x, (int) y);



                if (depth < 10) { //인식되는 물체가 일정이상 가까워지면 tts를 종료하고 비프음을 울림
                    tts.stop();
                    tts.makeBeep();


                    state=false;
                }
                else
                    state=true;
        }

        return state;
    }


    //private void
    private int getDetectedDepth(int x, int y) {
        ArrayList<Integer> target = new ArrayList<>();
        target.add(x);
        target.add(y);
        if (!tofDetector.isTOFAvailable()) return 0;
        else return tofDetector.getTargetDistance(target);
    }

    // Get detected data according to confidence and location
    private List<Classifier.Recognition> getSortedDetectedDataList(List<Classifier.Recognition> recognitions) {
        if(recognitions.size() >= 2) {
            Classifier.Recognition temp;
            for (int i = 0; i < recognitions.size(); i++) {
                for (int j = 0; j < (recognitions.size() - 1); j++) {
                    if (recognitions.get(j).getConfidence() < recognitions.get(j + 1).getConfidence()) {
                        temp = recognitions.get(j);
                        recognitions.remove(j);
                        recognitions.add(j + 1, temp);
                    }
                }
            }
        }
        return recognitions;
    }

    // 가장 가까운 사물 검출 후 알림
    private void alertClosestObj(List<Classifier.Recognition> recognitions) {
        if(recognitions.size() > 1){
            Float maxY = -1000000f;
            int minIndex = 0;
            int i = 0;
            for (Classifier.Recognition recognition : recognitions) {
                Float y = recognition.getLocation().centerX();
                Float height = recognition.getLocation().width();
                Float bottomY = y + (height / 2);
                if (bottomY > maxY) {
                    maxY = bottomY;
                    minIndex = i;
                }
                ++i;
            }

            Classifier.Recognition minRecognition = recognitions.get(minIndex);
            String label = labelTable.get(minRecognition.getDetectedClass());
            String korLabel = koreanLabelTable.get(label);
            Float height = minRecognition.getLocation().width();
            tts.readTextWithInterference(korLabel + "." + String.valueOf(height));
        }
    }

    // Select object with bottomY below previewHeight
    private List<Classifier.Recognition> getCloseObj (List<Classifier.Recognition> recognitions) {
        List<Classifier.Recognition> closeObj = new ArrayList<>();
        if(recognitions.size() < 2) {
            closeObj = recognitions;
        }
        else {
            for (Classifier.Recognition recognition : recognitions) {
                Float y = recognition.getLocation().centerX();
                Float height = recognition.getLocation().width();
                Float bottomY = y + (height / 2);
                if(bottomY >= (previewWidth * 0.7) ) {
                    closeObj.add(recognition);
                }
            }
        }
        return closeObj;
    }


    private int getRandDetectedData(List<Classifier.Recognition> recognitions) {
        if (recognitions.size() == 0) { return -1; }
        int randData = ThreadLocalRandom.current().nextInt(0, recognitions.size());
        return randData;
    }

    private ArrayList<ArrayList<Double>> getDetectedDataLocation(List<Classifier.Recognition> recognitions) {
        ArrayList<ArrayList<Double>> detectedLocations = new ArrayList<>();

        for (Classifier.Recognition recognition : recognitions) {
            ArrayList<Double> locations = new ArrayList<>();
            RectF rectLocation = recognition.getLocation();
            locations.add((double) rectLocation.centerY());
            locations.add((double) rectLocation.centerX());
            locations.add((double) rectLocation.height());
            locations.add((double) rectLocation.width());
            detectedLocations.add(locations);
        }
        return detectedLocations;
    }



    // 평균 크기에 따른 필터링
    private List<Classifier.Recognition> getFilteredDetectedDataBySize(List<Classifier.Recognition> recognitions) {
        List<Classifier.Recognition> newRecognitions = new LinkedList<>(); // 새로 반환할 리스트
        int inputSize = detector.getInputSize();
        for (Classifier.Recognition recognition : recognitions) {
            int labelIndex = recognition.getDetectedClass();
            RectF rectLocation = recognition.getLocation();
            double width = rectLocation.width();
            double height = rectLocation.height();

            ArrayList<Double> avgSizeInfo = avgSizeTable.get(labelIndex);
            if (avgSizeInfo != null) {
                double avgWidth = avgSizeInfo.get(0) * previewWidth;
                double avgHeight = avgSizeInfo.get(1) * previewHeight;

                if (width > avgWidth && height > avgHeight) {
                    newRecognitions.add(recognition);
                }
            }
            else { // 평균 크기 정보가 없으면 일단 그냥 반환값에 넣어둠
                newRecognitions.add(recognition);
            }
        }
        return newRecognitions;
    }

    @Override

    public void onDestroy() {

        if (tts != null) {

            tts.stop();

        }

        super.onDestroy();

    }
}
