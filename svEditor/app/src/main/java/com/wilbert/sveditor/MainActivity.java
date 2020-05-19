package com.wilbert.sveditor;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.wilbert.sveditor.utils.FileUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "MainActivity";
    public static final int REQUEST_CODE_VIDEO_COMPRESS = 1;
    public static final int PERMISSION_REQUEST_VIDEO_COMPRESS = 1;

    public Button btnCompress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnCompress = findViewById(R.id.btn_compress);
        btnCompress.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_compress:
                startCompress();
                break;
        }
    }

    public void startCompress() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (shouldShowRequestPermissionRationale(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                }
                requestPermissions(new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_VIDEO_COMPRESS);
            } else {
                selectVideoFile(REQUEST_CODE_VIDEO_COMPRESS);
            }
        } else {
            selectVideoFile(REQUEST_CODE_VIDEO_COMPRESS);
        }
    }

    private void selectVideoFile(int code) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");// 设置类型，我这里是任意类型，任意后缀的可以这样写。
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, code);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VIDEO_COMPRESS) {
            if (resultCode == RESULT_OK) {
                Uri uri = data.getData();
                if (uri != null) {
                    String path = FileUtils.getUriPath(this, uri);
                    if (path != null) {
                        File file = new File(path);
                        if (file.exists()) {
                            Log.i(TAG, path);
                            startActivity(EditActivity.createIntent(this, path, 1));
                        }
                    }
                }
            }
        }
    }
}
