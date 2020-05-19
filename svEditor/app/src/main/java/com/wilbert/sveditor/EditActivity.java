package com.wilbert.sveditor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wilbert.sveditor.contexts.SvPlayer;
import com.wilbert.sveditor.contexts.abs.IPlayer;
import com.wilbert.sveditor.contexts.abs.IPrepareListener;

public class EditActivity extends AppCompatActivity implements View.OnClickListener {

    final String TAG = "EditActivity";
    GLSurfaceView mSurfaceView;
    TextView mPauseBtn, mSeekBtn;
    SeekBar mSeekBar;
    SvPlayer mPlayer;

    public static Intent createIntent(Context context, String filepath, int simpleCompress) {
        Intent intent = new Intent(context, EditActivity.class);
        intent.putExtra("uri", filepath);
        intent.putExtra("simplecompress", simpleCompress);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        mSurfaceView = (GLSurfaceView) findViewById(R.id.gl_surfaceview);

        mPauseBtn = findViewById(R.id.btn_pause);
        mSeekBar = findViewById(R.id.seekBar);
        mPauseBtn.setOnClickListener(this);
        mSeekBtn = findViewById(R.id.btn_seek);
        mSeekBtn.setOnClickListener(this);
        String url = getIntent().getStringExtra("uri");

        mPlayer = new SvPlayer(this);
        mPlayer.setDataSource(url);
        mPlayer.prepare(mSurfaceView);
        mPlayer.setOnPreparedListener(mPreparedListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSurfaceView.onResume();
        mPlayer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSurfaceView.onPause();
        mPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.release();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_pause:
                if (mPlayer.isPlaying()) {
                    mPlayer.pause();
                } else {
                    mPlayer.start();
                }
                break;
            case R.id.btn_seek:
                mPlayer.seekTo(mPlayer.getCurrentTimeUs() + 3000_000);
                break;
        }
    }

    IPrepareListener mPreparedListener = new IPrepareListener() {
        @Override
        public void onPrepared(final IPlayer player) {
            mSeekBar.post(new Runnable() {
                @Override
                public void run() {
                    mSeekBar.setMax((int) (player.getDuration() / 1000));
                    mSeekBar.post(posRunnable);
                }
            });
        }
    };

    private Runnable posRunnable = new Runnable() {
        @Override
        public void run() {
            mSeekBar.setProgress((int) (mPlayer.getCurrentTimeUs() / 1000));
            mSeekBar.postDelayed(posRunnable, 20);
        }
    };
}
