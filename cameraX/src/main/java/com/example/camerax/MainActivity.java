package com.example.camerax;

import static androidx.camera.core.CameraSelector.LENS_FACING_BACK;
import static androidx.camera.core.CameraSelector.LENS_FACING_FRONT;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.camerax.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private com.example.camerax.databinding.ActivityMainBinding mViewBinding;
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private ExecutorService mExecutorService;
    private ExecutorService mCameraExecutor;
    private boolean mIsBackCamera = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
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
        //创建 ProcessCameraProvider 的实例
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        //向 cameraProviderFuture 添加监听器
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                //用于将摄像机的生命周期绑定到生命周期所有者
                try {
                    //将相机的生命周期绑定到应用进程中的 LifecycleOwner
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        //初始化 Preview 对象，在其上调用 build，从取景器中获取 Surface 提供程序，然后在预览上进行设置
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();
        // 设置预览界面
        preview.setSurfaceProvider(mViewBinding.viewFinder.getSurfaceProvider());
        //创建 CameraSelector 对象,默认选择 DEFAULT_BACK_CAMERA(后置摄像头)
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(mIsBackCamera ? LENS_FACING_BACK : LENS_FACING_FRONT)
                .build();
        Executor executor = Executors.newSingleThreadExecutor();

        // 在重新绑定之前取消绑定用例
        cameraProvider.unbindAll();

        // 将用例绑定到相机
        cameraProvider.bindToLifecycle(
                this, cameraSelector, preview);
    }


    private void takePhoto() {
    }

    private void captureVideo() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            //已授予权限，请调用 startCamera()
            if (allPermissionsGranted()) {
                startCamera();
            } else {//未授予权限，显示一个消息框，通知用户未授予权限
                Toast.makeText(this,
                        "用户未授予权限",
                        Toast.LENGTH_SHORT).show();
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