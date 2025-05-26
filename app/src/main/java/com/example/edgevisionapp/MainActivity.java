package com.example.edgevisionapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;

import com.example.edgevisionapp.gl.GLRenderer;
import com.example.edgevisionapp.gl.GLView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    // Load native C++ library
    static {
        System.loadLibrary("native-lib");
    }

    // JNI method declaration
    public native void processFrame(long inputMatAddr, long outputMatAddr);

    // UI and camera
    private TextureView textureView;
    private GLView glView;
    private GLRenderer glRenderer;

    // Camera2 components
    private ImageReader imageReader;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private Size previewSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glView.setZOrderOnTop(true);
        glView.bringToFront(); // ✅ Force it to top


        // Setup OpenGL surface and renderer
        glRenderer = new GLRenderer();
        glView = new GLView(this, glRenderer);

        //FrameLayout container = findViewById(R.id.gl_container);
        //container.addView(glView);
        glView.setZOrderOnTop(true);

        textureView = findViewById(R.id.textureView);

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed");
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully");
            // Uncomment to test with static image from assets
            // testProcessFrameWithStaticImage();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();

        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }

    @Override
    protected void onPause() {
        stopBackgroundThread();
        closeCamera();
        super.onPause();
    }

    private final TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            openCamera();
        }

        @Override public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
            return false;
        }

        @Override public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
    };

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            if (map != null) {
                previewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            } else {
                Log.e("Camera", "StreamConfigurationMap is null.");
                return;
            }

            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
                return;
            }

            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e("Camera", "Camera access error: " + e.getMessage());
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            Log.e("Camera", "Camera error: " + error);
        }
    };

    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            if (texture == null) {
                Log.e("Camera", "SurfaceTexture is null");
                return;
            }
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            Surface previewSurface = new Surface(texture);
            Surface imageSurface = imageReader.getSurface();

            final CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(previewSurface);
            builder.addTarget(imageSurface);

            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, imageSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                session.setRepeatingRequest(builder.build(), null, backgroundHandler);
                            } catch (CameraAccessException e) {
                                Log.e("Camera", "Failed to start camera preview: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.e("Camera", "Camera preview configuration failed");
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e("Camera", "Failed to create camera preview: " + e.getMessage());
        }
    }

    private final ImageReader.OnImageAvailableListener onImageAvailableListener = reader -> {
        Image image = reader.acquireLatestImage();
        if (image != null) {
            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            processCameraFrame(nv21, image.getWidth(), image.getHeight());
            image.close();
        }
    };

    public void processCameraFrame(byte[] yuvData, int width, int height) {
        Mat yuv = new Mat(height + height / 2, width, CvType.CV_8UC1);
        yuv.put(0, 0, yuvData);

        Mat rgbaInput = new Mat();
        Imgproc.cvtColor(yuv, rgbaInput, Imgproc.COLOR_YUV2RGBA_NV21);

        Mat rgbaOutput = new Mat(height, width, CvType.CV_8UC4);

        processFrame(rgbaInput.getNativeObjAddr(), rgbaOutput.getNativeObjAddr());

        Bitmap bitmap = Bitmap.createBitmap(rgbaOutput.cols(), rgbaOutput.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rgbaOutput, bitmap);

        runOnUiThread(() -> glView.updateBitmap(bitmap));

        yuv.release();
        rgbaInput.release();
        rgbaOutput.release();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e("Thread", "Failed to stop background thread: " + e.getMessage());
            }
        }
    }

    /*
    // Optional test method for static image processing
    private void testProcessFrameWithStaticImage() {
        try {
            InputStream inputStream = getAssets().open("test.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            Mat rgbaInput = new Mat();
            Utils.bitmapToMat(bitmap, rgbaInput);
            Imgproc.cvtColor(rgbaInput, rgbaInput, Imgproc.COLOR_BGR2RGBA);

            Mat rgbaOutput = new Mat(rgbaInput.rows(), rgbaInput.cols(), CvType.CV_8UC4);

            processFrame(rgbaInput.getNativeObjAddr(), rgbaOutput.getNativeObjAddr());

            Bitmap outputBitmap = Bitmap.createBitmap(rgbaOutput.cols(), rgbaOutput.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(rgbaOutput, outputBitmap);

            runOnUiThread(() -> glView.updateBitmap(outputBitmap));

            rgbaInput.release();
            rgbaOutput.release();

        } catch (IOException e) {
            Log.e("Test", "Failed to load test image: " + e.getMessage());
        }
    }
    */
}
