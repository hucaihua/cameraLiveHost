package com.example.cameralivehost;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowMetrics;

public class MainActivity extends AppCompatActivity implements SocketLive.SocketCallback {

    LocalSurfaceView localSurfaceView;
    Surface surface;
    H264Decoder decoder;
    SurfaceView removeSurfaceView;
    SocketLive socketLive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        socketLive = new SocketLive(this);
//        socketLive.close();
        initView();
        checkPermission();
    }

    private void initView() {
        localSurfaceView = findViewById(R.id.localSurfaceView);
        removeSurfaceView = findViewById(R.id.removeSurfaceView);
        removeSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                surface = holder.getSurface();
                decoder = new H264Decoder(surface , getWindowManager().getDefaultDisplay().getWidth() ,  getWindowManager().getDefaultDisplay().getHeight());

            }
            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }
            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });

    }
    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
            }, 1);

        }
        return false;
    }

    @Override
    public void callBack(byte[] data) {
        decoder.decodeFrame(data);
    }

    public void connect(View view) {


        socketLive.start();
        localSurfaceView.startCapture(socketLive);
    }

    @Override
    public void finish() {
        super.finish();
        socketLive.close();
    }
}