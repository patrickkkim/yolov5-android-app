package org.tensorflow.lite.examples.detection;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TOFDetector extends CameraDevice.StateCallback {
    private static int SENSOR_WIDTH = 0;
    private static int SENSOR_HEIGHT = 0;
    private static int SCREEN_WIDTH = 0;
    private static int SCREEN_HEIGHT = 0;
    private static float WIDTH_RATIO = 0;
    private static float HEIGHT_RATIO = 0;
    private static int[] depthMask;

    private Context context;
    private CameraManager cameraManager;
    private ImageReader previewReader;
    private boolean isTOFAvailable = false;

    public TOFDetector(Context con, CameraManager camMan, Size size) {
        context = con;
        cameraManager = camMan;
        SCREEN_WIDTH = size.getWidth();
        SCREEN_HEIGHT = size.getHeight();
    }

    public String getTOFCamera() {
        try {
            for (String camera : cameraManager.getCameraIdList()) {
                CameraCharacteristics chars = cameraManager.getCameraCharacteristics(camera);
                final int[] capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

                boolean backFacing = (chars.get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK);
                boolean depthCapable = false;
                for (int capability : capabilities) {
                    boolean capable = (capability == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT);
                    depthCapable = (depthCapable || capable);
                }

                if (depthCapable && backFacing) {
                    // 센서 크기는 실제 캡쳐 화면 크기보다 클 수 있다.
                    SizeF sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                    Log.i("TOF", "Sensor Size = " + sensorSize);
                    if (sensorSize != null) {
                        float width = sensorSize.getWidth();
                        float height = sensorSize.getHeight();
                        SENSOR_WIDTH = (int) (width * 100);
                        SENSOR_HEIGHT = (int) (height * 100);
                        this.computeScreenRatio();
                    }

                    // FOV 리사이징용 정보 출력
                    float[] focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                    if (focalLengths.length > 0) {
                        float focalLength = focalLengths[0];
                        double fov = 2 * Math.atan(sensorSize.getWidth() / (2 * focalLength));
                        Log.i("TOF", "Computed FOV = " + fov);
                    }
                    isTOFAvailable = true;
                    return camera;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void openCamera(String cameraID) {
        if (!isTOFAvailable) {
            Log.e("TOF", "TOF camera not available.");
            return;
        }
        try {
            int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
            if (PackageManager.PERMISSION_GRANTED == permission) {
                cameraManager.openCamera(cameraID, this, null);
            }
            else {
                Log.e("TOF", "Camera Permission Not Available");
            }
        }
        catch (Exception e) {
            Log.e("TOF", "Open Camera Exception" + e);
            e.printStackTrace();
        }
    }

    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
        DepthFrameAvailableListener imageAvailableListener = new DepthFrameAvailableListener(context);
        previewReader = ImageReader.newInstance(SENSOR_WIDTH, SENSOR_HEIGHT, ImageFormat.DEPTH16, 2);
        previewReader.setOnImageAvailableListener(imageAvailableListener, null);
        try {
            CaptureRequest.Builder previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
            Range<Integer> fpsRange = new Range<>(15, 30);
            previewBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);
            previewBuilder.addTarget(previewReader.getSurface());

            List<Surface> targetSurfaces = Arrays.asList(previewReader.getSurface());
            cameraDevice.createCaptureSession(
                    targetSurfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.i("TOF", "Capture Session Created");
                            previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                            try {
                                cameraCaptureSession.setRepeatingRequest(previewBuilder.build(), null, null);
                            }
                            catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e("TOF", "Camera Capture Session Callback Internal Error!!");
                        }
                    }, null
            );
        }
        catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) { }
    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int i) { }



    public int getTargetDistance(ArrayList<Integer> target) {
        float widthRatio = TOFDetector.getWidthRatio();
        float heightRatio = TOFDetector.getHeightRatio();
        ArrayList<Integer> interpolatedCoord = this.interpolateCoord(target, widthRatio, heightRatio);
        int distance = this.getDistance(interpolatedCoord.get(0), interpolatedCoord.get(1));
        return distance;
    }

    private int interpolatePoint(float ratio, int point) {
        float result = point * ratio;
        return (int) result;
    }

    private ArrayList<Integer> interpolateCoord(ArrayList<Integer> target, float widthRatio, float heightRatio) {
        int x = target.get(0);
        int y = target.get(1);
        int newX = this.interpolatePoint(widthRatio, x);
        int newY = this.interpolatePoint(heightRatio, y);
        ArrayList<Integer> newCoord = new ArrayList<>();
        newCoord.add(newX);
        newCoord.add(newY);
        return newCoord;
    }

    private int getDistance(int x, int y) {
        int index = y * SENSOR_WIDTH + x;
        return depthMask[index];
    }

    private void computeScreenRatio() {
        WIDTH_RATIO = (float) SENSOR_WIDTH / SCREEN_WIDTH;
        HEIGHT_RATIO = (float) SENSOR_HEIGHT / SCREEN_HEIGHT;
    }


    public static int getSensorWidth() {
        return SENSOR_WIDTH;
    }
    public static int getSensorHeight() {
        return SENSOR_HEIGHT;
    }
    public static float getWidthRatio() {
        return WIDTH_RATIO;
    }
    public static float getHeightRatio() {
        return HEIGHT_RATIO;
    }
    public static int[] getDepthMask() {
        return depthMask;
    }
    public static void setDepthMask(int[] depthMask) {
        TOFDetector.depthMask = depthMask;
    }
    public boolean isTOFAvailable() {
        return isTOFAvailable;
    }
    public void setTOFAvailable(boolean TOFAvailable) {
        isTOFAvailable = TOFAvailable;
    }
}
