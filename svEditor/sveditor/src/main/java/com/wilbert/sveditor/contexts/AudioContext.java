package com.wilbert.sveditor.contexts;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.wilbert.sveditor.clips.abs.IFrameWorker;
import com.wilbert.sveditor.codecs.abs.FrameInfo;
import com.wilbert.sveditor.contexts.abs.ITimeline;
import com.wilbert.sveditor.log.ALog;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * author : Administrator
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public class AudioContext {
    private final String TAG = "AudioContext";
    private AtomicBoolean mPlaying = new AtomicBoolean(true);
    private IFrameWorker mWorker;
    private ITimeline mTimeline;
    private LinkedBlockingDeque<FrameInfo> mFrameInfos = new LinkedBlockingDeque<>(1);
    private AudioTrack mAudioTrack;
    private boolean mPrepared = false;
    private int mSessionId = -1;
    private final int EMPTY_WAITING = 5_000;//us
    private Object mLock = new Object();

    public AudioContext(IFrameWorker frameWorker, ITimeline timeline, int audioSessionId) {
        this.mWorker = frameWorker;
        this.mTimeline = timeline;
        this.mSessionId = audioSessionId;
        new Thread(mFrameRunnable, TAG + "_worker").start();
        new Thread(mRenderRunnable, TAG + "_render").start();
    }

    private Runnable mFrameRunnable = new Runnable() {


        @Override
        public void run() {
            ALog.i(TAG, "worker started");
            while (mPlaying.get()) {
                if (mWorker != null) {
                    FrameInfo frameInfo = mWorker.getNextFrame();
                    if (frameInfo != null) {
                        try {
                            mFrameInfos.putFirst(frameInfo);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    ALog.i(TAG, "no worker and quit");
                    mPlaying.set(false);
                }
            }
        }
    };

    private Runnable mRenderRunnable = new Runnable() {
        private boolean mFirstFrame = true;

        @Override
        public void run() {
            while (mPlaying.get()) {
                FrameInfo frameInfo = null;
                try {
                    frameInfo = mFrameInfos.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (frameInfo == null || frameInfo.size <= 0) {
                    ALog.i(TAG, "renderer null frameInfo");
                    continue;
                }
                if (mFirstFrame) {
                    mTimeline.start();
                    mFirstFrame = false;
                }
                if (!mPrepared) {
                    initAudioTrack(frameInfo);
                }
                long waiting = mTimeline.compareTime(frameInfo.presentationTimeUs);
                while (waiting > 0) {
                    try {
                        synchronized (mLock) {
                            long timeWait = waiting / 1000;
                            if (timeWait > 0) {
                                mLock.wait(timeWait);
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    waiting = mTimeline.compareTime(frameInfo.presentationTimeUs);
                }
                playAudio(frameInfo);
                mWorker.releaseFrame(frameInfo);
            }
            releaseAudioTrack();
        }
    };

    private void initAudioTrack(FrameInfo frameInfo) {
        if (frameInfo == null) {
            return;
        }
        AudioAttributes attributes = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC).build();
        int channelConfig = frameInfo.channels <= 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        AudioFormat audioFormat = new AudioFormat.Builder().setSampleRate(frameInfo.sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(channelConfig).build();
        int minBufferSize = AudioTrack.getMinBufferSize(frameInfo.sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
        mAudioTrack = new AudioTrack(attributes, audioFormat, minBufferSize, AudioTrack.MODE_STREAM, mSessionId);
        mPrepared = true;
    }

    private void playAudio(FrameInfo frameInfo) {
        if (frameInfo == null || frameInfo.size <= 0 || !mPrepared) {
            return;
        }
        try {
            mAudioTrack.play();
            mAudioTrack.write(frameInfo.outputBuffer, frameInfo.size, AudioTrack.WRITE_NON_BLOCKING/*one of {@link #WRITE_BLOCKING}, {@link #WRITE_NON_BLOCKING}. It has no effect in static mode.*/);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void onFlush() {
        if (mAudioTrack != null) {
            mAudioTrack.flush();
        }
    }

    private void releaseAudioTrack() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
            mPrepared = false;
        }
    }

    public void release() {
        if (!mPlaying.get())
            return;
        mPlaying.set(false);
        if (mFrameInfos != null) {
            mFrameInfos.offer(new FrameInfo(null, -1, -1, -1));
        }
    }
}
