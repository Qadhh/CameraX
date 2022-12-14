package com.example.camerax;

import static androidx.camera.core.CameraSelector.LENS_FACING_BACK;
import static androidx.camera.core.CameraSelector.LENS_FACING_FRONT;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.camerax.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private final        String                                              TAG                      = "cameraX";
    private              com.example.camerax.databinding.ActivityMainBinding mViewBinding;
    private static final int                                                 REQUEST_CODE_PERMISSIONS = 10;
    private static final String                                              FILENAME_FORMAT          = "yyyy-MM-dd-HH-mm-ss-SSS";
    private final        String[]                                            REQUIRED_PERMISSIONS     = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private              ExecutorService                                     mExecutorService;
    private              ExecutorService                                     mCameraExecutor;
    private              boolean                                             mIsBackCamera            = true;
    private              ImageCapture                                        imageCapture             = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Set up the listeners for take photo and video capture buttons
        mViewBinding.imageCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
        mViewBinding.videoCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureVideo();
            }
        });
        mCameraExecutor = Executors.newSingleThreadExecutor();
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    private void startCamera() {
        //?????? ProcessCameraProvider ?????????
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);
        //??? cameraProviderFuture ???????????????
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                //???????????????????????????????????????????????????????????????
                try {
                    //??????????????????????????????????????????????????? LifecycleOwner
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        //????????? Preview ???????????????????????? build???????????????????????? Surface ?????????????????????????????????????????????
        Preview preview = new Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).build();
        // ??????????????????
        preview.setSurfaceProvider(mViewBinding.viewFinder.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder().build();
        //?????? CameraSelector ??????,???????????? DEFAULT_BACK_CAMERA(???????????????)
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(mIsBackCamera ? LENS_FACING_BACK :
                        LENS_FACING_FRONT).build();
        Executor executor = Executors.newSingleThreadExecutor();

        // ???????????????????????????????????????
        cameraProvider.unbindAll();

        // ????????????????????????
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }


    private void takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        if (imageCapture == null)
            return;
        String path = getDataDir() + File.separator + System.currentTimeMillis() + "CameraX.jpg";
        // Create time stamped name and MediaStore entry.
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FILENAME_FORMAT, Locale.US);
        simpleDateFormat.format(System.currentTimeMillis());

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, String.valueOf(simpleDateFormat));
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image");
        }

        //Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build();

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                String msg = "Photo capture succeeded: ${output.savedUri}";
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                Log.d(TAG, msg);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed: ${exc.message}", exception);
            }
        });
    }

    private void captureVideo() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            //??????????????????????????? startCamera()
            if (allPermissionsGranted()) {
                startCamera();
            } else {//?????????????????????????????????????????????????????????????????????
                Toast.makeText(this, "?????????????????????", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mCameraExecutor.shutdown();
    }


}