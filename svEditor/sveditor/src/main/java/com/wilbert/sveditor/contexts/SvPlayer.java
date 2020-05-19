package com.wilbert.sveditor.contexts;

import android.content.Context;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.text.TextUtils;

import com.wilbert.sveditor.clips.AudioClip;
import com.wilbert.sveditor.clips.VideoClip;
import com.wilbert.sveditor.clips.abs.IPreparedListener;
import com.wilbert.sveditor.codecs.SvMediaExtractorWrapper;
import com.wilbert.sveditor.contexts.abs.IPlayer;
import com.wilbert.sveditor.contexts.abs.IPrepareListener;

import java.util.concurrent.Semaphore;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/19
 * desc   :
 */
public class SvPlayer implements IPlayer {
    AudioContext mAudioContext;
    VideoContext mVideoContext;
    VideoClip mVideoClip;
    AudioClip mAudioClip;
    Timeline mTimeline;
    AudioManager mAudioManager;
    String mFilepath;
    IPrepareListener mPreparedLister;
    Semaphore mPreparePhore = new Semaphore(2);

    int mAudioSession = -1;

    public SvPlayer(Context mContext) {
        if (mContext == null)
            return;
        mTimeline = new Timeline();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mAudioSession = mAudioManager.generateAudioSessionId();
    }

    @Override
    public void setDataSource(String filepath) {
        mFilepath = filepath;
    }

    @Override
    public void prepare(GLSurfaceView surfaceView) {
        if (TextUtils.isEmpty(mFilepath))
            return;
        mVideoClip = new VideoClip();
        mVideoClip.prepare(mFilepath);
        mVideoClip.setOnPreparedListener(mVideoListener);
        mVideoContext = new VideoContext(surfaceView, mVideoClip, mTimeline);

        mAudioClip = new AudioClip();
        mAudioClip.prepare(mFilepath);
        mAudioClip.setOnPreparedListener(mAudioListener);
        mAudioContext = new AudioContext(mAudioClip, mTimeline, mAudioSession);
    }

    @Override
    public void setOnPreparedListener(IPrepareListener listener) {
        mPreparedLister = listener;
    }

    @Override
    public long getDuration() {
        if (mPreparePhore.availablePermits() > 0) {
            return 0;
        }
        return Math.max(mAudioClip.getDuration(), mVideoClip.getDuration());
    }

    @Override
    public long getCurrentTimeUs() {
        return mTimeline.getCurrentTime();
    }

    @Override
    public boolean isPlaying() {
        return mTimeline.isPlaying();
    }

    @Override
    public void start() {
        mTimeline.start();
    }

    @Override
    public void pause() {
        mTimeline.pause();
        mVideoContext.pause();
    }

    @Override
    public void seekTo(long timeUs) {
        mTimeline.seekTo(timeUs);
    }

    @Override
    public void release() {
        mVideoClip.release();
        mAudioContext.release();
        mAudioClip.release();
    }

    IPreparedListener mAudioListener = new IPreparedListener() {
        @Override
        public void onPrepared(SvMediaExtractorWrapper extractorWrapper) {
            mPreparePhore.tryAcquire(1);
            if (mPreparePhore.availablePermits() <= 0 && mPreparedLister != null) {
                mPreparedLister.onPrepared(SvPlayer.this);
            }
        }
    };

    IPreparedListener mVideoListener = new IPreparedListener() {
        @Override
        public void onPrepared(SvMediaExtractorWrapper extractorWrapper) {
            mPreparePhore.tryAcquire(1);
            if (mPreparePhore.availablePermits() <= 0 && mPreparedLister != null) {
                mPreparedLister.onPrepared(SvPlayer.this);
            }
        }
    };
}
